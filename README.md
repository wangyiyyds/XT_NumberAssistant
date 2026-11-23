# 先霆自媒体获客系统（Python 版）

原 Java（Spring Boot）后端整体迁移为 Python（FastAPI），`frontend/` 里的 Vue 3 前端保持不变，
API 路径、字段与响应结构逐一对齐，前端无需改动。

- 后端：FastAPI + Uvicorn，SQLAlchemy 2.0 async + asyncpg，Pydantic v2，OpenAI SDK 调用 DeepSeek
- 前端：Vue 3 + TypeScript + Vite + Pinia + Element Plus
- 数据库：PostgreSQL 16（Docker Compose，宿主端口 `5433`）
- 对话：SSE 流式输出（conversation / message / final / error 四类事件）
- 工具链：uv + Python 3.12

## 目录

```
app/                 FastAPI 后端源码（入口 app/main.py）
db/                  schema.sql（9 张表）、data.sql（演示数据）
scripts/             init_db.py（重建库）、dev.ps1（Windows 一键启动后端）
tests/               pytest 测试、mock DeepSeek、Playwright 前端回归脚本
frontend/            Vue 3 前端（端口 5173，/api 代理到 8080）
docker-compose.yml   PostgreSQL 16（端口 5433）
```

> Java 版后端源码已从 `fjx2` 移除，完整保留在 `fjx` 分支。

## 环境要求

| 工具 | 版本 |
| --- | --- |
| Docker + Docker Compose | 任意较新版本 |
| uv | 0.5+（会自动下载 Python 3.12，无需本机预装） |
| Node.js / npm | Node 20+，npm 10+ |

## 快速开始

### 1. 启动数据库

```bash
docker compose up -d
```

首次启动会自动执行 `db/schema.sql` 建表并执行 `db/data.sql` 导入演示数据。

- 端口：`localhost:5433`（容器内 5432）
- 库 / 用户 / 密码：`xianting_chat` / `xianting` / `xianting`
- 数据卷：`xianting_pgdata`，时区 `Asia/Shanghai`

确认健康状态：

```bash
docker compose ps
# STATUS 列应为 Up (healthy)
```

重置数据库（清空数据卷并重新导入种子数据）：

```bash
docker compose down -v && docker compose up -d
```

> `db/schema.sql` 自带 `DROP TABLE IF EXISTS`，也可在库已存在时单独重跑：
> `uv run python scripts/init_db.py`

### 2. 安装后端依赖

```bash
uv sync
```

### 3. 配置 `.env`

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

至少填写 `DEEPSEEK_API_KEY`。缺省时应用仍可正常启动，仅聊天接口会返回
`500 DEEPSEEK_API_KEY 未配置，无法调用大模型`。其余配置项保持默认即可：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DATABASE_URL` | `postgresql+asyncpg://xianting:xianting@localhost:5433/xianting_chat` | 数据库连接 |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek 密钥，聊天接口必填 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | 模型名 |
| `APP_JWT_SECRET` | 与 Java 版一致 | JWT 密钥，旧 token 继续有效 |
| `APP_BALANCE_START` | `100000` | 新用户初始积分 |
| `LOG_LEVEL` | `DEBUG` | 日志级别，请求日志随之输出 |

### 4. 启动后端（8080）

```bash
uv run uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```

Windows 也可用一键脚本（自动启动数据库、生成 `.env`、安装依赖并启动后端）：

```powershell
./scripts/dev.ps1
```

健康检查：

```bash
curl http://localhost:8080/health
# {"status":"ok"}
```

接口文档：

- http://localhost:8080/docs
- http://localhost:8080/redoc
- http://localhost:8080/swagger-ui.html （307 重定向到 `/docs`，兼容 Java 版访问习惯）

每个请求都会输出访问日志，例如：

```
2026-11-23 20:21:13 INFO app.request - POST /api/auth/login -> 200 cost=140.7ms
```

### 5. 启动前端（5173）

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 ，使用演示账号登录：

```
test@xianting.cn / 123456
```

前端通过 Vite 代理把 `/api`、`/health` 转发到 `http://localhost:8080`，无需额外配置跨域。

## 接口说明

所有业务接口返回统一结构：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

