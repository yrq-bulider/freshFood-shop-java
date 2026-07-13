# freshfood-shop 精简设计文档

> 日期：2026-07-11
> 目标:把项目从 90 端点 / 15 表 / 7 模块精简到 19 端点 / 9 表 / 6 模块,聚焦"浏览→加购→下单→发货→评价"主链路,代码/SQL/文档三方同步精简,满足课程验收。

---

## 一、设计目标

| 维度 | 当前 | 目标 |
|---|---|---|
| Maven 模块 | 7 | 6(砍 `freshfood-admin`) |
| REST 端点 | 90 | 19(用户 14 + 商家 4 + app 1) |
| 数据库表 | 15 | 9(砍 admin/message/search_history/address/banner/hot_word) |
| `.java` 文件 | ~225 | ~110 |
| 三端覆盖 | 用户/商家/管理 | 用户 + 商家 |

**保留范围**：用户端核心 6 功能(首页-搜索-加购-下单-发货-评价)+ 必要支撑(登录/注册、商品详情是搜索和加购的前提、订单详情是收货评价的前提)。
**砍掉范围**：管理端整套;用户端的装饰功能(消息/追评/再次购买/地址簿/密码修改/个人资料/首页轮播/首页推荐/热搜词/商品评价单独接口);商家端的商品管理全套。

---

## 二、模块调整

### 2.1 删除
- **`freshfood-admin/`**:整个 Maven 模块,包含 6 个 controller、6 个 service、6 个 mapper、12 个 DTO/VO。

### 2.2 保留
- `freshfood-common/`、`freshfood-framework/`、`freshfood-model/` 不动。
- `freshfood-user/`、`freshfood-merchant/`、`freshfood-app/` 保留但内部代码精简。

### 2.3 父 POM 调整
- `pom.xml` 删 `freshfood-admin` 模块声明。
- `freshfood-app/pom.xml` 删 `freshfood-admin` 依赖。
- 整个 `freshfood-admin/` 目录删除(包括 `pom.xml`)。

---

## 三、接口清单(19 个)

### 3.1 用户端 `/api/v1/*` (14 端点)

| 类别 | Method | Path | 功能 |
|---|---|---|---|
| 账号 | POST | `/auth/register` | 用户注册 |
| 账号 | POST | `/auth/login` | 用户登录 |
| 首页 | GET | `/home/categories` | 分类树 |
| 商品 | GET | `/products/{id}` | 商品详情(响应体含 `reviews` 字段,首屏评价) |
| 搜索 | GET | `/search/products` | 搜索商品 |
| 购物车 | GET | `/cart` | 我的购物车 |
| 购物车 | POST | `/cart` | 加入购物车 |
| 购物车 | PUT | `/cart/{id}` | 更新数量 |
| 购物车 | DELETE | `/cart/{id}` | 删除单项 |
| 订单 | POST | `/orders` | 提交订单(必填收货人/电话/地址) |
| 订单 | POST | `/orders/{id}/pay` | 模拟支付 |
| 订单 | POST | `/orders/{id}/confirm` | 确认收货 |
| 订单 | GET | `/orders` | 我的订单分页 |
| 订单 | GET | `/orders/{id}` | 订单详情 |
| 评价 | POST | `/reviews` | 发表评价 |

### 3.2 商家端 `/api/v1/merchant/*` (4 端点)

| 类别 | Method | Path | 功能 |
|---|---|---|---|
| 账号 | POST | `/auth/login` | 商家登录 |
| 订单 | GET | `/orders` | 商家订单分页 |
| 订单 | GET | `/orders/{id}` | 商家订单详情 |
| 订单 | POST | `/orders/{id}/ship` | 商家发货 |

### 3.3 统一登录 `/api/v1/*` (1 端点,app 模块)

| 类别 | Method | Path | 功能 |
|---|---|---|---|
| 认证 | POST | `/auth/login` | 统一登录(user → merchant 顺序匹配) |

> 不再需要管理端匹配。

---

## 四、Controller 处理

### 4.1 删(8 个 controller)

| 模块 | Controller | 原因 |
|---|---|---|
| user | `MessageController` | 消息系统整体砍 |
| user | `UserController` | 个人资料 + 密码修改砍 |
| user | `AddressController` | 地址簿砍,收货信息直接进订单表 |
| merchant | `MerchantProfileController` | 店铺资料维护砍 |
| merchant | `MerchantProductController` | 商品管理砍(演示数据 SQL 直接写) |
| merchant | `MerchantSkuController` | SKU 管理砍 |
| admin | `AdminAuthController` / `AdminAccountController` / `AdminContentController` / `AdminMerchantController` / `AdminProductController` / `AdminUserController` | 管理端整砍 |

