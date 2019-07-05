// eslint-disable-next-line
import { UserLayout, BasicLayout, RouteView, BlankLayout, PageView } from '@/components/layouts'

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

      // compute
      {
        path: '/compute',
        name: 'compute',
        meta: { title: 'Compute', keepAlive: true, icon: 'cloud', permission: [ 'listVirtualMachinesMetrics', 'listVirtualMachines' ] },
        component: RouteView,
        redirect: '/vm',
        children: [
          {
            path: '/vm',
            name: 'vm',
            meta: { title: 'Instances', keepAlive: true, icon: 'desktop', permission: [ 'listVirtualMachinesMetrics', 'listVirtualMachines' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/vm/:id',
                meta: { title: 'Instances', keepAlive: true, icon: 'cloud', permission: [ 'listVirtualMachinesMetrics', 'listVirtualMachines' ] },
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/kubernetes',
            name: 'kubernetes',
            meta: { title: 'Kubernetes', keepAlive: true, icon: 'radar-chart', permission: [ 'listVirtualMachines' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/kubernetes/:id',
                meta: { title: 'Instances', keepAlive: true, icon: 'cloud', permission: [ 'listVirtualMachinesMetrics', 'listVirtualMachines' ] },
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/ssh',
            name: 'ssh',
            meta: { title: 'SSH Keys', icon: 'key', permission: [ 'listSSHKeyPairs' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/ssh/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/affinitygroups',
            name: 'affinitygroups',
            meta: { title: 'Affinity Groups', icon: 'swap', permission: [ 'listAffinityGroups' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/affinitygroups/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // storage
      {
        path: '/storage',
        name: 'storage',
        meta: { title: 'Storage', keepAlive: true, icon: 'database', permission: [ 'listVolumesMetrics', 'listVolumes' ] },
        component: RouteView,
        redirect: '/volume',
        children: [
          {
            path: '/volume',
            name: 'volume',
            meta: { title: 'Volumes', icon: 'hdd', permission: [ 'listVolumesMetrics', 'listVolumes' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/volume/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/snapshot',
            name: 'snapshot',
            meta: { title: 'Snapshots', icon: 'build', permission: [ 'listSnapshots' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/snapshot/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/vmsnapshot',
            name: 'vmsnapshot',
            meta: { title: 'VM Snapshots', icon: 'camera', permission: [ 'listVMSnapshot' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/vmsnapshot/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // network
      {
        path: '/network',
        name: 'network',
        meta: { title: 'Network', keepAlive: true, icon: 'wifi', permission: [ 'listNetworks' ] },
        component: RouteView,
        redirect: '/guestnetwork',
        children: [
          {
            path: '/guestnetwork',
            name: 'guestnetwork',
            meta: { title: 'Guest Networks', icon: 'gateway', permission: [ 'listNetworks' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/guestnetwork/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/vpc',
            name: 'vpc',
            meta: { title: 'VPCs', icon: 'deployment-unit', permission: [ 'listVPCs' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/vpc/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/securitygroups',
            name: 'securitygroups',
            meta: { title: 'Security Groups', icon: 'fire', permission: [ 'listSecurityGroups' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/securitygroups/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/vpngateway',
            name: 'vpngateway',
            meta: { title: 'VPN Gateways', icon: 'lock', permission: [ 'listVpnCustomerGateways' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/vpngateway/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // image
      {
        path: '/image',
        name: 'image',
        meta: { title: 'Images', keepAlive: true, icon: 'picture', permission: [ 'listTemplates' ] },
        component: RouteView,
        redirect: '/template',
        children: [
          {
            path: '/template',
            name: 'template',
            meta: { title: 'Templates', icon: 'save', permission: [ 'listTemplates' ], params: { 'templatefilter': 'executable' } },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/template/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          },
          {
            path: '/iso',
            name: 'iso',
            meta: { title: 'Isos', icon: 'usb', permission: [ 'listIsos' ] },
            component: () => import('@/components/CloudMonkey/Resource.vue'),
            hideChildrenInMenu: true,
            children: [
              {
                path: '/iso/:id',
                component: () => import('@/components/CloudMonkey/Resource.vue')
              }
            ]
          }
        ]
      },

      // project
      {
        path: '/project',
        name: 'project',
        meta: { title: 'Projects', icon: 'project', permission: [ 'listProjects' ] },
        component: () => import('@/components/CloudMonkey/Resource.vue'),
        hideChildrenInMenu: true,
        children: [
          {
            path: '/project/:id',
            component: () => import('@/components/CloudMonkey/Resource.vue')
          }
        ]
      },

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
        redirect: '/exception/403',
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
        component: () => import('@/views/Home')
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
