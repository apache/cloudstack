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
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('label.privateport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              autoFocus
              v-model="newRule.privateport"
              :placeholder="$t('label.start')"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none; backgroundColor: #fff; text-align:
              center; margin-right: 0;"></a-input>
            <a-input
              v-model="newRule.privateendport"
              :placeholder="$t('label.end')"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.publicport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              v-model="newRule.publicport"
              :placeholder="$t('label.start')"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none; backgroundColor: #fff;
              text-align: center; margin-right: 0;"></a-input>
            <a-input
              v-model="newRule.publicendport"
              :placeholder="$t('label.end')"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.protocol') }}</div>
          <a-select v-model="newRule.protocol" style="width: 100%;">
            <a-select-option value="tcp">{{ $t('label.tcp') }}</a-select-option>
            <a-select-option value="udp">{{ $t('label.udp') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item" style="margin-left: auto;">
          <div class="form__label">{{ $t('label.add.vm') }}</div>
          <a-button :disabled="!('createPortForwardingRule' in $store.getters.apis)" type="primary" @click="openAddVMModal">{{ $t('label.add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="portForwardRules"
      :pagination="false"
      :rowKey="record => record.id">
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
        <div><a-icon type="desktop"/>
          <router-link
            :to="{ path: '/vm/' + record.virtualmachineid }">
            {{ record.virtualmachinename }}</router-link> ({{ record.vmguestip }})</div>
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <tooltip-button :tooltip="$t('label.tags')" icon="tag" buttonClass="rule-action" @click="() => openTagsModal(record.id)" />
          <tooltip-button
            :tooltip="$t('label.remove.rule')"
            type="danger"
            icon="delete"
            buttonClass="rule-action"
            :disabled="!('deletePortForwardingRule' in $store.getters.apis)"
            @click="deleteRule(record)" />
        </div>
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
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :title="$t('label.edit.tags')"
      v-model="tagsModalVisible"
      :footer="null"
      :maskClosable="false"
      :afterClose="closeModal">
      <span v-show="tagsModalLoading" class="tags-modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <div class="add-tags">
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.key') }}</p>
          <a-input autoFocus v-model="newTag.key"></a-input>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.value') }}</p>
          <a-input v-model="newTag.value"></a-input>
        </div>
        <a-button type="primary" @click="() => handleAddTag()">{{ $t('label.add') }}</a-button>
      </div>

      <a-divider></a-divider>

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.done') }}</a-button>
    </a-modal>

    <a-modal
      :title="$t('label.add.vm')"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      v-model="addVmModalVisible"
      class="vm-modal"
      width="60vw"
      @ok="addRule"
      :okButtonProps="{ props:
        {disabled: newRule.virtualmachineid === null } }"
      @cancel="closeModal"
    >
      <div>
        <span
          v-if="'vpcid' in resource && !('associatednetworkid' in resource)">
          <strong>{{ $t('label.select.tier') }} </strong>
          <a-select
            :autoFocu="'vpcid' in resource && !('associatednetworkid' in resource)"
            v-model="selectedTier"
            @change="fetchVirtualMachines()"
            :placeholder="$t('label.select.tier')" >
            <a-select-option
              v-for="tier in tiers.data"
              :loading="tiers.loading"
              :key="tier.id">
              {{ tier.displaytext }}
            </a-select-option>
          </a-select>
        </span>
        <a-input-search
          :autoFocu="!('vpcid' in resource && !('associatednetworkid' in resource))"
          class="input-search"
          :placeholder="$t('label.search')"
          v-model="searchQuery"
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
          <div slot="name" slot-scope="text, record">
            <span>
              {{ text }}
            </span>
            <a-icon v-if="addVmModalNicLoading" type="loading"></a-icon>
            <a-select
              style="display: block"
              v-else-if="!addVmModalNicLoading && newRule.virtualmachineid === record.id"
              v-model="newRule.vmguestip"
            >
              <a-select-option v-for="(nic, nicIndex) in nics" :key="nic" :value="nic">
                {{ nic }}{{ nicIndex === 0 ? ` (${$t('label.primary')})` : null }}
              </a-select-option>
            </a-select>
          </div>

          <div slot="state" slot-scope="text">
            <status :text="text ? text : ''" displayText></status>
          </div>

          <div slot="action" slot-scope="text, record" style="text-align: center">
            <a-radio :value="record.id" @change="e => fetchNics(e)" />
          </div>
        </a-table>
        <a-pagination
          class="pagination"
          size="small"
          :current="vmPage"
          :pageSize="vmPageSize"
          :total="vmCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="handleChangePage"
          @showSizeChange="handleChangePageSize"
          showSizeChanger>
          <template slot="buildOptionText" slot-scope="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  components: {
    Status,
    TooltipButton
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
      newTag: {
        key: null,
        value: null
      },
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
          title: this.$t('label.privateport'),
          scopedSlots: { customRender: 'privateport' }
        },
        {
          title: this.$t('label.publicport'),
          scopedSlots: { customRender: 'publicport' }
        },
        {
          title: this.$t('label.protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.vm'),
          scopedSlots: { customRender: 'vm' }
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'actions' }
        }
      ],
      tiers: {
        loading: false,
        data: []
      },
      vmColumns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' },
          width: 210
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
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
          title: this.$t('label.select'),
          dataIndex: 'action',
          scopedSlots: { customRender: 'action' },
          width: 80
        }
      ],
      vmPage: 1,
      vmPageSize: 10,
      vmCount: 0,
      searchQuery: null
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
    }
  },
  methods: {
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
        this.$forceUpdate()
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
    deleteRule (rule) {
      this.loading = true
      api('deletePortForwardingRule', { id: rule.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteportforwardingruleresponse.jobid,
          successMessage: this.$t('message.success.remove.port.forward'),
          successMethod: () => this.fetchData(),
          errorMessage: this.$t('message.remove.port.forward.failed'),
          errorMethod: () => this.fetchData(),
          loadingMessage: this.$t('message.delete.port.forward.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => this.fetchData()
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    addRule () {
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
            this.parentFetchData()
            this.fetchData()
          },
          errorMessage: this.$t('message.add.port.forward.failed'),
          errorMethod: () => {
            this.closeModal()
            this.parentFetchData()
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
      this.newTag.key = null
      this.newTag.value = null
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.addVmModalVisible = false
      this.newRule.virtualmachineid = null
      this.addVmModalLoading = false
      this.addVmModalNicLoading = false
      this.nics = []
      this.resetTagInputs()
    },
    openTagsModal (id) {
      this.tagsModalLoading = true
      this.selectedRule = id
      this.tagsModalVisible = true
      this.tags = []
      this.resetTagInputs()
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
    handleAddTag () {
      this.tagsModalLoading = true
      api('createTags', {
        'tags[0].key': this.newTag.key,
        'tags[0].value': this.newTag.value,
        resourceIds: this.selectedRule,
        resourceType: 'PortForwardingRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.createtagsresponse.jobid,
          successMessage: this.$t('message.success.add.tag'),
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: this.$t('message.add.tag.failed'),
          errorMethod: () => {
            this.parentFetchData()
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
      }).catch(error => {
        this.$notifyError(error)
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
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.parentFetchData()
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
      this.addVmModalVisible = true
      this.fetchVirtualMachines()
    },
    fetchNics (e) {
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
