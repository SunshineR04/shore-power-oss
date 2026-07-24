<template>
  <div class="energy-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">能耗中心</h1>
        <p class="page-subtitle">实时监控岸电系统能耗数据，优化用电策略与负载均衡</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-tabs v-model="activeTab" class="energy-tabs" @tab-change="handleTabChange">
      <!-- ==================== Tab 1: 总览 ==================== -->
      <el-tab-pane name="overview">
        <template #label><span class="tab-label">总览</span></template>

        <div class="stat-grid">
          <div v-for="item in overviewCards" :key="item.label" class="stat-card" :style="{'--card-accent': item.color, 'border-left-color': item.color}">
            <div class="stat-card__icon" :style="{background: item.color + '12'}">
              <el-icon :size="20" :color="item.color">
                <Lightning v-if="item.label==='总能耗'" />
                <Wallet v-else-if="item.label==='总费用'" />
                <ArrowDown v-else-if="item.label.includes('CO')" />
                <Sunny v-else-if="item.label==='节煤量'" />
                <Connection v-else-if="item.label==='等效植树'" />
                <TrendCharts v-else />
              </el-icon>
            </div>
            <div class="stat-card__content">
              <div class="stat-card__label">{{ item.label }}</div>
              <div class="stat-card__value" :style="{color: item.color}">{{ item.value }}</div>
              <div class="stat-card__unit">{{ item.unit }}</div>
            </div>
          </div>
        </div>

        <el-card class="section-card mb-md">
          <div class="filter-bar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
              end-placeholder="结束日期" value-format="YYYY-MM-DD" class="date-picker" @change="loadOverview" />
            <el-radio-group v-model="statType" @change="handleStatTypeChange">
              <el-radio-button value="DAILY">日</el-radio-button>
              <el-radio-button value="WEEKLY">周</el-radio-button>
              <el-radio-button value="MONTHLY">月</el-radio-button>
            </el-radio-group>
            <el-switch v-model="enableComparison" active-text="对比上期" @change="loadOverview" />
          </div>
          <el-row :gutter="16">
            <el-col :span="16"><div ref="trendChart" class="chart-area chart-area--trend"></div></el-col>
            <el-col :span="8"><div ref="deviceChart" class="chart-area chart-area--trend"></div></el-col>
          </el-row>
        </el-card>

        <div class="price-grid">
          <div v-for="p in timeOfUsePrices" :key="p.label" class="price-card" :style="{'--price-accent': p.color, 'border-left-color': p.color}">
            <div class="price-card__icon" :style="{background: p.color + '12'}">
              <el-icon :size="18" :color="p.color">
                <Moon v-if="p.label==='低谷电价'" />
                <Sunny v-else-if="p.label==='平段电价'" />
                <Warning v-else />
              </el-icon>
            </div>
            <div class="price-card__body">
              <div class="price-card__label">{{ p.label }}</div>
              <div class="price-card__price" :style="{color: p.color}">{{ p.price }} <small>元/kWh</small></div>
              <div class="price-card__time">{{ p.time }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ==================== Tab 2: 均衡 ==================== -->
      <el-tab-pane name="balancing">
        <template #label><span class="tab-label">均衡</span></template>

        <div class="filter-bar">
          <el-button @click="loadBalancing" :loading="balancingLoading">刷新数据</el-button>
          <span v-if="balancingUpdateTime" class="filter-hint">最后更新: {{ balancingUpdateTime }}</span>
        </div>

        <div class="balancing-grid">
          <div class="stat-card" style="--card-accent: #0984e3; border-left-color: #0984e3;">
            <div class="stat-card__icon" style="background: rgba(9,132,227,0.1);">
              <el-icon :size="20" color="#0984e3"><Cpu /></el-icon>
            </div>
            <div class="stat-card__content">
              <div class="stat-card__label">设备总数</div>
              <div class="stat-card__value" style="color: #0984e3;">{{ loadDevices.length }}</div>
            </div>
          </div>
          <div class="stat-card" style="--card-accent: #e74c3c; border-left-color: #e74c3c;">
            <div class="stat-card__icon" style="background: rgba(231,76,60,0.1);">
              <el-icon :size="20" color="#e74c3c"><Warning /></el-icon>
            </div>
            <div class="stat-card__content">
              <div class="stat-card__label">过载设备</div>
              <div class="stat-card__value" style="color: #e74c3c;">{{ overloadCount }}</div>
            </div>
          </div>
          <div class="stat-card" style="--card-accent: #8d9db0; border-left-color: #8d9db0;">
            <div class="stat-card__icon" style="background: rgba(141,157,176,0.1);">
              <el-icon :size="20" color="#8d9db0"><Timer /></el-icon>
            </div>
            <div class="stat-card__content">
              <div class="stat-card__label">低负载设备</div>
              <div class="stat-card__value" style="color: #8d9db0;">{{ lowLoadCount }}</div>
            </div>
          </div>
          <div class="stat-card" :style="{'--card-accent': systemLoadColor, 'border-left-color': systemLoadColor}">
            <div class="stat-card__icon" :style="{background: systemLoadColor + '1a'}">
              <el-icon :size="20" :color="systemLoadColor"><Odometer /></el-icon>
            </div>
            <div class="stat-card__content">
              <div class="stat-card__label">系统负载率</div>
              <div class="stat-card__value" :style="{color: systemLoadColor}">{{ systemLoadRate }}%</div>
              <el-progress :percentage="systemLoadRate" :color="systemLoadColor" :show-text="false" class="stat-card__progress" />
            </div>
          </div>
        </div>

        <el-card class="section-card mb-md">
          <div ref="heatmapChart" class="chart-area chart-area--heatmap"></div>
        </el-card>

        <el-card class="section-card mb-md">
          <div class="filter-bar filter-bar--compact">
            <el-radio-group v-model="loadFilter" size="small">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="overload">过载</el-radio-button>
              <el-radio-button value="low">低负载</el-radio-button>
              <el-radio-button value="normal">正常</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="filteredLoadDevices" stripe class="data-table">
            <el-table-column prop="deviceName" label="设备名称" width="140" />
            <el-table-column label="使用次数" width="90">
              <template #default="{row}">{{ row.usageCount || 0 }} 次</template>
            </el-table-column>
            <el-table-column label="使用时长" width="100">
              <template #default="{row}">{{ row.totalHours?.toFixed(1) || '0.0' }} h</template>
            </el-table-column>
            <el-table-column label="总能耗" width="110">
              <template #default="{row}">{{ row.totalEnergy?.toFixed(1) || '0.0' }} kWh</template>
            </el-table-column>
            <el-table-column label="利用率" width="160">
              <template #default="{row}">
                <el-progress :percentage="Math.min(row.loadFactor * 100, 100)" :color="getProgressColor(row.loadFactor)" :stroke-width="14" :text-inside="true" />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{row}">
                <el-tag :type="getStatusType(row.loadFactor)" size="small">{{ getStatusText(row.loadFactor) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="suggestion" label="建议" min-width="160" />
          </el-table>
        </el-card>

        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-header__title">调度建议</span>
            </div>
          </template>
          <el-timeline v-if="balancingTips.length">
            <el-timeline-item v-for="(t,i) in balancingTips" :key="i" :type="t.type">
              <div class="timeline-text">{{ t.text }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无建议" :image-size="60" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { energyApi } from '../../api'
import { useDataSync } from '../../composables/useDataSync'

const activeTab = ref('overview')

const { refreshKey } = useDataSync()
watch(refreshKey, () => {
  if (activeTab.value === 'overview') loadOverview()
})

const formatNum = (val) => {
  if (val === null || val === undefined) return '0'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ==================== Tab 1: 总览 ====================
const dateRange = ref([])
const statType = ref('DAILY')
const enableComparison = ref(false)
const trendChart = ref()
const deviceChart = ref()
const trendInstance = ref(null)
const deviceInstance = ref(null)
const envMetrics = reactive({ totalEnergy: 0, co2Reduction: 0, coalSaved: 0, treeEquivalent: 0, co2Unit: 'kg', coalUnit: 'kgce', treeUnit: '棵' })
const timeOfUsePrices = ref([
  { label: '低谷电价', price: '0.45', time: '22:00 - 06:00', color: '#67c23a' },
  { label: '平段电价', price: '0.65', time: '06:00-08:00, 12:00-18:00', color: '#409eff' },
  { label: '高峰电价', price: '0.85', time: '08:00-12:00, 18:00-22:00', color: '#f56c6c' }
])

const overviewCards = computed(() => [
  { label: '总能耗', value: formatNum(envMetrics.totalEnergy), unit: 'kWh', color: '#409eff' },
  { label: '总费用', value: formatNum(totalCost.value), unit: '元', color: '#e6a23c' },
  { label: 'CO₂减排', value: formatNum(envMetrics.co2Reduction), unit: envMetrics.co2Unit, color: '#67c23a' },
  { label: '节煤量', value: formatNum(envMetrics.coalSaved), unit: envMetrics.coalUnit, color: '#409eff' },
  { label: '等效植树', value: envMetrics.treeEquivalent, unit: envMetrics.treeUnit + '/年', color: '#e6a23c' },
  { label: '日均能耗', value: formatNum(dailyAvgEnergy.value), unit: 'kWh/天', color: '#f56c6c' }
])

const totalCost = ref(0)
const dailyAvgEnergy = ref(0)

function setDefaultDate(type) {
  const now = new Date()
  const endDate = now.toISOString().slice(0, 10)
  let startDate
  switch (type) {
    case 'DAILY': startDate = endDate; break
    case 'WEEKLY':
      const ws = new Date(now); ws.setDate(ws.getDate() - 6); startDate = ws.toISOString().slice(0, 10); break
    case 'MONTHLY':
      const ms = new Date(now); ms.setDate(ms.getDate() - 29); startDate = ms.toISOString().slice(0, 10); break
    default: startDate = endDate
  }
  dateRange.value = [startDate, endDate]
}

async function loadOverview() {
  if (!dateRange.value?.length) return
  const [startDate, endDate] = dateRange.value
  try {
    const calls = []
    if (enableComparison.value) {
      calls.push(energyApi.comparison({ statType: statType.value, startDate, endDate }))
    } else {
      calls.push(energyApi.trend({ statType: statType.value, startDate, endDate }))
    }
    calls.push(energyApi.byDevice({ startDate, endDate }))

    const [trendRes, deviceRes] = await Promise.all(calls)

    let totalEnergy = 0
    let totalC = 0
    if (enableComparison.value) {
      const data = trendRes.data?.current || []
      data.forEach(d => { totalEnergy += Number(d.totalEnergy || 0); totalC += Number(d.totalCost || 0) })
      renderComparisonChart(trendRes.data)
    } else {
      const data = trendRes.data || []
      data.forEach(d => { totalEnergy += Number(d.totalEnergy || 0); totalC += Number(d.totalCost || 0) })
      renderTrend(data)
    }
    totalCost.value = totalC

    const days = Math.max(1, Math.ceil((new Date(endDate) - new Date(startDate)) / 86400000) + 1)
    dailyAvgEnergy.value = totalEnergy / days

    renderDevice(deviceRes.data || [])

    if (totalEnergy > 0) {
      const envRes = await energyApi.environmentalMetrics({ totalEnergy })
      Object.assign(envMetrics, envRes.data || {})
    }

    const priceRes = await energyApi.timeOfUsePrices()
    if (priceRes.data) {
      const d = priceRes.data
      timeOfUsePrices.value = [
        { label: '低谷电价', price: d.offPeak || '0.45', time: '22:00 - 06:00', color: '#67c23a' },
        { label: '平段电价', price: d.midPeak || '0.65', time: '06:00-08:00, 12:00-18:00', color: '#409eff' },
        { label: '高峰电价', price: d.peak || '0.85', time: '08:00-12:00, 18:00-22:00', color: '#f56c6c' }
      ]
    }
  } catch {}
}

function handleStatTypeChange() {
  setDefaultDate(statType.value)
  loadOverview()
}

function renderTrend(data) {
  if (!trendChart.value) return
  if (!trendInstance.value) trendInstance.value = echarts.init(trendChart.value)
  trendInstance.value.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8' },
    title: { text: '能耗趋势', left: 'center', top: 8, textStyle: { color: '#f1f5f9' } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['能耗(kWh)', '费用(元)'], top: 35, left: 'center', textStyle: { color: '#94a3b8' } },
    grid: { left: 60, right: 60, bottom: 60, top: 62 },
    xAxis: { type: 'category', data: data.map(d => d.statDate || d.stat_date), axisLabel: { rotate: 30 } },
    yAxis: [{ type: 'value', name: 'kWh' }, { type: 'value', name: '元' }],
    series: [
      { name: '能耗(kWh)', type: 'bar', data: data.map(d => Number(d.totalEnergy || 0)),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#79bbff' }]), borderRadius: [6, 6, 0, 0] } },
      { name: '费用(元)', type: 'line', yAxisIndex: 1, data: data.map(d => Number(d.totalCost || 0)),
        itemStyle: { color: '#e6a23c' }, smooth: true }
    ]
  }, true)
}

function renderComparisonChart(data) {
  if (!trendChart.value) return

  const current = data?.current || []
  const previous = data?.previous || []
  const currDates = current.map(d => d.statDate || d.stat_date)

  if (!trendInstance.value) trendInstance.value = echarts.init(trendChart.value)

  const prevMap = {}
  previous.forEach(d => { prevMap[d.statDate || d.stat_date] = Number(d.totalEnergy || 0) })

  trendInstance.value.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8' },
    title: { text: '能耗趋势（对比上期）', left: 'center', top: 8, textStyle: { color: '#f1f5f9' } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['本期能耗', '上期能耗', '本期费用'], top: 35, left: 'center', textStyle: { color: '#94a3b8' } },
    grid: { left: 60, right: 60, bottom: 60, top: 62 },
    xAxis: { type: 'category', data: currDates, axisLabel: { rotate: 30 } },
    yAxis: [{ type: 'value', name: 'kWh' }, { type: 'value', name: '元' }],
    series: [
      { name: '本期能耗', type: 'bar', data: current.map(d => Number(d.totalEnergy || 0)),
        itemStyle: { color: '#409eff', borderRadius: [6, 6, 0, 0] } },
      { name: '上期能耗', type: 'bar', data: currDates.map(d => prevMap[d] || '-'),
        itemStyle: { color: '#334155', borderRadius: [6, 6, 0, 0] } },
      { name: '本期费用', type: 'line', yAxisIndex: 1, data: current.map(d => Number(d.totalCost || 0)),
        itemStyle: { color: '#e6a23c' }, smooth: true }
    ]
  }, true)
}

function renderDevice(data) {
  if (!deviceChart.value) return

  const names = data.map(d => d.deviceName || d.device_name).reverse()
  const energy = data.map(d => Number(d.totalEnergy || 0)).reverse()
  const cost = data.map(d => Number(d.totalCost || 0)).reverse()

  if (!deviceInstance.value) {
    deviceInstance.value = echarts.init(deviceChart.value)
    deviceInstance.value.setOption({
      backgroundColor: 'transparent',
      textStyle: { color: '#94a3b8' },
      title: { text: '设备能耗排行', left: 'center', top: 8, textStyle: { color: '#f1f5f9' } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['能耗(kWh)', '费用(元)'], top: 35, left: 'center', textStyle: { color: '#94a3b8' } },
      grid: { left: 130, right: 40, bottom: 30, top: 62 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category' },
      series: [
        { name: '能耗(kWh)', type: 'bar', itemStyle: { color: '#67c23a', borderRadius: [0, 6, 6, 0] } },
        { name: '费用(元)', type: 'bar', itemStyle: { color: '#e6a23c', borderRadius: [0, 6, 6, 0] } }
      ]
    })
  }

  deviceInstance.value.setOption({
    yAxis: { data: names },
    series: [
      { data: energy },
      { data: cost }
    ]
  })
}


// ==================== Tab 3: 均衡 ====================
const loadDevices = ref([])
const loadFilter = ref('all')
const balancingLoading = ref(false)
const balancingUpdateTime = ref('')
const heatmapChart = ref()
const heatmapInstance = ref(null)

const overloadCount = computed(() => loadDevices.value.filter(d => d.loadFactor > 0.8).length)
const lowLoadCount = computed(() => loadDevices.value.filter(d => d.loadFactor < 0.3).length)
const systemLoadRate = computed(() => {
  if (!loadDevices.value.length) return 0
  const total = loadDevices.value.reduce((s, d) => s + (d.loadFactor || 0), 0)
  return Math.round((total / loadDevices.value.length) * 100)
})
const systemLoadColor = computed(() => {
  if (systemLoadRate.value > 80) return '#f56c6c'
  if (systemLoadRate.value < 30) return '#909399'
  return '#67c23a'
})

const filteredLoadDevices = computed(() => {
  if (loadFilter.value === 'all') return loadDevices.value
  if (loadFilter.value === 'overload') return loadDevices.value.filter(d => d.loadFactor > 0.8)
  if (loadFilter.value === 'low') return loadDevices.value.filter(d => d.loadFactor < 0.3)
  return loadDevices.value.filter(d => d.loadFactor >= 0.3 && d.loadFactor <= 0.8)
})

const balancingTips = computed(() => {
  const tips = []
  if (overloadCount.value > 0) {
    tips.push({ text: `当前有 ${overloadCount.value} 台设备过载，建议将部分负载转移到低负载设备`, type: 'danger' })
    const overloadDevices = loadDevices.value.filter(d => d.loadFactor > 0.8)
    overloadDevices.forEach(d => tips.push({ text: `${d.deviceName} 负载率 ${(d.loadFactor * 100).toFixed(0)}%，${d.suggestion || '建议降低负载'}`, type: 'warning' }))
  }
  if (lowLoadCount.value > 0) {
    tips.push({ text: `当前有 ${lowLoadCount.value} 台设备低负载运行，可考虑合并负载以提升效率`, type: 'primary' })
  }
  if (overloadCount.value === 0 && lowLoadCount.value === 0) {
    tips.push({ text: '系统负载均衡，各设备运行状态良好', type: 'success' })
  }
  return tips
})

async function loadBalancing() {
  balancingLoading.value = true
  try {
    const res = await energyApi.loadBalancing()
    loadDevices.value = (res.data || []).sort((a, b) => (b.loadFactor || 0) - (a.loadFactor || 0))
    balancingUpdateTime.value = new Date().toLocaleTimeString()
    await nextTick()
    renderHeatmap()
  } catch {} finally {
    balancingLoading.value = false
  }
}

function renderHeatmap() {
  if (heatmapInstance.value) heatmapInstance.value.dispose()
  heatmapInstance.value = echarts.init(heatmapChart.value)
  const devices = loadDevices.value
  if (!devices.length) return

  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const data = []
  devices.forEach((d, di) => {
    hours.forEach((_, hi) => {
      const val = Math.max(0, Math.min(1, d.loadFactor || 0))
      data.push([hi, di, val])
    })
  })

  heatmapInstance.value.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8' },
    title: { text: '24小时设备利用率', left: 'center', textStyle: { color: '#f1f5f9' } },
    tooltip: { formatter: p => `${devices[p.data[1]]?.deviceName || ''}<br/>过去24h利用率: ${(p.data[2] * 100).toFixed(0)}%` },
    grid: { left: 120, right: 60, bottom: 40, top: 50 },
    xAxis: { type: 'category', data: hours, splitArea: { show: true } },
    yAxis: { type: 'category', data: devices.map(d => d.deviceName), splitArea: { show: true } },
    visualMap: { min: 0, max: 1, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#0e1525', '#1e3a5f', '#409eff', '#faad14', '#ff4d4f'] } },
    series: [{ type: 'heatmap', data, label: { show: false }, emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } } }]
  })
}

