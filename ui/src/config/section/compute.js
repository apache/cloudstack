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
        'displayname', 'state', 'instancename', { 'ipaddress': (record) => { return record.nic[0].ipaddress } }, 'account', 'zonename',
        'cpunumber', 'cpuused', 'cputotal', 'memoryintfreekbs', 'memorytotal',
        'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal'
      ],
      hidden: ['instancename', 'account'],
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
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'fingerprint', 'account', 'domain'],
      actions: [
        {
          api: 'createSSHKeyPair',
          icon: 'plus',
          label: 'Create SSH key pair',
          params: ['name', 'publickey', 'domainid']
        },
        {
          api: 'deleteSSHKeyPair',
          icon: 'delete',
          label: 'Delete SSH key pair',
          params: ['name', 'domainid', 'account']
        }
      ]
    },
    {
      name: 'affinitygroups',
      title: 'Affinity Groups',
      icon: 'swap',
      permission: [ 'listAffinityGroups' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'type', 'description', 'account', 'domain'],
      actions: [
        {
          api: 'createAffinityGroup',
          icon: 'plus',
          label: 'New Affinity Group',
          params: ['name', 'description', 'type']
        },
        {
          api: 'deleteAffinityGroup',
          icon: 'delete',
          label: 'Delete Affinity Group',
          params: ['id']
        }
      ]
    }
  ]
}
