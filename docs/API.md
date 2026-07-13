# 线上生鲜商场 · 接口文档

> 后端 REST 接口精简版（19 端点）。本文档为前端对接离线参考；在线 swagger-ui 实时反映代码注解，字段最全。
>
> 精简日期：2026-07-13。设计依据：`docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`
>
> 本版本修正：① 删除不存在的端点 ② 状态流转字段对齐代码 ③ 补全错误码 ④ 补全每个端点 JSON 样例 ⑤ 新增启动清单 ⑥ **合并 user/merchant 为单 user 表 + role 字段（1=商家/2=买家）；单 Sa-Token 体系；统一单注册端点 + role 参数；22→19**

---

## 0. 启动清单（前端先看这节）

1. **拉代码 / 同步后端**
   ```bash
   git clone https://github.com/yrq-bulider/freshFood-shop-java.git
   cd freshFood-shop-java/freshfood-shop
   ```
2. **启动 MySQL 5.7+ / 8.x**，建库 `freshfood_shop`，跑 `sql/01_init.sql`（建表 + 默认数据）
3. **后端起服**（需 JDK 17+）：
   ```bash
   ./mvnw -pl freshfood-app -am spring-boot:run
   ```
   启动后访问 http://localhost:8080/swagger-ui.html 即可见完整字段
4. **演示账号**（`sql/01_init.sql` 注释 + `/auth/register` 自行创建）：

   | 端 | 用户名 | 密码 |
   |---|---|---|
   | 用户 | `zhangsan` | `123456` |
   | 用户 | `lisi` | `123456` |
   | 商家 | `m01` | `123456` |

5. **联调第一步**：先 `POST /api/v1/auth/login` 拿 `token`，所有受保护接口 Header 加 `satoken: <token>`

---

## 1. 通用约定

### 1.1 基础地址

- 协议/地址：`http://localhost:8080`
- 所有接口前缀：`/api/v1`
- 在线 swagger-ui：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### 1.2 鉴权

- 框架：Sa-Token（**单 StpLogic 体系**）
- 登录态：Header `satoken: <token>`
- 公开接口（`@SaIgnore`）：注册、登录、统一登录、首页分类、商品详情、搜索、登出无需 token
- 受保护接口：未携带 / 过期 → HTTP 401，body `{"code":401,"msg":"未登录"}`
- 角色隔离：商家端接口（`/api/v1/merchant/*`）加 `@SaCheckRole("MERCHANT")`，非商家账号 token 调商家接口 → HTTP 403
- 账号统一在 `user` 表，**role 字段区分**（1=商家/2=买家）；不再有 `merchant` 表

### 1.3 统一响应

```json
{
  "code": 0,
  "msg": "ok",
  "data": { ... }
}
```

- `code = 0`：成功
- `code != 0`：失败，`msg` 含错误描述
- 分页响应字段：`{ total, pages, current, size, records: [...] }`

---

## 2. 错误码全表

| code | 域 | 含义 | 触发场景 |
|---:|---|---|---|
| 0 | - | 成功 | - |
| 401 | 通用 | 未登录 | 缺 / 错 / 过期 satoken |
| 403 | 通用 | 无权限 | 跨端 token、禁用账号 |
| 1001 | 通用 | 参数校验失败 | `@Valid` 失败，`msg` 含具体字段 |
| 1004 | 通用 | 资源不存在 | id 错或已删 |
| 1005 | 通用 | 用户名或密码错误 | 登录失败统一抛（防账号枚举） |
| 1002 | 通用 | 用户名已被使用（跨账号表） | 注册时 user / merchant 表都查 |
| 2001 | 用户 | 用户不存在 | - |
| 2002 | 用户 | 密码错误 | - |
| 2003 | 用户 | 账号已禁用 | - |
| 2004 | 用户 | 用户名已存在 | - |
| 3001 | 商品 | 商品已下架 | 加购 / 下单时 |
| 3002 | 商品 | 库存不足 | - |
| 7001 | 商家 | 商家不存在 | - |
| 7002 | 商家 | 商家未通过审核 | `audit_status != 1` |
| 7003 | 商家 | 商家审核状态不允许此操作 | - |
| 7004 | 商家 | 分类存在子分类或被商品引用，不可删除 | - |
| 4001 | 订单 | 订单状态不允许该操作 | 在非合法状态调用 pay / ship / confirm |
| 4002 | 订单 | 订单不存在 | - |
| 5001 | 支付 | 支付失败 | 模拟支付 10% 概率触发 |
| 8001 | 系统 | 系统异常 | 未捕获异常 |

