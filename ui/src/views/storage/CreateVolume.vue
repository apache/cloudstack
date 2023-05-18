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
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="apiParams.name.description" />
      </a-form-item>
      <a-form-item ref="zoneid" name="zoneid" v-if="!createVolumeFromSnapshot">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          :loading="loading"
          @change="zone => fetchDiskOfferings(zone)"
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
      <a-form-item ref="diskofferingid" name="diskofferingid" v-if="!createVolumeFromSnapshot || (createVolumeFromSnapshot && resource.volumetype === 'ROOT')">
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
            v-for="(offering, index) in offerings"
            :value="offering.id"
            :key="index"
            :label="offering.displaytext || offering.name">
            {{ offering.displaytext || offering.name }}
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
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVolume',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      zones: [],
      offerings: [],
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false
    }
  },
  computed: {
    createVolumeFromSnapshot () {
      return this.$route.path.startsWith('/snapshot')
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
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
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
      if (!this.createVolumeFromSnapshot) {
        this.rules.name = [{ required: true, message: this.$t('message.error.volume.name') }]
        this.rules.diskofferingid = [{ required: true, message: this.$t('message.error.select') }]
      }
    },
    fetchData () {
      this.loading = true
      api('listZones', { showicon: true }).then(json => {
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
        if (!this.createVolumeFromSnapshot) {
          this.form.diskofferingid = this.offerings[0].id || ''
        }
        this.customDiskOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        if (this.createVolumeFromSnapshot) {
          values.snapshotid = this.resource.id
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
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
