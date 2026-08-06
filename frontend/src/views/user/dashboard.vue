<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">充电桩预约</h1>
        <p class="page-subtitle">查看可用充电桩并预约使用</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <div class="stats-bar">
      <div class="metric-tile">
        <div class="metric-tile__value text-success">{{ onlineCount }}</div>
        <div class="metric-tile__label">可用充电桩</div>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-tile">
        <div class="metric-tile__value text-primary">{{ reservationCount }}</div>
        <div class="metric-tile__label">进行中预约</div>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-tile">
        <div class="metric-tile__value text-warning">{{ usageCount }}<span class="metric-tile__unit"> 次</span></div>
        <div class="metric-tile__label">累计使用</div>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-tile">
        <div class="metric-tile__value text-muted">{{ shipCount }}</div>
        <div class="metric-tile__label">我的船舶</div>
      </div>
      <div class="stats-spacer"></div>
      <div class="stats-actions">
        <el-button class="action-btn" text @click="$router.push('/user/ship')">
          <el-icon><Ship /></el-icon> 管理船舶
        </el-button>
        <el-button class="action-btn" text @click="$router.push('/user/reservations')">
          <el-icon><Calendar /></el-icon> 我的预约
        </el-button>
      </div>
    </div>

    <el-card v-if="!shipCount" class="empty-ship-card">
      <el-empty description="请先添加船舶，系统将为您展示匹配的充电桩" :image-size="120">
        <el-button type="primary" @click="$router.push('/user/ship')">
          <el-icon><Plus /></el-icon> 添加船舶
        </el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <el-card class="device-section-card">
        <template #header>
          <div class="section-header">
            <span class="section-title">充电桩</span>
            <div class="section-filter">
              <el-select v-model="filterType" placeholder="全部类型" clearable size="small" style="width: 150px">
                <el-option v-for="t in availablePileTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </div>
          </div>
        </template>
        <el-row :gutter="16">
          <el-col :span="6" v-for="device in filteredDevices" :key="device.id" class="device-col">
            <div class="device-card" @click="$router.push(`/user/device/${device.id}`)">
              <div class="device-card__header">
                <span class="device-card__name">{{ device.deviceName }}</span>
                <el-tag
                  :type="device.status === 'ONLINE' ? 'success' : device.status === 'FAULT' ? 'danger' : 'info'"
                  size="small"
                  effect="dark"
                  class="device-card__status"
                >
                  {{ statusMap[device.status] }}
                </el-tag>
              </div>
              <div class="device-card__location">
                <el-icon><Location /></el-icon> {{ device.location || '-' }}
              </div>
              <div class="device-card__tags">
                <el-tag size="small" type="info">{{ typeMap[device.deviceType] || device.deviceType }}</el-tag>
                <el-tag size="small" class="tag-power">{{ device.ratedPower }}kW</el-tag>
                <el-tag size="small" type="warning">{{ device.ratedVoltage }}V</el-tag>
              </div>
              <div class="device-card__compat">
                适用：{{ getCompatibleShipLabels(device.deviceType) }}
              </div>
              <el-button
                v-if="device.status === 'ONLINE' && !device.hasActiveReservation"
                class="btn-reserve"
                @click.stop="openReserve(device)"
              >
                立即预约
              </el-button>
              <el-button v-else-if="device.hasActiveReservation" class="btn-disabled-full" size="default" disabled>已有预约</el-button>
              <el-button v-else class="btn-disabled-full" size="default" disabled>{{ statusMap[device.status] }}</el-button>
            </div>
          </el-col>
        </el-row>
        <el-empty v-if="!filteredDevices.length" description="暂无匹配您船舶的充电桩" :image-size="80" />
      </el-card>

      <el-card v-if="activeReservations.length" class="reservation-section-card">
        <template #header>
          <div class="section-header">
            <span class="section-title">当前预约</span>
            <el-button type="primary" size="small" text @click="$router.push('/user/reservations')">查看全部</el-button>
          </div>
        </template>
        <el-row :gutter="16">
          <el-col :span="8" v-for="r in activeReservations" :key="r.id">
            <div class="reservation-card" :class="'reservation-card--' + resStatusType[r.status]">
              <div class="reservation-card__header">
                <span class="reservation-card__name">{{ r.deviceName }}</span>
                <el-tag :type="resStatusType[r.status]" size="small">{{ resStatusMap[r.status] }}</el-tag>
              </div>
              <div class="reservation-card__time">
                {{ formatTime(r.startTime) }} ~ {{ formatTime(r.endTime) }}
              </div>
              <div class="reservation-card__actions">
                <el-button v-if="r.status === 'CONFIRMED'" type="success" size="small" @click="startUse(r)">开始使用</el-button>
                <el-button v-if="r.status === 'IN_USE'" type="warning" size="small" @click="endUse(r)">结束使用</el-button>
                <el-button v-if="r.status === 'PENDING_PAYMENT'" type="danger" size="small" @click="handlePay(r)">去支付</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </template>

    <el-dialog v-model="reserveVisible" title="预约充电桩" width="500" append-to-body destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="设备">{{ reserveDevice?.deviceName }}</el-form-item>
        <el-form-item label="选择船舶">
          <el-select v-model="reserveForm.shipId" placeholder="选择船舶" style="width: 100%">
            <el-option v-for="s in compatibleShips" :key="s.id" :label="s.shipName" :value="s.id">
              <span>{{ s.shipName }}</span>
              <span style="float: right; color: #909399; font-size: 12px">{{ computedShipTypeLabels[s.shipType] || s.shipType }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="reserveForm.date" type="date" placeholder="选择开始日期"
            :disabled-date="disabledDate" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时段">
          <el-select v-model="reserveForm.startSlot" placeholder="选择开始时段" style="width: 100%" @change="onStartSlotChange">
            <el-option v-for="slot in availableStartSlots" :key="slot.value" :label="slot.label" :value="slot.value" :disabled="slot.disabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="reserveForm.endDate" type="date" placeholder="选择结束日期"
            :disabled-date="disabledEndDate" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束时段">
          <el-select v-model="reserveForm.endSlot" placeholder="选择结束时段" style="width: 100%">
            <el-option v-for="slot in availableEndSlots" :key="slot.value" :label="slot.label" :value="slot.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="reserveForm.startSlot !== null && reserveForm.endSlot !== null" label="预约时段">
          <el-tag type="success" size="large">{{ formatSlotRange() }}</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveVisible = false">取消</el-button>
        <el-button type="primary" :loading="reserving" @click="submitReserve">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deviceApi, reservationApi, shipApi, systemConfigApi } from '../../api'

