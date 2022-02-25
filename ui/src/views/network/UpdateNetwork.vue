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
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: `${$t('message.error.required.input')}` }]
            }]"
            :placeholder="apiParams.displaytext.description"
            autoFocus />
        </a-form-item>
        <a-form-item v-if="isUpdatingIsolatedNetwork">
          <tooltip-label slot="label" :title="$t('label.networkofferingid')" :tooltip="apiParams.networkofferingid.description"/>
          <span v-if="networkOffering.id && networkOffering.id != resource.networkofferingid">
            <a-alert type="warning">
              <span slot="message" v-html="$t('message.network.offering.change.warning')" />
            </a-alert>
            <br/>
          </span>
          <a-select
            id="offering-selection"
            v-decorator="['networkofferingid', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="networkOfferingLoading"
            :placeholder="apiParams.networkofferingid.description"
            @change="val => { networkOffering = networkOfferings[val] }">
            <a-select-option v-for="(opt, optIndex) in networkOfferings" :key="optIndex">
              {{ opt.displaytext || opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.guestvmcidr')" :tooltip="apiParams.guestvmcidr.description"/>
          <a-input
            v-decorator="['guestvmcidr', {}]"
            :placeholder="apiParams.guestvmcidr.description"
            @change="(e) => { cidrChanged = e.target.value !== resource.cidr }" />
        </a-form-item>
        <a-form-item v-if="cidrChanged">
          <tooltip-label slot="label" :title="$t('label.changecidr')" :tooltip="apiParams.changecidr.description"/>
          <a-switch v-decorator="['changecidr', {}]" />
        </a-form-item>
        <a-form-item v-if="isUpdatingIsolatedNetwork">
          <tooltip-label slot="label" :title="$t('label.networkdomain')" :tooltip="apiParams.guestvmcidr.description"/>
          <a-input
            v-decorator="['networkdomain', {}]"
            :placeholder="apiParams.networkdomain.description"
            autoFocus />
        </a-form-item>
        <a-form-item v-if="resource.redundantrouter">
          <tooltip-label slot="label" :title="$t('label.updateinsequence')" :tooltip="apiParams.updateinsequence.description"/>
          <a-switch v-decorator="['maclearning', {initialValue: false}]" />
        </a-form-item>
        <a-form-item v-if="isAdmin()">
          <tooltip-label slot="label" :title="$t('label.displaynetwork')" :tooltip="apiParams.displaynetwork.description"/>
          <a-switch v-decorator="['displaynetwork', {}]" :defaultChecked="resource.displaynetwork" />
        </a-form-item>
        <a-form-item v-if="isAdmin()">
          <tooltip-label slot="label" :title="$t('label.forced')" :tooltip="apiParams.forced.description"/>
          <a-switch v-decorator="['forced', {}]" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import { isAdmin } from '@/role'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'UpdateNetwork',
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
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('updateNetwork')
  },
  created () {
    this.resourceValues = {
      name: this.resource.name,
      displaytext: this.resource.displaytext,
      guestvmcidr: this.resource.cidr
    }
    if (this.isUpdatingIsolatedNetwork) {
      this.resourceValues.networkdomain = this.resource.networkdomain
    }
    for (var field in this.resourceValues) {
      var fieldValue = this.resourceValues[field]
      if (fieldValue) {
        this.form.getFieldDecorator(field, { initialValue: fieldValue })
      }
    }
    this.fetchData()
  },
  computed: {
    isUpdatingIsolatedNetwork () {
      return this.resource && this.resource.type === 'Isolated'
    }
  },
  methods: {
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
      if (!this.isUpdatingIsolatedNetwork) return
      const params = {
        zoneid: this.resource.zoneid,
        state: 'Enabled',
        guestiptype: this.resource.type,
        forvpc: !!this.resource.vpcid
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
              this.form.setFieldsValue({
                networkofferingid: i
              })
              break
            }
          }
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
