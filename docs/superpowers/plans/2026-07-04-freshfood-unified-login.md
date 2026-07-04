# Plan 6 — 统一登录 + 全局 username 唯一 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把现有 3 个独立登录端点（user/merchant/admin）合并为 1 个统一登录端点 `POST /api/v1/auth/login`，返回 `role + profile`，让前端按 role 路由 UI；同时强制 3 张账号表 `username` 全局唯一。

**Architecture:** 新增 `UnifiedAuthController`（`freshfood-app` 模块）+ `UnifiedAuthService` 跨 3 张表查 username（优先级 user → merchant → admin），命中后委派给该角色现有 `AuthService.doLogin(DO)` 写 Sa-Token + 组 profile。新增 `UsernameUniquenessChecker`（`freshfood-app` 模块）应用层跨表 guard，由 `UserAuthService.register` 和 `AdminAccountService.create` 接入。统一登录合并两种失败（用户名不存在 / 密码错误）为单一错误码 `LOGIN_FAILED(1005)`，避免泄露账号枚举。老 merchant / admin 登录端点保留 + `@Deprecated`。

**Tech Stack:** Spring Boot 3.5.16 · Java 17 · MyBatis-Plus 3.5.9 · Sa-Token 1.37.0（`StpLogicJwtForSimple`，3 个 bean：stpUserLogic `@Primary` / stpMerchantLogic / stpAdminLogic）· Lombok · jakarta.validation · JUnit 5 + Mockito + Mockito MockedStatic

**与现有代码的差异（自检后修正）：**
- 设计稿原文用 `PASSWORD_WRONG(2002)` / `USERNAME_NOT_FOUND(2001)`；实际现有 `ErrorCode` 是 `PASSWORD_ERROR(2002)` / `USER_NOT_FOUND(2001)`；本计划以**实际枚举名**为准。
- 统一登录失败合并为 `LOGIN_FAILED(1005)`（用户名不存在 / 密码错误共用一条），不泄露账号是否存在于某个端。

---

## File Structure

**Modify (12 files):**
- `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java` — 追加 2 条：`LOGIN_FAILED(1005)` 和 `GLOBAL_USERNAME_EXISTS(1002)`
- `freshfood-common/pom.xml` — 无变更
- `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/UserMapper.java` — +1 default 方法 `countByUsername(String)`
- `freshfood-user/src/main/java/com/yan/freshfood/user/service/AuthService.java` — 接口 +1 `doLogin(UserDO)`
- `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AuthServiceImpl.java` — 实现 `doLogin`；`register` 接入 UsernameUniquenessChecker
- `freshfood-user/src/main/java/com/yan/freshfood/user/controller/AuthController.java` — **删除 `login` 端点**
- `freshfood-merchant/src/main/java/com/yan\freshfood\merchant\mapper\MerchantMapper.java` — +1 方法 `countByUsername(String)`
- `freshfood-merchant/src/main/java/com/yan\freshfood\merchant\service\MerchantAuthService.java` — 接口 +1 `doLogin(MerchantDO)`
- `freshfood-merchant/src/main/java/com/yan\freshfood\merchant\service\impl\MerchantAuthServiceImpl.java` — 实现 `doLogin`
- `freshfood-merchant/src/main/java/com/yan\freshfood\merchant\controller\MerchantAuthController.java` — `login` 端点加 `@Deprecated`
- `freshfood-admin/src/main/java/com/yan\freshfood\admin\mapper\AdminMapper.java` — +1 方法 `countByUsername(String)`
- `freshfood-admin/src/main/java/com/yan\freshfood\admin\service\AdminAuthService.java` — 接口 +1 `doLogin(AdminDO)`
- `freshfood-admin/src/main/java/com/yan\freshfood\admin\service\impl\AdminAuthServiceImpl.java` — 实现 `doLogin`
- `freshfood-admin/src/main/java/com/yan\freshfood\admin\service\impl\AdminAccountServiceImpl.java` — `create` 接入 UsernameUniquenessChecker（替换原 local `selectCount` 检查）
- `freshfood-admin/src/main/java/com/yan\freshfood\admin\controller\AdminAuthController.java` — `login` 端点加 `@Deprecated`
- `接口文档.md` — §一 / §2.1 / §3.1 / §4.1 文字更新

**Create (5 new files in `freshfood-app`):**
- `controller/UnifiedAuthController.java` — 1 个端点 `POST /auth/login`
- `service/UnifiedAuthService.java` — 接口
- `service/impl/UnifiedAuthServiceImpl.java` — 跨表调度
- `service/UsernameUniquenessChecker.java` — 跨表 username 唯一性
- `vo/UnifiedLoginVO.java` — `token` / `role` / `profile(Object)`

**Create (2 new tests):**
- `freshfood-app/src/test/java/.../UnifiedAuthServiceImplTest.java` — 6 用例
- `freshfood-app/src/test/java/.../UsernameUniquenessCheckerTest.java` — 4 用例

**Branching:** 新建 `feature/unified-login` 分支（基于 main fast-forward 合入）。

---

## Tasks

### Task 1: 建 feature 分支 + 验证基础

**Files:** none (branch only)

- [ ] **Step 1: 确认 main 已更新**

```bash
cd "C:\Users\yan\Desktop\线上生鲜商场购物平台开发-后端\freshfood-shop"
git status
git checkout main
git pull origin main
```

Expected: 当前目录为 `freshfood-shop`；main 分支最新。`git status` 应无未提交改动（除非有未推送）。

- [ ] **Step 2: 建并切到 feature 分支**

```bash
git checkout -b feature/unified-login
```

Expected: 切到新分支 `feature/unified-login`。

- [ ] **Step 3: 验证三个 mapper 现状**

