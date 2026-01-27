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
        <a-input v-model:value="newKey" :placeholder="$t('label.key')" class="input-field" />
      </a-form-item>
      <a-form-item no-style>
        <a-input v-model:value="newValue" :placeholder="$t('label.value')" class="input-field" />
      </a-form-item>
      <a-button type="primary" class="add-button" @click="addEntry" :disabled="!newKey || !newValue">
        Add
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :dataSource="tableData"
      rowKey="key"
      size="small"
      :pagination="false"
      :showHeader="showTableHeaders"
      class="table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <template v-if="record.editing">
            <div class="flex-gap">
              <tooltip-button :tooltip="$t('label.ok')" icon="check-outlined" @onClick="saveEdit(record)" />
              <tooltip-button :tooltip="$t('label.cancel')" icon="close-outlined" @onClick="cancelEdit(record)" />
            </div>
          </template>
          <template v-else>
            <div class="flex-gap">
              <tooltip-button :tooltip="$t('label.edit')" icon="edit-outlined" @onClick="editRow(record)" />
              <tooltip-button type="danger" :tooltip="$t('label.remove')" icon="delete-outlined" @onClick="removeEntry(record.key)" />
            </div>
          </template>
        </template>

        <template v-else-if="record.editing">
          <a-form-item no-style>
            <a-input v-model:value="editBuffer[column.key]" />
          </a-form-item>
        </template>

        <template v-else>
          {{ record[column.key] }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'DetailsInput',
  components: { TooltipButton },
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    showTableHeaders: {
      type: Boolean,
      default: true
    }
  },
  data () {
    return {
      columns: [
        { title: this.$t('label.key'), dataIndex: 'key', key: 'key', width: '40%', ellipsis: true },
        { title: this.$t('label.value'), dataIndex: 'value', key: 'value', width: '40%', ellipsis: true },
        { title: this.$t('label.actions'), key: 'actions', width: '20%' }
      ],
      newKey: '',
      newValue: '',
      tableData: [],
      editBuffer: {}
    }
  },
  created () {
    if (this.value && typeof this.value === 'object') {
      this.tableData = this.mapToTableData(this.value)
    }
  },
  watch: {
    value (newVal) {
      this.tableData = this.mapToTableData(newVal)
    }
  },
  emits: ['update:value'],
  methods: {
    mapToTableData (obj) {
      return Object.entries(obj || {}).map(([key, value]) => ({
        key,
        value,
        editing: false
      }))
    },
    addEntry () {
      if (!this.newKey || !this.newValue) return
      const existingIndex = this.tableData.findIndex(row => row.key === this.newKey)
      if (existingIndex !== -1) {
        this.tableData[existingIndex].value = this.newValue
      } else {
        this.tableData.push({ key: this.newKey, value: this.newValue, editing: false })
      }
      this.updateData()
      this.newKey = ''
      this.newValue = ''
    },
    removeEntry (key) {
      this.tableData = this.tableData.filter(item => item.key !== key)
      this.updateData()
    },
    editRow (record) {
      this.editBuffer = {
        key: record.key,
        value: record.value
      }
      record.editing = true
    },
    cancelEdit (record) {
      record.editing = false
      this.editBuffer = {}
    },
    saveEdit (record) {
      record.key = this.editBuffer.key
      record.value = this.editBuffer.value
      record.editing = false
      this.updateData()
    },
    updateData () {
      const obj = {}
      this.tableData.forEach(({ key, value }) => {
        obj[key] = value
      })
      this.$emit('update:value', obj)
    }
  }
}
</script>
<style lang="css" scoped>
.input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.input-field {
  flex: 1; /* each takes half of the remaining 80% */
}

.add-button {
  width: 20%;
  min-width: 100px; /* optional: prevents it from getting too narrow */
}

.table {
  width: 100%;
}

.flex-gap {
  display: flex;
  gap: 8px;
}
</style>
