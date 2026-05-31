-- =============================================
-- os_school 测试数据脚本
-- 前提：已执行 schema.sql 建表
-- 所有测试用户密码均为: 123456
-- =============================================

USE os_school;

-- =============================================
-- 1. 测试用户（3 个，密码均为 123456 的 BCrypt 哈希）
-- =============================================
INSERT INTO user (id, email, username, password, create_time) VALUES
(1, 'alice@example.com',  'Alice',  '$2b$10$aCsWFnK090TXfLGutCeccOHv2fTREZMm.tsj.LHwca4F8qcxF3YL.', '2024-12-01 09:00:00'),
(2, 'bob@example.com',    'Bob',    '$2b$10$aCsWFnK090TXfLGutCeccOHv2fTREZMm.tsj.LHwca4F8qcxF3YL.', '2024-12-02 10:00:00'),
(3, 'carol@example.com',  'Carol',  '$2b$10$aCsWFnK090TXfLGutCeccOHv2fTREZMm.tsj.LHwca4F8qcxF3YL.', '2024-12-03 11:00:00');

-- =============================================
-- 2. 测试邮件（6 封，覆盖一对一、一对多、TO+CC、草稿）
-- =============================================
INSERT INTO mail (id, sender_id, subject, content, status, send_time) VALUES
-- 邮件1：Alice → Bob（普通一对一）
(1, 1, 'Hello Bob',
 'This is a test email from Alice to Bob. Welcome to osSchoolWork!',
 'SENT', '2024-12-10 09:00:00'),

-- 邮件2：Bob → Alice（回复）
(2, 2, 'Re: Hello Bob',
 'Hi Alice, thanks for the welcome! The system looks great.',
 'SENT', '2024-12-10 09:30:00'),

-- 邮件3：Alice → Bob + Carol（一对多，TO 两人）
(3, 1, 'Team Meeting Notice',
 'Dear team,\n\nWe will have a meeting at 3pm tomorrow.\nPlease prepare your weekly reports.\n\nBest,\nAlice',
 'SENT', '2024-12-11 14:00:00'),

-- 邮件4：Carol → Alice + Bob（TO Alice, CC Bob）
(4, 3, 'Project Update',
 'Hi everyone,\n\nThe backend API is almost done.\nPlease review the code when you get a chance.\n\n--Carol',
 'SENT', '2024-12-12 10:00:00'),

-- 邮件5：Bob → Carol（普通一对一，含已读标记）
(5, 2, 'Lunch?',
 'Hey Carol, want to grab lunch today?',
 'SENT', '2024-12-12 12:00:00'),

-- 邮件6：Alice 保存的草稿
(6, 1, 'Draft: Weekly Report',
 'This is a draft email, not yet sent.',
 'DRAFT', '2024-12-13 08:00:00');

-- =============================================
-- 3. 收件关系（覆盖 TO/CC、已读/未读）
-- =============================================
INSERT INTO receiver (mail_id, receiver_id, receiver_type, is_read, deleted, folder) VALUES
-- 邮件1：Bob 收到，已读
(1, 2, 'TO', 1, 0, 'INBOX'),

-- 邮件2：Alice 收到，已读
(2, 1, 'TO', 1, 0, 'INBOX'),

-- 邮件3：Bob 收到，未读；Carol 收到，未读
(3, 2, 'TO', 0, 0, 'INBOX'),
(3, 3, 'TO', 0, 0, 'INBOX'),

-- 邮件4：Alice(TO) 已读，Bob(CC) 未读
(4, 1, 'TO', 1, 0, 'INBOX'),
(4, 2, 'CC', 0, 0, 'INBOX'),

-- 邮件5：Carol 收到，未读
(5, 3, 'TO', 0, 0, 'INBOX'),

-- 邮件5b：Bob 自己也有一条（发件箱查询用不到 receiver，但保留完整性）
(5, 2, 'TO', 0, 1, 'TRASH');

-- =============================================
-- 4. 附件数据（2 条，关联邮件1和邮件4）
-- =============================================
INSERT INTO attachment (id, mail_id, file_name, file_path, file_size, upload_time) VALUES
(1, 1, 'meeting-notes.pdf', 'uploads/meeting-notes.pdf', 245760, '2024-12-10 09:01:00'),
(2, 4, 'project-api-doc.docx', 'uploads/project-api-doc.docx', 512000, '2024-12-12 10:05:00');
