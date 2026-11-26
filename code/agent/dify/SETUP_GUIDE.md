# Dify 知识库与 Chatflow Agent 配置指南

适用版本：Dify `1.17.1`，控制台地址 `http://localhost`。

## 1. 配置 DeepSeek 模型供应商

1. 打开 `http://localhost` 并登录 Dify。
2. 点击右上角头像，进入 **设置 -> 模型供应商**。
3. 找到 `DeepSeek`，填入你的 DeepSeek API Key。
4. 保存后确认 `deepseek-chat` 模型状态为可用。

## 2. 创建知识库

1. 顶部进入 **知识库**。
2. 点击 **创建知识库**。
3. 名称填写：`土豆宠物之家知识库`。
4. 索引方式选择：
   - 当前已配置 Embedding 模型时选 **高质量**。
   - 未配置 Embedding 模型时先选 **经济型**，便于快速跑通。
5. 上传文件：
   - `rag/knowledge/01_brand.md`
   - `rag/knowledge/02_stores.md`
   - `rag/knowledge/03_services.md`
   - `rag/knowledge/04_membership_recharge.md`
   - `rag/knowledge/05_pet_care.md`
   - `rag/knowledge/06_faq.md`
   - `rag/knowledge/07_emergency.md`
   - `rag/knowledge/土豆宠物之家常见问答.xlsx`
6. 高频问答增强语料（可选，当前线上未导入）：
   - `rag/data/qa_pairs.json`
   - 如 Dify 不支持直接上传 JSON，可打开文件后复制内容，或使用自动导入脚本逐条创建文档。
7. 等待文档索引完成，状态全部变为 **已完成**。

## 3. 创建 Chatflow Agent

1. 顶部进入 **创建应用**，应用类型选择 **Chatflow**。
2. 应用名称：`AI宠物专家`。
3. 编排画布按以下顺序连接节点：

```text
开始 -> 知识检索 -> LLM -> 直接回复
```

### 节点参数

| 节点 | 关键配置 |
| --- | --- |
| 开始 | 输入变量：`sys.query`；可选加入 `customer_name` |
| 知识检索 | 选择“土豆宠物之家知识库”，TopK 设为 4；该知识库应包含 7 个 Markdown 文档和常见问答.xlsx |
| LLM | 模型选择 `deepseek-chat`；系统提示词填写 `prompts/system_prompt.md` 的内容 |
| 直接回复 | 输出 LLM 结果 |

> 重要：知识检索节点不要只挂载“常见问答.xlsx”。只挂 FAQ 时，主知识库中的服务项目、
> 应急处理和品牌口号等内容无法被检索到，会导致“门店可以上门喂猫吗”等正确答案缺失。

### LLM 上下文组装

在 LLM 节点的 Prompt 中建议写成：

```text
请严格依据以下知识库资料回答用户问题。

知识库资料：
{{#context#}}

用户问题：
{{#sys.query#}}
```

## 4. 发布与获取 API Key

1. 点击右上角 **发布**。
2. 发布后进入 **访问 API**，创建 API 密钥。
3. 将 API 地址和密钥填入后端配置，后续由 Spring Boot 代理调用。

## 5. 流式接入

后端调用 Dify Chatflow 接口时使用：

```http
POST http://localhost/v1/chat-messages
Authorization: Bearer <API Key>
Content-Type: application/json

{
  "inputs": {},
  "query": "用户问题",
  "response_mode": "streaming",
  "conversation_id": "",
  "user": "当前用户标识"
}
```

响应为 SSE，后端读取 `answer` 事件后通过 `SseEmitter` 转发给小程序和网页端。
