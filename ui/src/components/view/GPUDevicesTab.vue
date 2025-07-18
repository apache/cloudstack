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
    <!-- Toolbar for bulk actions -->
    <div
      v-if="resourceType === 'Host'"
      style="margin-bottom: 16px;"
    >
      <a-space wrap>
        <!-- Bulk GPU Device Actions -->
        <a-popconfirm
          :title="$t('message.confirm.manage.gpu.devices', { count: selectedDeviceCount })"
          @confirm="bulkManageGpuDevices"
          :disabled="!hasSelectedDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button
            type="primary"
            :disabled="!hasSelectedDevices"
          >
            {{ $t('label.gpu.devices.manage') }}
          </a-button>
        </a-popconfirm>

        <a-popconfirm
          :title="$t('message.confirm.unmanage.gpu.devices', { count: selectedDeviceCount })"
          @confirm="bulkUnmanageGpuDevices"
          :disabled="!hasSelectedDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button
            type="primary"
            danger
            :disabled="!hasSelectedDevices"
          >
            {{ $t('label.gpu.devices.unmanage') }}
          </a-button>
        </a-popconfirm>

        <a-popconfirm
          :title="$t('message.confirm.delete.gpu.devices', { count: selectedDeviceCount })"
          @confirm="bulkDeleteGpuDevices"
          :disabled="!hasSelectedDevices"
          okText="Yes"
          cancelText="No"
        >
          <a-button
            v-if="isAdmin"
            type="primary"
            danger
            :disabled="!hasSelectedDevices"
          >
            {{ $t('label.gpu.devices.delete') }}
          </a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-table
      :loading="loading"
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.id"
      :childrenColumnName="'children'"
      :defaultExpandAllRows="true"
      :expandedRowKeys="expandedRowKeys"
      @expand="onExpand"
      :rowSelection="resourceType === 'Host' && isAdmin ? {
        selectedRowKeys: selectedGpuDeviceIds,
        onChange: onGpuDeviceSelectionChange
      } : null"
      :customRow="customRowProps"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'busaddress'">
          <span :style="{ paddingLeft: record.parentgpudeviceid ? '20px' : '0px' }">
            {{ record.busaddress }}
          </span>
        </template>
        <template v-else-if="column.key === 'virtualmachinename'">
          <div style="display: flex; align-items: center; gap: 8px;">
            <Status
              v-if="record.vmstate"
              :text="record.vmstate"
              :displayText="false"
              :vmState="true"
            />
            <div v-if="record.virtualmachinename">
              <router-link
                v-if="record.virtualmachineid"
                :to="{ path: '/vm/' + record.virtualmachineid }"
              >
                {{ record.virtualmachinename }}
              </router-link>
              <span v-else>{{ record.virtualmachinename }}</span>
            </div>
            <span v-else>{{ record.virtualmachinename }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'gpucardname'">
          <router-link
            v-if="record.gpucardid"
            :to="{ path: '/gpucard/' + record.gpucardid }"
            :title="record.gpucardname"
            class="text-ellipsis"
          >
            {{ record.gpucardname }}
          </router-link>
          <span
            v-else
            :title="record.gpucardname"
            class="text-ellipsis"
          >{{ record.gpucardname }}</span>
        </template>
        <template v-else-if="column.key === 'vgpuprofilename'">
          <span v-if="!(record.children && record.children.length > 0)">
            <router-link
              v-if="record.vgpuprofileid"
              :to="{ path: '/vgpuprofile/' + record.vgpuprofileid }"
              :title="record.vgpuprofilename"
              class="text-ellipsis"
            >
              {{ record.vgpuprofilename }}
            </router-link>
            <span
              v-else
              :title="record.vgpuprofilename"
              class="text-ellipsis"
            >{{ record.vgpuprofilename }}</span>
          </span>
          <span v-else></span>
        </template>
        <template v-else-if="column.key === 'hostname'">
          <router-link
            v-if="record.hostid"
            :to="{ path: '/host/' + record.hostid }"
            :title="record.hostname"
            class="text-ellipsis"
          >
            {{ record.hostname }}
          </router-link>
          <span
            v-else
            :title="record.hostname"
            class="text-ellipsis"
          >{{ record.hostname }}</span>
        </template>
        <template v-else-if="column.key === 'managedstate'">
          <Status
            v-if="!record.children || record.children.length === 0"
            :text="record.managedstate"
            :displayText="true"
          />
          <span v-else></span>
        </template>
        <template v-else-if="column.key === 'state'">
          <Status
            v-if="!record.children || record.children.length === 0"
            :text="record.state"
            :displayText="true"
          />
          <span v-else></span>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space v-if="!record.children || record.children.length === 0">
            <!-- Manage/Unmanage Action -->
            <a-popconfirm
              v-if="record.managedstate && record.managedstate.toLowerCase() === 'unmanaged'"
              :title="$t('message.confirm.manage.gpu.devices')"
              @confirm="manageGpuDevice(record)"
              okText="Yes"
              cancelText="No"
            >
              <a-tooltip :title="$t('label.gpu.devices.manage')">
                <a-button
                  type="primary"
                  size="small"
                  shape="circle"
                >
                  <template #icon><play-circle-outlined /></template>
                </a-button>
              </a-tooltip>
            </a-popconfirm>
            <a-popconfirm
              v-else-if="record.managedstate && record.managedstate.toLowerCase() === 'managed'"
              :title="$t('message.confirm.unmanage.gpu.devices')"
              @confirm="unmanageGpuDevice(record)"
              okText="Yes"
              cancelText="No"
            >
              <a-tooltip :title="$t('label.gpu.devices.unmanage')">
                <a-button
                  size="small"
                  shape="circle"
                >
                  <template #icon><pause-circle-outlined /></template>
                </a-button>
              </a-tooltip>
            </a-popconfirm>

            <!-- Edit Action -->
            <a-tooltip :title="$t('label.edit')">
              <a-button
                type="primary"
                size="small"
                shape="circle"
                @click="showUpdateGpuDeviceModal(record)"
              >
                <template #icon><edit-outlined /></template>
              </a-button>
            </a-tooltip>

            <!-- Delete Action -->
            <a-popconfirm
              :title="$t('message.confirm.delete.gpu.devices')"
              @confirm="deleteGpuDevice(record)"
              okText="Yes"
              cancelText="No"
            >
              <a-tooltip :title="$t('label.delete')">
                <a-button
                  type="primary"
                  danger
                  size="small"
                  shape="circle"
                >
                  <template #icon><delete-outlined /></template>
                </a-button>
              </a-tooltip>
            </a-popconfirm>
          </a-space>
        </template>
      </template>

      <!-- Custom Filter Dropdown for Column Selection -->
      <template #customFilterDropdown="{ column }">
        <div
          v-if="column.key === 'columnFilter'"
          style="padding: 8px; min-width: 200px;"
        >
          <div style="margin-bottom: 8px; font-weight: 500;">{{ $t('label.select.columns') }}</div>
          <div style="margin-bottom: 8px;">
            <a-space>
              <a-button
                size="small"
                @click="selectAllColumns"
              >
                {{ $t('label.select.all') }}
              </a-button>
              <a-button
                size="small"
                @click="clearAllColumns"
              >
                {{ $t('label.clear.all') }}
              </a-button>
            </a-space>
          </div>
          <div style="max-height: 200px; overflow-y: auto;">
            <div
              v-for="columnKey in columnKeys"
              :key="columnKey"
              style="margin-bottom: 4px;"
            >
              <a-checkbox
                :checked="selectedColumnKeys.includes(columnKey)"
                @change="updateSelectedColumns(columnKey)"
              >
                {{ $t('label.' + String(columnKey).toLowerCase()) }}
              </a-checkbox>
            </div>
          </div>
        </div>
      </template>
    </a-table>
  </div>

  <!-- Update GPU Device Modal -->
  <a-modal
    :visible="updateGpuDeviceModalVisible"
    :title="$t('label.update.gpu.device')"
    @ok="updateGpuDevice"
    @cancel="updateGpuDeviceModalVisible = false"
  >
    <a-form layout="vertical">
      <a-form-item
        v-for="field in updateFormFields"
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
</template>

<script>
import { getAPI, postAPI } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import Status from '@/components/widgets/Status'
import { EditOutlined, DeleteOutlined, PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons-vue'

export default {
  name: 'GPUDevicesTab',
  components: {
    Status,
    EditOutlined,
    DeleteOutlined,
    PlayCircleOutlined,
    PauseCircleOutlined
  },
  emits: ['refresh'],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    resourceType: {
      type: String,
      required: true,
      default: 'Host'
    }
  },
  data () {
    return {
      columnKeys: ['gpucardname', 'vgpuprofilename', 'managedstate', 'state'],
      selectedColumnKeys: [],
      columns: [],
      items: [],
      expandedRowKeys: [],
      selectedGpuDeviceIds: [],
      updateGpuDeviceModalVisible: false,
      selectedGpuDevice: null,
      gpuDeviceForm: {},
      gpuCards: [],
      vgpuProfiles: [],
      parentGpuDevices: [],
      loadingGpuCards: false,
      loadingVgpuProfiles: false,
      loadingParentDevices: false,
      updateApiParams: {},
      updateFormFields: []
    }
  },
  computed: {
    hasSelectedDevices () {
      return this.selectedGpuDeviceIds.length > 0
    },
    selectedDeviceCount () {
      return this.selectedGpuDeviceIds.length
    },
    isAdmin () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    }
  },
  created () {
    this.fetchUpdateApiParams()
    this.selectedColumnKeys = this.columnKeys
    if (this.resourceType === 'VirtualMachine') {
      // For VMs: GPU card, GPU profile, BUS ID/uuid, VRAM/metadata, Host (admin only)
      this.columnKeys = ['gpucardname', 'vgpuprofilename']
      if (this.$store.getters.userInfo.roletype === 'Admin') {
        this.columnKeys.push('hostname', 'busaddress', 'managedstate', 'state')
      }
    } else {
      // For other resource types (like Host)
      this.columnKeys = ['busaddress', 'gpucardname', 'vgpuprofilename', 'managedstate', 'state', 'virtualmachinename']
    }
    this.selectedColumnKeys = this.columnKeys
    this.updateColumns()
    this.fetchDevicesData()
  },
  watch: {
    resource: {
      handler () {
        this.fetchDevicesData()
      }
    }
  },
  methods: {
    fetchDevicesData () {
      if (!this.resource.id) {
        return
      }
      // Reset expanded keys when fetching new data
      this.expandedRowKeys = []

      const params = {}
      if (this.resourceType === 'Host') {
        params.hostid = this.resource.id
      } else if (this.resourceType === 'VirtualMachine') {
        params.virtualmachineid = this.resource.id
      }
      getAPI('listGpuDevices', params).then(json => {
        const devices = json?.listgpudevicesresponse?.gpudevice || []
        this.items = this.buildGpuTree(devices)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    buildGpuTree (devices) {
      // Separate parent devices and vGPUs
      const parentDevices = []
      const vgpuDevices = []
      for (const device of devices) {
        if (device.parentgpudeviceid) {
          vgpuDevices.push(device)
        } else {
          parentDevices.push(device)
        }
      }

      // Sort parent devices by busaddress
      parentDevices.sort((a, b) => {
        const busAddressA = a.busaddress || ''
        const busAddressB = b.busaddress || ''
        return busAddressA.localeCompare(busAddressB)
      })

      // Group vGPUs by their parent ID
      const vgpusByParent = {}
      vgpuDevices.forEach(vgpu => {
        const parentId = vgpu.parentgpudeviceid
        if (!vgpusByParent[parentId]) {
          vgpusByParent[parentId] = []
        }
        vgpusByParent[parentId].push(vgpu)
      })

      // Sort vGPUs within each parent group by busaddress
      Object.keys(vgpusByParent).forEach(parentId => {
        vgpusByParent[parentId].sort((a, b) => {
          const busA = a.busaddress || ''
          const busB = b.busaddress || ''
          return busA.localeCompare(busB, undefined, { numeric: true })
        })
      })

      // Build tree structure and collect parent IDs that have children
      const expandedKeys = []
      const treeData = parentDevices.map(parent => {
        const children = vgpusByParent[parent.id] || []
        if (children.length > 0) {
          expandedKeys.push(parent.id)
          return {
            ...parent,
            children: children
          }
        }
        return parent
      })

      // Set expanded row keys for all parents with children
      this.expandedRowKeys = expandedKeys

      if (treeData.length === 0) {
        // Sort standalone vGPU devices by busaddress
        return vgpuDevices.sort((a, b) => {
          const busA = a.busaddress || ''
          const busB = b.busaddress || ''
          return busA.localeCompare(busB, undefined, { numeric: true })
        })
      }

      return treeData
    },
    validateBulkOperation () {
      if (this.selectedGpuDeviceIds.length === 0) {
        this.$notification.warning({
          message: this.$t('label.warning'),
          description: this.$t('message.please.select.gpu.devices')
        })
        return false
      }
      return true
    },
    handleBulkOperationSuccess (messageKey, count) {
      this.$notification.success({
        message: this.$t('label.success'),
        description: this.$t(messageKey, { count })
      })
      this.selectedGpuDeviceIds = []
      this.refresh()
    },
    updateSelectedColumns (key) {
      if (this.selectedColumnKeys.includes(key)) {
        this.selectedColumnKeys = this.selectedColumnKeys.filter(x => x !== key)
      } else {
        this.selectedColumnKeys.push(key)
      }
      this.updateColumns()
    },
    updateColumns () {
      this.columns = []
      for (var columnKey of this.columnKeys) {
        if (!this.selectedColumnKeys.includes(columnKey)) continue
        this.columns.push({
          key: columnKey,
          title: this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey,
          sorter: (a, b) => { return genericCompare(a[columnKey] || '', b[columnKey] || '') }
        })
      }

      // Add actions column for admin users (only for Host resource type)
      if (this.isAdmin && this.resourceType === 'Host') {
        this.columns.push({
          key: 'actions',
          title: this.$t('label.actions'),
          width: 120,
          fixed: 'right'
        })
      }

      // Add column filter as the last column
      this.columns.push({
        key: 'columnFilter',
        title: null,
        dataIndex: 'columnFilter',
        customFilterDropdown: true,
        onFilter: () => true
      })
    },
    onGpuDeviceSelectionChange (keys) {
      this.selectedGpuDeviceIds = keys
    },
    onExpand (expanded, record) {
      if (expanded) {
        if (!this.expandedRowKeys.includes(record.id)) {
          this.expandedRowKeys.push(record.id)
        }
      } else {
        this.expandedRowKeys = this.expandedRowKeys.filter(key => key !== record.id)
      }
    },
    customRowProps (record) {
      return {
        class: record.parentgpudeviceid ? 'vgpu-row' : 'parent-gpu-row'
      }
    },
    selectAllColumns () {
      this.selectedColumnKeys = this.columnKeys
      this.updateColumns()
    },
    clearAllColumns () {
      this.selectedColumnKeys = []
      this.updateColumns()
    },
    bulkManageGpuDevices () {
      if (!this.validateBulkOperation()) return

      getAPI('manageGpuDevice', {
        ids: this.selectedGpuDeviceIds.join(',')
      }).then(() => {
        this.handleBulkOperationSuccess('message.success.manage.gpu.devices', this.selectedGpuDeviceIds.length)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    bulkUnmanageGpuDevices () {
      if (!this.validateBulkOperation()) return

      getAPI('unmanageGpuDevice', {
        ids: this.selectedGpuDeviceIds.join(',')
      }).then(() => {
        this.handleBulkOperationSuccess('message.success.unmanage.gpu.devices', this.selectedGpuDeviceIds.length)
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    manageGpuDevice (record) {
      getAPI('manageGpuDevice', {
        ids: record.id
      }).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t('message.success.manage.gpu.devices')
        })
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    unmanageGpuDevice (record) {
      getAPI('unmanageGpuDevice', {
        ids: record.id
      }).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t('message.success.unmanage.gpu.devices')
        })
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    filterOption (input, option) {
      return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    fetchUpdateApiParams () {
      this.updateApiParams = this.$getApiParams('updateGpuDevice') || {}
      this.generateUpdateFormFields()
    },
    generateUpdateFormFields () {
      // Generate update form fields
      this.updateFormFields = this.buildFormFieldsFromParams(this.updateApiParams, 'update')
    },
    buildFormFieldsFromParams (apiParams, formType) {
      const fields = []
      const fieldOrder = ['gpucardid', 'vgpuprofileid', 'type', 'parentgpudeviceid']

      fieldOrder.forEach(paramName => {
        if (paramName === 'hostid') return // Skip hostid as it's auto-populated

        const param = apiParams[paramName]
        if (!param) return

        const field = {
          key: paramName,
          label: this.$t(`label.${paramName}`),
          required: param.required,
          placeholder: param.description,
          type: this.getFieldType(paramName, param)
        }

        // Add special configurations for dropdown fields
        if (field.type === 'select') {
          field.options = this.getFieldOptions(paramName)
          field.loading = this.getFieldLoading(paramName)
          field.showSearch = true
          field.allowClear = true

          if (paramName === 'gpucardid') {
            field.onChange = this.onGpuCardChange
          }
        }

        fields.push(field)
      })

      return fields
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
        this.generateUpdateFormFields() // Refresh form fields with new data
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
        this.generateUpdateFormFields() // Refresh form fields with new data
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
        this.generateUpdateFormFields() // Refresh form fields with new data
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingParentDevices = false
      })
    },

    showUpdateGpuDeviceModal (record) {
      this.selectedGpuDevice = record
      this.gpuDeviceForm = {
        gpucardid: record.gpucardid,
        vgpuprofileid: record.vgpuprofileid,
        type: record.type,
        numanode: record.numanode,
        parentgpudeviceid: record.parentgpudeviceid
      }
      this.fetchGpuCards()
      // Fetch vGPU profiles for the selected card if available
      if (record.gpucardid) {
        this.fetchVgpuProfiles(record.gpucardid)
      } else {
        this.vgpuProfiles = []
      }
      this.fetchParentGpuDevices()
      this.updateGpuDeviceModalVisible = true
    },

    updateGpuDevice () {
      const params = {
        id: this.selectedGpuDevice.id
      }

      // Add only fields that have values
      if (this.gpuDeviceForm.gpucardid) {
        params.gpucardid = this.gpuDeviceForm.gpucardid
      }
      if (this.gpuDeviceForm.vgpuprofileid) {
        params.vgpuprofileid = this.gpuDeviceForm.vgpuprofileid
      }
      if (this.gpuDeviceForm.type) {
        params.type = this.gpuDeviceForm.type
      }
      if (this.gpuDeviceForm.numanode) {
        params.numanode = this.gpuDeviceForm.numanode
      }
      if (this.gpuDeviceForm.parentgpudeviceid) {
        params.parentgpudeviceid = this.gpuDeviceForm.parentgpudeviceid
      }

      postAPI('updateGpuDevice', params).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t('message.success.update.gpu.device')
        })
        this.updateGpuDeviceModalVisible = false
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    deleteGpuDevice (record) {
      this.deleteGpuDevices([record.id], 'message.success.delete.gpu.devices')
    },
    bulkDeleteGpuDevices () {
      if (!this.validateBulkOperation()) return
      this.deleteGpuDevices(this.selectedGpuDeviceIds, 'message.success.delete.gpu.devices')
    },
    deleteGpuDevices (deviceIds, successMessageKey) {
      postAPI('deleteGpuDevice', {
        ids: deviceIds.join(',')
      }).then(() => {
        this.$notification.success({
          message: this.$t('label.success'),
          description: this.$t(successMessageKey, { count: deviceIds.length })
        })
        if (deviceIds.length > 1) {
          this.selectedGpuDeviceIds = []
        }
        this.refresh()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    refresh () {
      this.fetchDevicesData()
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
