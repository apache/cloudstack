export default {
  name: 'compute',
  title: 'Compute',
  icon: 'cloud',
  children: [
    {
      name: 'vm',
      title: 'Instances',
      icon: 'desktop',
      permission: [ 'listVirtualMachinesMetrics', 'listVirtualMachines' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: [
        'name', 'instancename', 'state', 'nic[].ipaddress', 'zonename', 'account', 'domain',
        'cpunumber', 'cpuused', 'cputotal', 'memoryintfreekbs', 'memorytotal',
        'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal'
      ],
      hidden: ['zonename', 'account', 'domain'],
      actions: [
        {
          api: 'deployVirtualMachine',
          icon: 'plus',
          label: 'Deploy VM',
          params: ['name', 'zoneid', 'diskofferingid']
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'Start VM',
          params: ['name', 'zoneid', 'diskofferingid']
        },
        {
          api: 'stopVirtualMachine',
          icon: 'stop',
          label: 'Stop VM',
          params: ['name', 'zoneid', 'diskofferingid']
        }
      ]
    },
    {
      name: 'kubernetes',
      title: 'Kubernetes',
      icon: 'radar-chart',
      permission: [ 'listVirtualMachines' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'ssh',
      title: 'SSH Keys',
      icon: 'key',
      permission: [ 'listSSHKeyPairs' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'affinitygroups',
      title: 'Affinity Groups',
      icon: 'swap',
      permission: [ 'listAffinityGroups' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
