# 计划 3：商家端业务 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在计划 1（商家登录）+ 计划 2（12 业务表 + DO）基础上，实现商家端 4 个核心模块 15 个接口（店铺信息 / 商品管理 / SKU 库存 / 订单发货），全部通过 Sa-Token 商家端 StpLogic 鉴权，严格按 `merchantId` 数据隔离。

**Architecture:** 沿用 4 层架构（Controller / Service / Mapper）+ DO/DTO/VO 分离。所有接口位于 `/api/v1/merchant/...`，仅复用 Plan 2 已有的 12 张表与 DOs，**不新增表或列**。商家侧获取当前登录 ID 必须走 `StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null).getLoginIdAsLong()`。每个"按 id 加载 + 改/发"的链路必须严格校验 `entity.merchantId == currentMerchantId`，不匹配统一抛 `NOT_FOUND(1004)`。

**Tech Stack:** MyBatis-Plus 3.5.9（LambdaQueryWrapper + `Page`）、Sa-Token 1.37.0（多 StpLogic）、Hutool 5.8.27（`BeanUtil.copyProperties`，仅必要场景）、Spring Validation、Plan 1 已搭好的 `EncryptedStringTypeHandler`（contactName / contactPhone 自动加解密）、Plan 1 已搭好的 `R`/`PageR` 响应。

---

## 一、文件结构总览

实施完成后 `freshfood-merchant/` 应新增（已存在文件保留）：

```
freshfood-merchant/src/main/java/com/yan/freshfood/merchant/
├── controller/
│   ├── MerchantAuthController.java          # 已有，仅 login/logout
│   ├── MerchantProfileController.java       # 3.1
│   ├── MerchantProductController.java       # 3.2
│   ├── MerchantSkuController.java           # 3.3
│   └── MerchantOrderController.java         # 3.4
├── service/
│   ├── MerchantAuthService.java             # 已有
│   ├── MerchantProfileService.java          # 3.1
│   ├── MerchantProductService.java          # 3.2
│   ├── MerchantSkuService.java              # 3.3
│   └── MerchantOrderService.java            # 3.4
├── service/impl/
│   ├── MerchantAuthServiceImpl.java         # 已有
│   ├── MerchantProfileServiceImpl.java      # 3.1
│   ├── MerchantProductServiceImpl.java      # 3.2
│   ├── MerchantSkuServiceImpl.java          # 3.3
│   └── MerchantOrderServiceImpl.java        # 3.4
├── dto/
│   ├── MerchantLoginDTO.java                # 已有
│   ├── MerchantUpdateDTO.java               # 3.1
│   ├── ProductCreateDTO.java                # 3.2
│   ├── ProductUpdateDTO.java                # 3.2
│   ├── SkuCreateDTO.java                    # 3.3
│   └── SkuUpdateDTO.java                    # 3.3
├── vo/
│   ├── MerchantLoginVO.java                 # 已有
│   ├── MerchantVO.java                      # 已有
│   ├── SkuVO.java                           # 3.3（merchant 内副本，避免跨模块依赖）
│   ├── MerchantProductVO.java               # 3.2
│   ├── MerchantOrderVO.java                 # 3.4
│   └── MerchantOrderItemVO.java             # 3.4
└── mapper/
    ├── MerchantMapper.java                  # 已有
    ├── ProductMapper.java                   # 3.2
    ├── CategoryMapper.java                  # 3.2
    ├── SkuMapper.java                       # 3.3
    ├── OrderMapper.java                     # 3.4
    └── OrderItemMapper.java                 # 3.4

freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/
├── MerchantProductServiceImplTest.java      # 任务 12
├── MerchantSkuServiceImplTest.java          # 任务 12
└── MerchantOrderServiceImplTest.java        # 任务 12

freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java   # 任务 2 加 2 条
```

> 复用 `freshfood-model` 已有的 `MerchantDO`（含 `contactName/contactPhone` 加密）/ `ProductDO` / `SkuDO` / `OrderDO` / `OrderItemDO` / `CategoryDO`。**零新增表 / 零新增列。**

---

## 二、关键约定（影响后续所有任务）

### 2.1 登录态与当前商家

```java
// 在任何 Service 内：
Long currentMerchantId = StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null)
                              .getLoginIdAsLong();
```

禁止用 `StpUtil.getLoginIdAsLong()`（默认 user 端）。

### 2.2 数据隔离规则（**核心安全约束**）

| 场景 | 必须过滤 |
|---|---|
| 列 / 查 / 改 / 上下架 商品 | `WHERE merchant_id = currentMerchantId` |
| 列 / 查 / 改 / 删 SKU | 通过 product 间接校验：`sku → product.merchantId == currentMerchantId` |
| 列 / 查 / 改订单（含发货） | `WHERE merchant_id = currentMerchantId` |

- 按 id 加载实体后，**第一件事**就是比对 `entity.merchantId == currentMerchantId`；不匹配 → 直接抛 `BusinessException(ErrorCode.NOT_FOUND)`，**不要抛出更具体的错误（避免泄露资源存在性）**。
- 不允许用 `BeanUtil.copyProperties` 整段拷贝用户可控字段 → 改商品时**严格按字段 set**，杜绝改 `auditStatus` / `status` / `sales` / `rating` / `merchantId`。

### 2.3 字段加密

- `MerchantDO.contactName / contactPhone` 已配 `EncryptedStringTypeHandler`（计划 1 已完成）
- 读写走 Service 内的 `MerchantDO → MerchantVO.toVO(m)`，**不要在 Service 外解密**
- 订单里的 `addressSnapshot` 是 JSON 字符串，已被计划 2 OrderServiceImpl 加密后落库；商家端**不解密 buyer 姓名 / 手机号**（spec 最终决策）

### 2.4 新增错误码（任务 2 引入）

| 码 | 名称 | 文案 |
|---|---|---|
| 3003 | PRODUCT_PENDING_AUDIT | 商品待审核，不可上架 |
| 3004 | SKU_HAS_SALES | SKU 已有销量，不可删除 |

加在 `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`，与 3001/3002 同一区段，**单独 commit**。

### 2.5 状态机

