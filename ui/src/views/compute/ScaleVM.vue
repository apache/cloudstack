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
  <a-form class="form" v-ctrl-enter="handleSubmit">
    <p v-html="getMessage()"></p>

    <div v-if="loading" class="loading">
      <loading-outlined style="color: #1890ff;" />
    </div>

    <a-alert v-if="fixedOfferingKvm" type="error" show-icon>
      <template #message><span style="margin-bottom: 5px" v-html="$t('message.error.fixed.offering.kvm')" /></template>
    </a-alert>

    <compute-offering-selection
      :compute-items="offerings"
      :loading="loading"
      :rowCount="total"
      size="small"
      @select-compute-item="($event) => updateComputeOffering($event)"
      @handle-search-filter="($event) => fetchData($event)" />

    <compute-selection
      v-if="selectedOffering && (selectedOffering.iscustomized || selectedOffering.iscustomizediops)"
      :cpu-number-input-decorator="cpuNumberKey"
      :cpu-speed-input-decorator="cpuSpeedKey"
      :memory-input-decorator="memoryKey"
      :computeOfferingId="selectedOffering.id"
      :isConstrained="'serviceofferingdetails' in selectedOffering"
      :minCpu="getMinCpu()"
      :maxCpu="'serviceofferingdetails' in selectedOffering ? selectedOffering.serviceofferingdetails.maxcpunumber*1 : Number.MAX_SAFE_INTEGER"
      :cpuSpeed="getCPUSpeed()"
      :minMemory="getMinMemory()"
      :maxMemory="'serviceofferingdetails' in selectedOffering ? selectedOffering.serviceofferingdetails.maxmemory*1 : Number.MAX_SAFE_INTEGER"
      :isCustomized="selectedOffering.iscustomized"
      :isCustomizedIOps="'iscustomizediops' in selectedOffering && selectedOffering.iscustomizediops"
      @update-compute-cpunumber="updateFieldValue"
      @update-compute-cpuspeed="updateFieldValue"
      @update-compute-memory="updateFieldValue" />

    <disk-size-selection
      v-if="selectedDiskOffering && (selectedDiskOffering.iscustomized || selectedDiskOffering.iscustomizediops)"
      :inputDecorator="rootDiskSizeKey"
      :minDiskSize="minDiskSize"
      :rootDiskSelected="selectedDiskOffering"
      :isCustomized="selectedDiskOffering.iscustomized"
      @handler-error="handlerError"
      @update-disk-size="updateFieldValue"
      @update-root-disk-iops-value="updateIOPSValue"/>

    <a-form-item :label="$t('label.automigrate.volume')">
      <template #label>
        <tooltip-label :title="$t('label.automigrate.volume')" :tooltip="apiParams.automigrate.description"/>
      </template>
      <a-switch
        v-model:checked="autoMigrate"
        @change="val => { autoMigrate = val }"/>
    </a-form-item>

    <div :span="24" class="action-button">
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { api } from '@/api'
import ComputeOfferingSelection from '@views/compute/wizard/ComputeOfferingSelection'
import ComputeSelection from '@views/compute/wizard/ComputeSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ScaleVM',
  components: {
    ComputeOfferingSelection,
    ComputeSelection,
    DiskSizeSelection,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      offeringsMap: {},
      offerings: [],
      selectedOffering: {},
      selectedDiskOffering: {},
      autoMigrate: true,
      total: 0,
      params: { id: this.resource.id },
      loading: false,
      cpuNumberKey: 'details[0].cpuNumber',
      cpuSpeedKey: 'details[0].cpuSpeed',
      memoryKey: 'details[0].memory',
      rootDiskSizeKey: 'details[0].rootdisksize',
      minIopsKey: 'details[0].minIops',
      maxIopsKey: 'details[0].maxIops',
      fixedOfferingKvm: false,
      minDiskSize: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('scaleVirtualMachine')
  },
  created () {
    this.fetchData({
      keyword: '',
      pageSize: 10,
      page: 1
    })
  },
  methods: {
    fetchData (options) {
      this.loading = true
      this.total = 0
      this.offerings = []
      this.offeringsMap = []
      api('listServiceOfferings', {
        virtualmachineid: this.resource.id,
        keyword: options.keyword,
        page: options.page,
        pageSize: options.pageSize,
        details: 'min',
        response: 'json'
      }).then(response => {
        this.total = response.listserviceofferingsresponse.count
        if (this.total === 0) {
          return
        }
        this.offerings = response.listserviceofferingsresponse.serviceoffering || []
        if (this.resource.state === 'Running' && this.resource.hypervisor === 'KVM') {
          this.offerings = this.offerings.filter(offering => offering.id === this.resource.serviceofferingid)
          this.currentOffer = this.offerings[0]
          if (this.currentOffer === undefined) {
            this.fixedOfferingKvm = true
          }
        }
        this.offerings.map(i => { this.offeringsMap[i.id] = i })
      }).finally(() => {
        this.loading = false
      })
    },
    getMinCpu () {
      // We can only scale up while a VM is running
      if (this.resource.state === 'Running') {
        return this.resource.cpunumber
      }
      return this.selectedOffering?.serviceofferingdetails?.mincpunumber * 1 || 1
    },
    getMinMemory () {
      // We can only scale up while a VM is running
      if (this.resource.state === 'Running') {
        return this.resource.memory
      }
      return this.selectedOffering?.serviceofferingdetails?.minmemory * 1 || 32
    },
    getCPUSpeed () {
      // We can only scale up while a VM is running
      if (this.resource.state === 'Running') {
        return this.resource.cpuspeed
      }
      this.getMinDiskSize()
      return this.selectedOffering?.serviceofferingdetails?.cpuspeed * 1 || 1
    },
    getTemplate () {
      return new Promise((resolve, reject) => {
        api('listTemplates', {
          templatefilter: 'all',
          id: this.resource.templateid
        }).then(response => {
          var template = response?.listtemplatesresponse?.template?.[0] || null
          resolve(template)
        }).catch(error => {
          reject(error)
        })
      })
    },
    async getMinDiskSize () {
      const template = await this.getTemplate()
      this.minDiskSize = Math.ceil(template?.size / (1024 * 1024 * 1024) || 0)
    },
    getMessage () {
      if (this.resource.hypervisor === 'VMware') {
        return this.$t('message.read.admin.guide.scaling.up')
      }
      return this.$t('message.change.offering.confirm')
    },
    updateIOPSValue (input, value) {
      console.log(input)
      const key = input === 'minIops' ? this.minIopsKey : this.maxIopsKey
      this.params[key] = value
    },
    updateComputeOffering (id) {
      // Delete custom details
      delete this.params[this.cpuNumberKey]
      delete this.params[this.cpuSpeedKey]
      delete this.params[this.memoryKey]
      delete this.params[this.rootDiskSizeKey]

      this.params.serviceofferingid = id
      this.selectedOffering = this.offeringsMap[id]
      this.selectedDiskOffering = null
      if (this.selectedOffering.diskofferingid) {
        api('listDiskOfferings', {
          id: this.selectedOffering.diskofferingid
        }).then(response => {
          const diskOfferings = response.listdiskofferingsresponse.diskoffering || []
          if (this.diskOfferings) {
            this.selectedDiskOffering = diskOfferings[0]
          }
        }).catch(error => {
          this.$notifyError(error)
        })
      }
      this.params.automigrate = this.autoMigrate
    },
    updateFieldValue (name, value) {
      this.params[name] = value
    },
    closeAction () {
      this.$emit('close-action')
    },
    handlerError (error) {
      this.error = error
    },
    handleSubmit () {
      if (this.loading) return
      this.loading = true

      if ('cpuspeed' in this.selectedOffering && this.selectedOffering.iscustomized) {
        delete this.params[this.cpuSpeedKey]
      }

      api('scaleVirtualMachine', this.params).then(response => {
        const jobId = response.scalevirtualmachineresponse.jobid
        if (jobId) {
          this.$pollJob({
            jobId,
            title: this.$t('label.scale.vm'),
            description: this.resource.name,
            successMethod: result => {
              this.$notification.success({
                message: this.$t('message.success.change.offering')
              })
            },
            loadingMessage: this.$t('message.scale.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }
        this.$emit('close-action')
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="scss">

.form {
  width: 90vw;
  @media (min-width: 700px) {
    width: 50vw;
  }
}
</style>
