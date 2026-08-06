package com.shorepower.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缓存注册防护测试：
 * 所有 @Cacheable/@CacheEvict 使用的缓存名必须被 CacheConfig 注册，
 * 否则运行时抛 "Cannot find cache named" 异常（历史回归：漏注册 deviceTypes/prices）。
 */
class CacheConfigTest {

    @Test
    void allUsedCacheNamesAreRegistered() {
        CacheManager manager = new CacheConfig().cacheManager();
        Collection<String> registered = manager.getCacheNames();

        // 与代码中实际使用的缓存名保持一致：
        // SystemConfigService → configs；JwtAuthFilter/UserService → userAuth
        // DeviceTypeService → deviceTypes；ElectricityPriceService → prices
        assertTrue(registered.contains("configs"), "缺少 configs 缓存");
        assertTrue(registered.contains("userAuth"), "缺少 userAuth 缓存");
        assertTrue(registered.contains("deviceTypes"), "缺少 deviceTypes 缓存");
        assertTrue(registered.contains("prices"), "缺少 prices 缓存");
    }
}
