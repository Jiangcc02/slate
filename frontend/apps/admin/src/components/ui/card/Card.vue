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

const props = withDefaults(defineProps<{
  class?: HTMLAttributes['class']
  size?: 'default' | 'sm'
}>(), {
  size: 'default',
})
</script>

<template>
  <div
    data-slot="card"
    :data-size="size"
    :class="cn('ring-foreground/10 bg-card text-card-foreground gap-4 overflow-hidden rounded-xl py-4 text-sm ring-1 has-data-[slot=card-footer]:pb-0 has-[>img:first-child]:pt-0 data-[size=sm]:gap-3 data-[size=sm]:py-3 data-[size=sm]:has-data-[slot=card-footer]:pb-0 *:[img:first-child]:rounded-t-xl *:[img:last-child]:rounded-b-xl group/card flex flex-col', props.class)"
  >
    <slot />
  </div>
</template>
