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
  <loading-outlined v-if="loadingTable" class="main-loading-spinner" />
  <div v-else>
    <div v-if="!disabled" class="rules-list ant-list ant-list-bordered">
      <div class="rules-table-item ant-list-item">
        <div class="rules-table__col rules-table__col--grab" />
        <div class="rules-table__col rules-table__col--rule rules-table__col--new">
          <a-auto-complete
            :key="autocompleteKey"
            v-focus="true"
            :filterOption="filterOption"
            :options="apis"
            v-model:value="newRule"
            :placeholder="$t('label.rule')"
            :class="{'rule-dropdown-error' : newRuleSelectError}" />
        </div>
        <div class="rules-table__col rules-table__col--permission">
          <permission-editable
            :default-value="newRulePermission"
            :value="newRulePermission"
            @onChange="updateNewPermission()" />
        </div>
        <div class="rules-table__col rules-table__col--description">
          <a-input v-model:value="newRuleDescription" :placeholder="$t('label.description')" />
        </div>
        <div class="rules-table__col rules-table__col--actions">
          <tooltip-button
            tooltipPlacement="bottom"
            :tooltip="$t('label.save.new.rule')"
            icon="plus-outlined"
            type="primary"
            @onClick="onRuleSave" />
        </div>
      </div>

      <draggable
        v-model="rules"
        @change="updateRules"
        handle=".drag-handle"
        ghostClass="drag-ghost"
        :component-data="{type: 'transition'}"
        item-key="rule">
        <template #item="{element, index}">
          <div class="rules-table-item ant-list-item">
            <div class="rules-table__col rules-table__col--grab drag-handle">
              <drag-outlined />
            </div>
            <div class="rules-table__col rules-table__col--rule">
              {{ element.rule }}
            </div>
            <div class="rules-table__col rules-table__col--permission">
              <permission-editable
                :default-value="element.permission"
                @onChange="onPermissionChange(element, $event, index)" />
            </div>
            <div class="rules-table__col rules-table__col--description">
              <div v-if="element.description">
                {{ element.description }}
              </div>
              <div v-else class="no-description">
                {{ $t('message.no.description') }}
              </div>
            </div>
            <div class="rules-table__col rules-table__col--actions">
              <tooltip-button
                :tooltip="$t('label.delete.rule')"
                tooltipPlacement="bottom"
                type="primary"
                :danger="true"
                icon="delete-outlined"
                :disabled="false"
                @onClick="onRuleDelete(element.rule, index)" />
            </div>
          </div>
        </template>
      </draggable>
    </div>

    <div :style="{width: '100%', display: 'flex', marginTop: this.rules.length > 0 ? '12px' : '0'}" v-if="this.rules.length > 0 && !disabled">
      <a-button
        style="width: 100%;"
        danger
        @click="deleteAllRules()">
        <template #icon><delete-outlined /></template>
        {{ $t('label.delete.all.rules') }}
      </a-button>
    </div>

    <a-table
      v-else-if="disabled"
      :columns="columns"
      :dataSource="rules"
      rowKey="rule"
      size="large"
      :pagination="pagination"
      @change="handlePaginationChange">
      <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
        <div style="padding: 8px">
          <a-input
            ref="searchInput"
            :placeholder="$t('label.search')"
            :value="selectedKeys[0]"
            style="width: 100%; margin-bottom: 8px; display: block"
            @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
            @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)"
          />
          <div style="display: flex; gap: 8px">
            <a-button
              type="primary"
              size="small"
              style="width: 112px;"
              @click="handleSearch(selectedKeys, confirm, column.dataIndex)">
              <template #icon>
                <search-outlined />
              </template>
              {{ $t('label.search') }}
            </a-button>

            <a-button
              size="small"
              style="width: 112px;"
              @click="handleReset(clearFilters)">
              {{ $t('label.reset') }}
            </a-button>
          </div>
        </div>
      </template>

      <template #customFilterIcon="{ filtered }">
        <search-outlined :style="{ color: filtered ? '#1890ff' : '', fontSize: '14px' }" />
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'permission'">
          <a-tag
            class="permission-tag"
            :style="{
              backgroundColor: record.permission === 'allow' ? '#d9f7be' : '#fff2f0',
              color: record.permission === 'allow' ? '#135200' : '#cf1322'
            }">
            <check-outlined v-if="record.permission === 'allow'" />
            <close-outlined v-else />
            {{ record.permission === 'allow' ? $t('label.allow') : $t('label.deny') }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'description' && record.description">
          {{ record.description }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import draggable from 'vuedraggable'
import PermissionEditable from './PermissionEditable'
import TooltipButton from '@/components/widgets/TooltipButton'
import { genericCompare } from '@/utils/sort'

export default {
  name: 'ApiKeyPairPermissionTable',
  components: {
    PermissionEditable,
    draggable,
    TooltipButton
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loadingTable: true,
      disabled: false,
      rules: [],
      newRule: '',
      newRulePermission: 'allow',
      newRuleDescription: '',
      newRuleSelectError: false,
      drag: false,
      apis: [],
      currRules: new Set(),
      searchText: '',
      searchedColumn: '',
      columns: [
        {
          title: this.$t('label.rule'),
          dataIndex: 'rule',
          key: 'rule',
          ellipsis: true,
          customFilterDropdown: true,
          onFilter: (value, record) => {
            return record.rule.toString().toLowerCase().includes(value.toLowerCase())
          },
          sorter: (a, b) => { return genericCompare(a.rule || '', b.rule || '') },
          width: 480
        },
        {
          title: this.$t('label.permission'),
          dataIndex: 'permission',
          key: 'permission',
          width: 160,
          align: 'center',
          sorter: (a, b) => { return genericCompare(a.permission || '', b.permission || '') }
        },
        {
          title: this.$t('label.description'),
          dataIndex: 'description',
          key: 'description'
        }
      ],
      pagination: {
        pageSize: 20,
        pageSizeOptions: ['10', '20', '40', '80', '100', '200'],
        showSizeChanger: true
      },
      autocompleteKey: 0
    }
  },
  async created () {
    if (this.$route.path.startsWith('/keypair')) {
      await this.fetchKeyData()
      this.disabled = true
    } else {
      this.getApis()
    }
    this.loadingTable = false
  },
  methods: {
    handleSearch (selectedKeys, confirm, dataIndex) {
      confirm()
      this.searchText = selectedKeys[0]
      this.searchedColumn = dataIndex
    },
    handleReset (clearFilters) {
      clearFilters()
      this.searchText = ''
    },
    handlePaginationChange (pagination) {
      this.pagination.pageSize = pagination.pageSize
      this.pagination.current = pagination.current
    },
    filterOption (input, option) {
      return option.value.toUpperCase().indexOf(input.toUpperCase()) >= 0
    },
    async fetchKeyData () {
      try {
        const response = await getAPI('listUserKeyRules', { keypairid: this.resource.id })
        this.rules = response?.listuserkeyrulesresponse?.keypermission ?? []
      } catch (e) {
        this.$notifyError(e)
      }
    },
    getApis () {
      this.apis = Object.keys(this.$store.getters.apis).sort((a, b) => a.localeCompare(b)).map(value => { return { value: value } })
    },
    onRuleDelete (rule, idx) {
      this.rules.splice(idx, 1)
      this.currRules.delete(rule)
      this.updateRules()
    },
    onRuleSave () {
      if (!this.newRule || this.currRules.has(this.newRule)) {
        return
      }
      this.rules.push({
        rule: this.newRule,
        permission: this.newRulePermission,
        description: this.newRuleDescription,
        roleid: this.resource.id
      })
      this.currRules.add(this.newRule)
      this.newRule = ''
      this.newRuleDescription = ''
      this.autocompleteKey += 1
      this.updateRules()
    },
    async deleteAllRules () {
      this.loadingTable = true
      return new Promise((resolve) => {
        setTimeout(() => {
          this.rules = []
          this.currRules = new Set()
          this.updateRules()
          resolve()
          this.loadingTable = false
        })
      })
    },
    updateRules () {
      this.$emit('update-rules', this.rules)
    },
    updateNewPermission () {
      this.newRulePermission = (this.newRulePermission === 'allow') ? 'deny' : 'allow'
    },
    onPermissionChange (record, value, idx) {
      if (!record) return
      this.rules[idx].permission = value
      record.permission = value
      this.updateRules()
    }
  }
}
</script>

<style lang="scss" scoped>
.main-loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}
.role-add-btn {
  margin-bottom: 15px;
}
.new-role-controls {
  display: flex;

  button {
    &:not(:last-child) {
      margin-right: 5px;
    }
  }
}

