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
    <div class="form-layout" v-ctrl-enter="handleSubmitForm">
      <div class="form">
        <a-form
          :form="form"
          layout="vertical"
          @submit="handleSubmitForm">
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.zonenamelabel')" :tooltip="placeholder.zoneid"/>
            <a-select
              v-decorator="['zoneid', {
                initialValue: this.zoneId,
                rules: [{ required: true, message: $t('message.error.select') }]
              }]"
              :placeholder="placeholder.zoneid"
              autoFocus
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="fetchPods">
              <a-select-option
                v-for="zone in zonesList"
                :value="zone.id"
                :key="zone.id"
                :label="zone.name">
                <span>
                  <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <a-icon v-else type="global" style="margin-right: 5px" />
                  {{ zone.name }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.podname')" :tooltip="placeholder.podid"/>
            <a-select
              v-decorator="['podid', {
                initialValue: podId,
                rules: [{ required: true, message: $t('message.error.select') }]
              }]"
              :placeholder="placeholder.podid"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="fetchClusters">
              <a-select-option
                v-for="pod in podsList"
                :value="pod.id"
                :key="pod.id">
                {{ pod.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.clustername')" :tooltip="placeholder.clusterid"/>
            <a-select
              v-decorator="['clusterid', {
                initialValue: clusterId,
                rules: [{ required: true, message: $t('message.error.select') }]
              }]"
              :placeholder="placeholder.clusterid"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="handleChangeCluster">
              <a-select-option
                v-for="cluster in clustersList"
                :value="cluster.id"
                :key="cluster.id">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label
              slot="label"
              :title="selectedClusterHyperVisorType === 'VMware' ? $t('label.esx.host') : $t('label.hostnamelabel')"
              :tooltip="placeholder.url"/>
            <a-input
              v-decorator="['hostname', {
                initialValue: hostname,
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="placeholder.url"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType !== 'VMware'">
            <tooltip-label slot="label" :title="$t('label.username')" :tooltip="placeholder.username"/>
            <a-input
              v-decorator="['username', {
                initialValue: username,
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="placeholder.username"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType !== 'VMware'">
            <tooltip-label slot="label" :title="$t('label.authentication.method')" :tooltip="$t('label.authentication.method')"/>
            <a-radio-group
              v-decorator="['authmethod', {
                initialValue: authMethod
              }]"
              buttonStyle="solid"
              :defaultValue="authMethod"
              @change="selected => { handleAuthMethodChange(selected.target.value) }">
              <a-radio-button value="password">
                {{ $t('label.password') }}
              </a-radio-button>
              <a-radio-button value="sshkey" v-if="selectedClusterHyperVisorType === 'KVM'">
                {{ $t('label.authentication.sshkey') }}
              </a-radio-button>
            </a-radio-group>
            <div v-if="authMethod === 'sshkey'">
              <a-alert type="warning">
                <span style="display:block;width:300px;word-wrap:break-word;" slot="message" v-html="$t('message.add.host.sshkey')" />
              </a-alert>
            </div>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType !== 'VMware' && authMethod === 'password'">
            <tooltip-label slot="label" :title="$t('label.password')" :tooltip="placeholder.password"/>
            <a-input-password
              v-decorator="['password', {
                initialValue: password,
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="placeholder.password"></a-input-password>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <tooltip-label slot="label" :title="$t('label.agent.username')" :tooltip="$t('label.agent.username')"/>
            <a-input
              v-decorator="['agentusername', { initialValue: agentusername }]"
              :placeholder="$t('label.agent.username')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <tooltip-label slot="label" :title="$t('label.agent.password')" :tooltip="$t('label.agent.password')"/>
            <a-input
              v-decorator="['agentpassword', { initialValue: agentpassword }]"
              :placeholder="$t('label.agent.password')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <tooltip-label slot="label" :title="$t('label.agentport')" :tooltip="$t('label.agentport')"/>
            <a-input
              v-decorator="['agentport', { initialValue: agentport }]"
              :placeholder="$t('label.agentport')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <tooltip-label slot="label" :title="$t('label.baremetalcpucores')" :tooltip="$t('label.baremetalcpucores')"/>
            <a-input
              v-decorator="['baremetalcpucores', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="$t('label.baremetalcpucores')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <tooltip-label slot="label" :title="$t('label.baremetalcpu')" :tooltip="$t('label.baremetalcpu')"/>
            <a-input
              v-decorator="['baremetalcpu', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="$t('label.baremetalcpu')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <tooltip-label slot="label" :title="$t('label.baremetalmemory')" :tooltip="$t('label.baremetalmemory')"/>
            <a-input
              v-decorator="['baremetalmemory', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="$t('label.baremetalmemory')"></a-input>
          </a-form-item>
          <a-form-item v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <tooltip-label slot="label" :title="$t('label.baremetalmac')" :tooltip="$t('label.baremetalmac')"/>
            <a-input
              v-decorator="['baremetalmac', {
                rules: [{ required: true, message: $t('message.error.required.input') }]
              }]"
              :placeholder="$t('label.baremetalmac')"></a-input>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.hosttags')" :tooltip="placeholder.hosttags"/>
            <a-select
              mode="tags"
              :placeholder="placeholder.hosttags"
              showSearch
              optionFilterProp="children"
              :filterOption="(input, option) => {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              v-decorator="['hosttags', {
                rules: hostTagRules
              }]">
              <a-select-option v-for="tag in hostTagsList" :key="tag.name">{{ tag.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <tooltip-label slot="label" :title="$t('label.isdedicated')"/>
            <a-checkbox @change="toggleDedicated"></a-checkbox>
          </a-form-item>
          <template v-if="showDedicated">
            <DedicateDomain
              @domainChange="id => dedicatedDomainId = id"
              @accountChange="id => dedicatedAccount = id"
              :error="domainError" />
          </template>

          <a-divider></a-divider>

          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button @click="handleSubmitForm" ref="submit" type="primary">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'HostAdd',
  components: {
    DedicateDomain,
    ResourceIcon,
    TooltipLabel
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
      authMethod: 'password',
      selectedCluster: null,
      selectedClusterHyperVisorType: null,
      showDedicated: false,
      dedicatedDomainId: null,
      dedicatedAccount: null,
      domainError: false,
      params: [],
      placeholder: {
        zoneid: null,
        podid: null,
        clusterid: null,
        url: null,
        username: null,
        password: null,
        hosttags: null,
        isdedicated: null
      }
    }
  },
  computed: {
    hostTagRules () {
      let rules = []
      if (this.selectedClusterHyperVisorType === 'BareMetal') {
        rules = [{ required: true, message: this.$t('message.error.select') }]
      }

      return rules
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
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
          this.handleChangeCluster(this.clusterId)
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
        const listTagExists = []
        const hostTagsList = response.listhosttagsresponse.hosttag || []
        hostTagsList.forEach(tag => {
          if (listTagExists.includes(tag.name)) {
            return true
          }

          listTagExists.push(tag.name)
          this.hostTagsList.push(tag)
        })
      }).catch(error => {
        this.$notifyError(error)
        this.hostTagsList = []
      }).finally(() => {
        this.loading = false
      })
    },
    handleChangeCluster (value) {
      this.clusterId = value
      this.selectedCluster = this.clustersList.find(i => i.id === this.clusterId)
      this.selectedClusterHyperVisorType = this.selectedCluster.hypervisortype
    },
    toggleDedicated () {
      this.dedicatedDomainId = null
      this.dedicatedAccount = null
      this.showDedicated = !this.showDedicated
    },
    handleAuthMethodChange (val) {
      this.authMethod = val
    },
    handleSubmitForm () {
      if (this.loading) return
      this.form.validateFieldsAndScroll((err, values) => {
        if (err) return

        if (values.hostname.indexOf('http://') === -1) {
          this.url = `http://${values.hostname}`
        } else {
          this.url = values.hostname
        }

        const args = {
          zoneid: values.zoneid,
          podid: values.podid,
          clusterid: values.clusterid,
          hypervisor: this.selectedClusterHyperVisorType,
          clustertype: this.selectedCluster.clustertype,
          hosttags: values.hosttags ? values.hosttags.join() : null,
          username: values.username,
          password: this.authMethod !== 'password' ? '' : values.password,
          url: this.url,
          agentusername: values.agentusername,
          agentpassword: values.agentpassword,
          agentport: values.agentport
        }
        if (this.selectedClusterHyperVisorType === 'BareMetal') {
          args.cpunumber = values.baremetalcpucores
          args.cpuspeed = values.baremetalcpu
          args.memory = values.baremetalmemory
          args.hostmac = values.baremetalmac
        }
        Object.keys(args).forEach((key) => (args[key] == null) && delete args[key])
        this.loading = true
        api('addHost', {}, 'POST', args).then(response => {
          const host = response.addhostresponse.host[0] || {}
          if (host.id && this.showDedicated) {
            this.dedicateHost(host.id)
          }
          this.parentFetchData()
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.addhostresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
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
          title: this.$t('message.host.dedicated'),
          description: `${this.$t('label.domainid')} : ${this.dedicatedDomainId}`,
          successMessage: this.$t('message.host.dedicated'),
          successMethod: () => {
            this.loading = false
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
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="scss">
  .form {
    &__label {
      margin-bottom: 5px;

      .required {
        margin-left: 10px;
      }
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
      &--error {
        display: block;
      }
    }
  }
</style>
