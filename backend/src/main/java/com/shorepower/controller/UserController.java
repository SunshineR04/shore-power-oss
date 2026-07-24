package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.entity.SysUser;
import com.shorepower.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public Result<?> currentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(userService.getById(userId));
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(Authentication auth, @RequestBody SysUser profile) {
        Long userId = (Long) auth.getPrincipal();
        profile.setId(userId);
        profile.setRole(null);
        profile.setStatus(null);
        profile.setUsername(null);
        profile.setPassword(null);
        userService.update(profile);
        return Result.ok(userService.getById(userId));
    }

    @PutMapping("/password")
    public Result<?> changePassword(Authentication auth, @RequestBody java.util.Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String role) {
        return Result.ok(userService.page(pageNum, pageSize, keyword, role));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> add(@RequestBody SysUser user) {
        userService.add(user);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody SysUser user) {
        userService.update(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> toggle(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }
}
