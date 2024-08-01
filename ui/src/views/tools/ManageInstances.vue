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
      <a-row>
        <a-col
          :span="device === 'mobile' ? 24 : 12"
          style="padding-left: 12px; margin-top: 10px"
        >
          <breadcrumb :resource="resource">
            <template #end>
              <a-tooltip placement="bottom">
                <template #title>{{ $t('label.refresh') }}</template>
                <a-button
                  style="margin-top: 4px"
                  :loading="viewLoading"
                  shape="round"
                  size="small"
                  @click="fetchData()"
                >
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  {{ $t('label.refresh') }}
                </a-button>
              </a-tooltip>
            </template>
          </breadcrumb>
        </a-col>
      </a-row>
    </a-card>
    </a-col>
    <a-col
      :md="24">
      <div>
        <a-card>
          <a-alert
            type="info"
            :showIcon="true"
            :message="wizardTitle"
          >
            <template #description>
              <span v-html="wizardDescription" />
            </template>
          </a-alert>
          <br />
          <a-row :gutter="12">
            <a-card class="source-dest-card">
              <a-col :md="24" :lg="48">
                <a-form
                  style="min-width: 170px"
                  :ref="formRef"
                  :model="form"
                  :rules="rules"
                  layout="vertical"
                >
                  <a-col :md="24" :lg="24">
                    <a-form-item name="sourcehypervisor" ref="sourcehypervisor" :label="$t('label.source')">
                      <a-radio-group
                        style="text-align: center; width: 100%"
                        v-model:value="form.sourceHypervisor"
                        @change="selected => { onSelectHypervisor(selected.target.value) }"
                        buttonStyle="solid">
                        <a-radio-button value="vmware" style="width: 50%; text-align: center">
                          VMware
                        </a-radio-button>
                        <a-radio-button value="kvm" style="width: 50%; text-align: center">
                          KVM
                        </a-radio-button>
                      </a-radio-group>
                    </a-form-item>
                    <a-form-item name="sourceaction" ref="sourceaction" :label="$t('label.action')" v-if="sourceActions">
                      <a-select
                        v-model:value="form.sourceAction"
                        showSearch
                        optionFilterProp="label"
                        :filterOption="(input, option) => {
                          return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                        }"
                        @change="onSelectSourceAction"
                        :loading="optionLoading.sourcehypervisor"
                        v-focus="true"
                      >
                        <a-select-option v-for="opt in sourceActions" :key="opt.name" :label="opt.label">
                          <span>
                            {{ opt.label }}
                          </span>
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                    <a-col v-if="showExtHost" :md="24" :lg="12">
                        <a-form-item
                                name="hostname"
                                ref="hostname">
                          <template #label>
                            <tooltip-label
                              :title="$t('label.hostname')"
                              :tooltip="$t('label.ext.hostname.tooltip')"/>
                          </template>
                            <a-input
                                    v-model:value="form.hostname"
                            ></a-input>
                        </a-form-item>
                    </a-col>
                    <a-col v-if="showExtHost" :md="24" :lg="12">
                        <a-form-item
                                name="username"
                                ref="username">
                          <template #label>
                            <tooltip-label
                              :title="$t('label.username')"
                              :tooltip="$t('label.username.tooltip')"/>
                          </template>
                            <a-input
                                    v-model:value="form.username"
                            ></a-input>
                        </a-form-item>
                    </a-col>
                    <a-col v-if="showExtHost" :md="24" :lg="12">
                        <a-form-item
                                name="password"
                                ref="password">
                          <template #label>
                            <tooltip-label
                              :title="$t('label.password')"
                              :tooltip="$t('label.password.tooltip')"/>
                          </template>
                            <a-input-password
                                    v-model:value="form.password"
                            ></a-input-password>
                        </a-form-item>
                    </a-col>
                    <a-col v-if="showExtHost" :md="24" :lg="12">
                        <a-form-item
                                name="tmppath"
                                ref="tmppath">
                          <template #label>
                            <tooltip-label
                              :title="$t('label.tmppath')"
                              :tooltip="$t('label.tmppath.tooltip')"/>
                          </template>
                            <a-input
                                    v-model:value="form.tmppath"
                            ></a-input>
                        </a-form-item>
                    </a-col>
                </a-form>
              </a-col>
            </a-card>
            <!-- ------------ -->
            <!-- RIGHT COLUMN -->
            <!-- ------------ -->
            <a-card class="source-dest-card">
              <template #title>
                Destination
              </template>
              <a-col :md="24" :lg="48">
                <a-form
                  style="min-width: 170px"
                  :ref="formRef"
                  :model="form"
                  :rules="rules"
                  layout="vertical"
                >
                <a-form-item v-if="showPool" name="scope" ref="scope">
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
                    <a-select-option :value="'cluster'" :label="$t('label.clusterid')"> {{ $t('label.clusterid') }} </a-select-option>
                    <a-select-option :value="'zone'" :label="$t('label.zoneid')"> {{ $t('label.zoneid') }} </a-select-option>
                  </a-select>
                </a-form-item>
                  <a-form-item
                    name="zoneid"
                    ref="zoneid"
                    :label="isMigrateFromVmware ? $t('label.destination.zone') : $t('label.zoneid')"
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
                    :label="isMigrateFromVmware ? $t('label.destination.pod') : $t('label.podid')">
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
                    :label="isMigrateFromVmware ? $t('label.destination.cluster') : $t('label.clusterid')">
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
                  <a-form-item v-if="isDestinationKVM && isMigrateFromVmware && clusterId != undefined">
                    <SelectVmwareVcenter
                      @onVcenterTypeChanged="updateVmwareVcenterType"
                      @loadingVmwareUnmanagedInstances="() => this.unmanagedInstancesLoading = true"
                      @listedVmwareUnmanagedInstances="($e) => onListUnmanagedInstancesFromVmware($e)"
                    />
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
                    v-if="isDiskImport"
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
                  <a-form-item
                    v-if="showDiskPath"
                    name="diskpath"
                    ref="diskpath">
                    <template #label>
                          <tooltip-label
                            :title="$t('label.disk')"
                            :tooltip="$t('label.disk.tooltip')"/>
                    </template>
                    <a-input
                      v-model:value="form.diskpath"
                    ></a-input>
                  </a-form-item>
                  <a-col v-if="showDiskPath" :md="24" :lg="8">
                    <a-button
                        type="primary"
                        @click="onImportInstanceAction">
                      <template #icon><import-outlined /></template>
                      {{ $t('label.import.instance') }}
                    </a-button>
                  </a-col>
                </a-form>
              </a-col>
            </a-card>
          </a-row>
          <a-row v-if="showExtHost">
            <a-col class="fetch-instances-column">
              <div>
                <a-button
                  shape="round"
                  type="primary"
                  @click="() => { fetchExtKVMInstances() }">
                  {{ $t('label.fetch.instances') }}
                </a-button>
              </div>
            </a-col>
          </a-row>
          <a-divider />
          <a-row :gutter="12">
            <a-col v-if="!isDiskImport" :md="24" :lg="(!isMigrateFromVmware && showManagedInstances) ? 12 : 24">
              <a-card class="instances-card">
                <template #title>
                  {{ (isMigrateFromVmware && vmwareVcenterType === 'existing') ? $t('label.instances') : $t('label.unmanaged.instances') }}
                  <a-tooltip :title="(isMigrateFromVmware && vmwareVcenterType === 'existing') ? $t('message.instances.migrate.vmware') : $t('message.instances.unmanaged')">
                    <info-circle-outlined />
                  </a-tooltip>
                  <a-button
                    style="margin-left: 12px; margin-top: 4px"
                    :loading="unmanagedInstancesLoading"
                    size="small"
                    shape="round"
                    @click="fetchUnmanagedInstances()" >
                    <template #icon><reload-outlined /></template>
                  </a-button>
                  <span style="float: right; width: 50%">
                    <search-view
                      :searchFilters="searchFilters.unmanaged"
                      :searchParams="searchParams.unmanaged"
                      :apiName="listInstancesApi.unmanaged"
                      @search="searchUnmanagedInstances"
                    />
                  </span>
                </template>
                <a-table
                  v-if="!isExternal"
                  class="instances-card-table"
                  :loading="unmanagedInstancesLoading"
                  :rowSelection="unmanagedInstanceSelection"
                  :rowKey="(record, index) => index"
                  :columns="unmanagedInstancesColumns"
                  :data-source="unmanagedInstances"
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
                <a-table
                  v-if="isExternal"
                  class="instances-card-table"
                  :loading="unmanagedInstancesLoading"
                  :rowSelection="unmanagedInstanceSelection"
                  :rowKey="(record, index) => index"
                  :columns="externalInstancesColumns"
                  :data-source="unmanagedInstances"
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
                <div class="instances-card-footer">
                  <a-pagination
                    class="row-element"
                    size="small"
                    :current="page.unmanaged"
                    :pageSize="pageSize.unmanaged"
                    :total="itemCount.unmanaged"
                    :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page.unmanaged-1)*pageSize.unmanaged))}-${Math.min(page.unmanaged*pageSize.unmanaged, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
                    @change="fetchUnmanagedInstances"
                    showQuickJumper>
                    <template #buildOptionText="props">
                      <span>{{ props.value }} / {{ $t('label.page') }}</span>
                    </template>
                  </a-pagination>
                  <div :span="24" class="action-button-right">
                    <a-button
                      :loading="importUnmanagedInstanceLoading"
                      :disabled="!(('importUnmanagedInstance' in $store.getters.apis) && unmanagedInstancesSelectedRowKeys.length > 0)"
                      type="primary"
                      @click="onManageInstanceAction">
                      <template #icon><import-outlined /></template>
                      {{ $t('label.import.instance') }}
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
            <a-col :md="24" :lg="12" v-if="!isMigrateFromVmware && showManagedInstances">
              <a-card class="instances-card">
                <template #title>
                  {{ $t('label.managed.instances') }}
                  <a-tooltip :title="$t('message.instances.managed')">
                    <info-circle-outlined />
                  </a-tooltip>
                  <a-button
                    style="margin-left: 12px; margin-top: 4px"
                    :loading="managedInstancesLoading"
                    size="small"
                    shape="round"
                    @click="fetchManagedInstances()" >
                    <template #icon><reload-outlined /></template>
                  </a-button>
                  <span style="float: right; width: 50%">
                    <search-view
                      :searchFilters="searchFilters.managed"
                      :searchParams="searchParams.managed"
                      :apiName="listInstancesApi.managed"
                      @search="searchManagedInstances"
                    />
                  </span>
                </template>
                <a-table
                  class="instances-card-table"
                  :loading="managedInstancesLoading"
                  :rowSelection="managedInstanceSelection"
                  :rowKey="(record, index) => index"
                  :columns="managedInstancesColumns"
                  :data-source="managedInstances"
                  :pagination="false"
                  size="middle"
                  :rowClassName="getRowClassName"
                >
                  <template #bodyCell="{ column, text, record }">
                    <template v-if="column.key === 'name'">
                      <router-link :to="{ path: '/vm/' + record.id }">{{ text }}</router-link>
                    </template>
                    <template v-if="column.key === 'state'">
                      <status :text="text ? text : ''" displayText />
                    </template>
                  </template>
                </a-table>
                <div class="instances-card-footer">
                  <a-pagination
                    class="row-element"
                    size="small"
                    :current="page.managed"
                    :pageSize="pageSize.managed"
                    :total="itemCount.managed"
                    :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page.managed-1)*pageSize.managed))}-${Math.min(page.managed*pageSize.managed, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
                    @change="fetchManagedInstances"
                    showQuickJumper>
                    <template #buildOptionText="props">
                      <span>{{ props.value }} / {{ $t('label.page') }}</span>
                    </template>
                  </a-pagination>
                  <div :span="24" class="action-button-right">
                    <a-button

                      :disabled="!(('unmanageVirtualMachine' in $store.getters.apis) && managedInstancesSelectedRowKeys.length > 0)"
                      type="primary"
                      @click="onUnmanageInstanceAction">
                      <template #icon><disconnect-outlined /></template>
                      {{ managedInstancesSelectedRowKeys.length > 1 ? $t('label.action.unmanage.instances') : $t('label.action.unmanage.instance') }}
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </a-card>

        <a-modal
          v-if="showUnmanageForm"
          :visible="showUnmanageForm"
          :title="$t('label.import.instance')"
          :closable="true"
          :maskClosable="false"
          :footer="null"
          :cancelText="$t('label.cancel')"
          @cancel="showUnmanageForm = false"
          centered
          ref="importModal"
          width="auto">
          <import-unmanaged-instances
            class="importform"
            :resource="selectedUnmanagedInstance"
            :cluster="selectedCluster"
            :host="selectedHost"
            :pool="selectedPool"
            :importsource="selectedSourceAction"
            :zoneid="this.zoneId"
            :hypervisor="this.destinationHypervisor"
            :exthost="this.values?.hostname || ''"
            :username="this.values?.username || ''"
            :password="this.values?.password || ''"
            :tmppath="this.values?.tmppath || ''"
            :diskpath="this.values?.diskpath || ''"
            :isOpen="showUnmanageForm"
            :selectedVmwareVcenter="selectedVmwareVcenter"
            @refresh-data="fetchInstances"
            @close-action="closeImportUnmanagedInstanceForm"
            @loading-changed="updateManageInstanceActionLoading"
            @track-import-jobid="trackImportJobId"
          />
        </a-modal>
      </div>
    </a-col>
  </a-row>
</template>

<script>
import { message } from 'ant-design-vue'
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import Status from '@/components/widgets/Status'
import SearchView from '@/components/view/SearchView'
import ImportUnmanagedInstances from '@/views/tools/ImportUnmanagedInstance'
import ResourceIcon from '@/components/view/ResourceIcon'
import SelectVmwareVcenter from '@/views/tools/SelectVmwareVcenter'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'

export default {
  components: {
    TooltipLabel,
    Breadcrumb,
    Status,
    SearchView,
    ImportUnmanagedInstances,
    ResourceIcon,
    SelectVmwareVcenter
  },
  name: 'ManageVms',
  data () {
    const AllSourceActions = [
      {
        name: 'unmanaged',
        label: 'Manage/Unmanage existing instances',
        sourceDestHypervisors: {
          vmware: 'vmware',
          kvm: 'kvm'
        },
        wizardTitle: this.$t('label.desc.importexportinstancewizard'),
        wizardDescription: this.$t('message.desc.importexportinstancewizard')
      },
      {
        name: 'vmware',
        label: 'Migrate existing instances to KVM',
        sourceDestHypervisors: {
          vmware: 'kvm'
        },
        wizardTitle: this.$t('label.desc.importmigratefromvmwarewizard'),
        wizardDescription: this.$t('message.desc.importmigratefromvmwarewizard')
      },
      {
        name: 'external',
        label: 'Import Instance from remote KVM host',
        sourceDestHypervisors: {
          kvm: 'kvm'
        },
        wizardTitle: this.$t('label.desc.import.ext.kvm.wizard'),
        wizardDescription: this.$t('message.desc.import.ext.kvm.wizard')
      },
      {
        name: 'local',
        label: 'Import QCOW2 image from Local Storage',
        sourceDestHypervisors: {
          kvm: 'kvm'
        },
        wizardTitle: this.$t('label.desc.import.local.kvm.wizard'),
        wizardDescription: this.$t('message.desc.import.local.kvm.wizard')
      },
      {
        name: 'shared',
        label: 'Import QCOW2 image from Shared Storage',
        sourceDestHypervisors: {
          kvm: 'kvm'
        },
        wizardTitle: this.$t('label.desc.import.shared.kvm.wizard'),
        wizardDescription: this.$t('message.desc.import.shared.kvm.wizard')
      }
    ]
    const unmanagedInstancesColumns = [
      {
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 100
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'powerstate'
      },
      {
        title: this.$t('label.hostname'),
        dataIndex: 'hostname'
      },
      {
        title: this.$t('label.clustername'),
        dataIndex: 'clustername'
      },
      {
        title: this.$t('label.ostypename'),
        dataIndex: 'osdisplayname'
      }
    ]
    const externalInstancesColumns = [
      {
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 200
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'powerstate'
      }
    ]
    const managedInstancesColumns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: 100
      },
      {
        title: this.$t('label.instancename'),
        dataIndex: 'instancename'
      },
      {
        key: 'state',
        title: this.$t('label.state'),
        dataIndex: 'state'
      },
      {
        title: this.$t('label.hostname'),
        dataIndex: 'hostname'
      },
      {
        title: this.$t('label.templatename'),
        dataIndex: 'templatedisplaytext'
      }
    ]
    return {
      options: {
        hypervisors: [],
        zones: [],
        pods: [],
        clusters: [],
        hosts: [],
        pools: []
      },
      rowCount: {},
      optionLoading: {
        sourceaction: false,
        hypervisors: false,
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
      hypervisors: [],
      sourceHypervisor: 'vmware',
      destinationHypervisor: 'vmware',
      sourceActions: undefined,
      selectedSourceAction: undefined,
      wizardTitle: this.$t('label.desc.importexportinstancewizard'),
      wizardDescription: this.$t('message.desc.importexportinstancewizard'),
      zone: {},
      pod: {},
      cluster: {},
      values: undefined,
      zoneId: undefined,
      podId: undefined,
      clusterId: undefined,
      hostname: undefined,
      username: undefined,
      password: undefined,
      hostId: undefined,
      poolId: undefined,
      diskpath: undefined,
      tmppath: undefined,
      poolscope: 'cluster',
      listInstancesApi: {
        unmanaged: 'listUnmanagedInstances',
        managed: 'listVirtualMachines',
        migratefromvmware: 'listVmwareDcVms',
        external: 'listVmsForImport'
      },
      unmanagedInstancesColumns,
      externalInstancesColumns,
      AllSourceActions,
      unmanagedInstancesLoading: false,
      unmanagedInstances: [],
      unmanagedInstancesSelectedRowKeys: [],
      importUnmanagedInstanceLoading: false,
      managedInstancesColumns,
      managedInstancesLoading: false,
      managedInstances: [],
      managedInstancesSelectedRowKeys: [],
      showUnmanageForm: false,
      selectedUnmanagedInstance: {},
      query: {},
      vmwareVcenterType: undefined,
      selectedVmwareVcenter: undefined
    }
  },
  created () {
    this.page.unmanaged = parseInt(this.$route.query.unmanagedpage || 1)
    this.page.managed = parseInt(this.$route.query.managedpage || 1)
    this.initForm()
    this.fetchData()
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
    isExternal () {
      return this.selectedSourceAction === 'external'
    },
    isMigrateFromVmware () {
      return this.selectedSourceAction === 'vmware'
    },
    isDestinationKVM () {
      return this.destinationHypervisor === 'kvm'
    },
    showPod () {
      if (this.selectedSourceAction === 'shared') {
        return this.poolscope !== 'zone'
      }
      return (this.selectedSourceAction !== 'external')
    },
    showCluster () {
      if (this.selectedSourceAction === 'shared') {
        return this.poolscope !== 'zone'
      }
      return (this.selectedSourceAction !== 'external')
    },
    showHost () {
      return (this.selectedSourceAction === 'local')
    },
    showPool () {
      return (this.selectedSourceAction === 'shared')
    },
    showExtHost () {
      return (this.selectedSourceAction === 'external')
    },
    showDiskPath () {
      return ((this.selectedSourceAction === 'local') || (this.selectedSourceAction === 'shared'))
    },
    showManagedInstances () {
      return ((this.selectedSourceAction !== 'local') && (this.selectedSourceAction !== 'shared') && (this.selectedSourceAction !== 'external'))
    },
    isDiskImport () {
      return ((this.selectedSourceAction === 'local') || (this.selectedSourceAction === 'shared'))
    },
    getPoolScope () {
      if (this.selectedSourceAction === 'local') {
        return 'host'
      } else {
        return this.poolscope
      }
    },
    params () {
      return {
        zones: {
          list: 'listZones',
          isLoad: false,
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
            podid: this.podId,
            hypervisor: this.destinationHypervisor
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
            scope: this.getPoolScope
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
      return this.unmanagedInstancesLoading || this.managedInstancesLoading
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
    unmanagedInstanceSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.unmanagedInstancesSelectedRowKeys || [],
        onChange: this.onUnmanagedInstanceSelectRow
      }
    },
    managedInstanceSelection () {
      return {
        type: 'checkbox',
        selectedRowKeys: this.managedInstancesSelectedRowKeys || [],
        onChange: this.onManagedInstanceSelectRow
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
        sourceHypervisor: this.sourceHypervisor
      })
      this.rules = reactive({
        hostname: [{ required: true, message: this.$t('message.error.input.value') }],
        username: [{ required: true, message: this.$t('message.error.input.value') }],
        password: [{ required: true, message: this.$t('message.error.input.value') }]
      })
    },
    fetchData () {
      this.unmanagedInstances = []
      this.managedInstances = []
      this.onSelectHypervisor(this.sourceHypervisor)
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
      if (['zoneid', 'podid', 'clusterid'].includes(field)) {
        query.managedpage = 1
        query.unmanagedpage = 1
      }
      this.$router.push({ query })
    },
    resetLists () {
      this.page.unmanaged = 1
      this.unmanagedInstances = []
      this.unmanagedInstancesSelectedRowKeys = []
      this.page.managed = 1
      this.managedInstances = []
      this.managedInstancesSelectedRowKeys = []
    },
    onSelectHypervisor (value) {
      this.sourceHypervisor = value
      this.sourceActions = this.AllSourceActions.filter(x => x.sourceDestHypervisors[value])
      this.form.sourceAction = this.sourceActions[0].name || ''
      this.selectedVmwareVcenter = undefined
      this.onSelectSourceAction(this.form.sourceAction)
    },
    onSelectSourceAction (value) {
      this.selectedSourceAction = value
      const selectedAction = _.find(this.AllSourceActions, (option) => option.name === value)
      this.destinationHypervisor = selectedAction.sourceDestHypervisors[this.sourceHypervisor]
      this.wizardTitle = selectedAction.wizardTitle
      this.wizardDescription = selectedAction.wizardDescription
      this.form.zoneid = undefined
      this.form.podid = undefined
      this.form.clusterid = undefined
      this.fetchOptions(this.params.zones, 'zones')
      this.resetLists()
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
      this.updateQuery('podid', value)
      this.fetchOptions(this.params.clusters, 'clusters', value)
    },
    onSelectClusterId (value) {
      this.clusterId = value
      this.cluster = _.find(this.options.clusters, (option) => option.id === value)
      this.resetLists()
      this.updateQuery('clusterid', value)
      if (this.isUnmanaged) {
        this.fetchInstances()
      } else if (this.showHost) {
        this.fetchOptions(this.params.hosts, 'hosts', value)
      } else if (this.showPool) {
        this.fetchOptions(this.params.pools, 'pools', value)
      }
    },
    onSelectHostId (value) {
      this.hostId = value
      this.updateQuery('scope', 'local')
      this.fetchOptions(this.params.pools, 'pools', value)
    },
    onSelectPoolId (value) {
      this.poolId = value
    },
    onSelectPoolScope (value) {
      this.poolscope = value
      this.poolId = null
      this.updateQuery('scope', value)
      this.fetchOptions(this.params.pools, 'pools', value)
    },
    fetchInstances () {
      this.fetchUnmanagedInstances()
      if (this.isUnmanaged) {
        this.fetchManagedInstances()
      } else if (this.kvmOption === 'external') {
        this.fetchExternalInstances()
      }
    },
    fetchUnmanagedInstances (page, pageSize) {
      if (this.isExternal) {
        this.fetchExtKVMInstances(page, pageSize)
        return
      }
      const params = {
        clusterid: this.clusterId
      }
      const query = Object.assign({}, this.$route.query)
      this.page.unmanaged = page || parseInt(query.unmanagedpage) || this.page.unmanaged
      this.updateQuery('unmanagedpage', this.page.unmanaged)
      params.page = this.page.unmanaged
      this.pageSize.unmanaged = pageSize || this.pageSize.unmanaged
      params.pagesize = this.pageSize.unmanaged
      this.unmanagedInstances = []
      this.unmanagedInstancesSelectedRowKeys = []
      if (this.searchParams.unmanaged.keyword) {
        params.keyword = this.searchParams.unmanaged.keyword
      }
      if (!this.clusterId) {
        return
      }
      this.unmanagedInstancesLoading = true
      this.searchParams.unmanaged = params

      let apiName = this.listInstancesApi.unmanaged
      if (this.isMigrateFromVmware && this.selectedVmwareVcenter) {
        apiName = this.listInstancesApi.migratefromvmware
        if (this.selectedVmwareVcenter.vcenter) {
          params.datacentername = this.selectedVmwareVcenter.datacentername
          params.vcenter = this.selectedVmwareVcenter.vcenter
          params.username = this.selectedVmwareVcenter.username
          params.password = this.selectedVmwareVcenter.password
        } else {
          params.existingvcenterid = this.selectedVmwareVcenter.existingvcenterid
        }
      }

      api(apiName, params).then(json => {
        const response = this.isMigrateFromVmware ? json.listvmwaredcvmsresponse : json.listunmanagedinstancesresponse
        const listUnmanagedInstances = response.unmanagedinstance
        if (this.arrayHasItems(listUnmanagedInstances)) {
          this.unmanagedInstances = this.unmanagedInstances.concat(listUnmanagedInstances)
        }
        this.itemCount.unmanaged = response.count
      }).finally(() => {
        this.unmanagedInstancesLoading = false
      })
    },
    fetchExtKVMInstances (page, pageSize) {
      const params = {
        zoneid: this.zoneid
      }
      const query = Object.assign({}, this.$route.query)
      this.page.unmanaged = page || parseInt(query.unmanagedpage) || this.page.unmanaged
      this.updateQuery('unmanagedpage', this.page.unmanaged)
      params.page = this.page.unmanaged
      this.pageSize.unmanaged = pageSize || this.pageSize.unmanaged
      params.pagesize = this.pageSize.unmanaged
      this.unmanagedInstances = []
      this.unmanagedInstancesSelectedRowKeys = []
      if (this.searchParams.unmanaged.keyword) {
        params.keyword = this.searchParams.unmanaged.keyword
      }
      this.values = toRaw(this.form)
      this.unmanagedInstancesLoading = true
      params.zoneid = this.zoneId
      params.host = this.values.hostname
      params.username = this.values.username
      params.password = this.values.password
      params.hypervisor = this.destinationHypervisor
      var details = ['host', 'username', 'password']
      for (var detail of details) {
        if (!params[detail]) {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.' + detail.toLowerCase())
          })
          return
        }
      }
      this.searchParams.unmanaged = params
      api(this.listInstancesApi.external, params).then(json => {
        const listUnmanagedInstances = json.listvmsforimportresponse.unmanagedinstance
        if (this.arrayHasItems(listUnmanagedInstances)) {
          this.unmanagedInstances = this.unmanagedInstances.concat(listUnmanagedInstances)
        }
        this.itemCount.unmanaged = json.listvmsforimportresponse.count
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.unmanagedInstancesLoading = false
      })
    },
    searchUnmanagedInstances (params) {
      this.searchParams.unmanaged.keyword = params.searchQuery
      this.fetchUnmanagedInstances()
    },
    fetchManagedInstances (page, pageSize) {
      const params = {
        listall: true,
        clusterid: this.clusterId
      }
      const query = Object.assign({}, this.$route.query)
      this.page.managed = page || parseInt(query.managedpage) || this.page.managed
      this.updateQuery('managedpage', this.page.managed)
      params.page = this.page.managed
      this.pageSize.managed = pageSize || this.pageSize.managed
      params.pagesize = this.pageSize.managed
      this.managedInstances = []
      this.managedInstancesSelectedRowKeys = []
      if (this.searchParams.managed.keyword) {
        params.keyword = this.searchParams.managed.keyword
      }
      if (!this.clusterId) {
        return
      }
      this.managedInstancesLoading = true
      this.searchParams.managed = params
      api(this.listInstancesApi.managed, params).then(json => {
        const listManagedInstances = json.listvirtualmachinesresponse.virtualmachine
        if (this.arrayHasItems(listManagedInstances)) {
          this.managedInstances = this.managedInstances.concat(listManagedInstances)
        }
        this.itemCount.managed = json.listvirtualmachinesresponse.count
      }).finally(() => {
        this.managedInstancesLoading = false
      })
    },
    searchManagedInstances (params) {
      this.searchParams.managed.keyword = params.searchQuery
      this.fetchManagedInstances()
    },
    onUnmanagedInstanceSelectRow (value) {
      this.unmanagedInstancesSelectedRowKeys = value
    },
    onManagedInstanceSelectRow (value) {
      this.managedInstancesSelectedRowKeys = value
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
    updateManageInstanceActionLoading (value) {
      this.importUnmanagedInstanceLoading = value
      if (!value) {
        this.fetchInstances()
      }
    },
    onManageInstanceAction () {
      this.selectedUnmanagedInstance = {}
      if (this.unmanagedInstances.length > 0 &&
        this.unmanagedInstancesSelectedRowKeys.length > 0) {
        this.selectedUnmanagedInstance = this.unmanagedInstances[this.unmanagedInstancesSelectedRowKeys[0]]
        this.selectedUnmanagedInstance.ostypename = this.selectedUnmanagedInstance.osdisplayname
        this.selectedUnmanagedInstance.state = this.selectedUnmanagedInstance.powerstate
      }
      if (this.isMigrateFromVmware && this.selectedUnmanagedInstance.state === 'PowerOn' && this.selectedUnmanagedInstance.ostypename.toLowerCase().includes('windows')) {
        message.error({
          content: () => 'Cannot import Running Windows VMs, please gracefully shutdown the source VM before importing',
          style: {
            marginTop: '20vh',
            color: 'red'
          }
        })
        this.showUnmanageForm = false
      } else {
        this.showUnmanageForm = true
      }
    },
    onImportInstanceAction () {
      this.selectedUnmanagedInstance = {}
      this.values = toRaw(this.form)
      if (!this.values.diskpath) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.disk.path')
        })
        return
      }
      if (this.showPool && !this.values.poolid) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.please.enter.valid.value') + ': ' + this.$t('label.storagepool')
        })
        return
      }
      this.showUnmanageForm = true
    },
    closeImportUnmanagedInstanceForm () {
      this.selectedUnmanagedInstance = {}
      this.showUnmanageForm = false
    },
    onUnmanageInstanceAction () {
      const self = this
      const title = this.managedInstancesSelectedRowKeys.length > 1
        ? this.$t('message.action.unmanage.instances')
        : this.$t('message.action.unmanage.instance')
      var vmNames = []
      for (var index of this.managedInstancesSelectedRowKeys) {
        vmNames.push(this.managedInstances[index].name)
      }
      const content = vmNames.join(', ')
      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        content: content,
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.unmanageInstances()
        }
      })
    },
    trackImportJobId (details) {
      const jobId = details[0]
      const name = details[1]
      this.$pollJob({
        jobId,
        title: this.$t('label.import.instance'),
        description: this.$t('label.import.instance'),
        loadingMessage: `${this.$t('label.import.instance')} ${name} ${this.$t('label.in.progress')}`,
        catchMessage: this.$t('error.fetching.async.job.result'),
        successMessage: this.$t('message.success.import.instance') + ' ' + name,
        successMethod: (result) => {
          this.fetchInstances()
        }
      })
    },
    unmanageInstances () {
      for (var index of this.managedInstancesSelectedRowKeys) {
        const vm = this.managedInstances[index]
        var params = { id: vm.id }
        api('unmanageVirtualMachine', params).then(json => {
          const jobId = json.unmanagevirtualmachineresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.unmanage.instance'),
            description: vm.name,
            loadingMessage: `${this.$t('label.unmanage.instance')} ${vm.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: this.$t('message.success.unmanage.instance') + ' ' + vm.name,
            successMethod: result => {
              if (index === this.managedInstancesSelectedRowKeys[this.managedInstancesSelectedRowKeys.length - 1]) {
                this.fetchInstances()
              }
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }
    },
    onListUnmanagedInstancesFromVmware (obj) {
      this.selectedVmwareVcenter = obj.params
      this.unmanagedInstances = obj.response.unmanagedinstance
      this.itemCount.unmanaged = obj.response.count
      this.unmanagedInstancesLoading = false
    },
    updateVmwareVcenterType (type) {
      this.vmwareVcenterType = type
    }
  }
}
</script>

<style scoped lang="less">
:deep(.ant-table-small) > .ant-table-content > .ant-table-body {
  margin: 0;
}

.importform {
  width: 80vw;
}
.instances-card {
  height: 100%;
}
.source-dest-card {
  width: 50%;
  height: 100%;
}
.instances-card-table {
  overflow-y: auto;
  margin-bottom: 100px;
}
.instances-card-footer {
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
.fetch-instances-column {
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
