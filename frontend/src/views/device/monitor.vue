<template>
  <div class="monitor-page">
    <!-- 设备状态总览 -->
    <div class="stat-row">
      <div
        v-for="(stat, index) in statusStats"
        :key="stat.key"
        :class="['stat-card', `stat-card--${stat.key}`, 'animate-fade-in-up', `stagger-${index + 1}`]"
      >
        <div :class="['stat-card__icon', `stat-card__icon--${stat.key}`]">
          <el-icon :size="20">
            <Monitor v-if="stat.key === 'total'" />
            <CircleCheck v-else-if="stat.key === 'online'" />
            <WarningFilled v-else-if="stat.key === 'fault'" />
            <CircleClose v-else-if="stat.key === 'offline'" />
            <Bell v-else-if="stat.key === 'alarm'" />
          </el-icon>
        </div>
        <div class="stat-card__content">
          <div :class="['stat-card__value', `stat-card__value--${stat.key}`]">{{ stat.value }}</div>
          <div class="stat-card__label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 多设备实时看板 + 告警消息流 -->
    <el-row :gutter="12">
      <el-col :xs="24" :sm="24" :md="16" class="board-col">
        <el-card class="equal-height-card board-card">
          <template #header>
            <div class="section-header">
              <span class="section-header__title">设备实时看板</span>
              <el-tag type="success" effect="dark" size="small" v-if="wsConnected">实时</el-tag>
              <el-tag type="danger" effect="dark" size="small" v-else>已断开</el-tag>
            </div>
          </template>
          <div class="scrollable-body">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="12" :md="12" :lg="8" v-for="dev in deviceList" :key="dev.id" class="device-col">
                <div
                  :class="['device-card', { 'device-card--selected': selectedDeviceId === dev.id }]"
                  @click="selectDevice(dev)"
                >
                  <div class="device-card__header">
                    <span class="device-card__name">{{ dev.deviceName }}</span>
                    <el-tag :type="getStatusTag(dev.status)" size="small" effect="dark">{{ getStatusText(dev.status) }}</el-tag>
                  </div>
                  <el-row :gutter="8">
                    <el-col :span="12">
                      <div class="metric-item">
                        <span class="metric-label">电压</span>
                        <span class="metric-value metric-value--accent">{{ dev.voltage ?? '-' }}</span>
                        <span class="metric-unit">V</span>
                      </div>
                      <div class="metric-item">
                        <span class="metric-label">电流</span>
                        <span class="metric-value metric-value--primary">{{ dev.currentVal ?? '-' }}</span>
                        <span class="metric-unit">A</span>
                      </div>
                      <div class="metric-item">
                        <span class="metric-label">功率</span>
                        <span class="metric-value metric-value--warning">{{ dev.power ?? '-' }}</span>
                        <span class="metric-unit">kW</span>
                      </div>
                    </el-col>
                    <el-col :span="12">
                      <div class="metric-item">
                        <span class="metric-label">温度</span>
                        <span :class="['metric-value', dev.temperature > 50 ? 'metric-value--danger' : 'metric-value--default']">{{ dev.temperature ?? '-' }}</span>
                        <span class="metric-unit">℃</span>
                      </div>
                      <div class="metric-item">
                        <span class="metric-label">功率因数</span>
                        <span class="metric-value metric-value--purple">{{ dev.powerFactor ?? '-' }}</span>
                        <span class="metric-unit"></span>
                      </div>
                      <div class="metric-item">
                        <span class="metric-label">频率</span>
                        <span class="metric-value metric-value--teal">{{ dev.frequency ?? '-' }}</span>
                        <span class="metric-unit">Hz</span>
                      </div>
                    </el-col>
                  </el-row>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" class="board-col">
        <el-card class="equal-height-card board-card">
          <template #header>
            <div class="section-header">
              <span class="section-header__title">实时告警</span>
              <el-badge :value="alarmFeed.length" :max="99" v-if="alarmFeed.length > 0" />
            </div>
          </template>
          <div class="scrollable-body">
            <div v-if="alarmFeed.length === 0" class="alarm-empty">
              <el-icon :size="32"><BellFilled /></el-icon>
              <span>暂无告警</span>
            </div>
            <div
              v-for="(alarm, idx) in alarmFeed"
              :key="idx"
              :class="['alarm-item', alarm.alarmLevel === 'CRITICAL' ? 'alarm-item--critical' : 'alarm-item--warning']"
            >
              <div class="alarm-item__header">
                <span :class="['alarm-item__dot', alarm.alarmLevel === 'CRITICAL' ? 'alarm-item__dot--critical' : 'alarm-item__dot--warning']"></span>
                <el-tag :type="alarm.alarmLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small" effect="dark">
                  {{ alarm.alarmLevel === 'CRITICAL' ? '严重' : '警告' }}
                </el-tag>
                <span class="alarm-item__time">{{ alarm.alarmTime || alarm.createTime }}</span>
              </div>
              <div class="alarm-item__content">{{ alarm.alarmContent }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选中设备详情 -->
    <el-card class="detail-card" v-if="selectedDevice">
      <template #header>
        <div class="detail-header">
          <div class="detail-header__left">
            <span class="detail-header__name">{{ selectedDevice.deviceName }}</span>
            <el-tag :type="getStatusTag(selectedDevice.status)" size="small">{{ getStatusText(selectedDevice.status) }}</el-tag>
            <span class="detail-header__rated">额定: {{ selectedDevice.ratedPower || '-' }}kW / {{ selectedDevice.ratedVoltage || '-' }}V / {{ selectedDevice.ratedCurrent || '-' }}A</span>
          </div>
          <div class="detail-header__right">
            <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
              <el-radio-button value="1">1小时</el-radio-button>
              <el-radio-button value="6">6小时</el-radio-button>
              <el-radio-button value="24">24小时</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="24" :md="16">
          <div ref="trendChart" class="trend-chart" role="img" aria-label="设备运行趋势图"></div>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <el-row :gutter="8">
            <el-col :xs="12" :sm="12" :md="12" v-for="item in gaugeItems" :key="item.key" class="gauge-col">
              <div class="gauge-card">
                <div :ref="el => setGaugeRef(item.key, el)" class="gauge-card__chart"></div>
                <div class="gauge-card__label">{{ item.label }}</div>
              </div>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useChartResize } from '../../composables/useChartResize'
