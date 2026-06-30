# Plan 4 — 管理员端业务（Design Spec）

> **日期：** 2026-07-01
> **范围：** 管理员后台 4 个核心模块（商家审核 / 商品审核 / 用户管理 / 运营内容）共 **28 个 REST 接口**。
> **目标读者：** Plan 4 实施 subagent + 人工 code reviewer。
> **起点：** Plan 1 已搭好三端骨架，admin 模块已实现 `AdminAuthController`（登录/登出）。本计划在已有骨架上扩展 4 个业务模块。

---

## 1. 背景与上下文

Plan 1-3 已完成并合入 main：
- Plan 1：多模块骨架 + 三端登录（admin 模块已有 AdminAuthController）
- Plan 2：用户端 9 模块 40 接口
- Plan 3：商家端 4 模块 15 接口

当前 `freshfood-admin` 模块除登录/登出外还是空壳，已有：
- `AdminMapper`、`AdminDO`
- `AdminAuthController` / `AdminAuthService`(Impl)
- `AdminLoginDTO` / `AdminLoginVO` / `AdminVO`
- 测试数据 `admin / 123456`（SQL 已 seed）

本计划把 `freshfood-admin` 扩展为完整管理员后台。**不新建数据库表**，复用 12 张业务表 + `admin` 表。

## 2. 模块拆解与端点清单

合计 **28 接口**，全部位于 `/api/v1/admin/...` 前缀下（`/api/v1/admin/auth/login` 已在 Plan 1 实现）。

### M1. 商家审核（merchant audit） — 5 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/v1/admin/merchants?keyword=&auditStatus=&status=&pageNum=&pageSize=` | → `PageR<AdminMerchantVO>` | 分页查询；keyword 匹配 username/shopName；auditStatus 0/1/2 可选；status 0/1 可选 |
| 2 | GET | `/api/v1/admin/merchants/{id}` | → `AdminMerchantVO` | 详情；含解密 contactName / contactPhone |
| 3 | POST | `/api/v1/admin/merchants/{id}/audit` | `MerchantAuditDTO{auditStatus}` → `R<Void>` | 审核：`auditStatus` 1 通过 / 2 拒绝；前置校验当前 auditStatus==0，**否则抛 MERCHANT_AUDIT_INVALID(7003)** |
| 4 | POST | `/api/v1/admin/merchants/{id}/status` | `MerchantStatusDTO{status}` → `R<Void>` | 启用-禁用：status 0 禁用 / 1 启用；不限当前 status |
| 5 | GET | `/api/v1/admin/merchants/audit-pending` | → `Long` | 待审核商家数量（dashboard 角标用） |

### M2. 商品审核（product audit） — 5 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 6 | GET | `/api/v1/admin/products?keyword=&auditStatus=&status=&merchantId=&pageNum=&pageSize=` | → `PageR<AdminProductVO>` | 分页查询；keyword 匹配商品名；merchantId 过滤；auditStatus/status 可选 |
| 7 | GET | `/api/v1/admin/products/{id}` | → `AdminProductVO` | 详情；含 merchantName、categoryName |
| 8 | POST | `/api/v1/admin/products/{id}/audit` | `ProductAuditDTO{auditStatus}` → `R<Void>` | 审核：1 通过 / 2 拒绝；前置校验当前 auditStatus==0，**否则抛 PRODUCT_AUDIT_INVALID(3005)** |
| 9 | POST | `/api/v1/admin/products/{id}/off-shelf` | — → `R<Void>` | 强制下架：status 1→0；不限当前 status |
| 10 | GET | `/api/v1/admin/products/audit-pending` | → `Long` | 待审核商品数量（dashboard 角标用） |

### M3. 用户管理（user management） — 4 接口

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 11 | GET | `/api/v1/admin/users?keyword=&status=&pageNum=&pageSize=` | → `PageR<AdminUserVO>` | 分页查询；keyword 匹配 username/nickname；status 0/1 可选 |
| 12 | GET | `/api/v1/admin/users/{id}` | → `AdminUserVO` | 详情；含解密 phone / email |
| 13 | POST | `/api/v1/admin/users/{id}/status` | `UserStatusDTO{status}` → `R<Void>` | 启用-禁用：status 0/1；不限当前 status |
| 14 | POST | `/api/v1/admin/users/{id}/reset-password` | — → `R<Void>` | 重置密码为 `123456`（BCrypt 哈希后写入） |

### M4. 运营内容（content management） — 14 接口

