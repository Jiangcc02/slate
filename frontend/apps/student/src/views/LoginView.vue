<!--
  域/模块: 平台底座/认证授权（学生端）
  类型: 页面
  职责: 学生端登录页——shadcn 组件表单，调用 auth/login（契约 AUTH 前缀）；骨架阶段验证统一响应解包链路
  设计文档: docs/design/平台底座/design.md
  维护者: 协调者 / agent-fffabc
-->
<template>
  <main class="flex min-h-screen items-center justify-center bg-muted">
    <Card class="w-full max-w-sm">
      <CardHeader>
        <CardTitle class="text-xl">
          slate 学生端
        </CardTitle>
        <CardDescription>学习入口</CardDescription>
      </CardHeader>
      <CardContent>
        <form
          class="grid gap-4"
          @submit.prevent="onSubmit"
        >
          <div class="grid gap-2">
            <Label for="username">用户名</Label>
            <Input
              id="username"
              v-model="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              required
            />
          </div>
          <div class="grid gap-2">
            <Label for="password">密码</Label>
            <Input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            />
          </div>
          <p
            v-if="errorMessage"
            class="text-sm text-destructive"
            role="alert"
          >
            {{ errorMessage }}
          </p>
          <Button
            type="submit"
            :disabled="loading"
          >
            {{ loading ? "登录中…" : "登录" }}
          </Button>
        </form>
      </CardContent>
    </Card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ApiError } from "@slate/shared";
import { authApi } from "../api/auth";
import { saveTokenPair } from "../api/http";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const form = reactive({ username: "", password: "" });
const loading = ref(false);
const errorMessage = ref("");

async function onSubmit() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const tokens = await authApi.login(form.username, form.password);
    saveTokenPair(tokens.accessToken, tokens.refreshToken);
  } catch (error) {
    // 后端 auth 未实现前此处多为网络错误；实现后展示契约错误码（语义冻结，见 api-conventions §4）
    errorMessage.value =
      error instanceof ApiError
        ? `${error.code}：${error.message}`
        : "网络异常，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>
