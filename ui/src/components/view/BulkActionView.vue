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
      :ok-button-props="{props: { type: 'default' } }"
      :cancel-button-props="{props: { type: 'primary' } }"
      centered>
      <template #title>
        {{ $t(message.title) }}
      </template>
      <span>
        <a-alert
          v-if="isDestructiveAction()"
          type="error">
          <template #message>
            <exclamation-circle-outlined style="color: red; fontSize: 30px; display: inline-flex" />
            <span style="padding-left: 5px" v-html="`<b>${selectedRowKeys.length} ` + $t('label.items.selected') + `. </b>`" />
            <span v-html="$t(message.confirmMessage)" />
          </template>
        </a-alert>
        <a-alert v-else type="warning">
          <template #message>
            <span v-if="selectedRowKeys.length > 0" v-html="`<b>${selectedRowKeys.length} ` + $t('label.items.selected') + `. </b>`" />
            <span v-html="$t(message.confirmMessage)" />
          </template>
        </a-alert>
        <a-divider />
        <a-table
          size="middle"
          :columns="selectedColumns"
          :dataSource="selectedItems"
          :rowKey="record => $route.path.includes('/iso/') ? record.zoneid : record.id"
          :pagination="true"
          style="overflow-y: auto">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'algorithm'">
              {{ returnAlgorithmName(record.algorithm) }}
            </template>
            <template v-if="column.key === 'column'">
              <span v-for="(column, index) in selectedColumns" :key="index"> {{ text }} ==== {{ column }}</span>
            </template>
            <template v-if="column.key === 'privateport'">
              {{ record.privateport }} - {{ record.privateendport }}
            </template>
            <template v-if="column.key === 'publicport'">
              {{ record.publicport }} - {{ record.publicendport }}
            </template>
            <template v-if="column.key === 'protocol'">
              {{ capitalise(record.protocol) }}
            </template>
            <template v-if="column.key === 'vm'">
              <div><desktop-outlined /> {{ record.virtualmachinename }} ({{ record.vmguestip }})</div>
            </template>
            <template v-if="column.key === 'startport'">
              {{ record.icmptype || record.startport >= 0 ? record.icmptype || record.startport : $t('label.all') }}
            </template>
            <template v-if="column.key === 'endport'">
              {{ record.icmpcode || record.endport >= 0 ? record.icmpcode || record.endport : $t('label.all') }}
            </template>
            <template v-if="column.key === 'cidrlist'">
              <span style="white-space: pre-line"> {{ record.cidrlist?.replaceAll(" ", "\n") }}</span>
            </template>
          </template>
        </a-table>
        <a-divider />
        <br/>
      </span>
    </a-modal>

    <bulk-action-progress
      :showGroupActionModal="showGroupActionModal"
      :selectedItems="selectedItems"
      :selectedColumns="selectedColumns"
      :message="message"
      @handle-cancel="handleCancel" />
  </div>
</template>

<script>
import Status from '@/components/widgets/Status'
import BulkActionProgress from '@/components/view/BulkActionProgress'

export default {
  name: 'BulkActionView',
  components: {
    Status,
    BulkActionProgress
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
  inject: ['parentFetchData'],
  data () {
    return {
      totalCount: 0,
      page: 1,
      pageSize: 10
    }
  },
  methods: {
    capitalise (val) {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    },
    handleCancel () {
      this.$emit('handle-cancel')
    },
    groupAction () {
      this.$emit('group-action')
    },
    isDestructiveAction () {
      if (new RegExp(['remove', 'delete', 'destroy', 'stop', 'release', 'disassociate'].join('|')).test(this.action)) {
        return true
      }
      return false
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