| 资源 | 状态 | 说明 |
|---|---|---|
| `product.status` | 0=下架 / 1=上架 | on-shelf 前置 `audit_status == 1`（已通过） |
| `product.audit_status` | 0=待审 / 1=通过 / 2=拒绝 | merchant 创建商品时**强制写 0**（待审） |
| `order.status` | 1待付 / 2待发 / 3待收 / 4待评 / 5完成 / 6售后 / 7取消 | merchant 仅触发 2→3 发货 |

### 2.6 MerchantVO.toVO 复用

`MerchantAuthServiceImpl` 已有私有方法 `toVO(MerchantDO m)` —— merchant profile 模块**复用同样的转换逻辑**（必要时提到公共静态方法或直接复制 5 行代码；本计划走"复制 5 行"简单方案，避免过早抽象）。

### 2.7 模块独立 VO

- `SkuVO`：Plan 2 在 `com.yan.freshfood.user.vo` 包，**merchant 模块不依赖 user 模块**，故在 merchant 模块下**复制**一份（字段完全一致，注释指明与 user 版同语义）。
- `MerchantProductVO`：merchant 列表专用，含 `priceRange` / `stock` / `categoryName` 等商家侧字段。
- `MerchantOrderVO`：merchant 订单专用，含 `addressSnapshot` 原 JSON 字符串 + 反序列化的 `items`。

### 2.8 分页与异常断言

- 列表接口统一用 `Page<ProductDO>` / `Page<OrderDO>`，封 `PageR<VO>` 返回；`pageNum < 1` 静默修正为 1，`pageSize` 限制上限 100（计划 2 风格，必要时直接复制该逻辑的 Service 入口判定）。
- 测试中所有对 `BusinessException` 的断言用 `ex.getCode()`（仅 `Lombok @Getter` 暴露的 getter），**不要**写 `getErrorCode()`（计划 2 踩过此坑）。

### 2.9 git commit 风格

- `type` 用英文（`feat` / `fix` / `test` / `chore` / `docs`）
- `subject` + `body` 用中文（用户偏好，已落到 memory）
- `git add <specific-files>`，**不允许 `git add -A` / `.`**
- 不可逆操作（`reset --hard` / 强推 / 删分支）必须先停下来问

---

## 三、任务清单

### Task 1：建分支 + 创建 5 个 Mapper 接口

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/ProductMapper.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/CategoryMapper.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/SkuMapper.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/OrderMapper.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/OrderItemMapper.java`

- [ ] **Step 1：建并切到 `feature/merchant-business` 分支**

```bash
cd freshfood-shop
git fetch origin
git checkout -b feature/merchant-business origin/main
git status
```

预期：`On branch feature/merchant-business`，`nothing to commit, working tree clean`。

- [ ] **Step 2：5 个 Mapper 接口（全部空，继承 MyBatis-Plus `BaseMapper`）**

每个文件结构相同（只有类型不同），以 `ProductMapper.java` 为例：

```java
package com.yan.freshfood.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {
}
```

替换规则：
- `CategoryMapper.java` → `CategoryDO`，包 `com.yan.freshfood.model.entity.product.CategoryDO`
- `SkuMapper.java` → `SkuDO`，包 `com.yan.freshfood.model.entity.product.SkuDO`
- `OrderMapper.java` → `OrderDO`，包 `com.yan.freshfood.model.entity.trade.OrderDO`
- `OrderItemMapper.java` → `OrderItemDO`，包 `com.yan.freshfood.model.entity.trade.OrderItemDO`

- [ ] **Step 3：编译该模块确认依赖 OK**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。失败多半是 model 模块缺字段，按报错补 DO 字段即可（不该有——计划 2 已建好）。

- [ ] **Step 4：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/ProductMapper.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/CategoryMapper.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/SkuMapper.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/OrderMapper.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/mapper/OrderItemMapper.java
git commit -m "$(cat <<'EOF'
feat(merchant-mapper): 新增 5 个 MyBatis-Plus Mapper 接口

为商家端后续 4 个模块预创建空 Mapper，全部继承 BaseMapper：
- ProductMapper / CategoryMapper：商品模块
- SkuMapper：SKU 库存模块
- OrderMapper / OrderItemMapper：订单发货模块

复用 freshfood-model 已有的 DO（ProductDO/SkuDO/OrderDO/OrderItemDO/CategoryDO），
本计划不新增表与列。
EOF
)"
```

---

### Task 2：ErrorCode 新增 2 条错误码

**Files:**
- Modify: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`

- [ ] **Step 1：在 3002 与 4001 之间插入 2 条新枚举**

打开 `ErrorCode.java`，找到这一段：

```java
    PRODUCT_OFF_SHELF(3001, "商品已下架"),
    STOCK_NOT_ENOUGH(3002, "库存不足"),

    ORDER_STATUS_INVALID(4001, "订单状态不允许该操作"),
```

替换为：

```java
    PRODUCT_OFF_SHELF(3001, "商品已下架"),
    STOCK_NOT_ENOUGH(3002, "库存不足"),
    PRODUCT_PENDING_AUDIT(3003, "商品待审核，不可上架"),
    SKU_HAS_SALES(3004, "SKU 已有销量，不可删除"),

    ORDER_STATUS_INVALID(4001, "订单状态不允许该操作"),
```

- [ ] **Step 2：编译 common 模块**

```bash
cd freshfood-shop
mvn -pl freshfood-common compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 3：commit**

```bash
cd freshfood-shop
git add freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java
git commit -m "$(cat <<'EOF'
feat(common-errorcode): 新增商家端 2 条错误码

- 3003 PRODUCT_PENDING_AUDIT：商品待审核，不可上架
- 3004 SKU_HAS_SALES：SKU 已有销量，不可删除

插入位置在 3002 与 4001 之间，保持 3xxx 为商品相关、4xxx 为订单相关的区段约定。
EOF
)"
```

---

### Task 3：merchant VOs - SkuVO + MerchantProductVO + MerchantOrderVO + MerchantOrderItemVO

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/SkuVO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantProductVO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantOrderVO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantOrderItemVO.java`

- [ ] **Step 1：`SkuVO.java`**（字段与 Plan 2 user 版完全一致，注释注明同语义）

```java
package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家端 SKU 返回。
 * 字段语义与 freshfood-user.vo.SkuVO 一致；为避免 merchant 依赖 user 模块，
 * 本类复制一份独立维护。
 */
