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
      <a-button
        style="margin-left: 10px"
        type="primary"
        danger
        @click="disableVpn = true"
        :disabled="!('deleteRemoteAccessVpn' in $store.getters.apis)">
        {{ $t('label.disable.vpn') }}
      </a-button>
      <a-button><router-link :to="{ path: '/vpnuser'}">{{ $t('label.manage.vpn.user') }}</router-link></a-button>
    </div>

    <a-modal
      :visible="disableVpn"
      :footer="null"
      :title="$t('label.disable.vpn')"
      :closable="true"
      :maskClosable="false"
      @cancel="disableVpn = false">
      <div v-ctrl-enter="handleDisableVpn">
        <p>{{ $t('message.disable.vpn') }}</p>

        <a-divider />

        <div class="actions">
          <a-button @click="() => disableVpn = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleDisableVpn">{{ $t('label.yes') }}</a-button>
        </div>
      </div>
    </a-modal>

  </div>
  <div v-else>
    <a-button
      :disabled="!('createRemoteAccessVpn' in $store.getters.apis)"
      type="primary"
      style="margin-left: 10px"
      @click="enableVpn = true">
      {{ $t('label.enable.vpn') }}
    </a-button>

    <a-modal
      :visible="enableVpn"
      :footer="null"
      :title="$t('label.enable.vpn')"
      :maskClosable="false"
      :closable="true"
      @cancel="enableVpn = false">
      <div v-ctrl-enter="handleCreateVpn">
        <p>{{ $t('message.enable.vpn') }}</p>

        <a-divider />

        <div class="actions">
          <a-button @click="() => enableVpn = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleCreateVpn">{{ $t('label.yes') }}</a-button>
        </div>
      </div>
    </a-modal>

  </div>

  <br>
  <div v-if="vpnGateway">
    <div>
      <a-button
        :disabled="!('deleteVpnGateway' in $store.getters.apis)"
        style="margin-left: 10px"
        danger
        type="primary"
        @click="deleteVpnGateway = true">
        {{ $t('label.delete.vpn.gateway') }}
      </a-button>
    </div>
    <a-modal
      :visible="deleteVpnGateway"
      :footer="null"
      :title="$t('label.enable.vpn')"
      :maskClosable="false"
      :closable="true"
      @cancel="deleteVpnGateway = false">
      <div v-ctrl-enter="handleDeleteVpnGateway">
        <p>{{ $t('message.delete.vpn.gateway') }}</p>
        <div :span="24" class="action-button">
          <a-button @click="deleteVpnGateway = false">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleDeleteVpnGateway" ref="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
  <div v-else-if="vpnGatewayEnabled">
    <div>
      <a-button
        :disabled="!('createVpnGateway' in $store.getters.apis)"
        style="margin-left: 10px"
        type="primary"
        @click="createVpnGateway = true">
        {{ $t('label.add.vpn.gateway') }}
      </a-button>
    </div>
    <a-modal
      :visible="createVpnGateway"
      :footer="null"
      :title="$t('label.add.vpn.gateway')"
      :maskClosable="false"
      :closable="true"
      @cancel="createVpnGateway = false">
      <div v-ctrl-enter="handleCreateVpnGateway">
        <p>{{ $t('message.add.vpn.gateway') }}</p>
        <div :span="24" class="action-button">
          <a-button @click="createVpnGateway = false">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" type="primary" @click="handleCreateVpnGateway" ref="submit">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>

</template>

<script>
import { getAPI, postAPI } from '@/api'

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
      disableVpn: false,
      vpnGateway: null,
      vpnGatewayEnabled: false,
      createVpnGateway: false,
      deleteVpnGateway: false,
      isSubmitted: false
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      getAPI('listRemoteAccessVpns', {
        publicipid: this.resource.id,
        listAll: true
      }).then(response => {
        this.remoteAccessVpn = response.listremoteaccessvpnsresponse.remoteaccessvpn
          ? response.listremoteaccessvpnsresponse.remoteaccessvpn[0] : null
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      })
      if (this.resource.vpcid) {
        this.vpnGatewayEnabled = true
        getAPI('listVpnGateways', {
          vpcid: this.resource.vpcid,
          listAll: true
        }).then(response => {
          const vpnGateways = response.listvpngatewaysresponse.vpngateway || []
          for (const vpnGateway of vpnGateways) {
            if (vpnGateway.publicip === this.resource.ipaddress) {
              this.vpnGateway = vpnGateway
            }
          }
        }).catch(error => {
          console.log(error)
          this.$notifyError(error)
        })
      }
    },
    handleCreateVpn () {
      if (this.isSubmitted) return
      this.isSubmitted = true
      this.parentToggleLoading()
      this.enableVpn = false
      postAPI('createRemoteAccessVpn', {
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
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          errorMessage: this.$t('message.enable.vpn.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.enable.vpn.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
        this.isSubmitted = false
      })
    },
    handleDisableVpn () {
      if (this.isSubmitted) return
      this.isSubmitted = true
      this.parentToggleLoading()
      this.disableVpn = false
      postAPI('deleteRemoteAccessVpn', {
        publicipid: this.resource.id,
        domainid: this.resource.domainid
      }).then(response => {
        this.$pollJob({
          jobId: response.deleteremoteaccessvpnresponse.jobid,
          successMessage: this.$t('message.success.disable.vpn'),
          successMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          errorMessage: this.$t('message.disable.vpn.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.disable.vpn.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
        this.isSubmitted = false
      })
    },
    handleCreateVpnGateway () {
      if (this.isSubmitted) return
      this.isSubmitted = true
      this.parentToggleLoading()
      this.createVpnGateway = false
      const params = {
        vpcid: this.resource.vpcid,
        ipaddressid: this.resource.id
      }
      postAPI('createVpnGateway', params).then(response => {
        this.$pollJob({
          jobId: response.createvpngatewayresponse.jobid,
          successMessage: this.$t('message.success.add.vpn.gateway'),
          successMethod: result => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          errorMessage: this.$t('message.add.vpn.gateway.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.add.vpn.gateway.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
        this.isSubmitted = false
      })
    },
    handleDeleteVpnGateway () {
      if (this.isSubmitted) return
      this.isSubmitted = true
      this.parentToggleLoading()
      this.deleteVpnGateway = false
      postAPI('deleteVpnGateway', {
        id: this.vpnGateway.id
      }).then(response => {
        this.$pollJob({
          jobId: response.deletevpngatewayresponse.jobid,
          successMessage: this.$t('message.success.delete.vpn.gateway'),
          successMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          errorMessage: this.$t('message.delete.vpn.gateway.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFetchData()
            this.parentToggleLoading()
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.parentFetchData()
        this.parentToggleLoading()
        this.isSubmitted = false
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
