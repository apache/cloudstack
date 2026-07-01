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
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit">
      <ownership-selection @fetch-owner="fetchOwnerOptions" />
      <a-form-item ref="value" name="value">
        <template #label>
          <tooltip-label :title="$t('label.value')" :tooltip="apiParams.value.description"/>
        </template>
        <a-input-number
          v-model:value="form.value"
          :placeholder="$t('placeholder.quota.credit.add.value')" />
      </a-form-item>
      <a-form-item ref="min_balance" name="min_balance">
        <template #label>
          <tooltip-label :title="$t('label.min_balance')" :tooltip="apiParams.min_balance.description"/>
        </template>
        <a-input-number
          v-model:value="form.min_balance"
          :placeholder="$t('placeholder.quota.credit.add.min_balance')" />
      </a-form-item>
      <a-form-item ref="quota_enforce" name="quota_enforce">
        <template #label>
          <tooltip-label :title="$t('label.quota.enforce')" :tooltip="apiParams.quota_enforce.description"/>
        </template>
        <a-switch
          v-model:checked="form.quota_enforce" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { getAPI } from '@/api'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import { ref, reactive, toRaw } from 'vue'
import { mixinForm } from '@/utils/mixin'
import store from '@/store'

export default {
  name: 'AddQuotaCredit',
  mixins: [mixinForm],
  components: {
    OwnershipSelection,
    TooltipLabel
  },
  data () {
    return {
      loading: false,
      domainList: [],
      accountList: [],
      domainId: undefined,
      domainLoading: false,
      domainError: false,
      owner: {
        projectid: store.getters.project?.id,
        domainid: store.getters.project?.id ? null : store.getters.userInfo.domainid,
        account: store.getters.project?.id ? null : store.getters.userInfo.account,
        name: store.getters.project?.id ? store.getters.project.name : store.getters.userInfo.account
      }
    }
  },
  inject: ['parentFetchData'],
  beforeCreate () {
    this.apiParams = this.$getApiParams('quotaCredits')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        value: [{ required: true, message: this.$t('message.action.quota.credit.add.error.valuerequired') }]
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return

      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        values.ignoreproject = true

        if (this.owner.projectid) {
          values.projectid = this.owner.projectid
        } else {
          values.account = this.owner.account
          values.domainid = this.owner.domainid
        }

        this.loading = true
        getAPI('quotaCredits', values).then(response => {
          this.$message.success(this.$t('message.action.quota.credit.add.success',
            { credit: response.quotacreditsresponse.quotacredits.credit, account: this.owner.name }))
          this.parentFetchData()
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    fetchOwnerOptions (OwnerOptions) {
      this.owner = {}
      if (OwnerOptions.selectedAccountType === 'Account') {
        if (!OwnerOptions.selectedAccount) {
          return
        }
        this.owner.account = OwnerOptions.selectedAccount
        this.owner.domainid = OwnerOptions.selectedDomain
        this.owner.name = OwnerOptions.selectedAccount
      } else if (OwnerOptions.selectedAccountType === 'Project') {
        if (!OwnerOptions.selectedProject) {
          return
        }
        this.owner.projectid = OwnerOptions.selectedProject
        this.owner.name = OwnerOptions.projects.find(p => p.id === OwnerOptions.selectedProject).name
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/objects/form.scss';
</style>
