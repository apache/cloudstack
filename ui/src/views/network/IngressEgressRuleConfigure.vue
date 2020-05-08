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
      <div style="margin-bottom: 20px;">
        <div class="form__label">Add by:</div>
        <a-radio-group @change="resetAllRules" v-model="addType">
          <a-radio value="cidr">CIDR</a-radio>
          <a-radio value="account">Account</a-radio>
        </a-radio-group>
      </div>

      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('protocol') }}</div>
          <a-select v-model="newRule.protocol" style="width: 100%;" @change="resetRulePorts">
            <a-select-option value="tcp">{{ $t('tcp') | capitalise }}</a-select-option>
            <a-select-option value="udp">{{ $t('udp') | capitalise }}</a-select-option>
            <a-select-option value="icmp">{{ $t('icmp') | capitalise }}</a-select-option>
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
        <div class="form__item" v-if="addType === 'cidr'">
          <div class="form__label">CIDR</div>
          <a-input v-model="newRule.cidrlist"></a-input>
        </div>
        <div class="form__item" v-if="addType === 'account'">
          <div class="form__label">Account, Security Group</div>
          <div style="display:flex;">
            <a-input v-model="newRule.usersecuritygrouplist.account" style="margin-right: 10px;"></a-input>
            <a-input v-model="newRule.usersecuritygrouplist.group"></a-input>
          </div>
        </div>
        <div class="form__item" style="flex: 0">
          <a-button type="primary" @click="handleAddRule">{{ $t('label.add') }}</a-button>
        </div>
      </div>
    </div>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="rules"
      :pagination="false"
      :rowKey="record => record.id">
      <template slot="protocol" slot-scope="record">
        {{ record.protocol | capitalise }}
      </template>
      <template slot="account" slot-scope="record">
        <div v-if="record.account && record.securitygroupname">
          {{ record.account }} - {{ record.securitygroupname }}
        </div>
      </template>
      <template slot="actions" slot-scope="record">
        <a-button shape="round" icon="tag" class="rule-action" @click="() => openTagsModal(record)" />
        <a-popconfirm
          :title="$t('label.delete') + '?'"
          @confirm="handleDeleteRule(record)"
          okText="Yes"
          cancelText="No"
        >
          <a-button shape="round" type="danger" icon="delete" class="rule-action" />
        </a-popconfirm>
      </template>
    </a-table>

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null" :afterClose="closeModal">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else>
        <a-form :form="newTagsForm" class="add-tags" @submit="handleAddTag">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('key') }}</p>
            <a-form-item>
              <a-input v-decorator="['key', { rules: [{ required: true, message: 'Please specify a tag key'}] }]" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('value') }}</p>
            <a-form-item>
              <a-input v-decorator="['value', { rules: [{ required: true, message: 'Please specify a tag value'}] }]" />
            </a-form-item>
          </div>
          <a-button type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
        </a-form>

        <a-divider style="margin-top: 0;"></a-divider>

        <div class="tags-container">
          <div class="tags" v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </div>
        </div>

        <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('OK') }}</a-button>
      </div>

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
      rules: [],
      newRule: {
        protocol: 'tcp',
        startport: null,
        endport: null,
        cidrlist: null,
        icmptype: null,
        icmpcode: null,
        usersecuritygrouplist: {
          account: null,
          group: null
        }
      },
      newTagsForm: this.$form.createForm(this),
      tagsModalVisible: false,
      tags: [],
      newTag: {
        key: null,
        value: null
      },
      selectedRule: null,
      tagsLoading: false,
      addType: 'cidr',
      tabType: null,
      columns: [
        {
          title: this.$t('protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: this.$t('startport'),
          dataIndex: 'startport'
        },
        {
          title: this.$t('endport'),
          dataIndex: 'endport'
        },
        {
          title: 'ICMP Type',
          dataIndex: 'icmptype'
        },
        {
          title: 'ICMP Code',
          dataIndex: 'icmpcode'
        },
        {
          title: 'CIDR',
          dataIndex: 'cidr'
        },
        {
          title: 'Account, Security Group',
          scopedSlots: { customRender: 'account' }
        },
        {
          title: this.$t('action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.tabType = this.$parent.tab === 'Ingress Rule' ? 'ingress' : 'egress'
      this.rules = this.tabType === 'ingress' ? this.resource.ingressrule : this.resource.egressrule
    },
    handleAddRule () {
      this.parentToggleLoading()
      api(this.tabType === 'ingress' ? 'authorizeSecurityGroupIngress' : 'authorizeSecurityGroupEgress', {
        securitygroupid: this.resource.id,
        domainid: this.resource.domainid,
        account: this.resource.account,
        protocol: this.newRule.protocol,
        startport: this.newRule.startport,
        endport: this.newRule.endport,
        cidrlist: this.newRule.cidrlist,
        icmptype: this.newRule.icmptype,
        icmpcode: this.newRule.icmpcode,
        'usersecuritygrouplist[0].account': this.newRule.usersecuritygrouplist.account,
        'usersecuritygrouplist[0].group': this.newRule.usersecuritygrouplist.group
      }).then(response => {
        this.$pollJob({
          jobId: this.tabType === 'ingress' ? response.authorizesecuritygroupingressresponse.jobid
            : response.authorizesecuritygroupegressresponse.jobid,
          successMessage: `Successfully added new rule`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: 'Failed to add new rule',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: `Adding new security-group rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.parentToggleLoading()
      })
    },
    handleDeleteRule (rule) {
      this.parentToggleLoading()
      api(this.tabType === 'ingress' ? 'revokeSecurityGroupIngress' : 'revokeSecurityGroupEgress', {
        id: rule.ruleid,
        domainid: this.resource.domainid,
        account: this.resource.account
      }).then(response => {
        this.$pollJob({
          jobId: this.tabType === 'ingress' ? response.revokesecuritygroupingressresponse.jobid
            : response.revokesecuritygroupegressresponse.jobid,
          successMessage: `Successfully deleted rule`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: 'Failed to delete rule',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: `Deleting security-group rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.parentToggleLoading()
      })
    },
    fetchTags (rule) {
      api('listTags', {
        resourceId: rule.ruleid,
        resourceType: 'SecurityGroupRule',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    handleDeleteTag (tag) {
      this.parentToggleLoading()
      this.tagsLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedRule.ruleid,
        resourceType: 'SecurityGroupRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: `Successfully deleted tag`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          errorMessage: 'Failed to delete tag',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          loadingMessage: `Deleting tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.parentToggleLoading()
        this.tagsLoading = false
      })
    },
    handleAddTag (e) {
      this.tagsLoading = true

      e.preventDefault()
      this.newTagsForm.validateFields((err, values) => {
        if (err) {
          this.tagsLoading = false
          return
        }

        this.parentToggleLoading()
        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule.ruleid,
          resourceType: 'SecurityGroupRule'
        }).then(response => {
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            successMessage: `Successfully added new tag`,
            successMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            errorMessage: 'Failed to add new tag',
            errorMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            loadingMessage: `Adding new tag...`,
            catchMessage: 'Error encountered while fetching async job result',
            catchMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
          this.parentToggleLoading()
          this.tagsLoading = false
        })
      })
    },
    openTagsModal (rule) {
      this.selectedRule = rule
      this.newTagsForm.resetFields()
      this.fetchTags(this.selectedRule)
      this.tagsModalVisible = true
    },
    closeModal () {
      this.newTag.key = null
      this.newTag.value = null
      this.selectedRule = null
    },
    resetRulePorts () {
      this.newRule.startport = null
      this.newRule.endport = null
      this.newRule.icmptype = null
      this.newRule.icmpcode = null
    },
    resetAllRules () {
      this.newRule.protocol = 'tcp'
      this.newRule.cidrlist = null
      this.newRule.usersecuritygrouplist.account = null
      this.newRule.usersecuritygrouplist.group = null
      this.resetRulePorts()
    }
  }
}
</script>

<style scoped lang="scss">
  .list {

    &__title {
      font-weight: bold;
    }

    &__col {
      flex: 1;
    }

  }

  .actions {
    display: flex;
    align-items: center;
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
      flex: 1;
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
    align-items: center;
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
</style>
