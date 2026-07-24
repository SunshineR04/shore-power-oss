<template>
  <div class="user-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统用户账户、角色权限与状态</p>
      </div>
    </div>

    <el-card class="main-card">
      <div class="filter-bar">
        <div class="filter-group">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名/姓名"
            clearable
            class="filter-input"
            @clear="loadPage"
            @keyup.enter="loadPage"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="query.role" placeholder="角色" clearable class="filter-select" @change="loadPage">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="运维人员" value="OPERATOR" />
            <el-option label="普通用户" value="USER" />
          </el-select>
          <el-button type="primary" class="search-btn" @click="loadPage">搜索</el-button>
        </div>
        <el-button type="primary" class="btn-create" @click="openDialog()">
          <el-icon><Plus /></el-icon>添加用户
        </el-button>
      </div>

      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'OPERATOR' ? 'warning' : 'primary'" size="small">{{ row.role === 'ADMIN' ? '管理员' : row.role === 'OPERATOR' ? '运维人员' : '普通用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="handleToggle(row.id)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }"
        @size-change="s => { query.pageSize = s; loadPage() }"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="500" append-to-body destroy-on-close class="user-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="isEdit" /></el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '密码'" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '不修改请留空' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" class="full-width">
            <el-option label="管理员" value="ADMIN" /><el-option label="运维人员" value="OPERATOR" /><el-option label="普通用户" value="USER" />
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
import { userApi } from '../../api'

const tableData = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', role: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', password: '', realName: '', phone: '', email: '', role: 'USER' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => loadPage())

async function loadPage() {
  const res = await userApi.page(query)
  tableData.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row, password: '' } : { id: null, username: '', password: '', realName: '', phone: '', email: '', role: 'USER' })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  isEdit.value ? await userApi.update(form) : await userApi.add(form)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  loadPage()
}

async function handleDelete(id) {
  await userApi.del(id)
  ElMessage.success('删除成功')
  loadPage()
}

async function handleToggle(id) {
  await userApi.toggle(id)
  loadPage()
}
</script>

<style scoped>
.user-page {
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

.filter-input {
  width: 220px;
}

.filter-select {
  width: 150px;
}

.btn-create {
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-weight: 600;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.3);
  transition: all var(--transition-normal);
}

.btn-create:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(34, 197, 94, 0.4);
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

.user-dialog :deep(.el-dialog) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.user-dialog :deep(.el-dialog__header) {
  background: var(--bg-hover);
  padding: 18px 24px;
  margin-right: 0;
  border-bottom: 1px solid var(--border-light);
}

.user-dialog :deep(.el-dialog__title) {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 17px;
  color: var(--text-primary);
}

.user-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.user-dialog :deep(.el-form-item__label) {
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-secondary);
}

.user-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

.user-dialog :deep(.el-dialog__footer .el-button--primary) {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.25);
}

.full-width {
  width: 100%;
}
</style>
