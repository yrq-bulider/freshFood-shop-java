# 计划 1：基础架构 + 三端登录 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `freshfood-shop` 从单模块 Spring Boot 改造为 7 模块 Maven 项目，搭建通用工具类、MyBatis-Plus / Sa-Token / Knife4j / Druid / CORS 配置、用户 / 商家 / 管理员三端基础实体与登录注册接口。

**Architecture:** 经典分层架构 —— 父 POM 统一管理依赖版本；`common` 放通用类（响应、异常、工具）；`framework` 放配置类；`model` 放 DO/DTO/VO；`user / merchant / admin` 三个业务模块各自含 Controller / Service / Mapper；`app` 作为聚合启动模块。认证用 Sa-Token，三个端用各自的 StpLogic 区分会话。

**Tech Stack:** Spring Boot 3.5.16、JDK 17、MyBatis-Plus 3.5.9、Sa-Token 1.37.0、Druid 1.2.23、Knife4j 4.5.0、Hutool 5.8.27、MySQL 8.0、JUnit 5。

---

## 一、文件结构总览

实施完成后 `freshfood-shop/` 应为如下结构：

```
freshfood-shop/                              # 父 POM（聚合）
├── pom.xml                                  # packaging=pom，统一依赖版本
├── docs/
│   └── superpowers/plans/
│       └── 2026-06-29-freshfood-foundation.md
├── freshfood-common/                        # 通用模块
│   ├── pom.xml
│   └── src/main/java/com/yan/freshfood/common/
│       ├── response/{R,PageR}.java
│       ├── exception/{BusinessException,ErrorCode}.java
│       ├── handler/GlobalExceptionHandler.java
│       ├── constant/CommonConstants.java
│       └── util/{IdUtil,BeanUtil}.java
├── freshfood-framework/                     # 框架配置模块
│   ├── pom.xml
│   └── src/main/java/com/yan/freshfood/framework/
│       ├── config/{MybatisPlusConfig,SaTokenConfig,Knife4jConfig,WebMvcConfig}.java
│       └── handler/MyMetaObjectHandler.java
├── freshfood-model/                         # 实体模块
│   ├── pom.xml
│   └── src/main/java/com/yan/freshfood/model/
│       └── entity/{BaseDO,UserDO,MerchantDO,AdminDO}.java
├── freshfood-user/                          # 用户端业务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/yan/freshfood/user/
│       │   ├── FreshfoodUserApplication.java
│       │   ├── controller/{AuthController,UserController}.java
│       │   ├── service/{UserService,AuthService}.java
│       │   ├── service/impl/{UserServiceImpl,AuthServiceImpl}.java
│       │   ├── mapper/UserMapper.java
│       │   ├── dto/{LoginDTO,RegisterDTO,UpdatePasswordDTO}.java
│       │   └── vo/{LoginVO,UserVO}.java
│       └── resources/
│           ├── application-user.yml
│           └── mapper/UserMapper.xml
├── freshfood-merchant/                      # 商家端业务
│   ├── pom.xml
│   └── src/main/...
├── freshfood-admin/                         # 管理端业务
│   ├── pom.xml
│   └── src/main/...
├── freshfood-app/                           # 聚合启动
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/yan/freshfood/FreshfoodShopApplication.java
│       └── resources/application.yml
└── sql/
    └── 01_init_schema.sql                   # 数据库初始化脚本
```

---

## 二、任务清单

### Task 1：备份现有代码

**Files:**
- 操作：临时备份现有 `pom.xml` 与 `src/`

- [ ] **Step 1：备份**

```bash
# 在 freshfood-shop/ 下执行
cp pom.xml pom.xml.bak
mv src src.bak
```

> 后续会把 `src.bak` 中的 `FreshfoodShopApplication.java` 移到新位置。

---

### Task 2：创建父 POM

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1：覆盖根 pom.xml 为父 POM**

