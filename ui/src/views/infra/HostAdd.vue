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
    <div class="form">

      <div class="form__item">
        <div class="form__label"><span class="required">* </span>{{ $t('label.zonenamelabel') }}</div>
        <a-select v-model="zoneId" @change="fetchPods" autoFocus>
          <a-select-option
            v-for="zone in zonesList"
            :value="zone.id"
            :key="zone.id">
            {{ zone.name }}
          </a-select-option>
        </a-select>
      </div>

      <div class="form__item">
        <div class="form__label"><span class="required">* </span>{{ $t('label.podname') }}</div>
        <a-select v-model="podId" @change="fetchClusters">
          <a-select-option
            v-for="pod in podsList"
            :value="pod.id"
            :key="pod.id">
            {{ pod.name }}
          </a-select-option>
        </a-select>
      </div>

      <div class="form__item">
        <div class="form__label"><span class="required">* </span>{{ $t('label.clustername') }}</div>
        <a-select v-model="clusterId" @change="handleChangeCluster">
          <a-select-option
            v-for="cluster in clustersList"
            :value="cluster.id"
            :key="cluster.id">
            {{ cluster.name }}
          </a-select-option>
        </a-select>
      </div>

      <div class="form__item required-field">
        <div class="form__label"><span class="required">* </span>{{ selectedClusterHyperVisorType === 'VMware' ? $t('label.esx.host') : $t('label.hostnamelabel') }}</div>
        <span class="required required-label">{{ $t('label.required') }}</span>
        <a-input v-model="hostname"></a-input>
      </div>

      <div class="form__item required-field" v-if="selectedClusterHyperVisorType !== 'VMware'">
        <div class="form__label"><span class="required">* </span>{{ $t('label.username') }}</div>
        <span class="required required-label">{{ $t('label.required') }}</span>
        <a-input :placeholder="placeholder.username" v-model="username"></a-input>
      </div>

      <div class="form__item required-field" v-if="selectedClusterHyperVisorType !== 'VMware'">
        <div class="form__label"><span class="required">* </span>{{ $t('label.password') }}</div>
        <span class="required required-label">{{ $t('label.required') }}</span>
        <a-input :placeholder="placeholder.password" type="password" v-model="password"></a-input>
      </div>

      <template v-if="selectedClusterHyperVisorType === 'Ovm3'">
        <div class="form__item">
          <div class="form__label">{{ $t('label.agent.username') }}</div>
          <a-input v-model="agentusername"></a-input>
        </div>
        <div class="form__item required-field">
          <div class="form__label"><span class="required">* </span>{{ $t('label.agent.password') }}</div>
          <span class="required required-label">{{ $t('label.required') }}</span>
          <a-input type="password" v-model="agentpassword"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.agentport') }}</div>
          <a-input v-model="agentport"></a-input>
        </div>
      </template>

      <div class="form__item">
        <div class="form__label">{{ $t('label.hosttags') }}</div>
        <a-select
          mode="tags"
          :placeholder="placeholder.hosttags"
          v-model="selectedTags"
        >
          <a-select-option v-for="tag in hostTagsList" :key="tag.name">{{ tag.name }}</a-select-option>
        </a-select>
      </div>

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

      <a-divider></a-divider>

      <div class="actions">
        <a-button @click="() => this.$parent.$parent.close()">{{ $t('label.cancel') }}</a-button>
        <a-button @click="handleSubmitForm" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import DedicateDomain from '../../components/view/DedicateDomain'

