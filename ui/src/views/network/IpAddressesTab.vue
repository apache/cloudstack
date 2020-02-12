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
    <a-button type="dashed" icon="plus" style="width: 100%; margin-bottom: 15px" @click="acquireIpAddress">
      {{ $t("label.acquire.new.ip") }}
    </a-button>
    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="ips"
      :rowKey="item => item.id"
      :pagination="false" >
      <template slot="ipaddress" slot-scope="text, record">
        <router-link :to="{ path: '/publicip/' + record.id }" >{{ text }} </router-link>
        <a-tag v-if="record.issourcenat === true">source-nat</a-tag>
      </template>

      <template slot="state" slot-scope="text, record">
        <status :text="record.state" displayText />
      </template>

      <template slot="virtualmachineid" slot-scope="text, record">
        <a-icon type="desktop" v-if="record.virtualmachineid" />
        <router-link :to="{ path: '/vm/' + record.virtualmachineid }" > {{ record.virtualmachinename || record.virtualmachineid }} </router-link>
      </template>

      <template slot="action" slot-scope="text, record">
        <a-button
          v-if="record.issourcenat !== true"
          type="danger"
          icon="delete"
          shape="circle"
          @click="releaseIpAddress(record)" />
      </template>
    </a-table>
    <a-divider/>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalIps"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger/>
  </a-spin>
</template>
<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'IpAddressesTab',
  components: {
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
      totalIps: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('ipaddress'),
          dataIndex: 'ipaddress',
          scopedSlots: { customRender: 'ipaddress' }
        },
        {
          title: this.$t('state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('vm'),
          dataIndex: 'virtualmachineid',
          scopedSlots: { customRender: 'virtualmachineid' }
        },
        {
          title: this.$t('Network'),
          dataIndex: 'associatednetworkname'
        },
        {
          title: '',
          scopedSlots: { customRender: 'action' }
        }
      ],
      fetchLoading: false,
      ips: [],
      regions: [],
      clicked: '',
      action: {
        acquire: 'Please confirm that you want to acquire new IP',
        release: 'Please confirm that you want to release this IP'
      },
      visible: false
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      const params = {
        listall: true,
        page: this.page,
        pagesize: this.pageSize
      }
      if (this.$route.path.startsWith('/vpc')) {
        params.vpcid = this.resource.id
        params.forvirtualnetwork = true
      } else {
        params.associatednetworkid = this.resource.id
      }
      this.fetchLoading = true
      api('listPublicIpAddresses', params).then(json => {
        this.totalIps = json.listpublicipaddressesresponse.count || 0
        this.ips = json.listpublicipaddressesresponse.publicipaddress || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    acquireIpAddress () {
      const params = {}
      if (this.$route.path.startsWith('/vpc')) {
        params.vpcid = this.resource.id
      } else {
        params.networkid = this.resource.id
      }
      this.fetchLoading = true
      api('associateIpAddress', params).then(response => {
        this.$pollJob({
          jobId: response.associateipaddressresponse.jobid,
          successMessage: `Successfully acquired IP for ${this.resource.name}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: 'Failed to acquire IP',
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: `Acquiring IP for ${this.resource.name} is in progress`,
          catchMessage: 'Error encountered while fetching async job result'
        })
      }).catch(error => {
        this.fetchLoading = false
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      })
    },
    releaseIpAddress (ip) {
      this.fetchLoading = true
      api('disassociateIpAddress', {
        id: ip.id
      }).then(response => {
        this.$pollJob({
          jobId: response.disassociateipaddressresponse.jobid,
          successMessage: 'Successfully released IP',
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: 'Failed to release IP',
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: `Releasing IP for ${this.resource.name} is in progress`,
          catchMessage: 'Error encountered while fetching async job result'
        })
      }).catch(error => {
        this.fetchLoading = false
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
</style>
