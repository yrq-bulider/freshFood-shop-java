-- ========================================
-- 线上生鲜商场 - 数据库初始化（精简版，2026-07-11）
-- 9 张表 + 演示数据（账号/分类/商品/SKU/订单/评价）
-- 数据库：freshfood_shop
-- 演示账号统一密码 123456；图片用 picsum 占位
-- 2026-07-13 重构：合并 user/merchant 为单 user 表 + role 字段；商家扩展信息拆到 merchant_profile
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
DROP TABLE IF EXISTS `merchant_profile`;
DROP TABLE IF EXISTS `user`;

-- ============================================================
-- 1. 账号表（1 张 user + 1 张 merchant_profile）
-- ============================================================

CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称（买家用）',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `phone`       VARCHAR(255) DEFAULT NULL COMMENT '手机号（AES-256-CBC 加密，Base64）',
    `email`       VARCHAR(255) DEFAULT NULL COMMENT '邮箱（AES-256-CBC 加密，Base64）',
    `role`        TINYINT      NOT NULL DEFAULT 2 COMMENT '1 商家 / 2 买家',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表（含买家与商家，role 区分）';

CREATE TABLE `merchant_profile` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '关联 user.id（role=1 商家）',
    `shop_name`     VARCHAR(100) NOT NULL COMMENT '店铺名',
    `contact_name`  VARCHAR(255) DEFAULT NULL COMMENT '联系人（AES-256-CBC 加密）',
    `contact_phone` VARCHAR(255) DEFAULT NULL COMMENT '联系电话（AES-256-CBC 加密）',
    `logo`          VARCHAR(255) DEFAULT NULL,
    `audit_status`  TINYINT      NOT NULL DEFAULT 1 COMMENT '0 待审核 / 1 通过 / 2 拒绝（演示数据默认通过）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家扩展信息（1:1 关联 user）';

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
-- 5. 演示数据
-- ============================================================
-- 图片统一使用 picsum 占位（https://picsum.photos/seed/<seed>/<w>/<h>），按 seed 保证稳定
-- 密码统一 123456（BCrypt 哈希）
-- ============================================================

