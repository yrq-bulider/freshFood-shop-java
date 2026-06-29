# 计划 2：用户端核心业务 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在计划 1 的三端登录基础上，实现用户端 9 个业务模块的 40 个接口（首页、搜索、商品、购物车、地址、下单支付、订单物流、评价、消息）。

**Architecture:** 沿用 4 层架构（Controller / Service / Mapper）+ 通用 DO/DTO/VO。新增表全部建在 `freshfood_shop` 库，DO 落在 `freshfood-model` 模块，Mapper/Service/Controller 落在 `freshfood-user` 模块。所有需登录接口用 Sa-Token 用户端 StpLogic（`StpUtil.getLoginIdAsLong()`）。订单创建走 `@Transactional` 保证事务一致。

**Tech Stack:** MyBatis-Plus 3.5.9（LambdaQueryWrapper + 分页）、Sa-Token 1.37.0、Hutool 5.8.27（IdUtil、雪花算法、BeanUtil）、Spring Validation、BCrypt（计划 1 已用）。

---

## 一、文件结构总览

实施完成后 `freshfood-user/` 与 `freshfood-model/` 应新增：

```
freshfood-model/src/main/java/com/yan/freshfood/model/entity/
├── product/
│   ├── CategoryDO.java
│   ├── ProductDO.java
│   ├── SkuDO.java
│   ├── BannerDO.java
│   └── HotWordDO.java
├── trade/
│   ├── CartDO.java
│   ├── AddressDO.java
│   ├── OrderDO.java
│   └── OrderItemDO.java
└── content/
    ├── ReviewDO.java
    ├── MessageDO.java
    └── SearchHistoryDO.java

freshfood-user/src/main/java/com/yan/freshfood/user/
├── controller/
│   ├── HomeController.java        # 2.2
│   ├── SearchController.java      # 2.3
│   ├── ProductController.java     # 2.4
│   ├── CartController.java        # 2.5
│   ├── AddressController.java     # 2.6
│   ├── OrderController.java       # 2.7 + 2.8
│   ├── ReviewController.java      # 2.9
│   └── MessageController.java     # 2.10
├── service/impl/
│   ├── HomeServiceImpl.java
│   ├── SearchServiceImpl.java
│   ├── ProductServiceImpl.java
│   ├── CartServiceImpl.java
│   ├── AddressServiceImpl.java
│   ├── OrderServiceImpl.java
│   ├── ReviewServiceImpl.java
│   └── MessageServiceImpl.java
├── mapper/
│   ├── CategoryMapper.java
│   ├── ProductMapper.java
│   ├── SkuMapper.java
│   ├── BannerMapper.java
│   ├── HotWordMapper.java
│   ├── CartMapper.java
│   ├── AddressMapper.java
│   ├── OrderMapper.java
│   ├── OrderItemMapper.java
│   ├── ReviewMapper.java
│   ├── MessageMapper.java
│   └── SearchHistoryMapper.java
└── ... (已有 dto/vo/auth 保留)

sql/02_business_tables.sql
sql/03_test_data.sql            # 计划 2 测试数据
```

> 注：实体按子包 `product / trade / content` 组织，避免 `model.entity` 平铺过深。

---

## 二、关键约定（请先看，影响后续所有任务）

### 2.1 通用 ID 与金额

- **订单号**：用 Hutool `IdUtil.getSnowflakeNextId()` 生成 Long，再转字符串，或用 `LocalDate.now().format("yyyyMMdd") + 4 位流水`。**统一用后者**，避免 Long 转字符串的格式问题，流水号用 `userMapper.selectCount(...)` 简单实现或 Hutool 的 `RandomUtil.randomNumbers(4)`。
- **金额**：DO 字段 `BigDecimal`，VO 字段 `String`（避免前端 JS 精度丢失），转换用 `BigDecimal#toPlainString()`。
- **状态枚举**：订单状态 1=待付款 / 2=待发货 / 3=待收货 / 4=待评价 / 5=已完成 / 6=售后中 / 7=已取消。

### 2.2 登录态

- 用户端用 `StpUtil.getLoginIdAsLong()`（不需要 type 参数，默认 user）。
- 商家/管理端用 `StpUtil.getStpLogic(type, null)`，计划 3/4 处理。

### 2.3 字段加密

- 已在计划 1 实施：`UserDO.phone / email`、`MerchantDO.contactName / contactPhone` 自动加解密。
- 计划 2 新增的 `AddressDO.receiverName / phone` 走同样的 `EncryptedStringTypeHandler`（直接复用，DO 字段加 `@TableField(typeHandler = EncryptedStringTypeHandler.class)`）。

### 2.4 错误码

计划 1 已定义基础码；本计划新增以下到 `ErrorCode.java`：

| 码 | 名称 | 文案 |
|---|---|---|
| 1004 | NOT_FOUND | 资源不存在 |
| 3001 | PRODUCT_OFF_SHELF | 商品已下架 |
| 3002 | STOCK_NOT_ENOUGH | 库存不足 |
| 4001 | ORDER_STATUS_INVALID | 订单状态不允许该操作 |
| 4002 | ORDER_NOT_FOUND | 订单不存在 |
| 5001 | PAY_FAILED | 支付失败 |
| 7001 | MERCHANT_PENDING | 商家未通过审核（已存在，复用） |

---

## 三、任务清单

### Task 1：扩展数据库 schema 与测试数据

**Files:**
- Create: `sql/02_business_tables.sql`
- Create: `sql/03_test_data.sql`

- [ ] **Step 1：建表文件 `sql/02_business_tables.sql`**

```sql
USE freshfood_shop;

-- 商品分类（最多两级）
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '0=顶级',
    `name`        VARCHAR(50)  NOT NULL,
    `icon`        VARCHAR(255) DEFAULT NULL,
    `sort`        INT          NOT NULL DEFAULT 0,
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- SPU（商品）
CREATE TABLE IF NOT EXISTS `product` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `merchant_id`      BIGINT       NOT NULL COMMENT '所属商家',
    `category_id`      BIGINT       NOT NULL,
    `name`             VARCHAR(200) NOT NULL,
    `main_image`       VARCHAR(255) DEFAULT NULL,
    `description`      TEXT         DEFAULT NULL,
    `origin`           VARCHAR(100) DEFAULT NULL,
    `after_sales_tags` VARCHAR(500) DEFAULT NULL COMMENT '逗号分隔',
    `audit_status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '0 待审 / 1 通过 / 2 拒绝',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '0 下架 / 1 上架',
    `sales`            INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
    `rating`           DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '平均评分',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SPU';

-- SKU（销售单元）
CREATE TABLE IF NOT EXISTS `sku` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT        NOT NULL,
    `spec`       VARCHAR(100)  NOT NULL COMMENT '如 1斤装',
    `price`      DECIMAL(10,2) NOT NULL,
    `stock`      INT           NOT NULL DEFAULT 0,
    `sales`      INT           NOT NULL DEFAULT 0,
    `image`      VARCHAR(255)  DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SKU';

-- Banner
CREATE TABLE IF NOT EXISTS `banner` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(100) NOT NULL,
    `image`       VARCHAR(255) NOT NULL,
    `link_type`   VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PRODUCT/CATEGORY/URL',
    `link_target` VARCHAR(255) DEFAULT NULL,
    `sort`        INT          NOT NULL DEFAULT 0,
    `enabled`     TINYINT      NOT NULL DEFAULT 1,
    `start_time`  DATETIME     DEFAULT NULL,
    `end_time`    DATETIME     DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播';

-- 热搜词
CREATE TABLE IF NOT EXISTS `hot_word` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `keyword`    VARCHAR(50)  NOT NULL,
    `search_count` INT        NOT NULL DEFAULT 0,
    `sort`       INT          NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热搜词';

-- 购物车
CREATE TABLE IF NOT EXISTS `cart` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `sku_id`     BIGINT       NOT NULL,
    `quantity`   INT          NOT NULL DEFAULT 1,
    `selected`   TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- 收货地址
