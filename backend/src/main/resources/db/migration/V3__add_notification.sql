-- Flyway Migration V3: 新增通知表

CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '通知标题',
    content     VARCHAR(1000) COMMENT '通知内容',
    ref_type    VARCHAR(30) COMMENT '关联类型: MAINTENANCE, ALARM',
    ref_id      BIGINT COMMENT '关联记录ID',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_notify_user (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
