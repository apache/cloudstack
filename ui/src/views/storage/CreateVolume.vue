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
    <a-form
      class="form"
      :form="form"
      @submit="handleSubmit"
      v-ctrl-enter="handleSubmit"
      layout="vertical">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        <a-input
          v-decorator="['name', {
            rules: [{ required: true, message: $t('message.error.volume.name') }]
          }]"
          :placeholder="$t('label.volumename')"
          autoFocus />
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        <a-select
          v-decorator="['zoneid', {
            initialValue: selectedZoneId,
            rules: [{ required: true, message: $t('message.error.zone') }] }]"
          :loading="loading"
          @change="zone => fetchDiskOfferings(zone)"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.propsData.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <a-icon v-else type="global" style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || 'Disk Offering'"/>
        <a-select
          v-decorator="['diskofferingid', {
            initialValue: selectedDiskOfferingId,
            rules: [{ required: true, message: $t('message.error.select') }]}]"
          :loading="loading"
          @change="id => onChangeDiskOffering(id)"
          showSearch
          optionFilterProp="children"
          :filterOption="(input, option) => {
            return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(offering, index) in offerings"
            :value="offering.id"
            :key="index">
            {{ offering.displaytext || offering.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <span v-if="customDiskOffering">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
          <a-input
            v-decorator="['size', {
              rules: [{ required: true, message: $t('message.error.custom.disk.size') }]}]"
            :placeholder="$t('label.disksize')"/>
        </a-form-item>
      </span>
      <span v-if="isCustomizedDiskIOps">
        <a-form-item>
          <span slot="label">
            {{ $t('label.miniops') }}
            <a-tooltip :title="apiParams.miniops.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['miniops', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.miniops')"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.maxiops') }}
            <a-tooltip :title="apiParams.maxiops.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['maxiops', {
              rules: [{
                validator: (rule, value, callback) => {
                  if (value && (isNaN(value) || value <= 0)) {
                    callback(this.$t('message.error.number'))
                  }
                  callback()
                }
              }]
            }]"
            :placeholder="this.$t('label.maxiops')"/>
        </a-form-item>
      </span>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVolume',
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      zones: [],
      offerings: [],
      selectedZoneId: '',
      selectedDiskOfferingId: '',
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('createVolume')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listZones', { showicon: true }).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.selectedZoneId = this.zones[0].id || ''
        this.fetchDiskOfferings(this.selectedZoneId)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchDiskOfferings (zoneId) {
      this.loading = true
      api('listDiskOfferings', {
        zoneid: zoneId,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
        this.selectedDiskOfferingId = this.offerings[0].id || ''
        this.customDiskOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.form.validateFieldsAndScroll((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        api('createVolume', values).then(response => {
          this.$pollJob({
            jobId: response.createvolumeresponse.jobid,
            title: this.$t('message.success.create.volume'),
            description: values.name,
            successMessage: this.$t('message.success.create.volume'),
            errorMessage: this.$t('message.create.volume.failed'),
            loadingMessage: this.$t('message.create.volume.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    onChangeDiskOffering (id) {
      const offering = this.offerings.filter(x => x.id === id)
      this.customDiskOffering = offering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = offering[0]?.iscustomizediops || false
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
</style>
