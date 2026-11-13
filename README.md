# 珠峰AI超级系统（克隆版）

根据对 https://chat.ezhufeng.cn 的被动观察（API 契约 + 页面结构）重建的全栈演示项目。

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router
- 后端：Spring Boot 3 + MyBatis Plus，默认 H2 文件库（MySQL 兼容模式），可切换 MySQL
- 对话：SSE 流式输出（mock AI 回复）

## 目录

```
backend/    Spring Boot 后端（端口 8080）
frontend/   Vue3 + TS 前端（端口 5173，/api 代理到 8080）
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
