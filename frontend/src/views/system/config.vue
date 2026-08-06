<template>
  <div class="config-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">配置电价策略、告警阈值与系统运行参数</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleBatchSave" :disabled="!hasChanges" class="save-all-btn">
          <el-icon><Check /></el-icon>保存全部
        </el-button>
        <el-button @click="loadConfigs" :disabled="!hasChanges">重置</el-button>
      </div>
      <div class="header-decoration"></div>
    </div>

    <div class="config-body">
      <div class="config-tabs animate-fade-in-up stagger-1">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          :class="['config-tab-item', { 'config-tab-item--active': activeTab === tab.name }]"
          @click="activeTab = tab.name"
        >
          <div :class="['config-tab-icon', `config-tab-icon--${tab.theme}`]" v-html="tab.icon"></div>
          <span class="config-tab-label">{{ tab.label }}</span>
        </div>
      </div>

      <div class="config-content animate-fade-in-up stagger-2">
        <el-form :model="configForm" label-width="200px" class="config-form">
          <div v-if="activeTab === 'electricity_price'">
            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--primary">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                </div>
                <span class="config-section-title">基础电价</span>
              </div>
              <div class="config-section-body">
                <el-form-item :label="getLabel('electricity.price')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['electricity.price']" :min="0.1" :max="5" :step="0.05" :precision="2" class="config-input-number" />
                    <span class="config-unit">元/kWh</span>
                    <span class="config-remark">{{ getRemark('electricity.price') }}</span>
                  </div>
                </el-form-item>
              </div>
            </div>

            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--accent">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </div>
                <span class="config-section-title">分时电价</span>
              </div>
              <div class="config-section-body">
                <el-row :gutter="20">
                  <el-col :xs="24" :sm="12" :md="12" :lg="8">
                    <el-form-item :label="getLabel('electricity.price.off_peak')" label-width="140px">
                      <el-input-number v-model="configForm['electricity.price.off_peak']" :min="0.1" :max="5" :step="0.05" :precision="2" class="config-input-number-sm" />
                    </el-form-item>
                    <div class="config-time-hint">低谷时段 22:00-6:00</div>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="12" :lg="8">
                    <el-form-item :label="getLabel('electricity.price.mid_peak')" label-width="140px">
                      <el-input-number v-model="configForm['electricity.price.mid_peak']" :min="0.1" :max="5" :step="0.05" :precision="2" class="config-input-number-sm" />
                    </el-form-item>
                    <div class="config-time-hint">平段时段 6:00-8:00,12:00-18:00</div>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="12" :lg="8">
                    <el-form-item :label="getLabel('electricity.price.peak')" label-width="140px">
                      <el-input-number v-model="configForm['electricity.price.peak']" :min="0.1" :max="5" :step="0.05" :precision="2" class="config-input-number-sm" />
                    </el-form-item>
                    <div class="config-time-hint">高峰时段 8:00-12:00,18:00-22:00</div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'alarm_threshold'">
            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--warning">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/><path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg>
                </div>
                <span class="config-section-title">温度告警</span>
              </div>
              <div class="config-section-body">
                <el-form-item :label="getLabel('alarm.temperature.warning')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['alarm.temperature.warning']" :min="10" :max="100" :step="5" class="config-input-number" />
                    <span class="config-unit">℃</span>
                    <span class="config-hint config-hint--warning">超过此值触发 WARNING 告警</span>
                  </div>
                </el-form-item>
                <el-form-item :label="getLabel('alarm.temperature.critical')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['alarm.temperature.critical']" :min="10" :max="120" :step="5" class="config-input-number" />
                    <span class="config-unit">℃</span>
                    <span class="config-hint config-hint--danger">超过此值触发 CRITICAL 告警</span>
                  </div>
                </el-form-item>
              </div>
            </div>

            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--danger">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                </div>
                <span class="config-section-title">电压告警</span>
              </div>
              <div class="config-section-body">
                <el-form-item :label="getLabel('alarm.voltage.ratio')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['alarm.voltage.ratio']" :min="0.01" :max="0.5" :step="0.01" :precision="2" class="config-input-number" />
                    <span class="config-unit">(比例值)</span>
                    <span class="config-remark">电压超出额定电压 ±{{ (configForm['alarm.voltage.ratio'] * 100).toFixed(0) }}% 时触发告警</span>
                  </div>
                </el-form-item>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'system'">
            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--accent">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
                </div>
                <span class="config-section-title">系统运行参数</span>
              </div>
              <div class="config-section-body">
                <el-form-item :label="getLabel('device.polling.interval')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['device.polling.interval']" :min="1000" :max="60000" :step="1000" class="config-input-number" />
                    <span class="config-unit">毫秒</span>
                    <span class="config-remark">{{ getRemark('device.polling.interval') }}</span>
                  </div>
                </el-form-item>
                <el-form-item :label="getLabel('reservation.slot.minutes')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['reservation.slot.minutes']" :min="5" :max="60" :step="5" class="config-input-number" />
                    <span class="config-unit">分钟</span>
                    <span class="config-remark">{{ getRemark('reservation.slot.minutes') }}</span>
                  </div>
                </el-form-item>
              </div>
            </div>

            <div class="config-section">
              <div class="config-section-header">
                <div class="config-section-icon config-section-icon--warning">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 22h20L12 2z"/><line x1="12" y1="10" x2="12" y2="16"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
                </div>
                <span class="config-section-title">天气与环境</span>
              </div>
              <div class="config-section-body">
                <el-form-item :label="getLabel('weather.api.key')">
                  <div class="config-field">
                    <el-input v-model="configForm['weather.api.key']" placeholder="输入 OpenWeatherMap API Key" class="config-input-text" />
                    <span class="config-remark">{{ getRemark('weather.api.key') }}</span>
                  </div>
                </el-form-item>
                <el-form-item :label="getLabel('weather.location')">
                  <div class="config-field">
                    <el-cascader v-model="weatherLocation" :options="provinceCities" placeholder="选择省份/城市" class="config-cascader" clearable filterable @change="onLocationChange" />
                    <span class="config-remark">{{ getRemark('weather.location') }}</span>
                  </div>
                </el-form-item>
                <el-form-item :label="getLabel('temperature.ambient.base')">
                  <div class="config-field">
                    <el-input-number v-model="configForm['temperature.ambient.base']" :min="-20" :max="50" :step="5" class="config-input-number" />
                    <span class="config-unit">℃</span>
                    <span class="config-remark">{{ getRemark('temperature.ambient.base') }}</span>
                    <el-button size="small" type="primary" plain :loading="weatherLoading" @click="fetchWeather" class="weather-btn">获取当前温度</el-button>
                    <span v-if="weatherResult" :class="['weather-result', { 'weather-result--success': weatherSuccess, 'weather-result--fail': !weatherSuccess }]">{{ weatherResult }}</span>
                  </div>
                </el-form-item>
              </div>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { systemConfigApi, weatherApi } from '../../api'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const activeTab = ref('electricity_price')
