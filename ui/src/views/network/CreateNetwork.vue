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
  <a-spin :spinning="loading" class="form-layout">
    <a-tabs v-model:activeKey="defaultNetworkTypeTabKey" :animated="false" v-if="!loading">
      <a-tab-pane :tab="$t('label.isolated')" key="1" v-if="isAdvancedZoneWithoutSGAvailable">
        <CreateIsolatedNetworkForm
          :loading="loading"
          :resource="resource"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.l2')" key="3">
        <CreateL2NetworkForm
          :loading="loading"
          :resource="resource"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.shared')" key="2">
        <CreateSharedNetworkForm
          :loading="loading"
          :resource="resource"
          @close-action="closeAction"
          @refresh-data="refreshParent"
          @refresh="handleRefresh"/>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
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
      isAdvancedZoneWithoutSGAvailable: false,
      defaultNetworkTypeTabKey: '1',
      loading: false,
      actionZones: [],
      actionZoneLoading: false
    }
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.fetchData()
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      const promises = []
      promises.push(this.fetchActionZoneData())
      Promise.all(promises).then(() => {
        this.isAdvancedZoneWithoutSGAvailable = false
        this.defaultNetworkTypeTabKey = '2'

        for (const i in this.actionZones) {
          const zone = this.actionZones[i]
          if (zone.networktype === 'Advanced' && zone.securitygroupsenabled !== true) {
            this.isAdvancedZoneWithoutSGAvailable = true
            this.defaultNetworkTypeTabKey = '1'
            return
          }
        }
      })
    },
    fetchActionZoneData () {
      this.loading = true
      const params = {}
      if (this.resource.zoneid && this.$route.name === 'deployVirtualMachine') {
        params.id = this.resource.zoneid
      }
      this.actionZoneLoading = true
      return api('listZones', params).then(json => {
        this.actionZones = json.listzonesresponse.zone
      }).finally(() => {
        this.actionZoneLoading = false
        this.loading = false
      })
    },
    handleRefresh () {
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
    width: 80vw;
    @media (min-width: 700px) {
      width: 600px;
    }
  }
</style>
