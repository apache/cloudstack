export default {
  name: 'infra',
  title: 'Infrastructure',
  icon: 'bank',
  permission: [ 'listInfrastructure' ],
  children: [
    {
      name: 'zone',
      title: 'Zones',
      icon: 'global',
      permission: [ 'listZones', 'listZonesMetrics' ],
      columns: [ 'name', 'allocationstate', 'networktype', 'guestcidraddress' ],
      actions: [
        {
          api: 'createZone',
          icon: 'plus',
          label: 'Add Zone',
          listView: true,
          popup: true,
          component: () => import('@/views/infra/ZoneWizard.vue')
        }
      ]
    },
    {
      name: 'pod',
      title: 'Pods',
      icon: 'appstore',
      permission: [ 'listPods' ],
      columns: [ 'name', 'allocationstate', 'gateway', 'netmask', 'zonename' ]
    },
    {
      name: 'cluster',
      title: 'Clusters',
      icon: 'cluster',
      permission: [ 'listClusters', 'listClustersMetrics' ],
      columns: [ 'name', 'allocationstate', 'clustertype', 'hypervisortype', 'podname', 'zonename' ]
    },
    {
      name: 'host',
      title: 'Hosts',
      icon: 'desktop',
      permission: [ 'listHosts', 'listHostsMetrics' ],
      resourceType: 'Host',
      params: { 'type': 'routing' },
      columns: [ 'name', 'state', 'resourcestate', 'ipaddress', 'hypervisor', 'hypervisorversion', 'clustername', 'zonename' ]
    },
    {
      name: 'storagepool',
      title: 'Primary Storage',
      icon: 'database',
      permission: [ 'listStoragePools', 'listStoragePoolsMetrics' ],
      columns: [ 'name', 'state', 'ipaddress', 'type', 'path', 'scope', 'clustername', 'zonename' ]
    },
    {
      name: 'imagestore',
      title: 'Secondary Storages',
      icon: 'picture',
      permission: [ 'listImageStores' ],
      columns: [ 'name', 'url', 'protocol', 'scope', 'zonename' ]
    },
    {
      name: 'systemvm',
      title: 'System VMs',
      icon: 'thunderbolt',
      permission: [ 'listSystemVms' ],
      columns: [ 'name', 'state', 'agentstate', 'systemvmtype', 'publicip', 'privateip', 'hostname', 'zonename' ]
    },
    {
      name: 'router',
      title: 'Virtual Routers',
      icon: 'fork',
      permission: [ 'listRouters' ],
      columns: [ 'name', 'state', 'publicip', 'guestnetworkname', 'vpcname', 'redundantstate', 'version', 'hostname', 'account', 'zonename', 'requiresupgrade' ]
    },
    {
      name: 'cpusocket',
      title: 'CPU Sockets',
      icon: 'api',
      permission: [ 'listHosts' ],
      params: { 'type': 'routing' },
      columns: [ 'hypervisor', 'hosts', 'cpusockets' ]
    },
    {
      name: 'managementserver',
      title: 'Management Servers',
      icon: 'rocket',
      permission: [ 'listManagementServers' ],
      columns: [ 'name', 'state', 'version' ]
    }
  ]
}
