# RentFlow 数码设备租赁平台 V1.0 产品需求文档

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 产品名称 | RentFlow 数码设备租赁平台 |
| 智能助手名称 | GearMate |
| 文档版本 | V1.0 |
| 产品阶段 | 第一版 MVP |
| 文档状态 | 待开发 |
| 目标用户 | 有短期数码设备租赁需求的个人用户 |
| 核心目标 | 完成“查询商品 → 选择租期 → 获取报价 → 预占具体设备并冻结价格 → 创建订单 → 查询订单”的预订闭环 |

## 2. 产品背景

用户在短期拍摄、旅行、直播、会议或临时办公等场景下，需要租赁相机、镜头、无人机、电脑等数码设备。传统租赁系统如果只按商品数量维护库存，容易在租期重叠、预占过期和多人并发下发生超卖。

RentFlow 第一版以具体设备实例为库存分配单位，使用统一的半开时间区间规则处理租期冲突，并通过数据库事务和行锁保证最后一台设备不会被多个用户同时租出。

V1 定位为**租赁预订闭环**，终点是创建并查询 `CREATED` 订单，不包含支付、交付、实际租赁开始、归还或售后处理，因此不称为完整租赁履约闭环。

GearMate 是独立运行的智能租赁助手。第一版只提供商品推荐、真实库存查询和报价查询，不直接创建或修改订单。所有库存判断、价格计算和订单规则均由 RentFlow Java 服务执行。

## 3. 第一版产品目标

### 3.1 核心目标

1. 用户可以登录平台并保持认证状态。
2. 用户可以按关键词和租赁时间搜索商品。
3. 系统可以根据具体设备实例判断指定时间段是否可租。
4. 用户可以创建有明确有效期的库存预占。
5. 系统可以根据租期计算租金与押金，并生成不可变的价格快照。
6. 用户可以将有效预占转换为订单。
7. 用户可以查看自己的订单列表和订单详情。
8. GearMate 可以调用 RentFlow API，基于真实商品、库存和报价回答用户问题。
9. 系统可以正确处理最后一台设备的并发争抢、预占过期和重复下单。

### 3.2 成功标准

- 两个用户同时争抢同一租期的最后一台设备时，只允许一个用户预占成功。
- 已被有效预占或订单占用的设备不能分配给时间段冲突的请求。
- 预占到期后，设备可以再次被其他用户预占。
- 相同用户使用同一个幂等键重复提交订单时，只产生一张订单。
- GearMate 返回的库存和报价与租赁页面查询结果一致。
- Java 服务在 GearMate 不可用时仍能独立完成租赁主流程。

## 4. 第一版范围

### 4.1 包含范围

#### RentFlow Java 服务

- 用户登录与 JWT 鉴权
- 用户身份与基础角色
- 商品、型号和类目查询
- 设备实例管理与初始化数据
- 按租期查询商品可租状态
- 具体设备实例分配
- 库存预占与主动释放
- 预占自动过期
- 租金与押金报价
- 价格快照
- 幂等创建订单
- 用户订单列表与详情
- 订单状态历史
- 统一错误响应
- Correlation ID
- 关键写操作审计

#### GearMate Python 服务

- 独立 FastAPI 服务
- LangGraph 对话流程编排
- 可插拔模型适配层
- 商品搜索工具
- 真实库存查询工具
- 租赁报价工具
- 多轮会话
- 流式 SSE 输出
- 会话和图执行状态持久化
- 工具调用日志与错误处理

#### RentFlow Vue Web

- 登录页
- 商品列表与搜索
- 商品详情
- 租期选择
- 可租状态与报价展示
- 库存预占
- 预占倒计时
- 下单确认
- 订单列表
- 订单详情
- GearMate 对话页

### 4.2 不包含范围

第一版不实现以下功能：

- 注册、短信验证码、第三方登录和找回密码
- 在线支付、退款和支付回调
- 优惠券、会员等级、积分和营销活动
- 发货、配送、门店自提和签收
- 续租
- 提前归还与逾期处理
- 归还验收、损坏和缺件赔偿
- 维修、报废和完整设备生命周期后台
- Agent 创建订单或修改业务数据
- Agent 写操作确认卡片
- 说明书上传与 RAG
- PostgreSQL pgvector 检索
- RabbitMQ、RocketMQ、Kafka
- Transactional Outbox
- Redis 分布式缓存和限流
- MinIO 文件管理
- Elasticsearch
- 独立管理后台
- 行为分析、推荐算法和数据大屏

## 5. 用户角色

### 5.1 普通用户

第一版主要角色，可以：

- 登录系统
- 浏览和搜索商品
- 选择租期
- 查询库存和报价
- 预占设备
- 创建订单
- 查看自己的订单
- 使用 GearMate 获取选型、库存和报价建议

普通用户只能访问自己的预占、报价和订单，不能通过请求参数查询其他用户数据。

### 5.2 运营管理员

第一版不开发完整管理后台。管理员仅用于初始化测试数据和必要的内部接口权限，不作为主要验收流程。

## 6. 核心业务概念

### 6.1 商品与设备实例

- **商品 Product**：面向用户展示和租赁的商品，例如“Sony A7M4 相机机身”。
- **设备实例 Equipment Unit**：一台可独立出租的实体设备，例如序列号为 `RF-A7M4-0001` 的相机。
- 一个商品可以对应多台设备实例。
- 库存判断和分配必须以设备实例为单位，不能只维护商品的可用数量。

### 6.2 租赁时间段

所有租期统一使用半开区间：

```text
[start_at, end_at)
```

含义：

- `start_at` 包含在租期内。
- `end_at` 不包含在租期内。
- 一段租期在另一段租期结束时开始，不视为冲突。

冲突条件：

```text
existing.start_at < requested.end_at
AND existing.end_at > requested.start_at
```

示例：

| 已有租期 | 请求租期 | 是否冲突 |
|---|---|---|
| 7月1日 10:00–7月3日 10:00 | 7月3日 10:00–7月5日 10:00 | 否 |
| 7月1日 10:00–7月3日 10:00 | 7月2日 10:00–7月4日 10:00 | 是 |
| 7月1日 10:00–7月3日 10:00 | 7月1日 10:00–7月2日 10:00 | 是 |

第一版所有服务内部时间使用 UTC，租期规则如下：

- `start_at` 必须晚于数据库当前时间。
- V1 不额外规定最短提前预订分钟数，但预占到期时间不得晚于 `start_at`，且创建订单时租期必须仍未开始。
- 最短租期为 1 小时；最小计费单位仍为 1 天。
- 最长租期为 30 天。
- 最远允许提前 90 天预订。
- 前端时间选择粒度为 30 分钟。
- API 时间统一使用 ISO 8601 带时区格式。
- 服务端转换为 UTC 后存储和比较。
- 用户资料或会话记录用户的 IANA 时区，例如 `Asia/Shanghai`。
- 前端按用户 IANA 时区展示时间，不能将浏览器本地字符串直接作为业务时间提交。

