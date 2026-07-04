# Plan 6 — 统一登录 + 全局 username 唯一（Design Spec）

> **日期：** 2026-07-04
> **范围：** 把现有的 3 个独立登录端点合并为 1 个统一登录端点；3 张账号表 `username` 全局唯一。
> **目标读者：** Plan 6 实施 subagent + 人工 code reviewer。
> **起点：** Plan 1-5 已合；3 端账号表 (`user`/`merchant`/`admin`) + 各自登录 + 各自 Sa-Token StpLogic 已实现。`接口文档.md` 已有 §2/§3/§4 三端登录描述。

---

## 1. 背景与上下文

**当前状态：**
- 3 个独立登录端点：`POST /api/v1/auth/login`（user），`POST /api/v1/merchant/auth/login`（merchant），`POST /api/v1/admin/auth/login`（admin）。
- 3 张独立账号表，`user.username` / `merchant.username` / `admin.username` 各自 `UNIQUE KEY uk_username`，3 张表之间不互斥（同名跨表合法）。
- 3 套 Sa-Token 命名空间：`StpUser`（user 模块默认）、`StpMerchant`、`StpAdmin`；token 不跨端通用。
- 现有登录响应（user 端示例）：
  ```json
  { "code": 0, "data": { "token": "...", "user": { "id": 10001, "username": "zhangsan", "nickname": "张三", "avatar": "..." } } }
  ```

**前端形态：** 前端只有 1 个登录页（不分端），登录成功后按后端返回的 `role` 字段跳到用户端 / 商家端 / 管理端界面，UI 按 role 区分。

**当前痛点：** 3 套独立登录 + 端点分散，前端单一登录页没法拼出"调哪个、跳哪个"。后端需要把它收敛成一个入口 + 在响应里带 role。

**本计划补全：** 统一登录入口 + role 信息返回 + 用户名跨表全局唯一。

---

## 2. 决策记录（已与用户确认）

| 决策 | 选择 | 理由 |
|---|---|---|
| 统一登录路径 | **复用** `POST /api/v1/auth/login`（原 user 端登录） | 与"不堆功能 / 文档优先"对齐；前端只记 1 个端点；merchant / admin 路径不冲突 |
| 老端点去留 | merchant / admin 登录端点**保留 + `@Deprecated`** | 不破坏现有调试 / 后台脚本调用；文档标注"推荐统一端点" |
| 用户名唯一性实现 | **应用层守护**（service 跨表查 3 张表） | DB 跨表 UNIQUE 不支持；触发器 / 中间表都是 schema 变更；课设级并发冲突概率极低 |
| 优先级 | user → merchant → admin 顺序查表 | 与现有接口路径顺序一致；同名横跨时固定行为 |
| 响应结构 | 用 `profile` 替代原 `user`，新增 `role` 字段 | 字段语义清晰；前端按 role 决定 profile 取哪些键 |
| Sa-Token token | 落到命中的 StpLogic（StpUser / StpMerchant / StpAdmin） | 既有命名空间不变；后续请求 Header `satoken` 按既有路径走权限校验 |
| 鉴权 / 角色字段 schema | **不动 schema** | Plan 5 已 ship admin 无 role 字段；保持一致 |
| 旧 token 处理 | 不升级 / 不迁移 | 各自 namespace 不互通，老 token 不受影响 |

---

## 3. 端点清单

合计 **改造 1 个 + 加 1 个新错误码 + 加 1 个新 service + 加 1 个新公共错误**。

| # | 类型 | Path / 位置 | 说明 |
|---|---|---|---|
| 1 | 接口改造 | `POST /api/v1/auth/login` | 行为升级为统一入口（user/merchant/admin 任一命中） |
| 2 | 接口保留 | `POST /api/v1/merchant/auth/login` | 不动；接口标 `@Deprecated`；doc 标 deprecated |
| 3 | 接口保留 | `POST /api/v1/admin/auth/login` | 不动；接口标 `@Deprecated`；doc 标 deprecated |
| 4 | 新增 endpoint | （无新增路径） | — |
| 5 | 错误码 | `ErrorCode.GLOBAL_USERNAME_EXISTS(1002)` | 跨表 username 冲突 |
| 6 | 新 service | `UnifiedAuthService.login(username, password)` | 跨表查 / 验密 / 调对应 doLogin / 包装 VO |
| 7 | 新 service | `UsernameUniquenessChecker.checkAvailable(username)` | 3 张表任一命中 → 抛错 |
| 8 | 新 controller | `UnifiedAuthController` | 路由到 `UnifiedAuthService` |

