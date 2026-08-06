-- Flyway Migration V6: 新增支付订单表，移除钱包相关表

DROP TABLE IF EXISTS recharge_record;
DROP TABLE IF EXISTS user_wallet;

CREATE TABLE IF NOT EXISTS payment_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id  BIGINT NOT NULL COMMENT '关联预约ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    amount          DECIMAL(10,2) NOT NULL COMMENT '支付金额(元)',
    method          VARCHAR(20) COMMENT '支付方式: ALIPAY/WECHAT',
    qr_code_url     VARCHAR(500) COMMENT '支付二维码URL',
    trade_no        VARCHAR(100) COMMENT '第三方交易号',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/FAILED',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time        TIMESTAMP NULL COMMENT '支付时间',
    INDEX idx_payment_reservation (reservation_id),
    INDEX idx_payment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';
