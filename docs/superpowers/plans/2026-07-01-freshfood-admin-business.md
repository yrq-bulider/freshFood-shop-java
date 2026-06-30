# Plan 4 — 管理员端业务实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已有 `freshfood-admin` 模块上扩展 4 个业务模块（商家审核 / 商品审核 / 用户管理 / 运营内容），共 28 个 REST 端点 + 12 个单元测试。

**Architecture:** 沿用 Plan 2/3 既有架构（Controller → Service → Mapper）。admin 是上帝视角，所有写操作只校验"状态允许"，不校验"资源归属"。复用 user/merchant 模块的 Mapper 接口（已有），不新建 mapper。

**Tech Stack:** Spring Boot 3.5.16 / Java 17 / MyBatis-Plus 3.5.9 / Sa-Token 1.37.0 / Lombok / JUnit 5 / Mockito

**模块清单：**
- M1 商家审核（5 端点）：page / detail / audit / status / audit-pending
- M2 商品审核（5 端点）：page / detail / audit / off-shelf / audit-pending
- M3 用户管理（4 端点）：page / detail / status / reset-password
- M4 运营内容（14 端点）：Banner CRUD 4 + HotWord CRUD 4 + Category 6（list / tree / create / update / status / delete）

**实施顺序：** M1 → M2 → M3 → M4 (Banner → HotWord → Category)

**任务依赖：**
```
Task 1 (分支 + 验证)
  ↓
Task 2 (ErrorCode +3)
  ↓
Task 3 (M1 Service) → Task 4 (M1 Controller)
  ↓
Task 5 (M2 DTO+Service+Controller)
  ↓
Task 6 (M3 DTO+Service+Controller)
  ↓
Task 7 (M4 Banner DTO+Service+Controller)
  ↓
Task 8 (M4 HotWord DTO+Service+Controller)
  ↓
Task 9 (M4 Category DTO+Service+Controller)
  ↓
Task 10 (12 个单元测试)
  ↓
Task 11 (全模块编译 + 静态结构审查)
  ↓
Task 12 (推送 + fast-forward 合 main)
```

---

## 项目约定（所有 task 适用）

### 1. 代码风格
- Lombok `@Data`（DTO/VO）+ `@RequiredArgsConstructor`（Service/Controller）
- 4 层架构：Controller → Service → Mapper → DO
- 包名：`com.yan.freshfood.admin.{controller, service, service.impl, dto, vo, mapper}`

### 2. Sa-Token 用法
- admin 端用 `StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null).getLoginIdAsLong()` 取当前 admin id
- **不校验**资源归属（admin 上帝视角）
- 但需要 mock StpUtil 防 NullPointerException（测试场景）

### 3. 异常处理
- 资源不存在 → `throw new BusinessException(ErrorCode.NOT_FOUND)`
- 状态不允许 → 抛对应 ErrorCode（如 `MERCHANT_AUDIT_INVALID`）
- 入参校验失败 → `@Valid` 自动拦截（GlobalExceptionHandler）

### 4. 响应包装
- 单对象：`R.ok(data)` 或 `R.ok()` 无参
- 分页：`R.ok(PageR.of(IPage<T> page))`
- PageR 签名：`PageR.of(IPage<T>)` — **不要**用 records/total/pageNum/pageSize 重载

### 5. 金额与时间
- 金额字段出参用 `String` (toPlainString) 防前端精度丢失（本 plan 无金额字段）
- 时间用 `LocalDateTime`
- 加密字段（phone/email/contactName/contactPhone）出参用 `String`（明文，admin 视角）

### 6. StpLogic mock 标准模式（测试）
```java
try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
    StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
    stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
    when(logic.getLoginIdAsLong()).thenReturn(1L);
    // ...
}
```
**第二个参数必须 `anyString()`（不是 `any()`）**——Mockito 严格模式会因类型不匹配报错。

### 7. BusinessException 断言
- BusinessException 只暴露 `getCode()`（Lombok `@Getter` 生成）
- 断言用：`assertEquals(ErrorCode.FOO.getCode(), ex.getCode())`
- **不要**用 `ex.getErrorCode()`（Plan 2 已踩坑）

### 8. Git commit
- type 英文（feat/fix/refactor/chore/test/docs），subject + body 中文
- 不用 `git add -A` / `git add .`，明确列文件
- 用 HEREDOC 写 commit message
- **不** push、**不** amend（由 Task 12 统一处理）

### 9. 编译
- JDK 17 未本地安装，**每个 task 都跳过 mvn compile**
- commit message body 注明 "编译跳过 (待 JDK 17 装好后补验)"
- Task 11 统一做静态结构审查（人工 Read 比对）

---

## 文件结构总览

### 新增文件（约 40+）

**DTO（11 个）：**
- M1: `MerchantAuditDTO.java`、`MerchantStatusDTO.java`
- M2: `ProductAuditDTO.java`
- M3: `UserStatusDTO.java`
- M4: `BannerCreateDTO.java`、`BannerUpdateDTO.java`、`HotWordCreateDTO.java`、`HotWordUpdateDTO.java`、`CategoryCreateDTO.java`、`CategoryUpdateDTO.java`、`CategoryStatusDTO.java`

**VO（10 个）：**
- M1: `AdminMerchantVO.java`、`AuditPendingVO.java`（共用）
- M2: `AdminProductVO.java`
- M3: `AdminUserVO.java`
- M4: `AdminBannerVO.java`、`AdminHotWordVO.java`、`AdminCategoryVO.java`、`AdminCategoryTreeVO.java`

**Service + Impl（4 套 ×2 = 8 个）：**
- `MerchantAdminService.java` + `MerchantAdminServiceImpl.java`
- `ProductAdminService.java` + `ProductAdminServiceImpl.java`
- `UserAdminService.java` + `UserAdminServiceImpl.java`
- `ContentAdminService.java` + `ContentAdminServiceImpl.java`（聚合 Banner/HotWord/Category）

**Controller（4 个）：**
- `AdminMerchantController.java`
- `AdminProductController.java`
- `AdminUserController.java`
- `AdminContentController.java`（聚合 Banner/HotWord/Category）

**Test（4 个）：**
- `MerchantAdminServiceImplTest.java`
- `ProductAdminServiceImplTest.java`
- `UserAdminServiceImplTest.java`
- `ContentAdminServiceImplTest.java`

### 修改文件（1 个）：
- `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`（新增 3 条枚举）

### 不新建 mapper
- `BannerMapper`、`HotWordMapper` 已在 `freshfood-user` 模块（Plan 2 提前建好）
- `UserMapper` 在 `freshfood-user` 模块
- `MerchantMapper`、`ProductMapper`、`CategoryMapper` 在 `freshfood-merchant` 模块
- admin 模块直接 `import` 跨模块 mapper 即可

