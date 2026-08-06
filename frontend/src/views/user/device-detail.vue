<template>
  <div v-loading="loading" class="device-detail-container animate-fade-in-up">
    <div v-if="device" class="detail-body">
      <div class="device-header">
        <div class="header-left">
          <button class="back-btn" @click="$router.back()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
            <span>返回</span>
          </button>
          <div class="header-title-group">
            <h1 class="device-title">{{ device.deviceName }}</h1>
            <el-tag :type="getStatusType(device.status)" size="large" class="status-tag">{{ getStatusText(device.status) }}</el-tag>
          </div>
        </div>
      </div>

      <div class="content-grid">
        <div class="content-main">
          <div class="info-card">
            <div class="card-header">
              <div class="card-icon info-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              </div>
              <h2 class="card-title">设备信息</h2>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">设备编号</span>
                <span class="info-value">{{ device.deviceCode }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">设备类型</span>
                <span class="info-value">{{ device.deviceType }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">设备状态</span>
                <span class="info-value"><el-tag :type="getStatusType(device.status)" size="small">{{ getStatusText(device.status) }}</el-tag></span>
              </div>
              <div class="info-item">
                <span class="info-label">所在位置</span>
                <span class="info-value">{{ device.location || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">额定电压</span>
                <span class="info-value">{{ device.ratedVoltage || '-' }} <small>V</small></span>
              </div>
              <div class="info-item">
                <span class="info-label">额定电流</span>
                <span class="info-value">{{ device.ratedCurrent || '-' }} <small>A</small></span>
              </div>
              <div class="info-item">
                <span class="info-label">额定功率</span>
                <span class="info-value">{{ device.ratedPower || '-' }} <small>kW</small></span>
              </div>
              <div class="info-item">
                <span class="info-label">制造商</span>
                <span class="info-value">{{ device.manufacturer || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="realtime-card">
            <div class="card-header">
              <div class="card-icon realtime-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              </div>
              <h2 class="card-title">实时数据</h2>
              <span class="realtime-dot"></span>
            </div>
            <div v-if="latestData" class="metrics-grid">
              <div class="metric-card metric-voltage">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.voltage || '-' }}</span>
                  <span class="metric-unit">V</span>
                </div>
                <span class="metric-label">电压</span>
              </div>
              <div class="metric-card metric-current">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 16l4-4-4-4"/><path d="M6 8l-4 4 4 4"/><path d="M14.5 4l-5 16"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.currentVal || '-' }}</span>
                  <span class="metric-unit">A</span>
                </div>
                <span class="metric-label">电流</span>
              </div>
              <div class="metric-card metric-power">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><path d="M12 6v12"/><path d="M2 12h20"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.power || '-' }}</span>
                  <span class="metric-unit">kW</span>
                </div>
                <span class="metric-label">功率</span>
              </div>
              <div class="metric-card metric-temperature">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.temperature || '-' }}</span>
                  <span class="metric-unit">℃</span>
                </div>
                <span class="metric-label">温度</span>
              </div>
              <div class="metric-card metric-humidity">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.humidity || '-' }}</span>
                  <span class="metric-unit">%</span>
                </div>
                <span class="metric-label">湿度</span>
              </div>
              <div class="metric-card metric-factor">
                <div class="metric-icon-wrap">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                </div>
                <div class="metric-content">
                  <span class="metric-value">{{ latestData.powerFactor || '-' }}</span>
                  <span class="metric-unit"></span>
                </div>
                <span class="metric-label">功率因数</span>
              </div>
            </div>
            <el-empty v-else description="暂无实时数据" />
          </div>

          <div class="ratings-card">
            <div class="card-header">
              <div class="card-icon rating-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </div>
              <h2 class="card-title">设备评价</h2>
            </div>
            <div v-if="ratings.length > 0" class="ratings-list">
              <div v-for="r in ratings" :key="r.id" class="rating-item">
                <div class="rating-top">
                  <div class="rating-user">
                    <div class="rating-avatar">{{ (r.userName || '?')[0] }}</div>
                    <span class="rating-username">{{ r.userName }}</span>
                  </div>
                  <el-rate v-model="r.rating" disabled class="rating-stars" />
                </div>
                <p class="rating-comment">{{ r.comment || '暂无评价' }}</p>
                <span class="rating-time">{{ r.createTime }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无评价" />
          </div>

          <div class="submit-rating-card">
            <div class="card-header">
              <div class="card-icon submit-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              </div>
              <h2 class="card-title">提交评价</h2>
            </div>
            <el-form :model="ratingForm" label-width="80px" class="rating-form">
              <el-form-item label="评分">
                <el-rate v-model="ratingForm.rating" />
              </el-form-item>
              <el-form-item label="评价内容">
                <el-input v-model="ratingForm.comment" type="textarea" :rows="3" placeholder="请输入评价内容" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitRating" class="submit-btn">提交评价</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="content-sidebar">
          <div class="reserve-card">
            <div class="reserve-card-header">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              <h3>预约设备</h3>
            </div>
            <div class="reserve-card-body">
              <div class="reserve-device-name">{{ device.deviceName }}</div>
              <div class="reserve-device-status">
                <span class="reserve-status-dot" :class="'status-' + device.status"></span>
                <span>{{ getStatusText(device.status) }}</span>
              </div>
              <el-button v-if="canReserveThisDevice()" type="primary" class="reserve-btn" @click="showReserveDialog">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
                立即预约
              </el-button>
              <el-button v-else-if="hasActiveReservation" type="info" disabled class="reserve-btn reserve-btn-disabled">
                已有预约
              </el-button>
              <el-button v-else type="info" disabled class="reserve-btn reserve-btn-disabled">
                {{ getStatusText(device?.status) }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="reserveDialogVisible" title="预约充电" width="600px" append-to-body class="reserve-dialog">
      <el-form :model="reserveForm" label-width="100px">
        <el-form-item label="设备名称">
          <el-input v-model="reserveForm.deviceName" disabled />
        </el-form-item>
        <el-form-item label="开始时段">
          <el-select v-model="reserveForm.startSlot" placeholder="选择开始时段" class="full-width" @change="onStartSlotChange">
            <el-option
              v-for="slot in availableStartSlots"
              :key="slot.value"
              :label="slot.label"
              :value="slot.value"
              :disabled="slot.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="reserveForm.endDate"
            type="date"
            placeholder="选择结束日期"
            class="full-width"
            :disabled-date="disabledEndDate"
            @change="onEndDateChange"
          />
        </el-form-item>
        <el-form-item label="结束时段">
          <el-select v-model="reserveForm.endSlot" placeholder="选择结束时段" class="full-width">
            <el-option
              v-for="slot in availableEndSlots"
              :key="slot.value"
              :label="slot.label"
              :value="slot.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时段" v-if="reserveForm.startSlot !== null && reserveForm.endSlot !== null">
          <el-tag type="success" size="large">
            {{ formatSlotRange() }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReserve">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deviceApi, reservationApi } from '../../api'
import { DEVICE_STATUS, statusMeta } from '../../utils/status'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const device = ref(null)
const latestData = ref(null)
const ratings = ref([])
const reserveDialogVisible = ref(false)
const reserveForm = ref({
  deviceId: null,
  deviceName: '',
  startDate: null,
  endDate: null,
  startSlot: null,
  endSlot: null
})
const ratingForm = ref({
  rating: 5,
  comment: ''
})

const loadDevice = async () => {
  loading.value = true
  try {
    const res = await deviceApi.get(route.params.id)
    device.value = res.data
    const pileTypes = ['SMALL_YACHT', 'INLAND_CARGO', 'COASTAL_CARGO', 'CONTAINER_SHIP', 'TANKER']
    if (device.value && !pileTypes.includes(device.value.deviceType)) {
      ElMessage.warning('只能查看岸电桩设备详情')
      router.replace('/dashboard')
      return
    }
    await checkDeviceReservation()
  } catch (e) {
    ElMessage.error('加载设备信息失败')
  } finally {
    loading.value = false
  }
}

const hasActiveReservation = ref(false)

const checkDeviceReservation = async () => {
  try {
    const res = await reservationApi.list()
    const reservations = res.data || []
    hasActiveReservation.value = reservations.some(r =>
      r.deviceId === device.value?.id &&
      ['PENDING', 'CONFIRMED', 'IN_USE', 'PENDING_PAYMENT'].includes(r.status)
    )
  } catch {}
}

const canReserveThisDevice = () => {
  return device.value?.status === 'ONLINE' && !hasActiveReservation.value
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const disabledEndDate = (time) => {
  if (reserveForm.value.startDate) {
    return time.getTime() < new Date(reserveForm.value.startDate).getTime() - 8.64e7
  }
  return time.getTime() < Date.now() - 8.64e7
}

const generateTimeSlots = () => {
  const slots = []
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 15) {
      const value = h * 60 + m
      const label = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
      slots.push({ value, label })
    }
  }
  return slots
}

const allSlots = generateTimeSlots()

const getCurrentSlotIndex = () => {
  const now = new Date()
  return now.getHours() * 60 + Math.floor(now.getMinutes() / 15) * 15
}

const isToday = (date) => {
  if (!date) return false
  const d = new Date(date)
  const now = new Date()
  return d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
}

const isSameDay = () => {
  if (!reserveForm.value.startDate || !reserveForm.value.endDate) return true
  const s = new Date(reserveForm.value.startDate)
  const e = new Date(reserveForm.value.endDate)
  return s.getFullYear() === e.getFullYear() &&
    s.getMonth() === e.getMonth() &&
    s.getDate() === e.getDate()
}

const availableStartSlots = computed(() => {
  const currentSlot = getCurrentSlotIndex()
  const today = isToday(reserveForm.value.startDate)
  if (!today) return allSlots
  return allSlots.filter(slot => slot.value >= currentSlot)
})

const availableEndSlots = computed(() => {
  if (reserveForm.value.startSlot === null) return []
  if (isSameDay()) {
    return allSlots.filter(slot => slot.value > reserveForm.value.startSlot)
  }
  return allSlots
})

const onEndDateChange = () => {
  reserveForm.value.endSlot = null
}

const onStartSlotChange = () => {
  reserveForm.value.endSlot = null
}

const slotValueToTime = (value) => {
  const h = Math.floor(value / 60)
  const m = value % 60
  return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

const formatSlotRange = () => {
  const startDate = formatDate(reserveForm.value.startDate)
  const endDate = formatDate(reserveForm.value.endDate)
  const startTime = slotValueToTime(reserveForm.value.startSlot)
  const endTime = slotValueToTime(reserveForm.value.endSlot)
  if (startDate === endDate) {
    return startDate + ' ' + startTime + ' - ' + endTime
  }
  return startDate + ' ' + startTime + ' 至 ' + endDate + ' ' + endTime
}

const loadLatestData = async () => {
  try {
    const res = await deviceApi.latest(route.params.id)
    latestData.value = res.data
  } catch {}
}

const loadRatings = async () => {
  try {
    const res = await reservationApi.getDeviceRatings(route.params.id)
    ratings.value = res.data || []
  } catch {}
}

const getStatusType = (status) => statusMeta(DEVICE_STATUS, status).type
const getStatusText = (status) => statusMeta(DEVICE_STATUS, status).label

const showReserveDialog = () => {
  const today = new Date()
  reserveForm.value = {
    deviceId: device.value.id,
    deviceName: device.value.deviceName,
    startDate: today,
    endDate: today,
    startSlot: null,
    endSlot: null
  }
  reserveDialogVisible.value = true
}

const handleReserve = async () => {
  if (!reserveForm.value.startDate || !reserveForm.value.endDate || reserveForm.value.startSlot === null || reserveForm.value.endSlot === null) {
    ElMessage.warning('请选择完整的预约时间')
    return
  }
  try {
    const startDateStr = formatDate(reserveForm.value.startDate)
    const startH = Math.floor(reserveForm.value.startSlot / 60)
    const startM = reserveForm.value.startSlot % 60
    const startTime = startDateStr + ' ' +
      String(startH).padStart(2, '0') + ':' +
      String(startM).padStart(2, '0') + ':00'

    const endDateStr = formatDate(reserveForm.value.endDate)
    const endH = Math.floor(reserveForm.value.endSlot / 60)
    const endM = reserveForm.value.endSlot % 60
    const endTime = endDateStr + ' ' +
      String(endH).padStart(2, '0') + ':' +
      String(endM).padStart(2, '0') + ':00'

    if (startTime >= endTime) {
      ElMessage.warning('结束时间必须晚于开始时间')
      return
    }

    await reservationApi.create(reserveForm.value.deviceId, startTime, endTime, null)
    ElMessage.success('预约成功')
    reserveDialogVisible.value = false
    checkDeviceReservation()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.response?.data?.message || '预约失败')
  }
}

const submitRating = async () => {
  try {
    await reservationApi.submitRating(device.value.id, ratingForm.value.rating, ratingForm.value.comment)
    ElMessage.success('评价成功')
    ratingForm.value = { rating: 5, comment: '' }
    loadRatings()
  } catch (e) {
    ElMessage.error('评价失败')
  }
}

onMounted(() => {
  loadDevice()
  loadLatestData()
  loadRatings()
})
</script>

<style scoped>
.device-header {
  margin-bottom: 28px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  margin-top: 4px;
}

.back-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-bg);
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.15);
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.device-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.5px;
}

