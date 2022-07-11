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

      <a-divider/>
      <div class="form" v-ctrl-enter="updateAutoScalePolicy">
        <div class="form__item">
          <div class="form__label">{{ $t('label.duration') }}</div>
          <a-input v-model:value="duration" type="number"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.quiettime') }}</div>
          <a-input v-model:value="quiettime" type="number"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('updateAutoScalePolicy' in $store.getters.apis) || resource.state !== 'Disabled'" type="primary" @click="updateAutoScalePolicy(null, null)">
            <template #icon><edit-outlined /></template>
            {{ $t('label.edit') }}
          </a-button>
        </div>
      </div>

      <a-divider/>
      <div class="form" v-ctrl-enter="addCondition">
        <div class="form__item" ref="newConditionCounterId">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.counter') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="newCondition.counterid">
            <a-select-option v-for="(counter, index) in countersList" :value="counter.id" :key="index">
              {{ counter.name }}
            </a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newConditionRelationalOperator">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.relationaloperator') }}</div>
          <a-select
            v-model:value="newCondition.relationaloperator"
            style="width: 100%;"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
            <a-select-option value="GE">{{ getOperator('GE') }}</a-select-option>
            <a-select-option value="LT">{{ getOperator('LT') }}</a-select-option>
            <a-select-option value="LE">{{ getOperator('LE') }}</a-select-option>
            <a-select-option value="EQ">{{ getOperator('EQ') }}</a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newConditionThreshold">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.threshold') }}</div>
          <a-input v-model:value="newCondition.threshold" type="number"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('createCondition' in $store.getters.apis) || resource.state !== 'Disabled'" type="primary" @click="addCondition">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add') }}
          </a-button>
        </div>
      </div>
    </div>

    <a-divider/>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="conditions"
      :pagination="false"
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
          :tooltip="$t('label.edit')"
          :disabled="!('updateCondition' in $store.getters.apis) || resource.state !== 'Disabled'"
          icon="edit-outlined"
          @onClick="() => openUpdateConditionModal(record)" />
        <tooltip-button
          :tooltip="$t('label.delete')"
          :disabled="!('deleteCondition' in $store.getters.apis) || resource.state !== 'Disabled'"
          type="primary"
          :danger="true"
          icon="delete-outlined"
          @onClick="deleteConditionFromAutoScalePolicy(record.id)" />
      </template>
    </a-table>

    <a-modal
      :title="$t('label.update.condition')"
      :visible="updateConditionModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="updateConditionModalVisible = false">
      <span v-show="updateConditionModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <div class="update-condition" v-if="selectedCondition" v-ctrl-enter="handleSubmitUpdateConditionForm">
        <div class="update-condition__item">
          <p class="update-condition__label">{{ $t('label.counter') }}</p>
          {{ updateConditionDetails.countername }}
        </div>
        <div class="update-condition__item">
          <p class="update-condition__label">{{ $t('label.relationaloperator') }}</p>
          <a-select
            v-model:value="updateConditionDetails.relationaloperator"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
            <a-select-option value="GE">{{ getOperator('GE') }}</a-select-option>
            <a-select-option value="LT">{{ getOperator('LT') }}</a-select-option>
            <a-select-option value="LE">{{ getOperator('LE') }}</a-select-option>
            <a-select-option value="EQ">{{ getOperator('EQ') }}</a-select-option>
          </a-select>
        </div>
        <div class="update-condition__item">
          <p class="update-condition__label">{{ $t('label.threshold') }}</p>
          <a-input v-focus="true" v-model:value="updateConditionDetails.threshold" />
        </div>
        <div :span="24" class="action-button">
          <a-button @click="() => updateConditionModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmitUpdateConditionForm">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'conditionsTab',
  components: {
    Status,
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
      filterColumns: ['Action'],
      loading: true,
      policyid: null,
      duration: null,
      quiettime: null,
      conditions: [],
      newCondition: {
        counterid: null,
        relationaloperator: null,
        threshold: null
      },
      selectedCondition: null,
      updateConditionModalVisible: false,
      updateConditionModalLoading: false,
      updateConditionDetails: {
        counter: '',
        relationaloperator: '',
        threshold: ''
      },
      countersList: [],
      columns: [
        {
          title: this.$t('label.counter'),
          dataIndex: 'countername'
        },
        {
          title: this.$t('label.relationaloperator'),
          slots: { customRender: 'relationaloperator' }
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
        this.duration = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.scaleuppolicies?.[0]?.duration
        this.quiettime = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.scaleuppolicies?.[0]?.quiettime
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
    handleCancel () {
      this.parentFetchData()
    },
    getOperator (val) {
      if (val === 'GT' || val === 'gt') return this.$t('label.operator.greater')
      if (val === 'GE' || val === 'ge') return this.$t('label.operator.greater.or.equal')
      if (val === 'LT' || val === 'lt') return this.$t('label.operator.less')
      if (val === 'LE' || val === 'le') return this.$t('label.operator.less.or.equal')
      if (val === 'EQ' || val === 'eq') return this.$t('label.operator.equal')
      return val
    },
    deleteCondition (conditionId) {
      this.loading = true
      api('deleteCondition', { id: conditionId }).then(response => {
        const jobId = response.deleteconditionresponse.jobid
        this.$pollJob({
          title: this.$t('label.action.delete.condition'),
          description: conditionId,
          jobId: jobId,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.delete.condition.failed'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: this.$t('message.delete.condition.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => this.fetchData()
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    openUpdateConditionModal (condition) {
      this.selectedCondition = condition
      this.updateConditionModalVisible = true
      this.updateConditionDetails.countername = this.selectedCondition.countername
      this.updateConditionDetails.relationaloperator = this.selectedCondition.relationaloperator
      this.updateConditionDetails.threshold = this.selectedCondition.threshold
    },
    handleSubmitUpdateConditionForm () {
      if (this.updateConditionModalLoading) return
      this.updateConditionModalLoading = true
      api('updateCondition', {
        id: this.selectedCondition.id,
        relationaloperator: this.updateConditionDetails.relationaloperator,
        threshold: this.updateConditionDetails.threshold
      }).then(response => {
        this.$pollJob({
          jobId: response.updateconditionresponse.jobid,
          successMessage: this.$t('message.success.update.condition'),
          successMethod: () => {
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.update.condition.failed'),
          errorMethod: () => {
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.update.condition.processing')
        })
      }).catch(error => {
        this.$notifyError(error)
        this.updateConditionModalLoading = false
      })
    },
    deleteConditionFromAutoScalePolicy (conditionId) {
      this.updateAutoScalePolicy(null, conditionId)
    },
    addCondition () {
      if (this.loading) return

      if (!this.newCondition.counterid) {
        this.$refs.newConditionCounterId.classList.add('error')
      } else {
        this.$refs.newConditionCounterId.classList.remove('error')
      }

      if (!this.newCondition.relationaloperator) {
        this.$refs.newConditionRelationalOperator.classList.add('error')
      } else {
        this.$refs.newConditionRelationalOperator.classList.remove('error')
      }

      if (!this.newCondition.threshold) {
        this.$refs.newConditionThreshold.classList.add('error')
      } else {
        this.$refs.newConditionThreshold.classList.remove('error')
      }

      if (!this.newCondition.counterid || !this.newCondition.relationaloperator || !this.newCondition.threshold) {
        return
      }

      this.loading = true

      api('createCondition', { ...this.newCondition }).then(response => {
        this.$pollJob({
          jobId: response.conditionresponse.jobid,
          successMethod: (result) => {
            const newConditionId = result.jobresult.condition?.id
            this.updateAutoScalePolicy(newConditionId, null)
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
    updateAutoScalePolicy (conditionIdToAdd, conditionIdToRemove) {
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
        duration: this.duration,
        quiettime: this.quiettime,
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
      this.updateConditionModalVisible = false
      this.updateConditionModalLoading = false
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

  .update-condition {

    .ant-select {
      width: 100%;
    }

    &__item {
      margin-bottom: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;
    align-items: flex-start;

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

    &__required {
      margin-right: 5px;
      color: red;
    }

    .error-text {
      display: none;
      color: red;
      font-size: 0.8rem;
    }

    .error {

      input {
        border-color: red;
      }

      .error-text {
        display: block;
      }

    }

  }
  .pagination {
    margin-top: 20px;
  }
</style>
