package com.shorepower.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {
    private long totalDevices;
    private long onlineDevices;
    private long faultDevices;
    private int pendingAlarms;
    private int pendingTasks;
    private List<Map<String, Object>> alarmByLevel;
    private List<Map<String, Object>> deviceByStatus;
}
