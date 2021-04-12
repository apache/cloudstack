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
      ref="zoneNetStep"
      progressDot
      :current="currentStep"
      size="small"
      style="margin-left: 0px; margin-top: 16px;">
      <a-step
        v-for="(step, index) in steps"
        :key="step.title"
        :title="$t(step.title)"
        :style="stepScales"
        :ref="`netStep${index}`"></a-step>
    </a-steps>
    <zone-wizard-physical-network-setup-step
      v-if="steps && steps[currentStep].formKey === 'physicalNetwork'"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      :prefillContent="prefillContent"
      :isFixError="isFixError"
    />
    <static-inputs-form
      v-if="steps && steps[currentStep].formKey === 'netscaler'"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      :fields="netscalerFields"
      :prefillContent="prefillContent"
      :description="netscalerSetupDescription"
      :isFixError="isFixError"
    />
    <ip-address-range-form
      v-if="steps && steps[currentStep].formKey === 'publicTraffic'"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      traffic="public"
      :description="publicTrafficDescription[zoneType.toLowerCase()]"
      :prefillContent="prefillContent"
      :isFixError="isFixError"
    />

    <static-inputs-form
      v-if="steps && steps[currentStep].formKey === 'pod'"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      :fields="podFields"
      :prefillContent="prefillContent"
      :description="podSetupDescription"
      :isFixError="isFixError"
    />

    <div v-if="guestTrafficRangeMode">
      <static-inputs-form
        v-if="steps && steps[currentStep].formKey === 'guestTraffic'"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :fields="guestTrafficFields"
        :prefillContent="prefillContent"
        :description="guestTrafficDescription[this.zoneType.toLowerCase()]"
        :isFixError="isFixError"
      />
    </div>
    <div v-else>
      <advanced-guest-traffic-form
        v-if="steps && steps[currentStep].formKey === 'guestTraffic'"
        @nextPressed="nextPressed"
        @backPressed="handleBack"
        @fieldsChanged="fieldsChanged"
        @submitLaunchZone="submitLaunchZone"
        :prefillContent="prefillContent"
        :description="guestTrafficDescription[this.zoneType.toLowerCase()]"
        :isFixError="isFixError"
      />
    </div>

    <ip-address-range-form
      v-if="steps && steps[currentStep].formKey === 'storageTraffic'"
      @nextPressed="nextPressed"
      @backPressed="handleBack"
      @fieldsChanged="fieldsChanged"
      @submitLaunchZone="submitLaunchZone"
      traffic="storage"
      :description="storageTrafficDescription"
      :prefillContent="prefillContent"
      :isFixError="isFixError"
    />
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import ZoneWizardPhysicalNetworkSetupStep from '@views/infra/zone/ZoneWizardPhysicalNetworkSetupStep'
import IpAddressRangeForm from '@views/infra/zone/IpAddressRangeForm'
import StaticInputsForm from '@views/infra/zone/StaticInputsForm'
import AdvancedGuestTrafficForm from '@views/infra/zone/AdvancedGuestTrafficForm'

