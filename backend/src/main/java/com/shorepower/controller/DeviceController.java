package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.entity.Device;
import com.shorepower.service.DeviceService;
import com.shorepower.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 设备管理 API
 *
 * 功能：
 *   page         - 分页查询设备（支持关键字/状态/类型筛选）
 *   list         - 获取全部设备列表
 *   add/update/delete - CRUD（仅 ADMIN 权限）
 *   status-count - 各状态设备数量统计（卡片展示）
 *   latest/trend - 设备实时数据和趋势
 *   types        - 设备类型字典数据
 */
@RestController
@RequestMapping("/api/device")
@Validated
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceTypeService deviceTypeService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String type) {
        return Result.ok(deviceService.page(pageNum, pageSize, keyword, status, type));
    }

    @GetMapping("/list")
    public Result<?> listAll() {
        return Result.ok(deviceService.listAll());
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(deviceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> add(@RequestBody Device device) {
        deviceService.add(device);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody Device device) {
        deviceService.update(device);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }

    @GetMapping("/status-count")
    public Result<?> statusCount() {
        return Result.ok(deviceService.countByStatus());
    }

    @GetMapping("/{id}/latest")
    public Result<?> latestData(@PathVariable Long id) {
        return Result.ok(deviceService.getLatestData(id));
    }

    @GetMapping("/{id}/trend")
    public Result<?> trend(@PathVariable Long id,
                           @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours) {
        return Result.ok(deviceService.getDeviceTrend(id, hours));
    }

    @GetMapping("/latest-all")
    public Result<?> latestAll() {
        return Result.ok(deviceService.getLatestAllData());
    }

    @GetMapping("/types")
    public Result<?> deviceTypes() {
        return Result.ok(deviceTypeService.getDeviceTypes());
    }
}
