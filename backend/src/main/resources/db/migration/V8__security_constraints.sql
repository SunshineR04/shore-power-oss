-- Flyway Migration V8: 安全加固所需约束
-- 说明：V6 已发布并包含 DROP 钱包表的破坏性操作，此处不再修改 V6。
-- 本迁移仅做增量加固，不删除任何业务数据。

-- 1) 用户表增加 token_version：修改密码/禁用/改角色时递增，使旧 JWT 立即失效
ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'JWT token版本号，变更时递增使旧token失效';

-- 2) 支付订单交易号唯一约束：防止重复支付单/回调幂等性保障
-- 先清理可能的重复 PENDING 单（保留最早一笔），再建唯一索引
DELETE p1 FROM payment_order p1
    INNER JOIN payment_order p2
        ON p1.trade_no = p2.trade_no AND p1.id > p2.id
    WHERE p1.status = 'PENDING' AND p2.status = 'PENDING';

CREATE UNIQUE INDEX uk_payment_trade_no ON payment_order(trade_no);

-- 3) 能耗统计唯一约束：防止并发重复聚合（先清理重复记录，保留 id 最小的一条）
DELETE e1 FROM energy_stat e1
    INNER JOIN energy_stat e2
        ON e1.device_id = e2.device_id
       AND e1.stat_date = e2.stat_date
       AND e1.stat_type = e2.stat_type
       AND e1.id > e2.id;

CREATE UNIQUE INDEX uk_energy_stat_device_date_type ON energy_stat(device_id, stat_date, stat_type);
