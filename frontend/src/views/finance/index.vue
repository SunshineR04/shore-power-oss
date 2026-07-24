<template>
  <div class="finance-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">营收统计</h1>
        <p class="page-subtitle">查看营收趋势、消费数据与排行分析</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card stat-card--recharge">
          <div class="stat-card__icon stat-card__icon--recharge">
            <el-icon :size="20"><Coin /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">累计消费</div>
            <div class="stat-card__value stat-card__value--recharge">¥{{ formatNum(summary.totalSpent) }}</div>
            <div class="stat-card__sub">注册用户: {{ summary.totalUsers || 0 }} 人</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card--month">
          <div class="stat-card__icon stat-card__icon--month">
            <el-icon :size="20"><Calendar /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">本月消费</div>
            <div class="stat-card__value stat-card__value--month">¥{{ formatNum(summary.monthSpent) }}</div>
            <div class="stat-card__sub">本周消费: ¥{{ formatNum(summary.weekSpent) }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card--today">
          <div class="stat-card__icon stat-card__icon--today">
            <el-icon :size="20"><Odometer /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">今日使用</div>
            <div class="stat-card__value stat-card__value--today">{{ summary.todayUsageCount || 0 }}</div>
            <div class="stat-card__sub">今日消费: ¥{{ formatNum(summary.todaySpent) }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-header__title">每日营收趋势</span>
              <el-date-picker v-model="trendDateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
                end-placeholder="结束日期" value-format="YYYY-MM-DD" class="date-picker" @change="loadTrend" />
            </div>
          </template>
          <div ref="trendChartRef" class="chart-area chart-area--trend"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card week-card">
          <template #header>
            <div class="section-header">
              <span class="section-header__title">本周营收概览</span>
            </div>
          </template>
          <div class="week-body">
            <div class="week-stats-row">
              <div v-for="item in weekStats" :key="item.label" class="week-stat-item">
                <div class="week-stat-label">{{ item.label }}</div>
                <div class="week-stat-value" :style="{ color: item.color }">¥{{ formatNum(item.value) }}</div>
              </div>
            </div>
            <div ref="weekChartRef" class="chart-area chart-area--week"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="ranking-row">
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <span class="section-header__title">设备收益排行</span>
          </template>
          <div class="rank-list">
            <div v-for="(row, idx) in deviceRanking" :key="idx" class="rank-item" :class="{ 'rank-item--top': idx < 3 }">
              <div class="rank-item__rank">
                <span v-if="idx === 0" class="rank-medal">🥇</span>
                <span v-else-if="idx === 1" class="rank-medal">🥈</span>
                <span v-else-if="idx === 2" class="rank-medal">🥉</span>
                <span v-else class="rank-num">{{ idx + 1 }}</span>
              </div>
              <div class="rank-item__avatar rank-item__avatar--device">
                <span class="rank-avatar rank-avatar--device">{{ (row.deviceName || '?')[0] }}</span>
              </div>
              <div class="rank-item__info">
                <div class="rank-item__name">{{ row.deviceName }}</div>
                <div class="rank-item__usage">{{ row.usageCount || 0 }} 次使用</div>
              </div>
              <div class="rank-item__bar">
                <div class="rank-bar-track">
                  <div class="rank-bar-fill rank-bar-fill--revenue" :style="{ width: (row.totalRevenue / (deviceRanking[0]?.totalRevenue || 1)) * 100 + '%' }"></div>
                </div>
              </div>
              <div class="rank-item__value rank-item__value--revenue">¥{{ formatNum(row.totalRevenue) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <span class="section-header__title">用户消费排行</span>
          </template>
          <div class="rank-list">
            <div v-for="(row, idx) in userRanking" :key="idx" class="rank-item" :class="{ 'rank-item--top': idx < 3 }">
              <div class="rank-item__rank">
                <span v-if="idx === 0" class="rank-medal rank-medal--gold">🥇</span>
                <span v-else-if="idx === 1" class="rank-medal rank-medal--silver">🥈</span>
                <span v-else-if="idx === 2" class="rank-medal rank-medal--bronze">🥉</span>
                <span v-else class="rank-num">{{ idx + 1 }}</span>
              </div>
              <div class="rank-item__avatar">
                <span class="rank-avatar">{{ (row.userName || row.username || '?')[0] }}</span>
              </div>
              <div class="rank-item__info">
                <div class="rank-item__name">{{ row.userName || row.username }}</div>
                <div class="rank-item__usage">{{ row.usageCount || 0 }} 次使用</div>
              </div>
              <div class="rank-item__bar">
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: (row.totalSpent / (userRanking[0]?.totalSpent || 1)) * 100 + '%' }"></div>
                </div>
              </div>
              <div class="rank-item__value">¥{{ formatNum(row.totalSpent) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, nextTick, onUnmounted } from 'vue'
import { financeApi } from '../../api'
import * as echarts from 'echarts'
import { useDataSync } from '../../composables/useDataSync'

const summary = reactive({})
const deviceRanking = ref([])
const userRanking = ref([])
const trendChartRef = ref(null)
const weekChartRef = ref(null)
let trendChart = null
let weekChart = null

const { refreshKey } = useDataSync()
watch(refreshKey, () => {
  loadSummary()
  loadRankings()
  loadTrend()
})

const today = new Date()
const weekAgo = new Date(today)
weekAgo.setDate(weekAgo.getDate() - 6)
const trendDateRange = ref([
  weekAgo.toISOString().slice(0, 10),
  today.toISOString().slice(0, 10)
])

const weekStats = reactive([
  { label: '本周消费', color: '#e6a23c', value: 0 },
  { label: '今日消费', color: '#f56c6c', value: 0 }
])

const formatNum = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadSummary = async () => {
  try {
    const res = await financeApi.summary()
    const data = res.data || {}
    Object.assign(summary, data)
    weekStats[0].value = data.weekSpent || 0
    weekStats[1].value = data.todaySpent || 0
  } catch {}
}

const loadRankings = async () => {
  try {
    const [devRes, userRes] = await Promise.all([
      financeApi.deviceRanking(),
      financeApi.userRanking()
    ])
    deviceRanking.value = (devRes.data || []).slice(0, 10)
    userRanking.value = (userRes.data || []).slice(0, 10)
  } catch {}
}

const initTrendChart = () => {
  if (trendChart || !trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['消费金额', '使用次数'], top: 8, left: 'center', textStyle: { color: '#94a3b8' } },
    grid: { left: '3%', right: '4%', bottom: 60, top: 40 },
    xAxis: { type: 'category', axisLabel: { rotate: 30, color: '#94a3b8' }, axisLine: { lineStyle: { color: '#334155' } } },
    yAxis: [
      { type: 'value', name: '金额(元)', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#1e293b' } } },
      { type: 'value', name: '次数', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#1e293b' } } }
    ],
    series: [
      { name: '消费金额', type: 'bar', itemStyle: { color: '#e6a23c' } },
      { name: '使用次数', type: 'line', yAxisIndex: 1, itemStyle: { color: '#67c23a' } }
    ]
  })
}

const initWeekChart = () => {
  if (weekChart || !weekChartRef.value) return
  weekChart = echarts.init(weekChartRef.value)
  weekChart.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['消费'], top: 4, left: 'center', textStyle: { color: '#94a3b8' } },
    grid: { left: '3%', right: '4%', bottom: 40, top: 30 },
    xAxis: { type: 'category', axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#334155' } } },
    yAxis: { type: 'value', name: '金额(元)', nameTextStyle: { color: '#94a3b8' }, axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: '#1e293b' } } },
    series: [
      { name: '消费', type: 'line', areaStyle: {}, itemStyle: { color: '#e6a23c' }, smooth: true }
    ]
  })
}

