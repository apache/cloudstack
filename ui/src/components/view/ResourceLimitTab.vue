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
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      layout="vertical"
      v-ctrl-enter="handleSubmit"
    >
      <div v-for="(item, index) in dataResource" :key="index">
        <a-form-item
          v-if="item.resourcetypename !== 'project'"
          :v-bind="item.resourcetypename"
          :label="$t('label.max' + (item.resourcetypename ? item.resourcetypename.replace('_', '') : '')) + (item.tag ? ' [' + item.tag + ']': '')"
          :name="item.key"
          :ref="item.key">
          <a-input-number
            :disabled="!('updateResourceLimit' in $store.getters.apis)"
            style="width: 100%;"
            v-model:value="form[item.key]"
            v-focus="index === 0"
          />
        </a-form-item>
        <a-collapse
            v-if="item.taggedresource && item.taggedresource.length > 0"
            class="tagged-limit-collapse"
            @change="handleCollapseChange(item.resourcetypename)">
          <a-collapse-panel key="1" :header="collpaseActive[item.resourcetypename] ? $t('label.tagged.limits') : $t('label.tagged.limits') + ' - ' + item.tagsasstring">
            <div v-for="(subItem, subItemIndex) in item.taggedresource" :key="subItemIndex">
              <a-form-item
                :v-bind="subItem.resourcetypename"
                :label="$t('label.max') + ' #' + subItem.tag"
                :name="subItem.key"
                :ref="subItem.key">
                <a-input-number
                  :disabled="!('updateResourceLimit' in $store.getters.apis)"
                  style="width: 100%;"
                  v-model:value="form[subItem.key]"
                />
              </a-form-item>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div class="card-footer">
        <a-button
          :disabled="!('updateResourceLimit' in $store.getters.apis)"
          v-if="!($route.meta.name === 'domain' && resource.level === 0)"
          :loading="formLoading"
          type="primary"
          ref="submit"
          @click="handleSubmit">{{ $t('label.submit') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'

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
      dataResource: [],
      collpaseActive: {},
      resourceTypeIdNames: {}
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
    this.dataResource = this.resource
    this.fetchData()
  },
  watch: {
    resource: {
      handler (newData) {
        if (!newData || !newData.id) {
          return
        }
        this.fetchData()
      }
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
      const form = reactive({})
      try {
        this.formLoading = true
        this.dataResource = await this.listResourceLimits(params)
        this.dataResource.forEach(item => {
          this.resourceTypeIdNames[item.resourcetype] = item.resourcetypename
          item.key = item.tag ? (item.resourcetype + '-' + item.tag) : item.resourcetype
          form[item.key] = item.max || -1
          item.taggedresource.forEach(subItem => {
            subItem.key = subItem.tag ? (subItem.resourcetype + '-' + subItem.tag) : subItem.resourcetype
            form[subItem.key] = subItem.max || -1
          })
          form[item.resourcetype] = item.max == null ? -1 : item.max
        })
        this.form = form
        this.formRef.value.resetFields()
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

      if (this.formLoading) return
      if (!this.validateTaggedLimitsForUntaggedLimits(toRaw(this.form))) {
        return
      }

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const arrAsync = []
        const params = this.getParams()
        for (const key in values) {
          const input = values[key]

          if (input === undefined) {
            continue
          }
          params.resourcetype = key
          if (key.includes('-')) {
            const idx = key.indexOf('-')
            params.resourcetype = key.substring(0, idx)
            params.tag = key.substring(idx + 1)
          }
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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    validateTaggedLimitsForUntaggedLimits (values) {
      for (const key in values) {
        const input = values[key]
        if (input === undefined) {
          continue
        }
        if (key.includes('-')) {
          const idx = key.indexOf('-')
          const resourcetype = key.substring(0, idx)
          const tag = key.substring(idx + 1)
          const untaggedInput = values[resourcetype]
          if (untaggedInput > 0 && untaggedInput < input) {
            var err = this.$t('message.update.resource.limit.max.untagged.error').replace('%x', this.$t('label.max' + this.resourceTypeIdNames[resourcetype].replace('_', '')))
            err = err.replace('%y', untaggedInput).replace('%z', tag)
            this.$notification.error({
              message: this.$t('message.request.failed'),
              description: err
            })
            return false
          }
        }
      }
      return true
    },
    listResourceLimits (params) {
      return new Promise((resolve, reject) => {
        let dataResource = []
        api('listResourceLimits', params).then(json => {
          if (json.listresourcelimitsresponse.resourcelimit) {
            dataResource = json.listresourcelimitsresponse.resourcelimit
            dataResource.sort((a, b) => a.resourcetype - b.resourcetype)
            var taggedResource = dataResource?.filter(x => x.tag !== null && x.tag !== undefined) || []
            dataResource = dataResource?.filter(x => x.tag === null || x.tag === undefined) || []
            for (var untaggedResource of dataResource) {
              var tagged = taggedResource.filter(x => x.resourcetype === untaggedResource.resourcetype) || []
              tagged.sort((a, b) => a.tag.localeCompare(b.tag))
              untaggedResource.taggedresource = tagged
              var tags = _.map(tagged, 'tag')
              untaggedResource.tags = tags
              untaggedResource.tagsasstring = '#' + tags.join(', #')
            }
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
    handleCollapseChange (type) {
      if (this.collpaseActive[type]) {
        this.collpaseActive[type] = null
        return
      }
      this.collpaseActive[type] = true
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
  .tagged-limit-collapse {
    margin-top: 10px;
    margin-bottom: 20px;
  }
</style>