const statusMap = { ONLINE: '在线', OFFLINE: '离线', FAULT: '故障', MAINTENANCE: '维护中' }
const resStatusMap = { PENDING: '待确认', CONFIRMED: '已确认', IN_USE: '使用中', PENDING_PAYMENT: '待支付', COMPLETED: '已完成', CANCELLED: '已取消' }
const resStatusType = { PENDING: 'warning', CONFIRMED: 'primary', IN_USE: 'success', PENDING_PAYMENT: 'danger', COMPLETED: 'info', CANCELLED: 'info' }

const deviceTypes = ref({
  pileTypes: [],
  shipTypeLabels: {},
  shipToPileMap: {}
})

const computedPileTypes = computed(() => deviceTypes.value.pileTypes)
const computedShipTypeLabels = computed(() => deviceTypes.value.shipTypeLabels)
const computedShipToPileMap = computed(() => deviceTypes.value.shipToPileMap)
const typeMap = computed(() => {
  const map = {}
  computedPileTypes.value.forEach(t => { map[t.value] = t.label })
  return map
})

const pileToShipMap = computed(() => {
  const map = {}
  Object.entries(computedShipToPileMap.value).forEach(([shipType, pileTypeList]) => {
    pileTypeList.forEach(pt => {
      if (!map[pt]) map[pt] = []
      if (!map[pt].includes(shipType)) map[pt].push(shipType)
    })
  })
  return map
})

const router = useRouter()
const onlineCount = ref(0)
const reservationCount = ref(0)
const usageCount = ref(0)
const shipCount = ref(0)
const devices = ref([])
const activeReservations = ref([])
const ships = ref([])
const filterType = ref('')
const slotMinutes = ref(15)

const userPileTypes = computed(() => {
  const types = new Set()
  ships.value.forEach(s => {
    const pileList = computedShipToPileMap.value[s.shipType]
    if (pileList) pileList.forEach(t => types.add(t))
  })
  return types
})

const availablePileTypes = computed(() => {
  return computedPileTypes.value.filter(t => userPileTypes.value.has(t.value))
})

const filteredDevices = computed(() => {
  let list = devices.value
  if (filterType.value) {
    list = list.filter(d => d.deviceType === filterType.value)
  }
  return list
})

