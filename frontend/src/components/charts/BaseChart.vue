<template>
  <div ref="chartRef" class="base-chart" :style="{ width: width, height: height }" />
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, toRefs } from 'vue'
import type { EChartsOption } from 'echarts'
import { initChart, disposeChart } from '../../utils/charts'
import type { ECharts } from 'echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  width?: string
  height?: string
}>(), {
  width: '100%',
  height: '400px'
})

const chartRef = ref<HTMLElement>()
let chart: ECharts | null = null

onMounted(() => {
  if (chartRef.value) {
    chart = initChart(chartRef.value)
    chart.setOption(props.option)
  }
})

onBeforeUnmount(() => {
  disposeChart(chart)
})

watch(() => props.option, (opt) => {
  chart?.setOption(opt, true)
}, { deep: true })
</script>

<style scoped>
.base-chart {
  min-height: 200px;
}
</style>
