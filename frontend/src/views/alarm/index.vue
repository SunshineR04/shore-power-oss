<template>
  <div class="alarm-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">故障预警</h1>
        <p class="page-subtitle">实时监控岸电系统异常状态，及时处理告警信息</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="filter-bar">
        <div class="filter-group">
          <el-select v-model="query.status" placeholder="处理状态" clearable class="filter-select" @change="loadPage">
            <el-option label="待处理" value="PENDING" /><el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" /><el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-select v-model="query.level" placeholder="告警级别" clearable class="filter-select" @change="loadPage">
            <el-option label="提示" value="INFO" /><el-option label="警告" value="WARNING" /><el-option label="严重" value="CRITICAL" />
          </el-select>
          <el-select v-model="query.deviceId" placeholder="选择设备" clearable filterable class="filter-select" @change="loadPage">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
          <el-button type="primary" class="filter-btn" @click="loadPage"><el-icon><Search /></el-icon>搜索</el-button>
          <div class="ws-status" :class="{ connected: wsConnected }">
            <span class="ws-dot"></span>{{ wsConnected ? '实时' : '离线' }}
          </div>
        </div>
      </div>

      <el-table :data="tableData" border stripe class="alarm-table">
        <el-table-column prop="alarmTime" label="告警时间" width="170" />
        <el-table-column prop="deviceName" label="设备名称" width="130" />
        <el-table-column prop="alarmLevel" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="levelType[row.alarmLevel]" effect="dark" size="small">{{ levelMap[row.alarmLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmType" label="类型" width="90">
          <template #default="{ row }">{{ typeMap[row.alarmType] || row.alarmType }}</template>
        </el-table-column>
        <el-table-column prop="alarmContent" label="告警内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="alarmValue" label="告警值" width="100" />
        <el-table-column prop="thresholdValue" label="阈值" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ALARM_STATUS" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="primary" link @click="openHandle(row)">处理</el-button>
            <el-button v-if="row.status === 'RESOLVED' || row.status === 'IGNORED'" size="small" type="warning" link @click="reopenAlarm(row)">重新打开</el-button>
            <el-button v-if="row.handleRemark" size="small" type="info" link @click="viewRemark(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }" />
    </el-card>

    <el-dialog v-model="handleVisible" title="处理告警" width="500" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="处理方式">
          <el-radio-group v-model="handleForm.status">
            <el-radio value="RESOLVED">已解决</el-radio>
            <el-radio value="IGNORED">忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.remark" type="textarea" :rows="3" placeholder="请输入处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { alarmApi, deviceApi } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { ALARM_STATUS } from '../../utils/status'

const tableData = ref([])
const total = ref(0)
const devices = ref([])
const wsConnected = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, status: '', level: '', deviceId: null })
const handleVisible = ref(false)
const handleForm = reactive({ id: null, status: 'RESOLVED', remark: '' })
let stompClient = null
let alarmPollTimer = null

const levelMap = { INFO: '提示', WARNING: '警告', CRITICAL: '严重' }
const levelType = { INFO: 'primary', WARNING: 'warning', CRITICAL: 'danger' }
const typeMap = { VOLTAGE: '电压', CURRENT: '电流', TEMPERATURE: '温度', POWER: '功率', COMMUNICATION: '通信', OTHER: '其他' }

onMounted(async () => {
  loadPage()
  const res = await deviceApi.list()
  devices.value = res.data || []
  initWebSocket()
})

onUnmounted(() => {
  stompClient?.deactivate()
  if (alarmPollTimer) clearInterval(alarmPollTimer)
})

function startAlarmPolling() {
  // 幂等：已有轮询则先清理，避免 WS 抖动时定时器叠加
  if (alarmPollTimer) { clearInterval(alarmPollTimer); alarmPollTimer = null }
  alarmPollTimer = setInterval(() => loadPage(), 15000)
}

function initWebSocket() {
  const token = sessionStorage.getItem('token')
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      wsConnected.value = true
      if (alarmPollTimer) { clearInterval(alarmPollTimer); alarmPollTimer = null }
      stompClient.subscribe('/topic/alarm', () => loadPage())
    },
    onDisconnect: () => {
      wsConnected.value = false
      console.warn('[alarm] WebSocket 已断开，启用 15s 轮询兜底')
      startAlarmPolling()
    },
    onStompError: frame => {
      wsConnected.value = false
      console.warn('[alarm] STOMP 错误:', frame?.headers?.message || frame?.body || frame)
      startAlarmPolling()
    }
  })
  stompClient.activate()
}

async function loadPage() {
  const res = await alarmApi.page(query)
  tableData.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openHandle(row) {
  handleForm.id = row.id
  handleForm.status = 'RESOLVED'
  handleForm.remark = ''
  handleVisible.value = true
}

async function submitHandle() {
  await alarmApi.handle(handleForm.id, { status: handleForm.status, remark: handleForm.remark })
  ElMessage.success('处理成功')
  handleVisible.value = false
  loadPage()
}

async function reopenAlarm(row) {
  await ElMessageBox.confirm('确认重新打开此告警？', '提示', { type: 'warning' })
  await alarmApi.handle(row.id, { status: 'PENDING', remark: '' })
  ElMessage.success('已重新打开')
  loadPage()
}

function viewRemark(row) {
  const content = [
    row.handleRemark ? `处理备注: ${row.handleRemark}` : '',
    row.handlerName ? `处理人: ${row.handlerName}` : '',
    row.handleTime ? `处理时间: ${row.handleTime}` : ''
  ].filter(Boolean).join('\n') || '无记录'
  ElMessageBox.alert(content, '告警详情', {
    confirmButtonText: '关闭'
  })
}
</script>

<style scoped>
.alarm-page {
  font-family: var(--font-body);
  color: var(--text-primary);
}

.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out both;
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
  background: linear-gradient(180deg, var(--danger) 0%, var(--danger) 100%);
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

.main-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.main-card :deep(.el-card__body) {
  padding: 24px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select {
  width: 160px;
}

.filter-btn {
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-weight: 500;
}

.alarm-table {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.ws-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  background: var(--danger-bg);
  color: var(--danger);
  transition: all var(--transition-normal);
}

.ws-status.connected {
  background: var(--primary-bg);
  color: var(--primary);
}

.ws-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
  animation: pulse-glow 2s ease-in-out infinite;
}

.ws-status.connected .ws-dot {
  animation: none;
  opacity: 1;
}
</style>