> 注：simplify 后砍了管理端，对应 `9xxx` 错误码不再出现；`6xxx` 退款（已砍）保留枚举但不会触发。

---

## 3. 端点清单

| # | 模块 | Method | Path | 鉴权 |
|---:|---|---|---|---|
| 1 | 统一登录 | POST | `/api/v1/auth/login` | 否 |
| 2 | 用户-账号 | POST | `/api/v1/auth/register` | 否 |
| 3 | 用户-账号 | POST | `/api/v1/auth/logout` | **是**（注销当前） |
| 4 | 用户-首页 | GET | `/api/v1/home/categories` | 否 |
| 5 | 用户-商品 | GET | `/api/v1/products/{id}` | 否 |
| 6 | 用户-搜索 | GET | `/api/v1/search/products` | 否 |
| 7 | 用户-购物车 | GET | `/api/v1/cart` | 是 |
| 8 | 用户-购物车 | POST | `/api/v1/cart` | 是 |
| 9 | 用户-购物车 | PUT | `/api/v1/cart/{id}` | 是 |
| 10 | 用户-购物车 | DELETE | `/api/v1/cart/{id}` | 是 |
| 11 | 用户-订单 | POST | `/api/v1/orders` | 是 |
| 12 | 用户-订单 | POST | `/api/v1/orders/{id}/pay` | 是 |
| 13 | 用户-订单 | POST | `/api/v1/orders/{id}/confirm` | 是 |
| 14 | 用户-订单 | GET | `/api/v1/orders` | 是 |
| 15 | 用户-订单 | GET | `/api/v1/orders/{id}` | 是 |
| 16 | 用户-评价 | POST | `/api/v1/reviews` | 是 |
| 17 | 商家-订单 | GET | `/api/v1/merchant/orders` | 是 |
| 18 | 商家-订单 | GET | `/api/v1/merchant/orders/{id}` | 是 |
| 19 | 商家-订单 | POST | `/api/v1/merchant/orders/{id}/ship` | 是 |

**单一注册端点**：
- `POST /api/v1/auth/register`：body 加 `role` 字段（1=商家 / 2=买家），不传默认 2 买家；注册成功立即登录返回 token
- 商家注册时多带：`shopName`（必填）、`contactName`、`contactPhone`、`logo`（都可选；联系方式 AES 加密存 `merchant_profile`）

**单一登录入口**：
- `POST /api/v1/auth/login`：单 user 表查，命中后看 `role` 字段返回 `role: "MERCHANT"` 或 `"USER"`
- 前端不用选身份，不用选端点，看响应 `role` 字段决定路由

---

## 4. 端点详细定义

### 4.1 统一登录

#### ① `POST /api/v1/auth/login`

- **鉴权**：否
- **请求体**（`LoginDTO`）：

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `username` | 是 | 3-20 字符 |
| `password` | 是 | 6-20 字符 |

