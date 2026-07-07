# 线上生鲜商场购物平台 · 接口文档

> 后端 REST 接口全集（90+ 端点）。本文档为前端对接离线参考，详细字段说明请以在线 swagger-ui 文档为准。

## 阅读说明

- 基础地址：`http://localhost:8080`
- 在线文档：http://localhost:8080/swagger-ui.html
- 统一登录入口：`POST /api/v1/auth/login`（推荐），按 `user → merchant → admin` 顺序匹配账号
- 登录态：Sa-Token，Header 携带 `satoken: <token>`（swagger-ui「Authorize」按钮可一键登录）
- 统一响应：`R<T> = { code, msg, data }`，`code = 0` 成功，其余见错误码表
- 分页响应：`PageR<T> = { total, pages, current, size, records }`

## 通用约定

### 鉴权

- 公开接口（标 `[SaIgnore]`）：注册、登录、首页/搜索、分类、热词、商品详情、评价详情
- 受保护接口：需 Header `satoken: <token>`，未携带或过期返回 `401`
- 多端隔离：`user`/`merchant`/`admin` 的 token 互不通用

### 错误码（节选）

| 范围 | 域 |
|---|---|
| 1xxx | 通用（参数、登录态） |
| 2xxx | 用户 |
| 3xxx | 商品 |
| 4xxx | 订单 |
| 5xxx | 支付 |
| 6xxx | 退款 |
| 7xxx | 商家 |
| 9xxx | 管理端 |

---

## 一、统一登录（app 模块）

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 否 | 统一登录（user/merchant/admin 顺序匹配） |

---

## 二、用户端（user 模块）· `/api/v1`

### 2.1 账号

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/auth/register` | 否 | 用户注册 |
| POST | `/api/v1/auth/logout` | 否 | 用户登出（需已登录态，否则 NotLoginException） |

### 2.2 个人信息

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/users/me` | 是 | 当前用户信息 |
| PUT | `/api/v1/users/me` | 是 | 更新当前用户资料 |
| PUT | `/api/v1/users/me/password` | 是 | 修改密码（old + new） |

### 2.3 收货地址

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/addresses` | 是 | 我的地址列表 |
| POST | `/api/v1/addresses` | 是 | 新建地址 |
| PUT | `/api/v1/addresses/{id}` | 是 | 编辑地址 |
| DELETE | `/api/v1/addresses/{id}` | 是 | 删除地址 |
| PUT | `/api/v1/addresses/{id}/default` | 是 | 设为默认地址 |

### 2.4 首页

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/home/banners` | 否 | 首页轮播图 |
| GET | `/api/v1/home/categories` | 否 | 分类树 |
| GET | `/api/v1/home/recommendations` | 否 | 首页推荐商品 |

### 2.5 商品

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/products/{id}` | 否 | 商品详情 |
| GET | `/api/v1/products/{id}/reviews` | 否 | 商品评价分页 |
| GET | `/api/v1/products/{id}/recommendations` | 否 | 同类推荐 |

### 2.6 搜索

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/search/hot-words` | 否 | 热门搜索词 |
| GET | `/api/v1/search/products` | 否 | 搜索商品（关键字/分类/价格/排序） |
| GET | `/api/v1/search/history` | 是 | 我的搜索历史 |
| DELETE | `/api/v1/search/history` | 是 | 清空搜索历史 |
| DELETE | `/api/v1/search/history/{id}` | 是 | 删除单条搜索历史 |

> 搜索 query 参数：`keyword`、`categoryId`、`minPrice`、`maxPrice`、`sort`（sales_desc/sales_asc/price_asc/price_desc/new）、`pageNum`、`pageSize`

### 2.7 购物车

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/cart` | 是 | 我的购物车 |
| POST | `/api/v1/cart` | 是 | 加入购物车 |
| PUT | `/api/v1/cart/{id}` | 是 | 更新数量 |
| DELETE | `/api/v1/cart/{id}` | 是 | 删除单项 |
| DELETE | `/api/v1/cart` | 是 | 批量删除（body 为 ID 列表） |
| PUT | `/api/v1/cart/select` | 是 | 切换单项选中（query `id` + `selected`） |
| PUT | `/api/v1/cart/select-all` | 是 | 全选/全不选（query `selected`） |

### 2.8 订单

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/orders/preview` | 是 | 下单预览（金额/运费/明细） |
| POST | `/api/v1/orders` | 是 | 提交订单 |
| POST | `/api/v1/orders/{id}/pay` | 是 | 支付订单（body `{ payMethod: "MOCK" }`） |
| POST | `/api/v1/orders/{id}/cancel` | 是 | 取消订单（仅待付款） |
| GET | `/api/v1/orders` | 是 | 我的订单分页（query `status`） |
| GET | `/api/v1/orders/{id}` | 是 | 订单详情 |
| GET | `/api/v1/orders/{id}/logistics` | 是 | 物流轨迹 |
| POST | `/api/v1/orders/{id}/confirm` | 是 | 确认收货 |
| POST | `/api/v1/orders/{id}/rebuy` | 是 | 再次购买 |

