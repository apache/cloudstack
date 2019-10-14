// eslint-disable-next-line
import { UserLayout, BasicLayout, RouteView, BlankLayout, PageView } from '@/layouts'
import AutogenView from '@/views/AutogenView.vue'

import compute from '@/config/section/compute'
import storage from '@/config/section/storage'
import network from '@/config/section/network'
import image from '@/config/section/image'
import project from '@/config/section/project'
import monitor from '@/config/section/monitor'
import iam from '@/config/section/iam'
import infra from '@/config/section/infra'
import offering from '@/config/section/offering'
import config from '@/config/section/config'
import plugin from '@/config/section/plugin'

export function generateRouterMap (section) {
  var map = {
    name: section.name,
    path: '/' + section.name,
    meta: { title: section.title, keepAlive: true, icon: section.icon },
    component: RouteView
  }

  if (section.children && section.children.length > 0) {
    map.redirect = '/' + section.children[0].name
    map.meta.permission = section.children[0].permission
    map.children = []
    for (const child of section.children) {
      var component = child.component ? child.component : AutogenView
      var route = {
        name: child.name,
        path: '/' + child.name,
        meta: {
          title: child.title,
          name: child.name,
          keepAlive: true,
          icon: child.icon,
          permission: child.permission,
          resourceType: child.resourceType,
          params: child.params ? child.params : {},
          columns: child.columns,
          details: child.details,
          actions: child.actions
        },
        component: component,
        hideChildrenInMenu: true,
        children: [
          {
            path: '/' + child.name + '/:id',
            meta: {
              title: child.title,
              name: child.name,
              keepAlive: true,
              icon: child.icon,
              permission: child.permission,
              resourceType: child.resourceType,
              params: child.params ? child.params : {},
              details: child.details,
              actions: child.actions ? child.actions : [],
              viewComponent: child.viewComponent
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
            path: '/action/' + action.api,
            meta: {
              title: child.title,
              name: child.name,
              keepAlive: true,
              permission: [ action.api ]
            },
            component: action.component,
            hidden: true
          })
        })
      }
      map.children.push(route)
    }
  } else {
    map.component = section.component ? section.component : AutogenView
    map.hideChildrenInMenu = true
    map.children = [{
      path: '/' + section.name + '/:id',
      actions: section.actions ? section.actions : [],
      meta: {
        title: section.title,
        name: section.name,
        keepAlive: true,
        icon: section.icon,
        permission: section.permission,
        resourceType: section.resourceType,
        params: section.params ? section.params : {},
        details: section.details,
        actions: section.actions ? section.actions : [],
        viewComponent: section.viewComponent
      },
      component: section.viewComponent ? section.viewComponent : AutogenView
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

  return map
}

export const asyncRouterMap = [
  {
    path: '/',
    name: 'index',
    component: BasicLayout,
    meta: { icon: 'home' },
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        meta: { title: 'Dashboard', keepAlive: true, icon: 'dashboard' },
        component: () => import('@/views/dashboard/Dashboard')
      },

      generateRouterMap(compute),
      generateRouterMap(storage),
      generateRouterMap(network),
      generateRouterMap(image),
      generateRouterMap(project),
      generateRouterMap(monitor),
      generateRouterMap(iam),
      generateRouterMap(infra),
      generateRouterMap(offering),
      generateRouterMap(config),
      generateRouterMap(plugin),

      {
        path: '/exception',
        name: 'exception',
        component: RouteView,
        hidden: true,
        redirect: '/exception/404',
        meta: { title: 'Exception', icon: 'warning' },
        children: [
          {
            path: '/exception/403',
            name: 'Exception403',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/403'),
            meta: { title: '403' }
          },
          {
            path: '/exception/404',
            name: 'Exception404',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/404'),
            meta: { title: '404' }
          },
          {
            path: '/exception/500',
            name: 'Exception500',
            hidden: true,
            component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/500'),
            meta: { title: '500' }
          }
        ]
      }
    ]
  },
  {
    path: '*', redirect: '/exception/404', hidden: true
  }
]

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
        component: () => import(/* webpackChunkName: "user" */ '@/views/user/Login')
      }
    ]
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
