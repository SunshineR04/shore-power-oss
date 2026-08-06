package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.SystemConfigService;
import com.shorepower.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final SystemConfigService configService;

    /** 允许手动刷新的地点白名单（防止任意 location 参数触发外部请求） */
    private static final Set<String> ALLOWED_LOCATIONS = Set.of(
        "shanghai", "wuhan", "nanjing", "chongqing", "yichang", "beijing", "guangzhou"
    );

    @GetMapping("/current")
    public Result<?> getCurrent() {
        double temp = weatherService.getCurrentAmbient();
        String location = configService.getConfigValue("weather.location");
        if (location == null) location = "shanghai";
        return Result.ok(Map.of("temperature", String.format("%.1f", temp), "location", location));
    }

    /** 手动刷新天气：仅管理员可触发，且地点必须在白名单内 */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> refresh(@RequestParam(required = false) String location) {
        if (location == null || location.isEmpty()) {
            location = configService.getConfigValue("weather.location");
        }
        if (location == null || location.isEmpty()) location = "shanghai";
        if (!ALLOWED_LOCATIONS.contains(location.toLowerCase())) {
            return Result.fail(400, "不支持的地点");
        }
        weatherService.refreshNow(location.toLowerCase());
        double temp = weatherService.getCurrentAmbient();
        return Result.ok(Map.of("temperature", String.format("%.1f", temp), "location", location.toLowerCase()));
    }
}
