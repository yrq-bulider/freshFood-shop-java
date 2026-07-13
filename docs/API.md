# 线上生鲜商场 · 接口文档

> 后端 REST 接口精简版（19 端点）。本文档为前端对接离线参考，详细字段说明请以在线 swagger-ui 文档为准。
>
> 精简日期：2026-07-11。设计依据：`docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`

## 阅读说明

- 基础地址：`http://localhost:8080`
- 在线文档：http://localhost:8080/swagger-ui.html
- 统一登录入口：`POST /api/v1/auth/login`（推荐），按 `user → merchant` 顺序匹配账号
- 登录态：Sa-Token，Header 携带 `satoken: <token>`（swagger-ui「Authorize」按钮可一键登录）
- 统一响应：`R<T> = { code, msg, data }`，`code = 0` 成功，其余见错误码表
- 分页响应：`PageR<T> = { total, pages, current, size, records }`

## 通用约定

### 鉴权

- 公开接口（标 `[SaIgnore]`）：注册、登录、首页/搜索/分类/热词、商品详情、评价详情
- 受保护接口：需 Header `satoken: <token>`，未携带或过期返回 `401`
- 多端隔离：`user` / `merchant` 两套 StpLogic，token 互不通用

### 错误码（节选）

| 范围 | 域 |
|---|---|
| 1xxx | 通用（参数、登录态） |
| 2xxx | 用户 |
| 3xxx | 商品 |
| 4xxx | 订单 |
| 5xxx | 支付 |
| 7xxx | 商家 |

---

## 一、统一登录（app 模块）

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 否 | 统一登录（user → merchant 顺序匹配） |

---

## 二、用户端（user 模块）· `/api/v1`

### 2.1 账号

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/auth/register` | 否 | 用户注册 |
| POST | `/api/v1/auth/login` | 否 | 用户登录（独立端点，与统一登录等价） |

### 2.2 首页

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/home/categories` | 否 | 分类树 |

> 首页 banner 图、推荐商品、热搜词前端硬编码，不走接口。

### 2.3 商品

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/products/{id}` | 否 | 商品详情（响应体含 `reviews` 数组，最近 10 条评价） |

### 2.4 搜索

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/search/products` | 否 | 搜索商品（关键字/分类/价格/排序） |

> 搜索 query 参数：`keyword`、`categoryId`、`minPrice`、`maxPrice`、`sort`（sales_desc/sales_asc/price_asc/price_desc/new）、`pageNum`、`pageSize`

### 2.5 购物车

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/cart` | 是 | 我的购物车 |
| POST | `/api/v1/cart` | 是 | 加入购物车 |
| PUT | `/api/v1/cart/{id}` | 是 | 更新数量 |
| DELETE | `/api/v1/cart/{id}` | 是 | 删除单项 |

### 2.6 订单

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/orders` | 是 | 提交订单（body 含 `receiverName` / `receiverPhone` / `receiverAddress`） |
| POST | `/api/v1/orders/{id}/pay` | 是 | 模拟支付（body `{ payMethod: "MOCK" }`，状态 2→3） |
| POST | `/api/v1/orders/{id}/confirm` | 是 | 确认收货（状态 3→4→5） |
| GET | `/api/v1/orders` | 是 | 我的订单分页（query `status`） |
| GET | `/api/v1/orders/{id}` | 是 | 订单详情 |

### 2.7 评价

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/reviews` | 是 | 发表评价（`orderId` + `productId` + `rating` + `content`） |

---

## 三、商家端（merchant 模块）· `/api/v1/merchant`

### 3.1 账号

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/merchant/auth/login` | 否 | 商家登录 |

### 3.2 订单

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/merchant/orders` | 是 | 商家订单分页（query `status`） |
| GET | `/api/v1/merchant/orders/{id}` | 是 | 商家订单详情 |
| POST | `/api/v1/merchant/orders/{id}/ship` | 是 | 发货（待发货→待收货，需填物流单号） |

---

## 四、关键业务字段（速查）

### 订单状态 `status`

| 值 | 含义 |
|---|---|
| 1 | 待付款 |
| 2 | 待发货 |
| 3 | 待收货 |
| 4 | 已完成 |
| 5 | 已取消 |

> 精简版只覆盖 1~5 五种状态，去掉原 `退款中 (6) / 已退款 (7)`。

### 订单收货快照字段

精简后 `orders` 表直接存储收货信息（替代原 `address` 表）：

| 字段 | 说明 |
|---|---|
| `receiver_name` | 收货人姓名 |
| `receiver_phone` | 收货人电话（加密存储） |
| `receiver_address` | 收货地址 |

### 上下架 `status`（product）

| 值 | 含义 |
|---|---|
| 0 | 下架 |
| 1 | 上架 |

### 商品可见性

`audit_status = 1`（通过）+ `status = 1`（上架）+ `sku.stock > 0` → 用户可见可买

---

## 五、前端集成提示

### 1. 登录态保存

```js
// 登录成功后保存 token
localStorage.setItem('satoken', res.data.token)
localStorage.setItem('role', res.data.role) // user / merchant

// axios 拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers['satoken'] = token
  return config
})
```

### 2. 401 处理

```js
axios.interceptors.response.use(
  resp => resp.data.code === 0 ? resp.data : Promise.reject(resp.data),
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)
```

### 3. 收货地址

精简后无独立 `address` 接口。下单时直接传 `receiverName` / `receiverPhone` / `receiverAddress` 三字段，写入 `orders` 表快照。如需保存常用地址，前端用 LocalStorage 自管。

### 4. 演示账号（来自 `sql/01_init.sql`）

| 端 | 账号 | 密码 |
|---|---|---|
| 商家 | `m01` | `123456` |
| 用户 | `zhangsan` | `123456` |
| 用户 | `lisi` | `123456` |

---

## 六、端到端主链路示例

**用户下单 → 商家发货 → 用户确认收货 → 评价**：

```bash
# 1. 用户登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}' | jq -r .data.token)

# 2. 加入购物车
curl -X POST http://localhost:8080/api/v1/cart \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d '{"skuId":1,"quantity":2}'

# 3. 提交订单（带收货快照）
ORDER_ID=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d '{"cartItemIds":[1],"receiverName":"张三","receiverPhone":"13800138000","receiverAddress":"北京市朝阳区XX路1号"}' \
  | jq -r .data.id)

# 4. 模拟支付
curl -X POST http://localhost:8080/api/v1/orders/$ORDER_ID/pay \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d '{"payMethod":"MOCK"}'

# 5. 商家登录后发货
M_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/merchant/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456"}' | jq -r .data.token)
curl -X POST http://localhost:8080/api/v1/merchant/orders/$ORDER_ID/ship \
  -H "Content-Type: application/json" -H "satoken: $M_TOKEN" \
  -d '{"trackingNo":"SF1234567890","carrier":"顺丰"}'

# 6. 用户确认收货
curl -X POST http://localhost:8080/api/v1/orders/$ORDER_ID/confirm \
  -H "satoken: $TOKEN"

# 7. 评价
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d "{\"orderId\":$ORDER_ID,\"productId\":1,\"rating\":5,\"content\":\"很新鲜\"}"
```

---

## 七、变更记录

- 2026-07-11：精简版。端点 90 → 19，删除管理端、消息、退款、追评、再次购买、地址簿、密码修改、个人资料、商家商品管理、店铺资料维护、首页轮播/推荐/热搜词接口、商品评价单独接口。详见 `docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`
- 2026-07-05：补全全部 controller `@Operation`/`@Parameter` 注解，补 DTO/VO `@Schema`，输出本文档供前端离线参考