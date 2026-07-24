package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.SystemConfigService;
import com.shorepower.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final SystemConfigService configService;

    @GetMapping("/current")
    public Result<?> getCurrent() {
        double temp = weatherService.getCurrentAmbient();
        String location = configService.getConfigValue("weather.location");
        if (location == null) location = "shanghai";
        return Result.ok(Map.of("temperature", String.format("%.1f", temp), "location", location));
    }

    @PostMapping("/refresh")
    public Result<?> refresh(@RequestParam(required = false) String location) {
        if (location == null || location.isEmpty()) {
            location = configService.getConfigValue("weather.location");
        }
        if (location == null || location.isEmpty()) location = "shanghai";
        weatherService.refreshNow(location);
        double temp = weatherService.getCurrentAmbient();
        return Result.ok(Map.of("temperature", String.format("%.1f", temp), "location", location));
    }
}