const updateTrendChart = (data) => {
  if (!trendChart) return
  trendChart.setOption({
    xAxis: { data: data.map(d => d.date.slice(5)) },
    series: [
      { data: data.map(d => d.spent) },
      { data: data.map(d => d.usageCount) }
    ]
  })
}

const updateWeekChart = (data) => {
  if (!weekChart) return
  weekChart.setOption({
    xAxis: { data: data.map(d => d.date.slice(5)) },
    series: [
      { data: data.map(d => d.spent) }
    ]
  })
}

const loadTrend = async () => {
  if (!trendDateRange.value) return
  try {
    const [start, end] = trendDateRange.value
    const res = await financeApi.dailyTrend(start, end)
    const data = res.data || []
    updateTrendChart(data)
    updateWeekChart(data)
  } catch {}
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadRankings()])
  nextTick(() => {
    initTrendChart()
    initWeekChart()
    loadTrend()
  })
})

onUnmounted(() => {
  trendChart?.dispose()
  weekChart?.dispose()
})
</script>

<style scoped>
.finance-page {
  font-family: var(--font-body);
  color: var(--text-primary);
  background: var(--bg-deep);
  min-height: 100%;
  padding: 4px 0;
}

.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Page Header ── */
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
  background: linear-gradient(180deg, var(--warning) 0%, #b45309 100%);
  border-radius: 2px 0 0 2px;
}

.page-header::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 100%;
  background: radial-gradient(ellipse at right center, rgba(245, 158, 11, 0.06) 0%, transparent 70%);
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
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  font-weight: 400;
}

/* ── Stat Cards ── */
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--border-color);
  border: 1px solid var(--border-color);
  border-left-width: 4px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.25s ease, transform 0.25s ease, border-color 0.25s ease;
}

.stat-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
  border-color: var(--border-hover);
  background: var(--bg-card-hover);
}

.stat-card--recharge { border-left-color: var(--accent); }
.stat-card--balance { border-left-color: var(--primary); }
.stat-card--month { border-left-color: var(--warning); }
.stat-card--today { border-left-color: var(--danger); }