const configForm = reactive({})
const originalForm = reactive({})
const configMeta = ref({})

const tabs = [
  {
    name: 'electricity_price',
    label: '电价配置',
    theme: 'primary',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>'
  },
  {
    name: 'alarm_threshold',
    label: '告警阈值',
    theme: 'warning',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
  },
  {
    name: 'system',
    label: '系统参数',
    theme: 'accent',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>'
  }
]

const provinceCities = [
  { label: '北京', value: 'beijing', children: [{ label: '北京', value: 'beijing' }] },
  { label: '上海', value: 'shanghai', children: [{ label: '上海', value: 'shanghai' }] },
  { label: '天津', value: 'tianjin', children: [{ label: '天津', value: 'tianjin' }] },
  { label: '重庆', value: 'chongqing', children: [{ label: '重庆', value: 'chongqing' }] },
  { label: '广东', value: 'guangdong', children: [
    { label: '广州', value: 'guangzhou' }, { label: '深圳', value: 'shenzhen' },
    { label: '珠海', value: 'zhuhai' }, { label: '东莞', value: 'dongguan' },
    { label: '佛山', value: 'foshan' }, { label: '中山', value: 'zhongshan' }
  ]},
  { label: '浙江', value: 'zhejiang', children: [
    { label: '杭州', value: 'hangzhou' }, { label: '宁波', value: 'ningbo' },
    { label: '温州', value: 'wenzhou' }, { label: '嘉兴', value: 'jiaxing' }
  ]},
  { label: '江苏', value: 'jiangsu', children: [
    { label: '南京', value: 'nanjing' }, { label: '苏州', value: 'suzhou' },
    { label: '无锡', value: 'wuxi' }, { label: '南通', value: 'nantong' }
  ]},
  { label: '福建', value: 'fujian', children: [
    { label: '厦门', value: 'xiamen' }, { label: '福州', value: 'fuzhou' },
    { label: '泉州', value: 'quanzhou' }
  ]},
  { label: '山东', value: 'shandong', children: [
    { label: '青岛', value: 'qingdao' }, { label: '济南', value: 'jinan' },
    { label: '烟台', value: 'yantai' }, { label: '威海', value: 'weihai' }
  ]},
  { label: '辽宁', value: 'liaoning', children: [
    { label: '大连', value: 'dalian' }, { label: '沈阳', value: 'shenyang' }
  ]},
  { label: '湖北', value: 'hubei', children: [
    { label: '武汉', value: 'wuhan' }, { label: '宜昌', value: 'yichang' }
  ]},
  { label: '湖南', value: 'hunan', children: [
    { label: '长沙', value: 'changsha' }, { label: '株洲', value: 'zhuzhou' }
  ]},
  { label: '四川', value: 'sichuan', children: [
    { label: '成都', value: 'chengdu' }, { label: '绵阳', value: 'mianyang' }
  ]},
  { label: '陕西', value: 'shaanxi', children: [
    { label: '西安', value: 'xian' }, { label: '咸阳', value: 'xianyang' }
  ]},
  { label: '海南', value: 'hainan', children: [
    { label: '海口', value: 'haikou' }, { label: '三亚', value: 'sanya' }
  ]}
]

