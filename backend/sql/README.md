# 数据库脚本文档

## 执行顺序

```bash
# 1. 建库 + 建表
mysql -u root -p < backend/sql/schema.sql

# 2. 导入测试数据（可选）
mysql -u root -p < backend/sql/test-data.sql
```

---

## 字段对照表（数据库 ↔ Java Entity）

### user 表

| 数据库字段 | Java 属性 | 类型 | 说明 |
|-----------|-----------|------|------|
| id | id | Long | 自增主键 |
| email | email | String | 邮箱，唯一 |
| username | username | String | 用户名 |
| password | password | String | BCrypt 加密 |
| create_time | createTime | LocalDateTime | 注册时间 |

### mail 表

| 数据库字段 | Java 属性 | 类型 | 说明 |
|-----------|-----------|------|------|
| id | id | Long | 自增主键 |
| sender_id | senderId | Long | 发件人 → user.id |
| subject | subject | String | 主题 |
| content | content | String | 正文 (LONGTEXT) |
| status | status | String | DRAFT / SENT |
| send_time | sendTime | LocalDateTime | 发送时间 |

### receiver 表

| 数据库字段 | Java 属性 | 类型 | 说明 |
|-----------|-----------|------|------|
| id | id | Long | 自增主键 |
| mail_id | mailId | Long | 邮件 → mail.id |
| receiver_id | receiverId | Long | 收件人 → user.id |
| receiver_type | receiverType | String | TO / CC |
| is_read | isRead | Integer | 0 未读 / 1 已读 |
| deleted | deleted | Integer | 0 正常 / 1 已删除 |
| folder | folder | String | INBOX / TRASH |

### attachment 表

| 数据库字段 | Java 属性 | 类型 | 说明 |
|-----------|-----------|------|------|
| id | id | Long | 自增主键 |
| mail_id | mailId | Long | 邮件 → mail.id |
| file_name | fileName | String | 原始文件名 |
| file_path | filePath | String | 磁盘存储路径 |
| file_size | fileSize | Long | 字节数 |
| upload_time | uploadTime | LocalDateTime | 上传时间 |

> MyBatis-Plus 配置 `map-underscore-to-camel-case: true` 自动完成 `sender_id` → `senderId` 转换。

---

## 常用排查 SQL

### 查某用户的收件箱

```sql
SELECT m.id, m.subject, u.username AS sender, r.is_read, r.receiver_type
FROM receiver r
JOIN mail m ON m.id = r.mail_id
JOIN user u ON u.id = m.sender_id
WHERE r.receiver_id = 1       -- Alice
  AND r.deleted = 0
  AND r.folder = 'INBOX'
ORDER BY m.send_time DESC;
```

### 查某用户的发件箱

```sql
SELECT id, subject, status, send_time
FROM mail
WHERE sender_id = 1            -- Alice
  AND status = 'SENT'
ORDER BY send_time DESC;
```

### 查某邮件的所有收件人

```sql
SELECT u.username, u.email, r.receiver_type, r.is_read
FROM receiver r
JOIN user u ON u.id = r.receiver_id
WHERE r.mail_id = 3;
```

### 查某邮件是否有附件

```sql
SELECT id, file_name, file_size
FROM attachment
WHERE mail_id = 1;
```

### 验证外键数据完整性

```sql
-- receiver 中引用了不存在的 mail_id
SELECT r.id, r.mail_id
FROM receiver r
LEFT JOIN mail m ON m.id = r.mail_id
WHERE m.id IS NULL;

-- receiver 中引用了不存在的 receiver_id
SELECT r.id, r.receiver_id
FROM receiver r
LEFT JOIN user u ON u.id = r.receiver_id
WHERE u.id IS NULL;

-- attachment 中引用了不存在的 mail_id
SELECT a.id, a.mail_id
FROM attachment a
LEFT JOIN mail m ON m.id = a.mail_id
WHERE m.id IS NULL;
```

### 统计各用户邮件数量

```sql
SELECT u.username,
  (SELECT COUNT(*) FROM mail WHERE sender_id = u.id AND status = 'SENT') AS sent_count,
  (SELECT COUNT(*) FROM receiver WHERE receiver_id = u.id AND deleted = 0 AND folder = 'INBOX') AS inbox_count,
  (SELECT COUNT(*) FROM receiver WHERE receiver_id = u.id AND is_read = 0 AND deleted = 0 AND folder = 'INBOX') AS unread_count
FROM user u;
```

---

## 重建数据库

```bash
# 方式一：重新执行 schema.sql（含 DROP TABLE）
mysql -u root -p < backend/sql/schema.sql

# 方式二：手动
mysql -u root -p -e "DROP DATABASE IF EXISTS os_school; CREATE DATABASE os_school DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p os_school < backend/sql/schema.sql
mysql -u root -p os_school < backend/sql/test-data.sql
```
