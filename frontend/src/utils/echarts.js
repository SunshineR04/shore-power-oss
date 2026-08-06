/**
 * ECharts 按需注册模块
 *
 * 仅注册本项目实际使用的图表类型与组件，显著减小打包体积。
 * 页面统一从本模块导入 echarts 实例。
 */
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, GaugeChart, HeatmapChart, ScatterChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
  DataZoomComponent, VisualMapComponent, MarkLineComponent, MarkPointComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart, BarChart, PieChart, GaugeChart, HeatmapChart, ScatterChart,
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
  DataZoomComponent, VisualMapComponent, MarkLineComponent, MarkPointComponent,
  CanvasRenderer
])

export * from 'echarts/core'
export default echarts