.status-tag {
  font-family: var(--font-body);
  font-weight: 500;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

.content-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.content-sidebar {
  position: sticky;
  top: 20px;
}

.info-card,
.realtime-card,
.ratings-card,
.submit-rating-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 24px;
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
}

.info-card:hover,
.realtime-card:hover,
.ratings-card:hover,
.submit-rating-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-icon {
  background: var(--accent-bg);
  color: var(--accent);
}

.realtime-icon {
  background: var(--primary-bg);
  color: var(--primary);
}

.rating-icon {
  background: var(--warning-bg);
  color: var(--warning);
}

.submit-icon {
  background: var(--primary-bg);
  color: var(--primary);
}

.card-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.realtime-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 8px var(--primary), 0 0 16px rgba(37, 99, 235, 0.3);
  animation: pulse 2s ease-in-out infinite;
  margin-left: auto;
}

@keyframes pulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 8px var(--primary), 0 0 16px rgba(37, 99, 235, 0.3); }
  50% { opacity: 0.4; box-shadow: 0 0 4px var(--primary), 0 0 8px rgba(37, 99, 235, 0.15); }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0;
}

.info-item {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: background 0.2s ease;
}

.info-item:hover {
  background: var(--bg-hover);
}

.info-item:nth-child(odd) {
  border-right: 1px solid var(--border-light);
}

