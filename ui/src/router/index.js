import { createRouter, createWebHashHistory } from 'vue-router'
import { constantRouterMap } from '@/config/router'

const router = createRouter({
  history: createWebHashHistory(process.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRouterMap
})

export default router
