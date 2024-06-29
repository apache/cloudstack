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
        layout="vertical"
        @finish="handleSubmit"
       >
        <a-form-item name="name" ref="name" :label="$t('label.name')">
          <a-input v-model:value="form.name" v-focus="true" />
        </a-form-item>
        <a-form-item name="description" ref="description" :label="$t('label.description')">
          <a-input v-model:value="form.description" v-focus="true" />
        </a-form-item>
        <a-form-item ref="zoneid" name="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            v-model:value="form.zoneid"
            :loading="loading"
            @change="zone => fetchServiceOfferings(zone), fetchDiskOfferings(zone), fetchNetworks(zone)"
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
        <a-form-item ref="provider" name="provider">
          <template #label>
            <tooltip-label :title="$t('label.provider')" :tooltip="apiParams.provider.description"/>
          </template>
          <a-select
            v-model:value="form.provider"
            :loading="loading"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="(provider, index) in providers"
              :value="provider.name"
              :key="index"
              :label="provider.name">
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="diskofferingid" name="diskofferingid">
          <template #label>
            <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || 'Disk Offering'"/>
          </template>
          <a-select
            v-model:value="form.diskofferingid"
            :loading="loading"
            @change="id => onChangeDiskOffering(id)"
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
        <a-form-item ref="serviceofferingid" name="serviceofferingid">
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description || 'Service Offering'"/>
          </template>
          <a-select
            v-model:value="form.serviceofferingid"
            :loading="loading"
            @change="id => onChangeServiceOffering(id)"
            :placeholder="apiParams.serviceofferingid.description || $t('label.serviceofferingid')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="(offering, index) in offerings"
              :value="offering.id"
              :key="index"
              :label="offering.displaytext || offering.name">
              {{ offering.displaytext || offering.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="networkid" name="networkid">
          <template #label>
            <tooltip-label :title="$t('label.networkid')" :tooltip="apiParams.networkid.description || 'Network'"/>
          </template>
          <a-select
            v-model:value="form.networkid"
            :loading="loading"
            @change="id => onChangeNetwork(id)"
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
        <a-form-item ref="size" name="size">
          <template #label>
            <tooltip-label :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
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
  name: 'CreateFileSHare',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon,
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      snapshotZoneIds: [],
      zones: [],
      offerings: [],
      diskofferings: [],
      networks: [],
      customServiceOffering: false,
      customDiskOffering: false,
      isCustomizedDiskIOps: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createFileShare')
  },
  created () {
    this.initForm()
    this.policyList = ['Public', 'Private']
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
        name: [{ required: true, message: this.$t('label.required') }],
        serviceofferingid: [{ required: true, message: this.$t('label.required') }],
        diskofferingid: [{ required: true, message: this.$t('label.required') }],
        networkid: [{ required: true, message: this.$t('label.required') }],
        size: [{ required: true, message: this.$t('message.error.custom.disk.size') }]
      })
    },
    fetchData () {
      this.fetchZones(null)
      this.fetchFileShareProviders(null)
    },
    listFileShares () {
      this.loading = true
      api('listFileShares').then(json => {
        this.fileshares = json.listfilesharesresponse.fileshare || []
        if (this.fileshares.length > 0) {
          this.form.fileshare = this.fileshares[0].id
        }
      }).finally(() => {
        this.loading = false
      })
    },
    fetchZones () {
      this.loading = true
      const params = { showicon: true }
      api('listZones', params).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
        this.fetchServiceOfferings(this.form.zoneid)
        this.fetchDiskOfferings(this.form.zoneid)
        this.fetchNetworks(this.form.zoneid)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchFileShareProviders (id) {
      this.loading = true
      api('listFileShareProviders').then(json => {
        this.providers = json.listfileshareprovidersresponse.fileshareprovider || []
        this.form.provider = this.providers[0].name || ''
      }).finally(() => {
        this.loading = false
      })
    },
    fetchServiceOfferings (zoneId) {
      this.loading = true
      var params = {
        zoneid: zoneId,
        listall: true
      }
      api('listServiceOfferings', params).then(json => {
        this.offerings = json.listserviceofferingsresponse.serviceoffering || []
        this.form.serviceofferingid = this.offerings[0].id || ''
        this.customServiceOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    fetchDiskOfferings (zoneId) {
      this.loading = true
      var params = {
        zoneid: zoneId,
        listall: true
      }
      api('listDiskOfferings', params).then(json => {
        this.diskofferings = json.listdiskofferingsresponse.diskoffering || []
        this.form.diskeofferingid = this.diskofferings[0].id || ''
        this.customDiskOffering = this.diskofferings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.diskofferings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    fetchNetworks (zoneId) {
      this.loading = true
      var params = {
        zoneid: zoneId,
        listall: true
      }
      api('listNetworks', params).then(json => {
        this.networks = json.listnetworksresponse.network || []
        this.form.networkid = this.networks[0].id || ''
      }).finally(() => {
        this.loading = false
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    onChangeServiceOffering (id) {
      const offering = this.offerings.filter(x => x.id === id)
      this.customServiceOffering = offering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = offering[0]?.iscustomizediops || false
    },
    onChangeDiskeOffering (id) {
      const diskoffering = this.diskofferings.filter(x => x.id === id)
      this.customDiskOffering = diskoffering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = diskoffering[0]?.iscustomizediops || false
    },
    onChangeNetwork (id) {
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
          provider: values.provider
        }
        this.loading = true
        api('createFileShare', data).then(response => {
          this.$pollJob({
            jobId: response.createfileshareresponse.jobid,
            title: this.$t('label.create.bucket'),
            description: values.name,
            successMessage: this.$t('message.success.create.bucket'),
            errorMessage: this.$t('message.create.bucket.failed'),
            loadingMessage: this.$t('message.create.bucket.processing'),
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
.form-layout {
  width: 85vw;

  @media (min-width: 1000px) {
    width: 35vw;
  }
}
</style>
