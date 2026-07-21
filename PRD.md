# RentFlow 数码潮玩商城产品说明

## 1. 产品定位

RentFlow 是面向个人用户的数码商品商城，覆盖商品浏览、SKU 选购、库存锁定、待支付订单、模拟支付、发货、确认收货和已购评价。

GearMate 是独立运行的选品与购买助手。它负责理解用户的品类、用途、品牌和预算需求，通过 RentFlow API 查询真实商品、SKU 售价、可售库存和商城订单，不直接支付或绕过商城业务规则。

## 2. 核心流程

```text
商品搜索
  -> 查看商品与 SKU
  -> 选择规格和数量
  -> 填写收货地址
  -> 创建待支付订单并锁定 SKU 库存
  -> 支付成功后扣减库存
  -> 管理员发货
  -> 用户确认收货
  -> 用户发表已购评价
```

待支付订单可以由用户取消，也可以在支付窗口到期后自动关闭。取消或关闭必须释放已锁定库存。

## 3. 业务范围

### 3.1 RentFlow Java 服务

- 用户登录、JWT 鉴权和角色校验
- 商品分类、商品、用途标签和结构化搜索
- SKU 售价、规格、现货数量和启停状态
- 商城结算、订单状态流转和订单查询
- 收货地址、模拟支付和物流信息
- 已购商品评价
- 幂等控制、审计日志、Outbox 事件和通知
- 待支付订单超时关闭与库存释放

### 3.2 RentFlow Web

- 登录
- 商品列表、分类筛选和商品详情
- SKU、价格与库存展示
- 结算、订单列表和订单详情
- 支付、取消、确认收货和评价
- GearMate 对话与商品卡片快捷购买
- 商城通知

### 3.3 GearMate Agent

- 商品搜索与商品详情
- SKU 售价和库存查询
- 购买准备与规格选择
- 商城订单列表和订单详情
- 多轮对话、短期记忆和用户偏好记忆
- 确定性动作执行、事实校验和 SSE 事件

## 4. 核心数据模型

### 商品目录

- `categories`：商品分类
- `products`：商品 SPU、品牌、型号、描述和用途角色
- `catalog_use_cases`：动态用途分类
- `catalog_use_case_aliases`：用途别名
- `product_use_cases`：商品与用途的加权关联

### SKU 与库存

- `product_skus`：SKU、规格、售价、现货数量、锁定数量和版本号
- `stock_movements`：锁定、销售和释放库存的流水

可售数量计算为：

```text
available_quantity = on_hand_quantity - reserved_quantity
```

### 商城订单

- `commerce_orders`：订单主表
- `commerce_order_items`：商品与价格快照
- `order_address_snapshots`：收货地址快照
- `payments`：支付成功记录
- `shipments`：物流记录
- `commerce_order_history`：状态历史
- `store_idempotency`：商城写操作幂等结果

### 评价与通知

- `commerce_product_reviews`：已购商品评价
- `user_notifications`：商城订单通知
- `outbox_events`：待发布领域事件
- `consumer_inbox`：消费者去重记录

## 5. 订单状态

```text
PENDING_PAYMENT -> PAID -> SHIPPED -> RECEIVED
PENDING_PAYMENT -> CANCELLED
PENDING_PAYMENT -> CLOSED
```

- `PENDING_PAYMENT`：订单已创建，库存已锁定
- `PAID`：支付成功，锁定库存转为实际销售
- `SHIPPED`：管理员已填写物流并发货
- `RECEIVED`：用户已确认收货，可以评价
- `CANCELLED`：用户取消，库存已释放
- `CLOSED`：支付超时，库存已释放

## 6. 一致性与并发

1. 创建订单时按 SKU ID 排序并锁定 SKU 行，避免多 SKU 下单产生不稳定锁顺序。
2. 锁定后重新校验 `available_quantity`，库存不足时整个事务回滚。
3. 创建订单、订单项、地址快照、库存锁定、库存流水、状态历史、Outbox 和审计记录在同一数据库事务内完成。
4. 支付、取消和超时关闭必须锁定订单及对应 SKU，并通过条件更新保证库存只处理一次。
5. 写接口使用 `Idempotency-Key`；同一用户、接口和幂等键只能对应同一请求摘要。
6. Outbox 发布采用至少一次语义，消费者通过 `consumer_inbox` 去重。

## 7. 接口边界

公开目录接口：

- `GET /api/v1/categories`
- `GET /api/v1/catalog/use-cases`
- `GET /api/v1/products`
- `GET /api/v1/products/{productId}`
- `GET /api/v1/store/products/{productId}/skus`
- `GET /api/v1/store/skus/{skuId}`
- `GET /api/v1/store/products/{productId}/reviews`

认证商城接口：

- `POST /api/v1/store/orders/checkout`
- `GET /api/v1/store/orders`
- `GET /api/v1/store/orders/{orderId}`
- `POST /api/v1/store/orders/{orderId}/pay`
- `POST /api/v1/store/orders/{orderId}/cancel`
- `POST /api/v1/store/orders/{orderId}/receive`
- `POST /api/v1/store/products/{productId}/reviews`
- `POST /api/v1/store/admin/orders/{orderId}/ship`

## 8. 非目标

- 真实支付网关、退款和支付回调
- 优惠券、积分和会员体系
- 多仓库调拨
- 复杂物流轨迹
- 退换货和售后工单
- Agent 自动支付或自动提交订单

## 9. 技术约束

### RentFlow Server

- Java 21
- Spring Boot 3.5
- Spring Security
- MyBatis
- Flyway
- MySQL 8.4
- RabbitMQ 与事务 Outbox
- JUnit 5

### RentFlow Web

- Vue 3.5
- TypeScript strict
- Vite
- Pinia
- Element Plus
- Axios 与 Fetch SSE

### GearMate

- Python 3.12
- FastAPI
- LangGraph
- Pydantic
- SQLAlchemy、Alembic 和 PostgreSQL/pgvector

## 10. 验收要求

- 商品列表和详情展示售价、规格、库存和商品说明。
- SKU 售价和库存来自 RentFlow 当前数据。
- 两个并发订单争抢最后库存时，最多一个成功。
- 重复提交相同幂等请求不创建重复订单或重复扣减库存。
- 取消和支付超时均释放锁定库存。
- 支付成功只扣减一次现货和锁定数量。
- 用户只能查看和操作自己的订单。
- 只有管理员可以发货。
- 用户只能评价已确认收货且尚未评价的订单商品。
- GearMate 的商品、库存和订单回答以 RentFlow 工具结果为准。