.rules-list {
  max-height: 600px;
  overflow: auto;

  &--overflow-hidden {
    overflow: hidden;
  }
}

.rules-table {
  &-item {
    position: relative;
    display: flex;
    align-items: stretch;
    padding: 0;
    flex-wrap: wrap;

    @media (min-width: 760px) {
      flex-wrap: nowrap;
      padding-right: 25px;
    }
  }

  &__col {
    display: flex;
    align-items: center;
    padding: 15px;

    @media (min-width: 760px) {
      padding: 15px 0;

      &:not(:first-child) {
        padding-left: 20px;
      }

      &:not(:last-child) {
        border-right: 1px solid #e8e8e8;
        padding-right: 20px;
      }
    }

    &--grab {
      position: absolute;
      top: 4px;
      left: 0;
      width: 100%;

      @media (min-width: 760px) {
        position: relative;
        top: auto;
        width: 35px;
        padding-left: 25px;
        justify-content: center;
      }
    }

    &--rule,
    &--description {
      word-break: break-all;
      flex: 1;
      width: 100%;

      @media (min-width: 760px) {
        width: auto;
      }
    }

    &--rule {
      padding-left: 60px;
      background-color: rgba(#e6f7ff, 0.7);

      @media (min-width: 760px) {
        padding-left: 20px;
        background: none;
      }
    }

    &--permission {
      justify-content: center;
      width: 100%;

      .ant-select {
        width: 100%;
      }

      @media (min-width: 760px) {
        width: auto;

        .ant-select {
          width: auto;
        }
      }
    }

    &--actions {
      max-width: 60px;
      width: 100%;
      padding-right: 0;

      @media (min-width: 760px) {
        width: auto;
        max-width: 70px;
        padding-right: 15px;
      }
    }

    &--new {
      padding-left: 15px;
      background-color: transparent;

      div {
        width: 100%;
      }
    }
  }
}

.no-description {
  opacity: 0.4;
  font-size: 0.7rem;

  @media (min-width: 760px) {
    display: none;
  }

}

.drag-handle {
  cursor: pointer;
}

.drag-ghost {
  opacity: 0.5;
  background: #f0f2f5;
}

.loading-overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: #39A7DE;
  background-color: rgba(#fff, 0.8);
}

.permission-tag {
  border: none;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.rules-table__col--new {
  .ant-select {
    width: 100%;
  }
}

.rule-dropdown-error {
  .ant-input {
    border-color: #ff0000
  }
}
</style>
