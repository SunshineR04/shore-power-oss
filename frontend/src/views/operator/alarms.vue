<template>
  <div class="page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">告警处理</h1>
        <p class="page-subtitle">查看和处理待解决的告警信息</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="filter-bar">
        <div class="filter-group">
          <el-select v-model="query.status" placeholder="处理状态" clearable class="filter-select" @change="loadPage">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已解决" value="RESOLVED" /><el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-select v-model="query.level" placeholder="告警级别" clearable class="filter-select" @change="loadPage">
            <el-option label="提示" value="INFO" /><el-option label="警告" value="WARNING" /><el-option label="严重" value="CRITICAL" />
          </el-select>
          <el-button type="primary" @click="loadPage"><el-icon><Search /></el-icon>搜索</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="alarmTime" label="告警时间" width="170" />
        <el-table-column prop="deviceName" label="设备名称" width="130" />
        <el-table-column prop="alarmLevel" label="级别" width="70">
          <template #default="{ row }">
            <el-tag :type="levelType[row.alarmLevel]" effect="dark" size="small">{{ levelMap[row.alarmLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmType" label="类型" width="80">
          <template #default="{ row }">{{ typeMap[row.alarmType] || row.alarmType }}</template>
        </el-table-column>
        <el-table-column prop="alarmContent" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="alarmValue" label="实际值" width="80" />
        <el-table-column prop="thresholdValue" label="阈值" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="sType[row.status]" size="small">{{ sMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="primary" link @click="openHandle(row)">处理</el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { alarmApi } from '../../api'

const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: 'PENDING', level: '' })
const handleVisible = ref(false)
const handleForm = reactive({ id: null, status: 'RESOLVED', remark: '' })

const levelMap = { INFO: '提示', WARNING: '警告', CRITICAL: '严重' }
const levelType = { INFO: 'primary', WARNING: 'warning', CRITICAL: 'danger' }
const typeMap = { VOLTAGE: '电压', CURRENT: '电流', TEMPERATURE: '温度', POWER: '功率', COMMUNICATION: '通信', OTHER: '其他' }
const sMap = { PENDING: '待处理', RESOLVED: '已解决', IGNORED: '已忽略' }
const sType = { PENDING: 'danger', RESOLVED: 'success', IGNORED: 'info' }

onMounted(() => loadPage())

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
</script>

<style scoped>
.page {
  padding: 24px;
}
.main-card :deep(.el-card__body) { padding: 24px; }
.filter-bar { display: flex; gap: 14px; margin-bottom: 20px; align-items: center; justify-content: flex-start; }
.filter-group { display: flex; gap: 14px; align-items: center; }
.filter-select { width: 150px; }
.data-table { border-radius: var(--radius-sm); overflow: hidden; }
.data-table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 13px; color: var(--text-secondary); background: var(--bg-hover);
}
.data-table :deep(.el-table__row:hover > td) { background: var(--primary-bg) !important; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.pagination :deep(.el-pagination.is-background .el-pager li.is-active) { background: var(--primary); border-radius: var(--radius-sm); }
</style>
