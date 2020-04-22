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
    <a-steps progressDot :current="currentStep" size="small" style="margin-left: 0; margin-top: 16px;">
      <a-step
        v-for="step in steps"
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
import StaticInputsForm from '@views/infra/zone/StaticInputsForm'
import { api } from '@/api'

export default {
  components: {
    StaticInputsForm
  },
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
      steps.push({
        title: 'label.primary.storage',
        fromKey: 'primaryResource',
        description: 'message.desc.primary.storage'
      })
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
          placeHolder: 'Please enter cluster name',
          required: true
        },
        {
          title: 'label.vcenter.host',
          key: 'vCenterHost',
          placeHolder: 'Please enter vCenter Host',
          required: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.username',
          key: 'vCenterUsername',
          placeHolder: 'Please enter vCenter Username',
          required: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.password',
          key: 'vCenterPassword',
          placeHolder: 'Please enter vCenter Password',
          required: true,
          password: true,
          display: {
            hypervisor: ['VMware', 'Ovm3']
          }
        },
        {
          title: 'label.vcenter.datacenter',
          key: 'vCenterDatacenter',
          placeHolder: 'Please enter vCenter Datacenter',
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
          placeHolder: 'Please enter Nexus 1000v IP Address',
          required: false,
          display: {
            vSwitchEnabled: true
          }
        },
        {
          title: 'label.cisco.nexus1000v.username',
          key: 'vsmusername',
          placeHolder: 'Please enter Nexus 1000v Username',
          required: false,
          display: {
            vSwitchEnabled: true
          }
        },
        {
          title: 'label.cisco.nexus1000v.password',
          key: 'vsmpassword',
          placeHolder: 'Please enter Nexus 1000v Password',
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
          placeHolder: 'Please enter host name',
          required: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.username',
          key: 'hostUserName',
          placeHolder: 'Please enter host username',
          required: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.password',
          key: 'hostPassword',
          placeHolder: 'Please enter host password',
          required: true,
          password: true,
          display: {
            hypervisor: ['VMware', 'BareMetal', 'Ovm', 'Hyperv', 'KVM', 'XenServer', 'LXC', 'Simulator']
          }
        },
        {
          title: 'label.agent.username',
          key: 'agentUserName',
          placeHolder: 'Please enter Agent username',
          required: false,
          defaultValue: 'Oracle',
          display: {
            hypervisor: 'Ovm'
          }
        },
        {
          title: 'label.agent.password',
          key: 'agentPassword',
          placeHolder: 'Please enter Agent password',
          required: true,
          password: true,
          display: {
            hypervisor: 'Ovm'
          }
        },
        {
          title: 'label.tags',
          key: 'hostTags',
          placeHolder: 'Please enter host tags',
          required: false
        }
      ]
    },
    primaryStorageFields () {
      return [
        {
          title: 'label.name',
          key: 'primaryStorageName',
          placeHolder: 'Please enter name',
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
          placeHolder: 'Please select option',
          required: true,
          select: true,
          options: this.primaryStorageProtocols
        },
        {
          title: 'label.server',
          key: 'primaryStorageServer',
          placeHolder: 'Please enter server',
          required: true,
          display: {
            primaryStorageProtocol: ['nfs', 'iscsi', 'gluster', 'SMB']
          }
        },
        {
          title: 'label.path',
          key: 'primaryStoragePath',
          placeHolder: 'Please enter path',
          required: true,
          display: {
            primaryStorageProtocol: ['nfs', 'SMB', 'SharedMountPoint', 'ocfs2']
          }
        },
        {
          title: 'label.SR.name',
          key: 'primaryStorageSRLabel',
          placeHolder: 'Please enter SR Name-Label',
          required: true,
          display: {
            primaryStorageProtocol: 'PreSetup'
          }
        },
        {
          title: 'label.target.iqn',
          key: 'primaryStorageTargetIQN',
          placeHolder: 'Please enter Target IQN',
          required: true,
          display: {
            primaryStorageProtocol: 'iscsi'
          }
        },
        {
          title: 'label.LUN.number',
          key: 'primaryStorageLUN',
          placeHolder: 'Please enter LUN #',
          required: true,
          display: {
            primaryStorageProtocol: 'iscsi'
          }
        },
        {
          title: 'label.smb.domain',
          key: 'primaryStorageSMBDomain',
          placeHolder: 'Please enter SMB Domain',
          required: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.smb.username',
          key: 'primaryStorageSMBUsername',
          placeHolder: 'Please enter SMB Username',
          required: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.smb.password',
          key: 'primaryStorageSMBPassword',
          placeHolder: 'Please enter SMB Password',
          required: true,
          password: true,
          display: {
            primaryStorageProtocol: 'SMB'
          }
        },
        {
          title: 'label.rados.monitor',
          key: 'primaryStorageRADOSMonitor',
          placeHolder: 'Please enter RADOS Monitor',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.pool',
          key: 'primaryStorageRADOSPool',
          placeHolder: 'Please enter RADOS Pool',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.user',
          key: 'primaryStorageRADOSUser',
          placeHolder: 'Please enter RADOS User',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.rados.secret',
          key: 'primaryStorageRADOSSecret',
          placeHolder: 'Please enter RADOS Secret',
          required: false,
          display: {
            primaryStorageProtocol: ['rbd']
          }
        },
        {
          title: 'label.volgroup',
          key: 'primaryStorageVolumeGroup',
          placeHolder: 'Please enter Volume Group',
          required: true,
          display: {
            primaryStorageProtocol: 'clvm'
          }
        },
        {
          title: 'label.volume',
          key: 'primaryStorageVolume',
          placeHolder: 'Please enter Volume',
          required: true,
          display: {
            primaryStorageProtocol: 'gluster'
          }
        },
        {
          title: 'label.vcenter.datacenter',
          key: 'primaryStorageVmfsDatacenter',
          placeHolder: 'Please enter vCenter Datacenter',
          required: true,
          display: {
            primaryStorageProtocol: 'vmfs'
          }
        },
        {
          title: 'label.vcenter.datastore',
          key: 'primaryStorageVmfsDatastore',
          placeHolder: 'Please enter vCenter Datastore',
          required: true,
          display: {
            primaryStorageProtocol: 'vmfs'
          }
        },
        {
          title: 'label.storage.tags',
          key: 'primaryStorageTags',
          placeHolder: 'Please enter storage tags',
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
          placeHolder: 'Please enter Server',
          display: {
            secondaryStorageProvider: ['NFS', 'SMB']
          }
        },
        {
          title: 'label.path',
          key: 'secondaryStoragePath',
          required: true,
          placeHolder: 'Please enter Path',
          display: {
            secondaryStorageProvider: ['NFS', 'SMB']
          }
        },
        {
          title: 'label.smb.domain',
          key: 'secondaryStorageSMBDomain',
          required: true,
          placeHolder: 'Please enter SMB Domain',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.smb.username',
          key: 'secondaryStorageSMBUsername',
          required: true,
          placeHolder: 'Please enter SMB Username',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.smb.password',
          key: 'secondaryStorageSMBPassword',
          required: true,
          password: true,
          placeHolder: 'Please enter SMB Password',
          display: {
            secondaryStorageProvider: ['SMB']
          }
        },
        {
          title: 'label.s3.access_key',
          key: 'secondaryStorageAccessKey',
          required: true,
          placeHolder: 'Please enter Access Key',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.secret_key',
          key: 'secondaryStorageSecretKey',
          required: true,
          placeHolder: 'Please enter Secret Key',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.bucket',
          key: 'secondaryStorageBucket',
          required: true,
          placeHolder: 'Please enter Bucket',
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
          placeHolder: 'Please enter S3 NFS Server',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.s3.nfs.path',
          key: 'secondaryStorageNFSPath',
          required: true,
          placeHolder: 'Please enter S3 NFS Path',
          display: {
            secondaryStorageProvider: ['S3']
          }
        },
        {
          title: 'label.url',
          key: 'secondaryStorageURL',
          required: true,
          placeHolder: 'Please enter URL',
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
      currentStep: 0,
      options: ['primaryStorageScope', 'primaryStorageProtocol', 'provider']
    }
  },
  mounted () {
    if (this.stepChild && this.stepChild !== '') {
      this.currentStep = this.steps.findIndex(item => item.fromKey === this.stepChild)
    }
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
      }
    },
    handleBack (e) {
      if (this.currentStep === 0) {
        this.$emit('backPressed')
      } else {
        this.currentStep--
      }
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
          description: this.$t('zone')
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