### 4.2 简化(3 个 controller)

| Controller | 删的端点 | 留的端点 |
|---|---|---|
| `OrderController`(user) | `preview` / `cancel` / `rebuy` / `logistics` | `create` / `pay` / `confirm` / `list` / `detail` |
| `ReviewController`(user) | `append`(追评) / `getReviewableItems` / `getReview` 详情 | `create` |
| `HomeController`(user) | `banners` / `recommendations`(2 端点) | `categories`(1 端点) |

### 4.3 留(7 个 controller)

`AuthController` / `HomeController` / `ProductController` / `SearchController` / `CartController` / `OrderController` / `ReviewController` / `MerchantAuthController` / `MerchantOrderController` / `UnifiedAuthController`。

> **HomeController 精简**：原 banners / categories / recommendations 3 端点 → 只剩 `categories`。
> **ProductController 精简**：原 detail + reviews 2 端点 → 只剩 `detail`,reviews 列表字段并入 detail 响应体(返回该商品最新 10 条评价)。
> **SearchController 精简**：原 hot-words + products 2 端点 → 只剩 `products`。
> **CartController 精简**：只留 `list / create / update / delete` 4 端点。

---

## 五、Service / Mapper / DTO / VO 处理

每个 controller 对应一个 Service + ServiceImpl + Mapper + 多个 DTO/VO。精简规则:

1. **随 controller 删除** —— 整个 service 包、dto、vo 全删。
2. **简化 controller** —— 对应 Service 删未使用的方法(避免死代码),DTO/VO 删未使用的类。
3. **mapper.java + mapper.xml** —— 删未使用的方法;用 MyBatis-Plus `BaseMapper` 自带 CRUD 替代手写 SQL 的,直接复用。
4. **不动** —— `common` / `framework` / `model` 里的 `BaseDO` / `R` / `PageR` / `ErrorCode` 等基础设施。

---

## 六、数据库表变更

### 6.1 删除 6 张表

| 表 | 删除原因 |
|---|---|
| `admin` | 管理端砍了 |
| `message` | 消息系统砍了 |
| `search_history` | 搜索历史功能砍了(可换前端 LocalStorage) |
| `address` | 地址簿砍了,收货信息进订单表 |
| `banner` | 首页轮播砍了(前端硬编码 banner 图) |
| `hot_word` | 热搜词砍了(前端硬编码热搜词) |

### 6.2 修改 1 张表

**`orders` 表新增 3 个字段**(替代 address 表):

```sql
`receiver_name`   VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
`receiver_phone`  VARCHAR(20)  NOT NULL COMMENT '收货人电话',
`receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
```

迁移:旧 `address` 表的数据如果有,本期不迁移(演示环境重建)。

### 6.3 保留 9 张表

`user` / `merchant` / `category` / `product` / `sku` / `cart` / `orders` / `order_item` / `review`。

### 6.4 字段精简(可选)

- `review` 表的 `reply` / `reply_time` 字段保留(商家可能回复,虽然本期没做商家回复端点,但数据完整)。
- `merchant.audit_status` 保留(演示账号 `m01` 状态置为 1 通过)。
- `product.audit_status` 同上(演示数据全部审核通过)。

---

## 七、SQL 脚本

### 7.1 现状
`sql/00_init_all_tables.sql` 一份大脚本(292 行,只建表不灌数据,演示账号靠 `/auth/register` 自助创建)。
`sql/02_alter_encrypted_field_length.sql` 一份迁移脚本(把加密字段扩到 VARCHAR(255))。

### 7.2 处理

**新建** `sql/01_init.sql`(单一脚本):
- 删 `admin` / `message` / `search_history` / `address` / `banner` / `hot_word` 6 张表
- `orders` 表结构调整:
  - 删 `address_snapshot` JSON 字段
  - 新增 3 个字段:`receiver_name` (VARCHAR(255),加密) / `receiver_phone` (VARCHAR(255),加密) / `receiver_address` (VARCHAR(255))
  - `status` 注释精简:`1待付/2待发/3待收/4完成/5取消`(去掉待评/售后)
- 9 张表的建表语句全保留(用户/商家/分类/商品/SKU/购物车/订单/订单明细/评价)
- DROP 表清单同步精简
- 不灌演示数据(BCrypt 密码无法 SQL 注入,演示账号靠 `/auth/register` 创建)

**删**:
- `sql/00_init_all_tables.sql`(已被 `01_init.sql` 替代)
- `sql/02_alter_encrypted_field_length.sql`(新建表时加密字段直接用 VARCHAR(255),后期补丁冗余)

### 7.3 README 同步
更新 README 中 `### 2. 初始化数据库` 章节,只引用 1 份脚本 `01_init.sql`。