完整文件内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.yan</groupId>
    <artifactId>freshfood-shop</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <name>freshfood-shop</name>
    <description>线上生鲜商场购物平台 - 后端</description>

    <modules>
        <module>freshfood-common</module>
        <module>freshfood-framework</module>
        <module>freshfood-model</module>
        <module>freshfood-user</module>
        <module>freshfood-merchant</module>
        <module>freshfood-admin</module>
        <module>freshfood-app</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <sa-token.version>1.37.0</sa-token.version>
        <druid.version>1.2.23</druid.version>
        <knife4j.version>4.5.0</knife4j.version>
        <hutool.version>5.8.27</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.yan</groupId>
                <artifactId>freshfood-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.yan</groupId>
                <artifactId>freshfood-framework</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.yan</groupId>
                <artifactId>freshfood-model</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-jsqlparser</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-jwt</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Druid -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>

            <!-- Knife4j -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>

            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2：验证 pom 文件有效**

```bash
cd freshfood-shop && mvn validate -q
```

预期：命令静默成功，无报错（此时还没创建子模块，Maven 可能会报"找不到 module"警告，忽略）。

---

### Task 3：创建 freshfood-common 模块骨架

**Files:**
- Create: `freshfood-common/pom.xml`

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-common/src/main/java/com/yan/freshfood/common/{response,exception,handler,constant,util}
```

- [ ] **Step 2：写 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yan</groupId>
        <artifactId>freshfood-shop</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>freshfood-common</artifactId>
    <name>freshfood-common</name>
    <description>通用模块：响应、异常、工具</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3：编译验证**

```bash
mvn -pl freshfood-common compile -q
```

预期：BUILD SUCCESS

---

### Task 4：实现 ErrorCode 错误码枚举

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_INVALID(1001, "参数校验失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(1004, "资源不存在"),
    SYSTEM_ERROR(8001, "系统异常"),

    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    USER_DISABLED(2003, "账号已禁用"),
    USER_ALREADY_EXISTS(2004, "用户名已存在"),

    MERCHANT_NOT_FOUND(7001, "商家不存在"),
    MERCHANT_PENDING(7002, "商家未通过审核"),

    ADMIN_NOT_FOUND(9001, "管理员不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

---

### Task 5：实现 BusinessException 自定义异常

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/BusinessException.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

---

### Task 6：实现 R<T> 统一响应

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/response/R.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yan.freshfood.common.exception.ErrorCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setMessage(ErrorCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
```

---

