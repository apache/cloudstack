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
    <div style="width: 100%; display: flex; margin-bottom: 10px">
      <a-button type="dashed" @click="exportRolePermissions" style="width: 100%">
        <template #icon><download-outlined /></template>
        {{ $t('label.export.rules') }}
      </a-button>
    </div>
    <div v-if="updateTable" class="loading-overlay">
      <loading-outlined />
    </div>
    <div
      class="rules-list ant-list ant-list-bordered"
      :class="{'rules-list--overflow-hidden' : updateTable}" >

      <div class="rules-table-item ant-list-item">
        <div class="rules-table__col rules-table__col--grab"></div>
        <div class="rules-table__col rules-table__col--rule rules-table__col--new">
          <a-auto-complete
            v-focus="true"
            :filterOption="filterOption"
            :options="apis"
            v-model:value="newRule"
            @change="val => newRule = val"
            :placeholder="$t('label.rule')"
            :class="{'rule-dropdown-error' : newRuleSelectError}" />
        </div>
        <div class="rules-table__col rules-table__col--permission">
          <permission-editable
            :defaultValue="newRulePermission"
            @onChange="onPermissionChange(null, $event)" />
        </div>
        <div class="rules-table__col rules-table__col--description">
          <a-input v-model:value="newRuleDescription" :placeholder="$t('label.description')"></a-input>
        </div>
        <div class="rules-table__col rules-table__col--actions">
          <tooltip-button
            tooltipPlacement="bottom"
            :tooltip="$t('label.save.new.rule')"
            :disabled="!('createRolePermission' in $store.getters.apis)"
            icon="plus-outlined"
            type="primary"
            @onClick="onRuleSave" />
        </div>
      </div>

      <draggable
        v-model="rules"
        @change="changeOrder"
        :disabled="!('updateRolePermission' in $store.getters.apis)"
        handle=".drag-handle"
        animation="200"
        ghostClass="drag-ghost"
        :component-data="{type: 'transition'}"
        item-key="id">
        <template #item="{element}">
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
                @onChange="onPermissionChange(element, $event)" />
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
              <rule-delete
                :disabled="!('deleteRolePermission' in $store.getters.apis)"
                :record="element"
                @delete="onRuleDelete(element.id)" />
            </div>
          </div>
        </template>
      </draggable>
    </div>
  </div>
</template>

<script>
import { api } from '@/api'
import draggable from 'vuedraggable'
import PermissionEditable from './PermissionEditable'
import RuleDelete from './RuleDelete'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'RolePermissionTab',
  components: {
    RuleDelete,
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
      updateTable: false,
      rules: null,
      newRule: '',
      newRulePermission: 'allow',
      newRuleDescription: '',
      newRuleSelectError: false,
      drag: false,
      apis: []
    }
  },
  created () {
    this.apis = Object.keys(this.$store.getters.apis)
      .sort((a, b) => a.localeCompare(b))
      .map(value => { return { value: value } })
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.fetchData(() => {
          this.resetNewFields()
        })
      }
    }
  },
  methods: {
    filterOption (input, option) {
      return (
        option.value.toUpperCase().indexOf(input.toUpperCase()) >= 0
      )
    },
    resetNewFields () {
      this.newRule = ''
      this.newRulePermission = 'allow'
      this.newRuleDescription = ''
      this.newRuleSelectError = false
    },
    fetchData (callback = null) {
      if (!this.resource.id) return
      api('listRolePermissions', { roleid: this.resource.id }).then(response => {
        this.rules = response.listrolepermissionsresponse.rolepermission
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingTable = false
        this.updateTable = false
        this.updateApis()
        if (callback) callback()
      })
    },
    updateApis () {
      this.apis = Object.keys(this.$store.getters.apis)
        .sort((a, b) => a.localeCompare(b))
      var apisSupported = this.rules?.map(rule => rule.rule) || []
      this.apis = this.apis.filter(api => !apisSupported.includes(api.value)).map(value => { return { value: value } })
    },
    changeOrder () {
      this.updateTable = true
      api('updateRolePermission', {}, 'POST', {
        roleid: this.resource.id,
        ruleorder: this.rules.map(rule => rule.id)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.updateTable = false
        this.fetchData()
      })
    },
    onRuleDelete (key) {
      this.updateTable = true
      api('deleteRolePermission', { id: key }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.updateTable = false
        this.fetchData()
      })
    },
    onPermissionChange (record, value) {
      this.newRulePermission = value

      if (!record) return

      this.updateTable = true
      api('updateRolePermission', {
        roleid: this.resource.id,
        ruleid: record.id,
        permission: value
      }).then(() => {
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.updateTable = false
      })
    },
    onRuleSelect (value) {
      this.newRule = value
    },
    onRuleSave () {
      if (!this.newRule) {
        this.newRuleSelectError = true
        return
      }

      this.updateTable = true
      api('createRolePermission', {
        rule: this.newRule,
        permission: this.newRulePermission,
        description: this.newRuleDescription,
        roleid: this.resource.id
      }).then(() => {
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.resetNewFields()
        this.fetchData()
        this.updateTable = false
      })
    },
    rulesDataToCsv ({ data = null, columnDelimiter = ',', lineDelimiter = '\n' }) {
      if (data === null || !data.length) {
        return null
      }

      const keys = ['rule', 'permission', 'description']
      let result = ''
      result += keys.join(columnDelimiter)
      result += lineDelimiter

      data.forEach(item => {
        keys.forEach(key => {
          if (item[key] === undefined) {
            item[key] = ''
          }
          result += typeof item[key] === 'string' && item[key].includes(columnDelimiter) ? `"${item[key]}"` : item[key]
          result += columnDelimiter
        })
        result = result.slice(0, -1)
        result += lineDelimiter
      })

      return result
    },
    exportRolePermissions () {
      const rulesCsvData = this.rulesDataToCsv({ data: this.rules })
      const hiddenElement = document.createElement('a')
      hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(rulesCsvData)
      hiddenElement.target = '_blank'
      hiddenElement.download = this.resource.name + '_' + this.resource.type + '.csv'
      hiddenElement.click()
      hiddenElement.remove()
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
          padding-left: 0;
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
</style>

<style lang="scss">
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