- **响应**（`UnifiedLoginVO`）：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "token": "3a7d2c1e-xxxx-xxxx",
    "role": "USER",
    "profile": {
      "userId": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "https://..."
    }
  }
}
```

| 字段 | 说明 |
|---|---|
| `token` | 登录令牌，后续请求 Header `satoken: <token>` |
| `role` | `USER` / `MERCHANT` |
| `profile` | USER 含 `id/username/nickname/avatar/role(2)/createTime`；MERCHANT 含同字段 + `shopName/logo/contactName/contactPhone`（来自 `merchant_profile`） |

---

### 4.2 用户端 - 账号

#### ② `POST /api/v1/auth/register`

- **鉴权**：否
- **请求体**（`RegisterDTO`，**统一买家 + 商家注册入口**）：

**买家示例**（`role` 不传默认 2）：
```json
{
  "username": "wangwu",
  "password": "123456",
  "nickname": "王五",
  "phone": "13800138000",
  "role": 2
}
```

**商家示例**（`role: 1`，必带 `shopName`）：
```json
{
  "username": "m_shop_02",
  "password": "123456",
  "role": 1,
  "shopName": "鲜果园旗舰店",
  "contactName": "张三",
  "contactPhone": "13800138000",
  "logo": "https://cdn.example.com/logo.png"
}
```

| 字段 | 必填 | 校验 |
|---|---|---|
| `username` | 是 | 3-20 字符 |
| `password` | 是 | 6-20 字符 |
| `nickname` | 否 | 仅买家展示 |
| `phone` | 否 | `^1[3-9]\d{9}$` |
| `role` | 否 | `1`（商家）/ `2`（买家）；不传默认 2；写库到 `user.role` |
| `shopName` | role=1 时必填 | ≤ 100 字符，写 `merchant_profile` |
| `contactName` | 否 | AES-256-CBC 加密 |
| `contactPhone` | 否 | `^1[3-9]\d{9}$`，AES-256-CBC 加密 |
| `logo` | 否 | 店铺 logo URL |

- **行为**：用户名查重（`user` 表）→ 用户名重复 `2004`；商家注册默认 `audit_status=1` 通过；注册成功立即登录返回 token。
- **响应**：`UnifiedLoginVO`：
  - 买家：`role: "USER"`、`profile.role=2`
  - 商家：`role: "MERCHANT"`、`profile.role=1` + `shopName/logo/contactName/contactPhone`

#### ③ `POST /api/v1/auth/logout`

- **鉴权**：是（任意账号 token）
- **请求体**：无
- **响应**：`{"code":0,"msg":"ok","data":null}`

> 商家和买家共用同一个 logout 端点（不再有商家独立 logout）

---

### 4.3 用户端 - 首页

#### ④ `GET /api/v1/home/categories`

- **鉴权**：否
- **Query**：无
- **响应**（`List<CategoryVO>`）：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1, "parentId": 0, "name": "蔬菜水果", "icon": "https://...", "sort": 1,
      "children": [
        { "id": 11, "parentId": 1, "name": "叶菜", "icon": null, "sort": 1, "children": [] }
      ]
    }
  ]
}
```

> **首页 banner 图 / 热搜词 / 推荐商品**：前端硬编码，不走接口（spec §7.3）。

---

### 4.4 用户端 - 商品

#### ⑤ `GET /api/v1/products/{id}`

- **鉴权**：否
- **Path**：`id` - 商品 ID
- **响应**（`ProductDetailVO`）：

```json
{
  "code": 0,
  "data": {
    "productId": 1,
    "name": "本地草莓 500g 装",
    "mainImage": "https://...",
    "categoryId": 11,
    "merchantId": 100,
    "origin": "山东烟台",
    "afterSalesTags": ["坏品包赔", "次日达"],
    "description": "当季新鲜草莓...",
    "skus": [
      { "id": 2001, "spec": "500g/盒", "price": "59.90", "stock": 100, "sales": 23, "image": "https://..." }
    ],
    "specs": [
      { "name": "重量", "values": ["500g", "1kg", "2kg"] }
    ],
    "ratingStats": { "average": 4.8, "total": 56, "five": 50, "four": 4, "three": 1, "two": 1, "one": 0 },
    "reviews": [
      {
        "id": 9001, "username": "张*", "avatar": "https://...",
        "rating": 5, "tasteRating": 5, "freshnessRating": 5, "logisticsRating": 4,
        "content": "非常新鲜", "images": ["https://..."],
        "merchantReply": "感谢支持", "createTime": "2026-07-10T14:23:00"
      }
    ]
  }
}
```

> `reviews` 默认最多 10 条；评价图片 `images` 是 URL 列表。

---

### 4.5 用户端 - 搜索

#### ⑥ `GET /api/v1/search/products`

- **鉴权**：否
- **Query 参数**：

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `keyword` | string | 否 | - | 搜索关键词 |
| `categoryId` | long | 否 | - | 分类 ID |
| `minPrice` | decimal | 否 | - | 最低价 |
| `maxPrice` | decimal | 否 | - | 最高价 |
| `sort` | string | 否 | `sales_desc` | `sales_desc` / `sales_asc` / `price_asc` / `price_desc` / `new` |
| `pageNum` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 10 | 页大小 |

- **响应**（`PageR<ProductSimpleVO>`）：

```json
{
  "code": 0,
  "data": {
    "total": 23, "pages": 3, "current": 1, "size": 10,
    "records": [
      { "productId": 1, "name": "...", "mainImage": "...", "origin": "...", "minPrice": 59.90, "sales": 23, "rating": 4.8 }
    ]
  }
}
```

---

### 4.6 用户端 - 购物车

#### ⑦ `GET /api/v1/cart`

- **鉴权**：是（USER）
- **响应**（`CartVO`）：

