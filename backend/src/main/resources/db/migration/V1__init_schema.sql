-- Flyway Migration V1: 岸电码头运维系统初始表结构
-- 兼容 MySQL 5.7+ / 8.0+

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    status INT NOT NULL DEFAULT 1,
    avatar LONGTEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code VARCHAR(50) NOT NULL UNIQUE,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    location VARCHAR(200),
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    rated_voltage DECIMAL(10,2),
    rated_current DECIMAL(10,2),
    rated_power DECIMAL(10,2),
    manufacturer VARCHAR(100),
    install_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    voltage DECIMAL(10,2),
    current_val DECIMAL(10,2),
    power DECIMAL(10,2),
    temperature DECIMAL(6,2),
    humidity DECIMAL(6,2),
    power_factor DECIMAL(4,2),
    frequency DECIMAL(6,2),
    energy_consumption DECIMAL(12,2),
    energy_cost DECIMAL(10,2),
    collect_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dd_device_time (device_id, collect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alarm (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    alarm_type VARCHAR(30) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    alarm_content VARCHAR(500) NOT NULL,
    alarm_value VARCHAR(50),
    threshold_value VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handler_id BIGINT,
    handle_time TIMESTAMP NULL,
    handle_remark VARCHAR(500),
    alarm_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alarm_device (device_id, alarm_time),
    INDEX idx_alarm_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    operation_content VARCHAR(500),
    operation_result VARCHAR(10),
    ip_address VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS energy_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    stat_type VARCHAR(10) NOT NULL,
    total_energy DECIMAL(12,2),
    peak_power DECIMAL(10,2),
    avg_power DECIMAL(10,2),
    running_hours DECIMAL(6,2),
    energy_cost DECIMAL(10,2) DEFAULT 0 COMMENT '能耗费用(元)',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS energy_consumption (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    total_energy DECIMAL(12,2),
    peak_power DECIMAL(10,2),
    avg_power DECIMAL(10,2),
    running_hours DECIMAL(6,2),
    energy_cost DECIMAL(10,2),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ec_device_date (device_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT,
    task_type VARCHAR(20) NOT NULL,
    task_title VARCHAR(200) NOT NULL,
    task_content LONGTEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assignee_id BIGINT,
    plan_start_time TIMESTAMP NULL,
    plan_end_time TIMESTAMP NULL,
    actual_start_time TIMESTAMP NULL,
    actual_end_time TIMESTAMP NULL,
    completion_remark LONGTEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    ship_id BIGINT COMMENT '关联船舶ID',
    reservation_no VARCHAR(32) NOT NULL UNIQUE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    estimated_cost DECIMAL(10,2),
    actual_cost DECIMAL(10,2),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_res_user (user_id),
    INDEX idx_res_device (device_id),
    INDEX idx_res_ship (ship_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS usage_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    total_energy DECIMAL(12,2),
    total_cost DECIMAL(10,2),
    rating INT,
    comment VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_usage_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rating_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_wallet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_recharge DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_spent DECIMAL(10,2) NOT NULL DEFAULT 0,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recharge_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recharge_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ship (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    ship_name VARCHAR(100) NOT NULL COMMENT '船名',
    ship_type VARCHAR(30) NOT NULL DEFAULT 'CARGO' COMMENT '船舶类型',
    mmsi VARCHAR(20) COMMENT 'MMSI码',
    imo VARCHAR(20) COMMENT 'IMO编号',
    nationality VARCHAR(50) COMMENT '船籍',
    tonnage DECIMAL(10,2) COMMENT '总吨位(GT)',
    length DECIMAL(10,2) COMMENT '船长(米)',
    width DECIMAL(10,2) COMMENT '船宽(米)',
    draft DECIMAL(10,2) COMMENT '吃水深度(米)',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ship_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    config_name VARCHAR(200) NOT NULL,
    config_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    remark VARCHAR(500),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
