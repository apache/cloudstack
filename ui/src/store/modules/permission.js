// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import { asyncRouterMap, constantRouterMap } from '@/config/router'

function hasApi (apis, route) {
  if (route.meta && route.meta.permission) {
    for (const permission of route.meta.permission) {
      if (!apis.includes(permission)) {
        return false
      }
    }
    return true
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
        const accessedRouters = filterAsyncRouter(asyncRouterMap(), apis)
        commit('SET_ROUTERS', accessedRouters)
        resolve()
      })
    }
  }
}

export default permission
