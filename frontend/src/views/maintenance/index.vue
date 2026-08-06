<template>
  <div class="maintenance-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">维护调度</h1>
        <p class="page-subtitle">管理维护任务调度、指派与执行跟踪</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="filter-bar">
        <div class="filter-group">
          <el-select v-model="query.status" placeholder="任务状态" clearable class="filter-select" @change="loadPage">
            <el-option label="待处理" value="PENDING" /><el-option label="已指派" value="ASSIGNED" />
            <el-option label="进行中" value="IN_PROGRESS" /><el-option label="已完成" value="COMPLETED" />
          </el-select>
          <el-select v-model="query.priority" placeholder="优先级" clearable class="filter-select filter-select--sm" @change="loadPage">
            <el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" />
          </el-select>
        </div>
        <el-button v-if="store.isSuperAdmin()" type="primary" class="btn-create" @click="openDialog()">
          <el-icon><Plus /></el-icon>新建任务
        </el-button>
      </div>

      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="taskTitle" label="任务标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="taskType" label="类型" width="90">
          <template #default="{ row }">{{ taskTypeMap[row.taskType] }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="priorityType[row.priority]" size="small">{{ priorityMap[row.priority] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="TASK_STATUS" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="指派人" width="100" />
        <el-table-column prop="planStartTime" label="计划开始" width="155">
            <template #default="{ row }">{{ fmtDateTime(row.planStartTime) }}</template>
          </el-table-column>
        <el-table-column prop="planEndTime" label="计划结束" width="155">
            <template #default="{ row }">{{ fmtDateTime(row.planEndTime) }}</template>
          </el-table-column>
        <el-table-column prop="actualStartTime" label="实际开始" width="150">
            <template #default="{ row }">{{ fmtDateTime(row.actualStartTime) }}</template>
          </el-table-column>
        <el-table-column prop="actualEndTime" label="实际结束" width="150">
            <template #default="{ row }">{{ fmtDateTime(row.actualEndTime) }}</template>
          </el-table-column>
        <el-table-column prop="completionRemark" label="完成备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm v-if="store.isSuperAdmin()" title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="600" append-to-body destroy-on-close class="task-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务标题" prop="taskTitle"><el-input v-model="form.taskTitle" /></el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" class="form-select-full">
            <el-option label="巡检" value="INSPECTION" /><el-option label="维修" value="REPAIR" />
            <el-option label="更换" value="REPLACEMENT" /><el-option label="校准" value="CALIBRATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" class="form-select-full">
            <el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备">
          <el-select v-model="form.deviceId" filterable clearable class="form-select-full" placeholder="选择关联设备">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务内容"><el-input v-model="form.taskContent" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12"><el-form-item label="计划开始"><el-date-picker v-model="form.planStartTime" type="datetime" class="form-select-full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="计划结束"><el-date-picker v-model="form.planEndTime" type="datetime" class="form-select-full" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="指派人">
          <el-select v-model="form.assigneeId" filterable clearable class="form-select-full" placeholder="选择运维人员（选填，选后自动指派）">
            <el-option v-for="u in operators" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { maintenanceApi, deviceApi, userApi } from '../../api'
import { useUserStore } from '../../store/user'
import StatusTag from '../../components/StatusTag.vue'
import { TASK_STATUS } from '../../utils/status'

const store = useUserStore()

/** ISO 时间 → 本地 'YYYY-MM-DD HH:mm'，兼容后端返回格式 */
function fmtDateTime(s) {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return String(s).replace('T', ' ').slice(0, 16)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
const tableData = ref([])
const total = ref(0)
const devices = ref([])
const operators = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, status: '', priority: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: null, taskTitle: '', taskType: '', priority: 'MEDIUM', deviceId: null, taskContent: '', planStartTime: null, planEndTime: null, assigneeId: null })

const taskTypeMap = { INSPECTION: '巡检', REPAIR: '维修', REPLACEMENT: '更换', CALIBRATION: '校准' }
const priorityMap = { LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急' }
const priorityType = { LOW: 'info', MEDIUM: 'primary', HIGH: 'warning', URGENT: 'danger' }

const rules = {
  taskTitle: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

onMounted(async () => {
  loadPage()
  const [devRes, userRes] = await Promise.all([deviceApi.list(), userApi.page({ pageSize: 100 })])
  devices.value = devRes.data || []
  operators.value = (userRes.data?.records || []).filter(u => u.role === 'OPERATOR')
})

async function loadPage() {
  const res = await maintenanceApi.page(query)
  tableData.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, taskTitle: '', taskType: '', priority: 'MEDIUM', deviceId: null, taskContent: '', planStartTime: null, planEndTime: null, assigneeId: null })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  isEdit.value ? await maintenanceApi.update(form) : await maintenanceApi.add(form)
  ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
  dialogVisible.value = false
  loadPage()
}

async function handleDelete(id) {
  await maintenanceApi.del(id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<style scoped>
.maintenance-page {
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
  background: linear-gradient(180deg, var(--primary) 0%, var(--primary-dark) 100%);
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select {
  width: 150px;
}

.filter-select--sm {
  width: 130px;
}

.btn-create {
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-weight: 600;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: all var(--transition-normal);
}

.btn-create:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
}

.btn-create:active {
  transform: translateY(0);
}

.data-table {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-select-full {
  width: 100%;
}

.form-select-full :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.task-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.task-dialog :deep(.el-dialog__header) {
  background: var(--bg-hover);
  padding: 18px 24px;
  margin-right: 0;
  border-bottom: 1px solid var(--border-light);
}

.task-dialog :deep(.el-dialog__title) {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 17px;
  color: var(--text-primary);
}

.task-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.task-dialog :deep(.el-form-item__label) {
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-secondary);
}

.task-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

.task-dialog :deep(.el-dialog__footer .el-button--primary) {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}
</style>
