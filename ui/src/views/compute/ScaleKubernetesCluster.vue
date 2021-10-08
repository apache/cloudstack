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
        <span
          slot="message"
          v-html="resource.autoscalingenabled ? $t('message.action.scale.kubernetes.cluster.warning') : $t('message.kubernetes.cluster.scale')" />
      </a-alert>
      <br />
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item v-if="apiParams.autoscalingenabled">
          <tooltip-label slot="label" :title="$t('label.cks.cluster.autoscalingenabled')" :tooltip="apiParams.autoscalingenabled.description"/>
          <a-switch :checked="this.autoscalingenabled" @change="val => { this.autoscalingenabled = val }" />
        </a-form-item>
        <span v-if="this.autoscalingenabled">
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.cks.cluster.minsize')" :tooltip="apiParams.minsize.description"/>
            <a-input
              v-decorator="['minsize', {
                initialValue: minsize,
                rules: [{
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback(this.$t('message.error.number'))
                    }
                    callback()
                  }
                }]
              }]"
              :placeholder="apiParams.minsize.description"/>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.cks.cluster.maxsize')" :tooltip="apiParams.maxsize.description"/>
            <a-input
              v-decorator="['maxsize', {
                initialValue: maxsize,
                rules: [{
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback(this.$t('message.error.number'))
                    }
                    callback()
                  }
                }]
              }]"
              :placeholder="apiParams.maxsize.description"/>
          </a-form-item>
        </span>
        <span v-else>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
            <a-select
              id="offering-selection"
              v-decorator="['serviceofferingid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="serviceOfferingLoading"
              :placeholder="apiParams.serviceofferingid.description">
              <a-select-option v-for="(opt, optIndex) in this.serviceOfferings" :key="optIndex">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.cks.cluster.size')" :tooltip="apiParams.size.description"/>
            <a-input
              v-decorator="['size', {
                initialValue: originalSize,
                rules: [{
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback(this.$t('message.error.number'))
                    }
                    callback()
                  }
                }]
              }]"
              :placeholder="apiParams.size.description"/>
          </a-form-item>
        </span>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ScaleKubernetesCluster',
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
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('scaleKubernetesCluster')
  },
  created () {
    if (!this.isObjectEmpty(this.resource)) {
      this.originalSize = this.resource.size
      if (this.apiParams.autoscalingenabled) {
        this.autoscalingenabled = this.resource.autoscalingenabled ? true : null
        this.minsize = this.resource.minsize
        this.maxsize = this.resource.maxsize
      }
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
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
              this.form.setFieldsValue({
                serviceofferingid: i
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
