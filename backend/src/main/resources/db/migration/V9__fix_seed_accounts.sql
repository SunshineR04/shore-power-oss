-- Flyway Migration V9: 种子数据修复
-- 背景：
--   V2 中 admin/user1 使用了同一个占位 BCrypt 散列（并非 README 声称的密码 123456），
--   且缺少 OPERATOR 角色账号，导致运维端演示无法登录。
-- 本迁移：为演示账号设置 README 承诺的统一密码 123456，并补充运维账号 op1。

-- 更新演示账号密码（BCrypt("123456")，强度 12）
UPDATE sys_user SET password = '$2a$12$yhme6XOW8Q5BrhebvGQcFuJ5BlBVlF0hiMs49DuKhKXK0aCPTfZaC', token_version = token_version + 1
WHERE username IN ('admin', 'user1') AND status = 1;

-- 补充 OPERATOR 演示账号（密码同样为 123456）
INSERT IGNORE INTO sys_user (username, password, real_name, role, status, token_version)
SELECT 'op1', '$2a$12$yhme6XOW8Q5BrhebvGQcFuJ5BlBVlF0hiMs49DuKhKXK0aCPTfZaC', '运维演示员', 'OPERATOR', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'op1');

-- 历史数据兼容：把空字符串手机号/邮箱归一为 NULL（配合 V7 唯一索引与注册逻辑）
UPDATE sys_user SET phone = NULL WHERE phone = '';
UPDATE sys_user SET email = NULL WHERE email = '';
