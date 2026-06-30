# Plan 3 — 商家端业务（Design Spec）

> **日期：** 2026-06-30
> **范围：** 商家后台 4 个核心模块（店铺信息 / 商品管理 / SKU 库存 / 订单发货）共 15 个 REST 接口。
> **目标读者：** Plan 3 实施 subagent + 人工 code reviewer。

---

## 1. 背景与上下文

Plan 1 已搭好三端骨架并完成商家登录（`MerchantAuthController.login/logout`，`m01` 测试账号）。Plan 2 完成了用户端 9 模块 40 接口并归并到 `main`。当前 `freshfood-merchant` 模块除 login/logout 外还是空壳。

本计划把 `freshfood-merchant` 扩展为完整商家后台。**不新建数据库表**，复用 Plan 2 已有的 12 张业务表 + Plan 1 的 `merchant` 表 + 已有 DOs（`MerchantDO`、`ProductDO`、`SkuDO`、`OrderDO`、`OrderItemDO`）。所有端点登录态由 Sa-Token merchant StpLogic 管控。

## 2. 模块拆解与端点清单

合计 **15 接口**，全部位于 `/api/v1/merchant/...` 前缀下。

### M1. 店铺信息（shop profile） — 2 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/v1/merchant/profile` | → `MerchantVO` | 当前登录商家的完整信息 |
| 2 | PUT | `/api/v1/merchant/profile` | `MerchantUpdateDTO` → `MerchantVO` | 更新店铺名/logo/联系人姓名(加密)/联系人电话(加密)；`username` / `auditStatus` / `status` 仅服务端可控，前端不允许改 |

### M2. 商品管理（product CRUD） — 6 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 3 | GET | `/api/v1/merchant/products?status=&pageNum=&pageSize=` | → `PageR<ProductVO>` | 自己商家（merchantId = 当前登录商家）的商品分页；status 可选 0/1 |
| 4 | GET | `/api/v1/merchant/products/{id}` | → `ProductVO` | 详情（含 SKU 列表、商家信息），仅自己商家商品可见 |
| 5 | POST | `/api/v1/merchant/products` | `ProductCreateDTO` → `ProductVO` | 新建商品；`merchantId` 取登录商家；`auditStatus=0`（待审）；`status=0`（下架）；sales=0；rating=0 |
| 6 | PUT | `/api/v1/merchant/products/{id}` | `ProductUpdateDTO` → `ProductVO` | 修改 `name` / `categoryId` / `description` / `origin` / `mainImage`；不改 status，不改 auditStatus |
| 7 | POST | `/api/v1/merchant/products/{id}/on-shelf` | — → `R<Void>` | 上架：`status=1`；前置校验 `auditStatus==1`（已审核），否则抛 `PRODUCT_PENDING_AUDIT(3003)` |
| 8 | POST | `/api/v1/merchant/products/{id}/off-shelf` | — → `R<Void>` | 下架：`status=0`；不校验审核状态（商家可随时下架） |

### M3. SKU 库存 — 4 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 9 | GET | `/api/v1/merchant/products/{productId}/skus` | → `List<SkuVO>` | 列某商品的 SKU |
| 10 | POST | `/api/v1/merchant/products/{productId}/skus` | `SkuCreateDTO` → `SkuVO` | 新增 SKU；product 必须属于当前商家 |
| 11 | PUT | `/api/v1/merchant/skus/{id}` | `SkuUpdateDTO` → `SkuVO` | 改价/规格/库存/图片；SKU 必须属于当前商家 |
| 12 | DELETE | `/api/v1/merchant/skus/{id}` | — → `R<Void>` | 物理删除；前置校验 `sales==0`，否则抛 `SKU_HAS_SALES(3004)` |

### M4. 订单发货 — 3 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 13 | GET | `/api/v1/merchant/orders?status=&pageNum=&pageSize=` | → `PageR<MerchantOrderVO>` | 按 `merchantId = 当前商家` 过滤订单；status 可选 1-7 |
| 14 | GET | `/api/v1/merchant/orders/{id}` | → `MerchantOrderVO` | 详情（含 `items`、反序列化的 `address`）；merchantId 不匹配 → 404 |
| 15 | POST | `/api/v1/merchant/orders/{id}/ship` | — → `R<Void>` | 发货：`status` 2→3，`shipTime = now`；前置校验 merchantId + `status==2`，否则 `ORDER_STATUS_INVALID` / `ORDER_NOT_FOUND` |

