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
        <a-form-item name="provider" ref="provider" :label="$t('label.providername')">
          <a-select
            v-model:value="form.provider"
            @change="val => { form.provider = val }"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              :value="prov"
              v-for="(prov,idx) in providers"
              :key="idx"
            >{{ prov }}</a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="!['Swift', 'S3'].includes(form.provider)">
          <a-form-item name="zone" ref="zone" :label="$t('label.zone')">
            <a-select
              v-model:value="form.zone"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                :value="zone.id"
                v-for="(zone) in zones"
                :key="zone.id"
                :label="zone.name">
                <span>
                  <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ zone.name }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="server" ref="server" :label="$t('label.server')">
            <a-input v-model:value="form.server" />
          </a-form-item>
          <a-form-item name="path" ref="path" :label="$t('label.path')">
            <a-input v-model:value="form.path" />
          </a-form-item>
        </div>
        <div v-if="form.provider === 'SMB/CIFS'">
          <a-form-item name="smbUsername" ref="smbUsername" :label="$t('label.smbusername')">
            <a-input v-model:value="form.smbUsername" />
          </a-form-item>
          <a-form-item name="smbPassword" ref="smbPassword" :label="$t('label.smbpassword')">
            <a-input-password v-model:value="form.smbPassword" />
          </a-form-item>
          <a-form-item name="smbDomain" ref="smbDomain" :label="$t('label.smbdomain')">
            <a-input v-model:value="form.smbDomain" />
          </a-form-item>
        </div>
        <div v-if="form.provider === 'Swift'">
          <a-form-item name="url" ref="url" :label="$t('label.url')">
            <a-input v-model:value="form.url" />
          </a-form-item>
          <a-form-item name="account" ref="account" :label="$t('label.account')">
            <a-input v-model:value="form.account" />
          </a-form-item>
          <a-form-item name="username" ref="username" :label="$t('label.username')">
            <a-input v-model:value="form.username" />
          </a-form-item>
          <a-form-item name="key" ref="key" :label="$t('label.key')">
            <a-input v-model:value="form.key" />
          </a-form-item>
          <a-form-item name="storagepolicy" ref="storagepolicy" :label="$t('label.storagepolicy')">
            <a-input v-model:value="form.storagepolicy" />
          </a-form-item>
        </div>
        <div v-if="form.provider === 'S3'">
          <a-form-item name="zone" ref="zone" :label="$t('label.zone')">
            <a-select
              v-model:value="form.zone"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                :value="zone.id"
                v-for="(zone) in zones"
                :key="zone.id"
                :label="zone.name">
                <span>
                  <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <global-outlined v-else style="margin-right: 5px" />
                  {{ zone.name }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="secondaryStorageAccessKey" ref="secondaryStorageAccessKey" :label="$t('label.s3.access.key')">
            <a-input v-model:value="form.secondaryStorageAccessKey" />
          </a-form-item>
          <a-form-item name="secondaryStorageSecretKey" ref="secondaryStorageSecretKey" :label="$t('label.s3.secret.key')">
            <a-input v-model:value="form.secondaryStorageSecretKey" />
          </a-form-item>
          <a-form-item name="secondaryStorageBucket" ref="secondaryStorageBucket" :label="$t('label.s3.bucket')">
            <a-input v-model:value="form.secondaryStorageBucket"/>
          </a-form-item>
          <a-form-item name="secondaryStorageEndpoint" ref="secondaryStorageEndpoint" :label="$t('label.s3.endpoint')">
            <a-input v-model:value="form.secondaryStorageEndpoint"/>
          </a-form-item>
          <a-form-item name="secondaryStorageHttps" ref="secondaryStorageHttps" :label="$t('label.s3.use.https')">
            <a-switch v-model:checked="form.secondaryStorageHttps" />
          </a-form-item>
          <a-form-item name="secondaryStorageConnectionTimeout" ref="secondaryStorageConnectionTimeout" :label="$t('label.s3.connection.timeout')">
            <a-input v-model:value="form.secondaryStorageConnectionTimeout"/>
          </a-form-item>
          <a-form-item name="secondaryStorageMaxError" ref="secondaryStorageMaxError" :label="$t('label.s3.max.error.retry')">
            <a-input v-model:value="form.secondaryStorageMaxError"/>
          </a-form-item>
          <a-form-item name="secondaryStorageSocketTimeout" ref="secondaryStorageSocketTimeout" :label="$t('label.s3.socket.timeout')">
            <a-input v-model:value="form.secondaryStorageSocketTimeout"/>
          </a-form-item>
          <a-form-item name="secondaryStorageNFSStaging" ref="secondaryStorageNFSStaging" :label="$t('label.create.nfs.secondary.staging.storage')">
            <a-switch v-model:checked="form.secondaryStorageNFSStaging" @change="val => secondaryStorageNFSStaging = val" />
          </a-form-item>
        </div>
        <div v-if="secondaryStorageNFSStaging">
          <a-form-item name="secondaryStorageNFSServer" ref="secondaryStorageNFSServer" :label="$t('label.s3.nfs.server')">
            <a-input v-model:value="form.secondaryStorageNFSServer" />
          </a-form-item>
          <a-form-item name="secondaryStorageNFSPath" ref="secondaryStorageNFSPath" :label="$t('label.s3.nfs.path')">
            <a-input v-model:value="form.secondaryStorageNFSPath"/>
          </a-form-item>
        </div>
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

export default {
  name: 'AddSecondryStorage',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon
  },
  inject: ['parentFetchData'],
  data () {
    return {
      providers: ['NFS', 'SMB/CIFS', 'S3', 'Swift'],
      zones: [],
      loading: false,
      secondaryStorageNFSStaging: false
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        provider: 'NFS',
        secondaryStorageHttps: true
      })
      this.rules = reactive({
        zone: [{ required: true, message: this.$t('label.required') }],
        server: [{ required: true, message: this.$t('label.required') }],
        path: [{ required: true, message: this.$t('label.required') }],
        smbUsername: [{ required: true, message: this.$t('label.required') }],
        smbPassword: [{ required: true, message: this.$t('label.required') }],
        smbDomain: [{ required: true, message: this.$t('label.required') }],
        url: [{ required: true, message: this.$t('label.required') }],
        account: [{ required: true, message: this.$t('label.required') }],
        username: [{ required: true, message: this.$t('label.required') }],
        key: [{ required: true, message: this.$t('label.required') }],
        secondaryStorageAccessKey: [{ required: true, message: this.$t('label.required') }],
        secondaryStorageSecretKey: [{ required: true, message: this.$t('label.required') }],
        secondaryStorageBucket: [{ required: true, message: this.$t('label.required') }],
        secondaryStorageNFSServer: [{ required: true, message: this.$t('label.required') }],
        secondaryStorageNFSPath: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.listZones()
    },
    closeModal () {
      this.$emit('close-action')
    },
    listZones () {
      api('listZones', { showicon: true }).then(json => {
        if (json && json.listzonesresponse && json.listzonesresponse.zone) {
          this.zones = json.listzonesresponse.zone
          if (this.zones.length > 0) {
            this.form.zone = this.zones[0].id || ''
          }
        }
      })
    },
    nfsURL (server, path) {
      var url
      if (path.substring(0, 1) !== '/') {
        path = '/' + path
      }
      if (server.indexOf('://') === -1) {
        url = 'nfs://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    smbURL (server, path, smbUsername, smbPassword, smbDomain) {
      var url = ''
      if (path.substring(0, 1) !== '/') {
        path = '/' + path
      }
      if (server.indexOf('://') === -1) {
        url += 'cifs://'
      }
      url += (server + path)
      return url
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          name: values.name
        }
        var url = ''
        var provider = values.provider
        if (provider === 'NFS') {
          url = this.nfsURL(values.server, values.path)
        }
        if (provider === 'SMB/CIFS') {
          provider = 'SMB'
          url = this.smbURL(values.server, values.path, values.smbUsername, values.smbPassword, values.smbDomain)
          const smbParams = {
            user: values.smbUsername,
            password: values.smbPassword,
            domain: values.smbDomain
          }
          Object.keys(smbParams).forEach((key, index) => {
            data['details[' + index.toString() + '].key'] = key
            data['details[' + index.toString() + '].value'] = smbParams[key]
          })
        }
        if (provider === 'Swift') {
          url = values.url
          const swiftParams = {
            account: values.account,
            username: values.username,
            key: values.key,
            storagepolicy: values.storagepolicy
          }
          Object.keys(swiftParams).forEach((key, index) => {
            data['details[' + index.toString() + '].key'] = key
            data['details[' + index.toString() + '].value'] = swiftParams[key]
          })
        } else if (provider === 'S3') {
          let detailIdx = 0
          const s3Params = {
            accesskey: values.secondaryStorageAccessKey,
            secretkey: values.secondaryStorageSecretKey,
            bucket: values.secondaryStorageBucket,
            usehttps: values?.secondaryStorageHttps || false
          }
          Object.keys(s3Params).forEach((key, index) => {
            data['details[' + index.toString() + '].key'] = key
            data['details[' + index.toString() + '].value'] = s3Params[key]
            detailIdx = index
          })

          if (values.secondaryStorageEndpoint && values.secondaryStorageEndpoint.length > 0) {
            detailIdx++
            data['details[' + detailIdx.toString() + '].key'] = 'endpoint'
            data['details[' + detailIdx.toString() + '].value'] = values.secondaryStorageEndpoint
          }

          if (values.secondaryStorageConnectionTimeout && values.secondaryStorageConnectionTimeout.length > 0) {
            detailIdx++
            data['details[' + detailIdx.toString() + '].key'] = 'connectiontimeout'
            data['details[' + detailIdx.toString() + '].value'] = values.secondaryStorageConnectionTimeout
          }

          if (values.secondaryStorageMaxError && values.secondaryStorageMaxError.length > 0) {
            detailIdx++
            data['details[' + detailIdx.toString() + '].key'] = 'maxerrorretry'
            data['details[' + detailIdx.toString() + '].value'] = values.secondaryStorageMaxError
          }

          if (values.secondaryStorageSocketTimeout && values.secondaryStorageSocketTimeout.length > 0) {
            detailIdx++
            data['details[' + detailIdx.toString() + '].key'] = 'sockettimeout'
            data['details[' + detailIdx.toString() + '].value'] = values.secondaryStorageSocketTimeout
          }
        }

        if (provider !== 'S3') {
          data.url = url
        }
        data.provider = provider
        if (values.zone && !['Swift', 'S3'].includes(provider)) {
          data.zoneid = values.zone
        }

        const nfsParams = {}
        if (values.secondaryStorageNFSStaging) {
          const nfsServer = values.secondaryStorageNFSServer
          const path = values.secondaryStorageNFSPath
          const nfsUrl = this.nfsURL(nfsServer, path)

          nfsParams.provider = 'nfs'
          nfsParams.zoneid = values.zone
          nfsParams.url = nfsUrl
        }

        this.loading = true

        try {
          await this.addImageStore(data)

          if (values.secondaryStorageNFSStaging) {
            await this.createSecondaryStagingStore(nfsParams)
          }
          this.$notification.success({
            message: this.$t('label.add.secondary.storage'),
            description: this.$t('label.add.secondary.storage')
          })
          this.loading = false
          this.closeModal()
          this.parentFetchData()
        } catch (error) {
          this.$notifyError(error)
          this.loading = false
        }
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    addImageStore (params) {
      return new Promise((resolve, reject) => {
        api('addImageStore', params).then(json => {
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },
    createSecondaryStagingStore (params) {
      return new Promise((resolve, reject) => {
        api('createSecondaryStagingStore', params).then(json => {
          resolve()
        }).catch(error => {
          reject(error)
        })
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
