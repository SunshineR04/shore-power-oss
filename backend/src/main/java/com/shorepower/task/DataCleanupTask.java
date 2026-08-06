package com.shorepower.task;

import com.shorepower.mapper.DeviceDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupTask {

    private final DeviceDataMapper deviceDataMapper;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldDeviceData() {
        int deleted = deviceDataMapper.deleteOlderThanDays(7);
        if (deleted > 0) {
            log.info("清理了 {} 条旧设备数据记录", deleted);
        }
    }
}
