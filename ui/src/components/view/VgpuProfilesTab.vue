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
    <a-button
      type="primary"
      @click="createVgpuProfile"
    >
      {{ $t('label.add.vgpu.profile') }}
    </a-button>
    <list-view
      :tabLoading="tabLoading"
      :columns="columns"
      :items="items"
      :columnKeys="columnKeys"
      :selectedColumns="selectedColumnKeys"
      ref="listview"
      @update-selected-columns="updateSelectedColumns"
      @refresh="this.fetchData"
    >
      <template #actionButtons="{ record }">
        <a-space>
          <!-- Edit Action -->
          <a-tooltip :title="$t('label.edit')">
            <a-button
              type="primary"
              size="small"
              shape="circle"
              @click="editVgpuProfile(record)"
            >
              <template #icon><edit-outlined /></template>
            </a-button>
          </a-tooltip>

          <!-- Delete Action -->
          <a-popconfirm
            :title="$t('message.confirm.delete.vgpu.profile')"
            @confirm="deleteVgpuProfile(record)"
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
    </list-view>

    <!-- Create/Update VGPU Profile Modal -->
    <a-modal
      :visible="showModal"
      :title="isEditing ? $t('label.update.vgpu.profile') : $t('label.add.vgpu.profile')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="closeModal"
      centered
      width="450px"
    >
      <a-spin
        :spinning="actionLoading"
        v-ctrl-enter="handleSubmit"
      >
        <a-form
          ref="vgpuForm"
          :model="form"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item
            v-for="field in formFields"
            :key="field.key"
            :name="field.key"
            :ref="field.key"
            :label="field.label"
          >
            <!-- Input field -->
            <a-input
              v-if="field.type === 'input'"
              v-model:value="form[field.key]"
              :placeholder="field.placeholder"
              :v-focus="field.key === 'name'"
            />

            <!-- Number input field -->
            <a-input-number
              v-else-if="field.type === 'number'"
              style="width: 100%"
              v-model:value="form[field.key]"
              :min="field.min"
              :placeholder="field.placeholder"
            />
          </a-form-item>

          <div
            :span="24"
            class="action-button"
          >
            <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
            <a-button
              type="primary"
              ref="submit"
              @click="handleSubmit"
            >{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'

export default {
  name: 'VgpuProfilesTab',
  components: {
    ListView,
    EditOutlined,
    DeleteOutlined
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      tabLoading: false,
      columnKeys: ['name', 'videoram', 'maxheads', 'resolution', 'maxvgpuperphysicalgpu'],
      selectedColumnKeys: [],
      columns: [],
      cols: [],
      items: [],
      showModal: false,
      isEditing: false,
      selectedVgpuProfile: null,
      actionLoading: false,
      form: {},
      rules: {},
      formFields: [],
      createApiParams: {},
      updateApiParams: {}
    }
  },
  computed: {
    isAdmin () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    }
  },
  created () {
    this.fetchApiParams()
    this.selectedColumnKeys = this.columnKeys
    this.updateColumns()
    this.fetchData()
  },
  methods: {
    fetchApiParams () {
      this.createApiParams = this.$getApiParams('createVgpuProfile') || {}
      this.updateApiParams = this.$getApiParams('updateVgpuProfile') || {}
    },
    generateFormFields (isEdit = false) {
      const apiParams = isEdit ? this.updateApiParams : this.createApiParams
      const fields = []
      const fieldOrder = ['name', 'description', 'videoram', 'maxheads', 'maxresolutionx', 'maxresolutiony', 'maxvgpuperphysicalgpu']

      fieldOrder.forEach(paramName => {
        if (paramName === 'gpucardid' && !isEdit) return // Skip gpucardid for create as it's auto-populated

        const param = apiParams[paramName]
        if (!param && !isEdit) return // For create, only include params that exist in API

        const field = {
          key: paramName,
          label: this.$t(`label.${paramName}`),
          required: param?.required || (paramName === 'name' || paramName === 'maxvgpuperphysicalgpu'),
          placeholder: param?.description || this.$t(`label.${paramName}`),
          type: this.getFieldType(paramName, param)
        }

        fields.push(field)
      })

      this.formFields = fields
    },
    getFieldType (paramName, param) {
      const numberFields = ['maxvgpuperphysicalgpu']
      const selectFields = []

      if (numberFields.includes(paramName)) {
        return 'number'
      } else if (selectFields.includes(paramName)) {
        return 'select'
      } else {
        return 'input'
      }
    },
    initForm (isEdit = false, record = null) {
      const formData = {
        name: '',
        description: '',
        maxvgpuperphysicalgpu: 1
      }

      if (isEdit && record) {
        formData.name = record.name || ''
        formData.description = record.description || ''
        formData.maxvgpuperphysicalgpu = record.maxvgpuperphysicalgpu || 1
        formData.videoram = record.videoram || 0
        formData.maxheads = record.maxheads || 0
        formData.maxresolutionx = record.maxresolutionx || 0
        formData.maxresolutiony = record.maxresolutiony || 0
      }

      this.form = reactive(formData)

      const validationRules = {
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        maxvgpuperphysicalgpu: [{ required: true, message: this.$t('message.error.required.input') }]
      }

      this.rules = reactive(validationRules)
    },
    createVgpuProfile () {
      this.isEditing = false
      this.selectedVgpuProfile = null
      this.generateFormFields(false)
      this.initForm(false)
      this.showModal = true
    },
    editVgpuProfile (record) {
      this.isEditing = true
      this.selectedVgpuProfile = record
      this.generateFormFields(true)
      this.initForm(true, record)
      this.showModal = true
    },
    closeModal () {
      this.showModal = false
      this.isEditing = false
      this.selectedVgpuProfile = null
      this.actionLoading = false
    },
    filterOption (input, option) {
      return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    handleSubmit (e) {
      if (e) {
        e.preventDefault()
      }
      if (this.actionLoading) return

      this.$refs.vgpuForm.validate().then(() => {
        this.actionLoading = true
        const formRaw = toRaw(this.form)

        if (this.isEditing) {
          this.updateVgpuProfile(formRaw)
        } else {
          this.createVgpuProfileSubmit(formRaw)
        }
      }).catch((error) => {
        this.$message.error(error)
      })
    },
    createVgpuProfileSubmit (formRaw) {
      const params = {
        name: formRaw.name,
        gpucardid: this.resource.id,
        maxvgpuperphysicalgpu: formRaw.maxvgpuperphysicalgpu
      }

      if (formRaw.description) {
        params.description = formRaw.description
      }

      postAPI('createVgpuProfile', params).then(response => {
        this.$message.success(this.$t('message.success.create.vgpu.profile'))
        this.closeModal()
        this.fetchData()
      }).catch(error => {
        console.error('Error creating vGPU profile:', error)
        this.$notifyError(error)
      }).finally(() => {
        this.actionLoading = false
      })
    },
    updateVgpuProfile (formRaw) {
      const params = {
        id: this.selectedVgpuProfile.id
      }

      // Add only fields that have values
      if (formRaw.name) {
        params.name = formRaw.name
      }
      if (formRaw.description) {
        params.description = formRaw.description
      }
      if (formRaw.maxvgpuperphysicalgpu) {
        params.maxvgpuperphysicalgpu = formRaw.maxvgpuperphysicalgpu
      }

      postAPI('updateVgpuProfile', params).then(() => {
        this.$message.success(this.$t('message.success.update.vgpu.profile'))
        this.closeModal()
        this.fetchData()
      }).catch(error => {
        console.error('Error updating vGPU profile:', error)
        this.$notifyError(error)
      }).finally(() => {
        this.actionLoading = false
      })
    },
    deleteVgpuProfile (record) {
      postAPI('deleteVgpuProfile', {
        id: record.id
      }).then(() => {
        this.$message.success(this.$t('message.success.delete.vgpu.profile'))
        this.fetchData()
      }).catch(error => {
        console.error('Error deleting vGPU profile:', error)
        this.$notifyError(error)
      })
    },
    fetchData () {
      this.fetchVgpuProfiles()
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
          title: columnKey === 'name' ? this.$t('label.vgpu.profile') : this.$t('label.' + String(columnKey).toLowerCase()),
          dataIndex: columnKey,
          sorter: (a, b) => { return genericCompare(a[columnKey] || '', b[columnKey] || '') }
        })
      }

      // Add actions column for admin users
      if (this.isAdmin) {
        this.columns.push({
          key: 'vgpuActions',
          title: this.$t('label.actions'),
          width: 120,
          fixed: 'right'
        })
      }

      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    fetchVgpuProfiles () {
      getAPI('listVgpuProfiles', {
        gpucardid: this.resource.id
      }).then(res => {
        this.items = res?.listvgpuprofilesresponse?.vgpuprofile || []
      })
    }
  }
}
</script>