const weatherLocation = ref([])
const weatherLoading = ref(false)
const weatherResult = ref('')
const weatherSuccess = ref(false)

function onLocationChange(val) {
  configForm['weather.location'] = val?.[1] || ''
}

async function fetchWeather() {
  const key = configForm['weather.api.key']
  if (!key) { weatherResult.value = '请先填写 API Key'; weatherSuccess.value = false; return }
  const loc = configForm['weather.location']
  if (!loc) { weatherResult.value = '请先选择城市'; weatherSuccess.value = false; return }
  weatherLoading.value = true
  weatherResult.value = ''
  try {
    const res = await weatherApi.refresh(loc)
    const data = res.data || {}
    weatherResult.value = `${data.location} 当前温度: ${data.temperature}°C`
    weatherSuccess.value = true
  } catch {
    weatherResult.value = '获取失败，请检查 API Key 和网络连接'
    weatherSuccess.value = false
  } finally {
    weatherLoading.value = false
  }
}

const hasChanges = computed(() => {
  return Object.keys(configForm).some(k => configForm[k] !== originalForm[k])
})

const getLabel = (key) => {
  return configMeta.value[key]?.configName || key
}

const getRemark = (key) => {
  return configMeta.value[key]?.remark || ''
}

const loadConfigs = async () => {
  try {
    const res = await systemConfigApi.list()
    const list = res.data || []
    const meta = {}
    list.forEach(item => {
      meta[item.configKey] = item
    })
    configMeta.value = meta

    const numericKeys = ['electricity.price', 'electricity.price.off_peak', 'electricity.price.mid_peak',
      'electricity.price.peak', 'alarm.temperature.warning', 'alarm.temperature.critical',
      'alarm.voltage.ratio', 'device.polling.interval', 'reservation.slot.minutes',
      'temperature.ambient.base']

    numericKeys.forEach(key => {
      if (meta[key]) {
        configForm[key] = Number(meta[key].configValue)
        originalForm[key] = Number(meta[key].configValue)
      }
    })

    const textKeys = ['weather.api.key', 'weather.location']
    textKeys.forEach(key => {
      if (meta[key]) {
        configForm[key] = meta[key].configValue
        originalForm[key] = meta[key].configValue
      }
    })
    // 从保存的城市代码还原级联路径
    if (configForm['weather.location']) {
      const saved = configForm['weather.location']
      for (const p of provinceCities) {
        const found = p.children?.find(c => c.value === saved)
        if (found) { weatherLocation.value = [p.value, saved]; break }
      }
    }
  } catch (e) {
    ElMessage.error('加载系统配置失败')
  }
}

