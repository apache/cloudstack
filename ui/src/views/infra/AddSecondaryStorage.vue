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
      <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical" @finish="handleSubmit">
        <a-form-item name="name" ref="name" :label="$t('label.name')">
          <a-input v-model:value="form.name" autoFocus />
        </a-form-item>
        <a-form-item name="" ref="" :label="$t('label.providername')">
          <a-select
            v-model:value="form.provider"
            @change="val => { form.provider = val }"
          >
            <a-select-option
              :value="prov"
              v-for="(prov,idx) in providers"
              :key="idx"
            >{{ prov }}</a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="form.provider !== 'Swift'">
          <a-form-item name="zone" ref="zone" :label="$t('label.zone')">
            <a-select v-model:value="form.zone">
              <a-select-option
                :value="zone.id"
                v-for="(zone) in zones"
                :key="zone.id"
              >{{ zone.name }}</a-select-option>
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

export default {
  name: 'AddSecondryStorage',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      providers: ['NFS', 'SMB/CIFS', 'Swift'],
      zones: [],
      loading: false
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
        provider: 'NFS'
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
        key: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.listZones()
    },
    closeModal () {
      this.$parent.$parent.close()
    },
    listZones () {
      api('listZones').then(json => {
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
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

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
        }

        data.url = url
        data.provider = provider
        if (values.zone && provider !== 'Swift') {
          data.zoneid = values.zone
        }

        this.loading = true
        api('addImageStore', data).then(json => {
          this.$notification.success({
            message: this.$t('label.add.secondary.storage'),
            description: this.$t('label.add.secondary.storage')
          })
          this.closeModal()
          this.parentFetchData()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
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
