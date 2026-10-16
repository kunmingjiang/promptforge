import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './styles/base.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(ElementPlus)
for (const [name, component] of Object.entries(Icons)) {
  app.component(name, component)
}
app.mount('#app')
