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
  <loading-outlined v-if="loadingTable" class="main-loading-spinner"></loading-outlined>
  <div v-else>
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
            placeholder="Rule"
            :class="{'rule-dropdown-error' : newRuleSelectError}" />
        </div>
        <div class="rules-table__col rules-table__col--permission">
          <permission-editable
            :defaultValue="newRulePermission"
            @onChange="onPermissionChange(null, $event)" />
        </div>
        <div class="rules-table__col rules-table__col--description">
          <a-input v-model:value="newRuleDescription" placeholder="Description"></a-input>
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
        @change="changeOrder"
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
                :defaultValue="element.permission"
                @onChange="onPermissionChange(element, $event)" />
            </div>
            <div class="rules-table__col rules-table__col--description">
              <template v-if="element.description">
                {{ record.description }}
              </template>
              <div v-else class="no-description">
                {{ $t('message.no.description') }}
              </div>
            </div>
            <div class="rules-table__col rules-table__col--actions">
              <rule-delete
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
import PermissionEditable from '@/views/iam/PermissionEditable'
import RuleDelete from '@/views/iam/RuleDelete'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'ProjectRolePermissionTab',
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
    },
    role: {
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
      newRulePermission: 'deny',
      newRuleDescription: '',
      newRuleSelectError: false,
      drag: false,
      apis: []
    }
  },
  mounted () {
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
      this.newRulePermission = 'deny'
      this.newRuleDescription = ''
      this.newRuleSelectError = false
    },
    fetchData (callback = null) {
      if (!this.resource.id) return
      api('listProjectRolePermissions', {
        projectid: this.resource.id,
        projectroleid: this.role.id
      }).then(response => {
        this.rules = response.listprojectrolepermissionsresponse.projectrolepermission
      }).catch(error => {
        console.error(error)
      }).finally(() => {
        this.loadingTable = false
        this.updateTable = false
        if (callback) callback()
      })
    },
    changeOrder () {
      api('updateProjectRolePermission', {}, 'POST', {
        projectid: this.resource.id,
        projectroleid: this.role.id,
        ruleorder: this.rules.map(rule => rule.id)
      }).catch(error => {
        console.error(error)
      }).finally(() => {
        this.fetchData()
      })
    },
    onRuleDelete (key) {
      this.updateTable = true
      api('deleteProjectRolePermission', {
        id: key,
        projectid: this.resource.id
      }).catch(error => {
        console.error(error)
      }).finally(() => {
        this.fetchData()
      })
    },
    onPermissionChange (record, value) {
      this.newRulePermission = value
      if (!record) return
      this.updateTable = true
      api('updateProjectRolePermission', {
        projectid: this.resource.id,
        projectroleid: this.role.id,
        projectrolepermissionid: record.id,
        permission: value
      }).then(() => {
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
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
      api('createProjectRolePermission', {
        rule: this.newRule,
        permission: this.newRulePermission,
        description: this.newRuleDescription,
        projectroleid: this.role.id,
        projectid: this.resource.id
      }).then(() => {
      }).catch(error => {
        console.error(error)
        this.$notifyError(error)
      }).finally(() => {
        this.resetNewFields()
        this.fetchData()
      })
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