.info-item:nth-last-child(-n+2) {
  border-bottom: none;
}

.info-label {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.info-value small {
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
  margin-left: 2px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.metric-card {
  border-radius: var(--radius-md);
  padding: 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  border: 1px solid var(--border-color);
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-hover);
  box-shadow: var(--shadow-md);
}

.metric-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  border-radius: 4px 0 0 4px;
}

.metric-voltage {
  background: var(--accent-bg);
}
.metric-voltage::before { background: var(--accent); }
.metric-voltage .metric-icon-wrap { color: var(--accent); }
.metric-voltage .metric-value { color: var(--accent); }

.metric-current {
  background: var(--primary-bg);
}
.metric-current::before { background: var(--primary); }
.metric-current .metric-icon-wrap { color: var(--primary); }
.metric-current .metric-value { color: var(--primary); }

.metric-power {
  background: var(--accent-bg);
}
.metric-power::before { background: var(--accent); }
.metric-power .metric-icon-wrap { color: var(--accent); }
.metric-power .metric-value { color: var(--accent); }

.metric-temperature {
  background: var(--danger-bg);
}
.metric-temperature::before { background: var(--danger); }
.metric-temperature .metric-icon-wrap { color: var(--danger); }
.metric-temperature .metric-value { color: var(--danger); }

