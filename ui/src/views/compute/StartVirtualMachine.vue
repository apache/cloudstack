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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-alert type="warning">
        <span slot="message" v-html="$t('message.action.start.instance')" />
      </a-alert>
      <br />
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <div v-if="this.$store.getters.userInfo.roletype === 'Admin'">
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.podid')" :tooltip="apiParams.podid.description"/>
            <a-select
              v-decorator="['podid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="podsLoading"
              :placeholder="apiParams.podid.description"
              @change="handlePodChange"
              :autoFocus="this.$store.getters.userInfo.roletype === 'Admin'">
              <a-select-option v-for="pod in this.pods" :key="pod.id">
                {{ pod.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.clusterid')" :tooltip="apiParams.clusterid.description"/>
            <a-select
              id="cluster-selection"
              v-decorator="['clusterid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="clustersLoading"
              :placeholder="apiParams.clusterid.description"
              @change="handleClusterChange">
              <a-select-option v-for="cluster in this.clusters" :key="cluster.id">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.hostid')" :tooltip="apiParams.hostid.description"/>
            <a-select
              id="host-selection"
              v-decorator="['hostid', {}]"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="hostsLoading"
              :placeholder="apiParams.hostid.description">
              <a-select-option v-for="host in this.hosts" :key="host.id">
                {{ host.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <a-form-item v-if="resource.hypervisor === 'VMware'">
          <tooltip-label slot="label" :title="$t('label.bootintosetup')" :tooltip="apiParams.bootintosetup.description"/>
          <a-switch
            v-decorator="['bootintosetup']">
          </a-switch>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import eventBus from '@/config/eventBus'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'StartVirtualMachine',
  components: {
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
      pods: [],
      clusters: [],
      hosts: [],
      podsLoading: false,
      clustersLoading: false,
      hostsLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('startVirtualMachine')
  },
  created () {
    if (this.$store.getters.userInfo.roletype === 'Admin') {
      this.fetchPods()
      this.fetchClusters()
      this.fetchHosts()
    }
  },
  methods: {
    fetchPods () {
      this.pods = []
      this.podsLoading = true
      const params = { zoneid: this.resource.zoneid }
      api('listPods', params).then(json => {
        this.pods = json.listpodsresponse.pod || []
        if (this.pods.length === 0) {
          this.$notification.error({
            message: 'No pods found',
            duration: 0
          })
        }
      }).finally(() => {
        this.podsLoading = false
      })
    },
    fetchClusters (podid) {
      this.clusters = []
      this.clustersLoading = true
      const params = { zoneid: this.resource.zoneid }
      if (podid) {
        params.podid = podid
      }
      api('listClusters', params).then(json => {
        this.clusters = json.listclustersresponse.cluster || []
        if (this.clusters.length === 0) {
          this.$notification.error({
            message: 'No clusters found',
            duration: 0
          })
        }
      }).finally(() => {
        this.clustersLoading = false
      })
    },
    fetchHosts (podid, clusterid) {
      this.hosts = []
      this.hostsLoading = true
      const params = { zoneid: this.resource.zoneid, type: 'Routing', state: 'Up' }
      if (podid) {
        params.podid = podid
      }
      if (clusterid) {
        params.clusterid = clusterid
      }
      api('listHosts', params).then(json => {
        this.hosts = json.listhostsresponse.host || []
        if (this.hosts.length === 0) {
          this.$notification.error({
            message: 'No hosts found',
            duration: 0
          })
        }
      }).finally(() => {
        this.hostsLoading = false
      })
    },
    handlePodChange (podid) {
      this.form.clearField('clusterid')
      this.form.clearField('hostid')
      this.fetchClusters(podid)
      this.fetchHosts(podid)
    },
    handleClusterChange (clusterid) {
      this.form.clearField('hostid')
      this.fetchHosts('', clusterid)
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

        this.loading = true
        const params = {
          id: this.resource.id
        }
        for (const key in values) {
          if (values[key]) {
            params[key] = values[key]
          }
        }
        api('startVirtualMachine', params).then(json => {
          const jobId = json.startvirtualmachineresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.action.start.instance'),
            description: this.resource.name,
            loadingMessage: `${this.$t('label.action.start.instance')} ${this.resource.name}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('label.action.start.instance')} ${this.resource.name}`,
            response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `The password of VM <b>${result.virtualmachine.displayname}</b> is <b>${result.virtualmachine.password}</b>` : null }
          })
          const resourceId = this.resource.id
          eventBus.$emit('update-job-details', jobId, resourceId)
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 60vw;

    @media (min-width: 500px) {
      width: 450px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
