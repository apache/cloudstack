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
      class="form-layout"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      v-ctrl-enter="handleSubmit"
      @finish="handleSubmit">
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="netmask" ref="netmask">
        <template #label>
          <tooltip-label :title="$t('label.netmask')" :tooltip="$t('label.netmask.description')"/>
        </template>
        <a-input v-model:value="form.netmask" />
      </a-form-item>
      <a-form-item name="gateway" ref="gateway">
        <template #label>
          <tooltip-label :title="$t('label.gateway')" :tooltip="$t('label.gateway.description')"/>
        </template>
        <a-input v-model:value="form.gateway" />
      </a-form-item>
      <a-form-item name="storageaccessgroups" ref="storageaccessgroups">
        <template #label>
          <tooltip-label :title="$t('label.storageaccessgroups')" :tooltip="apiParamsConfigureStorageAccess.storageaccessgroups.description"/>
        </template>
        <a-select
          mode="tags"
          v-model:value="form.storageaccessgroups"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children?.[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="storageAccessGroupsLoading"
          :placeholder="apiParamsConfigureStorageAccess.storageaccessgroups.description">
          <a-select-option v-for="(opt) in storageAccessGroups" :key="opt">
            {{ opt }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'PodUpdate',
  components: {
    TooltipLabel
  },
  props: {
    action: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      storageAccessGroups: [],
      storageAccessGroupsLoading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updatePod')
    this.apiParamsConfigureStorageAccess = this.$getApiParams('configureStorageAccess')
  },
  created () {
    this.initForm()
    this.fetchStorageAccessGroupsData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: this.resource.name,
        netmask: this.resource.netmask,
        gateway: this.resource.gateway,
        storageaccessgroups: this.resource.storageaccessgroups
          ? this.resource.storageaccessgroups.split(',')
          : []
      })
      this.rules = reactive({})
    },
    fetchStorageAccessGroupsData () {
      const params = {}
      this.storageAccessGroupsLoading = true
      getAPI('listStorageAccessGroups', params).then(json => {
        const sags = json.liststorageaccessgroupsresponse.storageaccessgroup || []
        for (const sag of sags) {
          if (!this.storageAccessGroups.includes(sag.name)) {
            this.storageAccessGroups.push(sag.name)
          }
        }
      }).finally(() => {
        this.storageAccessGroupsLoading = false
      })
      this.rules = reactive({})
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        console.log(values)
        const params = {}
        params.id = this.resource.id
        params.netmask = values.netmask
        params.gateway = values.gateway
        params.name = values.name
        this.loading = true

        postAPI('updatePod', params).then(json => {
          this.$message.success({
            content: `${this.$t('label.action.update.pod')} - ${values.name}`,
            duration: 2
          })

          if (values.storageaccessgroups != null && values.storageaccessgroups.length > 0) {
            params.storageaccessgroups = values.storageaccessgroups.join(',')
          } else {
            params.storageaccessgroups = ''
          }

          if (params.storageaccessgroups !== undefined && (this.resource.storageaccessgroups ? this.resource.storageaccessgroups.split(',').join(',') : '') !== params.storageaccessgroups) {
            postAPI('configureStorageAccess', {
              podid: params.id,
              storageaccessgroups: params.storageaccessgroups
            }).then(response => {
              this.$pollJob({
                jobId: response.configurestorageaccessresponse.jobid,
                successMethod: () => {
                  this.$message.success({
                    content: this.$t('label.action.configure.storage.access.group'),
                    duration: 2
                  })
                },
                errorMessage: this.$t('message.configuring.storage.access.failed')
              })
            })
          }

          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => { this.loading = false })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    onCloseAction () {
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

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
