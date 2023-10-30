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
          <span v-if="text!==undefined">{{ `${currency} ${text}` }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { api } from '@/api'
import moment from 'moment'

export default {
  name: 'QuotaUsage',
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
      dataSource: [],
      pattern: 'YYYY-MM-DD',
      currency: '',
      totalQuota: 0,
      account: null
    }
  },
  computed: {
    columns () {
      return [
        {
          key: 'name',
          title: this.$t('label.quota.type.name'),
          dataIndex: 'name',
          width: 'calc(100% / 3)'
        },
        {
          key: 'unit',
          title: this.$t('label.quota.type.unit'),
          dataIndex: 'unit',
          width: 'calc(100% / 3)'
        },
        {
          key: 'quota',
          title: this.$t('label.quota.usage'),
          dataIndex: 'quota',
          width: 'calc(100% / 3)'
        }
      ]
    }
  },
  watch: {
    tab () {
      if (this.tab === 'quota.statement.quota') {
        this.fetchData()
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    async fetchData () {
      this.loading = true
      this.dataSource = []
      this.account = this.$route.query && this.$route.query.account ? this.$route.query.account : null

      try {
        const resource = await this.quotaBalance()
        const quotaStatement = await this.quotaStatement(resource)
        const quotaUsage = quotaStatement.quotausage
        this.dataSource = this.dataSource.concat(quotaUsage)
        this.currency = quotaStatement.currency
        this.totalQuota = quotaStatement.totalquota
        this.dataSource.push({
          name: `${this.$t('label.quota.total')}: `,
          quota: this.totalQuota
        })
        this.dataSource.unshift({
          type: 0,
          name: `${this.$t('startdate')}: ${moment(this.resource.startdate).format(this.pattern)}`,
          unit: `${this.$t('enddate')}: ${moment(this.resource.enddate).format(this.pattern)}`
        })
        this.loading = false
      } catch (e) {
        console.log(e)
        this.loading = false
      }
    },
    quotaBalance () {
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
    quotaStatement (resource) {
      return new Promise((resolve, reject) => {
        const params = {}
        params.domainid = this.resource.domainid
        params.account = this.account
        params.startdate = moment(this.resource.startdate).format(this.pattern)
        params.enddate = moment(resource.startdate).format(this.pattern)

        api('quotaStatement', params).then(json => {
          const quotaStatement = json.quotastatementresponse.statement || {}
          resolve(quotaStatement)
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