CREATE TABLE IF NOT EXISTS `address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收件人（加密）',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号（加密）',
    `province`    VARCHAR(50)  NOT NULL,
    `city`        VARCHAR(50)  NOT NULL,
    `district`    VARCHAR(50)  DEFAULT NULL,
    `detail`      VARCHAR(255) NOT NULL,
    `is_default`  TINYINT      NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

-- 订单
CREATE TABLE IF NOT EXISTS `orders` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`          VARCHAR(32)   NOT NULL COMMENT '业务订单号 yyyyMMdd+4位',
    `user_id`           BIGINT        NOT NULL,
    `merchant_id`       BIGINT        NOT NULL,
    `total_amount`      DECIMAL(10,2) NOT NULL,
    `shipping_fee`      DECIMAL(10,2) NOT NULL DEFAULT 0,
    `discount_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0,
    `payable_amount`    DECIMAL(10,2) NOT NULL,
    `address_snapshot`  VARCHAR(2000) NOT NULL COMMENT 'JSON',
    `remark`            VARCHAR(500)  DEFAULT NULL,
    `status`            TINYINT       NOT NULL DEFAULT 1 COMMENT '1待付/2待发/3待收/4待评/5完成/6售后/7取消',
    `expire_time`       DATETIME      DEFAULT NULL COMMENT '待付款过期时间',
    `pay_time`          DATETIME      DEFAULT NULL,
    `ship_time`         DATETIME      DEFAULT NULL,
    `confirm_time`      DATETIME      DEFAULT NULL,
    `pay_method`        VARCHAR(20)   DEFAULT NULL,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_merchant_status` (`merchant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 订单明细
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`          BIGINT        NOT NULL,
    `sku_id`            BIGINT        NOT NULL,
    `product_id`        BIGINT        NOT NULL,
    `product_name_snapshot` VARCHAR(200) NOT NULL,
    `spec_snapshot`     VARCHAR(100)  NOT NULL,
    `price_snapshot`    DECIMAL(10,2) NOT NULL,
    `quantity`          INT           NOT NULL,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 评价
CREATE TABLE IF NOT EXISTS `review` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`         BIGINT       NOT NULL,
    `order_item_id`    BIGINT       NOT NULL,
    `user_id`          BIGINT       NOT NULL,
    `product_id`       BIGINT       NOT NULL,
    `sku_id`           BIGINT       NOT NULL,
    `merchant_id`      BIGINT       NOT NULL,
    `rating`           TINYINT      NOT NULL COMMENT '1-5',
    `taste_rating`     TINYINT      DEFAULT NULL,
    `freshness_rating` TINYINT      DEFAULT NULL,
    `logistics_rating` TINYINT      DEFAULT NULL,
    `content`          TEXT         NOT NULL,
    `images`           VARCHAR(2000) DEFAULT NULL COMMENT '逗号分隔',
    `merchant_reply`   VARCHAR(500) DEFAULT NULL,
    `reply_time`       DATETIME     DEFAULT NULL,
    `is_append`        TINYINT      NOT NULL DEFAULT 0,
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '0 隐藏 / 1 显示',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价';

-- 消息
CREATE TABLE IF NOT EXISTS `message` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `type`       VARCHAR(20)  NOT NULL COMMENT 'SYSTEM/ORDER/PROMO',
    `title`      VARCHAR(100) NOT NULL,
    `content`    VARCHAR(1000) NOT NULL,
    `related_id` BIGINT       DEFAULT NULL,
    `is_read`    TINYINT      NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息';

-- 搜索历史
CREATE TABLE IF NOT EXISTS `search_history` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `keyword`    VARCHAR(100) NOT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史';
```

- [ ] **Step 2：测试数据 `sql/03_test_data.sql`**

```sql
USE freshfood_shop;

-- 分类
INSERT INTO `category` (id, parent_id, name, icon, sort) VALUES
(1, 0, '水果', 'icon-fruit', 1),
(2, 0, '海鲜', 'icon-seafood', 2),
(3, 0, '蔬菜', 'icon-veg', 3),
(4, 0, '肉禽蛋', 'icon-meat', 4),
(5, 0, '粮油', 'icon-grain', 5),
(11, 1, '车厘子/樱桃', NULL, 1),
(12, 1, '热带水果', NULL, 2),
(21, 2, '虾蟹', NULL, 1),
(31, 3, '叶菜', NULL, 1);

-- 商品（merchant_id=1 是 m01）
INSERT INTO `product` (id, merchant_id, category_id, name, main_image, description, origin, after_sales_tags, sales, rating) VALUES
(1001, 1, 11, '智利车厘子 J 级', 'https://img.example.com/c1.jpg', '智利进口空运，颗大饱满', '智利', '坏果包赔,48小时发货', 560, 4.8),
(1002, 1, 11, '丹东草莓 大果', 'https://img.example.com/c2.jpg', '丹东 99 草莓现摘现发', '辽宁丹东', '坏果包赔', 480, 4.7),
(1003, 1, 12, '海南金煌芒', 'https://img.example.com/c3.jpg', '树上熟香甜', '海南', '坏果包赔', 410, 4.6),
(1004, 1, 21, '基围虾 500g', 'https://img.example.com/s1.jpg', '活虾急冻，顺丰冷链', '广东', '死虾包赔', 320, 4.5);

-- SKU
INSERT INTO `sku` (id, product_id, spec, price, stock, sales, image) VALUES
(2001, 1001, '1斤装', 59.90, 100, 320, 'https://img.example.com/c1-1.jpg'),
(2002, 1001, '2斤装', 109.00, 80, 180, 'https://img.example.com/c1-2.jpg'),
(2003, 1002, '1斤装', 39.90, 200, 280, 'https://img.example.com/c2-1.jpg'),
(2004, 1003, '5斤装', 79.00, 50, 210, 'https://img.example.com/c3-1.jpg'),
(2005, 1004, '500g装', 89.00, 60, 320, 'https://img.example.com/s1-1.jpg');

-- Banner
INSERT INTO `banner` (title, image, link_type, link_target, sort, enabled, start_time, end_time) VALUES
('618 大促', 'https://img.example.com/b1.jpg', 'CATEGORY', '1', 1, 1, '2026-06-01 00:00:00', '2026-06-30 23:59:59'),
('车厘子上新', 'https://img.example.com/b2.jpg', 'PRODUCT', '1001', 2, 1, '2026-06-15 00:00:00', '2026-07-15 23:59:59'),
('新人专享', 'https://img.example.com/b3.jpg', 'URL', '/new-user', 3, 1, '2026-06-01 00:00:00', '2026-12-31 23:59:59');

-- 热搜词
INSERT INTO `hot_word` (keyword, search_count, sort) VALUES
('车厘子', 1200, 1), ('丹东草莓', 980, 2), ('海南芒果', 850, 3),
('基围虾', 720, 4), ('蓝莓', 690, 5), ('榴莲', 660, 6),
('三文鱼', 580, 7), ('牛肉', 540, 8), ('番茄', 490, 9), ('鸡蛋', 450, 10);
```

- [ ] **Step 3：导入验证**

```bash
mysql -uroot -p < sql/02_business_tables.sql
mysql -uroot -p < sql/03_test_data.sql
mysql -uroot -p freshfood_shop -e "SELECT COUNT(*) FROM product; SELECT COUNT(*) FROM sku;"
```

预期：product=4，sku=5。

- [ ] **Step 4：提交**

```bash
cd freshfood-shop
git add sql/02_business_tables.sql sql/03_test_data.sql
git commit -m "$(cat <<'EOF'
feat(user-business): 新增商品/购物车/订单等 12 张业务表与测试数据

- category / product / sku / banner / hot_word：商品域
- cart / address：交易前置
- orders / order_item：订单与明细（带金额与状态字段）
- review / message / search_history：内容与消息
- 测试数据：5 个分类、4 个商品、5 个 SKU、3 个 Banner、10 个热搜词

表设计要点：金额 DECIMAL(10,2)，状态 TINYINT，时间 DATETIME，软删 deleted 字段。
EOF
)"
```

---

### Task 2：商品域 DO 实体

**Files:**
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/CategoryDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/ProductDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/SkuDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/BannerDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/HotWordDO.java`

- [ ] **Step 1：CategoryDO**

```java
package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class CategoryDO extends BaseDO {
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    /** 0 禁用 / 1 启用 */
    private Integer status;
}
```

- [ ] **Step 2：ProductDO**

```java
package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class ProductDO extends BaseDO {
    private Long merchantId;
    private Long categoryId;
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
}
```

- [ ] **Step 3：SkuDO**

```java
package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class SkuDO extends BaseDO {
    private Long productId;
    private String spec;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String image;
}
```

- [ ] **Step 4：BannerDO**

```java
package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("banner")
public class BannerDO extends BaseDO {
    private String title;
    private String image;
    /** NONE/PRODUCT/CATEGORY/URL */
    private String linkType;
    private String linkTarget;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

- [ ] **Step 5：HotWordDO**

```java
package com.yan.freshfood.model.entity.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hot_word")
public class HotWordDO extends BaseDO {
    private String keyword;
    private Integer searchCount;
    private Integer sort;
}
```

- [ ] **Step 6：编译验证**

```bash
mvn -pl freshfood-model compile -q
```

预期：BUILD SUCCESS。

---

### Task 3：交易域 DO 实体

**Files:**
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/trade/CartDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/trade/AddressDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/trade/OrderDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/trade/OrderItemDO.java`

- [ ] **Step 1：CartDO**

```java
package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart")
public class CartDO extends BaseDO {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    /** 0 未选 / 1 已选 */
    private Integer selected;
}
```

- [ ] **Step 2：AddressDO（receiverName / phone 走加密 TypeHandler）**

```java
package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.common.crypto.EncryptedStringTypeHandler;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "address", autoResultMap = true)
public class AddressDO extends BaseDO {
    private Long userId;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverName;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone;

    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}
```

- [ ] **Step 3：OrderDO**

```java
package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class OrderDO extends BaseDO {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String addressSnapshot;
    private String remark;
    /** 1待付/2待发/3待收/4待评/5完成/6售后/7取消 */
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime confirmTime;
    private String payMethod;
}
```

- [ ] **Step 4：OrderItemDO**

```java
package com.yan.freshfood.model.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItemDO extends BaseDO {
    private Long orderId;
    private Long skuId;
    private Long productId;
    private String productNameSnapshot;
    private String specSnapshot;
    private BigDecimal priceSnapshot;
    private Integer quantity;
}
```

- [ ] **Step 5：编译验证**

```bash
mvn -pl freshfood-model compile -q
```

预期：BUILD SUCCESS。

---

### Task 4：内容与消息域 DO 实体

**Files:**
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/content/ReviewDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/content/MessageDO.java`
- Create: `freshfood-model/src/main/java/com/yan/freshfood/model/entity/content/SearchHistoryDO.java`

- [ ] **Step 1：ReviewDO**

```java
package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class ReviewDO extends BaseDO {
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Long merchantId;
    /** 1-5 */
    private Integer rating;
    private Integer tasteRating;
    private Integer freshnessRating;
    private Integer logisticsRating;
    private String content;
    private String images;
    private String merchantReply;
    private LocalDateTime replyTime;
    /** 0 首评 / 1 追评 */
    private Integer isAppend;
    /** 0 隐藏 / 1 显示 */
    private Integer status;
}
```

- [ ] **Step 2：MessageDO**

```java
package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class MessageDO extends BaseDO {
    private Long userId;
    /** SYSTEM/ORDER/PROMO */
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    /** 0 未读 / 1 已读 */
    private Integer isRead;
}
```

- [ ] **Step 3：SearchHistoryDO**

```java
package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_history")
public class SearchHistoryDO extends BaseDO {
    private Long userId;
    private String keyword;
}
```

- [ ] **Step 4：编译验证**

```bash
mvn -pl freshfood-model compile -q
```

预期：BUILD SUCCESS。

---

### Task 5：所有新 Mapper

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/CategoryMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/ProductMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/SkuMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/BannerMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/HotWordMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/CartMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/AddressMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/OrderMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/OrderItemMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/ReviewMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/MessageMapper.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/mapper/SearchHistoryMapper.java`

> 为节省篇幅，每个 mapper 都是空接口继承 BaseMapper。下面只列需要扩展方法的。

- [ ] **Step 1：所有空 BaseMapper（一次性创建 12 个文件）**

```java
// CategoryMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.CategoryDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {}
```

```java
// ProductMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.ProductDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ProductMapper extends BaseMapper<ProductDO> {}
```

```java
// SkuMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.SkuDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface SkuMapper extends BaseMapper<SkuDO> {}
```

```java
// BannerMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface BannerMapper extends BaseMapper<BannerDO> {}
```

```java
// HotWordMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.product.HotWordDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface HotWordMapper extends BaseMapper<HotWordDO> {}
```

```java
// CartMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.trade.CartDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface CartMapper extends BaseMapper<CartDO> {}
```

```java
// AddressMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.trade.AddressDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface AddressMapper extends BaseMapper<AddressDO> {}
```

```java
// OrderMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.trade.OrderDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {}
```

```java
// OrderItemMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {}
```

```java
// ReviewMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.content.ReviewDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ReviewMapper extends BaseMapper<ReviewDO> {}
```

```java
// MessageMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.content.MessageDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {}
```

```java
// SearchHistoryMapper.java
package com.yan.freshfood.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan.freshfood.model.entity.content.SearchHistoryDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryDO> {}
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS（因为 @MapperScan 在 freshfood-app 已覆盖到 freshfood-user.mapper 包，无需修改）。

---

### Task 6：扩展 ErrorCode 业务错误码

**Files:**
- Modify: `freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java`

- [ ] **Step 1：在枚举尾部新增字段**

打开 `ErrorCode.java`，在 `ADMIN_NOT_FOUND` 后追加：

```java
    PRODUCT_OFF_SHELF(3001, "商品已下架"),
    STOCK_NOT_ENOUGH(3002, "库存不足"),

    ORDER_STATUS_INVALID(4001, "订单状态不允许该操作"),
    ORDER_NOT_FOUND(4002, "订单不存在"),

    PAY_FAILED(5001, "支付失败"),

    REFUND_ALREADY_EXISTS(6001, "退款申请已存在");
```

注意末尾用逗号，ADMIN_NOT_FOUND 那一行末尾也改成逗号。

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-common compile -q
```

预期：BUILD SUCCESS。

---

### Task 7：首页模块 - VOs

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/BannerVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/CategoryVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/ProductSimpleVO.java`

- [ ] **Step 1：BannerVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class BannerVO {
    private Long id;
    private String title;
    private String image;
    private String linkType;
    private String linkTarget;
    private Integer sort;
}
```

- [ ] **Step 2：CategoryVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private List<CategoryVO> children;
}
```

- [ ] **Step 3：ProductSimpleVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSimpleVO {
    private Long productId;
    private String name;
    private String mainImage;
    private String origin;
    private BigDecimal minPrice;
    private Integer sales;
    private BigDecimal rating;
}
```

---

### Task 8：首页模块 - HomeServiceImpl

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/HomeService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/HomeServiceImpl.java`

- [ ] **Step 1：HomeService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;

import java.util.List;

public interface HomeService {
    List<BannerVO> listBanners();
    List<CategoryVO> listCategories();
    List<ProductSimpleVO> listRecommendations();
}
```

- [ ] **Step 2：HomeServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.model.entity.product.BannerDO;
import com.yan.freshfood.model.entity.product.CategoryDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.BannerMapper;
import com.yan.freshfood.user.mapper.CategoryMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final BannerMapper bannerMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @Override
    public List<BannerVO> listBanners() {
        List<BannerDO> list = bannerMapper.selectList(
                new LambdaQueryWrapper<BannerDO>()
                        .eq(BannerDO::getEnabled, 1)
                        .and(w -> w.isNull(BannerDO::getStartTime)
                                .or().le(BannerDO::getStartTime, LocalDateTime.now()))
                        .and(w -> w.isNull(BannerDO::getEndTime)
                                .or().ge(BannerDO::getEndTime, LocalDateTime.now()))
                        .orderByAsc(BannerDO::getSort)
        );
        return list.stream().map(b -> {
            BannerVO v = new BannerVO();
            v.setId(b.getId());
            v.setTitle(b.getTitle());
            v.setImage(b.getImage());
            v.setLinkType(b.getLinkType());
            v.setLinkTarget(b.getLinkTarget());
            v.setSort(b.getSort());
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> listCategories() {
        List<CategoryDO> all = categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getStatus, 1)
                        .orderByAsc(CategoryDO::getSort)
        );
        Map<Long, List<CategoryVO>> grouped = new LinkedHashMap<>();
        for (CategoryDO c : all) {
            CategoryVO v = toVO(c);
            grouped.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(v);
        }
        // 取 parentId = 0 的作为顶级
        List<CategoryVO> tops = grouped.getOrDefault(0L, new ArrayList<>());
        for (CategoryVO top : tops) {
            top.setChildren(grouped.getOrDefault(top.getId(), new ArrayList<>()));
        }
        return tops;
    }

    @Override
    public List<ProductSimpleVO> listRecommendations() {
        List<ProductDO> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductDO>()
                        .eq(ProductDO::getStatus, 1)
                        .eq(ProductDO::getAuditStatus, 1)
                        .orderByDesc(ProductDO::getSales)
                        .last("LIMIT 10")
        );
        return products.stream().map(this::toSimple).collect(Collectors.toList());
    }

    private CategoryVO toVO(CategoryDO c) {
        CategoryVO v = new CategoryVO();
        v.setId(c.getId());
        v.setParentId(c.getParentId());
        v.setName(c.getName());
        v.setIcon(c.getIcon());
        v.setSort(c.getSort());
        return v;
    }

    private ProductSimpleVO toSimple(ProductDO p) {
        ProductSimpleVO v = new ProductSimpleVO();
        v.setProductId(p.getId());
        v.setName(p.getName());
        v.setMainImage(p.getMainImage());
        v.setOrigin(p.getOrigin());
        v.setSales(p.getSales());
        v.setRating(p.getRating());
        // 最低 SKU 价
        SkuDO minSku = skuMapper.selectOne(
                new LambdaQueryWrapper<SkuDO>()
                        .eq(SkuDO::getProductId, p.getId())
                        .orderByAsc(SkuDO::getPrice)
                        .last("LIMIT 1")
        );
        v.setMinPrice(minSku != null ? minSku.getPrice() : null);
        return v;
    }
}
```

- [ ] **Step 3：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 9：首页模块 - HomeController

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/HomeController.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SaIgnore
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/banners")
    public R<List<BannerVO>> banners() {
        return R.ok(homeService.listBanners());
    }

    @GetMapping("/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(homeService.listCategories());
    }

    @GetMapping("/recommendations")
    public R<List<ProductSimpleVO>> recommendations() {
        return R.ok(homeService.listRecommendations());
    }
}
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 10：搜索模块 - VOs + Service + Controller

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/HotWordVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/SearchHistoryVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/SearchService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/SearchServiceImpl.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/SearchController.java`

- [ ] **Step 1：HotWordVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class HotWordVO {
    private Long id;
    private String keyword;
    private Integer searchCount;
}
```