@Data
public class SkuVO {
    private Long id;
    private String spec;
    /** 价格：String, 避免前端精度丢失 */
    private String price;
    private Integer stock;
    private Integer sales;
    private String image;
}
```

- [ ] **Step 2：`MerchantOrderItemVO.java`**（订单内嵌明细）

```java
package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家视角的订单明细。
 */
@Data
public class MerchantOrderItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    /** 快照价, String */
    private String price;
    private Integer quantity;
}
```

- [ ] **Step 3：`MerchantProductVO.java`**（商品列表 / 详情共用简化版）

```java
package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家端商品视图（列表 + 简版详情）。
 * 价格区间从 SKU 聚合；库存为该商品所有 SKU 之和。
 */
@Data
public class MerchantProductVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String mainImage;
    private String description;
    private String origin;
    /** 价格区间，如 "59.90~109.00"；无 SKU 时为 null */
    private String priceRange;
    /** 总库存（所有 SKU stock 之和） */
    private Integer stock;
    private Integer sales;
    private Integer auditStatus;
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4：`MerchantOrderVO.java`**（订单列表 / 详情）

```java
package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantOrderVO {
    private Long id;
    private String orderNo;
    private Integer status;
    private String statusText;
    private String totalAmount;
    private String payableAmount;
    /** 内嵌明细 */
    private List<MerchantOrderItemVO> items;
    /**
     * 订单创建时的地址快照 JSON 字符串（不解密 buyer 姓名 / 手机号）。
     * 前端直接字符串展示即可。
     */
    private String addressSnapshot;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
}
```

- [ ] **Step 5：编译 merchant 模块**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 6：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/SkuVO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantOrderItemVO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantProductVO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/vo/MerchantOrderVO.java
git commit -m "$(cat <<'EOF'
feat(merchant-vo): 新增商家端 4 个 VO

- SkuVO：字段同 user 版，merchant 模块独立副本避免跨模块依赖
- MerchantOrderItemVO：商家视角订单明细
- MerchantProductVO：商品列表/简版详情，含 priceRange 与 stock 聚合
- MerchantOrderVO：含内嵌 items 与原始 addressSnapshot JSON 字符串
EOF
)"
```

---

### Task 4：M1 - Profile DTO + Service + Controller (2 接口)

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/MerchantUpdateDTO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantProfileService.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantProfileServiceImpl.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantProfileController.java`

- [ ] **Step 1：`MerchantUpdateDTO.java`**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantUpdateDTO {
    @NotBlank(message = "店铺名不能为空")
    @Size(max = 50, message = "店铺名不超过 50 字")
    private String shopName;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Size(max = 500, message = "logo URL 不超过 500 字")
    private String logo;
}
```

- [ ] **Step 2：`MerchantProfileService.java`**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.vo.MerchantVO;

public interface MerchantProfileService {
    MerchantVO getProfile();
    MerchantVO updateProfile(MerchantUpdateDTO dto);
}
```

- [ ] **Step 3：`MerchantProfileServiceImpl.java`**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.mapper.MerchantMapper;
import com.yan.freshfood.merchant.service.MerchantProfileService;
import com.yan.freshfood.merchant.vo.MerchantVO;
import com.yan.freshfood.model.entity.MerchantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantProfileServiceImpl implements MerchantProfileService {

    private final MerchantMapper merchantMapper;

    @Override
    public MerchantVO getProfile() {
        return toVO(loadCurrent());
    }

    @Override
    public MerchantVO updateProfile(MerchantUpdateDTO dto) {
        MerchantDO m = loadCurrent();
        m.setShopName(dto.getShopName());
        m.setContactName(dto.getContactName());
        m.setContactPhone(dto.getContactPhone());
        m.setLogo(dto.getLogo());
        merchantMapper.updateById(m);
        return toVO(m);
    }

    private MerchantDO loadCurrent() {
        Long mid = StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null).getLoginIdAsLong();
        MerchantDO m = merchantMapper.selectById(mid);
        if (m == null) throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        return m;
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

- [ ] **Step 4：`MerchantProfileController.java`**

```java
package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProfileService;
import com.yan.freshfood.merchant.vo.MerchantVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantProfileService merchantProfileService;

    @GetMapping
    public R<MerchantVO> get() {
        return R.ok(merchantProfileService.getProfile());
    }

    @PutMapping
    public R<MerchantVO> update(@Valid @RequestBody MerchantUpdateDTO dto) {
        return R.ok(merchantProfileService.updateProfile(dto));
    }
}
```

- [ ] **Step 5：编译验证**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 6：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/MerchantUpdateDTO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantProfileService.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantProfileServiceImpl.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantProfileController.java
git commit -m "$(cat <<'EOF'
feat(merchant-profile): 实现店铺信息模块 2 个接口

- GET /api/v1/merchant/profile：当前登录商家完整信息
- PUT /api/v1/merchant/profile：更新店铺名/logo/联系人(加密)

DTO 校验：shopName ≤50 / contactName 必填 / contactPhone 11 位手机号 / logo URL ≤500
Service 内 toVO 私有方法（5 行）与 MerchantAuthServiceImpl 重复；后续如再复用再抽。
EOF
)"
```

---

### Task 5：M2 - Product DTOs (ProductCreateDTO + ProductUpdateDTO)

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/ProductCreateDTO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/ProductUpdateDTO.java`

- [ ] **Step 1：`ProductCreateDTO.java`**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCreateDTO {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不超过 100 字")
    private String name;

    @NotNull(message = "分类 id 不能为空")
    private Long categoryId;

    @NotBlank(message = "主图不能为空")
    private String mainImage;

    @Size(max = 2000, message = "描述不超过 2000 字")
    private String description;

    @Size(max = 50, message = "产地不超过 50 字")
    private String origin;
}
```

- [ ] **Step 2：`ProductUpdateDTO.java`**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新商品基本资料。
 * 状态 / 审核 / 销量 / 评分 / 所属商家 均不允许改动，单独接口处理。
 */
@Data
public class ProductUpdateDTO {
    @NotNull(message = "商品 id 不能为空")
    private Long id;

    @Size(max = 100, message = "商品名称不超过 100 字")
    private String name;

    @NotNull(message = "分类 id 不能为空")
    private Long categoryId;

    private String mainImage;

    @Size(max = 2000, message = "描述不超过 2000 字")
    private String description;

    @Size(max = 50, message = "产地不超过 50 字")
    private String origin;
}
```