```bash
cat freshfood-user/src/main/java/com/yan/freshfood/user/mapper/UserMapper.java
cat freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/MerchantMapper.java
cat freshfood-admin/src/main/java/com/yan/freshfood/admin/mapper/AdminMapper.java
```

Expected: 三者均为空接口 `extends BaseMapper<XxxDO>`。

- [ ] **Step 4: 验证 CommonConstants 与 ErrorCode 路径**

```bash
grep -n "TYPE_USER\|TYPE_MERCHANT\|TYPE_ADMIN" freshfood-common/src/main/java/com/yan/freshfood/common/constant/CommonConstants.java
grep -n "PASSWORD_ERROR\|USER_NOT_FOUND" freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java
```

Expected: 前者输出 3 行 `TYPE_USER` / `TYPE_MERCHANT` / `TYPE_ADMIN`；后者输出 `PASSWORD_ERROR(2002)` 和 `USER_NOT_FOUND(2001)`（确认了实际枚举名）。

- [ ] **Step 5: 验证 SaTokenConfig 已有 3 个 bean**

```bash
grep -n "StpLogic\|@Bean" freshfood-framework/src/main/java/com/yan/freshfood/framework/config/SaTokenConfig.java
```

Expected: 文件含 `stpUserLogic`（带 `@Primary`）、`stpMerchantLogic`、`stpAdminLogic` 共 3 个 bean。

若任何一步不符，**STOP 并报告 blocker**。本任务不需 commit。

---

### Task 2: 加 2 条 ErrorCode 枚举

**Files:**
- Modify: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`

- [ ] **Step 1: 在 `NOT_FOUND(1004, ...)` 之后追加 2 条**

打开 `ErrorCode.java`，在 `NOT_FOUND(1004, "资源不存在"),` 之后、`SYSTEM_ERROR(8001, "系统异常"),` 之前插入：

```java
    GLOBAL_USERNAME_EXISTS(1002, "用户名已被使用（跨账号表）"),
    LOGIN_FAILED(1005, "用户名或密码错误"),
```

完整文件结构应如下（`NOT_FOUND` 之后新增两行，其他不变）：

```java
    PARAM_INVALID(1001, "参数校验失败"),
    GLOBAL_USERNAME_EXISTS(1002, "用户名已被使用（跨账号表）"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(1004, "资源不存在"),
    LOGIN_FAILED(1005, "用户名或密码错误"),
    SYSTEM_ERROR(8001, "系统异常"),
```

注：`1002` 和 `1005` 是新占位，原枚举段没有这俩 code。`UNAUTHORIZED`/`FORBIDDEN` 段顺序保留即可。

- [ ] **Step 2: 验证编译**

```bash
cd freshfood-common
mvn compile -q
```

Expected: 编译通过无错误。

- [ ] **Step 3: Commit**

```bash
cd ..
git add freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java
git commit -m "feat(auth): 加 LOGIN_FAILED 与 GLOBAL_USERNAME_EXISTS 错误码"
```

---

### Task 3: 三个 mapper 各加 `countByUsername`（含测试）

**Files:**
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/UserMapper.java`
- Modify: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/MerchantMapper.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/mapper/AdminMapper.java`
- Create: `freshfood-user/src/test/java/com/yan/freshfood/user/mapper/UserMapperTest.java`（如果当前没 Mapper 测试目录，可跳过；后续 UsernameUniquenessCheckerTest 会覆盖）
- Create: `freshfood-merchant/src/test/java/com/yan/freshfood/merchant/mapper/MerchantMapperTest.java`
- Create: `freshfood-admin/src/test/java/com/yan/freshfood/admin/mapper/AdminMapperTest.java`

注：mapper 默认方法本身用 MyBatis-Plus 链式查询不易单元测（需要 SQL）。本任务**不写 mapper 单测**，由 Task 4 的 `UsernameUniquenessCheckerTest` 通过 mock mapper 间接覆盖 mapper 调用（mock 只关心传入参数是否正确调用 `countByUsername(...)`）。

- [ ] **Step 1: 改 `UserMapper.java`**

完整文件内容：

```java
package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getUsername, username)
        );
    }
}
```

- [ ] **Step 2: 改 `MerchantMapper.java`**

```java
package com.yan.freshfood.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<MerchantDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantDO>()
                        .eq(MerchantDO::getUsername, username)
        );
    }
}
```

- [ ] **Step 3: 改 `AdminMapper.java`**

```java
package com.yan.freshfood.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.AdminDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<AdminDO> {

    default long countByUsername(String username) {
        return selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminDO>()
                        .eq(AdminDO::getUsername, username)
        );
    }
}
```

- [ ] **Step 4: 编译三个模块验证**

```bash
cd freshfood-user && mvn compile -q && cd ..
cd freshfood-merchant && mvn compile -q && cd ..
cd freshfood-admin && mvn compile -q && cd ..
```

Expected: 3 个模块均编译通过。

- [ ] **Step 5: Commit**

```bash
git add freshfood-user/src/main/java/com/yan/freshfood/user/mapper/UserMapper.java
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/MerchantMapper.java
git add freshfood-admin/src/main/java/com/yan/freshfood/admin/mapper/AdminMapper.java
git commit -m "feat(auth): 三张账号表 mapper 加 countByUsername 默认方法"
```

---

### Task 4: 新建 `UsernameUniquenessChecker` + 测试（TDD）

**Files:**
- Create: `freshfood-app/src/main/java/com/yan/freshfood/app/service/UsernameUniquenessChecker.java`
- Create: `freshfood-app/src/test/java/com/yan/freshfood/app/service/UsernameUniquenessCheckerTest.java`

注：`freshfood-app` 目前只有 `FreshfoodShopApplication.java` 一个文件，需要建子包。`@ComponentScan` 已包含 `com.yan.freshfood`，所以新类自动被扫描。

- [ ] **Step 1: 写失败测试（先写）**

新建 `UsernameUniquenessCheckerTest.java`：

```java
package com.yan.freshfood.app.service;

