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
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            autoFocus
            v-model:value="form.name"
            :placeholder="$t('label.name')"/>
        </a-form-item>
        <a-form-item name="displaytext" ref="displaytext">
          <template #label>
            {{ $t('label.displaytext') }}
            <a-tooltip :title="apiParams.displaytext.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.displaytext"
            :placeholder="$t('label.displaytext')"/>
        </a-form-item>
        <a-form-item name="storagetype" ref="storagetype">
          <template #label>
            {{ $t('label.storagetype') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.storagetype"
            buttonStyle="solid">
            <a-radio-button value="shared">
              {{ $t('label.shared') }}
            </a-radio-button>
            <a-radio-button value="local">
              {{ $t('label.local') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="provisioningtype" ref="provisioningtype">
          <template #label>
            {{ $t('label.provisioningtype') }}
            <a-tooltip :title="apiParams.provisioningtype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.provisioningtype"
            buttonStyle="solid">
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
        <a-form-item name="customdisksize" ref="customdisksize">
          <template #label>
            {{ $t('label.customdisksize') }}
            <a-tooltip :title="apiParams.customized.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.customdisksize" />
        </a-form-item>
        <a-form-item name="disksize" ref="disksize" v-if="!form.customdisksize">
          <template #label>
            {{ $t('label.disksize') }}
            <a-tooltip :title="apiParams.disksize.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.disksize"
            :placeholder="$t('label.disksize')"/>
        </a-form-item>
        <a-form-item name="qostype" ref="qostype" :label="$t('label.qostype')">
          <a-radio-group
            v-model:value="form.qostype"
            buttonStyle="solid">
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
        <a-form-item name="diskbytesreadrate" ref="diskbytesreadrate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskbytesreadrate') }}
            <a-tooltip :title="apiParams.bytesreadrate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskbytesreadrate"
            :placeholder="$t('label.diskbytesreadrate')"/>
        </a-form-item>
        <a-form-item name="diskbyteswriterate" ref="diskbyteswriterate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskbyteswriterate') }}
            <a-tooltip :title="apiParams.byteswriterate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskbyteswriterate"
            :placeholder="$t('label.diskbyteswriterate')"/>
        </a-form-item>
        <a-form-item name="diskiopsreadrate" ref="diskiopsreadrate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskiopsreadrate') }}
            <a-tooltip :title="apiParams.iopsreadrate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsreadrate"
            :placeholder="$t('label.diskiopsreadrate')"/>
        </a-form-item>
        <a-form-item name="diskiopswriterate" ref="diskiopswriterate" v-if="form.qostype === 'hypervisor'">
          <template #label>
            {{ $t('label.diskiopswriterate') }}
            <a-tooltip :title="apiParams.iopswriterate.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopswriterate"
            :placeholder="$t('label.diskiopswriterate')"/>
        </a-form-item>
        <a-form-item name="iscustomizeddiskiops" ref="iscustomizeddiskiops" v-if="form.qostype === 'storage'">
          <template #label>
            {{ $t('label.iscustomizeddiskiops') }}
            <a-tooltip :title="apiParams.customizediops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="form.iscustomizeddiskiops" />
        </a-form-item>
        <a-form-item name="diskiopsmin" ref="diskiopsmin" v-if="form.qostype === 'storage' && !form.iscustomizeddiskiops">
          <template #label>
            {{ $t('label.diskiopsmin') }}
            <a-tooltip :title="apiParams.miniops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsmin"
            :placeholder="$t('label.diskiopsmin')"/>
        </a-form-item>
        <a-form-item name="diskiopsmax" ref="diskiopsmax" v-if="form.qostype === 'storage' && !form.iscustomizeddiskiops">
          <template #label>
            {{ $t('label.diskiopsmax') }}
            <a-tooltip :title="apiParams.maxiops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.diskiopsmax"
            :placeholder="$t('label.diskiopsmax')"/>
        </a-form-item>
        <a-form-item name="hypervisorsnapshotreserve" ref="hypervisorsnapshotreserve" v-if="form.qostype === 'storage'">
          <template #label>
            {{ $t('label.hypervisorsnapshotreserve') }}
            <a-tooltip :title="apiParams.hypervisorsnapshotreserve.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.hypervisorsnapshotreserve"
            :placeholder="$t('label.hypervisorsnapshotreserve')"/>
        </a-form-item>
        <a-form-item name="writecachetype" ref="writecachetype">
          <template #label>
            {{ $t('label.writecachetype') }}
            <a-tooltip :title="apiParams.cachemode.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-radio-group
            v-model:value="form.writecachetype"
            buttonStyle="solid">
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
        <a-form-item name="tags" ref="tags" v-if="isAdmin()">
          <template #label>
            {{ $t('label.storagetags') }}
            <a-tooltip :title="apiParams.tags.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            mode="tags"
            v-model:value="form.tags"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="storageTagLoading"
            :placeholder="$t('label.tags')"
            v-if="isAdmin()">
            <a-select-option v-for="(opt) in storageTags" :key="opt">
              {{ opt }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="ispublic" ref="ispublic" :label="$t('label.ispublic')" v-show="isAdmin()">
          <a-switch v-model:checked="form.ispublic" />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="!form.ispublic">
          <template #label>
            {{ $t('label.domainid') }}
            <a-tooltip :title="apiParams.domainid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            mode="multiple"
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="$t('label.domainid')">
            <a-select-option v-for="(opt, optIndex) in domains" :key="optIndex">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            id="zone-selection"
            mode="multiple"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @select="val => fetchvSphereStoragePolicies(val)"
            :loading="zoneLoading"
            :placeholder="$t('label.zoneid')">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="storagepolicy" ref="storagepolicy" v-if="'listVsphereStoragePolicies' in $store.getters.apis && storagePolicies !== null">
          <template #label>
            {{ $t('label.vmware.storage.policy') }}
            <a-tooltip :title="apiParams.storagetype.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="form.storagepolicy"
            :placeholder="apiParams.storagepolicy.description">
            <a-select-option v-for="policy in storagePolicies" :key="policy.id">
              {{ policy.name || policy.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import { reactive, ref, toRaw } from 'vue'

export default {
  name: 'AddDiskOffering',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
  },
  data () {
    return {
      isCustomizedDiskIops: false,
      selectedDomains: [],
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
    this.apiParams = this.$getApiParams('createDiskOffering')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    this.isPublic = this.isAdmin()
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        storagetype: 'shared',
        provisioningtype: 'thin',
        customdisksize: true,
        writecachetype: 'none',
        ispublic: this.isPublic
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        displaytext: [{ required: true, message: this.$t('message.error.required.input') }],
        disksize: [
          { type: 'number', required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateNumber }
        ],
        diskbytesreadrate: [{ type: 'number', validator: this.validateNumber }],
        diskbyteswriterate: [{ type: 'number', validator: this.validateNumber }],
        diskiopsreadrate: [{ type: 'number', validator: this.validateNumber }],
        diskiopswriterate: [{ type: 'number', validator: this.validateNumber }],
        diskiopsmax: [{ type: 'number', validator: this.validateNumber }],
        hypervisorsnapshotreserve: [{ type: 'number', validator: this.validateNumber }],
        domainid: [{ required: true, message: this.$t('message.error.select') }],
        zoneid: [{
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
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
      if (zoneIndex === 0 || this.form.zoneid.length > 1) {
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
    handleSubmit (e) {
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
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

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
