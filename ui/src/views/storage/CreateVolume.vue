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
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical">
      <a-form-item ref="name" name="name">
        <template #label>
          {{ $t('label.name') }}
          <a-tooltip :title="apiParams.name.description">
            <info-circle-outlined style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </template>
        <a-input
          v-model:value="form.name"
          :placeholder="$t('label.volumename')"
          autoFocus />
      </a-form-item>
      <a-form-item ref="zoneid" name="zoneid">
        <template #label>
          {{ $t('label.zoneid') }}
          <a-tooltip :title="apiParams.zoneid.description">
            <info-circle-outlined style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </template>
        <a-select
          v-model:value="form.zoneid"
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
      <a-form-item ref="diskofferingid" name="diskofferingid">
        <template #label>
          {{ $t('label.diskoffering') }}
          <a-tooltip :title="apiParams.diskofferingid.description || 'Disk Offering'">
            <info-circle-outlined style="color: rgba(0,0,0,.45)" />
          </a-tooltip>
        </template>
        <a-select
          v-model:value="form.diskofferingid"
          :loading="loading"
          @change="id => onChangeDiskOffering(id)"
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
          <template #label>
            {{ $t('label.sizegb') }}
            <a-tooltip :title="apiParams.size.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="$t('label.disksize')"/>
        </a-form-item>
      </span>
      <span v-if="isCustomizedDiskIOps">
        <a-form-item>
          <template #label>
            {{ $t('label.miniops') }}
            <a-tooltip :title="apiParams.miniops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.miniops"
            :placeholder="this.$t('label.miniops')"/>
        </a-form-item>
        <a-form-item>
          <template #label>
            {{ $t('label.maxiops') }}
            <a-tooltip :title="apiParams.maxiops.description">
              <info-circle-outlined style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="form.maxiops"
            :placeholder="this.$t('label.maxiops')"/>
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
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'CreateVolume',
  data () {
    return {
      zones: [],
      offerings: [],
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVolume')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.volume.name') }],
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
        diskofferingid: [{ required: true, message: this.$t('message.error.select') }],
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
    fetchData () {
      this.loading = true
      api('listZones').then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
        this.fetchDiskOfferings(this.form.zoneid)
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
        this.form.diskofferingid = this.offerings[0].id || ''
        this.customDiskOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
