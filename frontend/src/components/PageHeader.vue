<template>
  <div class="page-header">
    <div class="page-header__text">
      <h2 class="page-header__title">{{ title }}</h2>
      <p v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.default" class="page-header__actions">
      <slot />
    </div>
  </div>
</template>

<script setup>
/**
 * 统一页面头部：标题 + 可选副标题 + 右侧操作区（默认插槽）
 * 样式与各页原 .page-header 一致（左侧品牌色竖条 + 底部渐变分隔线）
 */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
  position: relative;
}

/* 左侧品牌色竖条 */
.page-header::before {
  content: '';
  position: absolute;
  left: -20px;
  top: 4px;
  bottom: 10px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--primary), var(--accent));
  opacity: 0.85;
}

.page-header__title {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  line-height: 1.3;
  margin: 0;
}

.page-header__subtitle {
  margin-top: 4px;
  font-size: var(--font-size-md);
  color: var(--text-muted);
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding-top: 2px;
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