.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon--recharge { background: var(--accent-bg); color: var(--accent); }
.stat-card__icon--balance { background: var(--primary-bg); color: var(--primary); }
.stat-card__icon--month { background: var(--warning-bg); color: var(--warning); }
.stat-card__icon--today { background: var(--danger-bg); color: var(--danger); }

.stat-card__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-card__label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-card__value {
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.stat-card__value--recharge { color: var(--accent); }
.stat-card__value--balance { color: var(--primary); }
.stat-card__value--month { color: var(--warning); }
.stat-card__value--today { color: var(--danger); }

.stat-card__sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  font-family: var(--font-mono);
}

/* ── Chart Section ── */
.chart-row {
  margin-bottom: 16px;
}

.section-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  background: var(--bg-card);
  transition: border-color 0.25s ease;
}

.section-card:hover {
  border-color: var(--border-hover);
}

.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-card);
}

.section-card :deep(.el-card__body) {
  padding: 16px 20px;
  background: var(--bg-card);
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

.date-picker {
  width: 260px;
}

.date-picker :deep(.el-input__wrapper) {
  background: var(--bg-elevated);
  border-color: var(--border-color);
  box-shadow: none;
}

.date-picker :deep(.el-input__inner) {
  color: var(--text-primary);
}

.chart-area {
  width: 100%;
}

.chart-area--trend {
  height: 350px;
}

.chart-area--week {
  height: 200px;
}

/* ── Week Card ── */
.week-card :deep(.el-card__body) {
  padding: 0;
}

.week-body {
  display: flex;
  flex-direction: column;
  height: 350px;
}

.week-stats-row {
  display: flex;
  gap: 12px;
  padding: 16px 20px 12px;
}

.week-stat-item {
  flex: 1;
  text-align: center;
  padding: 14px 8px;
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.week-stat-item:hover {
  background: var(--bg-hover);
  border-color: var(--border-hover);
}

.week-stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.chart-area--week {
  flex: 1;
  height: auto;
  min-height: 0;
}

.week-stat-value {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

/* ── Ranking Section ── */
.ranking-row {
  margin-bottom: 16px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  transition: background 0.2s ease;
}

.rank-item:hover {
  background: var(--bg-hover);
}

.rank-item--top {
  background: rgba(34, 197, 94, 0.06);
  border: 1px solid rgba(34, 197, 94, 0.1);
}

.rank-item__rank {
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}

.rank-medal {
  font-size: 18px;
}

.rank-num {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-muted);
}

.rank-item__avatar {
  flex-shrink: 0;
}

.rank-item__avatar--device .rank-avatar {
  background: linear-gradient(135deg, var(--warning) 0%, #b45309 100%);
}

.rank-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary) 0%, #15803d 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-family: var(--font-display);
}

.rank-item__info {
  flex: 1;
  min-width: 0;
}

.rank-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-item__usage {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  font-family: var(--font-mono);
}

.rank-item__bar {
  flex: 1;
  max-width: 120px;
}

.rank-bar-track {
  height: 6px;
  background: var(--bg-elevated);
  border-radius: 3px;
  overflow: hidden;
}

.rank-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--danger) 0%, var(--warning) 100%);
  transition: width 0.4s ease;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.3);
}

.rank-bar-fill--revenue {
  background: linear-gradient(90deg, var(--warning) 0%, var(--primary) 100%);
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.3);
}

.rank-item__value {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 700;
  color: var(--danger);
  white-space: nowrap;
  min-width: 90px;
  text-align: right;
}

.rank-item__value--revenue {
  color: var(--warning);
}

/* ── Element Plus Deep Overrides ── */
:deep(.el-card) {
  background: var(--bg-card);
  border-color: var(--border-color);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

:deep(.el-card__header) {
  background: var(--bg-card);
  border-color: var(--border-light);
  color: var(--text-primary);
}

:deep(.el-card__body) {
  color: var(--text-primary);
}

:deep(.el-date-editor) {
  --el-fill-color-blank: var(--bg-elevated);
  --el-text-color-regular: var(--text-primary);
  --el-border-color: var(--border-color);
  --el-border-color-light: var(--border-light);
}

:deep(.el-date-editor .el-input__wrapper) {
  background: var(--bg-elevated) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

:deep(.el-date-editor .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover) inset !important;
}

:deep(.el-date-editor .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
}

:deep(.el-date-editor .el-input__inner) {
  color: var(--text-primary) !important;
}

:deep(.el-date-editor .el-input__inner::placeholder) {
  color: var(--text-placeholder);
}

:deep(.el-date-editor .el-input__icon) {
  color: var(--text-muted);
}

:deep(.el-date-editor .el-range-separator) {
  color: var(--text-muted);
}

:deep(.el-date-editor .el-range-input) {
  color: var(--text-primary);
  background: transparent;
}

:deep(.el-row) {
  --el-row-gutter: 16px;
}
</style>
