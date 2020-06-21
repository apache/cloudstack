// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import kubernetes from '@/assets/icons/kubernetes.svg?inline'
import store from '@/store'

export default {
  name: 'compute',
  title: 'label.compute',
  icon: 'cloud',
  children: [
    {
      name: 'vm',
      title: 'label.instances',
      icon: 'desktop',
      docHelp: 'adminguide/virtual_machines.html',
      permission: ['listVirtualMachinesMetrics'],
      resourceType: 'UserVm',
      filters: ['self', 'running', 'stopped'],
      columns: () => {
        const fields = ['name', 'state', 'ipaddress']
        const metricsFields = ['cpunumber', 'cpuused', 'cputotal',
          {
            memoryused: (record) => {
              return record.memorykbs && record.memoryintfreekbs ? parseFloat(100.0 * (record.memorykbs - record.memoryintfreekbs) / record.memorykbs).toFixed(2) + '%' : '0.0%'
            }
          },
          'memorytotal', 'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal'
        ]

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }

        if (store.getters.userInfo.roletype === 'Admin') {
          fields.splice(2, 0, 'instancename')
          fields.push('account')
          fields.push('hostname')
          fields.push('zonename')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.splice(2, 0, 'displayname')
          fields.push('account')
          fields.push('zonename')
        } else {
          fields.splice(2, 0, 'displayname')
          fields.push('zonename')
        }
        return fields
      },
      related: [{
        name: 'volume',
        title: 'label.volumes',
        param: 'virtualmachineid'
      }, {
        name: 'vmsnapshot',
        title: 'label.vm.snapshots',
        param: 'virtualmachineid'
      }, {
        name: 'backup',
        title: 'label.backup',
        param: 'virtualmachineid'
      }, {
        name: 'affinitygroup',
        title: 'label.affinity.groups',
        param: 'virtualmachineid'
      }],
      tabs: [{
        name: 'hardware',
        component: () => import('@/views/compute/InstanceHardware.vue')
      }, {
        name: 'settings',
        component: () => import('@/components/view/DetailSettings')
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
          label: 'label.action.edit.instance',
          dataView: true,
          args: ['name', 'displayname', 'ostypeid', 'isdynamicallyscalable', 'haenable', 'group'],
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'label.action.start.instance',
          message: 'message.action.start.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          args: (record, store) => {
            var fields = []
            if (store.userInfo.roletype === 'Admin') {
              fields = ['podid', 'clusterid', 'hostid']
            }
            if (record.hypervisor === 'VMware') {
              if (store.apis.startVirtualMachine.params.filter(x => x.name === 'bootintosetup').length > 0) {
                fields.push('bootintosetup')
              }
            }
            return fields
          },
          response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `Password of the VM is ${result.virtualmachine.password}` : null }
        },
        {
          api: 'stopVirtualMachine',
          icon: 'poweroff',
          label: 'label.action.stop.instance',
          message: 'message.action.stop.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          args: ['forced'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'reload',
          label: 'label.action.reboot.instance',
          message: 'message.action.reboot.instance',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) },
          args: (record, store) => {
            var fields = []
            if (record.hypervisor === 'VMware') {
              if (store.apis.rebootVirtualMachine.params.filter(x => x.name === 'bootintosetup').length > 0) {
                fields.push('bootintosetup')
              }
            }
            return fields
          }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'sync',
          label: 'label.reinstall.vm',
          message: 'message.reinstall.vm',
          dataView: true,
          args: ['virtualmachineid', 'templateid'],
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) },
          mapping: {
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera',
          label: 'label.action.vmsnapshot.create',
          dataView: true,
          args: ['virtualmachineid', 'name', 'description', 'snapshotmemory', 'quiescevm'],
          show: (record) => {
            return ((['Running'].includes(record.state) && record.hypervisor !== 'LXC') ||
              (['Stopped'].includes(record.state) && record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC'))
          },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'assignVirtualMachineToBackupOffering',
          icon: 'folder-add',
          label: 'label.backup.offering.assign',
          message: 'label.backup.offering.assign',
          dataView: true,
          args: ['virtualmachineid', 'backupofferingid'],
          show: (record) => { return !record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createBackup',
          icon: 'cloud-upload',
          label: 'label.create.backup',
          dataView: true,
          args: ['virtualmachineid'],
          show: (record) => { return record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createBackupSchedule',
          icon: 'schedule',
          label: 'Configure Backup Schedule',
          dataView: true,
          popup: true,
          show: (record) => { return record.backupofferingid },
          component: () => import('@/views/compute/BackupScheduleWizard.vue'),
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            },
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'removeVirtualMachineFromBackupOffering',
          icon: 'scissor',
          label: 'label.backup.offering.remove',
          message: 'label.backup.offering.remove',
          dataView: true,
          args: ['virtualmachineid', 'forced'],
          show: (record) => { return record.backupofferingid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'attachIso',
          icon: 'paper-clip',
          label: 'label.action.attach.iso',
          dataView: true,
          args: ['id', 'virtualmachineid'],
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && !record.isoid },
          mapping: {
            id: {
              api: 'listIsos',
              params: (record) => { return { zoneid: record.zoneid } }
            },
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'detachIso',
          icon: 'link',
          label: 'label.action.detach.iso',
          message: 'message.detach.iso.confirm',
          dataView: true,
          args: ['virtualmachineid'],
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && 'isoid' in record && record.isoid },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'updateVMAffinityGroup',
          icon: 'swap',
          label: 'label.change.affinity',
          dataView: true,
          args: ['affinitygroupids'],
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'scaleVirtualMachine',
          icon: 'arrows-alt',
          // label: label.change.service.offering
          label: 'Scale VM',
          dataView: true,
          args: ['serviceofferingid', 'details'],
          show: (record) => { return ['Running'].includes(record.state) && record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC' }
        },
        {
          api: 'changeServiceForVirtualMachine',
          icon: 'sliders',
          label: 'label.change.service.offering',
          dataView: true,
          args: ['serviceofferingid'],
          show: (record) => { return ['Stopped'].includes(record.state) || (['Running'].includes(record.state) && record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC') }
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'label.migrate.instance.to.host',
          dataView: true,
          show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          component: () => import('@/views/compute/MigrateWizard'),
          popup: true,
          args: ['hostid', 'virtualmachineid'],
          mapping: {
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'label.migrate.instance.to.ps',
          message: 'message.migrate.instance.to.ps',
          dataView: true,
          show: (record, store) => { return ['Stopped'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          args: ['storageid', 'virtualmachineid'],
          mapping: {
            storageid: {
              api: 'listStoragePools',
              params: (record) => { return { zoneid: record.zoneid } }
            },
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key',
          label: 'label.action.reset.password',
          message: 'message.action.instance.reset.password',
          dataView: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) },
          response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `Password of the VM is ${result.virtualmachine.password}` : null }
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock',
          label: 'label.reset.ssh.key.pair',
          message: 'message.desc.reset.ssh.key.pair',
          dataView: true,
          args: ['keypair', 'account', 'domainid'],
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) },
          mapping: {
            keypair: {
              api: 'listSSHKeyPairs',
              params: (record) => { return { account: record.account, domainid: record.domainid } }
            },
            account: {
              value: (record) => { return record.account }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        },
        {
          api: 'assignVirtualMachine',
          icon: 'user-add',
          label: 'label.assign.instance.another',
          dataView: true,
          component: () => import('@/views/compute/AssignInstance'),
          popup: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'recoverVirtualMachine',
          icon: 'medicine-box',
          label: 'label.recover.vm',
          message: 'message.recover.vm',
          dataView: true,
          show: (record, store) => { return ['Destroyed'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'unmanageVirtualMachine',
          icon: 'disconnect',
          label: 'label.action.unmanage.virtualmachine',
          dataView: true,
          groupAction: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && record.hypervisor === 'VMware' }
        },
        {
          api: 'expungeVirtualMachine',
          icon: 'delete',
          label: 'label.action.expunge.instance',
          message: 'message.action.expunge.instance',
          dataView: true,
          show: (record, store) => { return ['Destroyed', 'Expunging'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete',
          label: 'label.action.destroy.instance',
          message: 'message.action.destroy.instance',
          args: ['expunge', 'volumeids'],
          mapping: {
            volumeids: {
              api: 'listVolumes',
              params: (record) => { return { virtualmachineid: record.id, type: 'DATADISK' } }
            }
          },
          dataView: true,
          groupAction: true,
          show: (record) => { return ['Running', 'Stopped', 'Error'].includes(record.state) }
        }
      ]
    },
    {
      name: 'kubernetes',
      title: 'label.kubernetes',
      icon: kubernetes,
      permission: ['listKubernetesClusters'],
      columns: ['name', 'state', 'size', 'cpunumber', 'memory', 'account', 'zonename'],
      details: ['name', 'description', 'zonename', 'kubernetesversionname', 'size', 'masternodes', 'cpunumber', 'memory', 'keypair', 'associatednetworkname', 'account', 'domain', 'zonename'],
      tabs: [{
        name: 'k8s',
        component: () => import('@/views/compute/KubernetesServiceTab.vue')
      }],
      actions: [
        {
          api: 'createKubernetesCluster',
          icon: 'plus',
          label: 'label.kubernetes.cluster.create',
          listView: true,
          popup: true,
          component: () => import('@/views/compute/CreateKubernetesCluster.vue')
        },
        {
          api: 'startKubernetesCluster',
          icon: 'caret-right',
          label: 'label.kubernetes.cluster.start',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'stopKubernetesCluster',
          icon: 'poweroff',
          label: 'label.kubernetes.cluster.stop',
          dataView: true,
          show: (record) => { return !['Stopped'].includes(record.state) }
        },
        {
          api: 'scaleKubernetesCluster',
          icon: 'swap',
          label: 'label.kubernetes.cluster.scale',
          dataView: true,
          show: (record) => { return ['Created', 'Running'].includes(record.state) },
          popup: true,
          component: () => import('@/views/compute/ScaleKubernetesCluster.vue')
        },
        {
          api: 'upgradeKubernetesCluster',
          icon: 'plus-circle',
          label: 'label.kubernetes.cluster.upgrade',
          dataView: true,
          show: (record) => { return ['Created', 'Running'].includes(record.state) },
          popup: true,
          component: () => import('@/views/compute/UpgradeKubernetesCluster.vue')
        },
        {
          api: 'deleteKubernetesCluster',
          icon: 'delete',
          label: 'label.kubernetes.cluster.delete',
          dataView: true,
          show: (record) => { return !['Destroyed', 'Destroying'].includes(record.state) }
        }
      ]
    },
    {
      name: 'vmgroup',
      title: 'label.instance.groups',
      icon: 'gold',
      docHelp: 'adminguide/virtual_machines.html#changing-the-vm-name-os-or-group',
      permission: ['listInstanceGroups'],
      columns: ['name', 'account', 'domain'],
      details: ['name', 'id', 'account', 'domain', 'created'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'groupid'
      }],
      actions: [
        {
          api: 'createInstanceGroup',
          icon: 'plus',
          label: 'New Instance Group',
          listView: true,
          args: ['name']
        },
        {
          api: 'updateInstanceGroup',
          icon: 'edit',
          label: 'Update Instance Group',
          dataView: true,
          args: ['name']
        },
        {
          api: 'deleteInstanceGroup',
          icon: 'delete',
          label: 'Delete Instance Group',
          dataView: true
        }
      ]
    },
    {
      name: 'ssh',
      title: 'label.ssh.key.pairs',
      icon: 'key',
      docHelp: 'adminguide/virtual_machines.html#using-ssh-keys-for-authentication',
      permission: ['listSSHKeyPairs'],
      columns: ['name', 'fingerprint', 'account', 'domain'],
      details: ['name', 'fingerprint', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'keypair'
      }],
      actions: [
        {
          api: 'createSSHKeyPair',
          icon: 'plus',
          label: 'label.create.ssh.key.pair',
          listView: true,
          popup: true,
          component: () => import('@/views/compute/CreateSSHKeyPair.vue')
        },
        {
          api: 'deleteSSHKeyPair',
          icon: 'delete',
          label: 'label.remove.ssh.key.pair',
          dataView: true,
          args: ['name', 'account', 'domainid'],
          mapping: {
            name: {
              value: (record, params) => { return record.name }
            },
            account: {
              value: (record, params) => { return record.account }
            },
            domainid: {
              value: (record, params) => { return record.domainid }
            }
          }
        }
      ]
    },
    {
      name: 'affinitygroup',
      title: 'label.affinity.groups',
      icon: 'swap',
      docHelp: 'adminguide/virtual_machines.html#affinity-groups',
      permission: ['listAffinityGroups'],
      columns: ['name', 'type', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'type', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'affinitygroupid'
      }],
      actions: [
        {
          api: 'createAffinityGroup',
          icon: 'plus',
          label: 'label.add.affinity.group',
          listView: true,
          args: ['name', 'description', 'type'],
          mapping: {
            type: {
              options: ['host anti-affinity', 'host affinity']
            }
          }
        },
        {
          api: 'deleteAffinityGroup',
          icon: 'delete',
          label: 'label.delete.affinity.group',
          message: 'message.delete.affinity.group',
          dataView: true
        }
      ]
    }
  ]
}
