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
  <a-spin :spinning="loading">
    <a-alert
      v-if="disableSettings"
      banner
      :message="$t('message.action.settings.warning.vm.running')" />
    <div v-else>
      <div v-show="!showAddDetail">
        <a-button
          type="dashed"
          style="width: 100%"
          :disabled="!('updateTemplate' in $store.getters.apis && 'updateVirtualMachine' in $store.getters.apis && isAdminOrOwner())"
          @click="onShowAddDetail">
          <template #icon><plus-outlined /></template>
          {{ $t('label.add.setting') }}
        </a-button>
      </div>
      <div v-show="showAddDetail">
        <a-input-group
          type="text"
          compact>
          <a-auto-complete
            class="detail-input"
            ref="keyElm"
            :filterOption="filterOption"
            v-model:value="newKey"
            :options="detailKeys"
            :placeholder="$t('label.name')"
            @change="e => onAddInputChange(e, 'newKey')" />
          <a-input
            class="tag-disabled-input"
            style=" width: 30px; border-left: 0; pointer-events: none; text-align: center"
            placeholder="="
            disabled />
          <a-auto-complete
            class="detail-input"
            :filterOption="filterOption"
            v-model:value="newValue"
            :options="detailValues"
            :placeholder="$t('label.value')"
            @change="e => onAddInputChange(e, 'newValue')" />
          <tooltip-button :tooltip="$t('label.add.setting')" icon="check-outlined" @onClick="addDetail" buttonClass="detail-button" />
          <tooltip-button :tooltip="$t('label.cancel')" icon="close-outlined" @onClick="closeDetail" buttonClass="detail-button" />
        </a-input-group>
        <p v-if="error" style="color: red"> {{ $t(error) }} </p>
      </div>
    </div>
    <a-list size="large">
      <a-list-item :key="index" v-for="(item, index) in details">
        <a-list-item-meta>
          <template #title>
            {{ item.name }}
          </template>
          <template #description>
            <div v-if="item.edit" style="display: flex">
              <a-auto-complete
                style="width: 100%"
                v-model:value="item.value"
                :options="detailOptions[item.name]"
                @change="val => handleInputChange(val, index)"
                @pressEnter="e => updateDetail(index)" />
              <tooltip-button
                buttonClass="edit-button"
                :tooltip="$t('label.cancel')"
                @onClick="hideEditDetail(index)"
                v-if="item.edit"
                iconType="close-circle-two-tone"
                iconTwoToneColor="#f5222d" />
              <tooltip-button
                buttonClass="edit-button"
                :tooltip="$t('label.ok')"
                @onClick="updateDetail(index)"
                v-if="item.edit"
                iconType="check-circle-two-tone"
                iconTwoToneColor="#52c41a" />
            </div>
            <span v-else style="word-break: break-all">{{ item.value }}</span>
          </template>
        </a-list-item-meta>
        <template #actions>
          <div
            v-if="!disableSettings && 'updateTemplate' in $store.getters.apis &&
              'updateVirtualMachine' in $store.getters.apis && isAdminOrOwner() && allowEditOfDetail(item.name)">
            <tooltip-button
              :tooltip="$t('label.edit')"
              icon="edit-outlined"
              :disabled="deployasistemplate === true"
              v-if="!item.edit"
              @onClick="showEditDetail(index)" />
          </div>
          <div
            v-if="!disableSettings && 'updateTemplate' in $store.getters.apis &&
              'updateVirtualMachine' in $store.getters.apis && isAdminOrOwner() && allowEditOfDetail(item.name)">
            <a-popconfirm
              :title="`${$t('label.delete.setting')}?`"
              @confirm="deleteDetail(index)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
              placement="left"
            >
              <tooltip-button :tooltip="$t('label.delete')" :disabled="deployasistemplate === true" type="primary" :danger="true" icon="delete-outlined" />
            </a-popconfirm>
          </div>
        </template>
      </a-list-item>
    </a-list>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  components: { TooltipButton },
  name: 'DetailSettings',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      details: [],
      detailOptions: {},
      showAddDetail: false,
      disableSettings: false,
      newKey: '',
      newValue: '',
      loading: false,
      resourceType: 'UserVm',
      deployasistemplate: false,
      error: false
    }
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        this.updateResource(newItem)
      }
    }
  },
  computed: {
    detailKeys () {
      return Object.keys(this.detailOptions).map(key => {
        return { value: key }
      })
    },
    detailValues () {
      if (!this.newKey) {
        return []
      }
      if (!Array.isArray(this.detailOptions[this.newKey])) {
        return { value: this.detailOptions[this.newKey] }
      }
      return this.detailOptions[this.newKey].map(value => {
        return { value: value }
      })
    }
  },
  created () {
    this.updateResource(this.resource)
  },
  methods: {
    filterOption (input, option) {
      return (
        option.value.toUpperCase().indexOf(input.toUpperCase()) >= 0
      )
    },
    updateResource (resource) {
      this.details = []
      if (!resource) {
        return
      }
      this.resourceType = this.$route.meta.resourceType
      if (resource.details) {
        this.details = Object.keys(resource.details).map(k => {
          return { name: k, value: resource.details[k], edit: false }
        })
      }
      api('listDetailOptions', { resourcetype: this.resourceType, resourceid: resource.id }).then(json => {
        this.detailOptions = json.listdetailoptionsresponse.detailoptions.details
      })
      this.disableSettings = (this.$route.meta.name === 'vm' && resource.state !== 'Stopped')
      api('listTemplates', { templatefilter: 'all', id: resource.templateid }).then(json => {
        this.deployasistemplate = json.listtemplatesresponse.template[0].deployasis
      })
    },
    allowEditOfDetail (name) {
      if (this.resource.readonlydetails) {
        if (this.resource.readonlydetails.split(',').map(item => item.trim()).includes(name)) {
          return false
        }
      }
      return true
    },
    showEditDetail (index) {
      this.details[index].edit = true
      this.details[index].originalValue = this.details[index].value
    },
    hideEditDetail (index) {
      this.details[index].edit = false
      this.details[index].value = this.details[index].originalValue
    },
    handleInputChange (val, index) {
      this.details[index].value = val
    },
    onAddInputChange (val, obj) {
      this.error = false
      this[obj] = val
    },
    isAdminOrOwner () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) ||
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.account === this.$store.getters.userInfo.account) ||
        this.resource.project && this.resource.projectid === this.$store.getters.project.id
    },
    getDetailsParam (details) {
      var params = {}
      var filteredDetails = details
      if (this.resource.readonlydetails && filteredDetails) {
        filteredDetails = []
        var readOnlyDetailNames = this.resource.readonlydetails.split(',').map(item => item.trim())
        for (var detail of this.details) {
          if (!readOnlyDetailNames.includes(detail.name)) {
            filteredDetails.push(detail)
          }
        }
      }
      if (filteredDetails.length === 0) {
        params.cleanupdetails = true
      } else {
        filteredDetails.forEach(function (item, index) {
          params['details[0].' + item.name] = item.value
        })
      }
      return params
    },
    runApi () {
      var apiName = ''
      if (this.resourceType === 'UserVm') {
        apiName = 'updateVirtualMachine'
      } else if (this.resourceType === 'Template') {
        apiName = 'updateTemplate'
      }
      if (!(apiName in this.$store.getters.apis)) {
        this.$notification.error({
          message: this.$t('error.execute.api.failed') + ' ' + apiName,
          description: this.$t('message.user.not.permitted.api')
        })
        return
      }

      var params = { id: this.resource.id }
      params = Object.assign(params, this.getDetailsParam(this.details))
      this.loading = true
      api(apiName, params).then(json => {
        var details = {}
        if (this.resourceType === 'UserVm' && json.updatevirtualmachineresponse.virtualmachine.details) {
          details = json.updatevirtualmachineresponse.virtualmachine.details
        } else if (this.resourceType === 'Template' && json.updatetemplateresponse.template.details) {
          details = json.updatetemplateresponse.template.details
        }
        this.details = Object.keys(details).map(k => {
          return { name: k, value: details[k], edit: false }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.loading = false
        this.showAddDetail = false
        this.newKey = ''
        this.newValue = ''
      })
    },
    addDetail () {
      if (this.newKey === '' || this.newValue === '') {
        this.error = this.$t('message.error.provide.setting')
        return
      }
      if (!this.allowEditOfDetail(this.newKey)) {
        this.error = this.$t('error.unable.to.proceed')
        return
      }
      this.error = false
      this.details.push({ name: this.newKey, value: this.newValue })
      this.runApi()
    },
    updateDetail (index) {
      this.runApi()
    },
    deleteDetail (index) {
      this.details.splice(index, 1)
      this.runApi()
    },
    onShowAddDetail () {
      this.showAddDetail = true
      setTimeout(() => {
        this.$refs.keyElm.focus()
      })
    },
    closeDetail () {
      this.newKey = ''
      this.newValue = ''
      this.error = false
      this.showAddDetail = false
    }
  }
}
</script>

<style scoped lang="less">
.detail-input {
  width: calc(calc(100% / 2) - 45px);
}

.detail-button {
  width: 30px;
}
</style>
