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
      :ref="formRef"
      :model="form"
      :rules="rules"
      v-ctrl-enter="handleSubmit"
      @finish="handleSubmit">
      <a-alert style="margin-bottom: 5px" type="warning" show-icon>
        <template #message>
          <span v-html="$t('message.restart.vm.to.update.settings')" />
        </template>
      </a-alert>
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="displayname" ref="displayname">
        <template #label>
          <tooltip-label :title="$t('label.displayname')" :tooltip="apiParams.displayname.description"/>
        </template>
        <a-input v-model:value="form.displayname" />
      </a-form-item>
      <a-form-item name="ostypeid" ref="ostypeid">
        <template #label>
          <tooltip-label :title="$t('label.ostypeid')" :tooltip="apiParams.ostypeid.description"/>
        </template>
        <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="osTypes.loading"
          v-model:value="form.ostypeid">
          <a-select-option v-for="(ostype) in osTypes.opts" :key="ostype.id" :label="ostype.description">
            {{ ostype.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="isdynamicallyscalable" ref="isdynamicallyscalable">
        <template #label>
          <tooltip-label :title="$t('label.isdynamicallyscalable')" :tooltip="apiParams.isdynamicallyscalable.description"/>
        </template>
        <a-switch v-model:checked="form.isdynamicallyscalable" />
      </a-form-item>
      <a-form-item name="haenable" ref="haenable" v-if="serviceOffering ? serviceOffering.offerha : false">
        <template #label>
          <tooltip-label :title="$t('label.haenable')" :tooltip="apiParams.haenable.description"/>
        </template>
        <a-switch v-model:checked="form.haenable" />
      </a-form-item>
      <a-form-item name="group" ref="group">
        <template #label>
          <tooltip-label :title="$t('label.group')" :tooltip="apiParams.group.description"/>
        </template>
        <a-auto-complete
          v-model:value="form.group"
          :filterOption="(input, option) => {
            return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :options="groups.opts" />
      </a-form-item>
      <a-form-item v-if="userDataEnabled">
        <template #label>
          <tooltip-label :title="$t('label.userdata')" :tooltip="apiParams.userdata.description"/>
        </template>
        <a-textarea v-model:value="form.userdata">
        </a-textarea>
      </a-form-item>
      <a-form-item ref="securitygroupids" name="securitygroupids" :label="$t('label.security.groups')" v-if="securityGroupsEnabled">
        <a-select
          mode="multiple"
          :placeholder="$t('label.select.security.groups')"
          v-model:value="form.securitygroupids"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="securitygroups.loading"
          v-focus="true">
          <a-select-option v-for="securitygroup in securitygroups.opts" :key="securitygroup.id" :label="securitygroup.name ||  securitygroup.id">
            <div>
              {{ securitygroup.name ||  securitygroup.id }}
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="deleteprotection" ref="deleteprotection">
        <template #label>
          <tooltip-label :title="$t('label.deleteprotection')" :tooltip="apiParams.deleteprotection.description"/>
        </template>
        <a-switch v-model:checked="form.deleteprotection" />
      </a-form-item>
      <a-form-item name="showLeaseOptions" ref="showLeaseOptions" v-if="isLeaseEditable">
        <template #label>
          <tooltip-label :title="$t('label.lease.enable')" :tooltip="$t('label.lease.enable.tooltip')" />
        </template>
        <a-switch v-model:checked="showLeaseOptions" @change="onToggleLeaseData"/>
      </a-form-item>
      <a-row :gutter="12" v-if="showLeaseOptions">
        <a-col :md="12" :lg="12">
          <a-form-item name="leaseduration" ref="leaseduration">
            <template #label>
              <tooltip-label :title="$t('label.leaseduration')" />
            </template>
            <a-input
              v-model:value="form.leaseduration"
              :placeholder="$t('label.instance.lease.placeholder')"/>
          </a-form-item>
        </a-col>
        <a-col :md="12" :lg="12">
          <a-form-item name="leaseexpiryaction" ref="leaseexpiryaction">
            <template #label>
              <tooltip-label :title="$t('label.leaseexpiryaction')"  />
            </template>
            <a-select v-model:value="form.leaseexpiryaction" :defaultValue="expiryActions">
              <a-select-option v-for="action in expiryActions" :key="action" :label="action" />
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'EditVM',
  components: {
    TooltipLabel
  },
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
      userDataEnabled: false,
      securityGroupsEnabled: false,
      dynamicScalingVmConfig: false,
      loading: false,
      securitygroups: {
        loading: false,
        opts: []
      },
      osTypes: {
        loading: false,
        opts: []
      },
      groups: {
        loading: false,
        opts: []
      },
      isLeaseEditable: this.$store.getters.features.instanceleaseenabled && this.resource.leaseduration > -1,
      showLeaseOptions: false,
      leaseduration: this.resource.leaseduration === undefined ? 90 : this.resource.leaseduration,
      leaseexpiryaction: this.resource.leaseexpiryaction === undefined ? 'STOP' : this.resource.leaseexpiryaction,
      expiryActions: ['STOP', 'DESTROY'],
      naturalNumberRule: {
        type: 'number',
        validator: this.validateNumber
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateVirtualMachine')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: this.resource.name,
        displayname: this.resource.displayname,
        ostypeid: this.resource.ostypeid,
        isdynamicallyscalable: this.resource.isdynamicallyscalable,
        deleteprotection: this.resource.deleteprotection,
        group: this.resource.group,
        userdata: '',
        haenable: this.resource.haenable,
        leaseduration: this.resource.leaseduration,
        leaseexpiryaction: this.resource.leaseexpiryaction
      })
      this.rules = reactive({
        leaseduration: [this.naturalNumberRule]
      })
      this.showLeaseOptions = this.isLeaseEditable
    },
    fetchData () {
      this.fetchZoneDetails()
      this.fetchSecurityGroups()
      this.fetchOsTypes()
      this.fetchInstaceGroups()
      this.fetchServiceOfferingData()
      this.fetchTemplateData()
      this.fetchDynamicScalingVmConfig()
      this.fetchUserData()
    },
    fetchZoneDetails () {
      api('listZones', {
        id: this.resource.zoneid
      }).then(response => {
        const zone = response?.listzonesresponse?.zone || []
        this.securityGroupsEnabled = zone?.[0]?.securitygroupsenabled || this.$store.getters.showSecurityGroups
      })
    },
    fetchSecurityGroups () {
      this.securitygroups.loading = true
      api('listSecurityGroups', {
        zoneid: this.resource.zoneid
      }).then(json => {
        const items = json.listsecuritygroupsresponse.securitygroup || []
        if (items && items.length > 0) {
          for (let i = 0; i < items.length; i++) {
            this.securitygroups.opts.push(items[i])
          }
          this.securitygroups.opts.sort((a, b) => {
            if (a.name < b.name) return -1
            if (a.name > b.name) return 1
            return 0
          })
        }
      }).finally(() => {
        this.securitygroups.loading = false
      })
    },
    fetchServiceOfferingData () {
      const params = {}
      params.id = this.resource.serviceofferingid
      params.isrecursive = true
      var apiName = 'listServiceOfferings'
      api(apiName, params).then(json => {
        const offerings = json?.listserviceofferingsresponse?.serviceoffering || []
        this.serviceOffering = offerings[0] || {}
      })
    },
    fetchTemplateData () {
      const params = {}
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
      api('listOsTypes').then(json => {
        this.osTypes.opts = json.listostypesresponse.ostype || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.osTypes.loading = false })
    },
    fetchInstaceGroups () {
      this.groups.loading = true
      this.groups.opts = []
      const params = {
        domainid: this.$store.getters.userInfo.domainid,
        listall: true
      }
      if (this.$store.getters.project && this.$store.getters.project.id) {
        params.projectid = this.$store.getters.project.id
      } else {
        params.account = this.$store.getters.userInfo.account
      }
      api('listInstanceGroups', params).then(json => {
        const groups = json.listinstancegroupsresponse.instancegroup || []
        groups.forEach(x => {
          this.groups.opts.push({ id: x.name, value: x.name })
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.groups.loading = false })
    },
    decodeUserData (userdata) {
      const decodedData = Buffer.from(userdata, 'base64')
      return decodedData.toString('utf-8')
    },
    fetchUserData () {
      let networkId
      this.resource.nic.forEach(nic => {
        if (nic.isdefault) {
          networkId = nic.networkid
        }
      })
      if (!networkId) {
        return
      }
      const listNetworkParams = {
        id: networkId,
        listall: true
      }
      api(`listNetworks`, listNetworkParams).then(json => {
        json.listnetworksresponse.network[0].service.forEach(service => {
          if (service.name === 'UserData') {
            this.userDataEnabled = true

            const listVmParams = {
              id: this.resource.id,
              userdata: true,
              listall: true
            }
            api('listVirtualMachines', listVmParams).then(json => {
              this.form.userdata = atob(json.listvirtualmachinesresponse.virtualmachine[0].userdata || '')
            })
          }
        })
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.id = this.resource.id
        params.name = values.name
        params.displayname = values.displayname
        params.ostypeid = values.ostypeid
        if (this.securityGroupsEnabled && Array.isArray(values.securitygroupids) && values.securitygroupids.length > 0) {
          params.securitygroupids = values.securitygroupids
        }
        if (values.isdynamicallyscalable !== undefined) {
          params.isdynamicallyscalable = values.isdynamicallyscalable
        }
        if (values.deleteprotection !== undefined) {
          params.deleteprotection = values.deleteprotection
        }
        if (values.haenable !== undefined) {
          params.haenable = values.haenable
        }
        if (values.group && values.group.length > 0) {
          params.group = values.group
        }
        if (values.userdata && values.userdata.length > 0) {
          params.userdata = this.$toBase64AndURIEncoded(values.userdata)
        }
        if (values.leaseduration !== undefined && (values.leaseduration === -1 || values.leaseduration > 0)) {
          params.leaseduration = values.leaseduration
          if (values.leaseexpiryaction !== undefined) {
            params.leaseexpiryaction = values.leaseexpiryaction
          }
        }
        this.loading = true

        api('updateVirtualMachine', {}, 'POST', params).then(json => {
          this.$message.success({
            content: `${this.$t('label.action.edit.instance')} - ${values.name}`,
            duration: 2
          })
          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => { this.loading = false })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    onCloseAction () {
      this.$emit('close-action')
    },
    onToggleLeaseData () {
      if (this.showLeaseOptions === false) {
        this.form.leaseduration = -1
        this.form.leaseexpiryaction = undefined
      } else {
        this.form.leaseduration = this.leaseduration
        this.form.leaseexpiryaction = this.leaseexpiryaction
      }
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
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
