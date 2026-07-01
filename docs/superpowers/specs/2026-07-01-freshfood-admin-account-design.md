# Plan 5 — Admin 自身账号管理（Design Spec）

> **日期：** 2026-07-01
> **范围：** Admin 账号完整生命周期管理 — 7 个 REST 端点。
> **目标读者：** Plan 5 实施 subagent + 人工 code reviewer。
> **起点：** Plan 4 已合 main，admin 表已 seed `admin/123456`，`AdminAuthController` 已实现 login/logout。admin 表无 role 字段（不变更 schema）。

---

## 1. 背景与上下文

Plan 1-4 已完成：
- Plan 1：多模块骨架 + 三端登录（admin 端 `AdminAuthController` + Sa-Token admin StpLogic）
- Plan 2：用户端 9 模块 40 接口
- Plan 3：商家端 4 模块 15 接口
- Plan 4：admin 端 4 业务模块 28 接口（商家审核/商品审核/用户管理/运营内容）

**当前 admin 端缺口**：
- admin 表已有数据但无任何 CRUD 端点
- 无法新增 admin（只能 seed SQL 插入）
- 无法禁用/启用其他 admin
- 无法重置其他 admin 密码（除直接改 SQL）
- 无 nickname 修改入口
- 无 admin 清理入口

本计划补全 admin 账号管理最后一环。**不修改 schema**（admin 表已有 `id/username/password/nickname/status/deleted` 全部所需字段）。

## 2. 决策记录（已与用户确认）

| 决策 | 选择 | 理由 |
|---|---|---|
| RBAC 角色系统 | **不加** | Plan 1 已 ship 无 role 字段；最小化变更；MVP 阶段任何 admin 互等足够 |
| Super admin 保护 | **id=1 锁死** | 防止 seed super admin 被误操作导致整个系统锁死 |
| 自操作限制 | **不可自伤** | 当前 admin 不可禁/删/重置自己密码 |
| 初始密码策略 | **Create 传明文** | 调用方传 password，service BCrypt 加密入库；不返回明文 |

## 3. 模块拆解与端点清单

合计 **7 接口**，全部位于 `/api/v1/admin/admins` 前缀下（`/api/v1/admin/auth/*` 已在 Plan 1 实现）。

| # | Method | Path | 请求 / 响应 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/v1/admin/admins?keyword=&status=&pageNum=&pageSize=` | → `PageR<AdminAccountVO>` | 分页查询；keyword 匹配 username/nickname；status 0/1 可选 |
| 2 | GET | `/api/v1/admin/admins/{id}` | → `AdminAccountVO` | 详情；**不含 password** |
| 3 | POST | `/api/v1/admin/admins` | `AdminCreateDTO{username, password, nickname}` → `AdminAccountVO` | 新建；username 唯一；password 必填 6-20 字；nickname 可选 |
| 4 | PUT | `/api/v1/admin/admins/{id}` | `AdminUpdateDTO{nickname}` → `AdminAccountVO` | 改 nickname；不改 username/password/status |
| 5 | POST | `/api/v1/admin/admins/{id}/status` | `AdminStatusDTO{status}` → `R<Void>` | 启用-禁用：status 0 禁用 / 1 启用 |
| 6 | POST | `/api/v1/admin/admins/{id}/reset-password` | `AdminResetPasswordDTO{password}` → `R<Void>` | 重置密码（调用方传明文，service BCrypt 后写库） |
| 7 | DELETE | `/api/v1/admin/admins/{id}` | — → `R<Void>` | 逻辑删除 |

合计：**1 + 1 + 1 + 1 + 1 + 1 + 1 = 7 接口**。

## 4. 保护规则（关键）

| 规则 | 触发 | 抛错 | 备注 |
|---|---|---|---|
| 不可操作 id=1（super admin） | create/update/status/resetPassword/delete 影响 id=1 | `ADMIN_PROTECTED(9002)` | 例外：GET detail 仍可看 |
| 不可禁/删自己 | 当前 admin id == path id 且操作是 status=0 或 delete | `ADMIN_SELF_OP_INVALID(9003)` | |
| 不可重置自己密码 | 当前 admin id == path id 且操作是 resetPassword | `ADMIN_SELF_OP_INVALID(9003)` | 防误操作锁出自己 |
| username 唯一 | create 时 username 已存在 | `ADMIN_USERNAME_EXISTS(9004)` | 与 `user`/`merchant` 表无关，独立 UNIQUE 约束 |
| 修改 nickname 为空 | updateDTO.nickname 为 null/blank | `@Valid` 自动拦截 | 强制要求非空 |

