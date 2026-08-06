-- Flyway Migration V7: 添加唯一索引防并发注册
-- sys_user 表的 username 已在 V1 有 UNIQUE 约束
-- phone 和 email 缺少唯一索引，并发注册可能产生重复
-- Flyway 保证每个迁移脚本只执行一次，无需 IF NOT EXISTS
CREATE UNIQUE INDEX uk_user_phone ON sys_user(phone);
CREATE UNIQUE INDEX uk_user_email ON sys_user(email);
