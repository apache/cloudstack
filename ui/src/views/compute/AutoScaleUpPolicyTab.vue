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
      <a-alert type="info" v-if="resource.state !== 'DISABLED'">
        <template #message>
          <div
            v-html="$t('message.autoscale.policies.update')" />
        </template>
      </a-alert>

      <a-divider/>
      <div class="form">
        <strong>{{ $t('label.scaleup.policy') }} &nbsp;&nbsp;&nbsp;</strong>
        <a-select
          style="width: 320px"
          v-model:value="selectedPolicyId"
          @change="switchPolicy()"
          :placeholder="$t('label.scaleup.policy')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }" >
          <a-select-option
            v-for="(scalepolicy, index) in this.policies"
            :value="scalepolicy.id"
            :key="index"
            :label="scalepolicy.displaytext || scalepolicy.name">
            {{ scalepolicy.name || scalepolicy.id }}
          </a-select-option>
        </a-select>
        <a-button style="margin-left: 10px" ref="submit" type="primary" @click="addPolicyModalVisible = true" :disabled="!('createAutoScalePolicy' in $store.getters.apis) || resource.state !== 'DISABLED'">
          <template #icon><plus-outlined /></template>
          {{ $t('label.add.policy') }}
        </a-button>
        <a-popconfirm
          :title="$t('label.remove.policy') + '?'"
          @confirm="removeScalePolicyFromGroup"
          :okText="$t('label.yes')"
          :cancelText="$t('label.no')"
        >
          <a-button style="margin-left: 10px" ref="submit" type="primary" :danger="true" :disabled="!('updateAutoScaleVmGroup' in $store.getters.apis) || resource.state !== 'DISABLED'">
            <template #icon><delete-outlined /></template>
            {{ $t('label.remove.policy') }}
          </a-button>
        </a-popconfirm>

      </div>

      <a-divider/>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.name')" :tooltip="createAutoScalePolicyApiParams.name.description"/>
          </div>
          <a-input v-model:value="policy.name" :disabled="!this.isEditable"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.duration')" :tooltip="createAutoScalePolicyApiParams.duration.description"/>
          </div>
          <a-input v-model:value="policy.duration" type="number" :disabled="!this.isEditable"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.quiettime')" :tooltip="createAutoScalePolicyApiParams.quiettime.description"/>
          </div>
          <a-input v-model:value="policy.quiettime" type="number" :disabled="!this.isEditable"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('updateAutoScalePolicy' in $store.getters.apis) || resource.state !== 'DISABLED'" type="primary" @click="updateAutoScalePolicy(null, null)">
            <template #icon><edit-outlined /></template>
            {{ isEditOrApply() }}
          </a-button>
        </div>
      </div>
    </div>

    <a-divider/>
    <div class="title">
      <div class="form__label">
        <tooltip-label :title="$t('label.conditions')" :tooltip="createAutoScalePolicyApiParams.conditionids.description"/>
      </div>
    </div>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="policy.conditions"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'relationaloperator'">
        {{ getOperator(record.relationaloperator) }}
        </template>
        <template v-if="column.key === 'actions'">
          <tooltip-button
            :tooltip="$t('label.edit')"
            :disabled="!('updateCondition' in $store.getters.apis) || resource.state !== 'DISABLED'"
            icon="edit-outlined"
            @onClick="() => openUpdateConditionModal(record)" />
          <tooltip-button
            :tooltip="$t('label.delete')"
            :disabled="!('deleteCondition' in $store.getters.apis) || resource.state !== 'DISABLED'"
            type="primary"
            :danger="true"
            icon="delete-outlined"
            @onClick="deleteConditionFromAutoScalePolicy(record.id)" />
        </template>
      </template>
    </a-table>

    <div>
      <a-divider/>
      <div class="form" v-ctrl-enter="addConditionToPolicy">
        <div class="form__item" ref="newConditionCounterId">
          <div class="form__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.counter')" :tooltip="$t('label.counter.name')"/>
          </div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="newCondition.counterid">
            <a-select-option
              v-for="(counter, index) in countersList"
              :value="counter.id"
              :key="index"
              :label="counter.name">>
              {{ counter.name }}
            </a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newConditionRelationalOperator">
          <div class="form__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.relationaloperator')" :tooltip="createConditionApiParams.relationaloperator.description"/>
          </div>
          <a-select
            v-model:value="newCondition.relationaloperator"
            style="width: 100%;"
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newConditionThreshold">
          <div class="form__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.threshold')" :tooltip="$t('label.threshold.description')"/>
          </div>
          <a-input v-model:value="newCondition.threshold" type="number"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('createCondition' in $store.getters.apis) || resource.state !== 'DISABLED'" type="primary" @click="addConditionToPolicy">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add.condition') }}
          </a-button>
        </div>
      </div>
    </div>

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
          <p class="update-condition__label"><span class="form__required">*</span>{{ $t('label.counter') }}</p>
          {{ updateConditionDetails.countername }}
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.relationaloperator')" :tooltip="createConditionApiParams.relationaloperator.description"/>
          </div>
          <a-select
            v-model:value="updateConditionDetails.relationaloperator"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
          </a-select>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.threshold')" :tooltip="$t('label.threshold.description')"/>
          </div>
          <a-input v-focus="true" v-model:value="updateConditionDetails.threshold" type="number" />
        </div>
        <div :span="24" class="action-button">
          <a-button @click="() => updateConditionModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmitUpdateConditionForm">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      :title="$t('label.add.policy')"
      :visible="addPolicyModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="addPolicyModalVisible = false">

      <div class="update-condition">
        <div class="update-condition__item">
          <div class="update-condition__label">
            <tooltip-label :title="$t('label.name')" :tooltip="createAutoScalePolicyApiParams.name.description"/>
          </div>
          <a-input v-model:value="newPolicy.name" v-focus="true"></a-input>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.duration')" :tooltip="createAutoScalePolicyApiParams.duration.description"/>
          </div>
          <a-input v-model:value="newPolicy.duration" type="number"></a-input>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.quiettime')" :tooltip="createAutoScalePolicyApiParams.quiettime.description"/>
          </div>
          <a-input v-model:value="newPolicy.quiettime" type="number"></a-input>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.counter')" :tooltip="$t('label.counter.name')"/>
          </div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-model:value="newPolicy.counterid">
            <a-select-option
              v-for="(counter, index) in countersList"
              :value="counter.id"
              :key="index"
              :label="counter.name">
              {{ counter.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.relationaloperator')" :tooltip="createConditionApiParams.relationaloperator.description"/>
          </div>
          <a-select
            v-model:value="newPolicy.relationaloperator"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="GT">{{ getOperator('GT') }}</a-select-option>
          </a-select>
        </div>
        <div class="update-condition__item">
          <div class="update-condition__label">
            <span class="form__required">*</span>
            <tooltip-label :title="$t('label.threshold')" :tooltip="$t('label.threshold.description')"/>
          </div>
          <a-input v-model:value="newPolicy.threshold" type="number"></a-input>
        </div>
      </div>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="addNewScalePolicyToGroup">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'conditionsTab',
  components: {
    Status,
    TooltipButton,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      filterColumns: ['Actions'],
      loading: true,
      policies: [],
      isEditable: false,
      policy: {
        duration: null,
        quiettime: null,
        conditions: []
      },
      selectedPolicyId: null,
      newCondition: {
        counterid: null,
        relationaloperator: 'GT',
        threshold: null
      },
      addPolicyModalVisible: null,
      newPolicy: {
        duration: null,
        quiettime: 300,
        counterid: null,
        relationaloperator: 'GT',
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
          key: 'relationaloperator'
        },
        {
          title: this.$t('label.threshold'),
          dataIndex: 'threshold'
        },
        {
          title: this.$t('label.actions'),
          key: 'actions'
        }
      ]
    }
  },
  beforeCreate () {
    this.createConditionApi = this.$store.getters.apis.createCondition || {}
    this.createConditionApiParams = {}
    this.createConditionApi.params.forEach(param => {
      this.createConditionApiParams[param.name] = param
    })
    this.createAutoScalePolicyApi = this.$store.getters.apis.createAutoScalePolicy || {}
    this.createAutoScalePolicyApiParams = {}
    this.createAutoScalePolicyApi.params.forEach(param => {
      this.createAutoScalePolicyApiParams[param.name] = param
    })
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
        const lbruleid = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.lbruleid
        this.policies = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]?.scaleuppolicies
        if (this.selectedPolicyId) {
          this.switchPolicy()
        } else {
          this.policy = this.policies?.[0]
          this.selectedPolicyId = this.policy.id
        }
        api('listLoadBalancerRules', {
          listAll: true,
          id: lbruleid
        }).then(response => {
          const networkid = response.listloadbalancerrulesresponse?.loadbalancerrule?.[0]?.networkid
          api('listNetworks', {
            listAll: true,
            projectid: this.resource.projectid,
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
        id: this.selectedPolicyId
      }).then(response => {
        this.policy = response.listautoscalepoliciesresponse?.autoscalepolicy[0]
      }).finally(() => {
        this.loading = false
      })
    },
    switchPolicy () {
      this.policy = this.policies.filter(policy => policy.id === this.selectedPolicyId)[0]
      this.isEditable = false
    },
    isEditOrApply () {
      if (this.isEditable) return this.$t('label.apply')
      else return this.$t('label.edit')
    },
    handleCancel () {
      this.parentFetchData()
    },
    isNumber (value) {
      if (value && (isNaN(value) || value < 0)) {
        return false
      }
      return true
    },
    isPositiveNumber (value) {
      if (value && (isNaN(value) || value <= 0)) {
        return false
      }
      return true
    },
    getOperator (val) {
      if (val === 'GT' || val === 'gt') return this.$t('label.operator.greater')
      if (val === 'GE' || val === 'ge') return this.$t('label.operator.greater.or.equal')
      if (val === 'LT' || val === 'lt') return this.$t('label.operator.less')
      if (val === 'LE' || val === 'le') return this.$t('label.operator.less.or.equal')
      if (val === 'EQ' || val === 'eq') return this.$t('label.operator.equal')
      return val
    },
    async pollJob (jobId) {
      return new Promise(resolve => {
        const asyncJobInterval = setInterval(() => {
          api('queryAsyncJobResult', { jobId }).then(async json => {
            const result = json.queryasyncjobresultresponse
            if (result.jobstatus === 0) {
              return
            }

            clearInterval(asyncJobInterval)
            resolve(result)
          })
        }, 1000)
      })
    },
    addCondition (params) {
      this.loading = true
      return new Promise((resolve, reject) => {
        api('createCondition', params).then(async json => {
          this.$pollJob({
            jobId: json.conditionresponse.jobid,
            successMethod: (result) => {
              resolve(result.jobresult.condition)
            },
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          reject(error)
        }).finally(
          this.loading = false
        )
      })
    },
    deleteCondition (conditionId) {
      this.loading = true
      return new Promise((resolve, reject) => {
        api('deleteCondition', { id: conditionId }).then(response => {
          const jobId = response.deleteconditionresponse.jobid
          this.$pollJob({
            title: this.$t('label.action.delete.condition'),
            description: conditionId,
            jobId: jobId,
            successMethod: () => {
              return resolve()
            },
            errorMessage: this.$t('message.delete.condition.failed'),
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            loadingMessage: this.$t('message.delete.condition.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => this.fetchData()
          })
        }).catch(error => {
          reject(error)
        }).finally(() => {
          this.fetchData()
          this.loading = false
        })
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

      if (!this.isNumber(this.updateConditionDetails.threshold)) {
        this.$notification.error({
          message: this.$t('label.threshold'),
          description: this.$t('message.validate.number')
        })
        return
      }

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
    async deleteConditionFromAutoScalePolicy (conditionId) {
      await this.updateAutoScalePolicy(null, conditionId)
    },
    async addConditionToPolicy () {
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

      if (!this.isNumber(this.newCondition.threshold)) {
        this.$notification.error({
          message: this.$t('label.threshold'),
          description: this.$t('message.validate.number')
        })
        return
      }

      const params = { ...this.newCondition }
      params.domainid = this.resource.domainid
      params.account = this.resource.account
      const newCondition = await this.addCondition(params)

      await this.updateAutoScalePolicy(newCondition.id, null)
    },
    async addNewScalePolicyToGroup () {
      if (this.loading) return

      if (!this.newPolicy.duration || !this.newPolicy.counterid || !this.newPolicy.relationaloperator || !this.newPolicy.threshold) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.please.enter.valid.value')
        })
        return
      }

      if (!this.isPositiveNumber(this.newPolicy.duration)) {
        this.$notification.error({
          message: this.$t('label.duration'),
          description: this.$t('message.validate.positive.number')
        })
        return
      }

      if (!this.isNumber(this.newPolicy.threshold)) {
        this.$notification.error({
          message: this.$t('label.threshold'),
          description: this.$t('message.validate.number')
        })
        return
      }

      this.loading = true

      const newCondition = await this.addCondition({
        domainid: this.resource.domainid,
        account: this.resource.account,
        counterid: this.newPolicy.counterid,
        relationaloperator: this.newPolicy.relationaloperator,
        threshold: this.newPolicy.threshold
      })

      const newPolicy = await this.createAutoScalePolicy({
        domainid: this.resource.domainid,
        account: this.resource.account,
        name: this.newPolicy.name,
        conditionids: newCondition.id,
        duration: this.newPolicy.duration,
        quiettime: this.newPolicy.quiettime,
        action: 'ScaleUp'
      })

      this.policies.push(newPolicy)
      this.policy = newPolicy
      this.selectedPolicyId = newPolicy.id

      await this.updateAutoScaleVmGroup({
        id: this.resource.id,
        scaleuppolicyids: this.policies.map(policy => { return policy.id }).join(',')
      })

      this.addPolicyModalVisible = false
    },
    async removeScalePolicyFromGroup () {
      this.policies = this.policies.filter(policy => policy.id !== this.selectedPolicyId)

      await this.updateAutoScaleVmGroup({
        id: this.resource.id,
        scaleuppolicyids: this.policies.map(policy => { return policy.id }).join(',')
      })

      this.selectedPolicyId = this.policies[this.policies.length - 1].id
      this.isEditable = false
    },
    createAutoScalePolicy (params) {
      this.loading = true
      return new Promise((resolve, reject) => {
        api('createAutoScalePolicy', params).then(async json => {
          this.$pollJob({
            jobId: json.autoscalepolicyresponse.jobid,
            successMethod: (result) => {
              resolve(result.jobresult.autoscalepolicy)
            },
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          reject(error)
        }).finally(
          this.loading = false
        )
      })
    },
    updateAutoScalePolicy (conditionIdToAdd, conditionIdToRemove) {
      if (conditionIdToAdd === null && conditionIdToRemove === null && !this.isEditable) {
        this.isEditable = true
        return
      }
      if (this.loading) return
      this.loading = true

      let newConditionIds
      if (conditionIdToAdd) {
        newConditionIds = this.policy.conditions.map(condition => { return condition.id }).join(',') + ',' + conditionIdToAdd
      } else if (conditionIdToRemove) {
        newConditionIds = this.policy.conditions.filter(condition => condition.id !== conditionIdToRemove).map(condition => { return condition.id }).join(',')
      }

      return new Promise((resolve, reject) => {
        api('updateAutoScalePolicy', {
          id: this.policy.id,
          name: this.policy.name,
          duration: this.policy.duration,
          quiettime: this.policy.quiettime,
          conditionids: newConditionIds
        }).then(response => {
          this.$pollJob({
            jobId: response.updateautoscalepolicyresponse.jobid,
            successMethod: (result) => {
              resolve(result.jobresult.autoscalepolicy)
            },
            errorMessage: this.$t('message.update.autoscale.policy.failed'),
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          reject(error)
        }).finally(
          this.loading = false
        )
      })
    },
    updateAutoScaleVmGroup (params) {
      if (this.loading) return
      this.loading = true

      return new Promise((resolve, reject) => {
        api('updateAutoScaleVmGroup', params).then(response => {
          this.$pollJob({
            jobId: response.updateautoscalevmgroupresponse.jobid,
            successMethod: (result) => {
              resolve(result.jobresult.autoscalevmgroup)
            },
            errorMessage: this.$t('message.update.autoscale.vmgroup.failed'),
            errorMethod: (result) => {
              reject(result.jobresult.errortext)
            },
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          reject(error)
        }).finally(
          this.loading = false
        )
      })
    },
    closeModal () {
      this.updateConditionModalVisible = false
      this.updateConditionModalLoading = false
      this.addPolicyModalVisible = false
      this.isEditable = false
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

.title {
  margin-bottom: 5px;
  font-weight: bold;
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
