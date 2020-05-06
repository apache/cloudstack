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

export default {
  name: 'compute',
  title: 'Compute',
  icon: 'cloud',
  children: [
    {
      name: 'vm',
      title: 'Instances',
      icon: 'desktop',
      docHelp: 'adminguide/virtual_machines.html',
      permission: ['listVirtualMachinesMetrics'],
      resourceType: 'UserVm',
      columns: [
        'name', 'state', 'instancename', 'ipaddress', 'cpunumber', 'cpuused', 'cputotal',
        {
          memoryused: (record) => {
            return record.memorykbs && record.memoryintfreekbs ? parseFloat(100.0 * (record.memorykbs - record.memoryintfreekbs) / record.memorykbs).toFixed(2) + '%' : '0.0%'
          }
        },
        'memorytotal', 'networkread', 'networkwrite', 'diskkbsread', 'diskkbswrite', 'diskiopstotal',
        'account', 'zonename'
      ],
      related: [{
        name: 'volume',
        title: 'Volumes',
        param: 'virtualmachineid'
      }, {
        name: 'vmsnapshot',
        title: 'VM Snapshots',
        param: 'virtualmachineid'
      }, {
        name: 'backup',
        title: 'Backups',
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
          label: 'Update VM',
          dataView: true,
          args: ['name', 'displayname', 'ostypeid', 'isdynamicallyscalable', 'haenable', 'group']
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right',
          label: 'Start VM',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          args: ['podid', 'clusterid', 'hostid'],
          response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `Password of the VM is ${result.virtualmachine.password}` : null }
        },
        {
          api: 'stopVirtualMachine',
          icon: 'stop',
          label: 'label.action.stop.instance',
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
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'sync',
          label: 'label.reinstall.vm',
          dataView: true,
          args: ['virtualmachineid', 'templateid'],
          mapping: {
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera',
          label: 'Create VM Snapshot',
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
          label: 'Assign VM to Backup Offering',
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
          label: 'Create Backup',
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
          label: 'Remove VM from Backup Offering',
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
          show: (record) => { return !record.isoid },
          mapping: {
            id: {
              api: 'listIsos'
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
          dataView: true,
          args: ['virtualmachineid'],
          show: (record) => { return 'isoid' in record && record.isoid },
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
          label: 'Scale VM',
          dataView: true,
          args: ['serviceofferingid', 'details'],
          show: (record) => { return ['Running'].includes(record.state) && record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC' }
        },
        {
          api: 'changeServiceForVirtualMachine',
          icon: 'sliders',
          label: 'Change Service Offering',
          dataView: true,
          args: ['serviceofferingid'],
          show: (record) => { return ['Stopped'].includes(record.state) || (['Running'].includes(record.state) && record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC') }
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag',
          label: 'label.migrate.instance.to.host',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) },
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
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          args: ['storageid', 'virtualmachineid'],
          mapping: {
            storageid: {
              api: 'listStoragePools'
            },
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key',
          label: 'Reset Instance Password',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `Password of the VM is ${result.virtualmachine.password}` : null }
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock',
          label: 'Reset SSH Key',
          dataView: true,
          args: ['keypair'],
          show: (record) => { return ['Stopped'].includes(record.state) },
          mapping: {
            keypair: {
              api: 'listSSHKeyPairs'
            }
          }
        },
        {
          api: 'assignVirtualMachine',
          icon: 'user-add',
          label: 'Assign Instance to Another Account',
          dataView: true,
          component: () => import('@/views/compute/AssignInstance'),
          popup: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          args: ['virtualmachineid', 'account', 'domainid'],
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'recoverVirtualMachine',
          icon: 'medicine-box',
          label: 'label.recover.vm',
          dataView: true,
          show: (record) => { return ['Destroyed'].includes(record.state) }
        },
        {
          api: 'expungeVirtualMachine',
          icon: 'delete',
          label: 'label.action.expunge.instance',
          dataView: true,
          show: (record) => { return ['Destroyed'].includes(record.state) }
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete',
          label: 'label.action.destroy.instance',
          args: ['expunge', 'volumeids'],
          dataView: true,
          groupAction: true
        }
      ]
    },
    {
      name: 'kubernetes',
      title: 'Kubernetes',
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
          label: 'Create Kubernetes Cluster',
          listView: true,
          popup: true,
          component: () => import('@/views/compute/CreateKubernetesCluster.vue')
        },
        {
          api: 'startKubernetesCluster',
          icon: 'caret-right',
          label: 'Start Kubernetes Cluster',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'stopKubernetesCluster',
          icon: 'stop',
          label: 'Stop Kubernetes Cluster',
          dataView: true,
          show: (record) => { return !['Stopped'].includes(record.state) }
        },
        // {
        //   api: 'getKubernetesClusterConfig',
        //   icon: 'cloud-download',
        //   label: 'Download Cluster Config',
        //   dataView: true,
        //   show: (record) => { return !['Stopped'].includes(record.state) }
        // },
        {
          api: 'scaleKubernetesCluster',
          icon: 'swap',
          label: 'Scale Kubernetes Cluster',
          dataView: true,
          show: (record) => { return ['Created', 'Running'].includes(record.state) },
          popup: true,
          component: () => import('@/views/compute/ScaleKubernetesCluster.vue')
        },
        {
          api: 'upgradeKubernetesCluster',
          icon: 'plus-circle',
          label: 'Upgrade Kubernetes Cluster',
          dataView: true,
          show: (record) => { return ['Created', 'Running'].includes(record.state) },
          popup: true,
          component: () => import('@/views/compute/UpgradeKubernetesCluster.vue')
        },
        {
          api: 'deleteKubernetesCluster',
          icon: 'delete',
          label: 'Delete Kubernetes Cluster',
          dataView: true,
          show: (record) => { return !['Destroyed', 'Destroying'].includes(record.state) }
        }
      ]
    },
    {
      name: 'vmgroup',
      title: 'Instance Groups',
      icon: 'gold',
      docHelp: 'adminguide/virtual_machines.html#changing-the-vm-name-os-or-group',
      permission: ['listInstanceGroups'],
      columns: ['name', 'account', 'domain'],
      details: ['name', 'id', 'account', 'domain', 'created'],
      related: [{
        name: 'vm',
        title: 'Instances',
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
      title: 'SSH Key Pairs',
      icon: 'key',
      docHelp: 'adminguide/virtual_machines.html#using-ssh-keys-for-authentication',
      permission: ['listSSHKeyPairs'],
      columns: ['name', 'fingerprint', 'account', 'domain'],
      details: ['name', 'fingerprint', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'keypair'
      }],
      actions: [
        {
          api: 'createSSHKeyPair',
          icon: 'plus',
          label: 'Create SSH Key Pair',
          listView: true,
          popup: true,
          component: () => import('@/views/compute/CreateSSHKeyPair.vue')
        },
        {
          api: 'deleteSSHKeyPair',
          icon: 'delete',
          label: 'Delete SSH key pair',
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
      title: 'Affinity Groups',
      icon: 'swap',
      docHelp: 'adminguide/virtual_machines.html#affinity-groups',
      permission: ['listAffinityGroups'],
      columns: ['name', 'type', 'description', 'account', 'domain'],
      details: ['name', 'id', 'description', 'type', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'affinitygroupid'
      }],
      actions: [
        {
          api: 'createAffinityGroup',
          icon: 'plus',
          label: 'New Affinity Group',
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
          label: 'Delete Affinity Group',
          dataView: true
        }
      ]
    }
  ]
}
