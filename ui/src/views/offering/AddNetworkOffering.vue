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
  <div class="form-layout" @keyup.ctrl.enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-focus="true"
            v-model:value="form.name"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item name="networkrate" ref="networkrate">
          <template #label>
            <tooltip-label :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
          </template>
          <a-input
            v-model:value="form.networkrate"
            :placeholder="apiParams.networkrate.description"/>
        </a-form-item>
        <a-form-item name="guestiptype" ref="guestiptype">
          <template #label>
            <tooltip-label :title="$t('label.guestiptype')" :tooltip="apiParams.guestiptype.description"/>
          </template>
          <a-radio-group
            v-model:value="form.guestiptype"
            buttonStyle="solid"
            @change="selected => { handleGuestTypeChange(selected.target.value) }">
            <a-radio-button value="isolated">
              {{ $t('label.isolated') }}
            </a-radio-button>
            <a-radio-button value="l2">
              {{ $t('label.l2') }}
            </a-radio-button>
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="internetprotocol" ref="internetprotocol" v-if="guestType === 'isolated'">
          <template #label>
            <tooltip-label :title="$t('label.internetprotocol')" :tooltip="apiParams.internetprotocol.description"/>
          </template>
          <span v-if="!ipv6NetworkOfferingEnabled || internetProtocolValue!=='ipv4'">
            <a-alert type="warning">
              <template #message>
                <span v-html="ipv6NetworkOfferingEnabled ? $t('message.offering.internet.protocol.warning') : $t('message.offering.ipv6.warning')" />
              </template>
            </a-alert>
            <br/>
          </span>
          <a-radio-group
            v-model:value="form.internetprotocol"
            :disabled="!ipv6NetworkOfferingEnabled"
            buttonStyle="solid"
            @change="e => { internetProtocolValue = e.target.value }" >
            <a-radio-button value="ipv4">
              {{ $t('label.ip.v4') }}
            </a-radio-button>
            <a-radio-button value="dualstack">
              {{ $t('label.ip.v4.v6') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item name="specifyvlan" ref="specifyvlan">
              <template #label>
                <tooltip-label :title="$t('label.specifyvlan')" :tooltip="apiParams.specifyvlan.description"/>
              </template>
              <a-switch v-model:checked="form.specifyvlan" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="ispersistent" ref="ispersistent" v-if="guestType !== 'shared'">
              <template #label>
                <tooltip-label :title="$t('label.ispersistent')" :tooltip="apiParams.ispersistent.description"/>
              </template>
              <a-switch v-model:checked="form.ispersistent" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="forvpc" ref="forvpc" v-if="guestType === 'isolated'">
          <template #label>
            <tooltip-label :title="$t('label.vpc')" :tooltip="apiParams.forvpc.description"/>
          </template>
          <a-switch v-model:checked="form.forvpc" @change="val => { handleForVpcChange(val) }" />
        </a-form-item>
        <a-form-item name="userdatal2" ref="userdatal2" :label="$t('label.userdatal2')" v-if="guestType === 'l2'">
          <a-switch v-model:checked="form.userdatal2" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :md="12" :lg="12">
            <a-form-item name="promiscuousmode" ref="promiscuousmode">
              <template #label>
                <tooltip-label :title="$t('label.promiscuousmode')" :tooltip="$t('message.network.offering.promiscuous.mode')"/>
              </template>
              <a-radio-group
                v-model:value="form.promiscuousmode"
                buttonStyle="solid">
                <a-radio-button value="">
                  {{ $t('label.none') }}
                </a-radio-button>
                <a-radio-button value="true">
                  {{ $t('label.accept') }}
                </a-radio-button>
                <a-radio-button value="false">
                  {{ $t('label.reject') }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item name="macaddresschanges" ref="macaddresschanges">
              <template #label>
                <tooltip-label :title="$t('label.macaddresschanges')" :tooltip="$t('message.network.offering.mac.address.changes')"/>
              </template>
              <a-radio-group
                v-model:value="form.macaddresschanges"
                buttonStyle="solid">
                <a-radio-button value="">
                  {{ $t('label.none') }}
                </a-radio-button>
                <a-radio-button value="true">
                  {{ $t('label.accept') }}
                </a-radio-button>
                <a-radio-button value="false">
                  {{ $t('label.reject') }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="forgedtransmits" ref="forgedtransmits">
              <template #label>
                <tooltip-label :title="$t('label.forgedtransmits')" :tooltip="$t('message.network.offering.forged.transmits')"/>
              </template>
              <a-radio-group
                v-model:value="form.forgedtransmits"
                buttonStyle="solid">
                <a-radio-button value="">
                  {{ $t('label.none') }}
                </a-radio-button>
                <a-radio-button value="true">
                  {{ $t('label.accept') }}
                </a-radio-button>
                <a-radio-button value="false">
                  {{ $t('label.reject') }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item name="maclearning" ref="maclearning">
              <template #label>
                <tooltip-label :title="$t('label.maclearning')" :tooltip="$t('message.network.offering.mac.learning')"/>
              </template>
              <span v-if="form.maclearning !== ''">
                <a-alert type="warning">
                  <template #message>
                    <div v-html="$t('message.network.offering.mac.learning.warning')"></div>
                  </template>
                </a-alert>
                <br/>
              </span>
              <a-radio-group
                v-model:value="form.maclearning"
                buttonStyle="solid">
                <a-radio-button value="">
                  {{ $t('label.none') }}
                </a-radio-button>
                <a-radio-button value="true">
                  {{ $t('label.accept') }}
                </a-radio-button>
                <a-radio-button value="false">
                  {{ $t('label.reject') }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="guestType !== 'l2'">
          <template #label>
            <tooltip-label :title="$t('label.supportedservices')" :tooltip="apiParams.supportedservices.description"/>
          </template>
          <div class="supported-services-container" scroll-to="last-child">
            <a-list itemLayout="horizontal" :dataSource="supportedServices">
              <template #renderItem="{item}">
                <a-list-item>
                  <CheckBoxSelectPair
                    :resourceKey="item.name"
                    :checkBoxLabel="item.description"
                    :selectOptions="!supportedServiceLoading ? item.provider: []"
                    @handle-checkselectpair-change="handleSupportedServiceChange"/>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-form-item>
        <a-form-item name="lbtype" ref="lbtype" :label="$t('label.lbtype')" v-if="forVpc && lbServiceChecked">
          <a-radio-group
            v-model:value="form.lbtype"
            buttonStyle="solid"
            @change="e => { handleLbTypeChange(e.target.value) }" >
            <a-radio-button value="publicLb">
              {{ $t('label.public.lb') }}
            </a-radio-button>
            <a-radio-button value="internalLb">
              {{ $t('label.internal.lb') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="serviceofferingid" ref="serviceofferingid">
          <a-alert v-if="!isVirtualRouterForAtLeastOneService" type="warning" style="margin-bottom: 10px">
            <template #message>
              <span v-if="guestType === 'l2'" v-html="$t('message.vr.alert.upon.network.offering.creation.l2')" />
              <span v-else v-html="$t('message.vr.alert.upon.network.offering.creation.others')" />
            </template>
          </a-alert>
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
          </template>
          <a-select
            showSearch
            optionFilterProp="label"
            v-model:value="form.serviceofferingid"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt) in serviceOfferings" :key="opt.id" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="redundantroutercapability"
          ref="redundantroutercapability"
          :label="$t('label.redundantrouter')"
          v-if="(guestType === 'shared' || guestType === 'isolated') && sourceNatServiceChecked && !isVpcVirtualRouterForAtLeastOneService">
          <a-switch v-model:checked="form.redundantroutercapability" />
        </a-form-item>
        <a-form-item name="sourcenattype" ref="sourcenattype" :label="$t('label.sourcenattype')" v-if="(guestType === 'shared' || guestType === 'isolated') && sourceNatServiceChecked">
          <a-radio-group
            v-model:value="form.sourcenattype"
            buttonStyle="solid">
            <a-radio-button value="peraccount">
              {{ $t('label.per.account') }}
            </a-radio-button>
            <a-radio-button value="perzone">
              {{ $t('label.per.zone') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          name="vmautoscalingcapability"
          ref="vmautoscalingcapability"
          :label="$t('label.supportsvmautoscaling')"
          v-if="lbServiceChecked && ['Netscaler', 'VirtualRouter', 'VpcVirtualRouter'].includes(lbServiceProvider)">
          <a-switch v-model:checked="form.vmautoscalingcapability" />
        </a-form-item>
        <a-form-item
          name="elasticlb"
          ref="elasticlb"
          :label="$t('label.service.lb.elasticlbcheckbox')"
          v-if="guestType === 'shared' && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-switch v-model:checked="form.elasticlb" />
        </a-form-item>
        <a-form-item
          name="inlinemode"
          ref="inlinemode"
          :label="$t('label.service.lb.inlinemodedropdown')"
          v-if="['shared', 'isolated'].includes(guestType) && lbServiceChecked && firewallServiceChecked && ['F5BigIp', 'Netscaler'].includes(lbServiceProvider) && ['JuniperSRX'].includes(firewallServiceProvider)">
          <a-radio-group
            v-model:value="form.inlinemode"
            buttonStyle="solid">
            <a-radio-button value="false">
              {{ $t('side.by.side') }}
            </a-radio-button>
            <a-radio-button value="true">
              {{ $t('inline') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          name="netscalerservicepackages"
          ref="netscalerservicepackages"
          :label="$t('label.service.lb.netscaler.servicepackages')"
          v-if="(guestType === 'shared' || guestType === 'isolated') && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-select
            v-model:value="form.netscalerservicepackages"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="registeredServicePackageLoading"
            :placeholder="$t('label.service.lb.netscaler.servicepackages')">
            <a-select-option v-for="(opt, optIndex) in registeredServicePackages" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="netscalerservicepackagesdescription"
          ref="netscalerservicepackagesdescription"
          :label="$t('label.service.lb.netscaler.servicepackages.description')"
          v-if="(guestType === 'shared' || guestType === 'isolated') && lbServiceChecked && lbServiceProvider === 'Netscaler'">
          <a-input
            v-model:value="form.netscalerservicepackagesdescription"
            :placeholder="$t('label.service.lb.netscaler.servicepackages.description')"/>
        </a-form-item>
        <a-form-item name="isolation" ref="isolation" :title="$t('label.service.lb.lbisolationdropdown')" v-show="false">
          <a-radio-group
            v-model:value="form.isolation"
            buttonStyle="solid">
            <a-radio-button value="dedicated">
              {{ $t('label.dedicated') }}
            </a-radio-button>
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          name="elasticip"
          ref="elasticip"
          :label="$t('label.service.staticnat.elasticipcheckbox')"
          v-if="guestType === 'shared' && staticNatServiceChecked && staticNatServiceProvider === 'Netscaler'">
          <a-switch v-model:checked="form.elasticip" />
        </a-form-item>
        <a-form-item
          name="associatepublicip"
          ref="associatepublicip"
          :label="$t('label.service.staticnat.associatepublicip')"
          v-if="isElasticIp && staticNatServiceChecked && staticNatServiceProvider === 'Netscaler'">
          <a-switch v-model:checked="form.associatepublicip" />
        </a-form-item>
        <a-form-item
          name="supportsstrechedl2subnet"
          ref="supportsstrechedl2subnet"
          :label="$t('label.supportsstrechedl2subnet')"
          v-if="connectivityServiceChecked">
          <a-switch v-model:checked="form.supportsstrechedl2subnet" />
        </a-form-item>
        <a-form-item name="supportspublicaccess" ref="supportspublicaccess" :label="$t('label.supportspublicaccess')" v-show="false">
          <a-switch v-model:checked="form.supportspublicaccess" />
        </a-form-item>
        <a-form-item
          name="conservemode"
          ref="conservemode"
          v-if="(guestType === 'shared' || guestType === 'isolated') && !isVpcVirtualRouterForAtLeastOneService">
          <template #label>
            <tooltip-label :title="$t('label.conservemode')" :tooltip="apiParams.conservemode.description"/>
          </template>
          <a-switch v-model:checked="form.conservemode" />
        </a-form-item>
        <a-form-item name="tags" ref="tags">
          <template #label>
            <tooltip-label :title="$t('label.tags')" :tooltip="apiParams.tags.description"/>
          </template>
          <a-input
            v-model:value="form.tags"
            :placeholder="apiParams.tags.description"/>
        </a-form-item>
        <a-form-item name="availability" ref="availability" v-if="requiredNetworkOfferingExists && guestType === 'isolated' && sourceNatServiceChecked">
          <template #label>
            <tooltip-label :title="$t('label.availability')" :tooltip="apiParams.availability.description"/>
          </template>
          <a-radio-group
            v-model:value="form.availability"
            buttonStyle="solid">
            <a-radio-button value="optional">
              {{ $t('label.optional') }}
            </a-radio-button>
            <a-radio-button value="required">
              {{ $t('label.required') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="egressdefaultpolicy" ref="egressdefaultpolicy" v-if="firewallServiceChecked">
          <template #label>
            <tooltip-label :title="$t('label.egressdefaultpolicy')" :tooltip="apiParams.egressdefaultpolicy.description"/>
          </template>
          <a-radio-group
            v-model:value="form.egressdefaultpolicy"
            buttonStyle="solid">
            <a-radio-button value="allow">
              {{ $t('label.allow') }}
            </a-radio-button>
            <a-radio-button value="deny">
              {{ $t('label.deny') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-model:checked="form.ispublic" @change="val => { isPublic = val }" />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="!isPublic">
          <template #label>
            <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          </template>
          <a-select
            mode="multiple"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="enable" ref="enable" v-if="apiParams.enable">
          <template #label>
            <tooltip-label :title="$t('label.enable.network.offering')" :tooltip="apiParams.enable.description"/>
          </template>
          <a-switch v-model:checked="form.enable" />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddNetworkOffering',
  mixins: [mixinForm],
  components: {
    CheckBoxSelectPair,
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      hasAdvanceZone: false,
      requiredNetworkOfferingExists: false,
      guestType: 'isolated',
      internetProtocolValue: 'ipv4',
      selectedDomains: [],
      selectedZones: [],
      forVpc: false,
      lbType: 'publicLb',
      macLearningValue: '',
      supportedServices: [],
      supportedServiceLoading: false,
      isVirtualRouterForAtLeastOneService: false,
      isVpcVirtualRouterForAtLeastOneService: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      sourceNatServiceChecked: false,
      lbServiceChecked: false,
      lbServiceProvider: '',
      registeredServicePackages: [],
      registeredServicePackageLoading: false,
      isElasticIp: false,
      staticNatServiceChecked: false,
      staticNatServiceProvider: '',
      connectivityServiceChecked: false,
      firewallServiceChecked: false,
      firewallServiceProvider: '',
      selectedServiceProviderMap: {},
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      ipv6NetworkOfferingEnabled: false,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createNetworkOffering')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        internetprotocol: this.internetProtocolValue,
        guestiptype: this.guestType,
        specifyvlan: true,
        lbtype: this.lbType,
        promiscuousmode: '',
        macaddresschanges: '',
        forgedtransmits: '',
        maclearning: this.macLearningValue,
        sourcenattype: 'peraccount',
        inlinemode: 'false',
        vmautoscalingcapability: true,
        isolation: 'dedicated',
        conservemode: true,
        availability: 'optional',
        egressdefaultpolicy: 'deny',
        ispublic: this.isPublic
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        networkrate: [{ type: 'number', validator: this.validateNumber }],
        serviceofferingid: [{ required: true, message: this.$t('message.error.select') }],
        domainid: [{ type: 'array', required: true, message: this.$t('message.error.select') }],
        zoneid: [{
          type: 'array',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      this.fetchSupportedServiceData()
      this.fetchServiceOfferingData()
      this.fetchIpv6NetworkOfferingConfiguration()
    },
    isAdmin () {
      return isAdmin()
    },
    isSupportedServiceObject (obj) {
      return (obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object && 'provider' in obj)
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchIpv6NetworkOfferingConfiguration () {
      this.ipv6NetworkOfferingEnabled = false
      var params = { name: 'ipv6.offering.enabled' }
      api('listConfigurations', params).then(json => {
        var value = json?.listconfigurationsresponse?.configuration?.[0].value || null
        this.ipv6NetworkOfferingEnabled = value === 'true'
      })
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    handleGuestTypeChange (val) {
      this.guestType = val
      if (val === 'l2') {
        this.form.forvpc = false
        this.form.lbtype = 'publicLb'
        this.isVirtualRouterForAtLeastOneService = false
        this.isVpcVirtualRouterForAtLeastOneService = false
        this.serviceOfferings = []
        this.serviceOfferingLoading = false
        this.sourceNatServiceChecked = false
        this.lbServiceChecked = false
        this.lbServiceProvider = ''
        this.registeredServicePackages = []
        this.registeredServicePackageLoading = false
        this.isElasticIp = false
        this.staticNatServiceChecked = false
        this.staticNatServiceProvider = ''
        this.connectivityServiceChecked = false
        this.firewallServiceChecked = false
        this.firewallServiceProvider = ''
        this.selectedServiceProviderMap = {}
        this.updateSupportedServices()
      }
    },
    fetchSupportedServiceData () {
      this.supportedServiceLoading = true
      this.supportedServices = []
      api('listSupportedNetworkServices').then(json => {
        this.supportedServices = json.listsupportednetworkservicesresponse.networkservice
        for (var i in this.supportedServices) {
          var networkServiceObj = this.supportedServices[i]
          var serviceName = networkServiceObj.name
          var serviceDisplayName = serviceName

          // Sanitize names
          // switch (serviceName) {
          //   case 'Vpn':
          //     serviceDisplayName = this.$t('label.vpn')
          //     break
          //   case 'Dhcp':
          //     serviceDisplayName = this.$t('label.dhcp')
          //     break
          //   case 'Dns':
          //     serviceDisplayName = this.$t('label.dns')
          //     break
          //   case 'Lb':
          //     serviceDisplayName = this.$t('label.load.balancer')
          //     break
          //   case 'SourceNat':
          //     serviceDisplayName = this.$t('label.source.nat')
          //     break
          //   case 'StaticNat':
          //     serviceDisplayName = this.$t('label.static.nat')
          //     break
          //   case 'PortForwarding':
          //     serviceDisplayName = this.$t('label.port.forwarding')
          //     break
          //   case 'UserData':
          //     serviceDisplayName = this.$t('label.user.data')
          //     break
          //   case 'Connectivity':
          //     serviceDisplayName = this.$t('label.virtual.networking')
          //     break
          //   default:
          //     serviceDisplayName = serviceName
          //     break
          // }
          var providers = []
          for (var j in this.supportedServices[i].provider) {
            var provider = this.supportedServices[i].provider[j]
            provider.description = provider.name
            provider.enabled = true
            if (provider.name === 'VpcVirtualRouter') {
              provider.enabled = false
            }
            if (provider.name === 'VirtualRouter') {
              providers.unshift(provider)
            } else {
              providers.push(provider)
            }
          }
          this.supportedServices[i].provider = providers
          this.supportedServices[i].description = serviceDisplayName
        }
      }).finally(() => {
        this.supportedServiceLoading = false
        this.updateSupportedServices()
      })
    },
    fetchServiceOfferingData () {
      const params = {}
      params.issystem = true
      params.systemvmtype = 'domainrouter'
      this.serviceOfferingLoading = true
      api('listServiceOfferings', params).then(json => {
        const listServiceOfferings = json.listserviceofferingsresponse.serviceoffering
        this.serviceOfferings = this.serviceOfferings.concat(listServiceOfferings)
        this.form.serviceofferingid = this.serviceOfferings.length > 0 ? this.serviceOfferings[0].id : ''
      }).finally(() => {
        this.serviceOfferingLoading = false
      })
    },
    fetchRegisteredServicePackageData () {
      this.registeredServicePackageLoading = true
      this.registeredServicePackages = []
      api('listRegisteredServicePackages', {}).then(json => {
        var servicePackages = json.listregisteredservicepackage.registeredServicepackage
        if (servicePackages === undefined || servicePackages == null || !servicePackages) {
          servicePackages = json.listregisteredservicepackage
        }
        for (var i in servicePackages) {
          this.registeredServicePackages.push({
            id: servicePackages[i].id,
            description: servicePackages[i].name,
            desc: servicePackages[i].description
          })
        }
      }).finally(() => {
        this.registeredServicePackageLoading = false
      })
    },
    updateSupportedServices () {
      this.supportedServiceLoading = true
      var supportedServices = this.supportedServices
      var self = this
      supportedServices.forEach(function (svc, index) {
        if (svc.name !== 'Connectivity') {
          var providers = svc.provider
          providers.forEach(function (provider, providerIndex) {
            if (self.forVpc) { // *** vpc ***
              var enabledProviders = ['VpcVirtualRouter', 'Netscaler', 'BigSwitchBcf', 'ConfigDrive']
              if (self.lbType === 'internalLb') {
                enabledProviders.push('InternalLbVm')
              }
              provider.enabled = enabledProviders.includes(provider.name)
            } else { // *** non-vpc ***
              provider.enabled = !['InternalLbVm', 'VpcVirtualRouter'].includes(provider.name)
            }
            providers[providerIndex] = provider
          })
          svc.provider = providers
          supportedServices[index] = svc
        }
      })
      setTimeout(() => {
        self.supportedServices = supportedServices
        self.supportedServiceLoading = false
      }, 50)
    },
    handleForVpcChange (forVpc) {
      this.forVpc = forVpc
      this.updateSupportedServices()
    },
    handleLbTypeChange (lbType) {
      this.lbType = lbType
      this.updateSupportedServices()
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      } else if (service === 'Lb') {
        if (checked) {
          this.fetchRegisteredServicePackageData()
          if (provider != null & provider !== undefined) {
            this.lbServiceProvider = provider
          }
        } else {
          this.lbServiceProvider = ''
        }
        this.lbServiceChecked = checked
      } else if (service === 'StaticNat') {
        this.staticNatServiceChecked = checked
        if (checked && provider != null & provider !== undefined) {
          this.staticNatServiceProvider = provider
        } else {
          this.staticNatServiceProvider = ''
        }
      } else if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      } else if (service === 'Firewall') {
        this.firewallServiceChecked = checked
        if (checked && provider != null & provider !== undefined) {
          this.staticNatServiceProvider = provider
        } else {
          this.staticNatServiceProvider = ''
        }
      }
      if (checked && provider != null & provider !== undefined) {
        this.selectedServiceProviderMap[service] = provider
      } else {
        delete this.selectedServiceProviderMap[service]
      }
      var providers = Object.values(this.selectedServiceProviderMap)
      this.isVirtualRouterForAtLeastOneService = false
      this.isVpcVirtualRouterForAtLeastOneService = false
      var self = this
      providers.forEach(function (prvdr, idx) {
        if (prvdr === 'VirtualRouter') {
          self.isVirtualRouterForAtLeastOneService = true
        }
        if (prvdr === 'VpcVirtualRouter') {
          self.isVpcVirtualRouterForAtLeastOneService = true
        }
        if ((self.isVirtualRouterForAtLeastOneService || self.isVpcVirtualRouterForAtLeastOneService) &&
          self.serviceOfferings.length === 0) {
          self.fetchServiceOfferingData()
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        var params = {}

        var keys = Object.keys(values)
        const detailsKey = ['promiscuousmode', 'macaddresschanges', 'forgedtransmits', 'maclearning']
        const ignoredKeys = [...detailsKey, 'state', 'status', 'allocationstate', 'forvpc', 'lbType', 'specifyvlan', 'ispublic', 'domainid', 'zoneid', 'egressdefaultpolicy', 'isolation', 'supportspublicaccess']
        keys.forEach(function (key, keyIndex) {
          if (!ignoredKeys.includes(key) &&
            values[key] != null && values[key] !== undefined &&
            !(key === 'availability' && values[key] === 'Optional')) {
            params[key] = values[key]
          }
        })

        if (values.guestiptype === 'shared') { // specifyVlan checkbox is disabled, so inputData won't include specifyVlan
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          params.specifyipranges = true
          delete params.ispersistent
        } else if (values.guestiptype === 'isolated') { // specifyVlan checkbox is shown
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          if (values.ispersistent) {
            params.ispersistent = true
          } else { // Isolated Network with Non-persistent network
            delete params.ispersistent
          }
        } else if (values.guestiptype === 'l2') {
          if (values.specifyvlan === true) {
            params.specifyvlan = true
          }
          if (values.userdatal2 === true) {
            params['serviceProviderList[0].service'] = 'UserData'
            params['serviceProviderList[0].provider'] = 'ConfigDrive'
            params.supportedservices = 'UserData'
          }
          // Conserve mode is irrelevant on L2 network offerings as there are no resources to conserve, do not pass it, true by default on server side
          delete params.conservemode
        }

        if (values.forvpc === true) {
          params.forvpc = true
        }
        if (values.guestiptype === 'shared' || values.guestiptype === 'isolated') {
          if (values.conservemode !== true) {
            params.conservemode = false
          }
        }
        if (this.selectedServiceProviderMap != null) {
          var supportedServices = Object.keys(this.selectedServiceProviderMap)
          params.supportedservices = supportedServices.join(',')
          for (var k in supportedServices) {
            params['serviceProviderList[' + k + '].service'] = supportedServices[k]
            params['serviceProviderList[' + k + '].provider'] = this.selectedServiceProviderMap[supportedServices[k]]
          }
          var serviceCapabilityIndex = 0
          if (supportedServices.includes('Connectivity')) {
            if (values.supportsstrechedl2subnet === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RegionLevelVpc'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.supportspublicaccess === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'DistributedRouter'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            delete params.supportsstrechedl2subnet
            delete params.supportspublicaccess
          }
          if (supportedServices.includes('SourceNat')) {
            if (values.redundantroutercapability === true) {
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'SourceNat'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = 'RedundantRouter'
              params['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'SourceNat'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedSourceNatTypes'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.sourcenattype
            serviceCapabilityIndex++
            delete params.redundantroutercapability
            delete params.sourcenattype
          }
          if (supportedServices.includes('SourceNat')) {
            if (values.elasticip === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticIp'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.elasticip === true || values.associatepublicip === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'associatePublicIP'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.associatepublicip
              serviceCapabilityIndex++
            }
            delete params.elasticip
            delete params.associatepublicip
          }
          if (supportedServices.includes('Lb')) {
            if ('vmautoscalingcapability' in values) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'VmAutoScaling'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.vmautoscalingcapability
              serviceCapabilityIndex++
            }
            if (values.elasticlb === true) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticLb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true
              serviceCapabilityIndex++
            }
            if (values.inlinemode === true && ((this.selectedServiceProviderMap.Lb === 'F5BigIp') || (this.selectedServiceProviderMap.Lb === 'Netscaler'))) {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'InlineMode'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.inlinemode
              serviceCapabilityIndex++
            }
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedLbIsolation'
            params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = values.isolation
            serviceCapabilityIndex++
            if (this.selectedServiceProviderMap.Lb === 'InternalLbVm') {
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'lbSchemes'
              params['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = 'internal'
              serviceCapabilityIndex++
            }
            if ('netscalerservicepackages' in values &&
              this.registeredServicePackages.length > values.netscalerservicepackages &&
              'netscalerservicepackagesdescription' in values) {
              params['details[' + 0 + '].servicepackageuuid'] = this.registeredServicePackages[values.netscalerservicepackages].id
              params['details[' + 1 + '].servicepackagedescription'] = values.netscalerservicepackagesdescription
            }
          }
        } else {
          if (!('supportedservices' in params)) {
            params.supportedservices = ''
          }
        }

        if (values.guestiptype === 'l2' && values.userdatal2 === true) {
          params.supportedservices = 'UserData'
        }

        if ('egressdefaultpolicy' in values && values.egressdefaultpolicy !== 'allow') {
          params.egressdefaultpolicy = false
        }
        for (const key of detailsKey) {
          if (values[key]) {
            params['details[0].' + key] = values[key]
          }
        }
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        var zoneIndexes = values.zoneid
        var zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }
        if (values.enable) {
          params.enable = values.enable
        }
        params.traffictype = 'GUEST' // traffic type dropdown has been removed since it has only one option ('Guest'). Hardcode traffic type value here.
        api('createNetworkOffering', params).then(json => {
          this.$message.success('Network offering created: ' + values.name)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
    }
  }
}
</script>

<style scoped lang="scss">
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 600px;
    }
  }
  .supported-services-container {
    height: 250px;
    overflow: auto;
  }
</style>
