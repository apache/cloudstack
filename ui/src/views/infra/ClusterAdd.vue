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
  <div class="form-layout" v-ctrl-enter="handleSubmitForm">
    <a-form
      :ref="formRef"
      :model="form"
      :loading="loading"
      :rules="rules"
      layout="vertical"
      @submit="handleSubmitForm">
      <a-form-item name="zoneid" ref="zoneid">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          @change="fetchPods"
          v-focus="true"
          :placeholder="apiParams.zoneid.description"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="zone in zonesList"
            :value="zone.id"
            :key="zone.id"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px" />
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="podid" ref="podid">
        <template #label>
          <tooltip-label :title="$t('label.podid')" :tooltip="apiParams.podid.description"/>
        </template>
        <a-select
          v-model:value="form.podid"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="pod in podsList"
            :value="pod.id"
            :key="pod.id"
            :label="pod.name">
            {{ pod.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="clustername" ref="clustername">
        <template #label>
          <tooltip-label :title="$t('label.clustername')" :tooltip="apiParams.clustername.description"/>
        </template>
        <a-input :placeholder="apiParams.clustername.description" v-model:value="form.clustername"></a-input>
      </a-form-item>
      <a-row :gutter="12">
        <a-col :md="24" :lg="form.hypervisor === 'External' ? 12 : 24">
          <a-form-item name="hypervisor" ref="hypervisor">
            <template #label>
              <tooltip-label :title="$t('label.hypervisor')" :tooltip="apiParams.hypervisor.description"/>
            </template>
            <a-select
              v-model:value="form.hypervisor"
              @change="hypervisor => onChangeHypervisor(hypervisor)"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="hv in hypervisorsList"
                :value="hv.name"
                :key="hv.name">
                {{ hv.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :md="24" :lg="12" v-if="form.hypervisor === 'External'">
          <a-form-item name="extensionid" ref="extensionid">
            <template #label>
              <tooltip-label :title="$t('label.extensionid')" :tooltip="apiParams.extensionid.description"/>
            </template>
            <a-select
              v-model:value="form.extensionid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="extension in extensionsList"
                :value="extension.id"
                :key="extension.id"
                :label="extension.name">
                {{ extension.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item name="externaldetails" ref="externaldetails" v-if="form.hypervisor === 'External' && !!form.extensionid">
        <template #label>
          <tooltip-label :title="$t('label.configuration.details')" :tooltip="apiParams.externaldetails.description"/>
        </template>
        <a-switch v-model:checked="externalDetailsEnabled" @change="onExternalDetailsEnabledChange"/>
        <a-card v-if="externalDetailsEnabled" style="margin-top: 10px">
          <div style="margin-bottom: 10px">{{ $t('message.add.extension.resource.details') }}</div>
          <details-input
            v-model:value="form.externaldetails" />
        </a-card>
      </a-form-item>
      <a-form-item name="arch" ref="arch">
        <template #label>
          <tooltip-label :title="$t('label.arch')" :tooltip="apiParams.arch.description"/>
        </template>
        <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          v-model:value="form.arch">
          <a-select-option v-for="opt in architectureTypes.opts" :key="opt.id">
            {{ opt.name || opt.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div v-if="form.hypervisor === 'VMware'">
        <a-form-item name="host" ref="host" :label="$t('label.vcenter.host')">
          <a-input v-model:value="form.host"></a-input>
        </a-form-item>
        <a-form-item name="datacenter" ref="datacenter" :label="$t('label.vcenterdatacenter')">
          <a-input v-model:value="form.datacenter"></a-input>
        </a-form-item>
        <a-form-item name="usedefaultvmwarecred" ref="usedefaultvmwarecred" :label="$t('label.use.existing.vcenter.credentials.from.zone')">
          <a-switch v-model:checked="form.usedefaultvmwarecred" @change="onChangeUseDefaultVMwareCred()" />
        </a-form-item>
        <div v-if="form.usedefaultvmwarecred === false">
          <a-form-item name="username" ref="username" :label="$t('label.vcenterusername')">
            <a-input v-model:value="form.username"></a-input>
          </a-form-item>
          <a-form-item name="password" ref="password" :label="$t('label.vcenterpassword')">
            <a-input type="password" v-model:value="form.password" />
          </a-form-item>
        </div>
      </div>

      <a-form-item name="isdedicated" ref="isdedicated" :label="$t('label.isdedicated')">
        <a-switch v-model:checked="showDedicated" @change="toggleDedicated" />
      </a-form-item>

      <template v-if="showDedicated">
        <DedicateDomain
          @domainChange="id => dedicatedDomainId = id"
          @accountChange="id => dedicatedAccount = id"
          :error="false" />
      </template>

      <a-divider />

      <div :span="24" class="action-button">
        <a-button @click="() => $emit('close-action')">{{ $t('label.cancel') }}</a-button>
        <a-button @click="handleSubmitForm" ref="submit" type="primary">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'
import DetailsInput from '@/components/widgets/DetailsInput'

export default {
  name: 'ClusterAdd',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    DedicateDomain,
    ResourceIcon,
    DetailsInput
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      loading: false,
      clustertype: 'CloudManaged',
      zonesList: [],
      hypervisorsList: [],
      extensionsList: [],
      podsList: [],
      showDedicated: false,
      dedicatedDomainId: null,
      dedicatedAccount: null,
      architectureTypes: {},
      externalDetailsEnabled: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('addCluster')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        usedefaultvmwarecred: false
      })
      this.rules = reactive({
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        podid: [{ required: true, message: this.$t('message.error.select') }],
        clustername: [{ required: true, message: this.$t('message.error.required.input') }],
        hypervisor: [{ required: true, message: this.$t('message.error.select') }],
        host: [{ required: true, message: this.$t('message.error.required.input') }],
        datacenter: [{ required: true, message: this.$t('message.error.required.input') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      this.fetchZones()
      this.fetchHypervisors()
      this.fetchExtensionsList()
      this.architectureTypes.opts = this.$fetchCpuArchitectureTypes()
      this.form.arch = this.architectureTypes?.opts?.[0]?.id || null
    },
    fetchZones () {
      this.loading = true
      getAPI('listZones', { showicon: true }).then(response => {
        this.zonesList = response.listzonesresponse.zone || []
        this.form.zoneid = this.zonesList?.[0]?.id || null
        this.fetchPods()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.form.zoneid = this.zonesList?.[0]?.id || null
        this.loading = false
      })
    },
    fetchHypervisors () {
      this.loading = true
      getAPI('listHypervisors').then(response => {
        this.hypervisorsList = response.listhypervisorsresponse.hypervisor || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.form.hypervisor = this.hypervisorsList?.[0]?.name || null
        this.loading = false
      })
    },
    fetchPods () {
      this.loading = true
      getAPI('listPods', {
        zoneid: this.form.zoneid
      }).then(response => {
        this.podsList = response.listpodsresponse.pod || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.form.podid = this.podsList?.[0]?.id || null
        this.loading = false
      })
    },
    fetchExtensionsList () {
      this.loading = true
      getAPI('listExtensions', {
      }).then(response => {
        this.extensionsList = response.listextensionsresponse.extension || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.extensionsList.unshift({ id: null, name: '' })
        this.loading = false
      })
    },
    fetchVMwareCred () {
      this.loading = true
      this.clustertype = 'ExternalManaged'
      getAPI('listVmwareDcs', {
        zoneid: this.form.zoneid
      }).then(response => {
        var vmwaredcs = response.listvmwaredcsresponse.VMwareDC
        if (vmwaredcs !== null) {
          this.form.host = vmwaredcs[0].vcenter
          this.form.datacenter = vmwaredcs[0].name
        }
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.listvmwaredcsresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.loading = false
      })
    },
    toggleDedicated () {
      this.dedicatedDomainId = null
      this.dedicatedAccount = null
    },
    handleSubmitForm () {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        if (values.hypervisor === 'Ovm3') {
          values.ovm3pool = 'on'
          values.ovm3cluster = 'undefined'
          values.ovm3vip = ''
        }
        this.addCluster(values)
      })
    },
    addCluster (values) {
      var clustername = values.clustername
      var url = ''
      if (values.hypervisor === 'VMware') {
        clustername = `${this.form.host}/${this.form.datacenter}/${clustername}`
        url = `http://${clustername}`
      }
      this.loading = true
      this.parentToggleLoading()
      var data = {
        zoneid: values.zoneid,
        hypervisor: values.hypervisor,
        clustertype: this.clustertype,
        podid: values.podid,
        clustername: clustername,
        arch: values.arch,
        url: url
      }
      if (values.ovm3pool) {
        data.ovm3pool = values.ovm3pool
      }
      if (values.ovm3cluster) {
        data.ovm3cluster = values.ovm3cluster
      }
      if (values.ovm3vip) {
        data.ovm3vip = values.ovm3vip
      }
      if (values.username) {
        data.username = values.username
      }
      if (values.password) {
        data.password = values.password
      }
      if (values.hypervisor === 'External') {
        if (values.extensionid) {
          data.extensionid = values.extensionid
        }
        if (values.externaldetails) {
          Object.entries(values.externaldetails).forEach(([k, v]) => {
            data['externaldetails[0].' + k] = v
          })
        }
      }
      postAPI('addCluster', data).then(response => {
        const cluster = response.addclusterresponse.cluster[0] || {}
        if (cluster.id && this.showDedicated) {
          this.dedicateCluster(cluster.id)
        }
        this.parentFetchData()
        this.parentToggleLoading()
        this.$emit('close-action')
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.addclusterresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.loading = false
      })
    },
    dedicateCluster (clusterId) {
      this.loading = true
      postAPI('dedicateCluster', {
        clusterId,
        domainId: this.dedicatedDomainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicateclusterresponse.jobid,
          title: this.$t('message.cluster.dedicated'),
          description: `${this.$t('label.domainid')} : ${this.dedicatedDomainId}`,
          successMessage: this.$t('message.cluster.dedicated'),
          successMethod: () => {
            this.loading = false
          },
          errorMessage: this.$t('error.dedicate.cluster.failed'),
          errorMethod: () => {
            this.loading = false
          },
          loadingMessage: this.$t('message.dedicate.zone'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loading = false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
        this.loading = false
      })
    },
    onChangeHypervisor (hypervisor) {
      this.clustertype = 'CloudManaged'
      this.form.username = null
      this.form.password = null
      this.form.url = null
      this.form.host = null
      this.form.datacenter = null
      this.form.extensionid = null
      this.form.externaldetails = null
      this.ovm3pool = null
      this.ovm3cluster = null
      this.ovm3vip = null
      if (hypervisor === 'VMware') {
        this.fetchVMwareCred()
      }
    },
    onChangeUseDefaultVMwareCred () {
      if (this.form.usedefaultvmwarecred) {
        this.form.username = null
        this.form.password = null
      }
    },
    onExternalDetailsEnabledChange (val) {
      if (val || !this.form.externaldetails) {
        return
      }
      this.form.externaldetails = undefined
    }
  }
}
</script>

<style scoped lang="scss">
  .form-layout {
    width: 60vw;
    @media (min-width: 600px) {
      width: 550px;
    }
  }

  .required {
    color: #ff0000;

    &-label {
      display: none;

      &--visible {
        display: block;
      }
    }
  }
</style>
