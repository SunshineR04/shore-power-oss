package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.entity.Ship;
import com.shorepower.service.ShipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/ship")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService shipService;

    @PostMapping("/add")
    public Result<?> add(Authentication auth, @RequestBody Ship ship) {
        Long userId = (Long) auth.getPrincipal();
        return shipService.addShip(userId, ship);
    }

    @GetMapping("/list")
    public Result<?> getList(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return shipService.getUserShips(userId);
    }

    @GetMapping("/{id}")
    public Result<?> getDetail(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return shipService.getShipDetail(id, userId, isAdmin);
    }

    @PutMapping("/update")
    public Result<?> update(Authentication auth, @RequestBody Ship ship) {
        Long userId = (Long) auth.getPrincipal();
        return shipService.updateShip(userId, ship);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return shipService.deleteShip(userId, id);
    }

    @PutMapping("/toggle/{id}")
    public Result<?> toggle(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return shipService.toggleStatus(userId, id);
    }
}