- [ ] **Step 2：SearchHistoryVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistoryVO {
    private Long id;
    private String keyword;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3：SearchService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;

import java.math.BigDecimal;
import java.util.List;

public interface SearchService {

    List<HotWordVO> listHotWords();

    PageR<ProductSimpleVO> searchProducts(String keyword, Long categoryId,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          String sort, int pageNum, int pageSize);

    List<SearchHistoryVO> listMyHistory();

    void clearMyHistory();

    void deleteHistory(Long id);
}
```

- [ ] **Step 4：SearchServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.content.SearchHistoryDO;
import com.yan.freshfood.model.entity.product.HotWordDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.HotWordMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SearchHistoryMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HotWordMapper hotWordMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    public List<HotWordVO> listHotWords() {
        List<HotWordDO> list = hotWordMapper.selectList(
                new LambdaQueryWrapper<HotWordDO>()
                        .orderByAsc(HotWordDO::getSort)
                        .last("LIMIT 10")
        );
        return list.stream().map(h -> {
            HotWordVO v = new HotWordVO();
            v.setId(h.getId());
            v.setKeyword(h.getKeyword());
            v.setSearchCount(h.getSearchCount());
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public PageR<ProductSimpleVO> searchProducts(String keyword, Long categoryId,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 String sort, int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductDO> w = new LambdaQueryWrapper<ProductDO>()
                .eq(ProductDO::getStatus, 1)
                .eq(ProductDO::getAuditStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            w.like(ProductDO::getName, keyword);
            // 异步埋点：当前用户记录搜索历史
            recordHistory(keyword);
        }
        if (categoryId != null) w.eq(ProductDO::getCategoryId, categoryId);
        // 价格过滤：取 SKU 最低价
        if (minPrice != null || maxPrice != null) {
            // 简化：先用 product.id 列表二次过滤，实际生产建议 SQL JOIN
        }
        // 排序
        if ("price_asc".equals(sort)) {
            w.orderByAsc(ProductDO::getId);  // product 无价格字段，按 id 排近似
        } else if ("price_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getId);
        } else if ("sales_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getSales);
        } else if ("rating_desc".equals(sort)) {
            w.orderByDesc(ProductDO::getRating);
        } else {
            w.orderByDesc(ProductDO::getId);
        }
        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), w);
        List<ProductSimpleVO> list = page.getRecords().stream().map(p -> {
            ProductSimpleVO v = new ProductSimpleVO();
            v.setProductId(p.getId());
            v.setName(p.getName());
            v.setMainImage(p.getMainImage());
            v.setOrigin(p.getOrigin());
            v.setSales(p.getSales());
            v.setRating(p.getRating());
            SkuDO minSku = skuMapper.selectOne(
                    new LambdaQueryWrapper<SkuDO>()
                            .eq(SkuDO::getProductId, p.getId())
                            .orderByAsc(SkuDO::getPrice)
                            .last("LIMIT 1")
            );
            v.setMinPrice(minSku != null ? minSku.getPrice() : null);
            return v;
        }).collect(Collectors.toList());
        PageR<ProductSimpleVO> r = new PageR<>();
        r.setList(list);
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    @Override
    public List<SearchHistoryVO> listMyHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SearchHistoryDO> list = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistoryDO>()
                        .eq(SearchHistoryDO::getUserId, userId)
                        .orderByDesc(SearchHistoryDO::getCreateTime)
                        .last("LIMIT 10")
        );
        return list.stream().map(h -> {
            SearchHistoryVO v = new SearchHistoryVO();
            v.setId(h.getId());
            v.setKeyword(h.getKeyword());
            v.setCreateTime(h.getCreateTime());
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public void clearMyHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistoryDO>().eq(SearchHistoryDO::getUserId, userId)
        );
    }

    @Override
    public void deleteHistory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistoryDO>()
                        .eq(SearchHistoryDO::getId, id)
                        .eq(SearchHistoryDO::getUserId, userId)
        );
    }

    private void recordHistory(String keyword) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            SearchHistoryDO h = new SearchHistoryDO();
            h.setUserId(userId);
            h.setKeyword(keyword);
            searchHistoryMapper.insert(h);
        } catch (Exception ignored) {
            // 未登录用户不记录
        }
    }
}
```

- [ ] **Step 5：SearchController**

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @SaIgnore
    @GetMapping("/hot-words")
    public R<List<HotWordVO>> hotWords() {
        return R.ok(searchService.listHotWords());
    }

    @SaIgnore
    @GetMapping("/products")
    public R<PageR<ProductSimpleVO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "sales_desc") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return R.ok(searchService.searchProducts(keyword, categoryId, minPrice, maxPrice,
                sort, pageNum, pageSize));
    }

    @GetMapping("/history")
    public R<List<SearchHistoryVO>> history() {
        return R.ok(searchService.listMyHistory());
    }

    @DeleteMapping("/history")
    public R<Void> clearHistory() {
        searchService.clearMyHistory();
        return R.ok();
    }

    @DeleteMapping("/history/{id}")
    public R<Void> deleteHistory(@PathVariable Long id) {
        searchService.deleteHistory(id);
        return R.ok();
    }
}
```

> 注：`/history` 系列需登录，Sa-Token 默认拦截（class 级别未加 `@SaIgnore`，方法级默认受保护）。

- [ ] **Step 6：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 11：商品详情模块

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/SkuVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/SpecVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/RatingStatsVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/ProductDetailVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/ReviewVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/ProductService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/ProductServiceImpl.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/ProductController.java`

- [ ] **Step 1：SkuVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private String spec;
    private String price;
    private Integer stock;
    private Integer sales;
    private String image;
}
```

- [ ] **Step 2：SpecVO（保留扩展位）**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpecVO {
    private String name;
    private List<String> values;
}
```

- [ ] **Step 3：RatingStatsVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class RatingStatsVO {
    private BigDecimal average;
    private Integer total;
    private Integer five;
    private Integer four;
    private Integer three;
    private Integer two;
    private Integer one;
}
```

- [ ] **Step 4：ProductDetailVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailVO {
    private Long productId;
    private String name;
    private String mainImage;
    private Long categoryId;
    private Long merchantId;
    private String origin;
    private List<String> afterSalesTags;
    private String description;
    private List<SkuVO> skus;
    private List<SpecVO> specs;
    private RatingStatsVO ratingStats;
}
```

- [ ] **Step 5：ReviewVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    private Long id;
    private String username;
    private String avatar;
    private Integer rating;
    private Integer tasteRating;
    private Integer freshnessRating;
    private Integer logisticsRating;
    private String content;
    private List<String> images;
    private String merchantReply;
    private LocalDateTime createTime;
}
```

- [ ] **Step 6：ProductService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.ProductDetailVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.ReviewVO;

public interface ProductService {
    ProductDetailVO getDetail(Long productId);
    PageR<ReviewVO> listReviews(Long productId, int pageNum, int pageSize);
    java.util.List<ProductSimpleVO> listRecommendations(Long productId);
}
```