- [ ] **Step 3：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 4：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/ProductCreateDTO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/ProductUpdateDTO.java
git commit -m "$(cat <<'EOF'
feat(merchant-product-dto): 新增商品创建/更新 DTO

- ProductCreateDTO：name / categoryId / mainImage 必填
- ProductUpdateDTO：可更新 name/categoryId/mainImage/description/origin，
  不含 status / auditStatus / sales / rating / merchantId（这些字段不允许商户改）
EOF
)"
```

---

### Task 6：M2 - Product Service（6 方法）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantProductService.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantProductServiceImpl.java`

- [ ] **Step 1：`MerchantProductService.java`**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.vo.MerchantProductVO;

import java.util.List;

public interface MerchantProductService {
    PageR<MerchantProductVO> page(Integer status, long pageNum, long pageSize);
    MerchantProductVO detail(Long id);
    MerchantProductVO create(ProductCreateDTO dto);
    MerchantProductVO update(ProductUpdateDTO dto);
    void onShelf(Long id);
    void offShelf(Long id);
}
```

- [ ] **Step 2：`MerchantProductServiceImpl.java`**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.merchant.service.MerchantProductService;
import com.yan.freshfood.merchant.vo.MerchantProductVO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantProductServiceImpl implements MerchantProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;

    @Override
    public PageR<MerchantProductVO> page(Integer status, long pageNum, long pageSize) {
        Long mid = currentMerchantId();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<ProductDO> q = new LambdaQueryWrapper<ProductDO>()
                .eq(ProductDO::getMerchantId, mid)
                .orderByDesc(ProductDO::getCreateTime);
        if (status != null) q.eq(ProductDO::getStatus, status);

        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantProductVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        // 注意 PageR.of 接受 IPage<T>，这里我们转换 records 后保留分页元数据
        Page<MerchantProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public MerchantProductVO detail(Long id) {
        ProductDO p = loadAndCheck(id);
        return toVO(p);
    }

    @Override
    public MerchantProductVO create(ProductCreateDTO dto) {
        Long mid = currentMerchantId();
        CategoryDO cat = categoryMapper.selectById(dto.getCategoryId());
        if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        ProductDO p = new ProductDO();
        p.setMerchantId(mid);
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setMainImage(dto.getMainImage());
        p.setDescription(dto.getDescription());
        p.setOrigin(dto.getOrigin());
        // 默认值（注意 merchantId 已 set）——不允许 BeanUtil.copyProperties
        p.setAuditStatus(0);   // 待审
        p.setStatus(0);        // 下架
        p.setSales(0);
        p.setRating(new BigDecimal("5.00"));
        productMapper.insert(p);
        return toVO(p);
    }

    @Override
    public MerchantProductVO update(ProductUpdateDTO dto) {
        ProductDO p = loadAndCheck(dto.getId());
        if (dto.getName() != null && !dto.getName().isBlank()) p.setName(dto.getName());
        if (dto.getCategoryId() != null) {
            CategoryDO cat = categoryMapper.selectById(dto.getCategoryId());
            if (cat == null) throw new BusinessException(ErrorCode.NOT_FOUND);
            p.setCategoryId(dto.getCategoryId());
        }
        if (dto.getMainImage() != null) p.setMainImage(dto.getMainImage());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getOrigin() != null) p.setOrigin(dto.getOrigin());
        // 显式不修改 auditStatus / status / sales / rating / merchantId / createTime
        productMapper.updateById(p);
        return toVO(p);
    }

    @Override
    public void onShelf(Long id) {
        ProductDO p = loadAndCheck(id);
        if (p.getAuditStatus() == null || p.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_PENDING_AUDIT);
        }
        p.setStatus(1);
        productMapper.updateById(p);
    }

    @Override
    public void offShelf(Long id) {
        ProductDO p = loadAndCheck(id);
        p.setStatus(0);
        productMapper.updateById(p);
    }

    /** 加载 + merchantId 校验；不匹配统一抛 NOT_FOUND，避免泄露资源存在性 */
    private ProductDO loadAndCheck(Long id) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(id);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return p;
    }

    private MerchantProductVO toVO(ProductDO p) {
        MerchantProductVO vo = new MerchantProductVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCategoryId(p.getCategoryId());
        CategoryDO cat = categoryMapper.selectById(p.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());
        vo.setMainImage(p.getMainImage());
        vo.setDescription(p.getDescription());
        vo.setOrigin(p.getOrigin());

        // 价格区间 + 库存聚合
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>().eq(SkuDO::getProductId, p.getId()));
        if (skus != null && !skus.isEmpty()) {
            BigDecimal min = skus.stream().map(SkuDO::getPrice).min(BigDecimal::compareTo).orElse(null);
            BigDecimal max = skus.stream().map(SkuDO::getPrice).max(BigDecimal::compareTo).orElse(null);
            if (min != null && max != null) {
                vo.setPriceRange(min.compareTo(max) == 0
                        ? min.toPlainString()
                        : min.toPlainString() + "~" + max.toPlainString());
            }
            int totalStock = skus.stream().mapToInt(s -> s.getStock() == null ? 0 : s.getStock()).sum();
            vo.setStock(totalStock);
        }
        vo.setSales(p.getSales());
        vo.setAuditStatus(p.getAuditStatus());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private Long currentMerchantId() {
        return StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null).getLoginIdAsLong();
    }
}
```

- [ ] **Step 3：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。常见错：缺 `Page` 包导入 / `PageR.of` 签名（参 Plan 2 OrderServiceImpl 一致用法）。

- [ ] **Step 4：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantProductService.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantProductServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(merchant-product-service): 商品管理 Service 6 个方法

- page(status, pageNum, pageSize)：分页（自动过滤 status 空 → 全量）
- detail(id)：按 id 加载并校验 merchantId
- create(dto)：默认 auditStatus=0 待审, status=0 下架, sales=0, rating=5.00
- update(dto)：仅修改 name/categoryId/mainImage/description/origin 五个字段
- onShelf(id)：前置 auditStatus==1 通过, 否则抛 PRODUCT_PENDING_AUDIT(3003)
- offShelf(id)：无条件下架