### 2.9 评价

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/reviews/orders/{orderId}/reviewable-items` | 是 | 订单可评价商品 |
| POST | `/api/v1/reviews` | 是 | 发表评价 |
| POST | `/api/v1/reviews/{id}/append` | 是 | 追评（body `{ content, images[] }`） |
| GET | `/api/v1/reviews/{id}` | 否 | 评价详情 |

### 2.10 消息

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/messages` | 是 | 消息分页（query `type`：ORDER/PROMO/SYSTEM） |
| GET | `/api/v1/messages/unread-count` | 是 | 未读消息数量 |
| PUT | `/api/v1/messages/{id}/read` | 是 | 标记单条已读 |
| PUT | `/api/v1/messages/read-all` | 是 | 全部标记已读 |

---

## 三、商家端（merchant 模块）· `/api/v1/merchant`

### 3.1 账号

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/merchant/auth/register` | 否 | 商家入驻申请（默认 auditStatus=0 待审核，审核通过后才能登录） |
| POST | `/api/v1/merchant/auth/login` | 否 | 商家登录（兼容老端点；推荐统一登录） |
| POST | `/api/v1/merchant/auth/logout` | 否 | 商家登出（需已登录态，否则 NotLoginException） |

### 3.2 店铺资料

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/merchant/profile` | 是 | 我的店铺资料 |
| PUT | `/api/v1/merchant/profile` | 是 | 更新店铺资料 |

### 3.3 商品

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/merchant/products` | 是 | 商品分页（query `status`） |
| GET | `/api/v1/merchant/products/{id}` | 是 | 商品详情 |
| POST | `/api/v1/merchant/products` | 是 | 新建商品（默认待审+下架） |
| PUT | `/api/v1/merchant/products/{id}` | 是 | 编辑商品 |
| POST | `/api/v1/merchant/products/{id}/on-shelf` | 是 | 上架（需审核通过） |
| POST | `/api/v1/merchant/products/{id}/off-shelf` | 是 | 下架 |

### 3.4 SKU

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/merchant/products/{productId}/skus` | 是 | 商品 SKU 列表 |
| POST | `/api/v1/merchant/products/{productId}/skus` | 是 | 新增 SKU |
| PUT | `/api/v1/merchant/skus/{id}` | 是 | 更新 SKU |
| DELETE | `/api/v1/merchant/skus/{id}` | 是 | 删除 SKU（已售禁止） |

### 3.5 订单

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/merchant/orders` | 是 | 订单分页（query `status`） |
| GET | `/api/v1/merchant/orders/{id}` | 是 | 订单详情 |
| POST | `/api/v1/merchant/orders/{id}/ship` | 是 | 发货（待发货→待收货） |

---

## 四、管理端（admin 模块）· `/api/v1/admin`

### 4.1 认证

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| POST | `/api/v1/admin/auth/login` | 否 | 管理员登录（兼容老端点） |
| POST | `/api/v1/admin/auth/logout` | 否 | 管理员登出（需已登录态，否则 NotLoginException） |

### 4.2 账号管理（管理员自身）

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/admins` | 是 | 管理员分页（query `keyword`、`status`） |
| GET | `/api/v1/admin/admins/{id}` | 是 | 管理员详情 |
| POST | `/api/v1/admin/admins` | 是 | 新建管理员 |
| PUT | `/api/v1/admin/admins/{id}` | 是 | 编辑管理员 |
| POST | `/api/v1/admin/admins/{id}/status` | 是 | 启停管理员 |
| POST | `/api/v1/admin/admins/{id}/reset-password` | 是 | 重置管理员密码 |
| DELETE | `/api/v1/admin/admins/{id}` | 是 | 删除管理员（id=1 超级管理员禁止） |