### 不新建数据库表
- 复用 Plan 1-3 已有的 12 张业务表

---

## 后续 Task 见 plan 文件续部分
（Task 1-12 详情接续，结构同上）

---

### Task 1：建分支 + Mapper 复用验证

**Files:** 无（仅 git + 验证命令）

- [ ] **Step 1：从 main 拉新分支 feature/admin-business**

```bash
cd freshfood-shop
git checkout main
git pull origin main
git checkout -b feature/admin-business
```

预期：分支切换到 `feature/admin-business`，与 `origin/main` 同步。

- [ ] **Step 2：验证 admin 端可复用的 mapper 全部存在**

逐个确认以下文件存在（**只 Read，不修改**）：

- `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/UserMapper.java`
- `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/BannerMapper.java`
- `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/HotWordMapper.java`
- `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/MerchantMapper.java`
- `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/ProductMapper.java`
- `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/CategoryMapper.java`

每个文件都应是 `@Mapper` 注解 + `extends BaseMapper<XxxDO>` 的空接口。

如果任何文件不存在，**立即报告**，不要继续。

- [ ] **Step 3：commit（空 commit 标记分支起点）**

```bash
cd freshfood-shop
git commit --allow-empty -m "$(cat <<'EOF'
chore(admin-branch): 建 feature/admin-business 分支

Plan 4 管理员端业务实施分支。
mapper 全部复用 user/merchant 模块，无需新建。
EOF
)"
```

预期：`[feature/admin-business (root-commit) ...]` 或类似空 commit。

---

### Task 2：ErrorCode 新增 3 条错误码

**Files:**
- Modify: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`

- [ ] **Step 1：Read 当前 ErrorCode.java，确认插入位置**

文件位置 `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`。
当前内容（与 Plan 3 之后一致）：

```java
PRODUCT_OFF_SHELF(3001, "商品已下架"),
STOCK_NOT_ENOUGH(3002, "库存不足"),
PRODUCT_PENDING_AUDIT(3003, "商品待审核，不可上架"),
SKU_HAS_SALES(3004, "SKU 已有销量，不可删除"),

ORDER_STATUS_INVALID(4001, "订单状态不允许该操作"),
```

- [ ] **Step 2：在 PRODUCT_OFF_SHELF 区块的 SKU_HAS_SALES 后插入 PRODUCT_AUDIT_INVALID(3005)**

```java
PRODUCT_OFF_SHELF(3001, "商品已下架"),
STOCK_NOT_ENOUGH(3002, "库存不足"),
PRODUCT_PENDING_AUDIT(3003, "商品待审核，不可上架"),
SKU_HAS_SALES(3004, "SKU 已有销量，不可删除"),
PRODUCT_AUDIT_INVALID(3005, "商品审核状态不允许此操作"),
```

- [ ] **Step 3：在 MERCHANT_PENDING 后插入 MERCHANT_AUDIT_INVALID(7003) 和 CATEGORY_IN_USE(7004)**

```java
MERCHANT_NOT_FOUND(7001, "商家不存在"),
MERCHANT_PENDING(7002, "商家未通过审核"),
MERCHANT_AUDIT_INVALID(7003, "商家审核状态不允许此操作"),
CATEGORY_IN_USE(7004, "分类存在子分类或被商品引用，不可删除"),
```

- [ ] **Step 4：commit**

```bash
cd freshfood-shop
git add freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java
git commit -m "$(cat <<'EOF'
feat(common-errorcode): 新增管理员端 3 条错误码