private loadAndCheck(id) 是数据隔离的唯一入口；merchantId 不匹配 → 统一 NOT_FOUND(1004)。
聚合价格区间、库存、Sku 数量，全部基于 MyBatis-Plus LambdaQueryWrapper。
EOF
)"
```

---

### Task 7：M2 - Product Controller（6 端点）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantProductController.java`

- [ ] **Step 1：Controller**

```java
package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProductService;
import com.yan.freshfood.merchant.vo.MerchantProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final MerchantProductService merchantProductService;

    @GetMapping
    public R<PageR<MerchantProductVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantProductService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<MerchantProductVO> detail(@PathVariable Long id) {
        return R.ok(merchantProductService.detail(id));
    }

    @PostMapping
    public R<MerchantProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return R.ok(merchantProductService.create(dto));
    }

    @PutMapping("/{id}")
    public R<MerchantProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        dto.setId(id);
        return R.ok(merchantProductService.update(dto));
    }

    @PostMapping("/{id}/on-shelf")
    public R<Void> onShelf(@PathVariable Long id) {
        merchantProductService.onShelf(id);
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    public R<Void> offShelf(@PathVariable Long id) {
        merchantProductService.offShelf(id);
        return R.ok();
    }
}
```

- [ ] **Step 2：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 3：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantProductController.java
git commit -m "$(cat <<'EOF'
feat(merchant-product-controller): 商品管理 6 个 REST 端点

- GET    /api/v1/merchant/products?status=&pageNum=&pageSize=     分页
- GET    /api/v1/merchant/products/{id}                             详情
- POST   /api/v1/merchant/products                                 创建
- PUT    /api/v1/merchant/products/{id}                             更新
- POST   /api/v1/merchant/products/{id}/on-shelf                    上架
- POST   /api/v1/merchant/products/{id}/off-shelf                   下架

PUT 的 id 直接 set 进 dto 后调 service；status 0/1 允许查询过滤。
EOF
)"
```

---

### Task 8：M3 - SKU DTOs + Service（4 方法）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/SkuCreateDTO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/SkuUpdateDTO.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantSkuService.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantSkuServiceImpl.java`

- [ ] **Step 1：`SkuCreateDTO.java`**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuCreateDTO {
    @NotBlank(message = "规格不能为空")
    @Size(max = 50, message = "规格不超过 50 字")
    private String spec;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    @Size(max = 500, message = "图片 URL 不超过 500 字")
    private String image;
}
```

- [ ] **Step 2：`SkuUpdateDTO.java`**

```java
package com.yan.freshfood.merchant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 SKU；至少 1 个非空字段（service 层校验）。
 */
@Data
public class SkuUpdateDTO {
    @Size(max = 50, message = "规格不超过 50 字")
    private String spec;

    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    @Size(max = 500, message = "图片 URL 不超过 500 字")
    private String image;
}
```

- [ ] **Step 3：`MerchantSkuService.java`**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.vo.SkuVO;

import java.util.List;

public interface MerchantSkuService {
    List<SkuVO> list(Long productId);
    SkuVO create(Long productId, SkuCreateDTO dto);
    SkuVO update(Long id, SkuUpdateDTO dto);
    void delete(Long id);
}
```

- [ ] **Step 4：`MerchantSkuServiceImpl.java`**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.merchant.service.MerchantSkuService;
import com.yan.freshfood.merchant.vo.SkuVO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantSkuServiceImpl implements MerchantSkuService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SkuVO> list(Long productId) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(productId);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>().eq(SkuDO::getProductId, productId));
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SkuVO create(Long productId, SkuCreateDTO dto) {
        Long mid = currentMerchantId();
        ProductDO p = productMapper.selectById(productId);
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SkuDO sku = new SkuDO();
        sku.setProductId(productId);
        sku.setSpec(dto.getSpec());
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        sku.setImage(dto.getImage());
        sku.setSales(0);
        skuMapper.insert(sku);
        return toVO(sku);
    }

    @Override
    public SkuVO update(Long id, SkuUpdateDTO dto) {
        SkuDO sku = loadAndCheck(id);
        if (dto.getSpec() != null) sku.setSpec(dto.getSpec());
        if (dto.getPrice() != null) sku.setPrice(dto.getPrice());
        if (dto.getStock() != null) sku.setStock(dto.getStock());
        if (dto.getImage() != null) sku.setImage(dto.getImage());
        // 至少 1 个非空字段：上方至少触发一次 set
        skuMapper.updateById(sku);
        return toVO(sku);
    }

    @Override
    public void delete(Long id) {
        SkuDO sku = loadAndCheck(id);
        if (sku.getSales() != null && sku.getSales() > 0) {
            throw new BusinessException(ErrorCode.SKU_HAS_SALES);
        }
        // 物理删除：本计划不挂 ON DELETE RESTRICT；生产应补外键
        skuMapper.deleteById(id);
    }

    /** 加载 SKU，并经由 product 校验 merchantId */
    private SkuDO loadAndCheck(Long id) {
        Long mid = currentMerchantId();
        SkuDO sku = skuMapper.selectById(id);
        if (sku == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        ProductDO p = productMapper.selectById(sku.getProductId());
        if (p == null || !p.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return sku;
    }

    private SkuVO toVO(SkuDO s) {
        SkuVO vo = new SkuVO();
        vo.setId(s.getId());
        vo.setSpec(s.getSpec());
        vo.setPrice(s.getPrice() == null ? null : s.getPrice().toPlainString());
        vo.setStock(s.getStock());
        vo.setSales(s.getSales());
        vo.setImage(s.getImage());
        return vo;
    }

    private Long currentMerchantId() {
        return StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null).getLoginIdAsLong();
    }
}
```

- [ ] **Step 5：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 6：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/SkuCreateDTO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/dto/SkuUpdateDTO.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantSkuService.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantSkuServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(merchant-sku-service): SKU 模块 Service 4 个方法

- list(productId)：按 productId 加载列表，前置校验 product.merchantId
- create(productId, dto)：同前置校验
- update(id, dto)：动态判断 4 个字段是否设置
- delete(id)：前置 sales==0 校验, 否则 SKU_HAS_SALES(3004); 物理删除

loadAndCheck 通过 sku → product 间接校验 merchantId, 防止越权。
EOF
)"
```