const handleBatchSave = async () => {
  try {
    const configs = Object.keys(configForm).map(key => ({
      configKey: key,
      configValue: String(configForm[key])
    }))
    await systemConfigApi.batchUpdate(configs)
    Object.assign(originalForm, configForm)
    ElMessage.success('所有配置已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.config-page {
  font-family: var(--font-body);
  color: var(--text-primary);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, var(--accent) 0%, var(--accent) 100%);
  border-radius: 2px 0 0 2px;
}

.header-content {
  position: relative;
  z-index: 1;
}

.page-title {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  font-weight: 400;
}

.header-actions {
  display: flex;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.save-all-btn {
  font-weight: 600;
}

.config-body {
  display: flex;
  gap: 20px;
}

.config-tabs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 200px;
  flex-shrink: 0;
}

.config-tab-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.config-tab-item:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
}

.config-tab-item--active {
  background: linear-gradient(135deg, var(--primary-bg) 0%, rgba(37, 99, 235, 0.02) 100%);
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary), var(--shadow-md);
}

.config-tab-item--active .config-tab-label {
  color: var(--primary);
  font-weight: 600;
}

.config-tab-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.config-tab-icon--primary {
  background: var(--primary-bg);
  color: var(--primary);
}

.config-tab-icon--warning {
  background: var(--warning-bg);
  color: var(--warning);
}

.config-tab-icon--accent {
  background: var(--accent-bg);
  color: var(--accent);
}

.config-tab-label {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all var(--transition-normal);
}

.config-content {
  flex: 1;
  min-width: 0;
}

.config-form {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.config-section {
  border-bottom: 1px solid var(--border-light);
}

.config-section:last-child {
  border-bottom: none;
}

.config-section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 24px;
  background: var(--bg-hover);
  border-bottom: 1px solid var(--border-light);
}

.config-section-icon {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.config-section-icon--primary {
  background: var(--primary-bg);
  color: var(--primary);
}

.config-section-icon--accent {
  background: var(--accent-bg);
  color: var(--accent);
}

.config-section-icon--warning {
  background: var(--warning-bg);
  color: var(--warning);
}

.config-section-icon--danger {
  background: var(--danger-bg);
  color: var(--danger);
}

.config-section-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.config-section-body {
  padding: 20px 24px;
}

.config-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-input-number {
  width: 200px;
}

.config-input-number-sm {
  width: 160px;
}

.config-input-text {
  width: 300px;
}

.config-unit {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
}

.config-remark {
  font-size: 13px;
  color: var(--text-muted);
}

.config-hint {
  font-size: 13px;
  white-space: nowrap;
}

.config-hint--warning {
  color: var(--warning);
}

.config-hint--danger {
  color: var(--danger);
}

.config-time-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 140px;
  margin-top: -12px;
  margin-bottom: 8px;
}

.weather-btn {
  margin-left: 12px;
}

.weather-result {
  margin-left: 12px;
  font-size: 13px;
  font-weight: 600;
}
.weather-result--success { color: var(--success); }
.weather-result--fail { color: var(--danger); }

.config-select {
  width: 200px;
}

.config-cascader {
  width: 260px;
}
</style>