### Task 7：实现 PageR<T> 分页响应

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/response/PageR.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class PageR<T> {

    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;

    public static <T> PageR<T> of(IPage<T> page) {
        PageR<T> r = new PageR<>();
        r.setList(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    public static <T> PageR<T> empty(int pageNum, int pageSize) {
        PageR<T> r = new PageR<>();
        r.setList(Collections.emptyList());
        r.setTotal(0L);
        r.setPageNum(pageNum);
        r.setPageSize(pageSize);
        r.setPages(0);
        return r;
    }
}
```

---

### Task 8：实现 GlobalExceptionHandler 全局异常处理

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/handler/GlobalExceptionHandler.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        return R.fail(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException e) {
        return R.fail(ErrorCode.FORBIDDEN.getCode(), "无权限访问");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常，请稍后再试");
    }
}
```

---

### Task 9：实现 CommonConstants

**Files:**
- Create: `freshfood-common/src/main/java/com/yan/freshfood/common/constant/CommonConstants.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.common.constant;

public final class CommonConstants {

    private CommonConstants() {}

    /** 登录 Token Header 名（Sa-Token 默认） */
    public static final String TOKEN_HEADER = "satoken";

    /** 角色标识 */
    public static final String ROLE_USER = "user";
    public static final String ROLE_MERCHANT = "merchant";
    public static final String ROLE_ADMIN = "admin";

    /** Sa-Token 类型名称 */
    public static final String TYPE_USER = "user";
    public static final String TYPE_MERCHANT = "merchant";
    public static final String TYPE_ADMIN = "admin";

    /** 默认密码（管理员重置用） */
    public static final String DEFAULT_PASSWORD = "123456";
}
```

---

### Task 10：编写 Common 单元测试

**Files:**
- Create: `freshfood-common/src/test/java/com/yan/freshfood/common/response/RTest.java`

- [ ] **Step 1：写测试**

```java
package com.yan.freshfood.common.response;

import com.yan.freshfood.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void ok_withData_returnsSuccessResponse() {
        R<String> r = R.ok("hello");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("hello", r.getData());
    }

    @Test
    void fail_withErrorCode_returnsFailureResponse() {
        R<Void> r = R.fail(ErrorCode.USER_NOT_FOUND);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), r.getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void ok_noData_returnsSuccessWithoutData() {
        R<Void> r = R.ok();
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }
}
```

- [ ] **Step 2：运行测试**

```bash
mvn -pl freshfood-common test -Dtest=RTest -q
```

预期：Tests run: 3, Failures: 0, Errors: 0

---

### Task 11：创建 freshfood-model 模块

**Files:**
- Create: `freshfood-model/pom.xml`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/BaseDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/UserDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/MerchantDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/AdminDO.java`

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-model/src/main/java/com/yan/freshfood/model/entity
```

- [ ] **Step 2：写 model pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yan</groupId>
        <artifactId>freshfood-shop</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>freshfood-model</artifactId>
    <name>freshfood-model</name>
    <description>实体模块：DO/DTO/VO</description>

    <dependencies>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-annotation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

> 注：`mybatis-plus-annotation` 只含注解，不含运行时，model 模块可被 common 依赖时不传递引入 MyBatis 运行时。

- [ ] **Step 3：写 BaseDO**

```java
package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
```

- [ ] **Step 4：写 UserDO**

```java
package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class UserDO extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    /** 0 禁用 / 1 正常 */
    private Integer status;
}
```

- [ ] **Step 5：写 MerchantDO**

```java
package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class MerchantDO extends BaseDO {

    private String username;
    private String password;
    private String shopName;
    private String contactName;
    private String contactPhone;
    private String logo;
    /** 0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer auditStatus;
    /** 0 禁用 / 1 正常 */
    private Integer status;
}
```

- [ ] **Step 6：写 AdminDO**

```java
package com.yan.freshfood.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class AdminDO extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    /** 0 禁用 / 1 正常 */
    private Integer status;
}
```

- [ ] **Step 7：编译验证**

```bash
mvn -pl freshfood-model compile -q
```

预期：BUILD SUCCESS

---

### Task 12：编写数据库初始化 SQL

**Files:**
- Create: `sql/01_init_schema.sql`

- [ ] **Step 1：建目录与文件**

```bash
mkdir -p sql
```

写入 `sql/01_init_schema.sql`：

```sql
-- ========================================
-- 线上生鲜商场购物平台 - 数据库初始化脚本
-- 计划 1：仅基础三端账号表
-- 数据库：freshfood_shop
-- ========================================

CREATE DATABASE IF NOT EXISTS freshfood_shop
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE freshfood_shop;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商家表
CREATE TABLE IF NOT EXISTS `merchant` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `username`       VARCHAR(50)  NOT NULL,
    `password`       VARCHAR(100) NOT NULL,
    `shop_name`      VARCHAR(100) NOT NULL COMMENT '店铺名',
    `contact_name`   VARCHAR(50)  DEFAULT NULL,
    `contact_phone`  VARCHAR(20)  DEFAULT NULL,
    `logo`           VARCHAR(255) DEFAULT NULL,
    `audit_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待审核 / 1 通过 / 2 拒绝',
    `status`         TINYINT      NOT NULL DEFAULT 1,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL,
    `password`    VARCHAR(100) NOT NULL,
    `nickname`    VARCHAR(50)  DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 测试数据：用户 zhangsan / 123456，商家 m01 / 123456，管理员 admin / 123456
-- 密码为 BCrypt 加密后的 "123456"
INSERT INTO `user` (username, password, nickname, status)
VALUES ('zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 1);

INSERT INTO `merchant` (username, password, shop_name, contact_name, audit_status, status)
VALUES ('m01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '鲜果园旗舰店', '李老板', 1, 1);

INSERT INTO `admin` (username, password, nickname, status)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);
```

> ⚠️ 上述 BCrypt 字符串对应明文 `123456`，可登录测试用。如不一致，可用 Sa-Token 的 `BCrypt.putpw("123456", "salt")` 在临时类里生成替换。

---

### Task 13：创建 freshfood-framework 模块

**Files:**
- Create: `freshfood-framework/pom.xml`

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-framework/src/main/java/com/yan/freshfood/framework/{config,handler}
```

- [ ] **Step 2：写 framework pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yan</groupId>
        <artifactId>freshfood-shop</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>freshfood-framework</artifactId>
    <name>freshfood-framework</name>
    <description>框架配置模块</description>

    <dependencies>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-model</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>

        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-jwt</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

### Task 14：实现 MyMetaObjectHandler（自动填充）

**Files:**
- Create: `freshfood-framework/src/main/java/com/yan/freshfood/framework/handler/MyMetaObjectHandler.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.framework.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

### Task 15：实现 MybatisPlusConfig

**Files:**
- Create: `freshfood-framework/src/main/java/com/yan/freshfood/framework/config/MybatisPlusConfig.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

---

### Task 16：实现 SaTokenConfig（多端登录）

**Files:**
- Create: `freshfood-framework/src/main/java/com/yan/freshfood/framework/config/SaTokenConfig.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.framework.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import com.yan.freshfood.common.constant.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenConfig {

    /** 用户端 */
    @Bean(name = "stpUserLogic")
    public StpLogic stpUserLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_USER);
    }

    /** 商家端 */
    @Bean(name = "stpMerchantLogic")
    public StpLogic stpMerchantLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_MERCHANT);
    }

    /** 管理员端 */
    @Bean(name = "stpAdminLogic")
    public StpLogic stpAdminLogic() {
        return new StpLogicJwtForSimple(CommonConstants.TYPE_ADMIN);
    }

    /** 当前端类型，方便在拦截器中获取 */
    public static String currentType() {
        if (cn.dev33.satoken.stp.StpUtil.isLogin()) return CommonConstants.TYPE_USER;
        if (isLogin(CommonConstants.TYPE_MERCHANT)) return CommonConstants.TYPE_MERCHANT;
        if (isLogin(CommonConstants.TYPE_ADMIN)) return CommonConstants.TYPE_ADMIN;
        return CommonConstants.TYPE_USER;
    }

    private static boolean isLogin(String type) {
        return cn.dev33.satoken.stp.StpUtil.getStpLogic(type, null).isLogin();
    }
}
```

> ⚠️ 由于 Sa-Token 的 `StpLogic` API 在多类型下需要单独调用，这里简化为使用 `StpUtil.isLogin()` 检测用户端。各业务模块在自己的 Service 中通过 `StpUtil.getLoginIdAsLong()`（用户端）或对应类型的 `stpLogic.getLoginIdAsLong()` 获取登录态。

---

### Task 17：实现 Knife4jConfig

**Files:**
- Create: `freshfood-framework/src/main/java/com/yan/freshfood/framework/config/Knife4jConfig.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI freshfoodOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("线上生鲜商场 API 文档")
                        .description("接口文档")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("satoken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("satoken")))
                .addSecurityItem(new SecurityRequirement().addList("satoken"));
    }
}
```

---

### Task 18：实现 WebMvcConfig（CORS + 拦截器）

**Files:**
- Create: `freshfood-framework/src/main/java/com/yan/freshfood/framework/config/WebMvcConfig.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

> 登录接口放行在 Controller 上加 `@SaIgnore` 注解，不需要在拦截器放行。

---

### Task 19：创建 freshfood-user 模块（含用户端登录）

**Files:**
- Create: `freshfood-user/pom.xml`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/FreshfoodUserApplication.java`（如不需要单独启动，可省略）

> 注：实际启动在 `freshfood-app` 模块，业务模块不需要独立启动类，但保留模块可单独运行便于调试。

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-user/src/main/java/com/yan/freshfood/user/{controller,service/impl,mapper,dto,vo}
mkdir -p freshfood-user/src/main/resources/mapper
```

- [ ] **Step 2：写 user pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yan</groupId>
        <artifactId>freshfood-shop</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>freshfood-user</artifactId>
    <name>freshfood-user</name>

    <dependencies>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-model</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3：写 LoginDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 4：写 RegisterDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3-20 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 之间")
    private String password;

    private String nickname;
    private String phone;
}
```

- [ ] **Step 5：写 LoginVO**

```java
package com.yan.freshfood.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserVO user;
}
```

- [ ] **Step 6：写 UserVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private LocalDateTime createTime;
}
```

- [ ] **Step 7：写 UpdatePasswordDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需在 6-20 之间")
    private String newPassword;
}
```

- [ ] **Step 8：写 UserMapper**

```java
package com.yan.freshfood.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
```

- [ ] **Step 9：写 AuthService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    LoginVO register(RegisterDTO dto);

    void logout();
}
```

- [ ] **Step 10：写 AuthServiceImpl**

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
        // 1. 查用户
        UserDO user = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 2. 校验状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        // 3. 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        // 4. 登录（Sa-Token 用户端）
        StpUtil.login(user.getId());
        // 5. 返回
        return new LoginVO(StpUtil.getTokenValue(), toVO(user));
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 1. 检查用户名重复
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        // 2. 构建实体
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        // 3. 插入
        userMapper.insert(user);
        // 4. 自动登录
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

- [ ] **Step 11：写 UserService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.vo.UserVO;

public interface UserService {

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UserVO vo);

    void updatePassword(UpdatePasswordDTO dto);
}
```

- [ ] **Step 12：写 UserServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.service.UserService;
import com.yan.freshfood.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    public UserVO updateCurrentUser(UserVO vo) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = new UserDO();
        user.setId(userId);
        user.setNickname(vo.getNickname());
        user.setAvatar(vo.getAvatar());
        user.setPhone(vo.getPhone());
        user.setEmail(vo.getEmail());
        userMapper.updateById(user);
        return getCurrentUser();
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null || !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        UserDO update = new UserDO();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(update);
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

- [ ] **Step 13：写 AuthController**

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.service.AuthService;
import com.yan.freshfood.user.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaIgnore
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
```

- [ ] **Step 14：写 UserController**

```java
package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.UpdatePasswordDTO;
import com.yan.freshfood.user.service.UserService;
import com.yan.freshfood.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public R<UserVO> updateMe(@RequestBody UserVO vo) {
        return R.ok(userService.updateCurrentUser(vo));
    }

    @PutMapping("/me/password")
    public R<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(dto);
        return R.ok();
    }
}
```

---

### Task 20：编写 AuthService 单元测试

**Files:**
- Create: `freshfood-user/src/test/java/com/yan/freshfood/user/service/impl/AuthServiceImplTest.java`

- [ ] **Step 1：写测试**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.dto.LoginDTO;
import com.yan.freshfood.user.dto.RegisterDTO;
import com.yan.freshfood.user.mapper.UserMapper;
import com.yan.freshfood.user.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserDO buildUser(String username, String rawPwd) {
        UserDO u = new UserDO();
        u.setId(1L);
        u.setUsername(username);
        u.setPassword(BCrypt.hashpw(rawPwd));
        u.setStatus(1);
        u.setNickname("nick");
        return u;
    }

    @Test
    void login_success() {
        UserDO user = buildUser("zhangsan", "123456");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        LoginVO vo = authService.login(new LoginDTO("zhangsan", "123456"));
        assertNotNull(vo.getToken());
        assertEquals(1L, vo.getUser().getId());
        assertEquals("zhangsan", vo.getUser().getUsername());
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginDTO("nobody", "123456")));
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void login_wrongPassword_throwsException() {
        UserDO user = buildUser("zhangsan", "123456");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginDTO("zhangsan", "wrong")));
        assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), ex.getCode());
    }

    @Test
    void login_disabledUser_throwsException() {
        UserDO user = buildUser("zhangsan", "123456");
        user.setStatus(0);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginDTO("zhangsan", "123456")));
        assertEquals(ErrorCode.USER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void register_duplicateUsername_throwsException() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("dup");
        dto.setPassword("123456");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(dto));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void register_success() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(UserDO.class))).thenAnswer(inv -> {
            UserDO u = inv.getArgument(0);
            u.setId(99L);
            return 1;
        });

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newone");
        dto.setPassword("123456");
        dto.setNickname("新人");
        LoginVO vo = authService.register(dto);
        assertNotNull(vo.getToken());
        assertEquals("newone", vo.getUser().getUsername());
    }
}
```