---

### Task 9：M3 - SKU Controller（4 端点）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantSkuController.java`

- [ ] **Step 1：Controller**

```java
package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantSkuService;
import com.yan.freshfood.merchant.vo.SkuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantSkuController {

    private final MerchantSkuService merchantSkuService;

    @GetMapping("/products/{productId}/skus")
    public R<List<SkuVO>> list(@PathVariable Long productId) {
        return R.ok(merchantSkuService.list(productId));
    }

    @PostMapping("/products/{productId}/skus")
    public R<SkuVO> create(@PathVariable Long productId,
                           @Valid @RequestBody SkuCreateDTO dto) {
        return R.ok(merchantSkuService.create(productId, dto));
    }

    @PutMapping("/skus/{id}")
    public R<SkuVO> update(@PathVariable Long id,
                           @Valid @RequestBody SkuUpdateDTO dto) {
        return R.ok(merchantSkuService.update(id, dto));
    }

    @DeleteMapping("/skus/{id}")
    public R<Void> delete(@PathVariable Long id) {
        merchantSkuService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 2：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 3：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantSkuController.java
git commit -m "$(cat <<'EOF'
feat(merchant-sku-controller): SKU 模块 4 个 REST 端点

- GET    /api/v1/merchant/products/{productId}/skus   列表
- POST   /api/v1/merchant/products/{productId}/skus   新增
- PUT    /api/v1/merchant/skus/{id}                   改价/规格/库存/图片
- DELETE /api/v1/merchant/skus/{id}                   删除(sales==0 前置)
EOF
)"
```

---

### Task 10：M4 - Order Service（3 方法 + transactional ship）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantOrderService.java`
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantOrderServiceImpl.java`

- [ ] **Step 1：`MerchantOrderService.java`**

```java
package com.yan.freshfood.merchant.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;

public interface MerchantOrderService {
    PageR<MerchantOrderVO> page(Integer status, long pageNum, long pageSize);
    MerchantOrderVO detail(Long id);
    void ship(Long id);
}
```

- [ ] **Step 2：`MerchantOrderServiceImpl.java`**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.merchant.mapper.OrderItemMapper;
import com.yan.freshfood.merchant.mapper.OrderMapper;
import com.yan.freshfood.merchant.service.MerchantOrderService;
import com.yan.freshfood.merchant.vo.MerchantOrderItemVO;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private static final Map<Integer, String> STATUS_TEXT = new HashMap<>();
    static {
        STATUS_TEXT.put(1, "待付款");
        STATUS_TEXT.put(2, "待发货");
        STATUS_TEXT.put(3, "待收货");
        STATUS_TEXT.put(4, "待评价");
        STATUS_TEXT.put(5, "已完成");
        STATUS_TEXT.put(6, "售后中");
        STATUS_TEXT.put(7, "已取消");
    }

    @Override
    public PageR<MerchantOrderVO> page(Integer status, long pageNum, long pageSize) {
        Long mid = currentMerchantId();
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        LambdaQueryWrapper<OrderDO> q = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getMerchantId, mid)
                .orderByDesc(OrderDO::getCreateTime);
        if (status != null) q.eq(OrderDO::getStatus, status);

        Page<OrderDO> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantOrderVO> records = page.getRecords().stream()
                .map(o -> toVO(o, null)).collect(Collectors.toList());
        Page<MerchantOrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageR.of(voPage);
    }

    @Override
    public MerchantOrderVO detail(Long id) {
        Long mid = currentMerchantId();
        OrderDO order = orderMapper.selectById(id);
        if (order == null || !order.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, id));
        return toVO(order, items);
    }

    @Override
    @Transactional
    public void ship(Long id) {
        Long mid = currentMerchantId();
        OrderDO order = orderMapper.selectById(id);
        if (order == null || !order.getMerchantId().equals(mid)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(3); // 待收货
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private MerchantOrderVO toVO(OrderDO o, List<OrderItemDO> items) {
        MerchantOrderVO vo = new MerchantOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setStatus(o.getStatus());
        vo.setStatusText(STATUS_TEXT.getOrDefault(o.getStatus(), "未知"));
        vo.setTotalAmount(o.getTotalAmount() == null ? null : o.getTotalAmount().toPlainString());
        vo.setPayableAmount(o.getPayableAmount() == null ? null : o.getPayableAmount().toPlainString());
        vo.setAddressSnapshot(o.getAddressSnapshot());
        vo.setRemark(o.getRemark());
        vo.setCreateTime(o.getCreateTime());
        vo.setPayTime(o.getPayTime());
        vo.setShipTime(o.getShipTime());
        if (items != null) {
            vo.setItems(items.stream().map(it -> {
                MerchantOrderItemVO v = new MerchantOrderItemVO();
                v.setId(it.getId());
                v.setSkuId(it.getSkuId());
                v.setProductId(it.getProductId());
                v.setProductName(it.getProductNameSnapshot());
                v.setSpec(it.getSpecSnapshot());
                v.setPrice(it.getPriceSnapshot() == null ? null : it.getPriceSnapshot().toPlainString());
                v.setQuantity(it.getQuantity());
                return v;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private Long currentMerchantId() {
        return StpUtil.getStpLogic(CommonConstants.TYPE_MERCHANT, null).getLoginIdAsLong();
    }
}
```

- [ ] **Step 3：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 4：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/MerchantOrderService.java \
           freshfood-merchant/src/main/java/com/yan/freshfood/merchant/service/impl/MerchantOrderServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(merchant-order-service): 订单发货模块 Service 3 个方法

- page(status, pageNum, pageSize)：按 merchantId 过滤; status 可选 1-7
- detail(id)：按 merchantId 校验; 返回内嵌 items 与原始 addressSnapshot JSON
- ship(id) 走 @Transactional：merchantId 不匹配 NOT_FOUND;
  status != 2 → ORDER_STATUS_INVALID(4001); 2 → 3, shipTime=now

