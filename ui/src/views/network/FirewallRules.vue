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
          <div class="form__label">{{ $t('sourcecidr') }}</div>
          <a-input v-model="newRule.cidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('protocol') }}</div>
          <a-select v-model="newRule.protocol" style="width: 100%;" @change="resetRulePorts">
            <a-select-option value="tcp">{{ $t('tcp') }}</a-select-option>
            <a-select-option value="udp">{{ $t('udp') }}</a-select-option>
            <a-select-option value="icmp">{{ $t('icmp') }}</a-select-option>
          </a-select>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('startport') }}</div>
          <a-input v-model="newRule.startport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('endport') }}</div>
          <a-input v-model="newRule.endport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('icmptype') }}</div>
          <a-input v-model="newRule.icmptype"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('icmpcode') }}</div>
          <a-input v-model="newRule.icmpcode"></a-input>
        </div>
        <div class="form__item" style="margin-left: auto;">
          <a-button type="primary" @click="addRule">{{ $t('add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="firewallRules"
      :pagination="false"
      :rowKey="record => record.id">
      <template slot="protocol" slot-scope="record">
        {{ record.protocol | capitalise }}
      </template>
      <template slot="startport" slot-scope="record">
        {{ record.icmptype || record.startport >= 0 ? record.icmptype || record.startport : $t('all') }}
      </template>
      <template slot="endport" slot-scope="record">
        {{ record.icmpcode || record.endport >= 0 ? record.icmpcode || record.endport : $t('all') }}
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <a-button shape="round" icon="tag" class="rule-action" @click="() => openTagsModal(record.id)" />
          <a-button shape="round" type="danger" icon="delete" class="rule-action" @click="deleteRule(record)" />
        </div>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger/>

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null" :afterClose="closeModal">
      <div class="add-tags">
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('key') }}</p>
          <a-input v-model="newTag.key"></a-input>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('value') }}</p>
          <a-input v-model="newTag.value"></a-input>
        </div>
        <a-button type="primary" @click="() => handleAddTag()" :loading="addTagLoading">{{ $t('add') }}</a-button>
      </div>

      <a-divider></a-divider>

      <div class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('done') }}</a-button>
    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
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
      addTagLoading: false,
      firewallRules: [],
      newRule: {
        protocol: 'tcp',
        cidrlist: null,
        ipaddressid: this.resource.id,
        icmptype: null,
        icmpcode: null,
        startport: null,
        endport: null
      },
      tagsModalVisible: false,
      selectedRule: null,
      tags: [],
      newTag: {
        key: null,
        value: null
      },
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('sourcecidr'),
          dataIndex: 'cidrlist'
        },
        {
          title: this.$t('protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: `${this.$t('startport')}/${this.$t('icmptype')}`,
          scopedSlots: { customRender: 'startport' }
        },
        {
          title: `${this.$t('endport')}/${this.$t('icmpcode')}`,
          scopedSlots: { customRender: 'endport' }
        },
        {
          title: this.$t('state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  mounted () {
    this.fetchData()
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
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
  methods: {
    fetchData () {
      this.loading = true
      api('listFirewallRules', {
        listAll: true,
        ipaddressid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.firewallRules = response.listfirewallrulesresponse.firewallrule || []
        this.totalCount = response.listfirewallrulesresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    deleteRule (rule) {
      this.loading = true
      api('deleteFirewallRule', { id: rule.id }).then(response => {
        this.$pollJob({
          jobId: response.deletefirewallruleresponse.jobid,
          successMessage: `Successfully removed Firewall rule`,
          successMethod: () => this.fetchData(),
          errorMessage: 'Removing Firewall rule failed',
          errorMethod: () => this.fetchData(),
          loadingMessage: `Deleting Firewall rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => this.fetchData()
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    addRule () {
      this.loading = true
      api('createFirewallRule', { ...this.newRule }).then(response => {
        this.$pollJob({
          jobId: response.createfirewallruleresponse.jobid,
          successMessage: `Successfully added new Firewall rule`,
          successMethod: () => {
            this.resetAllRules()
            this.fetchData()
          },
          errorMessage: 'Adding new Firewall rule failed',
          errorMethod: () => {
            this.resetAllRules()
            this.fetchData()
          },
          loadingMessage: `Adding new Firewall rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.resetAllRules()
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.resetAllRules()
        this.fetchData()
      })
    },
    resetAllRules () {
      this.newRule.protocol = 'tcp'
      this.newRule.cidrlist = null
      this.newRule.networkid = this.resource.id
      this.resetRulePorts()
    },
    resetRulePorts () {
      this.newRule.icmptype = null
      this.newRule.icmpcode = null
      this.newRule.startport = null
      this.newRule.endport = null
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.newTag.key = null
      this.newTag.value = null
    },
    openTagsModal (id) {
      this.selectedRule = id
      this.tagsModalVisible = true
      api('listTags', {
        resourceId: id,
        resourceType: 'FirewallRule',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleAddTag () {
      this.addTagLoading = true
      api('createTags', {
        'tags[0].key': this.newTag.key,
        'tags[0].value': this.newTag.value,
        resourceIds: this.selectedRule,
        resourceType: 'FirewallRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.createtagsresponse.jobid,
          successMessage: `Successfully added tag`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: 'Failed to add new tag',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: `Adding tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      }).finally(() => {
        this.addTagLoading = false
      })
    },
    handleDeleteTag (tag) {
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedRule,
        resourceType: 'FirewallRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: `Successfully removed tag`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: 'Failed to remove tag',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: `Removing tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
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
    align-items: flex-end;
    margin-right: -20px;
    flex-direction: column;
    margin-bottom: 20px;

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
  .pagination {
    margin-top: 20px;
  }

</style>