import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameUniquenessCheckerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private UsernameUniquenessChecker checker;

    @Test
    void checkAvailable_allZero_passes() {
        when(userMapper.countByUsername(eq("xxx"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("xxx"))).thenReturn(0L);
        when(adminMapper.countByUsername(eq("xxx"))).thenReturn(0L);

        assertDoesNotThrow(() -> checker.checkAvailable("xxx"));
    }

    @Test
    void checkAvailable_userHas1_throws1002() {
        when(userMapper.countByUsername(eq("u"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("u"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void checkAvailable_merchantHas1_throws1002() {
        when(userMapper.countByUsername(eq("m"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("m"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("m"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void checkAvailable_adminHas1_throws1002() {
        when(userMapper.countByUsername(eq("a"))).thenReturn(0L);
        when(merchantMapper.countByUsername(eq("a"))).thenReturn(0L);
        when(adminMapper.countByUsername(eq("a"))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> checker.checkAvailable("a"));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }
}
```

加 import：

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
```

（追加到 `import static ...` 段之前已有 `assertThrows` 之上）

- [ ] **Step 2: 跑测试，应该失败**

```bash
cd freshfood-app
mvn test -Dtest=UsernameUniquenessCheckerTest
```

Expected: 编译失败（`UsernameUniquenessChecker` 类不存在）+ 4 个测试找不到类。

- [ ] **Step 3: 写实现**

`freshfood-app/src/main/java/com/yan/freshfood/app/service/UsernameUniquenessChecker.java`：

```java
package com.yan.freshfood.app.service;

import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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

- [ ] **Step 4: 重跑测试，应该通过**

```bash
cd freshfood-app
mvn test -Dtest=UsernameUniquenessCheckerTest
```

Expected: 4 tests passed。

- [ ] **Step 5: Commit**

```bash
cd ..
git add freshfood-app/src/main/java/com/yan/freshfood/app/service/UsernameUniquenessChecker.java
git add freshfood-app/src/test/java/com/yan/freshfood/app/service/UsernameUniquenessCheckerTest.java
git commit -m "feat(auth): 新增 UsernameUniquenessChecker 跨表守卫"
```

---

### Task 5: 抽出 3 个 `AuthService.doLogin(DO)` 公共方法（TDD，三模块并行）

**Files:**
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/service/AuthService.java`
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AuthServiceImpl.java`
- Modify: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantAuthService.java`
- Modify: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantAuthServiceImpl.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/AdminAuthService.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAuthServiceImpl.java`

本任务只**抽出 doLogin 公共方法**，不改既有 login(DTO) 行为；既有测试不回归即可。doLogin 假定入参 DO 非 null（由调用方先查表）。

- [ ] **Step 1: 改 `AuthService.java`（user）接口**

完整文件：

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    LoginVO doLogin(UserDO user);

    LoginVO register(RegisterDTO dto);

    void logout();
}
```

- [ ] **Step 2: 改 `AuthServiceImpl.java`**

把现有的 `login(LoginDTO)` 重构：把 BCrypt 校验 + StpUtil.login + 组 LoginVO 抽到 `doLogin(UserDO)`；`login(LoginDTO)` 改为：查 user → 调 doLogin。

完整文件：

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        UserDO user = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return doLogin(user);
    }

    @Override
    public LoginVO doLogin(UserDO user) {
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(user.getPassword(), extractRawPassword(user))) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    private boolean passwordMatches(UserDO user, String raw) {
        return BCrypt.checkpw(raw, user.getPassword());
    }

    private String extractRawPassword(UserDO user) {
        return user.getPassword();
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private UserVO toVO(UserDO user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
```

⚠️ **注意**：上面的 `doLogin(UserDO)` 写法有问题——`UserDO.getPassword()` 是 hash 后的密码，不是明文，没法 `BCrypt.checkpw(hash, raw)`。**正确写法**：`doLogin(UserDO, String rawPassword)` 或 `doLogin(UserDO, LoginDTO)`，第二个参数携带明文密码。

**修正方案**：把抽出的 doLogin 签名改为 `doLogin(UserDO user, String rawPassword)`，由调用方传入明文密码。`login(LoginDTO)` 改为先查 user → 调 `doLogin(user, dto.getPassword())`。`UnifiedAuthService` 调用时也传 rawPassword。

完整文件（**最终正确版本**）：

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        UserDO user = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return doLogin(user, dto.getPassword());
    }

    @Override
    public LoginVO doLogin(UserDO user, String rawPassword) {
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private UserVO toVO(UserDO user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
```

接口改为：

```java
public interface AuthService {
    LoginVO login(LoginDTO dto);
    LoginVO doLogin(UserDO user, String rawPassword);
    LoginVO register(RegisterDTO dto);
    void logout();
}
```

- [ ] **Step 3: 改 `MerchantAuthService.java` 接口**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.model.entity.MerchantDO;

public interface MerchantAuthService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    MerchantLoginVO doLogin(MerchantDO merchant, String rawPassword);

    void logout();
}
```

- [ ] **Step 4: 改 `MerchantAuthServiceImpl.java`**

完整文件：

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private final MerchantMapper merchantMapper;

    @Override
    public MerchantLoginVO login(MerchantLoginDTO dto) {
        MerchantDO m = merchantMapper.selectOne(
                new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getUsername, dto.getUsername())
        );
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return doLogin(m, dto.getPassword());
    }

    @Override
    public MerchantLoginVO doLogin(MerchantDO m, String rawPassword) {
        if (m.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (m.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_PENDING);
        }
        if (!BCrypt.checkpw(rawPassword, m.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT);
        logic.login(m.getId());
        return new MerchantLoginVO(logic.getTokenValue(), toVO(m));
    }

    @Override
    public void logout() {
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_MERCHANT);
        logic.logout();
    }

    private MerchantVO toVO(MerchantDO m) {
        MerchantVO vo = new MerchantVO();
        vo.setId(m.getId());
        vo.setUsername(m.getUsername());
        vo.setShopName(m.getShopName());
        vo.setContactName(m.getContactName());
        vo.setContactPhone(m.getContactPhone());
        vo.setLogo(m.getLogo());
        vo.setAuditStatus(m.getAuditStatus());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 5: 改 `AdminAuthService.java` 接口**

```java
package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.model.entity.AdminDO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginDTO dto);

    AdminLoginVO doLogin(AdminDO admin, String rawPassword);

    void logout();
}
```

- [ ] **Step 6: 改 `AdminAuthServiceImpl.java`**

完整文件：

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.admin.vo.AdminVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.AdminDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminDO a = adminMapper.selectOne(
                new LambdaQueryWrapper<AdminDO>().eq(AdminDO::getUsername, dto.getUsername())
        );
        if (a == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        return doLogin(a, dto.getPassword());
    }

    @Override
    public AdminLoginVO doLogin(AdminDO a, String rawPassword) {
        if (a.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(rawPassword, a.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_ADMIN);
        logic.login(a.getId());
        return new AdminLoginVO(logic.getTokenValue(), toVO(a));
    }

    @Override
    public void logout() {
        StpLogic logic = SaManager.getStpLogic(CommonConstants.TYPE_ADMIN);
        logic.logout();
    }

    private AdminVO toVO(AdminDO a) {
        AdminVO vo = new AdminVO();
        vo.setId(a.getId());
        vo.setUsername(a.getUsername());
        vo.setNickname(a.getNickname());
        vo.setStatus(a.getStatus());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 7: 编译 3 个模块，确认现有测试不回归**

```bash
cd freshfood-user && mvn test -q && cd ..
cd freshfood-merchant && mvn test -q && cd ..
cd freshfood-admin && mvn test -q && cd ..
```

Expected: 三模块都通过；老的 `AuthServiceImpl` / `MerchantAuthServiceImpl` / `AdminAuthServiceImpl` 测试若存在应不回归。

- [ ] **Step 8: Commit**

```bash
git add freshfood-user/src/main/java/com/yan/freshfood/user/service/AuthService.java
git add freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AuthServiceImpl.java
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantAuthService.java
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantAuthServiceImpl.java
git add freshfood-admin/src/main/java/com/yan/freshfood/admin/service/AdminAuthService.java
git add freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAuthServiceImpl.java
git commit -m "refactor(auth): 抽出 doLogin(DO, raw) 公共方法供 unified 复用"
```

---

### Task 6: `UserAuthService.register` 接入 UsernameUniquenessChecker

**Files:**
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AuthServiceImpl.java`

注：`freshfood-user` 模块目前依赖关系是 `freshfood-common` + `freshfood-model` + `freshfood-framework`。要让 `freshfood-user` 注入 `UsernameUniquenessChecker`（位于 `freshfood-app`）会形成反向依赖（child 依赖 parent）。**不允许**这样改。

**改方案：** 在 `AuthServiceImpl.register` 里**直接调 3 张 mapper 的 countByUsername**，不通过 `UsernameUniquenessChecker` bean（应用层守护逻辑内联，本计划范围内动作同一文件即可）。重复代码 3-4 行，可接受（YAGNI）。

- [ ] **Step 1: 改 `AuthServiceImpl.register`**

把现有 `register` 方法改写如下（**仅 `register` 方法体**，其余方法保持 Task 5 末态）：

```java
    @Override
    public LoginVO register(RegisterDTO dto) {
        if (userMapper.countByUsername(dto.getUsername()) > 0
                || merchantMapper.countByUsername(dto.getUsername()) > 0
                || adminMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.GLOBAL_USERNAME_EXISTS);
        }
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }
```

加 2 个 import：

```java
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
```

加 2 个字段（在 `private final UserMapper userMapper;` 后）：

```java
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;
```

需在 `freshfood-user/pom.xml` 加 2 个依赖（当前没有 admin / merchant 依赖）：

```xml
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-merchant</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-admin</artifactId>
        </dependency>
```

⚠️ **依赖问题审视**：`freshfood-user` 当前没有依赖 `freshfood-merchant` / `freshfood-admin`。如果引入依赖，会让 user 模块反向依赖 admin / merchant，可能违反现有依赖方向。**先检查父 pom**：打开 `pom.xml`（父），看是否有显式 `dependencyManagement` 约束。若没有，加依赖合法。

**实施前 STOP**：先 `cat pom.xml` 确认依赖方向。如果父 pom 已声明 `dependencyManagement`，且禁止 user → admin/merchant 的依赖，则**改方案 B**：把 username 唯一性检查内联在 `register` 里只用 `userMapper`，跨表检查由 admin / merchant 的未来 register 端点各自保证（应用层守护责任分散）。本任务不引入反向依赖。

- [ ] **Step 2: 如果允许引入依赖 → 编译验证**

```bash
cd freshfood-user
mvn compile -q
```

Expected: 编译通过。

- [ ] **Step 3: 跑现有 AuthService 测试（如有）确认不回归**

```bash
cd freshfood-user
mvn test -Dtest=AuthServiceImplTest
```

若文件名不同：

```bash
ls freshfood-user/src/test/java/com/yan/freshfood/user/service/impl/
```

Expected: 不回归。

- [ ] **Step 4: Commit**

```bash
cd ..
git add freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AuthServiceImpl.java
git add freshfood-user/pom.xml
git commit -m "feat(auth): UserAuthService.register 接入跨表 username 唯一检查"
```

注：若走方案 B，则只 commit `AuthServiceImpl.java`（只内联 userMapper.checkAvailable），不带 pom 改动。

---

### Task 7: `AdminAccountService.create` 接入 UsernameUniquenessChecker

**Files:**
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAccountServiceImpl.java`

- [ ] **Step 1: 看现有 `create` 方法**

```bash
grep -n "create\|countByUsername\|ADMIN_USERNAME_EXISTS" freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAccountServiceImpl.java
```

确认现有 `create` 有一个本地 `adminMapper.selectCount(...)` 抛 `ADMIN_USERNAME_EXISTS(9004)` 的分支。

- [ ] **Step 2: 加 merchantMapper 字段 + 替换 username 检查**

⚠️ **依赖问题**：`freshfood-admin` 依赖 `freshfood-user` 还是 `freshfood-merchant`？先 `cat freshfood-admin/pom.xml`。若两个都没依赖，引入 `freshfood-merchant` 是新依赖；引入 `freshfood-user` 也是新依赖。

**实施前 STOP**：先 `cat pom.xml`（admin）确认依赖现状。

- [ ] **Step 3: 替换 create 方法里的 username 检查**

把现有 `create` 方法里抛 `ADMIN_USERNAME_EXISTS(9004)` 的 `selectCount` 分支替换为跨表检查：

```java
        if (userMapper.countByUsername(dto.getUsername()) > 0
                || merchantMapper.countByUsername(dto.getUsername()) > 0
                || adminMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.GLOBAL_USERNAME_EXISTS);
        }
```

加 import：

```java
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
```

加字段：

```java
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
```

- [ ] **Step 4: 更新对应测试**

打开 `freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/AdminAccountServiceImplTest.java`，在类内加 2 个 `@Mock` 字段：

```java
    @Mock
    private UserMapper userMapper;

    @Mock
    private MerchantMapper merchantMapper;
```

加 import：

```java
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.user.mapper.UserMapper;
```

把现有的 `create_duplicateUsername_throws9004` 测试**替换**为：

```java
    @Test
    void create_duplicateUsernameInAdminTable_throws1002() {
        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("password123");

        when(userMapper.countByUsername("admin")).thenReturn(0L);
        when(merchantMapper.countByUsername("admin")).thenReturn(0L);
        when(adminMapper.countByUsername("admin")).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateUsernameInUserTable_throws1002() {
        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("shared");
        dto.setPassword("password123");

        when(userMapper.countByUsername("shared")).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void create_duplicateUsernameInMerchantTable_throws1002() {
        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("shared");
        dto.setPassword("password123");

        when(userMapper.countByUsername("shared")).thenReturn(0L);
        when(merchantMapper.countByUsername("shared")).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));
        assertEquals(ErrorCode.GLOBAL_USERNAME_EXISTS.getCode(), ex.getCode());
    }
```

若 `create_success_inserts_with_bcrypt_and_status1` 测试有 `when(adminMapper.selectCount(...)).thenReturn(0L)` 的 stub（这是 Plan 5 admin 用的本地检查 stub），现在不再需要——保留它**不影响**新增 1002 测试（Mockito strict stubbing 在 @MockitoExtension 默认是 lenient，可不删）。如遇到 "Unnecessary stubbing" 报错，删除原 9004 测试的 `selectCount` stub。

为简洁起见：直接修改原 `create_duplicateUsername_throws9004` 测试名+断言为 `create_duplicateUsernameInAdminTable_throws1002`，再追加 2 条覆盖 user / merchant 命中场景。

- [ ] **Step 5: 编译并跑测试**

```bash
cd freshfood-admin
mvn test -q
```

Expected: 所有 admin 测试通过。

- [ ] **Step 6: Commit**

```bash
cd ..
git add freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAccountServiceImpl.java
git add freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/AdminAccountServiceImplTest.java
git commit -m "feat(auth): AdminAccountService.create 接入跨表 username 唯一检查（替换原 9004 检查）"
```

注：Task 6 + Task 7 两条依赖 import 风险高，实施时如遇 pom 报错，转方案 B：username 唯一检查只在当前模块的 mapper 内进行，跨表检查由 Task 4 的 `UsernameUniquenessChecker`（位于 `freshfood-app`）留作 forward-compatible 占位；admin create / user register 仅检查自己表 + 文档约定推荐前端做唯一性。

---

### Task 8: 新建 `UnifiedLoginVO`

**Files:**
- Create: `freshfood-app/src/main/java/com/yan/freshfood/app/vo/UnifiedLoginVO.java`

- [ ] **Step 1: 写文件**

```java
package com.yan.freshfood.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一登录响应")
public class UnifiedLoginVO {

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "账号角色", allowableValues = {"USER", "MERCHANT", "ADMIN"})
    private String role;

    @Schema(description = "账号基本信息；字段集合按 role 变化：USER 含 nickname/avatar，MERCHANT 含 shopName/contactName/logo，ADMIN 含 nickname")
    private Object profile;
}
```

- [ ] **Step 2: 编译**

```bash
cd freshfood-app
mvn compile -q
```

Expected: 通过。

- [ ] **Step 3: Commit**

```bash
cd ..
git add freshfood-app/src/main/java/com/yan/freshfood/app/vo/UnifiedLoginVO.java
git commit -m "feat(auth): 加 UnifiedLoginVO"
```

---

### Task 9: 新建 `UnifiedAuthService` 跨表调度 + 测试（TDD）

**Files:**
- Create: `freshfood-app/src/main/java/com/yan/freshfood/app/service/UnifiedAuthService.java`
- Create: `freshfood-app/src/main/java/com/yan/freshfood/app/service/impl/UnifiedAuthServiceImpl.java`
- Create: `freshfood-app/src/test/java/com/yan/freshfood/app/service/impl/UnifiedAuthServiceImplTest.java`

- [ ] **Step 1: 写失败测试（先写）**

`UnifiedAuthServiceImplTest.java`：

```java
package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.admin.vo.AdminVO;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.AdminDO;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import com.yan.freshfood.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedAuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private AdminMapper adminMapper;
    @Mock private AuthService userAuthService;
    @Mock private MerchantAuthService merchantAuthService;
    @Mock private AdminAuthService adminAuthService;

    @InjectMocks private UnifiedAuthServiceImpl service;

    private UserDO userDO(Long id, String username) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUsername(username);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        return u;
    }

    private MerchantDO merchantDO(Long id, String username) {
        MerchantDO m = new MerchantDO();
        m.setId(id);
        m.setUsername(username);
        m.setStatus(1);
        m.setAuditStatus(1);
        m.setShopName("shop");
        m.setCreateTime(LocalDateTime.now());
        return m;
    }

    private AdminDO adminDO(Long id, String username) {
        AdminDO a = new AdminDO();
        a.setId(id);
        a.setUsername(username);
        a.setStatus(1);
        a.setCreateTime(LocalDateTime.now());
        return a;
    }

    @Test
    void login_userHits_returnsRoleUSER() {
        UserDO u = userDO(1L, "alice");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);

        UserVO uvo = new UserVO();
        uvo.setId(1L);
        uvo.setUsername("alice");
        uvo.setNickname("Alice");
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-user", uvo));

        UnifiedLoginVO vo = service.login("alice", "pwd");

        assertEquals("tok-user", vo.getToken());
        assertEquals("USER", vo.getRole());
        assertNotNull(vo.getProfile());
        verify(userAuthService).doLogin(eq(u), eq("pwd"));
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userMiss_merchantHits_returnsRoleMERCHANT() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        MerchantDO m = merchantDO(2L, "bob");
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(m);

        MerchantVO mvo = new MerchantVO();
        mvo.setId(2L);
        mvo.setUsername("bob");
        when(merchantAuthService.doLogin(eq(m), eq("pwd"))).thenReturn(new MerchantLoginVO("tok-m", mvo));

        UnifiedLoginVO vo = service.login("bob", "pwd");

        assertEquals("MERCHANT", vo.getRole());
        assertEquals("tok-m", vo.getToken());
    }

    @Test
    void login_userAndMerchantMiss_adminHits_returnsRoleADMIN() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        AdminDO a = adminDO(3L, "admin1");
        when(adminMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(a);

        AdminVO avo = new AdminVO();
        avo.setId(3L);
        avo.setUsername("admin1");
        when(adminAuthService.doLogin(eq(a), eq("pwd"))).thenReturn(new AdminLoginVO("tok-a", avo));

        UnifiedLoginVO vo = service.login("admin1", "pwd");

        assertEquals("ADMIN", vo.getRole());
        assertEquals("tok-a", vo.getToken());
    }

    @Test
    void login_allMiss_throws1005() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(adminMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("ghost", "pwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
        verify(userAuthService, never()).doLogin(any(), any());
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userHitsBadPassword_throws1005() {
        UserDO u = userDO(1L, "alice");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(userAuthService.doLogin(eq(u), eq("badpwd"))).thenThrow(
                new BusinessException(ErrorCode.PASSWORD_ERROR));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("alice", "badpwd"));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), ex.getCode());
        verify(merchantAuthService, never()).doLogin(any(), any());
        verify(adminAuthService, never()).doLogin(any(), any());
    }

    @Test
    void login_userAndMerchantBothHaveSameUsername_userWins() {
        UserDO u = userDO(1L, "dupe");
        MerchantDO m = merchantDO(2L, "dupe");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(m);

        UserVO uvo = new UserVO();
        uvo.setId(1L);
        uvo.setUsername("dupe");
        when(userAuthService.doLogin(eq(u), eq("pwd"))).thenReturn(new LoginVO("tok-u", uvo));

        UnifiedLoginVO vo = service.login("dupe", "pwd");

        assertEquals("USER", vo.getRole());
        verify(merchantAuthService, never()).doLogin(any(), any());
    }
}
```

- [ ] **Step 2: 跑测试，应该编译失败**

```bash
cd freshfood-app
mvn test -Dtest=UnifiedAuthServiceImplTest
```

Expected: `UnifiedAuthService` / `UnifiedAuthServiceImpl` 类不存在。

- [ ] **Step 3: 写接口 `UnifiedAuthService.java`**

```java
package com.yan.freshfood.app.service;

import com.yan.freshfood.app.vo.UnifiedLoginVO;

public interface UnifiedAuthService {

    UnifiedLoginVO login(String username, String rawPassword);
}
```

- [ ] **Step 4: 写实现 `UnifiedAuthServiceImpl.java`**

```java
package com.yan.freshfood.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.mapper.AdminMapper;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.model.entity.AdminDO;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnifiedAuthServiceImpl implements UnifiedAuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;
    private final AuthService userAuthService;
    private final MerchantAuthService merchantAuthService;
    private final AdminAuthService adminAuthService;

    @Override
    public UnifiedLoginVO login(String username, String rawPassword) {
        try {
            UserDO user = userMapper.selectOne(
                    new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
            if (user != null) {
                LoginVO vo = userAuthService.doLogin(user, rawPassword);
                return new UnifiedLoginVO(vo.getToken(), "USER", vo.getUser());
            }

            MerchantDO merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getUsername, username));
            if (merchant != null) {
                var vo = merchantAuthService.doLogin(merchant, rawPassword);
                return new UnifiedLoginVO(vo.getToken(), "MERCHANT", vo.getMerchant());
            }

            AdminDO admin = adminMapper.selectOne(
                    new LambdaQueryWrapper<AdminDO>().eq(AdminDO::getUsername, username));
            if (admin != null) {
                AdminLoginVO vo = adminAuthService.doLogin(admin, rawPassword);
                return new UnifiedLoginVO(vo.getToken(), "ADMIN", vo.getAdmin());
            }
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        throw new BusinessException(ErrorCode.LOGIN_FAILED);
    }
}
```

⚠️ **catch 块设计说明**：统一登录端点把 **所有登录失败**（用户名不存在 / 密码错 / 账号禁用 / 待审核）合并为 `LOGIN_FAILED(1005)`，不泄露账号是否存在于某个端、是否被禁用、是否待审核。这是登录端点安全最佳实践。`doLogin` 内部抛何种 `BusinessException`（`USER_DISABLED` / `MERCHANT_PENDING` / `PASSWORD_ERROR`）都会被外层统一替换为 `LOGIN_FAILED`。

- [ ] **Step 5: 重跑测试，应该通过**

```bash
cd freshfood-app
mvn test -Dtest=UnifiedAuthServiceImplTest
```

Expected: 6 tests passed.

- [ ] **Step 6: Commit**

```bash
cd ..
git add freshfood-app/src/main/java/com/yan/freshfood/app/service/UnifiedAuthService.java
git add freshfood-app/src/main/java/com/yan/freshfood/app/service/impl/UnifiedAuthServiceImpl.java
git add freshfood-app/src/test/java/com/yan/freshfood/app/service/impl/UnifiedAuthServiceImplTest.java
git commit -m "feat(auth): UnifiedAuthService 跨表调度 + 6 用例测试"
```

---

### Task 10: 新建 `UnifiedAuthController`

**Files:**
- Create: `freshfood-app/src/main/java/com/yan/freshfood/app/controller/UnifiedAuthController.java`

- [ ] **Step 1: 写文件**

```java
package com.yan.freshfood.app.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yan.freshfood.user.dto.LoginDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "统一登录", description = "用户/商家/管理员统一登录入口")
@SaIgnore
@RestController
@RequiredArgsConstructor
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "统一登录", description = "按 user → merchant → admin 顺序匹配；返回 token、role、profile。无需登录鉴权。")
    public R<UnifiedLoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(unifiedAuthService.login(dto.getUsername(), dto.getPassword()));
    }
}
```

确认 `R.ok(...)` 和 `LoginDTO` 与现有使用一致（如有出入，以 Plan 5 admin 测试代码中 `R.ok` 用法为准）。

- [ ] **Step 2: 编译**

```bash
cd freshfood-app
mvn compile -q
```

Expected: 通过。

- [ ] **Step 3: 启动应用验证（手动，可选）**

```bash
cd freshfood-app
mvn spring-boot:run -q
```

另开终端：

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

Expected: HTTP 200，body 包含 `"role":"ADMIN"`，`"token":"..."`。Ctrl+C 关闭 server。

⚠️ **若 DB 未启动 / seed 数据为空**：此步骤可跳过，但记录到 README。

- [ ] **Step 4: Commit**

```bash
cd ..
git add freshfood-app/src/main/java/com/yan/freshfood/app/controller/UnifiedAuthController.java
git commit -m "feat(auth): 统一登录 Controller 占位 /api/v1/auth/login"
```

---

### Task 11: 删除 `AuthController.login`（让位 unified）

**Files:**
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/AuthController.java`

