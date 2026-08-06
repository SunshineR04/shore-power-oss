package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.mapper.UsageRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final UsageRecordMapper usageRecordMapper;
    private final DeviceMapper deviceMapper;
    private final SysUserMapper userMapper;

    public Map<String, Object> getSummary() {
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUser::getRole, "USER");
        long totalUsers = userMapper.selectCount(userQuery);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);

        BigDecimal todaySpent = usageRecordMapper.sumCostByDateRange(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal weekSpent = usageRecordMapper.sumCostByDateRange(weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal monthSpent = usageRecordMapper.sumCostByDateRange(monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long todayUsageCount = usageRecordMapper.countByDateRange(today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        BigDecimal totalSpent = usageRecordMapper.sumCostByDateRange(
            LocalDate.of(2000, 1, 1).atStartOfDay(), LocalDate.of(2100, 1, 1).atStartOfDay());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalUsers", totalUsers);
        summary.put("todaySpent", todaySpent.setScale(2, RoundingMode.HALF_UP));
        summary.put("weekSpent", weekSpent.setScale(2, RoundingMode.HALF_UP));
        summary.put("monthSpent", monthSpent.setScale(2, RoundingMode.HALF_UP));
        summary.put("todayUsageCount", todayUsageCount);
        return summary;
    }

    public List<Map<String, Object>> getDeviceRevenueRanking() {
        List<Map<String, Object>> grouped = usageRecordMapper.revenueGroupByDevice();
        Set<Long> deviceIds = grouped.stream()
            .map(r -> ((Number) r.get("deviceId")).longValue())
            .collect(Collectors.toSet());
        Map<Long, String> deviceNames = new HashMap<>();
        deviceMapper.selectBatchIds(deviceIds).forEach(d -> deviceNames.put(d.getId(), d.getDeviceName()));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : grouped) {
            Long deviceId = ((Number) row.get("deviceId")).longValue();
            BigDecimal total = (BigDecimal) row.get("totalCost");
            long count = ((Number) row.get("cnt")).longValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", deviceId);
            item.put("deviceName", deviceNames.getOrDefault(deviceId, "未知设备"));
            item.put("totalRevenue", total.setScale(2, RoundingMode.HALF_UP));
            item.put("usageCount", count);
            ranking.add(item);
        }
        ranking.sort((a, b) -> ((BigDecimal) b.get("totalRevenue")).compareTo((BigDecimal) a.get("totalRevenue")));
        return ranking;
    }

    public List<Map<String, Object>> getUserSpendingRanking() {
        List<Map<String, Object>> grouped = usageRecordMapper.spendingGroupByUser();
        Set<Long> userIds = grouped.stream()
            .map(r -> ((Number) r.get("userId")).longValue())
            .collect(Collectors.toSet());
        Map<Long, SysUser> users = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(u -> users.put(u.getId(), u));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : grouped) {
            Long userId = ((Number) row.get("userId")).longValue();
            BigDecimal total = (BigDecimal) row.get("totalCost");
            long count = ((Number) row.get("cnt")).longValue();
            SysUser user = users.get(userId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", userId);
            item.put("userName", user != null ? user.getRealName() : "未知用户");
            item.put("username", user != null ? user.getUsername() : "unknown");
            item.put("totalSpent", total.setScale(2, RoundingMode.HALF_UP));
            item.put("usageCount", count);
            ranking.add(item);
        }
        ranking.sort((a, b) -> ((BigDecimal) b.get("totalSpent")).compareTo((BigDecimal) a.get("totalSpent")));
        return ranking;
    }

    public List<Map<String, Object>> getDailyTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = usageRecordMapper.dailyTrend(
            startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            dateMap.put(date, row);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            String dateStr = date.toString();
            Map<String, Object> row = dateMap.get(dateStr);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            if (row != null) {
                item.put("spent", ((BigDecimal) row.get("spent")).setScale(2, RoundingMode.HALF_UP));
                item.put("usageCount", ((Number) row.get("usageCount")).longValue());
            } else {
                item.put("spent", BigDecimal.ZERO.setScale(2));
                item.put("usageCount", 0L);
            }
            trend.add(item);
            date = date.plusDays(1);
        }
        return trend;
    }
}