所有边界判断使用数据库当前时间。MySQL、Java 和 PostgreSQL 会话时区统一配置为 UTC。

### 6.3 预占

预占用于在用户确认订单前暂时锁定一台具体设备，并冻结创建预占时的报价快照。

- 默认有效期：15 分钟。
- 实际到期时间统一计算为：

```sql
reservation.expires_at = LEAST(
    CURRENT_TIMESTAMP(6) + INTERVAL :reservation_ttl_seconds SECOND,
    start_at
)
```

- 创建预占和创建订单时都必须满足 `start_at > CURRENT_TIMESTAMP(6)`。预占不能跨过租赁开始时间，租期已经开始时不得创建订单。
- 有效期和单用户有效预占上限必须通过配置传入；V1 默认每个用户最多同时拥有 3 个有效预占。
- 权威判定为：

```text
有效预占 = status = ACTIVE
          AND expires_at > CURRENT_TIMESTAMP(6)
```

- 库存查询、创建预占、释放预占和创建订单均必须在 SQL 中使用 MySQL `CURRENT_TIMESTAMP(6)` 判断有效性。
- MySQL 业务时间字段统一使用 `DATETIME(6)`，默认值和 SQL 比较统一使用 `CURRENT_TIMESTAMP(6)`；避免存储精度和边界比较精度不一致。
- 清理任务只负责将逻辑上已过期的记录更新为 `EXPIRED`。即使清理任务尚未执行，过期预占也不能阻塞库存或创建订单。
- 预占查询 API 必须返回计算后的 `effectiveStatus`。当持久化 `status = ACTIVE` 且 `expires_at <= CURRENT_TIMESTAMP(6)` 时，`effectiveStatus = EXPIRED`；业务调用方不得将其表现为有效 `ACTIVE`。
- `status` 是持久化状态，`effectiveStatus` 是业务有效状态；预占查询、列表和写接口响应均按 OpenAPI 明确定义两者语义。
- 清理任务最终将持久化状态收敛为 `EXPIRED`，但不参与正确性判断。
- 前端倒计时只用于展示，不能作为预占有效性的事实依据。
- 预占成功后返回具体预占记录、到期时间、`source_quote_id` 和冻结价格快照。
- 预占到期、主动释放或转为订单后，不再占用库存。
- 创建预占必须携带 `Idempotency-Key`。相同用户、相同接口、相同幂等键和相同请求返回原预占；同一键对应不同请求返回 `IDEMPOTENCY_CONFLICT`。
- 单用户有效预占数量限制必须在事务中使用用户级锁定记录或等效数据库串行化方案实现，禁止“先查数量再无锁插入”。

### 6.4 报价与价格快照

报价由 Java 服务统一计算。创建报价不会锁定库存。

报价至少包含：

- `quote_id`
- `user_id`
- `product_id`
- `start_at`、`end_at`
- `billing_days`
- `pricing_version`
- `pricing_rule`
- `price_snapshot`
- `expires_at`

V1 计费规则：

1. 币种统一为 `CNY`。
2. 最小计费单位为 1 天。
3. `计费天数 = ceil((end_at - start_at) / 24 小时)`，最少为 1 天。
4. `rental_amount = 商品日租金 × 计费天数`。
5. `deposit_amount = 商品配置的固定押金`，不随租期变化。
6. `total_amount = rental_amount + deposit_amount`。
7. 金额统一保留 2 位小数，使用 `HALF_UP` 舍入。
8. Java 服务是金额计算的唯一来源；Python 和前端不得计算最终金额。

报价、预占和订单绑定规则：

1. 创建预占必须提交 `quote_id` 和 `Idempotency-Key`。
2. 一个 `quote_id` 最多成功创建一个预占。创建预占时必须锁定报价记录。
3. 创建预占时，在同一事务内校验报价属于当前用户且未过期，并校验商品和租期与报价完全一致。
4. 校验通过后锁定并分配具体设备，将报价快照复制到预占，并记录 `source_quote_id`。
5. `reservation.source_quote_id` 建立唯一约束，抵御同一报价使用不同幂等键并发创建多个预占。
6. 报价一旦成功用于预占，不得再次使用；预占释放或过期后也不恢复报价，用户必须重新获取报价。
7. 报价只需在创建预占时有效。
8. 预占成功后，价格冻结到预占到期，不再受原报价有效期或商品价格变化影响。
9. 原报价保持不可变，用于审计，不因预占创建而修改。
11. 创建订单只消费预占及其冻结价格快照，不重新计算价格。
12. 创建订单时使用 `SELECT ... FOR UPDATE` 锁定预占记录，并再次校验归属、`effectiveStatus` 和 `start_at > CURRENT_TIMESTAMP(6)`。
13. `orders.source_reservation_id` 建立唯一约束，保证一个预占最多创建一个订单。
14. 消费预占必须执行 `ACTIVE → CONSUMED` 条件更新并检查影响行数；影响行数不是 1 时返回 `RESERVATION_STATE_CONFLICT`。
15. 订单记录 `source_reservation_id`，形成报价 → 预占 → 订单追踪链。

第一版不接入支付，因此押金和租金只作为订单金额记录，不产生真实资金交易。

### 6.5 订单

第一版订单只覆盖创建后的基础查询，不处理交付和归还。

订单初始状态：

```text
CREATED
```

订单创建必须满足：

- 用户已认证。
- 预占属于当前用户。
- 预占满足有效预占判定，且未释放、未被消费。
- 预占租期尚未开始，即 `start_at > CURRENT_TIMESTAMP(6)`。
- 预占中的冻结价格快照完整有效。
- 设备当前状态允许出租。
- 请求包含有效的 `Idempotency-Key`。

## 7. 核心用户流程

### 7.1 登录流程

```text
用户输入账号和密码
  → 前端提交登录请求
  → Java 校验凭据
  → 返回访问令牌和用户信息
  → 前端保存认证状态
  → 进入商品页
```

异常情况：

- 账号不存在或密码错误：统一提示“账号或密码错误”。
- 用户被禁用：提示账号不可用。

### 7.2 搜索与可租查询流程

```text
用户选择开始时间和结束时间
  → 输入关键词或浏览商品
  → Java 查询匹配商品
  → Java 根据设备实例和占用记录计算可租状态
  → 返回商品列表、可租数量和最低参考价格
```

要求：

- `start_at` 必须早于 `end_at`。
- 开始时间不得早于允许的最早租赁时间。
- 商品搜索与实时库存判断分离执行。
- 商品存在不代表指定时间一定可租。
- 列表中的可租结果只代表查询时刻，不保证后续预占一定成功。

### 7.3 商品详情与报价流程

```text
用户进入商品详情
  → 查看商品参数和租赁说明
  → 选择租期
  → 请求可租状态和报价
  → Java 返回当前可租数量、租金、押金和规则摘要
```

