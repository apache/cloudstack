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
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
      class="form"
      layout="vertical"
    >
      <a-alert type="warning">
        <template #message>
          <span v-html="$t('label.outofbandmanagement.configure')" />
        </template>
      </a-alert>
      <div style="margin-top: 10px;">
        <a-form-item name="address" ref="address">
          <template #label>
            <tooltip-label :title="$t('label.address')" :tooltip="apiParams.address.description"/>
          </template>
          <a-input
            v-model:value="form.address"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="port" ref="port">
          <template #label>
            <tooltip-label :title="$t('label.port')" :tooltip="apiParams.port.description"/>
          </template>
          <a-input
            v-model:value="form.port" />
        </a-form-item>
        <a-form-item name="username" ref="username">
          <template #label>
            <tooltip-label :title="$t('label.username')" :tooltip="apiParams.username.description"/>
          </template>
          <a-input
            v-model:value="form.username" />
        </a-form-item>
        <a-form-item name="password" ref="password">
          <template #label>
            <tooltip-label :title="$t('label.password')" :tooltip="apiParams.password.description"/>
          </template>
          <a-input-password
            v-model:value="form.password"
            :placeholder="apiParams.password.description"/>
        </a-form-item>
        <a-form-item name="driver" ref="driver">
          <template #label>
            <tooltip-label :title="$t('label.driver')" :tooltip="apiParams.driver.description"/>
          </template>
          <a-select
            v-model:value="form.driver"
              style="width: 100%;"
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option key="" label="">{{ }}</a-select-option>
              <a-select-option value="ipmitool">ipmitool</a-select-option>
              <a-select-option value="nestedcloudstack">nestedcloudstack</a-select-option>
              <a-select-option value="redfish">redfish</a-select-option>
            </a-select>
        </a-form-item>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'ConfigureHostOOBM',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('configureOutOfBandManagement')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        address: this.resource.outofbandmanagement.address || '',
        port: this.resource.outofbandmanagement.port || '',
        username: this.resource.outofbandmanagement.username || '',
        password: '',
        driver: this.resource.outofbandmanagement.driver || ''
      })
      this.rules = reactive({
        address: [{ required: true, message: this.$t('message.error.required.input') }],
        port: [{ required: true, message: this.$t('message.error.required.input') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }],
        driver: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {
          hostid: this.resource.id,
          address: values.address,
          port: values.port,
          username: values.username,
          password: values.password,
          driver: values.driver
        }

        api('configureOutOfBandManagement', {}, 'POST', params).then(_ => {
          this.$message.success(this.$t('message.oobm.configured'))
          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        })
      })
    },
    onCloseAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped>
.form-layout {
    width: 30vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }
</style>