合计：**0 新增路径 + 1 改造 + 2 标 deprecated + 1 错误码 + 2 新 service + 1 新 controller**。

---

## 4. 接口契约

### 4.1 统一登录

**请求：**
```
POST /api/v1/auth/login
Content-Type: application/json
```
```json
{ "username": "zhangsan", "password": "123456" }
```

**响应（成功）：** HTTP 200，code = 0
```json
{
  "code": 0,
  "data": {
    "token": "xxxx-xxxx",
    "role": "USER",
    "profile": { "id": 10001, "username": "zhangsan", "nickname": "张三", "avatar": "https://..." }
  }
}
```

**响应（错误）：** HTTP 200，code ≠ 0
| code | message | 触发条件 |
|---|---|---|
| 2001 | 用户名不存在 | 3 张表均未命中 username |
| 2002 | 密码错误 | 命中任一张表但 BCrypt 验证失败 |
| 1001 | 入参校验失败 | username/password 为空等 @Valid 拦截 |

### 4.2 profile 字段映射（按 role）

| role | profile 字段 | 来源 |
|---|---|---|
| `USER` | `id` / `username` / `nickname` / `avatar` / `phone` / `email` / `createTime` | 复用现有 `UserVO` 全字段 |
| `MERCHANT` | `id` / `username` / `shopName` / `contactName` / `contactPhone` / `logo` / `auditStatus` / `status` / `createTime` | 复用现有 `MerchantVO` 全字段 |
| `ADMIN` | `id` / `username` / `nickname` / `status` / `createTime` | 复用现有 `AdminVO` 全字段 |

> 复用现有 VO 全字段，profile 实际类型为 `Object`（Jackson 按实际 VO 类型出字段）。前端按 `role` 判断取哪些键，不应假定跨 role 通用。

### 4.3 老端点（deprecated）

```
POST /api/v1/merchant/auth/login   @Deprecated   行为不变
POST /api/v1/admin/auth/login      @Deprecated   行为不变
```

这两个端点的请求 / 响应 / token 全部保持现状，只在接口层加 `@Deprecated` 注解 + 接口文档标 deprecated，**不破坏任何已有调用**。

---

## 5. 架构

### 5.1 模块归属

| 文件 | 所在模块 | 原因 |
|---|---|---|
| `controller/UnifiedAuthController.java` | `freshfood-app` | 父模块；已经依赖 user/merchant/admin 三子模块，跨表最自然 |
| `service/UnifiedAuthService.java` | `freshfood-app` | 同上 |
| `service/UsernameUniquenessChecker.java` | `freshfood-app` | 跨表查 3 张 mapper，唯一放父模块 |
| `service/impl/UnifiedAuthServiceImpl.java` | `freshfood-app` | 同上 |
| ErrorCode 改动 | `freshfood-common` | 唯一改点：`ErrorCode.GLOBAL_USERNAME_EXISTS(1002)` |
| 三角色现有 doLogin 抽取 | user / merchant / admin 模块内 | 各自抽出 public `doLogin(username, password) → LoginResultVO` 方法供 UnifiedAuthService 调用；不改各 controller 既有路径行为 |
| 现有 AuthController（user）改造 | `freshfood-user` | **删除 `login` 端点**（路径让给 UnifiedAuthController）；保留 `register` / `logout` 端点不变 |

**关键决定：** `UnifiedAuthController` 是新文件，**不复用** `freshfood-user.AuthController`，因为：
- `AuthController` 当前类级别标 `@SaIgnore`，且继承自 user 模块的异常处理风格；
- 新 controller 路径虽然相同，但行为和跨表逻辑都不同，混进去会模糊边界；
- **实施方式：** `AuthController.login` **完全删除**，路径让给 `UnifiedAuthController`；前端改调新的统一入口；原文件 `register` / `logout` 端点不动。

