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
    />

    <a-modal
      :visible="showCreateModal"
      :title="$t('label.add.vgpu.profile')"
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
            name="name"
            ref="name"
            :label="$t('label.name')"
          >
            <a-input
              v-model:value="form.name"
              :placeholder="$t('label.name')"
              v-focus="true"
            />
          </a-form-item>
          <a-form-item
            name="description"
            ref="description"
            :label="$t('label.description')"
          >
            <a-input
              v-model:value="form.description"
              :placeholder="$t('label.description')"
            />
          </a-form-item>
          <a-form-item
            name="maxvgpuperphysicalgpu"
            ref="maxvgpuperphysicalgpu"
            :label="$t('label.maxvgpuperphysicalgpu')"
          >
            <a-input-number
              style="width: 100%"
              v-model:value="form.maxvgpuperphysicalgpu"
              :min="1"
              :placeholder="$t('label.maxvgpuperphysicalgpu')"
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
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import ListView from '@/components/view/ListView'

export default {
  name: 'VgpuProfilesTab',
  components: {
    ListView
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
      showCreateModal: false,
      actionLoading: false,
      form: {},
      rules: {}
    }
  },
  created () {
    this.selectedColumnKeys = this.columnKeys
    this.updateColumns()
    this.fetchData()
    this.initForm()
  },
  methods: {
    initForm () {
      this.form = reactive({
        name: '',
        description: '',
        maxvgpuperphysicalgpu: 1
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        maxvgpuperphysicalgpu: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    createVgpuProfile () {
      this.showCreateModal = true
      this.initForm()
    },
    closeModal () {
      this.showCreateModal = false
      this.actionLoading = false
    },
    handleSubmit (e) {
      if (e) {
        e.preventDefault()
      }
      if (this.actionLoading) return

      this.$refs.vgpuForm.validate().then(() => {
        this.actionLoading = true
        const formRaw = toRaw(this.form)
        const params = {
          name: formRaw.name,
          gpucardid: this.resource.id,
          maxvgpuperphysicalgpu: formRaw.maxvgpuperphysicalgpu
        }

        if (formRaw.description) {
          params.description = formRaw.description
        }

        api('createVgpuProfile', params).then(response => {
          this.$message.success(this.$t('message.success.create.vgpu.profile'))
          this.closeModal()
          this.fetchData()
        }).catch(error => {
          console.error('Error creating vGPU profile:', error)
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      }).catch((error) => {
        this.$message.error(error)
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
      if (this.columns.length > 0) {
        this.columns[this.columns.length - 1].customFilterDropdown = true
      }
    },
    fetchVgpuProfiles () {
      api('listVgpuProfiles', {
        gpucardid: this.resource.id
      }).then(res => {
        this.items = res?.listvgpuprofilesresponse?.vgpuprofile || []
      })
    }
  }
}
</script>
