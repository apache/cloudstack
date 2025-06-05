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
      :rules="rules"
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-form-item name="customactionid" ref="customactionid">
        <template #label>
          <tooltip-label :title="$t('label.customactionid')" :tooltip="apiParams.customactionid.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.customactionid"
          :placeholder="apiParams.customactionid.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in customActions" :key="opt.id" :label="opt.name || opt.id">
            {{ opt.name || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-card v-if="form.customactionid" style="margin-bottom: 10px;">
        <div style="margin: 10px 0px;">{{ currentDescription }}</div>
        <a-divider />
        <div v-for="(field, fieldIndex) in currentParameters" :key="fieldIndex">
          <a-form-item :name="field.name" :ref="field.name">
            <template #label>
              <tooltip-label :title="$t('label.' + field.name)" :tooltip="$t('label.' + field.name)"/>
            </template>
            <a-switch
              v-if="field.type === 'BOOLEAN'"
              v-model:checked="form[field.name]"
              :placeholder="field.name"
              v-focus="fieldIndex === 0"
            />
            <a-date-picker
              v-else-if="field.type === 'DATE'"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              :placeholder="field.name"
              v-focus="fieldIndex === 0"
            />
            <a-input-number
              v-else-if="['FLOAT', 'INTEGER', 'SHORT', 'LONG'].includes(field.type)"
              :precision="['FLOAT'].includes(field.type) ? 2 : 0"
              v-focus="fieldIndex === 0"
              v-model:value="form[field.name]"
              :placeholder="field.name" />
            <a-input
              v-else
              v-focus="fieldIndex === 0"
              v-model:value="form[field.name]"
              :placeholder="field.name" />
          </a-form-item>
        </div>
      </a-card>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'RunCustomAction',
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
      customActions: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('runCustomAction')
  },
  created () {
    this.initForm()
    this.fetchCustomActions()
  },
  computed: {
    resourceType () {
      const metaResourceType = this.$route.meta.resourceType
      if (metaResourceType && !['UserVm', 'DomainRouter', 'SystemVm'].includes(metaResourceType)) {
        return metaResourceType
      }
      return 'VirtualMachine'
    },
    currentAction () {
      if (!this.customActions || this.customActions.length === 0 || !this.form.customactionid) {
        return []
      }
      return this.customActions.find(i => i.id === this.form.customactionid)
    },
    currentDescription () {
      return this.currentAction?.description || ''
    },
    currentParameters () {
      return this.currentAction?.parameters || []
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        customactionid: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchCustomActions () {
      this.loading = true
      this.customActions = []
      const params = {
        resourcetype: this.resourceType,
        resourceid: this.resource.id
      }
      api('listCustomActions', params).then(json => {
        this.customActions = json?.listcustomactionsresponse?.extensioncustomaction || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          resourcetype: this.resourceType,
          resourceid: this.resource.id,
          enabled: true
        }
        console.log('values----------------', values)
        for (const key of Object.keys(values)) {
          var value = values[key]
          if (value !== undefined && value != null &&
              (typeof value !== 'string' || (typeof value === 'string' && value.trim().length > 0))) {
            params[key] = value
          }
        }
        if (params) {
          console.log('----------------', params)
          return
        }
        api('runCustomAction', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.run.custom.action'),
            description: this.$t('message.success.run.custom.action')
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
