<!--
  域/模块: 平台底座/认证授权（管理端）
  类型: 页面
  职责: 管理端登录页——用户名/密码表单，调用 auth/login（契约 AUTH 前缀）；骨架阶段验证统一响应解包链路
  设计文档: docs/design/平台底座/design.md
  维护者: 协调者 / agent-fffabc
-->
<template>
  <main class="login-page">
    <el-card class="login-card">
      <h1 class="login-title">
        slate 管理端
      </h1>
      <el-form
        :model="form"
        @submit.prevent="onSubmit"
      >
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="login-button"
            :loading="loading"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { ApiError } from "@slate/shared";
import { authApi } from "../api/auth";
import { saveAccessToken } from "../api/http";

const form = reactive({ username: "", password: "" });
const loading = ref(false);

async function onSubmit() {
  loading.value = true;
  try {
    const tokens = await authApi.login(form.username, form.password);
    saveAccessToken(tokens.accessToken);
    ElMessage.success("登录成功");
  } catch (error) {
    // 后端 auth 未实现前此处多为网络错误；实现后展示契约错误码 message（ApiError.code 语义冻结）
    const message = error instanceof ApiError ? `${error.code}：${error.message}` : "网络异常，请稍后重试";
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f2f3f5;
}

.login-card {
  width: 360px;
}

.login-title {
  margin: 0 0 16px;
  font-size: 20px;
  text-align: center;
}

.login-button {
  width: 100%;
}
</style>
