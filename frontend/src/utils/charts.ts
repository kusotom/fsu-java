import * as echarts from 'echarts'

export { echarts }

/**
 * 创建 ECharts 实例并绑定到 DOM 元素 (FE-INFRA-001)。
 */
export function initChart(el: HTMLElement): echarts.ECharts {
  return echarts.init(el)
}

/**
 * 销毁 ECharts 实例。
 */
export function disposeChart(chart: echarts.ECharts | null): void {
  if (chart && !chart.isDisposed()) {
    chart.dispose()
  }
}