```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 101, "skuId": 2001, "productId": 1,
        "productName": "本地草莓 500g 装", "spec": "500g/盒", "price": "59.90",
        "quantity": 2, "selected": true, "valid": true, "stock": 100,
        "mainImage": "https://..."
      }
    ],
    "totalAmount": "119.80",
    "selectedAmount": "119.80",
    "shippingFee": "0.00",
    "invalidCount": 0,
    "selectedCount": 2
  }
}
```

| 字段 | 说明 |
|---|---|
| `valid` | false 时表示商品下架 / SKU 删了 / 库存不足，前端灰色展示 |
| `selected` | 结算时只算 `selected=true` 的项 |

#### ⑧ `POST /api/v1/cart`

- **鉴权**：是
- **请求体**（`CartAddDTO`）：

```json
{ "skuId": 2001, "quantity": 2 }
```

#### ⑨ `PUT /api/v1/cart/{id}`

- **鉴权**：是
- **请求体**（`CartUpdateDTO`）：

```json
{ "quantity": 3 }
```

#### ⑩ `DELETE /api/v1/cart/{id}`

- **鉴权**：是
- **请求体**：无
- **响应**：`{"code":0,"data":null}`

---

### 4.7 用户端 - 订单

#### ⑪ `POST /api/v1/orders`

- **鉴权**：是
- **请求体**（`OrderCreateDTO`）：

```json
{
  "cartIds": [101, 102],
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "receiverAddress": "北京市朝阳区某街道 1 号",
  "remark": "请尽快发货"
}
```

| 字段 | 必填 | 校验 |
|---|---|---|
| `cartIds` | 是 | 非空数组（**注意是 `cartIds` 不是 `cartItemIds`**） |
| `receiverName` | 是 | - |
| `receiverPhone` | 是 | `^1[3-9]\d{9}$` |
| `receiverAddress` | 是 | - |
| `remark` | 否 | - |

- **响应**（`OrderVO`）：

```json
{
  "code": 0,
  "data": {
    "id": 8888,
    "orderId": "20260713ABCD",
    "status": 1,
    "statusText": "待付款",
    "totalAmount": "119.80",
    "shippingFee": "0.00",
    "discountAmount": "0.00",
    "payableAmount": "119.80",
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "receiverAddress": "北京市朝阳区某街道 1 号",
    "remark": "请尽快发货",
    "trackingNo": null,
    "carrier": null,
    "payMethod": null,
    "expireTime": "2026-07-13T13:30:00",
    "payTime": null,
    "shipTime": null,
    "confirmTime": null,
    "createTime": "2026-07-13T12:30:00",
    "items": [
      { "id": 9001, "skuId": 2001, "productId": 1, "productName": "本地草莓 500g 装", "spec": "500g/盒", "price": "59.90", "quantity": 2, "mainImage": "https://..." }
    ]
  }
}
```

> **状态流转**：创建后 `status=1`（待付款），**注意字段是 `id`（数字 ID）和 `orderId`（业务订单号）都返回**。

#### ⑫ `POST /api/v1/orders/{id}/pay`

- **鉴权**：是
- **Path**：`id` - 订单 ID
- **请求体**：

```json
{ "payMethod": "MOCK" }
```

- **状态流转**：`1`（待付款） → `2`（待发货）。**有 10% 概率触发 `5001` 支付失败**（模拟）。
- **响应**：`{"code":0,"data":null}`

#### ⑬ `POST /api/v1/orders/{id}/confirm`

- **鉴权**：是
- **请求体**：无
- **状态流转**：`3`（待收货） → `4`（已完成）。**注意：没有"5"，确认后直接到已完成，5 是已取消（本次未做取消端点）**。
- **响应**：`{"code":0,"data":null}`

#### ⑭ `GET /api/v1/orders`

- **鉴权**：是
- **Query**：`status`（可选 1-5）、`pageNum`（默认 1）、`pageSize`（默认 10）
- **响应**（`PageR<OrderVO>`，简化版）：

```json
{
  "code": 0,
  "data": {
    "total": 5, "pages": 1, "current": 1, "size": 10,
    "records": [ { ...OrderVO, "items": null... } ]
  }
}
```

> 列表接口的 `items` 字段为 null（不返回明细），需要明细走 `⑮`

#### ⑮ `GET /api/v1/orders/{id}`

- **鉴权**：是
- **响应**：完整 `OrderVO`（含 `items`）

---

### 4.8 用户端 - 评价

#### ⑯ `POST /api/v1/reviews`

- **鉴权**：是
- **请求体**（`ReviewCreateDTO`）：

