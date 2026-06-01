<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
      <div class="app-logo">
        <span v-if="!isCollapse">动环监控平台</span>
        <span v-else>DH</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#1B2A47"
        text-color="#D6E1F2"
        active-text-color="#FFFFFF"
        router
        style="border-right: none"
      >
        <!-- ===== 1. 监控中心 ===== -->
        <el-sub-menu index="monitor-center">
          <template #title>
            <el-icon><HomeFilled /></el-icon>
            <span>监控中心</span>
          </template>
          <el-menu-item index="/dashboard">监控驾驶舱</el-menu-item>
          <el-menu-item index="/sites/realtime">站点实时数据</el-menu-item>
          <el-menu-item index="/alarms">告警中心</el-menu-item>
        </el-sub-menu>

        <!-- ===== 2. 站点监控 ===== -->
        <el-sub-menu index="site-monitor">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>站点监控</span>
          </template>
          <el-menu-item index="/sites">站点列表</el-menu-item>
          <el-menu-item index="/b-interface/fsus">FSU 管理</el-menu-item>
        </el-sub-menu>

        <!-- ===== 3. 三方授权 ===== -->
        <el-sub-menu v-if="userStore.isTenantAdmin" index="tenant">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>三方授权</span>
          </template>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
          <el-menu-item index="/system/roles">角色管理</el-menu-item>
          <el-menu-item index="/system/permissions">权限管理</el-menu-item>
          <el-menu-item index="/system/site-authorizations">站点授权</el-menu-item>
          <el-menu-item index="/system/fsu-authorizations">FSU 授权</el-menu-item>
        </el-sub-menu>

        <!-- ===== 4. 系统设置 ===== -->
        <el-menu-item index="/profile/security">
          <el-icon><Setting /></el-icon>
          <template #title>安全设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div style="display: flex; align-items: center">
          <el-button @click="isCollapse = !isCollapse" text>
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <span class="app-header__title">机房动环监控平台</span>
        </div>
        <div style="display: flex; align-items: center; gap: 12px">
          <span style="color: var(--text-secondary); font-size: 13px">{{ currentTime }}</span>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Monitor, UserFilled, Setting, Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
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

<style scoped>
.app-sidebar {
  background-color: var(--app-sidebar-bg) !important;
  transition: width 0.3s;
}
.app-logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 18px; font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.app-header {
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--border-light);
  display: flex; align-items: center; justify-content: space-between;
  height: 60px;
}
.app-header__title {
  margin-left: 12px; font-size: 16px; font-weight: 500;
  color: var(--text-primary);
}
.app-main {
  background-color: var(--app-bg);
  padding: 20px;
}
</style>
