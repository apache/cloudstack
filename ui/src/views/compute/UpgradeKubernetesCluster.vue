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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('kubernetesversionid')">
          <a-select
            id="version-selection"
            v-decorator="['kubernetesversionid', {
              rules: [{ required: true }]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="kubernetesVersionLoading"
            :placeholder="apiParams.kubernetesversionid.description">
            <a-select-option v-for="(opt, optIndex) in this.kubernetesVersions" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'UpgradeKubernetesCluster',
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
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.upgradeKubernetesCluster || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
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
          this.form.setFieldsValue({
            kubernetesversionid: 0
          })
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          id: this.resource.id
        }
        if (this.isValidValueForKey(values, 'kubernetesversionid') && this.arrayHasItems(this.kubernetesVersions)) {
          params.kubernetesversionid = this.kubernetesVersions[values.kubernetesversionid].id
        }
        api('upgradeKubernetesCluster', params).then(json => {
          this.$message.success('Successfully upgraded Kubernetes cluster: ' + this.resource.name)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.$emit('refresh-data')
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
    width: 60vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