import echarts from '../../utils/echarts'
import { CHART_COLORS, CHART_TEXT, CHART_LABEL, CHART_SPLIT_LINE, CHART_TRACK } from '../../utils/chartTheme'
import { DEVICE_STATUS, statusMeta } from '../../utils/status'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { deviceApi, alarmApi, systemConfigApi } from '../../api'

const deviceList = ref([])
const selectedDeviceId = ref(null)
const selectedDevice = ref(null)
const trendRange = ref('1')
const trendChart = ref(null)
const alarmFeed = ref([])
const wsConnected = ref(false)
const pendingAlarmCount = ref(0)

const gaugeRefsMap = reactive({})
const gaugeCharts = {}
let trendChartInstance = null
let stompClient = null
let lastDeviceData = {}
let pollTimer = null
let trendPollTimer = null
let pollingStartTimer = null
let statusPollTimer = null
let wsSilentTimer = null
let wsReconnectTimer = null
let pollingInterval = 3000

async function loadPollingConfig() {
  try {
    const res = await systemConfigApi.publicConfig()
    pollingInterval = res.data?.pollingInterval || 3000
  } catch {
    pollingInterval = 3000
  }
}

const setGaugeRef = (key, el) => {
  if (el) gaugeRefsMap[key] = el
}

const statusStats = reactive([
  { key: 'total', label: '总设备', value: 0 },
  { key: 'online', label: '在线', value: 0 },
  { key: 'fault', label: '故障', value: 0 },
  { key: 'offline', label: '离线', value: 0 },
  { key: 'alarm', label: '待处理告警', value: 0 }
])

const getStatusTag = (status) => statusMeta(DEVICE_STATUS, status).type
const getStatusText = (status) => statusMeta(DEVICE_STATUS, status).label

