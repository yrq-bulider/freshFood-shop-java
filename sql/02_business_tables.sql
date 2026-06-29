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
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
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
