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
        <div class="form__label">{{ $t('label.add.by') }}:</div>
        <a-radio-group @change="resetAllRules" v-model="addType">
          <a-radio value="cidr">{{ $t('label.cidr') }}</a-radio>
          <a-radio value="account">{{ $t('label.account') }}</a-radio>
        </a-radio-group>
      </div>

      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('label.protocol') }}</div>
          <a-select
            autoFocus
            v-model="newRule.protocol"
            style="width: 100%;"
            @change="resetRulePorts">
            <a-select-option value="tcp">{{ $t('label.tcp') | capitalise }}</a-select-option>
            <a-select-option value="udp">{{ $t('label.udp') | capitalise }}</a-select-option>
            <a-select-option value="icmp">{{ $t('label.icmp') | capitalise }}</a-select-option>
            <a-select-option value="all">{{ $t('label.all') | capitalise }}</a-select-option>
            <a-select-option value="protocolnumber">{{ $t('label.protocol.number') | capitalise }}</a-select-option>
          </a-select>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('label.startport') }}</div>
          <a-input v-model="newRule.startport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'tcp' || newRule.protocol === 'udp'" class="form__item">
          <div class="form__label">{{ $t('label.endport') }}</div>
          <a-input v-model="newRule.endport"></a-input>
        </div>
        <div v-show="newRule.protocol === 'protocolnumber'" class="form__item">
          <div class="form__label">{{ $t('label.protocol.number') }}</div>
          <a-input v-model="newRule.protocolnumber"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('label.icmptype') }}</div>
          <a-input v-model="newRule.icmptype"></a-input>
        </div>
        <div v-show="newRule.protocol === 'icmp'" class="form__item">
          <div class="form__label">{{ $t('label.icmpcode') }}</div>
          <a-input v-model="newRule.icmpcode"></a-input>
        </div>
        <div class="form__item" v-if="addType === 'cidr'">
          <div class="form__label">{{ $t('label.cidr') }}</div>
          <a-input v-model="newRule.cidrlist"></a-input>
        </div>
        <template v-if="addType === 'account'">
          <div class="form__item">
            <div class="form__label">{{ $t('label.account') }}</div>
            <a-input v-model="newRule.usersecuritygrouplist.account" style="margin-right: 10px;"></a-input>
          </div>
          <div class="form__item">
            <div class="form__label">{{ $t('label.securitygroup') }}</div>
            <a-input v-model="newRule.usersecuritygrouplist.group"></a-input>
          </div>
        </template>
        <div class="form__item" style="flex: 0">
          <a-button :disabled="!('authorizeSecurityGroupInress' in $store.getters.apis) && !('authorizeSecurityGroupEgress' in $store.getters.apis)" type="primary" @click="handleAddRule">{{ $t('label.add') }}</a-button>
        </div>
      </div>
    </div>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="rules"
      :pagination="{ pageSizeOptions: ['10', '20', '40', '80', '100', '200'], showSizeChanger: true}"
      :rowKey="record => record.ruleid">
      <template slot="protocol" slot-scope="record">
        {{ record.protocol | capitalise }}
      </template>
      <template slot="account" slot-scope="record">
        <div v-if="record.account && record.securitygroupname">
          {{ record.account }} - {{ record.securitygroupname }}
        </div>
      </template>
      <template slot="startport" slot-scope="text, record">
        <div v-if="!['tcp', 'udp', 'icmp'].includes(record.protocol)">{{ $t('label.all') }}</div>
        <div v-else>{{ text }}</div>
      </template>
      <template slot="endport" slot-scope="text, record">
        <div v-if="!['tcp', 'udp', 'icmp'].includes(record.protocol)">{{ $t('label.all') }}</div>
        <div v-else>{{ text }}</div>
      </template>
      <template slot="actions" slot-scope="record">
        <tooltip-button :tooltip="$t('label.edit.tags')" icon="tag" buttonClass="rule-action" @click="() => openTagsModal(record)" />
        <a-popconfirm
          :title="$t('label.delete') + '?'"
          @confirm="handleDeleteRule(record)"
          :okText="$t('label.yes')"
          :cancelText="$t('label.no')"
        >
          <tooltip-button
            :disabled="!('revokeSecurityGroupIngress' in $store.getters.apis) && !('revokeSecurityGroupEgress' in $store.getters.apis)"
            :tooltip="$t('label.delete')"
            type="danger"
            icon="delete"
            buttonClass="rule-action" />
        </a-popconfirm>
      </template>
    </a-table>

    <a-modal
      :title="$t('label.edit.tags')"
      v-model="tagsModalVisible"
      :footer="null"
      :afterClose="closeModal"
      :maskClosable="false">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else>
        <a-form :form="newTagsForm" class="add-tags" @submit="handleAddTag">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.key') }}</p>
            <a-form-item>
              <a-input
                autoFocus
                v-decorator="['key', { rules: [{ required: true, message: this.$t('message.specifiy.tag.key')}] }]" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.value') }}</p>
            <a-form-item>
              <a-input v-decorator="['value', { rules: [{ required: true, message: this.$t('message.specifiy.tag.value')}] }]" />
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

        <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  components: {
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
      rules: [],
      newRule: {
        protocol: 'tcp',
        startport: null,
        endport: null,
        protocolnumber: null,
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
      page: 1,
      pagesize: 10,
      columns: [
        {
          title: this.$t('label.protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: this.$t('label.startport'),
          dataIndex: 'startport',
          scopedSlots: { customRender: 'startport' }
        },
        {
          title: this.$t('label.endport'),
          dataIndex: 'endport',
          scopedSlots: { customRender: 'endport' }
        },
        {
          title: this.$t('label.icmptype'),
          dataIndex: 'icmptype'
        },
        {
          title: this.$t('label.icmpcode'),
          dataIndex: 'icmpcode'
        },
        {
          title: this.$t('label.cidr'),
          dataIndex: 'cidr'
        },
        {
          title: this.$t('label.account.and.security.group'),
          scopedSlots: { customRender: 'account' }
        },
        {
          title: this.$t('label.action'),
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
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.tabType = this.$parent.tab === this.$t('label.ingress.rule') ? 'ingress' : 'egress'
      this.rules = this.tabType === 'ingress' ? this.resource.ingressrule : this.resource.egressrule
    },
    handleAddRule () {
      this.parentToggleLoading()
      api(this.tabType === 'ingress' ? 'authorizeSecurityGroupIngress' : 'authorizeSecurityGroupEgress', {
        securitygroupid: this.resource.id,
        domainid: this.resource.domainid,
        account: this.resource.account,
        protocol: this.newRule.protocol === 'protocolnumber' ? this.newRule.protocolnumber : this.newRule.protocol,
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
          successMessage: this.$t('message.success.add.rule'),
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: this.$t('message.add.rule.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: this.$t('message.add.rule.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
          successMessage: this.$t('message.success.remove.rule'),
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          errorMessage: this.$t('message.remove.rule.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
          },
          loadingMessage: this.$t('message.remove.securitygroup.rule.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
          successMessage: this.$t('message.success.delete.tag'),
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          loadingMessage: this.$t('message.delete.tag.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
            successMessage: this.$t('message.success.add.tag'),
            successMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            errorMessage: this.$t('message.add.tag.failed'),
            errorMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            loadingMessage: this.$t('message.add.tag.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
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
