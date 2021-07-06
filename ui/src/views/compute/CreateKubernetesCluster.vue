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
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.description') }}
            <a-tooltip :title="apiParams.description.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description"
            @change="val => { this.handleZoneChange(this.zones[val]) }">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.kubernetesversionid') }}
            <a-tooltip :title="apiParams.kubernetesversionid.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="version-selection"
            v-model:value="form.kubernetesversionid"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="kubernetesVersionLoading"
            :placeholder="apiParams.kubernetesversionid.description"
            @change="val => { this.handleKubernetesVersionChange(this.kubernetesVersions[val]) }">
            <a-select-option v-for="(opt, optIndex) in this.kubernetesVersions" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.serviceofferingid') }}
            <a-tooltip :title="apiParams.serviceofferingid.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="offering-selection"
            v-model:value="form.serviceofferingid"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt, optIndex) in this.serviceOfferings" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.noderootdisksize') }}
            <a-tooltip :title="apiParams.noderootdisksize.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.noderootdisksize"
            :placeholder="apiParams.noderootdisksize.description"/>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.networkid') }}
            <a-tooltip :title="apiParams.networkid.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="network-selection"
            v-model:value="form.networkid"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="networkLoading"
            :placeholder="apiParams.networkid.description">
            <a-select-option v-for="(opt, optIndex) in this.networks" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="this.selectedKubernetesVersion != null && this.selectedKubernetesVersion != undefined && this.selectedKubernetesVersion.supportsha === true">
          <template #label>
            {{ $t('label.haenable') }}
            <a-tooltip :title="apiParams.haenable.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:cheked="form.haenable" />
        </a-form-item>
        <a-form-item v-if="form.haEnabled">
          <template #label>
            {{ $t('label.masternodes') }}
            <a-tooltip :title="apiParams.masternodes.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.masternodes"
            :placeholder="apiParams.masternodes.description"/>
        </a-form-item>
        <a-form-item v-if="form.haEnabled">
          <template #label>
            {{ $t('label.externalloadbalanceripaddress') }}
            <a-tooltip :title="apiParams.externalloadbalanceripaddress.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.externalloadbalanceripaddress"
            :placeholder="apiParams.externalloadbalanceripaddress.description"/>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.cks.cluster.size') }}
            <a-tooltip :title="apiParams.size.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.keypair') }}
            <a-tooltip :title="apiParams.keypair.description">
              <info-circle style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="keypair-selection"
            v-model:value="form.keypair"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="keyPairLoading"
            :placeholder="apiParams.keypair.description">
            <a-select-option v-for="(opt, optIndex) in this.keyPairs" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="$store.getters.features.kubernetesclusterexperimentalfeaturesenabled">
          <a-form-item :label="$t('label.private.registry')">
            <template #label>
              {{ $t('label.private.registry') }}
              <a-tooltip :title="apiParams.keprivateregistryypair.description">
                <info-circle style="color: rgba(0,0,0,.45)" />
              </a-tooltip>
            </template>
            <a-switch v-model:checked="form.privateregistry" />
          </a-form-item>
          <div v-if="usePrivateRegistry">
            <a-form-item>
              <template #label>
                {{ $t('label.username') }}
                <a-tooltip :title="apiParams.dockerregistryusername.description">
                  <info-circle style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="form.dockerregistryusername"
                :placeholder="apiParams.dockerregistryusername.description"/>
            </a-form-item>
            <a-form-item>
              <template #label>
                {{ $t('label.password') }}
                <a-tooltip :title="apiParams.dockerregistrypassword.description">
                  <info-circle style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </template>
              <a-input-password
                v-model:value="form.dockerregistrypassword"
                :placeholder="apiParams.dockerregistrypassword.description"/>
            </a-form-item>
            <a-form-item>
              <template #label>
                {{ $t('label.url') }}
                <a-tooltip :title="apiParams.dockerregistryurl.description">
                  <info-circle style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="form.dockerregistryurl"
                :placeholder="apiParams.dockerregistryurl.description"/>
            </a-form-item>
            <a-form-item>
              <template #label>
                {{ $t('label.email') }}
                <a-tooltip :title="apiParams.dockerregistryemail.description">
                  <info-circle style="color: rgba(0,0,0,.45)" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="form.dockerregistryemail"
                :placeholder="apiParams.dockerregistryemail.description"/>
            </a-form-item>
          </div>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'CreateKubernetesCluster',
  props: {},
  data () {
    return {
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      kubernetesVersions: [],
      kubernetesVersionLoading: false,
      selectedKubernetesVersion: {},
      serviceOfferings: [],
      serviceOfferingLoading: false,
      networks: [],
      networkLoading: false,
      keyPairs: [],
      keyPairLoading: false,
      usePrivateRegistry: false,
      loading: false
    }
  },
  beforeCreate () {
    this.initForm()
    this.apiConfig = this.$store.getters.apis.createKubernetesCluster || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.networks = [
      {
        id: null,
        name: ''
      }
    ]
    this.keyPairs = [
      {
        id: null,
        name: ''
      }
    ]
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: undefined,
        description: undefined,
        zoneid: undefined,
        kubernetesversionid: undefined,
        serviceofferingid: undefined,
        noderootdisksize: undefined,
        networkid: undefined,
        haenable: false,
        masternodes: 2,
        externalloadbalanceripaddress: undefined,
        size: 1,
        keypair: undefined,
        privateregistry: false,
        dockerregistryusername: undefined,
        dockerregistrypassword: undefined,
        dockerregistryurl: undefined,
        dockerregistryemail: undefined
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.kubecluster.name') }],
        description: [{ required: true, message: this.$t('message.error.cluster.description') }],
        zoneid: [{ required: true, message: this.$t('message.error.zone.for.cluster') }],
        kubernetesversionid: [{ required: true, message: this.$t('message.error.version.for.cluster') }],
        serviceofferingid: [{ required: true, message: this.$t('message.error.serviceoffering.for.cluster') }],
        noderootdisksize: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.this.$t('message.validate.number'))
            }
            return Promise.resolve()
          }
        }],
        masternodes: [
          { required: true, message: this.$t('message.error.input.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value < 2)) {
                return Promise.reject(this.this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ],
        size: [
          { required: true, message: this.$t('message.error.size.for.cluster') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value <= 0)) {
                return Promise.reject(this.this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ],
        dockerregistryusername: [{ required: true, message: this.$t('label.required') }],
        dockerregistrypassword: [{ required: true, message: this.$t('label.required') }],
        dockerregistryurl: [{ required: true, message: this.$t('label.required') }],
        dockerregistryemail: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.fetchZoneData()
      this.fetchNetworkData()
      this.fetchKeyPairData()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    fetchZoneData () {
      const params = {}
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.setFieldsValue({
            zoneid: 0
          })
          this.handleZoneChange(this.zones[0])
        }
      })
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.fetchKubernetesVersionData()
    },
    fetchKubernetesVersionData () {
      this.kubernetesVersions = []
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone)) {
        params.zoneid = this.selectedZone.id
      }
      this.kubernetesVersionLoading = true
      api('listKubernetesSupportedVersions', params).then(json => {
        const versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion
        if (this.arrayHasItems(versionObjs)) {
          for (var i = 0; i < versionObjs.length; i++) {
            if (versionObjs[i].state === 'Enabled' && versionObjs[i].isostate === 'Ready') {
              this.kubernetesVersions.push(versionObjs[i])
            }
          }
        }
      }).finally(() => {
        this.kubernetesVersionLoading = false
        if (this.arrayHasItems(this.kubernetesVersions)) {
          this.form.setFieldsValue({
            kubernetesversionid: 0
          })
          this.handleKubernetesVersionChange(this.kubernetesVersions[0])
        }
      })
    },
    handleKubernetesVersionChange (version) {
      this.selectedKubernetesVersion = version
      this.fetchServiceOfferingData()
    },
    fetchServiceOfferingData () {
      this.serviceOfferings = []
      const params = {}
      this.serviceOfferingLoading = true
      api('listServiceOfferings', params).then(json => {
        var items = json.listserviceofferingsresponse.serviceoffering
        var minCpu = 2
        var minMemory = 2048
        if (!this.isObjectEmpty(this.selectedKubernetesVersion)) {
          minCpu = this.selectedKubernetesVersion.mincpunumber
          minMemory = this.selectedKubernetesVersion.minmemory
        }
        if (items != null) {
          for (var i = 0; i < items.length; i++) {
            if (items[i].iscustomized === false &&
                items[i].cpunumber >= minCpu && items[i].memory >= minMemory) {
              this.serviceOfferings.push(items[i])
            }
          }
        }
      }).finally(() => {
        this.serviceOfferingLoading = false
        if (this.arrayHasItems(this.serviceOfferings)) {
          this.form.setFieldsValue({
            serviceofferingid: 0
          })
        }
      })
    },
    fetchNetworkData () {
      const params = {}
      this.networkLoading = true
      api('listNetworks', params).then(json => {
        const listNetworks = json.listnetworksresponse.network
        if (this.arrayHasItems(listNetworks)) {
          this.networks = this.networks.concat(listNetworks)
        }
      }).finally(() => {
        this.networkLoading = false
        if (this.arrayHasItems(this.networks)) {
          this.form.setFieldsValue({
            networkid: 0
          })
        }
      })
    },
    fetchKeyPairData () {
      const params = {}
      this.keyPairLoading = true
      api('listSSHKeyPairs', params).then(json => {
        const listKeyPairs = json.listsshkeypairsresponse.sshkeypair
        if (this.arrayHasItems(listKeyPairs)) {
          for (var i = 0; i < listKeyPairs.length; i++) {
            this.keyPairs.push({
              id: listKeyPairs[i].name,
              description: listKeyPairs[i].name
            })
          }
        }
      }).finally(() => {
        this.keyPairLoading = false
        if (this.arrayHasItems(this.keyPairs)) {
          this.form.setFieldsValue({
            keypair: 0
          })
        }
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        const params = {
          name: values.name,
          description: values.description,
          zoneid: this.zones[values.zoneid].id,
          kubernetesversionid: this.kubernetesVersions[values.kubernetesversionid].id,
          serviceofferingid: this.serviceOfferings[values.serviceofferingid].id,
          size: values.size
        }
        if (this.isValidValueForKey(values, 'noderootdisksize') && values.noderootdisksize > 0) {
          params.noderootdisksize = values.noderootdisksize
        }
        if (this.isValidValueForKey(values, 'masternodes') && values.masternodes > 0) {
          params.masternodes = values.masternodes
        }
        if (this.isValidValueForKey(values, 'externalloadbalanceripaddress') && values.externalloadbalanceripaddress !== '') {
          params.externalloadbalanceripaddress = values.externalloadbalanceripaddress
        }
        if (this.isValidValueForKey(values, 'networkid') && this.arrayHasItems(this.networks) && this.networks[values.networkid].id != null) {
          params.networkid = this.networks[values.networkid].id
        }
        if (this.isValidValueForKey(values, 'keypair') && this.arrayHasItems(this.keyPairs) && this.keyPairs[values.keypair].id != null) {
          params.keypair = this.keyPairs[values.keypair].id
        }
        if (this.usePrivateRegistry) {
          params.dockerregistryusername = values.dockerregistryusername
          params.dockerregistrypassword = values.dockerregistrypassword
          params.dockerregistryurl = values.dockerregistryurl
          params.dockerregistryemail = values.dockerregistryemail
        }

        api('createKubernetesCluster', params).then(json => {
          const jobId = json.createkubernetesclusterresponse.jobid
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('label.kubernetes.cluster.create'),
            jobid: jobId,
            description: values.name,
            status: 'progress'
          })
          this.$pollJob({
            jobId,
            loadingMessage: `${this.$t('label.kubernetes.cluster.create')} ${values.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: this.$t('message.success.create.kubernetes.cluter') + ' ' + values.name,
            successMethod: result => {
              this.$emit('refresh-data')
            }
          })
          this.closeAction()
          this.$emit('refresh-data')
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
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
