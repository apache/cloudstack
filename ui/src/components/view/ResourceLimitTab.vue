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
  <a-spin :spinning="formLoading">
    <a-form
      :form="form"
      @submit="handleSubmit"
      layout="vertical"
    >
      <a-form-item
        v-for="(item, index) in dataResource"
        :key="index"
        v-if="item.resourcetypename !== 'project'"
        :v-bind="item.resourcetypename"
        :label="$t('label.max' + item.resourcetypename.replace('_', ''))">
        <a-input-number
          :disabled="!('updateResourceLimit' in $store.getters.apis)"
          style="width: 100%;"
          v-decorator="[item.resourcetype, {
            initialValue: item.max
          }]"
          :autoFocus="index === 0"
        />
      </a-form-item>
      <div class="card-footer">
        <a-button
          :disabled="!('updateResourceLimit' in $store.getters.apis)"
          v-if="!($route.meta.name === 'domain' && resource.level === 0)"
          :loading="formLoading"
          type="primary"
          @click="handleSubmit">{{ $t('label.submit') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ResourceLimitTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formLoading: false,
      dataResource: []
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource (newData, oldData) {
      if (!newData || !newData.id) {
        return
      }
      this.resource = newData
      this.fetchData()
    }
  },
  methods: {
    getParams () {
      const params = {}
      if (this.$route.meta.name === 'account') {
        params.account = this.resource.name
        params.domainid = this.resource.domainid
      } else if (this.$route.meta.name === 'domain') {
        params.domainid = this.resource.id
      } else { // project
        params.projectid = this.resource.id
      }
      return params
    },
    async fetchData () {
      const params = this.getParams()
      try {
        this.formLoading = true
        this.dataResource = await this.listResourceLimits(params)
        this.formLoading = false
      } catch (e) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: e
        })
        this.formLoading = false
      }
    },
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const arrAsync = []
        const params = this.getParams()
        for (const key in values) {
          const input = values[key]

          if (input === undefined) {
            continue
          }
          params.resourcetype = key
          params.max = input
          arrAsync.push(this.updateResourceLimit(params))
        }

        this.formLoading = true

        Promise.all(arrAsync).then(() => {
          this.$message.success(this.$t('message.apply.success'))
          this.fetchData()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.formLoading = false
        })
      })
    },
    listResourceLimits (params) {
      return new Promise((resolve, reject) => {
        let dataResource = []
        api('listResourceLimits', params).then(json => {
          if (json.listresourcelimitsresponse.resourcelimit) {
            dataResource = json.listresourcelimitsresponse.resourcelimit
            dataResource.sort((a, b) => a.resourcetype - b.resourcetype)
          }
          resolve(dataResource)
        }).catch(error => {
          reject(error.response.headers['x-description'])
        })
      })
    },
    updateResourceLimit (params) {
      return new Promise((resolve, reject) => {
        api('updateResourceLimit', params).then(json => {
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .card-footer {
    button + button {
      margin-left: 8px;
    }
  }
</style>