#### Banner（4 接口）

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 15 | GET | `/api/v1/admin/banners?enabled=` | → `List<AdminBannerVO>` | 全量列表（不分页，Banner 数量 < 50）；enabled 可选过滤 |
| 16 | POST | `/api/v1/admin/banners` | `BannerCreateDTO` → `AdminBannerVO` | 新增；字段：title, image, linkType, linkTarget, sort, enabled, startTime, endTime |
| 17 | PUT | `/api/v1/admin/banners/{id}` | `BannerUpdateDTO` → `AdminBannerVO` | 更新 |
| 18 | DELETE | `/api/v1/admin/banners/{id}` | — → `R<Void>` | 逻辑删除（@TableLogic 自动 set deleted=1） |

#### HotWord（4 接口）

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 19 | GET | `/api/v1/admin/hot-words?keyword=` | → `List<AdminHotWordVO>` | 全量列表（不分页，热搜词 < 100）；keyword 可选过滤 |
| 20 | POST | `/api/v1/admin/hot-words` | `HotWordCreateDTO{keyword, sort}` → `AdminHotWordVO` | 新增；keyword 唯一（uk_keyword）；searchCount 默认 0 |
| 21 | PUT | `/api/v1/admin/hot-words/{id}` | `HotWordUpdateDTO{keyword, searchCount, sort}` → `AdminHotWordVO` | 更新；keyword 唯一 |
| 22 | DELETE | `/api/v1/admin/hot-words/{id}` | — → `R<Void>` | 逻辑删除 |

#### Category（6 接口）

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 23 | GET | `/api/v1/admin/categories` | → `List<AdminCategoryVO>` | 扁平列表（page 模式），按 sort ASC |
| 24 | GET | `/api/v1/admin/categories/tree` | → `List<AdminCategoryTreeVO>` | 嵌套树形（不分页，service 内存 build） |
| 25 | POST | `/api/v1/admin/categories` | `CategoryCreateDTO` → `AdminCategoryVO` | 新增；parentId=0 为顶级 |
| 26 | PUT | `/api/v1/admin/categories/{id}` | `CategoryUpdateDTO` → `AdminCategoryVO` | 更新（不改 parentId，防止误改层级） |
| 27 | POST | `/api/v1/admin/categories/{id}/status` | `CategoryStatusDTO{status}` → `R<Void>` | 启用-禁用：status 0/1 |
| 28 | DELETE | `/api/v1/admin/categories/{id}` | — → `R<Void>` | 逻辑删除；前置校验：不允许删除有子分类或被商品引用的分类，**否则抛 CATEGORY_IN_USE(7004)** |

## 3. 架构

### 3.1 模块归属

全部代码在 `freshfood-admin` 模块下：
- `controller/Admin{Module}Controller.java`
- `service/Admin{Module}Service.java` + `impl/Admin{Module}ServiceImpl.java`
- `dto/{Action}DTO.java`
- `vo/Admin{Module}{Entity}VO.java`
- `mapper/{Banner,HotWord}Mapper.java`（新增，其他 mapper 复用 user/merchant 模块的）

### 3.2 Mapper 复用

admin 模块操作 `user/merchant/product/category/sku` 等已存在的表时，**直接引用** user/merchant 模块的 mapper 接口：

```java
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;  // 注意：plan 1 的 merchant mapper 不在 user/merchant 模块名空间下，需确认位置
```

⚠️ MerchantMapper 不在 `freshfood-user` 也不在 `freshfood-merchant` 模块（merchant 模块用的是 user 模块的 mapper？需要 Task 1 阶段确认）。如果找不到，**admin 模块自己建** `MerchantMapper extends BaseMapper<MerchantDO>`（放 admin 模块自己的 mapper 包下）。

> **确认点（Task 1 实施时）**：`MerchantMapper` 的物理位置。预期在 `com.yan.freshfood.user.mapper`（plan 1 把 merchant/user 表的 mapper 都放在 user 模块下）— Task 1 直接 import 验证。

### 3.3 新增 mapper

`BannerMapper`、`HotWordMapper` 放 admin 模块，继承 BaseMapper：
```java
public interface BannerMapper extends BaseMapper<BannerDO> {}
public interface HotWordMapper extends BaseMapper<HotWordDO> {}
```

### 3.4 权限模型

- admin 是上帝视角，**不校验** merchantId / userId 隔离
- 但所有 admin 端点必须登录（@SaIgnore 仅 AdminAuthController 有，其他 controller 不加）
- 当前 admin id 取自 `StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null).getLoginIdAsLong()`，**仅用于审计日志占位字段**（本计划不写日志表，仅 mock 拿到 id 防 NPE）

## 4. 数据流

### 4.1 审核类操作

```
controller 接收 DTO
  ↓
service 校验当前状态：
  - 查实体 → null 抛 NOT_FOUND
  - 状态不允许 → 抛 MERCHANT_AUDIT_INVALID / PRODUCT_AUDIT_INVALID
  ↓
service 修改状态字段 + updateById
  ↓
controller 返回 R<Void>
```

### 4.2 分页查询（含 JOIN 信息）

例：商品列表需要展示 merchantName + categoryName