.metric-humidity {
  background: var(--accent-bg);
}
.metric-humidity::before { background: var(--accent); }
.metric-humidity .metric-icon-wrap { color: var(--accent); }
.metric-humidity .metric-value { color: var(--accent); }

.metric-factor {
  background: var(--primary-bg);
}
.metric-factor::before { background: var(--primary); }
.metric-factor .metric-icon-wrap { color: var(--primary); }
.metric-factor .metric-value { color: var(--primary); }

.metric-icon-wrap {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-content {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.metric-value {
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.metric-unit {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

.metric-label {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.ratings-list {
  display: flex;
  flex-direction: column;
}

.rating-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.2s ease;
}

.rating-item:hover {
  background: var(--bg-hover);
  margin: 0 -24px;
  padding-left: 24px;
  padding-right: 24px;
}

.rating-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rating-item:first-child {
  padding-top: 0;
}

.rating-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.rating-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: var(--bg-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.2);
}

.rating-username {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.rating-stars {
  --el-rate-icon-size: 16px;
}

.rating-comment {
  font-family: var(--font-body);
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 8px 0;
  padding-left: 46px;
}

.rating-time {
  font-family: var(--font-mono);
  color: var(--text-muted);
  font-size: 12px;
  padding-left: 46px;
}

.rating-form {
  margin-top: 4px;
}

.submit-btn {
  background: var(--primary) !important;
  border-color: var(--primary) !important;
  font-family: var(--font-body);
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 10px 28px;
  transition: all 0.25s ease;
}

.submit-btn:hover {
  background: var(--primary) !important;
  border-color: var(--primary) !important;
  transform: translateY(-1px);
  box-shadow: 0 0 20px rgba(37, 99, 235, 0.35), var(--shadow-glow);
}

.reserve-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.reserve-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-md);
}

.reserve-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--primary-bg), var(--accent-bg));
  border-bottom: 1px solid var(--border-light);
  color: var(--primary);
}

