package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.entity.SysConfig;
import com.shorepower.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SysConfigMapper configMapper;

    @Cacheable(value = "configs", key = "'all'")
    public List<SysConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    public List<SysConfig> getConfigsByType(String type) {
        return configMapper.selectList(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigType, type)
        );
    }

    @Cacheable(value = "configs", key = "#key")
    public String getConfigValue(String key) {
        SysConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        return config != null ? config.getConfigValue() : null;
    }

    @Cacheable(value = "configs", key = "'decimal:' + #key")
    public BigDecimal getDecimalConfig(String key, BigDecimal defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Cacheable(value = "configs", key = "'int:' + #key")
    public int getIntConfig(String key, int defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Cacheable(value = "configs", key = "'long:' + #key")
    public long getLongConfig(String key, long defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @CacheEvict(value = "configs", allEntries = true)
    public void updateConfig(String key, String value) {
        SysConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (config != null) {
            config.setConfigValue(value);
            configMapper.updateById(config);
        }
    }

    @CacheEvict(value = "configs", allEntries = true)
    public void batchUpdate(List<SysConfig> configs) {
        for (SysConfig config : configs) {
            SysConfig existing = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, config.getConfigKey())
            );
            if (existing != null) {
                existing.setConfigValue(config.getConfigValue());
                configMapper.updateById(existing);
            }
        }
    }

    public Map<String, String> getConfigMap() {
        List<SysConfig> list = configMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }
}