const compatibleShips = computed(() => {
  if (!reserveDevice.value) return []
  const pileType = reserveDevice.value.deviceType
  return ships.value.filter(s => {
    const pileList = computedShipToPileMap.value[s.shipType]
    return pileList && pileList.includes(pileType)
  })
})

function getCompatibleShipLabels(pileType) {
  const shipTypes = pileToShipMap.value[pileType] || []
  return shipTypes.map(t => computedShipTypeLabels.value[t] || t).join('、')
}

const reserveVisible = ref(false)
const reserveDevice = ref(null)
const reserving = ref(false)
const reserveForm = ref({ shipId: null, date: null, endDate: null, startSlot: null, endSlot: null })

function generateAllSlots() {
  const slots = []
  const step = slotMinutes.value
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += step) {
      const value = h * 60 + m
      const label = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
      slots.push({ value, label })
    }
  }
  return slots
}

function isToday(dateStr) {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const now = new Date()
  return d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
}

const availableStartSlots = computed(() => {
  const allSlots = generateAllSlots()
  const today = isToday(reserveForm.value.date)
  if (!today) return allSlots
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  return allSlots.filter(slot => slot.value >= currentMinutes)
})

const availableEndSlots = computed(() => {
  if (reserveForm.value.startSlot === null) return []
  const allSlots = generateAllSlots()
  const sameDay = reserveForm.value.date === reserveForm.value.endDate
  if (sameDay) {
    return allSlots.filter(slot => slot.value > reserveForm.value.startSlot)
  }
  return allSlots
})

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const disabledEndDate = (time) => {
  if (reserveForm.value.date) {
    const startDate = new Date(reserveForm.value.date)
    return time.getTime() < startDate.getTime()
  }
  return time.getTime() < Date.now() - 8.64e7
}

function onStartSlotChange() {
  reserveForm.value.endSlot = null
}

function slotValueToTime(value) {
  const h = Math.floor(value / 60)
  const m = value % 60
  return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
}

function formatSlotRange() {
  const startDate = reserveForm.value.date || ''
  const endDate = reserveForm.value.endDate || startDate
  const start = slotValueToTime(reserveForm.value.startSlot)
  const end = slotValueToTime(reserveForm.value.endSlot)
  if (startDate === endDate) {
    return startDate + ' ' + start + ' - ' + end
  }
  return startDate + ' ' + start + ' 至 ' + endDate + ' ' + end
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

async function loadData() {
  const results = await Promise.allSettled([
    deviceApi.list(),
    reservationApi.list(),
    shipApi.list(),
    systemConfigApi.publicConfig()
  ])

  const devRes = results[0].status === 'fulfilled' ? results[0].value : { data: [] }
  const resRes = results[1].status === 'fulfilled' ? results[1].value : { data: [] }
  const shipRes = results[2].status === 'fulfilled' ? results[2].value : { data: [] }
  const configRes = results[3].status === 'fulfilled' ? results[3].value : { data: {} }

  const typesRes = await deviceApi.types().catch(() => ({ data: null }))
  if (typesRes?.data) {
    deviceTypes.value = typesRes.data
  }

  if (configRes.data?.reservationSlotMinutes) {
    slotMinutes.value = configRes.data.reservationSlotMinutes
  }

  ships.value = shipRes.data || []
  shipCount.value = ships.value.length

  const allDevices = devRes.data || []
  devices.value = allDevices.filter(d => userPileTypes.value.has(d.deviceType))
  onlineCount.value = devices.value.filter(d => d.status === 'ONLINE').length

  const reservations = resRes.data || []
  const activeStatuses = ['PENDING', 'CONFIRMED', 'IN_USE']
  activeReservations.value = reservations.filter(r => activeStatuses.includes(r.status))
  reservationCount.value = activeReservations.value.length

  const activeDeviceIds = new Set(
    reservations.filter(r => activeStatuses.includes(r.status)).map(r => r.deviceId)
  )
  devices.value.forEach(d => { d.hasActiveReservation = activeDeviceIds.has(d.id) })

  try {
    const usageRes = await reservationApi.usageRecords()
    usageCount.value = (usageRes.data || []).length
  } catch {}
}

onMounted(() => loadData())

function handlePay(row) {
  router.push('/user/reservations')
}

function openReserve(device) {
  reserveDevice.value = device
  const today = new Date()
  const dateStr = today.getFullYear() + '-' +
    String(today.getMonth() + 1).padStart(2, '0') + '-' +
    String(today.getDate()).padStart(2, '0')
  reserveForm.value = { shipId: null, date: dateStr, endDate: dateStr, startSlot: null, endSlot: null }
  reserveVisible.value = true
}

async function submitReserve() {
  if (!reserveForm.value.shipId) {
    ElMessage.warning('请选择船舶')
    return
  }
  if (!reserveForm.value.date || !reserveForm.value.endDate || reserveForm.value.startSlot === null || reserveForm.value.endSlot === null) {
    ElMessage.warning('请选择完整的预约时间段')
    return
  }

  const startH = Math.floor(reserveForm.value.startSlot / 60)
  const startM = reserveForm.value.startSlot % 60
  const startTime = reserveForm.value.date + ' ' +
    String(startH).padStart(2, '0') + ':' +
    String(startM).padStart(2, '0') + ':00'

  const endH = Math.floor(reserveForm.value.endSlot / 60)
  const endM = reserveForm.value.endSlot % 60
  const endDate = reserveForm.value.endDate || reserveForm.value.date
  const endTime = endDate + ' ' +
    String(endH).padStart(2, '0') + ':' +
    String(endM).padStart(2, '0') + ':00'

  if (startTime >= endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  reserving.value = true
  try {
    await reservationApi.create(
      reserveDevice.value.id,
      startTime,
      endTime,
      reserveForm.value.shipId
    )
    ElMessage.success('预约成功')
    reserveVisible.value = false
    loadData()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '预约失败')
  } finally {
    reserving.value = false
  }
}

async function startUse(r) {
  try {
    await reservationApi.startUsage(r.id)
    ElMessage.success('已开始使用')
    loadData()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}

async function endUse(r) {
  try {
    await reservationApi.endUsage(r.id)
    ElMessage.success('已结束使用')
    loadData()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}
</script>

<style scoped>
.stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 18px 24px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
  border: 1px solid var(--border-light);
}

.metric-tile {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 24px;
}

.metric-tile__value {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}

.metric-tile__unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 2px;
}

.metric-tile__label {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-family: var(--font-mono);
}

.metric-divider {
  width: 1px;
  align-self: stretch;
  background: var(--border-light);
  margin: 4px 0;
}

.stats-spacer {
  flex: 1;
}

.stats-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  color: var(--text-secondary) !important;
  font-weight: 500;
}

