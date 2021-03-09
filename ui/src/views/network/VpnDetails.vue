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
  <div v-if="remoteAccessVpn">
    <div>
      <p>{{ $t('message.enabled.vpn') }} <strong>{{ remoteAccessVpn.publicip }}</strong></p>
      <p>{{ $t('message.enabled.vpn.ip.sec') }} <strong>{{ remoteAccessVpn.presharedkey }}</strong></p>
      <a-divider/>
      <a-button><router-link :to="{ path: '/vpnuser'}">{{ $t('label.manage.vpn.user') }}</router-link></a-button>
      <a-button style="margin-left: 10px" type="danger" @click="disableVpn = true" :disabled="!('deleteRemoteAccessVpn' in $store.getters.apis)">
        {{ $t('label.disable.vpn') }}
      </a-button>
    </div>

    <a-modal
      v-model="disableVpn"
      :footer="null"
      oncancel="disableVpn = false"
      :title="$t('label.disable.vpn')"
      :maskClosable="false">
      <p>{{ $t('message.disable.vpn') }}</p>

      <a-divider></a-divider>

      <div class="actions">
        <a-button @click="() => disableVpn = false">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" @click="handleDisableVpn">{{ $t('label.yes') }}</a-button>
      </div>
    </a-modal>

  </div>
  <div v-else>
    <a-button :disabled="!('createRemoteAccessVpn' in $store.getters.apis)" type="primary" @click="enableVpn = true">
      {{ $t('label.enable.vpn') }}
    </a-button>

    <a-modal
      v-model="enableVpn"
      :footer="null"
      onCancel="enableVpn = false"
      :title="$t('label.enable.vpn')"
      :maskClosable="false">
      <p>{{ $t('message.enable.vpn') }}</p>

      <a-divider></a-divider>

      <div class="actions">
        <a-button @click="() => enableVpn = false">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" @click="handleCreateVpn">{{ $t('label.yes') }}</a-button>
      </div>
    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      remoteAccessVpn: null,
      enableVpn: false,
      disableVpn: false
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  created () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      api('listRemoteAccessVpns', {
        publicipid: this.resource.id,
        listAll: true
      }).then(response => {
        this.remoteAccessVpn = response.listremoteaccessvpnsresponse.remoteaccessvpn
          ? response.listremoteaccessvpnsresponse.remoteaccessvpn[0] : null
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      })
    },
    handleCreateVpn () {
      this.parentToggleLoading()
      this.enableVpn = false
      api('createRemoteAccessVpn', {
        publicipid: this.resource.id,
        domainid: this.resource.domainid,
        account: this.resource.account
      }).then(response => {
        this.$pollJob({
          jobId: response.createremoteaccessvpnresponse.jobid,
          successMethod: result => {
            const res = result.jobresult.remoteaccessvpn
            this.$notification.success({
              message: this.$t('label.status'),
              description:
                `${this.$t('message.enabled.vpn')} ${res.publicip}. ${this.$t('message.enabled.vpn.ip.sec')} ${res.presharedkey}`,
              duration: 0
            })
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: this.$t('message.enable.vpn.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: this.$t('message.enable.vpn.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
      })
    },
    handleDisableVpn () {
      this.parentToggleLoading()
      this.disableVpn = false
      api('deleteRemoteAccessVpn', {
        publicipid: this.resource.id,
        domainid: this.resource.domainid
      }).then(response => {
        this.$pollJob({
          jobId: response.deleteremoteaccessvpnresponse.jobid,
          successMessage: this.$t('message.success.disable.vpn'),
          successMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: this.$t('message.disable.vpn.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: this.$t('message.disable.vpn.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .actions {
    display: flex;
    justify-content: flex-end;

    button {
      &:not(:last-child) {
        margin-right: 20px;
      }
    }
  }
</style>