**设计选择**：
- 启用自己的 status (1→0 自己禁自己) 抛 `ADMIN_SELF_OP_INVALID(9003)` — 但当前已是禁用的可以接受（前端场景罕见）
- 启用自己的 status (0→1 自己解禁) 允许 — 不防误操作
- 改自己 nickname 允许 — 不锁
- 看自己 detail 允许 — 不锁

## 5. 架构

### 5.1 模块归属

全部代码在 `freshfood-admin` 模块下：
- `controller/AdminAccountController.java`
- `service/AdminAccountService.java` + `service/impl/AdminAccountServiceImpl.java`
- `dto/{AdminCreateDTO, AdminUpdateDTO, AdminStatusDTO, AdminResetPasswordDTO}.java`
- `vo/AdminAccountVO.java`

### 5.2 Mapper 复用

`AdminMapper` 已在 `freshfood-admin/src/main/java/com/yan/freshfood/admin/mapper/AdminMapper.java`（Plan 1 建好），无需新建。

### 5.3 Sa-Token 取当前 admin id

与 Plan 4 同模式：
```java
Long currentAdminId = StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null).getLoginIdAsLong();
```

所有"自伤"判断基于 `currentAdminId == pathId`。

### 5.4 数据流（典型：create）

```
controller 接收 AdminCreateDTO
  ↓
service 校验：
  - username 已存在? → throw ADMIN_USERNAME_EXISTS(9004)
  ↓
service 加密 password (BCrypt)
  ↓
service insert admin
  ↓
service 转换为 VO（不含 password）
  ↓
controller 返回 R<AdminAccountVO>
```

### 5.5 数据流（典型：updateStatus）

```
controller 接收 {id, status}
  ↓
service 校验：
  - admin 不存在? → throw NOT_FOUND(1004)
  - id == 1 (super admin)? → throw ADMIN_PROTECTED(9002)
  - id == currentAdminId && status == 0? → throw ADMIN_SELF_OP_INVALID(9003)
  ↓
service updateById
  ↓
controller 返回 R<Void>
```

## 6. 数据对象

### 6.1 DTO

| DTO | 字段 | 校验 |
|---|---|---|
| `AdminCreateDTO` | `username` (String @NotBlank @Size(3-50) @Pattern(^[a-zA-Z0-9_]+$)) / `password` (String @NotBlank @Size(6-20)) / `nickname` (String @Size(max=50)) | username 字母数字下划线 |
| `AdminUpdateDTO` | `nickname` (String @NotBlank @Size(max=50)) | nickname 必填非空 |
| `AdminStatusDTO` | `status` (Integer @NotNull 0/1) | |
| `AdminResetPasswordDTO` | `password` (String @NotBlank @Size(6-20)) | 新密码要求 |

### 6.2 VO

`AdminAccountVO` 字段：
- `Long id`
- `String username`
- `String nickname` (可为 null)
- `Integer status` (0 禁用 / 1 正常)
- `LocalDateTime createTime`
- `LocalDateTime updateTime`

**明确不含 `password`** — 任何出参都不能泄露 hash。

### 6.3 命名一致性

- 时间用 `LocalDateTime`
- 字段名与 AdminDO 保持一致（直接 `getX` / `setX`）
- 加密字段用 `String`（明文，admin 视角看自己 nickname 不加密）

## 7. 错误处理

### 7.1 复用错误码（已有）

| Code | 含义 | 触发 |
|---|---|---|
| `1004 NOT_FOUND` | 资源不存在 | update/status/resetPassword/delete 时 admin 不存在 |
| `1001 PARAM_INVALID` | 入参校验失败 | @Valid 自动拦截 |

### 7.2 新增错误码（3 条，Task 2 阶段添加）

| Code | 含义 | 触发 |
|---|---|---|
| `9002 ADMIN_PROTECTED` | 超级管理员（id=1）受保护 | 任何对 id=1 的写操作（create 不适用） |
| `9003 ADMIN_SELF_OP_INVALID` | 不能对自己做该操作 | 自禁/自删/自重置密码 |
| `9004 ADMIN_USERNAME_EXISTS` | admin 用户名已存在 | create 时 username 冲突 |

加在 `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java` 现有 `ADMIN_NOT_FOUND(9001)` 之后。

## 8. 测试策略

### 8.1 单元测试（4 个测试类，~7-8 个用例）