const gaugeItems = computed(() => {
  const dev = selectedDevice.value
  const data = lastDeviceData[dev?.id] || {}
  return [
    { key: 'voltage', label: '电压(V)', max: (dev?.ratedVoltage || 500) * 1.2, color: CHART_COLORS.primary, val: data.voltage },
    { key: 'currentVal', label: '电流(A)', max: (dev?.ratedCurrent || 500) * 1.2, color: CHART_COLORS.success, val: data.currentVal },
    { key: 'power', label: '功率(kW)', max: (dev?.ratedPower || 300) * 1.2, color: CHART_COLORS.warning, val: data.power },
    { key: 'temperature', label: '温度(℃)', max: 100, color: CHART_COLORS.danger, val: data.temperature },
    { key: 'powerFactor', label: '功率因数', max: 1, color: CHART_COLORS.purple, val: data.powerFactor },
    { key: 'frequency', label: '频率(Hz)', max: 55, min: 45, color: CHART_COLORS.accent, val: data.frequency }
  ]
})

function applyDeviceData(data) {
  if (!data?.deviceId) return
  const dev = deviceList.value.find(d => d.id === data.deviceId)
  if (!dev) return
  dev.voltage = data.voltage ?? '-'
  dev.currentVal = data.currentVal ?? '-'
  dev.power = data.power ?? '-'
  dev.temperature = data.temperature ?? '-'
  dev.powerFactor = data.powerFactor ?? '-'
  dev.frequency = data.frequency ?? '-'
  lastDeviceData[data.deviceId] = { ...data }
}

async function pollLatestData() {
  try {
    const res = await deviceApi.latestAll()
    const list = res.data || []
    list.forEach(item => applyDeviceData(item))
    if (selectedDeviceId.value && lastDeviceData[selectedDeviceId.value]) {
      updateGauges()
    }
  } catch {}
}

async function pollDeviceStatus() {
  try {
    const res = await deviceApi.list()
    const list = res.data || []
    let changed = false
    list.forEach(item => {
      const dev = deviceList.value.find(d => d.id === item.id)
      if (dev && dev.status !== item.status) {
        dev.status = item.status
        changed = true
      }
    })
    if (changed) updateStatusStats()
  } catch {}
}

async function pollTrend() {
  if (!selectedDeviceId.value) return
  try {
    const res = await deviceApi.trend(selectedDeviceId.value, Number(trendRange.value))
    renderTrend(res.data || [])
  } catch {}
}

function startPolling() {
  if (pollTimer) return // 幂等：已有轮询则不重复启动（避免断线重连叠加定时器）
  pollLatestData()
  pollDeviceStatus()
  pollTrend()
  pollTimer = setInterval(pollLatestData, pollingInterval)
  trendPollTimer = setInterval(pollTrend, 30000)
}

function startStatusPolling() {
  if (statusPollTimer) return // 幂等
  pollDeviceStatus()
  statusPollTimer = setInterval(pollDeviceStatus, 10000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  if (trendPollTimer) { clearInterval(trendPollTimer); trendPollTimer = null }
  if (pollingStartTimer) { clearTimeout(pollingStartTimer); pollingStartTimer = null }
  if (statusPollTimer) { clearInterval(statusPollTimer); statusPollTimer = null }
  if (wsSilentTimer) { clearTimeout(wsSilentTimer); wsSilentTimer = null }
  if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null }
}

