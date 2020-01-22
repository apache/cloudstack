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
              rules: [{ required: true, message: 'Please enter name' }]
            }]"
            :placeholder="this.$t('Name')"/>
        </a-form-item>
        <a-form-item :label="$t('description')">
          <a-input
            v-decorator="['description', {
              rules: [{ required: true, message: 'Please enter description' }]
            }]"
            :placeholder="this.$t('Description')"/>
        </a-form-item>
        <a-form-item :label="$t('label.storagetype')">
          <a-radio-group
            v-decorator="['storagetype', {
              initialValue: this.storageType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleStorageTypeChange(selected.target.value) }">
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('label.local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.provisioningtype')">
          <a-radio-group
            v-decorator="['provisioningtype', {
              initialValue: this.provisioningType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">
              {{ $t('label.thin') }}
            </a-radio-button>
            <a-radio-button value="sparse">
              {{ $t('label.sparse') }}
            </a-radio-button>
            <a-radio-button value="fat">
              {{ $t('label.fat') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.customdisksize')">
          <a-switch v-decorator="['ciustomdisksize']" :checked="this.isCustomDiskSize" @change="val => { this.isCustomDiskSize = val }" />
        </a-form-item>
        <a-form-item :label="$t('label.disksize.gb')" v-if="this.isCustomDiskSize">
          <a-input
            v-decorator="['disksize', {
              rules: [{ required: true, message: 'Please enter disk size' },
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
            :placeholder="this.$t('Description')"/>
        </a-form-item>
        <a-form-item :label="$t('label.qostype')">
          <a-radio-group
            v-decorator="['qostype', {
              initialValue: this.qosType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleQosTypeChange(selected.target.value) }">
            <a-radio-button value="">
              {{ $t('label.none') }}
            </a-radio-button>
            <a-radio-button value="hypervisor">
              {{ $t('label.hypervisor') }}
            </a-radio-button>
            <a-radio-button value="storage">
              {{ $t('label.storage') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('label.disk.bytes.read.rate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskbytesreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.bytes.read.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.bytes.write.rate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskbyteswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.bytes.write.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.read.rate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskiopsreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.iops.read.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.write.rate')" v-if="this.qosType === 'hypervisor'">
          <a-input
            v-decorator="['diskiopswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.iops.write.rate')"/>
        </a-form-item>
        <a-form-item :label="$t('label.custom.disk.iops')" v-if="this.qosType === 'storage'">
          <a-switch v-decorator="['iscustomizeddiskiops']" :checked="this.isCustomizedDiskIops" @change="val => { this.isCustomizedDiskIops = val }" />
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.min')" v-if="!this.isCustomizedDiskIops">
          <a-input
            v-decorator="['diskiopsmin', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.iops.min')"/>
        </a-form-item>
        <a-form-item :label="$t('label.disk.iops.max')" v-if="!this.isCustomizedDiskIops">
          <a-input
            v-decorator="['diskiopsmax', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.disk.iops.max')"/>
        </a-form-item>
        <a-form-item :label="$t('label.hypervisor.snapshot.reserve')" v-if="this.qosType === 'storage'">
          <a-input
            v-decorator="['hypervisorsnapshotreserve', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback('Please enter a valid number')
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.hypervisor.snapshot.reserve')"/>
        </a-form-item>
        <a-form-item :label="$t('label.writecachetype')">
          <a-radio-group
            v-decorator="['writecachetype', {
              initialValue: this.writeCacheType
            }]"
            buttonStyle="solid"
            @change="selected => { this.handleWriteCacheTypeChange(selected.target.value) }">
            <a-radio-button value="nodiskcache">
              {{ $t('label.nodiskcache') }}
            </a-radio-button>
            <a-radio-button value="writebackdiskcaching">
              {{ $t('label.writebackdiskcaching') }}
            </a-radio-button>
            <a-radio-button value="writethroughdiskcaching">
              {{ $t('label.writethroughdiskcaching') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('ispublic')" v-show="this.isAdmin()">
          <a-switch v-decorator="['ispublic']" :checked="this.isPublic" @change="val => { this.isPublic = val }" />
        </a-form-item>
        <a-form-item :label="$t('domainid')" v-if="!this.isPublic">
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: 'Please select option'
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="this.$t('label.domain')">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('zoneid')">
          <a-select
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback('All Zones cannot be combined with any other zone')
                    }
                    callback()
                  }
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="this.$t('label.zone')">
            <a-select-option v-for="(opt, optIndex) in this.zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('Cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('OK') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'AddDiskOffering',
  props: {
  },
  components: {
  },
  data () {
    return {
      storageType: 'shared',
      provisioningType: 'thin',
      isCustomDiskSize: false,
      qosType: '',
      isCustomizedDiskIops: false,
      writeCacheType: 'nodiskcache',
      selectedDomains: [],
      selectedZones: [],
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.zones = [
      {
        id: 'all',
        name: this.$t('label.all.zone')
      }
    ]
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.details = 'min'
      this.domainLoading = true
      api('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchZoneData () {
      const params = {}
      params.listAll = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    handleStorageTypeChange (val) {
      this.storageType = val
    },
    handleProvisioningTypeChange (val) {
      this.provisioningType = val
    },
    handleQosTypeChange (val) {
      this.qosType = val
    },
    handleWriteCacheTypeChange (val) {
      this.writeCacheType = val
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {}
        console.log(params)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="scss">
  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
