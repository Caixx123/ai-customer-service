# 小智 · 电商 AI 智能客服（AgentScope Java MVP）

一个可运行的电商售后客服 Demo，用 **AgentScope Java 2.x** 做多 Agent 编排，用本地 ONNX `bge-small-zh-v1.5` + 内存余弦索引做知识库检索，用 H2 模拟订单/物流/退款数据。

## 功能

- **多 Agent 协作（Supervisor 模式）**
  - `customer-service-supervisor`：客服主管，面向用户，负责分派和最终回复
  - `order-expert`：订单/物流/退款查询专家
  - `policy-expert`：售后政策/FAQ 专家
- **工具调用**
  - `list_my_orders` / `query_order` / `query_logistics` / `query_refund`
  - `search_knowledge_base`
  - `handoff_to_human`（创建转人工工单）
- **本地 RAG**：7 篇售后政策 Markdown，启动时切分、向量化、建内存索引；回答带来源
- **多轮会话**：按 `(userId, sessionId)` 隔离，`JsonFileAgentStateStore` 持久化
- **SSE 流式输出**：前端可看到主管委派专家、专家完成、最终回复的过程
- **数据隔离**：所有订单查询强制带当前用户，跨用户查询统一返回「未找到或不属于当前用户」

## 技术栈

| 组件 | 版本/方案 |
|---|---|
| JDK | 21（本机 `D:\JDK\jdk21.0.12_8`） |
| Spring Boot | 4.1.2 |
| Agent 框架 | AgentScope Java 2.0.3（`agentscope-core` + `agentscope-extensions-model-openai`） |
| 对话模型 | OpenAI 兼容端点，默认 DeepSeek `deepseek-chat` |
| Embedding | 本地 ONNX `bge-small-zh-v1.5`（512 维，量化版） |
| ONNX 推理 | `com.microsoft.onnxruntime:onnxruntime:1.21.1` |
| Tokenizer | `ai.djl.huggingface:tokenizers:0.36.0` |
| 向量检索 | 自建内存余弦索引（L2 归一化 + 点积 + topK + 阈值） |
| 业务数据 | H2 + Spring Data JPA |
| 会话状态 | `JsonFileAgentStateStore`（`./data/agent-state`） |
| 前端 | 原生 HTML/CSS/JS + `fetch` 流式读取 SSE |

> 本项目**没有引入 Spring AI**。AgentScope 负责 Agent 编排；ONNX Runtime 只作为本地推理库，用于加载现成的 bge 模型。

## 快速开始

### 1. 配置 JDK 21

本机默认 `JAVA_HOME` 可能不是 21，构建和运行前切换：

```bash
export JAVA_HOME="D:/JDK/jdk21.0.12_8"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

IDEA 中把 Project SDK 和 Maven Runner JRE 都设为 JDK 21。

### 2. 准备 ONNX 模型

`src/main/resources/onnx/` 下应包含：

- `model_quantized.onnx`（约 23MB）
- `tokenizer.json`（约 430KB）

仓库已包含这两个文件；如果缺失，执行：

```bash
bash scripts/download-model.sh
```

模型来源：`BAAI/bge-small-zh-v1.5` 的 ONNX 转换版 `Xenova/bge-small-zh-v1.5`。模型许可证请以原模型仓库为准，商用前自行确认。

### 3. 配置对话模型

```bash
export AI_BASE_URL="https://api.deepseek.com"
export AI_API_KEY="sk-your-key"
export AI_MODEL="deepseek-chat"
```

也可以复制模板并填写 `.env`（应用会自动加载，`.env` 已加入 `.gitignore`）：

```bash
cp .env.example .env
```

说明：

- 任何 **OpenAI Chat Completions 兼容** 且支持 **function calling + streaming** 的端点都可以，例如 DeepSeek、自建网关、百炼 OpenAI 兼容模式等。
- 如果端点不支持 function calling，多 Agent 委派会退化为纯文本，无法完成订单查询。
- 未配置 `AI_API_KEY` 时应用仍可启动（会打印 WARN），但聊天请求会返回错误。

也支持用 `AI_TIMEOUT_SECONDS` 调整单次请求超时，默认 90 秒。

### 4. 启动

```bash
mvn spring-boot:run
```

打开 <http://localhost:8080>。

也可以打包运行：

```bash
mvn -DskipTests package
java -jar target/ai-customer-service-0.0.1-SNAPSHOT.jar
```

## 无 Key 联调（本地 mock）

如果暂时没有模型 Key，可以用 `scripts/mock-llm.js` 验证多 Agent、工具调用和 SSE 链路：

```bash
# 终端 1
node scripts/mock-llm.js