```
service 调用 productMapper.selectPage(page, queryWrapper)
  ↓
拿到 List<ProductDO> 后批量查 merchantMapper.selectBatchIds(merchantIds) → Map<Long, String> merchantNameMap
                    categoryMapper.selectBatchIds(categoryIds) → Map<Long, String> categoryNameMap
  ↓
toVO(ProductDO, merchantNameMap, categoryNameMap) 列表转换
  ↓
PageR.of(IPage<VO>) 返回
```

### 4.3 分类树构建

```
categoryMapper.selectList(null)  // 全表（删除的 @TableLogic 自动过滤）
  ↓
service 内存 build：
  Map<Long, AdminCategoryTreeVO> voMap
  List<AdminCategoryTreeVO> roots
  for each row:
    转为 VO put 到 voMap
  for each row:
    if parentId == 0 → roots.add(voMap.get(id))
    else → voMap.get(parentId).children.add(voMap.get(id))
  ↓
返回 roots
```

## 5. 异常码

复用：
- `NOT_FOUND(1004)` — 资源不存在
- `PARAM_INVALID(1001)` — 入参校验
- `USER_DISABLED(2003)` — 用户已禁用
- `MERCHANT_NOT_FOUND(7001)` — 商家不存在
- `MERCHANT_PENDING(7002)` — 商家未通过审核

新增（Task 2 阶段添加）：
- `MERCHANT_AUDIT_INVALID(7003)` — 商家当前审核状态不允许此操作
- `CATEGORY_IN_USE(7004)` — 分类有子分类或被商品引用，不可删
- `PRODUCT_AUDIT_INVALID(3005)` — 商品当前审核状态不允许此操作

## 6. 数据对象

### 6.1 DTO

| DTO | 字段 | 校验 |
|---|---|---|
| MerchantAuditDTO | `Integer auditStatus`（@NotNull；1 或 2） | 1=通过 2=拒绝 |
| MerchantStatusDTO | `Integer status`（@NotNull；0 或 1） | 0=禁用 1=启用 |
| ProductAuditDTO | `Integer auditStatus`（@NotNull；1 或 2） | 同上 |
| UserStatusDTO | `Integer status`（@NotNull；0 或 1） | 同上 |
| BannerCreateDTO | title(String @NotBlank @Size(100)) / image(String @NotBlank @Size(255)) / linkType(String @NotBlank) / linkTarget(String @Size(255)) / sort(Integer) / enabled(Integer 0/1) / startTime / endTime | enabled 必填 0/1 |
| BannerUpdateDTO | 同上（id 由 path 传入） | 同上 |
| HotWordCreateDTO | keyword(String @NotBlank @Size(50)) / sort(Integer) | keyword 唯一 |
| HotWordUpdateDTO | keyword / searchCount(Integer) / sort(Integer) | 同上 |
| CategoryCreateDTO | parentId(Long @NotNull) / name(String @NotBlank @Size(50)) / icon(String @Size(255)) / sort(Integer) / status(Integer 0/1 @NotNull) | parentId=0 为顶级 |
| CategoryUpdateDTO | name / icon / sort / status | 不含 parentId |
| CategoryStatusDTO | status(Integer @NotNull 0/1) | |

### 6.2 VO

| VO | 字段 | 说明 |
|---|---|---|
| AdminUserVO | id, username, nickname, avatar, phone(String), email(String), status, createTime | phone/email 由 EncryptedStringTypeHandler 自动解密 |
| AdminMerchantVO | id, username, shopName, contactName, contactPhone, logo, auditStatus, status, createTime | contactName/contactPhone 自动解密 |
| AdminProductVO | id, merchantId, merchantName(String), categoryId, categoryName(String), name, mainImage, description, origin, afterSalesTags, auditStatus, status, sales, rating, createTime | merchantName/categoryName 由 service JOIN 注入 |
| AdminBannerVO | id, title, image, linkType, linkTarget, sort, enabled, startTime, endTime, createTime | |
| AdminHotWordVO | id, keyword, searchCount, sort, createTime | |
| AdminCategoryVO | id, parentId, name, icon, sort, status, createTime | |
| AdminCategoryTreeVO | extends AdminCategoryVO + `List<AdminCategoryTreeVO> children` | |

### 6.3 命名一致性

- 所有金额出参用 `String` (toPlainString) — 本 plan 无金额字段
- 时间用 `LocalDateTime`
- 加密字段用 `String`（明文）

## 7. 测试策略

### 7.1 单元测试（与 Plan 2/3 一致）

