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

<template>
  <div style="width: auto;">
    <a-steps
      ref="resourceStep"
      progressDot
      :current="currentStep"
      size="small"
      style="margin-left: 0; margin-top: 16px;">
      <a-step
        v-for="(step, index) in steps"
        :ref="`resourceStep${index}`"
        :key="step.title"
        :title="$t(step.title)"></a-step>
    </a-steps>
    <static-inputs-form
      v-if="currentStep === 0"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      :fields="clusterFields"
      :prefillContent="prefillContent"
      :description="steps[currentStep].description"
      :isFixError="isFixError"
    />

    <div v-if="hypervisor !== 'VMware'">
      <static-inputs-form
        v-if="currentStep === 1"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="hostFields"
        :prefillContent="prefillContent"
        :description="steps[currentStep].description"
        :isFixError="isFixError"
      />
      <static-inputs-form
        v-if="currentStep === 2"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="primaryStorageFields"
        :prefillContent="prefillContent"
        :description="steps[currentStep].description"
        :isFixError="isFixError"
      />
      <static-inputs-form
        v-if="currentStep === 3"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="secondaryStorageFields"
        :prefillContent="prefillContent"
        :description="steps[currentStep].description"
        :isFixError="isFixError"
      />
    </div>
    <div v-else>
      <static-inputs-form
        v-if="currentStep === 1"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="primaryStorageFields"
        :prefillContent="prefillContent"
        :description="steps[currentStep].description"
        :isFixError="isFixError"
      />
      <static-inputs-form
        v-if="currentStep === 2"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="secondaryStorageFields"
        :prefillContent="prefillContent"
        :description="steps[currentStep].description"
        :isFixError="isFixError"
      />
    </div>
  </div>
</template>
<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import StaticInputsForm from '@views/infra/zone/StaticInputsForm'

