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
  <a-spin :spinning="loading || formLoading">
    <a-form
      :form="form"
      @submit="handleSubmit"
      layout="vertical"
    >
      <a-form-item
        v-for="(item, index) in dataResource"
        v-if="dataSource.includes(item.resourcetypename)"
        :key="index"
        :v-bind="item.resourcetypename"
        :label="$t('max' + item.resourcetypename)">
        <a-input-number
          style="width: 100%;"
          v-decorator="[item.resourcetype, {
            initialValue: item.max
          }]"
          :placeholder="$t('project.' + item.resourcetypename + '.description')"
        />
      </a-form-item>
      <div class="card-footer">
        <!-- ToDo extract as component -->
        <a-button :loading="formLoading" type="primary" @click="handleSubmit">{{ this.$t('apply') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ResourceTab',
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
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  data () {
    return {
      formLoading: false,
      dataResource: [],
      dataSource: []
    }
  },
  created () {
    this.dataSource = [
      'network',
      'volume',
      'public_ip',
      'template',
      'user_vm',
      'snapshot',
      'vpc', 'cpu',
      'memory',
      'primary_storage',
      'secondary_storage'
    ]
  },
  mounted () {
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
    fetchData () {
      const params = {}
      params.projectid = this.resource.id

      this.formLoading = true

      api('listResourceLimits', params).then(json => {
        if (json.listresourcelimitsresponse.resourcelimit) {
          this.dataResource = json.listresourcelimitsresponse.resourcelimit
        }
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        this.formLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()

      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        const arrAsync = []
        const params = {}
        params.projectid = this.resource.id

        // create parameter from form
        for (const key in values) {
          const input = values[key]

          if (input === undefined) {
            continue
          }

          params.resourcetype = key
          params.max = input

          arrAsync.push(new Promise((resolve, reject) => {
            api('updateResourceLimit', params).then(json => {
              resolve()
            }).catch(error => {
              reject(error)
            })
          }))
        }

        this.formLoading = true

        Promise.all(arrAsync).then(() => {
          this.$message.success('Apply Successful')
          this.fetchData()
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: error.response.headers['x-description']
          })
        }).finally(() => {
          this.formLoading = false
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .card-footer {
    text-align: right;

    button + button {
      margin-left: 8px;
    }
  }
</style>
