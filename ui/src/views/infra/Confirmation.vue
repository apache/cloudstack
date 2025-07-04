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
  <a-spin :spinning="loading">
    <div class="form-layout" v-ctrl-enter="handleSubmit">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
      >
        <a-form-item
          ref="algorithm"
          name="algorithm"
          v-if="isPrepareForMaintenance">
          <template #label>
            <tooltip-label :title="$t('label.algorithm')" :tooltip="prepareForMaintenanceApiParams.algorithm.description"/>
          </template>
          <a-select
            style="width: 500px"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-model:value="form.algorithm"
            :placeholder="prepareForMaintenanceApiParams.algorithm.description">
            <a-select-option v-for="opt in algorithms" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="forced" ref="forced">
          <template #label>
            <tooltip-label :title="$t('label.forced')" :tooltip="prepareForMaintenanceApiParams.forced.description"/>
          </template>
          <a-switch v-model:checked="form.forced" />
        </a-form-item>
        <a-divider/>
        <a-alert type="error">
          <template #message>
            <span v-html="$t(action.currentAction.message)" />
          </template>
        </a-alert>
        <a-alert type="warning" style="margin-top: 10px">
          <template #message>
            <span>{{ $t('message.confirm.type') }} "{{ action.currentAction.confirmationText }}"</span>
          </template>
        </a-alert>
        <a-form-item ref="confirmation" name="confirmation" style="margin-top: 10px">
            <a-input v-model:value="form.confirmation" />
        </a-form-item>
      </a-form>

      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>

    </div>
  </a-spin>
</template>

<script>

import { api } from '@/api'
import { ref, reactive } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'Confirmation',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      algorithms: ['', 'static', 'roundrobin', 'shuffle'],
      loading: false
    }
  },
  beforeCreate () {
    this.prepareForMaintenanceApiParams = this.$getApiParams('prepareForMaintenance')
  },
  created () {
    this.initForm()
  },
  computed: {
    isPrepareForMaintenance () {
      return this.action.currentAction.api === 'prepareForMaintenance'
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        confirmation: [{
          validator: this.checkConfirmation,
          message: this.$t('message.error.confirm.text')
        }]
      })
    },
    async checkConfirmation (rule, value) {
      if (value && value === this.action.currentAction.confirmationText) {
        return Promise.resolve()
      }
      return Promise.reject(rule.message)
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        this.loading = true
        const params = { managementserverid: this.resource.id }
        if (this.isPrepareForMaintenance && this.form.algorithm !== '') {
          params.algorithm = this.form.algorithm
        }
        params.forced = this.form.forced
        api(this.action.currentAction.api, params).then(() => {
          this.$message.success(this.$t(this.action.currentAction.label) + ' : ' + this.resource.name)
          this.closeAction()
          this.parentFetchData()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}

</script>
<style lang="scss" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }
}

.form {
  margin: 10px 0;
}
</style>
