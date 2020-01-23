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
    <a-tabs :animated="false" defaultActiveKey="0" tabPosition="left" type="card">
      <a-tab-pane v-for="(item, index) in traffictypes" :tab="item.traffictype" :key="index">
        <div>
          <strong>{{ $t('id') }}</strong> {{ item.id }}
        </div>
        <div v-for="(type, idx) in ['kvmnetworklabel', 'vmwarenetworklabel', 'xennetworklabel', 'hypervnetworklabel', 'ovm3networklabel']" :key="idx">
          <strong>{{ $t(type) }}</strong>
          {{ item[type] || 'Use default gateway' }}
        </div>
        <div v-if="item.traffictype === 'Public'">
          Insert here form/component to manage public IP ranges
          <IpRangesTab :resource="resource" />
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import IpRangesTab from './IpRangesTab'

export default {
  name: 'NspTab',
  components: {
    IpRangesTab,
    Status
  },
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
      traffictypes: [],
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
      api('listTrafficTypes', { physicalnetworkid: this.resource.id }).then(json => {
        this.traffictypes = json.listtraffictypesresponse.traffictype
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