状态文案通过本地静态 Map (1待付/2待发/3待收/4待评/5完成/6售后/7取消) 还原。
buyer 姓名/手机号不解密（spec 最终决策）。
EOF
)"
```

---

### Task 11：M4 - Order Controller（3 端点）

**Files:**
- Create: `freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantOrderController.java`

- [ ] **Step 1：Controller**

```java
package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.service.MerchantOrderService;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @GetMapping
    public R<PageR<MerchantOrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantOrderService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<MerchantOrderVO> detail(@PathVariable Long id) {
        return R.ok(merchantOrderService.detail(id));
    }

    @PostMapping("/{id}/ship")
    public R<Void> ship(@PathVariable Long id) {
        merchantOrderService.ship(id);
        return R.ok();
    }
}
```

- [ ] **Step 2：编译**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant compile -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 3：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/main/java/com/yan/freshfood/merchant/controller/MerchantOrderController.java
git commit -m "$(cat <<'EOF'
feat(merchant-order-controller): 订单发货 3 个 REST 端点

- GET  /api/v1/merchant/orders?status=&pageNum=&pageSize=
- GET  /api/v1/merchant/orders/{id}
- POST /api/v1/merchant/orders/{id}/ship

ship 仅允许 status=2 → 3, 其他状态由 service 抛 ORDER_STATUS_INVALID。
EOF
)"
```

---

### Task 12：单元测试 - 5 个用例（3 个测试类）

**Files:**
- Create: `freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantProductServiceImplTest.java`
- Create: `freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantSkuServiceImplTest.java`
- Create: `freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantOrderServiceImplTest.java`

> **测试工具：** Mockito + `@MockedStatic<StpUtil>`，全部走 `@ExtendWith(MockitoExtension.class)`。
>
> **断言约定：** BusinessException 只暴露 `getCode()`（Lombok `@Getter` 生成），断言用：
>
> ```java
> assertEquals(ErrorCode.FOO.getCode(), ex.getCode());
> ```
>
> **不要**写 `ex.getErrorCode()`（计划 2 已踩坑）。
>
> **StpLogic mock 标准模式**（`getStpLogic(Object loginType, String device)`，第二个参数是 String）：
>
> ```java
> try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
>     StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
>     stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
>     when(logic.getLoginIdAsLong()).thenReturn(1L);
>     // ... 测试体
> }
> ```
>
> 第二个参数必须用 `anyString()`（不能 `any()`）——Mockito 严格模式会因类型不匹配报错。

- [ ] **Step 1：`MerchantProductServiceImplTest.java`（1 个用例）**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.CategoryMapper;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
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
class MerchantProductServiceImplTest {

    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private SkuMapper skuMapper;

    @InjectMocks private MerchantProductServiceImpl service;

