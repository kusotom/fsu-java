import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * B接口页面全局状态 (FE-INFRA-001)。
 */
export const useBInterfaceStore = defineStore('binterface', () => {
  const selectedFsuId = ref<string | null>(null)
  const selectedFsuCode = ref<string | null>(null)
  const selectedDeviceId = ref<string | null>(null)
  const lastRefreshAt = ref<string | null>(null)

  function selectFsu(id: string, code: string) { selectedFsuId.value = id; selectedFsuCode.value = code }
  function selectDevice(id: string) { selectedDeviceId.value = id }
  function markRefreshed() { lastRefreshAt.value = new Date().toISOString() }

  return { selectedFsuId, selectedFsuCode, selectedDeviceId, lastRefreshAt, selectFsu, selectDevice, markRefreshed }
})
