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

        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description" />
        </a-form-item>

        <a-form-item name="using" ref="using" v-if="'roleid' in apiParams">
          <template #label>
            <tooltip-label :title="$t('label.based.on')" :tooltip="$t('label.based.on.role.id.or.type')"/>
          </template>
          <a-radio-group
            v-model:value="form.using"
            buttonStyle="solid">
            <a-radio-button value="type">
              {{ $t('label.type') }}
            </a-radio-button>
            <a-radio-button value="role">
              {{ $t('label.role') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-form-item name="type" ref="type" v-if="form.using === 'type'">
          <template #label>
            <tooltip-label :title="$t('label.type')" :tooltip="apiParams.type.description"/>
          </template>
          <a-select
            v-model:value="form.type"
            :placeholder="apiParams.type.description"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="role in defaultRoles" :key="role">
              {{ role }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="roleid" ref="roleid" v-if="form.using === 'role'">
          <template #label>
            <tooltip-label :title="$t('label.role')" :tooltip="apiParams.roleid.description"/>
          </template>
          <a-select
            v-model:value="form.roleid"
            :placeholder="apiParams.roleid.description"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="role in roles"
              :value="role.id"
              :key="role.id"
              :label="role.name">
              {{ role.name }}
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
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateRole',
  mixins: [mixinForm],
  components: {
    TooltipLabel
  },
  data () {
    return {
      roles: [],
      defaultRoles: ['Admin', 'DomainAdmin', 'User'],
      loading: false
    }
  },
  created () {
    this.initForm()
    this.fetchRoles()
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createRole')
  },
  watch: {
    '$route' (to, from) {
      if (to.fullPath !== from.fullPath && !to.fullPath.includes('action/')) {
        this.fetchRoles()
      }
    },
    '$i18n.global.locale' (to, from) {
      if (to !== from) {
        this.fetchRoles()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        using: 'type'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        type: [{ required: true, message: this.$t('message.error.select') }],
        roleid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        for (const key in values) {
          if (key === 'using') {
            continue
          }

          const input = values[key]
          if (input === undefined) {
            continue
          }

          params[key] = input
        }

        this.createRole(params)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    createRole (params) {
      this.loading = true
      api('createRole', params).then(json => {
        const role = json.createroleresponse.role
        if (role) {
          this.$emit('refresh-data')
          this.$notification.success({
            message: 'Create Role',
            description: 'Sucessfully created role ' + params.name
          })
        }
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchRoles () {
      const params = {}
      api('listRoles', params).then(json => {
        if (json && json.listrolesresponse && json.listrolesresponse.role) {
          this.roles = json.listrolesresponse.role
        }
      }).catch(error => {
        console.error(error)
      })
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }
</style>
