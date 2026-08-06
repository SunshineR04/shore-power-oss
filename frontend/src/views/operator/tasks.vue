<template>
  <div class="page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">我的任务</h1>
        <p class="page-subtitle">查看和处理分配给您的维护任务</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="任务状态" clearable class="filter-select" @change="loadPage">
          <el-option label="待处理" value="PENDING" /><el-option label="已指派" value="ASSIGNED" />
          <el-option label="进行中" value="IN_PROGRESS" /><el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-select v-model="query.priority" placeholder="优先级" clearable class="filter-select filter-select--sm" @change="loadPage">
          <el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" />
        </el-select>
      </div>

      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="taskTitle" label="任务标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="taskType" label="类型" width="80">
          <template #default="{ row }">{{ typeMap[row.taskType] }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="70">
          <template #default="{ row }">
            <el-tag :type="priorityType[row.priority]" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="TASK_STATUS" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="planStartTime" label="计划开始" width="150" />
        <el-table-column prop="actualStartTime" label="实际开始" width="150" />
        <el-table-column prop="actualEndTime" label="实际结束" width="150" />
        <el-table-column prop="completionRemark" label="完成备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ASSIGNED'" size="small" type="primary" @click="handleStart(row)">接单</el-button>
            <el-button v-if="row.status === 'IN_PROGRESS'" size="small" type="success" @click="showComplete(row)">完成</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" disabled>等待指派</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }" />
    </el-card>

    <el-dialog v-model="completeVisible" title="完成任务" width="450" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="实际开始时间">
          <span style="color: var(--success); font-weight: 600;">{{ completeActualStart || '未记录' }}</span>
        </el-form-item>
        <el-form-item label="实际结束时间">
          <span style="font-weight: 600;">{{ new Date().toLocaleString() }}</span>
        </el-form-item>
        <el-form-item label="完成备注">
          <el-input v-model="completeRemark" type="textarea" :rows="3" placeholder="请填写完成说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import StatusTag from '../../components/StatusTag.vue'
import { TASK_STATUS } from '../../utils/status'

const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: '', priority: '' })
const completeVisible = ref(false)
const completeTaskId = ref(null)
const completeRemark = ref('')
const completeActualStart = ref('')

const typeMap = { INSPECTION: '巡检', REPAIR: '维修', REPLACEMENT: '更换', CALIBRATION: '校准' }
const priorityType = { LOW: 'info', MEDIUM: '', HIGH: 'warning', URGENT: 'danger' }

onMounted(() => loadPage())

async function loadPage() {
  try {
    const res = await request.get('/maintenance/my-tasks', { params: query })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {}
}

async function handleStart(row) {
  try {
    await request.put(`/maintenance/start/${row.id}`)
    ElMessage.success('已接单，请开始工作')
    loadPage()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function showComplete(row) {
  completeTaskId.value = row.id
  completeRemark.value = ''
  completeActualStart.value = row.actualStartTime || ''
  completeVisible.value = true
}

async function handleComplete() {
  try {
    await request.put('/maintenance/complete', { taskId: completeTaskId.value, remark: completeRemark.value })
    ElMessage.success('任务已完成')
    completeVisible.value = false
    loadPage()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.main-card :deep(.el-card__body) { padding: 24px; }
.filter-bar { display: flex; gap: 14px; margin-bottom: 20px; align-items: center; }
.filter-select { width: 150px; }
.filter-select--sm { width: 130px; }
.data-table { border-radius: var(--radius-sm); overflow: hidden; }
.data-table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 13px; color: var(--text-secondary); background: var(--bg-hover);
}
.data-table :deep(.el-table__row:hover > td) { background: var(--primary-bg) !important; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.pagination :deep(.el-pagination.is-background .el-pager li.is-active) { background: var(--primary); border-radius: var(--radius-sm); }
</style>
