<template>
  <div class="login-page">
    <div class="login-card">
      <h2>机房动环监控平台</h2>
      <p style="color:#999;margin-bottom:24px">短信验证码登录</p>
      <el-alert type="info" :closable="false" style="margin-bottom:16px" title="开发环境验证码: 123456（仅 dev 可用）" />
      <ApiErrorAlert v-if="error" :error="error" :auditId="auditId" />
      <el-form :model="form" label-width="0">
        <el-form-item><el-input v-model="form.phone" placeholder="手机号" size="large" /></el-form-item>
        <el-form-item>
          <el-row :gutter="12" style="width:100%">
            <el-col :span="15"><el-input v-model="form.smsCode" placeholder="短信验证码" size="large" /></el-col>
            <el-col :span="9"><el-button size="large" style="width:100%" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</el-button></el-col>
          </el-row>
        </el-form-item>
        <el-form-item><el-button type="primary" size="large" style="width:100%" :loading="loading" @click="login">登录</el-button></el-form-item>
      </el-form>
      <div style="text-align:center"><router-link to="/forgot-password">忘记密码</router-link></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import { sendSmsLoginCode, loginBySmsCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { ApiError } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false); const error = ref<ApiError | null>(null); const auditId = ref(''); const countdown = ref(0)
const form = reactive({ phone: '', smsCode: '' })

async function sendCode() {
  if (!form.phone) { ElMessage.warning('请输入手机号'); return }
  error.value = null
  try { await sendSmsLoginCode({ phone: form.phone }); ElMessage.success('验证码已发送'); countdown.value = 60; const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(t) }, 1000) }
  catch { error.value = { code: 'send_failed', message: '发送验证码失败，接口未接入' } }
}

async function login() {
  if (!form.phone || !form.smsCode) { ElMessage.warning('请输入手机号和验证码'); return }
  loading.value = true; error.value = null
  try {
    const res: any = await loginBySmsCode({ phone: form.phone, smsCode: form.smsCode })
    // Axios response.data 解包后返回 {code, message, data}
    const loginData = res?.data ?? res
    if (loginData?.accessToken) {
      userStore.setAuth(loginData.accessToken, loginData.refreshToken || '', loginData.user?.username || '', loginData.roles?.map((r:any) => r.code || r), loginData.permissions)
      const redirect = route.query.redirect as string
      router.push(redirect || '/b-interface/fsus')
    } else {
      error.value = { code: 'login_failed', message: res?.message || '登录失败，请检查验证码' }
    }
  } catch { error.value = { code: 'login_failed', message: '登录失败，请检查验证码或稍后重试' } }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page { display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f5f7fa; }
.login-card { width:400px; padding:32px; background:#fff; border-radius:8px; box-shadow:0 2px 12px rgba(0,0,0,0.1); }
</style>
