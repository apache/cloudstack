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
      layout="vertical"
      @finish="handleSubmit"
     >
      <a-form-item name="name" ref="name" :label="$t('label.name')">
        <a-input
          v-model:value="form.name"
          :placeholder="$t('label.name')"/>
      </a-form-item>
      <a-form-item name="description" ref="description" :label="$t('label.description')">
        <a-input
          v-model:checked="form.description"
          :placeholder="$t('label.description')"/>
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
            :label="serviceoffering.displaytext || serviceoffering.name">
            {{ serviceoffering.displaytext || serviceoffering.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'updateFileShare',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      configLoading: false,
      serviceofferings: [],
      serviceofferingLoading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateFileShare')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    closeModal () {
      this.$emit('close-action')
    },
    fetchData () {
      this.loading = false
      this.fillEditFormFieldValues()
      this.fetchServiceOfferings()
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.loading = true
      Object.keys(this.apiParams).forEach(item => {
        const field = this.apiParams[item]
        let fieldValue = null
        let fieldName = null

        fieldName = field.name
        fieldValue = this.resource[fieldName] ? this.resource[fieldName] : null
        if (fieldValue) {
          form[field.name] = fieldValue
        }
      })
      this.loading = false
    },
    fetchConfig () {
      this.configLoading = true
      const params1 = {
        zoneid: this.resource.zoneid,
        name: 'storagefsvm.min.cpu.count'
      }
      const params2 = {
        zoneid: this.resource.zoneid,
        name: 'storagefsvm.min.ram.size'
      }
      const apiCall1 = api('listConfigurations', params1)
      const apiCall2 = api('listConfigurations', params2)
      Promise.all([apiCall1, apiCall2])
        .then(([json1, json2]) => {
          const configs1 = json1.listconfigurationsresponse.configuration || []
          const configs2 = json2.listconfigurationsresponse.configuration || []
          if (configs1.length > 0) {
            this.minCpu = parseInt(configs1[0].value) || 0
          } else {
            this.minCpu = 0
          }
          if (configs2.length > 0) {
            this.minMemory = parseInt(configs2[0].value) || 0
          } else {
            this.minMemory = 0
          }
        }).finally(() => {
          this.configLoading = false
        })
    },
    fetchServiceOfferings () {
      this.fetchConfig()
      this.serviceofferingLoading = true
      var params = {
        zoneid: this.resource.zoneid,
        listall: true,
        domainid: this.resource.domainid,
        account: this.resource.account
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
      }).finally(() => {
        this.serviceofferingLoading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          id: this.resource.id,
          name: values.name,
          description: values.description,
          serviceofferingid: values.serviceofferingid
        }
        this.loading = true
        api('updateFileShare', data).then(response => {
          this.$pollJob({
            jobId: response.updatefileshareresponse.jobid,
            title: this.$t('label.update.fileshare'),
            description: values.name,
            successMessage: this.$t('message.success.update.fileshare'),
            errorMessage: this.$t('message.update.fileshare.failed'),
            loadingMessage: this.$t('message.update.fileshare.processing'),
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

  @media (min-width: 760px) {
    width: 500px;
  }
}
</style>
