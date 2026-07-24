package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.dto.DashboardStats;
import com.shorepower.entity.Device;
import com.shorepower.entity.MaintenanceTask;
import com.shorepower.mapper.AlarmMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.MaintenanceTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DeviceMapper deviceMapper;
    private final AlarmMapper alarmMapper;
    private final MaintenanceTaskMapper taskMapper;

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        List<Device> devices = deviceMapper.selectList(null);
        stats.setTotalDevices(devices.size());
        stats.setOnlineDevices(devices.stream().filter(d -> "ONLINE".equals(d.getStatus())).count());
        stats.setFaultDevices(devices.stream().filter(d -> "FAULT".equals(d.getStatus())).count());
        stats.setPendingAlarms(alarmMapper.countPending());
        stats.setAlarmByLevel(alarmMapper.countByLevel());

        long pendingTasks = taskMapper.selectCount(
            new LambdaQueryWrapper<MaintenanceTask>()
                .in(MaintenanceTask::getStatus, "PENDING", "ASSIGNED", "IN_PROGRESS")
        );
        stats.setPendingTasks((int) pendingTasks);

        List<Map<String, Object>> deviceByStatus = devices.stream()
            .collect(Collectors.groupingBy(Device::getStatus, Collectors.counting()))
            .entrySet().stream()
            .map(e -> Map.<String, Object>of("status", e.getKey(), "count", e.getValue()))
            .toList();
        stats.setDeviceByStatus(deviceByStatus);

        return stats;
    }
}
