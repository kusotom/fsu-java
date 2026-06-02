<template>
  <el-container class="app-shell">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
      <div class="app-logo">
        <span class="app-logo__mark">动</span>
        <span v-if="!isCollapse" class="app-logo__text">机房动环监控平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="app-menu"
      >
        <!-- ===== 1. 监控中心 ===== -->
        <el-sub-menu index="monitor-center">
          <template #title>
            <el-icon><HomeFilled /></el-icon>
            <span>监控中心</span>
          </template>
          <el-menu-item index="/dashboard">监控主页</el-menu-item>
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
        <div class="app-header__left">
          <el-button class="app-header__collapse" @click="isCollapse = !isCollapse" text>
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <div>
            <span class="app-header__title">{{ currentTitle }}</span>
            <span class="app-header__subtitle">动力环境监控与站点运维</span>
          </div>
        </div>
        <div class="app-header__right">
          <span class="app-header__time">{{ currentTime }}</span>
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

const activeMenu = computed(() => {
  if (route.path.startsWith('/b-interface/fsus')) return '/b-interface/fsus'
  if (route.path.startsWith('/sites/realtime')) return '/sites/realtime'
  if (route.path.startsWith('/sites')) return '/sites'
  if (route.path.startsWith('/alarms')) return '/alarms'
  return route.path
})

const currentTitle = computed(() => String(route.meta?.title || '监控主页'))

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
.app-shell {
  height: 100vh;
  background: var(--app-page-bg);
}

.app-sidebar {
  background-color: var(--app-sidebar-bg) !important;
  border-right: 1px solid var(--app-sidebar-border);
  transition: width 0.3s;
}

.app-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 60px;
  padding: 0 14px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--app-sidebar-border);
}

.app-logo__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  background: var(--app-primary);
  border-radius: 8px;
  box-shadow: 0 8px 18px rgba(47, 128, 237, 0.22);
}

.app-logo__text {
  min-width: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-menu {
  border-right: none;
}

.app-menu :deep(.el-sub-menu__title),
.app-menu :deep(.el-menu-item) {
  height: 42px;
  margin: 4px 10px;
  border-radius: 8px;
  color: var(--app-sidebar-text);
}

.app-menu :deep(.el-sub-menu__title:hover),
.app-menu :deep(.el-menu-item:hover) {
  color: var(--app-sidebar-text-active);
  background: var(--app-sidebar-hover);
}

.app-menu :deep(.el-menu-item.is-active) {
  color: var(--app-sidebar-text-active);
  background: var(--app-sidebar-active);
  font-weight: 600;
}

.app-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--app-sidebar-text-active);
}

.app-header {
  background: var(--app-header-bg);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
}

.app-header__left,
.app-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__collapse {
  color: var(--text-secondary);
}

.app-header__title {
  display: block;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.app-header__subtitle {
  display: block;
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
}

.app-header__time {
  color: var(--text-secondary);
  font-size: 13px;
}

.app-main {
  background-color: var(--app-bg);
  padding: 18px 20px 24px;
  overflow: auto;
}
</style>
