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
    <a-form
      class="form-layout"
      layout="vertical"
      :form="form"
      @submit="handleSubmit">
      <a-form-item>
        <span slot="label">
          {{ $t('label.name') }}
          <a-tooltip :title="apiParams.name.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-input
          v-decorator="['name', { initialValue: resource.name || '' }]"
          autoFocus />
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.displayname') }}
          <a-tooltip :title="apiParams.displayname.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-input
          v-decorator="['displayname', { initialValue: resource.displayname || '' }]" />
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.ostypeid') }}
          <a-tooltip :title="apiParams.ostypeid.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-select
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="osTypes.loading"
          v-decorator="['ostypeid', { initialValue: resource.ostypeid || '' }]">
          <a-select-option v-for="(ostype) in osTypes.opts" :key="ostype.id">
            {{ ostype.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.isdynamicallyscalable') }}
          <a-tooltip :title="apiParams.isdynamicallyscalable.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-switch
          :default-checked="resource.isdynamicallyscalable"
          v-decorator="['isdynamicallyscalable']"
          :disabled="!canDynamicScalingEnabled()" />
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.haenable') }}
          <a-tooltip :title="apiParams.haenable.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-switch
          :default-checked="resource.haenable"
          v-decorator="['haenable']" />
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.group') }}
          <a-tooltip :title="apiParams.group.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-auto-complete
          v-decorator="['group', { initialValue: resource.group }]"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :dataSource="groups.opts" />
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'EditVM',
  props: {
    action: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      serviceOffering: {},
      template: {},
      dynamicScalingVmConfig: false,
      loading: false,
      osTypes: {
        loading: false,
        opts: []
      },
      groups: {
        loading: false,
        opts: []
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    const apiConfig = this.$store.getters.apis.updateVirtualMachine || {}
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchOsTypes()
      this.fetchInstaceGroups()
      this.fetchServiceOfferingData()
      this.fetchTemplateData()
      this.fetchDynamicScalingVmConfig()
    },
    fetchServiceOfferingData () {
      const params = {}
      params.id = this.resource.serviceofferingid
      params.isrecursive = true
      var apiName = 'listServiceOfferings'
      api(apiName, params).then(json => {
        const offerings = json.listserviceofferingsresponse.serviceoffering
        this.serviceOffering = offerings[0]
      })
    },
    fetchTemplateData () {
      const params = {}
      console.log('templateid ' + this.resource.templateid)
      params.id = this.resource.templateid
      params.isrecursive = true
      params.templatefilter = 'all'
      var apiName = 'listTemplates'
      api(apiName, params).then(json => {
        const templateResponses = json.listtemplatesresponse.template
        this.template = templateResponses[0]
      })
    },
    fetchDynamicScalingVmConfig () {
      const params = {}
      params.name = 'enable.dynamic.scale.vm'
      params.zoneid = this.resource.zoneid
      var apiName = 'listConfigurations'
      api(apiName, params).then(json => {
        const configResponse = json.listconfigurationsresponse.configuration
        this.dynamicScalingVmConfig = configResponse[0]?.value === 'true'
      })
    },
    canDynamicScalingEnabled () {
      return this.template.isdynamicallyscalable && this.serviceOffering.dynamicscalingenabled && this.dynamicScalingVmConfig
    },
    fetchOsTypes () {
      this.osTypes.loading = true
      this.osTypes.opts = []
      api('listOsTypes', { listAll: true }).then(json => {
        this.osTypes.opts = json.listostypesresponse.ostype || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.osTypes.loading = false })
    },
    fetchInstaceGroups () {
      this.groups.loading = true
      this.groups.opts = []
      api('listInstanceGroups', {
        account: this.$store.getters.userInfo.account,
        domainid: this.$store.getters.userInfo.domainid,
        listall: true
      }).then(json => {
        const groups = json.listinstancegroupsresponse.instancegroup || []
        groups.forEach(x => {
          this.groups.opts.push(x.name)
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.groups.loading = false })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) return

        const params = {}
        params.id = this.resource.id
        params.name = values.name
        params.displayname = values.displayname
        params.ostypeid = values.ostypeid
        params.isdynamicallyscalable = values.isdynamicallyscalable || false
        params.haenable = values.haenable || false
        params.group = values.group

        this.loading = true

        api('updateVirtualMachine', params).then(json => {
          this.$message.success({
            content: `${this.$t('label.action.edit.instance')} - ${values.name}`,
            duration: 2
          })
          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => { this.loading = false })
      })
    },
    onCloseAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
