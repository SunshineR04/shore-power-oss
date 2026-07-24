<template>
  <div class="wallet-container animate-fade-in-up">
    <div class="page-header">
      <h1 class="page-title">我的钱包</h1>
      <p class="page-subtitle">管理账户余额与充值记录</p>
    </div>

    <el-row :gutter="24" class="wallet-main-row">
      <el-col :span="12">
        <div class="wallet-card">
          <div class="wallet-card-body">
            <div class="balance-section">
              <span class="balance-label">账户余额</span>
              <div class="balance-value">
                <span class="balance-prefix">¥</span>
                <span class="balance-number">{{ formatNum(wallet.balance || 0, 2) }}</span>
              </div>
            </div>
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-label">累计充值</span>
                <span class="stat-value">¥{{ formatNum(wallet.totalRecharge || 0, 2) }}</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-label">累计消费</span>
                <span class="stat-value">¥{{ formatNum(wallet.totalSpent || 0, 2) }}</span>
              </div>
            </div>
            <button class="recharge-btn" @click="showRechargeDialog">
              <span class="recharge-btn-icon">⚡</span>
              立即充值
            </button>
          </div>
        </div>
      </el-col>

      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-icon">📋</span>
              <span class="section-title">使用记录</span>
            </div>
          </template>
          <el-table :data="usageRecords" class="styled-table" max-height="400">
            <el-table-column prop="deviceName" label="设备" />
            <el-table-column label="使用时间">
              <template #default="{row}">
                {{ row.startTime }} 至 {{ row.endTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="能耗(kWh)" width="100">
              <template #default="{row}">
                {{ formatNum(row.totalEnergy, 2) }}
              </template>
            </el-table-column>
            <el-table-column label="费用(¥)" width="100">
              <template #default="{row}">
                <span class="cost-text">¥{{ formatNum(row.totalCost, 2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{row}">
                <el-tag type="success" size="small">已支付</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card recharge-records-card">
      <template #header>
        <div class="section-header">
          <span class="section-icon">💳</span>
          <span class="section-title">充值记录</span>
        </div>
      </template>
      <el-table :data="rechargeRecords" class="styled-table">
        <el-table-column label="充值时间" prop="createTime" />
        <el-table-column label="充值金额">
          <template #default="{row}">
            <span class="amount-positive">+¥{{ formatNum(row.amount, 2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="充值方式">
          <template #default="{row}">
            {{ row.method === 'ALIPAY' ? '支付宝' : row.method === 'WECHAT' ? '微信' : row.method === 'BANK' ? '银行卡' : row.method }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag type="success" size="small">{{ row.status === 'SUCCESS' ? '成功' : row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rechargeDialogVisible" title="充值" width="400px" append-to-body class="recharge-dialog">
      <el-form :model="rechargeForm" label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeForm.amount" :min="10" :max="10000" :step="10" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-radio-group v-model="rechargeForm.method">
            <el-radio label="ALIPAY">支付宝</el-radio>
            <el-radio label="WECHAT">微信</el-radio>
            <el-radio label="BANK">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reservationApi } from '../../api'
import { ElMessage } from 'element-plus'

const wallet = ref({ balance: 0, totalRecharge: 0, totalSpent: 0 })
const usageRecords = ref([])
const rechargeRecords = ref([])
const rechargeDialogVisible = ref(false)
const rechargeForm = ref({ amount: 100, method: 'ALIPAY' })

const formatNum = (val, precision) => {
  if (val === null || val === undefined) return '-'
  return Number(val).toFixed(precision)
}

const loadWallet = async () => {
  try {
    const res = await reservationApi.getWallet()
    wallet.value = res.data || {}
  } catch {}
}

const loadUsageRecords = async () => {
  try {
    const res = await reservationApi.usageRecords()
    usageRecords.value = res.data || []
  } catch {}
}

const loadRechargeRecords = async () => {
  try {
    const res = await reservationApi.rechargeRecords()
    rechargeRecords.value = res.data || []
  } catch {}
}

const showRechargeDialog = () => {
  rechargeDialogVisible.value = true
}

const handleRecharge = async () => {
  try {
    await reservationApi.recharge(rechargeForm.value.amount, rechargeForm.value.method)
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    loadWallet()
    loadRechargeRecords()
  } catch (e) {
    ElMessage.error('充值失败')
  }
}

onMounted(() => {
  loadWallet()
  loadUsageRecords()
  loadRechargeRecords()
})
</script>

<style scoped>
.wallet-main-row {
  margin-bottom: 24px;
}

.wallet-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  position: relative;
  height: 100%;
  min-height: 320px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.wallet-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary) 0%, var(--primary-dark) 100%);
}

.wallet-card-body {
  position: relative;
  z-index: 1;
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 320px;
}

.balance-section {
  margin-bottom: 28px;
}

.balance-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  display: block;
  margin-bottom: 10px;
}

.balance-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.balance-prefix {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-secondary);
}

.balance-number {
  font-family: var(--font-mono);
  font-size: 42px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -1px;
  line-height: 1;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--bg-hover);
  border-radius: var(--radius-sm);
  padding: 16px 20px;
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-color);
  margin: 0 20px;
}

.recharge-btn {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: var(--text-primary);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: auto;
  box-shadow: 0 4px 15px rgba(34, 197, 94, 0.25);
}

.recharge-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(34, 197, 94, 0.35);
}

.recharge-btn:active {
  transform: translateY(0);
}

.recharge-btn-icon {
  font-size: 18px;
}

.section-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  height: 100%;
}

.section-card :deep(.el-card__header) {
  padding: 18px 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--border-light);
}

.section-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.recharge-records-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.recharge-records-card :deep(.el-card__header) {
  padding: 18px 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--border-light);
}

.recharge-records-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.styled-table :deep(.el-table__header-wrapper th) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--border-light) !important;
  border-bottom: 1px solid var(--border-color);
}

.styled-table :deep(.el-table__body-wrapper td) {
  font-size: 13px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-light);
}

.styled-table :deep(.el-table__row:hover > td) {
  background: rgba(34, 197, 94, 0.04) !important;
}

.cost-text {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--text-primary);
}

.amount-positive {
  font-family: var(--font-mono);
  font-weight: 700;
  color: var(--primary);
}

.recharge-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
}
</style>