---

## 八、文档更新

### 8.1 `README.md`
- 删"管理端"段落(`### 🔧 管理端`)
- 接口数 90 → 19
- 演示账号删 admin
- 模块结构图删 `freshfood-admin`
- "端到端流程示例" 改为只演示用户 + 商家两端

### 8.2 `docs/API.md`
- 整篇重写,只列 19 端点
- 删管理端整个章节
- 删"消息 / 追评 / 再购买 / 地址 / 密码 / 资料 / 退款"段落

### 8.3 `docs/superpowers/specs/`
- **删 2 份**:`2026-07-01-freshfood-admin-account-design.md` / `2026-07-01-freshfood-admin-business-design.md`
- **精简 2 份**:`2026-06-30-freshfood-merchant-business-design.md`(只保留商家订单 + 登录) / `2026-07-04-freshfood-unified-login-design.md`(去掉 admin 匹配)
- **新增 1 份**:本 spec(`2026-07-11-freshfood-simplify-design.md`)

### 8.4 `docs/superpowers/plans/`
- **删 2 份**:`2026-07-01-freshfood-admin-account.md` / `2026-07-01-freshfood-admin-business.md`
- **精简 3 份**:user-business / merchant-business / unified-login 3 份 plan
- 不动:`2026-06-29-freshfood-foundation.md`(基础设施 plan,跟精简无关)

---

## 九、Sa-Token 配置简化

`freshfood-framework` 中 Sa-Token 多 StpLogic 配置:
- **删**:`admin` StpLogic、`admin` 拦截器、`admin` 注解扫描
- **留**:`user` / `merchant` 两套 StpLogic

---

## 十、application.yml 调整

- 删 admin 相关配置(如有)
- 删 `sa-token` 中 admin 路由前缀

---

## 十一、验收标准

精简完成后,以下场景可演示通过:

1. **用户登录** → `POST /api/v1/auth/register` 或 `/auth/login` 拿 token
2. **首页浏览** → `GET /home/categories`(banner 图/热搜词/推荐商品前端硬编码)
3. **搜索商品** → `GET /search/products?keyword=苹果&sort=price_asc`
4. **查看详情** → `GET /products/{id}`(响应体含 `reviews` 数组)
5. **加入购物车** → `POST /cart` 加入,`GET /cart` 查看
6. **提交订单** → `POST /orders` (body 带 receiverName/Phone/Address)
7. **模拟支付** → `POST /orders/{id}/pay` (status 2 → 3)
8. **商家发货** → 商家登录 → `POST /merchant/orders/{id}/ship` (status 3 → 4)
9. **确认收货** → `POST /orders/{id}/confirm` (status 4 → 5)
10. **发表评价** → `POST /reviews` (orderId + productId + rating + content)

**演示账号**:
- 用户:zhangsan / lisi / 123456
- 商家:m01 / 123456

---

## 十二、不在范围内(后续可加)

- 退款/售后
- 优惠券/活动
- 物流轨迹模拟
- 用户消息
- 收藏/关注
- WebSocket / SSE 推送
- Elasticsearch / Redis 集成
- 文件上传
- 单元测试覆盖(仅靠 swagger-ui 演示)

---

## 十三、实施步骤概要

详细实施步骤由 writing-plans skill 输出 plan。本 spec 只定义"做什么",plan 定义"按什么顺序做"。

大致顺序:
1. SQL 脚本精简并新增 orders 字段
2. 删 admin 模块(目录 + pom + Sa-Token 配置)
3. 精简 user 模块:删 3 个 controller,简化 3 个 controller 的 service/dto/vo/mapper
4. 精简 merchant 模块:删 3 个 controller
5. 精简 cart controller(4 端点)
6. 更新 application.yml + Sa-Token 配置
7. 更新 README.md / API.md / specs / plans
8. mvn compile 验证 + 启动 swagger-ui 自测

---

## 十四、补遗(项目深查后发现)

### 14.1 `freshfood-model/` 实体类删除

| 文件 | 删除原因 |
|---|---|
| `entity/AdminDO.java` | admin 表删除 |
| `entity/content/MessageDO.java` | message 表删除 |
| `entity/content/SearchHistoryDO.java` | search_history 表删除 |
| `entity/trade/AddressDO.java` | address 表删除,收货信息进 orders |

保留:`UserDO` / `MerchantDO` / `BaseDO` / `content/ReviewDO` / `product/{Banner,Category,HotWord,Product,Sku}DO` / `trade/{Cart,Order,OrderItem}DO`

### 14.2 `freshfood-framework/` 配置简化

#### `SaTokenConfig.java`
删 `stpAdminLogic` Bean(原 23-26 行),保留 `stpUserLogic` / `stpMerchantLogic`。

