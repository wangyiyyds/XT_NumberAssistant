# 珠峰AI超级系统（克隆版）

根据对 https://chat.ezhufeng.cn 的被动观察（API 契约 + 页面结构）重建的全栈演示项目。

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router
- 后端：Spring Boot 3 + MyBatis Plus，默认 H2 文件库（MySQL 兼容模式），可切换 MySQL
- 对话：SSE 流式输出（mock AI 回复）

## 目录

```
backend/           客户端 Spring Boot 后端（端口 8080）
frontend/          客户端 Vue3 + TS 前端（端口 5173，/api 代理到 8080）
backend-manage/    管理端 Spring Boot 后端（端口 8081，与客户端共用 MySQL 库 video_assistant）
frontend-manage/   管理端 Vue3 + TS 前端（端口 5174，/api 代理到 8081）
```

## 启动

### 后端

要求 JDK 17+、Maven 3.6+。

```bash
cd backend
mvn package -DskipTests
java -jar target/chat-backend-1.0.0.jar
```

首次启动自动执行 `schema.sql` 建表，并由 DataSeeder 写入演示数据：
测试用户、10 万积分、7 个部门共 22 个预置智能体、灵感关键词「Aigc培训」+ 10 条示例视频。

数据文件在 `backend/data/`，删除该目录后重启即恢复初始演示数据。

健康检查：`GET http://localhost:8080/health` → `{"status":"ok"}`

### 配置文案提取助手

在“文案提取助手”对话中直接发送公开视频链接，系统采用两段式处理：先使用本地 FunASR 提取口播文案（不做画面字幕 OCR），再调用 DeepSeek 对文字进行保真校对（修正明显错字、标点和分段，不扩写内容）。本地转写默认接口为 `http://127.0.0.1:8091/transcribe`。

首次安装（Python 3.11，FFmpeg 需在 PATH 中）：

```powershell
powershell -ExecutionPolicy Bypass -File transcription/install.ps1
```

每次使用前在项目根目录启动：

```powershell
powershell -ExecutionPolicy Bypass -File transcription/start.ps1
```

首次启动会下载中文识别、语音分段、标点模型到 `transcription/models/`，后续使用本地缓存。健康检查：`http://127.0.0.1:8091/health`。
默认 CPU 运行，同一时间处理一个视频，视频限制 20 分钟、100MB。视频解析使用 yt-dlp；受平台支持情况和登录限制影响，并非所有分享链接均可下载。临时音视频在请求结束后自动清理。

抖音有时会拒绝没有浏览器验证信息的下载请求。新版 Edge/Chrome 会使用 Windows 的 App-Bound 加密，程序可能无法直接读取其中的 Cookie（会报 `Failed to decrypt with DPAPI`）。遇到这种情况，请在已能正常打开抖音的浏览器中导出 **Netscape 格式**的 `cookies.txt`，保存到仅自己可访问的位置，然后在启动前执行：

```powershell
$env:YTDLP_COOKIES_FILE = 'D:\\安全位置\\douyin-cookies.txt'
powershell -ExecutionPolicy Bypass -File transcription/start.ps1
```

该文件等同于已登录会话凭证，切勿上传、发送给他人或提交到 Git。服务只会在本机将它交给 yt-dlp，不会发送给本项目后端。若使用 Firefox，也可以改用 `$env:YTDLP_COOKIES_FROM_BROWSER = 'firefox'` 后再启动服务。

还需配置 `DEEPSEEK_API_KEY`，供第二阶段的文本校对使用；该阶段只会发送本地 ASR 生成的文本，不会发送音视频文件。

如果改为远程转写服务，可覆盖环境变量：

```bash
VIDEO_TRANSCRIPTION_URL=https://your-transcription-service.example.com/transcribe
VIDEO_TRANSCRIPTION_API_KEY=your-api-key  # 若服务不需要鉴权，可省略
```

服务应接收 `POST` JSON：`{"url":"https://..."}`，并返回 `{"text":"提取出的文案"}`。也兼容 `transcript`、`copywriting`、`content` 字段（可置于 `data` 内）。

### 前端

要求 Node 18+。

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 ，演示账号：

```
test@zhufeng.cn / 123456
```

### 管理端

管理端与客户端共用同一个 MySQL 库（`video_assistant`），负责：套餐管理（套餐等级决定用户可用哪些部门/功能）、部门管理、用户管理（分配套餐/禁用）、智能体管理与提示词版本管理（草稿→发布→自动归档旧版→可回滚；发布后客户端对话立即使用新提示词，客户端零改动）。

```bash
# 后端（端口 8081）
cd backend-manage
mvn package -DskipTests
java -jar target/manage-backend-1.0.0.jar

# 前端（端口 5174）
cd frontend-manage
npm install
npm run dev
```

首次启动自动执行 `schema-manage.sql`（幂等建 `t_admin`/`t_plan`/`t_plan_department`），给 `t_user` 幂等补 `plan_id` 列，并种入超管账号与 3 个套餐（基础版/专业版/旗舰版，默认按部门排序绑定 2/5/7 个部门）：

```
http://localhost:5174    admin / admin123456
```

注意：管理端 `app.jwt.secret` 与客户端不同（两端 token 载荷分别是 adminId/userId，共用密钥会导致身份串号）；建议先启动一次客户端 backend 让部门/智能体种子就绪，再启动管理端。

## 切换 MySQL

编辑 `backend/src/main/resources/application.yml`，把 H2 数据源注释掉、启用 MySQL 段（文件内有注释示例），并按需调整 `schema.sql` 的方言，重新打包即可。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/auth/register、/api/auth/login | 注册 / 登录，返回 Bearer token |
| GET | /api/departments | 部门列表 |
| GET | /api/agents | 智能体列表（OpenAI 风格分页结构） |
| GET | /api/agents/{id} | 智能体详情（含 conversation_starters） |
| GET | /api/balance | 积分余额（tokenCredits） |
| GET | /api/convos、/api/convos/{id}/messages | 会话列表 / 消息 |
| PATCH | /api/convos/{id} | 重命名会话 |
| DELETE | /api/convos/{id} | 删除会话 |
| POST | /api/agents/chat/agents | SSE 流式对话（conversation / message / final / error 事件） |
| GET/POST/DELETE | /api/inspiration... | 灵感中心关键词与视频（手动刷新 60 积分/次） |

计费规则：每次对话 10 积分，灵感关键词手动刷新 60 积分/次。
