package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.entity.SysConfig;
import com.shorepower.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping("/api/config/public")
    public Result<?> getPublicConfig() {
        Map<String, String> map = configService.getConfigMap();
        Map<String, Object> result = Map.of(
            "reservationSlotMinutes", configService.getIntConfig("reservation.slot.minutes", 15),
            "pollingInterval", configService.getIntConfig("device.polling.interval", 10000)
        );
        return Result.ok(result);
    }

    @GetMapping("/api/admin/config/list")
    public Result<?> getList() {
        List<SysConfig> list = configService.getAllConfigs();
        return Result.ok(list);
    }

    @GetMapping("/api/admin/config/types")
    public Result<?> getTypes() {
        Map<String, String> map = configService.getConfigMap();
        return Result.ok(map);
    }

    @PutMapping("/api/admin/config/update")
    public Result<?> update(@RequestBody SysConfig config) {
        configService.updateConfig(config.getConfigKey(), config.getConfigValue());
        return Result.ok();
    }

    @PutMapping("/api/admin/config/batch-update")
    public Result<?> batchUpdate(@RequestBody List<SysConfig> configs) {
        configService.batchUpdate(configs);
        return Result.ok();
    }
}
