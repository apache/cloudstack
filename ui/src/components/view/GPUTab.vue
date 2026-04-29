// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <div>
    <!-- GPU Device Management Buttons (before tabs, for Host resource type only) -->
    <div
      v-if="resourceType === 'Host'"
      style="margin-bottom: 16px;"
    >
      <a-space wrap>
        <!-- GPU Device Discovery -->
        <a-popconfirm
          :title="$t('message.confirm.discover.gpu.devices')"
          @confirm="discoverGpuDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button type="default">
            {{ $t('label.discover.gpu.devices') }}
          </a-button>
        </a-popconfirm>

        <!-- Add GPU Device (Admin Only) -->
        <a-button
          v-if="isAdmin"
          type="primary"
          @click="showAddGpuDeviceModal"
        >
          {{ $t('label.gpu.devices.add') }}
        </a-button>
      </a-space>
    </div>

    <!-- For VMs: Show tabs only for admin, summary only for regular users -->
    <div v-if="resourceType === 'VirtualMachine' && !isAdmin">
      <!-- Summary only for non-admin users viewing VMs -->
      <GPUSummaryTab
        ref="summaryTabSimple"
        :resource="resource"
        :resourceType="resourceType"
        :loading="loading"
        @refresh="$emit('refresh')"
      />
    </div>

    <!-- For admins on VMs or all users on other resource types: Show tabs -->
    <a-tabs
      v-else
      defaultActiveKey="summary"
      :tabBarStyle="{ marginBottom: '16px' }"
    >
      <!-- Summary Tab -->
      <a-tab-pane
        key="summary"
        :tab="$t('label.gpu.summary')"
      >
        <GPUSummaryTab
          ref="summaryTab"
          :resource="resource"
          :resourceType="resourceType"
          :loading="loading"
          @refresh="$emit('refresh')"
        />
      </a-tab-pane>

      <!-- Devices Tab -->
      <a-tab-pane
        key="devices"
        :tab="$t('label.gpu.devices')"
      >
        <GPUDevicesTab
          ref="devicesTab"
          :resource="resource"
          :resourceType="resourceType"
          :loading="loading"
          @refresh="refresh(true)"
        />
      </a-tab-pane>
    </a-tabs>

    <!-- Add GPU Device Modal -->
    <a-modal
      :visible="addGpuDeviceModalVisible"
      :title="$t('label.gpu.devices.add')"
      @ok="addGpuDevice"
      @cancel="addGpuDeviceModalVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item
          v-for="field in createFormFields"
          :key="field.key"
          :label="field.label"
          :required="field.required"
        >
          <!-- Input field -->
          <a-input
            v-if="field.type === 'input'"
            v-model:value="gpuDeviceForm[field.key]"
            :placeholder="field.placeholder"
          />

          <!-- Select field -->
          <a-select
            v-else-if="field.type === 'select'"
            v-model:value="gpuDeviceForm[field.key]"
            :placeholder="field.placeholder"
            :loading="field.loading"
            :show-search="field.showSearch"
            :filter-option="filterOption"
            :allow-clear="field.allowClear"
            @change="field.onChange"
          >
            <a-select-option
              v-for="option in field.options"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import GPUSummaryTab from '@/components/view/GPUSummaryTab'
import GPUDevicesTab from '@/components/view/GPUDevicesTab'

