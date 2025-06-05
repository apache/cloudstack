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
  <div>
    <div class="input-row">
      <a-form-item no-style>
        <a-input v-model:value="newParam.name" :placeholder="$t('label.name')" class="input-field" />
      </a-form-item>
      <a-form-item no-style>
        <a-select v-model:value="newParam.type" :placeholder="$t('label.type')" class="input-field">
          <a-select-option v-for="t in types" :key="t" :value="t">{{ t }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item no-style>
        <a-checkbox v-model:checked="newParam.required" class="required-checkbox">Required</a-checkbox>
      </a-form-item>
      <a-button type="primary" class="add-button" @click="addEntry" :disabled="!newParam.name || !newParam.type">
        Add
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :dataSource="tableData"
      rowKey="name"
      size="small"
      :pagination="false"
      :showHeader="showTableHeaders"
      class="table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <template v-if="record.editing">
            <div class="flex-gap">
              <tooltip-button :tooltip="$t('label.ok')" icon="check-outlined" @onClick="saveEdit(record)" />
              <tooltip-button :tooltip="$t('label.cancel')" icon="close-outlined" @onClick="cancelEdit(record)" />
            </div>
          </template>
          <template v-else>
            <div class="flex-gap">
              <tooltip-button :tooltip="$t('label.edit')" icon="edit-outlined" @onClick="editRow(record)" />
              <tooltip-button type="danger" :tooltip="$t('label.remove')" icon="delete-outlined" @onClick="removeEntry(record.name)" />
            </div>
          </template>
        </template>

        <template v-else-if="record.editing">
          <a-form-item no-style>
            <a-input v-if="column.key === 'name'" v-model:value="editBuffer.name" />
          </a-form-item>
          <a-form-item no-style>
            <a-select v-if="column.key === 'type'" v-model:value="editBuffer.type">
              <a-select-option v-for="t in types" :key="t" :value="t">{{ t }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item no-style>
            <a-checkbox v-if="column.key === 'required'" v-model:checked="editBuffer.required" />
          </a-form-item>
        </template>

        <template v-else>
          <template v-if="column.key === 'required'">{{ record.required ? 'Yes' : 'No' }}</template>
          <template v-else>{{ record[column.key] }}</template>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'ParametersInput',
  components: { TooltipButton },
  props: {
    value: {
      type: Array,
      default: () => []
    },
    showTableHeaders: {
      type: Boolean,
      default: true
    }
  },
  data () {
    return {
      types: [
        'BOOLEAN',
        'DATE',
        'FLOAT',
        'INTEGER',
        'SHORT',
        'LONG',
        'STRING',
        'UUID'
      ],
      newParam: { name: '', type: '', required: false },
      columns: [
        { title: 'Name', dataIndex: 'name', key: 'name', width: '30%' },
        { title: 'Type', dataIndex: 'type', key: 'type', width: '30%' },
        { title: 'Required', dataIndex: 'required', key: 'required', width: '20%' },
        { title: 'Action', key: 'action', width: '20%' }
      ],
      tableData: [],
      editBuffer: null
    }
  },
  watch: {
    value: {
      immediate: true,
      handler (newVal) {
        this.tableData = (newVal || []).map(p => ({
          ...p,
          editing: false
        }))
      }
    }
  },
  emits: ['update:value'],
  methods: {
    addEntry () {
      const existingIndex = this.tableData.findIndex(row => row.name === this.newParam.name)
      if (existingIndex !== -1) {
        this.tableData[existingIndex] = { ...this.newParam }
      } else {
        this.tableData.push({ ...this.newParam, editing: false })
      }
      this.updateData()
      this.newParam = { name: '', type: '', required: false }
    },
    removeEntry (name) {
      this.tableData = this.tableData.filter(item => item.name !== name)
      this.updateData()
    },
    editRow (record) {
      this.editBuffer = {
        name: record.name,
        type: record.type,
        required: record.required
      }
      record.editing = true
    },
    cancelEdit (record) {
      record.editing = false
      this.editBuffer = {}
    },
    saveEdit (record) {
      record.name = this.editBuffer.name
      record.type = this.editBuffer.type
      record.required = this.editBuffer.required
      record.editing = false
      this.updateData()
      this.editBuffer = {}
    },
    updateData () {
      const data = this.tableData.map(({ name, type, required }) => ({ name, type, required }))
      this.$emit('update:value', data)
    }
  }
}
</script>

<style scoped>
.input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.input-field {
  flex: 1;
}

.required-checkbox {
  white-space: nowrap;
}

.add-button {
  width: 20%;
  min-width: 100px;
}

.table {
  width: 100%;
}

.flex-gap {
  display: flex;
  gap: 8px;
}
</style>