### 5.2 Sa-Token 行为

`UnifiedAuthService.login(username, password)` 命中 user 表时：
```java
StpUtil.getStpLogic(TYPE_USER, null).login(userId);
String token = StpUtil.getStpLogic(TYPE_USER, null).getTokenValue();
```
对应 merchant / admin 同理，使用 `TYPE_MERCHANT` / `TYPE_ADMIN` 常量（这些常量已经在 `CommonConstants` 里，Plan 1 建立的）。

**token 命名空间不变**，后续请求按既有 Header `satoken` 通过各端点路径保护 `@SaCheckRole("ADMIN")` 等正常工作。

### 5.3 UnifiedAuthService 主流程

```
UnifiedAuthController.login(LoginDTO dto)
  ↓
UnifiedAuthService.login(username, password):
  1. userMapper.findByUsername(username) → UserDO
  2. if hit:
       BCrypt.matches(password, userDO.password)
       if fail → 抛 PASSWORD_WRONG(2002)
       UserAuthService.doLogin(userDO.id)
       return { token, role: "USER", profile: userVO }
  3. merchantMapper.findByUsername(username) → MerchantDO
  4. (同上分支)
  5. adminMapper.findByUsername(username) → AdminDO
  6. (同上分支)
  7. 三表都未命中 → 抛 USERNAME_NOT_FOUND(2001)
```

**doLogin 抽取：**

现有 `UserAuthService.login(LoginDTO) → LoginVO`：内部逻辑是 ① 查 user ② 验 BCrypt ③ `StpUser.login(id)` ④ 组 `LoginVO { token, user }`。

**新方案**——把步骤 ②③④ 抽成 public `UserAuthService.doLogin(UserDO userDO) → LoginVO` 供 UnifiedAuthService 复用。原 `login(LoginDTO)` 入口方法逻辑调整为：① 查 user → ② 验 BCrypt → ③ 调 `doLogin(userDO)`，仅作为内部 helper 保留（不再被 controller 调用，删除 `AuthController.login` 后成为 dead code 候选；为减少 diff 暂时保留，迭代可清理）。

merchant / admin 同理。

### 5.4 UsernameUniquenessChecker

```java
@Service
public class UsernameUniquenessChecker {
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;

    public void checkAvailable(String username) {
        if (userMapper.countByUsername(username) > 0
            || merchantMapper.countByUsername(username) > 0
            || adminMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.GLOBAL_USERNAME_EXISTS);
        }
    }
}
```

每个 mapper 上需要新增 `countByUsername(String) → long`，用 `selectCount(lambdaQuery().eq(UserDO::getUsername, username))` 一行 MyBatis-Plus 实现。删除（`deleted=1`）不计入冲突；禁用状态（`status=0`）当前**计入**冲突（与各端既有 `existsByUsername` 行为保持一致，避免同名账号横跨端同时存在）。

**接入点：**

| 位置 | 时机 | 备注 |
|---|---|---|
| `UserAuthService.register(RegisterDTO)` | 在 `existsByUsername` 通过后、insert user 前 | 现有唯一 |
| `MerchantAuthService.register` | **不存在，跳过** | 当前 `MerchantAuthController` 只有 login/logout，无 register |
| `AdminAccountService.create(...)` | 在 username 重名校验（已有 Plan 5 的 `ADMIN_USERNAME_EXISTS(9004)`）通过后、insert admin 前 | 现有唯一 |

> merchant register 暂不在范围内；如未来加 register 端点，需同步接入 UsernameUniquenessChecker。

### 5.5 错误处理

新增 1 条错误码（在 `freshfood-common` 的 `ErrorCode.java` 现有 9001 之前或之后追加，无需排序）：

```java
GLOBAL_USERNAME_EXISTS(1002, "用户名已被使用（跨账号表）"),
```

复用现有错误码：

| Code | 含义 | 触发 |
|---|---|---|
| `2001 USER_NOT_FOUND` | 用户名不存在 | unified login 三表都未命中 |
| `2002 PASSWORD_WRONG` | 密码错误 | 命中任一张表但 BCrypt 验证失败 |
| `1001 PARAM_INVALID` | 入参校验失败 | @Valid 自动拦截 |

