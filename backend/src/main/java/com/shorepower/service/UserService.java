package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.common.BusinessException;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    /** 与 JwtAuthFilter 中的认证缓存同名，变更用户状态后立即清除 */
    private static final String USER_CACHE = "userAuth";

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
        user.setTokenVersion(0);
        userMapper.insert(user);
    }

    public void update(SysUser user) {
        SysUser existing = user.getId() != null ? userMapper.selectById(user.getId()) : null;
        boolean authChanged = false;
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            authChanged = true; // 改密码：旧 token 失效
        } else {
            user.setPassword(null);
        }
        // 角色实际变化才递增版本；管理员编辑资料时回传相同角色不应让用户下线
        if (user.getRole() != null
                && (existing == null || !user.getRole().equals(existing.getRole()))) {
            authChanged = true;
        }
        if (authChanged) {
            user.setTokenVersion(incrementTokenVersion(user.getId()));
        }
        userMapper.updateById(user);
        if (authChanged) {
            evictUserCache(user.getId());
        }
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
        evictUserCache(id);
    }

    public void toggleStatus(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        // 禁用/启用都递增版本：被禁用户旧 token 立即失效
        user.setTokenVersion(incrementTokenVersion(id));
        userMapper.updateById(user);
        evictUserCache(id);
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
        user.setTokenVersion(incrementTokenVersion(userId));
        userMapper.updateById(user);
        evictUserCache(userId);
    }

    /** 读取当前版本号并 +1（无并发场景下直接读取；如后续高并发可改为 UPDATE ... SET token_version = token_version + 1） */
    private int incrementTokenVersion(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getTokenVersion() == null) return 1;
        return user.getTokenVersion() + 1;
    }

    private void evictUserCache(Long userId) {
        Cache cache = cacheManager.getCache(USER_CACHE);
        if (cache != null) cache.evict(userId);
    }
}