function initWebSocket() {
  const token = sessionStorage.getItem('token')

  const resetSilentTimer = () => {
    clearTimeout(wsSilentTimer)
    wsSilentTimer = setTimeout(() => {
      if (wsConnected.value) startPolling()
    }, 15000)
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      wsConnected.value = true
      stopPolling()
      resetSilentTimer()

      stompClient.subscribe('/topic/device-data', msg => {
        const data = JSON.parse(msg.body)
        if (Array.isArray(data)) {
          data.forEach(item => applyDeviceData(item))
          if (selectedDeviceId.value && lastDeviceData[selectedDeviceId.value]) {
            updateGauges()
          }
          resetSilentTimer()
        }
      })
      stompClient.subscribe('/topic/alarm', msg => {
        const alarm = JSON.parse(msg.body)
        alarmFeed.value.unshift({
          ...alarm,
          alarmTime: alarm.alarmTime || alarm.createTime || new Date().toLocaleString()
        })
        if (alarmFeed.value.length > 100) alarmFeed.value.length = 100
        pendingAlarmCount.value++
        statusStats.find(s => s.key === 'alarm').value = pendingAlarmCount.value
      })
      stompClient.subscribe('/topic/device-status', msg => {
        const update = JSON.parse(msg.body)
        const dev = deviceList.value.find(d => d.id === update.deviceId)
        if (dev) {
          dev.status = update.status
          updateStatusStats()
        }
      })
      stompClient.subscribe('/topic/alarm-resolved', () => {
        pendingAlarmCount.value = Math.max(0, pendingAlarmCount.value - 1)
        statusStats.find(s => s.key === 'alarm').value = pendingAlarmCount.value
      })
    },
    onDisconnect: () => {
      wsConnected.value = false
      clearTimeout(wsSilentTimer)
      console.warn('[monitor] WebSocket 已断开，启用 HTTP 轮询兜底（断线原因见上方 STOMP 错误/网络面板）')
      startStatusPolling()
      wsReconnectTimer = setTimeout(() => {
        if (!wsConnected.value) startPolling()
      }, 5000)
    },
    onStompError: frame => {
      wsConnected.value = false
      clearTimeout(wsSilentTimer)
      // 打印拒绝原因（如"无权订阅此主题"/"Token已失效"），方便定位
      console.warn('[monitor] STOMP 错误:', frame?.headers?.message || frame?.body || frame)
      startStatusPolling()
      wsReconnectTimer = setTimeout(() => {
        if (!wsConnected.value) startPolling()
      }, 5000)
    }
  })
  stompClient.activate()
  pollingStartTimer = setTimeout(() => {
    if (!wsConnected.value) startPolling()
  }, 3000)
}

function updateStatusStats() {
  const list = deviceList.value
  statusStats.find(s => s.key === 'total').value = list.length
  statusStats.find(s => s.key === 'online').value = list.filter(d => d.status === 'ONLINE' || d.status === 'IN_USE').length
  statusStats.find(s => s.key === 'fault').value = list.filter(d => d.status === 'FAULT').length
  statusStats.find(s => s.key === 'offline').value = list.filter(d => d.status === 'OFFLINE').length
}

function selectDevice(dev) {
  selectedDeviceId.value = dev.id
  selectedDevice.value = dev
  nextTick(() => {
    initGauges(dev.id)
    pollTrend()
  })
}