---

## 6. 数据对象

### 6.1 DTO

复用现有 `LoginDTO`：`username` / `password` 两个字段。无新增 DTO。

### 6.2 VO（profile 字段）

**现有 VO 复用：**

| role | 复用 VO | 说明 |
|---|---|---|
| USER | `LoginVO.user`（已存在的内嵌对象） | 字段 id/username/nickname/avatar |
| MERCHANT | merchant 端的 login VO（如果存在）或新加 `MerchantProfileVO` | 字段 id/username/shopName/contactName/logo |
| ADMIN | admin 端的 login VO（如果存在）或新加 `AdminProfileVO` | 字段 id/username/nickname |

> 已确认：现有 `MerchantLoginVO` 内嵌 `MerchantVO`、`AdminLoginVO` 内嵌 `AdminVO`，两者字段集合已包含 §4.2 表所需的所有字段。无须新建 `MerchantProfileVO` / `AdminProfileVO`，直接复用 `MerchantVO` / `AdminVO`。

### 6.3 统一响应 VO

新增 `freshfood-app` 模块下的 `UnifiedLoginVO`：

```java
public class UnifiedLoginVO {
    private String token;
    private String role;        // "USER" / "MERCHANT" / "ADMIN"
    private Object profile;     // 按 role 决定类型：UserVO / MerchantProfileVO / AdminProfileVO
}
```

> `profile` 用 `Object`（Jackson 序列化时按实际类型出字段）。前端按 `role` 字段决定如何解析 `profile`。

### 6.4 CommonConstants

确认已存在 `TYPE_USER` / `TYPE_MERCHANT` / `TYPE_ADMIN` 三个常量（Plan 1 已有）。无新增。

---

## 7. 测试策略

### 7.1 单元测试

**`UnifiedAuthServiceImplTest`**（6 用例）
| # | 用例 | 期望 |
|---|---|---|
| 1 | user 命中 + 密码对 | 返回 role=USER，profile=UserVO，token 非空 |
| 2 | user 未命中，merchant 命中 + 密码对 | 返回 role=MERCHANT |
| 3 | user/merchant 都未命中，admin 命中 + 密码对 | 返回 role=ADMIN |
| 4 | 三表都未命中 | 抛 BusinessException code=2001 |
| 5 | user 命中 + 密码错 | 抛 BusinessException code=2002 |
| 6 | user + merchant 同名（user 命中） | 返回 role=USER（优先级生效，不走 merchant） |

Mock 模式：与 Plan 5 一致，`@ExtendWith(MockitoExtension.class)` + `@Mock` mappers + `@InjectMocks`。

**`UsernameUniquenessCheckerTest`**（4 用例）
| # | 用例 | 期望 |
|---|---|---|
| 1 | 3 张表 count 全部 0 | 不抛 |
| 2 | user count ≥ 1 | 抛 GLOBAL_USERNAME_EXISTS(1002) |
| 3 | user/merchant 全 0，admin count ≥ 1 | 抛 GLOBAL_USERNAME_EXISTS(1002) |
| 4 | 三表 count 全 ≥ 1 | 抛 GLOBAL_USERNAME_EXISTS(1002) |

**`UserAuthServiceImplTest`** 追加 1 用例（如果 doLogin 被抽出）
| # | 用例 | 期望 |
|---|---|---|
| 1 | doLogin(userDO) 成功 | 返回 token + LoginVO.user |

### 7.2 边界场景

- 用户在 user 表的 username 全局唯一但是 disabled（status=0）—— 当前 `AuthService.login` 应已有 disabled 检查，未做统一端点"恢复"语义，遇到 disabled 返回什么？Plan 6 范围内：**沿用各端既有行为**——user 端 `AuthService.login` 的 disabled 检查原样保留；merchant / admin 同理。
- user 表有，BCrypt 抛异常（如 hash 损坏）—— **不限范围**，按现有行为冒泡到全局异常处理。
- 三个表都没有 —— 抛 2001，符合契约。

### 7.3 不做的事

