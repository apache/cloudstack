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
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @submit="handleSubmitForm">
          <a-form-item name="zoneid" ref="zoneid">
            <template #label>
              <tooltip-label :title="$t('label.zonenamelabel')" :tooltip="placeholder.zoneid"/>
            </template>
            <a-select
              v-focus="true"
              v-model:value="form.zoneid"
              :placeholder="placeholder.zoneid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="fetchPods">
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
              <tooltip-label :title="$t('label.podname')" :tooltip="placeholder.podid"/>
            </template>
            <a-select
              v-model:value="form.podid"
              :placeholder="placeholder.podid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="fetchClusters">
              <a-select-option
                v-for="pod in podsList"
                :value="pod.id"
                :key="pod.id"
                :label="pod.name">
                {{ pod.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="clusterid" ref="clusterid">
            <template #label>
              <tooltip-label :title="$t('label.clustername')" :tooltip="placeholder.clusterid"/>
            </template>
            <a-select
              v-model:value="form.clusterid"
              :placeholder="placeholder.clusterid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="handleChangeCluster">
              <a-select-option
                v-for="cluster in clustersList"
                :value="cluster.id"
                :key="cluster.id"
                :label="cluster.name">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="hostname" ref="hostname">
            <template #label>
              <tooltip-label
                :title="selectedClusterHyperVisorType === 'VMware' ? $t('label.esx.host') : $t('label.hostnamelabel')"
                :tooltip="placeholder.url"/>
            </template>
            <a-input
              v-model:value="form.hostname"
              :placeholder="placeholder.url"></a-input>
          </a-form-item>
          <a-form-item name="username" ref="username" v-if="selectedClusterHyperVisorType !== 'VMware'">
            <template #label>
              <tooltip-label :title="$t('label.username')" :tooltip="placeholder.username"/>
            </template>
            <a-input
              v-model:value="form.username"
              :placeholder="placeholder.username"></a-input>
          </a-form-item>
          <a-form-item name="authmethod" ref="authmethod" v-if="selectedClusterHyperVisorType !== 'VMware'">
            <template #label>
              <tooltip-label :title="$t('label.authentication.method')" :tooltip="$t('label.authentication.method')"/>
            </template>
            <a-radio-group
              v-model:value="form.authmethod"
              buttonStyle="solid"
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
                <template #message>
                  <span style="display:block;width:300px;word-wrap:break-word;" v-html="$t('message.add.host.sshkey')" />
                </template>
              </a-alert>
            </div>
          </a-form-item>
          <a-form-item name="password" ref="password" v-if="selectedClusterHyperVisorType !== 'VMware' && authMethod === 'password'">
            <template #label>
              <tooltip-label :title="$t('label.password')" :tooltip="placeholder.password"/>
            </template>
            <a-input-password
              v-model:value="form.password"
              :placeholder="placeholder.password" />
          </a-form-item>
          <a-form-item name="agentusername" ref="agentusername" v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <template #label>
              <tooltip-label :title="$t('label.agent.username')" :tooltip="$t('label.agent.username')"/>
            </template>
            <a-input
              v-model:value="form.agentusername"
              :placeholder="$t('label.agent.username')" />
          </a-form-item>
          <a-form-item name="agentpassword" ref="agentpassword" v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <template #label>
              <tooltip-label :title="$t('label.agent.password')" :tooltip="$t('label.agent.password')"/>
            </template>
            <a-input
              v-model:value="form.agentpassword"
              :placeholder="$t('label.agent.password')" />
          </a-form-item>
          <a-form-item name="agentport" ref="agentport" v-if="selectedClusterHyperVisorType === 'Ovm3'">
            <template #label>
              <tooltip-label :title="$t('label.agentport')" :tooltip="$t('label.agentport')"/>
            </template>
            <a-input
              v-model:value="form.agentport"
              :placeholder="$t('label.agentport')" />
          </a-form-item>
          <a-form-item name="baremetalcpucores" ref="baremetalcpucores" v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <template #label>
              <tooltip-label :title="$t('label.baremetalcpucores')" :tooltip="$t('label.baremetalcpucores')"/>
            </template>
            <a-input
              v-model:value="form.baremetalcpucores"
              :placeholder="$t('label.baremetalcpucores')" />
          </a-form-item>
          <a-form-item name="baremetalcpu" ref="baremetalcpu" v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <template #label>
              <tooltip-label :title="$t('label.baremetalcpu')" :tooltip="$t('label.baremetalcpu')"/>
            </template>
            <a-input
              v-model:value="form.baremetalcpu"
              :placeholder="$t('label.baremetalcpu')" />
          </a-form-item>
          <a-form-item name="baremetalmemory" ref="baremetalmemory" v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <template #label>
              <tooltip-label :title="$t('label.baremetalmemory')" :tooltip="$t('label.baremetalmemory')"/>
            </template>
            <a-input
              v-model:value="form.baremetalmemory"
              :placeholder="$t('label.baremetalmemory')" />
          </a-form-item>
          <a-form-item name="baremetalmac" ref="baremetalmac" v-if="selectedClusterHyperVisorType === 'BareMetal'">
            <template #label>
              <tooltip-label :title="$t('label.baremetalmac')" :tooltip="$t('label.baremetalmac')"/>
            </template>
            <a-input
              v-model:value="form.baremetalmac"
              :placeholder="$t('label.baremetalmac')" />
          </a-form-item>
          <a-form-item name="hosttags" ref="hosttags">
            <template #label>
              <tooltip-label :title="$t('label.hosttags')" :tooltip="placeholder.hosttags"/>
            </template>
            <a-select
              mode="tags"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              v-model:value="form.hosttags"
              :placeholder="placeholder.hosttags">
              <a-select-option v-for="tag in hostTagsList" :key="tag.name">{{ tag.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="isdedicated" ref="isdedicated">
            <template #label>
              <tooltip-label :title="$t('label.isdedicated')"/>
            </template>
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
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'HostAdd',
  mixins: [mixinForm],
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
      zonesList: [],
      clustersList: [],
      podsList: [],
      hostTagsList: [],
      url: null,
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
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        authmethod: this.authMethod
      })
      this.rules = reactive({
        zoneid: [{ required: true, message: this.$t('message.error.select') }],
        podid: [{ required: true, message: this.$t('message.error.select') }],
        clusterid: [{ required: true, message: this.$t('message.error.select') }],
        hostname: [{ required: true, message: this.$t('message.error.required.input') }],
        username: [{ required: true, message: this.$t('message.error.required.input') }],
        password: [{ required: true, message: this.$t('message.error.required.input') }],
        baremetalcpucores: [{ required: true, message: this.$t('message.error.required.input') }],
        baremetalcpu: [{ required: true, message: this.$t('message.error.required.input') }],
        baremetalmemory: [{ required: true, message: this.$t('message.error.required.input') }],
        baremetalmac: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
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
        this.form.zoneid = this.zonesList[0].id || null
        this.fetchPods(this.form.zoneid)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchPods (zoneId) {
      this.form.zoneid = zoneId
      this.loading = true
      api('listPods', {
        zoneid: this.form.zoneid
      }).then(response => {
        this.podsList = response.listpodsresponse.pod || []
        this.form.podid = this.podsList[0].id || null
        this.fetchClusters(this.form.podid)
      }).catch(error => {
        this.$notifyError(error)
        this.podsList = []
        this.form.podid = ''
      }).finally(() => {
        this.loading = false
      })
    },
    fetchClusters (podId) {
      this.form.clusterid = null
      this.clustersList = []
      if (!podId) return
      this.podId = podId
      this.loading = true
      api('listClusters', {
        podid: this.form.podid
      }).then(response => {
        this.clustersList = response.listclustersresponse.cluster || []
        this.form.clusterid = this.clustersList[0].id || null
        if (this.form.clusterid) {
          this.handleChangeCluster(this.form.clusterid)
        }
      }).catch(error => {
        this.$notifyError(error)
        this.clustersList = []
        this.form.clusterid = null
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
      this.form.clusterid = value
      this.selectedCluster = this.clustersList.find(i => i.id === this.form.clusterid)
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
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
