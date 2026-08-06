package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.entity.Device;
import com.shorepower.entity.DeviceData;
import com.shorepower.mapper.DeviceDataMapper;
import com.shorepower.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceDataMapper dataMapper;

    public IPage<Device> page(int pageNum, int pageSize, String keyword, String status, String type) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Device::getDeviceName, keyword)
                   .or().like(Device::getDeviceCode, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Device::getStatus, status);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Device::getDeviceType, type);
        }
        wrapper.orderByDesc(Device::getCreateTime);
        return deviceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Device> listAll() {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().orderByAsc(Device::getDeviceCode));
    }

    public Device getById(Long id) {
        return deviceMapper.selectById(id);
    }

    public void add(Device device) {
        deviceMapper.insert(device);
    }

    public void update(Device device) {
        deviceMapper.updateById(device);
    }

    public void delete(Long id) {
        deviceMapper.deleteById(id);
    }

    public Map<String, Object> getDeviceAvg(Long deviceId) {
        return dataMapper.getDeviceAvg24h(deviceId);
    }

    public List<Map<String, Object>> getDeviceTrend(Long deviceId, int hours) {
        return dataMapper.getDeviceTrend(deviceId, hours);
    }

    public DeviceData getLatestData(Long deviceId) {
        return dataMapper.selectOne(
            new LambdaQueryWrapper<DeviceData>()
                .eq(DeviceData::getDeviceId, deviceId)
                .orderByDesc(DeviceData::getCollectTime)
                .last("LIMIT 1")
        );
    }

    public List<Map<String, Object>> getLatestAllData() {
        return dataMapper.getLatestAllDevices();
    }

    public Map<String, Long> countByStatus() {
        List<Device> all = deviceMapper.selectList(null);
        long online = all.stream().filter(d -> "ONLINE".equals(d.getStatus())).count();
        long offline = all.stream().filter(d -> "OFFLINE".equals(d.getStatus())).count();
        long fault = all.stream().filter(d -> "FAULT".equals(d.getStatus())).count();
        long maintenance = all.stream().filter(d -> "MAINTENANCE".equals(d.getStatus())).count();
        return Map.of("total", (long) all.size(), "online", online, "offline", offline,
                       "fault", fault, "maintenance", maintenance);
    }
}
