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

// eslint-disable-next-line
import { UserLayout, BasicLayout, RouteView } from '@/layouts'
import AutogenView from '@/views/AutogenView.vue'
import IFramePlugin from '@/views/plugins/IFramePlugin.vue'

import { shallowRef } from 'vue'
import { vueProps } from '@/vue-app'

import compute from '@/config/section/compute'
import storage from '@/config/section/storage'
import network from '@/config/section/network'
import image from '@/config/section/image'
import project from '@/config/section/project'
import event from '@/config/section/event'
import user from '@/config/section/user'
import account from '@/config/section/account'
import domain from '@/config/section/domain'
import role from '@/config/section/role'
import infra from '@/config/section/infra'
import zone from '@/config/section/zone'
import offering from '@/config/section/offering'
import config from '@/config/section/config'
import tools from '@/config/section/tools'
import quota from '@/config/section/plugin/quota'
import cloudian from '@/config/section/plugin/cloudian'

function generateRouterMap (section) {
  var map = {
    name: section.name,
    path: '/' + section.name,
    hidden: 'show' in section ? !section.show() : section.hidden,
    meta: {
      title: section.title,
      icon: section.icon,
      docHelp: vueProps.$applyDocHelpMappings(section.docHelp),
      searchFilters: section.searchFilters,
      related: section.related
    },
    component: shallowRef(RouteView)
  }

  if (section.children && section.children.length > 0) {
    map.redirect = '/' + section.children[0].name
    map.meta.permission = section.children[0].permission
    map.children = []
    for (const child of section.children) {
      if ('show' in child && !child.show()) {
        continue
      }
      var component = child.component ? child.component : shallowRef(AutogenView)
      var route = {
        name: child.name,
        path: '/' + child.name,
        hidden: child.hidden,
        meta: {
          title: child.title,
          name: child.name,
          icon: child.icon,
          docHelp: vueProps.$applyDocHelpMappings(child.docHelp),
          permission: child.permission,
          resourceType: child.resourceType,
          filters: child.filters,
          params: child.params ? child.params : {},
          columns: child.columns,
          details: child.details,
          searchFilters: child.searchFilters,
          related: child.related,
          actions: child.actions,
          tabs: child.tabs
        },
        component: component,
        hideChildrenInMenu: true,
        children: [
          {
            path: '/' + child.name + '/:id',
            hidden: child.hidden,
            meta: {
              title: child.title,
              name: child.name,
              icon: child.icon,
              docHelp: vueProps.$applyDocHelpMappings(child.docHelp),
              permission: child.permission,
              resourceType: child.resourceType,
              params: child.params ? child.params : {},
              details: child.details,
              searchFilters: child.searchFilters,
              related: child.related,
              tabs: child.tabs,
              actions: child.actions ? child.actions : []
            },
            component: component
          }
        ]
      }
      if (child.actions) {
        child.actions.forEach(function (action) {
          if (!action.component || !action.api) {
            return
          }
          map.children.push({
            name: action.api,
            icon: child.icon,
            hidden: true,
            path: '/action/' + action.api,
            meta: {
              title: child.title,
              name: child.name,
              permission: [action.api]
            },
            component: action.component
          })
        })
      }
      map.children.push(route)
    }
  } else {
    map.component = section.component ? section.component : shallowRef(AutogenView)
    map.hideChildrenInMenu = true

    map.meta.name = section.name
    map.meta.permission = section.permission
    map.meta.resourceType = section.resourceType
    map.meta.details = section.details
    map.meta.actions = section.actions
    map.meta.filters = section.filters
    map.meta.treeView = section.treeView ? section.treeView : false
    map.meta.tabs = section.tabs

    map.children = [{
      path: '/' + section.name + '/:id',
      actions: section.actions ? section.actions : [],
      meta: {
        title: section.title,
        name: section.name,
        icon: section.icon,
        docHelp: vueProps.$applyDocHelpMappings(section.docHelp),
        hidden: section.hidden,
        permission: section.permission,
        resourceType: section.resourceType,
        params: section.params ? section.params : {},
        details: section.details,
        related: section.related,
        searchFilters: section.searchFilters,
        tabs: section.tabs,
        actions: section.actions ? section.actions : []
      },
      component: section.component ? section.component : shallowRef(AutogenView)
    }]
  }

  if (section.component) {
    map.component = section.component
  }

  if (section.permission) {
    map.meta.permission = section.permission
  }

  if (section.columns) {
    map.meta.columns = section.columns
  }

  if (section.actions) {
    map.meta.actions = section.actions
  }

  if (section.params) {
    map.meta.params = section.params
  }

  return map
}