function initGauges(devId) {
  const data = lastDeviceData[devId] || {}
  Object.values(gaugeCharts).forEach(c => c?.dispose())
  for (const k in gaugeCharts) delete gaugeCharts[k]
  gaugeItems.value.forEach(item => {
    const key = item.key
    const el = gaugeRefsMap[key]
    if (!el) return
    const chart = echarts.init(el)
    gaugeCharts[key] = chart
    chart.setOption({
      backgroundColor: 'transparent',
      textStyle: { color: CHART_LABEL },
      series: [{
        type: 'gauge',
        startAngle: 210, endAngle: -30,
        min: item.min || 0, max: item.max,
        pointer: { show: true, length: '50%', width: 3 },
        progress: { show: true, width: 8, roundCap: true, itemStyle: { color: item.color } },
        axisLine: { lineStyle: { width: 8, color: [[1, CHART_TRACK]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: { fontSize: 14, fontWeight: 'bold', offsetCenter: [0, '55%'], color: item.color,
          formatter: v => (v ?? 0).toFixed(1) },
        data: [{ value: data[key] ?? 0 }]
      }]
    })
  })
}

function updateGauges() {
  const devId = selectedDeviceId.value
  const data = lastDeviceData[devId]
  if (!data) return
  gaugeItems.value.forEach(item => {
    const key = item.key
    const chart = gaugeCharts[key]
    if (chart) {
      chart.setOption({ series: [{ data: [{ value: data[key] ?? 0 }] }] })
    }
  })
}

async function loadDevices() {
  try {
    const res = await deviceApi.list()
    deviceList.value = (res.data || []).map(d => ({
      ...d,
      voltage: '-', currentVal: '-', power: '-', temperature: '-',
      powerFactor: '-', frequency: '-'
    }))
    updateStatusStats()
    if (deviceList.value.length > 0) {
      selectedDeviceId.value = deviceList.value[0].id
      selectedDevice.value = deviceList.value[0]
    }
  } catch {}
}

async function loadTrend() {
  if (!selectedDeviceId.value) return
  try {
    const res = await deviceApi.trend(selectedDeviceId.value, Number(trendRange.value))
    renderTrend(res.data || [])
  } catch {}
}

async function loadAlarmCount() {
  try {
    const res = await alarmApi.pendingCount()
    pendingAlarmCount.value = res.data || 0
    statusStats.find(s => s.key === 'alarm').value = pendingAlarmCount.value
  } catch {}
}

async function loadRecentAlarms() {
  try {
    const res = await alarmApi.page({ pageNum: 1, pageSize: 20, status: 'PENDING' })
    alarmFeed.value = (res.data?.records || []).map(a => ({
      ...a,
      alarmTime: a.alarmTime || a.createTime
    }))
  } catch {}
}

function renderTrend(data) {
  if (!trendChart.value) return

  const timeData = data.map(d => d.time?.substring(11, 16) || d.collectTime?.substring(11, 16) || '')
  const isEmpty = data.length === 0

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value)
    trendChartInstance.setOption({
      backgroundColor: 'transparent',
      textStyle: { color: CHART_TEXT },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: params => {
          // 动态数据必须 HTML 转义，防止存储型 XSS
          const escapeHtml = s => String(s ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
          }[c]))
          let html = `<div style="font-weight:bold;margin-bottom:4px;">${escapeHtml(params[0]?.axisValue || '')}</div>`
          params.forEach(p => {
            html += `<div>${p.marker} ${escapeHtml(p.seriesName)}：<b>${escapeHtml(p.value?.toFixed(1) ?? '-')}</b></div>`
          })
          return html
        }
      },
      legend: { data: ['电压(V)', '电流(A)', '功率(kW)', '温度(℃)', '功率因数'], top: 0 },
      grid: { left: 60, right: 60, bottom: 60, top: 40 },
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, bottom: 8, height: 20 }
      ],
      xAxis: { type: 'category', boundaryGap: false },
      yAxis: [
        { type: 'value', name: '电压/电流/功率', position: 'left', splitLine: { lineStyle: { type: 'dashed', color: CHART_SPLIT_LINE } } },
        { type: 'value', name: '温度/功率因数', position: 'right', splitLine: { show: false } }
      ],
      series: [
        { name: '电压(V)', type: 'line', smooth: true, yAxisIndex: 0, symbol: 'circle', symbolSize: 3,
          lineStyle: { color: CHART_COLORS.primary, width: 1.5 }, itemStyle: { color: CHART_COLORS.primary },
          areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(37,99,235,0.15)' }, { offset: 1, color: 'rgba(37,99,235,0.02)' }] } } },
        { name: '电流(A)', type: 'line', smooth: true, yAxisIndex: 0, symbol: 'circle', symbolSize: 3,
          lineStyle: { color: CHART_COLORS.success, width: 1.5 }, itemStyle: { color: CHART_COLORS.success },
          areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(16,163,74,0.12)' }, { offset: 1, color: 'rgba(16,163,74,0.02)' }] } } },
        { name: '功率(kW)', type: 'line', smooth: true, yAxisIndex: 0, symbol: 'circle', symbolSize: 3,
          lineStyle: { color: CHART_COLORS.warning, width: 1.5 }, itemStyle: { color: CHART_COLORS.warning },
          areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(245,158,11,0.12)' }, { offset: 1, color: 'rgba(245,158,11,0.02)' }] } } },
        { name: '温度(℃)', type: 'line', smooth: true, yAxisIndex: 1, symbol: 'diamond', symbolSize: 4,
          lineStyle: { color: CHART_COLORS.danger, width: 1.5, type: 'dashed' }, itemStyle: { color: CHART_COLORS.danger } },
        { name: '功率因数', type: 'line', smooth: true, yAxisIndex: 1, symbol: 'triangle', symbolSize: 4,
          lineStyle: { color: CHART_COLORS.purple, width: 1.5, type: 'dotted' }, itemStyle: { color: CHART_COLORS.purple } }
      ]
    })
  }

  trendChartInstance.setOption({
    xAxis: { data: timeData, axisLabel: { rotate: data.length > 30 ? 45 : 0 } },
    series: [
      { data: data.map(d => d.voltage) },
      { data: data.map(d => d.currentVal) },
      { data: data.map(d => d.power) },
      { data: data.map(d => d.temperature) },
      { data: data.map(d => d.powerFactor) }
    ]
  })
}