- ❌ 集成测试（端到端）/ 接口联调（前端环境不在本计划）
- ❌ 性能压测
- ❌ 并发注册同名压测（理论上漏判，但实施范围接受）

---

## 8. 实施任务拆解

预计 6-9 个 commit，小且独立：

1. **ErrorCode 加 1 条** (`GLOBAL_USERNAME_EXISTS(1002)`)
2. **3 个 mapper 各加一个 countByUsername**
3. **`UsernameUniquenessChecker` 服务 + 测试**
4. **3 个 role 模块的 AuthService 各抽 doLogin(UserDO/MerchantDO/AdminDO) public 方法**（不改行为）
5. **`UnifiedLoginVO` + `UnifiedAuthService` + 测试**
6. **`UnifiedAuthController` + 删除 `AuthController.login` 端点**
7. **2 个 role 模块接入 `UsernameUniquenessChecker`**：`UserAuthService.register` + `AdminAccountService.create`（merchant 端无 register 跳过）
8. **merchant / admin controller `login` 端点加 `@Deprecated`**
9. **接口文档 `接口文档.md` 更新**：§2.1 / §3.1 / §4.1 + §一 加注；统一登录响应示例替换

---

## 9. 范围外（明确不做）

- ❌ RBAC 多级权限 / 给 admin 表加 role 字段（Plan 5 已确认不加）
- ❌ 同一用户名绑定多角色（一个 zhangsan 同时是 user 和 merchant）
- ❌ 老 token 自动升级 / 重新派发
- ❌ 邮箱 / 手机号登录（只 username + password）
- ❌ 跨端互登（user token 拿 merchant 资源）
- ❌ 删除已有同名跨表数据（seed 数据已不冲突，无需迁移）
- ❌ 集成 / e2e 测试
- ❌ 修改 `接口文档.md` 之外的 README / 设计文档 / 需求文档

---

## 10. 与现有项目兼容性

**复用：**
- `freshfood-common.exception.BusinessException/ErrorCode`（追加 1 条枚举）
- `freshfood-common.response.R`
- `freshfood-common.constant.CommonConstants.{TYPE_USER, TYPE_MERCHANT, TYPE_ADMIN}`（已存在）
- `freshfood-model.entity.{UserDO, MerchantDO, AdminDO}`
- `freshfood-{user,merchant,admin}.mapper.{UserMapper, MerchantMapper, AdminMapper}`（追加 1 方法）
- `freshfood-{user,merchant,admin}.service.Auth*Service`（抽公共方法）
- Sa-Token 3 个 StpLogic（Plan 1 已配）

**改动文件清单（预计）：**
| 文件 | 操作 |
|---|---|
| `freshfood-common/.../exception/ErrorCode.java` | +1 条枚举 |
| `freshfood-user/.../mapper/UserMapper.java` | +1 方法 (`countByUsername`) |
| `freshfood-merchant/.../mapper/MerchantMapper.java` | +1 方法 (`countByUsername`) |
| `freshfood-admin/.../mapper/AdminMapper.java` | +1 方法 (`countByUsername`) |
| `freshfood-user/.../service/UserAuthService*.java` | 抽 public `doLogin(UserDO) → LoginVO` |
| `freshfood-user/.../service/impl/UserAuthServiceImpl.java` | `register` 接入 checker；`doLogin` 实现；既有 `login(LoginDTO)` 内调 doLogin（向后兼容） |
| `freshfood-merchant/.../service/MerchantAuthService*.java` | 抽 public `doLogin(MerchantDO) → MerchantLoginVO` |
| `freshfood-admin/.../service/AdminAuthService*.java` | 抽 public `doLogin(AdminDO) → AdminLoginVO` |
| `freshfood-admin/.../service/AdminAccountServiceImpl.java` | `create` 接入 checker |
| `freshfood-user/.../controller/AuthController.java` | **删除 `login` 端点**（路径让给 `UnifiedAuthController`） |
| `freshfood-merchant/.../controller/MerchantAuthController.java` | +`@Deprecated` on `login` 端点 |
| `freshfood-admin/.../controller/AdminAuthController.java` | +`@Deprecated` on `login` 端点 |
| `freshfood-app/.../controller/UnifiedAuthController.java` | 新建（占位 `/api/v1/auth/login`） |
| `freshfood-app/.../service/UnifiedAuthService*.java` | 新建（含 impl） |
| `freshfood-app/.../service/UsernameUniquenessChecker.java` | 新建 |
| `freshfood-app/.../vo/UnifiedLoginVO.java` | 新建 |
| `freshfood-user/src/test/.../UnifiedAuthServiceImplTest.java` | 新建（6 用例） |
| `freshfood-app/src/test/.../UsernameUniquenessCheckerTest.java` | 新建（4 用例） |
| `freshfood-user/src/test/.../UserAuthServiceImplTest.java` | 追加 1 用例（`doLogin` 路径） |
| `接口文档.md` | §一 + §2.1 / §3.1 / §4.1 改 |