export function asyncRouterMap () {
  const routerMap = [{
    path: '/',
    name: 'index',
    component: shallowRef(BasicLayout),
    meta: { icon: 'HomeOutlined' },
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        meta: {
          title: 'label.dashboard',
          icon: 'DashboardOutlined'
        },
        component: () => import('@/views/dashboard/Dashboard')
      },

      generateRouterMap(compute),
      generateRouterMap(storage),
      generateRouterMap(network),
      generateRouterMap(image),
      generateRouterMap(event),
      generateRouterMap(project),
      generateRouterMap(user),
      generateRouterMap(role),
      generateRouterMap(account),
      generateRouterMap(domain),
      generateRouterMap(infra),
      generateRouterMap(zone),
      generateRouterMap(offering),
      generateRouterMap(config),
      generateRouterMap(tools),
      generateRouterMap(quota),
      generateRouterMap(cloudian),

      {
        path: '/exception',
        name: 'exception',
        component: shallowRef(RouteView),
        hidden: true,
        redirect: '/exception/404',
        meta: { title: 'Exception', icon: 'warning' },
        children: [
          {
            path: '/exception/403',
            name: '403',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/403'),
            meta: { title: '403' }
          },
          {
            path: '/exception/404',
            name: '404',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/404'),
            meta: { title: '404' }
          },
          {
            path: '/exception/500',
            name: '500',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/500'),
            meta: { title: '500' }
          }
        ]
      }
    ]
  },
  {
    path: '/:catchAll(.*)', redirect: '/exception/404', hidden: true
  }]

  const plugins = vueProps.$config.plugins
  if (plugins && plugins.length > 0) {
    plugins.map(plugin => {
      if (!plugin.isExternalLink && plugin.path) {
        routerMap[0].children.push({
          path: '/plugins/' + plugin.name,
          name: plugin.name,
          component: IFramePlugin,
          meta: { title: plugin.name, icon: plugin.icon, path: plugin.path }
        })
      }
    })
  }

  return routerMap
}

export const constantRouterMap = [
  {
    path: '/user',
    component: UserLayout,
    redirect: '/user/login',
    hidden: true,
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import(/* webpackChunkName: "auth" */ '@/views/auth/Login')
      }
    ]
  },
  {
    path: '/verify2FA',
    name: 'VerifyTwoFa',
    meta: {
      title: 'label.two.factor.authentication',
      hidden: true
    },
    component: () => import('@/views/dashboard/VerifyTwoFa')
  },
  {
    path: '/verifyOauth',
    name: 'VerifyOauth',
    meta: {
      title: 'label.oauth.verification',
      hidden: true
    },
    component: () => import('@/views/dashboard/VerifyOauth')
  },
  {
    path: '/setup2FA',
    name: 'SetupTwoFaAtLogin',
    meta: {
      title: 'label.two.factor.authentication',
      hidden: true
    },
    component: () => import('@/views/dashboard/SetupTwoFaAtLogin')
  },
  {
    path: '/403',
    component: () => import(/* webpackChunkName: "forbidden" */ '@/views/exception/403')
  },
  {
    path: '/404',
    component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/404')
  },
  {
    path: '/500',
    component: () => import(/* webpackChunkName: "error" */ '@/views/exception/500')
  }
]
