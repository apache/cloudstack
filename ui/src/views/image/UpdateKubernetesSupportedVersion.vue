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
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical">
        <a-form-item ref="state" name="state" :label="$t('label.state')">
          <a-select
            id="state-selection"
            v-model:value="form.state"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="stateLoading"
            :placeholder="apiParams.state.description"
            v-focus="true" >
            <a-select-option v-for="(opt, optIndex) in states" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'UpdateKubernetesSupportedVersion',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      states: [],
      stateLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateKubernetesSupportedVersion')
  },
  created () {
    this.states = [
      {
        id: 'Enabled',
        name: this.$t('state.enabled')
      },
      {
        id: 'Disabled',
        name: this.$t('state.disabled')
      }
    ]
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        state: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      var selectedState = 0
      if (!this.isObjectEmpty(this.resource)) {
        for (var i = 0; i < this.states.length; ++i) {
          if (this.states[i].id === this.resource.state) {
            selectedState = i
            break
          }
        }
      }
      this.form.state = selectedState
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
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
        if (this.isValidValueForKey(values, 'state') && this.arrayHasItems(this.states)) {
          params.state = this.states[values.state].id
        }
        api('updateKubernetesSupportedVersion', params).then(json => {
          this.$message.success(`${this.$t('message.success.update.kubeversion')}: ${this.resource.name}`)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
    width: 60vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }
</style>