### 4.3 用户管理

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/users` | 是 | C 端用户分页（query `keyword`、`status`） |
| GET | `/api/v1/admin/users/{id}` | 是 | 用户详情 |
| POST | `/api/v1/admin/users/{id}/status` | 是 | 启停用户账号 |
| POST | `/api/v1/admin/users/{id}/reset-password` | 是 | 重置用户密码（默认 123456） |

### 4.4 商家审核

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/merchants` | 是 | 商家分页（query `keyword`、`auditStatus`、`status`） |
| GET | `/api/v1/admin/merchants/{id}` | 是 | 商家详情 |
| POST | `/api/v1/admin/merchants/{id}/audit` | 是 | 商家资质审核（auditStatus=1 通过；=2 拒绝） |
| POST | `/api/v1/admin/merchants/{id}/status` | 是 | 启停商家账号 |
| GET | `/api/v1/admin/merchants/audit-pending` | 是 | 待审核商家数量 |

### 4.5 商品审核

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/products` | 是 | 商品分页（query `keyword`、`auditStatus`、`status`、`merchantId`） |
| GET | `/api/v1/admin/products/{id}` | 是 | 商品详情 |
| POST | `/api/v1/admin/products/{id}/audit` | 是 | 商品审核（auditStatus=1 通过；=2 拒绝） |
| POST | `/api/v1/admin/products/{id}/off-shelf` | 是 | 强制下架 |
| GET | `/api/v1/admin/products/audit-pending` | 是 | 待审核商品数量 |

### 4.6 运营内容（Banner / 热词 / 分类）

**Banner**

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/banners` | 是 | Banner 列表（query `enabled`） |
| POST | `/api/v1/admin/banners` | 是 | 新建 Banner |
| PUT | `/api/v1/admin/banners/{id}` | 是 | 编辑 Banner |
| DELETE | `/api/v1/admin/banners/{id}` | 是 | 删除 Banner |

**热词**

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/hot-words` | 是 | 热词列表（query `keyword`） |
| POST | `/api/v1/admin/hot-words` | 是 | 新建热词 |
| PUT | `/api/v1/admin/hot-words/{id}` | 是 | 编辑热词 |
| DELETE | `/api/v1/admin/hot-words/{id}` | 是 | 删除热词 |

**分类**

| Method | Path | 鉴权 | 摘要 |
|---|---|---|---|
| GET | `/api/v1/admin/categories` | 是 | 分类列表（平铺） |
| GET | `/api/v1/admin/categories/tree` | 是 | 分类树 |
| POST | `/api/v1/admin/categories` | 是 | 新建分类 |
| PUT | `/api/v1/admin/categories/{id}` | 是 | 编辑分类 |
| POST | `/api/v1/admin/categories/{id}/status` | 是 | 启停分类 |
| DELETE | `/api/v1/admin/categories/{id}` | 是 | 删除分类（有子分类/被引用禁止） |

---

## 五、关键业务字段（速查）

### 订单状态 `status`

| 值 | 含义 |
|---|---|
| 1 | 待付款 |
| 2 | 待发货 |
| 3 | 待收货 |
| 4 | 已完成 |
| 5 | 已取消 |
| 6 | 退款中 |
| 7 | 已退款 |

### 审核状态 `auditStatus`

| 值 | 含义 |
|---|---|
| 0 | 待审核 |
| 1 | 通过 |
| 2 | 拒绝 |

### 启用/上下架 `status`（user / merchant / product）

| 值 | 含义 |
|---|---|
| 0 | 停用 / 下架 |
| 1 | 启用 / 上架 |

### 商品上下架 + 库存计算

- `auditStatus=1`（通过）+ `status=1`（上架）+ `sku.stock > 0` → 用户可见可买
- 已售 SKU 不允许删除（保护历史订单）

---

## 六、前端集成提示

### 1. 登录态保存

```js
// 登录成功后保存 token
localStorage.setItem('satoken', res.data.token)
localStorage.setItem('role', res.data.role) // user / merchant / admin

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

### 3. 文件上传

- 评价图片、商品图片等走 `/api/v1/common/upload`（如未实现可先用外链 URL 占位）

### 4. 演示账号（来自 `sql/03_test_data.sql`）

| 端 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `123456` |
| 商家 | `m01` | `123456` |
| 用户 | `zhangsan` | `123456` |

---

## 七、变更记录

- 2026-07-05：补全全部 controller `@Operation`/`@Parameter` 注解，补 DTO/VO `@Schema`，输出本文档供前端离线参考