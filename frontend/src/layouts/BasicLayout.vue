<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapse ? '64px' : '220px'" style="background-color: #304156; transition: width 0.3s">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid #1f2d3d">
        <span v-if="!isCollapse">动环监控平台</span>
        <span v-else>DH</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
        style="border-right: none"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>总览</template>
        </el-menu-item>

        <el-sub-menu index="resource">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>资源管理</span>
          </template>
          <el-menu-item index="/sites">站点管理</el-menu-item>
          <el-menu-item index="/cabinets">机柜管理</el-menu-item>
          <el-menu-item index="/devices">FSU设备管理</el-menu-item>
          <el-menu-item index="/points">监控点位</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="telemetry">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据监控</span>
          </template>
          <el-menu-item index="/telemetry/realtime">实时数据</el-menu-item>
          <el-menu-item index="/telemetry/history">历史数据</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="alarm">
          <template #title>
            <el-icon><WarningFilled /></el-icon>
            <span>告警管理</span>
          </template>
          <el-menu-item index="/alarms">告警中心</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="binterface">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>B接口管理</span>
          </template>
          <el-menu-item index="/b-interface">B接口总览</el-menu-item>
          <el-menu-item index="/b-interface/fsus">FSU注册状态</el-menu-item>
          <el-menu-item index="/b-interface/logs">B接口报文日志</el-menu-item>
          <el-menu-item index="/b-interface/commands">协议命令覆盖矩阵</el-menu-item>
          <el-menu-item index="/b-interface/calls">FSUService调用记录</el-menu-item>
          <el-menu-item index="/b-interface/ftp">FTP文件/图片记录</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; height: 60px">
        <div style="display: flex; align-items: center">
          <el-button @click="isCollapse = !isCollapse" text>
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <span style="margin-left: 12px; font-size: 16px; font-weight: 500">机房动环监控平台</span>
        </div>
        <div style="display: flex; align-items-center; gap: 12px">
          <span style="color: #666; font-size: 14px">{{ currentTime }}</span>
        </div>
      </el-header>

      <el-main style="background-color: #f0f2f5; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Folder, DataAnalysis, WarningFilled, Connection, Setting, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)
const currentTime = ref('')
let timer: number

const activeMenu = computed(() => route.path)

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN')
}

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>