    @Test
    void on_shelf_throws_when_pending_audit() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setMerchantId(1L);
            p.setAuditStatus(0); // 待审
            when(productMapper.selectById(1001L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.onShelf(1001L));
            assertEquals(ErrorCode.PRODUCT_PENDING_AUDIT.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 2：`MerchantSkuServiceImplTest.java`（1 个用例）**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.ProductMapper;
import com.yan.freshfood.merchant.mapper.SkuMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantSkuServiceImplTest {

    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private MerchantSkuServiceImpl service;

    @Test
    void delete_throws_when_has_sales() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            sku.setSales(1); // 已售
            when(skuMapper.selectById(2001L)).thenReturn(sku);

            ProductDO p = new ProductDO();
            p.setId(1001L);
            p.setMerchantId(1L);
            when(productMapper.selectById(1001L)).thenReturn(p);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(2001L));
            assertEquals(ErrorCode.SKU_HAS_SALES.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 3：`MerchantOrderServiceImplTest.java`（3 个用例）**

```java
package com.yan.freshfood.merchant.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.merchant.mapper.OrderItemMapper;
import com.yan.freshfood.merchant.mapper.OrderMapper;
import com.yan.freshfood.model.entity.trade.OrderDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    @InjectMocks private MerchantOrderServiceImpl service;

    @Test
    void ship_transitions_2_to_3_and_sets_ship_time() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8888L);
            order.setMerchantId(1L);
            order.setStatus(2);
            order.setTotalAmount(new BigDecimal("119.80"));
            order.setPayableAmount(new BigDecimal("119.80"));
            when(orderMapper.selectById(8888L)).thenReturn(order);
            when(orderMapper.updateById(any(OrderDO.class))).thenAnswer(inv -> 1);

            service.ship(8888L);

            assertEquals(3, order.getStatus());
            assertNotNull(order.getShipTime());
            // shipTime 应接近当前时间（允许秒级误差）
            long diff = Math.abs(java.time.Duration.between(order.getShipTime(), LocalDateTime.now()).getSeconds());
            assertEquals(true, diff < 5);
        }
    }

    @Test
    void ship_throws_when_status_not_2() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8889L);
            order.setMerchantId(1L);
            order.setStatus(1); // 待付款，不能发货
            when(orderMapper.selectById(8889L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8889L));
            assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
        }
    }

    @Test
    void ship_throws_when_not_owner() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StpLogic logic = org.mockito.Mockito.mock(StpLogic.class);
            stp.when(() -> StpUtil.getStpLogic(any(), anyString())).thenReturn(logic);
            when(logic.getLoginIdAsLong()).thenReturn(1L);

            OrderDO order = new OrderDO();
            order.setId(8890L);
            order.setMerchantId(99L); // 别的商家
            order.setStatus(2);
            when(orderMapper.selectById(8890L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.ship(8890L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
```

- [ ] **Step 4：跑测试**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant test -q
```

预期：
```
Tests run: 5, Failures: 0, Errors: 0
```

常见失败：
- `getErrorCode()` → 改 `getCode()`
- `StpLogic.getLoginIdAsLong()` 没被 mock → 用标准模式（见上方"StpLogic mock 标准模式"）
- Lombok `@RequiredArgsConstructor` 字段注入需 `@InjectMocks` + `@Mock`（MockitoExtension 已支持）

- [ ] **Step 5：commit**

```bash
cd freshfood-shop
git add freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantProductServiceImplTest.java \
           freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantSkuServiceImplTest.java \
           freshfood-merchant/src/test/java/com/yan/freshfood/merchant/service/impl/MerchantOrderServiceImplTest.java
git commit -m "$(cat <<'EOF'
test(merchant-service): 新增商家端 3 个 Service 的单元测试

- MerchantProductServiceImplTest：1 个用例（on-shelf 待审抛 PRODUCT_PENDING_AUDIT）
- MerchantSkuServiceImplTest：1 个用例（delete sku sales>0 抛 SKU_HAS_SALES）
- MerchantOrderServiceImplTest：3 个用例
  - ship 2→3 + shipTime 设置
  - ship status=1 抛 ORDER_STATUS_INVALID
  - ship 跨商家抛 NOT_FOUND

共 5 个用例，MockedStatic<StpUtil> + mock StpLogic.getLoginIdAsLong()。
EOF
)"
```

---

### Task 13：集成验证 - 全模块编译 + 测试 + mvn plugin 跳过 jar 打包

**Files:** 无（只跑命令）

- [ ] **Step 1：全工程 compile**

```bash
cd freshfood-shop
mvn clean compile -q
```

预期：`BUILD SUCCESS`，0 error，0 fatal warning。

- [ ] **Step 2：merchant 模块测试**

```bash
cd freshfood-shop
mvn -pl freshfood-merchant test -q
```

预期：`Tests run: 5, Failures: 0, Errors: 0`。

- [ ] **Step 3：本地启动应用并冒烟（待 JDK 17 装好后执行）**

```bash
cd freshfood-shop
mvn -pl freshfood-app spring-boot:run
```

启动后预期日志看到 `Started FreshfoodApp`。Knife4j 地址 `http://localhost:8080/doc.html`。

冒烟脚本（拿到计划 1 `m01` token 后执行）：

```bash
# 1) 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/merchant/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"m01pass"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# 2) 店铺信息
curl -s http://localhost:8080/api/v1/merchant/profile \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# 3) 商品列表
curl -s "http://localhost:8080/api/v1/merchant/products?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# 4) 创建商品
curl -s -X POST http://localhost:8080/api/v1/merchant/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试商品","categoryId":11,"mainImage":"https://img.example.com/t1.jpg"}' | python -m json.tool

# 5) m01 看 m02 的商品（应 404）
#    需要预先建 m02 才能验证（计划 4 数据）

# 6) SKU 列表
curl -s http://localhost:8080/api/v1/merchant/products/1001/skus \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# 7) 订单列表
curl -s "http://localhost:8080/api/v1/merchant/orders?status=2&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
```

预期（关键校验点）：
- `/profile` 返回 `m01` 完整信息（含解密后的 contactName/contactPhone）
- `/products` 返回 m01 拥有的 4 个商品（含 priceRange）
- `/products/1001/skus` 返回 1001 的 2 个 SKU（2001, 2002）
- `/orders` 按 merchantId 过滤返回自己的订单

- [ ] **Step 4：失败就 commit fix**

任何报错先**就地修复**，不许"先跳过"。修完单独 commit 一个 `fix(merchant-xxx)` 再继续。

---

### Task 14：收尾 - 推送 + fast-forward 合并到 main

**Files:** 无

- [ ] **Step 1：检查 git 状态**

```bash
cd freshfood-shop
git status
git log --oneline -10
```

预期：工作区干净，分支在 `feature/merchant-business`。

- [ ] **Step 2：推送到远程**

向用户报告（**不要直接 push**）：

```
即将 push：
  分支：feature/merchant-business
  远程：origin
  commits ahead：~12-13 commits

OK push / 先改 X / 撤回
```

拿到用户 OK 后执行：

```bash
cd freshfood-shop
git push -u origin feature/merchant-business
```

- [ ] **Step 3：本地 fast-forward 合入 main 并推送（待用户单独 OK）**

向用户报告：

```
即将 fast-forward 合入 main 并推 origin/main：

  git checkout main
  git merge --ff-only feature/merchant-business
  git push origin main

OK / 先改 X / 撤回
```

收到 OK 后：

```bash
cd freshfood-shop
git checkout main
git merge --ff-only feature/merchant-business
git push origin main
git log --oneline -5
```

预期：合并无冲突，`origin/main` 与本地 main 同步。

---

## 四、回顾与自检

| 检查项 | 状态 |
|---|---|
| `mvn clean compile` BUILD SUCCESS | ☐ |
| merchant 模块 5 个单元测试全部通过 | ☐ |
| 15 个端点在 Knife4j `/doc.html` 可见 | ☐ |
| m01 看不到 / 改不了 m02 的商品 / SKU / 订单 | ☐ |
| `on-shelf` 待审核商品抛 `PRODUCT_PENDING_AUDIT(3003)` | ☐ |
| `delete SKU sales>0` 抛 `SKU_HAS_SALES(3004)` | ☐ |
| `ship` 跨商家抛 `NOT_FOUND(1004)`；status!=2 抛 `ORDER_STATUS_INVALID(4001)` | ☐ |
| 发货后 `status=3` 且 `shipTime` 不为空 | ☐ |
| 商品创建后默认 `status=0, auditStatus=0, sales=0` | ☐ |
| 不允许商户改 `auditStatus / status / sales / rating / merchantId` | ☐ |
| 没有新增数据库表 / 列 | ☐ |
| Git history 中 `type` 英文 / `subject+body` 中文 | ☐ |
| `feature/merchant-business` 分支保留或合入 main（远端同步） | ☐ |

---

## 五、参考与依赖

- 已实施
  - 计划 1：`docs/superpowers/plans/2026-06-29-freshfood-foundation.md` —— 商家登录（`MerchantDO` + `MerchantAuthServiceImpl`）、Sa-Token 多 StpLogic、`ErrorCode` 基础码
  - 计划 2：`docs/superpowers/plans/2026-06-29-freshfood-user-business.md` —— 12 业务表 + DOs + `SkuVO(user 版)` + `PageR` + OrderServiceImpl（参考其 `@Transactional` 与 `ObjectMapper` 反序列化模式）
- 设计 spec：`docs/superpowers/specs/2026-06-30-freshfood-merchant-business-design.md`
- 外部依赖（计划 2 已配齐）
  - MyBatis-Plus 3.5.9 + `mybatis-plus-annotation`
  - Sa-Token 1.37.0（`StpUtil.getStpLogic`）
  - Hutool 5.8.27（BeanUtil 暂未使用 — 按需引入）
  - Spring Validation
- 后续
  - 计划 4：管理端 41 个接口（商家审核、商品审核、统计 ECharts）