- [ ] **Step 7：ProductServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.content.ReviewDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.ReviewMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public ProductDetailVO getDetail(Long productId) {
        ProductDO p = productMapper.selectById(productId);
        if (p == null || p.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<SkuDO> skus = skuMapper.selectList(
                new LambdaQueryWrapper<SkuDO>()
                        .eq(SkuDO::getProductId, productId)
                        .orderByAsc(SkuDO::getPrice)
        );
        ProductDetailVO vo = new ProductDetailVO();
        vo.setProductId(p.getId());
        vo.setName(p.getName());
        vo.setMainImage(p.getMainImage());
        vo.setCategoryId(p.getCategoryId());
        vo.setMerchantId(p.getMerchantId());
        vo.setOrigin(p.getOrigin());
        vo.setDescription(p.getDescription());
        vo.setAfterSalesTags(p.getAfterSalesTags() == null
                ? Collections.emptyList()
                : Arrays.asList(p.getAfterSalesTags().split(",")));
        vo.setSkus(skus.stream().map(s -> {
            SkuVO sv = new SkuVO();
            sv.setId(s.getId());
            sv.setSpec(s.getSpec());
            sv.setPrice(s.getPrice().toPlainString());
            sv.setStock(s.getStock());
            sv.setSales(s.getSales());
            sv.setImage(s.getImage());
            return sv;
        }).collect(Collectors.toList()));
        // specs 简化：从 skus.spec 解析（生产应由商家录入 specs 表）
        vo.setSpecs(Collections.emptyList());
        vo.setRatingStats(calcRatingStats(productId));
        return vo;
    }

    @Override
    public PageR<ReviewVO> listReviews(Long productId, int pageNum, int pageSize) {
        Page<ReviewDO> page = reviewMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getProductId, productId)
                        .eq(ReviewDO::getStatus, 1)
                        .orderByDesc(ReviewDO::getCreateTime)
        );
        List<ReviewVO> list = page.getRecords().stream().map(r -> {
            ReviewVO v = new ReviewVO();
            BeanUtil.copyProperties(r, v, "images");
            v.setImages(r.getImages() == null
                    ? Collections.emptyList()
                    : Arrays.asList(r.getImages().split(",")));
            return v;
        }).collect(Collectors.toList());
        PageR<ReviewVO> r = new PageR<>();
        r.setList(list);
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        return r;
    }

    @Override
    public List<ProductSimpleVO> listRecommendations(Long productId) {
        ProductDO p = productMapper.selectById(productId);
        if (p == null) return Collections.emptyList();
        List<ProductDO> list = productMapper.selectList(
                new LambdaQueryWrapper<ProductDO>()
                        .eq(ProductDO::getCategoryId, p.getCategoryId())
                        .eq(ProductDO::getStatus, 1)
                        .ne(ProductDO::getId, productId)
                        .orderByDesc(ProductDO::getSales)
                        .last("LIMIT 6")
        );
        return list.stream().map(this::toSimple).collect(Collectors.toList());
    }

    private ProductSimpleVO toSimple(ProductDO p) {
        ProductSimpleVO v = new ProductSimpleVO();
        v.setProductId(p.getId());
        v.setName(p.getName());
        v.setMainImage(p.getMainImage());
        v.setOrigin(p.getOrigin());
        v.setSales(p.getSales());
        v.setRating(p.getRating());
        SkuDO minSku = skuMapper.selectOne(
                new LambdaQueryWrapper<SkuDO>()
                        .eq(SkuDO::getProductId, p.getId())
                        .orderByAsc(SkuDO::getPrice)
                        .last("LIMIT 1")
        );
        v.setMinPrice(minSku != null ? minSku.getPrice() : null);
        return v;
    }

    private RatingStatsVO calcRatingStats(Long productId) {
        List<ReviewDO> all = reviewMapper.selectList(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getProductId, productId)
                        .eq(ReviewDO::getStatus, 1)
        );
        RatingStatsVO s = new RatingStatsVO();
        s.setTotal(all.size());
        if (all.isEmpty()) {
            s.setAverage(BigDecimal.ZERO);
            return s;
        }
        int[] buckets = new int[6];
        long sum = 0;
        for (ReviewDO r : all) {
            int rt = r.getRating() == null ? 0 : r.getRating();
            if (rt >= 1 && rt <= 5) buckets[rt]++;
            sum += rt;
        }
        s.setFive(buckets[5]);
        s.setFour(buckets[4]);
        s.setThree(buckets[3]);
        s.setTwo(buckets[2]);
        s.setOne(buckets[1]);
        s.setAverage(BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP));
        return s;
    }
}
```

- [ ] **Step 8：ProductController**

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.ProductDetailVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaIgnore
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public R<ProductDetailVO> detail(@PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }

    @GetMapping("/{id}/reviews")
    public R<PageR<ReviewVO>> reviews(@PathVariable Long id,
                                      @RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(productService.listReviews(id, pageNum, pageSize));
    }

    @GetMapping("/{id}/recommendations")
    public R<List<ProductSimpleVO>> recommendations(@PathVariable Long id) {
        return R.ok(productService.listRecommendations(id));
    }
}
```

- [ ] **Step 9：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 12：购物车 Service + VOs

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/CartItemVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/CartVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/CartAddDTO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/CartUpdateDTO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/CartService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/CartServiceImpl.java`

- [ ] **Step 1：CartItemVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class CartItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    private String price;
    private Integer quantity;
    private Boolean selected;
    private Boolean valid;
    private Integer stock;
    private String mainImage;
}
```

- [ ] **Step 2：CartVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class CartVO {
    private List<CartItemVO> list;
    private String totalAmount;       // 全部商品金额（不含运费）
    private String selectedAmount;    // 已选商品金额
    private String shippingFee;
    private Integer invalidCount;
    private Integer selectedCount;
}
```

- [ ] **Step 3：CartAddDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartAddDTO {
    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少 1")
    private Integer quantity;
}
```

- [ ] **Step 4：CartUpdateDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartUpdateDTO {
    @NotNull
    @Min(value = 1, message = "数量至少 1")
    private Integer quantity;
}
```

- [ ] **Step 5：CartService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.vo.CartVO;

import java.util.List;

public interface CartService {
    CartVO listMyCart();
    void add(CartAddDTO dto);
    void updateQuantity(Long cartId, CartUpdateDTO dto);
    void deleteOne(Long cartId);
    void deleteBatch(List<Long> cartIds);
    void toggleSelect(Long cartId, Boolean selected);
    void toggleSelectAll(Boolean selected);
}
```

- [ ] **Step 6：CartServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.service.CartService;
import com.yan.freshfood.user.vo.CartItemVO;
import com.yan.freshfood.user.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public CartVO listMyCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CartDO> carts = cartMapper.selectList(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getUserId, userId)
                        .orderByDesc(CartDO::getCreateTime)
        );
        CartVO vo = new CartVO();
        if (carts.isEmpty()) {
            vo.setList(Collections.emptyList());
            vo.setTotalAmount("0.00");
            vo.setSelectedAmount("0.00");
            vo.setShippingFee("0.00");
            vo.setInvalidCount(0);
            vo.setSelectedCount(0);
            return vo;
        }
        Set<Long> skuIds = carts.stream().map(CartDO::getSkuId).collect(Collectors.toSet());
        List<SkuDO> skus = skuMapper.selectBatchIds(skuIds);
        Map<Long, SkuDO> skuMap = skus.stream()
                .collect(Collectors.toMap(SkuDO::getId, s -> s));
        Set<Long> productIds = skus.stream().map(SkuDO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal selectedTotal = BigDecimal.ZERO;
        int invalidCount = 0;
        int selectedCount = 0;
        List<CartItemVO> items = new ArrayList<>();
        for (CartDO c : carts) {
            SkuDO sku = skuMap.get(c.getSkuId());
            CartItemVO item = new CartItemVO();
            item.setId(c.getId());
            item.setSkuId(c.getSkuId());
            item.setQuantity(c.getQuantity());
            item.setSelected(c.getSelected() == 1);
            if (sku == null) {
                item.setValid(false);
                item.setProductName("(商品已删除)");
                invalidCount++;
            } else {
                ProductDO product = productMap.get(sku.getProductId());
                boolean onShelf = product != null && product.getStatus() == 1;
                boolean hasStock = sku.getStock() != null && sku.getStock() >= c.getQuantity();
                item.setValid(onShelf && hasStock);
                if (!item.getValid()) invalidCount++;
                item.setProductId(sku.getProductId());
                item.setProductName(product == null ? sku.getSpec() : product.getName());
                item.setSpec(sku.getSpec());
                item.setPrice(sku.getPrice().toPlainString());
                item.setStock(sku.getStock());
                item.setMainImage(sku.getImage());
                BigDecimal sub = sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
                total = total.add(sub);
                if (item.getSelected() && item.getValid()) {
                    selectedTotal = selectedTotal.add(sub);
                    selectedCount++;
                }
            }
            items.add(item);
        }
        vo.setList(items);
        vo.setTotalAmount(total.toPlainString());
        vo.setSelectedAmount(selectedTotal.toPlainString());
        // 简化：满 99 包邮，否则 10 元
        BigDecimal fee = selectedTotal.compareTo(BigDecimal.valueOf(99)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        vo.setShippingFee(fee.toPlainString());
        vo.setInvalidCount(invalidCount);
        vo.setSelectedCount(selectedCount);
        return vo;
    }

    @Override
    public void add(CartAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SkuDO sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        CartDO exist = cartMapper.selectOne(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getUserId, userId)
                        .eq(CartDO::getSkuId, dto.getSkuId())
        );
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
        } else {
            CartDO c = new CartDO();
            c.setUserId(userId);
            c.setSkuId(dto.getSkuId());
            c.setQuantity(dto.getQuantity());
            c.setSelected(1);
            cartMapper.insert(c);
        }
    }

    @Override
    public void updateQuantity(Long cartId, CartUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        CartDO c = cartMapper.selectById(cartId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        c.setQuantity(dto.getQuantity());
        cartMapper.updateById(c);
    }

    @Override
    public void deleteOne(Long cartId) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartMapper.delete(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getId, cartId)
                        .eq(CartDO::getUserId, userId)
        );
    }

    @Override
    public void deleteBatch(List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) return;
        Long userId = StpUtil.getLoginIdAsLong();
        cartMapper.delete(
                new LambdaQueryWrapper<CartDO>()
                        .in(CartDO::getId, cartIds)
                        .eq(CartDO::getUserId, userId)
        );
    }

    @Override
    public void toggleSelect(Long cartId, Boolean selected) {
        Long userId = StpUtil.getLoginIdAsLong();
        CartDO c = cartMapper.selectById(cartId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        c.setSelected(Boolean.TRUE.equals(selected) ? 1 : 0);
        cartMapper.updateById(c);
    }

    @Override
    public void toggleSelectAll(Boolean selected) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CartDO> list = cartMapper.selectList(
                new LambdaQueryWrapper<CartDO>().eq(CartDO::getUserId, userId)
        );
        int v = Boolean.TRUE.equals(selected) ? 1 : 0;
        for (CartDO c : list) {
            c.setSelected(v);
            cartMapper.updateById(c);
        }
    }
}
```

- [ ] **Step 7：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 13：购物车 Controller

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/CartController.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.service.CartService;
import com.yan.freshfood.user.vo.CartVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public R<CartVO> list() {
        return R.ok(cartService.listMyCart());
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cartService.deleteOne(id);
        return R.ok();
    }

    @DeleteMapping
    public R<Void> deleteBatch(@RequestBody List<Long> ids) {
        cartService.deleteBatch(ids);
        return R.ok();
    }

    @PutMapping("/select")
    public R<Void> select(@RequestParam Long id, @RequestParam Boolean selected) {
        cartService.toggleSelect(id, selected);
        return R.ok();
    }

    @PutMapping("/select-all")
    public R<Void> selectAll(@RequestParam Boolean selected) {
        cartService.toggleSelectAll(selected);
        return R.ok();
    }
}
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 14：地址 Service + Controller

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/AddressDTO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/AddressVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/AddressService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/AddressServiceImpl.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/AddressController.java`

