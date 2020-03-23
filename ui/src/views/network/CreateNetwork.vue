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
    <a-tabs defaultActiveKey="1" :animated="false">
      <a-tab-pane :tab="$t('Isolated')" key="1" v-if="this.isAdvancedZoneWithoutSGAvailable()">
        <CreateIsolatedNetworkForm
          :loading="loading"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('L2')" key="2">
        <CreateL2NetworkForm
          :loading="loading"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('Shared')" key="3" v-if="this.isAdmin()">
        <CreateSharedNetworkForm
          :loading="loading"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { api } from '@/api'
import CreateIsolatedNetworkForm from '@/views/network/CreateIsolatedNetworkForm'
import CreateL2NetworkForm from '@/views/network/CreateL2NetworkForm'
import CreateSharedNetworkForm from '@/views/network/CreateSharedNetworkForm'

export default {
  name: 'CreateNetwork',
  components: {
    CreateIsolatedNetworkForm,
    CreateL2NetworkForm,
    CreateSharedNetworkForm
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      defaultNetworkTypeTabKey: '1',
      loading: false,
      actionZones: [],
      actionZoneLoading: false
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      this.fetchActionZoneData()
    },
    isAdmin () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchActionZoneData () {
      const params = {}
      params.listAll = true
      this.actionZonesLoading = true
      api('listZones', params).then(json => {
        this.actionZones = json.listzonesresponse.zone
      }).finally(() => {
        this.actionZoneLoading = false
        this.loading = false
      })
    },
    isAdvancedZoneWithoutSGAvailable () {
      for (const i in this.actionZones) {
        const zone = this.actionZones[i]
        if (zone.networktype === 'Advanced' && zone.securitygroupsenabled !== true) {
          return true
        }
      }
      return true
    },
    handleRefresh () {
      this.fetchData()
    },
    refreshParent () {
      this.$emit('refresh-data')
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="scss">
  .form-layout {
    width: 600px;
  }

  .random {
    width: 80%;
  }

  .action-button {
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
