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
    <a-affix :offsetTop="this.$store.getters.shutdownTriggered ? 103 : 78">
      <a-card class="breadcrumb-card" style="z-index: 10">
        <a-row>
          <a-col :span="device === 'mobile' ? 24 : 12" style="padding-left: 12px">
            <breadcrumb :resource="resource">
              <template #end>
                <a-button
                  :loading="loading"
                  style="margin-bottom: 5px"
                  shape="round"
                  size="small"
                  @click="fetchData({ irefresh: true })">
                  <template #icon><reload-outlined /></template>
                  {{ $t('label.refresh') }}
                </a-button>
                <a-switch
                  v-if="!dataView && ['vm', 'volume', 'zone', 'cluster', 'host', 'storagepool', 'managementserver'].includes($route.name)"
                  style="margin-left: 8px"
                  :checked-children="$t('label.metrics')"
                  :un-checked-children="$t('label.metrics')"
                  :checked="$store.getters.metrics"
                  @change="(checked, event) => { $store.dispatch('SetMetrics', checked) }"/>
                <a-switch
                  v-if="!projectView && hasProjectId"
                  style="margin-left: 8px"
                  :checked-children="$t('label.projects')"
                  :un-checked-children="$t('label.projects')"
                  :checked="$store.getters.listAllProjects"
                  @change="(checked, event) => { $store.dispatch('SetListAllProjects', checked) }"/>
                <a-tooltip placement="right">
                  <template #title>
                    {{ $t('label.filterby') }}
                  </template>
                  <a-select
                    v-if="!dataView && filters && filters.length > 0"
                    :placeholder="$t('label.filterby')"
                    :value="filterValue"
                    style="min-width: 120px; margin-left: 10px"
                    @change="changeFilter"
                    showSearch
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }" >
                    <template #suffixIcon><filter-outlined class="ant-select-suffix" /></template>
                    <a-select-option
                      v-if="['Admin', 'DomainAdmin'].includes($store.getters.userInfo.roletype) &&
                      ['vm', 'iso', 'template', 'pod', 'cluster', 'host', 'systemvm', 'router', 'storagepool'].includes($route.name) ||
                      ['account'].includes($route.name)"
                      key="all"
                      :label="$t('label.all')">
                      {{ $t('label.all') }}
                    </a-select-option>
                    <a-select-option
                      v-for="filter in filters"
                      :key="filter"
                      :label="$t('label.' + (['comment'].includes($route.name) ? 'filter.annotations.' : '') + filter)">
                      {{ $t('label.' + (['comment'].includes($route.name) ? 'filter.annotations.' : '') + filter) }}
                      <clock-circle-outlined v-if="['comment'].includes($route.name) && !['Admin'].includes($store.getters.userInfo.roletype) && filter === 'all'" />
                    </a-select-option>
                  </a-select>
                </a-tooltip>
              </template>
            </breadcrumb>
          </a-col>
          <a-col
            :span="device === 'mobile' ? 24 : 12"
            :style="device === 'mobile' ? { float: 'right', 'margin-top': '12px', 'margin-bottom': '-6px', display: 'table' } : { float: 'right', display: 'table', 'margin-bottom': '-6px' }" >
            <slot name="action" v-if="dataView && $route.path.startsWith('/publicip')"></slot>
            <action-button
              v-else
              :style="dataView ? { float: device === 'mobile' ? 'left' : 'right' } : { 'margin-right': '10px', display: getStyle(), padding: '5px' }"
              :loading="loading"
              :actions="actions"
              :selectedRowKeys="selectedRowKeys"
              :selectedItems="selectedItems"
              :dataView="dataView"
              :resource="resource"
              @exec-action="(action) => execAction(action, action.groupAction && !dataView)"/>
            <search-view
              v-if="!dataView"
              :searchFilters="searchFilters"
              :searchParams="searchParams"
              :apiName="apiName"
              @search="onSearch"
              @change-filter="changeFilter"/>
          </a-col>
        </a-row>
      </a-card>
    </a-affix>

    <div v-show="showAction">
      <keep-alive v-if="currentAction.component && (!currentAction.groupAction || selectedRowKeys.length === 0 || (this.selectedRowKeys.length > 0 && currentAction.api === 'destroyVirtualMachine'))">
        <a-modal
          :visible="showAction"
          :closable="true"
          :maskClosable="false"
          :cancelText="$t('label.cancel')"
          style="top: 20px;"
          @cancel="cancelAction"
          :confirmLoading="actionLoading"
          :footer="null"
          centered
          width="auto"
        >
          <template #title>
            <span v-if="currentAction.label">{{ $t(currentAction.label) }}</span>
            <a
              v-if="currentAction.docHelp || $route.meta.docHelp"
              style="margin-left: 5px"
              :href="$config.docBase + '/' + (currentAction.docHelp || $route.meta.docHelp)"
              target="_blank">
              <question-circle-outlined />
            </a>
          </template>
          <keep-alive>
            <component
              :is="currentAction.component"
              :resource="resource"
              :loading="loading"
              :action="{currentAction}"
              :selectedRowKeys="selectedRowKeys"
              :selectedItems="selectedItems"
              :chosenColumns="chosenColumns"
              v-bind="{currentAction}"
              @refresh-data="fetchData"
              @poll-action="pollActionCompletion"
              @close-action="closeAction"
              @cancel-bulk-action="handleCancel"/>
          </keep-alive>
        </a-modal>
      </keep-alive>
      <a-modal
        v-else
        :visible="showAction"
        :closable="true"
        :maskClosable="false"
        :footer="null"
        style="top: 20px;"
        :width="modalWidth"
        :ok-button-props="getOkProps()"
        :cancel-button-props="getCancelProps()"
        :confirmLoading="actionLoading"
        @cancel="cancelAction"
        centered
      >
        <template #title>
          <span v-if="currentAction.label">{{ $t(currentAction.label) }}</span>
          <a
            v-if="currentAction.docHelp || $route.meta.docHelp"
            style="margin-left: 5px"
            :href="$config.docBase + '/' + (currentAction.docHelp || $route.meta.docHelp)"
            target="_blank">
            <question-circle-outlined />
          </a>
        </template>
        <a-spin :spinning="actionLoading" v-ctrl-enter="handleSubmit">
          <span v-if="currentAction.message">
            <div v-if="selectedRowKeys.length > 0">
              <a-alert
                v-if="['delete-outlined', 'DeleteOutlined', 'poweroff-outlined', 'PoweroffOutlined'].includes(currentAction.icon)"
                type="error">
                <template #message>
                  <exclamation-circle-outlined style="color: red; fontSize: 30px; display: inline-flex" />
                  <span style="padding-left: 5px" v-html="`<b>${selectedRowKeys.length} ` + $t('label.items.selected') + `. </b>`" />
                  <span v-html="$t(currentAction.message)" />
                </template>
              </a-alert>
              <a-alert v-else type="warning">
                <template #message>
                  <span v-if="selectedRowKeys.length > 0" v-html="`<b>${selectedRowKeys.length} ` + $t('label.items.selected') + `. </b>`" />
                  <span v-html="$t(currentAction.message)" />
                </template>
              </a-alert>
            </div>
            <div v-else>
              <a-alert type="warning">
                <template #message>
                  <span v-html="$t(currentAction.message)" />
                </template>
              </a-alert>
            </div>
            <div v-if="selectedRowKeys.length > 0">
              <a-divider />
              <a-table
                v-if="selectedRowKeys.length > 0"
                size="middle"
                :columns="chosenColumns"
                :dataSource="selectedItems"
                :rowKey="(record, idx) => record.id || record.name || record.usageType || idx + '-' + Math.random()"
                :pagination="true"
                style="overflow-y: auto"
              >
              </a-table>
            </div>
            <br v-if="currentAction.paramFields.length > 0"/>
          </span>
          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleSubmit"
            layout="vertical">
            <div v-for="(field, fieldIndex) in currentAction.paramFields" :key="fieldIndex">
              <a-form-item
                :name="field.name"
                :ref="field.name"
                :v-bind="field.name"
                v-if="!(currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].value)"
              >
                <template #label>
                  <tooltip-label :title="$t('label.' + field.name)" :tooltip="field.description"/>
                </template>

                <a-switch
                  v-if="field.type==='boolean'"
                  v-model:checked="form[field.name]"
                  :placeholder="field.description"
                  v-focus="fieldIndex === firstIndex"
                />
                <a-select
                  v-else-if="currentAction.mapping && field.name in currentAction.mapping && currentAction.mapping[field.name].options"
                  :loading="field.loading"
                  v-model:value="form[field.name]"
                  :placeholder="field.description"
                  v-focus="fieldIndex === firstIndex"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                >
                  <a-select-option key="" label="">{{ }}</a-select-option>
                  <a-select-option
                    v-for="(opt, optIndex) in currentAction.mapping[field.name].options"
                    :key="optIndex"
                    :label="opt">
                    {{ opt }}
                  </a-select-option>
                </a-select>
                <a-select
                  v-else-if="field.name==='keypair' ||
                    (field.name==='account' && !['addAccountToProject', 'createAccount'].includes(currentAction.api))"
                  showSearch
                  optionFilterProp="label"
                  v-model:value="form[field.name]"
                  :loading="field.loading"
                  :placeholder="field.description"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  v-focus="fieldIndex === firstIndex"
                >
                  <a-select-option key="" label="">{{ }}</a-select-option>
                  <a-select-option
                    v-for="(opt, optIndex) in field.opts"
                    :key="optIndex"
                    :label="opt.name || opt.description || opt.traffictype || opt.publicip">
                    {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                  </a-select-option>
                </a-select>
                <a-select
                  v-else-if="field.type==='uuid'"
                  showSearch
                  optionFilterProp="label"
                  v-model:value="form[field.name]"
                  :loading="field.loading"
                  :placeholder="field.description"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  v-focus="fieldIndex === firstIndex"
                >
                  <a-select-option key="" label="">{{ }}</a-select-option>
                  <a-select-option v-for="opt in field.opts" :key="opt.id" :label="opt.name || opt.description || opt.traffictype || opt.publicip">
                    <div>
                      <span v-if="(field.name.startsWith('template') || field.name.startsWith('iso'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <os-logo v-else :osId="opt.ostypeid" :osName="opt.ostypename" size="lg" style="margin-left: -1px" />
                      </span>
                      <span v-if="(field.name.startsWith('zone'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <global-outlined v-else style="margin-right: 5px" />
                      </span>
                      <span v-if="(field.name.startsWith('project'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <project-outlined v-else style="margin-right: 5px" />
                      </span>
                      <span v-if="(field.name.startsWith('account') || field.name.startsWith('user'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <user-outlined v-else style="margin-right: 5px"/>
                      </span>
                      <span v-if="(field.name.startsWith('network'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <apartment-outlined v-else style="margin-right: 5px"/>
                      </span>
                      <span v-if="(field.name.startsWith('domain'))">
                        <span v-if="opt.icon">
                          <resource-icon :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                        </span>
                        <block-outlined v-else style="margin-right: 5px"/>
                      </span>
                      {{ opt.name || opt.description || opt.traffictype || opt.publicip }}
                    </div>
                  </a-select-option>
                </a-select>
                <a-select
                  v-else-if="field.type==='list'"
                  :loading="field.loading"
                  mode="multiple"
                  v-model:value="form[field.name]"
                  :placeholder="field.description"
                  v-focus="fieldIndex === firstIndex"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                >
                  <a-select-option
                    v-for="(opt, optIndex) in field.opts"
                    :key="optIndex"
                    :label="opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description">
                    {{ opt.name && opt.type ? opt.name + ' (' + opt.type + ')' : opt.name || opt.description }}
                  </a-select-option>
                </a-select>
                <a-input-number
                  v-else-if="field.type==='long'"
                  v-focus="fieldIndex === firstIndex"
                  style="width: 100%;"
                  v-model:value="form[field.name]"
                  :placeholder="field.description"
                />
                <a-input-password
                  v-else-if="field.name==='password' || field.name==='currentpassword' || field.name==='confirmpassword'"
                  v-model:value="form[field.name]"
                  :placeholder="field.description"
                  @blur="($event) => handleConfirmBlur($event, field.name)"
                  v-focus="fieldIndex === firstIndex"
                />
                <a-textarea
                  v-else-if="field.name==='certificate' || field.name==='privatekey' || field.name==='certchain'"
                  rows="2"
                  v-model:value="form[field.name]"
                  :placeholder="field.description"
                  v-focus="fieldIndex === firstIndex"
                />
                <a-input
                  v-else
                  v-focus="fieldIndex === firstIndex"
                  v-model:value="form[field.name]"
                  :placeholder="field.description" />
              </a-form-item>
            </div>

            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-spin>
        <br />
      </a-modal>
    </div>

    <div :style="this.$store.getters.shutdownTriggered ? 'margin-top: 25px;' : null">
      <div v-if="dataView" style="margin-top: -10px">
        <slot name="resource" v-if="$route.path.startsWith('/quotasummary') || $route.path.startsWith('/publicip')"></slot>
        <resource-view
          v-else
          :resource="resource"
          :loading="loading"
          :tabs="$route.meta.tabs" />
      </div>
      <div class="row-element" v-else>
        <list-view
          :loading="loading"
          :columns="columns"
          :items="items"
          :actions="actions"
          :columnKeys="columnKeys"
          :selectedColumns="selectedColumns"
          ref="listview"
          @update-selected-columns="updateSelectedColumns"
          @selection-change="onRowSelectionChange"
          @refresh="fetchData"
          @edit-tariff-action="(showAction, record) => $emit('edit-tariff-action', showAction, record)"/>
        <a-pagination
          class="row-element"
          style="margin-top: 10px"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="itemCount"
          :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="pageSizeOptions"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger
          showQuickJumper>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </div>
    </div>
    <bulk-action-progress
      :showGroupActionModal="showGroupActionModal"
      :selectedItems="selectedItems"
      :selectedColumns="bulkColumns"
      :message="modalInfo"
      @handle-cancel="handleCancel" />
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import { genericCompare } from '@/utils/sort.js'
import store from '@/store'
import eventBus from '@/config/eventBus'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ListView from '@/components/view/ListView'
import ResourceView from '@/components/view/ResourceView'
import ActionButton from '@/components/view/ActionButton'
import SearchView from '@/components/view/SearchView'
import OsLogo from '@/components/widgets/OsLogo'
import ResourceIcon from '@/components/view/ResourceIcon'
import BulkActionProgress from '@/components/view/BulkActionProgress'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'Resource',
  components: {
    Breadcrumb,
    ResourceView,
    ListView,
    ActionButton,
    SearchView,
    BulkActionProgress,
    TooltipLabel,
    OsLogo,
    ResourceIcon
  },
  mixins: [mixinDevice],
  provide: function () {
    return {
      parentFetchData: this.fetchData,
      parentToggleLoading: this.toggleLoading,
      parentStartLoading: this.startLoading,
      parentFinishLoading: this.finishLoading,
      parentSearch: this.onSearch,
      parentChangeFilter: this.changeFilter,
      parentChangeResource: this.changeResource,
      parentPollActionCompletion: this.pollActionCompletion
    }
  },
  data () {
    return {
      apiName: '',
      loading: false,
      actionLoading: false,
      columnKeys: [],
      allColumns: [],
      columns: [],
      bulkColumns: [],
      selectedColumns: [],
      chosenColumns: [],
      customColumnsDropdownVisible: false,
      showGroupActionModal: false,
      selectedItems: [],
      items: [],
      modalInfo: {},
      itemCount: 0,
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      resource: {},
      selectedRowKeys: [],
      currentAction: {},
      showAction: false,
      dataView: false,
      projectView: false,
      hasProjectId: false,
      selectedFilter: '',
      filters: [],
      searchFilters: [],
      searchParams: {},
      actions: [],
      confirmDirty: false,
      firstIndex: 0,
      modalWidth: '30vw',
      promises: []
    }
  },
  beforeUnmount () {
    eventBus.off('vm-refresh-data')
    eventBus.off('async-job-complete')
    eventBus.off('exec-action')
  },
  mounted () {
    eventBus.on('exec-action', (args) => {
      const { action, isGroupAction } = args
      this.execAction(action, isGroupAction)
    })
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({})
    eventBus.on('vm-refresh-data', () => {
      if (this.$route.path === '/vm' || this.$route.path.includes('/vm/')) {
        this.fetchData()
      }
    })
    eventBus.on('refresh-icon', () => {
      if (this.$showIcon()) {
        this.fetchData()
      }
    })
    eventBus.on('async-job-complete', (action) => {
      if (this.$route.path.includes('/vm/')) {
        if (action && 'api' in action && ['destroyVirtualMachine'].includes(action.api)) {
          return
        }
      }

      if ((this.$route.path.includes('/publicip/') && ['firewall', 'portforwarding', 'loadbalancing'].includes(this.$route.query.tab)) ||
        (this.$route.path.includes('/guestnetwork/') && (this.$route.query.tab === 'egress.rules' || this.$route.query.tab === 'public.ip.addresses'))) {
        return
      }

      if (this.$route.path.includes('/template/') || this.$route.path.includes('/iso/')) {
        return
      }
      this.fetchData()
    })
    eventBus.on('update-bulk-job-status', (args) => {
      var { items, action } = args
      for (const item of items) {
        this.$store.getters.headerNotices.map(function (j) {
          if (j.jobid === item.jobid) {
            j.bulkAction = action
          }
        })
      }
    })

    eventBus.on('update-resource-state', (args) => {
      var {
        selectedItems,
        resource,
        state,
        jobid
      } = args
      if (selectedItems.length === 0) {
        return
      }
      var tempResource = []
      this.selectedItems = selectedItems
      if (selectedItems && resource) {
        if (resource.includes(',')) {
          resource = resource.split(',')
          tempResource = resource
        } else {
          tempResource.push(resource)
        }
        for (var r = 0; r < tempResource.length; r++) {
          var objIndex = 0
          if (this.$route.path.includes('/template') || this.$route.path.includes('/iso')) {
            objIndex = selectedItems.findIndex(obj => (obj.zoneid === tempResource[r]))
          } else if (this.$route.path.includes('/router')) {
            objIndex = selectedItems.findIndex(obj => (obj.guestnetworkid === tempResource[r]))
          } else {
            objIndex = selectedItems.findIndex(obj => (obj.id === tempResource[r] || obj.username === tempResource[r] || obj.name === tempResource[r]))
          }
          if (state && objIndex !== -1) {
            this.selectedItems[objIndex].status = state
          }
          if (jobid && objIndex !== -1) {
            this.selectedItems[objIndex].jobid = jobid
          }
        }
      }
    })

    this.currentPath = this.$route.fullPath
    this.fetchData()
    if ('projectid' in this.$route.query) {
      this.switchProject(this.$route.query.projectid)
    }
    this.setModalWidthByScreen()
  },
  beforeRouteUpdate (to, from, next) {
    this.currentPath = this.$route.fullPath
    next()
  },
  beforeRouteLeave (to, from, next) {
    this.currentPath = this.$route.fullPath
    next()
  },
  watch: {
    '$route' (to, from) {
      if (to.fullPath !== from.fullPath && !to.fullPath.includes('action/')) {
        if ('page' in to.query) {
          this.page = Number(to.query.page)
          this.pageSize = Number(to.query.pagesize)
        } else {
          this.page = 1
        }
        this.itemCount = 0
        this.fetchData()
        if ('projectid' in to.query) {
          this.switchProject(to.query.projectid)
        }
      }
    },
    '$i18n.locale' (to, from) {
      if (to !== from) {
        this.fetchData()
      }
    },
    '$store.getters.metrics' (oldVal, newVal) {
      this.fetchData()
    },
    '$store.getters.listAllProjects' (oldVal, newVal) {
      this.fetchData()
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    },
    pageSizeOptions () {
      var sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    },
    filterValue () {
      if (this.$route.query.filter) {
        return this.$route.query.filter
      }
      const routeName = this.$route.name
      if ((this.projectView && routeName === 'vm') || (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype) && ['vm', 'iso', 'template', 'pod', 'cluster', 'host', 'systemvm', 'router', 'storagepool'].includes(routeName)) || ['account', 'guestnetwork', 'guestvlans'].includes(routeName)) {
        return 'all'
      }
      if (['publicip'].includes(routeName)) {
        return 'allocated'
      }
      if (['volume'].includes(routeName)) {
        return 'user'
      }
      if (['event'].includes(routeName)) {
        return 'active'
      }
      return 'self'
    }
  },
  methods: {
    getStyle () {
      if (['snapshot', 'vmsnapshot', 'publicip'].includes(this.$route.name)) {
        return 'table-cell'
      }
      return 'inline-flex'
    },
    getOkProps () {
      if (this.selectedRowKeys.length > 0 && this.currentAction?.groupAction) {
        return { props: { type: 'default' } }
      } else {
        return { props: { type: 'primary' } }
      }
    },
    getCancelProps () {
      if (this.selectedRowKeys.length > 0 && this.currentAction?.groupAction) {
        return { props: { type: 'primary' } }
      } else {
        return { props: { type: 'default' } }
      }
    },
    switchProject (projectId) {
      if (!projectId || !projectId.length || projectId.length !== 36) {
        return
      }
      api('listProjects', { id: projectId, listall: true, details: 'min' }).then(json => {
        if (!json || !json.listprojectsresponse || !json.listprojectsresponse.project) return
        const project = json.listprojectsresponse.project[0]
        this.$store.dispatch('SetProject', project)
        this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
        this.$message.success(`${this.$t('message.switch.to')} "${project.name}"`)
        const query = Object.assign({}, this.$route.query)
        delete query.projectid
        this.$router.replace({ query })
      })
    },
    fetchData (params = {}) {
      if (this.$route.name === 'deployVirtualMachine') {
        return
      }
      if (this.routeName !== this.$route.name) {
        this.routeName = this.$route.name
        this.items = []
      }
      if (!this.routeName) {
        this.routeName = this.$route.matched[this.$route.matched.length - 1].meta.name
      }
      this.apiName = ''
      this.actions = []
      this.columns = []
      this.columnKeys = []
      this.selectedColumns = []
      const refreshed = ('irefresh' in params)

      params.listall = true
      if (this.$route.meta.params) {
        const metaParams = this.$route.meta.params
        if (typeof metaParams === 'function') {
          Object.assign(params, metaParams())
        } else {
          Object.assign(params, metaParams)
        }
      }
      if (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype) &&
        'templatefilter' in params && this.routeName === 'template') {
        params.templatefilter = 'all'
      }
      if (['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype) &&
        'isofilter' in params && this.routeName === 'iso') {
        params.isofilter = 'all'
      }
      if (Object.keys(this.$route.query).length > 0) {
        if ('page' in this.$route.query) {
          this.page = Number(this.$route.query.page)
        }
        if ('pagesize' in this.$route.query) {
          this.pagesize = Number(this.$route.query.pagesize)
        }
        Object.assign(params, this.$route.query)
      }
      delete params.q
      delete params.filter
      delete params.irefresh

      this.searchFilters = this.$route && this.$route.meta && this.$route.meta.searchFilters
      this.filters = this.$route && this.$route.meta && this.$route.meta.filters
      if (typeof this.filters === 'function') {
        this.filters = this.filters()
      }

      this.projectView = Boolean(store.getters.project && store.getters.project.id)
      this.hasProjectId = ['vm', 'vmgroup', 'ssh', 'affinitygroup', 'volume', 'snapshot', 'vmsnapshot', 'guestnetwork', 'vpc', 'securitygroups', 'publicip', 'vpncustomergateway', 'template', 'iso', 'event', 'kubernetes', 'autoscalevmgroup'].includes(this.$route.name)

      if ((this.$route && this.$route.params && this.$route.params.id) || this.$route.query.dataView) {
        this.dataView = true
        if (!refreshed) {
          this.resource = {}
          this.$emit('change-resource', this.resource)
        }
      } else {
        this.dataView = false
      }

      if ('listview' in this.$refs && this.$refs.listview) {
        this.$refs.listview.resetSelection()
      }

      if (this.$route && this.$route.meta && this.$route.meta.permission) {
        this.apiName = this.$route.meta.permission[0]
        if (this.$route.meta.columns) {
          const columns = this.$route.meta.columns
          if (columns && typeof columns === 'function') {
            this.columnKeys = columns(this.$store.getters)
          } else {
            this.columnKeys = columns
          }
        }

        if (this.$route.meta.actions) {
          this.actions = this.$route.meta.actions
        }
      }

      if (this.apiName === '' || this.apiName === undefined) {
        return
      }

      if (!this.columnKeys || this.columnKeys.length === 0) {
        for (const field of store.getters.apis[this.apiName].response) {
          this.columnKeys.push(field.name)
        }
        this.columnKeys = [...new Set(this.columnKeys)]
        this.columnKeys.sort(function (a, b) {
          if (a === 'name' && b !== 'name') { return -1 }
          if (a < b) { return -1 }
          if (a > b) { return 1 }
          return 0
        })
      }

      for (var columnKey of this.columnKeys) {
        let key = columnKey
        let title = columnKey === 'cidr' && this.columnKeys.includes('ip6cidr') ? 'ipv4.cidr' : columnKey
        if (typeof columnKey === 'object') {
          if ('customTitle' in columnKey && 'field' in columnKey) {
            key = columnKey.field
            title = columnKey.customTitle
          } else {
            key = Object.keys(columnKey)[0]
            title = Object.keys(columnKey)[0]
          }
        }
        this.columns.push({
          key: key,
          title: this.$t('label.' + String(title).toLowerCase()),
          dataIndex: key,
          sorter: function (a, b) { return genericCompare(a[key] || '', b[key] || '') }
        })
        this.selectedColumns.push(key)
      }
      this.allColumns = this.columns

      if (!store.getters.metrics) {
        if (!this.$store.getters.customColumns[this.$store.getters.userInfo.id]) {
          this.$store.getters.customColumns[this.$store.getters.userInfo.id] = {}
          this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] = this.selectedColumns
        } else {
          this.selectedColumns = this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] || this.selectedColumns
          this.updateSelectedColumns()
        }
      }

      this.chosenColumns = this.columns.filter(column => {
        return ![this.$t('label.state'), this.$t('label.hostname'), this.$t('label.hostid'), this.$t('label.zonename'),
          this.$t('label.zone'), this.$t('label.zoneid'), this.$t('label.ip'), this.$t('label.ipaddress'), this.$t('label.privateip'),
          this.$t('label.linklocalip'), this.$t('label.size'), this.$t('label.sizegb'), this.$t('label.current'),
          this.$t('label.created'), this.$t('label.order')].includes(column.title)
      })
      this.chosenColumns.splice(this.chosenColumns.length - 1, 1)

      if (['listTemplates', 'listIsos'].includes(this.apiName) && this.dataView) {
        delete params.showunique
      }

      this.loading = true
      if (this.$route.params && this.$route.params.id) {
        params.id = this.$route.params.id
        if (['listSSHKeyPairs'].includes(this.apiName)) {
          if (!this.$isValidUuid(params.id)) {
            delete params.id
            params.name = this.$route.params.id
          }
        }
        if (['listPublicIpAddresses'].includes(this.apiName)) {
          params.allocatedonly = false
        }
        if (this.$route.path.startsWith('/vmsnapshot/')) {
          params.vmsnapshotid = this.$route.params.id
        } else if (this.$route.path.startsWith('/ldapsetting/')) {
          params.hostname = this.$route.params.id
        }
        if (this.$route.path.startsWith('/tungstenpolicy/')) {
          params.policyuuid = this.$route.params.id
        }
        if (this.$route.path.startsWith('/tungstenpolicyset/')) {
          params.applicationpolicysetuuid = this.$route.params.id
        }
        if (this.$route.path.startsWith('/tungstennetworkroutertable/')) {
          params.tungstennetworkroutetableuuid = this.$route.params.id
        }
        if (this.$route.path.startsWith('/tungsteninterfaceroutertable/')) {
          params.tungsteninterfaceroutetableuuid = this.$route.params.id
        }
        if (this.$route.path.startsWith('/tungstenfirewallpolicy/')) {
          params.firewallpolicyuuid = this.$route.params.id
        }
      }

      if (this.$store.getters.listAllProjects && !this.projectView) {
        params.projectid = '-1'
      }

      params.page = this.page
      params.pagesize = this.pageSize

      if (this.$showIcon()) {
        params.showIcon = true
      }

      if (['listAnnotations', 'listRoles', 'listZonesMetrics', 'listPods',
        'listClustersMetrics', 'listHostsMetrics', 'listStoragePoolsMetrics',
        'listImageStores', 'listSystemVms', 'listManagementServers',
        'listConfigurations', 'listHypervisorCapabilities',
        'listAlerts', 'listNetworkOfferings', 'listVPCOfferings'].includes(this.apiName)) {
        delete params.listall
      }

      api(this.apiName, params).then(json => {
        var responseName
        var objectName
        for (const key in json) {
          if (key.includes('response')) {
            responseName = key
            break
          }
        }
        this.itemCount = 0
        for (const key in json[responseName]) {
          if (key === 'count') {
            this.itemCount = json[responseName].count
            continue
          }
          objectName = key
          break
        }
        this.items = json[responseName][objectName]
        if (!this.items || this.items.length === 0) {
          this.items = []
        }

        if (['listTemplates', 'listIsos'].includes(this.apiName) && this.items.length > 1) {
          this.items = [...new Map(this.items.map(x => [x.id, x])).values()]
        }

        if (this.apiName === 'listProjects' && this.items.length > 0) {
          this.columns.map(col => {
            if (col.title === 'Account') {
              col.title = this.$t('label.project.owner')
            }
          })
        }

        if (this.apiName === 'listAnnotations') {
          this.columns.map(col => {
            if (col.title === 'label.entityid') {
              col.title = this.$t('label.annotation.entity')
            } else if (col.title === 'label.entitytype') {
              col.title = this.$t('label.annotation.entity.type')
            } else if (col.title === 'label.adminsonly') {
              col.title = this.$t('label.annotation.admins.only')
            }
          })
        }

        for (let idx = 0; idx < this.items.length; idx++) {
          this.items[idx].key = idx
          if (this.$route.path.startsWith('/ldapsetting')) {
            this.items[idx].id = this.items[idx].hostname
          }
        }
        if (this.items.length > 0) {
          if (!this.showAction || this.dataView) {
            this.resource = this.items[0]
            this.$emit('change-resource', this.resource)
          }
        } else {
          if (this.dataView) {
            this.$router.push({ path: '/exception/404' })
          }
        }
      }).catch(error => {
        if ([401].includes(error.response.status)) {
          return
        }

        if (Object.keys(this.searchParams).length > 0) {
          this.itemCount = 0
          this.items = []
          this.$message.error({
            content: error.response.headers['x-description'],
            duration: 5
          })
          return
        }

        this.$notifyError(error)

        if ([405].includes(error.response.status)) {
          this.$router.push({ path: '/exception/403' })
        }

        if ([430, 431, 432].includes(error.response.status)) {
          this.$router.push({ path: '/exception/404' })
        }

        if ([530, 531, 532, 533, 534, 535, 536, 537].includes(error.response.status)) {
          this.$router.push({ path: '/exception/500' })
        }
      }).finally(f => {
        this.loading = false
        this.searchParams = params
      })
    },
    closeAction () {
      this.actionLoading = false
      this.showAction = false
      this.currentAction = {}
    },
    cancelAction () {
      eventBus.emit('action-closing', { action: this.currentAction })
      this.closeAction()
    },
    onRowSelectionChange (selection) {
      this.selectedRowKeys = selection
      if (selection?.length > 0) {
        this.modalWidth = '50vw'
        this.selectedItems = (this.items.filter(function (item) {
          return selection.indexOf(item.id) !== -1
        }))
      } else {
        this.modalWidth = '30vw'
      }

      this.setModalWidthByScreen()
    },
    execAction (action, isGroupAction) {
      const self = this
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
      if (action.component && action.api && !action.popup) {
        const query = {}
        if (this.$route.path.startsWith('/vm')) {
          switch (true) {
            case ('templateid' in this.$route.query):
              query.templateid = this.$route.query.templateid
              break
            case ('isoid' in this.$route.query):
              query.isoid = this.$route.query.isoid
              break
            case ('networkid' in this.$route.query):
              query.networkid = this.$route.query.networkid
              break
            default:
              break
          }
        }
        this.$router.push({ name: action.api, query })
        return
      }
      this.currentAction = action
      this.currentAction.params = store.getters.apis[this.currentAction.api].params
      this.resource = action.resource
      this.$emit('change-resource', this.resource)
      var paramFields = this.currentAction.params
      paramFields.sort(function (a, b) {
        if (a.name === 'name' && b.name !== 'name') { return -1 }
        if (a.name !== 'name' && b.name === 'name') { return -1 }
        if (a.name === 'id') { return -1 }
        if (a.name < b.name) { return -1 }
        if (a.name > b.name) { return 1 }
        return 0
      })
      this.currentAction.paramFields = []
      if ('message' in action) {
        var message = action.message
        if (typeof action.message === 'function') {
          message = action.message(action.resource)
        }
        action.message = message
      }
      if ('args' in action) {
        var args = action.args
        if (typeof action.args === 'function') {
          args = action.args(action.resource, this.$store.getters, isGroupAction)
        }
        if (args.length > 0) {
          this.currentAction.paramFields = args.map(function (arg) {
            if (arg === 'confirmpassword') {
              return {
                type: 'password',
                name: 'confirmpassword',
                required: true,
                description: self.$t('label.confirmpassword.description')
              }
            }
            return paramFields.filter(function (param) {
              return param.name.toLowerCase() === arg.toLowerCase()
            })[0]
          })
        }
      }
      this.getFirstIndexFocus()

      this.showAction = true
      const listIconForFillValues = ['copy-outlined', 'CopyOutlined', 'edit-outlined', 'EditOutlined', 'share-alt-outlined', 'ShareAltOutlined']
      for (const param of this.currentAction.paramFields) {
        if (param.type === 'list' && ['tags', 'hosttags', 'storagetags', 'files'].includes(param.name)) {
          param.type = 'string'
        }
        this.setRules(param)
        if (param.type === 'uuid' || param.type === 'list' || param.name === 'account' || (this.currentAction.mapping && param.name in this.currentAction.mapping)) {
          this.listUuidOpts(param)
        }
      }
      this.actionLoading = false
      if (action.dataView && listIconForFillValues.includes(action.icon)) {
        this.fillEditFormFieldValues()
      }
    },
    getFirstIndexFocus () {
      this.firstIndex = 0
      for (let fieldIndex = 0; fieldIndex < this.currentAction.paramFields.length; fieldIndex++) {
        const field = this.currentAction.paramFields[fieldIndex]
        if (!(this.currentAction.mapping && field.name in this.currentAction.mapping && this.currentAction.mapping[field.name].value)) {
          this.firstIndex = fieldIndex
          break
        }
      }
    },
    listUuidOpts (param) {
      if (this.currentAction.mapping && param.name in this.currentAction.mapping && !this.currentAction.mapping[param.name].api) {
        return
      }
      var paramName = param.name
      var extractedParamName = paramName.replace('ids', '').replace('id', '').toLowerCase()
      var params = { listall: true }
      const possibleName = 'list' + extractedParamName + 's'
      var showIcon = false
      if (this.$showIcon(extractedParamName)) {
        showIcon = true
      }
      var possibleApi
      if (this.currentAction.mapping && param.name in this.currentAction.mapping && this.currentAction.mapping[param.name].api) {
        possibleApi = this.currentAction.mapping[param.name].api
        if (this.currentAction.mapping[param.name].params) {
          const customParams = this.currentAction.mapping[param.name].params(this.resource)
          if (customParams) {
            params = { ...params, ...customParams }
          }
        }
      } else if (paramName === 'id') {
        possibleApi = this.apiName
      } else {
        for (const api in store.getters.apis) {
          if (api.toLowerCase().startsWith(possibleName)) {
            possibleApi = api
            break
          }
        }
      }
      if (!possibleApi) {
        return
      }
      param.loading = true
      param.opts = []
      if (possibleApi === 'listTemplates') {
        params.templatefilter = 'executable'
      } else if (possibleApi === 'listIsos') {
        params.isofilter = 'executable'
      } else if (possibleApi === 'listHosts') {
        params.type = 'routing'
      } else if (possibleApi === 'listNetworkOfferings' && this.resource) {
        if (this.resource.type) {
          params.guestiptype = this.resource.type
        }
        if (!this.resource.vpcid) {
          params.forvpc = false
        }
      }
      if (showIcon) {
        params.showicon = true
      }
      api(possibleApi, params).then(json => {
        param.loading = false
        for (const obj in json) {
          if (obj.includes('response')) {
            if (possibleApi === 'listBackupOfferings' && json[obj].backupoffering) {
              json[obj].backupoffering.sort((a, b) => {
                return a.name > b.name
              })
            }
            for (const res in json[obj]) {
              if (res === 'count') {
                continue
              }
              param.opts = json[obj][res]
              if (this.currentAction.mapping && this.currentAction.mapping[param.name] && this.currentAction.mapping[param.name].filter) {
                const filter = this.currentAction.mapping[param.name].filter
                param.opts = json[obj][res].filter(filter)
              }
              if (['listTemplates', 'listIsos'].includes(possibleApi)) {
                param.opts = [...new Map(param.opts.map(x => [x.id, x])).values()]
              }
              break
            }
            break
          }
        }
      }).catch(function (error) {
        console.log(error)
        param.loading = false
      })
    },
    pollActionCompletion (jobId, action, resourceName, resource, showLoading = true) {
      if (this.shouldNavigateBack(action)) {
        action.isFetchData = false
      }
      return new Promise((resolve) => {
        this.$pollJob({
          jobId,
          title: this.$t(action.label),
          description: resourceName,
          name: resourceName,
          successMethod: result => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource, state: 'success' })
            }
            if (action.response) {
              const description = action.response(result.jobresult)
              if (description) {
                this.$notification.info({
                  message: this.$t(action.label),
                  description: (<span v-html={description}></span>),
                  duration: 0
                })
              }
            }
            if ('successMethod' in action) {
              action.successMethod(this, result)
            }
            resolve(true)
          },
          errorMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource, state: 'failed' })
            }
            resolve(true)
          },
          loadingMessage: `${this.$t(action.label)} - ${resourceName}`,
          showLoading: showLoading,
          catchMessage: this.$t('error.fetching.async.job.result'),
          action,
          bulkAction: `${this.selectedItems.length > 0}` && this.showGroupActionModal,
          resourceId: resource
        })
      })
    },
    fillEditFormFieldValues () {
      this.currentAction.paramFields.map(field => {
        let fieldValue = null
        let fieldName = null
        if (field.type === 'list' || field.name === 'account') {
          fieldName = field.name.replace('ids', 'name').replace('id', 'name')
        } else {
          fieldName = field.name
        }
        fieldValue = this.resource[fieldName] ? this.resource[fieldName] : null
        if (fieldValue) {
          this.form[field.name] = fieldValue
        }
      })
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.bulkColumns = []
      this.selectedRowKeys = []
      this.message = {}
    },
    handleSubmit (e) {
      if (this.actionLoading) return
      this.promises = []
      if (!this.dataView && this.currentAction.groupAction && this.selectedRowKeys.length > 0) {
        if (this.selectedRowKeys.length > 0) {
          this.bulkColumns = this.chosenColumns
          this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
          this.bulkColumns.splice(0, 0, {
            key: 'status',
            dataIndex: 'status',
            title: this.$t('label.operation.status'),
            filters: [
              { text: 'In Progress', value: 'InProgress' },
              { text: 'Success', value: 'success' },
              { text: 'Failed', value: 'failed' }
            ]
          })
          this.showGroupActionModal = true
          this.modalInfo.title = this.currentAction.label
          this.modalInfo.docHelp = this.currentAction.docHelp
        }
        this.formRef.value.validate().then(() => {
          const values = toRaw(this.form)
          this.actionLoading = true
          const itemsNameMap = {}
          this.items.map(x => {
            itemsNameMap[x.id] = x.name || x.displaytext || x.id
          })
          const paramsList = this.currentAction.groupMap(this.selectedRowKeys, values, this.items)
          for (const params of paramsList) {
            var resourceName = itemsNameMap[params.id]
            // Using a method for this since it's an async call and don't want wrong prarms to be passed
            this.promises.push(this.callGroupApi(params, resourceName))
          }
          this.$message.info({
            content: this.$t(this.currentAction.label),
            key: this.currentAction.label,
            duration: 3
          })
          Promise.all(this.promises).finally(() => {
            this.actionLoading = false
            this.fetchData()
          })
        }).catch(error => {
          this.formRef.value.scrollToField(error.errorFields[0].name)
        })
      } else {
        this.execSubmit(e)
      }
    },
    callGroupApi (params, resourceName) {
      return new Promise((resolve, reject) => {
        const action = this.currentAction
        api(action.api, params).then(json => {
          resolve(this.handleResponse(json, resourceName, this.getDataIdentifier(params), action, false))
          this.closeAction()
        }).catch(error => {
          if ([401].includes(error.response.status)) {
            return
          }
          if (this.selectedItems.length !== 0) {
            this.$notifyError(error)
            eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: this.getDataIdentifier(params), state: 'failed' })
          }
        })
      })
    },
    getDataIdentifier (params) {
      var dataIdentifier = ''
      dataIdentifier = params.id || params.username || params.name || params.vmsnapshotid || params.ids
      return dataIdentifier
    },
    handleResponse (response, resourceName, resource, action, showLoading = true) {
      return new Promise(resolve => {
        let jobId = null
        for (const obj in response) {
          if (obj.includes('response')) {
            if (response[obj].jobid) {
              jobId = response[obj].jobid
            } else {
              if (this.selectedItems.length > 0) {
                eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource, state: 'success' })
                if (resource) {
                  this.selectedItems.filter(item => item === resource)
                }
              }
              var message = action.successMessage ? this.$t(action.successMessage) : this.$t(action.label) +
                (resourceName ? ' - ' + resourceName : '')
              var duration = 2
              if (action.additionalMessage) {
                message = message + ' - ' + this.$t(action.successMessage)
                duration = 5
              }
              if (this.selectedItems.length === 0) {
                this.$message.success({
                  content: message,
                  key: action.label + resourceName,
                  duration: duration
                })
              }
              break
            }
          }
        }
        if (['addLdapConfiguration', 'deleteLdapConfiguration'].includes(action.api)) {
          this.$store.dispatch('UpdateConfiguration')
        }
        if (jobId) {
          eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource, state: 'InProgress', jobid: jobId })
          resolve(this.pollActionCompletion(jobId, action, resourceName, resource, showLoading))
        }
        resolve(false)
      })
    },
    execSubmit (e) {
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        const action = this.currentAction
        if ('id' in this.resource && action.params.map(i => { return i.name }).includes('id')) {
          params.id = this.resource.id
        }

        if (['updateDiskOffering'].includes(action.api) && values.tags === this.resource.tags) {
          delete values.tags
        }

        if (['updateServiceOffering'].includes(action.api)) {
          if (values.hosttags === this.resource.hosttags) {
            delete values.hosttags
          }
          if (values.storagetags === this.resource.storagetags) {
            delete values.tags
          }
        }

        for (const key in values) {
          const input = values[key]
          for (const param of action.params) {
            if (param.name !== key) {
              continue
            }
            if (input === undefined || input === null ||
              (input === '' && !['updateStoragePool', 'updateHost', 'updatePhysicalNetwork', 'updateDiskOffering', 'updateNetworkOffering', 'updateServiceOffering', 'updateZone', 'updateAccount'].includes(action.api))) {
              if (param.type === 'boolean') {
                params[key] = false
              }
              break
            }
            if (input === '' && !['tags', 'hosttags', 'storagetags', 'dns2', 'ip6dns1', 'ip6dns2', 'internaldns2', 'networkdomain'].includes(key)) {
              break
            }
            if (action.mapping && key in action.mapping && action.mapping[key].options) {
              params[key] = action.mapping[key].options[input]
              if (['createAffinityGroup'].includes(action.api) && key === 'type') {
                if (params[key] === 'host anti-affinity (Strict)') {
                  params[key] = 'host anti-affinity'
                } else if (params[key] === 'host affinity (Strict)') {
                  params[key] = 'host affinity'
                } else if (params[key] === 'host anti-affinity (Non-Strict)') {
                  params[key] = 'non-strict host anti-affinity'
                } else if (params[key] === 'host affinity (Non-Strict)') {
                  params[key] = 'non-strict host affinity'
                }
              }
            } else if (param.type === 'list') {
              params[key] = input.map(e => { return param.opts[e].id }).reduce((str, name) => { return str + ',' + name })
            } else if (param.name === 'account' || param.name === 'keypair') {
              if (['addAccountToProject', 'createAccount'].includes(action.api)) {
                params[key] = input
              } else {
                params[key] = param.opts[input].name
              }
            } else {
              params[key] = input
            }
            break
          }
        }

        for (const key in action.defaultArgs) {
          if (!params[key]) {
            params[key] = action.defaultArgs[key]
          }
        }

        if (!this.projectView || !['uploadSslCert'].includes(action.api)) {
          if (action.mapping) {
            for (const key in action.mapping) {
              if (!action.mapping[key].value) {
                continue
              }
              params[key] = action.mapping[key].value(this.resource, params, this.$route.query)
            }
          }
        }

        const resourceName = params.displayname || params.displaytext || params.name || params.hostname || params.username ||
          params.ipaddress || params.virtualmachinename || this.resource.name || this.resource.ipaddress || this.resource.id

        var hasJobId = false
        this.actionLoading = true
        let args = null
        if (action.post) {
          args = [action.api, {}, 'POST', params]
        } else {
          args = [action.api, params]
        }
        api(...args).then(json => {
          var response = this.handleResponse(json, resourceName, this.getDataIdentifier(params), action)
          if (!response) {
            this.fetchData()
            this.closeAction()
            return
          }
          response.then(jobId => {
            hasJobId = jobId
            if (this.shouldNavigateBack(action)) {
              this.$router.go(-1)
            } else {
              if (!hasJobId) {
                this.fetchData()
              }
            }
          })
          this.closeAction()
        }).catch(error => {
          if ([401].includes(error.response.status)) {
            return
          }

          console.log(error)
          eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: this.getDataIdentifier(params), state: 'failed' })
          this.$notifyError(error)
        }).finally(f => {
          this.actionLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    shouldNavigateBack (action) {
      return ((['delete-outlined', 'DeleteOutlined'].includes(action.icon) || ['archiveEvents', 'archiveAlerts', 'unmanageVirtualMachine'].includes(action.api)) && this.dataView)
    },
    getColumnKey (name) {
      if (typeof name === 'object') {
        name = Object.keys(name).includes('customTitle') ? name.customTitle : name.field
      }
      return name
    },
    updateSelectedColumns (name) {
      if (name) {
        name = this.getColumnKey(name)
        if (this.selectedColumns.includes(name)) {
          this.selectedColumns = this.selectedColumns.filter(x => x !== name)
        } else {
          this.selectedColumns.push(name)
        }
      }

      this.columns = this.allColumns.filter(x => this.selectedColumns.includes(x.dataIndex))
      const filterColumn = {
        key: 'filtercolumn',
        dataIndex: 'filtercolumn',
        title: '',
        customFilterDropdown: true,
        width: 5
      }
      if (this.columns.length === 0) {
        filterColumn.width = 'auto'
      }
      this.columns.push(filterColumn)
      if (!this.$store.getters.customColumns[this.$store.getters.userInfo.id]) {
        this.$store.getters.customColumns[this.$store.getters.userInfo.id] = {}
      }
      this.$store.getters.customColumns[this.$store.getters.userInfo.id][this.$route.path] = this.selectedColumns
      this.$store.dispatch('SetCustomColumns', this.$store.getters.customColumns)
    },
    changeFilter (filter) {
      const query = Object.assign({}, this.$route.query)
      delete query.templatefilter
      delete query.isofilter
      delete query.account
      delete query.domainid
      delete query.state
      delete query.annotationfilter
      if (this.$route.name === 'template') {
        query.templatefilter = filter
      } else if (this.$route.name === 'iso') {
        query.isofilter = filter
      } else if (this.$route.name === 'volume') {
        if (filter === 'all') {
          query.listsystemvms = true
        } else {
          delete query.listsystemvms
        }
      } else if (this.$route.name === 'guestnetwork') {
        if (filter === 'all') {
          delete query.networkfilter
        } else {
          query.networkfilter = filter
        }
      } else if (['account', 'publicip', 'systemvm', 'router'].includes(this.$route.name)) {
        if (filter !== 'all') {
          query.state = filter
        }
      } else if (this.$route.name === 'storagepool') {
        if (filter === 'all') {
          delete query.status
        } else {
          query.status = filter
        }
      } else if (['pod', 'cluster'].includes(this.$route.name)) {
        if (filter === 'all') {
          delete query.allocationstate
        } else {
          query.allocationstate = filter
        }
      } else if (['host'].includes(this.$route.name)) {
        if (filter === 'all') {
          delete query.resourcestate
          delete query.state
        } else if (['up', 'down', 'alert'].includes(filter)) {
          delete query.resourcestate
          query.state = filter
        } else {
          delete query.state
          query.resourcestate = filter
        }
      } else if (this.$route.name === 'vm') {
        if (filter === 'self') {
          query.account = this.$store.getters.userInfo.account
          query.domainid = this.$store.getters.userInfo.domainid
        } else if (['running', 'stopped'].includes(filter)) {
          query.state = filter
        }
      } else if (this.$route.name === 'comment') {
        query.annotationfilter = filter
      } else if (this.$route.name === 'guestvlans') {
        if (filter === 'all') {
          query.allocatedonly = 'false'
        } else if (filter === 'allocatedonly') {
          query.allocatedonly = 'true'
        }
      } else if (this.$route.name === 'event') {
        if (filter === 'archived') {
          query.archived = true
        } else {
          delete query.archived
        }
      }
      query.filter = filter
      query.page = '1'
      query.pagesize = this.pageSize.toString()
      this.$router.push({ query })
    },
    onSearch (opts) {
      const query = Object.assign({}, this.$route.query)
      for (const key in this.searchParams) {
        delete query[key]
      }
      delete query.name
      delete query.templatetype
      delete query.keyword
      delete query.q
      this.searchParams = {}
      if (opts && Object.keys(opts).length > 0) {
        this.searchParams = opts
        if ('searchQuery' in opts) {
          const value = opts.searchQuery
          if (value && value.length > 0) {
            if (this.$route.name === 'quotaemailtemplate') {
              query.templatetype = value
            } else if (this.$route.name === 'globalsetting') {
              query.name = value
            } else {
              query.keyword = value
            }
            query.q = value
          }
          this.searchParams = {}
        } else {
          Object.assign(query, opts)
        }
      }
      query.page = '1'
      query.pagesize = String(this.pageSize)
      if (JSON.stringify(query) === JSON.stringify(this.$route.query)) {
        this.fetchData(query)
        return
      }
      this.$router.push({ query })
    },
    changePage (page, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = page
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    changePageSize (currentPage, pageSize) {
      const query = Object.assign({}, this.$route.query)
      query.page = currentPage
      query.pagesize = pageSize
      this.$router.push({ query })
    },
    changeResource (resource) {
      this.resource = resource
    },
    start () {
      this.loading = true
      this.fetchData()
      setTimeout(() => {
        this.loading = false
        this.selectedRowKeys = []
      }, 1000)
    },
    toggleLoading () {
      this.loading = !this.loading
    },
    startLoading () {
      this.loading = true
    },
    finishLoading () {
      this.loading = false
    },
    handleConfirmBlur (e, name) {
      if (name !== 'confirmpassword') {
        return
      }
      const value = e.target.value
      this.confirmDirty = this.confirmDirty || !!value
    },
    async validateTwoPassword (rule, value) {
      if (!value || value.length === 0) {
        return Promise.resolve()
      } else if (rule.field === 'confirmpassword') {
        const messageConfirm = this.$t('message.validate.equalto')
        const passwordVal = this.form.password
        if (passwordVal && passwordVal !== value) {
          return Promise.reject(messageConfirm)
        } else {
          return Promise.resolve()
        }
      } else if (rule.field === 'password') {
        const confirmPasswordVal = this.form.confirmpassword
        if (!confirmPasswordVal || confirmPasswordVal.length === 0) {
          return Promise.resolve()
        } else if (value && this.confirmDirty) {
          this.formRef.value.validateFields('confirmpassword')
          return Promise.resolve()
        } else {
          return Promise.resolve()
        }
      } else {
        return Promise.resolve()
      }
    },
    setRules (field) {
      let rule = {}

      if (!field || Object.keys(field).length === 0) {
        return
      }

      if (!this.rules[field.name]) {
        this.rules[field.name] = []
      }

      switch (true) {
        case (field.type === 'boolean'):
          rule.required = field.required
          rule.message = this.$t('message.error.required.input')
          this.rules[field.name].push(rule)
          break
        case (this.currentAction.mapping && field.name in this.currentAction.mapping && 'options' in this.currentAction.mapping[field.name]):
          console.log('op: ' + field)
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          this.rules[field.name].push(rule)
          break
        case (field.name === 'keypair' || (field.name === 'account' && !['addAccountToProject', 'createAccount'].includes(this.currentAction.api))):
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          this.rules[field.name].push(rule)
          break
        case (field.type === 'uuid'):
          console.log('uuid: ' + field)
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          this.rules[field.name].push(rule)
          break
        case (field.type === 'list'):
          console.log('list: ' + field)
          rule.type = 'array'
          rule.required = field.required
          rule.message = this.$t('message.error.select')
          this.rules[field.name].push(rule)
          break
        case (field.type === 'long'):
          console.log(field)
          rule.type = 'number'
          rule.required = field.required
          rule.message = this.$t('message.validate.number')
          this.rules[field.name].push(rule)
          break
        case (field.name === 'password' || field.name === 'currentpassword' || field.name === 'confirmpassword'):
          rule.required = field.required
          rule.message = this.$t('message.error.required.input')
          this.rules[field.name].push(rule)

          rule = {}
          rule.validator = this.validateTwoPassword
          this.rules[field.name].push(rule)
          break
        case (field.name === 'certificate' || field.name === 'privatekey' || field.name === 'certchain'):
          rule.required = field.required
          rule.message = this.$t('message.error.required.input')
          this.rules[field.name].push(rule)
          break
        default:
          rule.required = field.required
          rule.message = this.$t('message.error.required.input')
          this.rules[field.name].push(rule)
          break
      }

      rule = {}
    },
    setModalWidthByScreen () {
      const screenWidth = window.innerWidth
      if (screenWidth <= 768) {
        this.modalWidth = '450px'
      }
    }
  }
}
</script>

<style scoped>
.breadcrumb-card {
  margin-left: -24px;
  margin-right: -24px;
  margin-top: -16px;
  margin-bottom: 12px;
}

.row-element {
  margin-bottom: 10px;
}

.ant-breadcrumb {
  vertical-align: text-bottom;
}

:deep(.ant-alert-message) {
  display: flex;
  align-items: center;
}

.hide {
  display: none !important;
}
</style>
