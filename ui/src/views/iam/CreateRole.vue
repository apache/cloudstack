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
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="name" ref="name">
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>

        <a-form-item name="description" ref="description">
          <template #label>
            {{ $t('label.description') }}
            <a-tooltip :title="apiParams.description.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description" />
        </a-form-item>

        <a-form-item name="using" ref="using" v-if="'roleid' in apiParams">
          <template #label>
            {{ $t('label.based.on') }}
            <a-tooltip :title="$t('label.based.on.role.id.or.type')">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
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
            {{ $t('label.type') }}
            <a-tooltip :title="apiParams.type.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.type"
            :placeholder="apiParams.type.description">
            <a-select-option v-for="role in defaultRoles" :key="role">
              {{ role }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="roleid" ref="roleid" v-if="form.using === 'role'">
          <template #label>
            {{ $t('label.role') }}
            <a-tooltip :title="apiParams.roleid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.roleid"
            :placeholder="apiParams.roleid.description">
            <a-select-option
              v-for="role in roles"
              :value="role.id"
              :key="role.id">
              {{ role.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" html-type="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'CreateRole',
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
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