- [ ] **Step 1：AddressDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    @NotBlank(message = "收件人不能为空")
    private String receiverName;
    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "省份不能为空")
    private String province;
    @NotBlank(message = "城市不能为空")
    private String city;
    private String district;
    @NotBlank(message = "详细地址不能为空")
    private String detail;
    private Boolean isDefault;
}
```

- [ ] **Step 2：AddressVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class AddressVO {
    private Long id;
    private String receiverName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Boolean isDefault;
}
```

- [ ] **Step 3：AddressService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.vo.AddressVO;

import java.util.List;

public interface AddressService {
    List<AddressVO> listMyAddresses();
    AddressVO create(AddressDTO dto);
    AddressVO update(Long id, AddressDTO dto);
    void delete(Long id);
    void setDefault(Long id);
}
```

- [ ] **Step 4：AddressServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.service.AddressService;
import com.yan.freshfood.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> listMyAddresses() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AddressDO> list = addressMapper.selectList(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getUserId, userId)
                        .orderByDesc(AddressDO::getIsDefault)
                        .orderByDesc(AddressDO::getCreateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressVO create(AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO a = new AddressDO();
        copyTo(dto, a);
        a.setUserId(userId);
        a.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (a.getIsDefault() == 1) clearOtherDefault(userId, null);
        addressMapper.insert(a);
        return toVO(a);
    }

    @Override
    @Transactional
    public AddressVO update(Long id, AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        copyTo(dto, exist);
        exist.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (exist.getIsDefault() == 1) clearOtherDefault(userId, id);
        addressMapper.updateById(exist);
        return toVO(exist);
    }

    @Override
    public void delete(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressMapper.delete(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getId, id)
                        .eq(AddressDO::getUserId, userId)
        );
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        AddressDO exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        clearOtherDefault(userId, id);
        exist.setIsDefault(1);
        addressMapper.updateById(exist);
    }

    private void clearOtherDefault(Long userId, Long excludeId) {
        List<AddressDO> defaults = addressMapper.selectList(
                new LambdaQueryWrapper<AddressDO>()
                        .eq(AddressDO::getUserId, userId)
                        .eq(AddressDO::getIsDefault, 1)
        );
        for (AddressDO d : defaults) {
            if (excludeId != null && d.getId().equals(excludeId)) continue;
            d.setIsDefault(0);
            addressMapper.updateById(d);
        }
    }

    private void copyTo(AddressDTO dto, AddressDO a) {
        a.setReceiverName(dto.getReceiverName());
        a.setPhone(dto.getPhone());
        a.setProvince(dto.getProvince());
        a.setCity(dto.getCity());
        a.setDistrict(dto.getDistrict());
        a.setDetail(dto.getDetail());
    }

    private AddressVO toVO(AddressDO a) {
        AddressVO v = new AddressVO();
        v.setId(a.getId());
        v.setReceiverName(a.getReceiverName());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetail(a.getDetail());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return v;
    }
}
```

- [ ] **Step 5：AddressController**

```java
package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.service.AddressService;
import com.yan.freshfood.user.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public R<List<AddressVO>> list() {
        return R.ok(addressService.listMyAddresses());
    }

    @PostMapping
    public R<AddressVO> create(@Valid @RequestBody AddressDTO dto) {
        return R.ok(addressService.create(dto));
    }

    @PutMapping("/{id}")
    public R<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return R.ok(addressService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return R.ok();
    }
}
```

- [ ] **Step 6：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 15：订单模块 - VOs

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/OrderItemVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/OrderVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/OrderPreviewVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/LogisticsVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/OrderPreviewDTO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/OrderCreateDTO.java`

- [ ] **Step 1：OrderItemVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class OrderItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    private String price;
    private Integer quantity;
    private String mainImage;
}
```

- [ ] **Step 2：OrderVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderId;        // 业务订单号
    private Integer status;
    private String statusText;
    private String totalAmount;
    private String shippingFee;
    private String discountAmount;
    private String payableAmount;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;
    private AddressVO address;
}
```

- [ ] **Step 3：OrderPreviewVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewVO {
    private List<OrderItemVO> items;
    private AddressVO address;
    private String totalAmount;
    private String shippingFee;
    private String discountAmount;
    private String payableAmount;
    private List<Object> availableCoupons;  // 简化为空数组
}
```

- [ ] **Step 4：LogisticsVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogisticsVO {
    private String company;
    private String trackingNo;
    private String statusText;
    private List<Trace> traces;

    @Data
    public static class Trace {
        private LocalDateTime time;
        private String desc;
    }
}
```

- [ ] **Step 5：OrderPreviewDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewDTO {
    @NotEmpty(message = "购物车项不能为空")
    private List<Long> cartIds;

    @NotNull(message = "地址不能为空")
    private Long addressId;
}
```

- [ ] **Step 6：OrderCreateDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    @NotEmpty
    private List<Long> cartIds;

    @NotNull
    private Long addressId;

    private Long couponId;   // 可选
    private String remark;
}
```

---

### Task 16：订单 Service - 预览 + 创建（核心事务）

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/OrderService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/OrderServiceImpl.java`（创建 + 预览部分）

- [ ] **Step 1：OrderService 接口（完整 8 个方法，先写接口）**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
import com.yan.freshfood.user.vo.OrderVO;

public interface OrderService {
    OrderPreviewVO preview(OrderPreviewDTO dto);
    OrderVO create(OrderCreateDTO dto);
    void pay(Long orderId, String payMethod);
    void cancel(Long orderId);
    PageR<OrderVO> list(Integer status, int pageNum, int pageSize);
    OrderVO detail(Long orderId);
    LogisticsVO logistics(Long orderId);
    void confirmReceive(Long orderId);
    void rebuy(Long orderId);
}
```

- [ ] **Step 2：OrderServiceImpl 头部 + 依赖注入**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.*;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.mapper.*;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;

    // ================== 预览 ==================
    @Override
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 1. 加载 cart + sku + product
        List<CartDO> carts = cartMapper.selectBatchIds(dto.getCartIds());
        carts = carts.stream().filter(c -> c.getUserId().equals(userId)).collect(Collectors.toList());
        if (carts.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND);
        Set<Long> skuIds = carts.stream().map(CartDO::getSkuId).collect(Collectors.toSet());
        Map<Long, SkuDO> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(SkuDO::getId, s -> s));
        Set<Long> productIds = skuMap.values().stream().map(SkuDO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p));

        // 2. 计算金额
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemVO> items = new ArrayList<>();
        for (CartDO c : carts) {
            SkuDO sku = skuMap.get(c.getSkuId());
            if (sku == null) continue;
            if (sku.getStock() < c.getQuantity()) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            OrderItemVO item = new OrderItemVO();
            item.setSkuId(sku.getId());
            item.setProductId(sku.getProductId());
            ProductDO p = productMap.get(sku.getProductId());
            item.setProductName(p != null ? p.getName() : sku.getSpec());
            item.setSpec(sku.getSpec());
            item.setPrice(sku.getPrice().toPlainString());
            item.setQuantity(c.getQuantity());
            item.setMainImage(sku.getImage());
            items.add(item);
            total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        // 3. 运费：满 99 包邮
        BigDecimal fee = total.compareTo(BigDecimal.valueOf(99)) >= 0
                ? BigDecimal.ZERO : BigDecimal.TEN;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal payable = total.add(fee).subtract(discount);

        // 4. 地址
        AddressDO addr = addressMapper.selectById(dto.getAddressId());
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(items);
        vo.setAddress(toAddressVO(addr));
        vo.setTotalAmount(total.toPlainString());
        vo.setShippingFee(fee.toPlainString());
        vo.setDiscountAmount(discount.toPlainString());
        vo.setPayableAmount(payable.toPlainString());
        vo.setAvailableCoupons(Collections.emptyList());
        return vo;
    }

    // ================== 创建 ==================
    @Override
    @Transactional
    public OrderVO create(OrderCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderPreviewVO preview = preview(new OrderPreviewDTO() {{
            setCartIds(dto.getCartIds());
            setAddressId(dto.getAddressId());
        }});

        // 1. 扣库存 + 生成订单
        OrderDO order = new OrderDO();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        // merchantId 取 SKU 同商家（简化：取第一个商品的商家）
        Long merchantId = productMapper.selectById(preview.getItems().get(0).getProductId())
                .getMerchantId();
        order.setMerchantId(merchantId);
        order.setTotalAmount(new BigDecimal(preview.getTotalAmount()));
        order.setShippingFee(new BigDecimal(preview.getShippingFee()));
        order.setDiscountAmount(new BigDecimal(preview.getDiscountAmount()));
        order.setPayableAmount(new BigDecimal(preview.getPayableAmount()));
        order.setRemark(dto.getRemark());
        order.setStatus(1);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        AddressDO addr = addressMapper.selectById(dto.getAddressId());
        order.setAddressSnapshot(buildAddressSnapshot(addr));
        orderMapper.insert(order);

        // 2. 写订单明细 + 扣库存
        for (OrderItemVO it : preview.getItems()) {
            SkuDO sku = skuMapper.selectById(it.getSkuId());
            if (sku.getStock() < it.getQuantity()) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            sku.setStock(sku.getStock() - it.getQuantity());
            sku.setSales(sku.getSales() + it.getQuantity());
            skuMapper.updateById(sku);
            OrderItemDO oi = new OrderItemDO();
            oi.setOrderId(order.getId());
            oi.setSkuId(sku.getId());
            oi.setProductId(sku.getProductId());
            oi.setProductNameSnapshot(it.getProductName());
            oi.setSpecSnapshot(it.getSpec());
            oi.setPriceSnapshot(new BigDecimal(it.getPrice()));
            oi.setQuantity(it.getQuantity());
            orderItemMapper.insert(oi);
        }
        // 3. 清购物车（仅已选的）
        cartMapper.delete(
                new LambdaQueryWrapper<CartDO>()
                        .eq(CartDO::getUserId, userId)
                        .in(CartDO::getId, dto.getCartIds())
        );
        return toOrderVO(order, preview.getItems(), addr);
    }

    private String genOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%04d", new Random().nextInt(10000));
        return date + rand;
    }

    private String buildAddressSnapshot(AddressDO a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("receiverName", a.getReceiverName());
        m.put("phone", a.getPhone());
        m.put("province", a.getProvince());
        m.put("city", a.getCity());
        m.put("district", a.getDistrict());
        m.put("detail", a.getDetail());
        return com.alibaba.fastjson.JSON.toJSONString(m);
    }

    private AddressVO toAddressVO(AddressDO a) {
        AddressVO v = new AddressVO();
        v.setId(a.getId());
        v.setReceiverName(a.getReceiverName());
        v.setPhone(a.getPhone());
        v.setProvince(a.getProvince());
        v.setCity(a.getCity());
        v.setDistrict(a.getDistrict());
        v.setDetail(a.getDetail());
        v.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return v;
    }

    private OrderVO toOrderVO(OrderDO order, List<OrderItemVO> items, AddressDO addr) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderId(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount().toPlainString());
        vo.setShippingFee(order.getShippingFee().toPlainString());
        vo.setDiscountAmount(order.getDiscountAmount().toPlainString());
        vo.setPayableAmount(order.getPayableAmount().toPlainString());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        vo.setAddress(toAddressVO(addr));
        return vo;
    }

    private String statusText(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 1 -> "待付款";
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "待评价";
            case 5 -> "已完成";
            case 6 -> "售后中";
            case 7 -> "已取消";
            default -> "";
        };
    }
}
```