合计：**2 + 6 + 4 + 3 = 15 接口**。

## 3. 数据模型

### 3.1 复用的已有 DOs/字段（**不新增表或列**）

| 实体 | 关键字段 | 来源 |
|---|---|---|
| `MerchantDO` | id / username / shopName / contactName(加密) / contactPhone(加密) / logo / auditStatus / status | Plan 1 |
| `ProductDO` | id / merchantId / categoryId / name / mainImage / description / origin / afterSalesTags / auditStatus / status / sales / rating | Plan 2 |
| `SkuDO` | id / productId / spec / price / stock / sales / image | Plan 2 |
| `OrderDO` | id / orderNo / userId / merchantId / totalAmount / shippingFee / discountAmount / payableAmount / addressSnapshot(JSON) / remark / status / expireTime / payTime / shipTime / confirmTime / payMethod | Plan 2 |
| `OrderItemDO` | id / orderId / skuId / productId / productNameSnapshot / specSnapshot / priceSnapshot / quantity | Plan 2 |

无需 `ALTER TABLE`。

### 3.2 状态码映射

- 商品 status：`0` 下架 / `1` 上架
- 商品 auditStatus：`0` 待审 / `1` 已通过 / `2` 已拒绝
- 订单 status（沿用 Plan 2 约定）：`1` 待付 / `2` 待发 / `3` 待收 / `4` 待评 / `5` 完成 / `6` 售后(本计划不用) / `7` 已取消
- 本计划唯一允许商家触发的状态转换：`order.status` 2→3（ship）

## 4. 数据流与权限隔离

### 4.1 商家登录身份获取

复用 Plan 1 的模式：

```java
Long currentMerchantId = StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null)
                              .getLoginIdAsLong();
```

### 4.2 数据隔离规则（重点）

所有读 / 写必须先用 `currentMerchantId` 过滤：

| 场景 | 隔离条件 |
|---|---|
| 列商品 | `ProductDO.merchantId == currentMerchantId` |
| 商品详情 | 同上，且 id 必须匹配；不匹配 → `BusinessException(NOT_FOUND)` |
| 改商品 | 同上 |
| 上下架 | 同上 |
| 列/详情/改/删 SKU | 通过 product 间接校验（`sku.productId → product.merchantId == currentMerchantId`） |
| 列订单 | `OrderDO.merchantId == currentMerchantId` |
| 订单详情 | 同上 |
| 发货 | 同上，且 `status==2` |

**严防越权：** 任何"通过 id 加载 + 改"的链路，校验 `entity.merchantId == currentMerchantId` 失败一律抛 `NOT_FOUND`（4001/1004），避免泄露资源存在性。

### 4.3 发货事务流（`OrderService.ship` 风格）

```
@Transactional
public void ship(Long orderId) {
    Long merchantId = currentMerchantId();
    OrderDO o = orderMapper.selectById(orderId);
    if (o == null || !o.getMerchantId().equals(merchantId)) {
        throw new BusinessException(ORDER_NOT_FOUND);
    }
    if (o.getStatus() != 2) {
        throw new BusinessException(ORDER_STATUS_INVALID);
    }
    o.setStatus(3);
    o.setShipTime(LocalDateTime.now());
    orderMapper.updateById(o);
}
```

不发物流消息、不发短信、不调外部物流 API。

## 5. 端点契约细节

### 5.1 DTO 字段

| DTO | 字段 | 校验 |
|---|---|---|
| `MerchantUpdateDTO` | shopName (String, @NotBlank @Size≤50), contactName (String, @NotBlank), contactPhone (String, @NotBlank @Pattern 手机号), logo (String, @Size≤500) | 三项 name + phone 必填 |
| `ProductCreateDTO` | name (String, @NotBlank @Size≤100), categoryId (Long, @NotNull), mainImage (String, @NotBlank), description (String, @Size≤2000), origin (String, @Size≤50) | categoryId 必填 |
| `ProductUpdateDTO` | 同上（校验略宽，部分字段可选） | 全部 @NotNull 或可选 |
| `SkuCreateDTO` | spec (String, @NotBlank @Size≤50), price (BigDecimal, @NotNull @DecimalMin("0.01")), stock (Integer, @NotNull @Min(0)), image (String, @Size≤500) | 价格 > 0；库存 >= 0 |
| `SkuUpdateDTO` | 同上，全可选 | 至少 1 个非空字段 |