export default {
  components: {
    StaticInputsForm
  },
  mixins: [mixinDevice],
  props: {
    prefillContent: {
      type: Object,
      default: function () {
        return {}
      }
    },
    stepChild: {
      type: String,
      default: ''
    },
    isFixError: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    zoneType () {
      return this.prefillContent.zoneType ? this.prefillContent.zoneType.value : null
    },
    hypervisor () {
      return this.prefillContent.hypervisor ? this.prefillContent.hypervisor.value : null
    },
    steps () {
      const steps = []
      const hypervisor = this.prefillContent.hypervisor ? this.prefillContent.hypervisor.value : null
      const localStorageEnabled = this.prefillContent.localstorageenabled.value
      const localStorageEnabledForSystemVM = this.prefillContent.localstorageenabledforsystemvm.value
      steps.push({
        title: 'label.cluster',
        fromKey: 'clusterResource',
        description: 'message.desc.cluster'
      })
      if (hypervisor !== 'VMware') {
        steps.push({
          title: 'label.host',
          fromKey: 'hostResource',
          description: 'message.desc.host'
        })
      }
      if (!localStorageEnabled || !localStorageEnabledForSystemVM) {
        steps.push({
          title: 'label.primary.storage',
          fromKey: 'primaryResource',
          description: 'message.desc.primary.storage'
        })
      }
      steps.push({
        title: 'label.secondary.storage',
        fromKey: 'secondaryResource',
        description: 'message.desc.secondary.storage'
      })

      return steps
    },
    clusterFields () {
      return [
        {
          title: 'label.cluster.name',
          key: 'clusterName',
          placeHolder: 'message.error.cluster.name',
          required: true
        },
        {
          title: 'label.vcenter.host',
          key: 'vCenterHost',
          placeHolder: 'message.error.vcenter.host',
          required: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.username',
          key: 'vCenterUsername',
          placeHolder: 'message.error.vcenter.username',
          required: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.password',
          key: 'vCenterPassword',
          placeHolder: 'message.error.vcenter.password',
          required: true,
          password: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.datacenter',
          key: 'vCenterDatacenter',
          placeHolder: 'message.error.vcenter.datacenter',
          required: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.override.public.traffic',
          key: 'overridepublictraffic',
          required: false,
          switch: true,
          display: {
            dvSwitchEnabled: true
          }
        },
        {
          title: 'label.override.guest.traffic',
          key: 'overrideguesttraffic',
          required: false,
          switch: true,
          display: {
            dvSwitchEnabled: true
          }
        },
        {
          title: 'label.cisco.nexus1000v.ip.address',
          key: 'vsmipaddress',
          placeHolder: 'message.error.nexus1000v.ipaddess',
          required: false,
          display: {
            vSwitchEnabled: true
          }
        },
        {
          title: 'label.cisco.nexus1000v.username',
          key: 'vsmusername',
          placeHolder: 'message.error.nexus1000v.username',
          required: false,
          display: {
            vSwitchEnabled: true
          }
        },
        {
          title: 'label.cisco.nexus1000v.password',
          key: 'vsmpassword',
          placeHolder: 'message.error.nexus1000v.password',
          required: false,
          display: {
            vSwitchEnabled: true
          }
        }
      ]
    },
    hostFields () {
      return [
        {
          title: 'label.host.name',
          key: 'hostName',
          placeHolder: 'message.error.host.name',
          required: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.username',
          key: 'hostUserName',
          placeHolder: 'message.error.host.username',
          required: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.password',
          key: 'hostPassword',
          placeHolder: 'message.error.host.password',
          required: true,
          password: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.agent.username',
          key: 'agentUserName',
          placeHolder: 'message.error.agent.username',
          required: false,
          defaultValue: 'Oracle',
          display: {
            hypervisor: 'Ovm'
          }
        },
        {
          title: 'label.agent.password',
          key: 'agentPassword',
          placeHolder: 'message.error.agent.password',
          required: true,
          password: true,
          display: {
            hypervisor: 'Ovm'
          }
        },
        {
          title: 'label.tags',
          key: 'hostTags',
          placeHolder: 'message.error.host.tags',
          required: false
        }
      ]
    },
    primaryStorageFields () {
      return [
        {
          title: 'label.name',
          key: 'primaryStorageName',
          placeHolder: 'message.error.name',
          required: true
        },
        {
          title: 'label.scope',
          key: 'primaryStorageScope',
          required: false,
          select: true,
          options: this.primaryStorageScopes
        },
        {
          title: 'label.protocol',
          key: 'primaryStorageProtocol',
          placeHolder: 'message.error.select',
          required: true,
          select: true,
          options: this.primaryStorageProtocols
        },
        {
          title: 'label.server',
          key: 'primaryStorageServer',
          placeHolder: 'message.error.server',
          required: true,
          display: {
            primaryStorageProtocol: ['nfs', 'iscsi', 'gluster', 'SMB']
          }
        },
        {
          title: 'label.path',
          key: 'primaryStoragePath',
          placeHolder: 'message.error.path',
          required: true,
          display: {
            primaryStorageProtocol: ['nfs', 'SMB', 'SharedMountPoint', 'ocfs2']
          }
        },
        {
          title: 'label.sr.name',
          key: 'primaryStorageSRLabel',
          placeHolder: 'message.error.sr.namelabel',
          required: true,
          display: {
            primaryStorageProtocol: 'PreSetup'
          }
        },
        {
          title: 'label.target.iqn',
          key: 'primaryStorageTargetIQN',
          placeHolder: 'message.error.target.iqn',
          required: true,
          display: {
            primaryStorageProtocol: 'iscsi'
          }
        },
        {
          title: 'label.LUN.number',
          key: 'primaryStorageLUN',
          placeHolder: 'message.error.lun',
          required: true,
          display: {
            primaryStorageProtocol: 'iscsi'
          }
        },
        {
          title: 'label.smb.domain',
          key: 'primaryStorageSMBDomain',
          placeHolder: 'message.error.sbdomain',
          required: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.smb.username',
          key: 'primaryStorageSMBUsername',
          placeHolder: 'message.error.sbdomain.username',
          required: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.smb.password',
          key: 'primaryStorageSMBPassword',
          placeHolder: 'message.error.sbdomain.password',
          required: true,
          password: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.rados.monitor',
          key: 'primaryStorageRADOSMonitor',
          placeHolder: 'message.error.rados.monitor',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.pool',
          key: 'primaryStorageRADOSPool',
          placeHolder: 'message.error.rados.pool',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.user',
          key: 'primaryStorageRADOSUser',
          placeHolder: 'message.error.rados.user',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.secret',
          key: 'primaryStorageRADOSSecret',
          placeHolder: 'message.error.rados.secret',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.volgroup',
          key: 'primaryStorageVolumeGroup',
          placeHolder: 'message.error.volumne.group',
          required: true,
          display: {
            primaryStorageProtocol: 'clvm'
          }
        },
        {
          title: 'label.volume',
          key: 'primaryStorageVolume',
          placeHolder: 'message.error.volumne',
          required: true,
          display: {
            primaryStorageProtocol: 'gluster'
          }
        },
        {
          title: 'label.vcenter.datacenter',
          key: 'primaryStorageVmfsDatacenter',
          placeHolder: 'message.error.vcenter.datacenter',
          required: true,
          display: {
            primaryStorageProtocol: 'vmfs'
          }
        },
        {
          title: 'label.vcenter.datastore',
          key: 'primaryStorageVmfsDatastore',
          placeHolder: 'message.error.vcenter.datastore',
          required: true,
          display: {
            primaryStorageProtocol: 'vmfs'
          }
        },
        {
          title: 'label.storage.tags',
          key: 'primaryStorageTags',
          placeHolder: 'message.error.storage.tags',
          required: false
        }
      ]
    },
    secondaryStorageFields () {
      return [
        {
          title: 'label.provider',
          key: 'secondaryStorageProvider',
          required: false,
          select: true,
          options: this.storageProviders
        },
        {
          title: 'label.name',
          key: 'secondaryStorageName',
          required: false,
          display: {
            secondaryStorageProvider: ['NFS', 'SMB', 'S3', 'Swift']
          }
        },
        {
          title: 'label.server',
          key: 'secondaryStorageServer',
          required: true,
          placeHolder: 'message.error.server',
          display: {
            secondaryStorageProvider: ['NFS', 'SMB']
          }
        },
        {
          title: 'label.path',
          key: 'secondaryStoragePath',
          required: true,
          placeHolder: 'message.error.path',
          display: {
            secondaryStorageProvider: ['NFS', 'SMB']
          }
        },
        {
          title: 'label.smb.domain',
          key: 'secondaryStorageSMBDomain',
          required: true,
          placeHolder: 'message.error.sbdomain',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.smb.username',
          key: 'secondaryStorageSMBUsername',
          required: true,
          placeHolder: 'message.error.smb.username',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.smb.password',
          key: 'secondaryStorageSMBPassword',
          required: true,
          password: true,
          placeHolder: 'message.error.smb.password',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.s3.access_key',
          key: 'secondaryStorageAccessKey',
          required: true,
          placeHolder: 'message.error.access.key',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.secret_key',
          key: 'secondaryStorageSecretKey',
          required: true,
          placeHolder: 'message.error.secret.key',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.bucket',
          key: 'secondaryStorageBucket',
          required: true,
          placeHolder: 'message.error.bucket',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.endpoint',
          key: 'secondaryStorageEndpoint',
          required: false,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.use_https',
          key: 'secondaryStorageHttps',
          required: false,
          switch: true,
          checked: true,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.connection_timeoutt',
          key: 'secondaryStorageConnectionTimeout',
          required: false,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.max_error_retry',
          key: 'secondaryStorageMaxError',
          required: false,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.socket_timeout',
          key: 'secondaryStorageSocketTimeout',
          required: false,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.create.nfs.secondary.staging.storage',
          key: 'secondaryStorageNFSStaging',
          required: false,
          switch: true,
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.nfs.server',
          key: 'secondaryStorageNFSServer',
          required: true,
          placeHolder: 'message.error.s3nfs.server',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.nfs.path',
          key: 'secondaryStorageNFSPath',
          required: true,
          placeHolder: 'message.error.s3nfs.path',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.url',
          key: 'secondaryStorageURL',
          required: true,
          placeHolder: 'message.error.url',
          display: {
            secondaryStorageProvider: ['Swift']
          }
        },
        {
          title: 'label.account',
          key: 'secondaryStorageAccount',
          required: false,
          display: {
            secondaryStorageProvider: ['Swift']
          }
        },
        {
          title: 'label.username',
          key: 'secondaryStorageUsername',
          required: false,
          display: {
            secondaryStorageProvider: ['Swift']
          }
        },
        {
          title: 'label.key',
          key: 'secondaryStorageKey',
          required: false,
          display: {
            secondaryStorageProvider: ['Swift']
          }
        }
      ]
    }
  },
  data () {
    return {
      physicalNetworks: null,
      currentHypervisor: null,
      primaryStorageScopes: [],
      primaryStorageProtocols: [],
      storageProviders: [],
      currentStep: null,
      options: ['primaryStorageScope', 'primaryStorageProtocol', 'provider']
    }
  },
  created () {
    this.currentStep = this.prefillContent.resourceStep ? this.prefillContent.resourceStep : 0
    if (this.stepChild && this.stepChild !== '') {
      this.currentStep = this.steps.findIndex(item => item.fromKey === this.stepChild)
    }
    this.scrollToStepActive()
    if (this.prefillContent.hypervisor.value === 'BareMetal') {
      this.$emit('nextPressed')
    } else {
      this.fetchConfigurationSwitch()
      this.options.forEach(this.fetchOptions)
      if (!this.prefillContent.lastHypervisor) {
        this.$emit('fieldsChanged', {
          lastHypervisor: this.prefillContent.hypervisor
        })
      } else if (this.prefillContent.lastHypervisor.value !== this.prefillContent.hypervisor.value) {
        this.$emit('fieldsChanged', {
          lastHypervisor: this.prefillContent.hypervisor,
          primaryStorageProtocol: null,
          primaryStorageScope: null
        })
      }
    }
  },
  methods: {
    nextPressed () {
      if (this.currentStep === this.steps.length - 1) {
        this.$emit('nextPressed')
      } else {
        this.currentStep++
        this.$emit('fieldsChanged', { resourceStep: this.currentStep })
      }

      this.scrollToStepActive()
    },
    handleBack (e) {
      if (this.currentStep === 0) {
        this.$emit('backPressed')
      } else {
        this.currentStep--
        this.$emit('fieldsChanged', { resourceStep: this.currentStep })
      }

      this.scrollToStepActive()
    },
    scrollToStepActive () {
      if (!this.isMobile()) {
        return
      }
      this.$nextTick(() => {
        if (!this.$refs.resourceStep) {
          return
        }
        if (this.currentStep === 0) {
          this.$refs.resourceStep.$el.scrollLeft = 0
          return
        }
        this.$refs.resourceStep.$el.scrollLeft = this.$refs['resourceStep' + (this.currentStep - 1)][0].$el.offsetLeft
      })
    },
    fieldsChanged (changed) {
      this.$emit('fieldsChanged', changed)
    },
    fetchOptions (name) {
      switch (name) {
        case 'primaryStorageScope':
          this.fetchScope()
          break
        case 'primaryStorageProtocol':
          this.fetchProtocol()
          break
        case 'provider':
          this.fetchProvider()
          break
        default:
          break
      }
    },
    fetchScope () {
      const hypervisor = this.prefillContent.hypervisor ? this.prefillContent.hypervisor.value : null
      const scope = []
      if (['KVM', 'VMware', 'Hyperv'].includes(hypervisor)) {
        scope.push({
          id: 'zone',
          description: this.$t('label.zone')
        })
        scope.push({
          id: 'cluster',
          description: this.$t('label.cluster')
        })
      } else {
        scope.push({
          id: 'cluster',
          description: this.$t('label.cluster')
        })
      }
      this.primaryStorageScopes = scope
      this.$forceUpdate()
    },
    fetchProtocol () {
      const hypervisor = this.prefillContent.hypervisor ? this.prefillContent.hypervisor.value : null
      const protocols = []
      if (hypervisor === 'KVM') {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
        protocols.push({
          id: 'SharedMountPoint',
          description: 'SharedMountPoint'
        })
        protocols.push({
          id: 'rbd',
          description: 'RBD'
        })
        protocols.push({
          id: 'clvm',
          description: 'CLVM'
        })
        protocols.push({
          id: 'gluster',
          description: 'Gluster'
        })
      } else if (hypervisor === 'XenServer') {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
        protocols.push({
          id: 'PreSetup',
          description: 'PreSetup'
        })
        protocols.push({
          id: 'iscsi',
          description: 'iscsi'
        })
      } else if (hypervisor === 'VMware') {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
        protocols.push({
          id: 'vmfs',
          description: 'vmfs'
        })
      } else if (hypervisor === 'Hyperv') {
        protocols.push({
          id: 'SMB',
          description: 'SMB/CIFS'
        })
      } else if (hypervisor === 'Ovm') {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
        protocols.push({
          id: 'ocfs2',
          description: 'ocfs2'
        })
      } else if (hypervisor === 'LXC') {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
        protocols.push({
          id: 'SharedMountPoint',
          description: 'SharedMountPoint'
        })
        protocols.push({
          id: 'rbd',
          description: 'RBD'
        })
      } else {
        protocols.push({
          id: 'nfs',
          description: 'nfs'
        })
      }

      this.primaryStorageProtocols = protocols
      this.$forceUpdate()
    },
    async fetchConfigurationSwitch () {
      const hypervisor = this.prefillContent.hypervisor ? this.prefillContent.hypervisor.value : null
      this.$emit('fieldsChanged', { dvSwitchEnabled: { value: false } })
      this.$emit('fieldsChanged', { vSwitchEnabled: { value: false } })
      if (hypervisor && hypervisor === 'VMware') {
        await this.fetchNexusSwitchConfig()
        await this.fetchDvSwitchConfig()
      }
    },
    fetchNexusSwitchConfig () {
      api('listConfigurations', { name: 'vmware.use.nexus.vswitch' }).then(json => {
        let vSwitchEnabled = false
        if (json.listconfigurationsresponse.configuration[0].value) {
          vSwitchEnabled = true
        }
        this.$emit('fieldsChanged', { vSwitchEnabled: { value: vSwitchEnabled } })
      })
    },
    fetchDvSwitchConfig () {
      let dvSwitchEnabled = false
      api('listConfigurations', { name: 'vmware.use.dvswitch' }).then(json => {
        if (json.listconfigurationsresponse.configuration[0].value) {
          dvSwitchEnabled = true
        }
        this.$emit('fieldsChanged', { dvSwitchEnabled: { value: dvSwitchEnabled } })
      })
    },
    fetchProvider () {
      const storageProviders = []
      api('listImageStores', { provider: 'S3' }).then(json => {
        const s3stores = json.listimagestoresresponse.imagestore
        if (s3stores != null && s3stores.length > 0) {
          storageProviders.push({ id: 'S3', description: 'S3' })
        } else {
          storageProviders.push({ id: 'NFS', description: 'NFS' })
          storageProviders.push({ id: 'SMB', description: 'SMB/CIFS' })
          storageProviders.push({ id: 'S3', description: 'S3' })
          storageProviders.push({ id: 'Swift', description: 'Swift' })
        }
        this.storageProviders = storageProviders
        this.$forceUpdate()
      })
    },
    submitLaunchZone () {
      this.$emit('submitLaunchZone')
    }
  }
}

</script>
