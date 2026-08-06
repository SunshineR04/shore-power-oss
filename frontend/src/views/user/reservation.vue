<template>
  <div class="reservation-container animate-fade-in-up">
    <div class="page-header">
      <div class="page-header-content">
        <h1 class="page-title">我的预约</h1>
        <p class="page-subtitle">管理您的岸电预约记录，查看状态与费用详情</p>
      </div>
      <div class="page-header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="tabs-wrapper">
        <el-tabs v-model="activeTab" @tab-change="loadReservations" class="reservation-tabs">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="待确认" name="PENDING" />
          <el-tab-pane label="已确认" name="CONFIRMED" />
          <el-tab-pane label="使用中" name="IN_USE" />
          <el-tab-pane label="待支付" name="PENDING_PAYMENT" />
          <el-tab-pane label="已完成" name="COMPLETED" />
          <el-tab-pane label="已取消" name="CANCELLED" />
        </el-tabs>
      </div>

      <el-table :data="filteredReservations" class="reservation-table">
        <el-table-column prop="reservationNo" label="预约编号" width="180" />
        <el-table-column prop="deviceName" label="设备名称" />
        <el-table-column prop="shipName" label="关联船舶">
          <template #default="{row}">
            <span class="ship-name">{{ row.shipName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" />
        <el-table-column label="预约时间" width="180">
          <template #default="{row}">
            <span class="time-range">{{ row.startTime }} 至 {{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实际使用时间" width="180">
          <template #default="{row}">
            <span v-if="row.usageStartTime" class="time-range">{{ row.usageStartTime }} 至 {{ row.usageEndTime || '使用中' }}</span>
            <span v-else class="time-range text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="getStatusType(row.status)" class="status-tag">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{row}">
            <div class="action-buttons">
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleConfirm(row.id)">确认</el-button>
              <el-button v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'" size="small" type="danger" @click="handleCancel(row.id)">取消</el-button>
              <el-button v-if="row.status === 'CONFIRMED'" size="small" type="primary" @click="handleStart(row.id)">开始使用</el-button>
              <el-button v-if="row.status === 'IN_USE'" size="small" type="warning" @click="handleEnd(row.id)">结束使用</el-button>
              <el-button v-if="row.status === 'PENDING_PAYMENT'" size="small" type="primary" @click="handlePayNow(row)">立即支付</el-button>
              <el-button v-if="row.status === 'COMPLETED'" size="small" @click="viewDetail(row.id)">查看详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredReservations.length === 0" description="暂无预约记录" class="empty-state" />
    </el-card>

    <el-dialog v-model="payDialogVisible" title="支付确认" width="460px" append-to-body class="pay-dialog" :close-on-click-modal="false">
      <div v-if="payDialogData" class="pay-dialog-body">
        <div class="pay-amount-section">
          <div class="pay-amount-label">本次费用</div>
          <div class="pay-amount-value">¥{{ formatCost(payDialogData.actualCost) }}</div>
        </div>
        <el-descriptions :column="1" border class="pay-descriptions">
          <el-descriptions-item label="设备名称">{{ payDialogData.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="预约编号">{{ payDialogData.reservationNo }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="!paymentQrData" class="pay-method-section">
          <div class="pay-method-label">选择支付方式</div>
          <el-radio-group v-model="payMethod" class="pay-method-group">
            <el-radio value="ALIPAY" class="pay-method-item">
              <span class="pay-method-icon">支付宝</span>
            </el-radio>
            <el-radio value="WECHAT" class="pay-method-item">
              <span class="pay-method-icon">微信支付</span>
            </el-radio>
          </el-radio-group>
        </div>

        <div v-if="paymentQrData" class="pay-qr-section">
          <div class="pay-qr-hint">{{ payMethod === 'ALIPAY' ? '支付宝' : '微信' }}扫码支付</div>
          <div class="pay-qr-wrapper">
            <img :src="paymentQrData.qrCodeUrl" alt="支付二维码" class="pay-qr-img" />
          </div>
          <div class="pay-qr-amount">¥{{ formatCost(paymentQrData.amount) }}</div>
        </div>
      </div>
      <template #footer>
        <div class="pay-dialog-footer">
          <el-button v-if="!paymentQrData" @click="payDialogVisible = false">取消</el-button>
          <el-button v-if="!paymentQrData" type="primary" @click="createPayment">生成支付码</el-button>
          <template v-else>
            <el-button @click="paymentQrData = null; payMethod = 'ALIPAY'">重新选择</el-button>
            <el-button type="success" @click="confirmPay" :loading="paying">我已支付</el-button>
          </template>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="预约详情" width="600px" append-to-body class="detail-dialog">
      <el-descriptions :column="2" border v-if="detailData" class="detail-descriptions">
        <el-descriptions-item label="预约编号">{{ detailData.reservationNo }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="关联船舶">{{ detailData.shipName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailData.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailData.endTime }}</el-descriptions-item>
        <el-descriptions-item label="实际使用时间" v-if="detailData.usageStartTime">
          <template #default>
            {{ detailData.usageStartTime }} 至 {{ detailData.usageEndTime || '使用中' }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预计费用">¥{{ detailData.estimatedCost || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际费用">¥{{ detailData.actualCost || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="billingDialogVisible" title="费用结算" width="500px" append-to-body class="billing-dialog">
      <el-descriptions :column="2" border v-if="billingData" class="billing-descriptions">
        <el-descriptions-item label="设备名称">{{ billingData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="预约编号">{{ billingData.reservationNo }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ billingData.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ billingData.endTime }}</el-descriptions-item>
        <el-descriptions-item label="使用时长">{{ billingData.usageMinutes || '-' }} 分钟</el-descriptions-item>
        <el-descriptions-item label="总能耗">{{ billingData.totalEnergy }} kWh</el-descriptions-item>
        <el-descriptions-item label="电度电费">¥{{ billingData.energyCost || '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="服务费">¥{{ billingData.serviceFee || '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="总费用" :content-style="{fontWeight:'bold',color:'#e74c3c'}">¥{{ billingData.totalCost }}</el-descriptions-item>
      </el-descriptions>
      <div class="billing-action">
        <el-button type="primary" @click="handlePay(billingData.usageRecordId)">立即支付</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'UserReservations' })

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { reservationApi } from '../../api'
import { getStatusText, getStatusType, formatCost } from './reservation-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('all')
const reservations = ref([])
const detailDialogVisible = ref(false)
const billingDialogVisible = ref(false)
const detailData = ref(null)
const billingData = ref(null)
const payDialogVisible = ref(false)
const payDialogData = ref(null)
const payMethod = ref('ALIPAY')
const paymentQrData = ref(null)
const paying = ref(false)

const filteredReservations = computed(() => {
  if (activeTab.value === 'all') return reservations.value
  return reservations.value.filter(r => r.status === activeTab.value)
})

const loadReservations = async () => {
  try {
    const res = await reservationApi.list()
    reservations.value = res.data || []
  } catch (e) {
    ElMessage.error('加载预约列表失败')
  }
}

const handleConfirm = async (id) => {
  try {
    await reservationApi.confirm(id)
    ElMessage.success('已确认')
    loadReservations()
  } catch (e) {
    ElMessage.error('确认失败')
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '提示', { type: 'warning' })
    await reservationApi.cancel(id)
    ElMessage.success('已取消')
    loadReservations()
  } catch {}
}

const handleStart = async (id) => {
  try {
    await reservationApi.startUsage(id)
    ElMessage.success('已开始使用')
    loadReservations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleEnd = async (id) => {
  try {
    const res = await reservationApi.endUsage(id)
    ElMessage.success('使用已结束，请尽快完成支付')
    loadReservations()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handlePayNow = async (row) => {
  payDialogData.value = {
    id: row.id,
    deviceName: row.deviceName,
    reservationNo: row.reservationNo,
    actualCost: row.actualCost || row.estimatedCost
  }
  payMethod.value = 'ALIPAY'
  paymentQrData.value = null
  payDialogVisible.value = true
}

const createPayment = async () => {
  try {
    const res = await reservationApi.pay(payDialogData.value.id, payMethod.value)
    paymentQrData.value = res.data || {}
    ElMessage.success('支付码已生成')
  } catch (e) {
    ElMessage.error(e?.msg || '支付失败')
  }
}

const confirmPay = async () => {
  if (!paymentQrData.value?.tradeNo) return
  paying.value = true
  try {
    await reservationApi.payCallback(paymentQrData.value.tradeNo)
    ElMessage.success('支付成功')
    payDialogVisible.value = false
    loadReservations()
  } catch {
    // request.js 拦截器已显示错误提示，此处不再重复弹窗
  } finally {
    paying.value = false
  }
}

const viewDetail = async (id) => {
  try {
    const res = await reservationApi.detail(id)
    detailData.value = res.data
    detailDialogVisible.value = true
  } catch {}
}

onMounted(() => {
  loadReservations()
})
</script>

<style scoped>
.main-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.main-card :deep(.el-card__body) {
  padding: 0;
}

.tabs-wrapper {
  padding: 20px 24px 0;
  border-bottom: 1px solid var(--border-light);
}

.reservation-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.reservation-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.reservation-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  transition: color 0.25s ease;
  border-bottom: 2px solid transparent;
}

.reservation-tabs :deep(.el-tabs__item:hover) {
  color: var(--primary);
}

.reservation-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary);
  font-weight: 600;
}

.reservation-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary);
  height: 2.5px;
  border-radius: 2px;
}

.reservation-table {
  width: 100%;
  padding: 0 8px;
}

.reservation-table :deep(.el-table__header th) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background-color: var(--border-light);
  border-bottom: none;
}

.reservation-table :deep(.el-table__row) {
  transition: background-color 0.2s ease;
}

.reservation-table :deep(.el-table__row:hover > td) {
  background-color: rgba(34, 197, 94, 0.03) !important;
}

.ship-name {
  color: var(--text-primary);
}

.time-range {
  font-size: 13px;
  color: var(--text-secondary);
}

.status-tag {
  font-weight: 500;
  border-radius: 6px;
}

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
}

.empty-state {
  padding: 60px 0;
}

.empty-state :deep(.el-empty__description p) {
  color: var(--text-muted);
  font-size: 14px;
}

.pay-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.pay-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  padding: 20px 24px;
  margin-right: 0;
}

.pay-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 17px;
}

.pay-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(241, 245, 249, 0.7);
}

.pay-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: var(--text-primary);
}

.pay-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.pay-dialog-body {
  text-align: center;
}

.pay-amount-section {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.06), rgba(6, 182, 212, 0.04));
  border: 1px solid rgba(34, 197, 94, 0.12);
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 20px;
}

.pay-amount-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.pay-amount-value {
  font-family: var(--font-mono);
  font-size: 36px;
  font-weight: 700;
  color: var(--warning);
  letter-spacing: -1px;
}

.pay-descriptions {
  margin-bottom: 16px;
}

.pay-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--text-primary) !important;
  background-color: var(--bg-elevated) !important;
  min-width: 90px;
}