### 5.2 VO 字段（沿用 Plan 2 结构）

- `MerchantVO`：id / username / shopName / contactName / contactPhone / logo / auditStatus / status / createTime — 已存在（Plan 1 建）
- `ProductVO`：复用 Plan 2 已有 `ProductDetailVO` 与 `ProductSimpleVO` 的并集；本计划新建独立的 `MerchantProductVO` 含 id / name / categoryId / categoryName / mainImage / priceRange / stock / sales / auditStatus / status / createTime — 用于商品列表简版
- `SkuVO`：复用 Plan 2 已有（在 `freshfood-user/.../vo/SkuVO.java`） — 需决定是否新建 merchant 版本；**方案：复用 Plan 2 的 SkuVO**，避免 VO 重复
- `MerchantOrderVO`：新建，独立于 Plan 2 的 `OrderVO`；字段：`id / orderNo / status / statusText / totalAmount / payableAmount / items (List<OrderItemVO>) / buyerName / buyerPhone / addressSnapshot (JSON string) / createTime / payTime / shipTime` — buyer 信息用加密字段，controllere 层临时解密后返回

### 5.3 buyer 信息解密

`OrderDO.addressSnapshot` 已是 JSON 字符串（Plan 2 创建订单时存）。订单详情接口反序列化即可拿到收货人姓名 + 手机号（**这两个字段就是加密存的，但 merchant 看自己店铺订单应该看得到**）。

设计决策：商家端 `MerchantOrderVO` 直接包含解密后的 `buyerName` / `buyerPhone` 字符串。service 层用 Plan 2 已搭好的 `FieldCrypto`（在 `com.yan.freshfood.common.crypto.FieldCryptoHolder`）解密。

**简化路径（推荐，先这样）：** merchant 端订单详情不暴露 buyer 姓名手机号，只显示 `addressSnapshot` JSON（前端可解）。理由：商家真实发货是看运单号 + 收货地址，不需要姓名手机号二次展示；姓名手机号加密字段保留为内部用。**最终决策：MVP 不展示解密 buyer 信息，address snapshot 直接以 JSON 字符串原样返回**。后续要展示再迭代。

## 6. 错误处理

### 6.1 复用错误码（已有）

| Code | 含义 | 触发 |
|---|---|---|
| `1004 NOT_FOUND` | 资源不存在 / 越权 | 加载实体 merchantId 不匹配 |
| `3002 STOCK_NOT_ENOUGH` | 库存不足 | （本计划用不到，预留） |
| `4001 ORDER_STATUS_INVALID` | 订单状态不允许该操作 | 发货时 status!=2 |
| `4002 ORDER_NOT_FOUND` | 订单不存在 | （复用 1004 也行） |

### 6.2 新增错误码（2 条，单独 commit）

| Code | 含义 | 触发 |
|---|---|---|
| `3003 PRODUCT_PENDING_AUDIT` | 商品待审核，不可上架 | on-shelf 时 `auditStatus != 1` |
| `3004 SKU_HAS_SALES` | SKU 已有销量，不可删除 | DELETE sku 时 `sales > 0` |

加在 `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java` 现有枚举里，与 Plan 2 的 6 条同一区段（3000 系列）。**不在数据库加列。**

## 7. 测试策略

仅 Service 单测（与 Plan 2 一致），覆盖高价值路径：

| Service 方法 | 测试用例 | 校验点 |
|---|---|---|
| `ProductService.create` | `create_throws_when_name_blank` | `name` 缺失抛 NOT_FOUND / InvalidArg（注：DTO 校验即可，省略） |
| `ProductService.onShelf` | `on_shelf_throws_when_pending_audit` | auditStatus=0 时抛 `PRODUCT_PENDING_AUDIT` |
| `SkuService.delete` | `delete_throws_when_has_sales` | sales=1 时抛 `SKU_HAS_SALES` |
| `OrderService.ship` | `ship_transitions_2_to_3_and_sets_ship_time` | status 2→3，shipTime 非空 |
| `OrderService.ship` | `ship_throws_when_status_not_2` | status=1 时抛 `ORDER_STATUS_INVALID` |

