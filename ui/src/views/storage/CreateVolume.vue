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
    <a-form class="form" :form="form" @submit="handleSubmit" layout="vertical">
      <a-form-item>
        <span slot="label">
          {{ $t('label.name') }}
          <a-tooltip :title="apiParams.name.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-input
          v-decorator="['name', {
            rules: [{ required: true, message: $t('message.error.volume.name') }]
          }]"
          :placeholder="$t('label.volumename')"
          autoFocus />
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.zoneid') }}
          <a-tooltip :title="apiParams.zoneid.description">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-select
          v-decorator="['zoneid', {
            initialValue: selectedZoneId,
            rules: [{ required: true, message: $t('message.error.zone') }] }]"
          :loading="loading"
          @change="zone => fetchDiskOfferings(zone)">
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index">
            {{ zone.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <span slot="label">
          {{ $t('label.diskoffering') }}
          <a-tooltip :title="apiParams.diskofferingid.description || 'Disk Offering'">
            <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </span>
        <a-select
          v-decorator="['diskofferingid', {
            initialValue: selectedDiskOfferingId,
            rules: [{ required: true, message: $t('message.error.select') }]}]"
          :loading="loading"
          @change="id => (customDiskOffering = offerings.filter(x => x.id === id)[0].iscustomized || false)"
        >
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
          <span slot="label">
            {{ $t('label.sizegb') }}
            <a-tooltip :title="apiParams.size.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['size', {
              rules: [{ required: true, message: $t('message.error.custom.disk.size') }]}]"
            :placeholder="$t('label.disksize')"/>
        </a-form-item>
      </span>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'CreateVolume',
  data () {
    return {
      zones: [],
      offerings: [],
      selectedZoneId: '',
      selectedDiskOfferingId: '',
      customDiskOffering: false,
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.createVolume || {}
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listZones').then(json => {
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
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        api('createVolume', values).then(response => {
          this.$pollJob({
            jobId: response.createvolumeresponse.jobid,
            successMessage: this.$t('message.success.create.volume'),
            successMethod: () => {
              this.$store.dispatch('AddAsyncJob', {
                title: this.$t('message.success.create.volume'),
                jobid: response.createvolumeresponse.jobid,
                description: values.name,
                status: 'progress'
              })
              this.$emit('refresh-data')
            },
            errorMessage: this.$t('message.create.volume.failed'),
            errorMethod: () => {
              this.$emit('refresh-data')
            },
            loadingMessage: this.$t('message.create.volume.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.$emit('refresh-data')
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

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
