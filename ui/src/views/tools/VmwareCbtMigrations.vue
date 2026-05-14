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
  <a-row :gutter="12">
    <a-col :md="24" :lg="24">
      <a-card class="instances-card">
        <template #title>
          {{ $t('label.vmware.cbt.migrations') }}
          <a-tooltip :title="$t('message.vmware.cbt.migrations')">
            <info-circle-outlined />
          </a-tooltip>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="this.$emit('fetch-vmware-cbt-migrations')" >
            <template #icon><reload-outlined /></template>
          </a-button>
          <span style="float: right; width: 50%">
            <a-select
              :placeholder="$t('label.filterby')"
              :value="filter"
              style="min-width: 140px; margin-left: 10px; margin-bottom: 5px"
              size=small
              @change="onFilterChange"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
            >
              <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
              <a-select-option
                v-for="filter in filters"
                :key="filter"
                :label="getFilterLabel(filter)">
                {{ getFilterLabel(filter) }}
              </a-select-option>
            </a-select>
          </span>
        </template>
        <a-table
          :data-source="migrations"
          class="instances-card-table"
          size="middle"
          :pagination="false"
          :rowKey="record => record.id"
          :columns="columns"
          :rowExpandable="record => getDisks(record).length > 0">
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.key === 'displayname'">
              <router-link v-if="record.virtualmachineid" :to="{ path: '/vm/' + record.virtualmachineid }">{{ record.displayname }}</router-link>
              <span v-else>{{ record.displayname }}</span>
            </template>
            <template v-else-if="column.key === 'convertinstancehostid'">
              <router-link v-if="record.convertinstancehostid" :to="{ path: '/host/' + record.convertinstancehostid }">{{ record.convertinstancehostname }}</router-link>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'created'">
              <span>{{ $toLocaleDate(record.created) }}</span>
            </template>
            <template v-else-if="['lastchangedbytes', 'totalchangedbytes'].includes(column.key)">
              <span v-if="text">{{ $bytesToHumanReadableSize(text) }}</span>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'lastdirtyrate'">
              <span v-if="text">{{ $bytesToHumanReadableSize(text) }}/s</span>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button
                  v-if="canRegisterTargets(record)"
                  size="small"
                  :disabled="!('registerVmwareCbtMigrationTarget' in $store.getters.apis)"
                  @click="openTargetRegistration(record)">
                  <template #icon><edit-outlined /></template>
                  {{ $t('label.register.targets') }}
                </a-button>
                <a-popconfirm
                  v-if="canSync(record)"
                  :title="$t('message.confirm.sync.vmware.cbt.migration')"
                  :okText="$t('label.ok')"
                  :cancelText="$t('label.cancel')"
                  @confirm="() => this.$emit('sync-vmware-cbt-migration', record)">
                  <a-button
                    size="small"
                    :disabled="!('syncVmwareCbtMigration' in $store.getters.apis)">
                    <template #icon><sync-outlined /></template>
                    {{ $t('label.sync.delta') }}
                  </a-button>
                </a-popconfirm>
                <a-popconfirm
                  v-if="canCutover(record)"
                  :title="$t('message.confirm.cutover.vmware.cbt.migration')"
                  :okText="$t('label.ok')"
                  :cancelText="$t('label.cancel')"
                  @confirm="() => this.$emit('cutover-vmware-cbt-migration', record)">
                  <a-button
                    size="small"
                    type="primary"
                    :disabled="!('cutoverVmwareCbtMigration' in $store.getters.apis)">
                    <template #icon><thunderbolt-outlined /></template>
                    {{ $t('label.cutover') }}
                  </a-button>
                </a-popconfirm>
                <a-popconfirm
                  v-if="canCancel(record)"
                  :title="$t('message.confirm.cancel.vmware.cbt.migration')"
                  :okText="$t('label.ok')"
                  :cancelText="$t('label.cancel')"
                  @confirm="() => this.$emit('cancel-vmware-cbt-migration', record)">
                  <a-button
                    size="small"
                    :danger="true"
                    :disabled="!('cancelVmwareCbtMigration' in $store.getters.apis)">
                    <template #icon><close-circle-outlined /></template>
                    {{ $t('label.cancel') }}
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
          <template #expandedRowRender="{ record }">
            <a-table
              size="small"
              :columns="diskColumns"
              :data-source="getDisks(record)"
              :rowKey="diskRowKey"
              :pagination="false">
              <template #bodyCell="{ column, text }">
                <template v-if="column.key === 'capacity'">
                  <span v-if="text">{{ $bytesToHumanReadableSize(text) }}</span>
                  <span v-else>-</span>
                </template>
                <template v-else-if="column.key === 'targetpath' || column.key === 'changeid' || column.key === 'snapshotmor'">
                  <span>{{ text || '-' }}</span>
                </template>
                <template v-else>
                  <span>{{ text }}</span>
                </template>
              </template>
            </a-table>
          </template>
        </a-table>
        <div class="instances-card-footer">
          <a-pagination
            class="row-element"
            size="small"
            :current="page"
            :pageSize="pageSize"
            :total="total"
            :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
            @change="onPaginationChange"
            showQuickJumper>
            <template #buildOptionText="props">
              <span>{{ props.value }} / {{ $t('label.page') }}</span>
            </template>
          </a-pagination>
        </div>
      </a-card>
    </a-col>
    <a-modal
      :visible="registerTargetsVisible"
      :title="$t('label.register.targets')"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      :confirmLoading="registerTargetsLoading"
      :maskClosable="false"
      width="80%"
      @ok="submitTargetRegistration"
      @cancel="closeTargetRegistration">
      <a-table
        size="small"
        :columns="targetDiskColumns"
        :data-source="targetDiskRows"
        :rowKey="diskRowKey"
        :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'capacity'">
            <span v-if="record.capacity">{{ $bytesToHumanReadableSize(record.capacity) }}</span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'targetpath'">
            <a-input v-model:value="record.targetpath" :placeholder="$t('label.target.path')" />
          </template>
          <template v-else-if="column.key === 'targetformat'">
            <a-select v-model:value="record.targetformat" style="width: 120px">
              <a-select-option value="qcow2">qcow2</a-select-option>
              <a-select-option value="raw">raw</a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'changeid'">
            <a-input v-model:value="record.changeid" :placeholder="$t('label.change.id')" />
          </template>
          <template v-else-if="column.key === 'snapshotmor'">
            <a-input v-model:value="record.snapshotmor" :placeholder="$t('label.snapshot.mor')" />
          </template>
          <template v-else>
            <span>{{ record[column.dataIndex] || '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </a-row>
</template>

<script>
import { postAPI } from '@/api'

export default {
  name: 'VmwareCbtMigrations',
  props: {
    migrations: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      required: false
    },
    filter: {
      type: String,
      required: false
    },
    total: {
      type: Number,
      required: true
    },
    page: {
      type: Number,
      required: true
    },
    pageSize: {
      type: Number,
      required: true
    }
  },
  data () {
    const columns = [
      {
        key: 'created',
        title: this.$t('label.created'),
        dataIndex: 'created'
      },
      {
        key: 'displayname',
        title: this.$t('label.displayname'),
        dataIndex: 'displayname'
      },
      {
        key: 'sourcevmname',
        title: this.$t('label.sourcevmname'),
        dataIndex: 'sourcevmname'
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state'
      },
      {
        key: 'currentstep',
        title: this.$t('label.currentstep'),
        dataIndex: 'currentstep'
      },
      {
        key: 'completedcycles',
        title: this.$t('label.completedcycles'),
        dataIndex: 'completedcycles'
      },
      {
        key: 'quietcycles',
        title: this.$t('label.quietcycles'),
        dataIndex: 'quietcycles'
      },
      {
        key: 'lastchangedbytes',
        title: this.$t('label.lastchangedbytes'),
        dataIndex: 'lastchangedbytes'
      },
      {
        key: 'lastdirtyrate',
        title: this.$t('label.lastdirtyrate'),
        dataIndex: 'lastdirtyrate'
      },
      {
        key: 'totalchangedbytes',
        title: this.$t('label.totalchangedbytes'),
        dataIndex: 'totalchangedbytes'
      },
      {
        key: 'convertinstancehostid',
        title: this.$t('label.conversionhost'),
        dataIndex: 'convertinstancehostid'
      },
      {
        key: 'vcenter',
        title: this.$t('label.vcenter'),
        dataIndex: 'vcenter'
      },
      {
        key: 'datacentername',
        title: this.$t('label.vcenter.datacenter'),
        dataIndex: 'datacentername'
      },
      {
        key: 'sourcehost',
        title: this.$t('label.sourcehost'),
        dataIndex: 'sourcehost'
      },
      {
        key: 'sourcecluster',
        title: this.$t('label.sourcecluster'),
        dataIndex: 'sourcecluster'
      },
      {
        key: 'lasterror',
        title: this.$t('label.lasterror'),
        dataIndex: 'lasterror'
      },
      {
        key: 'action',
        title: this.$t('label.action'),
        dataIndex: 'action'
      }
    ]
    const diskColumns = [
      {
        key: 'sourcediskid',
        title: this.$t('label.source.disk'),
        dataIndex: 'sourcediskid'
      },
      {
        key: 'sourcediskpath',
        title: this.$t('label.source.disk.path'),
        dataIndex: 'sourcediskpath'
      },
      {
        key: 'capacity',
        title: this.$t('label.capacity'),
        dataIndex: 'capacity'
      },
      {
        key: 'targetpath',
        title: this.$t('label.target.path'),
        dataIndex: 'targetpath'
      },
      {
        key: 'targetformat',
        title: this.$t('label.target.format'),
        dataIndex: 'targetformat'
      },
      {
        key: 'changeid',
        title: this.$t('label.change.id'),
        dataIndex: 'changeid'
      },
      {
        key: 'snapshotmor',
        title: this.$t('label.snapshot.mor'),
        dataIndex: 'snapshotmor'
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state'
      }
    ]
    return {
      columns,
      diskColumns,
      targetDiskColumns: diskColumns,
      filters: ['all', 'Created', 'InitialSync', 'Replicating', 'ReadyForCutover', 'CuttingOver', 'Completed', 'Failed', 'Cancelled'],
      registerTargetsVisible: false,
      registerTargetsLoading: false,
      selectedMigration: null,
      targetDiskRows: []
    }
  },
  methods: {
    canSync (record) {
      return ['Replicating', 'ReadyForCutover'].includes(record.state)
    },
    canCutover (record) {
      return record.state === 'ReadyForCutover'
    },
    canCancel (record) {
      return !['Completed', 'Failed', 'Cancelled'].includes(record.state)
    },
    canRegisterTargets (record) {
      return ['Created', 'InitialSync'].includes(record.state) && this.getDisks(record).length > 0
    },
    getFilterLabel (filter) {
      return filter === 'all' ? this.$t('label.all') : filter
    },
    getDisks (record) {
      const disks = record?.disk || record?.disks || []
      return Array.isArray(disks) ? disks : [disks]
    },
    diskRowKey (record, index) {
      return record.id || record.sourcediskid || index
    },
    openTargetRegistration (record) {
      this.selectedMigration = record
      this.targetDiskRows = this.getDisks(record).map((disk, index) => ({
        id: disk.id || disk.sourcediskid || index,
        sourcediskid: disk.sourcediskid,
        sourcediskpath: disk.sourcediskpath,
        capacity: disk.capacity || disk.capacitybytes,
        targetpath: disk.targetpath || '',
        targetformat: disk.targetformat || 'qcow2',
        changeid: disk.changeid || '',
        snapshotmor: disk.snapshotmor || '',
        state: disk.state
      }))
      this.registerTargetsVisible = true
    },
    closeTargetRegistration () {
      this.registerTargetsVisible = false
      this.selectedMigration = null
      this.targetDiskRows = []
    },
    submitTargetRegistration () {
      if (!this.selectedMigration) {
        return
      }
      if (this.targetDiskRows.some(disk => !disk.targetpath || disk.targetpath.trim() === '')) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.error.vmware.cbt.target.path')
        })
        return
      }

      const params = {
        id: this.selectedMigration.id
      }
      this.targetDiskRows.forEach((disk, index) => {
        params['targetdisklist[' + index + '].sourcediskid'] = disk.sourcediskid
        params['targetdisklist[' + index + '].targetpath'] = disk.targetpath.trim()
        params['targetdisklist[' + index + '].targetformat'] = disk.targetformat
        if (disk.changeid) {
          params['targetdisklist[' + index + '].changeid'] = disk.changeid
        }
        if (disk.snapshotmor) {
          params['targetdisklist[' + index + '].snapshotmor'] = disk.snapshotmor
        }
      })

      this.registerTargetsLoading = true
      postAPI('registerVmwareCbtMigrationTarget', params).then(() => {
        this.closeTargetRegistration()
        this.$emit('fetch-vmware-cbt-migrations')
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.registerTargetsLoading = false
      })
    },
    onFilterChange (e) {
      this.$emit('change-filter', e)
    },
    onPaginationChange (page, size) {
      this.$emit('change-pagination', page, size)
    }
  }
}
</script>
