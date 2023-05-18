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
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
       >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            :loading="loadingZone"
            v-model:value="form.zoneid"
            @change="val => changeZone(val)"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="zone in zones" :key="zone.id" :label="zone.name">
              <span>
                <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ zone.name }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="cidr" ref="cidr">
          <template #label>
            <tooltip-label :title="$t('label.cidr')" :tooltip="apiParams.cidr.description"/>
          </template>
          <a-input
            v-model:value="form.cidr"
            :placeholder="apiParams.cidr.description"/>
        </a-form-item>
        <a-form-item name="networkdomain" ref="networkdomain">
          <template #label>
            <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
          </template>
          <a-input
            v-model:value="form.networkdomain"
            :placeholder="apiParams.networkdomain.description"/>
        </a-form-item>
        <a-form-item name="vpcofferingid" ref="vpcofferingid">
          <template #label>
            <tooltip-label :title="$t('label.vpcofferingid')" :tooltip="apiParams.vpcofferingid.description"/>
          </template>
          <a-select
            :loading="loadingOffering"
            v-model:value="form.vpcofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @change="handleVpcOfferingChange" >
            <a-select-option :value="offering.id" v-for="offering in vpcOfferings" :key="offering.id" :label="offering.name">
              {{ offering.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="setMTU">
          <a-form-item
            ref="publicmtu"
            name="publicmtu">
            <template #label>
              <tooltip-label :title="$t('label.publicmtu')" :tooltip="apiParams.publicmtu.description"/>
            </template>
            <a-input-number
              style="width: 100%;"
              v-model:value="form.publicmtu"
              :placeholder="apiParams.publicmtu.description"
              @change="updateMtu()"/>
              <div style="color: red" v-if="errorPublicMtu" v-html="errorPublicMtu"></div>
          </a-form-item>
        </div>
        <a-row :gutter="12" v-if="selectedVpcOfferingSupportsDns">
          <a-col :md="12" :lg="12">
            <a-form-item v-if="'dns1' in apiParams" name="dns1" ref="dns1">
              <template #label>
                <tooltip-label :title="$t('label.dns1')" :tooltip="apiParams.dns1.description"/>
              </template>
              <a-input
                v-model:value="form.dns1"
                :placeholder="apiParams.dns1.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item v-if="'dns2' in apiParams" name="dns2" ref="dns2">
              <template #label>
                <tooltip-label :title="$t('label.dns2')" :tooltip="apiParams.dns2.description"/>
              </template>
              <a-input
                v-model:value="form.dns2"
                :placeholder="apiParams.dns2.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12" v-if="selectedVpcOfferingSupportsDns">
          <a-col :md="12" :lg="12">
            <a-form-item v-if="selectedVpcOffering && selectedVpcOffering.internetprotocol === 'DualStack' && 'ip6dns1' in apiParams" name="ip6dns1" ref="ip6dns1">
              <template #label>
                <tooltip-label :title="$t('label.ip6dns1')" :tooltip="apiParams.ip6dns1.description"/>
              </template>
              <a-input
                v-model:value="form.ip6dns1"
                :placeholder="apiParams.ip6dns1.description"/>
            </a-form-item>
          </a-col>
          <a-col :md="12" :lg="12">
            <a-form-item v-if="selectedVpcOffering && selectedVpcOffering.internetprotocol === 'DualStack' && 'ip6dns2' in apiParams" name="ip6dns2" ref="ip6dns2">
              <template #label>
                <tooltip-label :title="$t('label.ip6dns2')" :tooltip="apiParams.ip6dns2.description"/>
              </template>
              <a-input
                v-model:value="form.ip6dns2"
                :placeholder="apiParams.ip6dns2.description"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="start" ref="start">
          <template #label>
            <tooltip-label :title="$t('label.start')" :tooltip="apiParams.start.description"/>
          </template>
          <a-switch v-model:checked="form.start" />
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
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVpc',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      loadingZone: false,
      loadingOffering: false,
      setMTU: false,
      zoneid: '',
      zones: [],
      vpcOfferings: [],
      publicMtuMax: 1500,
      minMTU: 68,
      errorPublicMtu: '',
      selectedVpcOffering: {}
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVPC')
  },
  created () {
    this.initForm()
    this.fetchData()
    console.log(this.setMTU)
  },
  computed: {
    selectedVpcOfferingSupportsDns () {
      if (this.selectedVpcOffering) {
        const services = this.selectedVpcOffering?.service || []
        const dnsServices = services.filter(service => service.name === 'Dns')
        return dnsServices && dnsServices.length === 1
      }
      return false
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        start: true
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        zoneid: [{ required: true, message: this.$t('label.required') }],
        cidr: [{ required: true, message: this.$t('message.error.required.input') }],
        vpcofferingid: [{ required: true, message: this.$t('label.required') }]
      })
    },
    async fetchData () {
      this.fetchZones()
    },
    fetchPublicMtuForZone () {
      api('listConfigurations', {
        name: 'vr.public.interface.mtu',
        zoneid: this.form.zoneid
      }).then(json => {
        this.publicMtuMax = json?.listconfigurationsresponse?.configuration[0]?.value || 1500
      })
    },
    fetchZones () {
      this.loadingZone = true
      api('listZones', { showicon: true }).then((response) => {
        const listZones = response.listzonesresponse.zone || []
        this.zones = listZones.filter(zone => !zone.securitygroupsenabled)
        this.form.zoneid = ''
        if (this.zones.length > 0) {
          this.form.zoneid = this.zones[0].id
          this.changeZone(this.form.zoneid)
        }
      }).finally(() => {
        this.loadingZone = false
      })
    },
    changeZone (value) {
      this.form.zoneid = value
      if (this.form.zoneid === '') {
        this.form.vpcofferingid = ''
        return
      }
      for (var zone of this.zones) {
        if (zone.id === value) {
          this.setMTU = zone?.allowuserspecifyvrmtu || false
          this.publicMtuMax = zone?.routerpublicinterfacemaxmtu || 1500
        }
      }
      this.fetchOfferings()
    },
    fetchOfferings () {
      this.loadingOffering = true
      api('listVPCOfferings', { zoneid: this.form.zoneid, state: 'Enabled' }).then((response) => {
        this.vpcOfferings = response.listvpcofferingsresponse.vpcoffering
        this.form.vpcofferingid = this.vpcOfferings[0].id || ''
        this.selectedVpcOffering = this.vpcOfferings[0] || {}
      }).finally(() => {
        this.loadingOffering = false
      })
    },
    handleVpcOfferingChange (value) {
      this.selectedVpcOffering = {}
      if (!value) {
        return
      }
      for (var offering of this.vpcOfferings) {
        if (offering.id === value) {
          this.selectedVpcOffering = offering
          return
        }
      }
    },
    closeAction () {
      this.$emit('close-action')
    },
    updateMtu () {
      if (this.form.publicmtu > this.publicMtuMax) {
        this.errorPublicMtu = `${this.$t('message.error.mtu.public.max.exceed').replace('%x', this.publicMtuMax)}`
        this.form.publicmtu = this.publicMtuMax
      } else if (this.form.publicmtu < this.minMTU) {
        this.errorPublicMtu = `${this.$t('message.error.mtu.below.min').replace('%x', this.minMTU)}`
        this.form.publicmtu = this.minMTU
      } else {
        this.errorPublicMtu = ''
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (input === '' || input === null || input === undefined) {
            continue
          }
          params[key] = input
        }
        this.loading = true
        const title = this.$t('label.add.vpc')
        const description = this.$t('message.success.add.vpc.network')
        api('createVPC', params).then(json => {
          const jobId = json.createvpcresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description,
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
          }
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }
}

.form {
  margin: 10px 0;
}
</style>