export default {
  name: 'GPUTab',
  components: {
    GPUSummaryTab,
    GPUDevicesTab
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    },
    resourceType: {
      type: String,
      required: true,
      default: 'Host'
    }
  },
  data () {
    return {
      addGpuDeviceModalVisible: false,
      gpuDeviceForm: {},
      gpuCards: [],
      vgpuProfiles: [],
      parentGpuDevices: [],
      loadingGpuCards: false,
      loadingVgpuProfiles: false,
      loadingParentDevices: false,
      createApiParams: {},
      createFormFields: []
    }
  },
  computed: {
    isAdmin () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    }
  },
  created () {
    this.fetchApiParams()
  },
  methods: {
    discoverGpuDevices () {
      if (!this.resource.id) {
        return
      }

      getAPI('discoverGpuDevices', {
        id: this.resource.id
      }).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t('message.success.discover.gpu.devices')
        })
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    fetchApiParams () {
      this.createApiParams = this.$getApiParams('createGpuDevice') || {}
      this.generateFormFields()
    },
    generateFormFields () {
      const fields = []
      const fieldOrder = ['busaddress', 'gpucardid', 'vgpuprofileid', 'type', 'numanode', 'parentgpudeviceid']

      fieldOrder.forEach(paramName => {
        if (paramName === 'hostid') return // Skip hostid as it's auto-populated

        const param = this.createApiParams[paramName]
        if (!param) return

        const field = {
          key: paramName,
          label: this.$t(`label.${paramName}`),
          required: param.required || ['busaddress', 'gpucardid', 'vgpuprofileid'].includes(paramName),
          placeholder: param.description,
          type: this.getFieldType(paramName, param)
        }

        // Add special configurations for dropdown fields
        if (field.type === 'select') {
          field.options = this.getFieldOptions(paramName)
          field.loading = this.getFieldLoading(paramName)
          field.showSearch = true
          field.allowClear = false

          if (paramName === 'gpucardid') {
            field.onChange = this.onGpuCardChange
          }
        }

        fields.push(field)
      })

      this.createFormFields = fields
    },
    getFieldType (paramName, param) {
      const selectFields = ['gpucardid', 'vgpuprofileid', 'parentgpudeviceid', 'type']
      return selectFields.includes(paramName) ? 'select' : 'input'
    },
    getFieldOptions (paramName) {
      switch (paramName) {
        case 'gpucardid':
          return this.gpuCards.map(card => ({ value: card.id, label: card.name }))
        case 'vgpuprofileid':
          return this.vgpuProfiles.map(profile => ({ value: profile.id, label: profile.name }))
        case 'parentgpudeviceid':
          return this.parentGpuDevices.map(device => ({
            value: device.id,
            label: `${device.gpucardname} - ${device.busaddress}`
          }))
        case 'type':
          return [
            { value: 'PCI', label: 'PCI' },
            { value: 'MDEV', label: 'MDEV' },
            { value: 'VGPUOnly', label: 'VGPUOnly' }
          ]
        default:
          return []
      }
    },
    getFieldLoading (paramName) {
      switch (paramName) {
        case 'gpucardid':
          return this.loadingGpuCards
        case 'vgpuprofileid':
          return this.loadingVgpuProfiles
        case 'parentgpudeviceid':
          return this.loadingParentDevices
        default:
          return false
      }
    },
    onGpuCardChange (gpucardid) {
      // Clear the selected vGPU profile when GPU card changes
      this.gpuDeviceForm.vgpuprofileid = null
      // Fetch vGPU profiles for the selected GPU card
      if (gpucardid) {
        this.fetchVgpuProfiles(gpucardid)
      } else {
        this.vgpuProfiles = []
      }
    },
    fetchGpuCards () {
      this.loadingGpuCards = true
      getAPI('listGpuCards').then(json => {
        this.gpuCards = json?.listgpucardsresponse?.gpucard || []
        this.generateFormFields() // Refresh form fields with new data
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingGpuCards = false
      })
    },
    fetchVgpuProfiles (gpucardid = null) {
      this.loadingVgpuProfiles = true
      const params = {}
      if (gpucardid) {
        params.gpucardid = gpucardid
      }
      getAPI('listVgpuProfiles', params).then(json => {
        this.vgpuProfiles = json?.listvgpuprofilesresponse?.vgpuprofile || []
        this.generateFormFields() // Refresh form fields with new data
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingVgpuProfiles = false
      })
    },
    fetchParentGpuDevices () {
      if (!this.resource.id || this.resourceType !== 'Host') {
        return
      }
      this.loadingParentDevices = true
      getAPI('listGpuDevices', { hostid: this.resource.id }).then(json => {
        const devices = json?.listgpudevicesresponse?.gpudevice || []
        // Only include devices that can be parent devices (not virtual GPU devices)
        this.parentGpuDevices = devices.filter(device => !device.parentgpudeviceid)
        this.generateFormFields() // Refresh form fields with new data
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingParentDevices = false
      })
    },
    filterOption (input, option) {
      return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    showAddGpuDeviceModal () {
      this.gpuDeviceForm = {
        type: 'PCI' // Set default type
      }
      this.vgpuProfiles = [] // Clear profiles initially
      this.fetchGpuCards()
      this.fetchParentGpuDevices()
      this.addGpuDeviceModalVisible = true
    },
    addGpuDevice () {
      // Validate required fields
      if (!this.gpuDeviceForm.busaddress || !this.gpuDeviceForm.gpucardid || !this.gpuDeviceForm.vgpuprofileid) {
        this.$notification.warning({
          message: this.$t('label.warning'),
          description: this.$t('message.please.fill.required.fields')
        })
        return
      }

      const params = {
        hostid: this.resource.id,
        busaddress: this.gpuDeviceForm.busaddress,
        gpucardid: this.gpuDeviceForm.gpucardid,
        vgpuprofileid: this.gpuDeviceForm.vgpuprofileid
      }

      // Add optional parameters only if they have values
      if (this.gpuDeviceForm.type) {
        params.type = this.gpuDeviceForm.type
      }
      if (this.gpuDeviceForm.numanode) {
        params.numanode = this.gpuDeviceForm.numanode
      }
      if (this.gpuDeviceForm.parentgpudeviceid) {
        params.parentgpudeviceid = this.gpuDeviceForm.parentgpudeviceid
      }

      postAPI('createGpuDevice', params).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t('message.success.add.gpu.device')
        })
        this.addGpuDeviceModalVisible = false
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    refresh (skipDevicesTab = false) {
      // Refresh summary tabs
      const summaryTab = this.$refs.summaryTab || this.$refs.summaryTabSimple
      if (summaryTab?.refresh) {
        summaryTab.refresh()
      }
      // Refresh devices tab only if not skipped
      if (!skipDevicesTab && this.$refs.devicesTab?.refresh) {
        this.$refs.devicesTab.refresh()
      }
      // Emit refresh to parent
      this.$emit('refresh')
    }
  }
}
</script>

<style scoped>
/* Background colors for GPU device types */
:deep(.parent-gpu-row) {
  background-color: #fafafa;
}

:deep(.vgpu-row) {
  background-color: #f0f8ff;
}

:deep(.parent-gpu-row:hover) {
  background-color: #f5f5f5 !important;
}

:deep(.vgpu-row:hover) {
  background-color: #e6f4ff !important;
}

/* Text truncation for long names */
:deep(.ant-table-tbody .ant-table-cell) {
  max-width: 200px;
}

:deep(.ant-table-tbody .ant-table-cell:has([data-column="gpucardname"]),
  .ant-table-tbody .ant-table-cell:has([data-column="vgpuprofilename"]),
  .ant-table-tbody .ant-table-cell:has([data-column="hostname"])) {
  max-width: 150px;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  display: inline-block;
}
</style>
