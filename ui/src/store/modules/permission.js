import { apiConfig } from '@/config/apiConfig'
import { asyncRouterMap, constantRouterMap } from '@/config/router.config'

function hasApi (apis, route) {
  if (route.meta && route.meta.permission) {
    for (const api of apis) {
      if (route.meta.permission.includes(api)) {
        return true
      }
    }
    return false
  }
  return true
}

function filterAsyncRouter (routerMap, apis) {
  const accessedRouters = routerMap.filter(route => {
    if (hasApi(apis, route)) {
      if (route.children && route.children.length > 0) {
        route.children = filterAsyncRouter(route.children, apis)
      }
      return true
    }
    return false
  })
  return accessedRouters
}

const permission = {
  state: {
    routers: constantRouterMap,
    addRouters: []
  },
  mutations: {
    SET_ROUTERS: (state, routers) => {
      state.addRouters = routers
      state.routers = constantRouterMap.concat(routers)
    }
  },
  actions: {
    GenerateRoutes ({ commit }, data) {
      return new Promise(resolve => {
        const apis = Object.keys(data.apis)
        const accessedRouters = filterAsyncRouter(asyncRouterMap, apis)
        commit('SET_ROUTERS', asyncRouterMap)
        resolve()
      })
    }
  }
}

export default permission