| 测试类 | 用例数 | 覆盖 |
|---|---|---|
| `AdminAccountServiceImplTest` | 7-8 | create 成功/重名抛 9004、update 成功、updateStatus 启禁、resetPassword 写 BCrypt、delete 成功、id=1 保护抛 9002、自伤抛 9003、status 不存在抛 NOT_FOUND |

**Mock 模式**（与 Plan 4 一致）：
- `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- `MockedStatic<StpUtil>` + `getStpLogic(any(), anyString())` + `getLoginIdAsLong()` 返回 2L（id=1 是 seed super，测试用 id=2 模拟普通 admin）
- `assertEquals(ErrorCode.FOO.getCode(), ex.getCode())`

### 8.2 边界场景

- id=1 + status=0 → 抛 9002（不允许禁 super）
- id=1 + delete → 抛 9002
- currentAdminId=2 + pathId=2 + status=0 → 抛 9003（不可自禁）
- currentAdminId=2 + pathId=2 + resetPassword → 抛 9003（不可自重置密码）
- create username="admin"（已存在）→ 抛 9004
- update nickname=null → @Valid 拦截（无需 service 测试）

## 9. 实施任务拆解

预计 8-10 个 commit，小且独立：

1. **ErrorCode 加 3 条** (9002/9003/9004)
2. **DTO 4 个** (Create/Update/Status/ResetPassword) + **VO 1 个** (AdminAccountVO)
3. **Service 接口** (AdminAccountService 7 方法)
4. **Service impl** (AdminAccountServiceImpl 7 方法 + toVO helper)
5. **Controller** (AdminAccountController 7 端点)
6. **单元测试** (1 个 Test 类 7-8 用例)
7. **静态结构审查** (subagent Read 全部文件，替代 mvn compile)
8. **推送 + fast-forward 合 main**

## 10. 范围外（明确不做）

- ❌ RBAC 角色系统（已确认不加 role 字段）
- ❌ Admin 修改自己密码 / nickname 的独立端点（auth 改造不在 Plan 5 范围）
- ❌ Admin 头像/邮箱/手机号字段（admin 表无此字段，加字段属 schema 变更）
- ❌ Admin 操作日志（Plan 4 spec §8.2 已明确不做，留后续 plan）
- ❌ Admin 批量导入/导出
- ❌ Admin 锁定（连续登录失败 N 次后锁定 — Plan 1 没建，暂不补）
- ❌ 删除 admin 的级联检查（admin 删 admin 只需禁，无需业务级联）

## 11. 与现有项目兼容性

- 复用 `freshfood-common.exception.BusinessException/ErrorCode`
- 复用 `freshfood-common.response.R/PageR`
- 复用 `freshfood-common.constant.CommonConstants.{TYPE_ADMIN, DEFAULT_PASSWORD}`（重置密码可选是否用 DEFAULT_PASSWORD，但本设计用调用方传新密码）
- 复用 `freshfood-model.entity.AdminDO/BaseDO`（含 @TableLogic 逻辑删除）
- 复用 `freshfood-admin.mapper.AdminMapper`（Plan 1 已建）
- 复用 Sa-Token admin StpLogic（Plan 1 已配 `stpAdminLogic` bean）
- 不修改任何现有文件（除 ErrorCode 新增 3 条枚举）

## 12. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 误禁 super admin 导致所有 admin 锁死 | id=1 锁死；service 层强制校验 |
| 自禁自己导致被锁出 | service 层校验 currentAdminId == pathId 时拒 |
| 重置密码后调用方忘转交新密码 | 由调用方负责（前端应展示一次性明文） |
| 删除 admin 后仍有 Sa-Token session | 业务级"踢下线"需 Sa-Token kickout，超出 Plan 5 范围；建议 admin 删前先禁 |
| username 唯一约束冲突 | 复用表 `UNIQUE KEY uk_username`；service 显式查重 + 数据库兜底 |
| BCrypt 加密耗时 | 重置/创建各 1 次，影响忽略不计 |

## 13. 验收标准

完成时满足：

- [ ] `mvn clean compile` 通过（待 JDK 17 装好后）
- [ ] 7 个 REST 接口全部实现
- [ ] 7-8 个单元测试通过
- [ ] 7 个接口覆盖：page / detail / create / update / status / resetPassword / delete
- [ ] id=1 super admin 任何写操作抛 9002
- [ ] 当前 admin 不可自禁/自删/自重置（抛 9003）
- [ ] create 重名 username 抛 9004
- [ ] 所有出参 VO 不含 password
- [ ] `feature/admin-account` 分支 fast-forward 合入 main，origin 同步
- [ ] 单一 commit 主题、信息密度高、Chinese body
