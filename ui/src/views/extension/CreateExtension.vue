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
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          :placeholder="apiParams.name.description"
          v-focus="true" />
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
          <a-select-option v-for="opt in extensionTypeslist.opts" :key="opt.id" :label="opt.id || opt.description">
            {{ opt.id || opt.description }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item>
        <br />
        <span>{{ $t('message.add.external.details') }}</span><br/>
        <br />
        <a-button style="width: 100%" ref="details" type="primary" @click="addExternalDetails">
          <template #icon><plus-outlined /></template>
          {{ $t('label.add.external.details') }}
        </a-button>
        <a-form-item>
          <div v-show="showAddDetail">
            <br/>
            <a-input-group
              type="text"
              compact>
              <a-input
                style="width: 25%;"
                ref="keyElm"
                v-model:value="newKey"
                :placeholder="$t('label.name')"
                @change="e => onAddInputChange(e, 'newKey')" />
              <a-input
                class="tag-disabled-input"
                style=" width: 30px; margin-left: 10px; margin-right: 10px; pointer-events: none; text-align: center"
                placeholder="="
                disabled />
              <a-input
                style="width: 35%;"
                v-model:value="newValue"
                :placeholder="$t('label.value')"
                @change="e => onAddInputChange(e, 'newValue')" />
              <tooltip-button :tooltip="$t('label.add.setting')" :shape="null" icon="check-outlined" @onClick="addDetail" buttonClass="detail-button" />
              <tooltip-button :tooltip="$t('label.cancel')" :shape="null" icon="close-outlined" @onClick="closeDetail" buttonClass="detail-button" />
            </a-input-group>
          </div>
        </a-form-item>
        <a-list size="medium">
          <a-list-item :key="index" v-for="(item, index) in externalDetails">
            <span style="padding-left: 11px; width: 14%;"> {{ item.name }} </span>
            <span style="padding-left: 30px; width: 55%;"> {{ item.value }}</span>
            <tooltip-button
              style="width: 30%;"
              :tooltip="$t('label.delete')"
              :type="primary"
              :danger="true"
              icon="delete-outlined"
              @onClick="removeDetail(index)"/>
          </a-list-item>
        </a-list>
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
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'CreateExtension',
  components: {
    TooltipLabel,
    TooltipButton
  },
  data () {
    return {
      showAddDetail: false,
      newKey: '',
      newValue: '',
      externalDetails: [],
      extensionTypeslist: {},
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
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    addExternalDetails () {
      this.showAddDetail = true
    },
    onAddInputChange (val, obj) {
      this.error = false
      this[obj].concat(val.data)
    },
    addDetail () {
      if (this.newKey === '' || this.newValue === '') {
        this.error = this.$t('message.error.provide.setting')
        return
      }
      this.error = false
      this.externalDetails.push({ name: this.newKey, value: this.newValue })
      this.newKey = ''
      this.newValue = ''
    },
    removeDetail (index) {
      this.externalDetails.splice(index, 1)
      this.newKey = ''
      this.newValue = ''
    },
    fetchExtensionTypes () {
      const extensionTypes = []
      extensionTypes.push({
        id: 'Orchestrator',
        description: 'Orchestrator'
      })
      this.extensionTypeslist.opts = extensionTypes
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          name: values.name,
          type: values.type
        }
        if (this.externalDetails.length > 0) {
          this.externalDetails.forEach(function (item, index) {
            params['externaldetails[0].' + item.name] = item.value
          })
        }
        api('createExtension', params).then(response => {
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
    width: 450px;
  }
}
</style>