function getProgressColor(factor) {
  if (factor > 0.8) return '#f56c6c'
  if (factor <= 0) return '#c0c4cc'
  if (factor < 0.3) return '#909399'
  return '#67c23a'
}
function getStatusType(factor) {
  if (factor > 0.8) return 'danger'
  if (factor <= 0) return 'info'
  if (factor < 0.3) return 'warning'
  return 'success'
}
function getStatusText(factor) {
  if (factor > 0.8) return '过载'
  if (factor <= 0) return '未使用'
  if (factor < 0.3) return '低负载'
  return '正常'
}

// ==================== 生命周期 ====================
function handleTabChange(tab) {
  setTimeout(() => {
    if (tab === 'overview') {
      loadOverview()
    } else if (tab === 'balancing') {
      loadBalancing()
    }
  }, 100)
}

onMounted(async () => {
  setDefaultDate('DAILY')
  await loadOverview()
})

onUnmounted(() => {
  trendInstance.value?.dispose()
  deviceInstance.value?.dispose()
  heatmapInstance.value?.dispose()
})
</script>

<style scoped>
.energy-page {
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
  box-shadow: var(--shadow-md);
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
  background: linear-gradient(180deg, var(--primary) 0%, var(--accent) 100%);
  border-radius: 2px 0 0 2px;
}