- [ ] **Step 2：运行测试**

```bash
mvn -pl freshfood-user test -Dtest=AuthServiceImplTest -q
```

预期：Tests run: 6, Failures: 0, Errors: 0

> ⚠️ 该测试需要绕过 Sa-Token 的 `StpUtil.login`。如报错"未配置 Sa-Token 上下文"，需在测试类加 `@ActiveProfiles("test")` 并新建 `application-test.yml`（最小配置）。简化方案：在测试中用 PowerMock 或重写 Sa-Token 配置。最简单的方式是引入 `sa-token-test` 或在测试类加：

```java
@Configuration
static class TestConfig {
    @Bean public SaTokenConfig saTokenConfig() { return new SaTokenConfig(); }
}
```

如果跑不通，可先跳过单元测试，依赖后续 Knife4j 手动验证。

---

### Task 21：创建 freshfood-merchant 模块（含商家登录）

**Files:**
- Create: `freshfood-merchant/pom.xml`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/MerchantMapper.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/MerchantLoginDTO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantLoginVO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantVO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantAuthService.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantAuthServiceImpl.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantAuthController.java`

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-merchant/src/main/java/com/yan/freshfood/merchant/{controller,service/impl,mapper,dto,vo}
```

- [ ] **Step 2：写 merchant pom.xml**

