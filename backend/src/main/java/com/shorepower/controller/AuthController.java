package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.dto.LoginRequest;
import com.shorepower.dto.LoginResponse;
import com.shorepower.dto.RegisterRequest;
import com.shorepower.entity.SysUser;
import com.shorepower.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public Result<SysUser> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}
