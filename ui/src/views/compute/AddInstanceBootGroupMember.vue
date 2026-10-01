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
      :rules="rules"
      :loading="loading"
      layout="vertical"
      @finish="handleSubmit">
      <a-form-item name="membertype" ref="membertype">
        <template #label>
          <tooltip-label :title="$t('label.member.type')" />
        </template>
        <a-radio-group
          v-model:value="form.membertype"
          @change="onMemberTypeChange">
          <a-radio value="VirtualMachine">{{ $t('label.instance') }}</a-radio>
          <a-radio value="InstanceGroup">{{ $t('label.instance.group') }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item name="virtualmachineid" ref="virtualmachineid" v-if="form.membertype === 'VirtualMachine'">
        <template #label>
          <tooltip-label :title="$t('label.instance')" />
        </template>
        <infinite-scroll-select
          api="listVirtualMachines"
          :apiParams="{ listall: true }"
          resourceType="virtualmachine"
          defaultIcon="desktop-outlined"
          @change-option-value="onMemberSelected" />
      </a-form-item>
      <a-form-item name="instancegroupid" ref="memberid" v-else>
        <template #label>
          <tooltip-label :title="$t('label.instance.group')" />
        </template>
        <infinite-scroll-select
          api="listInstanceGroups"
          :apiParams="{ listall: true }"
          resourceType="instancegroup"
          defaultIcon="gold-outlined"
          @change-option-value="onMemberSelected" />
      </a-form-item>
      <a-form-item name="order" ref="order">
        <template #label>
          <tooltip-label :title="$t('label.boot.order')" :tooltip="apiParams.order.description" />
        </template>
        <a-input-number
          v-model:value="form.order"
          style="width: 100%"
          :min="0" />
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
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect'

export default {
  name: 'AddInstanceBootGroupMember',
  components: {
    TooltipLabel,
    InfiniteScrollSelect
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    memberCount: {
      type: Number,
      default: 0
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('addMemberToInstanceBootGroup')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        membertype: 'VirtualMachine',
        order: this.memberCount
      })
      this.rules = reactive({
        order: [{ required: true, type: 'number', message: `${this.$t('message.error.required.input')}` }],
        virtualmachineid: [{ required: true, message: this.$t('message.error.select') }],
        instancegroupid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    onMemberTypeChange () {
      this.form.virtualmachineid = undefined
      this.form.instancegroupid = undefined
    },
    onMemberSelected (value) {
      if (this.form.membertype === 'VirtualMachine') {
        this.form.virtualmachineid = value
      } else {
        this.form.instancegroupid = value
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          id: this.resource.id,
          order: values.order
        }
        if (values.membertype === 'VirtualMachine') {
          params.virtualmachineid = values.virtualmachineid
        } else {
          params.instancegroupid = values.instancegroupid
        }
        postAPI('addMemberToInstanceBootGroup', params).then(() => {
          this.$notification.success({
            message: this.$t('label.add.member'),
            description: this.$t('message.success.add.member')
          })
          this.closeAction()
          this.parentFetchData()
        }).catch(error => {
          this.$notifyError(error)
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

<style lang="scss" scoped>
</style>
