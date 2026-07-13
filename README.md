# 线上生鲜商场购物平台 - 后端

> 课设作业 · Spring Boot 多模块后端 · 用户端 / 商家端（单账号表 + role 字段区分）

## 项目简介

一个仿"每日优鲜 / 盒马"模式的生鲜电商平台后端，覆盖用户下单、商家运营主链路，集成 Sa-Token 鉴权（单 StpLogic + role 注解）、MyBatis-Plus 持久化、统一响应体、Bean Validation、springdoc-openapi API 文档等主流技术。

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.5.16 |
| 持久层 | MyBatis-Plus | 3.5.9 |
| 鉴权 | Sa-Token（单 StpLogic + @SaCheckRole） | 1.37.0 |
| 数据库 | MySQL | 5.7+ / 8.x |
| 连接池 | Druid | 1.2.23 |
| 工具库 | Hutool | 5.8.27 |
| API 文档 | springdoc-openapi (swagger-ui) | 2.8.15 |
| 单元测试 | JUnit 5 + Mockito | — |
| 构建 | Maven | 3.8+ |

## 模块结构

```
freshfood-shop/                        # 父 POM（聚合）
├── freshfood-common/                  # 公共：R/ErrorCode/异常/常量
├── freshfood-framework/               # 配置：MyBatis-Plus / Web / Sa-Token / 拦截器
├── freshfood-model/                   # 实体（DO）：user / merchant_profile / product / order ...
├── freshfood-user/                    # 用户端业务（含统一注册/登录/登出）：16 接口
├── freshfood-merchant/                # 商家端业务（订单/发货，受 @SaCheckRole 保护）：3 接口
└── freshfood-app/                     # Spring Boot 启动器（聚合两端 + 统一登录）
```

## 两端功能概览（精简版）

### 👤 用户端（`/api/v1/*`）
- 账号：注册（统一入口，role=2 买家） / 登录 / 登出
- 首页：分类树
- 商品：详情（含评价列表）
- 搜索：商品搜索
- 购物车：增删改
- 订单：下单 / 支付 / 确认收货 / 列表 / 详情
- 评价：发表评价

### 🏪 商家端（`/api/v1/merchant/*`）
- 账号：通过统一注册入口 `role=1` 创建，再走 `/api/v1/auth/login` 登录
- 订单：列表 / 详情 / 发货（受 `@SaCheckRole("MERCHANT")` 拦截）

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

# 2. 退出 mysql 后执行 SQL 脚本（精简版，仅 1 份建表脚本）
mysql -uroot -p freshfood_shop < sql/01_init.sql
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

- **API 文档（springdoc swagger-ui）**：http://localhost:8080/swagger-ui.html
- **健康检查**：http://localhost:8080/

### 5. 演示账号

精简版脚本不灌演示数据。通过 Swagger UI 或 curl 调用 `/api/v1/auth/register` 自助创建（密码统一 123456）：

| 端 | 账号 | 密码 |
|---|---|---|
| 商家 | `m01` | `123456` |
| 用户 | `zhangsan` | `123456` |
| 用户 | `lisi` | `123456` |

```bash
# 示例：创建买家
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456","nickname":"张三","phone":"13800138000","role":2}'

# 示例：创建商家（role=1 + shopName 必填；merchant_profile 表会写一行）
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456","role":1,"shopName":"鲜果园旗舰店","contactName":"老板张","contactPhone":"13900139000","logo":"https://..."}'
```

## API 文档

启动后访问 http://localhost:8080/swagger-ui.html 即可看到完整 API 文档，支持：

- 两端接口分组（用户 / 商家）+ 统一登录
- 在线调试（Try it out）
- 鉴权（Sa-Token）登录后自动带 token
- 请求/响应示例

> 离线文档：[`docs/API.md`](docs/API.md)

## 数据模型（核心表，9 张）