报价结果必须明确：

- 商品名称
- 租期
- 计费天数
- 单价规则
- 租金
- 押金
- 报价有效期
- 是否当前可租

报价不锁定库存。用户只有创建预占后才获得短期库存保障。

### 7.4 库存预占流程

```text
用户点击“立即预占”
  → 前端提交 quote_id 和 Idempotency-Key
  → Java 锁定报价记录
  → Java 校验报价归属、有效期、未被消费、商品和租期
  → Java 校验用户有效预占数量上限
  → Java 查询候选设备实例
  → 按稳定顺序锁定候选设备
  → 锁后重新检查时间冲突和设备状态
  → 同一事务内写入预占、复制价格快照并消费报价
  → 返回预占信息和到期时间
```

并发要求：

- 同一商品只剩最后一台可用设备时，并发请求只能有一个成功。
- 锁定顺序应稳定，降低死锁概率。
- 发生数据库死锁时只能有限重试。
- 锁后发现冲突时返回库存不足，不得超卖。

### 7.5 下单流程

```text
用户进入下单确认页
  → 前端显示预占倒计时和价格快照
  → 用户确认下单
  → 前端生成或复用 Idempotency-Key
  → Java 校验用户、有效预占、冻结价格快照和设备状态
  → 同一事务内创建订单、复制预占价格快照、保存明细与状态历史、消费预占
  → 返回订单详情
```

要求：

- 页面重复点击不能生成多张订单。
- 网络超时后，前端必须使用原 `Idempotency-Key` 重试。
- 相同幂等键和相同请求返回第一次成功结果。
- 相同幂等键对应不同请求内容时返回幂等冲突。
- 下单成功后预占状态变为已消费。

### 7.6 订单查询流程

```text
用户进入订单页
  → 查询当前用户订单列表
  → 查看订单详情
```

订单列表展示：

- 订单编号
- 商品名称
- 租赁时间
- 订单状态
- 租金
- 押金
- 创建时间

订单详情展示：

- 订单基本信息
- 商品快照
- 设备实例展示编号
- 租赁时间
- 价格明细和规则快照
- 状态历史

设备序列号等内部敏感字段不应完整暴露给普通用户。

### 7.7 GearMate 对话流程

```text
用户输入租赁需求
  → GearMate 判断是否需要查询商品
  → 调用商品搜索工具
  → 根据用户给出的租期调用库存工具
  → 必要时调用报价工具
  → 流式返回推荐、真实库存和报价
```

示例问题：

- “我周末拍毕业照，有什么相机适合新手？”
- “Sony A7M4 从周五晚上到周日下午还有吗？”
- “租两天大概要多少钱，押金多少？”

Agent 规则：

- 不得凭模型记忆编造商品、库存和价格。
- 涉及当前库存和报价时必须调用 RentFlow 工具。
- 用户未提供完整租期时，应要求补充开始和结束时间。
- 对“周五晚上”“下周末”等模糊时间，GearMate 必须结合用户 IANA 时区转换为明确的 ISO 8601 起止时间，并向用户展示确认；确认前不能查询实时库存或创建正式报价。
- 时间语义确认只用于确认租期，不属于 Agent 写操作审批。
- 工具失败时明确说明暂时无法查询，不得生成虚假结果。
- 第一版不能调用预占和下单接口。
- 最终回答应说明库存和报价具有时效性。

## 8. 功能需求

### 8.1 身份认证

| 编号 | 需求 |
|---|---|
| AUTH-01 | 用户可以使用账号和密码登录。 |
| AUTH-02 | 登录成功返回访问令牌、用户 ID、昵称和角色。 |
| AUTH-03 | 除公开商品浏览外，预占、下单和订单查询必须认证。 |
| AUTH-04 | 服务端从认证上下文读取用户身份，不信任请求体中的用户 ID。 |
| AUTH-05 | 密码必须使用安全哈希保存，不得明文存储或记录日志。 |
| AUTH-06 | 鉴权失败返回统一错误结构。 |

### 8.2 商品搜索

| 编号 | 需求 |
|---|---|
| CAT-01 | 支持按商品名称、品牌和型号关键词搜索。 |
| CAT-02 | 支持按类目筛选。 |
| CAT-03 | 支持分页，默认排序规则稳定。 |
| CAT-04 | 支持在提供租期时返回当前可租数量。 |
| CAT-05 | 商品列表不暴露内部成本或设备序列号。 |
| CAT-06 | 第一版使用 MySQL `FULLTEXT ... WITH PARSER ngram` 和普通组合索引，不使用 Elasticsearch；固定并记录 `ngram_token_size`。 |
| CAT-07 | 型号精确匹配优先于全文相关性排序，并覆盖中文、英文型号和中英混合关键词。 |

### 8.3 库存查询

| 编号 | 需求 |
|---|---|
| INV-01 | 根据商品和租期查询可租设备实例数量。 |
| INV-02 | 时间冲突使用统一半开区间条件。 |
| INV-03 | 有效预占和已创建订单均计入占用。 |
| INV-04 | 已过期预占不得阻塞新请求。 |
| INV-05 | 查询结果只作即时参考，最终结果以预占事务为准。 |
| INV-06 | 非可租状态的设备实例不得作为候选。 |

### 8.4 库存预占

| 编号 | 需求 |
|---|---|
| RES-01 | 用户可以为一个商品和租期创建预占。 |
| RES-02 | 预占成功时必须分配一台具体设备实例。 |
| RES-03 | 预占具有可配置的到期时间。 |
| RES-04 | 预占创建必须锁定候选设备并在锁后重查。 |
| RES-05 | 用户可以主动释放未消费的有效预占。 |
| RES-06 | 过期清理任务使用数据库时间判断。 |
| RES-07 | 清理任务多实例运行时不得重复处理或相互阻塞。 |
| RES-08 | 预占冲突返回明确的库存不足错误。 |
| RES-09 | 创建预占必须提交有效 `quote_id` 和 `Idempotency-Key`。 |
| RES-10 | 报价快照在创建预占时复制并冻结，订单不得重新计算价格。 |
| RES-11 | 单个用户最多同时拥有配置数量的有效预占，V1 默认上限为 3。 |
| RES-12 | 所有有效性判断使用 SQL 中的 `CURRENT_TIMESTAMP(6)`，MySQL 业务时间字段使用 `DATETIME(6)`，不依赖清理任务是否已运行。 |
| RES-13 | 一个报价最多成功创建一个预占，`reservation.source_quote_id` 必须唯一。 |
| RES-14 | 预占查询返回计算后的 `effectiveStatus`，逻辑过期记录不得表现为有效 `ACTIVE`。 |

### 8.5 报价

