<!--
  域/模块: 平台底座/前端 UI 组件（shadcn-vue 官方源码，CLI 生成，保持上游一致，修改须对齐上游）
  类型: UI 组件
  职责: shadcn-vue 组件源码，按需经 CLI add 引入（frontend/AGENTS.md 组件源码模式）
  设计文档: docs/design/平台底座/design.md
  维护者: 协调者 / agent-fffabc
-->
<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <div
    data-slot="card-content"
    :class="cn('px-4 group-data-[size=sm]/card:px-3', props.class)"
  >
    <slot />
  </div>
</template>
