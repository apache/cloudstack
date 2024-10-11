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
        <template #message>{{ $t('message.kubernetes.cluster.upgrade') }}</template>
      </a-alert>
      <br />
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="kubernetesversionid" ref="kubernetesversionid">
          <template #label>
            <tooltip-label :title="$t('label.kubernetesversionid')" :tooltip="apiParams.kubernetesversionid.description"/>
          </template>
          <a-select
            id="version-selection"
            v-model:value="form.kubernetesversionid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="kubernetesVersionLoading"
            :placeholder="apiParams.kubernetesversionid.description"
            v-focus="true" >
            <a-select-option v-for="(opt, optIndex) in this.kubernetesVersions" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
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
  name: 'UpgradeKubernetesCluster',
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
      kubernetesVersions: [],
      kubernetesVersionLoading: false,
      minCpu: 2,
      minMemory: 2048,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('upgradeKubernetesCluster')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        kubernetesversionid: [{ required: true }]
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
      this.kubernetesVersions = []
      const params = {}
      if (!this.isObjectEmpty(this.resource)) {
        params.minimumkubernetesversionid = this.resource.kubernetesversionid
      }
      this.kubernetesVersionLoading = true
      api('listKubernetesSupportedVersions', params).then(json => {
        const versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion
        if (this.arrayHasItems(versionObjs)) {
          var clusterVersion = null
          for (var j = 0; j < versionObjs.length; j++) {
            if (versionObjs[j].id === this.resource.kubernetesversionid) {
              clusterVersion = versionObjs[j]
              break
            }
          }
          for (var i = 0; i < versionObjs.length; i++) {
            if (versionObjs[i].id !== this.resource.kubernetesversionid &&
              (clusterVersion == null || (clusterVersion != null && versionObjs[i].semanticversion !== clusterVersion.semanticversion)) &&
              versionObjs[i].state === 'Enabled' && versionObjs[i].isostate === 'Ready') {
              this.kubernetesVersions.push({
                id: versionObjs[i].id,
                description: versionObjs[i].name
              })
            }
          }
        }
      }).finally(() => {
        this.kubernetesVersionLoading = false
        if (this.arrayHasItems(this.kubernetesVersions)) {
          this.form.kubernetesversionid = 0
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          id: this.resource.id
        }
        if (this.isValidValueForKey(values, 'kubernetesversionid') && this.arrayHasItems(this.kubernetesVersions)) {
          params.kubernetesversionid = this.kubernetesVersions[values.kubernetesversionid].id
        }
        api('upgradeKubernetesCluster', params).then(json => {
          this.$emit('refresh-data')
          const jobId = json.upgradekubernetesclusterresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.kubernetes.cluster.upgrade'),
            description: this.resource.name,
            loadingMessage: `${this.$t('label.kubernetes.cluster.upgrade')} ${this.resource.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.upgrade.kubernetes')} ${this.resource.name}`
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
