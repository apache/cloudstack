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
  <a-card>
    <a-row :gutter="[16, 8]">
      <a-col :span="18">
        <a-row :gutter="[16, 8]">
          <a-col :span="18">
            <a-form-item no-style>
              <tooltip-label :title="$t('label.name')" />
              <a-input
                v-model:value="newParam.name"
                :placeholder="$t('label.name')"
                class="input-field"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6" style="display: flex; align-items: flex-end;">
            <a-form-item no-style>
              <a-checkbox
                v-model:checked="newParam.required"
                class="required-checkbox"
              >
                Required
              </a-checkbox>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item no-style>
              <tooltip-label :title="$t('label.type')" />
              <a-select
                v-model:value="newParam.type"
                :placeholder="$t('label.type')"
                class="input-field"
                @change="onNewParamTypeChange"
              >
                <a-select-option
                  v-for="t in types"
                  :key="t"
                  :value="t"
                >
                  {{ t }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item no-style>
              <tooltip-label :title="$t('label.validationformat')" :tooltip="$t('message.desc.validationformat')" />
              <a-select
                :disabled="!['NUMBER', 'STRING'].includes(newParam.type)"
                v-model:value="newParam.validationformat"
                :placeholder="$t('message.desc.validationformat')"
                class="input-field"
              >
                <a-select-option
                  v-for="t in newParamFormats"
                  :key="t"
                  :value="t"
                >
                  {{ t }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item no-style>
              <tooltip-label :title="$t('label.valueoptions')" :tooltip="$t('message.desc.valueoptions')" />
              <a-input
                :disabled="!['NUMBER', 'STRING'].includes(newParam.type)"
                v-model:value="newParam.valueoptions"
                :placeholder="$t('message.desc.valueoptions')"
                class="input-field"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-col>
      <a-col :span="6" style="display: flex; align-items: flex-end; justify-content: center;">
        <a-button
          type="primary"
          class="add-button"
          @click="addEntry"
          :disabled="!newParam.name || !newParam.type"
        >
          Add
        </a-button>
      </a-col>
    </a-row>
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
              <tooltip-button type="danger" :tooltip="$t('label.remove')" icon="delete-outlined" @onClick="removeEntry(record.name)" />
            </div>
          </template>
        </template>

        <template v-else-if="record.editing">
          <a-form-item no-style>
            <a-input v-if="column.key === 'name'" v-model:value="editBuffer.name" />
          </a-form-item>
          <a-form-item no-style>
            <a-select v-if="column.key === 'type'" v-model:value="editBuffer.type" @change="onEditBufferTypeChange">
              <a-select-option v-for="t in types" :key="t" :value="t">{{ t }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item no-style>
            <a-select v-if="column.key === 'validationformat'" :disabled="!['NUMBER', 'STRING'].includes(editBuffer.type)" v-model:value="editBuffer.validationformat">
              <a-select-option v-for="t in editBufferFormats" :key="t" :value="t">{{ t }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item no-style>
            <a-input v-if="column.key === 'valueoptions'" :disabled="!['NUMBER', 'STRING'].includes(editBuffer.type)" v-model:value="editBuffer.valueoptions" />
          </a-form-item>
          <a-form-item no-style>
            <a-checkbox v-if="column.key === 'required'" v-model:checked="editBuffer.required" />
          </a-form-item>
        </template>

        <template v-else>
          <template v-if="column.key === 'required'">{{ record.required ? 'Yes' : 'No' }}</template>
          <template v-else-if="column.key === 'valueoptions'">
            {{ Array.isArray(record.valueoptions) ? record.valueoptions.join(',') : record.valueoptions }}
          </template>
          <template v-else>{{ record[column.key] }}</template>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script>
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ParametersInput',
  components: {
    TooltipButton,
    TooltipLabel
  },
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
      newParam: {},
      columns: [
        { title: this.$t('label.name'), dataIndex: 'name', key: 'name', width: '20%' },
        { title: this.$t('label.type'), dataIndex: 'type', key: 'type', width: '15%' },
        { title: this.$t('label.validationformat'), dataIndex: 'validationformat', key: 'validationformat', width: '15%' },
        { title: this.$t('label.valueoptions'), dataIndex: 'valueoptions', key: 'valueoptions', width: '25%' },
        { title: this.$t('label.required'), dataIndex: 'required', key: 'required', width: '10%' },
        { title: this.$t('label.actions'), key: 'actions', width: '15%' }
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
  computed: {
    types () {
      return [
        'BOOLEAN',
        'DATE',
        'NUMBER',
        'STRING'
      ]
    },
    newParamFormats () {
      return this.getFormatOptions(this.newParam?.type)
    },
    editBufferFormats () {
      return this.getFormatOptions(this.editBuffer?.type)
    }
  },
  methods: {
    getFormatOptions (type) {
      const formats = ['NONE']
      if (type === 'STRING') {
        formats.push('EMAIL', 'PASSWORD', 'URL', 'UUID')
      } else if (type === 'NUMBER') {
        formats.push('DECIMAL')
      }
      return formats
    },
    resetFormatAndOptions (obj) {
      delete obj.validationformat
      delete obj.valueoptions
    },
    onNewParamTypeChange () {
      this.resetFormatAndOptions(this.newParam)
    },
    onEditBufferTypeChange () {
      this.resetFormatAndOptions(this.editBuffer)
    },
    addEntry () {
      const existingIndex = this.tableData.findIndex(row => row.name === this.newParam.name)
      if (existingIndex !== -1) {
        this.tableData[existingIndex] = { ...this.newParam }
      } else {
        this.tableData.push({ ...this.newParam, editing: false })
      }
      this.updateData()
      this.newParam = {}
    },
    removeEntry (name) {
      this.tableData = this.tableData.filter(item => item.name !== name)
      this.updateData()
    },
    editRow (record) {
      this.editBuffer = { ...record }
      if (Array.isArray(this.editBuffer.valueoptions)) {
        this.editBuffer.valueoptions = this.editBuffer.valueoptions.join(',')
      }
      record.editing = true
    },
    cancelEdit (record) {
      record.editing = false
      this.editBuffer = {}
    },
    saveEdit (record) {
      if (!this.editBuffer) return
      Object.assign(record, this.editBuffer)
      if (!this.editBuffer.validationformat) {
        delete record.validationformat
      }
      if (!this.editBuffer.valueoptions) {
        delete record.valueoptions
      }
      record.editing = false
      this.updateData()
      this.editBuffer = {}
    },
    updateData () {
      const data = this.tableData.map(({ name, type, validationformat, valueoptions, required }) => ({
        name,
        type,
        validationformat,
        valueoptions: Array.isArray(valueoptions) ? valueoptions.join(',') : valueoptions,
        required
      }))
      this.$emit('update:value', data)
    }
  }
}
</script>

<style scoped>
.input-row {
  display: grid;
  grid-template-columns: 1fr auto; /* First column flexible, second column auto-sized */
  grid-template-rows: auto auto auto;
  gap: 8px 16px; /* row-gap and column-gap */
  align-items: center;
}
.grid-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.input-field {
  width: 100%;
}
.add-button {
  white-space: nowrap;
}

.table {
  margin-top: 10px;
  width: 100%;
}

.flex-gap {
  display: flex;
  gap: 8px;
}
</style>
