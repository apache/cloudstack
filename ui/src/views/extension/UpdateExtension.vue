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
      <a-form-item name="description" ref="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
        </template>
        <a-input
          v-model:value="form.description"
          :placeholder="apiParams.description.description"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="orchestratorrequirespreparevm" ref="orchestratorrequirespreparevm" v-if="resource.type === 'Orchestrator'">
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
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateExtension')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        description: this.resource.description,
        details: this.resource.details,
        orchestratorrequirespreparevm: this.resource.orchestratorrequirespreparevm
      })
    },
    fetchData () {
      this.loading = true
      getAPI('listExtensions', { id: this.resource.id }).then(json => {
        this.form.details = json?.listextensionsresponse?.extension?.[0]?.details
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
          id: this.resource.id
        }
        const keys = ['description', 'orchestratorrequirespreparevm']
        for (const key of keys) {
          if (values[key] !== undefined || values[key] !== null) {
            params[key] = values[key]
          }
        }
        if (values.details && Object.keys(values.details).length > 0) {
          Object.entries(values.details).forEach(([key, value]) => {
            params['details[0].' + key] = value
          })
        } else {
          params.cleanupdetails = true
        }
        postAPI('updateExtension', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.update.extension'),
            description: this.$t('message.success.update.extension')
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
