import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)//使用导入的App.vue创建一个实例

// 注册所有图标为全局组件（可选，不注册也行）
// for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
//   app.component(key, component as never)
// }

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')






