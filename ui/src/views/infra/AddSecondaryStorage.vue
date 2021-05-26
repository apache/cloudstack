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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-form :form="form" layout="vertical">
        <a-form-item :label="$t('label.name')">
          <a-input v-decorator="['name']" autoFocus />
        </a-form-item>
        <a-form-item :label="$t('label.providername')">
          <a-select
            v-decorator="[
              'provider',
              {
                initialValue: 'NFS'
              }]"
            @change="val => { this.provider = val }"
          >
            <a-select-option
              :value="prov"
              v-for="(prov,idx) in providers"
              :key="idx"
            >{{ prov }}</a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="provider !== 'Swift'">
          <a-form-item :label="$t('label.zone')">
            <a-select
              v-decorator="[
                'zone',
                {
                  initialValue: this.zoneSelected,
                  rules: [{ required: true, message: `${$t('label.required')}`}]
                }]"
              @change="val => { zoneSelected = val }"
            >
              <a-select-option
                :value="zone.id"
                v-for="(zone) in zones"
                :key="zone.id"
              >{{ zone.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('label.server')">
            <a-input
              v-decorator="[
                'server',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.path')">
            <a-input
              v-decorator="[
                'path',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
        </div>
        <div v-if="provider === 'SMB/CIFS'">
          <a-form-item :label="$t('label.smbusername')">
            <a-input
              v-decorator="[
                'smbUsername',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.smbpassword')">
            <a-input-password
              v-decorator="[
                'smbPassword',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.smbdomain')">
            <a-input
              v-decorator="[
                'smbDomain',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
        </div>
        <div v-if="provider === 'Swift'">
          <a-form-item :label="$t('label.url')">
            <a-input
              v-decorator="[
                'url',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.account')">
            <a-input
              v-decorator="[
                'account',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.username')">
            <a-input
              v-decorator="[
                'username',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.key')">
            <a-input
              v-decorator="[
                'key',
                {
                  rules: [{ required: true, message: `${$t('label.required')}` }]
                }]"
            />
          </a-form-item>
          <a-form-item :label="$t('label.storagepolicy')">
            <a-input
              v-decorator="[
                'storagepolicy'
              ]"
            />
          </a-form-item>
        </div>
        <div class="actions">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>
<script>
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
      provider: '',
      zones: [],
      zoneSelected: '',
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  methods: {
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
            this.zoneSelected = this.zones[0].id || ''
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
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }

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
