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
        v-if="checkExistFields(item.resourcetypename)"
        :key="index"
        :v-bind="item.resourcetypename"
        :label="getFieldLabel(item.resourcetypename)">
        <a-input-number
          style="width: 100%;"
          v-decorator="[item.resourcetype, {
            initialValue: item.max
          }]"
        />
      </a-form-item>
      <div class="card-footer">
        <a-button
          v-if="!disabled"
          :loading="formLoading"
          type="primary"
          @click="handleSubmit">{{ this.$t('apply') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ResourcesTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    params: {
      type: Object,
      default: () => {}
    },
    fields: {
      type: Array,
      default: () => []
    },
    disabled: {
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
    async fetchData () {
      const params = {}
      Object.keys(this.params).forEach((field) => {
        const resourceField = this.params[field]
        const fieldVal = this.resource[resourceField] ? this.resource[resourceField] : null
        params[field] = fieldVal
      })
      try {
        this.formLoading = true
        this.dataResource = await this.listResourceLimits(params)
        this.formLoading = false
      } catch (e) {
        this.$notification.error({
          message: 'Request Failed',
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
        const params = {}
        Object.keys(this.params).forEach((field) => {
          const resourceField = this.params[field]
          const fieldVal = this.resource[resourceField] ? this.resource[resourceField] : null
          params[field] = fieldVal
        })

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
    },
    listResourceLimits (params) {
      return new Promise((resolve, reject) => {
        let dataResource = []
        api('listResourceLimits', params).then(json => {
          if (json.listresourcelimitsresponse.resourcelimit) {
            dataResource = json.listresourcelimitsresponse.resourcelimit
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
    },
    checkExistFields (resourcetypename) {
      const fieldExists = this.fields.filter((item) => item.field === resourcetypename)
      if (fieldExists && fieldExists.length > 0) {
        return true
      }

      return false
    },
    getFieldLabel (resourcetypename) {
      const field = this.fields.filter((item) => item.field === resourcetypename)
      if (field && field.length > 0) {
        return this.$t(field[0].title)
      }

      return resourcetypename
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
