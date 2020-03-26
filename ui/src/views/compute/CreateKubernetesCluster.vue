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
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item :label="$t('name')">
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: 'Please enter Kubernetes cluster name' }]
            }]"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item :label="$t('description')">
          <a-input
            v-decorator="['description', {
              rules: [{ required: true, message: 'Please enter Kubernetes cluster description' }]
            }]"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item :label="$t('zoneid')">
          <a-select
            id="zone-selection"
            v-decorator="['zoneid', {
              rules: [{ required: true, message: 'Please select zone for Kubernetes cluster' }]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description"
            @change="val => { this.handleZoneChanged(this.zones[val]) }">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('kubernetesversionid')">
          <a-select
            id="version-selection"
            v-decorator="['kubernetesversionid', {
              rules: [{ required: true, message: 'Please select Kubernetes version for Kubernetes cluster' }]
            }]"
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
        <a-form-item :label="$t('serviceofferingid')">
          <a-select
            id="offering-selection"
            v-decorator="['serviceofferingid', {
              rules: [{ required: true, message: 'Please select service offering for Kubernetes cluster' }]
            }]"
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
        <a-form-item :label="$t('noderootdisksize')">
          <a-input
            v-decorator="['noderootdisksize', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.noderootdisksize.description"/>
        </a-form-item>
        <a-form-item :label="$t('networkid')">
          <a-select
            id="network-selection"
            v-decorator="['networkid', {}]"
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
        <a-form-item :label="$t('haenable')" v-if="this.selectedKubernetesVersion != null && this.selectedKubernetesVersion != undefined && this.selectedKubernetesVersion.supportsha === true">
          <a-switch v-decorator="['haenable', {initialValue: this.haEnabled}]" :checked="this.haEnabled" @change="val => { this.haEnabled = val }" />
        </a-form-item>
        <a-form-item :label="$t('masternodes')" v-if="this.haEnabled">
          <a-input
            v-decorator="['masternodes', {
              initialValue: '1',
              rules: [{ required: true, message: 'Please enter value' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.masternodes.description"/>
        </a-form-item>
        <a-form-item :label="$t('externalloadbalanceripaddress')" v-if="this.haEnabled">
          <a-input
            v-decorator="['externalloadbalanceripaddress', {}]"
            :placeholder="apiParams.externalloadbalanceripaddress.description"/>
        </a-form-item>
        <a-form-item :label="$t('cks.cluster.size')">
          <a-input
            v-decorator="['size', {
              initialValue: '1',
              rules: [{ required: true, message: 'Please enter size for Kubernetes cluster' },
                      {
                        validator: (rule, value, callback) => {
                          if (value && (isNaN(value) || value <= 0)) {
                            callback('Please enter a valid number')
                          }
                          callback()
                        }
                      }
              ]
            }]"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
        <a-form-item :label="$t('keypair')">
          <a-select
            id="keypair-selection"
            v-decorator="['keypair', {}]"
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

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
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
      haEnabled: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
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
  },
  mounted () {
    this.fetchData()
  },
  methods: {
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
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
        api('createKubernetesCluster', params).then(json => {
          this.$message.success('Successfully created Kubernetes cluster: ' + values.name)
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message
          })
        }).finally(() => {
          this.$emit('refresh-data')
          this.loading = false
          this.closeAction()
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
