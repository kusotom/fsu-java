<template>
  <div class="login-page">
    <div class="login-card">
      <h3>忘记密码</h3>
      <p style="color:#999;margin-bottom:24px">短信验证码重置密码</p>
      <ApiErrorAlert v-if="error" :error="error" />
      <el-form :model="form" label-width="0">
        <el-form-item><el-input v-model="form.phone" placeholder="手机号" size="large" /></el-form-item>
        <el-form-item>
          <el-row :gutter="12" style="width:100%">
            <el-col :span="15"><el-input v-model="form.smsCode" placeholder="验证码" size="large" /></el-col>
            <el-col :span="9"><el-button size="large" style="width:100%" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</el-button></el-col>
          </el-row>
        </el-form-item>
        <el-form-item><el-input v-model="form.newPassword" type="password" placeholder="新密码（至少8位）" size="large" /></el-form-item>
        <el-form-item><el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" size="large" /></el-form-item>
        <el-form-item><el-button type="primary" size="large" style="width:100%" :loading="loading" @click="resetPwd">重置密码</el-button></el-form-item>
      </el-form>
      <div style="text-align:center"><router-link to="/login">返回登录</router-link></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import { sendPasswordResetSmsCode, resetPasswordBySmsCode } from '@/api/auth'
import type { ApiError } from '@/types/api'

const router = useRouter()
const loading = ref(false); const error = ref<ApiError | null>(null); const countdown = ref(0)
const form = reactive({ phone: '', smsCode: '', newPassword: '', confirmPassword: '' })

async function sendCode() {
  if (!form.phone) { ElMessage.warning('请输入手机号'); return }
  try { await sendPasswordResetSmsCode({ phone: form.phone }); ElMessage.success('验证码已发送'); countdown.value = 60; const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(t) }, 1000) } catch { error.value = { code: 'send_failed', message: '发送失败，接口未接入' } }
}

async function resetPwd() {
  if (!form.phone || !form.smsCode || !form.newPassword) { ElMessage.warning('请完整填写'); return }
  if (form.newPassword !== form.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (form.newPassword.length < 8) { ElMessage.warning('密码至少8位'); return }
  loading.value = true
  try { await resetPasswordBySmsCode({ phone: form.phone, smsCode: form.smsCode, newPassword: form.newPassword, confirmPassword: form.confirmPassword }); ElMessage.success('密码已重置，请登录'); router.push('/login') }
  catch { error.value = { code: 'reset_failed', message: '重置失败，接口未接入' } }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page { display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f5f7fa; }
.login-card { width:400px; padding:32px; background:#fff; border-radius:8px; box-shadow:0 2px 12px rgba(0,0,0,0.1); }
</style>
