# 线上生鲜商场购物平台 - 后端

> 课设作业 · Spring Boot 多模块后端 · 用户端 / 商家端 / 管理端三端分离架构

## 项目简介

一个仿"每日优鲜 / 盒马"模式的生鲜电商平台后端，覆盖用户下单、商家运营、平台审核三大主链路，集成 Sa-Token 多端鉴权、MyBatis-Plus 持久化、统一响应体、Bean Validation、Knife4j API 文档等主流技术。

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.5.16 |
| 持久层 | MyBatis-Plus | 3.5.9 |
| 鉴权 | Sa-Token (JWT + 多 StpLogic) | 1.37.0 |
| 数据库 | MySQL | 5.7+ / 8.x |
| 连接池 | Druid | 1.2.23 |
| 工具库 | Hutool | 5.8.27 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 单元测试 | JUnit 5 + Mockito | — |
| 构建 | Maven | 3.8+ |

## 模块结构

```
freshfood-shop/                        # 父 POM（聚合）
├── freshfood-common/                  # 公共：R/ErrorCode/异常/常量
├── freshfood-framework/               # 配置：MyBatis-Plus / Web / Sa-Token / 拦截器
├── freshfood-model/                   # 实体（DO）：user / merchant / product / order ...
├── freshfood-user/                    # 用户端业务：9 模块 / 40 接口
├── freshfood-merchant/                # 商家端业务：4 模块 / 15 接口
├── freshfood-admin/                   # 管理端业务：4 模块 / 28 接口 + 自身账号 7 接口
└── freshfood-app/                     # Spring Boot 启动器（聚合三端）
```

## 三端功能概览

### 👤 用户端（`/api/v1/user/*`）
- 账号：注册/登录/登出
- 首页：轮播图/分类树/热门搜索词
- 商品：列表/详情/搜索/评价
- 购物车：增删改查 / 选中切换
- 订单：下单/支付/取消/收货/退款
- 地址：增删改查 / 设为默认
- 消息：系统通知列表

### 🏪 商家端（`/api/v1/merchant/*`）
- 账号：注册/登录/登出/资料维护
- 商品：发布/编辑/上下架/SKU 管理
- 订单：发货/确认/查询
- 审核：店铺资质待平台审核

### 🔧 管理端（`/api/v1/admin/*`）
- 账号：登录/登出 + 账号管理 7 端点
- 商家审核：待审/通过/拒绝
- 商品审核：待审/通过/拒绝
- 用户管理：分页/启停
- 运营内容：Banner / 搜索热词 / 分类树
- 自身账号：分页/详情/新建/启停/重置密码/删除

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 5.7+ / 8.x

### 2. 初始化数据库

```bash
mysql -uroot -p
# 1. 建库
CREATE DATABASE freshfood_shop DEFAULT CHARACTER SET utf8mb4;

# 2. 退出 mysql 后执行 SQL 脚本
mysql -uroot -p freshfood_shop < sql/01_init_schema.sql
mysql -uroot -p freshfood_shop < sql/02_business_tables.sql
mysql -uroot -p freshfood_shop < sql/03_test_data.sql
```

> 或者用 Docker 一键起 MySQL：`docker compose up -d`（参见仓库根 `docker-compose.yml`）
> - MySQL 端口：`3306`（root/root）
> - Adminer（Web 数据库管理）：http://localhost:8081（服务器填 `mysql`，用户名/密码 `root`/`root`）
> - 重置数据：`docker compose down -v && docker compose up -d`

### 3. 修改数据库连接

编辑 `freshfood-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/freshfood_shop?...
    username: root
    password: 你的密码
```

### 4. 启动项目

```bash
# 方式 1：命令行
mvn -pl freshfood-app -am spring-boot:run

# 方式 2：IDE
# IntelliJ 打开根 pom.xml，运行 FreshfoodShopApplication.main()
```

启动成功后访问：

- **API 文档（Knife4j）**：http://localhost:8080/doc.html
- **健康检查**：http://localhost:8080/

### 5. 默认演示账号

