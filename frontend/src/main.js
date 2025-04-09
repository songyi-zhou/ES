import './assets/css/main.css'
import 'element-plus/dist/index.css'
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import pinia from './stores'
import './assets/styles/global.css'

// 导入 axios 拦截器设置
import './utils/axiosSetup'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 设置 axios 默认配置
axios.defaults.baseURL = 'http://localhost:8080'  // 替换为你的后端 API URL
axios.defaults.timeout = 10000  // 10秒超时
axios.defaults.withCredentials = true  // 允许携带跨域 cookie

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default',
  zIndex: 3000
})

app.mount('#app')
