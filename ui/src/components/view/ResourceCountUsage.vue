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
  <a-spin :spinning="formLoading">
    <a-list
      size="small"
      :dataSource="resourceCountData"
    >
      <a-list-item slot="renderItem" slot-scope="item" class="list-item">
        <div class="list-item__container">
          <strong>
            {{ $t('label.' + String(item).toLowerCase() + '.count') }}
          </strong>
          <br/>
          <br/>
          <div class="list-item__vals">
            <div class="list-item__data">
              Current Usage: {{ resourceData[item + 'total'] }} / {{ resourceData[item + 'limit'] }}
            </div>
            <div class="list-item__data">
              Available: {{ resourceData[item + 'available'] }}
            </div>
            <a-progress
              status="normal"
              :percent="parseFloat(getPercentUsed(resourceData[item + 'total'], resourceData[item + 'limit']))"
              :format="p => parseFloat(getPercentUsed(resourceData[item + 'total'], resourceData[item + 'limit'])).toFixed(2) + '%'" />
          </div>
        </div>
      </a-list-item>
    </a-list>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ResourceCountTab',
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
      formLoading: false,
      resourceData: {
        type: Object,
        required: false
      },
      resourceCountData: ['vm', 'cpu', 'memory', 'primarystorage', 'ip',
        'volume', 'secondarystorage', 'snapshot',
        'template', 'network', 'vpc', 'project']
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    resource (newData, oldData) {
      if (!newData || !newData.id) {
        return
      }
      this.resource = newData
      this.fetchData()
    }
  },
  methods: {
    getResourceData () {
      const params = {}
      if (this.$route.meta.name === 'account') {
        params.account = this.resource.name
        params.domainid = this.resource.domainid
        this.listAccounts(params)
      } else if (this.$route.meta.name === 'domain') {
        params.id = this.resource.id
        this.listDomains(params)
      } else { // project
        params.id = this.resource.id
        params.listall = true
        this.listProjects(params)
      }
    },
    fetchData () {
      try {
        this.formLoading = true
        this.getResourceData()
        this.formLoading = false
      } catch (e) {
        this.$notification.error({
          message: 'Request Failed',
          description: e
        })
        this.formLoading = false
      }
    },
    listDomains (params) {
      api('listDomains', params).then(json => {
        const domains = json.listdomainsresponse.domain || []
        this.resourceData = domains[0] || {}
      }).catch(error => {
        this.handleErrors(error)
      }).finally(f => {
        this.loading = false
      })
    },
    listAccounts (params) {
      api('listAccounts', params).then(json => {
        const accounts = json.listaccountsresponse.account || []
        this.resourceData = accounts[0] || {}
      }).catch(error => {
        this.handleErrors(error)
      }).finally(f => {
        this.loading = false
      })
    },
    listProjects (params) {
      api('listProjects', params).then(json => {
        const projects = json.listprojectsresponse.project || []
        this.resourceData = projects[0] || {}
      }).catch(error => {
        this.handleErrors(error)
      }).finally(f => {
        this.loading = false
      })
    },
    handleErrors (error) {
      this.$notification.error({
        message: 'Request Failed',
        description: error.response.headers['x-description'],
        duration: 0
      })

      if ([401, 405].includes(error.response.status)) {
        this.$router.push({ path: '/exception/403' })
      }

      if ([430, 431, 432].includes(error.response.status)) {
        this.$router.push({ path: '/exception/404' })
      }

      if ([530, 531, 532, 533, 534, 535, 536, 537].includes(error.response.status)) {
        this.$router.push({ path: '/exception/500' })
      }
    },
    getPercentUsed (total, limit) {
      return (limit === 'Unlimited') ? 0 : (total / limit) * 100
    }
  }
}
</script>

<style scoped lang="scss">
  .list-item {

    &__container {
      max-width: 90%;
      width: 100%;

      @media (min-width: 760px) {
        max-width: 95%;
      }
    }

    &__title {
      font-weight: bold;
    }

    &__data {
      margin-right: 20px;
      white-space: nowrap;
    }

    &__vals {
      @media (min-width: 760px) {
        display: flex;
      }
    }
  }
</style>