# 终端 2
AI_BASE_URL=http://127.0.0.1:8899 AI_API_KEY=mock AI_MODEL=mock-model mvn spring-boot:run
```

mock 会固定返回「主管 → 订单/政策专家 → 业务工具」的调用链和固定文案，只用于验证工程链路，不代表真实模型能力。

## 演示数据

| 用户 | 订单 | 状态 |
|---|---|---|
| user-001 张先生 | O20261023001 无线降噪耳机 299 | 已签收，顺丰已签收 |
| user-001 | O20261023002 不锈钢保温杯 159 | 退款中，商家审核中 |
| user-001 | O20261023003 机械键盘 499 | 运输中，中通在上海转运中心 |
| user-001 | O20261023005 手机保护壳 39 | 待发货 |
| user-002 李女士 | O20261023004 27寸显示器 1299 | 待发货 |

用 user-002 查询 `O20261023001`，应返回「未找到该订单，或该订单不属于当前用户」。

## 推荐问题

- 查一下我最近的订单
- 订单 O20261023003 到哪了
- 我的退款到哪一步了
- 7 天无理由怎么算
- 我要投诉，商品有质量问题

## API

### 同步调试接口

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-001" \
  -d '{"sessionId":"s1","message":"查一下我最近的订单"}'
```

### SSE 流式接口

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-001" \
  -d '{"sessionId":"s1","message":"查一下我最近的订单"}'
```

事件序列示例：

```text
event: agent_start   data: {"sessionId":"s1"}
event: tool_call     data: {"name":"order_expert","toolCallId":"..."}
event: tool_result   data: {"name":"order_expert","state":"SUCCESS",...}
event: reply         data: {"delta":"你最近有 4 笔订单..."}
event: done          data: {"sessionId":"s1"}
```

错误统一走 `event: error`，例如模型端点不可用。

## 架构

```text
浏览器聊天页
  POST /api/chat/stream  {sessionId, message}  Header: X-User-Id
        │
  ChatStreamController
        │
  SupervisorAgent「小智主管」（ReActAgent，JsonFileAgentStateStore）
        ├─ order_expert(query)  → OrderExpertAgent（无状态）
        │       └─ list_my_orders / query_order / query_logistics / query_refund → H2
        ├─ policy_expert(query) → PolicyExpertAgent（无状态）
        │       └─ search_knowledge_base
        │              └─ CosineIndex ← OnnxEmbeddingModel ← bge-small-zh-v1.5
        └─ handoff_to_human → tickets 表
        │
  Flux<AgentEvent> → AgentEventMapper → SSE → 前端
