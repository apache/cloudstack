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
      <a-form :form="form" :loading="loading" @submit="handleSubmit" layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.traffictype')" :tooltip="apiParams.id.description"/>
          <a-select
            autoFocus
            v-decorator="['id', {
              initialValue: selectedType,
              rules: [{ required: true, message: $t('message.error.select') }] }]"
            :loading="typeLoading"
            :placeholder="apiParams.id.description"
            @change="onChangeTrafficType">
            <a-select-option v-for="type in trafficTypes" :key="type.id">
              {{ type.traffictype }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.kvmnetworklabel')" :tooltip="apiParams.kvmnetworklabel.description"/>
          <a-input
            v-decorator="['kvmnetworklabel', {
              initialValue: trafficResource.kvmnetworklabel
            }]"
            :placeholder="$t('label.network.label.display.for.blank.value')" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.vmwarenetworklabel')" :tooltip="apiParams.vmwarenetworklabel.description"/>
          <a-input
            v-decorator="['vmwarenetworklabel', {
              initialValue: trafficResource.vmwarenetworklabel
            }]"
            :placeholder="$t('label.network.label.display.for.blank.value')" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.xennetworklabel')" :tooltip="apiParams.xennetworklabel.description"/>
          <a-input
            v-decorator="['xennetworklabel', {
              initialValue: trafficResource.xennetworklabel
            }]"
            :placeholder="$t('label.network.label.display.for.blank.value')" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.hypervnetworklabel')" :tooltip="apiParams.hypervnetworklabel.description"/>
          <a-input
            v-decorator="['hypervnetworklabel', {
              initialValue: trafficResource.hypervnetworklabel
            }]"
            :placeholder="$t('label.network.label.display.for.blank.value')" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.ovm3networklabel')" :tooltip="apiParams.ovm3networklabel.description"/>
          <a-input
            v-decorator="['ovm3networklabel', {
              initialValue: trafficResource.ovm3networklabel
            }]"
            :placeholder="$t('label.network.label.display.for.blank.value')" />
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
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'EditTrafficLabel',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      loading: false,
      selectedType: undefined,
      typeLoading: false,
      traffictype: null,
      trafficTypes: [],
      trafficResource: {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('updateTrafficType')
  },
  inject: ['parentFetchData'],
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.typeLoading = true

      api('listTrafficTypes', { physicalnetworkid: this.resource.id })
        .then(json => {
          this.trafficTypes = json.listtraffictypesresponse.traffictype || []
          this.selectedType = this.trafficTypes[0].id || undefined
          this.trafficResource = this.trafficTypes[0] || {}
          this.traffictype = this.trafficTypes[0].traffictype || undefined
        })
        .catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
        })
        .finally(() => {
          this.typeLoading = false
        })
    },
    onChangeTrafficType (trafficId) {
      if (!trafficId) return
      this.trafficResource = this.trafficTypes.filter(item => item.id === trafficId)[0] || {}
      this.traffictype = this.trafficResource.traffictype || undefined
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {}
        for (const key in values) {
          params[key] = values[key]
        }
        const title = this.$t('label.update.traffic.label')
        const description = this.traffictype
        api('updateTrafficType', params).then(response => {
          this.$pollJob({
            jobId: response.updatetraffictyperesponse.jobid,
            title: title,
            description: description,
            successMessage: `${this.$t('label.update.traffic.label')} ${this.traffictype} ${this.$t('label.success')}`,
            loadingMessage: `${title} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
          this.closeAction()
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
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
