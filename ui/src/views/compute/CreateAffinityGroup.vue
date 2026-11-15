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
        @finish="handleSubmit"
        layout="vertical">
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
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item name="type" ref="type">
          <template #label>
            <tooltip-label :title="$t('label.type')" :tooltip="apiParams.type.description"/>
          </template>
          <a-select
            v-model:value="form.type"
            showSearch
            optionFilterProp="label"
            :placeholder="apiParams.type.description">
            <a-select-option v-for="opt in affinityGroupTypes" :key="opt" :label="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <ownership-selection v-if="isAdminOrDomainAdmin()" @fetch-owner="fetchOwnerOptions"/>
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
import { postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection'

export default {
  name: 'CreateAffinityGroup',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    OwnershipSelection
  },
  data () {
    return {
      loading: false,
      owner: {},
      affinityGroupTypes: [
        'host affinity',
        'host anti-affinity',
        'non-strict host affinity',
        'non-strict host anti-affinity'
      ]
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createAffinityGroup')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.name') }],
        type: [{ required: true, message: this.$t('label.required') }]
      })
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchOwnerOptions (ownerOptions) {
      this.owner = {}
      if (ownerOptions.selectedAccountType === 'Account') {
        if (!ownerOptions.selectedAccount) {
          return
        }
        this.owner.account = ownerOptions.selectedAccount
        this.owner.domainid = ownerOptions.selectedDomain
      } else if (ownerOptions.selectedAccountType === 'Project') {
        if (!ownerOptions.selectedProject) {
          return
        }
        this.owner.projectid = ownerOptions.selectedProject
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.loading = true
        const params = {
          name: values.name,
          description: values.description,
          type: values.type
        }
        if (this.owner.account) {
          params.account = this.owner.account
          params.domainid = this.owner.domainid
        } else if (this.owner.projectid) {
          params.projectid = this.owner.projectid
        }
        postAPI('createAffinityGroup', params).then(json => {
          this.$message.success(this.$t('label.add.affinity.group') + ' - ' + values.name)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.$emit('refresh-data')
          this.loading = false
          this.closeAction()
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
