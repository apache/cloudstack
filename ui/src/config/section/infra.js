export default {
  name: 'infra',
  title: 'Infrastructure',
  icon: 'bank',
  permission: [ 'listInfrastructure' ],
  children: [
    {
      name: 'zone',
      title: 'Zones',
      icon: 'table',
      permission: [ 'listZones', 'listZonesMetrics' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'pod',
      title: 'Pods',
      icon: 'appstore',
      permission: [ 'listPods' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'cluster',
      title: 'Clusters',
      icon: 'cluster',
      permission: [ 'listClusters', 'listClustersMetrics' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'host',
      title: 'Hosts',
      icon: 'desktop',
      permission: [ 'listHosts', 'listHostsMetrics' ],
      params: {'type': 'routing'},
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'storagepool',
      title: 'Primary Storages',
      icon: 'database',
      permission: [ 'listStoragePools', 'listStoragePoolsMetrics' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'imagestore',
      title: 'Secondary Storages',
      icon: 'picture',
      permission: [ 'listImageStores' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'systemvm',
      title: 'System VMs',
      icon: 'thunderbolt',
      permission: [ 'listSystemVms' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'router',
      title: 'Virtual Routers',
      icon: 'fork',
      permission: [ 'listRouters' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'cpusockets',
      title: 'CPU Sockets',
      icon: 'api',
      permission: [ 'listHosts' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    },
    {
      name: 'ms',
      title: 'Management Servers',
      icon: 'environment',
      permission: [ 'listManagementServers' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
    }
  ]
}
