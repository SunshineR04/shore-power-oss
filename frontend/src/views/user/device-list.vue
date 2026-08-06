<template>
  <div class="device-list-container animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">充电桩列表</h1>
        <p class="page-subtitle">查看所有岸电充电桩设备，选择可用设备进行预约充电</p>
      </div>
      <div class="header-search">
        <el-input v-model="keyword" placeholder="搜索设备名称或编号" class="search-input" clearable @clear="loadDevices" @keyup.enter="loadDevices">
          <template #append>
            <el-button @click="loadDevices">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div class="device-grid">
      <div class="device-card" v-for="device in devices" :key="device.id" @click="viewDetail(device.id)">
        <div class="card-header">
          <div class="device-name-section">
            <h3 class="device-name">{{ device.deviceName }}</h3>
            <p class="device-code">{{ device.deviceCode }}</p>
          </div>
          <el-tag :type="getStatusType(device.status)" effect="dark" class="status-tag">{{ getStatusText(device.status) }}</el-tag>
        </div>
        <div class="card-divider"></div>
        <div class="card-info">
          <div class="info-item">
            <el-icon><Location /></el-icon>
            <span>{{ device.location || '未设置' }}</span>
          </div>
          <div class="info-item">
            <el-icon><Lightning /></el-icon>
            <span>额定功率: {{ device.ratedPower || '-' }} kW</span>
          </div>
          <div class="info-item">
            <el-icon><Odometer /></el-icon>
            <span>额定电压: {{ device.ratedVoltage || '-' }} V</span>
          </div>
        </div>
        <div class="card-action">
          <el-button v-if="canReserveDevice(device)" type="primary" class="btn-reserve" @click.stop="showReserveDialog(device)">
            立即预约
          </el-button>
          <el-button v-else-if="isDeviceReserved(device.id)" type="info" disabled class="btn-disabled">
            已有预约
          </el-button>
          <el-button v-else type="info" disabled class="btn-disabled">
            {{ getStatusText(device.status) }}
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="devices.length === 0" description="暂无设备" />

    <el-dialog v-model="reserveDialogVisible" title="预约充电" width="600px" append-to-body class="reserve-dialog">
      <el-form :model="reserveForm" label-width="100px">
        <el-form-item label="设备名称">
          <el-input v-model="reserveForm.deviceName" disabled />
        </el-form-item>
        <el-form-item label="选择船舶">
          <el-select v-model="reserveForm.shipId" placeholder="选择预约船舶（可选）" class="form-select" clearable>
            <el-option v-for="ship in myShips" :key="ship.id" :label="ship.shipName" :value="ship.id">
              <span>{{ ship.shipName }}</span>
              <span class="ship-type-label">{{ getShipTypeText(ship.shipType) }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始时段">
          <el-select v-model="reserveForm.startSlot" placeholder="选择开始时段" class="form-select" @change="onStartSlotChange">
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
            class="form-select"
            :disabled-date="disabledEndDate"
            @change="onEndDateChange"
          />
        </el-form-item>
        <el-form-item label="结束时段">
          <el-select v-model="reserveForm.endSlot" placeholder="选择结束时段" class="form-select">
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
defineOptions({ name: 'UserDevices' })

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { deviceApi, reservationApi, shipApi } from '../../api'
import { DEVICE_STATUS, statusMeta } from '../../utils/status'
import { ElMessage } from 'element-plus'

const router = useRouter()
const devices = ref([])
const keyword = ref('')
const reserveDialogVisible = ref(false)
const myShips = ref([])
const reserveForm = ref({
  deviceId: null,
  deviceName: '',
  shipId: null,
  startDate: null,
  endDate: null,
  startSlot: null,
  endSlot: null
})
const reservedDeviceIds = ref(new Set())

const getShipTypeText = (type) => {
  const map = {
    CARGO: '货船', CONTAINER: '集装箱船', TANKER: '油轮',
    PASSENGER: '客船', BULK: '散货船', RO_RO: '滚装船',
    FISHING: '渔船', OTHER: '其他'
  }
  return map[type] || type || '未知'
}

const loadMyShips = async () => {
  try {
    const res = await shipApi.list()
    myShips.value = res.data || []
  } catch (e) {
    ElMessage.error('加载船舶列表失败')
  }
}

const loadDevices = async () => {
  try {
    const res = await deviceApi.list()
    const all = res.data || []
    const pileTypes = ['SMALL_YACHT', 'INLAND_CARGO', 'COASTAL_CARGO', 'CONTAINER_SHIP', 'TANKER']
    devices.value = all.filter(d => pileTypes.includes(d.deviceType))
    await Promise.all([loadReservations(), loadMyShips()])
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  }
}

const loadReservations = async () => {
  try {
    const res = await reservationApi.list()
    const reservations = res.data || []
    reservedDeviceIds.value = new Set()
    reservations.forEach(r => {
      if (['PENDING', 'CONFIRMED', 'IN_USE', 'PENDING_PAYMENT'].includes(r.status)) {
        reservedDeviceIds.value.add(r.deviceId)
      }
    })
  } catch (e) {
    ElMessage.error('加载预约状态失败')
  }
}

const isDeviceReserved = (deviceId) => {
  return reservedDeviceIds.value.has(deviceId)
}

const canReserveDevice = (device) => {
  return device.status === 'ONLINE' && !isDeviceReserved(device.id)
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

const getStatusType = (status) => statusMeta(DEVICE_STATUS, status).type
const getStatusText = (status) => statusMeta(DEVICE_STATUS, status).label

const viewDetail = (id) => {
  router.push(`/user/device/${id}`)
}

const showReserveDialog = (device) => {
  const today = new Date()
  reserveForm.value = {
    deviceId: device.id,
    deviceName: device.deviceName,
    shipId: null,
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

    await reservationApi.create(reserveForm.value.deviceId, startTime, endTime, reserveForm.value.shipId)
    ElMessage.success('预约成功')
    reserveDialogVisible.value = false
    loadReservations()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.response?.data?.message || '预约失败')
  }
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.device-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              border-color 0.3s;
  position: relative;
  overflow: hidden;
}

.device-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary), var(--accent));
  opacity: 0;
  transition: opacity 0.3s;
}

.device-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.12), 0 4px 12px rgba(15, 23, 42, 0.08);
  border-color: var(--primary);
}

.device-card:hover::before {
  opacity: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.device-name-section {
  flex: 1;
  min-width: 0;
}

.device-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-code {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  font-family: 'Courier New', monospace;
  letter-spacing: 0.3px;
}

.status-tag {
  flex-shrink: 0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.card-divider {
  height: 1px;
  background: var(--border-light);
  margin: 14px 0;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.info-item .el-icon {
  color: var(--primary);
  font-size: 15px;
  flex-shrink: 0;
}

.card-action {
  margin-top: 18px;
  text-align: center;
}

.btn-reserve {
  width: 100%;
  border-radius: var(--radius-sm);
  font-weight: 600;
  letter-spacing: 0.3px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  border: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-reserve:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.btn-disabled {
  width: 100%;
  border-radius: var(--radius-sm);
}

.form-select {
  width: 100%;
}

.ship-type-label {
  float: right;
  color: var(--text-muted);
  font-size: 13px;
}

.reserve-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
}

@media (max-width: 1400px) {
  .device-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1000px) {
  .device-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .header-search {
    margin-left: 0;
    width: 100%;
  }
  .search-input {
    width: 100%;
  }
}

@media (max-width: 600px) {
  .device-grid {
    grid-template-columns: 1fr;
  }
}
</style>
