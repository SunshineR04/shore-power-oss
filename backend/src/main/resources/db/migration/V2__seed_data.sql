-- Flyway Migration V2: 种子数据

INSERT IGNORE INTO sys_user (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2b$12$gdPRMGastSTrm6nqTuPAKuV4kdNXZenTNGnA.fvM2KqHq07JThRgy', '系统管理员', 'ADMIN', 1),
(2, 'user1', '$2b$12$gdPRMGastSTrm6nqTuPAKuV4kdNXZenTNGnA.fvM2KqHq07JThRgy', '张运维', 'USER', 1);

INSERT IGNORE INTO device (id, device_code, device_name, device_type, location, rated_voltage, rated_current, rated_power, status) VALUES
(1, 'SP-YT-001', '1号游艇桩', 'SMALL_YACHT', '游艇码头A区泊位1', 380.00, 80.00, 50.00, 'ONLINE'),
(2, 'SP-YT-002', '2号游艇桩', 'SMALL_YACHT', '游艇码头A区泊位2', 380.00, 80.00, 50.00, 'ONLINE'),
(3, 'SP-NH-001', '1号内河货船桩', 'INLAND_CARGO', '长江码头B区泊位1', 380.00, 200.00, 130.00, 'ONLINE'),
(4, 'SP-NH-002', '2号内河货船桩', 'INLAND_CARGO', '长江码头B区泊位2', 380.00, 200.00, 130.00, 'OFFLINE'),
(5, 'SP-YH-001', '1号沿海货船桩', 'COASTAL_CARGO', '深水港C区泊位1', 380.00, 400.00, 260.00, 'ONLINE'),
(6, 'SP-JZ-001', '1号集装箱船桩', 'CONTAINER_SHIP', '集装箱码头D区泊位1', 6600.00, 80.00, 630.00, 'ONLINE'),
(7, 'SP-YL-001', '1号油轮桩', 'TANKER', '油轮码头E区泊位1', 6600.00, 130.00, 1000.00, 'ONLINE');

INSERT IGNORE INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('electricity.price', '0.65', '实时电价(元/kWh)', 'electricity_price', '用于预约费用计算和模拟数据'),
('electricity.price.off_peak', '0.45', '低谷电价(元/kWh)', 'electricity_price', '22:00-6:00'),
('electricity.price.mid_peak', '0.65', '平段电价(元/kWh)', 'electricity_price', '6:00-8:00,12:00-18:00'),
('electricity.price.peak', '0.85', '高峰电价(元/kWh)', 'electricity_price', '8:00-12:00,18:00-22:00'),
('alarm.temperature.warning', '65', '温度告警阈值(℃)', 'alarm_threshold', '桩体温度超过此值触发WARNING告警'),
('alarm.temperature.critical', '80', '温度严重告警阈值(℃)', 'alarm_threshold', '桩体温度超过此值触发CRITICAL告警'),
('alarm.voltage.ratio', '0.1', '电压偏差告警比例', 'alarm_threshold', '电压超出额定电压±此比例时触发告警'),
('device.polling.interval', '10000', '数据轮询间隔(ms)', 'system', '设备数据采集推送频率'),
('reservation.slot.minutes', '15', '预约时段粒度(分钟)', 'system', '预约时间选择的最小间隔');
