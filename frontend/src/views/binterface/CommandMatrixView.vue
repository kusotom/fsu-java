<template>
  <div>
    <PageHeader title="协议命令覆盖矩阵" description="B接口命令清单及实现状态（只读）" />
    <el-alert type="danger" show-icon :closable="false" style="margin-bottom:16px" title="SET_FSUREBOOT 默认禁用 — 不提供启用或执行入口" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="commandCode" label="命令编码" width="160"><template #default="{row}"><span :style="{fontWeight:row.commandCode==='SET_FSUREBOOT'?'bold':'normal'}">{{row.commandCode}}</span></template></el-table-column>
        <el-table-column prop="commandName" label="命令名称" min-width="140" />
        <el-table-column prop="direction" label="方向" width="100" />
        <el-table-column prop="description" label="说明" min-width="180" />
        <el-table-column prop="implemented" label="已实现" width="80"><template #default="{row}"><el-tag :type="row.implemented?'success':'info'">{{row.implemented?'是':'否'}}</el-tag></template></el-table-column>
        <el-table-column prop="safeEnabled" label="安全启用" width="100"><template #default="{row}"><el-tag :type="row.safeEnabled?'success':'danger'">{{row.safeEnabled?'启用':'禁用'}}</el-tag></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getCommands } from '@/api/bInterface'
import type { BInterfaceCommand } from '@/types'

const loading = ref(false)
const tableData = ref<BInterfaceCommand[]>([])
onMounted(async () => { loading.value = true; try { const res: any = await getCommands(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