除标注「免鉴权」的接口外，均需请求头 `Authorization: Bearer <token>`（登录返回的 `token`）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册（免鉴权） |
| POST | `/api/auth/login` | 登录，返回 Bearer token（免鉴权） |
| POST | `/api/auth/refresh` | 刷新 token |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/config` | 站点配置（免鉴权） |
| GET | `/api/user` | 当前用户信息 |
| GET | `/api/balance` | 积分余额（`tokenCredits`） |
| GET | `/api/notifications` | 通知列表 |
| GET | `/api/roles/user` | 当前角色与权限 |
| GET | `/api/models` | 可用模型 |
| GET | `/api/search/enable` | 搜索功能开关 |
| GET | `/api/departments` | 部门列表 |
| GET | `/api/agents` | 智能体列表（OpenAI 风格分页，支持 `department`、`isPreset` 过滤） |
| GET | `/api/agents/my-access` | 可访问智能体范围 |
| GET | `/api/agents/chat/active` | 活跃会话（返回空列表） |
| GET | `/api/agents/{public_id}` | 智能体详情（含 `conversation_starters`） |
| POST | `/api/agents/chat/agents` | SSE 流式对话（见下） |
| GET | `/api/convos` | 会话列表（可选 `agent_id`） |
| GET | `/api/convos/{public_id}/messages` | 会话消息 |
| PATCH | `/api/convos/{public_id}` | 重命名会话 |
| DELETE | `/api/convos/{public_id}` | 删除会话 |
| GET | `/api/inspiration` | 灵感中心关键词与视频 |
| POST | `/api/inspiration/keywords` | 添加关键词 |
| POST | `/api/inspiration/keywords/{id}/refresh` | 手动刷新（扣 60 积分） |
| DELETE | `/api/inspiration/keywords/{id}` | 删除关键词 |

### SSE 对话

`POST /api/agents/chat/agents` 返回 `text/event-stream`，事件类型：

- `conversation`：会话创建 / 复用信息
- `message`：增量文本片段
- `final`：本轮结束（含完整消息与积分信息）
- `error`：异常信息

计费规则：每次对话扣 10 积分（余额不足仍继续本次对话）；灵感关键词手动刷新 60 积分/次，
余额不足返回 `402`。

## 测试

### 后端 pytest

先确保 `docker compose up -d` 的 PostgreSQL 已就绪，然后：

```bash
uv run pytest
```

- 测试自动创建并重置 `xianting_chat_test`（库名必须以 `_test` 结尾，否则拒绝启动），
  每次用例前执行 `db/schema.sql` + `db/data.sql`，与生产结构、种子数据一致；
  不会读写 `xianting_chat`。
- 覆盖：token / 密码 / DTO / 时间序列化单元测试；25 个 REST 端点的状态码与字段形状；
  无 token / 错误 token / 过期 token / 跨用户隔离；注册积分、对话扣 10、灵感刷新扣 60、
  余额不足 402、关键词上限 5；mock DeepSeek 的 SSE 事件名、顺序、字段与落库；
  以 Java DTO 定义生成的 golden snapshot 契约测试。
- 契约快照位于 `tests/golden/java_dto_shapes.json`（本机无 MySQL / Maven，按计划以 DTO 定义为准）。

### 前端全流程回归

前端代码零改动，通过真实 Vite dev server + 真实后端 + mock DeepSeek 跑浏览器流程
（登录 → 智能体中心 → 对话流式 → 会话管理 → 灵感中心 → 积分 → 退出）。

1. 初始化独立的 e2e 库：

   ```bash
   uv run python scripts/init_db.py --db xianting_chat_e2e --create
   ```

2. 启动 mock DeepSeek（端口 9001）：

   ```bash
   uv run python tests/support/mock_deepseek.py
   ```

3. 启动指向 e2e 库的后端（端口 8080）。PowerShell：

   ```powershell
   $env:DATABASE_URL="postgresql+asyncpg://xianting:xianting@localhost:5433/xianting_chat_e2e"
   $env:DEEPSEEK_BASE_URL="http://127.0.0.1:9001"
   $env:DEEPSEEK_API_KEY="test-key"
   uv run uvicorn app.main:app --host 127.0.0.1 --port 8080
   ```

   Bash：

   ```bash
   DATABASE_URL=postgresql+asyncpg://xianting:xianting@localhost:5433/xianting_chat_e2e \
   DEEPSEEK_BASE_URL=http://127.0.0.1:9001 DEEPSEEK_API_KEY=test-key \
   uv run uvicorn app.main:app --host 127.0.0.1 --port 8080
   ```

4. 启动前端（端口 5173）：

   ```bash
   cd frontend
   npm run dev
   ```

5. 运行回归脚本（使用本机已安装的 Chrome，不额外下载浏览器）：

   ```bash
   uv run --with playwright python tests/e2e/frontend_flow.py
   ```

   截图默认输出到系统临时目录 `xt-number-assistant-e2e/`（可用 `ARTIFACT_DIR` 覆盖）。

## 常见问题

- **端口被占用**：修改 `docker-compose.yml` 的 `5433` 映射，或后端改用其他端口，
  同时同步 `.env` 的 `DATABASE_URL` 与 `frontend/vite.config.ts` 中的代理地址。
- **数据库连接失败**：先 `docker compose ps` 确认容器 `healthy`；修改 schema 或数据后
  用 `docker compose down -v && docker compose up -d` 重置。
- **聊天无回复**：检查 `.env` 中 `DEEPSEEK_API_KEY` 是否填写，以及后端日志中的错误信息。