package com.shorepower.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 设备类型与船舶类型映射服务
 *
 * 提供：
 *   1. 5种充电桩类型及电气参数
 *   2. 9种船舶类型的中文标签
 *   3. 船舶类型→兼容充电桩类型的映射表（用于预约时的兼容性匹配）
 *   4. 各类型船舶的默认电气参数（供快速创建参考）
 *
 * 映射规则参考 JT/T 1542-2025 标准中的船舶分类和岸电桩功率等级对应关系。
 * 数据被 Caffeine 缓存（@Cacheable），避免重复计算。
 */
@Service
public class DeviceTypeService {

    @Cacheable("deviceTypes")
    public Map<String, Object> getDeviceTypes() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("pileTypes", List.of(
            Map.of("value", "SMALL_YACHT", "label", "游艇桩(50kW)", "desc", "380V / 80A / 50kW", "voltage", 380, "current", 80, "power", 50),
            Map.of("value", "INLAND_CARGO", "label", "内河货船桩(130kW)", "desc", "380V / 200A / 130kW", "voltage", 380, "current", 200, "power", 130),
            Map.of("value", "COASTAL_CARGO", "label", "沿海货船桩(260kW)", "desc", "380V / 400A / 260kW", "voltage", 380, "current", 400, "power", 260),
            Map.of("value", "CONTAINER_SHIP", "label", "集装箱船桩(630kW)", "desc", "6600V / 80A / 630kW", "voltage", 6600, "current", 80, "power", 630),
            Map.of("value", "TANKER", "label", "油轮桩(1000kW)", "desc", "6600V / 130A / 1000kW", "voltage", 6600, "current", 130, "power", 1000)
        ));

        result.put("shipTypeLabels", Map.of(
            "FISHING", "渔船",
            "YACHT", "游艇",
            "CARGO", "货船",
            "CONTAINER", "集装箱船",
            "TANKER", "油轮",
            "PASSENGER", "客船",
            "BULK", "散货船",
            "RO_RO", "滚装船",
            "OTHER", "其他"
        ));

        result.put("shipToPileMap", Map.of(
            "FISHING", List.of("SMALL_YACHT"),
            "YACHT", List.of("SMALL_YACHT"),
            "CARGO", List.of("INLAND_CARGO", "COASTAL_CARGO"),
            "CONTAINER", List.of("CONTAINER_SHIP"),
            "TANKER", List.of("TANKER"),
            "PASSENGER", List.of("SMALL_YACHT"),
            "BULK", List.of("INLAND_CARGO", "COASTAL_CARGO"),
            "RO_RO", List.of("COASTAL_CARGO"),
            "OTHER", List.of("SMALL_YACHT", "INLAND_CARGO")
        ));

        result.put("shipTypeDefaults", Map.of(
            "FISHING", Map.of("tonnage", 200, "length", 30, "width", 7, "draft", 2.5, "voltage", 380, "power", 50),
            "YACHT", Map.of("tonnage", 100, "length", 20, "width", 5, "draft", 2.0, "voltage", 380, "power", 50),
            "CARGO", Map.of("tonnage", 5000, "length", 100, "width", 16, "draft", 5.5, "voltage", 380, "power", 260),
            "CONTAINER", Map.of("tonnage", 15000, "length", 200, "width", 30, "draft", 8.5, "voltage", 6600, "power", 630),
            "TANKER", Map.of("tonnage", 20000, "length", 180, "width", 28, "draft", 9.0, "voltage", 6600, "power", 1000),
            "PASSENGER", Map.of("tonnage", 8000, "length", 150, "width", 22, "draft", 6.0, "voltage", 380, "power", 130),
            "BULK", Map.of("tonnage", 10000, "length", 170, "width", 25, "draft", 7.5, "voltage", 380, "power", 260),
            "RO_RO", Map.of("tonnage", 12000, "length", 180, "width", 26, "draft", 6.5, "voltage", 380, "power", 260),
            "OTHER", Map.of("tonnage", 1000, "length", 50, "width", 10, "draft", 3.0, "voltage", 380, "power", 130)
        ));

        return result;
    }
}
