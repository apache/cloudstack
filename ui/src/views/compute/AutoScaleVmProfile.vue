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
          v-html="$t('message.autoscale.vmprofile.update')" />
        </template>
      </a-alert>

      <a-divider/>
      <div class="form">
        <div class="form__item" v-if="resource.lbprovider === 'Netscaler'">
          <div class="form__label">{{ $t('label.user') }}</div>
          <a-select
            :disabled="true"
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="autoscaleuserid">
            <a-select-option
              v-for="(user, index) in usersList"
              :value="user.id"
              :key="index"
              :label="user.username">
              {{ user.username }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.expungevmgraceperiod')" :tooltip="createAutoScaleVmProfileApiParams.expungevmgraceperiod.description"/>
          </div>
          {{ expungevmgraceperiod }}
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.templatename')" :tooltip="createAutoScaleVmProfileApiParams.templateid.description"/>
          </div>
          {{ getTemplateName(templateid) }}
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="createAutoScaleVmProfileApiParams.serviceofferingid.description"/>
          </div>
          {{ getServiceOfferingName(serviceofferingid) }}
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <a-button ref="submit" :disabled="!('updateAutoScaleVmProfile' in $store.getters.apis) || resource.state !== 'DISABLED'" type="primary" @click="editProfileModalVisible = true">
            <template #icon><edit-outlined /></template>
            {{ $t('label.edit.autoscale.vmprofile') }}
          </a-button>
        </div>
      </div>

      <a-divider/>
      <div class="title">
        <div class="form__label">
          <tooltip-label :title="$t('label.params')" :tooltip="createAutoScaleVmProfileApiParams.otherdeployparams.description"/>
        </div>
      </div>
      <div class="form" v-ctrl-enter="addParam">
        <div class="form__item" ref="newParamName">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.param.name') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            v-model:value="newParam.name">
            <a-select-option v-for="(param, index) in paramNameList" :value="param" :key="index">
              {{ param }}
            </a-select-option>
          </a-select>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newParamValue">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.param.value') }}</div>
          <a-input v-model:value="newParam.value"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <a-button ref="submit" :disabled="!('updateAutoScaleVmProfile' in $store.getters.apis) || resource.state !== 'DISABLED'" type="primary" @click="addParam">
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
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
        <a-popconfirm
          :title="$t('label.delete') + '?'"
          @confirm="deleteParam(record.name)"
          :okText="$t('label.yes')"
          :cancelText="$t('label.no')"
        >
          <tooltip-button
            :tooltip="$t('label.delete')"
            :disabled="!('deleteCondition' in $store.getters.apis) || resource.state !== 'DISABLED'"
            type="primary"
            :danger="true"
            icon="delete-outlined" />
        </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal
      :title="$t('label.edit.autoscale.vmprofile')"
      :visible="editProfileModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="editProfileModalVisible = false">

      <div class="form">
        <div class="form__item" v-if="resource.lbprovider === 'Netscaler'">
          <div class="form__label">{{ $t('label.user') }}</div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }"
            v-focus="true"
            v-model:value="autoscaleuserid">
            <a-select-option
              v-for="(user, index) in usersList"
              :value="user.id"
              :key="index"
              :label="user.username">
              {{ user.username }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.expungevmgraceperiod')" :tooltip="createAutoScaleVmProfileApiParams.expungevmgraceperiod.description"/>
          </div>
          <a-input v-model:value="expungevmgraceperiod" type="number"></a-input>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.templatename')" :tooltip="createAutoScaleVmProfileApiParams.templateid.description"/>
          </div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
            v-focus="true"
            v-model:value="templateid">
            <a-select-option
              v-for="(template, index) in templatesList"
              :value="template.id"
              :key="index"
              :label="template.name">
              {{ template.name }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="createAutoScaleVmProfileApiParams.serviceofferingid.description"/>
          </div>
          <a-select
            style="width: 100%"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
            v-focus="true"
            v-model:value="serviceofferingid">
            <a-select-option
              v-for="(offering, index) in serviceOfferingsList"
              :value="offering.id"
              :key="index"
              :label="offering.name">
              {{ offering.name }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">
            <tooltip-label :title="$t('label.userdata')" :tooltip="createAutoScaleVmProfileApiParams.userdata.description"/>
          </div>
          <a-textarea v-model:value="userdata">
          </a-textarea>
        </div>
      </div>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="updateAutoScaleVmProfile">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import { isAdmin, isAdminOrDomainAdmin } from '@/role'
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
      editProfileModalVisible: false,
      profileid: null,
      autoscaleuserid: null,
      expungevmgraceperiod: null,
      templateid: null,
      serviceofferingid: null,
      userdata: null,
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
          title: this.$t('label.actions'),
          key: 'actions'
        }
      ]
    }
  },
  beforeCreate () {
    this.createAutoScaleVmProfileApi = this.$store.getters.apis.createAutoScaleVmProfile || {}
    this.createAutoScaleVmProfileApiParams = {}
    this.createAutoScaleVmProfileApi.params.forEach(param => {
      this.createAutoScaleVmProfileApiParams[param.name] = param
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
      this.counterParams = ['snmpcommunity', 'snmpport']
      this.deployParams = ['overridediskofferingid', 'rootdisksize', 'diskofferingid', 'disksize',
        'keypairs', 'affinitygroupids', 'networkids', 'securitygroupids']
      if (this.resource.lbprovider === 'Netscaler') {
        this.paramNameList = this.counterParams.concat(this.deployParams)
      } else if (('VirtualRouter', 'VpcVirtualRouter').includes(this.resource.lbprovider)) {
        this.paramNameList = this.deployParams
      }
      this.paramNameList = this.paramNameList.sort()
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
      const params = {
        listall: 'true',
        domainid: this.resource.domainid,
        account: this.resource.account
      }
      if (isAdmin()) {
        params.templatefilter = 'all'
      } else {
        params.templatefilter = 'executable'
      }
      api('listTemplates', params).then(json => {
        this.templatesList = json.listtemplatesresponse?.template || []
      })
    },
    fetchServiceOfferingData () {
      const params = {
        listall: 'true',
        issystem: 'false'
      }
      if (isAdminOrDomainAdmin()) {
        params.isrecursive = 'true'
      }
      api('listServiceOfferings', params).then(json => {
        this.serviceOfferingsList = json.listserviceofferingsresponse?.serviceoffering || []
        this.serviceOfferingsList = this.serviceOfferingsList.filter(offering => !offering.iscustomized)
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
        this.expungevmgraceperiod = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.expungevmgraceperiod
        this.serviceofferingid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.serviceofferingid
        this.templateid = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.templateid
        this.userdata = this.decodeUserData(decodeURIComponent(response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.userdata || ''))
        const counterparam = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.counterparam || {}
        const otherdeployparams = response.listautoscalevmprofilesresponse?.autoscalevmprofile?.[0]?.otherdeployparams || {}
        this.finalizeParams(counterparam, otherdeployparams)
      }).finally(() => {
        this.loading = false
      })
    },
    getTemplateName (templateid) {
      for (const template of this.templatesList) {
        if (template.id === templateid) {
          return template.name
        }
      }
      return ''
    },
    getServiceOfferingName (serviceofferingid) {
      for (const serviceoffering of this.serviceOfferingsList) {
        if (serviceoffering.id === serviceofferingid) {
          return serviceoffering.name
        }
      }
      return ''
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
      if (otherdeployparams.rootdisksize) {
        this.allParams.push({ name: 'rootdisksize', value: otherdeployparams.rootdisksize })
      }
      if (otherdeployparams.disksize) {
        this.allParams.push({ name: 'disksize', value: otherdeployparams.disksize })
      }
      if (otherdeployparams.overridediskofferingid) {
        this.allParams.push({ name: 'overridediskofferingid', value: otherdeployparams.overridediskofferingid })
      }
      if (otherdeployparams.keypairs) {
        this.allParams.push({ name: 'keypairs', value: otherdeployparams.keypairs })
      }
      if (otherdeployparams.affinitygroupids) {
        this.allParams.push({ name: 'affinitygroupids', value: otherdeployparams.affinitygroupids })
      }
      if (otherdeployparams.networkids) {
        this.allParams.push({ name: 'networkids', value: otherdeployparams.networkids })
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
      var j = 0
      for (var index = 0; index < this.allParams.length; index++) {
        var param = { ...this.allParams[index] }
        if (this.counterParams.includes(param.name) && param.name !== paramNameToAdd && param.name !== paramNameToRemove) {
          params['counterparam[' + i + '].name'] = param.name
          params['counterparam[' + i + '].value'] = param.value
          i++
        }
        if (this.deployParams.includes(param.name) && param.name !== paramNameToAdd && param.name !== paramNameToRemove) {
          params['otherdeployparams[' + j + '].name'] = param.name
          params['otherdeployparams[' + j + '].value'] = param.value
          j++
        }
      }
      if (paramNameToAdd && this.counterParams.includes(paramNameToAdd) && paramValueToAdd) {
        params['counterparam[' + i + '].name'] = paramNameToAdd
        params['counterparam[' + i + '].value'] = paramValueToAdd
      }

      if (paramNameToAdd && this.deployParams.includes(paramNameToAdd) && paramValueToAdd) {
        params['otherdeployparams[' + j + '].name'] = paramNameToAdd
        params['otherdeployparams[' + j + '].value'] = paramValueToAdd
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
      const params = {
        id: this.profileid,
        expungevmgraceperiod: this.expungevmgraceperiod,
        serviceofferingid: this.serviceofferingid,
        templateid: this.templateid
      }
      if (this.autoscaleuserid) {
        params.autoscaleuserid = this.autoscaleuserid
      }
      if (this.userdata && this.userdata.length > 0) {
        params.userdata = encodeURIComponent(btoa(this.sanitizeReverse(this.userdata)))
      }

      const httpMethod = params.userdata ? 'POST' : 'GET'
      const args = httpMethod === 'POST' ? {} : params
      const data = httpMethod === 'POST' ? params : {}

      api('updateAutoScaleVmProfile', args, httpMethod, data).then(response => {
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
    sanitizeReverse (value) {
      const reversedValue = value
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')

      return reversedValue
    },
    decodeUserData (userdata) {
      const decodedData = Buffer.from(userdata, 'base64')
      return decodedData.toString('utf-8')
    },
    closeModal () {
      this.editProfileModalVisible = false
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
