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
        <a-form-item>
          <a-alert type="warning">
            <template #message>
             <span v-html="$t('message.confirm.change.service.offering.for.sharedfs')" />
            </template>
         </a-alert>
        </a-form-item>
        <a-form-item ref="serviceofferingid" name="serviceofferingid">
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description || 'Service Offering'"/>
          </template>
          <a-select
            v-model:value="form.serviceofferingid"
            :loading="serviceofferingLoading"
            :placeholder="apiParams.serviceofferingid.description || $t('label.serviceofferingid')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="(serviceoffering, index) in serviceofferings"
              :value="serviceoffering.id"
              :key="index"
              :label="serviceoffering.name || serviceoffering.displaytext">
              {{ serviceoffering.name || serviceoffering.displaytext }}
            </a-select-option>
          </a-select>
        </a-form-item>
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
      configLoading: false,
      serviceofferings: [],
      serviceofferingLoding: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('changeSharedFileSystemServiceOffering')
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
        serviceofferingid: [{ required: true, message: this.$t('label.required') }]
      })
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchData () {
      this.fetchServiceOfferings()
    },
    fetchCapabilities (id) {
      api('listCapabilities').then(json => {
        this.capability = json.listcapabilitiesresponse.capability || []
        this.minCpu = this.capability.sharedfsvmmincpucount
        this.minMemory = this.capability.sharedfsvmminramsize
      })
    },
    fetchServiceOfferings () {
      this.fetchCapabilities()
      this.serviceofferingLoading = true
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
      api('listServiceOfferings', params).then(json => {
        var items = json.listserviceofferingsresponse.serviceoffering || []
        if (items != null) {
          for (var i = 0; i < items.length; i++) {
            if (items[i].iscustomized === false && items[i].offerha === true &&
                items[i].cpunumber >= this.minCpu && items[i].memory >= this.minMemory) {
              this.serviceofferings.push(items[i])
            }
          }
        }
        this.form.serviceofferingid = this.serviceofferings[0].id || ''
      })
      this.serviceofferingLoading = false
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          id: this.resource.id,
          serviceofferingid: values.serviceofferingid
        }
        this.loading = true
        api('changeSharedFileSystemServiceOffering', data).then(response => {
          this.$pollJob({
            jobId: response.changesharedfilesystemserviceofferingresponse.jobid,
            title: this.$t('label.change.service.offering'),
            description: values.name,
            successMessage: this.$t('message.success.change.offering'),
            errorMessage: this.$t('message.change.service.offering.sharedfs.failed'),
            loadingMessage: this.$t('message.change.service.offering.sharedfs.processing'),
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
