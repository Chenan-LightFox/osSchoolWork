-- =============================================
-- os_school 数据库初始化脚本
-- 字符集: utf8mb4  存储引擎: InnoDB
-- 执行方式: mysql -u root -p < schema.sql
-- =============================================

CREATE DATABASE IF NOT EXISTS os_school
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE os_school;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS receiver;
DROP TABLE IF EXISTS mail;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  email       VARCHAR(255) NOT NULL,
  username    VARCHAR(100) NOT NULL,
  password    VARCHAR(255) NOT NULL,
  create_time DATETIME     NOT NULL,
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 2. 邮件表
-- status: DRAFT(草稿) / SENT(已发送)
-- =============================================
CREATE TABLE mail (
  id        BIGINT       PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT       NOT NULL,
  subject   VARCHAR(500) NOT NULL DEFAULT '',
  content   LONGTEXT,
  status    VARCHAR(20)  NOT NULL DEFAULT 'SENT',
  send_time DATETIME     NOT NULL,
  INDEX idx_mail_sender (sender_id),
  INDEX idx_mail_send_time (send_time),
  CONSTRAINT fk_mail_sender FOREIGN KEY (sender_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 3. 收件关系表（支持多收件人、抄送、独立状态）
-- receiver_type: TO(收件人) / CC(抄送)
-- folder:        INBOX(收件箱) / TRASH(垃圾箱)
-- =============================================
CREATE TABLE receiver (
  id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
  mail_id       BIGINT      NOT NULL,
  receiver_id   BIGINT      NOT NULL,
  receiver_type VARCHAR(10) NOT NULL DEFAULT 'TO',
  is_read       TINYINT(1)  NOT NULL DEFAULT 0,
  deleted       TINYINT(1)  NOT NULL DEFAULT 0,
  folder        VARCHAR(20) NOT NULL DEFAULT 'INBOX',
  UNIQUE KEY uk_mail_receiver (mail_id, receiver_id, receiver_type),
  INDEX idx_receiver_user_folder (receiver_id, folder),
  CONSTRAINT fk_receiver_mail FOREIGN KEY (mail_id) REFERENCES mail(id),
  CONSTRAINT fk_receiver_user FOREIGN KEY (receiver_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 4. 附件表（正文附件分离存储）
-- =============================================
CREATE TABLE attachment (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  mail_id     BIGINT       NOT NULL,
  file_name   VARCHAR(500) NOT NULL,
  file_path   VARCHAR(1000) NOT NULL,
  file_size   BIGINT       NOT NULL DEFAULT 0,
  upload_time DATETIME     NOT NULL,
  INDEX idx_attachment_mail (mail_id),
  CONSTRAINT fk_attachment_mail FOREIGN KEY (mail_id) REFERENCES mail(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
