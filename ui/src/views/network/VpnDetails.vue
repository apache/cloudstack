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
      <p>Your Remote Access VPN is currently enabled and can be accessed via the IP <strong>{{ remoteAccessVpn.publicip }}</strong></p>
      <p>Your IPSec pre-shared key is <strong>{{ remoteAccessVpn.presharedkey }}</strong></p>
      <a-divider/>
      <a-button><router-link :to="{ path: '/vpnuser'}">Manage VPN Users</router-link></a-button>
      <a-button style="margin-left: 10px" type="danger" @click="disableVpn = true">Disable VPN</a-button>
    </div>

    <a-modal v-model="disableVpn" :footer="null" oncancel="disableVpn = false" title="Disable Remove Access VPN">
      <p>Are you sure you want to disable VPN?</p>

      <a-divider></a-divider>

      <div class="actions">
        <a-button @click="() => disableVpn = false">Cancel</a-button>
        <a-button type="primary" @click="handleDisableVpn">Yes</a-button>
      </div>
    </a-modal>

  </div>
  <div v-else>
    <a-button type="primary" @click="enableVpn = true">Enable VPN</a-button>

    <a-modal v-model="enableVpn" :footer="null" onCancel="enableVpn = false" title="Enable Remote Access VPN">
      <p>Please confirm that you want Remote Access VPN enabled for this IP address.</p>

      <a-divider></a-divider>

      <div class="actions">
        <a-button @click="() => enableVpn = false">Cancel</a-button>
        <a-button type="primary" @click="handleCreateVpn">Yes</a-button>
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
  mounted () {
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
              message: 'Status',
              description:
                `Your Remote Access VPN is currently enabled and can be accessed via the IP ${res.publicip}. Your IPSec pre-shared key is ${res.presharedkey}`,
              duration: 0
            })
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: 'Failed to enable VPN',
          errorMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: `Enabling VPN...`,
          catchMessage: 'Error encountered while fetching async job result',
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
          successMessage: 'Successfully disabled VPN',
          successMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: 'Failed to disable VPN',
          errorMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: `Disabling VPN...`,
          catchMessage: 'Error encountered while fetching async job result',
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
