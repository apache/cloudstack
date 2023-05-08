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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description"
            @change="val => { handleZoneChange(this.zones[val]) }">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="kubernetesversionid" ref="kubernetesversionid">
          <template #label>
            <tooltip-label :title="$t('label.kubernetesversionid')" :tooltip="apiParams.kubernetesversionid.description"/>
          </template>
          <a-select
            id="version-selection"
            v-model:value="form.kubernetesversionid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="kubernetesVersionLoading"
            :placeholder="apiParams.kubernetesversionid.description"
            @change="val => { handleKubernetesVersionChange(kubernetesVersions[val]) }">
            <a-select-option v-for="(opt, optIndex) in kubernetesVersions" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="serviceofferingid" ref="serviceofferingid">
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
          </template>
          <a-select
            id="offering-selection"
            v-model:value="form.serviceofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="noderootdisksize" ref="noderootdisksize">
          <template #label>
            <tooltip-label :title="$t('label.noderootdisksize')" :tooltip="apiParams.noderootdisksize.description"/>
          </template>
          <a-input
            v-model:value="form.noderootdisksize"
            :placeholder="apiParams.noderootdisksize.description"/>
        </a-form-item>
        <a-form-item name="networkid" ref="networkid">
          <template #label>
            <tooltip-label :title="$t('label.networkid')" :tooltip="apiParams.networkid.description"/>
          </template>
          <a-select
            id="network-selection"
            v-model:value="form.networkid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="networkLoading"
            :placeholder="apiParams.networkid.description">
            <a-select-option v-for="(opt, optIndex) in networks" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="haenable" ref="haenable" v-if="selectedKubernetesVersion != null && selectedKubernetesVersion !== undefined && selectedKubernetesVersion.supportsha === true">
          <template #label>
            <tooltip-label :title="$t('label.haenable')" :tooltip="apiParams.haenable?.description || ''"/>
          </template>
          <a-switch v-model:checked="form.haenable" />
        </a-form-item>
        <a-form-item v-if="form.haenable" name="controlnodes" ref="controlnodes">
          <template #label>
            <tooltip-label :title="$t('label.controlnodes')" :tooltip="apiParams.controlnodes.description"/>
          </template>
          <a-input
            v-model:value="form.controlnodes"
            :placeholder="apiParams.controlnodes.description"/>
        </a-form-item>
        <a-form-item v-if="form.haenable" name="externalloadbalanceripaddress" ref="externalloadbalanceripaddress">
          <template #label>
            <tooltip-label :title="$t('label.externalloadbalanceripaddress')" :tooltip="apiParams.externalloadbalanceripaddress.description"/>
          </template>
          <a-input
            v-model:value="form.externalloadbalanceripaddress"
            :placeholder="apiParams.externalloadbalanceripaddress.description"/>
        </a-form-item>
        <a-form-item name="size" ref="size">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.size')" :tooltip="apiParams.size.description"/>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
        <a-form-item name="keypair" ref="keypair">
          <template #label>
            <tooltip-label :title="$t('label.keypair')" :tooltip="apiParams.keypair.description"/>
          </template>
          <a-select
            id="keypair-selection"
            v-model:value="form.keypair"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="keyPairLoading"
            :placeholder="apiParams.keypair.description">
            <a-select-option v-for="(opt, optIndex) in keyPairs" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="$store.getters.features.kubernetesclusterexperimentalfeaturesenabled">
          <a-form-item name="privateregistry" ref="privateregistry" :label="$t('label.private.registry')">
            <template #label>
              <tooltip-label :title="$t('label.private.registry')" :tooltip="apiParams.keprivateregistryypair.description"/>
            </template>
            <a-switch v-model:checked="form.privateregistry" />
          </a-form-item>
          <div v-if="form.privateregistry">
            <a-form-item name="dockerregistryusername" ref="dockerregistryusername">
              <template #label>
                <tooltip-label :title="$t('label.username')" :tooltip="apiParams.dockerregistryusername.description"/>
              </template>
              <a-input
                v-model:value="form.dockerregistryusername"
                :placeholder="apiParams.dockerregistryusername.description"/>
            </a-form-item>
            <a-form-item name="dockerregistrypassword" ref="dockerregistrypassword">
              <template #label>
                <tooltip-label :title="$t('label.password')" :tooltip="apiParams.dockerregistrypassword.description"/>
              </template>
              <a-input-password
                v-model:value="form.dockerregistrypassword"
                :placeholder="apiParams.dockerregistrypassword.description"/>
            </a-form-item>
            <a-form-item name="dockerregistryurl" ref="dockerregistryurl">
              <template #label>
                <tooltip-label :title="$t('label.url')" :tooltip="apiParams.dockerregistryurl.description"/>
              </template>
              <a-input
                v-model:value="form.dockerregistryurl"
                :placeholder="apiParams.dockerregistryurl.description"/>
            </a-form-item>
          </div>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateKubernetesCluster',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ResourceIcon
  },
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
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createKubernetesCluster')
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
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        controlnodes: 2,
        size: 1,
        noderootdisksize: 8
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.kubecluster.name') }],
        description: [{ required: true, message: this.$t('message.error.cluster.description') }],
        zoneid: [{ required: true, message: this.$t('message.error.zone.for.cluster') }],
        kubernetesversionid: [{ required: true, message: this.$t('message.error.version.for.cluster') }],
        serviceofferingid: [{ required: true, message: this.$t('message.error.serviceoffering.for.cluster') }],
        noderootdisksize: [
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value < 8)) {
                return Promise.reject(this.$t('message.validate.min').replace('{0}', '8GB'))
              }
              return Promise.resolve()
            }
          }
        ],
        size: [
          {
            required: true,
            message: this.$t('message.error.size.for.cluster')
          },
          { type: 'number', validator: this.validateNumber }
        ],
        dockerregistryusername: [{ required: true, message: this.$t('label.required') }],
        dockerregistrypassword: [{ required: true, message: this.$t('label.required') }],
        dockerregistryurl: [{ required: true, message: this.$t('label.required') }],
        controlnodes: [
          { required: true, message: this.$t('message.error.input.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value < 2)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ]
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
      params.showicon = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
          this.zones = this.zones.filter(zone => zone.type !== 'Edge')
        }
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
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
          this.form.kubernetesversionid = 0
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
          this.form.serviceofferingid = 0
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
          this.form.networkid = 0
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
          this.form.keypair = 0
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
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
        if (this.isValidValueForKey(values, 'haenable') && values.haenable &&
          this.isValidValueForKey(values, 'controlnodes') && values.controlnodes > 0) {
          params.controlnodes = values.controlnodes
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
        if (values.privateregistry) {
          params.dockerregistryusername = values.dockerregistryusername
          params.dockerregistrypassword = values.dockerregistrypassword
          params.dockerregistryurl = values.dockerregistryurl
        }

        api('createKubernetesCluster', params).then(json => {
          const jobId = json.createkubernetesclusterresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.kubernetes.cluster.create'),
            description: values.name,
            loadingMessage: `${this.$t('label.kubernetes.cluster.create')} ${values.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: this.$t('message.success.create.kubernetes.cluter') + ' ' + values.name
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.validate.number'))
      }
      return Promise.resolve()
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
</style>