| 编号 | 需求 |
|---|---|
| PRI-01 | Java 根据商品和租期计算报价。 |
| PRI-02 | 金额使用 `BigDecimal` 和 MySQL `DECIMAL`。 |
| PRI-03 | 报价包含计费天数、租金、押金和规则摘要。 |
| PRI-04 | 报价具有有效期和唯一标识。 |
| PRI-05 | Agent 和 Vue 必须使用同一 Java 报价接口。 |
| PRI-06 | Python 和前端不得自行计算最终金额。 |
| PRI-07 | V1 使用 CNY、按 24 小时向上取整计费、固定押金和 HALF_UP 两位小数。 |
| PRI-08 | 报价不可变，只需在创建预占时有效。 |
| PRI-09 | 报价、预占与订单价格快照均包含 `pricingVersion` 和 `pricingRule`。 |

### 8.6 订单

| 编号 | 需求 |
|---|---|
| ORD-01 | 用户可以将自己的有效预占转换为订单。 |
| ORD-02 | 创建订单必须提供 `Idempotency-Key`。 |
| ORD-03 | 订单保存商品、价格规则和金额快照。 |
| ORD-04 | 订单保存初始状态和状态历史。 |
| ORD-05 | 用户只能查询自己的订单。 |
| ORD-06 | 重复请求不得重复创建订单。 |
| ORD-07 | 预占过期、租期已经开始或状态变化时返回冲突，不得下单。 |
| ORD-08 | 创建订单必须锁定预占，`orders.source_reservation_id` 唯一，并通过条件更新消费预占。 |

### 8.7 GearMate

| 编号 | 需求 |
|---|---|
| AGT-01 | 支持创建和继续对话会话。 |
| AGT-02 | 支持通过 SSE 流式返回文本和工具状态。 |
| AGT-03 | 提供 `search_products` 只读工具。 |
| AGT-04 | 提供 `check_availability` 只读工具。 |
| AGT-05 | 提供 `get_rental_quote` 只读工具。 |
| AGT-06 | 工具只通过 HTTP 调用 RentFlow API。 |
| AGT-07 | 第一版不提供预占、下单、取消等写工具。 |
| AGT-08 | 会话与 LangGraph checkpoint 持久化到 PostgreSQL。 |
| AGT-09 | 模型调用通过适配接口封装，首版只实现一个实际模型提供方。 |
| AGT-10 | 模型失败、工具失败和输出截断必须映射为统一内部状态。 |
| AGT-11 | 同一会话最多存在一个活动运行，创建 run 时锁定 conversation，并通过 PostgreSQL partial unique index 保证并发唯一；重复发送返回 `409 CONVERSATION_RUN_ACTIVE`。 |
| AGT-12 | GearMate 必须校验会话和运行属于当前认证用户。 |
| AGT-13 | SSE 运行事件支持稳定事件 ID、断线恢复和幂等取消。 |

## 9. 页面需求

### 9.1 登录页

组件：

- 账号输入框
- 密码输入框
- 登录按钮
- 错误提示

交互：

- 输入为空时禁止提交并显示校验提示。
- 登录中按钮进入加载状态，避免重复提交。
- 登录成功跳转商品列表页。

### 9.2 商品列表页

组件：

- 搜索框
- 类目筛选
- 开始时间与结束时间选择器
- 商品卡片
- 分页控件
- GearMate 入口

商品卡片显示：

- 商品名称
- 品牌和型号
- 简短描述
- 参考日租金
- 指定租期内可租数量或“暂无可租”

### 9.3 商品详情页

组件：

- 商品基本信息
- 核心参数
- 租期选择器
- 库存状态
- 报价明细
- “立即预占”按钮

状态：

- 未选择租期
- 查询中
- 可租
- 不可租
- 报价已过期
- 请求失败

### 9.4 下单确认页

组件：

- 商品摘要
- 租期
- 预占倒计时
- 租金和押金
- 计费规则摘要
- 确认下单按钮
- 释放预占按钮

交互：

- 倒计时归零后禁止提交，并提示重新查询。
- 下单请求超时时保留幂等键，允许重试。
- 下单成功跳转订单详情。

### 9.5 订单列表页

组件：

- 订单列表
- 状态筛选
- 分页
- 订单详情入口

第一版仅有 `CREATED` 状态，但前端展示需按枚举设计，避免使用自由文本。

### 9.6 订单详情页

显示：

- 订单编号和状态
- 商品快照
- 租赁时间
- 租金、押金和金额合计
- 价格规则快照
- 状态历史

### 9.7 GearMate 对话页

组件：

- 消息列表
- 文本输入框
- 发送和停止按钮
- 工具调用状态
- 商品推荐卡片
- 库存与报价卡片

GearMate 对话页使用的 SSE 事件、断线恢复和取消语义统一引用 **12.3 GearMate API**，不得在页面层另行定义事件名称。前端不能把工具返回的 HTML 直接插入页面，所有模型和工具文本默认按不可信内容处理。

## 10. 服务与数据边界

### 10.1 应用关系

```text
rentflow-web
├── HTTP → rentflow-server
└── SSE/HTTP → gearmate-agent

gearmate-agent
└── HTTP → rentflow-server

rentflow-server
└── 不依赖 gearmate-agent
```

### 10.2 数据所有权

| 服务 | 数据库 | 数据范围 |
|---|---|---|
| RentFlow Java | MySQL | 用户、商品、设备实例、预占、报价快照、订单、幂等和业务审计 |
| GearMate Python | PostgreSQL | 会话、消息、LangGraph checkpoint、模型调用和工具调用日志 |
| Vue | 无 | 仅保存必要的客户端认证和界面状态 |

约束：

- Python 禁止直接读取或修改 Java 的 MySQL 表。
- Java 禁止直接读取 Python 的 PostgreSQL 表。
- 跨服务数据访问必须通过明确的 REST API。
- MySQL 是库存、价格和订单的唯一业务事实源。

### 10.3 GearMate 认证与授权

V1 JWT 参数：

```text
alg = RS256
iss = rentflow-server
aud = rentflow-platform
access token TTL = 30 分钟
refresh token = V1 不实现
```

- RentFlow Java 是用户 JWT 的唯一签发方。
- Java 持有 RSA 私钥；Java 和 GearMate 配置对应公钥。
- 同一个用户 JWT 以 `aud = rentflow-platform` 同时供 RentFlow Java 与 GearMate 接受，两个服务执行相同的签名、`iss`、`aud`、`exp` 和权限校验。
- Web 调用 Java 或 GearMate 时必须携带 `Authorization: Bearer <JWT>`。
- GearMate 调用 RentFlow 只读工具时转发当前用户 JWT，不签发新用户令牌、不提升权限。
- Java 和 GearMate 均从认证上下文获取用户身份，不接受请求体中的 `user_id` 代替认证身份。
- JWT 只能存在于请求认证上下文中，不得进入模型提示词、会话消息、checkpoint、工具参数、工具结果或日志。
- GearMate 必须校验 conversation、message run 和事件流均属于当前用户。
- Web 将 JWT 存放于 `sessionStorage`。V1 不使用 `localStorage`，也不实现刷新令牌。
- JWT 过期或任一服务返回 401 时，Web 必须停止当前 Agent 流、清除 Pinia 用户状态和 `sessionStorage`，并跳转登录页。
- 退出登录执行相同的客户端清理；V1 无服务端令牌撤销列表，令牌依靠短有效期失效。
- `sessionStorage` 不能抵御 XSS，因此所有模型、工具和用户富文本均按不可信内容处理，禁止直接注入 HTML。
- Java 与 GearMate 分别通过配置维护 CORS 来源白名单。V1 仅允许配置的 Web Origin、必要方法和请求头；禁止使用 URL 查询参数传递 JWT，禁止在启用 credentials 时使用通配来源 `*`。

