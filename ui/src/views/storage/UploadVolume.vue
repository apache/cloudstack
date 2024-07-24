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
    <span v-if="uploadPercentage > 0">
      <loading-outlined />
      {{ $t('message.upload.file.processing') }}
      <a-progress :percent="uploadPercentage" />
    </span>
    <a-spin :spinning="loading" v-else>
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="url" ref="url" :label="$t('label.url')">
          <a-input
            v-model:value="form.url"
            :placeholder="apiParams.url.description"/>
        </a-form-item>
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="$t('label.volumename')"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="zoneId" ref="zoneId">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            v-model:value="form.zoneId"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option :value="zone.id" v-for="zone in zones" :key="zone.id" :label="zone.name || zone.description">
              <span>
                <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px"/>
                {{ zone.name || zone.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="format" ref="format">
          <template #label>
            <tooltip-label :title="$t('label.format')" :tooltip="apiParams.format.description"/>
          </template>
          <a-select
            v-model:value="form.format"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="format in formats" :key="format">
              {{ format }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="diskofferingid" ref="diskofferingid">
          <template #label>
            <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || $t('label.diskoffering')"/>
          </template>
          <a-select
            v-model:value="form.diskofferingid"
            :loading="loading"
            @change="id => onChangeDiskOffering(id)"
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
        <a-form-item name="checksum" ref="checksum">
          <template #label>
            <tooltip-label :title="$t('label.volumechecksum')" :tooltip="apiParams.checksum.description"/>
          </template>
          <a-input
            v-model:value="form.checksum"
            :placeholder="$t('label.volumechecksum.description')"
          />
        </a-form-item>
        <a-form-item name="domainid" ref="domainid" v-if="'listDomains' in $store.getters.apis">
          <template #label>
            <tooltip-label :title="$t('label.domain')" :tooltip="apiParams.domainid.description"/>
          </template>
          <a-select
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="$t('label.domainid')"
            @change="val => { handleDomainChange(domainList[val].id) }">
            <a-select-option v-for="(opt, optIndex) in domainList" :key="optIndex" :label="opt.path || opt.name || opt.description">
              {{ opt.path || opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="'listDomains' in $store.getters.apis">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <a-select
            v-model:value="form.account"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="$t('label.account')"
            @change="val => { handleAccountChange(val) }">
            <a-select-option v-for="(acc, index) in accountList" :value="acc.name" :key="index">
              {{ acc.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
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
  name: 'UploadVolume',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel
  },
  data () {
    return {
      zones: [],
      domainList: [],
      accountList: [],
      formats: ['RAW', 'VHD', 'VHDX', 'OVA', 'QCOW2'],
      offerings: [],
      zoneSelected: '',
      selectedDiskOfferingId: null,
      domainId: null,
      account: null,
      uploadParams: null,
      domainLoading: false,
      loading: false,
      uploadPercentage: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('uploadVolume')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        format: this.formats[0]
      })
      this.rules = reactive({
        url: [{ required: true, message: this.$t('message.error.required.input') }],
        name: [{ required: true, message: this.$t('message.error.volume.name') }],
        zoneId: [{ required: true, message: this.$t('message.error.select') }],
        format: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.loading = true
      api('listZones', { showicon: true }).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.zones = this.zones.filter(zone => zone.type !== 'Edge')
        this.form.zoneId = this.zones[0].id || ''
        this.fetchDiskOfferings(this.form.zoneId)
      }).finally(() => {
        this.loading = false
      })
      if ('listDomains' in this.$store.getters.apis) {
        this.fetchDomains()
      }
    },
    fetchDiskOfferings (zoneId) {
      this.loading = true
      api('listDiskOfferings', {
        zoneid: zoneId,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
      }).finally(() => {
        this.loading = false
      })
    },
    fetchDomains () {
      this.domainLoading = true
      api('listDomains', {
        listAll: true,
        details: 'min'
      }).then(response => {
        this.domainList = response.listdomainsresponse.domain

        if (this.domainList[0]) {
          this.handleDomainChange(null)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchAccounts () {
      api('listAccounts', {
        domainid: this.domainId
      }).then(response => {
        this.accountList = response.listaccountsresponse.account || []
        if (this.accountList && this.accountList.length === 0) {
          this.handleAccountChange(null)
        }
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    onChangeDiskOffering (id) {
      const offering = this.offerings.filter(x => x.id === id)
      this.customDiskOffering = offering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = offering[0]?.iscustomizediops || false
    },
    handleDomainChange (domain) {
      this.domainId = domain
      if ('listAccounts' in this.$store.getters.apis) {
        this.fetchAccounts()
      }
    },
    handleAccountChange (acc) {
      if (acc) {
        this.account = acc.name
      } else {
        this.account = acc
      }
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        params.domainId = this.domainId
        this.loading = true
        api('uploadVolume', params).then(json => {
          this.$notification.success({
            message: this.$t('message.success.upload'),
            description: this.$t('message.success.upload.volume.description')
          })
          this.closeAction()
          this.$emit('refresh-data')
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 700px) {
    width: 550px;
  }
}
</style>
