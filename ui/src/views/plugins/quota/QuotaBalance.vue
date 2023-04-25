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
  <div>
    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="record => record.name"
      :pagination="false"
      :scroll="{ y: '55vh' }"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.key === 'quota'">
          <span v-if="text!==null">{{ `${currency} ${text}` }}</span>
        </template>
        <template v-if="column.key === 'credit'">
          <span v-if="text!==null">{{ `${currency} ${text}` }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'

export default {
  name: 'QuotaBalance',
  props: {
    resource: {
      type: Object,
      required: true
    },
    tab: {
      type: String,
      default: () => ''
    }
  },
  data () {
    return {
      loading: false,
      pattern: 'YYYY-MM-DD',
      currency: '',
      dataSource: [],
      account: null
    }
  },
  computed: {
    columns () {
      return [
        {
          key: 'date',
          title: this.$t('label.date'),
          dataIndex: 'date',
          width: 'calc(100% / 3)'
        },
        {
          key: 'quota',
          title: this.$t('label.quota.value'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)'
        },
        {
          key: 'credit',
          title: this.$t('label.credit'),
          dataIndex: 'credit',
          width: 'calc(100% / 3)'
        }
      ]
    }
  },
  watch: {
    tab () {
      if (this.tab === 'quota.statement.balance') {
        this.fetchData()
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    async fetchData () {
      this.dataSource = []
      this.loading = true
      this.account = this.$route.query && this.$route.query.account ? this.$route.query.account : null

      try {
        const resource = await this.fetchResource()
        const quotaBalance = await this.quotaBalance(resource)
        this.currency = quotaBalance.currency
        this.dataSource = await this.createDataSource(quotaBalance)
        this.loading = false
      } catch (e) {
        console.log(e)
        this.loading = false
      }
    },
    createDataSource (quotaBalance) {
      const dataSource = []
      const credits = quotaBalance.credits || []

      dataSource.push({
        date: moment(quotaBalance.enddate).format(this.pattern),
        quota: quotaBalance.endquota,
        credit: null
      })
      dataSource.push({
        date: moment(quotaBalance.startdate).format(this.pattern),
        quota: quotaBalance.startquota,
        credit: null
      })
      credits.map(item => {
        dataSource.push({
          date: moment(item.updated_on).format(this.pattern),
          quota: null,
          credit: item.credits
        })
      })

      return dataSource
    },
    fetchResource () {
      return new Promise((resolve, reject) => {
        const params = {}
        params.domainid = this.resource.domainid
        params.account = this.account

        api('quotaBalance', params).then(json => {
          const quotaBalance = json.quotabalanceresponse.balance || {}
          resolve(quotaBalance)
        }).catch(error => {
          reject(error)
        })
      })
    },
    quotaBalance (resource) {
      return new Promise((resolve, reject) => {
        const params = {}
        params.domainid = this.resource.domainid
        params.account = this.account
        params.startdate = moment(this.resource.startdate).format(this.pattern)
        params.enddate = moment(resource.startdate).format(this.pattern)

        api('quotaBalance', params).then(json => {
          const quotaBalance = json.quotabalanceresponse.balance || {}
          resolve(quotaBalance)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
