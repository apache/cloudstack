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
  <div class="create-backup-schedule-layout">
    <div v-if="!isVMResource" class="vm-selection">
      <a-form layout="vertical">
        <a-form-item :label="$t('label.virtualmachine')" required>
          <a-select
            v-model:value="selectedVMId"
            :placeholder="$t('label.select.vm')"
            :loading="vmsLoading"
            show-search
            :filter-option="filterOption"
            @change="onVMChange"
          >
            <a-select-option
              v-for="vm in vms"
              :key="vm.id"
              :value="vm.id"
            >
              {{ vm.name }} ({{ vm.account }})
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>
    <div v-if="currentVMResource && currentVMResource.id">
      <BackupScheduleWizard
        ref="backupScheduleWizard"
        :resource="currentVMResource"
        @close-action="closeAction"
        @refresh="handleRefresh"
      />
    </div>
    <div v-if="!currentVMResource || !currentVMResource.id" class="no-vm-selected">
      <div class="empty-state">
        <p>{{ $t('message.select.vm.to.continue') }}</p>
      </div>
    </div>
  </div>
  </template>

<script>
import { getAPI } from '@/api'
import BackupScheduleWizard from '@/views/compute/BackupScheduleWizard'

export default {
  name: 'CreateBackupSchedule',
  components: {
    BackupScheduleWizard
  },
  props: {
    resource: {
      type: Object,
      required: false,
      default: () => null
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      vms: [],
      vmsLoading: false,
      selectedVMId: null,
      selectedVM: null
    }
  },
  computed: {
    resourceType () {
      if (!this.resource) return 'none'

      if (this.resource.vmstate !== undefined ||
        this.resource.guestosid !== undefined ||
        this.resource.hypervisor !== undefined ||
        this.resource.backupofferingid !== undefined ||
        this.resource.serviceofferingid !== undefined) {
        return 'vm'
      }
      if (this.resource.intervaltype !== undefined &&
          this.resource.schedule !== undefined) {
        return 'backupschedule'
      }

      if (this.resource.virtualmachineid !== undefined) {
        return 'backupschedule'
      }

      return 'unknown'
    },
    isVMResource () {
      return this.resourceType === 'vm'
    },
    currentVMResource () {
      if (this.isVMResource) {
        return this.resource
      } else {
        return this.selectedVM
      }
    }
  },
  created () {
    this.fetchVMs()
  },
  methods: {
    async fetchVMs () {
      this.vmsLoading = true
      try {
        const response = await getAPI('listVirtualMachines', { listAll: true })
        const vms = response.listvirtualmachinesresponse.virtualmachine || []
        this.vms = vms.filter(vm => {
          return vm.backupofferingid && ['Running', 'Stopped'].includes(vm.state)
        })
      } catch (error) {
        this.$message.error(this.$t('message.error.fetch.vms'))
        console.error('Error fetching VMs:', error)
      } finally {
        this.vmsLoading = false
      }
    },
    onVMChange (vmId) {
      const vm = this.vms.find(v => v.id === vmId)
      if (vm) {
        this.selectedVM = vm
        this.selectedVMId = vmId
      }
    },
    closeAction () {
      this.$emit('refresh')
      this.$emit('close-action')
    },
    handleRefresh () {
      this.$emit('refresh')
      this.parentFetchData()
    },
    filterOption (input, option) {
      return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  }
}
</script>

<style lang="less" scoped>
.create-backup-schedule-layout {
  .vm-selection {
    margin-bottom: 20px;
    padding: 16px;
    border: 1px solid #d9d9d9;
    border-radius: 6px;
    background-color: #fafafa;
    .ant-form-item {
      margin-bottom: 0;
      .ant-select {
        width: 100%;
        min-width: 400px;
      }
    }
  }

  .current-vm-info {
    margin-bottom: 16px;
  }

  .no-vm-selected {
    text-align: center;
    padding: 40px 20px;
  }
}
</style>
