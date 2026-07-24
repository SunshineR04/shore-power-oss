<template>
  <div class="page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">通知中心</h1>
        <p class="page-subtitle">查看系统通知和任务指派消息</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-card class="main-card">
      <div class="toolbar">
        <span class="toolbar-title">共 {{ total }} 条通知</span>
        <el-button size="small" @click="markAllRead" :disabled="total === 0">全部标记已读</el-button>
      </div>

      <div v-if="!list.length" class="empty">
        <el-empty description="暂无通知" :image-size="80" />
      </div>

      <div v-else class="notif-list">
        <div v-for="item in list" :key="item.id" class="notif-item" :class="{ unread: !item.isRead }" @click="handleClick(item)">
          <div class="notif-dot" v-if="!item.isRead"></div>
          <div class="notif-body">
            <div class="notif-title">{{ item.title }}</div>
            <div class="notif-content">{{ item.content }}</div>
            <div class="notif-time">{{ item.createTime }}</div>
          </div>
        </div>
      </div>

      <el-pagination class="pagination" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; loadPage() }" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'

const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20 })

onMounted(() => loadPage())

async function loadPage() {
  try {
    const res = await request.get('/notification/list', { params: query })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {}
}

async function markAllRead() {
  try {
    await request.put('/notification/read-all')
    list.value.forEach(n => { n.isRead = 1 })
  } catch {}
}

async function handleClick(item) {
  if (!item.isRead) {
    try {
      await request.put(`/notification/read/${item.id}`)
      item.isRead = 1
    } catch {}
  }
  if (item.refType === 'MAINTENANCE') {
    router.push('/operator/tasks')
  }
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.main-card :deep(.el-card__body) { padding: 24px; }
.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border-light);
}
.toolbar-title { font-size: 14px; color: var(--text-secondary); }
.empty { padding: 40px 0; }
.notif-list { display: flex; flex-direction: column; gap: 4px; }
.notif-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 16px; border-radius: var(--radius-sm);
  cursor: pointer; transition: background 0.2s;
}
.notif-item:hover { background: var(--bg-hover); }
.notif-item.unread { background: var(--primary-bg); }
.notif-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--primary); flex-shrink: 0;
  margin-top: 6px;
}
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.notif-item.unread .notif-title { color: var(--primary); }
.notif-content { font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
.notif-time { font-size: 12px; color: var(--text-muted); }
.pagination { margin-top: 20px; justify-content: flex-end; }
.pagination :deep(.el-pagination.is-background .el-pager li.is-active) { background: var(--primary); border-radius: var(--radius-sm); }
</style>
