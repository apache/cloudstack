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
  <div class="form">
    <a-steps
      labelPlacement="vertical"
      size="small"
      :current="currentStep">
      <a-step
        v-for="(item) in steps"
        :key="item.title"
        :title="$t(item.title)">
      </a-step>
    </a-steps>
    <div>
      <zone-wizard-zone-type-step
        v-if="currentStep === 0"
        @nextPressed="nextPressed"
        @fieldsChanged="onFieldsChanged"
        :prefillContent="zoneConfig"
      />
      <zone-wizard-zone-details-step
        v-else-if="currentStep === 1"
        @nextPressed="nextPressed"
        @backPressed="backPressed"
        @fieldsChanged="onFieldsChanged"
        @submitLaunchZone="onLaunchZone"
        :isFixError="stepFixError"
        :prefillContent="zoneConfig"
      />
      <zone-wizard-network-setup-step
        v-else-if="currentStep === 2"
        @nextPressed="nextPressed"
        @backPressed="backPressed"
        @fieldsChanged="onFieldsChanged"
        @submitLaunchZone="onLaunchZone"
        :stepChild="stepChild"
        :isFixError="stepFixError"
        :prefillContent="zoneConfig"
      />
      <zone-wizard-add-resources
        v-else-if="currentStep === 3"
        @nextPressed="nextPressed"
        @backPressed="backPressed"
        @fieldsChanged="onFieldsChanged"
        @submitLaunchZone="onLaunchZone"
        :stepChild="stepChild"
        :isFixError="stepFixError"
        :prefillContent="zoneConfig"
      />
      <zone-wizard-launch-zone
        v-else
        @backPressed="backPressed"
        @closeAction="onCloseAction"
        @refresh-data="onRefreshData"
        @stepError="onStepError"
        :launchZone="launchZone"
        :stepChild="stepChild"
        :launchData="launchData"
        :isFixError="stepFixError"
        :prefillContent="zoneConfig"
      />
    </div>
  </div>
</template>
<script>
import ZoneWizardZoneTypeStep from '@views/infra/zone/ZoneWizardZoneTypeStep'
import ZoneWizardZoneDetailsStep from '@views/infra/zone/ZoneWizardZoneDetailsStep'
import ZoneWizardNetworkSetupStep from '@views/infra/zone/ZoneWizardNetworkSetupStep'
import ZoneWizardAddResources from '@views/infra/zone/ZoneWizardAddResources'
import ZoneWizardLaunchZone from '@views/infra/zone/ZoneWizardLaunchZone'

export default {
  components: {
    ZoneWizardZoneTypeStep,
    ZoneWizardZoneDetailsStep,
    ZoneWizardNetworkSetupStep,
    ZoneWizardAddResources,
    ZoneWizardLaunchZone
  },
  data () {
    return {
      currentStep: 0,
      stepFixError: false,
      launchZone: false,
      launchData: {},
      stepChild: '',
      steps: [
        {
          title: 'label.zone.type',
          step: [],
          description: 'Select type of zone basic/advanced.',
          hint: 'This is the type of zone deployement that you want to use. Basic zone: provides a single network where each VM instance is assigned an IP directly from the network. Guest isolation can be provided through layer-3 means such as security groups (IP address source filtering). Advanced zone: For more sophisticated network topologies. This network model provides the most flexibility in defining guest networks and providing custom network offerings such as firewall, VPN, or load balancer support.'
        },
        {
          title: 'label.zone.details',
          step: ['stepAddZone', 'dedicateZone'],
          description: 'Populate zone details',
          hint: 'A zone is the largest organizational unit in CloudStack, and it typically corresponds to a single datacenter. Zones provide physical isolation and redundancy. A zone consists of one or more pods (each of which contains hosts and primary storage servers) and a secondary storage server which is shared by all pods in the zone.'
        },
        {
          title: 'label.network',
          step: ['physicalNetwork', 'netscaler', 'pod', 'guestTraffic', 'storageTraffic', 'publicTraffic'],
          description: 'Setup network and traffic',
          hint: 'Configure network components and public/guest/management traffic including IP addresses.'
        },
        {
          title: 'label.add.resources',
          step: ['clusterResource', 'hostResource', 'primaryResource', 'secondaryResource'],
          description: 'Add infrastructure resources',
          hint: 'Add infrastructure resources - pods, clusters, primary/secondary storages.'
        },
        {
          title: 'label.launch',
          step: ['launchZone'],
          description: 'Zone is ready to launch; please proceed to the next step.',
          hint: 'Configure network components and traffic including IP addresses.'
        }
      ],
      zoneConfig: {}
    }
  },
  methods: {
    nextPressed () {
      this.currentStep++
    },
    backPressed (data) {
      this.currentStep--
    },
    onFieldsChanged (data) {
      if (data.zoneType &&
        this.zoneConfig.zoneType &&
        data.zoneType.value !== this.zoneConfig.zoneType.value) {
        this.zoneConfig.physicalNetworks = null
      }

      this.zoneConfig = { ...this.zoneConfig, ...data }
    },
    onCloseAction () {
      this.$emit('close-action')
    },
    onRefreshData () {
      this.$message.success('Processing complete!')
      this.$emit('refresh-data')
      this.onCloseAction()
    },
    onStepError (step, launchData) {
      this.currentStep = this.steps.findIndex(item => item.step.includes(step))
      this.stepChild = step
      this.launchData = launchData
      this.launchZone = false
      this.stepFixError = true
    },
    onLaunchZone () {
      this.stepFixError = false
      this.launchZone = true
      this.currentStep = this.steps.findIndex(item => item.step.includes('launchZone'))
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    width: 95vw;
    @media (min-width: 1000px) {
      width: 800px;
    }

    /deep/.form-action {
      position: relative;
      margin-top: 16px;
      height: 35px;
    }

    /deep/.button-next {
      position: absolute;
      right: 0;
    }

    /deep/.button-next.ant-btn-loading:not(.ant-btn-circle):not(.ant-btn-circle-outline):not(.ant-btn-icon-only) {
      position: absolute;
      right: 0;
    }
  }

  /deep/.ant-form-text {
    width: 100%;
  }

  .steps-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
    background-color: #fafafa;
    min-height: 200px;
    text-align: center;
    vertical-align: center;
    padding: 8px;
    padding-top: 16px;
  }

  .steps-action {
    margin-top: 24px;
  }
</style>
