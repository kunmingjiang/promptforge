import { createI18n } from 'vue-i18n'
import zh from './zh'
import en from './en'

/**
 * 全站国际化：默认中文，语言偏好存 localStorage（promptforge.locale）。
 * Element Plus 组件库语言由 main.ts 根据 currentLocale 同步切换
 */
export const LOCALE_KEY = 'promptforge.locale'

function initialLocale(): string {
  const saved = localStorage.getItem(LOCALE_KEY)
  if (saved === 'zh' || saved === 'en') return saved
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'zh',
  messages: { zh, en }
})

export function setLocale(locale: 'zh' | 'en') {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_KEY, locale)
  ;(document.documentElement as { lang: string }).lang = locale === 'zh' ? 'zh-CN' : 'en'
}

export default i18n