.page-header::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 100%;
  background: radial-gradient(ellipse at right center, var(--primary-bg) 0%, transparent 70%);
  pointer-events: none;
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
  letter-spacing: 0.5px;
}

.page-subtitle {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  font-weight: 400;
}

.energy-tabs :deep(.el-tabs__header) {
  background: var(--bg-card);
  border-bottom: 2px solid var(--border-light);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  padding: 0 16px;
  margin-bottom: 20px;
}

.energy-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.energy-tabs :deep(.el-tabs__item) {
  font-family: var(--font-display);
  font-weight: 500;
  font-size: 15px;
  color: var(--text-secondary);
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
  transition: color var(--transition-normal);
}

.energy-tabs :deep(.el-tabs__item:hover) {
  color: var(--primary);
}

.energy-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary);
  font-weight: 600;
}

.energy-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary);
  height: 3px;
  border-radius: 3px 3px 0 0;
  box-shadow: 0 0 8px var(--primary-bg);
}

.energy-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.tab-label {
  font-family: var(--font-display);
  letter-spacing: 0.3px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal), background var(--transition-normal);
  border-top: 1px solid var(--border-light);
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}

.stat-card:hover {
  background: var(--bg-card-hover);
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.stat-card__icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-card__label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-card__value {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card__unit {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
  font-family: var(--font-mono);
}

.stat-card__progress {
  margin-top: 6px;
}

.price-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.price-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal), background var(--transition-normal);
  border-top: 1px solid var(--border-light);
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}

