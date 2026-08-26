import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    // antd 组件按需自动引入：只在 SFC 模板里生效（<a-button> 等）；
    // .tsx 里的 JSX 不走模板编译，组件需显式 import。
    // v4 用 cssinjs，无需引入样式文件。
    Components({
      resolvers: [AntDesignVueResolver({ importStyle: false })],
    }),
  ],
  // 开发时：npm run dev 起在 5173，/api 请求代理给后端 8081
  server: {
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
  // 构建：产物输出到 Spring Boot 静态资源目录
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  // 单测：jsdom 环境
  test: {
    environment: 'jsdom',
  },
})