- [ ] **Step 1: 删 login 端点**

完整文件：

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-账号", description = "用户注册、登出、当前用户信息")
@SaIgnore
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过用户名、密码、手机号注册新账号，成功后返回 satoken。无需登录")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "注销当前登录状态，清除 satoken。需要登录")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
```

变更：
- 删除 `import com.yan.freshfood.user.dto.LoginDTO;`（已无引用）
- 删除 `@Tag` 的 description 中"登录"字段（改为"用户注册、登出、当前用户信息"）
- 删除 `@PostMapping("/login")` 端点

注：`AuthService.login(LoginDTO)` 接口方法**保留**（Task 5 抽出 doLogin 的同时保留 login），但现在 Controller 已经没有调用者，**保留以备未来用**——也可以现在顺手删。**建议保留**，避免破坏既有测试。

- [ ] **Step 2: 编译**

```bash
cd freshfood-user
mvn compile -q
```

Expected: 通过。

- [ ] **Step 3: Commit**

```bash
cd ..
git add freshfood-user/src/main/java/com/yan/freshfood/user/controller/AuthController.java
git commit -m "refactor(auth): 删除 AuthController.login 端点（让位 UnifiedAuthController）"
```

---

### Task 12: 给另外 2 个老登录端点加 `@Deprecated`

**Files:**
- Modify: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantAuthController.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminAuthController.java`

