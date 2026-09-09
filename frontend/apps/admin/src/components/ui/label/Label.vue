<!--
  域/模块: 平台底座/前端 UI 组件（shadcn-vue 官方源码，CLI 生成，保持上游一致，修改须对齐上游）
  类型: UI 组件
  职责: shadcn-vue 组件源码，按需经 CLI add 引入（frontend/AGENTS.md 组件源码模式）
  设计文档: docs/design/平台底座/design.md
  维护者: 协调者 / agent-fffabc
-->
<script setup lang="ts">
import type { LabelProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { Label } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps<LabelProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')
</script>

<template>
  <Label
    data-slot="label"
    v-bind="delegatedProps"
    :class="
      cn(
        'gap-2 text-sm leading-none font-medium group-data-[disabled=true]:opacity-50 peer-disabled:opacity-50 flex items-center select-none group-data-[disabled=true]:pointer-events-none peer-disabled:cursor-not-allowed',
        props.class,
      )
    "
  >
    <slot />
  </Label>
</template>
