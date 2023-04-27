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
    <div class="form" v-ctrl-enter="handleSubmitForm">
      <div class="form__item">
        <div class="form__label"><span class="required">* </span>{{ $t('label.zonenamelabel') }}</div>
        <a-select
          v-model:value="zoneId"
          @change="fetchPods"
          v-focus="true"
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
      </div>

      <div class="form__item">
        <div class="form__label">{{ $t('label.hypervisor') }}</div>
        <a-select
          v-model:value="hypervisor"
          @change="resetAllFields"
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
      </div>

      <div class="form__item">
        <div class="form__label">{{ $t('label.podname') }}</div>
        <a-select
          v-model:value="podId"
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
      </div>

      <div class="form__item">
        <div class="form__label"><span class="required">* </span>{{ $t('label.clusternamelabel') }}</div>
        <span class="required required-label" ref="requiredCluster">{{ $t('label.required') }}</span>
        <a-input :placeholder="placeholder.clustername" v-model:value="clustername"></a-input>
      </div>

      <template v-if="hypervisor === 'VMware'">
        <div class="form__item">
          <div class="form__label">{{ $t('label.vcenter.host') }}</div>
          <a-input v-model:value="host"></a-input>
        </div>

        <div class="form__item">
          <div class="form__label">{{ $t('label.vcenterusername') }}</div>
          <a-input v-model:value="username"></a-input>
        </div>

        <div class="form__item">
          <div class="form__label">{{ $t('label.vcenterpassword') }}</div>
          <a-input type="password" v-model:value="password"></a-input>
        </div>

        <div class="form__item">
          <div class="form__label">{{ $t('label.vcenterdatacenter') }}</div>
          <a-input v-model:value="dataCenter"></a-input>
        </div>
      </template>

      <div class="form__item">
        <div class="form__label">{{ $t('label.isdedicated') }}</div>
        <a-checkbox @change="toggleDedicated" />
      </div>

      <template v-if="showDedicated">
        <DedicateDomain
          @domainChange="id => dedicatedDomainId = id"
          @accountChange="id => dedicatedAccount = id"
          :error="domainError" />
      </template>

      <a-divider />

      <div :span="24" class="action-button">
        <a-button @click="() => $emit('close-action')">{{ $t('label.cancel') }}</a-button>
        <a-button @click="handleSubmitForm" ref="submit" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ClusterAdd',
  components: {
    DedicateDomain,
    ResourceIcon
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
      zoneId: null,
      hypervisor: null,
      podId: null,
      clustername: null,
      clustertype: 'CloudManaged',
      username: null,
      password: null,
      url: null,
      host: null,
      dataCenter: null,
      ovm3pool: null,
      ovm3cluster: null,
      ovm3vip: null,
      zonesList: [],
      hypervisorsList: [],
      podsList: [],
      showDedicated: false,
      dedicatedDomainId: null,
      dedicatedAccount: null,
      domainError: false,
      params: [],
      placeholder: {
        clustername: null
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZones()
      this.fetchHypervisors()
      this.params = this.$store.getters.apis.addCluster.params
      Object.keys(this.placeholder).forEach(item => { this.returnPlaceholder(item) })
    },
    fetchZones () {
      this.loading = true
      api('listZones', { showicon: true }).then(response => {
        this.zonesList = response.listzonesresponse.zone || []
        this.zoneId = this.zonesList[0].id || null
        this.fetchPods()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchHypervisors () {
      this.loading = true
      api('listHypervisors').then(response => {
        this.hypervisorsList = response.listhypervisorsresponse.hypervisor || []
        this.hypervisor = this.hypervisorsList[0].name || null
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchPods () {
      this.loading = true
      api('listPods', {
        zoneid: this.zoneId
      }).then(response => {
        this.podsList = response.listpodsresponse.pod || []
        this.podId = this.podsList[0].id || null
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    toggleDedicated () {
      this.dedicatedDomainId = null
      this.dedicatedAccount = null
      this.showDedicated = !this.showDedicated
    },
    handleSubmitForm () {
      if (this.loading) return
      if (!this.clustername) {
        this.$refs.requiredCluster.classList.add('required-label--visible')
        return
      }
      this.$refs.requiredCluster.classList.remove('required-label--visible')

      if (this.hypervisor === 'Ovm3') {
        this.ovm3pool = 'on'
        this.ovm3cluster = 'undefined'
        this.ovm3vip = ''
      }

      if (this.hypervisor === 'VMware') {
        this.clustertype = 'ExternalManaged'
        if ((this.host === null || this.host.length === 0) &&
          (this.dataCenter === null || this.dataCenter.length === 0)) {
          api('listVmwareDcs', {
            zoneid: this.zoneId
          }).then(response => {
            var vmwaredcs = response.listvmwaredcsresponse.VMwareDC
            if (vmwaredcs !== null) {
              this.host = vmwaredcs[0].vcenter
              this.dataCenter = vmwaredcs[0].name
            }
            this.addCluster()
          }).catch(error => {
            this.$notification.error({
              message: `${this.$t('label.error')} ${error.response.status}`,
              description: error.response.data.listvmwaredcsresponse.errortext,
              duration: 0
            })
          })
          return
        }
      }
      this.addCluster()
    },
    addCluster () {
      let clustername = this.clustername

      if (this.hypervisor === 'VMware') {
        const clusternameVal = this.clustername
        this.url = `http://${this.host}/${this.dataCenter}/${clusternameVal}`
        clustername = `${this.host}/${this.dataCenter}/${clusternameVal}`
      }
      this.loading = true
      this.parentToggleLoading()
      var data = {
        zoneId: this.zoneId,
        hypervisor: this.hypervisor,
        clustertype: this.clustertype,
        podId: this.podId,
        clustername: clustername,
        url: this.url
      }
      if (this.ovm3pool) {
        data.ovm3pool = this.ovm3pool
      }
      if (this.ovm3cluster) {
        data.ovm3cluster = this.ovm3cluster
      }
      if (this.ovm3vip) {
        data.ovm3vip = this.ovm3vip
      }
      if (this.username) {
        data.username = this.username
      }
      if (this.password) {
        data.password = this.password
      }
      api('addCluster', {}, 'POST', data).then(response => {
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
      api('dedicateCluster', {
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
    resetAllFields () {
      this.clustertype = 'CloudManaged'
      this.username = null
      this.password = null
      this.url = null
      this.host = null
      this.dataCenter = null
      this.ovm3pool = null
      this.ovm3cluster = null
      this.ovm3vip = null
    },
    returnPlaceholder (field) {
      this.params.find(i => {
        if (i.name === field) this.placeholder[field] = i.description
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .form {

    &__label {
      margin-bottom: 5px;
    }

    &__item {
      margin-bottom: 20px;
    }

    .ant-select {
      width: 85vw;

      @media (min-width: 760px) {
        width: 400px;
      }
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
