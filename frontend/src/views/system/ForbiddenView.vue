<template>
  <div class="forbidden-page">
    <el-result icon="warning" title="403" sub-title="您没有访问此页面的权限">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.forbidden-page { display:flex; justify-content:center; align-items:center; min-height:60vh; }
</style>
