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
    <a-spin :spinning="fetchLoading">
      <a-button
        shape="round"
        style="float: right;margin-bottom: 10px; z-index: 8"
        @click="() => { showCreateForm = true }">
        <template #icon><plus-outlined /></template>
        {{ $t('label.add.ip.range') }}
      </a-button>
      <br />
      <br />

      <a-table
        size="small"
        style="overflow-y: auto; width: 100%;"
        :columns="columns"
        :dataSource="ipranges"
        :rowKey="item => item.id"
        :pagination="false" >

        <template #action="{ record }">
          <tooltip-button
            tooltipPlacement="bottom"
            :tooltip="$t('label.edit')"
            type="primary"
            @click="() => { handleUpdateIpRangeModal(record) }"
            icon="swap-outlined" />
          <a-popconfirm
            :title="$t('message.confirm.remove.ip.range')"
            @confirm="removeIpRange(record.id)"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')" >
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.action.delete.ip.range')"
              type="primary"
              :danger="true"
              icon="delete-outlined" />
          </a-popconfirm>
        </template>
      </a-table>

      <a-divider/>

      <a-pagination
        class="row-element pagination"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="total"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100']"
        @change="changePage"
        @showSizeChange="changePageSize"
        showSizeChanger>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </a-spin>
    <a-modal
      v-if="showCreateForm"
      :visible="showCreateForm"
      :title="$t('label.add.ip.range')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showCreateForm = false }"
      centered
      width="auto">
      <CreateVlanIpRange
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="showCreateForm = false" />
    </a-modal>
    <a-modal
      :visible="showUpdateForm"
      :title="$t('label.update.ip.range')"
      v-if="selectedItem"
      :maskClosable="false"
      :footer="null"
      @cancel="showUpdateForm = false">
      <a-form
        :ref="updRangeRef"
        :model="formUpdRange"
        :rules="updRangeRules"
        @finish="handleUpdateIpRange"
        v-ctrl-enter="handleUpdateIpRange"
        layout="vertical"
        class="form"
      >
        <div>
          <a-form-item name="startip" ref="startip" :label="$t('label.startip')" class="form__item">
            <a-input v-focus="true" v-model:value="formUpdRange.startip"></a-input>
          </a-form-item>
          <a-form-item name="endip" ref="endip" :label="$t('label.endip')" class="form__item">
            <a-input v-model:value="formUpdRange.endip"></a-input>
          </a-form-item>
          <a-form-item name="gateway" ref="gateway" :label="$t('label.gateway')" class="form__item">
            <a-input v-model:value="formUpdRange.gateway"></a-input>
          </a-form-item>
          <a-form-item name="netmask" ref="netmask" :label="$t('label.netmask')" class="form__item">
            <a-input v-model:value="formUpdRange.netmask"></a-input>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="showUpdateForm = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleUpdateIpRange">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { api } from '@/api'
import CreateVlanIpRange from '@/views/network/CreateVlanIpRange'
import TooltipButton from '@/components/widgets/TooltipButton'
import { reactive, ref, toRaw } from 'vue'

export default {
  name: 'GuestIpRanges',
  components: {
    CreateVlanIpRange,
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
      fetchLoading: false,
      showCreateForm: false,
      showUpdateForm: false,
      selectedItem: null,
      total: 0,
      ipranges: [],
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.startipv4'),
          dataIndex: 'startip'
        },
        {
          title: this.$t('label.endipv4'),
          dataIndex: 'endip'
        },
        {
          title: this.$t('label.startipv6'),
          dataIndex: 'startipv6'
        },
        {
          title: this.$t('label.endipv6'),
          dataIndex: 'endipv6'
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
          key: 'actions',
          title: ''
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      const params = {
        zoneid: this.resource.zoneid,
        networkid: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }
      this.fetchLoading = true
      api('listVlanIpRanges', params).then(json => {
        this.total = json.listvlaniprangesresponse.count || 0
        this.ipranges = json.listvlaniprangesresponse.vlaniprange || []
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    removeIpRange (id) {
      api('deleteVlanIpRange', { id: id }).then(json => {
      }).finally(() => {
        this.fetchData()
      })
    },
    initFormUpdateRange () {
      this.updRangeRef = ref()
      this.formUpdRange = reactive({})
      this.updRangeRules = reactive({
        startip: [{ required: true, message: this.$t('label.required') }],
        endip: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }]
      })
    },
    handleUpdateIpRangeModal (item) {
      this.initFormUpdateRange()
      this.selectedItem = item
      this.showUpdateForm = true

      this.formUpdRange = reactive({})
      this.formUpdRange.startip = this.selectedItem?.startip || ''
      this.formUpdRange.endip = this.selectedItem?.endip || ''
      this.formUpdRange.gateway = this.selectedItem?.gateway || ''
      this.formUpdRange.netmask = this.selectedItem?.netmask || ''
    },
    handleUpdateIpRange (e) {
      if (this.componentLoading) return
      this.updRangeRef.value.validate().then(() => {
        const values = toRaw(this.formUpdRange)

        this.componentLoading = true
        this.showUpdateForm = false
        var params = {
          id: this.selectedItem.id
        }
        var ipRangeKeys = ['gateway', 'netmask', 'startip', 'endip']
        for (const key of ipRangeKeys) {
          params[key] = values[key]
        }
        api('updateVlanIpRange', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.update.iprange')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.updatevlaniprangeresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
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
