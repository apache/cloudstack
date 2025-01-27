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
  <a-row :gutter="12" v-if="isPageAllowed">
    <a-col :md="24">
      <a-card class="breadcrumb-card">
        <a-col :md="24" style="display: flex">
          <breadcrumb style="padding-top: 6px; padding-left: 8px" />
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="viewLoading"
            size="small"
            shape="round"
            @click="fetchData()" >
            <template #icon><reload-outlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
        </a-col>
      </a-card>
    </a-col>
    <a-col
      :md="24">
      <div>
        <a-card>
          <a-alert
            type="info"
            :showIcon="true"
            :message="$t('label.desc.import.unmanage.volume')"
          >
            <template #description>
              <span v-html="$t('message.desc.import.unmanage.volume')" />
            </template>
          </a-alert>
          <br />
          <a-row :gutter="12">
            <!-- ------------ -->
            <!-- TOP -->
            <!-- ------------ -->
            <a-card class="source-dest-card">
              <template #title>
                {{ $t('label.storagepool') }}
              </template>
              <a-col :md="24" :lg="48">
                <a-form
                  style="min-width: 170px"
                  :ref="formRef"
                  :model="form"
                  :rules="rules"
                  layout="vertical"
                >
                <a-form-item name="scope" ref="scope">
                  <template #label>
                    <tooltip-label :title="$t('label.scope')" :tooltip="$t('label.scope.tooltip')"/>
                  </template>
                  <a-select
                    v-model:value="this.poolscope"
                    @change="onSelectPoolScope"
                    showSearch
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }" >
                    <a-select-option :value="'zone'" :label="$t('label.zoneid')"> {{ $t('label.zoneid') }} </a-select-option>
                    <a-select-option :value="'cluster'" :label="$t('label.clusterid')"> {{ $t('label.clusterid') }} </a-select-option>
                    <a-select-option :value="'host'" :label="$t('label.hostid')"> {{ $t('label.hostid') }} </a-select-option>
                  </a-select>
                </a-form-item>
                  <a-form-item
                    name="zoneid"
                    ref="zoneid"
                    :label="$t('label.zoneid')"
                  >
                    <a-select
                      v-model:value="form.zoneid"
                      showSearch
                      optionFilterProp="label"
                      :filterOption="(input, option) => {
                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                      }"
                      @change="onSelectZoneId"
                      :loading="optionLoading.zones"
                    >
                      <a-select-option v-for="zoneitem in zoneSelectOptions" :key="zoneitem.value" :label="zoneitem.label">
                        <span>
                          <resource-icon v-if="zoneitem.icon" :image="zoneitem.icon" size="1x" style="margin-right: 5px"/>
                          <global-outlined v-else style="margin-right: 5px" />
                          {{ zoneitem.label }}
                        </span>
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item
                    v-if="showPod"
                    name="podid"
                    ref="podid"
                    :label="$t('label.podid')">
                    <a-select
                      v-model:value="form.podid"
                      showSearch
                      optionFilterProp="label"
                      :filterOption="filterOption"
                      :options="podSelectOptions"
                      :loading="optionLoading.pods"
                      @change="onSelectPodId"
                    ></a-select>
                  </a-form-item>
                  <a-form-item
                    v-if="showCluster"
                    name="clusterid"
                    ref="clusterid"
                    :label="$t('label.clusterid')">
                    <a-select
                      v-model:value="form.clusterid"
                      showSearch
                      optionFilterProp="label"
                      :filterOption="filterOption"
                      :options="clusterSelectOptions"
                      :loading="optionLoading.clusters"
                      @change="onSelectClusterId"
                    ></a-select>
                  </a-form-item>
                  <a-form-item
                    v-if="showHost"
                    name="hostid"
                    ref="hostid">
                    <template #label>
                      <tooltip-label
                        :title="$t('label.hostname')"
                        :tooltip="$t('label.hostname.tooltip')"/>
                    </template>
                      <a-select
                        v-model:value="form.hostid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                        :options="hostSelectOptions"
                        :loading="optionLoading.hosts"
                        @change="onSelectHostId"
                      ></a-select>
                  </a-form-item>
                  <a-form-item
                    name="poolid"
                    ref="poolid">
                    <template #label>
                      <tooltip-label
                        :title="$t('label.storagepool')"
                        :tooltip="$t('label.storagepool.tooltip')"/>
                    </template>
                      <a-select
                        v-model:value="form.poolid"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="filterOption"
                        :options="poolSelectOptions"
                        :loading="optionLoading.pools"
                        @change="onSelectPoolId"
                      ></a-select>
                  </a-form-item>
                </a-form>
              </a-col>
            </a-card>
          </a-row>

          <a-row :gutter="12">
            <a-col :md="24" :lg="12">
              <a-card class="volumes-card">
                <template #title>
                  {{ $t('label.unmanaged.volumes') }}
                  <a-tooltip :title="$t('message.volumes.unmanaged')">
                    <info-circle-outlined />
                  </a-tooltip>
                  <a-button
                    style="margin-left: 12px; margin-top: 4px"
                    :loading="unmanagedVolumesLoading"
                    size="small"
                    shape="round"
                    @click="fetchUnmanagedVolumes()" >
                    <template #icon><reload-outlined /></template>
                  </a-button>
                  <span style="float: right; width: 50%">
                    <search-view
                      :searchFilters="searchFilters.unmanaged"
                      :searchParams="searchParams.unmanaged"
                      :apiName="listVolumesApi.unmanaged"
                      @search="searchUnmanagedVolumes"
                    />
                  </span>
                </template>
                <a-table
                  class="volumes-card-table"
                  :loading="unmanagedVolumesLoading"
                  :rowSelection="unmanagedVolumeSelection"
                  :rowKey="(record, index) => index"
                  :columns="unmanagedVolumesColumns"
                  :data-source="unmanagedVolumes"
                  :pagination="false"
                  size="middle"
                  :rowClassName="getRowClassName"
                >
                  <template #bodyCell="{ column, text }">
                    <template v-if="column.key === 'state'">
                      <status :text="text ? text : ''" displayText />
                    </template>
                  </template>
                </a-table>
                <div class="volumes-card-footer">
                  <a-pagination
                    class="row-element"
                    size="small"
                    :current="page.unmanaged"
                    :pageSize="pageSize.unmanaged"
                    :total="itemCount.unmanaged"
                    :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page.unmanaged-1)*pageSize.unmanaged))}-${Math.min(page.unmanaged*pageSize.unmanaged, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
                    @change="fetchUnmanagedVolumes"
                    showQuickJumper>
                    <template #buildOptionText="props">
                      <span>{{ props.value }} / {{ $t('label.page') }}</span>
                    </template>
                  </a-pagination>
                  <div :span="24" class="action-button-right">
                    <a-button
                      :loading="importUnmanagedVolumeLoading"
                      :disabled="!(('importVolume' in $store.getters.apis) && unmanagedVolumesSelectedRowKeys.length > 0)"
                      type="primary"
                      @click="onImportVolumeAction">
                      <template #icon><import-outlined /></template>
                      {{ $t('label.import.volume') }}
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
            <a-col :md="24" :lg="12">
              <a-card class="volumes-card">
                <template #title>
                  {{ $t('label.managed.volumes') }}
                  <a-tooltip :title="$t('message.volumes.managed')">
                    <info-circle-outlined />
                  </a-tooltip>
                  <a-button
                    style="margin-left: 12px; margin-top: 4px"
                    :loading="managedVolumesLoading"
                    size="small"
                    shape="round"
                    @click="fetchManagedVolumes()" >
                    <template #icon><reload-outlined /></template>
                  </a-button>
                  <span style="float: right; width: 50%">
                    <search-view
                      :searchFilters="searchFilters.managed"
                      :searchParams="searchParams.managed"
                      :apiName="listVolumesApi.managed"
                      @search="searchManagedVolumes"
                    />
                  </span>
                </template>
                <a-table
                  class="volumes-card-table"
                  :loading="managedVolumesLoading"
                  :rowSelection="managedVolumeSelection"
                  :rowKey="(record, index) => index"
                  :columns="managedVolumesColumns"
                  :data-source="managedVolumes"
                  :pagination="false"
                  size="middle"
                  :rowClassName="getRowClassName"
                >
                  <template #bodyCell="{ column, text, record }">
                    <template v-if="column.key === 'name'">
                      <router-link :to="{ path: '/volume/' + record.id }">{{ text }}</router-link>
                    </template>
                    <template v-if="column.key === 'state'">
                      <status :text="text ? text : ''" displayText />
                    </template>
                  </template>
                </a-table>
                <div class="volumes-card-footer">
                  <a-pagination
                    class="row-element"
                    size="small"
                    :current="page.managed"
                    :pageSize="pageSize.managed"
                    :total="itemCount.managed"
                    :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page.managed-1)*pageSize.managed))}-${Math.min(page.managed*pageSize.managed, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
                    @change="fetchManagedVolumes"
                    showQuickJumper>
                    <template #buildOptionText="props">
                      <span>{{ props.value }} / {{ $t('label.page') }}</span>
                    </template>
                  </a-pagination>
                  <div :span="24" class="action-button-right">
                    <a-button
                      :disabled="!(('unmanageVolume' in $store.getters.apis) && managedVolumesSelectedRowKeys.length > 0)"
                      type="primary"
                      @click="onUnmanageVolumeAction">
                      <template #icon><disconnect-outlined /></template>
                      {{ managedVolumesSelectedRowKeys.length > 1 ? $t('label.action.unmanage.volumes') : $t('label.action.unmanage.volume') }}
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </a-card>

        <a-modal
          v-if="showImportForm"
          :visible="showImportForm"
          :title="$t('label.import.volume')"
          :closable="true"
          :maskClosable="false"
          :footer="null"
          :cancelText="$t('label.cancel')"
          @cancel="onCloseImportVolumeForm"
          centered
          width="auto">
          <a-alert type="warning" style="margin-bottom: 20px">
            <template #message>
              <label v-html="$t('message.import.volume')"></label>
            </template>
          </a-alert>
          <a-form
            :ref="importFormRef"
            :model="importForm"
            @finish="handleSubmitImportVolumeForm"
            v-ctrl-enter="handleSubmitImportVolumeForm"
            class="import-form"
          >
            <a-form-item
              name="name"
              ref="name"
              :label="$t('label.name')">
              <a-input v-model:value="importForm.name" />
            </a-form-item>

            <a-form-item
              name="accounttype"
              ref="accounttype"
              :label="$t('label.accounttype')">
              <a-select
                @change="changeAccountType"
                v-model:value="importForm.selectedAccountType"
                v-focus="true"
                showSearch
                optionFilterProp="value"
                :filterOption="(input, option) => {
                  return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }">
                <a-select-option :value="null"></a-select-option>
                <a-select-option :value="$t('label.account')">{{ $t('label.account') }}</a-select-option>
                <a-select-option :value="$t('label.project')">{{ $t('label.project') }}</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item
              v-if="importForm.selectedAccountType === $t('label.account') || importForm.selectedAccountType === $t('label.project')"
              name="domain"
              ref="domain"
              :label="$t('label.domain')">
              <a-select
                @change="changeDomain"
                v-model:value="importForm.selectedDomain"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="domain in domains" :key="domain.name" :value="domain.id" :label="domain.path || domain.name || domain.description">
                <span>
                  <resource-icon v-if="domain && domain.icon" :image="domain.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <block-outlined v-else style="margin-right: 5px" />
                  {{ domain.path || domain.name || domain.description }}
                </span>
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item
              v-if="importForm.selectedAccountType === $t('label.account')"
              name="account"
              ref="account"
              :label="$t('label.account')">
              <a-select
                @change="changeAccount"
                v-model:value="importForm.selectedAccount"
                showSearch
                optionFilterProp="value"
                :filterOption="(input, option) => {
                    return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }" >
                <a-select-option v-for="account in accounts" :key="account.name" :value="account.name">
                  <span>
                    <resource-icon v-if="account && account.icon" :image="account.icon.base64image" size="1x" style="margin-right: 5px"/>
                    <team-outlined v-else style="margin-right: 5px" />
                    {{ account.name }}
                  </span>
                </a-select-option>
              </a-select>
              <span v-if="importForm.accountError" class="required">{{ $t('label.required') }}</span>
            </a-form-item>

            <a-form-item
              v-if="importForm.selectedAccountType === $t('label.project')"
              name="project"
              ref="project"
              :label="$t('label.project')">
              <a-select
                @change="changeProject"
                v-model:value="importForm.selectedProject"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option v-for="project in projects" :key="project.id" :value="project.id" :label="project.name">
                <span>
                  <resource-icon v-if="project && project.icon" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <project-outlined v-else style="margin-right: 5px" />
                  {{ project.name }}
                </span>
                </a-select-option>
              </a-select>
              <span v-if="importForm.projectError" class="required">{{ $t('label.required') }}</span>
            </a-form-item>

            <a-form-item
              name="diskoffering"
              ref="diskoffering"
              :label="$t('label.diskoffering')">
              <a-select
                v-model:value="importForm.selectedDiskoffering"
                :placeholder="$t('label.diskofferingid')"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="(offering, index) in diskOfferings"
                  :value="offering.id"
                  :key="index"
                  :label="offering.displaytext || offering.name">
                  {{ offering.displaytext || offering.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <div :span="24" class="action-button">
              <a-button @click="onCloseImportVolumeForm">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" ref="submit" @click="handleSubmitImportVolumeForm">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-modal>
      </div>
    </a-col>
  </a-row>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import Status from '@/components/widgets/Status'
import SearchView from '@/components/view/SearchView'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'

export default {
  components: {
    TooltipLabel,
    Breadcrumb,
    Status,
    SearchView,
    ResourceIcon
  },
  name: 'ManageVolumes',
  data () {
    const unmanagedVolumesColumns = [
      {
        title: this.$t('label.filename'),
        dataIndex: 'name',
        width: 250
      },
      {
        key: 'format',
        title: this.$t('label.format'),
        dataIndex: 'format'
      },
      {
        title: this.$t('label.size'),
        dataIndex: 'size'
      },
      {
        title: this.$t('label.virtualsize'),
        dataIndex: 'virtualsize'
      }
    ]
    const managedVolumesColumns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 100
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state'
      },
      {
        title: this.$t('label.type'),
        dataIndex: 'type'
      },
      {
        title: this.$t('label.account'),
        dataIndex: 'account'
      },
      {
        title: this.$t('label.vmname'),
        dataIndex: 'vmname'
      }
    ]
    return {
      domains: [],
      accounts: [],
      projects: [],
      diskOfferings: [],
      options: {
        zones: [],
        pods: [],
        clusters: [],
        hosts: [],
        pools: []
      },
      rowCount: {},
      optionLoading: {
        zones: false,
        pods: false,
        clusters: false,
        hosts: false,
        pools: false
      },
      page: {
        unmanaged: 1,
        managed: 1
      },
      pageSize: {
        unmanaged: 10,
        managed: 10
      },
      searchFilters: {
        unmanaged: [],
        managed: []
      },
      searchParams: {
        unmanaged: {},
        managed: {}
      },
      itemCount: {},
      zone: {},
      pod: {},
      cluster: {},
      values: undefined,
      zoneId: undefined,
      podId: undefined,
      clusterId: undefined,
      hostId: undefined,
      poolId: undefined,
      diskpath: undefined,
      poolscope: 'zone',
      listVolumesApi: {
        unmanaged: 'listVolumesForImport',
        managed: 'listVolumes'
      },
      unmanagedVolumesColumns,
      unmanagedVolumesLoading: false,
      unmanagedVolumes: [],
      unmanagedVolumesSelectedRowKeys: [],
      importUnmanagedVolumeLoading: false,
      managedVolumesColumns,
      managedVolumesLoading: false,
      managedVolumes: [],
      managedVolumesSelectedRowKeys: [],
      showImportForm: false,
      selectedUnmanagedVolume: null,
      query: {}
    }
  },
  created () {
    this.page.unmanaged = parseInt(this.$route.query.unmanagedpage || 1)
    this.page.managed = parseInt(this.$route.query.managedpage || 1)
    this.initForm()
    this.fetchData()
    this.fetchDomains()
  },
  computed: {
    isPageAllowed () {
      if (this.$route.meta.permission) {
        for (var apiName of this.$route.meta.permission) {
          if (!(apiName in this.$store.getters.apis)) {
            return false
          }
        }
      }
      return true
    },
    isUnmanaged () {
      return this.selectedSourceAction === 'unmanaged'
    },
    showPod () {
      return this.poolscope !== 'zone'
    },
    showCluster () {
      return this.poolscope !== 'zone'
    },
    showHost () {
      return this.poolscope === 'host'
    },
    params () {
      return {
        zones: {
          list: 'listZones',
          isLoad: true,
          field: 'zoneid',
          options: {
            showicon: true
          }
        },
        pods: {
          list: 'listPods',
          isLoad: false,
          options: {
            zoneid: _.get(this.zone, 'id')
          },
          field: 'podid'
        },
        clusters: {
          list: 'listClusters',
          isLoad: false,
          options: {
            zoneid: _.get(this.zone, 'id'),
            podid: this.podId
          },
          field: 'clusterid'
        },
        hosts: {
          list: 'listHosts',
          isLoad: false,
          options: {
            zoneid: _.get(this.zone, 'id'),
            podid: this.podId,
            clusterid: this.clusterId
          },
          field: 'hostid'
        },
        pools: {
          list: 'listStoragePools',
          isLoad: false,
          options: {
            zoneid: _.get(this.zone, 'id'),
            podid: this.podId,
            clusterid: this.clusterId,
            hostid: this.hostId,
            scope: this.poolscope
          },
          field: 'poolid'
        }
      }
    },
    viewLoading () {
      for (var key in this.optionLoading) {
        if (this.optionLoading[key]) {
          return true
        }
      }
      return this.unmanagedVolumesLoading || this.managedVolumesLoading
    },
    zoneSelectOptions () {
      return this.options.zones.map((zone) => {
        return {
          label: zone.name,
          value: zone.id,
          icon: zone?.icon?.base64image || ''
        }
      })
    },
    podSelectOptions () {
      const options = this.options.pods.map((pod) => {
        return {
          label: pod.name,
          value: pod.id
        }
      })
      return options
    },
    clusterSelectOptions () {
      const options = this.options.clusters.map((cluster) => {
        return {
          label: cluster.name,
          value: cluster.id
        }
      })
      return options
    },
    hostSelectOptions () {
      const options = this.options.hosts.map((host) => {
        return {
          label: host.name,
          value: host.id
        }
      })
      return options
    },
    poolSelectOptions () {
      const options = this.options.pools.map((pool) => {
        return {
          label: pool.name,
          value: pool.id
        }
      })
      return options
    },
    unmanagedVolumeSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.unmanagedVolumesSelectedRowKeys || [],
        onChange: this.onUnmanagedVolumeSelectRow
      }
    },
    managedVolumeSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.managedVolumesSelectedRowKeys || [],
        onChange: this.onManagedVolumeSelectRow,
        getCheckboxProps: (record) => {
          return {
            disabled: record.virtualmachineid !== undefined || record.state !== 'Ready' || record.hypervisor !== 'KVM'
          }
        }
      }
    },
    selectedCluster () {
      if (this.options.clusters &&
        this.options.clusters.length > 0 &&
        this.clusterId) {
        return _.find(this.options.clusters, (option) => option.id === this.clusterId)
      }
      return {}
    },
    selectedHost () {
      if (this.options.hosts &&
          this.options.hosts.length > 0 &&
          this.hostId) {
        return _.find(this.options.hosts, (option) => option.id === this.hostId)
      }
      return {}
    },
    selectedPool () {
      if (this.options.pools &&
          this.options.pools.length > 0 &&
          this.poolId) {
        return _.find(this.options.pools, (option) => option.id === this.poolId)
      }
      return {}
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
      })
      this.importFormRef = ref()
      this.importForm = reactive({
      })
    },
    fetchData () {
      this.unmanagedVolumes = []
      this.managedVolumes = []
      _.each(this.params, (param, name) => {
        if (param.isLoad) {
          this.fetchOptions(param, name)
        }
      })
    },
    filterOption (input, option) {
      return (
        option.label.toUpperCase().indexOf(input.toUpperCase()) >= 0
      )
    },
    fetchOptions (param, name, exclude) {
      if (exclude && exclude.length > 0) {
        if (exclude.includes(name)) {
          return
        }
      }
      this.optionLoading[name] = true
      param.loading = true
      param.opts = []
      const options = param.options || {}
      if (!('listall' in options) && !['zones', 'pods', 'clusters', 'hosts', 'pools'].includes(name)) {
        options.listall = true
      }
      api(param.list, options).then((response) => {
        param.loading = false
        _.map(response, (responseItem, responseKey) => {
          if (Object.keys(responseItem).length === 0) {
            this.rowCount[name] = 0
            this.options[name] = []
            return
          }
          if (!responseKey.includes('response')) {
            return
          }
          _.map(responseItem, (response, key) => {
            if (key === 'count') {
              this.rowCount[name] = response
              return
            }
            if (name === 'clusters') {
              response = response.filter(cluster => ['KVM'].includes(cluster.hypervisortype))
            }
            param.opts = response
            this.options[name] = response
          })
          this.handleFetchOptionsSuccess(name, param)
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      }).finally(() => {
        this.optionLoading[name] = false
      })
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    handleFetchOptionsSuccess (name, param) {
      if (['zones', 'pods', 'clusters', 'hosts', 'pools'].includes(name)) {
        let paramid = ''
        const query = Object.assign({}, this.$route.query)
        if (query[param.field] && _.find(this.options[name], (option) => option.id === query[param.field])) {
          paramid = query[param.field]
        }
        if (!paramid && this.options[name].length > 0) {
          paramid = (this.options[name])[0].id
        }
        if (paramid) {
          this.form[param.field] = paramid
          if (name === 'zones') {
            this.onSelectZoneId(paramid)
          } else if (name === 'pods') {
            this.form.podid = paramid
            this.onSelectPodId(paramid)
          } else if (name === 'clusters') {
            this.form.clusterid = paramid
            this.onSelectClusterId(paramid)
          } else if (name === 'hosts') {
            this.form.hostid = paramid
            this.onSelectHostId(paramid)
          } else if (name === 'pools') {
            this.form.poolid = paramid
            this.onSelectPoolId(paramid)
          }
        }
      }
    },
    updateQuery (field, value) {
      const query = Object.assign({}, this.$route.query)
      if (query[field] === value + '') {
        return
      }
      query[field] = value
      if (['zoneid', 'podid', 'clusterid', 'hostid', 'poolid'].includes(field)) {
        query.managedpage = 1
        query.unmanagedpage = 1
        this.searchParams.managed.keyword = null
        this.searchParams.unmanaged.keyword = null
      }
      this.$router.push({ query })
    },
    resetLists () {
      this.page.unmanaged = 1
      this.unmanagedVolumes = []
      this.unmanagedVolumesSelectedRowKeys = []
      this.page.managed = 1
      this.managedVolumes = []
      this.managedVolumesSelectedRowKeys = []
    },
    onSelectZoneId (value) {
      this.zoneId = value
      this.podId = null
      this.clusterId = null
      this.hostId = null
      this.poolId = null
      this.zone = _.find(this.options.zones, (option) => option.id === value)
      this.resetLists()
      this.form.clusterid = undefined
      this.form.podid = undefined
      this.form.hostid = undefined
      this.form.poolid = undefined
      this.updateQuery('zoneid', value)
      this.fetchOptions(this.params.pods, 'pods')
    },
    onSelectPodId (value) {
      this.podId = value
      this.pod = _.find(this.options.pods, (option) => option.id === value)
      this.resetLists()
      this.clusterId = null
      this.form.clusterid = undefined
      this.hostId = null
      this.poolId = null
      this.form.hostid = undefined
      this.form.poolid = undefined
      this.updateQuery('podid', value)
      this.fetchOptions(this.params.clusters, 'clusters', value)
    },
    onSelectClusterId (value) {
      this.clusterId = value
      this.cluster = _.find(this.options.clusters, (option) => option.id === value)
      this.resetLists()
      this.hostId = null
      this.poolId = null
      this.form.hostid = undefined
      this.form.poolid = undefined
      this.updateQuery('clusterid', value)
      if (this.showHost) {
        this.fetchOptions(this.params.hosts, 'hosts', value)
      } else {
        this.fetchOptions(this.params.pools, 'pools', value)
      }
    },
    onSelectHostId (value) {
      this.hostId = value
      this.updateQuery('scope', 'local')
      this.updateQuery('hostid', value)
      this.fetchOptions(this.params.pools, 'pools', value)
    },
    onSelectPoolId (value) {
      this.poolId = value
      this.updateQuery('poolid', value)
      this.fetchVolumes()
    },
    onSelectPoolScope (value) {
      this.poolscope = value
      this.zoneId = null
      this.podId = null
      this.clusterId = null
      this.poolId = null
      this.updateQuery('scope', value)
      this.fetchOptions(this.params.zones, 'zones', value)
    },
    fetchDomains () {
      api('listDomains', {
        response: 'json',
        listAll: true,
        showicon: true,
        details: 'min'
      }).then(response => {
        this.domains = response.listdomainsresponse.domain || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchAccounts () {
      this.loading = true
      api('listAccounts', {
        response: 'json',
        domainId: this.importForm.selectedDomain,
        showicon: true,
        state: 'Enabled',
        isrecursive: false
      }).then(response => {
        this.accounts = response.listaccountsresponse.account || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchProjects () {
      this.loading = true
      api('listProjects', {
        response: 'json',
        domainId: this.importForm.selectedDomain,
        state: 'Active',
        showicon: true,
        details: 'min',
        isrecursive: false
      }).then(response => {
        this.projects = response.listprojectsresponse.project || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    changeAccountType () {
      this.importForm.selectedDomain = null
      this.importForm.selectedAccount = null
      this.importForm.selectedProject = null
      this.importForm.selectedDiskoffering = null
      this.diskOfferings = {}
    },
    changeDomain () {
      this.importForm.selectedAccount = null
      this.importForm.selectedProject = null
      this.importForm.selectedDiskoffering = null
      this.diskOfferings = {}
      this.fetchAccounts()
      this.fetchProjects()
    },
    changeAccount () {
      this.importForm.selectedProject = null
      this.importForm.selectedDiskoffering = null
      this.diskOfferings = {}
      this.fetchDiskOfferings()
    },
    changeProject () {
      this.importForm.selectedAccount = null
      this.importForm.selectedDiskoffering = null
      this.diskOfferings = {}
      this.fetchDiskOfferings()
    },
    fetchDiskOfferings () {
      this.loading = true
      const selectedPool = this.options.pools.filter(pool => pool.id === this.poolId)
      const storagetype = selectedPool[0].scope === 'HOST' ? 'local' : 'shared'
      var params = {
        zoneid: this.zoneId,
        storageid: this.poolId,
        storagetype: storagetype,
        encrypt: false,
        listall: true
      }
      if (this.importForm.selectedAccountType === 'Account') {
        params.domainid = this.importForm.selectedDomain
        params.account = this.importForm.selectedAccount
      } else if (this.importForm.selectedAccountType === 'Project') {
        params.domainid = this.importForm.selectedDomain
        params.projectid = this.importForm.selectedProject
      }

      api('listDiskOfferings', params).then(json => {
        this.diskOfferings = json.listdiskofferingsresponse.diskoffering || []
      }).finally(() => {
        this.loading = false
      })
    },
    fetchVolumes () {
      this.fetchUnmanagedVolumes()
      this.fetchManagedVolumes()
    },
    fetchUnmanagedVolumes (page, pageSize) {
      const params = {
        storageid: this.poolId
      }
      const query = Object.assign({}, this.$route.query)
      this.page.unmanaged = page || parseInt(query.unmanagedpage) || this.page.unmanaged
      this.updateQuery('unmanagedpage', this.page.unmanaged)
      params.page = this.page.unmanaged
      this.pageSize.unmanaged = pageSize || this.pageSize.unmanaged
      params.pagesize = this.pageSize.unmanaged
      this.unmanagedVolumes = []
      this.unmanagedVolumesSelectedRowKeys = []
      if (this.searchParams.unmanaged.keyword) {
        params.keyword = this.searchParams.unmanaged.keyword
      }
      if (!this.poolId) {
        return
      }
      this.unmanagedVolumesLoading = true
      this.searchParams.unmanaged = params

      const apiName = this.listVolumesApi.unmanaged

      api(apiName, params).then(json => {
        const response = json.listvolumesforimportresponse
        const listUnmanagedVolumes = response.volumeforimport
        if (this.arrayHasItems(listUnmanagedVolumes)) {
          for (let index = (this.page.unmanaged - 1) * this.pageSize.unmanaged; index < this.page.unmanaged * this.pageSize.unmanaged - 1; index++) {
            if (listUnmanagedVolumes[index]) {
              this.unmanagedVolumes.push(listUnmanagedVolumes[index])
            }
          }
        }
        this.itemCount.unmanaged = response.count
      }).finally(() => {
        this.unmanagedVolumesLoading = false
      })
    },
    searchUnmanagedVolumes (params) {
      this.searchParams.unmanaged.keyword = params.searchQuery
      this.fetchUnmanagedVolumes()
    },
    fetchManagedVolumes (page, pageSize) {
      const params = {
        listall: true,
        projectId: -1,
        storageid: this.poolId
      }
      const query = Object.assign({}, this.$route.query)
      this.page.managed = page || parseInt(query.managedpage) || this.page.managed
      this.updateQuery('managedpage', this.page.managed)
      params.page = this.page.managed
      this.pageSize.managed = pageSize || this.pageSize.managed
      params.pagesize = this.pageSize.managed
      this.managedVolumes = []
      this.managedVolumesSelectedRowKeys = []
      if (this.searchParams.managed.keyword) {
        params.keyword = this.searchParams.managed.keyword
      }
      if (!this.poolId) {
        return
      }
      this.managedVolumesLoading = true
      this.searchParams.managed = params
      api(this.listVolumesApi.managed, params).then(json => {
        const response = json.listvolumesresponse
        const listManagedVolumes = response.volume
        if (this.arrayHasItems(listManagedVolumes)) {
          this.managedVolumes = this.managedVolumes.concat(listManagedVolumes)
        }
        this.itemCount.managed = response.count
      }).finally(() => {
        this.managedVolumesLoading = false
      })
    },
    searchManagedVolumes (params) {
      this.searchParams.managed.keyword = params.searchQuery
      this.fetchManagedVolumes()
    },
    onUnmanagedVolumeSelectRow (value) {
      this.unmanagedVolumesSelectedRowKeys = value
    },
    onManagedVolumeSelectRow (value) {
      this.managedVolumesSelectedRowKeys = value
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    updateManageVolumeActionLoading (value) {
      this.importUnmanagedVolumeLoading = value
      if (!value) {
        this.fetchVolumes()
      }
    },
    onImportVolumeAction () {
      this.selectedUnmanagedVolume = null
      if (this.unmanagedVolumes.length > 0 &&
        this.unmanagedVolumesSelectedRowKeys.length > 0) {
        this.selectedUnmanagedVolume = this.unmanagedVolumes[this.unmanagedVolumesSelectedRowKeys[0]]
        this.importForm.name = this.selectedUnmanagedVolume.name
      }
      this.fetchDiskOfferings()
      this.showImportForm = true
    },
    handleSubmitImportVolumeForm () {
      if (this.selectedUnmanagedVolume === null) {
        return
      }
      this.values = toRaw(this.importForm)
      const volumeName = this.selectedUnmanagedVolume.name

      let variableKey = ''
      let variableValue = ''
      if (this.values.selectedAccountType === 'Account') {
        if (!this.values.selectedAccount) {
          this.importForm.accountError = true
          return
        }
        variableKey = 'account'
        variableValue = this.values.selectedAccount
      } else if (this.values.selectedAccountType === 'Project') {
        if (!this.values.selectedProject) {
          this.importForm.projectError = true
          return
        }
        variableKey = 'projectid'
        variableValue = this.values.selectedProject
      }

      var params = {
        diskofferingid: this.importForm.selectedDiskoffering,
        domainid: this.importForm.selectedDomain,
        [variableKey]: variableValue,
        storageid: this.poolId,
        path: this.selectedUnmanagedVolume.path,
        name: this.values.name
      }
      api('importVolume', params).then(json => {
        const jobId = json.importvolumeresponse.jobid
        this.$pollJob({
          jobId,
          title: this.$t('label.import.volume'),
          description: volumeName,
          loadingMessage: `${this.$t('label.import.volume')} ${volumeName} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: this.$t('message.success.import.volume') + ' ' + volumeName,
          successMethod: result => {
            this.fetchVolumes()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
      this.selectedUnmanagedVolume = null
      this.onCloseImportVolumeForm()
    },
    onCloseImportVolumeForm () {
      this.showImportForm = false
      this.importForm.selectedAccountType = null
      this.importForm.selectedDomain = null
      this.importForm.selectedAccount = null
      this.importForm.selectedProject = null
    },
    onUnmanageVolumeAction () {
      const self = this
      const title = this.managedVolumesSelectedRowKeys.length > 1
        ? this.$t('message.action.unmanage.volumes')
        : this.$t('message.action.unmanage.volume')
      var volumeNames = []
      for (var index of this.managedVolumesSelectedRowKeys) {
        volumeNames.push(this.managedVolumes[index].name)
      }
      const content = volumeNames.join(', ')
      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        content: content,
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.unmanageVolumes()
        }
      })
    },
    unmanageVolumes () {
      for (var index of this.managedVolumesSelectedRowKeys) {
        const vm = this.managedVolumes[index]
        var params = { id: vm.id }
        api('unmanageVolume', params).then(json => {
          const jobId = json.unmanagevolumeresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.unmanage.volume'),
            description: vm.name,
            loadingMessage: `${this.$t('label.unmanage.volume')} ${vm.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: this.$t('message.success.unmanage.volume') + ' ' + vm.name,
            successMethod: result => {
              if (index === this.managedVolumesSelectedRowKeys[this.managedVolumesSelectedRowKeys.length - 1]) {
                this.fetchVolumes()
              }
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }
    }
  }
}
</script>

<style scoped lang="less">
:deep(.ant-table-small) > .ant-table-content > .ant-table-body {
  margin: 0;
}

.import-form {
  width: 85vw;

  @media (min-width: 760px) {
    width: 500px;
  }

  display: flex;
  flex-direction: column;

  &__item {
    display: flex;
    flex-direction: column;
    width: 100%;
    margin-bottom: 10px;
  }

  &__label {
    display: flex;
    font-weight: bold;
    margin-bottom: 5px;
  }

  .required {
    margin-right: 2px;
    color: red;
    font-weight: bold;
  }
}
.volumes-card {
  height: 100%;
}
.source-dest-card {
  width: 50%;
  height: 100%;
}
.volumes-card-table {
  overflow-y: auto;
  margin-bottom: 100px;
}
.volumes-card-footer {
  height: 100px;
  position: absolute;
  bottom: 0;
  left: 0;
  margin-left: 10px;
  right: 0;
  margin-right: 10px;
}
.row-element {
  margin-top: 10px;
  margin-bottom: 10px;
}
.action-button-left {
  text-align: left;
}
.action-button-right {
  text-align: right;
}
.fetch-volumes-column {
  width: 50%;
  margin-left: 50%;
  padding-left: 24px;
}

.breadcrumb-card {
  margin-left: -24px;
  margin-right: -24px;
  margin-top: -16px;
  margin-bottom: 12px;
}

.ant-breadcrumb {
  vertical-align: text-bottom;
}

</style>