onMounted(async () => {
  await loadPollingConfig()
  await Promise.all([loadDevices(), loadAlarmCount(), loadRecentAlarms()])
  if (deviceList.value.length > 0) {
    nextTick(() => { initGauges(deviceList.value[0].id); pollTrend() })
  }
  initWebSocket()
})

onUnmounted(() => {
  stopPolling()
  Object.values(gaugeCharts).forEach(c => c?.dispose())
  trendChartInstance?.dispose()
  stompClient?.deactivate()
})

useChartResize([
  () => trendChartInstance,
  // 仪表盘组：按指标名索引，需整体返回数组才能统一 resize
  () => Object.values(gaugeCharts)
])
</script>

<style scoped>
.monitor-page {
  min-height: 100%;
  font-family: var(--font-body);
  color: var(--text-primary);
  background: var(--bg-main);
}

.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1 1 160px;
  min-width: 150px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal), border-color var(--transition-normal);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
  background: var(--bg-card-hover);
}

.stat-card--total { border-left-color: var(--accent); }
.stat-card--online { border-left-color: var(--primary); }
.stat-card--fault { border-left-color: var(--danger); }
.stat-card--offline { border-left-color: var(--text-muted); }
.stat-card--alarm { border-left-color: var(--warning); }

.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon--total { background: var(--bg-elevated); color: var(--accent); }
.stat-card__icon--online { background: var(--bg-elevated); color: var(--primary); }
.stat-card__icon--fault { background: var(--bg-elevated); color: var(--danger); }
.stat-card__icon--offline { background: var(--bg-elevated); color: var(--text-muted); }
.stat-card__icon--alarm { background: var(--bg-elevated); color: var(--warning); }

.stat-card__content {
  display: flex;
  flex-direction: column;
}

.stat-card__value {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.stat-card__value--online { color: var(--primary); }
.stat-card__value--fault { color: var(--danger); }
.stat-card__value--alarm { color: var(--warning); }
.stat-card__value--offline { color: var(--text-muted); }

.stat-card__label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  font-weight: 500;
}

.board-col {
  display: flex;
}

.board-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.board-card :deep(.el-card__header) {
  padding: 14px 20px !important;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-color);
}

.board-card :deep(.el-card__body) {
  background: var(--bg-card);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header__title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.device-col {
  margin-bottom: 12px;
}

.device-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 14px;
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.device-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--border-hover);
  background: var(--bg-card-hover);
}

.device-card--selected {
  border: 2px solid var(--primary);
  box-shadow: 0 0 0 3px var(--primary-bg), var(--shadow-md), var(--shadow-glow);
}

.device-card--selected:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 0 3px var(--primary-bg), var(--shadow-lg), var(--shadow-glow);
}

.device-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.device-card__name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  font-family: var(--font-display);
}

.metric-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 3px 0;
  font-size: 12px;
}

.metric-label {
  color: var(--text-muted);
  min-width: 28px;
  font-weight: 500;
}