export default {
  name: 'HostAdd',
  components: {
    DedicateDomain
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
      podId: null,
      clusterId: null,
      hostname: null,
      username: null,
      password: null,
      selectedTags: [],
      zonesList: [],
      clustersList: [],
      podsList: [],
      hostTagsList: [],
      url: null,
      agentusername: null,
      agentpassword: null,
      agentport: null,
      selectedCluster: null,
      selectedClusterHyperVisorType: null,
      showDedicated: false,
      dedicatedDomainId: null,
      dedicatedAccount: null,
      domainError: false,
      params: [],
      placeholder: {
        username: null,
        password: null,
        hosttags: null
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZones()
      this.fetchHostTags()
      this.params = this.$store.getters.apis.addHost.params
      Object.keys(this.placeholder).forEach(item => { this.returnPlaceholder(item) })
    },
    fetchZones () {
      this.loading = true
      api('listZones').then(response => {
        this.zonesList = response.listzonesresponse.zone || []
        this.zoneId = this.zonesList[0].id || null
        this.fetchPods()
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
        this.fetchClusters()
      }).catch(error => {
        this.$notifyError(error)
        this.podsList = []
        this.podId = ''
      }).finally(() => {
        this.loading = false
      })
    },
    fetchClusters () {
      this.loading = true
      api('listClusters', {
        podid: this.podId
      }).then(response => {
        this.clustersList = response.listclustersresponse.cluster || []
        this.clusterId = this.clustersList[0].id || null
        if (this.clusterId) {
          this.handleChangeCluster()
        }
      }).catch(error => {
        this.$notifyError(error)
        this.clustersList = []
        this.clusterId = null
      }).finally(() => {
        this.loading = false
      })
    },
    fetchHostTags () {
      this.loading = true
      api('listHostTags').then(response => {
        this.hostTagsList = response.listhosttagsresponse.hosttag || []
      }).catch(error => {
        this.$notifyError(error)
        this.hostTagsList = []
      }).finally(() => {
        this.loading = false
      })
    },
    handleChangeCluster () {
      this.selectedCluster = this.clustersList.find(i => i.id === this.clusterId)
      this.selectedClusterHyperVisorType = this.selectedCluster.hypervisortype
    },
    toggleDedicated () {
      this.dedicatedDomainId = null
      this.dedicatedAccount = null
      this.showDedicated = !this.showDedicated
    },
    handleSubmitForm () {
      const requiredFields = document.querySelectorAll('.required-field')

      requiredFields.forEach(field => {
        const input = field.querySelector('.ant-input')
        if (!input.value) {
          input.parentNode.querySelector('.required-label').classList.add('required-label--error')
        } else {
          input.parentNode.querySelector('.required-label').classList.remove('required-label--error')
        }
      })

      if (this.$el.querySelectorAll('.required-label--error').length > 0) return

      if (this.selectedClusterHyperVisorType === 'VMware') {
        this.username = ''
        this.password = ''
      }

      if (this.hostname.indexOf('http://') === -1) {
        this.url = `http://${this.hostname}`
      } else {
        this.url = this.hostname
      }

      const args = {
        zoneid: this.zoneId,
        podid: this.podId,
        clusterid: this.clusterId,
        hypervisor: this.selectedClusterHyperVisorType,
        clustertype: this.selectedCluster.clustertype,
        hosttags: this.selectedTags.join(),
        username: this.username,
        password: this.password,
        url: this.url,
        agentusername: this.agentusername,
        agentpassword: this.agentpassword,
        agentport: this.agentport
      }
      Object.keys(args).forEach((key) => (args[key] == null) && delete args[key])

      this.loading = true
      api('addHost', {}, 'POST', args).then(response => {
        const host = response.addhostresponse.host[0] || {}
        if (host.id && this.showDedicated) {
          this.dedicateHost(host.id)
        }
        this.parentFetchData()
        this.$parent.$parent.close()
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.addhostresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.loading = false
      })
    },
    dedicateHost (hostId) {
      this.loading = true
      api('dedicateHost', {
        hostId,
        domainId: this.dedicatedDomainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicatehostresponse.jobid,
          successMessage: this.$t('message.host.dedicated'),
          successMethod: () => {
            this.loading = false
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('message.host.dedicated'),
              jobid: response.dedicatehostresponse.jobid,
              description: `${this.$t('label.domainid')} : ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: this.$t('error.dedicate.host.failed'),
          errorMethod: () => {
            this.loading = false
          },
          loadingMessage: this.$t('message.dedicating.host'),
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
  .actions {
    display: flex;
    justify-content: flex-end;
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  .required {
    color: #ff0000;
    &-label {
      display: none;
      &--error {
        display: block;
      }
    }
  }
</style>