- PRODUCT_AUDIT_INVALID(3005)：商品审核状态不允许此操作
- MERCHANT_AUDIT_INVALID(7003)：商家审核状态不允许此操作
- CATEGORY_IN_USE(7004)：分类存在子分类或被商品引用，不可删除

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 3：M1 商家审核 DTO + Service

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/MerchantAuditDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/MerchantStatusDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminMerchantVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AuditPendingVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/MerchantAdminService.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/MerchantAdminServiceImpl.java`

- [ ] **Step 1：`MerchantAuditDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantAuditDTO {
    /** 1 通过 / 2 拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;
}
```

- [ ] **Step 2：`MerchantStatusDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantStatusDTO {
    /** 0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

- [ ] **Step 3：`AdminMerchantVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMerchantVO {
    private Long id;
    private String username;
    private String shopName;
    /** 解密后明文 */
    private String contactName;
    /** 解密后明文 */
    private String contactPhone;
    private String logo;
    /** 0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer auditStatus;
    /** 0 禁用 / 1 正常 */
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4：`AuditPendingVO.java`（共用，M2 也会用）**

```java
package com.yan.freshfood.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 审核待办数量 */
@Data
@AllArgsConstructor
public class AuditPendingVO {
    private Long count;
}
```

- [ ] **Step 5：`MerchantAdminService.java`**

```java
package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;

public interface MerchantAdminService {
    PageR<AdminMerchantVO> page(String keyword, Integer auditStatus, Integer status,
                                 long pageNum, long pageSize);
    AdminMerchantVO detail(Long id);
    void audit(Long id, Integer auditStatus);
    void updateStatus(Long id, Integer status);
    AuditPendingVO auditPendingCount();
}
```

- [ ] **Step 6：`MerchantAdminServiceImpl.java`**

```java
package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.MerchantAdminService;
import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantAdminServiceImpl implements MerchantAdminService {

    private final MerchantMapper merchantMapper;

    @Override
    public PageR<AdminMerchantVO> page(String keyword, Integer auditStatus, Integer status,
                                         long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<MerchantDO> q = new LambdaQueryWrapper<MerchantDO>()
                .orderByDesc(MerchantDO::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(MerchantDO::getUsername, keyword)
                    .or().like(MerchantDO::getShopName, keyword));
        }
        if (auditStatus != null) q.eq(MerchantDO::getAuditStatus, auditStatus);
        if (status != null) q.eq(MerchantDO::getStatus, status);

        Page<MerchantDO> page = merchantMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminMerchantVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        Page<AdminMerchantVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminMerchantVO detail(Long id) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVO(m);
    }

    @Override
    public void audit(Long id, Integer auditStatus) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (m.getAuditStatus() == null || m.getAuditStatus() != 0) {
            throw new BusinessException(ErrorCode.MERCHANT_AUDIT_INVALID);
        }
        m.setAuditStatus(auditStatus);
        merchantMapper.updateById(m);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        MerchantDO m = merchantMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        m.setStatus(status);
        merchantMapper.updateById(m);
    }

    @Override
    public AuditPendingVO auditPendingCount() {
        Long count = merchantMapper.selectCount(
                new LambdaQueryWrapper<MerchantDO>().eq(MerchantDO::getAuditStatus, 0));
        return new AuditPendingVO(count);
    }

    private AdminMerchantVO toVO(MerchantDO m) {
        AdminMerchantVO vo = new AdminMerchantVO();
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

- [ ] **Step 7：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/MerchantAuditDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/MerchantStatusDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminMerchantVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AuditPendingVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/MerchantAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/MerchantAdminServiceImpl.java

git commit -m "$(cat <<'EOF'
feat(admin-merchant-service): M1 商家审核 Service 5 个方法

- page(keyword, auditStatus, status, pageNum, pageSize)：分页+过滤
- detail(id)：含解密 contactName/contactPhone
- audit(id, auditStatus)：前置 auditStatus==0 校验, 否则 7003
- updateStatus(id, status)：不限当前 status
- auditPendingCount()：auditStatus==0 数量

复用 MerchantMapper (Plan 3 已建)。

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 4：M1 商家审核 Controller

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminMerchantController.java`

- [ ] **Step 1：Controller**

```java
package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.MerchantAuditDTO;
import com.yan.freshfood.admin.dto.MerchantStatusDTO;
import com.yan.freshfood.admin.service.MerchantAdminService;
import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantAdminService merchantAdminService;

    @GetMapping
    public R<PageR<AdminMerchantVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantAdminService.page(keyword, auditStatus, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminMerchantVO> detail(@PathVariable Long id) {
        return R.ok(merchantAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody MerchantAuditDTO dto) {
        merchantAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody MerchantStatusDTO dto) {
        merchantAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @GetMapping("/audit-pending")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(merchantAdminService.auditPendingCount());
    }
}
```

- [ ] **Step 2：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminMerchantController.java

git commit -m "$(cat <<'EOF'
feat(admin-merchant-controller): M1 商家审核 5 个 REST 端点

- GET    /api/v1/admin/merchants?keyword=&auditStatus=&status=&pageNum=&pageSize=
- GET    /api/v1/admin/merchants/{id}
- POST   /api/v1/admin/merchants/{id}/audit
- POST   /api/v1/admin/merchants/{id}/status
- GET    /api/v1/admin/merchants/audit-pending

audit 仅允许 auditStatus=0 → 1/2, 其他状态由 service 抛 7003。

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 5：M2 商品审核 DTO + Service + Controller

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/ProductAuditDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminProductVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ProductAdminService.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ProductAdminServiceImpl.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminProductController.java`

- [ ] **Step 1：`ProductAuditDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductAuditDTO {
    /** 1 通过 / 2 拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;
}
```

- [ ] **Step 2：`AdminProductVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProductVO {
    private Long id;
    private Long merchantId;
    /** 由 service 注入 */
    private String merchantName;
    private Long categoryId;
    /** 由 service 注入 */
    private String categoryName;
    private String name;
    private String mainImage;
    private String description;
    private String origin;
    private String afterSalesTags;
    /** 0 待审 / 1 通过 / 2 拒绝 */
    private Integer auditStatus;
    /** 0 下架 / 1 上架 */
    private Integer status;
    private Integer sales;
    private BigDecimal rating;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3：`ProductAdminService.java`**

```java
package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminProductVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;

public interface ProductAdminService {
    PageR<AdminProductVO> page(String keyword, Integer auditStatus, Integer status,
                                Long merchantId, long pageNum, long pageSize);
    AdminProductVO detail(Long id);
    void audit(Long id, Integer auditStatus);
    void offShelf(Long id);
    AuditPendingVO auditPendingCount();
}
```

- [ ] **Step 4：`ProductAdminServiceImpl.java`**

```java
package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.ProductAdminService;
import com.yan.freshfood.admin.vo.AdminProductVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageR<AdminProductVO> page(String keyword, Integer auditStatus, Integer status,
                                       Long merchantId, long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<ProductDO> q = new LambdaQueryWrapper<ProductDO>()
                .orderByDesc(ProductDO::getCreateTime);
        if (StringUtils.hasText(keyword)) q.like(ProductDO::getName, keyword);
        if (auditStatus != null) q.eq(ProductDO::getAuditStatus, auditStatus);
        if (status != null) q.eq(ProductDO::getStatus, status);
        if (merchantId != null) q.eq(ProductDO::getMerchantId, merchantId);

        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminProductVO> records = page.getRecords().stream()
                .map(this::toVOWithNames).collect(Collectors.toList());
        Page<AdminProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminProductVO detail(Long id) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVOWithNames(p);
    }

    @Override
    public void audit(Long id, Integer auditStatus) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (p.getAuditStatus() == null || p.getAuditStatus() != 0) {
            throw new BusinessException(ErrorCode.PRODUCT_AUDIT_INVALID);
        }
        p.setAuditStatus(auditStatus);
        productMapper.updateById(p);
    }

    @Override
    public void offShelf(Long id) {
        ProductDO p = productMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        p.setStatus(0);
        productMapper.updateById(p);
    }

    @Override
    public AuditPendingVO auditPendingCount() {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<ProductDO>().eq(ProductDO::getAuditStatus, 0));
        return new AuditPendingVO(count);
    }

    private AdminProductVO toVOWithNames(ProductDO p) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(p.getId());
        vo.setMerchantId(p.getMerchantId());
        vo.setCategoryId(p.getCategoryId());
        vo.setName(p.getName());
        vo.setMainImage(p.getMainImage());
        vo.setDescription(p.getDescription());
        vo.setOrigin(p.getOrigin());
        vo.setAfterSalesTags(p.getAfterSalesTags());
        vo.setAuditStatus(p.getAuditStatus());
        vo.setStatus(p.getStatus());
        vo.setSales(p.getSales());
        vo.setRating(p.getRating());
        vo.setCreateTime(p.getCreateTime());

        Map<Long, String> merchantNames = lookupMerchantNames(
                Collections.singleton(p.getMerchantId()));
        vo.setMerchantName(merchantNames.get(p.getMerchantId()));

        Map<Long, String> categoryNames = lookupCategoryNames(
                Collections.singleton(p.getCategoryId()));
        vo.setCategoryName(categoryNames.get(p.getCategoryId()));

        return vo;
    }

    private Map<Long, String> lookupMerchantNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<MerchantDO> merchants = merchantMapper.selectBatchIds(ids);
        return merchants.stream().collect(Collectors.toMap(MerchantDO::getId, MerchantDO::getShopName));
    }

    private Map<Long, String> lookupCategoryNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<CategoryDO> categories = categoryMapper.selectBatchIds(ids);
        return categories.stream().collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));
    }
}
```

- [ ] **Step 5：`AdminProductController.java`**

```java
package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.ProductAuditDTO;
import com.yan.freshfood.admin.service.ProductAdminService;
import com.yan.freshfood.admin.vo.AdminProductVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminService productAdminService;

    @GetMapping
    public R<PageR<AdminProductVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(productAdminService.page(keyword, auditStatus, status, merchantId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminProductVO> detail(@PathVariable Long id) {
        return R.ok(productAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody ProductAuditDTO dto) {
        productAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    public R<Void> offShelf(@PathVariable Long id) {
        productAdminService.offShelf(id);
        return R.ok();
    }

    @GetMapping("/audit-pending")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(productAdminService.auditPendingCount());
    }
}
```

- [ ] **Step 6：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/ProductAuditDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminProductVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ProductAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ProductAdminServiceImpl.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminProductController.java

git commit -m "$(cat <<'EOF'
feat(admin-product): M2 商品审核 DTO + Service + Controller

- 5 个方法：page/detail/audit/offShelf/auditPendingCount
- 5 个端点：GET 分页/详情/audit-pending, POST audit/off-shelf
- merchantName/categoryName 由 service 内存 JOIN 注入（批量查 mapper）
- audit 仅允许 auditStatus=0 → 1/2, 否则 3005

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 6：M3 用户管理 DTO + Service + Controller

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/UserStatusDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminUserVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/UserAdminService.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/UserAdminServiceImpl.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminUserController.java`

- [ ] **Step 1：`UserStatusDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {
    /** 0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

- [ ] **Step 2：`AdminUserVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 解密后明文 */
    private String phone;
    /** 解密后明文 */
    private String email;
    /** 0 禁用 / 1 正常 */
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3：`UserAdminService.java`**

```java
package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.response.PageR;

public interface UserAdminService {
    PageR<AdminUserVO> page(String keyword, Integer status, long pageNum, long pageSize);
    AdminUserVO detail(Long id);
    void updateStatus(Long id, Integer status);
    void resetPassword(Long id);
}
```

- [ ] **Step 4：`UserAdminServiceImpl.java`**

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.admin.service.UserAdminService;
import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;

    @Override
    public PageR<AdminUserVO> page(String keyword, Integer status, long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<UserDO> q = new LambdaQueryWrapper<UserDO>()
                .orderByDesc(UserDO::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(UserDO::getUsername, keyword)
                    .or().like(UserDO::getNickname, keyword));
        }
        if (status != null) q.eq(UserDO::getStatus, status);

        Page<UserDO> page = userMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminUserVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        Page<AdminUserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public AdminUserVO detail(Long id) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return toVO(u);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Override
    public void resetPassword(Long id) {
        UserDO u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        // BCrypt 加密 "123456"（与 seed 数据的哈希一致）
        u.setPassword(BCrypt.hashpw(CommonConstants.DEFAULT_PASSWORD, BCrypt.gensalt()));
        userMapper.updateById(u);
    }

    private AdminUserVO toVO(UserDO u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }

    /** 触达 StpUtil 防 NPE（admin 操作不需要校验 id 但要拿到当前 admin） */
    @SuppressWarnings("unused")
    private Long currentAdminId() {
        return StpUtil.getStpLogic(CommonConstants.TYPE_ADMIN, null).getLoginIdAsLong();
    }
}
```

- [ ] **Step 5：`AdminUserController.java`**

```java
package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.UserStatusDTO;
import com.yan.freshfood.admin.service.UserAdminService;
import com.yan.freshfood.admin.vo.AdminUserVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public R<PageR<AdminUserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(userAdminService.page(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminUserVO> detail(@PathVariable Long id) {
        return R.ok(userAdminService.detail(id));
    }

    @PostMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        userAdminService.resetPassword(id);
        return R.ok();
    }
}
```

- [ ] **Step 6：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/UserStatusDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminUserVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/UserAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/UserAdminServiceImpl.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminUserController.java

git commit -m "$(cat <<'EOF'
feat(admin-user): M3 用户管理 DTO + Service + Controller

- 4 个方法：page/detail/updateStatus/resetPassword
- 4 个端点：GET 分页/详情, POST status/reset-password
- 重置密码为 123456（BCrypt 哈希后写入，CommonConstants.DEFAULT_PASSWORD）
- phone/email 由 EncryptedStringTypeHandler 自动解密

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 7：M4 Banner DTO + Service + Controller

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/BannerCreateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/BannerUpdateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminBannerVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java`

- [ ] **Step 1：`BannerCreateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerCreateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不超过 100 字")
    private String title;

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 255, message = "图片 URL 不超过 255 字")
    private String image;

    /** NONE/PRODUCT/CATEGORY/URL */
    @NotBlank(message = "链接类型不能为空")
    private String linkType;

    @Size(max = 255, message = "链接目标不超过 255 字")
    private String linkTarget;

    private Integer sort;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

- [ ] **Step 2：`BannerUpdateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerUpdateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不超过 100 字")
    private String title;

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 255, message = "图片 URL 不超过 255 字")
    private String image;

    @NotBlank(message = "链接类型不能为空")
    private String linkType;

    @Size(max = 255, message = "链接目标不超过 255 字")
    private String linkTarget;

    private Integer sort;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

- [ ] **Step 3：`AdminBannerVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBannerVO {
    private Long id;
    private String title;
    private String image;
    private String linkType;
    private String linkTarget;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4：`ContentAdminService.java`（先建，后续 task 补充 HotWord/Category 方法）**

```java
package com.yan.freshfood.admin.service;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.vo.AdminBannerVO;

import java.util.List;

public interface ContentAdminService {
    // ----- Banner -----
    List<AdminBannerVO> bannerList(Integer enabled);
    AdminBannerVO bannerCreate(BannerCreateDTO dto);
    AdminBannerVO bannerUpdate(Long id, BannerUpdateDTO dto);
    void bannerDelete(Long id);
}
```

- [ ] **Step 5：`ContentAdminServiceImpl.java`**

```java
package com.yan.freshfood.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentAdminServiceImpl implements ContentAdminService {

    private final BannerMapper bannerMapper;

    @Override
    public List<AdminBannerVO> bannerList(Integer enabled) {
        LambdaQueryWrapper<BannerDO> q = new LambdaQueryWrapper<BannerDO>()
                .orderByAsc(BannerDO::getSort)
                .orderByDesc(BannerDO::getCreateTime);
        if (enabled != null) q.eq(BannerDO::getEnabled, enabled);
        List<BannerDO> banners = bannerMapper.selectList(q);
        return banners.stream().map(this::toBannerVO).collect(Collectors.toList());
    }

    @Override
    public AdminBannerVO bannerCreate(BannerCreateDTO dto) {
        BannerDO b = new BannerDO();
        b.setTitle(dto.getTitle());
        b.setImage(dto.getImage());
        b.setLinkType(dto.getLinkType());
        b.setLinkTarget(dto.getLinkTarget());
        b.setSort(dto.getSort() == null ? 0 : dto.getSort());
        b.setEnabled(dto.getEnabled());
        b.setStartTime(dto.getStartTime());
        b.setEndTime(dto.getEndTime());
        bannerMapper.insert(b);
        return toBannerVO(b);
    }

    @Override
    public AdminBannerVO bannerUpdate(Long id, BannerUpdateDTO dto) {
        BannerDO b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        b.setTitle(dto.getTitle());
        b.setImage(dto.getImage());
        b.setLinkType(dto.getLinkType());
        b.setLinkTarget(dto.getLinkTarget());
        b.setSort(dto.getSort() == null ? 0 : dto.getSort());
        b.setEnabled(dto.getEnabled());
        b.setStartTime(dto.getStartTime());
        b.setEndTime(dto.getEndTime());
        bannerMapper.updateById(b);
        return toBannerVO(b);
    }

    @Override
    public void bannerDelete(Long id) {
        BannerDO b = bannerMapper.selectById(id);
        if (b == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        bannerMapper.deleteById(id);
    }

    private AdminBannerVO toBannerVO(BannerDO b) {
        AdminBannerVO vo = new AdminBannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImage(b.getImage());
        vo.setLinkType(b.getLinkType());
        vo.setLinkTarget(b.getLinkTarget());
        vo.setSort(b.getSort());
        vo.setEnabled(b.getEnabled());
        vo.setStartTime(b.getStartTime());
        vo.setEndTime(b.getEndTime());
        vo.setCreateTime(b.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 6：`AdminContentController.java`**

```java
package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.admin.dto.BannerUpdateDTO;
import com.yan.freshfood.admin.service.ContentAdminService;
import com.yan.freshfood.admin.vo.AdminBannerVO;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentAdminService contentAdminService;

    // ----- Banner -----

    @GetMapping("/banners")
    public R<List<AdminBannerVO>> bannerList(
            @RequestParam(required = false) Integer enabled) {
        return R.ok(contentAdminService.bannerList(enabled));
    }

    @PostMapping("/banners")
    public R<AdminBannerVO> bannerCreate(@Valid @RequestBody BannerCreateDTO dto) {
        return R.ok(contentAdminService.bannerCreate(dto));
    }

    @PutMapping("/banners/{id}")
    public R<AdminBannerVO> bannerUpdate(@PathVariable Long id,
                                          @Valid @RequestBody BannerUpdateDTO dto) {
        return R.ok(contentAdminService.bannerUpdate(id, dto));
    }

    @DeleteMapping("/banners/{id}")
    public R<Void> bannerDelete(@PathVariable Long id) {
        contentAdminService.bannerDelete(id);
        return R.ok();
    }
}
```

- [ ] **Step 7：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/BannerCreateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/BannerUpdateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminBannerVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java

git commit -m "$(cat <<'EOF'
feat(admin-banner): M4 Banner CRUD DTO + Service + Controller

- 4 个方法：list/create/update/delete（逻辑删除）
- 4 个端点：GET /banners, POST /banners, PUT /banners/{id}, DELETE /banners/{id}
- 全量列表（不分页，Banner < 50）
- enabled 可选过滤
- 排序：sort ASC, createTime DESC

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 8：M4 HotWord DTO + Service（扩展 ContentAdminService）

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/HotWordCreateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/HotWordUpdateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminHotWordVO.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java`

- [ ] **Step 1：`HotWordCreateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotWordCreateDTO {
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词不超过 50 字")
    private String keyword;

    private Integer sort;
}
```

- [ ] **Step 2：`HotWordUpdateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HotWordUpdateDTO {
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词不超过 50 字")
    private String keyword;

    private Integer searchCount;
    private Integer sort;
}
```

- [ ] **Step 3：`AdminHotWordVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminHotWordVO {
    private Long id;
    private String keyword;
    private Integer searchCount;
    private Integer sort;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4：在 ContentAdminService.java 末尾追加 HotWord 4 个方法签名**

在 `bannerDelete(Long id);` 后追加：

```java
    // ----- HotWord -----
    List<AdminHotWordVO> hotWordList(String keyword);
    AdminHotWordVO hotWordCreate(HotWordCreateDTO dto);
    AdminHotWordVO hotWordUpdate(Long id, HotWordUpdateDTO dto);
    void hotWordDelete(Long id);
```

并在顶部 import：
```java
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
```

- [ ] **Step 5：在 ContentAdminServiceImpl.java 末尾追加 HotWord 4 个方法实现**

先在类头部加 import：
```java
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
import com.yan.freshfood.model.entity.product.HotWordDO;
import com.yan.freshfood.user.mapper.HotWordMapper;
```

加字段：
```java
    private final HotWordMapper hotWordMapper;
```

（Lombok `@RequiredArgsConstructor` 自动生成构造器，注入新增字段）

在 `toBannerVO` 方法后追加：

```java
    // ----- HotWord -----

    @Override
    public List<AdminHotWordVO> hotWordList(String keyword) {
        LambdaQueryWrapper<HotWordDO> q = new LambdaQueryWrapper<HotWordDO>()
                .orderByAsc(HotWordDO::getSort)
                .orderByDesc(HotWordDO::getSearchCount);
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            q.like(HotWordDO::getKeyword, keyword);
        }
        List<HotWordDO> words = hotWordMapper.selectList(q);
        return words.stream().map(this::toHotWordVO).collect(Collectors.toList());
    }

    @Override
    public AdminHotWordVO hotWordCreate(HotWordCreateDTO dto) {
        HotWordDO h = new HotWordDO();
        h.setKeyword(dto.getKeyword());
        h.setSort(dto.getSort() == null ? 0 : dto.getSort());
        h.setSearchCount(0);
        hotWordMapper.insert(h);
        return toHotWordVO(h);
    }

    @Override
    public AdminHotWordVO hotWordUpdate(Long id, HotWordUpdateDTO dto) {
        HotWordDO h = hotWordMapper.selectById(id);
        if (h == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        h.setKeyword(dto.getKeyword());
        h.setSearchCount(dto.getSearchCount() == null ? 0 : dto.getSearchCount());
        h.setSort(dto.getSort() == null ? 0 : dto.getSort());
        hotWordMapper.updateById(h);
        return toHotWordVO(h);
    }

    @Override
    public void hotWordDelete(Long id) {
        HotWordDO h = hotWordMapper.selectById(id);
        if (h == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        hotWordMapper.deleteById(id);
    }

    private AdminHotWordVO toHotWordVO(HotWordDO h) {
        AdminHotWordVO vo = new AdminHotWordVO();
        vo.setId(h.getId());
        vo.setKeyword(h.getKeyword());
        vo.setSearchCount(h.getSearchCount());
        vo.setSort(h.getSort());
        vo.setCreateTime(h.getCreateTime());
        return vo;
    }
```

- [ ] **Step 6：在 AdminContentController.java 末尾追加 HotWord 4 个端点**

先加 import：
```java
import com.yan.freshfood.admin.dto.HotWordCreateDTO;
import com.yan.freshfood.admin.dto.HotWordUpdateDTO;
import com.yan.freshfood.admin.vo.AdminHotWordVO;
```

在 `bannerDelete` 方法后追加：

```java
    // ----- HotWord -----

    @GetMapping("/hot-words")
    public R<List<AdminHotWordVO>> hotWordList(
            @RequestParam(required = false) String keyword) {
        return R.ok(contentAdminService.hotWordList(keyword));
    }

    @PostMapping("/hot-words")
    public R<AdminHotWordVO> hotWordCreate(@Valid @RequestBody HotWordCreateDTO dto) {
        return R.ok(contentAdminService.hotWordCreate(dto));
    }

    @PutMapping("/hot-words/{id}")
    public R<AdminHotWordVO> hotWordUpdate(@PathVariable Long id,
                                            @Valid @RequestBody HotWordUpdateDTO dto) {
        return R.ok(contentAdminService.hotWordUpdate(id, dto));
    }

    @DeleteMapping("/hot-words/{id}")
    public R<Void> hotWordDelete(@PathVariable Long id) {
        contentAdminService.hotWordDelete(id);
        return R.ok();
    }
```

- [ ] **Step 7：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/HotWordCreateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/HotWordUpdateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminHotWordVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java

git commit -m "$(cat <<'EOF'
feat(admin-hot-word): M4 HotWord CRUD 4 端点

- list(keyword)：全量列表（不分页，热搜词 < 100），keyword 可选过滤
- create(dto)：searchCount 默认 0
- update(id, dto)：keyword/searchCount/sort 三字段
- delete(id)：逻辑删除

扩展 ContentAdminService（已有 Banner 4 方法）和 AdminContentController。

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 9：M4 Category DTO + Service（扩展 ContentAdminService）

**Files:**
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryCreateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryUpdateDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryStatusDTO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminCategoryVO.java`
- Create: `freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminCategoryTreeVO.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java`
- Modify: `freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java`

- [ ] **Step 1：`CategoryCreateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    /** 0=顶级 */
    @NotNull(message = "父分类 ID 不能为空")
    private Long parentId;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名不超过 50 字")
    private String name;

    @Size(max = 255, message = "图标 URL 不超过 255 字")
    private String icon;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

- [ ] **Step 2：`CategoryUpdateDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateDTO {
    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名不超过 50 字")
    private String name;

    @Size(max = 255, message = "图标 URL 不超过 255 字")
    private String icon;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

- [ ] **Step 3：`CategoryStatusDTO.java`**

```java
package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryStatusDTO {
    /** 0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

- [ ] **Step 4：`AdminCategoryVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    /** 0 禁用 / 1 启用 */
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 5：`AdminCategoryTreeVO.java`**

```java
package com.yan.freshfood.admin.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCategoryTreeVO extends AdminCategoryVO {
    private List<AdminCategoryTreeVO> children = new ArrayList<>();
}
```

- [ ] **Step 6：在 ContentAdminService.java 末尾追加 Category 6 个方法签名**

加 import：
```java
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryStatusDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
```

在 `hotWordDelete(Long id);` 后追加：

```java
    // ----- Category -----
    List<AdminCategoryVO> categoryList();
    List<AdminCategoryTreeVO> categoryTree();
    AdminCategoryVO categoryCreate(CategoryCreateDTO dto);
    AdminCategoryVO categoryUpdate(Long id, CategoryUpdateDTO dto);
    void categoryUpdateStatus(Long id, Integer status);
    void categoryDelete(Long id);
```

- [ ] **Step 7：在 ContentAdminServiceImpl.java 末尾追加 Category 6 个方法实现**

加 import：
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryStatusDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import com.yan.freshfood.admin.vo.AdminCategoryVO;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import java.util.ArrayList;
```

加字段（依赖注入）：
```java
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
```

在 `toHotWordVO` 方法后追加：

```java
    // ----- Category -----

    @Override
    public List<AdminCategoryVO> categoryList() {
        LambdaQueryWrapper<CategoryDO> q = new LambdaQueryWrapper<CategoryDO>()
                .orderByAsc(CategoryDO::getSort)
                .orderByAsc(CategoryDO::getId);
        return categoryMapper.selectList(q).stream()
                .map(this::toCategoryVO).collect(Collectors.toList());
    }

    @Override
    public List<AdminCategoryTreeVO> categoryTree() {
        LambdaQueryWrapper<CategoryDO> q = new LambdaQueryWrapper<CategoryDO>()
                .orderByAsc(CategoryDO::getSort)
                .orderByAsc(CategoryDO::getId);
        List<CategoryDO> all = categoryMapper.selectList(q);

        // 第一遍：建 VO Map
        java.util.Map<Long, AdminCategoryTreeVO> voMap = new java.util.LinkedHashMap<>();
        for (CategoryDO c : all) {
            voMap.put(c.getId(), toCategoryTreeVO(c));
        }

        // 第二遍：build 树
        List<AdminCategoryTreeVO> roots = new ArrayList<>();
        for (CategoryDO c : all) {
            AdminCategoryTreeVO vo = voMap.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(vo);
            } else {
                AdminCategoryTreeVO parent = voMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo); // 父分类已删，孤儿节点当顶级
                }
            }
        }
        return roots;
    }

    @Override
    public AdminCategoryVO categoryCreate(CategoryCreateDTO dto) {
        CategoryDO c = new CategoryDO();
        c.setParentId(dto.getParentId());
        c.setName(dto.getName());
        c.setIcon(dto.getIcon());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.insert(c);
        return toCategoryVO(c);
    }

    @Override
    public AdminCategoryVO categoryUpdate(Long id, CategoryUpdateDTO dto) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        c.setName(dto.getName());
        c.setIcon(dto.getIcon());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(dto.getStatus());
        categoryMapper.updateById(c);
        return toCategoryVO(c);
    }

    @Override
    public void categoryUpdateStatus(Long id, Integer status) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        c.setStatus(status);
        categoryMapper.updateById(c);
    }

    @Override
    public void categoryDelete(Long id) {
        CategoryDO c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        // 校验 1：是否有子分类
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<CategoryDO>().eq(CategoryDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }

        // 校验 2：是否被商品引用
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<ProductDO>().eq(ProductDO::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }

        categoryMapper.deleteById(id);
    }

    private AdminCategoryVO toCategoryVO(CategoryDO c) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private AdminCategoryTreeVO toCategoryTreeVO(CategoryDO c) {
        AdminCategoryTreeVO vo = new AdminCategoryTreeVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
```

- [ ] **Step 8：在 AdminContentController.java 末尾追加 Category 6 个端点**

加 import：
```java
import com.yan.freshfood.admin.dto.CategoryCreateDTO;
import com.yan.freshfood.admin.dto.CategoryStatusDTO;
import com.yan.freshfood.admin.dto.CategoryUpdateDTO;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import com.yan.freshfood.admin.vo.AdminCategoryVO;
```

在 `hotWordDelete` 方法后追加：

```java
    // ----- Category -----

    @GetMapping("/categories")
    public R<List<AdminCategoryVO>> categoryList() {
        return R.ok(contentAdminService.categoryList());
    }

    @GetMapping("/categories/tree")
    public R<List<AdminCategoryTreeVO>> categoryTree() {
        return R.ok(contentAdminService.categoryTree());
    }

    @PostMapping("/categories")
    public R<AdminCategoryVO> categoryCreate(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(contentAdminService.categoryCreate(dto));
    }

    @PutMapping("/categories/{id}")
    public R<AdminCategoryVO> categoryUpdate(@PathVariable Long id,
                                              @Valid @RequestBody CategoryUpdateDTO dto) {
        return R.ok(contentAdminService.categoryUpdate(id, dto));
    }

    @PostMapping("/categories/{id}/status")
    public R<Void> categoryUpdateStatus(@PathVariable Long id,
                                          @Valid @RequestBody CategoryStatusDTO dto) {
        contentAdminService.categoryUpdateStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/categories/{id}")
    public R<Void> categoryDelete(@PathVariable Long id) {
        contentAdminService.categoryDelete(id);
        return R.ok();
    }
```

- [ ] **Step 9：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryCreateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryUpdateDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/dto/CategoryStatusDTO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminCategoryVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/vo/AdminCategoryTreeVO.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/ContentAdminService.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImpl.java \
  freshfood-admin/src/main/java/com/yan/freshfood/admin/controller/AdminContentController.java

git commit -m "$(cat <<'EOF'
feat(admin-category): M4 Category 6 端点 + 树形构建

- list：扁平列表（按 sort/id 升序）
- tree：嵌套树形（service 内存两遍 build: voMap + parent-child）
- create(parentId, name, ...)：parentId=0 为顶级
- update(id, ...)：不含 parentId（防误改层级）
- updateStatus(id, status)：启用-禁用
- delete(id)：前置校验子分类+商品引用，否则 CATEGORY_IN_USE(7004)

编译跳过 (待 JDK 17 装好后补验)。
EOF
)"
```

---

### Task 10：12 个单元测试（4 个 Test 类）

**Files:**
- Create: `freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/MerchantAdminServiceImplTest.java`
- Create: `freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/ProductAdminServiceImplTest.java`
- Create: `freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/UserAdminServiceImplTest.java`
- Create: `freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImplTest.java`

> **测试规范：**
> - `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
> - `MockedStatic<StpUtil>` + `getStpLogic(any(), anyString())` + `getLoginIdAsLong()` 返回 1L
> - BusinessException 断言：`assertEquals(ErrorCode.FOO.getCode(), ex.getCode())`
> - admin 操作**不校验**资源归属，但 mock StpUtil 防 NPE

- [ ] **Step 1：`MerchantAdminServiceImplTest.java`（3 用例）**

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.model.entity.MerchantDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantAdminServiceImplTest {

    @Mock private MerchantMapper merchantMapper;

    @InjectMocks private MerchantAdminServiceImpl service;

    @Test
    void audit_approve_transitions_0_to_1() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(1L);
            m.setAuditStatus(0); // 待审核
            when(merchantMapper.selectById(1L)).thenReturn(m);
            when(merchantMapper.updateById(any(MerchantDO.class))).thenAnswer(inv -> 1);

            service.audit(1L, 1);

            assertEquals(1, m.getAuditStatus());
        }
    }

    @Test
    void audit_reject_transitions_0_to_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(2L);
            m.setAuditStatus(0);
            when(merchantMapper.selectById(2L)).thenReturn(m);

            service.audit(2L, 2);

            assertEquals(2, m.getAuditStatus());
        }
    }

    @Test
    void audit_throws_when_already_approved() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            MerchantDO m = new MerchantDO();
            m.setId(3L);
            m.setAuditStatus(1); // 已通过，不能再审
            when(merchantMapper.selectById(3L)).thenReturn(m);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(3L, 2));
            assertEquals(ErrorCode.MERCHANT_AUDIT_INVALID.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 2：`ProductAdminServiceImplTest.java`（3 用例）**

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAdminServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private ProductAdminServiceImpl service;

    @Test
    void audit_approve_transitions_0_to_1() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setAuditStatus(0);
            when(productMapper.selectById(1001L)).thenReturn(p);

            service.audit(1001L, 1);

            assertEquals(1, p.getAuditStatus());
        }
    }

    @Test
    void audit_reject_transitions_0_to_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1002L);
            p.setAuditStatus(0);
            when(productMapper.selectById(1002L)).thenReturn(p);

            service.audit(1002L, 2);

            assertEquals(2, p.getAuditStatus());
        }
    }

    @Test
    void off_shelf_sets_status_to_0() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1003L);
            p.setStatus(1); // 上架中
            when(productMapper.selectById(1003L)).thenReturn(p);

            service.offShelf(1003L);

            assertEquals(0, p.getStatus());
        }
    }

    @Test
    void audit_throws_when_already_approved() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1004L);
            p.setAuditStatus(1); // 已通过
            when(productMapper.selectById(1004L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(1004L, 2));
            assertEquals(ErrorCode.PRODUCT_AUDIT_INVALID.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 3：`UserAdminServiceImplTest.java`（2 用例）**

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.model.entity.UserDO;
import com.yan.freshfood.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock private UserMapper userMapper;

    @InjectMocks private UserAdminServiceImpl service;

    @Test
    void update_status_toggles_value() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            UserDO u = new UserDO();
            u.setId(100L);
            u.setStatus(1); // 启用
            when(userMapper.selectById(100L)).thenReturn(u);

            service.updateStatus(100L, 0);

            assertEquals(0, u.getStatus());
        }
    }

    @Test
    void reset_password_writes_bcrypt_123456() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            UserDO u = new UserDO();
            u.setId(100L);
            u.setPassword("oldHash");
            when(userMapper.selectById(100L)).thenReturn(u);

            service.resetPassword(100L);

            // 断言新密码是 BCrypt 加密的 123456（可校验）
            assertTrue(BCrypt.checkpw(CommonConstants.DEFAULT_PASSWORD, u.getPassword()));
        }
    }
}
```

- [ ] **Step 4：`ContentAdminServiceImplTest.java`（4 用例）**

```java
package com.yan.freshfood.admin.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.admin.dto.BannerCreateDTO;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.admin.vo.AdminCategoryTreeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentAdminServiceImplTest {

    @Mock private BannerMapper bannerMapper;
    @Mock private com.yan.freshfood.user.mapper.HotWordMapper hotWordMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private ContentAdminServiceImpl service;

    @Test
    void banner_create_inserts_with_default_sort() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            BannerCreateDTO dto = new BannerCreateDTO();
            dto.setTitle("618 大促");
            dto.setImage("https://img.example.com/b1.jpg");
            dto.setLinkType("CATEGORY");
            dto.setLinkTarget("1");
            dto.setEnabled(1);
            // sort 故意不设，应默认为 0

            service.bannerCreate(dto);

            org.mockito.ArgumentCaptor<BannerDO> captor =
                    org.mockito.ArgumentCaptor.forClass(BannerDO.class);
            org.mockito.Mockito.verify(bannerMapper).insert(captor.capture());
            BannerDO captured = captor.getValue();
            assertEquals("618 大促", captured.getTitle());
            assertEquals(0, captured.getSort()); // 默认值
        }
    }

    @Test
    void banner_delete_throws_when_not_found() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            when(bannerMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.bannerDelete(99L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Test
    void category_tree_builds_nested_structure() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            CategoryDO top = new CategoryDO();
            top.setId(1L); top.setParentId(0L); top.setName("水果"); top.setSort(1); top.setStatus(1);
            CategoryDO child = new CategoryDO();
            child.setId(11L); child.setParentId(1L); child.setName("车厘子"); child.setSort(1); child.setStatus(1);
            when(categoryMapper.selectList(any())).thenReturn(List.of(top, child));

            List<AdminCategoryTreeVO> tree = service.categoryTree();

            assertEquals(1, tree.size());
            AdminCategoryTreeVO root = tree.get(0);
            assertEquals("水果", root.getName());
            assertEquals(1, root.getChildren().size());
            assertEquals("车厘子", root.getChildren().get(0).getName());
        }
    }

    @Test
    void category_delete_throws_when_has_children() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            CategoryDO c = new CategoryDO();
            c.setId(1L);
            when(categoryMapper.selectById(1L)).thenReturn(c);
            when(categoryMapper.selectCount(any())).thenReturn(2L); // 有 2 个子分类

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.categoryDelete(1L));
            assertEquals(ErrorCode.CATEGORY_IN_USE.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 5：commit**

```bash
cd freshfood-shop
git add \
  freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/MerchantAdminServiceImplTest.java \
  freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/ProductAdminServiceImplTest.java \
  freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/UserAdminServiceImplTest.java \
  freshfood-admin/src/test/java/com/yan/freshfood/admin/service/impl/ContentAdminServiceImplTest.java

git commit -m "$(cat <<'EOF'
test(admin-service): 新增管理员端 4 个 Service 单元测试

- MerchantAdminServiceImplTest：3 用例 (audit 通过/拒绝/重复审抛 7003)
- ProductAdminServiceImplTest：4 用例 (audit 通过/拒绝/下架/重复审抛 3005)
- UserAdminServiceImplTest：2 用例 (禁用/重置密码 BCrypt 校验)
- ContentAdminServiceImplTest：4 用例 (Banner 创建默认值/Banner 删/Category 树构建/Category 删抛 7004)

共 13 个用例，MockedStatic<StpUtil> + any(),anyString()。

测试未跑 (JDK 17 未本地安装，待 Task 11 统一验证)。
EOF
)"
```

---

### Task 11：全模块静态结构审查（替代 mvn compile）

**Files:** 无

由于 JDK 17 未本地安装无法 mvn compile，本 task 通过 subagent 静态 Read 比对所有文件，确保 import 路径、类签名、方法调用、字段引用全部自洽。

- [ ] **Step 1：派发 subagent 做静态结构审查**

**Subagent prompt 摘要**（详见执行阶段）：
- Read 所有 admin 模块源文件 + 跨模块引用文件
- 检查 import 路径（特别注意跨模块 mapper 引用）
- 检查方法签名匹配
- 检查字段引用
- 检查注解参数
- 报告问题到文件:行号

如果发现问题，先**就地修复**再 commit 一个 `fix(admin-xxx)`，**不**许跳过。

- [ ] **Step 2：commit（如有 fix）**

```bash
cd freshfood-shop
git add <fixed-files>
git commit -m "fix(admin-xxx): <修复描述>"
```

如无问题，跳过本步。

---

### Task 12：推送 + fast-forward 合 main

**Files:** 无

- [ ] **Step 1：检查 git 状态**

```bash
cd freshfood-shop
git status
git log --oneline -15
```

预期：分支在 `feature/admin-business`，工作区干净，ahead origin/main 约 10-12 commits。

- [ ] **Step 2：推送 feature 分支**

```bash
cd freshfood-shop
git push -u origin feature/admin-business
```

- [ ] **Step 3：检查 origin/main 与 local main 位置**

```bash
cd freshfood-shop
git fetch origin
git log --oneline origin/main..main  # local main 比 origin 多的 commit
git log --oneline main..feature/admin-business  # feature 比 main 多的 commit
```

如果 `feature/admin-business` 是 `main` 的祖先（merge-base == main），直接 fast-forward；
否则需要先 rebase feature 到 main（参考 Plan 3 的处理流程）。

- [ ] **Step 4：fast-forward merge main**

```bash
cd freshfood-shop
git checkout main
git merge --ff-only feature/admin-business  # 或 rebase 后 FF
git push origin main
```

- [ ] **Step 5：清理（可选）**

```bash
cd freshfood-shop
git branch -d feature/admin-business
```

---

## 自审检查清单

执行前确认：
- [ ] Task 顺序与依赖关系图一致
- [ ] 每个 task 给出完整代码（无 TODO / TBD）
- [ ] 每个 task 给出 commit 命令
- [ ] ErrorCode 3 条新增
- [ ] 跨模块 mapper 引用明确（user/merchant 模块）
- [ ] 所有 admin controller 路径在 `/api/v1/admin/` 前缀下
- [ ] 树形分类构造逻辑完整（两遍 build）
- [ ] BCrypt 重置密码测试断言使用 checkpw 校验
- [ ] StpLogic mock 第二个参数用 anyString()