## 11. Java 模块边界

第一版实现以下模块：

| 模块 | 第一版职责 |
|---|---|
| `identity` | 登录、JWT、用户身份与角色 |
| `catalog` | 类目、商品、型号和商品搜索 |
| `inventory` | 设备实例、可租查询、预占与释放 |
| `pricing` | 租金、押金、报价和价格快照 |
| `ordering` | 创建订单、幂等、订单查询和状态历史 |
| `audit` | 关键写操作审计 |

以下模块保留在后续规划中，第一版不实现业务功能：

- `fulfillment`
- `returninspection`
- `asset`
- `integration`

模块只能调用其他模块公开的 application API，禁止跨模块调用 Mapper 或直接查询其他模块的数据表。

## 12. 接口契约

本节定义产品级接口语义。机器可执行的完整请求/响应 JSON Schema 由各服务 OpenAPI 文档维护，PRD 不复制第二份完整 Schema，避免契约漂移。Java 与 Python 的 OpenAPI 必须覆盖：

- 请求体、响应体及错误体 JSON Schema
- 必填字段、最大长度、数值范围和枚举
- ISO 8601 带时区时间格式
- 分页结构、默认页大小、最大页大小和稳定排序
- `Authorization`、`Idempotency-Key`、`Last-Event-ID` 等请求头
- 每个接口可能返回的业务错误码
- 成功及关键失败示例

### 12.1 通用契约

#### 认证

```http
Authorization: Bearer <user-jwt>
```

除明确标注公开的接口外，均要求用户 JWT。GearMate 工具调用转发当前用户 JWT，但不得将其写入业务载荷。

#### 时间

- 字段名统一为 `startAt`、`endAt`、`expiresAt` 等 camelCase JSON 字段。
- 时间值使用 ISO 8601 带时区格式，例如 `2026-07-17T18:00:00+08:00`。
- 服务端转换为 UTC 存储和比较。

#### 分页

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "sort": "createdAt,desc"
}
```

- `page` 从 0 开始。
- 默认 `size=20`，最大 `size=100`。
- 排序字段使用服务端白名单。
- 订单默认按 `createdAt DESC, id DESC` 排序，保证结果稳定。

#### 业务 ID

- V1 所有对外业务 ID 统一使用 26 字符 ULID，包括用户、商品、报价、预占、订单、会话、消息、运行和事件 ID。
- OpenAPI 统一定义为 `type: string`，格式正则为 `^[0-9A-HJKMNP-TV-Z]{26}$`。
- 禁止在同一接口契约中混用 UUID、数据库自增 ID 和 ULID。
- 数据库可以另设内部物理主键，但对外关联、日志和接口只使用 ULID；如直接以 ULID 为主键，应采用固定长度、区分大小写的存储与索引配置。

#### 幂等键

```http
Idempotency-Key: <16-128 printable ASCII characters>
```

- 作用域为“当前用户 + 接口 + 幂等键”。
- 服务端保存规范化请求摘要、执行状态和可重放响应。
- 已提交成功的响应和确定性的 4xx/409 业务结果必须保存；相同键和相同请求重放原 HTTP 状态、业务码和响应体。
- 事务完全回滚的临时网络错误或 5xx 不作为最终结果缓存，幂等记录标记为可重试；使用原键重试时允许重新执行。
- 无法确认事务是否提交时不得盲目重试业务写入，必须先通过幂等记录查询原执行结果。
- 相同键对应不同请求返回 `IDEMPOTENCY_CONFLICT`。
- 相同请求并发到达时，后续请求等待首个请求完成；超过等待上限返回 `IDEMPOTENCY_IN_PROGRESS`，并携带配置化的 `Retry-After` 响应头。
- 客户端只是重试同一次业务意图时复用原键；用户发起新的报价、预占或订单业务尝试时必须生成新键。
- 预占幂等记录至少保存至预占到期后 24 小时。
- 订单幂等记录长期保存，不随订单业务记录清理而提前删除。
- 主动释放预占和取消 Agent 运行本身设计为幂等操作。

### 12.2 RentFlow API

| 方法 | 路径 | 用途 | 认证 | 关键契约 |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | 用户登录 | 否 | 账号、密码；返回 JWT 和用户时区 |
| GET | `/api/v1/categories` | 类目列表 | 否 | 稳定排序 |
| GET | `/api/v1/products` | 商品搜索与分页 | 否 | `keyword`、`categoryId`、分页；可选租期 |
| GET | `/api/v1/products/{productId}` | 商品详情 | 否 | 26 字符 ULID 路径参数 |
| POST | `/api/v1/availability/search` | 查询指定租期可租状态 | 否 | 商品、明确租期；只读即时结果 |
| POST | `/api/v1/quotes` | 创建正式报价 | 是 | 商品、明确租期；返回不可变报价 |
| POST | `/api/v1/reservations` | 创建库存预占 | 是 | `quoteId`；要求幂等键 |
| DELETE | `/api/v1/reservations/{reservationId}` | 主动释放预占 | 是 | 幂等；仅限所有者 |
| GET | `/api/v1/reservations/{reservationId}` | 查询预占 | 是 | 仅限所有者 |
| POST | `/api/v1/orders` | 幂等创建订单 | 是 | `reservationId`；要求幂等键 |
| GET | `/api/v1/orders` | 当前用户订单列表 | 是 | 分页、状态筛选 |
| GET | `/api/v1/orders/{orderId}` | 当前用户订单详情 | 是 | 仅限所有者 |

关键写接口示例：

```http
POST /api/v1/reservations
Authorization: Bearer <user-jwt>
Idempotency-Key: 01J2RESERVATIONABC
Content-Type: application/json
```

```json
{
  "quoteId": "01J2QUOTEABC"
}
```

```json
{
  "reservationId": "01J2RESERVATIONXYZ",
  "sourceQuoteId": "01J2QUOTEABC",
  "productId": "01J2PRODUCTABC",
  "startAt": "2026-07-17T10:00:00Z",
  "endAt": "2026-07-18T10:00:00Z",
  "status": "ACTIVE",
  "effectiveStatus": "ACTIVE",
  "expiresAt": "2026-07-13T10:15:00Z",
  "priceSnapshot": {
    "currency": "CNY",
    "pricingVersion": 1,
    "pricingRule": "DAILY_CEIL_24H_FIXED_DEPOSIT",
    "billingDays": 1,
    "dailyRate": "200.00",
    "rentalAmount": "200.00",
    "depositAmount": "3000.00",
    "totalAmount": "3200.00",
    "roundingMode": "HALF_UP"
  }
}
```

```http
POST /api/v1/orders
Authorization: Bearer <user-jwt>
Idempotency-Key: 01J2ORDERABCDEF
Content-Type: application/json
```

```json
{
  "reservationId": "01J2RESERVATIONXYZ"
}
```

### 12.3 GearMate API

| 方法 | 路径 | 用途 | 认证 | 关键契约 |
|---|---|---|---|---|
| POST | `/api/v1/conversations` | 创建会话 | 是 | 返回 conversation ID，绑定当前用户和 IANA 时区 |
| GET | `/api/v1/conversations` | 当前用户会话列表 | 是 | 分页、更新时间倒序 |
| GET | `/api/v1/conversations/{conversationId}` | 会话详情 | 是 | 仅限所有者 |
| POST | `/api/v1/conversations/{conversationId}/messages` | 创建用户消息和运行 | 是 | 返回 `messageId`、`runId`；不直接承载 SSE |
| GET | `/api/v1/conversations/{conversationId}/runs/{runId}/events` | 读取运行 SSE | 是 | 支持 `Last-Event-ID` 或 `afterEventId` 恢复 |
| POST | `/api/v1/conversations/{conversationId}/runs/{runId}/cancel` | 取消运行 | 是 | 幂等；仅限所有者 |

创建消息响应示例：

```json
{
  "messageId": "01J2MESSAGEABC",
  "runId": "01J2RUNABC",
  "status": "RUNNING",
  "eventsUrl": "/api/v1/conversations/01J2CONVABC/runs/01J2RUNABC/events"
}
```

运行约束：

- 一个 conversation 同时最多只有一个活动 run。
- 创建 run 必须在 PostgreSQL 事务中锁定 conversation 行，并在锁内检查活动运行；同时对活动状态建立 partial unique index 作为数据库最终防线。
- partial unique index 仅覆盖 `RUNNING`、`TOOL_REQUESTED` 等活动状态，保证每个 `conversation_id` 最多一条活动记录。
- 已有活动 run 时重复发送消息返回 `409 CONVERSATION_RUN_ACTIVE`。
- 每个持久化 SSE 事件包含稳定、单调递增的 `id`、`runId`、`type` 和 `createdAt`。
- 客户端断线后通过 `Last-Event-ID` 或 `afterEventId` 读取遗漏事件。
- 已完成和已取消运行允许在事件保留期内重放事件，但不能恢复模型执行。
- `cancel` 为幂等操作；已结束运行再次取消返回当前最终状态。
- 运行事件至少保留 24 小时，具体期限通过配置提供。

SSE 事件：

| 事件 | 用途 |
|---|---|
| `run.started` | 运行开始 |
| `message.delta` | 增量文本 |
| `tool.started` | 工具开始执行 |
| `tool.completed` | 工具执行成功 |
| `tool.failed` | 工具执行失败 |
| `run.completed` | 本轮完成 |
| `run.cancelled` | 本轮已取消 |
| `run.failed` | 本轮失败 |

## 13. 状态设计

### 13.1 设备实例状态

第一版至少包含：

```text
AVAILABLE
DISABLED
```

- `AVAILABLE`：允许参与候选分配，但是否可租仍需检查时间占用。
- `DISABLED`：不可参与分配。

维修、丢失、报废等状态后续扩展。

### 13.2 预占状态

持久化状态：

```text
ACTIVE
CONSUMED
RELEASED
EXPIRED
```

允许转换：

```text
ACTIVE → CONSUMED
ACTIVE → RELEASED
ACTIVE → EXPIRED
```

其他转换一律拒绝。

API 额外返回 `effectiveStatus`：

```text
if status = ACTIVE and expires_at <= CURRENT_TIMESTAMP(6):
    effectiveStatus = EXPIRED