同 `freshfood-user/pom.xml`，改 `<artifactId>freshfood-merchant</artifactId>`。

- [ ] **Step 3：写 MerchantMapper**

```java
package com.yan.freshfood.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<MerchantDO> {
}
```

- [ ] **Step 4：写 MerchantLoginDTO**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 5：写 MerchantVO**

```java
package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantVO {
    private Long id;
    private String username;
    private String shopName;
    private String contactName;
    private String contactPhone;
    private String logo;
    private Integer auditStatus;
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 6：写 MerchantLoginVO**

```java
package com.yan.freshfood.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantLoginVO {
    private String token;
    private MerchantVO merchant;
}
```

- [ ] **Step 7：写 MerchantAuthService 接口**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;

public interface MerchantAuthService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    void logout();
}
```

- [ ] **Step 8：写 MerchantAuthServiceImpl**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
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
        if (m.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (m.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_PENDING);
        }
        if (!BCrypt.checkpw(dto.getPassword(), m.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        StpLogic logic = StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null);
        logic.login(m.getId());

        return new MerchantLoginVO(logic.getTokenValue(), toVO(m));
    }

    @Override
    public void logout() {
        StpLogic logic = StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null);
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

- [ ] **Step 9：写 MerchantAuthController**

```java
package com.yan.freshfood.merchant.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaIgnore
@RestController
@RequestMapping("/api/v1/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService authService;

    @PostMapping("/login")
    public R<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
```

