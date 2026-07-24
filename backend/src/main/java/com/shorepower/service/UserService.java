package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.common.BusinessException;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public IPage<SysUser> page(int pageNum, int pageSize, String keyword, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or().like(SysUser::getRealName, keyword);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public void add(SysUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
    }

    public void update(SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    public void toggleStatus(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException("密码长度需6-20位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