export default {
  components: {
    ZoneWizardPhysicalNetworkSetupStep,
    IpAddressRangeForm,
    StaticInputsForm,
    AdvancedGuestTrafficForm
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
    sgEnabled () {
      return this.prefillContent.securityGroupsEnabled ? this.prefillContent.securityGroupsEnabled.value : false
    },
    havingNetscaler () {
      return this.prefillContent.networkOfferingSelected ? this.prefillContent.networkOfferingSelected.havingNetscaler : false
    },
    guestTrafficRangeMode () {
      return this.zoneType === 'Basic' ||
        (this.zoneType === 'Advanced' && this.sgEnabled)
    },
    allSteps () {
      const steps = []
      steps.push({
        title: 'label.physical.network',
        formKey: 'physicalNetwork'
      })
      if (this.havingNetscaler) {
        steps.push({
          title: 'label.netScaler',
          formKey: 'netscaler'
        })
      }
      steps.push({
        title: 'label.public.traffic',
        formKey: 'publicTraffic',
        trafficType: 'public'
      })
      steps.push({
        title: 'label.pod',
        formKey: 'pod'
      })
      steps.push({
        title: 'label.guest.traffic',
        formKey: 'guestTraffic',
        trafficType: 'guest'
      })
      steps.push({
        title: 'label.storage.traffic',
        formKey: 'storageTraffic',
        trafficType: 'storage'
      })

      return steps
    },
    stepScales () {
      if (!this.isMobile() && this.steps.length > 4) {
        return { width: 'calc(100% / ' + this.steps.length + ')' }
      }
      return {}
    },
    netscalerFields () {
      return [
        {
          title: 'label.guest.ip',
          key: 'netscalerIp',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        },
        {
          title: 'label.username',
          key: 'netscalerUsername',
          required: false
        },
        {
          title: 'label.password',
          key: 'netscalerPassword',
          required: false,
          password: true
        },
        {
          title: 'label.type',
          key: 'netscalerType',
          required: false,
          select: true,
          options: this.netscalerType
        },
        {
          title: 'label.public.interface',
          key: 'publicinterface',
          required: false
        },
        {
          title: 'label.private.interface',
          key: 'privateinterface',
          required: false
        },
        {
          title: 'label.gslb.service',
          key: 'gslbprovider',
          required: false,
          switch: true
        },
        {
          title: 'label.gslb.service.public.ip',
          key: 'gslbproviderpublicip',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        },
        {
          title: 'label.gslb.service.private.ip',
          key: 'gslbproviderprivateip',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        },
        {
          title: 'label.numretries',
          key: 'numretries',
          required: false
        },
        {
          title: 'label.capacity',
          key: 'capacity',
          required: false
        }
      ]
    },
    guestTrafficFields () {
      const fields = [
        {
          title: 'label.guest.gateway',
          key: 'guestGateway',
          placeHolder: 'message.installwizard.tooltip.configureguesttraffic.guestgateway',
          required: false
        },
        {
          title: 'label.guest.netmask',
          key: 'guestNetmask',
          placeHolder: 'message.installwizard.tooltip.configureguesttraffic.guestnetmask',
          required: false
        },
        {
          title: 'label.guest.start.ip',
          key: 'guestStartIp',
          placeHolder: 'message.installwizard.tooltip.configureguesttraffic.gueststartip',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        },
        {
          title: 'label.guest.end.ip',
          key: 'guestStopIp',
          placeHolder: 'message.installwizard.tooltip.configureguesttraffic.guestendip',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        }
      ]

      if (this.sgEnabled) {
        fields.push({
          title: 'label.vlanid',
          key: 'guestVlan',
          required: false,
          ipV4: false
        })
      }

      return fields
    }
  },
  data () {
    return {
      physicalNetworks: null,
      currentStep: 0,
      steps: null,
      skipGuestTrafficStep: false,
      netscalerType: [],
      publicTrafficDescription: {
        advanced: 'message.public.traffic.in.advanced.zone',
        basic: 'message.public.traffic.in.basic.zone'
      },
      guestTrafficDescription: {
        advanced: 'message.guest.traffic.in.advanced.zone',
        basic: 'message.guest.traffic.in.basic.zone'
      },
      podSetupDescription: 'message.add.pod.during.zone.creation',
      netscalerSetupDescription: 'label.please.specify.netscaler.info',
      storageTrafficDescription: 'label.zonewizard.traffictype.storage',
      podFields: [
        {
          title: 'label.pod.name',
          key: 'podName',
          placeHolder: 'message.installwizard.tooltip.addpod.name',
          required: true
        },
        {
          title: 'label.reserved.system.gateway',
          key: 'podReservedGateway',
          placeHolder: 'message.installwizard.tooltip.addpod.reservedsystemgateway',
          required: true
        },
        {
          title: 'label.reserved.system.netmask',
          key: 'podReservedNetmask',
          placeHolder: 'message.tooltip.reserved.system.netmask',
          required: true
        },
        {
          title: 'label.start.reserved.system.ip',
          key: 'podReservedStartIp',
          placeHolder: 'message.installwizard.tooltip.addpod.reservedsystemstartip',
          required: true,
          ipV4: true,
          message: 'message.error.ipv4.address'
        },
        {
          title: 'label.end.reserved.system.ip',
          key: 'podReservedStopIp',
          placeHolder: 'message.installwizard.tooltip.addpod.reservedsystemendip',
          required: false,
          ipV4: true,
          message: 'message.error.ipv4.address'
        }
      ]
    }
  },
  created () {
    this.physicalNetworks = this.prefillContent.physicalNetworks
    this.steps = this.filteredSteps()
    this.currentStep = this.prefillContent.networkStep ? this.prefillContent.networkStep : 0
    if (this.stepChild && this.stepChild !== '') {
      this.currentStep = this.steps.findIndex(item => item.formKey === this.stepChild)
    }
    this.scrollToStepActive()
    if (this.zoneType === 'Basic' ||
      (this.zoneType === 'Advanced' && this.sgEnabled)) {
      this.skipGuestTrafficStep = false
    } else {
      this.fetchConfiguration()
    }
    this.$emit('fieldsChanged', { skipGuestTrafficStep: this.skipGuestTrafficStep })
    this.fetchNetscalerType()
  },
  methods: {
    fetchNetscalerType () {
      const items = []
      items.push({
        id: 'NetscalerMPXLoadBalancer',
        description: 'NetScaler MPX LoadBalancer'
      })
      items.push({
        id: 'NetscalerVPXLoadBalancer',
        description: 'NetScaler VPX LoadBalancer'
      })
      items.push({
        id: 'NetscalerSDXLoadBalancer',
        description: 'NetScaler SDX LoadBalancer'
      })
      this.netscalerType = items
      this.$forceUpdate()
    },
    nextPressed () {
      if (this.currentStep === this.steps.length - 1) {
        this.$emit('nextPressed')
      } else {
        this.currentStep++
        this.$emit('fieldsChanged', { networkStep: this.currentStep })
        this.scrollToStepActive()
      }
    },
    handleBack (e) {
      if (this.currentStep === 0) {
        this.$emit('backPressed')
      } else {
        this.currentStep--
        this.$emit('fieldsChanged', { networkStep: this.currentStep })
        this.scrollToStepActive()
      }
    },
    scrollToStepActive () {
      if (!this.isMobile()) {
        return
      }
      this.$nextTick(() => {
        if (!this.$refs.zoneNetStep) {
          return
        }
        if (this.currentStep === 0) {
          this.$refs.zoneNetStep.$el.scrollLeft = 0
          return
        }
        this.$refs.zoneNetStep.$el.scrollLeft = this.$refs['netStep' + (this.currentStep - 1)][0].$el.offsetLeft
      })
    },
    submitLaunchZone () {
      this.$emit('submitLaunchZone')
    },
    fieldsChanged (changed) {
      if (changed.physicalNetworks) {
        this.physicalNetworks = changed.physicalNetworks
        this.steps = this.filteredSteps()
      }
      this.$emit('fieldsChanged', changed)
    },
    filteredSteps () {
      return this.allSteps.filter(step => {
        if (!step.trafficType) return true
        if (this.physicalNetworks) {
          let neededTraffic = false
          this.physicalNetworks.forEach(net => {
            net.traffics.forEach(traffic => {
              if (traffic.type === step.trafficType) {
                neededTraffic = true
              }
            })
          })
          if (neededTraffic) return true
        }
        return false
      })
    },
    fetchConfiguration () {
      this.skipGuestTrafficStep = false
      api('listConfigurations', { name: 'sdn.ovs.controller' }).then(json => {
        const items = json.listconfigurationsresponse.configuration
        items.forEach(item => {
          if (item.name === 'sdn.ovs.controller') {
            if (item.value) {
              this.skipGuestTrafficStep = true
            }
            return false
          }
        })
      })
    }
  }
}
</script>
