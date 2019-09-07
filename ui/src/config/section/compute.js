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
        { 'name': (record) => { return record.displayname } }, 'state', 'instancename',
        { 'ipaddress': (record) => { return record.nic[0].ipaddress } }, 'account', 'zonename',
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
          api: 'updateVirtualMachine',
          icon: 'edit',
          label: 'Update VM',
          dataView: true
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'Start VM',
          dataView: true,
          groupAction: true,
          hidden: (record) => { return record.state !== 'Stopped' },
          options: ['podid', 'clusterid', 'hostid']
        },
        {
          api: 'stopVirtualMachine',
          icon: 'stop',
          label: 'Stop VM',
          dataView: true,
          groupAction: true,
          options: ['podid', 'clusterid', 'hostid'],
          hidden: (record) => { return record.state !== 'Running' }
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'sync',
          label: 'Reboot VM',
          dataView: true,
          hidden: (record) => { return record.state !== 'Running' }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'usb',
          label: 'Reinstall Instance',
          dataView: true,
          params: ['virtualmachineid']
        },
        {
          api: 'updateVMAffinityGroup',
          icon: 'swap',
          label: 'Update Affinity Group',
          dataView: true,
          params: ['id', 'serviceofferingid']
        },
        {
          api: 'changeServiceForVirtualMachine',
          icon: 'sliders',
          label: 'Change Service Offering',
          dataView: true,
          params: ['id', 'serviceofferingid']
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera',
          label: 'Create VM Snapshot',
          dataView: true
        },
        {
          api: 'attachIso',
          icon: 'paper-clip',
          label: 'Attach ISO',
          dataView: true,
          params: ['id', 'virtualmachineid']
        },
        {
          api: 'detachIso',
          icon: 'link',
          label: 'Detach ISO',
          dataView: true,
          params: ['id', 'virtualmachineid']
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'Migrate VM',
          dataView: true,
          hidden: (record) => { return record.state !== 'Running' }
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
          api: 'destroyVirtualMachine',
          icon: 'delete',
          label: 'Destroy VM',
          params: ['id'],
          dataView: true,
          groupAction: true
        }
      ]
    },
    /*
    {
      name: 'demo',
      title: 'Demo',
      icon: 'radar-chart',
      permission: [ 'listVirtualMachines' ],
      component: () => import('@/components/Test.vue')
    },
    */
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
