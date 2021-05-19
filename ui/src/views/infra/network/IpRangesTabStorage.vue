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
      :disabled="!('createStorageNetworkIpRange' in $store.getters.apis)"
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
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template slot="name" slot-scope="record">
        <div>{{ returnPodName(record.podid) }}</div>
      </template>
      <template slot="actions" slot-scope="record">
        <tooltip-button
          :tooltip="$t('label.remove.ip.range')"
          :disabled="!('deleteStorageNetworkIpRange' in $store.getters.apis)"
          icon="delete"
          type="danger"
          @click="handleDeleteIpRange(record.id)" />
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
            <a-select-option v-for="pod in pods" :key="pod.id" :value="pod.id">{{ pod.name }}</a-select-option>
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
      </a-form>
    </a-modal>

  </a-spin>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'IpRangesTabStorage',
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
      pods: [],
      domains: [],
      domainsLoading: false,
      addIpRangeModal: false,
      defaultSelectedPod: null,
      columns: [
        {
          title: this.$t('label.podid'),
          scopedSlots: { customRender: 'name' }
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
          dataIndex: 'vlanid'
        },
        {
          title: this.$t('label.startip'),
          dataIndex: 'startip'
        },
        {
          title: this.$t('label.endip'),
          dataIndex: 'endip'
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'actions' }
        }
      ],
      page: 1,
      pageSize: 10
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
      this.fetchPods()
      this.componentLoading = true
      api('listStorageNetworkIpRange', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.items = response.liststoragenetworkiprangeresponse.storagenetworkiprange || []
        this.total = response.liststoragenetworkiprangeresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    fetchPods () {
      this.componentLoading = true
      api('listPods', {
        zoneid: this.resource.zoneid
      }).then(response => {
        this.pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    returnPodName (id) {
      const match = this.pods.find(i => i.id === id)
      return match ? match.name : null
    },
    handleOpenAddIpRangeModal () {
      this.addIpRangeModal = true
      setTimeout(() => {
        if (this.items.length > 0) {
          this.form.setFieldsValue({
            pod: this.pods[0].id
          })
        }
      }, 200)
    },
    handleDeleteIpRange (id) {
      this.componentLoading = true
      api('deleteStorageNetworkIpRange', { id }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: this.$t('message.success.remove.iprange'),
          jobid: response.deletestoragenetworkiprangeresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.deletestoragenetworkiprangeresponse.jobid,
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
        api('createStorageNetworkIpRange', {
          podid: values.pod,
          zoneid: this.resource.zoneid,
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip,
          vlan: values.vlan || null
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: this.$t('message.success.add.iprange'),
            jobid: response.createstoragenetworkiprangeresponse.jobid,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createstoragenetworkiprangeresponse.jobid,
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
