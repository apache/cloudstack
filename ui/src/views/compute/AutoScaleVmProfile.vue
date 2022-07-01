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
          v-html="$t('message.autoscale.vmprofile.update')" />
        </template>
      </a-alert>

      <a-divider/>
      <div class="form" v-ctrl-enter="updateAutoScaleVmProfile">
        <div class="form__item">
          <div class="form__label">{{ $t('label.user') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="autoscaleuserid">
            <a-select-option v-for="(user, index) in usersList" :value="user.id" :key="index">
              {{ user.username }}
            </a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.destroyvmgraceperiod') }}</div>
          <a-input v-model:value="destroyvmgraceperiod" type="number"></a-input>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('label.templateid') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="templateid">
            <a-select-option v-for="(template, index) in templatesList" :value="template.id" :key="index">
              {{ template.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.serviceofferingid') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :disabled='true'
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="serviceofferingid">
            <a-select-option v-for="(offering, index) in serviceOfferingsList" :value="offering.id" :key="index">
              {{ offering.name }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <a-button ref="submit" :disabled="!('updateAutoScaleVmProfile' in $store.getters.apis) || resource.state !== 'Disabled'" type="primary" @click="updateAutoScaleVmProfile()">
            <template #icon><edit-outlined /></template>
            {{ $t('label.edit') }}
          </a-button>
        </div>
      </div>

      <a-divider/>
      <div class="form" v-ctrl-enter="addParam">
        <div class="form__item" ref="newParamName">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.param.name') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="newParam.name">
            <a-select-option v-for="(param, index) in counterParams" :value="param" :key="index">
              {{ param }}
            </a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newParamValue">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.param.value') }}</div>
          <a-input v-model:value="newParam.value" type="number"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('updateAutoScaleVmProfile' in $store.getters.apis) || resource.state !== 'Disabled'" type="primary" @click="addParam">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add.param') }}
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
      :dataSource="allParams"
      :pagination="false"
      :rowKey="record => record.name">
      <template #name="{ record }">
        {{ record.name }}
      </template>
      <template #threshold="{ record }">
        {{ record.threshold }}
      </template>
      <template #actions="{ record }">
        <tooltip-button
          :tooltip="$t('label.delete')"
          :disabled="!('deleteCondition' in $store.getters.apis) || resource.state !== 'Disabled'"
          type="primary"
          :danger="true"
          icon="delete-outlined"
          @onClick="deleteParam(record.name)" />
      </template>
    </a-table>
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
      profileid: null,
      autoscaleuserid: null,
      destroyvmgraceperiod: null,
      templateid: null,
      serviceofferingid: null,
      usersList: [],
      templatesList: [],
      serviceOfferingsList: [],
      allParams: [],
      newParam: {
        name: null,
        value: null
      },
      counterParams: [],
      deployParams: [],
      columns: [
        {
          title: this.$t('label.param.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.param.value'),
          dataIndex: 'value'
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
      this.counterParams = ['snmpcommunity', 'snmpport']
      this.deployParams = ['diskofferingid', 'securitygroupids']
      this.paramNameList = ['snmpcommunity', 'snmpport', 'diskofferingid', 'securitygroupids']
      this.fetchUserData()
      this.fetchTemplateData()
      this.fetchServiceOfferingData()
      this.fetchData()
    },
    fetchUserData () {
      api('listUsers', {
        domainid: this.resource.domainid,
        account: this.resource.account
      }).then(json => {
        this.usersList = json.listusersresponse?.user || []
      })
    },
    fetchTemplateData () {
      api('listTemplates', {
        templatefilter: 'all',
        listall: 'true',
        domainid: this.resource.domainid,
        account: this.resource.account
      }).then(json => {
        this.templatesList = json.listtemplatesresponse?.template || []
      })
    },
    fetchServiceOfferingData () {
      api('listServiceOfferings', {
        listall: 'true',
        isrecursive: 'true',
        issystem: 'false'
      }).then(json => {
        this.serviceOfferingsList = json.listserviceofferingsresponse?.serviceoffering || []
      })
    },
    fetchData () {
      this.loading = true
      api('listAutoScaleVmProfiles', {
        listAll: true,
        id: this.resource.vmprofileid
      }).then(response => {
        this.profileid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.id
        this.autoscaleuserid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.autoscaleuserid
        this.destroyvmgraceperiod = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.destroyvmgraceperiod
        this.serviceofferingid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.serviceofferingid
        this.templateid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.templateid
        const counterparam = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.counterparam || {}
        const otherdeployparams = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.otherdeployparams || {}
        this.finalizeParams(counterparam, otherdeployparams)
      }).finally(() => {
        this.loading = false
      })
    },
    handleCancel () {
      this.parentFetchData()
    },
    finalizeParams (counterparam, otherdeployparams) {
      this.allParams = []
      if (counterparam.snmpcommunity) {
        this.allParams.push({ name: 'snmpcommunity', value: counterparam.snmpcommunity })
      }
      if (counterparam.snmpport) {
        this.allParams.push({ name: 'snmpport', value: counterparam.snmpport })
      }
      if (otherdeployparams.diskofferingid) {
        this.allParams.push({ name: 'diskofferingid', value: otherdeployparams.diskofferingid })
      }
      if (otherdeployparams.securitygroupids) {
        this.allParams.push({ name: 'securitygroupids', value: otherdeployparams.securitygroupids })
      }
    },
    deleteParam (paramName) {
      this.updateAutoScaleVmProfileWithParam(null, null, paramName)
    },
    addParam () {
      if (this.loading) return
      if (!this.newParam.name) {
        this.$refs.newParamName.classList.add('error')
      } else {
        this.$refs.newParamName.classList.remove('error')
      }
      if (!this.newParam.value) {
        this.$refs.newParamValue.classList.add('error')
      } else {
        this.$refs.newParamValue.classList.remove('error')
      }
      if (!this.newParam.name || !this.newParam.value) {
        return
      }
      this.updateAutoScaleVmProfileWithParam(this.newParam.name, this.newParam.value, null)
    },
    updateAutoScaleVmProfileWithParam (paramNameToAdd, paramValueToAdd, paramNameToRemove) {
      if (this.loading) return
      this.loading = true

      var params = {
        id: this.profileid
      }
      var i = 0
      for (var index = 0; index < this.allParams.length; index++) {
        var param = { ...this.allParams[index] }
        if (param.name !== paramNameToAdd && param.name !== paramNameToRemove) {
          params['counterparam[' + i + '].name'] = param.name
          params['counterparam[' + i + '].value'] = param.value
        }
        i++
      }
      if (paramNameToAdd && this.counterParams.includes(paramNameToAdd) && paramValueToAdd) {
        params['counterparam[' + i + '].name'] = paramNameToAdd
        params['counterparam[' + i + '].value'] = paramValueToAdd
      }

      api('updateAutoScaleVmProfile', params).then(response => {
        this.$pollJob({
          jobId: response.updateautoscalevmprofileresponse.jobid,
          successMethod: (result) => {
          },
          errorMessage: this.$t('message.update.autoscale.vm.profile.failed'),
          errorMethod: () => {
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    updateAutoScaleVmProfile () {
      api('updateAutoScaleVmProfile', {
        id: this.profileid,
        autoscaleuserid: this.autoscaleuserid,
        destroyvmgraceperiod: this.destroyvmgraceperiod,
        serviceofferingid: this.serviceofferingid,
        templateid: this.templateid
      }).then(response => {
        this.$pollJob({
          jobId: response.updateautoscalevmprofileresponse.jobid,
          successMethod: (result) => {
          },
          errorMessage: this.$t('message.update.autoscale.vm.profile.failed'),
          errorMethod: () => {
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    closeModal () {
      this.showConfirmationAction = false
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
