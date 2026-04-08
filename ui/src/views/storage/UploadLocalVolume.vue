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
        <a-form-item :label="$t('label.templatefileupload')" ref="file" name="file">
          <a-upload-dragger
            :multiple="false"
            :fileList="fileList"
            @remove="handleRemove"
            :beforeUpload="beforeUpload"
            v-model:value="form.file">
            <p class="ant-upload-drag-icon">
              <cloud-upload-outlined />
            </p>
            <p class="ant-upload-text" v-if="fileList.length === 0">
              {{ $t('label.volume.volumefileupload.description') }}
            </p>
          </a-upload-dragger>
        </a-form-item>
        <a-form-item ref="name" name="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="$t('label.volumename')"
            v-focus="true" />
        </a-form-item>
        <a-form-item ref="zoneId" name="zoneId">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <infinite-scroll-select
            v-model:value="form.zoneId"
            api="listZones"
            :apiParams="zonesApiParams"
            resourceType="zone"
            optionValueKey="id"
            optionLabelKey="name"
            defaultIcon="global-outlined"
            selectFirstOption="true"
            @change-option-value="handleZoneChange" />
        </a-form-item>
        <a-form-item name="diskofferingid" ref="diskofferingid">
          <template #label>
            <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description"/>
          </template>
          <infinite-scroll-select
            v-model:value="form.diskofferingid"
            api="listDiskOfferings"
            :apiParams="diskOfferingsApiParams"
            resourceType="diskoffering"
            optionValueKey="id"
            optionLabelKey="displaytext"
            defaultIcon="hdd-outlined"
            :defaultOption="{ id: null, displaytext: ''}"
            allowClear="true"
            :placeholder="apiParams.diskofferingid.description"
            @change-option="onChangeDiskOffering" />
        </a-form-item>
        <a-form-item ref="format" name="format">
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
        <a-form-item ref="checksum" name="checksum">
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
          <infinite-scroll-select
            v-model:value="form.domainid"
            api="listDomains"
            :apiParams="domainsApiParams"
            resourceType="domain"
            optionValueKey="id"
            optionLabelKey="path"
            defaultIcon="block-outlined"
            :placeholder="$t('label.domainid')"
            allowClear="true"
            @change-option-value="handleDomainChange" />
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="'listDomains' in $store.getters.apis">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <infinite-scroll-select
            v-model:value="form.account"
            api="listAccounts"
            :apiParams="accountsApiParams"
            resourceType="account"
            optionValueKey="name"
            optionLabelKey="name"
            defaultIcon="team-outlined"
            allowClear="true"
            :placeholder="$t('label.account')"
            @change-option-value="handleAccountChange" />
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
import { getAPI } from '@/api'
import { axios } from '../../utils/request'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect.vue'

export default {
  name: 'UploadLocalVolume',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel,
    InfiniteScrollSelect
  },
  data () {
    return {
      fileList: [],
      formats: ['RAW', 'VHD', 'VHDX', 'OVA', 'QCOW2'],
      domainId: null,
      account: null,
      uploadParams: null,
      customDiskOffering: false,
      isCustomizedDiskIOps: false,
      loading: false,
      uploadPercentage: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('getUploadParamsForVolume')
  },
  computed: {
    zonesApiParams () {
      return {
        showicon: true
      }
    },
    diskOfferingsApiParams () {
      if (!this.form.zoneId) {
        return null
      }
      return {
        zoneid: this.form.zoneId,
        listall: true
      }
    },
    domainsApiParams () {
      return {
        listall: true,
        details: 'min'
      }
    },
    accountsApiParams () {
      if (!this.form.domainid) {
        return null
      }
      return {
        domainid: this.form.domainid
      }
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        file: undefined,
        name: undefined,
        zoneId: undefined,
        format: 'RAW',
        checksum: undefined
      })
      this.rules = reactive({
        file: [{ required: true, message: this.$t('message.error.required.input') }],
        name: [{ required: true, message: this.$t('message.error.volume.name') }],
        zoneId: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    handleZoneChange (zoneId) {
      this.form.zoneId = zoneId
      // InfiniteScrollSelect will auto-reload disk offerings when apiParams changes
    },
    onChangeDiskOffering (offering) {
      if (offering) {
        this.customDiskOffering = offering.iscustomized || false
        this.isCustomizedDiskIOps = offering.iscustomizediops || false
      } else {
        this.customDiskOffering = false
        this.isCustomizedDiskIOps = false
      }
    },
    handleRemove (file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
      this.form.file = undefined
    },
    beforeUpload (file) {
      this.fileList = [...this.fileList, file]
      this.form.file = file
      return false
    },
    handleDomainChange (domainId) {
      this.form.domainid = domainId
      this.domainId = domainId
      this.form.account = null
    },
    handleAccountChange (accountName) {
      this.form.account = accountName
      this.account = accountName
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
          if (key === 'file') {
            continue
          }
          params[key] = input
        }
        params.domainId = this.domainId
        this.loading = true
        getAPI('getUploadParamsForVolume', params).then(json => {
          this.uploadParams = json.postuploadvolumeresponse?.getuploadparams || ''
          const { fileList } = this
          if (this.fileList.length > 1) {
            this.$notification.error({
              message: this.$t('message.upload.volume.failed'),
              description: this.$t('message.upload.file.limit'),
              duration: 0
            })
          }
          const formData = new FormData()
          fileList.forEach(file => {
            formData.append('files[]', file)
          })
          this.uploadPercentage = 0
          axios.post(this.uploadParams.postURL,
            formData,
            {
              headers: {
                'content-type': 'multipart/form-data',
                'x-signature': this.uploadParams.signature,
                'x-expires': this.uploadParams.expires,
                'x-metadata': this.uploadParams.metadata
              },
              onUploadProgress: (progressEvent) => {
                this.uploadPercentage = Number(parseFloat(100 * progressEvent.loaded / progressEvent.total).toFixed(1))
              },
              timeout: 86400000
            }).then((json) => {
            this.$notification.success({
              message: this.$t('message.success.upload'),
              description: this.$t('message.success.upload.volume.description')
            })
            this.closeAction()
          }).catch(e => {
            this.$notification.error({
              message: this.$t('message.upload.failed'),
              description: `${this.$t('message.upload.volume.failed')} -  ${e}`,
              duration: 0
            })
          }).finally(() => {
            this.loading = false
          })
        }).catch(e => {
          this.$notification.error({
            message: this.$t('message.upload.failed'),
            description: `${this.$t('message.upload.volume.failed')} -  ${e?.response?.data?.postuploadvolumeresponse?.errortext || e}`,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
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