**不修改：** 任何 SQL schema、Sa-Token 配置、StpLogic 配置。

---

## 11. 风险与缓解

| 风险 | 缓解 |
|---|---|
| username 跨表同名的应用层竞态漏判 | 课设级并发冲突概率极低，文档约定"避免同名"；真正生产级别需要 DB 兜底（中间表 / 触发器）不在本计划 |
| merchant / admin 老端点继续被外部调用 | 加 `@Deprecated` 是软标记，不强制拦截；老调用照旧工作 |
| Spring Bean 循环依赖：`UnifiedAuthService` 依赖 3 个 `AuthService` | `UnifiedAuthService` 只调 `doLogin(id)` 公共方法，不持有完整 `AuthService` 依赖时可避免；如必须依赖，用 `@Lazy` 或拆方法 |
| 现有 user `AuthController.login` 行为变化导致前端报错 | 前端是自己控制的，且明确要做统一登录；接口文档同步更新；前端调用方同步切换 `/api/v1/auth/login` 走新逻辑 |
| profile 类型用 `Object` 序列化结果不可读 | Jackson 默认按实际类型出字段；接口文档给 3 个 role 的 profile 字段示例 |
| 抽 doLogin 改动触发 user/merchant/admin 测试回归 | 抽出后行为不变，老测试应继续通过；测试阶段执行 `mvn test` |

---

## 12. 验收标准

完成时满足：

- [ ] `POST /api/v1/auth/login` 用 admin 的 username / password 能登录，返回 `role: "ADMIN"`
- [ ] `POST /api/v1/auth/login` 用 merchant 的 username / password 能登录，返回 `role: "MERCHANT"`
- [ ] `POST /api/v1/auth/login` 用 user 的 username / password 能登录，返回 `role: "USER"`
- [ ] `POST /api/v1/auth/login` 三表都不命中 → HTTP 200 + code=2001
- [ ] `POST /api/v1/auth/login` 密码错 → HTTP 200 + code=2002
- [ ] `POST /api/v1/auth/login` 同名横跨 user + merchant → 返回 `role: "USER"`（优先级）
- [ ] user register：username 已被 merchant 占 → HTTP 200 + code=1002
- [ ] admin create：username 已被 user 占 → HTTP 200 + code=1002
- [ ] `POST /api/v1/merchant/auth/login` 仍能登录（接口标 deprecated）
- [ ] `POST /api/v1/admin/auth/login` 仍能登录（接口标 deprecated）
- [ ] `接口文档.md` 已更新：§一 加注、§2.1 替换登录响应、§3.1 / §4.1 标 deprecated
- [ ] `mvn clean compile` 通过
- [ ] 单测全部通过（UnifiedAuthServiceImplTest 6 例 + UsernameUniquenessCheckerTest 4 例 + 既有回归测试 0 回归）
- [ ] 单一 commit 主题、信息密度高、中文 body

---

## 13. 文档同步清单

| 文件 | 操作 |
|---|---|
| `接口文档.md` | §一「通用约定」加统一登录说明；§2.1「认证模块」替换登录端点说明 + 响应示例；§3.1 / §4.1 在 login 端点旁标 deprecated |
| （不更新）`需求分析与技术选型.md` | 无变更 |
| （不更新）`线上生鲜商场购物平台开发功能需求文档.md` | 无变更 |
| （不更新）`freshfood-shop/README.md` | 无变更 |
