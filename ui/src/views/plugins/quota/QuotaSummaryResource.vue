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
  <resource-view
    :loading="loading"
    :resource="quotaResource"
    :tabs="tabs"
    :historyTab="activeTab"
    @onTabChange="(tab) => { this.activeTab = tab }"/>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'

import ResourceView from '@/components/view/ResourceView'

export default {
  name: 'QuotaSummaryResource',
  components: {
    ResourceView
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    },
    tabs: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      loading: false,
      quotaResource: {},
      networkService: null,
      pattern: 'YYYY-MM-DD',
      activeTab: ''
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource () {
      if (Object.keys(this.resource).length === 0) {
        this.fetchData()
      }
    }
  },
  inject: ['parentChangeResource'],
  methods: {
    fetchData () {
      const params = {}
      if (Object.keys(this.$route.query).length > 0) {
        Object.assign(params, this.$route.query)
      }
      this.loading = true

      api('quotaBalance', params).then(json => {
        const quotaBalance = json.quotabalanceresponse.balance || {}
        if (Object.keys(quotaBalance).length > 0) {
          quotaBalance.currency = `${quotaBalance.currency} ${quotaBalance.startquota}`
          quotaBalance.startdate = moment(quotaBalance.startdate).format(this.pattern)
          quotaBalance.account = this.$route.params.id ? this.$route.params.id : null
          quotaBalance.domainid = this.$route.query.domainid ? this.$route.query.domainid : null
        }
        this.quotaResource = Object.assign({}, this.quotaResource, quotaBalance)
        this.parentChangeResource(this.quotaResource)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
</style>