.metric-value {
  font-weight: 700;
  font-size: 13px;
  font-family: var(--font-mono);
  letter-spacing: -0.01em;
}

.metric-value--default { color: var(--text-primary); }
.metric-value--accent { color: var(--text-primary); }
.metric-value--primary { color: var(--text-primary); }
.metric-value--warning { color: var(--text-primary); }
.metric-value--danger { color: var(--danger); }
.metric-value--purple { color: var(--text-primary); }
.metric-value--teal { color: var(--text-primary); }

.metric-unit {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 500;
  font-family: var(--font-mono);
}

.equal-height-card {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.equal-height-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.scrollable-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.scrollable-body::-webkit-scrollbar {
  width: 4px;
}

.scrollable-body::-webkit-scrollbar-track {
  background: transparent;
}

.scrollable-body::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 2px;
}

.scrollable-body::-webkit-scrollbar-thumb:hover {
  background: var(--border-hover);
}

.alarm-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 60px 0;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.alarm-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
  font-size: 12px;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-item--critical {
  border-left: 2px solid var(--danger);
  padding-left: 8px;
}

.alarm-item--warning {
  border-left: 2px solid var(--warning);
  padding-left: 8px;
}

.alarm-item__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.alarm-item__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.alarm-item__dot--critical {
  background: var(--danger);
  box-shadow: 0 0 6px var(--danger);
}

.alarm-item__dot--warning {
  background: var(--warning);
  box-shadow: 0 0 6px var(--warning);
}

.alarm-item__time {
  color: var(--text-muted);
  font-size: 11px;
  font-family: var(--font-mono);
}

.alarm-item__content {
  color: var(--text-secondary);
  padding-left: 14px;
  font-size: 12px;
  line-height: 1.5;
}

.detail-card {
  margin-top: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.detail-card :deep(.el-card__header) {
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-color);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-header__name {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}

.detail-header__rated {
  color: var(--text-muted);
  font-size: 13px;
  font-family: var(--font-mono);
}

.detail-header__right {
  display: flex;
  gap: 8px;
}

.detail-header__right :deep(.el-radio-group) {
  --el-radio-button-checked-bg: var(--primary);
  --el-radio-button-checked-border-color: var(--primary);
  --el-radio-button-checked-color: #fff;
}

.detail-header__right :deep(.el-radio-button__inner) {
  background: var(--bg-elevated);
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.detail-header__right :deep(.el-radio-button__inner:hover) {
  color: var(--primary);
}

.trend-chart {
  height: 380px;
}

.gauge-col {
  margin-bottom: 8px;
}

.gauge-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 10px 8px 8px;
  text-align: center;
  transition: border-color var(--transition-normal), box-shadow var(--transition-normal);
}

.gauge-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-sm);
}

.gauge-card__chart {
  height: 130px;
}

.gauge-card__label {
  color: var(--text-muted);
  font-size: 11px;
  margin-top: 2px;
  font-weight: 500;
  font-family: var(--font-mono);
}

:deep(.el-tag--dark.el-tag--success) {
  --el-tag-bg-color: var(--primary-bg);
  --el-tag-border-color: var(--primary);
  --el-tag-text-color: var(--primary);
}

:deep(.el-tag--dark.el-tag--danger) {
  --el-tag-bg-color: var(--danger-bg);
  --el-tag-border-color: var(--danger);
  --el-tag-text-color: var(--danger);
}

:deep(.el-tag--dark.el-tag--warning) {
  --el-tag-bg-color: var(--warning-bg);
  --el-tag-border-color: var(--warning);
  --el-tag-text-color: var(--warning);
}

:deep(.el-tag--dark.el-tag--info) {
  --el-tag-bg-color: var(--bg-elevated);
  --el-tag-border-color: var(--border-color);
  --el-tag-text-color: var(--text-muted);
}

:deep(.el-badge__content) {
  background: var(--danger);
  border: none;
}

:deep(.el-card) {
  --el-card-bg-color: transparent;
  --el-card-border-color: transparent;
}
</style>
