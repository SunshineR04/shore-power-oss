package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.Reservation;

import java.util.List;
import java.util.Map;

public interface ReservationMapper extends BaseMapper<Reservation> {

    List<Map<String, Object>> getUserReservations(Long userId);

    Map<String, Object> getReservationDetail(Long id);

    long countConflict(Long deviceId, Long userId, String startTime, String endTime);

    long countUserActiveReservations(Long deviceId, Long userId);

    List<Long> findInUseDeviceIds();
}
