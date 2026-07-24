package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.common.Result;
import com.shorepower.entity.Ship;
import com.shorepower.mapper.ShipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipService {

    private final ShipMapper shipMapper;

    public Ship getById(Long id) {
        return shipMapper.selectById(id);
    }

    public Result<List<Map<String, Object>>> getUserShips(Long userId) {
        List<Map<String, Object>> ships = shipMapper.getUserShips(userId);
        return Result.ok(ships);
    }

    public Result<Map<String, Object>> getShipDetail(Long id, Long userId, boolean isAdmin) {
        Map<String, Object> ship = shipMapper.getShipDetail(id);
        if (ship == null) {
            return Result.fail("船舶不存在");
        }
        Object ownerId = ship.get("userId");
        if (!isAdmin && (ownerId == null || !ownerId.toString().equals(userId.toString()))) {
            return Result.fail("无权查看此船舶");
        }
        return Result.ok(ship);
    }

    public Result<?> addShip(Long userId, Ship ship) {
        ship.setUserId(userId);
        ship.setStatus(1);
        shipMapper.insert(ship);
        return Result.ok();
    }

    public Result<?> updateShip(Long userId, Ship ship) {
        Ship existing = shipMapper.selectById(ship.getId());
        if (existing == null) {
            return Result.fail("船舶不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            return Result.fail("无权操作此船舶");
        }
        ship.setUserId(null);
        ship.setCreateTime(null);
        shipMapper.updateById(ship);
        return Result.ok();
    }

    public Result<?> deleteShip(Long userId, Long id) {
        Ship existing = shipMapper.selectById(id);
        if (existing == null) {
            return Result.fail("船舶不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            return Result.fail("无权操作此船舶");
        }
        shipMapper.deleteById(id);
        return Result.ok();
    }

    public Result<?> toggleStatus(Long userId, Long id) {
        Ship ship = shipMapper.selectById(id);
        if (ship == null) {
            return Result.fail("船舶不存在");
        }
        if (!ship.getUserId().equals(userId)) {
            return Result.fail("无权操作此船舶");
        }
        ship.setStatus(ship.getStatus() == 1 ? 0 : 1);
        shipMapper.updateById(ship);
        return Result.ok();
    }
}