---

### Task 22：创建 freshfood-admin 模块（含管理员登录）

**Files:**（同商家模块结构，改名 Admin）

- Create: `freshfood-admin/pom.xml`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/mapper/AdminMapper.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/AdminLoginDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminLoginVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/AdminAuthService.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/AdminAuthServiceImpl.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminAuthController.java`

- [ ] **Step 1：建目录与 pom.xml**

```bash
mkdir -p freshfood-admin/src/main/java/com/yan/freshfood/admin/{controller,service/impl,mapper,dto,vo}
```

pom.xml 同 user 模块，artifactId 改 `freshfood-admin`。

- [ ] **Step 2：写 AdminMapper**

```java
package com.yan.freshfood.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.AdminDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<AdminDO> {
}
```

- [ ] **Step 3：写 AdminLoginDTO / AdminVO / AdminLoginVO**

参考 Merchant 模块对应类，字段：`username / password / nickname / status / createTime`。

- [ ] **Step 4：写 AdminAuthServiceImpl**

参考 MerchantAuthServiceImpl，把 `merchantMapper`、`MerchantDO`、`MerchantLoginDTO`、`MerchantVO`、`MERCHANT_NOT_FOUND` 替换为对应 admin 实体。

```java
@Override
public AdminLoginVO login(AdminLoginDTO dto) {
    AdminDO a = adminMapper.selectOne(
            new LambdaQueryWrapper<AdminDO>().eq(AdminDO::getUsername, dto.getUsername())
    );
    if (a == null) throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
    if (a.getStatus() == 0) throw new BusinessException(ErrorCode.USER_DISABLED);
    if (!BCrypt.checkpw(dto.getPassword(), a.getPassword())) {
        throw new BusinessException(ErrorCode.PASSWORD_ERROR);
    }
    StpLogic logic = StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null);
    logic.login(a.getId());
    return new AdminLoginVO(logic.getTokenValue(), toVO(a));
}
```

- [ ] **Step 5：写 AdminAuthController**

路径：`/api/v1/admin/auth/login`、`/api/v1/admin/auth/logout`，参考 MerchantAuthController。

---

### Task 23：创建 freshfood-app 聚合启动模块

**Files:**
- Create: `freshfood-app/pom.xml`
- Create: `freshfood-app/src/main/java/com/yan/freshfood/FreshfoodShopApplication.java`
- Create: `freshfood-app/src/main/resources/application.yml`

- [ ] **Step 1：建目录**

```bash
mkdir -p freshfood-app/src/main/java/com/yan/freshfood
mkdir -p freshfood-app/src/main/resources
```

- [ ] **Step 2：写 app pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.yan</groupId>
        <artifactId>freshfood-shop</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>freshfood-app</artifactId>
    <name>freshfood-app</name>

    <dependencies>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-user</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-merchant</artifactId>
        </dependency>
        <dependency>
            <groupId>com.yan</groupId>
            <artifactId>freshfood-admin</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.yan.freshfood.FreshfoodShopApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3：写启动类**

```java
package com.yan.freshfood;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.yan.freshfood",
        "com.yan.freshfood.user",
        "com.yan.freshfood.merchant",
        "com.yan.freshfood.admin",
        "com.yan.freshfood.framework",
        "com.yan.freshfood.common"
})
@MapperScan(basePackages = {
        "com.yan.freshfood.user.mapper",
        "com.yan.freshfood.merchant.mapper",
        "com.yan.freshfood.admin.mapper"
})
public class FreshfoodShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshfoodShopApplication.class, args);
    }
}
```

- [ ] **Step 4：写 application.yml**

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: freshfood-shop
  profiles:
    active: dev
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/freshfood_shop?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto

sa-token:
  token-name: satoken
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: true
  token-style: uuid
  is-log: false
  is-read-cookie: false
  is-read-header: true

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
  default-flat-param-object: true

knife4j:
  enable: true
  setting:
    language: zh_cn

logging:
  level:
    com.yan.freshfood: debug
    com.baomidou.mybatisplus: info
```