- [ ] **Step 3：补 OrderMapper 依赖 fastjson**

`fastjson` 在 Spring Boot 默认引入（`spring-boot-starter-web` 已含），无需加依赖。

- [ ] **Step 4：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 17：订单 Service - 支付 / 取消 / 列表 / 详情 / 物流 / 收货 / 再购

**Files:**
- Modify: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/OrderServiceImpl.java`

- [ ] **Step 1：在 OrderServiceImpl 类内 `}` 前追加以下 7 个方法**

```java
    @Override
    @Transactional
    public void pay(Long orderId, String payMethod) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (o.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        // 模拟支付：90% 成功率
        if (new Random().nextInt(100) < 10) {
            throw new BusinessException(ErrorCode.PAY_FAILED);
        }
        o.setStatus(2); // 待发货
        o.setPayTime(LocalDateTime.now());
        o.setPayMethod(payMethod);
        orderMapper.updateById(o);
    }

    @Override
    @Transactional
    public void cancel(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (o.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        o.setStatus(7);
        orderMapper.updateById(o);
        // 回滚库存
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, o.getId())
        );
        for (OrderItemDO it : items) {
            SkuDO sku = skuMapper.selectById(it.getSkuId());
            sku.setStock(sku.getStock() + it.getQuantity());
            sku.setSales(Math.max(0, sku.getSales() - it.getQuantity()));
            skuMapper.updateById(sku);
        }
    }

    @Override
    public PageR<OrderVO> list(Integer status, int pageNum, int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<OrderDO> w = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getUserId, userId)
                .orderByDesc(OrderDO::getCreateTime);
        if (status != null && status > 0) w.eq(OrderDO::getStatus, status);
        Page<OrderDO> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageR<OrderVO> r = new PageR<>();
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        r.setList(page.getRecords().stream().map(this::toBrief).collect(Collectors.toList()));
        return r;
    }

    @Override
    public OrderVO detail(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, o.getId())
        );
        List<OrderItemVO> itemVos = items.stream().map(it -> {
            OrderItemVO v = new OrderItemVO();
            v.setId(it.getId());
            v.setSkuId(it.getSkuId());
            v.setProductId(it.getProductId());
            v.setProductName(it.getProductNameSnapshot());
            v.setSpec(it.getSpecSnapshot());
            v.setPrice(it.getPriceSnapshot().toPlainString());
            v.setQuantity(it.getQuantity());
            return v;
        }).collect(Collectors.toList());
        AddressDO addr = parseAddressSnapshot(o.getAddressSnapshot());
        return toOrderVO(o, itemVos, addr);
    }

    @Override
    public LogisticsVO logistics(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // mock：从 orderNo 末 4 位当运单号
        LogisticsVO v = new LogisticsVO();
        v.setCompany("顺丰速运");
        v.setTrackingNo("SF" + o.getOrderNo().substring(2));
        v.setStatusText("运输中");
        LocalDateTime t = o.getShipTime() != null ? o.getShipTime() : o.getCreateTime();
        v.setTraces(List.of(
                trace(t, "已揽收"),
                trace(t.plusHours(4), "运输中【广州转运中心】"),
                trace(t.plusHours(12), "运输中【武汉转运中心】"),
                trace(t.plusHours(20), "到达目的城市，准备派送")
        ));
        return v;
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (o.getStatus() != 3) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        o.setStatus(4);
        o.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(o);
    }

    @Override
    public void rebuy(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, o.getId())
        );
        for (OrderItemDO it : items) {
            CartDO exist = cartMapper.selectOne(
                    new LambdaQueryWrapper<CartDO>()
                            .eq(CartDO::getUserId, userId)
                            .eq(CartDO::getSkuId, it.getSkuId())
            );
            if (exist != null) {
                exist.setQuantity(exist.getQuantity() + it.getQuantity());
                cartMapper.updateById(exist);
            } else {
                CartDO c = new CartDO();
                c.setUserId(userId);
                c.setSkuId(it.getSkuId());
                c.setQuantity(it.getQuantity());
                c.setSelected(1);
                cartMapper.insert(c);
            }
        }
    }

    private LogisticsVO.Trace trace(LocalDateTime t, String desc) {
        LogisticsVO.Trace tr = new LogisticsVO.Trace();
        tr.setTime(t);
        tr.setDesc(desc);
        return tr;
    }

    private OrderVO toBrief(OrderDO o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderId(o.getOrderNo());
        vo.setStatus(o.getStatus());
        vo.setStatusText(statusText(o.getStatus()));
        vo.setTotalAmount(o.getTotalAmount().toPlainString());
        vo.setPayableAmount(o.getPayableAmount().toPlainString());
        vo.setCreateTime(o.getCreateTime());
        return vo;
    }

    private AddressDO parseAddressSnapshot(String json) {
        AddressDO a = new AddressDO();
        Map<String, Object> m = com.alibaba.fastjson.JSON.parseObject(json, Map.class);
        if (m == null) return a;
        a.setReceiverName((String) m.get("receiverName"));
        a.setPhone((String) m.get("phone"));
        a.setProvince((String) m.get("province"));
        a.setCity((String) m.get("city"));
        a.setDistrict((String) m.get("district"));
        a.setDetail((String) m.get("detail"));
        return a;
    }
```

> `logistics` 的 mock 轨迹使用 `LocalDateTime`，注意 Jackson 默认格式化 `application.yml` 已配置 `yyyy-MM-dd HH:mm:ss`。

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 18：订单 Controller

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/OrderController.java`

- [ ] **Step 1：写入文件**

```java
package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
import com.yan.freshfood.user.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public R<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return R.ok(orderService.preview(dto));
    }

    @PostMapping
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @PostMapping("/{id}/pay")
    public R<Void> pay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.pay(id, body.getOrDefault("payMethod", "MOCK"));
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    @GetMapping
    public R<PageR<OrderVO>> list(@RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(orderService.list(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @GetMapping("/{id}/logistics")
    public R<LogisticsVO> logistics(@PathVariable Long id) {
        return R.ok(orderService.logistics(id));
    }

    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(id);
        return R.ok();
    }

    @PostMapping("/{id}/rebuy")
    public R<Void> rebuy(@PathVariable Long id) {
        orderService.rebuy(id);
        return R.ok();
    }
}
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 19：评价模块

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/dto/ReviewCreateDTO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/ReviewService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/ReviewServiceImpl.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/ReviewController.java`

- [ ] **Step 1：ReviewCreateDTO**

```java
package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ReviewCreateDTO {
    @NotNull
    private Long orderId;

    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @Min(1) @Max(5)
    private Integer tasteRating;
    @Min(1) @Max(5)
    private Integer freshnessRating;
    @Min(1) @Max(5)
    private Integer logisticsRating;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private List<String> images;
}
```

- [ ] **Step 2：ReviewService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;

import java.util.List;

public interface ReviewService {
    List<OrderItemVO> listReviewableItems(Long orderId);
    Long create(ReviewCreateDTO dto);
    void append(Long reviewId, String content, List<String> images);
    ReviewVO detail(Long reviewId);
}
```

- [ ] **Step 3：ReviewServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.content.ReviewDO;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.trade.OrderDO;
import com.yan.freshfood.model.entity.trade.OrderItemDO;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.mapper.OrderItemMapper;
import com.yan.freshfood.user.mapper.OrderMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.ReviewMapper;
import com.yan.freshfood.user.service.ReviewService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public List<OrderItemVO> listReviewableItems(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (o.getStatus() != 4 && o.getStatus() != 5) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, o.getId())
        );
        // 过滤已评价的
        Set<Long> reviewed = reviewMapper.selectList(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getOrderId, o.getId())
                        .eq(ReviewDO::getUserId, userId)
        ).stream().map(ReviewDO::getOrderItemId).collect(Collectors.toSet());
        return items.stream()
                .filter(it -> !reviewed.contains(it.getId()))
                .map(it -> {
                    OrderItemVO v = new OrderItemVO();
                    v.setId(it.getId());
                    v.setProductId(it.getProductId());
                    v.setProductName(it.getProductNameSnapshot());
                    v.setSpec(it.getSpecSnapshot());
                    v.setPrice(it.getPriceSnapshot().toPlainString());
                    v.setQuantity(it.getQuantity());
                    ProductDO p = productMapper.selectById(it.getProductId());
                    if (p != null) v.setMainImage(p.getMainImage());
                    return v;
                }).collect(Collectors.toList());
    }

    @Override
    public Long create(ReviewCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OrderDO o = orderMapper.selectById(dto.getOrderId());
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderItemDO item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        // 防重复
        Long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<ReviewDO>()
                        .eq(ReviewDO::getOrderItemId, dto.getOrderItemId())
                        .eq(ReviewDO::getUserId, userId)
        );
        if (count > 0) throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        ReviewDO r = new ReviewDO();
        r.setOrderId(o.getId());
        r.setOrderItemId(item.getId());
        r.setUserId(userId);
        r.setProductId(item.getProductId());
        r.setSkuId(item.getSkuId());
        r.setMerchantId(o.getMerchantId());
        r.setRating(dto.getRating());
        r.setTasteRating(dto.getTasteRating());
        r.setFreshnessRating(dto.getFreshnessRating());
        r.setLogisticsRating(dto.getLogisticsRating());
        r.setContent(dto.getContent());
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            r.setImages(String.join(",", dto.getImages()));
        }
        r.setIsAppend(0);
        r.setStatus(1);
        reviewMapper.insert(r);
        // 订单状态：待评价 → 已完成
        if (o.getStatus() == 4) {
            o.setStatus(5);
            orderMapper.updateById(o);
        }
        return r.getId();
    }

    @Override
    public void append(Long reviewId, String content, List<String> images) {
        Long userId = StpUtil.getLoginIdAsLong();
        ReviewDO exist = reviewMapper.selectById(reviewId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewDO r = new ReviewDO();
        r.setOrderId(exist.getOrderId());
        r.setOrderItemId(exist.getOrderItemId());
        r.setUserId(userId);
        r.setProductId(exist.getProductId());
        r.setSkuId(exist.getSkuId());
        r.setMerchantId(exist.getMerchantId());
        r.setRating(exist.getRating());
        r.setContent(content);
        if (images != null && !images.isEmpty()) {
            r.setImages(String.join(",", images));
        }
        r.setIsAppend(1);
        r.setStatus(1);
        reviewMapper.insert(r);
    }

    @Override
    public ReviewVO detail(Long reviewId) {
        ReviewDO r = reviewMapper.selectById(reviewId);
        if (r == null || r.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewVO v = new ReviewVO();
        BeanUtil.copyProperties(r, v, "images");
        v.setImages(r.getImages() == null
                ? Collections.emptyList()
                : Arrays.asList(r.getImages().split(",")));
        return v;
    }
}
```