.reserve-card-header h3 {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.reserve-card-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reserve-device-name {
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.reserve-device-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--text-secondary);
}

.reserve-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.reserve-status-dot.status-ONLINE {
  background: var(--primary);
  box-shadow: 0 0 8px var(--primary), 0 0 16px rgba(37, 99, 235, 0.3);
}

.reserve-status-dot.status-OFFLINE {
  background: var(--text-muted);
}

.reserve-status-dot.status-FAULT {
  background: var(--danger);
  box-shadow: 0 0 8px var(--danger), 0 0 16px rgba(239, 68, 68, 0.3);
}

.reserve-status-dot.status-MAINTENANCE {
  background: var(--warning);
  box-shadow: 0 0 8px var(--warning), 0 0 16px rgba(245, 158, 11, 0.3);
}

.reserve-status-dot.status-IN_USE {
  background: var(--accent);
  box-shadow: 0 0 8px var(--accent), 0 0 16px rgba(14, 165, 233, 0.3);
}

.reserve-btn {
  width: 100%;
  height: 44px !important;
  border-radius: var(--radius-sm) !important;
  font-family: var(--font-body) !important;
  font-weight: 600 !important;
  font-size: 15px !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.25s ease;
  border: none !important;
  background: linear-gradient(135deg, var(--primary), #1d4ed8) !important;
  color: var(--bg-deep) !important;
}

.reserve-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba(37, 99, 235, 0.35), var(--shadow-glow);
}

.reserve-btn-disabled {
  background: var(--bg-elevated) !important;
  color: var(--text-muted) !important;
  cursor: not-allowed !important;
  opacity: 0.6;
}

.reserve-btn-disabled:hover {
  transform: none;
  box-shadow: none;
}

.full-width {
  width: 100%;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .content-sidebar {
    position: static;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .header-left {
    flex-direction: column;
    gap: 12px;
  }

  .device-title {
    font-size: 22px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item:nth-child(odd) {
    border-right: none;
  }

  .metrics-grid {
    grid-template-columns: 1fr 1fr;
  }

  .rating-comment,
  .rating-time {
    padding-left: 0;
  }
}
</style>