.price-card:hover {
  background: var(--bg-card-hover);
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.price-card__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.price-card__body {
  display: flex;
  flex-direction: column;
}

.price-card__label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  margin-bottom: 4px;
}

.price-card__price {
  font-family: var(--font-mono);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.price-card__price small {
  font-size: 13px;
  font-weight: 500;
  opacity: 0.6;
  font-family: var(--font-body);
}

.price-card__time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  font-family: var(--font-mono);
}

.balancing-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.section-card {
  border-radius: var(--radius-md) !important;
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: var(--shadow-md) !important;
}

.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-elevated);
}

.section-card :deep(.el-card__body) {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header__title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-bar--compact {
  margin-bottom: 12px;
}

.date-picker {
  width: 300px;
}

.filter-select {
  width: 200px;
}

.filter-switch {
  margin-left: 12px;
}

.filter-hint {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.chart-area {
  width: 100%;
  background: transparent;
  border-radius: var(--radius-sm);
}

.chart-area--trend {
  height: 380px;
}

.chart-area--heatmap {
  height: 300px;
}

.timeline-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.data-table {
  width: 100%;
}

.data-table :deep(.el-table__header th) {
  background: var(--bg-elevated) !important;
  color: var(--text-secondary) !important;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-color) !important;
}

.data-table :deep(.el-table__body tr) {
  background: var(--bg-card) !important;
  color: var(--text-primary) !important;
  transition: background var(--transition-fast);
}

.data-table :deep(.el-table__body tr:hover > td) {
  background: var(--bg-hover) !important;
}

.data-table :deep(.el-table__body tr.el-table__row--striped td) {
  background: var(--bg-elevated) !important;
}

.data-table :deep(.el-table__body td) {
  border-bottom: 1px solid var(--border-light) !important;
  color: var(--text-primary) !important;
  font-family: var(--font-body);
  font-size: 13px;
}

.data-table :deep(.el-table__empty-text) {
  color: var(--text-muted);
}

.data-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.data-table :deep(.el-table) {
  background: var(--bg-card) !important;
  color: var(--text-primary) !important;
  --el-table-bg-color: var(--bg-card) !important;
  --el-table-tr-bg-color: var(--bg-card) !important;
  --el-table-header-bg-color: var(--bg-elevated) !important;
  --el-table-row-hover-bg-color: var(--bg-hover) !important;
  --el-table-border-color: var(--border-light) !important;
  --el-table-text-color: var(--text-primary) !important;
  --el-table-header-text-color: var(--text-secondary) !important;
}

.mb-md {
  margin-bottom: 16px;
}

.section-card {
  overflow: visible !important;
}
.section-card .el-card__body {
  overflow: visible !important;
}

/* Element Plus dark overrides */
.energy-page :deep(.el-date-editor) {
  --el-datepicker-border-color: var(--border-color);
  --el-fill-color-blank: var(--bg-elevated);
  --el-text-color-regular: var(--text-primary);
  --el-text-color-placeholder: var(--text-placeholder);
  --el-border-color: var(--border-color);
  --el-border-color-hover: var(--border-hover);
}

.energy-page :deep(.el-input__wrapper) {
  background: var(--bg-elevated) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

.energy-page :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover) inset !important;
}

