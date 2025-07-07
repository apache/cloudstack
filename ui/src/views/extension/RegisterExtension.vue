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
    <a-form
      :ref="formRef"
      :model="form"
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-form-item ref="resourcetype" name="resourcetype">
        <template #label>
          <tooltip-label :title="$t('label.resourcetype')" :tooltip="apiParams.resourcetype.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.resourcetype"
          :placeholder="apiParams.resourcetype.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          @change="handleResourceTypeChange" >
          <a-select-option v-for="opt in resourceTypes" :key="opt.id" :label="opt.description || opt.id">
            {{ opt.description || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="resourceid" name="resourceid">
        <template #label>
          <tooltip-label :title="$t('label.resourceid')" :tooltip="apiParams.resourceid.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.resourceid"
          :placeholder="apiParams.resourceid.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in resources.opts" :key="opt.id" :label="opt.name || opt.description">
            {{ opt.name || opt.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <template #label>
          <tooltip-label :title="$t('label.details')" :tooltip="apiParams.details.description"/>
        </template>
        <div style="margin-bottom: 10px">{{ $t('message.add.extension.resource.details') }}</div>
        <details-input
          v-model:value="form.details" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'RegisterExtension',
  components: {
    TooltipLabel,
    DetailsInput
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      resourceTypes: [],
      resources: {
        loading: false,
        opts: []
      },
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('registerExtension')
  },
  created () {
    this.initForm()
    this.fetchExtensionResourceTypes()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        resourcetype: [{ required: true, message: `${this.$t('message.error.select')}` }],
        resourceid: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchExtensionResourceTypes () {
      this.resourceTypes = []
      const resourceTypesList = ['Cluster']
      resourceTypesList.forEach((item) => {
        this.resourceTypes.push({
          id: item,
          description: item
        })
      })
    },
    handleResourceTypeChange (id) {
      this.resources.opts = []
      if (!id) {
        return
      }
      this.resources.loading = true
      const resourceApi = 'list' + id + 's'
      const type = id.toLowerCase()
      const params = {}
      if (['cluster'].includes(type)) {
        params.hypervisor = 'External'
      }
      getAPI(resourceApi, params).then(json => {
        this.resources.opts = json?.[resourceApi.toLowerCase() + 'response']?.[type] || []
      }).finally(() => {
        this.resources.loading = false
        this.form.resourceid = this.resources?.opts?.[0]?.id || null
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          extensionid: this.resource.id,
          resourcetype: values.resourcetype,
          resourceid: values.resourceid
        }
        if (values.details) {
          Object.entries(values.details).forEach(([key, value]) => {
            params['details[0].' + key] = value
          })
        }
        postAPI('registerExtension', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.register.extension'),
            description: this.$t('message.success.register.extension')
          })
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.$notifyError(error)
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
    width: 550px;
  }
}
</style>
