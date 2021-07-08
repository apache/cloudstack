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
  <a-tooltip placement="bottom" :title="$t(getTooltip(text))">
    <a-badge
      style="display: inline-flex"
      :title="text"
      :color="getStatusColor(text)"
      :status="getBadgeStatus(text)"
      :text="getText()" />
  </a-tooltip>
</template>

<script>

export default {
  name: 'Status',
  props: {
    text: {
      type: String,
      required: true
    },
    displayText: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    getText () {
      if (this.displayText && this.text) {
        var state = this.text
        switch (state) {
          case 'Running':
            state = this.$t('state.running')
            break
          case 'Stopped':
            state = this.$t('state.stopped')
            break
          case 'Starting':
            state = this.$t('state.starting')
            break
          case 'Stopping':
            state = this.$t('state.stopping')
            break
          case 'Suspended':
            state = this.$t('state.suspended')
            break
          case 'Pending':
            state = this.$t('state.pending')
            break
          case 'Migrating':
            state = this.$t('state.migrating')
            break
          case 'Expunging':
            state = this.$t('state.expunging')
            break
          case 'Error':
            state = this.$t('state.error')
            break
          case 'ReadOnly':
            state = this.$t('state.readonly')
            break
          case 'ReadWrite':
            state = this.$t('state.readwrite')
            break
        }
        return state.charAt(0).toUpperCase() + state.slice(1)
      }
      return ''
    },
    getBadgeStatus (state) {
      var status = 'default'
      switch (state) {
        case 'Active':
        case 'BackedUp':
        case 'Completed':
        case 'Connected':
        case 'Download Complete':
        case 'Enabled':
        case 'Implemented':
        case 'Ready':
        case 'Running':
        case 'Setup':
        case 'Started':
        case 'Successfully Installed':
        case 'ReadWrite':
        case 'True':
        case 'Up':
        case 'enabled':
          status = 'success'
          break
        case 'Alert':
        case 'Declined':
        case 'Disabled':
        case 'Disconnected':
        case 'Down':
        case 'Error':
        case 'False':
        case 'Stopped':
          status = 'error'
          break
        case 'Migrating':
        case 'Scaling':
        case 'Starting':
        case 'Stopping':
        case 'Upgrading':
          status = 'processing'
          break
        case 'Allocated':
          if (this.$route.path.startsWith('/publicip')) {
            status = 'success'
          } else {
            status = 'warning'
          }
          break
        case 'Created':
        case 'Maintenance':
        case 'Pending':
        case 'ReadOnly':
          status = 'warning'
          break
      }
      return status
    },
    getStatusColor (state) {
      if (state === 'Scheduled') {
        return 'blue'
      }

      return null
    },
    getTooltip (state) {
      if (!(state && this.displayText)) {
        return
      }
      if (this.$route.path === '/vmsnapshot' || this.$route.path.includes('/vmsnapshot/')) {
        return 'message.vmsnapshot.state.' + state.toLowerCase()
      }
      if (this.$route.path === '/vm' || this.$route.path.includes('/vm/')) {
        return 'message.vm.state.' + state.toLowerCase()
      }
      if (this.$route.path === '/volume' || this.$route.path.includes('/volume/')) {
        return 'message.volume.state.' + state.toLowerCase()
      }
      if (this.$route.path === '/guestnetwork' || this.$route.path.includes('/guestnetwork/')) {
        return 'message.guestnetwork.state.' + state.toLowerCase()
      }
      if (this.$route.path === '/publicip' || this.$route.path.includes('/publicip/')) {
        return 'message.publicip.state.' + state.toLowerCase()
      }
      // Nothing for snapshots, vpcs, gateways, vnpnconn, vpnuser, kubectl, event, project, account, infra. They're all self explanatory
      return state
    }
  }
}
</script>

<style scoped>
/deep/ .ant-badge-status-dot {
  width: 12px;
  height: 12px;
  margin-top: 5px;
}
</style>
