# FE-REALTIME-VALUE-UNIT-STYLE-FIX-001 当前值单位弱化显示

**日期**: 2026-06-03 | **类型**: 前端样式修正

## 执行摘要

FE-REALTIME-VALUE-UNIT-STYLE-FIX-001 完成。单位与数值分离渲染，单位字体弱化。

## 修复

### adapter: displayUnit 独立字段

`BusinessRealtimePoint` 新增 `displayUnit: string`。`buildDisplayUnit()` 计算展示单位:
- 文本型值 (CLOSE/DRY): 无单位
- 数值型: 后端 unit > sensorName 推断 > 空

`buildDisplayValueWithUnit` 简化为仅返回值文本，单位由 `displayUnit` 独立渲染。

### Vue 模板: 分离渲染

```html
<span class="realtime-value-main">{{ row.displayValueWithUnit }}</span>
<span v-if="row.displayUnit" class="realtime-value-unit">{{ row.displayUnit }}</span>
```

### CSS

| 元素 | font-size | font-weight | color |
|------|-----------|-------------|-------|
| .realtime-value-main | 15px | 600 | text-primary |
| .realtime-value-unit | 11px | 400 | text-secondary |

## 验证

- `npm run build`: 通过

## 修改文件

`monitorAdapters.ts` — displayUnit 字段 + buildDisplayUnit
`RealtimeDataView.vue` — 分离渲染 + CSS
