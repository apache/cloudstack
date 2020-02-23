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
      <a-form-item :label="$t('name')">
        <a-input
          v-decorator="['name', {
            rules: [{ required: true, message: 'Please enter volume name' }]
          }]"
          :placeholder="$t('volumename')"/>
      </a-form-item>
      <a-form-item :label="$t('zoneid')">
        <a-select
          v-decorator="['zoneid', {
            initialValue: selectedZoneId,
            rules: [{ required: true, message: 'Please select a zone' }] }]"
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
      <a-form-item :label="$t('diskoffering')">
        <a-select
          v-decorator="['diskofferingid', {
            initialValue: selectedDiskOfferingId,
            rules: [{ required: true, message: 'Please select an option' }]}]"
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
        <a-form-item :label="$t('Size (GB)')">
          <a-input
            v-decorator="['size', {
              rules: [{ required: true, message: 'Please enter custom disk size' }]}]"
            :placeholder="$t('Enter Size in GB')"/>
        </a-form-item>
      </span>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('cancel') }}</a-button>
        <a-button type="primary" @click="handleSubmit">{{ $t('ok') }}</a-button>
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
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    log (o) {
      console.log(o)
    },
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
        zoneId: zoneId
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
            successMessage: `Successfully created volume`,
            successMethod: () => {
              this.$store.dispatch('AddAsyncJob', {
                title: `Successfully created Volume`,
                jobid: response.createvolumeresponse.jobid,
                description: values.name,
                status: 'progress'
              })
              this.$emit('refresh-data')
            },
            errorMessage: 'Failed to Create volume',
            errorMethod: () => {
              this.$emit('refresh-data')
            },
            loadingMessage: `Volume creation in progress`,
            catchMessage: 'Error encountered while fetching async job result'
          })
        }).catch(error => {
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
        }).finally(() => {
          this.loading = false
          this.closeModal()
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
