-- Flyway Migration V4: 船舶表新增电压/功率字段

ALTER TABLE ship
    ADD COLUMN rated_voltage DECIMAL(10,2) COMMENT '额定电压(V)',
    ADD COLUMN rated_power DECIMAL(10,2) COMMENT '额定功率(kW)';
