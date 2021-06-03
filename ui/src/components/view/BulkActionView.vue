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
    <a-modal
      v-if="showConfirmationAction"
      :visible="showConfirmationAction"
      :closable="true"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      style="top: 20px;"
      width="50vw"
      @ok="groupAction"
      @cancel="closeModal"
      centered>
      <span slot="title">
        {{ $t(message.title) }}
      </span>
      <span>
        <a-alert type="warning">
          <span v-if="selectedItems.length > 0" slot="message" v-html="`<b>${selectedItems.length} ` + $t('label.items.selected') + `. </b>`" />
          <span slot="message" v-html="$t(message.confirmMessage)" />
        </a-alert>
        <a-divider />
        <a-table
          size="middle"
          :columns="selectedColumns"
          :dataSource="selectedItems"
          :rowKey="(record, idx) => record.id"
          :pagination="true"
          style="overflow-y: auto">
          <template slot="algorithm" slot-scope="record">
            {{ returnAlgorithmName(record.algorithm) }}
          </template>
          <template v-for="(column, index) in selectedColumns" :slot="column" slot-scope="text" >
            <span :key="index"> {{ text }} ==== {{ column }} </span>
          </template>
          <template slot="privateport" slot-scope="record">
            {{ record.privateport }} - {{ record.privateendport }}
          </template>
          <template slot="publicport" slot-scope="record">
            {{ record.publicport }} - {{ record.publicendport }}
          </template>
          <template slot="protocol" slot-scope="record">
            {{ record.protocol | capitalise }}
          </template>
          <template slot="vm" slot-scope="record">
            <div><a-icon type="desktop"/> {{ record.virtualmachinename }} ({{ record.vmguestip }})</div>
          </template>
          <template slot="startport" slot-scope="record">
            {{ record.icmptype || record.startport >= 0 ? record.icmptype || record.startport : $t('label.all') }}
          </template>
          <template slot="endport" slot-scope="record">
            {{ record.icmpcode || record.endport >= 0 ? record.icmpcode || record.endport : $t('label.all') }}
          </template>
        </a-table>
        <a-divider />
        <br/>
      </span>
    </a-modal>

    <a-modal
      :visible="showGroupActionModal"
      :closable="true"
      :maskClosable="false"
      :cancelText="$t('label.cancel')"
      @cancel="handleCancel"
      width="50vw"
      style="top: 20px;overflow-y: auto"
      centered
    >
      <span slot="title"> {{ $t(message.title) }} </span>
      <template slot="footer">
        <a-button key="back" @click="handleCancel"> {{ $t('label.close') }} </a-button>
      </template>
      <div v-if="showGroupActionModal">
        <a-table
          v-if="selectedItems.length > 0"
          size="middle"
          :columns="selectedColumns"
          :dataSource="selectedItems"
          :rowKey="(record, idx) => record.id"
          :pagination="true"
          style="overflow-y: auto">
          <div slot="status" slot-scope="text">
            <status :text=" text ? text : $t('state.inprogress') " displayText></status>
          </div>
          <template slot="algorithm" slot-scope="record">
            {{ returnAlgorithmName(record.algorithm) }}
          </template>
          <template slot="privateport" slot-scope="record">
            {{ record.privateport }} - {{ record.privateendport }}
          </template>
          <template slot="publicport" slot-scope="record">
            {{ record.publicport }} - {{ record.publicendport }}
          </template>
          <template slot="protocol" slot-scope="record">
            {{ record.protocol | capitalise }}
          </template>
          <template slot="startport" slot-scope="record">
            {{ record.icmptype || record.startport >= 0 ? record.icmptype || record.startport : $t('label.all') }}
          </template>
          <template slot="endport" slot-scope="record">
            {{ record.icmpcode || record.endport >= 0 ? record.icmpcode || record.endport : $t('label.all') }}
          </template>
          <template slot="vm" slot-scope="record">
            <div><a-icon type="desktop"/> {{ record.virtualmachinename }} ({{ record.vmguestip }})</div>
          </template>
        </a-table>
        <a-divider />
        <a-card :bordered="false" style="background:#f1f1f1">
          <div><a-icon type="check-circle-o" style="color: #52c41a; margin-right: 8px"/> {{ $t('label.success') + ': ' + selectedItems.filter(item => item.status === 'success').length || 0 }}</div>
          <div><a-icon type="close-circle-o" style="color: #f5222d; margin-right: 8px"/> {{ $t('state.failed') + ': ' + selectedItems.filter(item => item.status === 'failed').length || 0 }}</div>
          <div><a-icon type="sync-o" style="color: #1890ff; margin-right: 8px"/> {{ $t('state.inprogress') + ': ' + selectedItems.filter(item => item.status === 'InProgress').length || 0 }}</div>
        </a-card>
        <br/>
      </div>
    </a-modal>
  </div>
</template>

<script>
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'BulkActionView',
  components: {
    Status,
    TooltipButton
  },
  props: {
    showConfirmationAction: {
      type: Boolean,
      default: false
    },
    showGroupActionModal: {
      type: Boolean,
      default: false
    },
    items: {
      type: Array,
      default: () => []
    },
    selectedRowKeys: {
      type: Array,
      default: () => []
    },
    selectedItems: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    selectedColumns: {
      type: Array,
      default: () => []
    },
    action: {
      type: String,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    message: {
      type: Object,
      default: () => {}
    }
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      totalCount: 0,
      page: 1,
      pageSize: 10
    }
  },
  methods: {
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    handleCancel () {
      this.$emit('handle-cancel')
    },
    groupAction () {
      this.$emit('group-action')
    },
    returnAlgorithmName (name) {
      switch (name) {
        case 'leastconn':
          return 'Least connections'
        case 'roundrobin' :
          return 'Round-robin'
        case 'source':
          return 'Source'
        default :
          return ''
      }
    },
    closeModal () {
      this.$emit('close-modal')
    }
  }
}
</script>
