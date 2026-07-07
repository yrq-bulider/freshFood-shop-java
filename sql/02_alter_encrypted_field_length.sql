-- ============================================================
-- 迁移：扩大被加密字段的长度
-- 背景：phone / email / contactName / contactPhone / receiverName
--       字段用 EncryptedStringTypeHandler 加密后变成 Base64(IV + 密文)，
--       11 位手机号 ≈ 44 字符，汉字姓名更长，原 VARCHAR(20/50/100) 装不下，
--       一旦前端传入这些字段就会报 Data truncation。
-- 解决：把"加密字段"统一扩到 VARCHAR(255) 兜底容量。
-- 日期：2026-07-07
-- ============================================================

-- 用户表
ALTER TABLE `user`
    MODIFY COLUMN `phone` VARCHAR(255) DEFAULT NULL COMMENT '手机号（AES-256-CBC 加密，Base64）',
    MODIFY COLUMN `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱（AES-256-CBC 加密，Base64）';

-- 商家表
ALTER TABLE `merchant`
    MODIFY COLUMN `contact_name`  VARCHAR(255) DEFAULT NULL COMMENT '联系人（AES-256-CBC 加密）',
    MODIFY COLUMN `contact_phone` VARCHAR(255) DEFAULT NULL COMMENT '联系电话（AES-256-CBC 加密）';

-- 收货地址
ALTER TABLE `address`
    MODIFY COLUMN `receiver_name` VARCHAR(255) NOT NULL COMMENT '收件人（AES-256-CBC 加密）',
    MODIFY COLUMN `phone`         VARCHAR(255) NOT NULL COMMENT '手机号（AES-256-CBC 加密）';
