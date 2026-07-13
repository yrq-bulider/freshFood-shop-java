-- ========================================
-- 线上生鲜商场 - 数据库初始化（精简版，2026-07-11）
-- 9 张表，覆盖用户端 + 商家端主链路
-- 数据库：freshfood_shop
-- 演示账号通过 /api/v1/auth/register 自助创建（密码统一 123456）
-- ========================================

CREATE DATABASE IF NOT EXISTS freshfood_shop
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE freshfood_shop;

-- ============================================================
-- 0. 先 DROP 所有表（按依赖反向）
-- ============================================================
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart`;
DROP TABLE IF EXISTS `sku`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `merchant`;
DROP TABLE IF EXISTS `user`;

-- ============================================================
-- 1. 账号表（2 张）
-- ============================================================

CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `phone`       VARCHAR(255) DEFAULT NULL COMMENT '手机号（AES-256-CBC 加密，Base64）',
    `email`       VARCHAR(255) DEFAULT NULL COMMENT '邮箱（AES-256-CBC 加密，Base64）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `merchant` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `username`       VARCHAR(50)  NOT NULL,
    `password`       VARCHAR(100) NOT NULL,
    `shop_name`      VARCHAR(100) NOT NULL COMMENT '店铺名',
    `contact_name`   VARCHAR(255) DEFAULT NULL COMMENT '联系人（AES-256-CBC 加密）',
    `contact_phone`  VARCHAR(255) DEFAULT NULL COMMENT '联系电话（AES-256-CBC 加密）',
    `logo`           VARCHAR(255) DEFAULT NULL,
    `audit_status`   TINYINT      NOT NULL DEFAULT 1 COMMENT '0 待审核 / 1 通过 / 2 拒绝（演示数据默认通过）',
    `status`         TINYINT      NOT NULL DEFAULT 1,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- ============================================================
-- 2. 商品域（3 张）
-- ============================================================

CREATE TABLE `category` (
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

CREATE TABLE `product` (
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

CREATE TABLE `sku` (
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

-- ============================================================
-- 3. 交易域（3 张）
-- ============================================================

CREATE TABLE `cart` (
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

CREATE TABLE `orders` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`          VARCHAR(32)   NOT NULL COMMENT '业务订单号 yyyyMMdd+4位',
    `user_id`           BIGINT        NOT NULL,
    `merchant_id`       BIGINT        NOT NULL,
    `total_amount`      DECIMAL(10,2) NOT NULL,
    `shipping_fee`      DECIMAL(10,2) NOT NULL DEFAULT 0,
    `discount_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0,
    `payable_amount`    DECIMAL(10,2) NOT NULL,
    `receiver_name`     VARCHAR(255)  NOT NULL COMMENT '收货人（AES-256-CBC 加密）',
    `receiver_phone`    VARCHAR(255)  NOT NULL COMMENT '收货电话（AES-256-CBC 加密）',
    `receiver_address`  VARCHAR(255)  NOT NULL COMMENT '收货地址',
    `remark`            VARCHAR(500)  DEFAULT NULL,
    `status`            TINYINT       NOT NULL DEFAULT 1 COMMENT '1待付/2待发/3待收/4完成/5取消',
    `expire_time`       DATETIME      DEFAULT NULL COMMENT '待付款过期时间',
    `pay_time`          DATETIME      DEFAULT NULL,
    `ship_time`         DATETIME      DEFAULT NULL,
    `confirm_time`      DATETIME      DEFAULT NULL,
    `tracking_no`       VARCHAR(50)   DEFAULT NULL COMMENT '物流单号',
    `carrier`           VARCHAR(20)   DEFAULT NULL COMMENT '物流公司',
    `pay_method`        VARCHAR(20)   DEFAULT NULL,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_merchant_status` (`merchant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE `order_item` (
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`              BIGINT        NOT NULL,
    `sku_id`                BIGINT        NOT NULL,
    `product_id`            BIGINT        NOT NULL,
    `product_name_snapshot` VARCHAR(200)  NOT NULL,
    `spec_snapshot`         VARCHAR(100)  NOT NULL,
    `price_snapshot`        DECIMAL(10,2) NOT NULL,
    `quantity`              INT           NOT NULL,
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- ============================================================
-- 4. 评价域（1 张）
-- ============================================================

CREATE TABLE `review` (
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

-- ============================================================
-- 5. 建表完成
-- ============================================================
SELECT 'Schema initialized (slim). 9 tables created.' AS status;