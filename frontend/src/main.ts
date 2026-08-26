import { createApp } from 'vue'
import App from './App.vue'

// 把根组件挂到 index.html 的 #app 上
// （Task 3 接入 router 后会变成 createApp(App).use(router).mount('#app')）
createApp(App).mount('#app')
