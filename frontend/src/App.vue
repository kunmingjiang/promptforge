<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

// Element Plus 组件库语言跟随当前 locale（zh → zhCn，en → en）
const { locale } = useI18n()
const elLocale = computed(() => (locale.value === 'zh' ? zhCn : en))
</script>

<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>
