package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.common.Result;
import com.shorepower.dto.LoginRequest;
import com.shorepower.dto.LoginResponse;
import com.shorepower.dto.RegisterRequest;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 *
 * 登录流程：验证用户名/密码 → 生成 JWT（含 userId、username、role）
 * 注册流程：查重（用户名/手机号/邮箱）→ BCrypt加密 → 创建用户（默认角色 USER）
 *
 * 密码使用 BCryptPasswordEncoder（加盐哈希，不可逆）存储，
 * 登录时通过 passwordEncoder.matches() 比对明文和哈希值
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录
     * 校验顺序：用户存在 → 账号启用 → 密码正确
     * 成功后返回 JWT Token + 用户信息
     */
    public Result<LoginResponse> login(LoginRequest req) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())
        );
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.fail("账号已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(),
                user.getTokenVersion() != null ? user.getTokenVersion() : 0);
        return Result.ok(new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(), user.getAvatar()));
    }

    @Transactional
    public Result<SysUser> register(RegisterRequest req) {
        if (req.getPassword() == null || req.getPassword().length() < 6 || req.getPassword().length() > 20) {
            return Result.fail("密码长度需6-20位");
        }
        // 空字符串归一为 NULL，避免与 sys_user 的唯一索引冲突（V7）
        String phone = (req.getPhone() != null && !req.getPhone().isEmpty()) ? req.getPhone() : null;
        String email = (req.getEmail() != null && !req.getEmail().isEmpty()) ? req.getEmail() : null;
        long count = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())
        );
        if (count > 0) {
            return Result.fail("用户名已存在");
        }
        if (phone != null) {
            long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)
            );
            if (phoneCount > 0) {
                return Result.fail("手机号已被注册");
            }
        }
        if (email != null) {
            long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)
            );
            if (emailCount > 0) {
                return Result.fail("邮箱已被注册");
            }
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole("USER");
        user.setStatus(1);
        user.setTokenVersion(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            return Result.fail("用户名、手机号或邮箱已被注册");
        }
        user.setPassword(null);
        return Result.ok(user);
    }
}
