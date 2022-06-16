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
    <div>
      <a-alert type="info" v-if="resource.state !== 'Disabled'">
        <template #message>
        <div
          v-html="$t('message.autoscale.policies.update')" />
        </template>
      </a-alert>
      <div class="form" v-ctrl-enter="addCondition">
        <div class="form__item">
          <div class="form__label">{{ $t('label.counterid') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="newCondition.counterId">
            <a-select-option v-for="(counter, index) in countersList" :value="counter.id" :key="index">
              {{ counter.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.relationaloperator') }}</div>
          <a-select
            v-model:value="newCondition.relationaloperator"
            style="width: 100%;"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="gt">{{ getOperator('GT') }}</a-select-option>
            <a-select-option value="ge">{{ getOperator('GE') }}</a-select-option>
            <a-select-option value="lt">{{ getOperator('LT') }}</a-select-option>
            <a-select-option value="le">{{ getOperator('LE') }}</a-select-option>
            <a-select-option value="eq">{{ getOperator('EQ') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.threshold') }}</div>
          <a-input v-model:value="newCondition.threshold"></a-input>
        </div>
        <div class="form__item">
          <a-button ref="submit" :disabled="!('createCondition' in $store.getters.apis)" type="primary" @click="addCondition">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add') }}
          </a-button>
        </div>
      </div>
    </div>

    <a-divider/>
    <a-button
      v-if="(('deleteCondition' in $store.getters.apis) && this.selectedRowKeys.length > 0)"
      type="primary"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="bulkActionConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t('label.action.bulk.delete.conditions') }}
    </a-button>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="conditions"
      :pagination="false"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :rowKey="record => record.id">
      <template #name="{ record }">
        {{ record.name }}
      </template>
      <template #relationaloperator="{ record }">
        {{ getOperator(record.relationaloperator) }}
      </template>
      <template #threshold="{ record }">
        {{ record.threshold }}
      </template>
      <template #actions="{ record }">
        <tooltip-button
          :tooltip="$t('label.delete')"
          :disabled="!('deleteCondition' in $store.getters.apis)"
          type="primary"
          :danger="true"
          icon="delete-outlined"
          @onClick="deleteConditionFromScalePolicy(record.id)" />
      </template>
    </a-table>

    <bulk-action-view
      v-if="showConfirmationAction || showGroupActionModal"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="conditions"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      action="deleteCondition"
      :loading="loading"
      :message="message"
      @group-action="deleteConditions"
      @handle-cancel="handleCancel"
      @close-modal="closeModal" />
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import BulkActionView from '@/components/view/BulkActionView'
import eventBus from '@/config/eventBus'

export default {
  name: 'conditionsTab',
  components: {
    Status,
    TooltipButton,
    BulkActionView
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      selectedRowKeys: [],
      showGroupActionModal: false,
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['Action'],
      showConfirmationAction: false,
      message: {
        title: this.$t('label.action.bulk.delete.conditions'),
        confirmMessage: this.$t('label.confirm.delete.conditions')
      },
      loading: true,
      policyid: null,
      conditions: [],
      newCondition: {
        counterid: null,
        relationaloperator: null,
        threshold: null
      },
      countersList: [],
      columns: [
        {
          title: this.$t('label.counter'),
          dataIndex: 'countername'
        },
        {
          title: this.$t('label.relationaloperator'),
          dataIndex: 'relationaloperator'
        },
        {
          title: this.$t('label.threshold'),
          slots: { customRender: 'threshold' }
        },
        {
          title: this.$t('label.action'),
          slots: { customRender: 'actions' }
        }
      ]
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  created () {
    this.fetchInitData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  inject: ['parentFetchData'],
  methods: {
    fetchInitData () {
      this.loading = true
      api('listAutoScaleVmGroups', {
        listAll: true,
        id: this.resource.id
      }).then(response => {
        this.policyid = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.scaleuppolicies?.[0]?.id
        this.conditions = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.scaleuppolicies?.[0]?.conditions || []
        const lbruleid = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.lbruleid
        api('listLoadBalancerRules', {
          listAll: true,
          id: lbruleid
        }).then(response => {
          const networkid = response.listloadbalancerrulesresponse?.loadbalancerrule?.[0]?.networkid
          api('listNetworks', {
            listAll: true,
            id: networkid
          }).then(response => {
            const services = response.listnetworksresponse?.network?.[0]?.service
            const index = services.map(svc => { return svc.name }).indexOf('Lb')
            const provider = services[index].provider[0].name
            api('listCounters', {
              listAll: true,
              provider: provider
            }).then(response => {
              this.countersList = response.counterresponse?.counter || []
            })
          })
        })
      }).finally(() => {
        this.loading = false
      })
    },
    fetchData () {
      this.loading = true
      api('listAutoScalePolicies', {
        listAll: true,
        id: this.policyid
      }).then(response => {
        this.conditions = response.listautoscalepoliciesresponse?.autoscalepolicy[0]?.conditions || []
      }).finally(() => {
        this.loading = false
      })
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.conditions.filter(function (item) {
        return selection.indexOf(item.id) !== -1
      }))
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.parentFetchData()
    },
    deleteConditions (e) {
      this.showConfirmationAction = false
      this.selectedColumns.splice(0, 0, {
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        slots: { customRender: 'status' },
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      for (const condition of this.selectedItems) {
        this.deleteConditionFromScalePolicy(condition.id)
      }
    },
    getOperator (val) {
      if (val === 'GT') return this.$t('label.operator.greater')
      if (val === 'GE') return this.$t('label.operator.greater.or.equal')
      if (val === 'LT') return this.$t('label.operator.less')
      if (val === 'LE') return this.$t('label.operator.less.or.equal')
      if (val === 'EQ') return this.$t('label.operator.equal')
      return val
    },
    deleteCondition (conditionId) {
      this.loading = true
      api('deleteCondition', { id: conditionId }).then(response => {
        const jobId = response.deleteconditionresponse.jobid
        eventBus.emit('update-job-details', { jobId, resourceId: null })
        this.$pollJob({
          title: this.$t('label.action.delete.condition'),
          description: conditionId,
          jobId: jobId,
          successMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: conditionId, state: 'success' })
            }
            this.fetchData()
          },
          errorMessage: this.$t('message.delete.condition.failed'),
          errorMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: conditionId, state: 'failed' })
            }
            this.fetchData()
          },
          loadingMessage: this.$t('message.delete.condition.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => this.fetchData(),
          bulkAction: `${this.selectedItems.length > 0}` && this.showGroupActionModal
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    deleteConditionFromScalePolicy (conditionId) {
      this.updateScalePolicy(null, conditionId)
    },
    addCondition () {
      if (this.loading) return
      this.loading = true

      api('createCondition', { ...this.newCondition }).then(response => {
        this.$pollJob({
          jobId: response.conditionresponse.jobid,
          successMethod: (result) => {
            const newConditionId = result.jobresult.condition?.id
            this.updateScalePolicy(newConditionId, null)
          },
          errorMessage: this.$t('message.create.condition.failed'),
          errorMethod: () => {
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    updateScalePolicy (conditionIdToAdd, conditionIdToRemove) {
      if (this.loading) return
      this.loading = true

      let newConditionIds
      if (conditionIdToAdd) {
        newConditionIds = this.conditions.map(condition => { return condition.id }).join(',') + ',' + conditionIdToAdd
      } else if (conditionIdToRemove) {
        newConditionIds = this.conditions.filter(condition => condition.id !== conditionIdToRemove).map(condition => { return condition.id }).join(',')
      }

      api('updateAutoScalePolicy', {
        id: this.policyid,
        conditionids: newConditionIds
      }).then(response => {
        this.$pollJob({
          jobId: response.updateautoscalepolicyresponse.jobid,
          successMethod: (result) => {
            if (conditionIdToRemove) {
              this.deleteCondition(conditionIdToRemove)
            }
          },
          errorMessage: this.$t('message.update.autoscale.policy.failed'),
          errorMethod: () => {
            if (conditionIdToAdd) {
              this.deleteCondition(conditionIdToAdd)
            }
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    closeModal () {
      this.showConfirmationAction = false
    },
    capitalise (val) {
      return val.toUpperCase()
    }
  }
}
</script>

<style scoped lang="scss">
  .condition {

    &-container {
      display: flex;
      width: 100%;
      flex-wrap: wrap;
      margin-right: -20px;
      margin-bottom: -10px;
    }

    &__item {
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        flex: 1;
      }

    }

    &__title {
      font-weight: bold;
    }

  }

  .add-btn {
    width: 100%;
    padding-top: 15px;
    padding-bottom: 15px;
    height: auto;
  }

  .add-actions {
    display: flex;
    justify-content: flex-end;
    margin-right: -20px;
    margin-bottom: 20px;

    @media (min-width: 760px) {
      margin-top: 20px;
    }

    button {
      margin-right: 20px;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;
    align-items: flex-end;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      flex: 1;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

    }

    &__label {
      font-weight: bold;
    }

  }
  .pagination {
    margin-top: 20px;
  }
</style>
