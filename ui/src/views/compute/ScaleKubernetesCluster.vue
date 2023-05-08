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
      <a-alert type="warning">
        <template #message>{{ resource.autoscalingenabled ? $t('message.action.scale.kubernetes.cluster.warning') : $t('message.kubernetes.cluster.scale') }}</template>
      </a-alert>
      <br />
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="autoscalingenabled" ref="autoscalingenabled" v-if="apiParams.autoscalingenabled">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.autoscalingenabled')" :tooltip="apiParams.autoscalingenabled.description"/>
          </template>
          <a-switch :checked="autoscalingenabled" @change="val => { autoscalingenabled = val }" />
        </a-form-item>
        <span v-if="autoscalingenabled">
          <a-form-item name="minsize" ref="minsize">
            <template #label>
              <tooltip-label :title="$t('label.cks.cluster.minsize')" :tooltip="apiParams.minsize.description"/>
            </template>
            <a-input
              v-model:value="form.minsize"
              :placeholder="apiParams.minsize.description"/>
          </a-form-item>
          <a-form-item name="maxsize" ref="maxsize">
            <template #label>
              <tooltip-label :title="$t('label.cks.cluster.maxsize')" :tooltip="apiParams.maxsize.description"/>
            </template>
            <a-input
              v-model:value="form.maxsize"
              :placeholder="apiParams.maxsize.description"/>
          </a-form-item>
        </span>
        <span v-else>
          <a-form-item name="serviceofferingid" ref="serviceofferingid">
            <template #label>
              <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
            </template>
            <a-select
              id="offering-selection"
              v-model:value="form.serviceofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="serviceOfferingLoading"
              :placeholder="apiParams.serviceofferingid.description">
              <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="size" ref="size">
            <template #label>
              <tooltip-label :title="$t('label.cks.cluster.size')" :tooltip="apiParams.size.description"/>
            </template>
            <a-input
              v-model:value="form.size"
              :placeholder="apiParams.size.description"/>
          </a-form-item>
        </span>
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
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ScaleKubernetesCluster',
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
      serviceOfferings: [],
      serviceOfferingLoading: false,
      minCpu: 2,
      minMemory: 2048,
      loading: false,
      originalSize: 1,
      autoscalingenabled: null,
      minsize: null,
      maxsize: null
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('scaleKubernetesCluster')
  },
  created () {
    this.originalSize = !this.isObjectEmpty(this.resource) ? this.resource.size : 1
    if (!this.isObjectEmpty(this.resource)) {
      this.originalSize = this.resource.size
      if (this.apiParams.autoscalingenabled) {
        this.autoscalingenabled = this.resource.autoscalingenabled ? true : null
        this.minsize = this.resource.minsize
        this.maxsize = this.resource.maxsize
      }
    }
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        size: this.originalSize,
        minsize: this.minsize,
        maxsize: this.maxsize
      })
      this.rules = reactive({
        minsize: [{ type: 'number', validator: this.validateNumber }],
        maxsize: [{ type: 'number', validator: this.validateNumber }],
        size: [{ type: 'number', validator: this.validateNumber }]
      })
    },
    fetchData () {
      this.fetchKubernetesVersionData()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    fetchKubernetesVersionData () {
      const params = {}
      if (!this.isObjectEmpty(this.resource)) {
        params.id = this.resource.kubernetesversionid
      }
      api('listKubernetesSupportedVersions', params).then(json => {
        const versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion
        if (this.arrayHasItems(versionObjs) && !this.isObjectEmpty(versionObjs[0])) {
          this.minCpu = versionObjs[0].mincpunumber
          this.minMemory = versionObjs[0].minmemory
        }
      }).finally(() => {
        this.fetchServiceOfferingData()
      })
    },
    fetchServiceOfferingData () {
      this.serviceOfferings = []
      const params = {}
      this.serviceOfferingLoading = true
      api('listServiceOfferings', params).then(json => {
        var items = json.listserviceofferingsresponse.serviceoffering
        if (items != null) {
          for (var i = 0; i < items.length; i++) {
            if (items[i].iscustomized === false &&
                items[i].cpunumber >= this.minCpu && items[i].memory >= this.minMemory) {
              this.serviceOfferings.push(items[i])
            }
          }
        }
      }).finally(() => {
        this.serviceOfferingLoading = false
        if (this.arrayHasItems(this.serviceOfferings)) {
          for (var i = 0; i < this.serviceOfferings.length; i++) {
            if (this.serviceOfferings[i].id === this.resource.serviceofferingid) {
              this.form.serviceofferingid = i
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
        const params = {
          id: this.resource.id
        }
        if (this.autoscalingenabled != null) {
          params.autoscalingenabled = this.autoscalingenabled
        }
        if (this.isValidValueForKey(values, 'size') && values.size > 0) {
          params.size = values.size
        }
        if (this.isValidValueForKey(values, 'serviceofferingid') && this.arrayHasItems(this.serviceOfferings) && this.autoscalingenabled == null) {
          params.serviceofferingid = this.serviceOfferings[values.serviceofferingid].id
        }
        if (this.isValidValueForKey(values, 'minsize')) {
          params.minsize = values.minsize
        }
        if (this.isValidValueForKey(values, 'maxsize')) {
          params.maxsize = values.maxsize
        }
        api('scaleKubernetesCluster', params).then(json => {
          const jobId = json.scalekubernetesclusterresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.kubernetes.cluster.scale'),
            description: this.resource.name,
            loadingMessage: `${this.$t('label.kubernetes.cluster.scale')} ${this.resource.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.scale.kubernetes')} ${this.resource.name}`
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
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.validate.number'))
      }
      return Promise.resolve()
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
