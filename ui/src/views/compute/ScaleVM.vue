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
  <a-form class="form">
    <p v-html="getMessage()"></p>

    <div v-if="loading" class="loading">
      <a-icon type="loading" style="color: #1890ff;"></a-icon>
    </div>

    <compute-offering-selection
      :compute-items="offerings"
      :loading="loading"
      :rowCount="total"
      size="small"
      @select-compute-item="($event) => updateComputeOffering($event)"
      @handle-search-filter="($event) => fetchData($event)" />

    <compute-selection
      v-if="selectedOffering && selectedOffering.iscustomized"
      :cpunumber-input-decorator="cpuNumberKey"
      :cpuspeed-input-decorator="cpuSpeedKey"
      :memory-input-decorator="memoryKey"
      :computeOfferingId="selectedOffering.id"
      :isConstrained="'serviceofferingdetails' in selectedOffering"
      :minCpu="getMinCpu()"
      :maxCpu="'serviceofferingdetails' in selectedOffering ? selectedOffering.serviceofferingdetails.maxcpunumber*1 : Number.MAX_SAFE_INTEGER"
      :minMemory="getMinMemory()"
      :maxMemory="'serviceofferingdetails' in selectedOffering ? selectedOffering.serviceofferingdetails.maxmemory*1 : Number.MAX_SAFE_INTEGER"
      @update-compute-cpunumber="updateFieldValue"
      @update-compute-cpuspeed="updateFieldValue"
      @update-compute-memory="updateFieldValue" />

    <div :span="24" class="action-button">
      <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
      <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { api } from '@/api'
import ComputeOfferingSelection from '@views/compute/wizard/ComputeOfferingSelection'
import ComputeSelection from '@views/compute/wizard/ComputeSelection'

export default {
  name: 'ScaleVM',
  components: {
    ComputeOfferingSelection,
    ComputeSelection
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
      total: 0,
      params: { id: this.resource.id },
      loading: false,
      cpuNumberKey: 'details[0].cpuNumber',
      cpuSpeedKey: 'details[0].cpuSpeed',
      memoryKey: 'details[0].memory'
    }
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
        this.offerings = response.listserviceofferingsresponse.serviceoffering
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
      return 'serviceofferingdetails' in this.selectedOffering ? this.selectedOffering.serviceofferingdetails.mincpunumber * 1 : 1
    },
    getMinMemory () {
      // We can only scale up while a VM is running
      if (this.resource.state === 'Running') {
        return this.resource.memory
      }
      return 'serviceofferingdetails' in this.selectedOffering ? this.selectedOffering.serviceofferingdetails.minmemory * 1 : 32
    },
    getMessage () {
      if (this.resource.hypervisor === 'VMware') {
        return this.$t('message.read.admin.guide.scaling.up')
      }
      return this.$t('message.change.offering.confirm')
    },
    updateComputeOffering (id) {
      // Delete custom details
      delete this.params[this.cpuNumberKey]
      delete this.params[this.cpuSpeedKey]
      delete this.params[this.memoryKey]

      this.params.serviceofferingid = id
      this.selectedOffering = this.offeringsMap[id]
    },
    updateFieldValue (name, value) {
      this.params[name] = value
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit () {
      this.loading = true

      if ('cpuspeed' in this.selectedOffering && this.selectedOffering.iscustomized) {
        delete this.params[this.cpuSpeedKey]
      }

      api('scaleVirtualMachine', this.params).then(response => {
        const jobId = response.scalevirtualmachineresponse.jobid
        if (jobId) {
          this.$pollJob({
            jobId,
            successMethod: result => {
              this.$notification.success({
                message: this.$t('message.success.change.offering')
              })
            },
            loadingMessage: this.$t('message.scale.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }
        this.$parent.$parent.close()
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

.action-button {
  margin-top: 10px;
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