```json
{
  "orderId": 8888,
  "orderItemId": 9001,
  "rating": 5,
  "tasteRating": 5,
  "freshnessRating": 5,
  "logisticsRating": 4,
  "content": "非常新鲜，配送及时",
  "images": ["https://..."]
}
```

| 字段 | 必填 | 校验 |
|---|---|---|
| `orderId` | 是 | - |
| `orderItemId` | 是 | - |
| `rating` | 是 | 1-5 |
| `tasteRating` / `freshnessRating` / `logisticsRating` | 否 | 1-5 |
| `content` | 是 | ≤1000 字 |
| `images` | 否 | URL 数组 |

- **响应**：`{"code":0,"data":null}`

---

### 4.9 商家端 - 订单

> 商家端**没有独立的登录端点**，统一通过 `POST /api/v1/auth/login` 登录（按 user → merchant 匹配），返回 `role: "MERCHANT"`。

#### ⑰ `GET /api/v1/merchant/orders`

- **鉴权**：是（MERCHANT）
- **Query**：`status`（可选 1-5）、`pageNum`、`pageSize`
- **响应**（`PageR<MerchantOrderVO>`）：字段同 ⑭，不含 `items` 详情

#### ⑱ `GET /api/v1/merchant/orders/{id}`

- **鉴权**：是
- **响应**（`MerchantOrderVO`）：

```json
{
  "code": 0,
  "data": {
    "id": 8888,
    "orderNo": "20260713ABCD",
    "status": 2,
    "statusText": "待发货",
    "totalAmount": "119.80",
    "payableAmount": "119.80",
    "items": [
      { "id": 9001, "skuId": 2001, "productId": 1, "productName": "...", "spec": "500g/盒", "price": "59.90", "quantity": 2 }
    ],
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "receiverAddress": "北京市朝阳区某街道 1 号",
    "remark": "请尽快发货",
    "createTime": "2026-07-13T12:30:00",
    "payTime": "2026-07-13T12:31:00",
    "shipTime": null,
    "trackingNo": null,
    "carrier": null
  }
}
```

> 商家端 `MerchantOrderVO` 字段名是 `orderNo`（不是用户的 `orderId`），其他字段同 `OrderVO` 减去 `expireTime` / `payMethod` / `discountAmount` / `confirmTime` / `items` 结构略有不同。

#### ⑲ `POST /api/v1/merchant/orders/{id}/ship`

- **鉴权**：是
- **请求体**（`ShipDTO`）：

```json
{ "trackingNo": "SF1234567890", "carrier": "顺丰" }
```

| 字段 | 必填 | 校验 |
|---|---|---|
| `trackingNo` | 是 | 非空 |
| `carrier` | 是 | 非空 |

- **状态流转**：`2`（待发货） → `3`（待收货），同时写入 `trackingNo` / `carrier` / `shipTime`
- **响应**：`{"code":0,"data":null}`

---

## 5. 关键业务字段

### 5.1 订单状态 `status`

| 值 | 含义 | 何时进入 |
|---:|---|---|
| 1 | 待付款 | 创建订单 |
| 2 | 待发货 | 调用 `/pay` 成功 |
| 3 | 待收货 | 商家调用 `/ship` 成功 |
| 4 | 已完成 | 用户调用 `/confirm` 成功 |
| 5 | 已取消 | **本次未做取消端点**，仅枚举保留 |

**状态机**：

```
创建 ─→ 1 ─pay─→ 2 ─ship─→ 3 ─confirm─→ 4
                    │
                    └─ 5001 支付失败：停留在 1
```

### 5.2 收货快照字段

精简后无独立 `address` 表。下单时 `receiverName` / `receiverPhone` / `receiverAddress` 直接写入 `orders` 表（`receiver_phone` AES-256-CBC 加密存储）。

### 5.3 商品可见性

`audit_status = 1`（通过） + `status = 1`（上架） + `sku.stock > 0` → 用户可见可买

### 5.4 商品上下架 `status`（`product` 表）

| 值 | 含义 |
|---:|---|
| 0 | 下架 |
| 1 | 上架 |

### 5.5 金额字段约定

所有金额字段（`totalAmount` / `payableAmount` / `shippingFee` / `discountAmount` / `price`）**以字符串形式返回**（如 `"119.80"`），避免 JS 浮点精度问题。

---

## 6. 前端集成提示

### 6.1 登录态保存

```js
// 登录成功后保存 token
const res = await axios.post('/api/v1/auth/login', { username, password })
localStorage.setItem('satoken', res.data.data.token)
localStorage.setItem('role', res.data.data.role) // USER / MERCHANT

// axios 拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers['satoken'] = token
  return config
})
```

