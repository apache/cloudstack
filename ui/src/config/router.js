// eslint-disable-next-line
import { UserLayout, BasicLayout, RouteView, BlankLayout, PageView } from '@/components/layouts'

import compute from '@/config/section/compute'
import storage from '@/config/section/storage'
import network from '@/config/section/network'
import image from '@/config/section/image'
import project from '@/config/section/project'

export function generateRouterMap (section) {
  var map = {
    name: section.name,
    path: '/' + section.name,
    meta: { title: section.title, keepAlive: true, icon: section.icon },
    component: RouteView
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

  if (section.children && section.children.length > 0) {
    map.redirect = '/' + section.children[0].name
    map.meta.permission = section.children[0].permission
    map.children = []
    for (const child of section.children) {
      map.children.push({
        name: child.name,
        path: '/' + child.name,
        meta: {
          title: child.title,
          keepAlive: true,
          icon: child.icon,
          permission: child.permission,
          params: child.params ? child.params : {},
          columns: child.columns,
          actions: child.actions
        },
        component: child.component,
        hideChildrenInMenu: true,
        children: [
          {
            path: '/' + child.name + '/:id',
            meta: {
              title: child.title,
              keepAlive: true,
              icon: child.icon,
              permission: child.permission,
              params: child.params ? child.params : {},
              actions: child.actions ? child.actions : []
            },
            component: child.viewComponent ? child.viewComponent : child.component
          }
        ]
      })
    }
  } else {
    map.hideChildrenInMenu = true
    map.children = [{
      path: '/' + section.name + '/:id',
      actions: section.actions ? section.actions : [],
      component: section.viewComponent ? section.viewComponent : section.component
    }]
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
      // dashboard
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

      // audit
      {
        path: '/audit',
        name: 'audit',
        meta: { title: 'Audit', keepAlive: true, icon: 'audit', permission: [ 'listEvents', 'listAlerts' ] },
        component: RouteView,
        redirect: '/events',
        children: [
          {
            path: '/events',
            name: 'events',
            meta: { title: 'Events', icon: 'schedule', permission: [ 'listEvents' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/events/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/alerts',
            name: 'alerts',
            meta: { title: 'Alerts', icon: 'flag', permission: [ 'listAlerts' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/alerts/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // org
      {
        path: '/iam',
        name: 'iam',
        meta: { title: 'Identity and Access', keepAlive: true, icon: 'solution', permission: [ 'listAccounts' ] },
        component: RouteView,
        redirect: '/account',
        children: [
          {
            path: '/domain',
            name: 'domain',
            meta: { title: 'Domains', icon: 'block', permission: [ 'listDomains' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/domain/:id',
                meta: { title: 'Domains', icon: 'block', permission: [ 'listDomains' ] },
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/account',
            name: 'account',
            meta: { title: 'Accounts', icon: 'team', permission: [ 'listAccounts' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/account/:id',
                meta: { title: 'Manage', keepAlive: true, icon: 'solution', permission: [ 'listAccounts' ] },
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/user',
            name: 'user',
            meta: { title: 'Users', icon: 'user', permission: [ 'listUsers' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/user/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/role',
            name: 'role',
            meta: { title: 'Roles', icon: 'idcard', permission: [ 'listRoles' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/role/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // infra
      {
        path: '/infra',
        name: 'infra',
        meta: { title: 'Infrastructure', keepAlive: true, icon: 'folder-open', permission: [ 'listInfrastructure' ] },
        component: RouteView,
        redirect: '/zone',
        children: [
          {
            path: '/zone',
            name: 'zone',
            meta: { title: 'Zones', icon: 'table', permission: [ 'listZonesMetrics', 'listZones' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/zone/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/pod',
            name: 'pod',
            meta: { title: 'Pods', icon: 'appstore', permission: [ 'listPods' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/pod/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/cluster',
            name: 'cluster',
            meta: { title: 'Clusters', icon: 'cluster', permission: [ 'listClustersMetrics', 'listClusters' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/cluster/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/host',
            name: 'host',
            meta: { title: 'Hosts', icon: 'desktop', permission: [ 'listHostsMetrics', 'listHosts' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/host/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/primarystorage',
            name: 'primarystorage',
            meta: { title: 'Primary Storage', icon: 'database', permission: [ 'listStoragePoolsMetrics', 'listStoragePools' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/primarystorage/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/secondarystorage',
            name: 'secondarystorage',
            meta: { title: 'Secondary Storage', icon: 'picture', permission: [ 'listImageStores' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/secondarystorage/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/systemvm',
            name: 'systemvm',
            meta: { title: 'System VMs', icon: 'thunderbolt', permission: [ 'listSystemVms' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/systemvm/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/router',
            name: 'router',
            meta: { title: 'Virtual Routers', icon: 'fork', permission: [ 'listRouters' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/router/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/cpusockets',
            name: 'cpusocket',
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            meta: { title: 'CPU Sockets', icon: 'api', permission: [ 'listHosts' ] }
          }
        ]
      },

      // offerings
      {
        path: '/offering',
        name: 'Offerings',
        meta: { title: 'Offerings', keepAlive: true, icon: 'shopping', permission: [ 'listServiceOfferings' ] },
        component: RouteView,
        redirect: '/computeoffering',
        children: [
          {
            path: '/computeoffering',
            name: 'computeoffering',
            meta: { title: 'Compute Offerings', icon: 'cloud', permission: [ 'listServiceOfferings' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/computeoffering/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/diskoffering',
            name: 'diskoffering',
            meta: { title: 'Disk Offerings', icon: 'hdd', permission: [ 'listDiskOfferings' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/diskoffering/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/networkoffering',
            name: 'networkoffering',
            meta: { title: 'Network Offerings', icon: 'wifi', permission: [ 'listNetworkOfferings' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/networkoffering/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/vpcoffering',
            name: 'vpcoffering',
            meta: { title: 'VPC Offerings', icon: 'deployment-unit', permission: [ 'listVPCOfferings' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/vpcoffering/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/systemoffering',
            name: 'systemoffering',
            meta: { title: 'System Offerings', icon: 'setting', permission: [ 'listServiceOfferings' ], params: { 'issystem': 'true' } },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/systemoffering/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // setting
      {
        path: '/setting',
        name: 'Settings',
        meta: { title: 'Settings', keepAlive: true, icon: 'setting', permission: [ 'listConfigurations' ] },
        component: RouteView,
        redirect: '/globalsetting',
        children: [
          {
            path: '/globalsetting',
            name: 'globalsetting',
            meta: { title: 'Global Settings', icon: 'global', permission: [ 'listConfigurations' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/globalsetting/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/ldapsetting',
            name: 'ldapsetting',
            meta: { title: 'LDAP Settings', icon: 'team', permission: [ 'listLdapConfigurations' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/ldapsetting/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/hypervisorcapability',
            name: 'hypervisorcapability',
            meta: { title: 'Hypervisor Capabilities', icon: 'database', permission: [ 'listHypervisorCapabilities' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/hypervisorcapability/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // Exceptions
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
    path: '/test',
    component: BlankLayout,
    redirect: '/test/home',
    children: [
      {
        path: 'home',
        name: 'TestHome',
        component: () => import('@/views/Test')
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
