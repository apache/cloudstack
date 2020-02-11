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
  <a-spin :spinning="fetchLoading">
    TODO: implement support for config drive, vpc router, ilbvm, virtual router
    <a-tabs :tabPosition="device === 'mobile' ? 'top' : 'left'" :animated="false">
      <a-tab-pane v-for="(nsp, index) in nsps" :key="index">
        <span slot="tab">
          {{ nsp.name }}
          <status :text="nsp.state" style="margin-bottom: 6px" />
        </span>
        <router-link :to="{ path: '/nsp/' + nsp.id + '?name=' + nsp.name + '&physicalnetworkid=' + resource.id }">{{ nsp.name }} </router-link>
        {{ nsp }}
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import Status from '@/components/widgets/Status'

export default {
  name: 'ServiceProvidersTab',
  components: {
    Status
  },
  mixins: [mixinDevice],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      nsps: [],
      fetchLoading: false
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      this.fetchLoading = true
      api('listNetworkServiceProviders', { physicalnetworkid: this.resource.id }).then(json => {
        this.nsps = json.listnetworkserviceprovidersresponse.networkserviceprovider || []
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
