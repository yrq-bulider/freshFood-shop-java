# 线上生鲜商场 · 前端对接指南

> 配套 [`docs/API.md`](./API.md) 看。**API.md 是字典**（端点/字段/样例），**本指南是教程**（怎么用 / 为什么这么设计 / 哪些坑要避）。
>
> 阅读对象：前端开发（Vue/React/H5 都行，后端无 CORS 限制）

---

## 0. 整体认知（5 分钟看完，再写代码）

### 0.1 后端是什么

Spring Boot 3 + MyBatis-Plus + Sa-Token，6 个 Maven 模块：

| 模块 | 职责 |
|---|---|
| `freshfood-common` | 公共常量、加密、异常、响应包装 `R<T>` / `PageR<T>` |
| `freshfood-framework` | Sa-Token 配置、CORS、MyBatis-Plus 配置 |
| `freshfood-model` | 实体类（与表一一对应） |
| `freshfood-user` | 用户端 16 端点（购物、订单、评价、统一注册/登录/登出） |
| `freshfood-merchant` | 商家端 3 端点（订单、发货） |
| `freshfood-app` | 统一登录入口（app 模块，跨端） |

数据库 MySQL 5.7+ / 8.x，**一张脚本 `sql/01_init.sql` 一次建表**（9 张业务表）。