-- 5.1 账号（user + merchant_profile）
-- 密码明文：123456 → BCrypt 哈希（$2a$10$GnyEXCur52Saeco0/cTzYuU0P7/pnX1OiheCbjh44.ZrdS7duRWZW）
-- phone/email/contact_name/contact_phone 是 AES-256-CBC 加密后的 Base64，明文分别见注释
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`) VALUES
-- zhangsan（买家）：phone=13800138000, email=zhangsan@example.com
(1, 'zhangsan', '$2a$10$GnyEXCur52Saeco0/cTzYuU0P7/pnX1OiheCbjh44.ZrdS7duRWZW', '张三', 'https://picsum.photos/seed/user-zhangsan/200/200', 'l9v71rpUXqdwEtDZloA2PQNh6m1MXED4WR5weGAWwtE=', '2r05bst+KIkesh/fHijmoRERimPhEioLzkCpyXdjSzu9cDG1k2QuPKVMrPQjikaH', 2, 1),
-- lisi（买家）：phone=13900139000, email=lisi@example.com
(2, 'lisi',     '$2a$10$GnyEXCur52Saeco0/cTzYuU0P7/pnX1OiheCbjh44.ZrdS7duRWZW', '李四', 'https://picsum.photos/seed/user-lisi/200/200',     'hHgy3czCYun67beyMYQQxc2ffuY+D3k2HK5ctY1Eu+Y=', 'q1RXvvsatye0u72a0AJD8XP785ZH+o8KB5KXvsQiy+/lRTzbLBJuAPCfx/A8oY9N', 2, 1),
-- m01（商家，user.role=1）：phone=13700137000, email=m01@example.com
(3, 'm01',      '$2a$10$GnyEXCur52Saeco0/cTzYuU0P7/pnX1OiheCbjh44.ZrdS7duRWZW', '老板张', 'https://picsum.photos/seed/merchant-m01/200/200', 'i01Mk0Dj0cZUNDUpzOMPBjMN3UCF1cPpOyc1mp3DllY=', '2URKhAVeDZa5rZinU/EdbZ7Ig7tvJSei5ILd7dQ+pK0=', 1, 1),
-- m02（商家，user.role=1）：phone=13600136000, email=m02@example.com
(4, 'm02',      '$2a$10$GnyEXCur52Saeco0/cTzYuU0P7/pnX1OiheCbjh44.ZrdS7duRWZW', '老板李', 'https://picsum.photos/seed/merchant-m02/200/200', 'XupI6Whv3lV/RFQn5R/9JgmXHYFOAwYojz8hFE+G3qY=', 'tk3wnjPVHVeHeBodSAIcSF/ILtksy/vRcNMwdNzTFiE=', 1, 1);

-- merchant_profile：contact_name/contact_phone AES 加密；logo 图片 URL 明文
INSERT INTO `merchant_profile` (`user_id`, `shop_name`, `contact_name`, `contact_phone`, `logo`, `audit_status`) VALUES
-- m01：contact_name=Boss Zhang, contact_phone=13800001000
(3, '鲜果园旗舰店',   'MUTq0zb2BkZsa3RkLKpAssC67rymccpciv18V2mfnl0=', 'vzZfdgkJdeiEl2kYWSb9A6EhJ31M3pEvDh8TmG9bmcw=', 'https://picsum.photos/seed/shop-m01/300/300', 1),
-- m02：contact_name=Boss Li, contact_phone=13800002000
(4, '海鲜之家直营店', 'EA0YHa1GGJG45n2UYwObsfM67hHB0WFQcVaRGv7PML4=', '1NpQN2fs/wU6AZtehNNEnIMh3neb1jpBfxN4ycm+mIQ=', 'https://picsum.photos/seed/shop-m02/300/300', 1);

-- 5.2 分类
INSERT INTO `category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`) VALUES
(1, 0, '蔬菜水果',  'https://picsum.photos/seed/cat-fruit/120/120', 1, 1),
(2, 0, '肉禽蛋类',  'https://picsum.photos/seed/cat-meat/120/120',  2, 1),
(3, 0, '海鲜水产',  'https://picsum.photos/seed/cat-sea/120/120',   3, 1),
(4, 0, '粮油调味',  'https://picsum.photos/seed/cat-oil/120/120',   4, 1),
(5, 0, '乳品烘焙',  'https://picsum.photos/seed/cat-dairy/120/120', 5, 1),
(11, 1, '新鲜水果',  'https://picsum.photos/seed/cat-fruit-sub/120/120', 1, 1),
(12, 1, '叶菜类',    'https://picsum.photos/seed/cat-leafy/120/120',    2, 1),
(21, 2, '猪肉',      'https://picsum.photos/seed/cat-pork/120/120',     1, 1),
(22, 2, '鸡蛋',      'https://picsum.photos/seed/cat-egg/120/120',      2, 1),
(31, 3, '海鲜',      'https://picsum.photos/seed/cat-fish/120/120',     1, 1);

-- 5.3 商品 SPU（关联 user.id=3 m01）
INSERT INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `main_image`, `description`, `origin`, `after_sales_tags`, `audit_status`, `status`, `sales`, `rating`) VALUES
(1, 3, 11, '智利车厘子 J 级特大果',     'https://picsum.photos/seed/cherry/600/600',     '智利进口，空运直达，果径 28-30mm，甜蜜爆汁',  '智利',  '坏果包赔,次日达,顺丰冷链', 1, 1, 312, 4.80),
(2, 3, 11, '海南金煌芒 单果 400g+',     'https://picsum.photos/seed/mango/600/600',      '树上熟采摘，香甜无丝，皮薄核小',              '海南',  '坏果包赔,次日达',          1, 1, 245, 4.70),
(3, 3, 12, '云南小青菜 露天现摘',       'https://picsum.photos/seed/veg/600/600',        '当日采摘，叶片嫩绿，清炒最佳',                '云南',  '次日达,新鲜直送',          1, 1, 89,  4.60),
(4, 3, 11, '泰国山竹 5A 大果',         'https://picsum.photos/seed/mangosteen/600/600', '5A 级别大果，洁白果肉，酸甜可口',             '泰国',  '坏果包赔,顺丰冷链',        1, 1, 156, 4.85),
(5, 3, 11, '新西兰猕猴桃 6 粒装',       'https://picsum.photos/seed/kiwi/600/600',       '佳沛金果，6 粒装，单果约 130g',               '新西兰','坏果包赔,次日达',          1, 1, 423, 4.90);

-- 商品 SPU（关联 user.id=4 m02）
INSERT INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `main_image`, `description`, `origin`, `after_sales_tags`, `audit_status`, `status`, `sales`, `rating`) VALUES
(6, 4, 22, '农家散养土鸡蛋 30 枚',       'https://picsum.photos/seed/egg/600/600',        '林下散养，虫草喂养，蛋黄色泽金黄',            '湖北',  '破损包赔,次日达',          1, 1, 567, 4.95),
(7, 4, 31, '冰鲜大虾 单冻 500g',        'https://picsum.photos/seed/shrimp/600/600',     '北海大虾，-196℃ 液氮急冻，Q 弹鲜甜',          '北海',  '死蟹包赔,顺丰冷链,次日达',1, 1, 198, 4.75),
(8, 4, 21, '黑猪五花肉 冷冻切块 500g',  'https://picsum.photos/seed/pork/600/600',       '散养黑猪，肥瘦相间，适合红烧',               '黑龙江','冷链直送,次日达',          1, 1, 134, 4.65);

-- 5.4 SKU
INSERT INTO `sku` (`id`, `product_id`, `spec`, `price`, `stock`, `sales`, `image`) VALUES
(1001, 1, '1 斤装 (500g)',  49.90,  300, 100, 'https://picsum.photos/seed/cherry-500/600/600'),
(1002, 1, '2 斤装 (1kg)',   89.90,  200, 80,  'https://picsum.photos/seed/cherry-1kg/600/600'),
(1003, 1, '礼盒 4 斤装',    198.00, 100, 50,  'https://picsum.photos/seed/cherry-gift/600/600'),
(1011, 2, '2 粒装 (800g)',  29.90,  400, 150, 'https://picsum.photos/seed/mango-2/600/600'),
(1012, 2, '4 粒装 (1.6kg)', 55.90,  300, 95,  'https://picsum.photos/seed/mango-4/600/600'),
(1021, 3, '1 份 (300g)',    9.90,   500, 89,  'https://picsum.photos/seed/veg-300/600/600'),
(1022, 3, '2 份 (600g)',    16.90,  400, 50,  'https://picsum.photos/seed/veg-600/600/600'),
(1031, 4, '1 斤装 (500g)',  59.90,  250, 60,  'https://picsum.photos/seed/mangosteen-500/600/600'),
(1032, 4, '2 斤装 (1kg)',   109.90, 150, 40,  'https://picsum.photos/seed/mangosteen-1kg/600/600'),
(1041, 5, '6 粒装',         39.90,  600, 200, 'https://picsum.photos/seed/kiwi-6/600/600'),
(1042, 5, '12 粒装',        69.90,  400, 150, 'https://picsum.photos/seed/kiwi-12/600/600'),
(1051, 6, '30 枚装',        29.90,  800, 300, 'https://picsum.photos/seed/egg-30/600/600'),
(1052, 6, '60 枚装',        55.90,  500, 180, 'https://picsum.photos/seed/egg-60/600/600'),
(1061, 7, '500g 装',        79.90,  300, 120, 'https://picsum.photos/seed/shrimp-500/600/600'),
(1062, 7, '1kg 装',         149.90, 200, 70,  'https://picsum.photos/seed/shrimp-1kg/600/600'),
(1071, 8, '500g 装',        39.90,  400, 100, 'https://picsum.photos/seed/pork-500/600/600'),
(1072, 8, '1kg 装',         75.90,  250, 50,  'https://picsum.photos/seed/pork-1kg/600/600');

-- 5.5 两条待发货订单（用于演示商家端发货操作）
-- receiver_name / receiver_phone 已 AES-256-CBC 加密（明文见注释）
INSERT INTO `orders` (`id`, `order_no`, `user_id`, `merchant_id`, `total_amount`, `shipping_fee`, `discount_amount`, `payable_amount`, `receiver_name`, `receiver_phone`, `receiver_address`, `remark`, `status`, `expire_time`, `pay_time`, `pay_method`) VALUES
-- 订单 1（zhangsan → m01）：receiver_name=Zhang San, receiver_phone=13800138000
(1, '202607130001', 1, 3, 119.80, 0.00, 0.00, 119.80, 'PytxV0uUUCdvf2iyFUV4Z5ScdAf9HCL3MBvtxrSUrJk=', 'f80N0rKv+07z7ikeqTfsn55dobE16lQ4FezPMIQy03A=', '北京市朝阳区某街道 1 号', '请尽快发货', 2, NULL, '2026-07-13 12:31:00', 'MOCK'),
-- 订单 2（lisi → m02）：receiver_name=Li Si, receiver_phone=13900139000
(2, '202607130002', 2, 4, 109.80, 0.00, 0.00, 109.80, 't4W+3+nsvzSh7SxXw8bLh7hVC3WviM5BMFMQ3EBPI9c=', 'HLQYqdJ8K8m/M7jVqI8PPqKbrHiZuEsDpVKX51r7APM=', '上海市浦东新区某路 88 号', NULL, 2, NULL, '2026-07-13 13:00:00', 'MOCK');

INSERT INTO `order_item` (`id`, `order_id`, `sku_id`, `product_id`, `product_name_snapshot`, `spec_snapshot`, `price_snapshot`, `quantity`) VALUES
(9001, 1, 1002, 1, '智利车厘子 J 级特大果', '2 斤装 (1kg)', 89.90, 1),
(9002, 1, 1011, 2, '海南金煌芒 单果 400g+', '2 粒装 (800g)', 29.90, 1),
(9003, 2, 1051, 6, '农家散养土鸡蛋 30 枚',   '30 枚装', 29.90, 1),
(9004, 2, 1061, 7, '冰鲜大虾 单冻 500g',    '500g 装', 79.90, 1);

-- 5.6 一些评价（演示用）
INSERT INTO `review` (`id`, `order_id`, `order_item_id`, `user_id`, `product_id`, `sku_id`, `merchant_id`, `rating`, `taste_rating`, `freshness_rating`, `logistics_rating`, `content`, `images`, `is_append`, `status`) VALUES
(1, 1, 9001, 1, 1, 1002, 3, 5, 5, 5, 5, '车厘子超大颗超甜，包装也很扎实', 'https://picsum.photos/seed/review-1-1/200/200,https://picsum.photos/seed/review-1-2/200/200', 0, 1),
(2, 1, 9002, 1, 2, 1011, 3, 5, 5, 4, 5, '芒果很新鲜，没有丝', 'https://picsum.photos/seed/review-2-1/200/200', 0, 1),
(3, 2, 9003, 2, 6, 1051, 4, 5, 5, 5, 4, '蛋黄色泽金黄，比超市的新鲜很多', NULL, 0, 1),
(4, 2, 9004, 2, 7, 1061, 4, 4, 4, 5, 4, '虾很新鲜，肉质 Q 弹', NULL, 0, 1);

-- ============================================================
-- 6. 建表 + 灌数据完成
-- ============================================================
SELECT 'Schema initialized + demo data inserted. 9 tables, 4 users, 2 merchants, 8 products, 17 SKUs, 2 orders, 4 reviews.' AS status;