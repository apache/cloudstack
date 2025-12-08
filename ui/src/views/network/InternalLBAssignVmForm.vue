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
  <a-spin :spinning="fetchLoading">
    <div v-ctrl-enter="handleSubmit">
      <div class="vm-modal__header">
        <span style="min-width: 200px;">{{ $t('label.name') }}</span>
        <span>{{ $t('label.state') }}</span>
        <span>{{ $t('label.instancename') }}</span>
        <span>{{ $t('label.displayname') }}</span>
        <span>{{ $t('label.ip') }}</span>
        <span>{{ $t('label.account') }}</span>
        <span>{{ $t('label.zonenamelabel') }}</span>
        <span>{{ $t('label.select') }}</span>
      </div>

      <a-checkbox-group style="width: 100%;">
        <div v-for="(vm, index) in vms" :key="index" class="vm-modal__item">
          <span style="min-width: 200px;">
            <span>
              {{ vm.name }}
            </span>
            <loading-outlined v-if="addVmModalNicLoading"  />
            <a-select
              v-focus="!addVmModalNicLoading && iLb.virtualmachineid[index] === vm.id && index === 0"
              v-else-if="!addVmModalNicLoading && iLb.virtualmachineid[index] === vm.id"
              mode="multiple"
              style="min-width: 200px;"
              v-model:value="iLb.vmguestip[index]"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="(nic, nicIndex) in nics[index]"
                :key="nic"
                :value="nic"
                :label="nic">
                {{ nic }}{{ nicIndex === 0 ? ` (${this.$t('label.primary')})` : '' }}
              </a-select-option>
            </a-select>
          </span>
          <span><status :text="vm.state" displayText /></span>
          <span>{{ vm.instancename }}</span>
          <span>{{ vm.displayname }}</span>
          <span></span>
          <span>{{ vm.account }}</span>
          <span>{{ vm.zonename }}</span>
          <a-checkbox
            v-focus="!(!addVmModalNicLoading && iLb.virtualmachineid[index] === vm.id) && index === 0"
            :value="vm.id"
            @change="e => fetchNics(e, index)" />
        </div>
        <a-divider/>
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="vmCounts"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </a-checkbox-group>
    </div>
    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" ref="submit" @click="handleSubmit">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'InternalLBAssignVmForm',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      page: 1,
      pageSize: 10,
      vmCounts: 0,
      addVmModalNicLoading: false,
      vms: [],
      nics: [],
      params: {},
      assignedVMs: [],
      iLb: {
        virtualmachineid: [],
        vmguestip: []
      },
      fetchLoading: false
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchLoadBalancers()
      this.fetchVirtualMachines()
    },
    fetchLoadBalancers () {
      this.fetchLoading = true
      api('listLoadBalancers', {
        id: this.resource.id
      }).then(response => {
        const lb = response.listloadbalancersresponse.loadbalancer
        this.assignedVMs = []
        if (Array.isArray(lb) && lb.length) {
          this.assignedVMs = lb[0].loadbalancerinstance || []
        }
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    differenceBy (array1, array2, key) {
      return array1.filter(a => !array2.some(b => b[key] === a[key]))
    },
    fetchVirtualMachines () {
      this.fetchLoading = true
      api('listVirtualMachines', {
        listAll: true,
        networkid: this.resource.networkid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        var vms = response.listvirtualmachinesresponse.virtualmachine || []
        this.vms = this.differenceBy(vms, this.assignedVMs, 'id')
        this.vmCounts = this.vms.length || 0
        this.vms.forEach((vm, index) => {
          this.iLb.virtualmachineid[index] = null
          this.nics[index] = null
          this.iLb.vmguestip[index] = null
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchNics (e, index) {
      if (!e.target.checked) {
        this.iLb.virtualmachineid[index] = null
        this.nics[index] = null
        this.iLb.vmguestip[index] = null
        return
      }
      this.iLb.virtualmachineid[index] = e.target.value
      this.addVmModalNicLoading = true
      api('listNics', {
        virtualmachineid: e.target.value,
        networkid: this.resource.networkid
      }).then(response => {
        if (!response.listnicsresponse.nic[0]) return
        const newItem = []
        newItem.push(response.listnicsresponse.nic[0].ipaddress)
        if (response.listnicsresponse.nic[0].secondaryip) {
          newItem.push(...response.listnicsresponse.nic[0].secondaryip.map(ip => ip.ipaddress))
        }
        this.nics[index] = newItem
        this.iLb.vmguestip[index] = this.nics[index][0]
        this.addVmModalNicLoading = false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit () {
      if (this.fetchLoading) {
        return
      }
      var j = 0
      this.params = {}
      for (var i = 0; i < this.iLb.virtualmachineid.length; i++) {
        if (this.iLb.virtualmachineid[i] !== null) {
          this.params['vmidipmap[' + j + '].vmid'] = this.iLb.virtualmachineid[i]
          this.params['vmidipmap[' + j + '].vmip'] = this.iLb.vmguestip[i]
          j++
        }
      }
      this.params.id = this.resource.id
      this.fetchLoading = true
      api('assignToLoadBalancerRule', this.params).then(response => {
        this.$pollJob({
          jobId: response.assigntoloadbalancerruleresponse.jobid,
          successMessage: `${this.$t('message.success.assigned.vms')} ${this.$t('label.to')} ${this.resource.name}`,
          errorMessage: `${this.$t('message.failed.to.assign.vms')} ${this.$t('label.to')} ${this.resource.name}`,
          loadingMessage: `${this.$t('label.assigning.vms')} ${this.$t('label.to')} ${this.resource.name}`,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
        this.closeModal()
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>

.vm-modal {

  &__header {
    display: flex;

    span {
      flex: 1;
      font-weight: bold;
      margin-right: 10px;
    }
  }

  &__item {
    display: flex;
    margin-top: 10px;

    span,
    label {
      display: block;
      flex: 1;
      margin-right: 10px;
    }
  }
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;

  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
