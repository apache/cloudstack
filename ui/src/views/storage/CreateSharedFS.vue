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
    <div v-if="!isNormalUserOrProject">
      <ownership-selection @fetch-owner="fetchOwnerOptions" />
    </div>
    <a-form
      class="form"
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item name="name" ref="name" :label="$t('label.name')">
        <a-input v-model:value="form.name" v-focus="true" />
      </a-form-item>
      <a-form-item name="description" ref="description" :label="$t('label.description')">
        <a-input v-model:value="form.description" />
      </a-form-item>
      <a-form-item ref="zoneid" name="zoneid">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          :loading="zoneLoading"
          @change="zone => handleZoneChange(id)"
          :placeholder="apiParams.zoneid.description"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="filesystem" name="filesystem">
        <template #label>
          <tooltip-label :title="$t('label.filesystem')" :tooltip="apiParams.filesystem.description"/>
        </template>
        <a-select
          v-model:value="form.filesystem"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option value="XFS" label="XFS">XFS</a-select-option>
          <a-select-option value="EXT4" label="EXT4">EXT4</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="networkid" name="networkid">
        <template #label>
          <tooltip-label :title="$t('label.networkid')" :tooltip="apiParams.networkid.description || 'Network'"/>
        </template>
        <a-select
          v-model:value="form.networkid"
          :loading="networkLoading"
          :placeholder="apiParams.networkid.description || $t('label.networkid')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(network, index) in networks"
            :value="network.id"
            :key="index"
            :label="network.name"> {{ network.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="diskofferingid" name="diskofferingid">
        <template #label>
          <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || 'Disk Offering'"/>
        </template>
        <a-select
          v-model:value="form.diskofferingid"
          :loading="diskofferingLoading"
          @change="id => handleDiskOfferingChange(id)"
          :placeholder="apiParams.diskofferingid.description || $t('label.diskofferingid')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(diskoffering, index) in diskofferings"
            :value="diskoffering.id"
            :key="index"
            :label="diskoffering.displaytext || diskoffering.name">
            {{ diskoffering.displaytext || diskoffering.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <span v-if="customDiskOffering">
        <a-form-item ref="size" name="size">
          <template #label>
            <tooltip-label :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
      </span>
      <span v-if="isCustomizedDiskIOps">
        <a-form-item ref="miniops" name="miniops">
          <template #label>
            <tooltip-label :title="$t('label.miniops')" :tooltip="apiParams.miniops.description"/>
          </template>
          <a-input
            v-model:value="form.miniops"
            :placeholder="apiParams.miniops.description"/>
        </a-form-item>
        <a-form-item ref="maxiops" name="maxiops">
          <template #label>
            <tooltip-label :title="$t('label.maxiops')" :tooltip="apiParams.maxiops.description"/>
          </template>
          <a-input
            v-model:value="form.maxiops"
            :placeholder="apiParams.maxiops.description"/>
        </a-form-item>
      </span>
      <a-form-item ref="serviceofferingid" name="serviceofferingid">
        <template #label>
          <tooltip-label :title="$t('label.compute.offering.for.sharedfs.instance')" :tooltip="apiParams.serviceofferingid.description || 'Service Offering'"/>
        </template>
        <a-select
          v-model:value="form.serviceofferingid"
          :loading="serviceofferingLoading"
          :placeholder="$t('label.serviceofferingid')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(serviceoffering, index) in serviceofferings"
            :value="serviceoffering.id"
            :key="index"
            :label="serviceoffering.displaytext || serviceoffering.name">
            {{ serviceoffering.displaytext || serviceoffering.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import store from '@/store'

export default {
  name: 'CreateSharedFS',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    OwnershipSelection,
    ResourceIcon,
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      owner: {
        projectid: store.getters.project?.id,
        domainid: store.getters.project?.id ? null : store.getters.userInfo.domainid,
        account: store.getters.project?.id ? null : store.getters.userInfo.account
      },
      loading: false,
      zones: [],
      zoneLoading: false,
      configLoading: false,
      networks: [],
      networkLoading: false,
      serviceofferings: [],
      serviceofferingLoading: false,
      diskofferings: [],
      diskofferingLoading: false,
      customDiskOffering: false,
      isCustomizedDiskIOps: false
    }
  },
  computed: {
    isNormalUserOrProject () {
      return ['User'].includes(this.$store.getters.userInfo.roletype) || store.getters.project?.id
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createSharedFileSystem')
  },
  created () {
    this.initForm()
    this.fetchData()
    this.form.filesystem = 'XFS'
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
        name: [{ required: true, message: this.$t('label.required') }],
        networkid: [{ required: true, message: this.$t('label.required') }],
        serviceofferingid: [{ required: true, message: this.$t('label.required') }],
        diskofferingid: [{ required: true, message: this.$t('label.required') }],
        size: [{ required: true, message: this.$t('message.error.custom.disk.size') }],
        miniops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }],
        maxiops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    fetchOwnerOptions (OwnerOptions) {
      this.owner = {}
      console.log('fetching owner')
      if (OwnerOptions.selectedAccountType === this.$t('label.account')) {
        if (!OwnerOptions.selectedAccount) {
          return
        }
        console.log('fetched account')
        this.owner.account = OwnerOptions.selectedAccount
        this.owner.domainid = OwnerOptions.selectedDomain
      } else if (OwnerOptions.selectedAccountType === this.$t('label.project')) {
        if (!OwnerOptions.selectedProject) {
          return
        }
        console.log('fetched project')
        this.owner.projectid = OwnerOptions.selectedProject
      }
      console.log('fetched owner')
      this.fetchData()
    },
    fetchData () {
      this.minCpu = store.getters.features.sharedfsvmmincpucount
      this.minMemory = store.getters.features.sharedfsvmminramsize
      this.fetchZones()
    },
    fetchZones () {
      this.zoneLoading = true
      const params = { showicon: true }
      api('listZones', params).then(json => {
        var listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = []
          listZones = listZones.filter(x => (x.allocationstate === 'Enabled' && x.networktype === 'Advanced' && x.securitygroupsenabled === false))
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = this.zones[0].id
          this.handleZoneChange(this.zones[0])
        }
      })
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.fetchServiceOfferings()
      this.fetchDiskOfferings()
      this.fetchNetworks()
    },
    fetchServiceOfferings () {
      this.serviceofferingLoading = true
      this.serviceofferings = []
      var params = {
        zoneid: this.selectedZone.id,
        listall: true,
        domainid: this.owner.domainid
      }
      if (this.owner.projectid) {
        params.projectid = this.owner.projectid
      } else {
        params.account = this.owner.account
      }
      api('listServiceOfferings', params).then(json => {
        var items = json.listserviceofferingsresponse.serviceoffering || []
        if (items != null) {
          for (var i = 0; i < items.length; i++) {
            if (items[i].iscustomized === false && items[i].offerha === true &&
                items[i].cpunumber >= this.minCpu && items[i].memory >= this.minMemory) {
              this.serviceofferings.push(items[i])
            }
          }
        }
        this.form.serviceofferingid = this.serviceofferings[0].id || ''
      }).finally(() => {
        this.serviceofferingLoading = false
      })
    },
    fetchDiskOfferings () {
      this.diskofferingLoading = true
      this.form.diskofferingid = null
      var params = {
        zoneid: this.selectedZone.id,
        listall: true,
        domainid: this.owner.domainid
      }
      if (this.owner.projectid) {
        params.projectid = this.owner.projectid
      } else {
        params.account = this.owner.account
      }
      api('listDiskOfferings', params).then(json => {
        this.diskofferings = json.listdiskofferingsresponse.diskoffering || []
        this.form.diskofferingid = this.diskofferings[0].id || ''
        this.customDiskOffering = this.diskofferings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.diskofferings[0]?.iscustomizediops || false
      }).finally(() => {
        this.diskofferingLoading = false
      })
    },
    fetchNetworks () {
      this.networkLoading = true
      this.form.networkid = null
      var params = {
        zoneid: this.selectedZone.id,
        canusefordeploy: true,
        domainid: this.owner.domainid
      }
      if (this.owner.projectid) {
        params.projectid = this.owner.projectid
      } else {
        params.account = this.owner.account
      }
      api('listNetworks', params).then(json => {
        this.networks = json.listnetworksresponse.network || []
        this.form.networkid = this.networks[0].id || ''
      }).finally(() => {
        this.networkLoading = false
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleDiskOfferingChange (id) {
      const diskoffering = this.diskofferings.filter(x => x.id === id)
      this.customDiskOffering = diskoffering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = diskoffering[0]?.iscustomizediops || false
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          name: values.name,
          description: values.description,
          zoneid: values.zoneid,
          serviceofferingid: values.serviceofferingid,
          diskofferingid: values.diskofferingid,
          networkid: values.networkid,
          size: values.size,
          filesystem: values.filesystem,
          miniops: values.miniops,
          maxiops: values.maxiops,
          domainid: this.owner.domainid
        }
        if (this.owner.projectid) {
          data.projectid = this.owner.projectid
        } else {
          data.account = this.owner.account
        }
        this.loading = true
        api('createSharedFileSystem', data).then(response => {
          this.$pollJob({
            jobId: response.createsharedfilesystemresponse.jobid,
            title: this.$t('label.create.sharedfs'),
            description: values.name,
            successMessage: this.$t('message.success.create.sharedfs'),
            errorMessage: this.$t('message.create.sharedfs.failed'),
            loadingMessage: this.$t('message.create.sharedfs.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    min-width: 400px;
    width: 100%;
  }
}
</style>
