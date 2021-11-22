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
        :form="form"
        @submit="handleSubmit"
        layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          <a-input
            autoFocus
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.storagetype')" :tooltip="apiParams.storagetype.description"/>
          <a-radio-group
            v-decorator="['storagetype', {
              initialValue: storageType
            }]"
            buttonStyle="solid"
            @change="selected => { handleStorageTypeChange(selected.target.value) }">
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('label.local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.provisioningtype')" :tooltip="apiParams.provisioningtype.description"/>
          <a-radio-group
            v-decorator="['provisioningtype', {
              initialValue: provisioningType
            }]"
            buttonStyle="solid"
            @change="selected => { handleProvisioningTypeChange(selected.target.value) }">
            <a-radio-button value="thin">
              {{ $t('label.provisioningtype.thin') }}
            </a-radio-button>
            <a-radio-button value="sparse">
              {{ $t('label.provisioningtype.sparse') }}
            </a-radio-button>
            <a-radio-button value="fat">
              {{ $t('label.provisioningtype.fat') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.customdisksize')" :tooltip="apiParams.customized.description"/>
          <a-switch v-decorator="['customdisksize', { initialValue: isCustomDiskSize }]" :checked="isCustomDiskSize" @change="val => { isCustomDiskSize = val }" />
        </a-form-item>
        <a-form-item v-if="!isCustomDiskSize">
          <tooltip-label slot="label" :title="$t('label.disksize')" :tooltip="apiParams.disksize.description"/>
          <a-input
            v-decorator="['disksize', {
              rules: [
                { required: true, message: $t('message.error.required.input') },
                {
                  validator: (rule, value, callback) => {
                    if (value && (isNaN(value) || value <= 0)) {
                      callback($t('message.error.number'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            :placeholder="apiParams.disksize.description"/>
        </a-form-item>
        <a-form-item :label="$t('label.qostype')">
          <a-radio-group
            v-decorator="['qostype', {
              initialValue: qosType
            }]"
            buttonStyle="solid"
            @change="selected => { handleQosTypeChange(selected.target.value) }">
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
        <a-form-item v-if="qosType === 'hypervisor'">
          <tooltip-label slot="label" :title="$t('label.diskbytesreadrate')" :tooltip="apiParams.bytesreadrate.description"/>
          <a-input
            v-decorator="['diskbytesreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.bytesreadrate.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'hypervisor'">
          <tooltip-label slot="label" :title="$t('label.diskbyteswriterate')" :tooltip="apiParams.byteswriterate.description"/>
          <a-input
            v-decorator="['diskbyteswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.byteswriterate.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'hypervisor'">
          <tooltip-label slot="label" :title="$t('label.diskiopsreadrate')" :tooltip="apiParams.iopsreadrate.description"/>
          <a-input
            v-decorator="['diskiopsreadrate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.iopsreadrate.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'hypervisor'">
          <tooltip-label slot="label" :title="$t('label.diskiopswriterate')" :tooltip="apiParams.iopswriterate.description"/>
          <a-input
            v-decorator="['diskiopswriterate', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.iopswriterate.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'storage'">
          <tooltip-label slot="label" :title="$t('label.iscustomizeddiskiops')" :tooltip="apiParams.customizediops.description"/>
          <a-switch v-decorator="['iscustomizeddiskiops']" :checked="isCustomizedDiskIops" @change="val => { isCustomizedDiskIops = val }" />
        </a-form-item>
        <a-form-item v-if="qosType === 'storage' && !isCustomizedDiskIops">
          <tooltip-label slot="label" :title="$t('label.diskiopsmin')" :tooltip="apiParams.miniops.description"/>
          <a-input
            v-decorator="['diskiopsmin', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.miniops.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'storage' && !isCustomizedDiskIops">
          <tooltip-label slot="label" :title="$t('label.diskiopsmax')" :tooltip="apiParams.maxiops.description"/>
          <a-input
            v-decorator="['diskiopsmax', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.maxiops.description"/>
        </a-form-item>
        <a-form-item v-if="qosType === 'storage'">
          <tooltip-label slot="label" :title="$t('label.hypervisorsnapshotreserve')" :tooltip="apiParams.hypervisorsnapshotreserve.description"/>
          <a-input
            v-decorator="['hypervisorsnapshotreserve', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback($t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="apiParams.hypervisorsnapshotreserve.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.writecachetype')" :tooltip="apiParams.cachemode.description"/>
          <a-radio-group
            v-decorator="['writecachetype', {
              initialValue: writeCacheType
            }]"
            buttonStyle="solid"
            @change="selected => { handleWriteCacheTypeChange(selected.target.value) }">
            <a-radio-button value="none">
              {{ $t('label.nodiskcache') }}
            </a-radio-button>
            <a-radio-button value="writeback">
              {{ $t('label.writeback') }}
            </a-radio-button>
            <a-radio-button value="writethrough">
              {{ $t('label.writethrough') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="isAdmin()">
          <tooltip-label slot="label" :title="$t('label.storagetags')" :tooltip="apiParams.tags.description"/>
          <a-select
            mode="tags"
            v-decorator="['tags', {}]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="apiParams.tags.description"
            v-if="isAdmin()">
            <a-select-option v-for="(opt) in storageTags" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-decorator="['ispublic', {initialValue: isPublic}]" :checked="isPublic" @change="val => { isPublic = val }" />
        </a-form-item>
        <a-form-item v-if="!isPublic">
          <tooltip-label slot="label" :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          <a-select
            mode="multiple"
            v-decorator="['domainid', {
              rules: [
                {
                  required: true,
                  message: $t('message.error.select')
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex" :label="opt.path || opt.name || opt.description">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="block" style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-decorator="['zoneid', {
              rules: [
                {
                  validator: (rule, value, callback) => {
                    if (value && value.length > 1 && value.indexOf(0) !== -1) {
                      callback($t('message.error.zone.combined'))
                    }
                    callback()
                  }
                }
              ]
            }]"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <a-icon v-else type="global" style="margin-right: 5px"/>
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <tooltip-label slot="label" :title="$t('label.vmware.storage.policy')" :tooltip="apiParams.storagepolicy.description"/>
          <a-select
            v-decorator="['storagepolicy']"
            :placeholder="apiParams.storagepolicy.description"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="policy in storagePolicies" :key="policy.id">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddDiskOffering',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      storageType: 'shared',
      provisioningType: 'thin',
      isCustomDiskSize: true,
      qosType: '',
      isCustomizedDiskIops: false,
      writeCacheType: 'none',
      selectedDomains: [],
      selectedZoneIndex: [],
      storageTags: [],
      storagePolicies: null,
      storageTagLoading: false,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (_, values) => {
        this.selectedZoneIndex = values.zoneid
      }
    })
    this.apiParams = this.$getApiParams('createDiskOffering')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.fetchData()
    this.isPublic = this.isAdmin()
  },
  methods: {
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      if (this.isAdmin()) {
        this.fetchStorageTagData()
      }
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
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
      params.showicon = true
      this.zoneLoading = true
      api('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        this.zones = this.zones.concat(listZones)
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchStorageTagData () {
      const params = {}
      params.listAll = true
      this.storageTagLoading = true
      api('listStorageTags', params).then(json => {
        const tags = json.liststoragetagsresponse.storagetag || []
        for (const tag of tags) {
          if (!this.storageTags.includes(tag.name)) {
            this.storageTags.push(tag.name)
          }
        }
      }).finally(() => {
        this.storageTagLoading = false
      })
    },
    fetchvSphereStoragePolicies (zoneIndex) {
      if (zoneIndex === 0 || this.selectedZoneIndex.length > 1) {
        this.storagePolicies = null
        return
      }
      const zoneid = this.zones[zoneIndex].id
      if ('importVsphereStoragePolicies' in this.$store.getters.apis) {
        this.storagePolicies = []
        api('listVsphereStoragePolicies', {
          zoneid: zoneid
        }).then(response => {
          this.storagePolicies = response.listvspherestoragepoliciesresponse.StoragePolicy || []
        })
      }
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
      if (this.loading) return
      const options = {
        scroll: {
          offsetTop: 10
        }
      }
      this.form.validateFieldsAndScroll(options, (err, values) => {
        if (err) {
          return
        }
        this.loading = true
        var params = {
          isMirrored: false,
          name: values.name,
          displaytext: values.displaytext,
          storageType: values.storagetype,
          cacheMode: values.writecachetype,
          provisioningType: values.provisioningtype,
          customized: values.customdisksize
        }
        if (values.customdisksize !== true) {
          params.disksize = values.disksize
        }
        if (values.qostype === 'storage') {
          var customIops = values.iscustomizeddiskiops === true
          params.customizediops = customIops
          if (!customIops) {
            if (values.diskiopsmin != null && values.diskiopsmin.length > 0) {
              params.miniops = values.diskiopsmin
            }
            if (values.diskiopsmax != null && values.diskiopsmax.length > 0) {
              params.maxiops = values.diskiopsmax
            }
            if (values.hypervisorsnapshotreserve != null && values.hypervisorsnapshotreserve.length > 0) {
              params.hypervisorsnapshotreserve = values.hypervisorsnapshotreserve
            }
          }
        } else if (values.qostype === 'hypervisor') {
          if (values.diskbytesreadrate != null && values.diskbytesreadrate.length > 0) {
            params.bytesreadrate = values.diskbytesreadrate
          }
          if (values.diskbyteswriterate != null && values.diskbyteswriterate.length > 0) {
            params.byteswriterate = values.diskbyteswriterate
          }
          if (values.diskiopsreadrate != null && values.diskiopsreadrate.length > 0) {
            params.iopsreadrate = values.diskiopsreadrate
          }
          if (values.diskiopswriterate != null && values.diskiopswriterate.length > 0) {
            params.iopswriterate = values.diskiopswriterate
          }
        }
        if (values.tags != null && values.tags.length > 0) {
          var tags = values.tags.join(',')
          params.tags = tags
        }
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        var zoneIndexes = values.zoneid
        var zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }
        if (values.storagepolicy) {
          params.storagepolicy = values.storagepolicy
        }
        api('createDiskOffering', params).then(json => {
          this.$message.success(`${this.$t('message.disk.offering.created')} ${values.name}`)
          this.$emit('refresh-data')
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

<style scoped lang="scss">
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 430px;
    }
  }
</style>
