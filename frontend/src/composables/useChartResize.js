import { onMounted, onUnmounted } from 'vue'

/**
 * ECharts 图表尺寸自适应 composable
 *
 * 传入图表实例的 getter 数组（如 [() => chart1, () => chart2]），
 * 每个 getter 可返回单个实例，也可返回实例数组（如仪表盘组）。
 * 挂载时注册 window resize 监听，卸载时移除并销毁实例。
 */
function eachChart(get) {
  const charts = get()
  return Array.isArray(charts) ? charts : [charts]
}

export function useChartResize(getters) {
  const onResize = () => {
    getters.forEach(get => {
      eachChart(get).forEach(chart => {
        if (chart && typeof chart.resize === 'function') {
          chart.resize()
        }
      })
    })
  }

  onMounted(() => {
    window.addEventListener('resize', onResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
    getters.forEach(get => {
      eachChart(get).forEach(chart => {
        if (chart && typeof chart.dispose === 'function') {
          chart.dispose()
        }
      })
    })
  })
}
