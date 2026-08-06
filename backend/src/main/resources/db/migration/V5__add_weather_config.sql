-- Flyway Migration V5: 天气相关系统配置

INSERT IGNORE INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('weather.api.key', '', 'OpenWeatherMap API Key', 'system', '用于获取实时气温，在 https://openweathermap.org 注册获取'),
('weather.location', 'shanghai', '设备所在城市', 'system', '用于天气API查询，如 shanghai / beijing / shenzhen'),
('temperature.ambient.base', '30', '环境基准温度(℃)', 'system', 'API不可用时兜底，夏季建议35冬季建议20');