> ⚠️ 上面的 MySQL 用户名/密码 `root/root`，请按本机实际修改。

- [ ] **Step 5：创建 dev profile（可选）**

新建 `freshfood-app/src/main/resources/application-dev.yml`：

```yaml
logging:
  level:
    com.yan.freshfood: debug
```

---

### Task 24：编译整个项目

**Files:**
- 操作：编译所有模块

- [ ] **Step 1：完整编译**

```bash
cd freshfood-shop && mvn clean compile -q
```

预期：BUILD SUCCESS，所有 7 个模块编译通过。

- [ ] **Step 2：解决可能出现的循环依赖或编译错误**

如果报错，按以下思路排查：
- `@MapperScan` 路径是否覆盖到所有 mapper 包
- `CommonConstants.TYPE_USER` 等常量在 framework 模块引用 common，是否在依赖里
- module 间循环依赖：model 不能依赖 framework 或 business；business 之间不能互依赖

---

### Task 25：导入 SQL 并启动应用

**Files:**
- 操作：导入 `sql/01_init_schema.sql`，启动 Spring Boot

- [ ] **Step 1：导入 SQL**

```bash
mysql -uroot -p < sql/01_init_schema.sql
```

- [ ] **Step 2：启动应用**

```bash
cd freshfood-shop && mvn -pl freshfood-app spring-boot:run
```