#### `CommonConstants.java`(common 模块)
删 `TYPE_ADMIN` 常量(原 15 行),保留 `TYPE_USER` / `TYPE_MERCHANT`。

#### 其他配置
`MybatisPlusConfig` / `OpenApiConfig` / `WebMvcConfig` / `CryptoConfig` 不动。

### 14.3 `freshfood-user/` mapper 清理

| 文件 | 处理 |
|---|---|
| `mapper/AddressMapper.java` | 删 |
| `mapper/MessageMapper.java` | 删 |
| `mapper/SearchHistoryMapper.java` | 删 |

保留:`UserMapper` / `CategoryMapper` / `CartMapper` / `ProductMapper` / `SkuMapper` / `ReviewMapper` / `OrderMapper` / `OrderItemMapper`

#### `freshfood-user/` mapper 删除

| 文件 | 处理 |
|---|---|
| `mapper/BannerMapper.java` | 删(`banner` 表删除,`HomeController.banners` 端点砍) |
| `mapper/HotWordMapper.java` | 删(`hot_word` 表删除,`SearchController.hot-words` 端点砍) |

### 14.4 `freshfood-user/` DTO 清理

| 文件 | 处理 |
|---|---|
| `dto/AddressDTO.java` | 删 |
| `dto/OrderPreviewDTO.java` | 删(`preview` 端点砍) |
| `dto/UpdatePasswordDTO.java` | 删(密码修改砍) |

保留:`LoginDTO` / `RegisterDTO` / `CartAddDTO` / `CartUpdateDTO` / `OrderCreateDTO` / `ReviewCreateDTO`

### 14.5 `freshfood-merchant/` 完整清理

#### Controller 全砍
- `MerchantProductController` + 整个 `MerchantProductService` / `MerchantProductServiceImpl` → 删
- `MerchantSkuController` + 整个 `MerchantSkuService` / `MerchantSkuServiceImpl` → 删
- `MerchantProfileController` + 整个 `MerchantProfileService` / `MerchantProfileServiceImpl` → 删

#### DTO 清理
| 文件 | 处理 |
|---|---|
| `dto/MerchantRegisterDTO.java` | 删(商家注册端点砍) |
| `dto/MerchantUpdateDTO.java` | 删(资料维护砍) |
| `dto/ProductCreateDTO.java` | 删 |
| `dto/ProductUpdateDTO.java` | 删 |
| `dto/SkuCreateDTO.java` | 删 |
| `dto/SkuUpdateDTO.java` | 删 |

保留:`MerchantLoginDTO`

#### VO 清理
| 文件 | 处理 |
|---|---|
| `vo/MerchantProductVO.java` | 删 |
| `vo/SkuVO.java` | 删 |

保留:`MerchantVO` / `MerchantLoginVO` / `MerchantOrderVO` / `MerchantOrderItemVO`

### 14.6 `freshfood-app/` 统一登录简化

#### `UnifiedAuthServiceImpl.java`
- 删 `adminMapper` / `adminAuthService` 字段及对应 import
- 删第 51-56 行 admin 匹配分支
- 保留 user → merchant 两段匹配

#### `freshfood-app/pom.xml`
删 `<dependency>freshfood-admin</dependency>`,`AdminMapper` / `AdminAuthService` / `AdminLoginVO` / `AdminDO` 引用随之消失。

### 14.7 加密模块 review

`freshfood-common/crypto/` 三件套(`EncryptedStringTypeHandler` / `FieldCrypto` / `FieldCryptoHolder`)**保留**,精简后还有加密字段:

| 表 | 加密字段 | 保留 |
|---|---|---|
| `user` | `phone` | ✅ |
| `merchant` | `contact_phone` | ✅ |
| `orders`(新增) | `receiver_phone` | ✅ |

### 14.8 测试代码

- `freshfood-admin/src/test/` 整目录随 admin 模块删除(不保留测试)。
- 其他模块无测试代码(我已确认)。

### 14.9 父 POM 完整调整

`pom.xml`:
- `<modules>` 删 `<module>freshfood-admin</module>`
- `<dependencyManagement>` 删 admin 内部依赖(若 admin 引入过独立依赖)

`freshfood-app/pom.xml`:
- `<dependencies>` 删 `freshfood-admin`
- 检查其他依赖,user/merchant 应已在

### 14.10 端点数复核

- 用户端 14 = 账号 2 + 首页 1 + 商品 1 + 搜索 1 + 购物车 4 + 订单 4 + 评价 1
- 商家端 4 = 登录 1 + 订单 3
- app 1 = 统一登录
- **合计 19 端点**,与一、设计目标一致。