### 6.2 401 / 403 处理

```js
axios.interceptors.response.use(
  resp => resp.data.code === 0 ? resp.data : Promise.reject(resp.data),
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
    } else if (err.response?.status === 403) {
      // 跨端 token：清掉跳登录
      localStorage.removeItem('satoken')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)
```

### 6.3 收货地址

无独立 `address` 接口。下单时直接传 `receiverName` / `receiverPhone` / `receiverAddress` 三个字段。
常用地址前端用 LocalStorage 自管。

### 6.4 CORS

后端默认允许同源；联调时如果前端是独立端口（如 `localhost:5173`）后端会自动放行（`WebMvcConfig` 已配）。

---

## 7. 端到端主链路示例

```bash
# 1. 用户登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}' | jq -r .data.data.token)

# 2. 加入购物车
curl -X POST http://localhost:8080/api/v1/cart \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d '{"skuId":2001,"quantity":2}'

# 3. 查看购物车（拿 cartId）
CART_ID=$(curl -s -H "satoken: $TOKEN" http://localhost:8080/api/v1/cart | jq -r '.data.data.list[0].id')

# 4. 提交订单（注意字段名是 cartIds）
ORDER_ID=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d "{\"cartIds\":[$CART_ID],\"receiverName\":\"张三\",\"receiverPhone\":\"13800138000\",\"receiverAddress\":\"北京市朝阳区XX路1号\"}" \
  | jq -r .data.data.id)

# 5. 模拟支付
curl -X POST http://localhost:8080/api/v1/orders/$ORDER_ID/pay \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d '{"payMethod":"MOCK"}'

# 6. 商家登录
M_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456"}' | jq -r .data.data.token)

# 7. 商家发货（注意带 body）
curl -X POST http://localhost:8080/api/v1/merchant/orders/$ORDER_ID/ship \
  -H "Content-Type: application/json" -H "satoken: $M_TOKEN" \
  -d '{"trackingNo":"SF1234567890","carrier":"顺丰"}'

# 8. 用户确认收货
curl -X POST http://localhost:8080/api/v1/orders/$ORDER_ID/confirm \
  -H "satoken: $TOKEN"

# 9. 评价（orderItemId 从订单详情拿）
ORDER_ITEM_ID=$(curl -s -H "satoken: $TOKEN" http://localhost:8080/api/v1/orders/$ORDER_ID | jq -r '.data.data.items[0].id')
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Content-Type: application/json" -H "satoken: $TOKEN" \
  -d "{\"orderId\":$ORDER_ID,\"orderItemId\":$ORDER_ITEM_ID,\"rating\":5,\"tasteRating\":5,\"freshnessRating\":5,\"logisticsRating\":4,\"content\":\"很新鲜\"}"
```

> 注：上面 jq 路径是 `data.data.X` 因为 `R<T>` 包了一层。`UnifiedLoginVO` 直接在 `data` 下。

---

## 8. 变更记录

- **2026-07-13**：本版本
  - 修 `POST /merchant/orders/{id}/ship` 不接 body 的 bug，加 `ShipDTO`（`trackingNo` / `carrier`），service 写入字段
  - 商家订单 VO 补 `trackingNo` / `carrier` 字段返回
  - 删掉 `UnifiedAuthController` description 里残留的"admin"
  - `UnifiedLoginVO.role` allowableValues 去掉 `ADMIN`
  - 删文档里不存在的"用户独立登录"和"商家独立登录"端点
  - 文档端点数 19 → 19（实际正确 19 个，之前错误地写成 19 但有 2 个不存在的端点）
  - 修 `pay` 状态流转描述：1→2（之前文档写 2→3 错）
  - 修 `confirm` 状态流转描述：3→4（之前文档写 3→4→5 错）
  - 修 `cartIds` 字段名（之前文档写 `cartItemIds` 错）
  - 补全 19 个错误码
  - 19 个端点全部补 JSON 样例
  - 新增"启动清单"小节，方便前端从头跑通
- **2026-07-11**：精简版。端点 90 → 19，删除管理端 / 消息 / 退款 / 追评 / 再次购买 / 地址簿 / 密码修改 / 个人资料 / 商家商品管理 / 店铺资料维护 / 首页轮播 / 推荐 / 热搜词 / 商品评价单独接口。详见 `docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`
- **2026-07-05**：补全 controller `@Operation`/`@Parameter` 注解、DTO/VO `@Schema`，输出本文档
