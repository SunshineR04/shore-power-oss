package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.DeviceData;

import java.util.List;
import java.util.Map;

public interface DeviceDataMapper extends BaseMapper<DeviceData> {

    Map<String, Object> getDeviceAvg24h(Long deviceId);

    List<Map<String, Object>> getDeviceTrend(Long deviceId, int hours);

    List<Map<String, Object>> getLatestAllDevices();

    int deleteOlderThanDays(int days);
}