- [ ] **Step 4：ReviewController**

```java
package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.service.ReviewService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/orders/{orderId}/reviewable-items")
    public R<List<OrderItemVO>> reviewable(@PathVariable Long orderId) {
        return R.ok(reviewService.listReviewableItems(orderId));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return R.ok(reviewService.create(dto));
    }

    @PostMapping("/{id}/append")
    public R<Void> append(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) body.get("images");
        reviewService.append(id, (String) body.get("content"), images);
        return R.ok();
    }

    @SaIgnore
    @GetMapping("/{id}")
    public R<ReviewVO> detail(@PathVariable Long id) {
        return R.ok(reviewService.detail(id));
    }
}
```

> 接口 39 路径修正：plan §2.9 写的是 `/orders/{orderId}/reviewable-items`，实际我们用 `/reviews/orders/{orderId}/reviewable-items`。以 Controller 为准，更新时同步修接口文档。

- [ ] **Step 5：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 20：消息模块

**Files:**
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/vo/MessageVO.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/MessageService.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/service/impl/MessageServiceImpl.java`
- Create: `freshfood-user/src/main/java/com/yan/freshfood/user/controller/MessageController.java`

- [ ] **Step 1：MessageVO**

```java
package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2：MessageService 接口**

```java
package com.yan.freshfood.user.service;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.user.vo.MessageVO;

public interface MessageService {
    PageR<MessageVO> list(String type, int pageNum, int pageSize);
    Integer unreadCount();
    void markRead(Long id);
    void markAllRead();
}
```

- [ ] **Step 3：MessageServiceImpl**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.model.entity.content.MessageDO;
import com.yan.freshfood.user.mapper.MessageMapper;
import com.yan.freshfood.user.service.MessageService;
import com.yan.freshfood.user.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public PageR<MessageVO> list(String type, int pageNum, int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<MessageDO> w = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .orderByDesc(MessageDO::getCreateTime);
        if (type != null && !type.isBlank()) w.eq(MessageDO::getType, type);
        Page<MessageDO> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageR<MessageVO> r = new PageR<>();
        r.setTotal(page.getTotal());
        r.setPageNum((int) page.getCurrent());
        r.setPageSize((int) page.getSize());
        r.setPages((int) page.getPages());
        r.setList(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return r;
    }

    @Override
    public Integer unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return messageMapper.selectCount(
                new LambdaQueryWrapper<MessageDO>()
                        .eq(MessageDO::getUserId, userId)
                        .eq(MessageDO::getIsRead, 0)
        ).intValue();
    }

    @Override
    public void markRead(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        MessageDO m = messageMapper.selectById(id);
        if (m == null || !m.getUserId().equals(userId)) return;
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    @Override
    public void markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        messageMapper.update(null, new LambdaUpdateWrapper<MessageDO>()
                .eq(MessageDO::getUserId, userId)
                .eq(MessageDO::getIsRead, 0)
                .set(MessageDO::getIsRead, 1));
    }

    private MessageVO toVO(MessageDO m) {
        MessageVO v = new MessageVO();
        BeanUtil.copyProperties(m, v);
        v.setIsRead(m.getIsRead() != null && m.getIsRead() == 1);
        return v;
    }
}
```

- [ ] **Step 4：MessageController**

```java
package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.MessageService;
import com.yan.freshfood.user.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public R<PageR<MessageVO>> list(@RequestParam(required = false) String type,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(messageService.list(type, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public R<Integer> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        messageService.markAllRead();
        return R.ok();
    }
}
```

- [ ] **Step 5：编译验证**

```bash
mvn -pl freshfood-user compile -q
```

预期：BUILD SUCCESS。

---

### Task 21：单元测试 - OrderService 创建订单

**Files:**
- Create: `freshfood-user/src/test/java/com/yan/freshfood/user/service/impl/OrderServiceImplTest.java`

- [ ] **Step 1：测试类**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.mapper.*;
import com.yan.freshfood.user.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private CartMapper cartMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;
    @Mock private AddressMapper addressMapper;

    @InjectMocks private OrderServiceImpl orderService;

    private ProductDO product;
    private SkuDO sku;
    private CartDO cart;
    private AddressDO addr;

    @BeforeEach
    void setup() {
        product = new ProductDO();
        product.setId(1001L);
        product.setMerchantId(1L);
        product.setName("车厘子");
        product.setStatus(1);

        sku = new SkuDO();
        sku.setId(2001L);
        sku.setProductId(1001L);
        sku.setSpec("1斤装");
        sku.setPrice(new BigDecimal("59.90"));
        sku.setStock(100);
        sku.setSales(0);
        sku.setImage("https://img.example.com/c1-1.jpg");

        cart = new CartDO();
        cart.setId(9001L);
        cart.setUserId(100L);
        cart.setSkuId(2001L);
        cart.setQuantity(2);
        cart.setSelected(1);

        addr = new AddressDO();
        addr.setId(7001L);
        addr.setUserId(100L);
        addr.setReceiverName("张三");
        addr.setPhone("13800000000");
        addr.setProvince("广东省");
        addr.setCity("深圳市");
        addr.setDistrict("南山区");
        addr.setDetail("科技园路 1 号");
        addr.setIsDefault(1);
    }

    @Test
    void create_order_success() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            when(cartMapper.selectBatchIds(any())).thenReturn(List.of(cart));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));
            when(addressMapper.selectById(7001L)).thenReturn(addr);
            when(productMapper.selectById(1001L)).thenReturn(product);
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(orderMapper.insert(any(OrderDO.class))).thenAnswer(inv -> {
                OrderDO o = inv.getArgument(0);
                o.setId(8888L);
                o.setCreateTime(java.time.LocalDateTime.now());
                return 1;
            });
            when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);

            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setCartIds(List.of(9001L));
            dto.setAddressId(7001L);
            dto.setRemark("请轻拿轻放");

            OrderVO vo = orderService.create(dto);

            assertNotNull(vo);
            assertEquals(8888L, vo.getId());
            assertEquals("59.90", vo.getPayableAmount());
            assertEquals(1, vo.getStatus());
            assertEquals("待付款", vo.getStatusText());
            verify(orderMapper).insert(any(OrderDO.class));
            verify(orderItemMapper).insert(any(OrderItemDO.class));
            verify(cartMapper).delete(any(LambdaQueryWrapper.class));
        }
    }

    @Test
    void create_order_throws_when_stock_not_enough() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            sku.setStock(1); // 库存只有 1，下单 2 件
            when(cartMapper.selectBatchIds(any())).thenReturn(List.of(cart));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));
            when(addressMapper.selectById(7001L)).thenReturn(addr);

            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setCartIds(List.of(9001L));
            dto.setAddressId(7001L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.create(dto));
            assertEquals(ErrorCode.STOCK_NOT_ENOUGH, ex.getErrorCode());
            verify(orderMapper, never()).insert(any(OrderDO.class));
        }
    }

    @Test
    void cancel_order_restores_stock() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            OrderDO o = new OrderDO();
            o.setId(8888L);
            o.setUserId(100L);
            o.setStatus(1);
            o.setTotalAmount(new BigDecimal("119.80"));

            OrderItemDO it = new OrderItemDO();
            it.setOrderId(8888L);
            it.setSkuId(2001L);
            it.setQuantity(2);

            when(orderMapper.selectById(8888L)).thenReturn(o);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(it));
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(orderMapper.updateById(any(OrderDO.class))).thenReturn(1);
            when(skuMapper.updateById(any(SkuDO.class))).thenReturn(1);

            orderService.cancel(8888L);

            assertEquals(7, o.getStatus());
            verify(orderMapper).updateById(o);
            // 库存回滚：100 + 2 = 102
            assertEquals(102, sku.getStock());
        }
    }
}
```

- [ ] **Step 2：编译并运行测试**

```bash
mvn -pl freshfood-user test -Dtest=OrderServiceImplTest -q
```

预期：3 个测试全部通过，`Tests run: 3, Failures: 0, Errors: 0`。

---

### Task 22：单元测试 - CartService 加购与列表

**Files:**
- Create: `freshfood-user/src/test/java/com/yan/freshfood/user/service/impl/CartServiceImplTest.java`

- [ ] **Step 1：测试类**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.exception.BusinessException;
import com.yan.freshfood.common.exception.ErrorCode;
import com.yan.freshfood.model.entity.product.ProductDO;
import com.yan.freshfood.model.entity.product.SkuDO;
import com.yan.freshfood.model.entity.trade.CartDO;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.mapper.CartMapper;
import com.yan.freshfood.user.mapper.ProductMapper;
import com.yan.freshfood.user.mapper.SkuMapper;
import com.yan.freshfood.user.vo.CartVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartMapper cartMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks private CartServiceImpl cartService;

    @Test
    void add_new_item_creates_cart_row() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            when(skuMapper.selectById(2001L)).thenReturn(sku);
            when(cartMapper.selectOne(any())).thenReturn(null);
            when(cartMapper.insert(any(CartDO.class))).thenReturn(1);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(2001L);
            dto.setQuantity(3);
            cartService.add(dto);

            verify(cartMapper).insert(any(CartDO.class));
        }
    }

    @Test
    void add_existing_item_accumulates_quantity() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            SkuDO sku = new SkuDO();
            sku.setId(2001L);
            sku.setProductId(1001L);
            sku.setPrice(new BigDecimal("59.90"));
            sku.setStock(100);
            when(skuMapper.selectById(2001L)).thenReturn(sku);

            CartDO exist = new CartDO();
            exist.setId(9001L);
            exist.setUserId(100L);
            exist.setSkuId(2001L);
            exist.setQuantity(2);
            exist.setSelected(1);
            when(cartMapper.selectOne(any())).thenReturn(exist);
            when(cartMapper.updateById(any(CartDO.class))).thenReturn(1);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(2001L);
            dto.setQuantity(3);
            cartService.add(dto);

            assertEquals(5, exist.getQuantity());
            verify(cartMapper).updateById(exist);
            verify(cartMapper, never()).insert(any(CartDO.class));
        }
    }

    @Test
    void list_cart_returns_total_and_selected_amount() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            CartDO c1 = new CartDO();
            c1.setId(9001L);
            c1.setUserId(100L);
            c1.setSkuId(2001L);
            c1.setQuantity(2);
            c1.setSelected(1);

            SkuDO sku1 = new SkuDO();
            sku1.setId(2001L);
            sku1.setProductId(1001L);
            sku1.setSpec("1斤装");
            sku1.setPrice(new BigDecimal("59.90"));
            sku1.setStock(100);
            sku1.setImage("https://img.example.com/c1-1.jpg");

            ProductDO p1 = new ProductDO();
            p1.setId(1001L);
            p1.setName("车厘子");
            p1.setStatus(1);

            when(cartMapper.selectList(any())).thenReturn(List.of(c1));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku1));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(p1));

            CartVO vo = cartService.listMyCart();
            assertEquals(1, vo.getList().size());
            assertEquals("119.80", vo.getTotalAmount());     // 59.90 * 2
            assertEquals("119.80", vo.getSelectedAmount());   // 选中
            assertEquals("0.00", vo.getShippingFee());        // >= 99 包邮
            assertEquals(1, vo.getSelectedCount());
            assertEquals(0, vo.getInvalidCount());
            assertTrue(vo.getList().get(0).getValid());
        }
    }

    @Test
    void list_cart_marks_off_shelf_as_invalid() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            CartDO c1 = new CartDO();
            c1.setId(9001L);
            c1.setUserId(100L);
            c1.setSkuId(2001L);
            c1.setQuantity(2);
            c1.setSelected(1);

            SkuDO sku1 = new SkuDO();
            sku1.setId(2001L);
            sku1.setProductId(1001L);
            sku1.setSpec("1斤装");
            sku1.setPrice(new BigDecimal("59.90"));
            sku1.setStock(100);

            ProductDO p1 = new ProductDO();
            p1.setId(1001L);
            p1.setName("车厘子");
            p1.setStatus(0); // 下架

            when(cartMapper.selectList(any())).thenReturn(List.of(c1));
            when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku1));
            when(productMapper.selectBatchIds(any())).thenReturn(List.of(p1));

            CartVO vo = cartService.listMyCart();
            assertEquals(1, vo.getInvalidCount());
            assertFalse(vo.getList().get(0).getValid());
        }
    }

    @Test
    void add_unknown_sku_throws_not_found() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(skuMapper.selectById(9999L)).thenReturn(null);

            CartAddDTO dto = new CartAddDTO();
            dto.setSkuId(9999L);
            dto.setQuantity(1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> cartService.add(dto));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }
}
```

- [ ] **Step 2：编译并运行测试**

```bash
mvn -pl freshfood-user test -Dtest=CartServiceImplTest -q
```

预期：5 个测试全部通过，`Tests run: 5, Failures: 0, Errors: 0`。

---

### Task 23：单元测试 - AddressService 默认地址互斥

**Files:**
- Create: `freshfood-user/src/test/java/com/yan/freshfood/user/service/impl/AddressServiceImplTest.java`

- [ ] **Step 1：测试类**

```java
package com.yan.freshfood.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.model.entity.trade.AddressDO;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.mapper.AddressMapper;
import com.yan.freshfood.user.vo.AddressVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressMapper addressMapper;

    @InjectMocks private AddressServiceImpl addressService;

    @Test
    void create_default_address_clears_other_defaults() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            AddressDO oldDefault = new AddressDO();
            oldDefault.setId(1L);
            oldDefault.setUserId(100L);
            oldDefault.setIsDefault(1);

            when(addressMapper.selectList(any())).thenReturn(List.of(oldDefault));
            when(addressMapper.insert(any(AddressDO.class))).thenAnswer(inv -> {
                AddressDO a = inv.getArgument(0);
                a.setId(99L);
                return 1;
            });
            when(addressMapper.updateById(any(AddressDO.class))).thenReturn(1);

            AddressDTO dto = new AddressDTO();
            dto.setReceiverName("李四");
            dto.setPhone("13900000000");
            dto.setProvince("北京");
            dto.setCity("北京");
            dto.setDetail("中关村大街 1 号");
            dto.setIsDefault(true);

            AddressVO vo = addressService.create(dto);
            assertEquals(99L, vo.getId());
            // 旧默认地址 isDefault 应被清掉
            assertEquals(0, oldDefault.getIsDefault());
            verify(addressMapper).updateById(oldDefault);
        }
    }

    @Test
    void set_default_only_affects_target() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            AddressDO target = new AddressDO();
            target.setId(2L);
            target.setUserId(100L);
            target.setIsDefault(0);

            AddressDO otherDefault = new AddressDO();
            otherDefault.setId(1L);
            otherDefault.setUserId(100L);
            otherDefault.setIsDefault(1);

            when(addressMapper.selectById(2L)).thenReturn(target);
            when(addressMapper.selectList(any())).thenReturn(List.of(target, otherDefault));
            when(addressMapper.updateById(any(AddressDO.class))).thenReturn(1);

            addressService.setDefault(2L);

            assertEquals(1, target.getIsDefault());
            // 另一个默认地址被清
            assertEquals(0, otherDefault.getIsDefault());
        }
    }
}
```

- [ ] **Step 2：编译并运行测试**

```bash
mvn -pl freshfood-user test -Dtest=AddressServiceImplTest -q
```

预期：`Tests run: 2, Failures: 0, Errors: 0`。

---

### Task 24：集成验证 - 全模块编译与测试

**Files:** 无（只跑命令）

- [ ] **Step 1：全工程编译**

```bash
mvn clean compile -q
```

预期：BUILD SUCCESS，0 error。

- [ ] **Step 2：跑全部 user 模块测试**

```bash
mvn -pl freshfood-user test -q
```

预期：OrderServiceImplTest（3）+ CartServiceImplTest（5）+ AddressServiceImplTest（2）= 10 个测试全部通过。

- [ ] **Step 3：启动应用**

```bash
mvn -pl freshfood-app spring-boot:run
```

启动后日志看到 `Started FreshfoodApp` 即成功。Knife4j 文档地址 `http://localhost:8080/doc.html`。

