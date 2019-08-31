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
          params: ['name', 'zoneid', 'diskofferingid'],
          listView: true
        },
        {
          api: 'startVirtualMachine',
          icon: 'right-square',
          label: 'View Console',
          dataView: true
        },

        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'Start VM',
          params: ['name', 'zoneid', 'diskofferingid'],
          listView: true,
          dataView: true
        },
        {
          api: 'stopVirtualMachine',
          icon: 'stop',
          label: 'Stop VM',
          params: ['name', 'zoneid', 'diskofferingid'],
          listView: true,
          dataView: true
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'sync',
          label: 'Reboot VM',
          dataView: true
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera',
          label: 'Create VM Snapshot',
          dataView: true
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'to-top',
          label: 'Reinstall Instance',
          dataView: true,
          params: ['virtualmachineid']
        },
        {
          api: 'attachIso',
          icon: 'paper-clip',
          label: 'Attach ISO to Instance',
          dataView: true,
          params: ['id', 'virtualmachineid']
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'Migrate VM',
          dataView: true
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key',
          label: 'Reset Instance Password',
          dataView: true,
          params: ['id']
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock',
          label: 'Reset SSH Key',
          dataView: true
        },
        {
          api: 'changeServiceForVirtualMachine',
          icon: 'swap',
          label: 'Change Service Offering',
          dataView: true,
          params: ['id', 'serviceofferingid']
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete',
          label: 'Destroy VM',
          params: ['id'],
          listView: true,
          dataView: true
        }
      ]
    },
    {
      name: 'demo',
      title: 'Demo',
      icon: 'radar-chart',
      permission: [ 'listVirtualMachines' ],
      component: () => import('@/components/Test.vue')
    },
    {
      name: 'ssh',
      title: 'SSH Key Pairs',
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