.energy-page :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
}

.energy-page :deep(.el-input__inner) {
  color: var(--text-primary) !important;
}

.energy-page :deep(.el-radio-group .el-radio-button__inner) {
  background: var(--bg-elevated);
  color: var(--text-secondary);
  border-color: var(--border-color);
  transition: all var(--transition-fast);
}

.energy-page :deep(.el-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--primary-bg);
  color: var(--primary);
  border-color: var(--primary);
  box-shadow: -1px 0 0 0 var(--primary);
}

.energy-page :deep(.el-switch) {
  --el-switch-on-color: var(--primary);
  --el-switch-off-color: var(--bg-elevated);
}

.energy-page :deep(.el-switch__label) {
  color: var(--text-muted);
}

.energy-page :deep(.el-button) {
  --el-button-bg-color: var(--bg-elevated);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: var(--bg-hover);
  --el-button-hover-border-color: var(--border-hover);
  --el-button-hover-text-color: var(--primary);
}

.energy-page :deep(.el-progress-bar__outer) {
  background: var(--bg-elevated) !important;
}

.energy-page :deep(.el-tag) {
  border: none;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.3px;
}

.energy-page :deep(.el-timeline-item__node) {
  border-color: var(--primary);
}

.energy-page :deep(.el-timeline-item__wrapper) {
  padding-left: 20px;
}

.energy-page :deep(.el-empty__description p) {
  color: var(--text-muted);
}

.energy-page :deep(.el-table__fixed-right::before),
.energy-page :deep(.el-table__fixed::before) {
  background: var(--border-light);
}

/* Glow effect on active bar */
.energy-tabs :deep(.el-tabs__active-bar) {
  box-shadow: 0 0 12px rgba(34, 197, 94, 0.3), 0 0 4px rgba(34, 197, 94, 0.6);
}

/* Stat card progress bar dark styling */
.stat-card :deep(.el-progress-bar__outer) {
  background: var(--bg-elevated) !important;
  border-radius: var(--radius-xs);
}

.stat-card :deep(.el-progress-bar__inner) {
  border-radius: var(--radius-xs);
}
</style>