预期：控制台看到 `Started FreshfoodShopApplication in X.XXX seconds`，无 ERROR 日志。

- [ ] **Step 3：访问 Knife4j**

打开浏览器：http://localhost:8080/doc.html

预期：看到三个分组（用户端、商家端、管理端），每个端都有登录接口。

---

### Task 26：手动验证登录接口

**Files:**
- 操作：使用 curl 或 Knife4j 调用接口

- [ ] **Step 1：用户端登录**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```

预期响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "xxxx-xxxx",
    "user": { "id": 1, "username": "zhangsan", "nickname": "张三" }
  },
  "timestamp": 1719657600000
}
```

- [ ] **Step 2：用户端错误密码**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"wrong"}'
```

预期响应：

```json
{ "code": 2002, "message": "密码错误", ... }
```

- [ ] **Step 3：商家登录**

```bash
curl -X POST http://localhost:8080/api/v1/merchant/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456"}'
```

预期：`code=0`，返回 token。

- [ ] **Step 4：管理员登录**

```bash
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

预期：`code=0`，返回 token。

- [ ] **Step 5：访问需登录接口**

```bash
TOKEN="从上面获取的 token"
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "satoken: $TOKEN"
```

预期：`code=0`，返回当前用户信息。

- [ ] **Step 6：未登录访问被拦截**

```bash
curl -X GET http://localhost:8080/api/v1/users/me
```

预期：`code=401`，message 含"请先登录"。

---

### Task 27：清理临时备份

**Files:**
- 操作：删除备份文件

- [ ] **Step 1：删除 src.bak 和 pom.xml.bak**

```bash
cd freshfood-shop
rm -rf src.bak pom.xml.bak
```

---

### Task 28：提交（如使用 git）

**Files:**
- 操作：初始化 git 仓库并提交

- [ ] **Step 1：初始化 git（项目尚未启用版本控制时）**

```bash
cd freshfood-shop
git init
git add .
git commit -m "feat: 搭建多模块基础架构 + 三端登录接口

- 多模块 Maven 结构（parent + common + framework + model + user + merchant + admin + app）
- 通用类：R/PageR 统一响应、ErrorCode、BusinessException、GlobalExceptionHandler
- 配置：MyBatis-Plus、Sa-Token（多端登录）、Knife4j、Druid、CORS、MyMetaObjectHandler
- 用户/商家/管理员三端登录、注册、个人信息接口
- 初始化 SQL（user/merchant/admin 表 + 测试数据）"
```

> 如果用户暂不需要 git，可跳过此任务。

---

## 三、自检清单（执行完成后逐项勾选）

- [ ] 父 POM 与 7 个子模块 POM 完整
- [ ] `R.ok(...)` / `R.fail(...)` 可正常使用
- [ ] `BusinessException` + `GlobalExceptionHandler` 统一拦截
- [ ] MyBatis-Plus 分页插件已配置
- [ ] Sa-Token 用户/商家/管理员三个 StpLogic 已注册
- [ ] Knife4j 可访问 `http://localhost:8080/doc.html`
- [ ] CORS 配置生效
- [ ] 用户/商家/管理员登录接口返回正确 token
- [ ] 错误密码返回 `code=2002`
- [ ] 未登录访问 `/users/me` 返回 `code=401`
- [ ] `MyMetaObjectHandler` 自动填充 createTime/updateTime/deleted

---

## 四、下一步（计划 2 预告）

完成计划 1 后，接下来进入 **计划 2：用户端核心业务**，包括：
- 商品域（分类、SPU、SKU、Banner、活动）建表 + CRUD + 公开查询接口
- 搜索（MySQL LIKE + 热搜 + 历史）
- 购物车
- 收货地址
- 下单 / 支付（含状态机、库存扣减、优惠计算、订单快照）
- 物流轨迹（定时任务模拟）
- 评价
- 消息

预估 40 个左右任务。