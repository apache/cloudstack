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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-alert
        v-if="resource"
        type="info"
        style="margin-bottom: 16px">
        <template #message>
          <div style="display: block; width: 100%;">
            <div style="display: block; margin-bottom: 8px;">
              <strong>{{ $t('message.clone.offering.from') }}: {{ resource.name }}</strong>
            </div>
            <div style="display: block; font-size: 12px;">
              {{ $t('message.clone.offering.edit.hint') }}
            </div>
          </div>
        </template>
      </a-alert>
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical"
      >
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
            @change="e => { internetProtocolValue = e.target.value }">
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
            <a-form-item name="forvpc" ref="forvpc" v-if="guestType === 'isolated'">
              <template #label>
                <tooltip-label :title="$t('label.vpc')" :tooltip="apiParams.forvpc.description"/>
              </template>
              <a-switch v-model:checked="form.forvpc" @change="handleForVpcChange" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item name="provider" ref="provider">
              <template #label>
                <tooltip-label :title="$t('label.provider')" :tooltip="apiParams.provider.description"/>
              </template>
              <a-select
                v-model:value="form.provider"
                disabled
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
                :placeholder="apiParams.provider.description">
                <a-select-option key="" value="">{{ $t('label.none') }}</a-select-option>
                <a-select-option :value="'NSX'" :label="$t('label.nsx')">{{ $t('label.nsx') }}</a-select-option>
                <a-select-option :value="'Netris'" :label="$t('label.netris')">{{ $t('label.netris') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <!-- NSX specific toggles (mirror AddNetworkOffering.vue) -->
        <a-row :gutter="12" v-if="form.provider === 'NSX'">
          <a-col :md="12" :lg="12">
            <a-form-item name="nsxsupportlb" ref="nsxsupportlb" v-if="guestType === 'isolated'">
              <template #label>
                <tooltip-label :title="$t('label.nsx.supports.lb')" :tooltip="apiParams.nsxsupportlb.description"/>
              </template>
              <a-switch v-model:checked="form.nsxsupportlb" @change="val => { handleNsxLbService(val) }" />
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12" v-if="form.nsxsupportlb && form.forvpc">
            <a-form-item name="nsxsupportsinternallb" ref="nsxsupportsinternallb" v-if="guestType === 'isolated'">
              <template #label>
                <tooltip-label :title="$t('label.nsx.supports.internal.lb')" :tooltip="apiParams.nsxsupportsinternallb.description"/>
              </template>
              <a-switch v-model:checked="form.nsxsupportsinternallb"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="networkmode" ref="networkmode" v-if="guestType === 'isolated' && form.provider">
          <template #label>
            <tooltip-label :title="$t('label.networkmode')" :tooltip="apiParams.networkmode.description"/>
          </template>
          <a-select
            v-model:value="form.networkmode"
            @change="val => { handleForNetworkModeChange(val) }"
            :disabled="provider === 'NSX' || provider === 'Netris'"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.networkmode.description">
            <a-select-option value="NATTED" :label="'NATTED'">NATTED</a-select-option>
            <a-select-option value="ROUTED" :label="'ROUTED'">ROUTED</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="routingmode" ref="routingmode" v-if="networkmode === 'ROUTED' || internetProtocolValue === 'ipv6' || internetProtocolValue === 'dualstack'">
          <template #label>
            <tooltip-label :title="$t('label.routingmode')" :tooltip="apiParams.routingmode.description"/>
          </template>
          <a-radio-group
            v-model:value="form.routingmode"
            buttonStyle="solid"
            @change="selected => { routingMode = selected.target.value }">
            <a-radio-button value="static">
              {{ $t('label.static') }}
            </a-radio-button>
            <a-radio-button value="dynamic">
              {{ $t('label.dynamic') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="userdatal2" ref="userdatal2" :label="$t('label.user.data')" v-if="guestType === 'l2'">
          <a-switch v-model:checked="form.userdatal2" />
        </a-form-item>
        <a-form-item name="networkrate" ref="networkrate">
          <template #label>
            <tooltip-label :title="$t('label.networkrate')" :tooltip="apiParams.networkrate.description"/>
          </template>
          <a-input
            v-model:value="form.networkrate"
            :placeholder="apiParams.networkrate.description"/>
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
            <tooltip-label :title="$t('label.supportedservices')" :tooltip="apiParams.supportedservices ? apiParams.supportedservices.description : ''"/>
          </template>
          <div class="supported-services-container" scroll-to="last-child">
            <a-spin v-if="!servicesReady" :spinning="true" />
            <a-list v-else itemLayout="horizontal" :dataSource="supportedServices">
              <template #renderItem="{item}">
                <a-list-item>
                  <CheckBoxSelectPair
                    :key="`${item.name}-${item.selectedProvider || 'none'}-${item.defaultChecked}`"
                    :resourceKey="item.name"
                    :checkBoxLabel="item.description"
                    :forExternalNetProvider="form.provider === 'NSX' || form.provider === 'Netris'"
                    :defaultCheckBoxValue="item.defaultChecked"
                    :defaultSelectValue="item.selectedProvider"
                    :selectOptions="item.provider"
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
        <a-form-item name="serviceofferingid" ref="serviceofferingid" v-if="guestType !== 'l2' && isVirtualRouterForAtLeastOneService">
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
            <a-select-option
              v-for="(offering, index) in serviceOfferings"
              :value="offering.id"
              :label="offering.displaytext || offering.name"
              :key="index">
              {{ offering.displaytext || offering.name }}
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
        <a-form-item
          name="conservemode"
          ref="conservemode"
          v-if="guestType === 'shared' || guestType === 'isolated'">
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
            <a-radio-button value="Optional">
              {{ $t('label.optional') }}
            </a-radio-button>
            <a-radio-button value="Required">
              {{ $t('label.required') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="egressdefaultpolicy" ref="egressdefaultpolicy">
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
        <a-form-item name="specifyipranges" ref="specifyipranges">
          <template #label>
            <tooltip-label :title="$t('label.specifyipranges')" :tooltip="apiParams.specifyipranges.description"/>
          </template>
          <a-switch v-model:checked="form.specifyipranges"/>
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
          <a-switch v-model:checked="form.enable"/>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import CheckBoxSelectPair from '@/components/CheckBoxSelectPair'
import { BlockOutlined, GlobalOutlined } from '@ant-design/icons-vue'
import { buildServiceCapabilityParams } from '@/composables/useServiceCapabilityParams'

export default {
  name: 'CloneNetworkOffering',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel,
    CheckBoxSelectPair,
    BlockOutlined,
    GlobalOutlined
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      VPCVR: {
        name: 'VPCVirtualRouter',
        description: 'VPCVirtualRouter',
        enabled: true
      },
      VR: {
        name: 'VirtualRouter',
        description: 'VirtualRouter',
        enabled: true
      },
      NSX: {
        name: 'Nsx',
        description: 'Nsx',
        enabled: true
      },
      Netris: {
        name: 'Netris',
        description: 'Netris',
        enabled: true
      },
      nsxSupportedServicesMap: {},
      netrisSupportedServicesMap: {},
      // supported services backup
      supportedSvcs: [],
      guestType: 'isolated',
      forVpc: false,
      provider: '',
      networkmode: '',
      routingMode: 'static',
      internetProtocolValue: 'ipv4',
      networkmodes: [
        {
          id: 0,
          name: 'NATTED'
        },
        {
          id: 1,
          name: 'ROUTED'
        }
      ],
      supportedServices: [],
      supportedServiceLoading: false,
      servicesReady: false,
      selectedServiceProviderMap: {},
      serviceProviderMap: {},
      serviceOfferings: [],
      serviceOfferingLoading: false,
      isVirtualRouterForAtLeastOneService: false,
      isVpcVirtualRouterForAtLeastOneService: false,
      requiredNetworkOfferingExists: false,
      sourceNatServiceChecked: false,
      lbServiceChecked: false,
      lbServiceProvider: '',
      firewallServiceChecked: false,
      firewallServiceProvider: '',
      staticNatServiceChecked: false,
      staticNatServiceProvider: '',
      connectivityServiceChecked: false,
      isElasticIp: false,
      registeredServicePackages: [],
      registeredServicePackageLoading: false,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      ipv6NetworkOfferingEnabled: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('cloneNetworkOffering')
  },
  watch: {
    'form.elasticip' (newVal) {
      this.isElasticIp = newVal
    }
  },
  async created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.isPublic = isAdmin()
    this.initForm()
    await this.checkRequiredNetworkOfferingExists()
    this.fetchSupportedServices()
    this.fetchServiceOfferingData()
    this.fetchDomainData()
    this.fetchZoneData()
    this.fetchIpv6NetworkOfferingConfiguration()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        guestiptype: this.guestType,
        internetprotocol: 'ipv4',
        forvpc: false,
        provider: '',
        networkmode: '',
        routingmode: 'static',
        lbtype: 'publicLb',
        userdatal2: false,
        availability: 'Optional',
        conservemode: true,
        ispersistent: false,
        specifyvlan: false,
        specifyipranges: false,
        egressdefaultpolicy: 'deny',
        enable: false,
        promiscuousmode: '',
        macaddresschanges: '',
        forgedtransmits: '',
        maclearning: '',
        ispublic: this.isPublic,
        domainid: [],
        zoneid: [],
        redundantroutercapability: false,
        sourcenattype: 'peraccount',
        vmautoscalingcapability: true,
        elasticlb: false,
        inlinemode: 'false',
        elasticip: false,
        associatepublicip: false,
        supportsstrechedl2subnet: false,
        isolation: 'dedicated',
        netscalerservicepackages: null,
        netscalerservicepackagesdescription: '',
        nsxsupportlb: true,
        nsxsupportsinternallb: false
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        networkrate: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value < 0)) {
              return Promise.reject(this.$t('message.validate.number'))
            }
            return Promise.resolve()
          }
        }],
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
    fetchServiceOfferingData () {
      this.serviceOfferingLoading = true
      getAPI('listServiceOfferings', { issystem: true, systemvmtype: 'domainrouter' }).then(json => {
        this.serviceOfferings = json.listserviceofferingsresponse.serviceoffering || []
      }).finally(() => {
        this.serviceOfferingLoading = false
      })
    },
    fetchSupportedServices () {
      this.supportedServiceLoading = true
      getAPI('listSupportedNetworkServices', {}).then(json => {
        this.supportedServices = json.listsupportednetworkservicesresponse.networkservice || []

        for (const i in this.supportedServices) {
          const networkServiceObj = this.supportedServices[i]
          const serviceName = networkServiceObj.name
          const serviceDisplayName = serviceName

          const providers = []
          for (const j in this.supportedServices[i].provider) {
            const provider = this.supportedServices[i].provider[j]
            providers.push({
              name: provider.name,
              enabled: true
            })
          }

          this.supportedServices[i].provider = providers
          this.supportedServices[i].description = serviceDisplayName
        }

        this.supportedSvcs = this.supportedServices.slice()
        this.populateFormFromResource()
        this.updateSupportedServices()
      }).finally(() => {
        this.supportedServiceLoading = false
      })
    },
    handleLbTypeChange (lbType) {
      this.lbType = lbType
      // Enable InternalLbVm provider for Lb service if internalLb is selected
      if (lbType === 'internalLb') {
        this.supportedServices = this.supportedServices.map(svc => {
          if (svc.name === 'Lb') {
            const providers = svc.provider.map(provider => {
              provider.enabled = provider.name === 'InternalLbVm'
              this.lbServiceProvider = provider
              return provider
            })
            return { ...svc, provider: providers, defaultChecked: true, selectedProvider: 'InternalLbVm' }
          }
          return svc
        })
        this.selectedServiceProviderMap.Lb = 'InternalLbVm'
      } else {
        // Revert to default providers for Lb service
        this.supportedServices = this.supportedServices.map(svc => {
          if (svc.name === 'Lb') {
            const providers = svc.provider.map(provider => {
              provider.enabled = provider.name !== 'InternalLbVm'
              return provider
            })
            // Pick the first enabled provider as selected
            const firstEnabled = providers.find(p => p.enabled)
            return { ...svc, provider: providers, defaultChecked: !!firstEnabled, selectedProvider: firstEnabled ? firstEnabled.name : null }
          }
          return svc
        })
        // Update selectedServiceProviderMap for Lb
        const lbSvc = this.supportedServices.find(svc => svc.name === 'Lb')
        if (lbSvc && lbSvc.selectedProvider) {
          this.selectedServiceProviderMap.Lb = lbSvc.selectedProvider
        } else {
          delete this.selectedServiceProviderMap.Lb
        }
      }
      this.updateSupportedServices()
    },
    populateFormFromResource () {
      if (!this.resource) return

      const r = this.resource
      this.form.name = r.name + ' - Clone'
      this.form.displaytext = r.displaytext || r.name

      if (r.guestiptype) {
        this.guestType = r.guestiptype.toLowerCase()
        this.form.guestiptype = r.guestiptype.toLowerCase()

        if (this.guestType === 'l2' && r.service && Array.isArray(r.service)) {
          this.form.userdatal2 = r.service.some(svc => svc.name === 'UserData')
        }
      }
      if (r.internetprotocol) {
        this.form.internetprotocol = r.internetprotocol.toLowerCase()
        this.internetProtocolValue = r.internetprotocol.toLowerCase()
      }
      if (r.forvpc !== undefined) {
        this.forVpc = r.forvpc
        this.form.forvpc = r.forvpc
      }

      if (r.service && Array.isArray(r.service)) {
        // Prefer provider from NetworkACL service if present (for NSX/Netris), else scan other services
        let detectedProvider = null
        const networkAclService = r.service.find(svc => svc.name === 'NetworkACL')
        if (networkAclService && networkAclService.provider && networkAclService.provider.length > 0) {
          const provName = (networkAclService.provider[0].name || '').toLowerCase()
          if (provName === 'nsx') detectedProvider = 'NSX'
          else if (provName === 'netris') detectedProvider = 'Netris'
        }

        if (!detectedProvider) {
          // fallback: detect provider (Nsx/Netris) from any service provider in the source offering
          for (const svc of r.service) {
            if (svc.provider && Array.isArray(svc.provider) && svc.provider.length > 0) {
              const provName = (svc.provider[0].name || '').toLowerCase()
              if (provName === 'nsx') {
                detectedProvider = 'NSX'
                break
              } else if (provName === 'netris') {
                detectedProvider = 'Netris'
                break
              }
            }
          }
        }

        if (detectedProvider) {
          this.provider = detectedProvider
          this.form.provider = detectedProvider
          if (detectedProvider === 'NSX') {
            this.nsxSupportedServicesMap = {}
            for (const svc of r.service) {
              if (svc.provider && svc.provider.length > 0) {
                const provName = svc.provider[0].name
                if (provName && provName.toLowerCase() === 'nsx') {
                  this.nsxSupportedServicesMap[svc.name] = this.NSX
                } else if (svc.name === 'Dhcp' || svc.name === 'Dns' || svc.name === 'UserData') {
                  this.nsxSupportedServicesMap[svc.name] = this.forVpc ? this.VPCVR : this.VR
                }
              }
            }
            if (this.forVpc) {
              this.nsxSupportedServicesMap.NetworkACL = this.NSX
            } else {
              this.nsxSupportedServicesMap.Firewall = this.NSX
            }
          }
        }
      }

      if (r.networkmode) {
        this.networkmode = r.networkmode
        this.form.networkmode = r.networkmode
      }

      if (r.supportsinternallb) {
        this.form.lbtype = 'internalLb'
      }

      if (r.availability) {
        const isIsolatedWithSourceNat = this.guestType === 'isolated' &&
                                        r.service &&
                                        r.service.some(svc => svc.name === 'SourceNat')
        if (isIsolatedWithSourceNat && this.requiredNetworkOfferingExists) {
          this.form.availability = 'Optional'
        } else {
          this.form.availability = r.availability
        }
      }
      if (r.tags) this.form.tags = r.tags
      if (r.conservemode !== undefined) this.form.conservemode = r.conservemode
      if (r.ispersistent !== undefined) this.form.ispersistent = r.ispersistent
      if (r.specifyvlan !== undefined) this.form.specifyvlan = r.specifyvlan
      if (r.specifyipranges !== undefined) this.form.specifyipranges = r.specifyipranges
      if (r.egressdefaultpolicy !== undefined) {
        this.form.egressdefaultpolicy = r.egressdefaultpolicy ? 'allow' : 'deny'
      }
      if (r.networkrate) this.form.networkrate = r.networkrate
      if (r.serviceofferingid) this.form.serviceofferingid = r.serviceofferingid

      if (r.details) {
        if (r.details.promiscuousmode) this.form.promiscuousmode = r.details.promiscuousmode
        if (r.details.macaddresschanges) this.form.macaddresschanges = r.details.macaddresschanges
        if (r.details.forgedtransmits) this.form.forgedtransmits = r.details.forgedtransmits
        if (r.details.maclearning) this.form.maclearning = r.details.maclearning
      }

      this.forVpc = r.forvpc || false

      if (r.service && Array.isArray(r.service)) {
        const sourceServiceMap = {}
        r.service.forEach(svc => {
          if (svc.provider && svc.provider.length > 0) {
            const providerName = svc.provider[0].name
            sourceServiceMap[svc.name] = providerName
          }
        })

        this.serviceProviderMap = sourceServiceMap

        const updatedServices = this.supportedServices.map(svc => {
          const serviceCopy = { ...svc, provider: [...svc.provider] }

          if (sourceServiceMap[serviceCopy.name]) {
            const providerName = sourceServiceMap[serviceCopy.name]

            const providerIndex = serviceCopy.provider.findIndex(p => p.name === providerName)

            if (providerIndex > 0) {
              const targetProvider = serviceCopy.provider[providerIndex]
              serviceCopy.provider.splice(providerIndex, 1)
              serviceCopy.provider.unshift(targetProvider)
            }

            serviceCopy.defaultChecked = true
            serviceCopy.selectedProvider = providerName
            this.selectedServiceProviderMap[serviceCopy.name] = providerName
          } else {
            serviceCopy.defaultChecked = false
            serviceCopy.selectedProvider = null
          }
          return serviceCopy
        })

        this.supportedServices = updatedServices
        this.supportedSvcs = updatedServices.map(svc => ({ ...svc }))

        this.sourceNatServiceChecked = !!this.selectedServiceProviderMap.SourceNat
        this.lbServiceChecked = !!this.selectedServiceProviderMap.Lb
        this.lbServiceProvider = this.selectedServiceProviderMap.Lb || ''
        this.firewallServiceChecked = !!this.selectedServiceProviderMap.Firewall
        this.firewallServiceProvider = this.selectedServiceProviderMap.Firewall || ''
        this.staticNatServiceChecked = !!this.selectedServiceProviderMap.StaticNat
        this.staticNatServiceProvider = this.selectedServiceProviderMap.StaticNat || ''
        this.connectivityServiceChecked = !!this.selectedServiceProviderMap.Connectivity

        this.$nextTick(() => {
          this.servicesReady = true
          this.$nextTick(() => {
            this.checkVirtualRouterForServices()
          })
        })
      }
      if (this.provider === 'NSX') {
        this.form.nsxsupportlb = Boolean(this.serviceProviderMap.Lb)
        this.form.nsxsupportsinternallb = Boolean(r.nsxsupportsinternallb)
        this.handleNsxLbService(this.form.nsxsupportlb)
      }
    },
    updateSupportedServices () {
      const supportedServices = this.supportedServices || []
      if (!this.supportedSvcs || this.supportedSvcs.length === 0) {
        this.supportedSvcs = supportedServices.slice()
      }

      if (this.provider !== 'NSX' && this.provider !== 'Netris') {
        const filtered = supportedServices.map(svc => {
          if (svc.name !== 'Connectivity') {
            const providers = svc.provider.map(provider => {
              if (this.forVpc) {
                const enabledProviders = ['VpcVirtualRouter', 'Netscaler', 'BigSwitchBcf', 'ConfigDrive']
                if (self.lbType === 'internalLb') {
                  enabledProviders.push('InternalLbVm')
                }
                provider.enabled = enabledProviders.includes(provider.name)
              } else {
                provider.enabled = !['InternalLbVm', 'VpcVirtualRouter', 'Nsx', 'Netris'].includes(provider.name)
              }
              return provider
            })
            return { ...svc, provider: providers }
          }
          return svc
        })
        this.supportedServices = filtered
      } else {
        let svcMap = this.provider === 'NSX' ? this.nsxSupportedServicesMap : this.netrisSupportedServicesMap
        if (!svcMap || Object.keys(svcMap).length === 0) {
          svcMap = {}
          const forVpc = this.forVpc
          const baseProvider = this.provider === 'NSX' ? this.NSX : this.Netris
          const vrProvider = forVpc ? this.VPCVR : this.VR
          svcMap.Dhcp = vrProvider
          svcMap.Dns = vrProvider
          svcMap.UserData = vrProvider
          if (this.networkmode === 'NATTED') {
            svcMap.SourceNat = baseProvider
            svcMap.StaticNat = baseProvider
            svcMap.PortForwarding = baseProvider
            svcMap.Lb = baseProvider
          }
          if (forVpc) {
            svcMap.NetworkACL = baseProvider
          } else {
            svcMap.Firewall = baseProvider
          }
        }

        const filtered = this.supportedSvcs.filter(svc => Object.keys(svcMap).includes(svc.name))
          .map(svc => {
            const preserveSelected = svc.selectedProvider !== undefined ? { defaultChecked: svc.defaultChecked, selectedProvider: svc.selectedProvider } : {}
            const mappedProvider = svcMap[svc.name]
            if (!['Dhcp', 'Dns', 'UserData'].includes(svc.name)) {
              return { ...svc, provider: [mappedProvider], ...preserveSelected }
            }
            return { ...svc, provider: [mappedProvider], ...preserveSelected }
          })
        this.supportedServices = filtered
        const newSelectedMap = {}
        for (const svc of filtered) {
          const mappedProvider = svc.provider && svc.provider[0]
          const providerName = svc.selectedProvider || (mappedProvider && mappedProvider.name) || null
          if (providerName) {
            newSelectedMap[svc.name] = providerName
          }
        }
        this.selectedServiceProviderMap = newSelectedMap
      }
    },
    checkVirtualRouterForServices () {
      this.isVirtualRouterForAtLeastOneService = false
      this.isVpcVirtualRouterForAtLeastOneService = false
      for (const service in this.selectedServiceProviderMap) {
        const provider = this.selectedServiceProviderMap[service]
        if (provider === 'VirtualRouter') {
          this.isVirtualRouterForAtLeastOneService = true
        } else if (provider === 'VpcVirtualRouter') {
          this.isVirtualRouterForAtLeastOneService = true
          this.isVpcVirtualRouterForAtLeastOneService = true
        }
      }
    },
    handleGuestTypeChange (val) {
      this.guestType = val
    },
    handleForVpcChange (val) {
      this.forVpc = val
      this.updateSupportedServices()
    },
    handleNsxLbService (supportLb) {
      const forVpc = this.forVpc
      const baseProvider = this.NSX
      const vrProvider = forVpc ? this.VPCVR : this.VR

      const map = {
        Dhcp: vrProvider,
        Dns: vrProvider,
        UserData: vrProvider
      }
      const wantSourceNat = this.networkmode === 'NATTED' || !!this.serviceProviderMap.SourceNat
      const wantStaticNat = this.networkmode === 'NATTED' || !!this.serviceProviderMap.StaticNat
      const wantPortForwarding = this.networkmode === 'NATTED' || !!this.serviceProviderMap.PortForwarding
      const wantLb = supportLb || !!this.serviceProviderMap.Lb

      if (wantSourceNat) map.SourceNat = baseProvider
      if (wantStaticNat) map.StaticNat = baseProvider
      if (wantPortForwarding) map.PortForwarding = baseProvider
      if (wantLb) map.Lb = baseProvider

      if (forVpc) map.NetworkACL = baseProvider
      else map.Firewall = baseProvider

      this.nsxSupportedServicesMap = map
      this.updateSupportedServices()
    },
    handleForNetworkModeChange (val) {
      this.networkmode = val

      const routedExcludedServices = ['SourceNat', 'StaticNat', 'Lb', 'PortForwarding', 'Vpn']

      if (val === 'ROUTED' && this.guestType === 'isolated') {
        routedExcludedServices.forEach(service => {
          if (this.selectedServiceProviderMap[service]) {
            this.handleSupportedServiceChange(service, false, null)
          }
        })

        this.supportedServices = this.supportedServices.map(svc => {
          if (routedExcludedServices.includes(svc.name)) {
            return {
              ...svc,
              defaultChecked: false,
              selectedProvider: null
            }
          }
          return svc
        })
      } else if (val === 'NATTED') {
        routedExcludedServices.forEach(service => {
          if (!this.selectedServiceProviderMap[service] && this.serviceProviderMap[service]) {
            const provider = this.serviceProviderMap[service]
            this.handleSupportedServiceChange(service, true, provider)
          }
        })

        this.supportedServices = this.supportedServices.map(svc => {
          if (routedExcludedServices.includes(svc.name) && this.serviceProviderMap[svc.name]) {
            return {
              ...svc,
              defaultChecked: true,
              selectedProvider: this.serviceProviderMap[svc.name]
            }
          }
          return svc
        })
      }
    },
    isAdmin () {
      return isAdmin()
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      getAPI('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    checkRequiredNetworkOfferingExists () {
      return getAPI('listNetworkOfferings', {
        guestiptype: 'Isolated',
        sourcenatsupported: true,
        availability: 'Required',
        state: 'Enabled'
      }).then(json => {
        const offerings = json.listnetworkofferingsresponse.networkoffering || []
        this.requiredNetworkOfferingExists = offerings.length > 0
        return this.requiredNetworkOfferingExists
      }).catch(error => {
        console.error('checkRequiredNetworkOfferingExists: Error checking for required offerings:', error)
        this.requiredNetworkOfferingExists = false
        return false
      })
    },
    fetchIpv6NetworkOfferingConfiguration () {
      this.ipv6NetworkOfferingEnabled = false
      const params = { name: 'ipv6.offering.enabled' }
      getAPI('listConfigurations', params).then(json => {
        const value = json?.listconfigurationsresponse?.configuration?.[0].value || null
        this.ipv6NetworkOfferingEnabled = value === 'true'
      })
    },
    fetchRegisteredServicePackages () {
      this.registeredServicePackageLoading = true
      getAPI('listRegisteredServicePackages', {}).then(json => {
        this.registeredServicePackages = json.listregisteredservicepackage?.registeredServicePackage || []
      }).finally(() => {
        this.registeredServicePackageLoading = false
      })
    },
    handleSupportedServiceChange (service, checked, provider) {
      if (checked) {
        const correctProvider = this.serviceProviderMap[service]
        if (correctProvider && provider !== correctProvider) {
          this.selectedServiceProviderMap[service] = correctProvider
        } else {
          this.selectedServiceProviderMap[service] = provider
        }
      } else {
        delete this.selectedServiceProviderMap[service]
      }

      if (service === 'SourceNat') {
        this.sourceNatServiceChecked = checked
      } else if (service === 'Lb') {
        this.lbServiceChecked = checked
        this.lbServiceProvider = checked ? provider : ''
        if (checked && provider === 'Netscaler') {
          this.fetchRegisteredServicePackages()
        }
      } else if (service === 'Firewall') {
        this.firewallServiceChecked = checked
        this.firewallServiceProvider = checked ? provider : ''
      } else if (service === 'StaticNat') {
        this.staticNatServiceChecked = checked
        this.staticNatServiceProvider = checked ? provider : ''
      } else if (service === 'Connectivity') {
        this.connectivityServiceChecked = checked
      }

      this.checkVirtualRouterForServices()
    },
    handleSubmit (e) {
      if (e) {
        e.preventDefault()
      }
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        const params = {
          sourceofferingid: this.resource.id,
          name: values.name
        }

        if (values.displaytext) {
          params.displaytext = values.displaytext
        }

        const forNsx = values.provider === 'NSX'
        if (forNsx) {
          params.provider = 'NSX'
          params.nsxsupportlb = values.nsxsupportlb
          if ('nsxsupportsinternallb' in values) {
            params.nsxsupportsinternallb = values.nsxsupportsinternallb
          }
        }
        const forNetris = values.provider === 'Netris'
        if (forNetris) {
          params.provider = 'Netris'
        }

        if (values.guestiptype) {
          params.guestiptype = values.guestiptype
        }

        // Use composable for service capability params
        if (this.selectedServiceProviderMap != null) {
          buildServiceCapabilityParams(params, values, this.selectedServiceProviderMap, this.registeredServicePackages)
        } else {
          if (!('supportedservices' in params)) {
            params.supportedservices = ''
          }
        }

        if (values.guestiptype === 'l2' && values.userdatal2 === true) {
          params.supportedservices = 'UserData'
        }

        if (values.availability) {
          params.availability = values.availability
        }
        if (values.tags) {
          params.tags = values.tags
        }
        if (values.specifyvlan !== undefined) {
          params.specifyvlan = values.specifyvlan
        }
        if (values.conservemode !== undefined) {
          params.conservemode = values.conservemode
        }
        if (values.ispersistent !== undefined) {
          params.ispersistent = values.ispersistent
        }
        if (values.specifyipranges !== undefined) {
          params.specifyipranges = values.specifyipranges
        }
        if (values.egressdefaultpolicy !== undefined) {
          params.egressdefaultpolicy = values.egressdefaultpolicy
        }
        if (values.networkrate) {
          params.networkrate = values.networkrate
        }
        if (values.serviceofferingid) {
          params.serviceofferingid = values.serviceofferingid
        }
        if (values.enable !== undefined) {
          params.enable = values.enable
        }

        if (values.ispublic !== true) {
          const domainIndexes = values.domainid
          let domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            const domainIds = []
            for (let i = 0; i < domainIndexes.length; i++) {
              domainIds.push(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        if (values.networkmode) {
          params.networkmode = values.networkmode
        }

        const zoneIndexes = values.zoneid
        let zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          const zoneIds = []
          for (let j = 0; j < zoneIndexes.length; j++) {
            zoneIds.push(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }

        params.traffictype = this.resource.traffictype || 'GUEST'

        let detailsIndex = 0
        if (values.promiscuousmode && values.promiscuousmode !== '') {
          params['details[' + detailsIndex + '].key'] = 'promiscuousmode'
          params['details[' + detailsIndex + '].value'] = values.promiscuousmode
          detailsIndex++
        }
        if (values.macaddresschanges && values.macaddresschanges !== '') {
          params['details[' + detailsIndex + '].key'] = 'macaddresschanges'
          params['details[' + detailsIndex + '].value'] = values.macaddresschanges
          detailsIndex++
        }
        if (values.forgedtransmits && values.forgedtransmits !== '') {
          params['details[' + detailsIndex + '].key'] = 'forgedtransmits'
          params['details[' + detailsIndex + '].value'] = values.forgedtransmits
          detailsIndex++
        }
        if (values.maclearning && values.maclearning !== '') {
          params['details[' + detailsIndex + '].key'] = 'maclearning'
          params['details[' + detailsIndex + '].value'] = values.maclearning
          detailsIndex++
        }

        this.loading = true
        postAPI('cloneNetworkOffering', params).then(json => {
          this.$message.success(`${this.$t('message.success.clone.network.offering')} ${values.name}`)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }

  .supported-services-container {
    height: 250px;
    overflow-y: scroll;
  }
</style>
