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
        @finish="handleSubmit"
       >
        <a-form-item ref="diskofferingid" name="diskofferingid">
          <template #label>
            <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || 'Disk Offering'"/>
          </template>
          <a-select
            v-model:value="form.diskofferingid"
            :loading="diskofferingLoading"
            @change="id => handleDiskOfferingChange(id)"
            :placeholder="apiParams.diskofferingid.description || $t('label.diskofferingid')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="(diskoffering, index) in diskofferings"
              :value="diskoffering.id"
              :key="index"
              :label="diskoffering.displaytext || diskoffering.name">
              {{ diskoffering.displaytext || diskoffering.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <span v-if="customDiskOffering">
          <a-form-item ref="size" name="size">
            <template #label>
              <tooltip-label :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
            </template>
            <a-input
              v-model:value="form.size"
              :placeholder="apiParams.size.description"/>
          </a-form-item>
        </span>
        <span v-if="isCustomizedDiskIOps">
          <a-form-item ref="miniops" name="miniops">
            <template #label>
              <tooltip-label :title="$t('label.miniops')" :tooltip="apiParams.miniops.description"/>
            </template>
            <a-input
              v-model:value="form.miniops"
              :placeholder="apiParams.miniops.description"/>
          </a-form-item>
          <a-form-item ref="maxiops" name="maxiops">
            <template #label>
              <tooltip-label :title="$t('label.maxiops')" :tooltip="apiParams.maxiops.description"/>
            </template>
            <a-input
              v-model:value="form.maxiops"
              :placeholder="apiParams.maxiops.description"/>
          </a-form-item>
        </span>
        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import store from '@/store'

export default {
  name: 'CreateSharedFS',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon,
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      owner: {
        projectid: store.getters.project?.id,
        domainid: store.getters.project?.id ? null : store.getters.userInfo.domainid,
        account: store.getters.project?.id ? null : store.getters.userInfo.account
      },
      loading: false,
      diskofferings: [],
      diskofferingLoading: false,
      customDiskOffering: false,
      isCustomizedDiskIOps: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('changeSharedFileSystemDiskOffering')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
        diskofferingid: [{ required: true, message: this.$t('label.required') }],
        size: [{ required: true, message: this.$t('message.error.custom.disk.size') }],
        miniops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }],
        maxiops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
    fetchData () {
      this.fetchDiskOfferings()
    },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      var params = {
        zoneid: this.resource.zoneid,
        listall: true,
        domainid: this.owner.domainid
      }
      if (this.owner.projectid) {
        params.projectid = this.owner.projectid
      } else {
        params.account = this.owner.account
      }
      api('listDiskOfferings', params).then(json => {
        this.diskofferings = json.listdiskofferingsresponse.diskoffering || []
        this.form.diskofferingid = this.diskofferings[0].id || ''
        this.customDiskOffering = this.diskofferings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.diskofferings[0]?.iscustomizediops || false
      }).finally(() => {
        this.diskOfferingLoading = false
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleDiskOfferingChange (id) {
      const diskoffering = this.diskofferings.filter(x => x.id === id)
      this.customDiskOffering = diskoffering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = diskoffering[0]?.iscustomizediops || false
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          id: this.resource.id,
          diskofferingid: values.diskofferingid,
          size: values.size,
          minops: values.miniops,
          maxops: values.maxiops
        }
        this.loading = true
        api('changeSharedFileSystemDiskOffering', data).then(response => {
          this.$pollJob({
            jobId: response.changesharedfilesystemdiskofferingresponse.jobid,
            title: this.$t('label.change.disk.offering'),
            description: values.name,
            successMessage: this.$t('message.success.change.offering'),
            errorMessage: this.$t('message.change.disk.offering.sharedfs.failed'),
            loadingMessage: this.$t('message.change.disk.offering.sharedfs.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}

</script>
<style lang="scss" scoped>
.form-layout {
  width: 85vw;

  @media (min-width: 1000px) {
    width: 35vw;
  }
}
</style>
