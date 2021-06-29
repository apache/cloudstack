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
  <a-spin :spinning="componentLoading">
    <a-button
      :disabled="!('createManagementNetworkIpRange' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpRangeModal">
      {{ $t('label.add.ip.range') }}
    </a-button>

    <a-table
      style="overflow-y: auto"
      size="small"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id + record.startip"
      :pagination="false"
    >
      <template slot="forsystemvms" slot-scope="text, record">
        <a-checkbox :checked="record.forsystemvms" />
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <tooltip-button
            tooltipPlacement="bottom"
            :tooltip="$t('label.remove.ip.range')"
            :disabled="!('deleteManagementNetworkIpRange' in $store.getters.apis)"
            icon="delete"
            type="danger"
            @click="handleDeleteIpRange(record)" />
        </div>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      style="overflow-y: auto"
      :current="page"
      :pageSize="pageSize"
      :total="total"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-model="addIpRangeModal"
      :title="$t('label.add.ip.range')"
      :maskClosable="false"
      @ok="handleAddIpRange">
      <a-form
        :form="form"
        @submit="handleAddIpRange"
        layout="vertical"
        class="form"
      >
        <a-form-item :label="$t('label.podid')" class="form__item">
          <a-select
            autoFocus
            v-decorator="['pod', {
              rules: [{ required: true, message: `${$t('label.required')}` }]
            }]"
          >
            <a-select-option v-for="item in items" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.gateway')" class="form__item">
          <a-input
            v-decorator="['gateway', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.netmask')" class="form__item">
          <a-input
            v-decorator="['netmask', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.vlan')" class="form__item">
          <a-input
            v-decorator="['vlan']">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.startip')" class="form__item">
          <a-input
            v-decorator="['startip', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.endip')" class="form__item">
          <a-input
            v-decorator="['endip', { rules: [{ required: true, message: `${$t('label.required')}` }] }]">
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('label.system.vms')" class="form__item">
          <a-checkbox v-decorator="['vms']"></a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>

  </a-spin>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'IpRangesTabManagement',
  components: {
    TooltipButton
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
      componentLoading: false,
      items: [],
      total: 0,
      domains: [],
      domainsLoading: false,
      addIpRangeModal: false,
      defaultSelectedPod: null,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.podid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlanid',
          scopedSlots: { customRender: 'vlan' }
        },
        {
          title: this.$t('label.startip'),
          dataIndex: 'startip',
          scopedSlots: { customRender: 'startip' }
        },
        {
          title: this.$t('label.endip'),
          dataIndex: 'endip',
          scopedSlots: { customRender: 'endip' }
        },
        {
          title: this.$t('label.system.vms'),
          dataIndex: 'forsystemvms',
          scopedSlots: { customRender: 'forsystemvms' }
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.componentLoading = true
      api('listPods', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = []
        this.total = response.listpodsresponse.count || 0
        const pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
        for (const pod of pods) {
          if (pod && pod.startip && pod.startip.length > 0) {
            for (var idx = 0; idx < pod.startip.length; idx++) {
              this.items.push({
                id: pod.id,
                name: pod.name,
                gateway: pod.gateway,
                netmask: pod.netmask,
                vlanid: pod.vlanid[idx],
                startip: pod.startip[idx],
                endip: pod.endip[idx],
                forsystemvms: pod.forsystemvms[idx] === '1'
              })
            }
          }
        }
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleOpenAddIpRangeModal () {
      this.addIpRangeModal = true
      setTimeout(() => {
        if (this.items.length > 0) {
          this.form.setFieldsValue({
            pod: this.items[0].id
          })
        }
      }, 200)
    },
    handleDeleteIpRange (record) {
      this.componentLoading = true
      api('deleteManagementNetworkIpRange', {
        podid: record.id,
        startip: record.startip,
        endip: record.endip,
        vlan: record.vlanid
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: this.$t('message.success.remove.iprange'),
          jobid: response.deletemanagementnetworkiprangeresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.deletemanagementnetworkiprangeresponse.jobid,
          successMethod: () => {
            this.componentLoading = false
            this.fetchData()
          },
          errorMessage: this.$t('message.remove.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchData()
          },
          loadingMessage: this.$t('message.remove.iprange.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.componentLoading = false
        this.fetchData()
      })
    },
    handleAddIpRange (e) {
      this.form.validateFields((error, values) => {
        if (error) return

        this.componentLoading = true
        this.addIpRangeModal = false
        api('createManagementNetworkIpRange', {
          podid: values.pod,
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip,
          forsystemvms: values.vms,
          vlan: values.vlan || null
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('message.success.add.iprange'),
            jobid: response.createmanagementnetworkiprangeresponse.jobid,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createmanagementnetworkiprangeresponse.jobid,
            successMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            errorMessage: this.$t('message.add.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            loadingMessage: this.$t('message.add.iprange.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchData()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
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
    }
  }
}
</script>

<style lang="scss" scoped>
  .list {
    &__item {
      display: flex;
    }

    &__data {
      display: flex;
      flex-wrap: wrap;
    }

    &__col {
      flex-basis: calc((100% / 3) - 20px);
      margin-right: 20px;
      margin-bottom: 10px;
    }

    &__label {
    }
  }

  .ant-list-item {
    padding-top: 0;
    padding-bottom: 0;

    &:not(:first-child) {
      padding-top: 20px;
    }

    &:not(:last-child) {
      padding-bottom: 20px;
    }
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-bottom: 10px;
      }
    }
  }

  .ant-select {
    width: 100%;
  }

  .form {
    .actions {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }

    }

    &__item {
    }
  }

  .pagination {
    margin-top: 20px;
  }
</style>
