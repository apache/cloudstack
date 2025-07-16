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
        :loading="loading"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item ref="newname" name="newname">
          <template #label>
            <tooltip-label :title="$t('label.newname')" :tooltip="apiParams.newname.description"/>
          </template>
          <a-input
            v-model:value="form.newname"
            :placeholder="apiParams.newname.description" />
        </a-form-item>
        <a-form-item ref="networkdomain" name="networkdomain">
          <template #label>
            <tooltip-label :title="$t('label.networkdomain')" :tooltip="apiParams.networkdomain.description"/>
          </template>
          <a-input
            v-model:value="form.networkdomain"
            :placeholder="apiParams.networkdomain.description" />
        </a-form-item>
        <a-form-item ref="roleid" name="roleid">
          <template #label>
            <tooltip-label :title="$t('label.role')" :tooltip="apiParams.roleid.description"/>
          </template>
          <a-select
            v-model:value="form.roleid"
            :loading="roleLoading"
            :placeholder="apiParams.roleid.description"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="isRootAdmin" ref="apikeyaccess" name="apikeyaccess">
          <template #label>
            <tooltip-label :title="$t('label.apikeyaccess')" :tooltip="apiParams.apikeyaccess.description"/>
          </template>
          <a-radio-group v-model:value="form.apikeyaccess" buttonStyle="solid">
            <a-radio-button value="ENABLED">Enabled</a-radio-button>
            <a-radio-button value="INHERIT">Inherit</a-radio-button>
            <a-radio-button value="DISABLED">Disabled</a-radio-button>
          </a-radio-group>
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
import { getAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'EditAccount',
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
      loading: false,
      roleLoading: false,
      roles: []
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateAccount')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  computed: {
    isRootAdmin () {
      return this.$store.getters.userInfo?.roletype === 'Admin'
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    fetchData () {
      this.account = this.resource.name
      this.domainId = this.resource.domainid
      this.form.apikeyaccess = this.resource.apikeyaccess
      this.fetchRoles()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    fetchRoles () {
      this.roleLoading = true
      const params = {}
      params.state = 'enabled'
      getAPI('listRoles', params).then(response => {
        this.roles = response.listrolesresponse.role || []
        this.form.roleid = this.resource.roleid
      }).finally(() => {
        this.roleLoading = false
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.loading = true
        const params = {
          newname: values.newname,
          networkdomain: values.networkdomain,
          roleid: values.roleid,
          apikeyaccess: values.apikeyaccess,
          account: this.account,
          domainid: this.domainId
        }
        if (this.isValidValueForKey(values, 'networkdomain') && values.networkdomain.length > 0) {
          params.networkdomain = values.networkdomain
        }

        getAPI('updateAccount', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.edit.account'),
            description: `${this.$t('message.success.update.account')} ${params.account}`
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
    width: 80vw;
    @media (min-width: 600px) {
      width: 450px;
    }
  }
</style>
