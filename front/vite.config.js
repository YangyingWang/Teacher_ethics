import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

import components from 'unplugin-vue-components/vite';
// import { AntDesignXVueResolver } from 'ant-design-x-vue/resolver-dist';
// import { AntDesignXVueResolver } from 'ant-design-x-vue'

import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  // plugins: [
  //   vue(),
  //   components({
  //     resolvers: [AntDesignXVueResolver()]
  //   })
  // ],
  plugins: [
      vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server:{
    proxy:{
      '/api':{//获取路径中包含了/api的请求
          target:'http://localhost:8081',//后台服务所在的源
          changeOrigin:true,//修改源
          rewrite:(path)=>path.replace(/^\/api/,'')///api替换为''
      }
    }
  }
})
