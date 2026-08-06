<template>
  <div class="device-page animate-fade-in-up">
    <div class="page-header">
      <div class="page-header-text">
        <h2 class="page-title">设备管理</h2>
        <p class="page-subtitle">管理岸电桩设备信息，查看设备状态与参数配置</p>
      </div>
    </div>

    <el-card class="main-card">
      <div class="search-bar">
        <div class="search-bar-left">
          <el-input v-model="query.keyword" placeholder="搜索设备名称/编号" clearable class="search-input" @clear="loadPage" @keyup.enter="loadPage">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.status" placeholder="设备状态" clearable class="search-select" @change="loadPage">
            <el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" />
            <el-option label="故障" value="FAULT" /><el-option label="维护中" value="MAINTENANCE" />
          </el-select>
          <el-select v-model="query.type" placeholder="岸电桩类型" clearable class="search-select-wide" @change="loadPage">
            <el-option v-for="t in pileTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-button type="primary" class="search-btn" @click="loadPage">搜索</el-button>
        </div>
        <el-button v-if="store.isAdmin()" type="primary" class="add-btn" @click="openDialog()">
          <el-icon><Plus /></el-icon>添加设备
        </el-button>
      </div>

      <el-table :data="tableData" border stripe class="device-table">
        <el-table-column prop="deviceCode" label="设备编号" width="130" />
        <el-table-column prop="deviceName" label="设备名称" min-width="150" />
        <el-table-column prop="deviceType" label="岸电桩类型" width="140">
          <template #default="{ row }">{{ typeMap[row.deviceType] || row.deviceType }}</template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ratedVoltage" label="额定电压(V)" width="120" />
        <el-table-column prop="ratedCurrent" label="额定电流(A)" width="120" />
        <el-table-column prop="ratedPower" label="额定功率(kW)" width="120" />
        <el-table-column label="操作" width="180" fixed="right" v-if="store.isAdmin()">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, sizes, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }"
        @size-change="s => { query.pageSize = s; loadPage() }" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备' : '添加设备'" width="600" append-to-body destroy-on-close class="device-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="device-form">
        <el-form-item label="设备编号" prop="deviceCode"><el-input v-model="form.deviceCode" placeholder="如 SP-YT-003" /></el-form-item>
        <el-form-item label="设备名称" prop="deviceName"><el-input v-model="form.deviceName" placeholder="如 3号游艇桩" /></el-form-item>
        <el-form-item label="岸电桩类型" prop="deviceType">
          <el-select v-model="form.deviceType" class="full-width" @change="handleTypeChange">
            <el-option v-for="t in pileTypes" :key="t.value" :label="t.label" :value="t.value">
              <span>{{ t.label }}</span>
              <span class="option-desc">{{ t.desc }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="安装位置"><el-input v-model="form.location" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="额定电压(V)" label-width="90px">
              <el-input-number v-model="form.ratedVoltage" :min="0" :step="10" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="额定电流(A)" label-width="90px">
              <el-input-number v-model="form.ratedCurrent" :min="0" :step="5" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="额定功率(kW)" label-width="100px">
              <el-input-number v-model="form.ratedPower" :min="0" :step="5" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生产厂商"><el-input v-model="form.manufacturer" /></el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" />
            <el-option label="故障" value="FAULT" /><el-option label="维护中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { deviceApi } from '../../api'
import { useUserStore } from '../../store/user'

const store = useUserStore()
const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', type: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, deviceCode: '', deviceName: '', deviceType: '', location: '', ratedVoltage: null, ratedCurrent: null, ratedPower: null, manufacturer: '', status: 'OFFLINE' })

const pileTypes = ref([])
const typeMap = computed(() => Object.fromEntries((pileTypes.value || []).map(t => [t.value, t.label])))

const statusMap = { ONLINE: '在线', OFFLINE: '离线', FAULT: '故障', MAINTENANCE: '维护中', IN_USE: '使用中' }
const statusType = { ONLINE: 'success', OFFLINE: 'info', FAULT: 'danger', MAINTENANCE: 'warning', IN_USE: 'primary' }

const rules = {
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择岸电桩类型', trigger: 'change' }]
}

function handleTypeChange(val) {
  const t = (pileTypes.value || []).find(p => p.value === val)
  if (t) {
    form.ratedVoltage = t.voltage
    form.ratedCurrent = t.current
    form.ratedPower = t.power
  }
}

async function loadTypes() {
  try {
    const res = await deviceApi.types()
    pileTypes.value = res.data?.pileTypes || []
  } catch {}
}

onMounted(async () => {
  await loadTypes()
  loadPage()
})

async function loadPage() {
  const res = await deviceApi.page(query)
  tableData.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, deviceCode: '', deviceName: '', deviceType: '', location: '', ratedVoltage: null, ratedCurrent: null, ratedPower: null, manufacturer: '', status: 'OFFLINE' })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    isEdit.value ? await deviceApi.update(form) : await deviceApi.add(form)
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadPage()
  } finally { saving.value = false }
}

async function handleDelete(id) {
  await deviceApi.del(id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<style scoped>
.device-page {
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
  background: linear-gradient(180deg, var(--primary) 0%, var(--primary-dark) 100%);
  border-radius: 2px 0 0 2px;
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
}

.main-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.main-card :deep(.el-card__body) {
  padding: 24px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
}

.search-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.search-select {
  width: 140px;
}

.search-select-wide {
  width: 160px;
}

.search-btn {
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-weight: 500;
}

.add-btn {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-weight: 600;
  padding: 8px 20px;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
  transition: all var(--transition-normal);
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.4);
  background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary) 100%);
}

.device-table {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.device-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.device-dialog :deep(.el-dialog__header) {
  background: var(--bg-hover);
  border-bottom: 1px solid var(--border-light);
  padding: 18px 24px;
  margin: 0;
}

.device-dialog :deep(.el-dialog__title) {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.device-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.device-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

.device-form :deep(.el-form-item__label) {
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-secondary);
}

.device-form :deep(.el-input__wrapper),
.device-form :deep(.el-input-number) {
  border-radius: var(--radius-sm);
}

.full-width {
  width: 100%;
}

.option-desc {
  float: right;
  color: var(--text-muted);
  font-size: 12px;
}

.animate-fade-in-up {
  animation: fadeInUp 0.45s ease-out both;
}
</style>
