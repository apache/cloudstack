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
      resourceType: 'UserVm',
      columns: [
        { 'name': (record) => { return record.displayname } }, 'state', 'instancename',
        { 'ipaddress': (record) => { return record.nic[0].ipaddress } }, 'account', 'zonename',
        'cpunumber', 'cpuused', 'cputotal', 'memoryintfreekbs', 'memorytotal',
        'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal'
      ],
      related: [{
        name: 'vmsnapshot',
        title: 'VM Snapshots',
        param: 'virtualmachineid'
      }, {
        name: 'affinitygroup',
        title: 'Affinity Groups',
        param: 'virtualmachineid'
      }],
      tabs: [{
        name: 'hardware',
        component: () => import('@/views/compute/InstanceHardware.vue')
      }, {
        name: 'settings',
        component: () => import('@/views/setting/ResourceSettingsTab.vue')
      }],
      actions: [
        {
          api: 'deployVirtualMachine',
          icon: 'plus',
          label: 'label.vm.add',
          listView: true,
          component: () => import('@/views/compute/DeployVM.vue')
        },
        {
          api: 'updateVirtualMachine',
          icon: 'edit',
          label: 'Update VM',
          dataView: true,
          args: ['name', 'displayname', 'ostypeid', 'isdynamicallyscalable', 'haenable', 'group']
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'Start VM',
          dataView: true,
          groupAction: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          args: ['podid', 'clusterid', 'hostid']
        },
        {
          api: 'stopVirtualMachine',
          icon: 'stop',
          label: 'label.action.stop.instance',
          dataView: true,
          groupAction: true,
          args: ['id'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'reload',
          label: 'label.action.reboot.instance',
          dataView: true,
          args: ['id'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'sync',
          label: 'label.reinstall.vm',
          dataView: true,
          args: ['virtualmachineid', 'templateid']
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera',
          label: 'Create VM Snapshot',
          dataView: true,
          args: ['virtualmachineid', 'name', 'description', 'snapshotmemory', 'quiescevm'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'attachIso',
          icon: 'paper-clip',
          label: 'label.action.attach.iso',
          dataView: true,
          args: ['id', 'virtualmachineid'],
          show: (record) => { return !record.isoid }
        },
        {
          api: 'detachIso',
          icon: 'link',
          label: 'label.action.detach.iso',
          dataView: true,
          args: ['id', 'virtualmachineid'],
          show: (record) => { return 'isoid' in record && record.isoid }
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'label.migrate.instance.to.host',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'migrateVirtualMachineWithVolume',
          icon: 'export',
          label: 'Migrate VM with Volume(s)',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'label.migrate.instance.to.ps',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'updateVMAffinityGroup',
          icon: 'swap',
          label: 'label.change.affinity',
          dataView: true,
          args: ['id', 'serviceofferingid'],
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'scaleVirtualMachine',
          icon: 'arrows-alt',
          label: 'Scale VM',
          dataView: true,
          args: ['id', 'serviceofferingid', 'details'],
          show: (record) => { return record.state === 'Stopped' || record.hypervisor === 'VMWare' }
        },
        {
          api: 'changeServiceForVirtualMachine',
          icon: 'sliders',
          label: 'Change Service Offering',
          dataView: true,
          args: ['id', 'serviceofferingid'],
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key',
          label: 'Reset Instance Password',
          dataView: true,
          args: ['id'],
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock',
          label: 'Reset SSH Key',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'assignVirtualMachine',
          icon: 'user-add',
          label: 'Assign Instance to Another Account',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'recoverVirtualMachine',
          icon: 'medicine-box',
          label: 'label.recover.vm',
          args: ['id'],
          dataView: true,
          show: (record) => { return ['Destroyed'].includes(record.state) }
        },
        {
          api: 'expungeVirtualMachine',
          icon: 'delete',
          label: 'label.action.expunge.instance',
          args: ['id'],
          dataView: true,
          show: (record) => { return ['Destroyed'].includes(record.state) }
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete',
          label: 'label.action.destroy.instance',
          args: ['id', 'expunge', 'volumeids'],
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
      columns: ['name', 'fingerprint', 'account', 'domain'],
      details: ['name', 'fingerprint', 'account', 'domain'],
      actions: [
        {
          api: 'createSSHKeyPair',
          icon: 'plus',
          label: 'Create SSH Key Pair',
          listView: true,
          args: ['name', 'account', 'domainid']
        },
        {
          api: 'registerSSHKeyPair',
          icon: 'key',
          label: 'Register SSH Public Key',
          listView: true,
          args: ['name', 'account', 'domainid', 'publickey']
        },
        {
          api: 'deleteSSHKeyPair',
          icon: 'delete',
          label: 'Delete SSH key pair',
          dataView: true,
          args: ['name', 'account', 'domainid']
        }
      ]
    },
    {
      name: 'affinitygroup',
      title: 'Affinity Groups',
      icon: 'swap',
      permission: [ 'listAffinityGroups' ],
      columns: ['name', 'type', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'type', 'account', 'domain'],
      actions: [
        {
          api: 'createAffinityGroup',
          icon: 'plus',
          label: 'New Affinity Group',
          listView: true,
          args: ['name', 'description', 'type']
        },
        {
          api: 'deleteAffinityGroup',
          icon: 'delete',
          label: 'Delete Affinity Group',
          dataView: true,
          args: ['id']
        }
      ]
    }
  ]
}
