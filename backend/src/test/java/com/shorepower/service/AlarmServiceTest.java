package com.shorepower.service;

import com.shorepower.common.BusinessException;
import com.shorepower.entity.Alarm;
import com.shorepower.mapper.AlarmMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AlarmService 单元测试：告警处理状态白名单。
 */
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock private AlarmMapper alarmMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SimpMessagingTemplate ws;

    private AlarmService alarmService;

    @BeforeEach
    void setUp() {
        alarmService = new AlarmService(alarmMapper, deviceMapper, userMapper, ws);
    }

    @Test
    void handle_rejectsInvalidStatus() {
        Alarm alarm = new Alarm();
        alarm.setId(1L);
        alarm.setStatus("PENDING");
        when(alarmMapper.selectById(1L)).thenReturn(alarm);

        assertThrows(BusinessException.class,
            () -> alarmService.handle(1L, 2L, "HACKED", "备注"));
        verify(alarmMapper, never()).updateById(any());
    }

    @Test
    void handle_acceptsValidStatus() {
        Alarm alarm = new Alarm();
        alarm.setId(1L);
        alarm.setDeviceId(3L);
        alarm.setStatus("PENDING");
        when(alarmMapper.selectById(1L)).thenReturn(alarm);
        when(alarmMapper.selectCount(any())).thenReturn(0L);

        alarmService.handle(1L, 2L, "RESOLVED", "已处理");

        verify(alarmMapper).updateById(any());
    }
}
