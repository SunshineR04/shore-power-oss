<template>
  <el-tag :type="meta.type" effect="light" round :size="size" class="status-tag">
    <span class="status-tag__dot" :style="{ background: meta.dot }"></span>
    {{ meta.label }}
  </el-tag>
</template>

<script setup>
/**
 * 统一状态标签
 * @prop status 状态 key（如 ONLINE）
 * @prop map    状态映射表，默认设备状态表 DEVICE_STATUS
 * @prop size   el-tag 尺寸 small | default
 */
import { computed } from 'vue'
import { DEVICE_STATUS, statusMeta } from '../utils/status'

const props = defineProps({
  status: { type: String, default: '' },
  map: { type: Object, default: () => DEVICE_STATUS },
  size: { type: String, default: 'default' }
})

const meta = computed(() => statusMeta(props.map, props.status))
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  font-weight: 500;
}

.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
