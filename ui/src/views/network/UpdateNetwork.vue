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
        @finish="handleSubmit"
        layout="vertical"
       >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="apiParams.displaytext.description"
            autoFocus />
        </a-form-item>
        <a-form-item name="networkofferingid" ref="networkofferingid" v-if="isUpdatingIsolatedNetwork">
          <template #label>
            <tooltip-label :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
          </template>
          <span v-if="networkOffering.id && networkOffering.id !== resource.networkofferingid">
            <a-alert type="warning">
              <template #message>
                <span v-html="$t('message.network.offering.change.warning')" />
              </template>
            </a-alert>
            <br/>
          </span>
          <a-select
            id="offering-selection"
            v-model:value="form.networkofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="networkOfferingLoading"
            :placeholder="apiParams.networkofferingid.description"
            @change="val => { networkOffering = networkOfferings[val] }">
            <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex">
              {{ opt.displaytext || opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="guestvmcidr" ref="guestvmcidr">
          <template #label>
            <tooltip-label :title="$t('label.guestvmcidr')" :tooltip="apiParams.guestvmcidr.description"/>
          </template>
          <a-input
            v-model:value="form.guestvmcidr"
            :placeholder="apiParams.guestvmcidr.description"
            @change="(e) => { cidrChanged = e.target.value !== resource.cidr }" />
        </a-form-item>
        <a-form-item name="changecidr" ref="changecidr" v-if="cidrChanged">
          <template #label>
            <tooltip-label :title="$t('label.changecidr')" :tooltip="apiParams.changecidr.description"/>
          </template>
          <a-switch v-model:checked="form.changecidr" />
        </a-form-item>
        <a-form-item name="networkdomain" ref="networkdomain" v-if="isUpdatingIsolatedNetwork">
          <template #label>
            <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.guestvmcidr.description"/>
          </template>
          <a-input
            v-model:value="form.networkdomain"
            :placeholder="apiParams.networkdomain.description"
            autoFocus />
        </a-form-item>
        <a-form-item name="updateinsequence" ref="updateinsequence" v-if="resource.redundantrouter">
          <template #label>
            <tooltip-label :title="$t('label.updateinsequence')" :tooltip="apiParams.updateinsequence.description"/>
          </template>
          <a-switch v-model:checked="form.updateinsequence" />
        </a-form-item>
        <div v-if="selectedNetworkOfferingSupportsDns">
          <a-row :gutter="12">
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
          <a-row :gutter="12">
            <a-col :md="12" :lg="12">
              <a-form-item v-if="networkOffering && ((networkOffering.guestiptype === 'Isolated' && networkOffering.internetprotocol === 'DualStack') || (networkOffering.guestiptype === 'Shared' && resource.ip6cidr)) && 'ip6dns1' in apiParams" name="ip6dns1" ref="ip6dns1">
                <template #label>
                  <tooltip-label :title="$t('label.ip6dns1')" :tooltip="apiParams.ip6dns1.description"/>
                </template>
                <a-input
                  v-model:value="form.ip6dns1"
                  :placeholder="apiParams.ip6dns1.description"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :lg="12">
              <a-form-item v-if="networkOffering && ((networkOffering.guestiptype === 'Isolated' && networkOffering.internetprotocol === 'DualStack') || (networkOffering.guestiptype === 'Shared' && resource.ip6cidr)) && 'ip6dns1' in apiParams" name="ip6dns2" ref="ip6dns2">
                <template #label>
                  <tooltip-label :title="$t('label.ip6dns2')" :tooltip="apiParams.ip6dns2.description"/>
                </template>
                <a-input
                  v-model:value="form.ip6dns2"
                  :placeholder="apiParams.ip6dns2.description"/>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <a-form-item name="displaynetwork" ref="displaynetwork" v-if="isAdmin()">
          <template #label>
            <tooltip-label :title="$t('label.displaynetwork')" :tooltip="apiParams.displaynetwork.description"/>
          </template>
          <a-switch v-model:checked="form.displaynetwork" />
        </a-form-item>
        <a-form-item name="forced" ref="forced" v-if="isAdmin()">
          <template #label>
            <tooltip-label :title="$t('label.forced')" :tooltip="apiParams.forced.description"/>
          </template>
          <a-switch v-model:checked="form.forced" />
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
import { api } from '@/api'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'UpdateNetwork',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      resourceValues: {},
      networkOfferings: [],
      networkOfferingLoading: false,
      networkOffering: {},
      cidrChanged: false,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateNetwork')
  },
  created () {
    this.initForm()
    this.resourceValues = {
      name: this.resource.name,
      displaytext: this.resource.displaytext,
      guestvmcidr: this.resource.cidr,
      dns1: this.resource.dns1,
      dns2: this.resource.dns2,
      ip6dns1: this.resource.ip6dns1,
      ip6dns2: this.resource.ip6dns2
    }
    if (this.isUpdatingIsolatedNetwork) {
      this.resourceValues.networkdomain = this.resource.networkdomain
    }
    for (var field in this.resourceValues) {
      var fieldValue = this.resourceValues[field]
      if (fieldValue) {
        this.form[field] = fieldValue
      }
    }
    this.fetchData()
  },
  computed: {
    isUpdatingIsolatedNetwork () {
      return this.resource && this.resource.type === 'Isolated'
    },
    selectedNetworkOfferingSupportsDns () {
      if (this.networkOffering) {
        const services = this.networkOffering?.service || []
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
        displaynetwork: this.resource.displaynetwork
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        displaytext: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      this.fetchNetworkOfferingData()
    },
    isAdmin () {
      return isAdmin()
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchNetworkOfferingData () {
      this.networkOfferings = []
      const params = {
        zoneid: this.resource.zoneid,
        state: 'Enabled',
        guestiptype: this.resource.type,
        forvpc: !!this.resource.vpcid
      }
      if (!this.isUpdatingIsolatedNetwork) {
        params.id = this.resource.networkofferingid
      }
      this.networkOfferingLoading = true
      api('listNetworkOfferings', params).then(json => {
        this.networkOfferings = json.listnetworkofferingsresponse.networkoffering
      }).finally(() => {
        this.networkOfferingLoading = false
        if (this.arrayHasItems(this.networkOfferings)) {
          for (var i = 0; i < this.networkOfferings.length; i++) {
            if (this.networkOfferings[i].id === this.resource.networkofferingid) {
              this.networkOffering = this.networkOfferings[i]
              this.form.networkofferingid = i
              break
            }
          }
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.loading = true
        var manualFields = ['name', 'networkofferingid']
        const params = {
          id: this.resource.id,
          name: values.name
        }
        for (var field in values) {
          if (manualFields.includes(field)) continue
          var fieldValue = values[field]
          if (fieldValue !== undefined &&
            fieldValue !== null &&
            (!(field in this.resourceValues) || this.resourceValues[field] !== fieldValue)) {
            params[field] = fieldValue
          }
        }
        if (values.networkofferingid !== undefined &&
          values.networkofferingid != null &&
          this.networkOfferings &&
          this.networkOfferings[values.networkofferingid].id !== this.resource.networkofferingid) {
          params.networkofferingid = this.networkOfferings[values.networkofferingid].id
        }
        api('updateNetwork', params).then(json => {
          const jobId = json.updatenetworkresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.update.network'),
            description: this.resource.name,
            loadingMessage: `${this.$t('label.update.network')} ${this.resource.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.update.network')} ${this.resource.name}`
          })
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
    width: 60vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }

</style>