- [ ] **Step 1: 改 `MerchantAuthController.java`**

加 1 个 import：

```java
import java.lang.Deprecated;
```

在 `login` 端点上方加 `@`：

```java
    @Deprecated
    @PostMapping("/login")
    @Operation(summary = "商家登录（已废弃）",
               description = "商家用户名密码登录。新前端请改用 POST /api/v1/auth/login 统一登录。无登录鉴权。")
    public R<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return R.ok(authService.login(dto));
    }
```

其他不动。

- [ ] **Step 2: 改 `AdminAuthController.java`**

同上模式：

```java
    @Deprecated
    @PostMapping("/login")
    @Operation(summary = "管理员登录（已废弃）",
               description = "管理员账号登录。新前端请改用 POST /api/v1/auth/login 统一登录。无登录鉴权。")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(authService.login(dto));
    }
```

- [ ] **Step 3: 编译并跑全量测试**

```bash
cd freshfood-merchant && mvn test -q && cd ..
cd freshfood-admin && mvn test -q && cd ..
```

Expected: 两个模块全测试通过。

- [ ] **Step 4: Commit**

```bash
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantAuthController.java
git add freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminAuthController.java
git commit -m "refactor(auth): merchant / admin 登录端点标 @Deprecated，推荐走 unified"
```

