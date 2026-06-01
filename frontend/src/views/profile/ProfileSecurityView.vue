<template>
  <div>
    <PageHeader title="安全设置" description="修改密码（登录接口接入前仅页面规划）" />
    <el-card style="max-width:500px">
      <ApiErrorAlert v-if="error" :error="error" />
      <el-form :model="form" label-width="100px">
        <el-form-item label="旧密码"><el-input v-model="form.oldPassword" type="password" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="changePwd">修改密码</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import { changePassword } from '@/api/auth'
import type { ApiError } from '@/types/api'

const loading = ref(false); const error = ref<ApiError | null>(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function changePwd() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) { ElMessage.warning('请完整填写'); return }
  if (form.newPassword !== form.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (form.newPassword.length < 8) { ElMessage.warning('密码至少8位'); return }
  loading.value = true
  try { await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword, confirmPassword: form.confirmPassword }); ElMessage.success('密码已修改') }
  catch { error.value = { code: 'change_failed', message: '修改失败，接口未接入' } }
  finally { loading.value = false }
}
</script>