不写 controller 层测试（Plan 2 也没写）。

## 8. 实施任务拆解（与 Plan 2 同结构）

预计 14-16 个 commit，每个小且独立：

1. SQL：零新增
2. DO 复用（零新增）
3. Mapper：在 `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/` 创建 5 个新接口（ProductMapper / SkuMapper / OrderMapper / OrderItemMapper / CategoryMapper），全部继承已有 BaseMapper
4. ErrorCode 加 2 条
5. **Module 1 店铺信息**：MerchantUpdateDTO → MerchantProfileController (2 接口)
6. **Module 2 商品管理**：MerchantProductVO + ProductCreateDTO + ProductUpdateDTO → MerchantProductServiceImpl (6 方法) → MerchantProductController
7. **Module 3 SKU**：SkuCreateDTO + SkuUpdateDTO → MerchantSkuServiceImpl (4 方法) → MerchantSkuController
8. **Module 4 订单发货**：MerchantOrderVO → MerchantOrderServiceImpl (3 方法 + transactional ship) → MerchantOrderController
9. 单元测试：~5 个测试方法跨 3 个 Test 类
10. 验证与提交：git status → 分批 add → push + fast-forward merge to main

预计总工作量类比 Plan 2 的 25 task，约 18-22 个 task。

## 9. 范围外（明确不做）

- ❌ 退款审批 — Plan 3 范围 A 不做
- ❌ 库存预警列表 / low-stock
- ❌ 评价回复（ReviewDO.merchantReply 字段保留不用）
- ❌ 数据统计 / dashboard / 图表
- ❌ 商家子账号、角色权限
- ❌ 商家注册 — 继续 Plan 4 admin 端创建 + 审核
- ❌ 运单号 / 物流商填写
- ❌ 真实短信 / 推送通知
- ❌ 财务结算、对账单

## 10. 与 Plan 2 / Plan 4 的关系

```
Plan 1 foundation ─┬─ 商家登录 (m01) ──── 用
                   ├─ 用户登录 (u01) ──── 用
                   └─ 管理员登录 ────── Plan 4 用

Plan 2 user ───────┬─ 12 业务表 schema ─── 用
                   ├─ ProductDO / SkuDO / OrderDO etc ── 用
                   └─ HttpStatusCode / VO pattern ── 用

Plan 3 merchant ── 在 Plan 1+2 基础上扩展 ── 本 spec

Plan 4 admin (后续) ── 同样模式
```

## 11. 风险与缓解

| 风险 | 缓解 |
|---|---|
| merchant 改自己商品描述时改了不该改的字段（改 auditStatus/status） | 严格只 set 计划内的字段，不用 `BeanUtil.copyProperties` |
| 商家 A 改商家 B 的商品（id 泄露） | 所有 update / shelf 之前先按 id+merchantId 加载，merchantId 不匹配立即 NOT_FOUND |
| 删除 SKU 引入外键错误（order_item 引用） | Sales>0 拒绝；生产中还需 ON DELETE RESTRICT，本计划 dev 环境够用 |
| 测试商家 `m01` 商品跨商家看到 | seed 数据中 `m01` 拥有 product_id=1001（Plan 3 seed），其他商家 0 商品，便于测隔离 |
| Plan 2 的 `OrderVO`/`SkuVO` 在 merchant 端不够用 | 单独新建 `MerchantOrderVO`、`MerchantProductVO`；`SkuVO` 复用（同字段语义） |

## 12. 验收标准

完成时满足：

- [ ] `mvn clean compile` 通过（待 JDK 17 装好后）
- [ ] 15 个 REST 接口全部实现并通过单元测试覆盖
- [ ] merchant a 看不到 / 改不了 merchant b 的商品 / SKU / 订单（测试覆盖）
- [ ] on-shelf / off-shelf / ship 等状态变更事务正确
- [ ] 商品创建 → 默认下架待审，符合 Plan 4 admin 审核流程
- [ ] 无新增数据库列 / 表
- [ ] 单一 commit 主题、信息密度高、Chinese body
- [ ] `feature/merchant-business` 分支 fast-forward 合入 main，origin 同步

---