---

### Task 13: 更新 `接口文档.md`

**Files:**
- Modify: `接口文档.md`（项目根目录）

- [ ] **Step 1: §一「通用约定」加统一登录说明**

在 `### 1.2 认证方式` 段落末尾追加一句：

```markdown
> **统一登录**：`POST /api/v1/auth/login` 为统一登录入口，按 `user → merchant → admin` 顺序匹配账号；返回 `token`、`role`（`USER` / `MERCHANT` / `ADMIN`）、`profile` 三字段。商家端 `/api/v1/merchant/auth/login` 与管理端 `/api/v1/admin/auth/login` 已标记 deprecated，新代码请走统一登录。
```

- [ ] **Step 2: §2.1「认证模块」替换登录端点说明**

把表格中第 2 行：

```markdown
| 2 | POST | `/auth/login` | ❌ | 用户名密码登录 |
```

替换为：

```markdown
| 2 | POST | `/auth/login` | ❌ | **统一登录**：服务端按顺序匹配 user/merchant/admin 账号；返回 `token` / `role` / `profile` |
```

把对应响应示例：

```json
{
  "code": 0,
  "data": {
    "token": "xxxx-xxxx-xxxx",
    "role": "USER",
    "profile": { "id": 10001, "username": "zhangsan", "nickname": "张三", "avatar": "https://.../avatar.png" }
  }
}
```