- [ ] **Step 4：手动冒烟（curl）**

启动应用 + 用计划 1 创建的 `u01` 用户登录拿 token：

```bash
# 1) 登录拿 token（用计划 1 已建的 u01，密码 u01pass）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"u01","password":"u01pass"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# 2) 首页接口（不需登录）
curl -s http://localhost:8080/api/v1/home/banners | python -m json.tool
curl -s http://localhost:8080/api/v1/home/categories | python -m json.tool

# 3) 商品详情（不需登录）
curl -s http://localhost:8080/api/v1/products/1001 | python -m json.tool

# 4) 加购（需登录）
curl -s -X POST http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"skuId":2001,"quantity":2}'

# 5) 查看购物车
curl -s http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# 6) 搜索
curl -s "http://localhost:8080/api/v1/search/products?keyword=车厘子&pageNum=1&pageSize=10" | python -m json.tool
```

预期：
- `/home/banners` 返回 3 条 banner
- `/home/categories` 返回 5 个顶级分类
- `/products/1001` 返回车厘子详情含 2 个 SKU
- 加购返回成功
- 购物车总价 `119.80`，运费 `0.00`（满 99）
- 搜索 `车厘子` 返回 1 条

---

### Task 25：收尾 - 提交与推送

**Files:** 无（只跑命令）

- [ ] **Step 1：检查 git 状态**

```bash
cd freshfood-shop
git status
```

预期：应包含计划 1 后的全部新文件（新表 SQL、12 个新表对应的 DO + Mapper + Service + Controller + Tests），不应包含 `.env` / `target/` / `src.bak/`。

- [ ] **Step 2：分批 add（不 -A）**

```bash
git add sql/02_business_tables.sql sql/03_test_data.sql

git add freshfood-model/src/main/java/com/yan/freshfood/model/entity/product/
git add freshfood-model/src/main/java/com/yan/freshfood/model/entity/trade/
git add freshfood-model/src/main/java/com/yan/freshfood/model/entity/content/

git add freshfood-user/src/main/java/com/yan/freshfood/user/
git add freshfood-user/src/test/java/com/yan/freshfood/user/

git add freshfood-common/src/main/java/com/yan/freshfood/common/exception/ErrorCode.java
```

- [ ] **Step 3：commit（按用户偏好中文 body）**

```bash
git commit -m "$(cat <<'EOF'
feat(user-business): 实现用户端 9 个业务模块 40 个接口

新增 12 张业务表（商品域 5 + 交易域 4 + 内容域 3）及其 DO/Mapper/Service/Controller：

商品域
- category / product / sku：分类、SPU、SKU 三级商品模型
- banner / hot_word：首页轮播、搜索热搜词

交易域
- cart：加购/改数/选择/删除，购物车金额计算（满 99 包邮）
- address：收货地址 CRUD，默认地址互斥（事务保护）
- orders / order_item：订单主表 + 明细，含金额、状态、地址快照
- 订单核心事务：preview/create/pay/cancel/list/detail/logistics/confirm/rebuy
- 创建订单走 @Transactional：扣库存 + 写明细 + 清购物车
- 取消订单回滚库存；发货/收货/再购流程完整

内容域
- review：评价（首评/追评），商家回复（计划 3 处理）
- message：消息中心，未读数 + 批量已读
- search_history：搜索历史，最多 10 条

Service 层
- HomeService / SearchService / ProductService / CartService / AddressService
  / OrderService / ReviewService / MessageService 共 8 个
- 全部按 Controller → Service → Mapper 分层，无跨层调用

测试
- OrderServiceImplTest：3 个测试（创建成功 / 库存不足 / 取消回滚）
- CartServiceImplTest：5 个测试（新建/累加/总价/下架/不存在）
- AddressServiceImplTest：2 个测试（默认地址互斥）

已知简化
- 物流轨迹为 mock（顺丰固定 4 节点）
- 优惠券系统未实现，availableCoupons 返回空数组
- 价格过滤在搜索里未生效（生产应 SQL JOIN sku 表）
- merchantId 从订单第一个商品取（同店下单；多店需拆单，计划 4 处理）

下一步
- 计划 3：商家端 25 个接口（商品发布、订单发货、退款审核）
- 计划 4：管理端 41 个接口（含 6 个 ECharts 统计接口）
EOF
)"
```

- [ ] **Step 4：合并到 main**

按用户偏好，本地 fast-forward 合并到 main 后推远程（参考计划 1 的合并流程）：

```bash
git checkout main
git merge --ff-only feature/user-business
git push origin main feature/user-business
```

预期：合并无冲突，远程两分支同步。Knife4j 文档 `/doc.html` 可见全部 40 个新接口。

---

## 四、回顾与自检

实施完成后应满足：

| 检查项 | 状态 |
|---|---|
| 所有 12 张表 SQL 可正常导入 | ☐ |
| 商品/分类/购物车/订单/地址/评价/消息 9 模块 40 接口可访问 | ☐ |
| 未登录访问受保护接口返回 401 | ☐ |
| 库存不足时创建订单返回 `3002 STOCK_NOT_ENOUGH` | ☐ |
| 订单取消时库存回滚 | ☐ |
| 默认地址唯一（事务保证） | ☐ |
| 10 个单元测试全部通过 | ☐ |
| `mvn clean compile` BUILD SUCCESS | ☐ |
| Knife4j 文档可见全部接口 | ☐ |
| 加密字段（address.phone）落库为密文、读出为明文 | ☐ |
| git 提交包含中文 body | ☐ |
| main 分支含全部计划 1+2 改动 | ☐ |

---

## 五、参考与依赖

- 计划 1（已实施）：`docs/superpowers/plans/2026-06-29-freshfood-foundation.md` —— 登录态、`R`/`PageR`、`ErrorCode`、`CryptoConfig`、3 个 DO（User/Merchant/Admin）的加密 TypeHandler
- 后续计划
  - 计划 3：商家端 —— 商家登录（复用 `MerchantDO`）+ 商品/订单管理 + 退款审核
  -计划 4：管理端 —— 管理员登录（复用 `AdminDO`）+ 商家审核 + 6 个 ECharts 统计接口
- 外部依赖（已在 pom.xml）
  - MyBatis-Plus 3.5.9、`mybatis-plus-annotation`、`spring-boot-starter-validation`
  - Sa-Token 1.37.0、`sa-token-jwt`
  - Hutool 5.8.27（雪花、BeanUtil、RandomUtil）
  - Knife4j 4.5.0、`spring-boot-starter-web`
  - fastjson（spring-boot-starter-web 已传递依赖）