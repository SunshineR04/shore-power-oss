/**
 * ECharts 主题色板与通用样式常量
 *
 * 与 style.css 中的 --chart-* / 中性色令牌对齐，
 * 各页图表配置统一从这里取色，避免散落硬编码旧色板。
 */
export const CHART_COLORS = {
  primary: '#2563eb', // 蓝
  accent: '#0ea5e9',  // 天蓝
  success: '#10b981', // 绿
  warning: '#f59e0b', // 琥珀
  danger: '#ef4444',  // 红
  purple: '#8b5cf6'   // 紫
}

/** 图表通用文字/轴线/网格样式 */
export const CHART_TEXT = '#64748b'        // 图例/标题文字
export const CHART_LABEL = '#94a3b8'       // 轴标签弱化文字
export const CHART_AXIS_LINE = '#cbd5e1'   // 轴线
export const CHART_SPLIT_LINE = '#eef2f7'  // 分割线（浅色背景）
export const CHART_TRACK = '#e2e8f0'       // 仪表盘/进度底色轨道