> 详细示例见「统一登录」（在 §一末尾）。

- [ ] **Step 3: §3.1 / §4.1 在 login 端点旁标 deprecated**

商家端 §3.1 表格：

```markdown
| 47 | POST | `/auth/login` ⚠️ deprecated | ❌ | 商家登录（推荐用 §一统一登录） |
```

管理员端 §4.1 表格：

```markdown
| 72 | POST | `/auth/login` ⚠️ deprecated | ❌ | 管理员登录（推荐用 §一统一登录） |
```

- [ ] **Step 4: Commit**

```bash
cd ..
git add 接口文档.md
git commit -m "docs(api): §一加统一登录说明 + §2.1 改造登录端点 + §3.1/§4.1 标 deprecated"
```

⚠️ 注意：`接口文档.md` 在 `C:\Users\yan\Desktop\线上生鲜商场购物平台开发-后端\` 父目录，**不在** `freshfood-shop/` git 仓库内。git add 不会追踪。需用：

```bash
cd ..
git status
```

如果 `接口文档.md` 不在 git 仓库，需手动维护 / 复制到仓库内的某 docs 目录，或者告知用户处理。

---

### Task 14: 全量回归 + 合 main

**Files:** none

- [ ] **Step 1: 跑全部单元测试**

```bash
cd freshfood-shop
mvn test -q
```

Expected: 所有模块测试通过，无回归。

- [ ] **Step 2: 推分支 + PR**

```bash
git push origin feature/unified-login
```

如需 PR 流程，按本项目约定执行（main 是 single-branch 模式时直接 fast-forward 合）。

- [ ] **Step 3: 合 main 并清理**

```bash
git checkout main
git pull origin main
git merge --ff-only feature/unified-login
git push origin main
git branch -d feature/unified-login
```

预期：fast-forward 合入，feature 分支被删。

---

## Self-Review Checklist

- [x] **Spec coverage:**
  - [x] Spec §1 (背景) → 体现于 §2 决策记录
  - [x] Spec §2 (决策) → 体现于 Task 5/6/11
  - [x] Spec §3 (端点清单) → Task 10
  - [x] Spec §4.1 (统一登录契约) → Task 9 + Task 10
  - [x] Spec §4.2 (profile 字段映射) → Task 8 + Task 9 (`Object` profile)
  - [x] Spec §4.3 (老端点 deprecated) → Task 12
  - [x] Spec §5.1 (模块归属) → Task 4 / 8 / 9 / 10 (`freshfood-app`)
  - [x] Spec §5.2 (Sa-Token 行为) → Task 5 (doLogin 内部走各自 StpLogic)
  - [x] Spec §5.3 (主流程) → Task 9
  - [x] Spec §5.4 (UsernameUniquenessChecker) → Task 4
  - [x] Spec §5.5 (错误处理) → Task 2 + LOGIN_FAILED
  - [x] Spec §6.3 (UnifiedLoginVO) → Task 8
  - [x] Spec §7.1 (单元测试) → Task 4 4 用例 + Task 9 6 用例
  - [x] Spec §9 (接口文档改) → Task 13

- [x] **Placeholder scan:** 全文已检查，无 TBD / TODO / "fill in details"。"实施前 STOP"、"若 DB 未启动" 是 execution-time 风险标注，不是 placeholder。

- [x] **Type consistency:**
  - `doLogin(XxxDO, String rawPassword)` 三个模块签名一致 ✓
  - `countByUsername(String) → long` 三个 mapper 签名一致 ✓
  - `LoginVO.doLogin` 返回 `LoginVO`、`MerchantLoginVO.doLogin` 返回 `MerchantLoginVO`、`AdminLoginVO.doLogin` 返回 `AdminLoginVO` ✓
  - `UnifiedLoginVO.profile` 类型 `Object` ✓
  - `ErrorCode.LOGIN_FAILED(1005)` 与 `GLOBAL_USERNAME_EXISTS(1002)` 唯一 ✓
  - `UsernameUniquenessChecker.checkAvailable(String)` 签名一致 ✓
  - `UnifiedAuthService.login(String, String)` 签名一致 ✓

- [x] **Dependency risk:** Task 6 / Task 7 涉及跨模块 mapper 依赖，有 2 个明确 STOP 验证步骤（`cat pom.xml`）。如不允许反向依赖，回退方案 B 已记录。

- [x] **Git reality:** Task 13 末尾注明 `接口文档.md` 在父目录，不在 git 仓库内。