| 端 | 账号 | 密码 | 备注 |
|---|---|---|---|
| 管理员 | `admin` | `123456` | id=1，超级管理员，写操作受保护 |
| 商家 | `m01` | `123456` | 店铺「鲜果园旗舰店」，已通过审核 |
| 用户 | `zhangsan` | `123456` | 普通用户；另含 `lisi` 同密码 |

## API 文档

启动后访问 http://localhost:8080/doc.html 即可看到完整 API 文档，支持：

- 三端接口分组（用户 / 商家 / 管理）
- 在线调试（Try it out）
- 鉴权（Sa-Token）登录后自动带 token
- 请求/响应示例

## 数据模型（核心表）

| 表 | 说明 |
|---|---|
| `user` | 用户表（含 BCrypt 密码、状态、积分） |
| `merchant` | 商家表（含审核状态、联系人、地址） |
| `admin` | 管理员表 |
| `category` | 商品分类（树形，parent_id 自引用） |
| `product` | 商品（关联 merchant + category） |
| `sku` | 商品 SKU（规格 + 库存 + 价格） |
| `cart` | 购物车 |
| `orders` / `order_item` | 订单主表 + 明细 |
| `refund` | 退款申请 |
| `address` | 收货地址 |
| `banner` / `hot_word` | 运营内容 |
| `review` | 商品评价 |

> 详细字段见 `sql/01_init_schema.sql` + `sql/02_business_tables.sql`

## 关键设计决策

| 主题 | 决策 | 原因 |
|---|---|---|
| 多端架构 | 7 个 Maven 模块，业务模块互不依赖 | 单部署 + 多模块解耦；业务可独立测试 |
| 鉴权 | Sa-Token 多 StpLogic（`user`/`merchant`/`admin` 三套） | 端间 token 互不干扰，统一鉴权 API |
| 密码 | BCrypt (sa-token `cn.dev33.satoken.secure.BCrypt`) | 行业标准 + 自带依赖 |
| 统一响应 | `R<T>` 包装类 + `PageR<T>` 分页 | 前端约定统一 code=0 表示成功 |
| 错误码 | 6 位分段（1xxx 通用 / 2xxx 用户 / 3xxx 商品 / 4xxx 订单 / 5xxx 支付 / 6xxx 退款 / 7xxx 商家 / 9xxx 管理） | 一眼定位问题域 |
| 逻辑删除 | MyBatis-Plus `@TableLogic` 字段 `deleted` | 软删 + 可恢复 |
| ID 策略 | 自增主键 (`@TableId(type = IdType.AUTO)`) | 简单可靠，够用 |
| 时间字段 | 实体 `BaseDO` 自动填充 `createTime` / `updateTime` | 避免手写 |

## 端对端流程示例

**用户下单 → 商家发货 → 平台旁观（最简链路）：**

```bash
# 1. 用户登录拿 token
curl -X POST http://localhost:8080/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"u01","password":"123456"}'

# 2. 加入购物车 / 下单（具体略，见 knife4j 文档）

# 3. 商家登录后查看订单并发货
curl -X POST http://localhost:8080/api/v1/merchant/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456"}'

# 4. 管理员可看全平台数据
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

## 项目结构

```
docs/                          # 设计文档 + 实施计划
├── superpowers/
│   ├── specs/                 # 5 份设计 spec
│   └── plans/                 # 5 份实施计划
sql/                           # 数据库脚本
├── 01_init_schema.sql         # 主表结构
├── 02_business_tables.sql     # 业务表结构
└── 03_test_data.sql           # 演示数据
docker-compose.yml             # MySQL + adminer 一键起
```

## 后续可扩展方向（参考）

- 集成 Elasticsearch 做商品全文搜索
- Redis 缓存商品详情 / 分类树
- OSS 存储商品图 / 商家资质
- RabbitMQ 异步处理订单通知
- 退款/优惠券/促销活动（高级业务）
- 集成测试 + 端到端测试
- CI/CD 流水线 + Docker 镜像发布

## 课设作者

- 项目类型：本科毕业设计 / 课程设计
- 范围：仅后端，前端另行
- 完成度：用户端 40 + 商家端 15 + 管理端 35 = 共 90 个 REST 端点
