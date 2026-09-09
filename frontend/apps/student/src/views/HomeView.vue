<!--
  域/模块: 平台底座/学生端应用
  类型: 页面
  职责: 学生端首页占位——登录闭环的可感知落点（用户名/角色取自 accessToken 声明），业务页面随各域任务包落位
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
        <CardDescription>登录成功</CardDescription>
      </CardHeader>
      <CardContent class="grid gap-4">
        <p class="text-sm">
          欢迎你，<span class="font-medium">{{ username || "同学" }}</span>
        </p>
        <p class="text-sm text-muted-foreground">
          当前角色：{{ roles.join("、") || "—" }}
        </p>
        <p class="text-sm text-muted-foreground">
          业务功能页面随各域任务包交付，当前为底座占位首页。
        </p>
        <Button
          variant="outline"
          @click="onLogout"
        >
          退出登录
        </Button>
      </CardContent>
    </Card>
  </main>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { clearTokens, getAccessToken } from "../api/http";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const router = useRouter();

const payload = decodePayload(getAccessToken() ?? "");
const username = computed(() => payload.uname ?? "");
const roles = computed(() => toRoleList(payload.roles));

/** 后端 JWT 的 roles 声明是逗号串/单值字符串，非数组，统一转列表 */
function toRoleList(roles: string | string[] | undefined): string[] {
  if (Array.isArray(roles)) return roles;
  return roles ? roles.split(/[,\s]+/).filter(Boolean) : [];
}

function decodePayload(token: string): { uname?: string; roles?: string | string[] } {
  try {
    const base64 = (token.split(".")[1] ?? "").replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64 + "=".repeat((4 - (base64.length % 4)) % 4)));
  } catch {
    return {};
  }
}

function onLogout() {
  // 占位阶段仅清本地凭证（与 401 回调同路径）；服务端 refresh token 吊销随登出接口接线
  clearTokens();
  router.push({ name: "login" });
}
</script>
