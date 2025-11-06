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
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical">
          <a-form-item v-if="isAdminOrDomainAdmin()" name="accountids" ref="accountids">
            <template #label>
              <tooltip-label :title="$t('label.account')" :tooltip="apiParams.accountids.description"/>
            </template>
            <infinite-scroll-select
              v-model:value="form.accountids"
              mode="multiple"
              :placeholder="apiParams.accountids.description"
              api="listAccounts"
              :apiParams="accountsApiParams"
              resourceType="account"
              defaultIcon="team-outlined" />
          </a-form-item>
          <a-form-item v-if="isAdminOrDomainAdmin()" name="projectids" ref="projectids">
            <template #label>
              <tooltip-label :title="$t('label.project')" :tooltip="apiParams.projectids.description"/>
            </template>
            <infinite-scroll-select
              v-model:value="form.projectids"
              mode="multiple"
              :placeholder="apiParams.projectids.description"
              api="listProjects"
              :apiParams="projectsApiParams"
              resourceType="project"
              defaultIcon="project-outlined" />
          </a-form-item>
          <a-form-item v-if="!isAdminOrDomainAdmin()">
            <template #label>
              <tooltip-label :title="$t('label.accounts')" :tooltip="apiParams.accounts.description"/>
            </template>
            <a-input
              v-model:value="form.accounts"
              :placeholder="apiParams.accounts.description"
              v-focus="true" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button
              :loading="loading"
              @click="closeAction">
              {{ this.$t('label.cancel') }}
            </a-button>
            <a-button
              :loading="loading"
              type="primary"
              ref="submit"
              @click="handleSubmit">
              {{ this.$t('label.ok') }}
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { postAPI } from '@/api'
import { isAdminOrDomainAdmin } from '@/role'
import { ref, reactive, toRaw } from 'vue'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect'

export default {
  name: 'CreateNetworkPermissions',
  components: {
    TooltipLabel,
    ResourceIcon,
    InfiniteScrollSelect
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
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
    this.apiParams = this.$getApiParams('createNetworkPermissions')
  },
  computed: {
    accountsApiParams () {
      return {
        details: 'min',
        domainid: this.resource.domainid
      }
    },
    projectsApiParams () {
      return {
        details: 'min'
      }
    }
  },
  methods: {
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.networkid = this.resource.id
        if (values.accountids && values.accountids.length > 0) {
          params.accountids = values.accountids.join(',')
        }
        if (values.projectids && values.projectids.length > 0) {
          params.projectids = values.projectids.join(',')
        }
        if (values.accounts && values.accounts.length > 0) {
          params.accounts = values.accounts
        }

        this.loading = true

        postAPI('createNetworkPermissions', params)
          .then(() => {
            this.$notification.success({
              message: this.$t('message.success.add.network.permissions')
            })
            this.closeAction()
            this.$emit('refresh-data')
          }).catch(error => {
            this.$notification.error({
              message: `${this.$t('label.error')} ${error.response.status}`,
              description: error.response.data.createnetworkpermissionsresponse.errortext || error.response.data.errorresponse.errortext,
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

<style lang="less" scoped>
.form-layout {
  width: 60vw;

  @media (min-width: 500px) {
    width: 450px;
  }
}
</style>
