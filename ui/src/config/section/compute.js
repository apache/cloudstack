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

import { shallowRef, defineAsyncComponent } from 'vue'
import kubernetes from '@/assets/icons/kubernetes.svg?inline'
import store from '@/store'

export default {
  name: 'compute',
  title: 'label.compute',
  icon: 'cloud-outlined',
  children: [
    {
      name: 'vm',
      title: 'label.instances',
      icon: 'desktop-outlined',
      docHelp: 'adminguide/virtual_machines.html',
      permission: ['listVirtualMachinesMetrics'],
      resourceType: 'UserVm',
      params: () => {
        var params = { details: 'servoff,tmpl,nics' }
        if (store.getters.metrics) {
          params = { details: 'servoff,tmpl,nics,stats' }
        }
        return params
      },
      filters: () => {
        const filters = ['running', 'stopped']
        if (!(store.getters.project && store.getters.project.id)) {
          filters.unshift('self')
        }
        return filters
      },
      columns: () => {
        const fields = ['name', 'state', 'ipaddress']
        const metricsFields = ['cpunumber', 'cputotal', 'cpuused', 'memorytotal',
          {
            memoryused: (record) => {
              if (record.memoryintfreekbs <= 0 || record.memorykbs <= 0) {
                return '-'
              }
              return parseFloat(100.0 * (record.memorykbs - record.memoryintfreekbs) / record.memorykbs).toFixed(2) + '%'
            }
          },
          'networkread', 'networkwrite', 'diskread', 'diskwrite', 'diskiopstotal']

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }

        if (store.getters.userInfo.roletype === 'Admin') {
          fields.splice(2, 0, 'instancename')
          fields.push('account')
          fields.push('hostname')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
        } else {
          fields.push('serviceofferingname')
        }
        fields.push('zonename')
        return fields
      },
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'groupid', 'tags'],
      details: () => {
        var fields = ['name', 'displayname', 'id', 'state', 'ipaddress', 'ip6address', 'templatename', 'ostypename',
          'serviceofferingname', 'isdynamicallyscalable', 'haenable', 'hypervisor', 'boottype', 'bootmode', 'account',
          'domain', 'zonename', 'userdataid', 'userdataname', 'userdataparams', 'userdatadetails', 'userdatapolicy', 'hostcontrolstate']
        const listZoneHaveSGEnabled = store.getters.zones.filter(zone => zone.securitygroupsenabled === true)
        if (!listZoneHaveSGEnabled || listZoneHaveSGEnabled.length === 0) {
          return fields
        }
        fields.push('securitygroup')
        return fields
      },
      tabs: [{
        component: shallowRef(defineAsyncComponent(() => import('@/views/compute/InstanceTab.vue')))
      }],
      actions: [
        {
          api: 'deployVirtualMachine',
          icon: 'plus-outlined',
          label: 'label.vm.add',
          docHelp: 'adminguide/virtual_machines.html#creating-vms',
          listView: true,
          component: () => import('@/views/compute/DeployVM.vue')
        },
        {
          api: 'updateVirtualMachine',
          icon: 'edit-outlined',
          label: 'label.action.edit.instance',
          docHelp: 'adminguide/virtual_machines.html#changing-the-vm-name-os-or-group',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/EditVM.vue')))
        },
        {
          api: 'startVirtualMachine',
          icon: 'caret-right-outlined',
          label: 'label.action.start.instance',
          message: 'message.action.start.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, considerlasthost: values.considerlasthost } }) },
          args: ['considerlasthost'],
          show: (record) => { return ['Stopped'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/StartVirtualMachine.vue')))
        },
        {
          api: 'stopVirtualMachine',
          icon: 'poweroff-outlined',
          label: 'label.action.stop.instance',
          message: 'message.action.stop.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          groupAction: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) },
          args: ['forced'],
          show: (record) => { return ['Running'].includes(record.state) }
        },
        {
          api: 'rebootVirtualMachine',
          icon: 'reload-outlined',
          label: 'label.action.reboot.instance',
          message: 'message.action.reboot.instance',
          docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          args: (record, store) => {
            var fields = []
            fields.push('forced')
            if (record.hypervisor === 'VMware') {
              if (store.apis.rebootVirtualMachine.params.filter(x => x.name === 'bootintosetup').length > 0) {
                fields.push('bootintosetup')
              }
            }
            return fields
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
        },
        {
          api: 'restoreVirtualMachine',
          icon: 'sync-outlined',
          label: 'label.reinstall.vm',
          message: 'message.reinstall.vm',
          dataView: true,
          args: ['virtualmachineid', 'templateid'],
          filters: (record) => {
            var filters = {}
            var filterParams = {}
            filterParams.hypervisortype = record.hypervisor
            filterParams.zoneid = record.zoneid
            filters.templateid = filterParams
            return filters
          },
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) },
          mapping: {
            virtualmachineid: {
              value: (record) => { return record.id }
            }
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          successMethod: (obj, result) => {
            const vm = result.jobresult.virtualmachine || {}
            if (result.jobstatus === 1 && vm.password) {
              const name = vm.displayname || vm.name || vm.id
              obj.$notification.success({
                message: `${obj.$t('label.reinstall.vm')}: ` + name,
                description: `${obj.$t('label.password.reset.confirm')}: ` + vm.password,
                duration: 0
              })
            }
          }
        },
        {
          api: 'createVMSnapshot',
          icon: 'camera-outlined',
          label: 'label.action.vmsnapshot.create',
          docHelp: 'adminguide/virtual_machines.html#virtual-machine-snapshots',
          dataView: true,
          args: ['virtualmachineid', 'name', 'description', 'snapshotmemory', 'quiescevm'],
          show: (record) => {
            return ((['Running'].includes(record.state) && record.hypervisor !== 'LXC') ||
              (['Stopped'].includes(record.state) && ((record.hypervisor !== 'KVM' && record.hypervisor !== 'LXC') ||
              (record.hypervisor === 'KVM' && record.pooltype === 'PowerFlex'))))
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' && record.hypervisor === 'KVM' },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'createSnapshot',
          icon: ['fas', 'camera-retro'],
          label: 'label.action.vmstoragesnapshot.create',
          docHelp: 'adminguide/virtual_machines.html#virtual-machine-snapshots',
          dataView: true,
          popup: true,
          show: (record) => {
            return ((['Running'].includes(record.state) && record.hypervisor !== 'LXC') ||
              (['Stopped'].includes(record.state) && !['KVM', 'LXC'].includes(record.hypervisor)))
          },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' && record.hypervisor === 'KVM' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/CreateSnapshotWizard.vue')))
        },
        {
          api: 'assignVirtualMachineToBackupOffering',
          icon: 'folder-add-outlined',
          label: 'label.backup.offering.assign',
          message: 'label.backup.offering.assign',
          docHelp: 'adminguide/virtual_machines.html#backup-offerings',
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
          icon: 'cloud-upload-outlined',
          label: 'label.create.backup',
          message: 'message.backup.create',
          docHelp: 'adminguide/virtual_machines.html#creating-vm-backups',
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
          icon: 'schedule-outlined',
          label: 'Configure Backup Schedule',
          docHelp: 'adminguide/virtual_machines.html#creating-vm-backups',
          dataView: true,
          popup: true,
          show: (record) => { return record.backupofferingid },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/BackupScheduleWizard.vue'))),
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
          icon: 'scissor-outlined',
          label: 'label.backup.offering.remove',
          message: 'label.backup.offering.remove',
          docHelp: 'adminguide/virtual_machines.html#restoring-vm-backups',
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
          icon: 'paper-clip-outlined',
          label: 'label.action.attach.iso',
          docHelp: 'adminguide/templates.html#attaching-an-iso-to-a-vm',
          dataView: true,
          popup: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && !record.isoid },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' || record.hostcontrolstate === 'Maintenance' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AttachIso.vue')))
        },
        {
          api: 'detachIso',
          icon: 'link-outlined',
          label: 'label.action.detach.iso',
          message: 'message.detach.iso.confirm',
          dataView: true,
          args: (record, store) => {
            var args = ['virtualmachineid']
            if (record && record.hypervisor && record.hypervisor === 'VMware') {
              args.push('forced')
            }
            return args
          },
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && 'isoid' in record && record.isoid },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' || record.hostcontrolstate === 'Maintenance' },
          mapping: {
            virtualmachineid: {
              value: (record, params) => { return record.id }
            }
          }
        },
        {
          api: 'updateVMAffinityGroup',
          icon: 'swap-outlined',
          label: 'label.change.affinity',
          docHelp: 'adminguide/virtual_machines.html#change-affinity-group-for-an-existing-vm',
          dataView: true,
          args: ['affinitygroupids'],
          show: (record) => { return ['Stopped'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ChangeAffinity'))),
          popup: true
        },
        {
          api: 'scaleVirtualMachine',
          icon: 'arrows-alt-outlined',
          label: 'label.scale.vm',
          docHelp: 'adminguide/virtual_machines.html#how-to-dynamically-scale-cpu-and-ram',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) || (['Running'].includes(record.state) && record.hypervisor !== 'LXC') },
          disabled: (record) => { return record.state === 'Running' && !record.isdynamicallyscalable },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ScaleVM.vue')))
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag-outlined',
          label: 'label.migrate.instance.to.host',
          docHelp: 'adminguide/virtual_machines.html#moving-vms-between-hosts-manual-live-migration',
          dataView: true,
          show: (record, store) => { return ['Running'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateWizard.vue')))
        },
        {
          api: 'migrateVirtualMachine',
          icon: 'drag-outlined',
          label: 'label.migrate.instance.to.ps',
          message: 'message.migrate.instance.to.ps',
          docHelp: 'adminguide/virtual_machines.html#moving-vms-between-hosts-manual-live-migration',
          dataView: true,
          show: (record, store) => { return ['Stopped'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateVMStorage'))),
          popup: true
        },
        {
          api: 'resetPasswordForVirtualMachine',
          icon: 'key-outlined',
          label: 'label.action.reset.password',
          message: 'message.action.instance.reset.password',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) && record.passwordenabled },
          response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `The password of VM <b>${result.virtualmachine.displayname}</b> is <b>${result.virtualmachine.password}</b>` : null }
        },
        {
          api: 'resetSSHKeyForVirtualMachine',
          icon: 'lock-outlined',
          label: 'label.reset.ssh.key.pair',
          message: 'message.desc.reset.ssh.key.pair',
          docHelp: 'adminguide/virtual_machines.html#resetting-ssh-keys',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ResetSshKeyPair')))
        },
        {
          api: 'resetUserDataForVirtualMachine',
          icon: 'solution-outlined',
          label: 'label.reset.userdata.on.vm',
          message: 'message.desc.reset.userdata',
          docHelp: 'adminguide/virtual_machines.html#resetting-userdata',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ResetUserData')))
        },
        {
          api: 'assignVirtualMachine',
          icon: 'user-add-outlined',
          label: 'label.assign.instance.another',
          dataView: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AssignInstance'))),
          popup: true,
          show: (record) => { return ['Stopped'].includes(record.state) }
        },
        {
          api: 'recoverVirtualMachine',
          icon: 'medicine-box-outlined',
          label: 'label.recover.vm',
          message: 'message.recover.vm',
          dataView: true,
          show: (record, store) => { return ['Destroyed'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'unmanageVirtualMachine',
          icon: 'disconnect-outlined',
          label: 'label.action.unmanage.virtualmachine',
          message: 'message.action.unmanage.virtualmachine',
          dataView: true,
          show: (record) => { return ['Running', 'Stopped'].includes(record.state) && record.hypervisor === 'VMware' }
        },
        {
          api: 'expungeVirtualMachine',
          icon: 'delete-outlined',
          label: 'label.action.expunge.instance',
          message: (record) => { return record.backupofferingid ? 'message.action.expunge.instance.with.backups' : 'message.action.expunge.instance' },
          docHelp: 'adminguide/virtual_machines.html#deleting-vms',
          dataView: true,
          show: (record, store) => { return ['Destroyed', 'Expunging'].includes(record.state) && store.features.allowuserexpungerecovervm }
        },
        {
          api: 'destroyVirtualMachine',
          icon: 'delete-outlined',
          label: 'label.action.destroy.instance',
          message: 'message.action.destroy.instance',
          docHelp: 'adminguide/virtual_machines.html#deleting-vms',
          dataView: true,
          groupAction: true,
          args: (record, store, group) => {
            return (['Admin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervm)
              ? ['expunge'] : []
          },
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, expunge: values.expunge } }) },
          show: (record) => { return ['Running', 'Stopped', 'Error'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/DestroyVM.vue')))
        }
      ]
    },
    {
      name: 'kubernetes',
      title: 'label.kubernetes',
      icon: shallowRef(kubernetes),
      docHelp: 'plugins/cloudstack-kubernetes-service.html',
      permission: ['listKubernetesClusters'],
      columns: (store) => {
        var fields = ['name', 'state', 'size', 'cpunumber', 'memory', 'kubernetesversionname']
        if (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype)) {
          fields.push('account')
        }
        if (store.apis.scaleKubernetesCluster.params.filter(x => x.name === 'autoscalingenabled').length > 0) {
          fields.splice(2, 0, 'autoscalingenabled')
        }
        fields.push('zonename')
        return fields
      },
      details: ['name', 'description', 'zonename', 'kubernetesversionname', 'autoscalingenabled', 'minsize', 'maxsize', 'size', 'controlnodes', 'cpunumber', 'memory', 'keypair', 'associatednetworkname', 'account', 'domain', 'zonename', 'created'],
      tabs: [{
        name: 'k8s',
        component: shallowRef(defineAsyncComponent(() => import('@/views/compute/KubernetesServiceTab.vue')))
      }],
      resourceType: 'KubernetesCluster',
      actions: [
        {
          api: 'createKubernetesCluster',
          icon: 'plus-outlined',
          label: 'label.kubernetes.cluster.create',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#creating-a-new-kubernetes-cluster',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/CreateKubernetesCluster.vue')))
        },
        {
          api: 'startKubernetesCluster',
          icon: 'caret-right-outlined',
          label: 'label.kubernetes.cluster.start',
          message: 'message.kubernetes.cluster.start',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#starting-a-stopped-kubernetes-cluster',
          dataView: true,
          show: (record) => { return ['Stopped'].includes(record.state) },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'stopKubernetesCluster',
          icon: 'poweroff-outlined',
          label: 'label.kubernetes.cluster.stop',
          message: 'message.kubernetes.cluster.stop',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#stopping-kubernetes-cluster',
          dataView: true,
          show: (record) => { return !['Stopped', 'Destroyed', 'Destroying'].includes(record.state) },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'scaleKubernetesCluster',
          icon: 'swap-outlined',
          label: 'label.kubernetes.cluster.scale',
          message: 'message.kubernetes.cluster.scale',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#scaling-kubernetes-cluster',
          dataView: true,
          show: (record) => { return ['Created', 'Running', 'Stopped'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/ScaleKubernetesCluster.vue')))
        },
        {
          api: 'upgradeKubernetesCluster',
          icon: 'plus-circle-outlined',
          label: 'label.kubernetes.cluster.upgrade',
          message: 'message.kubernetes.cluster.upgrade',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#upgrading-kubernetes-cluster',
          dataView: true,
          show: (record) => { return ['Created', 'Running'].includes(record.state) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/UpgradeKubernetesCluster.vue')))
        },
        {
          api: 'deleteKubernetesCluster',
          icon: 'delete-outlined',
          label: 'label.kubernetes.cluster.delete',
          message: 'message.kubernetes.cluster.delete',
          docHelp: 'plugins/cloudstack-kubernetes-service.html#deleting-kubernetes-cluster',
          dataView: true,
          show: (record) => { return !['Destroyed', 'Destroying'].includes(record.state) },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'autoscalevmgroup',
      title: 'label.autoscale.vm.groups',
      icon: 'ordered-list-outlined',
      docHelp: 'adminguide/autoscale_without_netscaler.html',
      resourceType: 'AutoScaleVmGroup',
      permission: ['listAutoScaleVmGroups'],
      columns: ['name', 'state', 'associatednetworkname', 'publicip', 'publicport', 'privateport', 'minmembers', 'maxmembers', 'availablevirtualmachinecount', 'account'],
      details: ['name', 'id', 'account', 'domain', 'associatednetworkname', 'associatednetworkid', 'lbruleid', 'lbprovider', 'publicip', 'publicipid', 'publicport', 'privateport', 'minmembers', 'maxmembers', 'availablevirtualmachinecount', 'interval', 'state', 'created'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'autoscalevmgroupid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'autoscale.vm.profile',
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AutoScaleVmProfile.vue')))
        },
        {
          name: 'loadbalancerrule',
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AutoScaleLoadBalancing.vue')))
        },
        {
          name: 'scaleup.policy',
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AutoScaleUpPolicyTab.vue')))
        },
        {
          name: 'scaledown.policy',
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/AutoScaleDownPolicyTab.vue')))
        },
        {
          name: 'events',
          resourceType: 'AutoScaleVmGroup',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      actions: [
        {
          api: 'createAutoScaleVmGroup',
          icon: 'plus-outlined',
          label: 'label.new.autoscale.vmgroup',
          listView: true,
          component: () => import('@/views/compute/CreateAutoScaleVmGroup.vue')
        },
        {
          api: 'enableAutoScaleVmGroup',
          icon: 'play-circle-outlined',
          label: 'label.enable.autoscale.vmgroup',
          message: 'message.confirm.enable.autoscale.vmgroup',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
          show: (record) => { return record.state === 'DISABLED' }
        },
        {
          api: 'disableAutoScaleVmGroup',
          icon: 'pause-circle-outlined',
          label: 'label.disable.autoscale.vmgroup',
          message: 'message.confirm.disable.autoscale.vmgroup',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
          show: (record) => { return ['ENABLED', 'SCALING'].includes(record.state) }
        },
        {
          api: 'updateAutoScaleVmGroup',
          icon: 'edit-outlined',
          label: 'label.update.autoscale.vmgroup',
          dataView: true,
          args: (record, store) => {
            var args = ['name']
            if (record.state === 'DISABLED') {
              args.push('maxmembers')
              args.push('minmembers')
              args.push('interval')
            }
            return args
          }
        },
        {
          api: 'deleteAutoScaleVmGroup',
          icon: 'delete-outlined',
          label: 'label.delete.autoscale.vmgroup',
          message: 'message.action.delete.autoscale.vmgroup',
          dataView: true,
          args: ['cleanup'],
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, cleanup: values.cleanup || null } }) }
        }
      ]
    },
    {
      name: 'vmgroup',
      title: 'label.instance.groups',
      icon: 'gold-outlined',
      docHelp: 'adminguide/virtual_machines.html#changing-the-vm-name-os-or-group',
      resourceType: 'VMInstanceGroup',
      permission: ['listInstanceGroups'],
      columns: ['name', 'account', 'domain'],
      details: ['name', 'id', 'account', 'domain', 'created'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'groupid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      actions: [
        {
          api: 'createInstanceGroup',
          icon: 'plus-outlined',
          label: 'label.new.instance.group',
          listView: true,
          args: ['name']
        },
        {
          api: 'updateInstanceGroup',
          icon: 'edit-outlined',
          label: 'label.update.instance.group',
          dataView: true,
          args: ['name']
        },
        {
          api: 'deleteInstanceGroup',
          icon: 'delete-outlined',
          label: 'label.delete.instance.group',
          message: 'message.action.delete.instance.group',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'ssh',
      title: 'label.ssh.key.pairs',
      icon: 'key-outlined',
      docHelp: 'adminguide/virtual_machines.html#using-ssh-keys-for-authentication',
      permission: ['listSSHKeyPairs'],
      columns: () => {
        var fields = ['name', 'fingerprint']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
          fields.push('domain')
        }
        return fields
      },
      resourceType: 'SSHKeyPair',
      details: ['id', 'name', 'fingerprint', 'account', 'domain', 'project'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'keypair'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      actions: [
        {
          api: 'createSSHKeyPair',
          icon: 'plus-outlined',
          label: 'label.create.ssh.key.pair',
          docHelp: 'adminguide/virtual_machines.html#creating-the-ssh-keypair',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/CreateSSHKeyPair.vue')))
        },
        {
          api: 'deleteSSHKeyPair',
          icon: 'delete-outlined',
          label: 'label.remove.ssh.key.pair',
          message: 'message.please.confirm.remove.ssh.key.pair',
          dataView: true,
          args: ['name', 'account', 'domainid', 'projectid'],
          mapping: {
            name: {
              value: (record, params) => { return record.name }
            },
            projectid: {
              value: (record, params) => { return record.projectid }
            },
            account: {
              value: (record, params) => { return record.account }
            },
            domainid: {
              value: (record, params) => { return record.domainid }
            }
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values, record) => {
            return selection.map(x => {
              const data = record.filter(y => { return y.id === x })
              return {
                name: data[0].name,
                account: data[0].account,
                domainid: data[0].domainid,
                projectid: data[0].projectid
              }
            })
          }
        }
      ]
    },
    {
      name: 'userdata',
      title: 'label.user.data',
      icon: 'solution-outlined',
      docHelp: 'adminguide/virtual_machines.html#user-data-and-meta-data',
      permission: ['listUserData'],
      columns: () => {
        var fields = ['name', 'id']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
          fields.push('domain')
        }
        return fields
      },
      resourceType: 'UserData',
      details: ['id', 'name', 'userdata', 'account', 'domain', 'params'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'userdata'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      actions: [
        {
          api: 'registerUserData',
          icon: 'plus-outlined',
          label: 'label.register.user.data',
          docHelp: 'adminguide/virtual_machines.html#creating-the-ssh-keypair',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/RegisterUserData.vue')))
        },
        {
          api: 'deleteUserData',
          icon: 'delete-outlined',
          label: 'label.remove.user.data',
          message: 'message.please.confirm.remove.user.data',
          dataView: true,
          args: ['id', 'account', 'domainid'],
          mapping: {
            id: {
              value: (record, params) => { return record.id }
            },
            account: {
              value: (record, params) => { return record.account }
            },
            domainid: {
              value: (record, params) => { return record.domainid }
            }
          },
          groupAction: true,
          popup: true,
          groupMap: (selection, values, record) => {
            return selection.map(x => {
              const data = record.filter(y => { return y.id === x })
              return {
                id: x, account: data[0].account, domainid: data[0].domainid
              }
            })
          }
        }
      ]
    },
    {
      name: 'affinitygroup',
      title: 'label.affinity.groups',
      icon: 'swap-outlined',
      docHelp: 'adminguide/virtual_machines.html#affinity-groups',
      permission: ['listAffinityGroups'],
      columns: () => {
        var fields = ['name', 'type', 'description']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
          fields.push('domain')
        }
        return fields
      },
      details: ['name', 'id', 'description', 'type', 'account', 'domain'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'affinitygroupid'
      }],
      actions: [
        {
          api: 'createAffinityGroup',
          icon: 'plus-outlined',
          label: 'label.add.affinity.group',
          docHelp: 'adminguide/virtual_machines.html#creating-a-new-affinity-group',
          listView: true,
          args: ['name', 'description', 'type'],
          mapping: {
            type: {
              options: ['host anti-affinity (Strict)', 'host affinity (Strict)', 'host anti-affinity (Non-Strict)', 'host affinity (Non-Strict)']
            }
          }
        },
        {
          api: 'deleteAffinityGroup',
          icon: 'delete-outlined',
          label: 'label.delete.affinity.group',
          docHelp: 'adminguide/virtual_machines.html#delete-an-affinity-group',
          message: 'message.delete.affinity.group',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    }
  ]
}
