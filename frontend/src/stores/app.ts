import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局应用状态 (FE-INFRA-001)。
 */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const pageLoading = ref(false)

  function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value }
  function setTheme(t: 'light' | 'dark') { theme.value = t }
  function setPageLoading(v: boolean) { pageLoading.value = v }

  return { sidebarCollapsed, theme, pageLoading, toggleSidebar, setTheme, setPageLoading }
})