| 测试类 | 用例数 | 覆盖 |
|--------|------|------|
| MerchantAdminServiceTest | 3 | 审核通过 0→1、审核拒绝 0→2、重复审核已通过的抛 7003 |
| ProductAdminServiceTest | 3 | 审核通过、审核拒绝、强制下架 |
| UserAdminServiceTest | 2 | 禁用用户、重置密码（断言 password 字段更新为 BCrypt("123456")） |
| ContentAdminServiceTest | 4 | Banner CRUD 完整链路 + Category 树构建（断言嵌套 children） |

合计 12 个用例。

### 7.2 Mockito 模式

- `@ExtendWith(MockitoExtension.class)`
- `@Mock private MapperXxxMapper xxxMapper;`
- `@InjectMocks private AdminXxxServiceImpl service;`
- StpUtil mock：`MockedStatic<StpUtil>` + `StpUtil.getStpLogic(any(), anyString())` + `getLoginIdAsLong()`
- 第二个参数必须 `anyString()`（非 `any()`）
- BusinessException 断言：`assertEquals(ErrorCode.FOO.getCode(), ex.getCode())`

### 7.3 admin 与 merchant 测试差异

- merchant service 测试要断言 merchantId 隔离
- **admin service 测试不需要**（admin 是上帝视角），mock StpUtil 主要防止 NPE

## 8. 风险与决策

### 8.1 已记录决策

- **删除策略**：所有 DELETE 端点走 MyBatis-Plus 逻辑删除（@TableLogic 自动），list 查询自动过滤
- **admin 不校验资源归属**：admin 看全量数据
- **rejectReason 不入库**：避免新增字段；DTO 只保留 auditStatus
- **商品/商家列表 N+1**：service 批量查 mapper + 内存 Map 注入展示字段，避免 SQL JOIN（简单可读）
- **Category 树内存构建**：全表 < 100 行，内存 build 足够
- **Banner/HotWord 不分页**：数据量小，全量列表即可
- **merchant mapper 位置**：待 Task 1 阶段确认；找不到就在 admin 模块新建
- **Map 复用 user/merchant 模块 mapper**：跨模块 Bean 注入，Spring 无问题

### 8.2 已知非阻塞警告（plan 4 范围内）

- 不实现 admin 自身账号管理（增删改其他 admin）— 留后续 plan
- 不实现操作日志表 — 仅 mock 当前 admin id 防 NPE
- 不实现数据统计 dashboard — 仅 2 个 audit-pending count 端点
- 不实现 SKU 管理（admin 不直接管 SKU，商家自己管）
- 不实现订单管理（admin 看订单通过 user/merchant 端点的数据，admin 不需要重复）

### 8.3 Task 数量预估

12 个实施 task：
1. 分支 + Mapper 复用验证 + 4 个新 mapper (Banner, HotWord)
2. ErrorCode 新增 3 条
3. 公共 VO（AuditPendingVO count 包装；MerchantStatusDTO 等共享 DTO）
4. M1 商家审核 DTO + Service
5. M1 商家审核 Controller
6. M2 商品审核 DTO + Service + Controller
7. M3 用户管理 DTO + Service + Controller
8. M4 Banner DTO + Service + Controller
9. M4 HotWord DTO + Service + Controller
10. M4 Category DTO + Service + Controller
11. 12 个单元测试
12. 全模块编译 + 测试 + 推送合 main

## 9. 实施顺序（task 之间依赖）

```
Task 1 (分支 + mapper 验证)
  ↓
Task 2 (ErrorCode +3 条)
  ↓
Task 3-10 (4 模块业务) — 各模块内部：DTO → Service → Controller
  ↓
Task 11 (12 个测试)
  ↓
Task 12 (编译验证 + 推送合 main)
```

每个 task 内部：subagent 实现 → spec reviewer 字节级审 → code quality reviewer 质量审 → 标记完成。

## 10. 与现有项目的兼容性

- 复用 `freshfood-common.exception.BusinessException/ErrorCode`
- 复用 `freshfood-common.response.R/PageR`
- 复用 `freshfood-common.constant.CommonConstants.TYPE_ADMIN`
- 复用 `freshfood-model.entity.{MerchantDO, UserDO, ProductDO, BannerDO, HotWordDO, CategoryDO, BaseDO}`
- 复用 Sa-Token admin StpLogic（Plan 1 已配置）
- 新增 mapper：`BannerMapper`、`HotWordMapper`（admin 模块）
- 不修改任何现有文件（除 ErrorCode 新增 3 条枚举）

## 11. 验收标准

- [ ] 所有 28 个端点按字面规范落地
- [ ] 12 个单元测试通过
- [ ] admin 登录后能完成：审核商家 → 该商家商品被审核 → 启用用户 → 下架 Banner 全流程
- [ ] 加密字段（contactName/contactPhone/phone/email）admin 看到明文
- [ ] 树形分类接口返回正确嵌套结构
- [ ] 逻辑删除生效（删后 list 查不到）
- [ ] 状态校验（重复审核、删有子分类的分类）抛对应异常码