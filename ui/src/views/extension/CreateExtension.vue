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
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          :placeholder="apiParams.name.description"
          @change="updatePath"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input
          v-model:value="form.description"
          :placeholder="apiParams.description.description" />
      </a-form-item>
      <a-form-item name="path" ref="path">
        <template #label>
          <tooltip-label :title="$t('label.path')" :tooltip="apiParams.path.description"/>
        </template>
        <div class="path-input-container">
          <span v-if="!!safeName" :title="extenstionBasePath" class="path-input-base">
            {{ extenstionBasePath }}
          </span>
          <a-input
            v-model:value="form.path"
            :placeholder="apiParams.path.description"
            @input="markPathModified"
            class="path-input-relative"
          />
        </div>
      </a-form-item>
      <a-form-item ref="type" name="type">
        <template #label>
          <tooltip-label :title="$t('label.type')" :tooltip="apiParams.type.description"/>
        </template>
        <a-select
          showSearch
          v-model:value="form.type"
          :placeholder="apiParams.type.description"
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option v-for="opt in extensionTypes" :key="opt.id" :label="opt.description || opt.id">
            {{ opt.description || opt.id }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="orchestratorrequirespreparevm" ref="orchestratorrequirespreparevm" v-if="form.type === 'Orchestrator'">
        <template #label>
          <tooltip-label :title="$t('label.orchestratorrequirespreparevm')" :tooltip="apiParams.orchestratorrequirespreparevm.description"/>
        </template>
        <a-switch v-model:checked="form.orchestratorrequirespreparevm" />
      </a-form-item>
      <a-form-item name="details" ref="details">
        <template #label>
          <tooltip-label :title="$t('label.configuration.details')" :tooltip="apiParams.details.description"/>
        </template>
        <div style="margin-bottom: 10px">{{ $t('message.add.extension.details') }}</div>
        <details-input
          v-model:value="form.details" />
      </a-form-item>
      <a-form-item name="state" ref="state">
        <template #label>
          <tooltip-label :title="$t('label.enabled')" :tooltip="apiParams.state.description"/>
        </template>
        <a-switch v-model:checked="form.state" />
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
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'CreateExtension',
  components: {
    TooltipLabel,
    DetailsInput
  },
  data () {
    return {
      pathModified: false,
      extensionTypes: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createExtension')
  },
  created () {
    this.initForm()
    this.fetchExtensionTypes()
  },
  computed: {
    safeName () {
      var value = this.form.name
      if (!value || value.length === 0) {
        return ''
      }
      return value.replace(/[^a-zA-Z0-9._-]/g, '_').toLowerCase()
    },
    extenstionBasePath () {
      return (this.$store.getters.features.extensionspath || '[EXTENSIONS_PATH]') + '/' + this.safeName + '/'
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        state: false
      })
      this.rules = reactive({
        name: [{ required: true, message: `${this.$t('message.error.name')}` }],
        type: [{ required: true, message: `${this.$t('message.error.select')}` }]
      })
    },
    fetchExtensionTypes () {
      this.extensionTypes = []
      const extensionTypesList = ['Orchestrator']
      extensionTypesList.forEach((item) => {
        this.extensionTypes.push({
          id: item,
          description: item
        })
      })
      this.form.type = this.extensionTypes?.[0]?.id
    },
    markPathModified () {
      this.pathModified = true
    },
    updatePath () {
      if (this.pathModified) {
        return
      }
      var value = this.safeName
      if (value.length === 0) {
        this.form.path = undefined
        return
      }
      this.form.path = value + '.sh'
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          name: values.name,
          type: values.type,
          state: values.state ? 'Enabled' : 'Disabled'
        }
        if (values.description) {
          params.description = values.description
        }
        var path = values.path
        if (!path) {
          path = this.safeName + '.sh'
        }
        params.path = this.safeName + '/' + path
        if (values.details) {
          Object.entries(values.details).forEach(([key, value]) => {
            params['details[0].' + key] = value
          })
        }
        postAPI('createExtension', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.create.extension'),
            description: this.$t('message.success.create.extension')
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

.path-input-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.path-input-base {
  max-width: 70%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.path-input-relative {
  flex: 1 1 0%;
  min-width: 0;
}
</style>
