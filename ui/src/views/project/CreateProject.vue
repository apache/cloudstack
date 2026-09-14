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
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
    >
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="$t('label.project.name')" />
      </a-form-item>
      <a-form-item>
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="$t('label.project.description.tooltip')"/>
        </template>
        <a-input
          v-model:value="form.displaytext"
          :placeholder="$t('label.description')"/>
      </a-form-item>
      <ownership-selection v-if="isAdminOrDomainAdmin()" @fetch-owner="fetchOwner" :show-owner-type-field="false" />
      <a-form-item v-if="Object.keys(this.selectedAccount).length !== 0" ref="userid" name="userid">
        <template #label>
          <tooltip-label :title="$t('label.user')" :tooltip="$t('label.project.user.tooltip')"/>
        </template>
        <a-select
          v-model:value="form.userid"
          :loading="loading"
          showSearch
          :placeholder="this.$t('label.user')"
        >
          <a-select-option v-for="user in selectedAccount.user" :value="user.id" :key="user.id">
            {{ user.username }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>

import { reactive, ref, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'
import ResourceIcon from '@/components/view/ResourceIcon.vue'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'
import { isAdminOrDomainAdmin } from '@/role'

export default {
  name: 'CreateProject',
  components: { OwnershipSelection, ResourceIcon, TooltipLabel },
  data () {
    return {
      loading: false,
      selectedAccount: {}
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.project.name') }]
      })
    },
    fetchOwner (ownerData) {
      if (ownerData.selectedAccountType === 'Account') {
        if (!ownerData.selectedAccount) {
          this.selectedAccount = {}
          return
        }

        this.selectedAccount = ownerData.accounts.find(acc => acc.name === ownerData.selectedAccount)
        this.form.account = ownerData.selectedAccount
        this.form.domainId = ownerData.selectedDomain
      }
    },
    isAdminOrDomainAdmin () {
      return isAdminOrDomainAdmin()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        this.loading = true
        const params = toRaw(this.form)

        if (isAdminOrDomainAdmin()) {
          params.accountid = this.selectedAccount.id
        }

        api('createProject', {}, 'POST', params).then(() => {
          this.$message.success(this.$t('message.success.project.creation'))
          this.closeAction()
          this.$emit('refresh-data')
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

<style lang="less" scoped>
  .form {
    width: 20vw;

  }
</style>
