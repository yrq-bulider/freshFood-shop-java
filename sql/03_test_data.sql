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