else:
    effectiveStatus = status
```

库存、订单和界面业务判断必须使用 `effectiveStatus`。持久化 `status` 仅用于状态收敛和审计。

### 13.3 报价状态

报价不可变且一次性使用。可用于预占必须同时满足：未过期、属于当前用户，且尚未被任何预占通过唯一 `source_quote_id` 消费。预占释放或过期后，原报价也不得恢复使用。

### 13.4 订单状态

第一版：

```text
CREATED
```

状态历史仍需建立，为后续取消、交付、租赁中和归还流程提供扩展基础。

### 13.5 Agent 运行状态

通用内部状态，不绑定具体模型厂商：

```text
RUNNING
TOOL_REQUESTED
COMPLETED
OUTPUT_TRUNCATED
REFUSED
FAILED
CANCELLED
```

## 14. 错误处理

统一错误结构建议：

```json
{
  "code": "INVENTORY_NOT_AVAILABLE",
  "message": "所选租期暂无可租设备",
  "correlationId": "01J...",
  "details": {}
}
```

关键错误码：

| 错误码 | HTTP 状态 | 场景 |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | 参数格式或租期无效 |
| `AUTHENTICATION_REQUIRED` | 401 | 未登录或令牌失效 |
| `ACCESS_DENIED` | 403 | 访问他人资源 |
| `PRODUCT_NOT_FOUND` | 404 | 商品不存在 |
| `RESERVATION_NOT_FOUND` | 404 | 预占不存在 |
| `ORDER_NOT_FOUND` | 404 | 订单不存在 |
| `INVENTORY_NOT_AVAILABLE` | 409 | 无可用设备或并发争抢失败 |
| `RESERVATION_EXPIRED` | 409 | 预占已过期 |
| `RESERVATION_STATE_CONFLICT` | 409 | 预占已释放、已消费，或被并发订单请求抢先消费 |
| `RENTAL_ALREADY_STARTED` | 409 | 创建预占或订单时租赁开始时间已经到达 |
| `QUOTE_NOT_FOUND` | 404 | 报价不存在或不属于当前用户 |
| `QUOTE_EXPIRED` | 409 | 报价过期 |
| `QUOTE_MISMATCH` | 409 | 报价的用户、商品或租期与预占请求不一致 |
| `QUOTE_ALREADY_CONSUMED` | 409 | 报价已经成功用于其他预占 |
| `IDEMPOTENCY_CONFLICT` | 409 | 相同幂等键对应不同请求 |
| `IDEMPOTENCY_IN_PROGRESS` | 409 | 相同幂等请求仍在处理中 |
| `ACTIVE_RESERVATION_LIMIT_EXCEEDED` | 409 | 用户有效预占数量达到上限 |
| `CONVERSATION_RUN_ACTIVE` | 409 | 会话已有活动运行 |
| `AGENT_RUN_NOT_FOUND` | 404 | Agent 运行不存在或不属于当前用户 |
| `AGENT_MODEL_UNAVAILABLE` | 503 | 模型服务不可用 |
| `UPSTREAM_RENTFLOW_UNAVAILABLE` | 503 | Agent 无法访问 Java 服务 |
| `INTERNAL_ERROR` | 500 | 未分类服务端错误 |

安全要求：错误响应不得暴露 SQL、堆栈、密钥、内部网络地址或数据库结构。

## 15. 非功能需求

### 15.1 一致性

- 库存预占和订单创建使用数据库事务。
- 关键库存 SQL 必须显式实现，不使用内存锁代替数据库一致性。
- 预占转订单时，订单创建与预占消费必须在同一事务完成。
- 金额不得使用浮点数。

### 15.2 性能目标

V1 性能基线采用以下统一条件：

- 环境：本地 Docker Compose，应用运行于同一开发机；记录 CPU、内存和磁盘型号。
- 数据规模：6 个商品、每个商品 1–3 台设备、10,000 条历史订单、10,000 条历史预占。
- 预热：每个场景先执行 20 次，不计入统计。
- 样本：每个场景至少 500 次请求。
- 并发：普通查询 20 个并发用户；预占和订单写入 10 个并发用户。
- 统计：报告 P50、P95、P99 和错误率；业务冲突响应不计为系统错误。

在该基线下：

- 普通商品查询 P95 小于 500ms。
- 可租查询 P95 小于 800ms。
- 预占创建 P95 小于 1s，不包含业务配置允许的数据库锁等待超时。
- 订单创建 P95 小于 1s。
- GearMate 收到模型提供方文本增量后，应在 100ms 内将对应 `message.delta` 写入 SSE 响应；模型首个输出延迟单独记录，不计入转发耗时。

以上为第一版本地验收基线，不作为生产容量承诺。

### 15.3 安全

- 密钥和密码通过环境变量或本地安全配置注入。
- 仓库只提供不含敏感值的示例配置。
- JWT 使用非对称签名，私钥仅由 Java 持有；Java 与 GearMate 使用公钥验签。
- JWT 必须校验签名、`iss`、`aud`、`exp` 和必要权限声明。
- 所有订单、报价、预占、Agent 会话和运行接口执行资源归属校验。
- MyBatis 参数必须使用安全绑定，禁止字符串拼接用户输入。
- 商品排序字段使用服务端白名单。
- 模型和工具输出均视为不可信内容。
- 日志不得记录密码、完整令牌或模型厂商密钥。

### 15.4 可观测性

每个请求至少记录：

- Correlation ID
- 服务名称
- 请求路径和方法
- 结果状态
- 处理耗时
- 当前用户匿名化标识

Agent 调用额外记录：

- 会话 ID
- 运行 ID
- 模型提供方与模型 ID
- 工具名称
- 工具耗时和结果状态
- Token 使用量（若模型提供）

日志不能记录完整敏感提示词、认证令牌或未经处理的个人信息。

### 15.5 可配置性

以下参数必须通过配置提供：

- 服务端口
- 数据库地址和凭据
- JWT 密钥与有效期
- 预占有效时间与单用户有效预占上限
- 报价有效时间
- 幂等请求等待时间和记录保留期
- Agent 运行事件保留期
- Agent 默认模型提供方和模型 ID
- 模型 API 地址和凭据
- RentFlow 内部 API 地址
- HTTP 超时和有限重试次数

## 16. 数据初始化

第一版提供可重复执行的开发演示数据：

- 2 个普通用户
- 至少 3 个商品类目
- 至少 6 个商品
- 每个商品 1–3 台设备实例
- 至少一个商品仅有 1 台设备，用于并发争抢测试
- 报价与价格快照包含 `pricingVersion` 和 `pricingRule`，并随报价复制到预占、再复制到订单。

示例商品可包括：

- Sony A7M4 相机机身
- Canon EOS R6 Mark II
- Sony FE 24-70mm F2.8 GM II
- DJI Air 3 无人机
- MacBook Pro 14
- GoPro HERO 系列运动相机

演示账号不得使用生产密码，凭据应在开发说明中明确标注仅供本地使用。

## 17. 验收测试

### 17.1 登录

- 正确账号密码登录成功。
- 错误密码不能登录。
- 未认证用户不能创建预占或订单。
- 用户不能查看他人订单。

### 17.2 时间区间与计费边界

- 相邻半开区间不冲突。
- 存在任意重叠的区间均冲突。
- 开始时间等于或晚于结束时间时拒绝。
- `start_at` 不晚于数据库当前时间时拒绝。
- 创建预占和创建订单两个阶段都必须重新执行该校验。
- 预占到期时间取“配置 TTL 到期时间”和 `start_at` 中较早者。
- 少于 1 小时、超过 30 天或开始时间超过未来 90 天时拒绝。
- API 正确解析 ISO 8601 时区偏移，并按 UTC 比较。
- `Asia/Shanghai` 用户选择跨午夜租期时，页面、Agent 确认时间和 Java 计费结果一致。

计费边界：

| 实际租期 | 计费天数 |
|---:|---:|
| 1 小时 | 1 天 |
| 23 小时 | 1 天 |
| 24 小时 | 1 天 |
| 25 小时 | 2 天 |
| 48 小时 | 2 天 |

前端报价页和 GearMate 必须明确提示“最短可租 1 小时，最小按 1 天计费”。

### 17.3 库存并发

准备只有一台设备的商品：

1. 两个用户对相同租期同时创建预占。
2. 仅一个请求成功。
3. 另一个请求返回 `INVENTORY_NOT_AVAILABLE`。
4. 数据库中只能存在一个有效冲突预占。

### 17.4 报价、预占绑定与过期

1. 报价有效时可以创建预占。
2. 报价已过期时创建预占返回 `QUOTE_EXPIRED`，不得锁定设备。
3. 报价成功绑定预占后，即使原报价随后过期，仍可在预占有效期内按冻结价格创建订单。
4. 商品日租金或押金在报价后发生变化时，已有报价不变；预占复制原报价；订单复制预占快照。
5. 到期前其他用户无法预占相同设备和冲突租期。
6. 到期后其他用户可以成功预占。
7. 即使清理任务尚未运行，`expires_at <= CURRENT_TIMESTAMP(6)` 的 `ACTIVE` 记录也不能阻塞库存或创建订单。
8. 前端倒计时与服务端状态不一致时，以服务端 `effectiveStatus` 为准。
9. 查询逻辑已过期但尚未清理的预占时，API 返回 `effectiveStatus=EXPIRED`，不得向页面表现为有效 `ACTIVE`。
10. 用户不能查询或释放他人的预占。

### 17.5 预占幂等与并发

1. 使用一个 `Idempotency-Key` 创建预占成功。
2. 使用相同键和相同 `quoteId` 重复提交，返回同一预占。
3. 相同键并发提交时只创建一条预占记录。
4. 相同键提交不同 `quoteId` 时返回 `IDEMPOTENCY_CONFLICT`。
5. 用户已有 3 个有效预占时，第 4 个预占被拒绝；并发创建也不能突破上限。
6. 使用不同幂等键并发提交同一 `quoteId` 时，最多一个请求成功，其余返回 `QUOTE_ALREADY_CONSUMED`。
7. 预占释放或过期后，再次使用原 `quoteId` 仍返回 `QUOTE_ALREADY_CONSUMED`。

### 17.6 报价与金额

- 在价格配置版本未变化时，相同商品与租期生成相同的价格快照；`quoteId`、创建时间和到期时间允许不同。
- 金额精度正确，不出现浮点误差。
- 订单保存的价格快照不随商品价格修改而变化。
- Agent 与页面查询相同条件时返回相同报价。

### 17.7 订单幂等与并发

1. 使用一个 `Idempotency-Key` 创建订单成功。
2. 使用相同键和相同请求重复提交，返回同一订单。
3. 相同订单键并发提交时，数据库只存在一张订单。
4. 使用相同键提交不同预占或不同请求，返回 `IDEMPOTENCY_CONFLICT`。
5. 创建订单时只复制预占冻结价格，不查询原报价有效期、不重新计算金额。
6. 使用不同幂等键并发消费同一预占时，只允许一个成功；失败请求返回 `RESERVATION_STATE_CONFLICT`。
7. 租赁开始时间到达后，即使清理尚未收敛状态，也不得创建订单。
8. 订单中的 `pricingVersion`、`pricingRule` 和金额字段与预占冻结快照完全一致。

### 17.8 GearMate、认证与 SSE

- 用户询问商品推荐时，Agent 能调用商品工具。
- 用户询问实时库存时，Agent 必须调用库存工具。
- 用户询问费用时，Agent 返回 Java 计算的报价。
- 用户未给租期时，Agent 要求补充信息。
- “周五晚上”等模糊时间在用户确认明确起止时间前，不能触发实时库存或正式报价工具。
- Java 服务不可用时，Agent 不编造库存和价格。
- GearMate 不提供预占或下单能力。
- 用户 A 无法读取、继续、停止或订阅用户 B 的 conversation 和 run。
- 同一会话已有活动 run 时再次发送消息返回 `CONVERSATION_RUN_ACTIVE`。
- 两个请求并发向同一会话发送消息时，只创建一个 run，另一个返回 `CONVERSATION_RUN_ACTIVE`。
- SSE 断线后使用最后事件 ID 恢复，已接收事件不重复渲染，遗漏事件可以补读。
- 主动停止是幂等的，停止后收到 `run.cancelled`，不得继续产生模型或工具事件。
- JWT 不出现在模型输入、会话消息、checkpoint、工具日志或普通应用日志中。

### 17.9 中文搜索

- 中文品牌、中文商品名称可以通过 MySQL ngram FULLTEXT 命中。
- `A7M4`、`EOS R6` 等型号精确匹配优先于普通全文结果。
- 中英文混合关键词返回稳定且相关的结果。
- 固定 `ngram_token_size` 后运行回归搜索集，配置变化必须重新验证索引结果。

### 17.10 独立运行

- GearMate 停止时，Java 与 Vue 的租赁主流程仍可用。
- PostgreSQL 停止时，仅 GearMate 会话功能不可用，MySQL 中的租赁事实不受影响。
- 服务重启后，订单、有效预占和 Agent 会话仍然存在。

## 18. 第一版技术约束

### RentFlow Java

- Java 21
- Spring Boot 3.5.x
- Spring Web MVC
- Spring Security
- Spring Validation
- Spring Modulith
- MyBatis-Plus
- 复杂库存 SQL 使用 XML Mapper 或显式 Mapper SQL
- Spring Transaction
- Flyway
- MySQL 8.4
- MySQL ngram FULLTEXT parser，并固定记录 `ngram_token_size`
- JUnit 5、Testcontainers、Awaitility、ArchUnit/Spring Modulith tests

不使用 JPA、Hibernate、Spring Cloud、WebFlux、Spring State Machine 和分布式事务框架。

### GearMate Python

- Python 3.12
- uv
- FastAPI
- LangGraph
- LangChain Core
- 可插拔模型适配接口
- httpx
- SQLAlchemy 2 + Alembic
- PostgreSQL
- pytest、pytest-asyncio、respx、Ruff、mypy

第一版只接入一个实际可用的模型提供方，不同时实现多家模型集成。Anthropic SDK 只有在首版选用 Claude 时才加入依赖。

开发启动前必须提交并批准模型集成 ADR，至少固定：

- 模型提供方
- 精确模型 ID
- 官方或 LangChain 集成包及锁定版本
- API Base URL 与认证注入方式
- 连接超时、首个输出超时、单次请求超时和整轮运行上限
- 最大输出长度
- 必需的流式输出、结构化工具调用和工具调用 ID 能力
- 提供方错误向 GearMate 通用运行状态的映射
- 开发、测试环境是否使用真实模型或受控替身

ADR 未确定前可完成模型无关的接口、图状态和工具层，但不得宣称 GearMate 模型链路开发完成。

### RentFlow Web

- Node.js 22 LTS
- pnpm 10.x
- Vue 3.5
- TypeScript strict
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios：仅用于普通 HTTP 请求
- Fetch API 或 `@microsoft/fetch-event-source`：用于 GearMate SSE，可设置 `Authorization` 与断线恢复游标
- 禁止使用原生 `EventSource` 连接需要 Bearer JWT 的接口
- 禁止通过 URL 查询参数传递 JWT
- 断线恢复显式发送 `Last-Event-ID` 请求头或 `afterEventId` 参数
- dayjs
- Vitest + Vue Test Utils
- Playwright

## 19. 第一版交付物

```text
compose.yaml
rentflow-server/
gearmate-agent/
rentflow-web/
```

应提供：

- 可复现的本地启动配置
- 无敏感值的示例环境变量
- MySQL 与 PostgreSQL schema 迁移
- 可重复初始化的演示数据
- OpenAPI 文档
- Java、Python 和 Vue 自动化测试
- 最后一台设备并发争抢测试
- Agent 真实库存与报价工具测试
- 核心 Playwright 端到端测试

## 20. 第一版完成定义

只有同时满足以下条件，第一版才视为完成：

1. 用户可以从登录开始完成商品查询、租期选择、报价、预占、下单和订单查询。
2. 最后一台设备并发争抢测试稳定通过。
3. 预占过期后库存可以重新分配。
4. 重复下单不会创建重复订单。
5. GearMate 能查询真实商品、库存和报价，且不能执行写操作。
6. Java 服务不依赖 GearMate 即可运行完整租赁主流程。
7. 三个项目分别具备明确的启动和测试命令。
8. 所有核心流程通过自动化测试和真实页面端到端验证。
