import { onMounted, onUnmounted } from 'vue'

/**
 * ECharts 图表尺寸自适应 composable
 *
 * 传入图表实例的 getter 数组（如 [() => chart1, () => chart2]），
 * 挂载时注册 window resize 监听，卸载时移除并销毁实例。
 */
export function useChartResize(getters) {
  const onResize = () => {
    getters.forEach(get => {
      const chart = get()
      if (chart && typeof chart.resize === 'function') {
        chart.resize()
      }
    })
  }

  onMounted(() => {
    window.addEventListener('resize', onResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
    getters.forEach(get => {
      const chart = get()
      if (chart && typeof chart.dispose === 'function') {
        chart.dispose()
      }
    })
  })
}