.action-btn:hover {
  color: var(--primary) !important;
}

.empty-ship-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.empty-ship-card :deep(.el-empty__description p) {
  color: var(--text-secondary);
  font-size: 14px;
}

.device-section-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-weight: 700;
  font-size: 17px;
  color: var(--text-primary);
}

.section-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.device-col {
  margin-bottom: 16px;
}

.device-card {
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-slow);
  background: var(--bg-card);
  position: relative;
}

.device-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary);
}

.device-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.device-card__name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.device-card__status {
  flex-shrink: 0;
}

.device-card__location {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.device-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.device-card__tags .el-tag {
  border-radius: var(--radius-xs);
  font-size: 11px;
}

.tag-power {
  background: var(--primary-bg);
  color: var(--primary);
  border-color: var(--primary-bg);
}

.device-card__compat {
  font-size: 12px;
  color: var(--primary);
  margin-bottom: 12px;
  font-weight: 500;
}

.btn-reserve {
  width: 100%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark)) !important;
  border: none !important;
  color: var(--white) !important;
  font-weight: 600 !important;
  border-radius: var(--radius-sm) !important;
  transition: all var(--transition-normal) !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25) !important;
}

.btn-reserve:hover {
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35) !important;
  transform: translateY(-1px);
}

.btn-disabled-full {
  width: 100%;
  border-radius: var(--radius-sm) !important;
}

.reservation-section-card {
  margin-top: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.reservation-card {
  padding: 16px 16px 16px 20px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  transition: all var(--transition-normal);
  border-left: 4px solid transparent;
}

.reservation-card:hover {
  box-shadow: var(--shadow-md);
}

.reservation-card--warning {
  border-left-color: var(--warning);
}

.reservation-card--primary {
  border-left-color: var(--accent);
}

.reservation-card--success {
  border-left-color: var(--primary);
}

.reservation-card--danger {
  border-left-color: var(--danger);
}

.reservation-card--info {
  border-left-color: var(--text-muted);
}

.reservation-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reservation-card__name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.reservation-card__time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.reservation-card__actions {
  display: flex;
  gap: 8px;
}
</style>
