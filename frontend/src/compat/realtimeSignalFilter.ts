/**
 * FE-P0-RECTIFY-001: 实时数据临时兼容过滤层
 *
 * WARNING: 这是临时保护逻辑，不是正式点位映射方案。
 * 当前 B接口实时页仍保留默认 FSU 兼容入口，后续应迁移为后端返回的 FSU 列表和
 * DataScope 结果驱动。
 *
 * @deprecated 临时兼容 shim，不扩散到其他页面组件。
 * TODO: 删除本文件，改用后端返回的 FSU 和 mappingStatus 字段驱动。
 */

/** @deprecated 临时硬编码，待后端 datasource 字段补齐后移除 */
export const HARDCODED_REALTIME_FSU_CODE = '51051243812345'
