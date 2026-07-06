# osSchoolWork
操作系统课程作业项目，包含后端（Spring Boot + MyBatis Plus + JWT）与前端（Vue 3 + Vite + Element Plus）。

---

## 1. 环境要求

- JDK 17/21
- Maven 3.9+
- Node.js 20+
- MySQL 8.0+

---

## 1.1 当前已实现功能

| 模块 | 功能 | 后端 | 前端 |
|------|------|:----:|:----:|
| 用户 | 注册（邮箱校验、密码 BCrypt 加密） | ✅ | ✅ |
| 用户 | 登录（JWT 签发） | ✅ | ✅ |
| 用户 | 路由守卫（未登录拦截） | ✅ | ✅ |
| 邮件 | 收件箱 / 已发送 | ✅ | ✅ |
| 邮件 | 抄送 CC | ✅ | ✅ |
| 邮件 | 全文搜索（主题/内容/发件人） | ✅ | ✅ |
| 邮件 | 已读 / 未读标记 | ✅ | ✅ |
| 邮件 | 移入垃圾箱 / 恢复 | ✅ | ✅ |
| 邮件 | 草稿箱（保存 / 编辑 / 发送） | ✅ | ✅ |
| 附件 | 上传（Multipart / 文件系统存储） | ✅ | ✅ |
| 附件 | 下载（blob 流式返回） | ✅ | ✅ |
| 推送 | WebSocket 实时新邮件通知 | ✅ | ✅ |

---

## 2. 数据库初始化

1. 创建数据库：`os_school`
2. 执行建表脚本：`backend/sql/schema.sql`
3. 按需修改 `backend/src/main/resources/application.yml` 中的数据库账号、密码与 JWT 配置。
4. （可选）导入测试数据：`mysql -u root -p os_school < backend/sql/test-data.sql`

> 详细字段对照表、常用排查 SQL 见 [`backend/sql/README.md`](backend/sql/README.md)

### E-R 图

```mermaid
erDiagram
    USER {
        bigint id PK "自增主键"
        varchar(255) email UK "邮箱，唯一"
        varchar(100) username "用户名"
        varchar(255) password "BCrypt 加密存储"
        datetime create_time "注册时间"
    }

    MAIL {
        bigint id PK "自增主键"
        bigint sender_id FK "发件人 → user.id"
        varchar(500) subject "邮件主题"
        longtext content "正文"
        varchar(20) status "DRAFT 草稿 / SENT 已发送"
        datetime send_time "发送时间"
    }

    RECEIVER {
        bigint id PK "自增主键"
        bigint mail_id FK "邮件 → mail.id"
        bigint receiver_id FK "收件人 → user.id"
        varchar(10) receiver_type "TO 收件 / CC 抄送"
        tinyint(1) is_read "0 未读 / 1 已读"
        tinyint(1) deleted "0 正常 / 1 已删除"
        varchar(20) folder "INBOX 收件箱 / TRASH 垃圾箱"
    }

    ATTACHMENT {
        bigint id PK "自增主键"
        bigint mail_id FK "邮件 → mail.id"
        varchar(500) file_name "原始文件名"
        varchar(1000) file_path "磁盘存储路径"
        bigint file_size "文件大小(字节)"
        datetime upload_time "上传时间"
    }

    USER ||--o{ MAIL : "发送（sender_id）"
    USER ||--o{ RECEIVER : "接收（receiver_id）"
    MAIL ||--o{ RECEIVER : "拥有收件关系"
    MAIL ||--o{ ATTACHMENT : "拥有附件"

    %% RECEIVER 表唯一约束: (mail_id, receiver_id, receiver_type)
    %% User ↔ Mail 为多对多关系，通过 receiver 表实现

---

## 3. 启动后端

在项目根目录执行：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8081`。

> 后端已新增邮件业务接口，前端 Dashboard 已接入。请确保 `backend/src/main/resources/application.yml` 中数据库和 JWT 配置已正确填写。

---

## 4. 启动前端

在项目根目录执行：

```bash
cd frontend
npm install
npm run dev
```

默认访问：`http://localhost:5173`

前端通过 Vite 代理访问后端 `/api`；如需更换后端地址，可在前端配置 `VITE_API_BASE_URL`。

> 默认 Dashboard 页面已经支持邮件系统，登录后即可在首页查看收件箱、发件箱、邮件详情，并进行发送操作。