### 0.2 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│  前端 (Vue/React/H5)                                         │
│                                                              │
│  ┌─────────────────┐            ┌─────────────────────┐     │
│  │  user 端 (USER) │            │ merchant 端 (MERCHANT)│   │
│  │  /user/*        │            │ /merchant/*         │     │
│  └────────┬────────┘            └─────────┬───────────┘     │
│           │                               │                 │
│           └───────────────┬───────────────┘                 │
│                           │  统一 satoken Header             │
│                           ▼                                  │
└─────────────────────────────────────────────────────────────┘
                            │  HTTP + JSON
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  后端 Spring Boot 8080                                        │
│                                                              │
│  POST /api/v1/auth/login      ← 统一登录（单 user 表 + role） │
│  POST /api/v1/auth/register   ← 统一注册（role: 1=商家/2=买家）│
│                                                              │
│  /api/v1/*                ← 用户端（@SaCheckLogin）         │
│  /api/v1/merchant/*       ← 商家端（@SaCheckRole MERCHANT） │
│                                                              │
│  单 Sa-Token StpLogic：所有账号共用同一份 token              │
│  区分靠 user.role 字段 + 注解拦截                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼  JDBC
                    MySQL: freshfood_shop
                    - user（role 字段 1=商家/2=买家）
                    - merchant_profile（1:1 商家扩展信息）
```

### 0.3 两个端**不是**按 URL 路径"物理隔离"的

| 误解 | 真相 |
|---|---|
| 商家是单独端口 | 共用 8080 |
| 商家是单独域名 | 共用 `localhost:8080` |
| 前端要分两个项目 | 一个项目即可，靠 `role` 字段 + 路由守卫区分 |

---

## 1. 第一次跑通（10 分钟）

### 1.1 后端启动

```bash
# 1) 拉代码
git clone https://github.com/yrq-bulider/freshFood-shop-java.git
cd freshFood-shop-java/freshfood-shop

# 2) MySQL 起服务（本地装好的就行），建库
mysql -u root -p
> CREATE DATABASE freshfood_shop DEFAULT CHARSET utf8mb4;
> exit
mysql -u root -p freshfood_shop < sql/01_init.sql

# 3) 后端起服（JDK 17）
./mvnw -pl freshfood-app -am spring-boot:run
```

启动成功后访问 `http://localhost:8080/swagger-ui.html` 可见所有接口。

### 1.2 演示账号

| 端 | 用户名 | 密码 |
|---|---|---|
| 用户 | `zhangsan` | `123456` |
| 用户 | `lisi` | `123456` |
| 商家 | `m01` | `123456` |

> 这些账号是**演示用**，正式上线要自己注册：
> - 买家：`POST /api/v1/auth/register`（`role` 不传或传 2）
> - 商家：`POST /api/v1/auth/register`（`role: 1` + `shopName` 必填）

### 1.3 第一次调通流程（建议路径）

1. swagger-ui 找"统一登录" → Try it out
2. 填 `{"username":"zhangsan","password":"123456"}` → Execute
3. 拿到 `data.token` 和 `data.role: "USER"`
4. 复制 token → 点 swagger-ui 右上 🔒 **Authorize** → 粘贴 → Authorize
5. 之后所有"锁"图标接口都能在 swagger-ui 直接调通
6. 再试 `GET /api/v1/home/categories` 看返回

**到这里你就能开工了**。

---

## 2. 鉴权机制详解（最核心的一节）

### 2.1 单 user 表 + role 字段

```
                    ┌─ ① user.role 字段区分身份（1=商家 / 2=买家）
                    │
   登录 → 浏览器    ├─ ② 单 Sa-Token StpLogic，所有账号共用同一份 token
                    │
                    └─ ③ 商家端接口用 @SaCheckRole("MERCHANT") 拦截
```

### 2.2 登录时怎么区分

`POST /api/v1/auth/login` 是**唯一登录入口**，单 `user` 表查：

```
输入 {username, password}
  │
  ├─→ 查 user 表（按 username）
  │     │
  │     ├─ 命中，user.role = 2 → 签发 token，role = "USER"
  │     │
  │     └─ 命中，user.role = 1 → 签发 token，role = "MERCHANT"
  │
  └─→ 不命中 → 1005 用户名或密码错误
```

**前端不要自己判断**是用户还是商家，**看响应里的 `role` 字段**。

### 2.3 注册时怎么区分

**单注册端点** `POST /api/v1/auth/register`，body 加 `role` 字段：

| 场景 | body 关键字段 |
|---|---|
| 注册买家 | `role: 2`（或不传） + `nickname`/`phone` 可选 |
| 注册商家 | `role: 1` + `shopName` 必填 + `contactName`/`contactPhone`/`logo` 可选 |

商家注册成功后，`merchant_profile` 表会写一行（user_id 关联 user.id）。

### 2.4 登录后怎么区分

单 Sa-Token 体系：

| 端 | 内部用 | 前端看到的 |
|---|---|---|
| 通用 | `StpUtil.login(id)` 默认 StpLogic | token 存 Header `satoken` |

**用户和商家共用一套 token 空间**——前端不用关心内部区分，只需看响应 `role` 字段。

**前端要做**：
- 登录成功 → 存 `satoken` + `role`
- 请求拦截器统一加 Header
- 响应 401/403 → 清掉跳登录

### 2.4 前端要做的 3 件事（最小集合）

```js
// ① 登录后保存
const res = await axios.post('/api/v1/auth/login', { username, password })
const { token, role, profile } = res.data.data   // 注意是 data.data
localStorage.setItem('satoken', token)
localStorage.setItem('role', role)               // 'USER' 或 'MERCHANT'

// ② 请求拦截器加 Header
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers['satoken'] = token
  return config
})

// ③ 401/403 处理
axios.interceptors.response.use(
  resp => resp.data.code === 0 ? resp.data : Promise.reject(resp.data),
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('satoken')
      localStorage.removeItem('role')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)
```

### 2.5 路由守卫

```js
router.beforeEach((to, from, next) => {
  const role = localStorage.getItem('role')
  if (to.path.startsWith('/merchant') && role !== 'MERCHANT') {
    return next('/login')
  }
  if (to.path.startsWith('/user') && role !== 'USER') {
    return next('/login')
  }
  next()
})
```

---

## 3. 端到端业务流程（前端要按这个写）

主链路：

```
用户登录 → 浏览/搜索 → 加购物车 → 提交订单 → 支付
                                          ↓
                            [模拟支付 10% 概率失败]
                                          ↓
                            商家登录 → 商家发货 → 状态 3 待收货
                                          ↓
                            用户确认收货 → 状态 4 已完成
                                          ↓
                            用户评价
```

### 3.1 用户端流程

#### ① 登录
```js
const res = await axios.post('/api/v1/auth/login', {
  username: 'zhangsan',
  password: '123456'
})
// 存 satoken + role
```

#### ② 加购物车
```js
await axios.post('/api/v1/cart', { skuId: 2001, quantity: 2 })
// 不用等响应，加完直接跳购物车页，GET /api/v1/cart 拉列表
```

#### ③ 提交订单
```js
// 注意字段名是 cartIds，不是 cartItemIds
const order = await axios.post('/api/v1/orders', {
  cartIds: [101, 102],   // 从 GET /api/v1/cart 拿 .data.list[].id
  receiverName: '张三',
  receiverPhone: '13800138000',
  receiverAddress: '北京市朝阳区...',
  remark: '请尽快发货'    // 可选
})
// order.data.id 是数字 ID，后续接口都用这个 id
// order.data.orderId 是业务订单号（字符串 yyyyMMdd+4位），展示用
```

#### ④ 模拟支付
```js
try {
  await axios.post(`/api/v1/orders/${orderId}/pay`, { payMethod: 'MOCK' })
} catch (e) {
  if (e.code === 5001) {
    alert('支付失败，请重试')   // 10% 概率
  }
}
```

#### ⑤ 用户确认收货（在订单详情页点"确认收货"）
```js
await axios.post(`/api/v1/orders/${orderId}/confirm`)
// 注意：没有 body
// 状态 3→4（一跳到已完成）
```

#### ⑥ 评价（在订单详情页点"评价"）
```js
// orderItemId 从订单详情 items[].id 拿
await axios.post('/api/v1/reviews', {
  orderId: 8888,
  orderItemId: 9001,     // 必填，是订单明细的 ID
  rating: 5,
  tasteRating: 5,        // 可选
  freshnessRating: 5,    // 可选
  logisticsRating: 4,    // 可选
  content: '很新鲜',
  images: ['https://...']
})
```

### 3.2 商家端流程

#### ① 商家登录 / 注册
```js
// 同一个登录端点 /api/v1/auth/login
const res = await axios.post('/api/v1/auth/login', {
  username: 'm01',
  password: '123456'
})
// 存 satoken + role = 'MERCHANT'

// 或者商家自助注册（演示环境无需审核，自动通过）
const reg = await axios.post('/api/v1/auth/register', {
  username: 'm_shop_02',
  password: '123456',
  role: 1,                       // ← 关键，1=商家
  shopName: '鲜果园旗舰店',      // ← role=1 必填
  contactName: '张三',
  contactPhone: '13800138000',
  logo: 'https://...'
})
// reg.data.data.token 直接可当 satoken 用，role = 'MERCHANT'
```

#### ② 查看订单列表
```js
const page = await axios.get('/api/v1/merchant/orders', {
  params: { status: 2, pageNum: 1, pageSize: 10 }   // status=2 待发货
})
// 待发货订单才需要处理
```

#### ③ 发货
```js
// 关键：body 必填 trackingNo + carrier
await axios.post(`/api/v1/merchant/orders/${orderId}/ship`, {
  trackingNo: 'SF1234567890',
  carrier: '顺丰'
})
// 状态 2 → 3（待收货），写入物流信息
```

---

## 4. 关键业务规则（设计层面）

### 4.1 订单状态机

```
         创建
          │
          ▼
    ┌──────────┐
    │ 1 待付款  │ ─── pay 成功 ──→ 2 待发货
    └──────────┘                       │
                                       │ ship
                                       ▼
                                 ┌──────────┐
                                 │ 3 待收货  │ ─── confirm ──→ 4 已完成
                                 └──────────┘                       │
                                                                     ▼
                                                                (结束)
         5 已取消：枚举保留，本次未做取消端点
```

**特别注意**：
- 状态流转是**单向**的，乱序调用会 `4001 订单状态不允许该操作`
- `pay` 有 10% 概率失败（`5001`），前端要处理重试
- `confirm` 状态变化是 3→4（一跳），**不是 3→4→5**（老文档错了，已修）

### 4.2 商品可见性

一个商品**用户能看能买**需要同时满足：

```
audit_status = 1   (审核通过)
status       = 1   (上架)
sku.stock    > 0   (任一 SKU 有库存)
```

任一不满足 → 商品对用户隐藏（或下架页可看但 `valid: false` 灰色展示）。

### 4.3 收货信息（设计决策）

**砍掉了独立的 `address` 表**——为什么？

| 原始设计 | 当前设计 |
|---|---|
| 有 `address` 表，存常用地址 | 无 `address` 表 |
| 下单时传 `addressId` | 下单时传 `receiverName/Phone/Address` 三个字段 |
| 后端 join 查地址 | 后端直接写 `orders` 表（`receiver_phone` AES-256-CBC 加密） |
| 收货信息改地址 → 改订单 | 订单创建后收货信息不变 |

**前端要做**：
- 常用地址用 **LocalStorage 自管**（不要试图调地址接口，不存在）
- 下单页用 LocalStorage 自动填
- 展示订单详情用后端返回的 `receiverName/Phone/Address`（后端解密后回传）

### 4.4 评价系统

- 评价要带 `orderId` + `orderItemId`（**两个都必填**）
- 一条订单明细只能评一次（数据库唯一约束）
- `tasteRating` / `freshnessRating` / `logisticsRating` 可选
- `images` 是 URL 数组，**前端要自己先上传图片拿 URL**（后端没做文件上传端点，演示可硬编码）

### 4.5 砍掉的功能（为什么没有这些接口）

| 没有的接口 | 原因 |
|---|---|
| 地址簿 | 砍 |
| 密码修改 | 砍 |
| 个人资料修改 | 砍 |
| 消息通知 | 砍 |
| 退款/售后 | 砍 |
| 取消订单 | 砍 |
| 再次购买 | 砍 |
| 追评 | 砍 |
| 收藏/关注 | 砍 |
| 管理后台 | 砍 |
| 商家商品管理 | 砍（演示数据 SQL 写死） |
| 商家店铺资料 | 砍 |
| 首页 banner / 热搜词 / 推荐 | 砍（前端硬编码） |

精简日期 2026-07-11，详见 `docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`。

---

## 5. 字段使用提醒（最容易踩的坑）

### 5.1 金额字段：字符串不是数字

后端所有金额字段（`totalAmount` / `payableAmount` / `shippingFee` / `discountAmount` / `price`）**以字符串形式返回**：

```json
"totalAmount": "119.80"   ← 字符串
"price": "59.90"          ← 字符串
```

**前端要这样处理**：

```js
// 展示：用 Number 包一层（或保留字符串）
const total = Number(order.totalAmount)   // 119.80

// 计算：千万不要 parseFloat 后累加
// const a = parseFloat('0.1') + parseFloat('0.2')  // 0.30000000000000004 ❌
const a = Number('0.10') + Number('0.20')   // 0.3 ✓

// 提交给后端：字符串或数字都行，后端会用 BigDecimal 转
```

### 5.2 订单 ID 两个字段

`OrderVO` 同时返回两个 ID：

```json
{
  "id": 8888,              ← 数字 ID，**后续接口都用这个**
  "orderId": "20260713ABCD"  ← 业务订单号（展示用），**别拿去调接口**
}
```

`MerchantOrderVO` 字段名是 `orderNo`（不是 `orderId`）—— **两个端字段名不一致**，注意分辨。

### 5.3 时间字段

所有时间字段是 **ISO 8601 格式**（无时区）：

```json
"createTime": "2026-07-13T12:30:00"
"payTime": "2026-07-13T12:31:00"
```

JS `new Date('2026-07-13T12:30:00')` 直接可解析。

### 5.4 商品规格两层结构

```json
{
  "specs": [                              ← 规格维度（用于渲染选择器 UI）
    { "name": "重量", "values": ["500g", "1kg"] },
    { "name": "包装", "values": ["礼盒", "简装"] }
  ],
  "skus": [                               ← 实际 SKU（用于下单）
    { "id": 2001, "spec": "500g/礼盒", "price": "59.90", "stock": 100 }
  ]
}
```

**前端逻辑**：
- 用 `specs` 渲染选择器
- 用户选择组合后，从 `skus` 找匹配 `spec` 的那条
- 加购物车时传 `skus[].id`（即 `skuId`），**不是 `productId`**

### 5.5 分页响应

`PageR<T>` 字段：

```json
{
  "total": 23,
  "pages": 3,
  "current": 1,
  "size": 10,
  "records": [...]
}
```

注意字段名是 `current`（不是 `pageNum`）和 `size`（不是 `pageSize`）—— **这是响应字段**。请求时是 `pageNum` / `pageSize`。

### 5.6 列表接口的 items 为 null

`GET /api/v1/orders`（列表）和 `GET /api/v1/orders/{id}`（详情）**响应结构不同**：

- 列表：`OrderVO.items = null`（不返回明细，减少流量）
- 详情：`OrderVO.items = [...]`（完整）

前端要 null-safe 处理 `items` 字段。

---

## 6. API 已知坑（已修，但前端要知道）

| 坑 | 现状 | 修复 commit |
|---|---|---|
| `POST /merchant/orders/{id}/ship` 之前不接 body | 现在 body 必带 `{trackingNo, carrier}` | `96bd61d` |
| 文档之前写了 2 个不存在的端点（用户独立登录 / 商家独立登录） | 删了 | `96bd61d` |
| 文档漏写 logout 端点 | 补了 | `96bd61d` |
| 字段名 `cartItemIds` 实际是 `cartIds` | 文档已对齐 | `96bd61d` |
| 订单状态 `pay` 文档写 2→3，实际 1→2 | 文档已对齐 | `96bd61d` |
| 订单状态 `confirm` 文档写 3→4→5，实际 3→4 | 文档已对齐 | `96bd61d` |
| `UnifiedLoginVO.role` 之前含 `ADMIN` | 已删 | `96bd61d` |
| 商家订单 VO 没 `trackingNo` / `carrier` 字段 | 已补 | `96bd61d` |
| 商家端之前没有注册端点（前端必须复用演示账号） | 已补 `/api/v1/merchant/auth/register` | （已撤回，见下方） |
| **架构重构**：user/merchant 两表合并为单 `user` 表 + `role` 字段；商家扩展拆 `merchant_profile`；Sa-Token 单 StpLogic + `@SaCheckRole`；统一单注册端点 `/api/v1/auth/register` 加 `role` 参数；`/api/v1/merchant/auth/*` 3 个端点全部删除；22→19 | 本次 |

**重构带来的影响（前端要知道）**：

- 注册入口只剩 1 个：`POST /api/v1/auth/register`，body 加 `role: 1|2`（不传默认 2 买家）
- 登录响应 `role` 字符串不变（仍是 `"USER"` / `"MERCHANT"`）
- 商家和买家用**同一套 token 空间**——前端不用改任何代码也能跑（如果之前调过 `/api/v1/merchant/auth/login`，现在改回 `/api/v1/auth/login`）
- 商家端 token 调用户端接口不再被隔离（共用 token），所以**路由守卫要靠前端 role 字段判断**

---

## 7. 调试技巧

### 7.1 swagger-ui 是最快的调试工具

`http://localhost:8080/swagger-ui.html` 实时反映所有字段注解、Schema、Example。

**不熟悉哪个字段？看 swagger-ui，比 API.md 全**。

### 7.2 常见错误码速查

| code | 看哪里 | 怎么解决 |
|---:|---|---|
| 401 | Header | 没带 `satoken` / token 过期 / 跨端 |
| 1001 | 请求体 | 字段校验失败，`msg` 含具体字段 |
| 1005 | 登录 | 用户名或密码错（不告诉你具体是哪个，防枚举） |
| 2003 | 登录 | 账号被禁 |
| 3001 | 加购 / 下单 | 商品下架 |
| 3002 | 加购 / 下单 | 库存不足 |
| 4001 | pay / ship / confirm | 订单状态不对 |
| 5001 | pay | 模拟支付 10% 失败，重试即可 |
| 7001 | 商家登录 | 商家用户名不存在 |
| 7002 | 商家登录 | 商家未通过审核 |
| 1002 / 2004 | 注册 | 用户名已被使用 |

### 7.3 直查数据库

联调时如果数据对不上，直接查数据库最快：

```bash
mysql -u root -p freshfood_shop
> SELECT id, status, tracking_no, carrier FROM orders ORDER BY id DESC LIMIT 5;
> SELECT * FROM order_item WHERE order_id = 8888;
> SELECT id, audit_status, status, name FROM product WHERE id = 1;
```

`receiver_phone` 是 AES-256-CBC 加密的（Base64 字符串），别想看明文就 SELECT。

### 7.4 后端日志

application.yml 已经设 `com.yan.freshfood: debug`，启动后控制台有完整 SQL 日志。

### 7.5 CORS

`WebMvcConfig` 设了 `allowedOriginPatterns("*")`，**前端随便什么端口**（5173 / 8080 / 3000）都通。如果不通就重启后端。

---

## 8. 前端要准备的"自测清单"

跑通下面这 7 个场景，对接就基本完成了：

- [ ] 用户注册 `/api/v1/auth/register`（`role` 不传或 2）→ 登录 → 拿 token
- [ ] 首页分类加载、搜索商品、查看商品详情（含评价）
- [ ] 加购物车 → 改数量 → 删一项 → 失效商品灰色展示
- [ ] 下单（带收货信息）→ 支付（10% 失败重试）→ 订单详情展示
- [ ] **切换到商家端**：商家注册 `/api/v1/auth/register`（`role: 1` + `shopName`，或用演示 `m01`）→ 看到待发货订单 → 发货（带物流单号）
- [ ] 切回用户端：订单状态变待收货 → 确认收货 → 状态变已完成
- [ ] 对已收货订单评价（带 orderItemId）
- [ ] **额外**：用买家 token 调 `/api/v1/merchant/orders` 应被 `@SaCheckRole` 挡掉（403）

---

## 9. 沟通渠道

- API 字段疑问 → 查 swagger-ui（最准）
- 业务规则疑问 → 查本指南
- 发现 bug → 直接喊后端，附 swagger-ui 接口路径 + 请求/响应截图
- 改字段约定 → **先**对齐再改代码，不要直接写

**对接愉快**。
