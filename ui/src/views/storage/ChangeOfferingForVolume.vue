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
  <a-form
    class="form"
    :ref="formRef"
    :model="form"
    :rules="rules"
    @finish="submitChangeOfferingForVolume"
    v-ctrl-enter="submitChangeOfferingForVolume"
    layout="vertical">
    <a-form-item>
      <a-alert type="warning">
        <template #message>
          <span v-html="$t('message.confirm.change.offering.for.volume')" />
        </template>
      </a-alert>
    </a-form-item>
    <a-form-item name="diskofferingid" ref="diskofferingid">
      <template #label>
        <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || $t('label.diskofferingid')"/>
      </template>
      <a-select
        v-model:value="form.diskofferingid"
        :loading="loading"
        showSearch
        optionFilterProp="label"
        :filterOption="(input, option) => {
          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
        }"
        @change="id => onChangeDiskOffering(id)">
        <a-select-option
          v-for="(offering, index) in diskOfferings"
          :value="offering.id"
          :key="index"
          :label="offering.displaytext || offering.name">
          {{ offering.displaytext || offering.name }}
        </a-select-option>
      </a-select>
    </a-form-item>
    <span v-if="customDiskOffering">
      <a-form-item name="size" ref="size">
        <template #label>
          <tooltip-label :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
        </template>
        <a-input
          v-model:value="form.size"
          :placeholder="$t('label.disksize')"/>
      </a-form-item>
    </span>
    <span v-if="isCustomizedDiskIOps">
      <a-form-item name="miniops" ref="miniops">
        <template #label>
          <tooltip-label :title="$t('label.miniops')" :tooltip="apiParams.miniops.description"/>
        </template>
        <a-input
          v-model:value="form.miniops"
          :placeholder="this.$t('label.miniops')"/>
      </a-form-item>
      <a-form-item name="maxiops" ref="maxiops">
        <template #label>
          <tooltip-label :title="$t('label.maxiops')" :tooltip="apiParams.maxiops.description"/>
        </template>
        <a-input
          v-model:value="form.maxiops"
          :placeholder="this.$t('label.maxiops')"/>
      </a-form-item>
    </span>
    <a-form-item name="autoMigrate" ref="autoMigrate" :label="$t('label.automigrate.volume')">
      <template #label>
        <tooltip-label :title="$t('label.automigrate.volume')" :tooltip="apiParams.automigrate.description"/>
      </template>
      <a-switch
        v-model:checked="form.autoMigrate"
        :checked="autoMigrate"
        @change="val => { autoMigrate = val }"/>
    </a-form-item>
    <a-form-item name="shrinkOk" ref="shrinkOk" :label="$t('label.shrinkok')">
      <a-switch
        v-model:checked="form.shrinkOk"
        :checked="shrinkOk"
        @change="val => { shrinkOk = val }"/>
    </a-form-item>
    <a-divider />
    <div class="actions">
      <a-button @click="closeModal">
        {{ $t('label.cancel') }}
      </a-button>
      <a-button type="primary" @click="submitChangeOfferingForVolume">
        {{ $t('label.ok') }}
      </a-button>
    </div>
  </a-form>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ChangeOfferingForVolume',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      diskOfferings: [],
      autoMigrate: true,
      shrinkOk: false,
      selectedDiskOfferingId: null,
      size: null,
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('changeOfferingForVolume')
  },
  created () {
    this.initForm()
    this.fetchDiskOfferings()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        diskofferingid: this.selectedDiskOfferingId
      })
      this.rules = reactive({
        diskofferingid: [{ required: true, message: this.$t('message.error.select') }],
        size: [{ required: true, message: this.$t('message.error.custom.disk.size') }],
        miniops: [{
          type: 'number',
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }],
        maxiops: [{
          type: 'number',
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }]
      })
    },
    fetchDiskOfferings () {
      api('listDiskOfferings', {
        volumeid: this.resource.id,
        listall: true
      }).then(response => {
        this.diskOfferings = response.listdiskofferingsresponse.diskoffering
        if (this.diskOfferings) {
          this.selectedDiskOfferingId = this.diskOfferings[0].id
          this.customDiskOffering = this.diskOfferings[0].iscustomized || false
          this.isCustomizedDiskIOps = this.diskOfferings[0]?.iscustomizediops || false
        }
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    submitChangeOfferingForVolume () {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.loading = true
        const params = {}
        params.diskofferingid = values.diskofferingid
        params.id = this.resource.id
        params.automigrate = values.autoMigrate
        params.shrinkok = values.shrinkOk
        if (values.size) {
          params.size = values.size
        }
        if (values.miniops) {
          params.miniops = values.miniops
        }
        if (values.maxiops) {
          params.maxiops = values.maxiops
        }
        api('changeOfferingForVolume', params).then(response => {
          this.$pollJob({
            jobId: response.changeofferingforvolumeresponse.jobid,
            successMessage: this.$t('message.change.offering.for.volume'),
            successMethod: () => {
              this.parentFetchData()
            },
            errorMessage: this.$t('message.change.offering.for.volume.failed'),
            errorMethod: () => {
              this.parentFetchData()
            },
            loadingMessage: this.$t('message.change.offering.for.volume.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.parentFetchData()
            }
          })
          this.closeModal()
          this.parentFetchData()
        }).catch(error => {
          this.$notifyError(error)
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    onChangeDiskOffering (id) {
      const offering = this.diskOfferings.filter(x => x.id === id)
      this.customDiskOffering = offering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = offering[0]?.iscustomizediops || false
    }
  }
}
</script>

<style scoped lang="scss">
  .change-offering-for-volume-container {
    width: 85vw;

    @media (min-width: 760px) {
      width: 500px;
    }
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }
</style>