.pay-descriptions :deep(.el-descriptions__content) {
  color: var(--text-primary) !important;
  background: transparent !important;
}

.balance-text {
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 15px;
}

.balance-sufficient {
  color: var(--primary);
}

.balance-insufficient {
  color: var(--danger);
}

.insufficient-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: var(--radius-sm);
  color: var(--danger);
  font-size: 13px;
  font-weight: 500;
}

.pay-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pay-dialog-footer .el-button {
  font-weight: 500;
  border-radius: var(--radius-sm);
  min-width: 90px;
}

.detail-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
}

.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-light);
  padding: 18px 24px;
}

.detail-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 17px;
}

.detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.detail-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--text-primary) !important;
  background-color: var(--bg-elevated) !important;
}

.detail-descriptions :deep(.el-descriptions__content) {
  color: var(--text-primary) !important;
  background: transparent !important;
}

.billing-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
}

.billing-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-light);
  padding: 18px 24px;
}

.billing-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 17px;
}

.billing-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.billing-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--text-primary) !important;
  background-color: var(--bg-elevated) !important;
}

.billing-descriptions :deep(.el-descriptions__content) {
  color: var(--text-primary) !important;
  background: transparent !important;
}

.billing-action {
  margin-top: 24px;
  text-align: center;
}

.billing-action .el-button {
  font-weight: 500;
  border-radius: var(--radius-sm);
  min-width: 120px;
}
</style>
