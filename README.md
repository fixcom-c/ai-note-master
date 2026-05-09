# 随手记 AI 行动管家

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen)
![Vue.js](https://img.shields.io/badge/Vue-3.4-42b883)
![TypeScript](https://img.shields.io/badge/TypeScript-5.4-3178c6)
![MySQL](https://img.shields.io/badge/MySQL-8.0-f29111)

一个面向个人效率场景的 AI 工作台：把随手记录自动转成任务、知识标签和日报，适合日常工作记录、个人待办整理和轻量复盘。

## 功能概览

- 快速记录：输入文本后保存为笔记，支持 `Ctrl + Enter` 快速提交。
- AI 分析：自动提取任务、摘要和知识标签。
- 任务管理：列表、卡片、日历三种视图，支持创建、编辑、完成、删除。
- 知识库：支持新建、编辑、搜索、标签过滤和导出。
- 智能日报：支持生成当日日报，并按日期查看历史日报内容。
- 用户认证：基于 JWT 的注册登录。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.3
- Spring Data JPA
- Spring Security + JWT
- MySQL 8
- Redis 7

### 前端

- Vue 3 + TypeScript
- Vite 5
- Pinia
- Vue Router
- Element Plus
- ECharts

## 项目结构

```text
ai-note-master/
├── backend/               # Spring Boot 后端
├── frontend/              # Vue 3 前端
├── docker-compose.yml     # 本地联调容器编排
└── README.md
```

## 运行端口

- 前端开发环境：`http://localhost:3000`
- 后端服务：`http://localhost:8083`
- Swagger 文档：`http://localhost:8083/swagger-ui.html`

## 本地启动

### 1. 准备依赖

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Redis 7+

### 2. 启动 MySQL 和 Redis

如果本机没有现成服务，建议直接使用 Docker：

```bash
docker compose up -d mysql redis
```

默认容器配置：

- MySQL 数据库：`ai_note`
- MySQL 用户：`root`
- MySQL 密码：`root123`
- Redis 端口：`6379`

### 3. 配置后端

后端配置文件路径：

`backend/src/main/resources/application.yml`

默认端口已经改为：

```yaml
server:
  port: ${SERVER_PORT:8083}
```

如果你使用本地服务而不是 Docker，请同步修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/ai_note?useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
    username: root
    password: your_password

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_password
      database: 2
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发代理已指向 `http://localhost:8083`，无需再额外改端口。

## Docker 一键启动

整个项目可以通过以下命令启动：

```bash
docker compose up --build
```

已修正的容器端口：

- 后端映射 `8083:8083`
- 前端映射 `3000:3000`

## AI 配置

项目默认优先走 `MiniMaxAiServiceImpl`，但当下列任一条件不满足时，会自动退回 Mock 模式：

- `MINIMAX_API_KEY` 未配置
- `MINIMAX_GROUP_ID` 未配置
- `MINIMAX_MOCK_MODE=true`

推荐本地开发时先使用 Mock 模式：

```bash
export MINIMAX_MOCK_MODE=true
```

如需接入真实模型：

```bash
export MINIMAX_API_KEY=your_key
export MINIMAX_GROUP_ID=your_group_id
export MINIMAX_MOCK_MODE=false
```

## 测试与构建

### 前端

```bash
cd frontend
npm run build
```

### 后端

```bash
cd backend
mvn test
```

当前仓库已补充基础单元测试，覆盖：

- 日报按日期生成
- 任务创建默认值和标题清洗

## 默认使用方式

项目没有预置账号，首次进入请直接注册。

建议体验路径：

1. 注册并登录。
2. 在“快速记录”中输入一段会议或工作内容。
3. 点击“一键 AI 分析”。
4. 到“任务列表”和“知识库”查看自动生成结果。
5. 在“日报”页面生成当天总结。

## 已知说明

- 日报内容目前是按日期实时生成，不会单独落库保存。
- 历史日报入口是基于任务日期生成摘要，不是独立存档文件。
- AI 标签和任务提取质量在 Mock 模式下仅用于演示。

## 本次已修复的问题

- 修复前端 TypeScript 构建失败。
- 补齐知识库编辑接口与前端保存链路。
- 修复日报按日期查看时前后端接口不一致的问题。
- 修复 Docker / README / 前端代理端口说明不一致的问题。
- 修复任务与笔记的部分数据联动缺失。
- 优化登录页、主布局和工作台视觉层次。