```

### 为什么不用 Spring AI

AgentScope 是多 Agent 编排框架，本身不内置 embedding 模型。上一版方案为了复用现成 ONNX embedding 引入 Spring AI，会让项目同时存在两套 AI 框架。本版改为：

- AgentScope：Agent 循环、工具、多 Agent、会话状态、流式事件
- ONNX Runtime + DJL tokenizer：只做本地 embedding 推理
- 自建余弦索引：只做内存向量检索

### 为什么 RAG 用自建检索而不是 AgentScope 原生 RAG

Java 版 AgentScope 的 RAG 扩展仍在快速演进，对本机 ONNX 模型和内存索引的适配需要额外评估。AgentScope 官方支持把检索封装成 Tool；本项目用这个方式，既符合框架用法，又能直接复用本地 bge 模型。后续可平滑替换为 AgentScope 原生 RAG、pgvector 或 Elasticsearch。

### 关于 MCP

**本期不接 MCP。** 业务工具都在同一个 Spring Boot 进程内，`@Tool` 链路更短、启动更快、断点更好打。MCP 会引入独立进程/网络、协议握手、工具发现、鉴权与超时治理，对单体 MVP 是额外成本。

后续出现以下信号时再考虑 MCP：

- 订单/知识库/工单工具由其他团队独立部署
- 需要跨语言或跨 Agent 复用同一套工具
- 需要独立扩缩容、限流、鉴权
- 已有现成的 MCP Server 可复用

迁移方式：把 `OrderService` / `KnowledgeBaseService` / `TicketService` 暴露成 MCP Server，订单专家 Toolkit 通过 `McpClientBuilder` 注册 MCP 客户端即可，Agent 层与提示词不变。MCP 工具在 Toolkit 中会以 `mcp/<name>` 命名。

## 测试

```bash
export JAVA_HOME="D:/JDK/jdk21.0.12_8"
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

当前离线测试覆盖：

- 本地 ONNX 模型加载、512 维、语义排序
- 7 篇知识文档切分与入库（28 个切片）
- 「7 天无理由」「价保」检索命中与来源
- 订单/物流/退款工具的用户隔离
- 跨用户订单查询拒绝
- Spring 上下文启动、Agent 与工具注册

真实模型调用需要有效 `AI_API_KEY`，不在默认测试中执行。

## 已知限制

- `X-User-Id` 仅为演示用，可被手动修改；生产必须替换为真实登录态/网关鉴权，工具层仍应保留归属校验。
- 业务数据为 H2 虚构数据，退款/取消等写操作未实现。
- `JsonFileAgentStateStore` 适合单实例；多副本需换 Redis/MySQL 状态存储，并处理同 session 串行化。
- 知识库为启动时全量入库，余弦索引在内存；文档更新后需重启，生产应接共享向量库和后台运营流程。
- ONNX Runtime 原生库会让 fat jar 明显变大（200MB+），Demo 可接受。
- 本地 bge 模型上下文为 512 token，切片已控制在 300 字左右；长文档需更细切片或换模型。
- 没有评测集；生产至少准备 100–300 条 golden question，持续看召回率、引用准确率和转人工准确率。

## 生产化清单

1. 鉴权：接入登录态/网关，禁用演示 `X-User-Id`。
2. 业务接口：替换 H2 Repository 为真实订单/物流/退款服务；工具层做权限、参数校验、超时和熔断。
3. 会话：Redis/MySQL 状态存储，多实例部署。
4. 知识库：后台运营、版本化、审核、生效时间；向量库换 pgvector/ES/Milvus。
5. Embedding：公开资料可用百炼 `text-embedding-v4`；敏感数据自建 `bge-m3` 等。
6. 可观测：全链路 Trace、Token 成本、工具成功率、转人工率、用户满意度。
7. 安全合规：PII 脱敏、内容审核、生成式 AI 服务备案/标识、审计留痕。
8. MCP：按上面的信号决定是否拆分工具服务。

## 目录结构

```text
src/main/java/com/example/cs/
├─ config/AgentScopeConfig.java      3 个 Agent、Toolkit、模型、状态存储
├─ config/AppProperties.java         配置绑定
├─ agent/CustomerServicePrompt.java  主管/订单专家/政策专家提示词
├─ agent/tools/                      订单、知识库、转人工、专家委派工具
├─ domain/                           订单/物流/退款/工单实体与 Repository
├─ rag/                              ONNX embedding、切分、余弦索引、入库
├─ service/TicketService.java        转人工工单
└─ web/                              ChatController、ChatStreamController、SSE 映射

src/main/resources/
├─ knowledge/*.md                    7 篇售后政策
├─ onnx/*.onnx, tokenizer.json       本地 embedding 模型
├─ schema.sql / data.sql             H2 演示数据
├─ application.yml
└─ static/index.html                 聊天页
```
