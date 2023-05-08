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
    <div>
      <div class="form" v-ctrl-enter="openAddVMModal">
        <div class="form__item">
          <div class="form__label">{{ $t('label.privateport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              v-focus="true"
              v-model:value="newRule.privateport"
              :placeholder="$t('label.start')"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              class="tag-disabled-input"
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none; text-align:
              center; margin-right: 0;"></a-input>
            <a-input
              v-model:value="newRule.privateendport"
              :placeholder="$t('label.end')"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.publicport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              v-model:value="newRule.publicport"
              :placeholder="$t('label.start')"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              class="tag-disabled-input"
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none;
              text-align: center; margin-right: 0;"></a-input>
            <a-input
              v-model:value="newRule.publicendport"
              :placeholder="$t('label.end')"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.protocol') }}</div>
          <a-select
            v-model:value="newRule.protocol"
            style="width: 100%;"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="tcp" label="$t('label.tcp')">{{ $t('label.tcp') }}</a-select-option>
            <a-select-option value="udp" :label="$t('label.udp')">{{ $t('label.udp') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item" style="margin-left: auto;">
          <div class="form__label">{{ $t('label.add.vm') }}</div>
          <a-button :disabled="!('createPortForwardingRule' in $store.getters.apis)" type="primary" @click="openAddVMModal">{{ $t('label.add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>
    <a-button
      v-if="(('deletePortForwardingRule' in $store.getters.apis) && this.selectedItems.length > 0)"
      type="primary"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="bulkActionConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t('label.action.bulk.delete.portforward.rules') }}
    </a-button>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="portForwardRules"
      :pagination="false"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'privateport'">
          {{ record.privateport }} - {{ record.privateendport }}
        </template>
        <template v-if="column.key === 'publicport'">
          {{ record.publicport }} - {{ record.publicendport }}
        </template>
        <template v-if="column.key === 'protocol'">
          {{ getCapitalise(record.protocol) }}
        </template>
        <template v-if="column.key === 'vm'">
          <div><desktop-outlined/>
            <router-link
              :to="{ path: '/vm/' + record.virtualmachineid }">
              {{ record.virtualmachinename }}</router-link> ({{ record.vmguestip }})</div>
        </template>
        <template v-if="column.key === 'actions'">
          <div class="actions">
            <tooltip-button :tooltip="$t('label.tags')" icon="tag-outlined" buttonClass="rule-action" @onClick="() => openTagsModal(record.id)" />
            <tooltip-button
              :tooltip="$t('label.remove.rule')"
              type="primary"
              :danger="true"
              icon="delete-outlined"
              buttonClass="rule-action"
              :disabled="!('deletePortForwardingRule' in $store.getters.apis)"
              @onClick="deleteRule(record)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :title="$t('label.edit.tags')"
      :visible="tagsModalVisible"
      :footer="null"
      :closable="true"
      :maskClosable="false"
      :afterClose="closeModal"
      @cancel="tagsModalVisible = false">
      <span v-show="tagsModalLoading" class="tags-modal-loading">
        <loading-outlined />
      </span>

      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        class="add-tags"
        @finish="handleAddTag"
        v-ctrl-enter="handleAddTag"
       >
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.key') }}</p>
          <a-form-item name="key" ref="key">
            <a-input
              v-focus="true"
              v-model:value="form.key" />
          </a-form-item>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.value') }}</p>
          <a-form-item name="value" ref="value">
            <a-input v-model:value="form.value" />
          </a-form-item>
        </div>

        <a-button style="margin-bottom: 5px;" type="primary" ref="submit" @click="handleAddTag">{{ $t('label.add') }}</a-button>
      </a-form>

      <a-divider />

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="true" @close="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.done') }}</a-button>
    </a-modal>

    <a-modal
      :title="$t('label.add.vm')"
      :maskClosable="false"
      :closable="true"
      :visible="addVmModalVisible"
      class="vm-modal"
      width="60vw"
      :footer="null"
      @cancel="closeModal">
      <div v-ctrl-enter="addRule">
        <span
          v-if="'vpcid' in resource && !('associatednetworkid' in resource)">
          <strong>{{ $t('label.select.tier') }} </strong>
          <a-select
            :v-focus="'vpcid' in resource && !('associatednetworkid' in resource)"
            v-model:value="selectedTier"
            @change="fetchVirtualMachines()"
            :placeholder="$t('label.select.tier')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="tier in tiers.data"
              :loading="tiers.loading"
              :key="tier.id"
              :label="tier.displaytext || ''">
              {{ tier.displaytext }}
            </a-select-option>
          </a-select>
        </span>
        <a-input-search
          v-focus="!('vpcid' in resource && !('associatednetworkid' in resource))"
          class="input-search"
          :placeholder="$t('label.search')"
          v-model:value="searchQuery"
          allowClear
          @search="onSearch" />
        <a-table
          size="small"
          class="list-view"
          :loading="addVmModalLoading"
          :columns="vmColumns"
          :dataSource="vms"
          :pagination="false"
          :rowKey="record => record.id"
          :scroll="{ y: 300 }">
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'name'">
              <span>
                {{ text }}
              </span>
              <loading-outlined v-if="addVmModalNicLoading"></loading-outlined>
              <a-select
                style="display: block"
                v-else-if="!addVmModalNicLoading && newRule.virtualmachineid === record.id"
                v-model:value="newRule.vmguestip"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="(nic, nicIndex) in nics"
                  :key="nic"
                  :value="nic"
                  :label="nic">
                  {{ nic }}{{ nicIndex === 0 ? ` (${$t('label.primary')})` : null }}
                </a-select-option>
              </a-select>
            </template>

            <template v-if="column.key === 'state'">
              <status :text="text ? text : ''" displayText></status>
            </template>

            <template v-if="column.key === 'actions'">
              <div style="text-align: center">
                <a-radio-group
                  class="radio-group"
                  :key="record.id"
                  v-model:value="checked"
                  @change="($event) => checked = $event.target.value">
                  <a-radio :value="record.id" @change="e => fetchNics(e)" />
                </a-radio-group>
              </div>
            </template>
          </template>
        </a-table>
        <a-pagination
          class="pagination"
          size="small"
          :current="vmPage"
          :pageSize="vmPageSize"
          :total="vmCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="handleChangeVmPage"
          @showSizeChange="handleChangeVmPageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" :disabled="newRule.virtualmachineid === null" @click="addRule">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>

    <bulk-action-view
      v-if="showConfirmationAction || showGroupActionModal"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="portForwardRules"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      :filterColumns="filterColumns"
      action="deletePortForwardingRule"
      :loading="loading"
      :message="message"
      @group-action="deleteRules"
      @handle-cancel="handleCancel"
      @close-modal="closeModal" />
  </div>
</template>

<script>
import { reactive, ref, toRaw } from 'vue'
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import BulkActionView from '@/components/view/BulkActionView'
import eventBus from '@/config/eventBus'

export default {
  components: {
    Status,
    TooltipButton,
    BulkActionView
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      checked: false,
      selectedRowKeys: [],
      showGroupActionModal: false,
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['State', 'Actions'],
      showConfirmationAction: false,
      message: {
        title: this.$t('label.action.bulk.delete.portforward.rules'),
        confirmMessage: this.$t('label.confirm.delete.portforward.rules')
      },
      loading: true,
      portForwardRules: [],
      newRule: {
        protocol: 'tcp',
        privateport: null,
        privateendport: null,
        publicport: null,
        publicendport: null,
        openfirewall: false,
        vmguestip: null,
        virtualmachineid: null
      },
      tagsModalVisible: false,
      selectedRule: null,
      selectedTier: null,
      tags: [],
      tagsModalLoading: false,
      addVmModalVisible: false,
      addVmModalLoading: false,
      addVmModalNicLoading: false,
      vms: [],
      nics: [],
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          key: 'privateport',
          title: this.$t('label.privateport')
        },
        {
          key: 'publicport',
          title: this.$t('label.publicport')
        },
        {
          key: 'protocol',
          title: this.$t('label.protocol')
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          key: 'vm',
          title: this.$t('label.vm')
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ],
      tiers: {
        loading: false,
        data: []
      },
      vmColumns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name',
          width: 210
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.displayname'),
          dataIndex: 'displayname'
        },
        {
          title: this.$t('label.ip'),
          dataIndex: 'ip',
          width: 100
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.zone'),
          dataIndex: 'zonename'
        },
        {
          key: 'actions',
          title: this.$t('label.select'),
          dataIndex: 'actions',
          width: 80
        }
      ],
      vmPage: 1,
      vmPageSize: 10,
      vmCount: 0,
      searchQuery: null
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  created () {
    console.log(this.resource)
    this.initForm()
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
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        key: [{ required: true, message: this.$t('message.specify.tag.key') }],
        value: [{ required: true, message: this.$t('message.specify.tag.value') }]
      })
    },
    fetchData () {
      this.fetchListTiers()
      this.fetchPFRules()
    },
    fetchListTiers () {
      if ('vpcid' in this.resource && 'associatednetworkid' in this.resource) {
        return
      }
      this.selectedTier = null
      this.tiers.loading = true
      api('listNetworks', {
        account: this.resource.account,
        domainid: this.resource.domainid,
        supportedservices: 'PortForwarding',
        vpcid: this.resource.vpcid
      }).then(json => {
        this.tiers.data = json.listnetworksresponse.network || []
        if (this.tiers.data && this.tiers.data.length > 0) {
          this.selectedTier = this.tiers.data[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.tiers.loading = false })
    },
    fetchPFRules () {
      this.loading = true
      api('listPortForwardingRules', {
        listAll: true,
        ipaddressid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.portForwardRules = response.listportforwardingrulesresponse.portforwardingrule || []
        this.totalCount = response.listportforwardingrulesresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.portForwardRules.filter(function (item) {
        return selection.indexOf(item.id) !== -1
      }))
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.parentFetchData()
    },
    deleteRules (e) {
      this.showConfirmationAction = false
      this.selectedColumns.splice(0, 0, {
        key: 'status',
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      for (const rule of this.selectedItems) {
        this.deleteRule(rule)
      }
    },
    getCapitalise (val) {
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
    },
    deleteRule (rule) {
      this.loading = true
      api('deletePortForwardingRule', { id: rule.id }).then(response => {
        const jobId = response.deleteportforwardingruleresponse.jobid
        eventBus.emit('update-job-details', { jobId, resourceId: null })
        this.$pollJob({
          title: this.$t('label.portforwarding.rule'),
          description: rule.id,
          jobId: jobId,
          successMessage: this.$t('message.success.remove.port.forward'),
          successMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: rule.id, state: 'success' })
            }
            this.fetchData()
          },
          errorMessage: this.$t('message.remove.port.forward.failed'),
          errorMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: rule.id, state: 'failed' })
            }
            this.fetchData()
          },
          loadingMessage: this.$t('message.delete.port.forward.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => this.fetchData(),
          bulkAction: `${this.selectedItems.length > 0}` && this.showGroupActionModal
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    addRule () {
      if (this.loading) return
      this.loading = true
      this.addVmModalVisible = false
      const networkId = ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      api('createPortForwardingRule', {
        ...this.newRule,
        ipaddressid: this.resource.id,
        networkid: networkId
      }).then(response => {
        this.$pollJob({
          jobId: response.createportforwardingruleresponse.jobid,
          successMessage: this.$t('message.success.add.port.forward'),
          successMethod: () => {
            this.closeModal()
            this.fetchData()
          },
          errorMessage: this.$t('message.add.port.forward.failed'),
          errorMethod: () => {
            this.closeModal()
            this.fetchData()
          },
          loadingMessage: this.$t('message.add.port.forward.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.closeModal()
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    resetAllRules () {
      this.newRule.protocol = 'tcp'
      this.newRule.privateport = null
      this.newRule.privateendport = null
      this.newRule.publicport = null
      this.newRule.publicendport = null
      this.newRule.openfirewall = false
      this.newRule.vmguestip = null
      this.newRule.virtualmachineid = null
    },
    resetTagInputs () {
      if (this.formRef.value) {
        this.formRef.value.resetFields()
      }
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.addVmModalVisible = false
      this.newRule.virtualmachineid = null
      this.addVmModalLoading = false
      this.addVmModalNicLoading = false
      this.showConfirmationAction = false
      this.nics = []
      this.checked = false
      this.resetTagInputs()
    },
    openTagsModal (id) {
      this.initForm()
      this.tagsModalLoading = true
      this.selectedRule = id
      this.tagsModalVisible = true
      this.tags = []
      api('listTags', {
        resourceId: id,
        resourceType: 'PortForwardingRule',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
        this.tagsModalLoading = false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleAddTag (e) {
      if (this.tagsModalLoading) return
      this.tagsModalLoading = true

      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.tagsModalLoading = false

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule,
          resourceType: 'PortForwardingRule'
        }).then(response => {
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            successMessage: this.$t('message.success.add.tag'),
            successMethod: () => {
              this.parentToggleLoading()
              this.openTagsModal(this.selectedRule)
            },
            errorMessage: this.$t('message.add.tag.failed'),
            errorMethod: () => {
              this.parentToggleLoading()
              this.closeModal()
            },
            loadingMessage: this.$t('message.add.tag.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.closeModal()
            }
          })
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleDeleteTag (tag) {
      this.tagsModalLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedRule,
        resourceType: 'PortForwardingRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: this.$t('message.success.delete.tag'),
          successMethod: () => {
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: this.$t('message.delete.tag.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    openAddVMModal () {
      if (this.addVmModalLoading) return
      this.addVmModalVisible = true
      this.fetchVirtualMachines()
    },
    fetchNics (e) {
      this.nics = []
      this.addVmModalNicLoading = true
      this.newRule.virtualmachineid = e.target.value
      api('listNics', {
        virtualmachineid: e.target.value,
        networkId: ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      }).then(response => {
        if (!response.listnicsresponse.nic || response.listnicsresponse.nic.length < 1) return
        const nic = response.listnicsresponse.nic[0]
        this.nics.push(nic.ipaddress)
        if (nic.secondaryip && nic.secondaryip.length > 0) {
          this.nics.push(...nic.secondaryip.map(ip => ip.ipaddress))
        }
        this.newRule.vmguestip = this.nics[0]
        this.addVmModalNicLoading = false
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
        this.closeModal()
      })
    },
    fetchVirtualMachines () {
      this.vmCount = 0
      this.vms = []
      this.addVmModalLoading = true
      const networkId = ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      if (!networkId) {
        this.addVmModalLoading = false
        return
      }
      api('listVirtualMachines', {
        listAll: true,
        keyword: this.searchQuery,
        page: this.vmPage,
        pagesize: this.vmPageSize,
        networkid: networkId,
        account: this.resource.account,
        domainid: this.resource.domainid
      }).then(response => {
        this.vmCount = response.listvirtualmachinesresponse.count || 0
        this.vms = response.listvirtualmachinesresponse.virtualmachine
        this.addVmModalLoading = false
      }).catch(error => {
        this.$notifyError(error)
      })
    },
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
    handleChangeVmPage (page, pageSize) {
      this.vmPage = page
      this.vmPageSize = pageSize
      this.fetchVirtualMachines()
    },
    handleChangeVmPageSize (currentPage, pageSize) {
      this.vmPage = currentPage
      this.vmPageSize = pageSize
      this.fetchVirtualMachines()
    },
    onSearch (value) {
      this.searchQuery = value
      this.fetchVirtualMachines()
    }
  }
}
</script>

<style scoped lang="scss">
  .rule {

    &-container {
      display: flex;
      width: 100%;
      flex-wrap: wrap;
      margin-right: -20px;
      margin-bottom: -10px;
    }

    &__item {
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        flex: 1;
      }

    }

    &__title {
      font-weight: bold;
    }

  }

  .add-btn {
    width: 100%;
    padding-top: 15px;
    padding-bottom: 15px;
    height: auto;
  }

  .add-actions {
    display: flex;
    justify-content: flex-end;
    margin-right: -20px;
    margin-bottom: 20px;

    @media (min-width: 760px) {
      margin-top: 20px;
    }

    button {
      margin-right: 20px;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      /*flex: 1;*/
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

      &__input-container {
        display: flex;

        input {

          &:not(:last-child) {
            margin-right: 10px;
          }

        }

      }

    }

    &__label {
      font-weight: bold;
    }

  }

  .rule-action {

    &:not(:last-of-type) {
      margin-right: 10px;
    }

  }

  .tags {
    margin-bottom: 10px;
  }

  .add-tags {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;

    &__input {
      margin-right: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }

  }

  .tags-container {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 10px;
  }

  .add-tags-done {
    display: block;
    margin-left: auto;
  }

  .tags-modal-loading {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(0,0,0,0.5);
    z-index: 1;
    color: #1890ff;
    font-size: 2rem;
  }

  .vm-modal {

    &__header {
      display: flex;

      span {
        flex: 1;
        font-weight: bold;
        margin-right: 10px;
      }

    }

    &__item {
      display: flex;
      margin-top: 10px;

      span,
      label {
        display: block;
        flex: 1;
        margin-right: 10px;
      }

    }

  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }

  .list-view {
    overflow-y: auto;
    display: block;
    width: 100%;
  }

  .filter {
    display: block;
    width: 240px;
    margin-bottom: 10px;

    .form__item {
      width: 100%;
    }
  }

  .input-search {
    margin-bottom: 10px;
    width: 50%;
    float: right;
  }
</style>