| 表 | 说明 |
|---|---|
| `user` | 账号表（含 BCrypt 密码、状态、`role` 字段 1=商家/2=买家） |
| `merchant_profile` | 商家扩展信息（1:1 关联 `user.id`，存店铺名/联系人/logo/审核状态） |
| `category` | 商品分类（树形，parent_id 自引用） |
| `product` | 商品（关联 user 表 role=1 的商家 + category） |
| `sku` | 商品 SKU（规格 + 库存 + 价格） |
| `cart` | 购物车 |
| `orders` / `order_item` | 订单主表 + 明细（订单表含收货人姓名/电话/地址快照） |
| `review` | 商品评价 |

> 详细字段见 `sql/01_init.sql`

## 关键设计决策

| 主题 | 决策 | 原因 |
|---|---|---|
| 多端架构 | 6 个 Maven 模块，业务模块互不依赖 | 单部署 + 多模块解耦；管理端砍掉，聚焦主链路 |
| 账号模型 | 单 `user` 表 + `role` 字段（1=商家 / 2=买家）；商家扩展拆 `merchant_profile` | 一张表统一账号，前端调试简单；商家字段不污染买家 |
| 鉴权 | Sa-Token 单 StpLogic + `@SaCheckRole("MERCHANT")` 拦截商家端接口 | 共用 token 空间；注解驱动，简单直观 |
| 密码 | BCrypt (sa-token `cn.dev33.satoken.secure.BCrypt`) | 行业标准 + 自带依赖 |
| 统一响应 | `R<T>` 包装类 + `PageR<T>` 分页 | 前端约定统一 code=0 表示成功 |
| 错误码 | 6 位分段（1xxx 通用 / 2xxx 用户 / 3xxx 商品 / 4xxx 订单 / 5xxx 支付 / 7xxx 商家） | 一眼定位问题域 |
| 逻辑删除 | MyBatis-Plus `@TableLogic` 字段 `deleted` | 软删 + 可恢复 |
| ID 策略 | 自增主键 (`@TableId(type = IdType.AUTO)`) | 简单可靠，够用 |
| 时间字段 | 实体 `BaseDO` 自动填充 `createTime` / `updateTime` | 避免手写 |

## 端对端主链路示例

**用户下单 → 商家发货 → 用户确认收货 → 评价**：

```bash
# 1. 用户登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}' | jq -r .data.token)

# 2. 加入购物车 + 提交订单 + 支付 + 确认收货 + 评价
# 3. 商家登录后发货
M_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"m01","password":"123456"}' | jq -r .data.token)
```

> 完整 curl 示例见 [`docs/API.md` § 六](docs/API.md)

## 项目结构

```
docs/                                  # 设计文档 + 实施计划 + API 文档
├── API.md                             # 接口文档（精简版）
└── superpowers/
    ├── specs/                         # 4 份设计 spec
    └── plans/                         # 4 份实施计划
sql/
└── 01_init.sql                        # 单一建表脚本（9 张表）
docker-compose.yml                     # MySQL + adminer 一键起
```

## 精简后定位

- 管理端砍掉（35 个端点）
- 砍掉消息/退款/追评/再购买/地址簿/密码修改/个人资料/商家商品管理/店铺资料维护
- 首页轮播/推荐/热搜词、商品评价单独接口 砍掉，前端硬编码 banner 图与热搜词
- 端点 90 → 19；表 15 → 9；模块 7 → 6
- 2026-07-13 重构：合并 user/merchant 单表 + role 字段；端点 19 → 19（去 3 加 0，但商家独立 auth 端点全部并入统一注册）
- 详细精简方案：[`docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md`](docs/superpowers/specs/2026-07-11-freshfood-simplify-design.md)

## 后续可扩展方向（参考）

- 退款/售后
- 优惠券/活动
- 物流轨迹模拟
- WebSocket / SSE 推送
- Redis 缓存商品详情 / 分类树
- Elasticsearch 商品全文搜索
- 集成测试 + 端到端测试

## 课设作者

- 项目类型：本科毕业设计 / 课程设计
- 范围：仅后端，前端另行
- 完成度：买家 14 + 商家 3 + 统一登录 1 + 统一注册/登出 2 = 共 19 个 REST 端点（单 `user` 表 + role 字段）
