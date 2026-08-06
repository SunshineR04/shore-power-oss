package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.common.BusinessException;
import com.shorepower.entity.Alarm;
import com.shorepower.entity.Device;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.AlarmMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 告警管理服务
 *
 * 功能：
 *   page       - 分页查询告警（支持按状态/等级/设备筛选），自动填充设备名和经办人
 *   handle     - 处理告警（PENDING → RESOLVED/IGNORED），填写处置备注
 *   createAlarm - 创建告警（由 DataSimulator 调用）
 *
 * 告警处理后的逻辑：
 *   1. 推送 /topic/alarm-resolved 通知前端更新告警计数
 *   2. 如果告警已解决，检查该设备是否还有其他未处理告警
 *   3. 如果没有了，且设备状态为 FAULT，将其恢复为 ONLINE
 *      并推送 /topic/device-status 通知前端更新设备状态
 */
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmMapper alarmMapper;
    private final DeviceMapper deviceMapper;
    private final SysUserMapper userMapper;
    private final SimpMessagingTemplate ws;

    public IPage<Alarm> page(int pageNum, int pageSize, String status, String level, Long deviceId) {
        LambdaQueryWrapper<Alarm> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Alarm::getStatus, status);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(Alarm::getAlarmLevel, level);
        }
        if (deviceId != null) {
            wrapper.eq(Alarm::getDeviceId, deviceId);
        }
        wrapper.orderByDesc(Alarm::getAlarmTime);
        IPage<Alarm> page = alarmMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        if (!page.getRecords().isEmpty()) {
            enrichAlarms(page.getRecords());
        }
        return page;
    }

    private void enrichAlarms(List<Alarm> alarms) {
        List<Long> deviceIds = alarms.stream().map(Alarm::getDeviceId).collect(Collectors.toList());
        List<Long> handlerIds = alarms.stream().map(Alarm::getHandlerId).collect(Collectors.toList());

        Map<Long, String> deviceNames = deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Device::getDeviceName, (a, b) -> a));

        Map<Long, String> handlerNames = userMapper.selectBatchIds(handlerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));

        for (Alarm a : alarms) {
            a.setDeviceName(deviceNames.get(a.getDeviceId()));
            a.setHandlerName(handlerNames.get(a.getHandlerId()));
        }
    }

    /** 告警可流转到的状态白名单：仅允许 PENDING(重新打开)/RESOLVED(已解决)/IGNORED(已忽略) */
    private static final Set<String> ALLOWED_TARGET_STATUS = Set.of("PENDING", "RESOLVED", "IGNORED");

    @Transactional
    public void handle(Long id, Long handlerId, String status, String remark) {
        Alarm alarm = alarmMapper.selectById(id);
        if (alarm == null) throw new BusinessException("告警不存在");
        if (status == null || !ALLOWED_TARGET_STATUS.contains(status)) {
            throw new BusinessException("非法的告警处理状态");
        }
        alarm.setStatus(status);
        alarm.setHandlerId(handlerId);
        alarm.setHandleTime(LocalDateTime.now());
        alarm.setHandleRemark(remark);
        alarmMapper.updateById(alarm);

        // 通知前端告警计数减一
        ws.convertAndSend("/topic/alarm-resolved", Map.of("alarmId", id, "deviceId", alarm.getDeviceId(), "status", status));

        // 告警已解决 → 检查设备是否还有其他未处理告警
        if ("RESOLVED".equals(status) && alarm.getDeviceId() != null) {
            long pendingCount = alarmMapper.selectCount(
                new LambdaQueryWrapper<Alarm>()
                    .eq(Alarm::getDeviceId, alarm.getDeviceId())
                    .eq(Alarm::getStatus, "PENDING")
            );
            if (pendingCount == 0) {
                Device device = deviceMapper.selectById(alarm.getDeviceId());
                if (device != null && "FAULT".equals(device.getStatus())) {
                    device.setStatus("ONLINE");
                    deviceMapper.updateById(device);
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("deviceId", device.getId());
                    statusUpdate.put("status", "ONLINE");
                    ws.convertAndSend("/topic/device-status", statusUpdate);
                }
            }
        }
    }

    public int countPending() {
        return alarmMapper.countPending();
    }

    public List<Map<String, Object>> countByLevel() {
        return alarmMapper.countByLevel();
    }

    public void createAlarm(Alarm alarm) {
        alarm.setAlarmTime(LocalDateTime.now());
        alarm.setStatus("PENDING");
        alarmMapper.insert(alarm);
    }
}
