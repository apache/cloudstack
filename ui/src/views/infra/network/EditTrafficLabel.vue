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
        :loading="loading"
        @finish="handleSubmit"
        layout="vertical"
       >
        <a-form-item name="id" ref="id">
          <template #label>
            <tooltip-label :title="$t('label.traffictype')" :tooltip="apiParams.id.description"/>
          </template>
          <a-select
            v-focus="true"
            v-model:value="form.id"
            :loading="typeLoading"
            :placeholder="apiParams.id.description"
            @change="onChangeTrafficType"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="type in trafficTypes" :key="type.id" :label="type.traffictype">
              {{ type.traffictype }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="kvmnetworklabel" ref="kvmnetworklabel">
          <template #label>
            <tooltip-label :title="$t('label.kvmnetworklabel')" :tooltip="apiParams.kvmnetworklabel.description"/>
          </template>
          <a-input
            v-model:value="form.kvmnetworklabel"
            :placeholder="apiParams.kvmnetworklabel.description" />
        </a-form-item>
        <a-form-item name="vmwarenetworklabel" ref="vmwarenetworklabel">
          <template #label>
            <tooltip-label :title="$t('label.vmwarenetworklabel')" :tooltip="apiParams.vmwarenetworklabel.description"/>
          </template>
          <a-input
            v-model:value="form.vmwarenetworklabel"
            :placeholder="apiParams.vmwarenetworklabel.description" />
        </a-form-item>
        <a-form-item name="xennetworklabel" ref="xennetworklabel">
          <template #label>
            <tooltip-label :title="$t('label.xennetworklabel')" :tooltip="apiParams.xennetworklabel.description"/>
          </template>
          <a-input
            v-model:value="form.xennetworklabel"
            :placeholder="apiParams.xennetworklabel.description" />
        </a-form-item>
        <a-form-item name="hypervnetworklabel" ref="hypervnetworklabel">
          <template #label>
            <tooltip-label :title="$t('label.hypervnetworklabel')" :tooltip="apiParams.hypervnetworklabel.description"/>
          </template>
          <a-input
            v-model:value="form.hypervnetworklabel"
            :placeholder="apiParams.hypervnetworklabel.description" />
        </a-form-item>
        <a-form-item name="ovm3networklabel" ref="ovm3networklabel">
          <template #label>
            <tooltip-label :title="$t('label.ovm3networklabel')" :tooltip="apiParams.ovm3networklabel.description"/>
          </template>
          <a-input
            v-model:value="form.ovm3networklabel"
            :placeholder="apiParams.ovm3networklabel.description" />
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
    this.apiParams = this.$getApiParams('updateTrafficType')
  },
  inject: ['parentFetchData'],
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        id: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fillEditFromFieldValues () {
      this.form.kvmnetworklabel = this.trafficResource.kvmnetworklabel
      this.form.vmwarenetworklabel = this.trafficResource.vmwarenetworklabel
      this.form.xennetworklabel = this.trafficResource.xennetworklabel
      this.form.hypervnetworklabel = this.trafficResource.hypervnetworklabel
      this.form.ovm3networklabel = this.trafficResource.ovm3networklabel
    },
    fetchData () {
      this.typeLoading = true

      api('listTrafficTypes', { physicalnetworkid: this.resource.id })
        .then(json => {
          this.trafficTypes = json.listtraffictypesresponse.traffictype || []
          this.form.id = this.trafficTypes[0].id || undefined
          this.trafficResource = this.trafficTypes[0] || {}
          this.traffictype = this.trafficTypes[0].traffictype || undefined
          this.fillEditFromFieldValues()
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
        }).finally(() => {
          this.typeLoading = false
        })
    },
    onChangeTrafficType (trafficId) {
      if (!trafficId) return
      this.trafficResource = this.trafficTypes.filter(item => item.id === trafficId)[0] || {}
      this.traffictype = this.trafficResource.traffictype || undefined
      this.fillEditFromFieldValues()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
            title,
            description,
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

  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
