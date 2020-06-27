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
      <div class="filter" v-if="'vpcid' in resource && !('associatednetworkid' in resource)">
        <div class="form">
          <div class="form__item" ref="newRuleTier">
            <div class="form__label">{{ $t('label.tiername') }}</div>
            <a-select v-model="newRule.tier">
              <a-select-option
                v-for="tier in tiers.data"
                :loading="tiers.loading"
                :key="tier.id">
                {{ tier.displaytext }}
              </a-select-option>
            </a-select>
          </div>
        </div>
      </div>
      <div class="form">
        <div class="form__item" ref="newRuleName">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.name') }}</div>
          <a-input v-model="newRule.name"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item" ref="newRulePublicPort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.publicport') }}</div>
          <a-input v-model="newRule.publicport"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item" ref="newRulePrivatePort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.privateport') }}</div>
          <a-input v-model="newRule.privateport"></a-input>
          <span class="error-text">Required</span>
        </div>
      </div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('label.algorithm') }}</div>
          <a-select v-model="newRule.algorithm">
            <a-select-option value="roundrobin">Round-robin</a-select-option>
            <a-select-option value="leastconn">Least connections</a-select-option>
            <a-select-option value="source">Source</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.protocol') }}</div>
          <a-select v-model="newRule.protocol" style="min-width: 100px">
            <a-select-option value="tcp-proxy">TCP Proxy</a-select-option>
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label" style="white-space: nowrap;">{{ $t('label.add.vms') }}</div>
          <a-button :disabled="!('createLoadBalancerRule' in $store.getters.apis)" type="primary" @click="handleOpenAddVMModal">Add</a-button>
        </div>
      </div>
    </div>

    <a-divider />

    <a-table
      size="small"
      class="list-view"
      :loading="loading"
      :columns="columns"
      :dataSource="lbRules"
      :pagination="false"
      :rowKey="record => record.id">
      <template slot="algorithm" slot-scope="record">
        {{ returnAlgorithmName(record.algorithm) }}
      </template>
      <template slot="protocol" slot-scope="record">
        {{ record.protocol | capitalise }}
      </template>
      <template slot="stickiness" slot-scope="record">
        <a-button @click="() => openStickinessModal(record.id)">
          {{ returnStickinessLabel(record.id) }}
        </a-button>
      </template>
      <template slot="add" slot-scope="record">
        <a-button type="primary" icon="plus" @click="() => { selectedRule = record; handleOpenAddVMModal() }">
          {{ $t('label.add') }}
        </a-button>
      </template>
      <template slot="expandedRowRender" slot-scope="record">
        <div class="rule-instance-list">
          <div v-for="instance in record.ruleInstances" :key="instance.loadbalancerruleinstance.id">
            <div v-for="ip in instance.lbvmipaddresses" :key="ip" class="rule-instance-list__item">
              <div>
                <status :text="instance.loadbalancerruleinstance.state" />
                <a-icon type="desktop" />
                <router-link :to="{ path: '/vm/' + instance.loadbalancerruleinstance.id }">
                  {{ instance.loadbalancerruleinstance.displayname }}
                </router-link>
              </div>
              <div>{{ ip }}</div>
              <a-button
                shape="circle"
                type="danger"
                icon="delete"
                @click="() => handleDeleteInstanceFromRule(instance, record, ip)" />
            </div>
          </div>
        </div>
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <a-button shape="circle" icon="edit" @click="() => openEditRuleModal(record)"></a-button>
          <a-button :disabled="!('editLoadBalancerRule' in $store.getters.apis)" shape="circle" icon="tag" @click="() => openTagsModal(record.id)" />
          <a-popconfirm
            :title="$t('label.delete') + '?'"
            @confirm="handleDeleteRule(record)"
            okText="Yes"
            cancelText="No"
          >
            <a-button :disabled="!('deleteLoadBalancerRule' in $store.getters.apis)" shape="circle" type="danger" icon="delete" />
          </a-popconfirm>
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

    <a-modal :title="$t('label.edit.tags')" v-model="tagsModalVisible" :footer="null" :afterClose="closeModal" class="tags-modal">
      <span v-show="tagsModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <a-form :form="newTagsForm" class="add-tags" @submit="handleAddTag">
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.key') }}</p>
          <a-form-item>
            <a-input v-decorator="['key', { rules: [{ required: true, message: 'Please specify a tag key'}] }]" />
          </a-form-item>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.value') }}</p>
          <a-form-item>
            <a-input v-decorator="['value', { rules: [{ required: true, message: 'Please specify a tag value'}] }]" />
          </a-form-item>
        </div>
        <a-button :disabled="!('createTags' in $store.getters.apis)" type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
      </a-form>

      <a-divider></a-divider>

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="'deleteTag' in $store.getters.apis" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.done') }}</a-button>
    </a-modal>

    <a-modal
      :title="$t('label.configure.sticky.policy')"
      v-model="stickinessModalVisible"
      :footer="null"
      :afterClose="closeModal"
      :okButtonProps="{ props: {htmlType: 'submit'}}">

      <span v-show="stickinessModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <a-form :form="stickinessPolicyForm" @submit="handleSubmitStickinessForm" class="custom-ant-form">
        <a-form-item :label="$t('label.stickiness.method')">
          <a-select v-decorator="['methodname']" @change="handleStickinessMethodSelectChange">
            <a-select-option value="LbCookie">LbCookie</a-select-option>
            <a-select-option value="AppCookie">AppCookie</a-select-option>
            <a-select-option value="SourceBased">SourceBased</a-select-option>
            <a-select-option value="none">None</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          :label="$t('label.sticky.name')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie' || stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['name', { rules: [{ required: true, message: 'Please specify a sticky name'}] }]" />
        </a-form-item>
        <a-form-item
          :label="$t('label.sticky.cookie-name')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-decorator="['cookieName']" />
        </a-form-item>
        <a-form-item
          :label="$t('label.sticky.mode')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-decorator="['mode']" />
        </a-form-item>
        <a-form-item :label="$t('label.sticky.nocache')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['nocache']" v-model="stickinessNoCache"></a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('label.sticky.indirect')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['indirect']" v-model="stickinessIndirect"></a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('label.sticky.postonly')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['postonly']" v-model="stickinessPostOnly"></a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('label.domain')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-input v-decorator="['domain']" />
        </a-form-item>
        <a-form-item :label="$t('label.sticky.length')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-decorator="['length']" type="number" />
        </a-form-item>
        <a-form-item :label="$t('label.sticky.holdtime')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-decorator="['holdtime']" type="number" />
        </a-form-item>
        <a-form-item :label="$t('label.sticky.request-learn')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-decorator="['requestLearn']" v-model="stickinessRequestLearn"></a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('label.sticky.prefix')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-decorator="['prefix']" v-model="stickinessPrefix"></a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('label.sticky.tablesize')" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['tablesize']" />
        </a-form-item>
        <a-form-item :label="$t('label.sticky.expire')" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['expire']" />
        </a-form-item>
        <a-button type="primary" html-type="submit">OK</a-button>
      </a-form>
    </a-modal>

    <a-modal :title="$t('label.edit.rule')" v-model="editRuleModalVisible" :afterClose="closeModal" @ok="handleSubmitEditForm">
      <span v-show="editRuleModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <div class="edit-rule" v-if="selectedRule">
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.name') }}</p>
          <a-input v-model="editRuleDetails.name" />
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.algorithm') }}</p>
          <a-select v-model="editRuleDetails.algorithm">
            <a-select-option value="roundrobin">Round-robin</a-select-option>
            <a-select-option value="leastconn">Least connections</a-select-option>
            <a-select-option value="source">Source</a-select-option>
          </a-select>
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.protocol') }}</p>
          <a-select v-model="editRuleDetails.protocol">
            <a-select-option value="tcp-proxy">TCP proxy</a-select-option>
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
          </a-select>
        </div>
      </div>
    </a-modal>

    <a-modal
      title="Add VMs"
      v-model="addVmModalVisible"
      class="vm-modal"
      width="60vw"
      @ok="handleAddNewRule"
      :okButtonProps="{ props:
        {disabled: newRule.virtualmachineid === [] } }"
      @cancel="closeModal"
    >
      <div>
        <a-input-search
          class="input-search"
          placeholder="Search"
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
          <div slot="name" slot-scope="text, record, index">
            <span>
              {{ text }}
            </span>
            <a-icon v-if="addVmModalNicLoading" type="loading"></a-icon>
            <a-select
              style="display: block"
              v-else-if="!addVmModalNicLoading && newRule.virtualmachineid[index] === record.id"
              mode="multiple"
              v-model="newRule.vmguestip[index]"
            >
              <a-select-option v-for="(nic, nicIndex) in nics[index]" :key="nic" :value="nic">
                {{ nic }}{{ nicIndex === 0 ? ' (Primary)' : null }}
              </a-select-option>
            </a-select>
          </div>

          <div slot="state" slot-scope="text">
            <status :text="text ? text : ''" displayText></status>
          </div>

          <div slot="action" slot-scope="text, record, index" style="text-align: center">
            <a-checkbox :value="record.id" @change="e => fetchNics(e, index)" />
          </div>
        </a-table>
        <a-pagination
          class="pagination"
          size="small"
          :current="vmPage"
          :pageSize="vmPageSize"
          :total="vmCount"
          :showTotal="total => `Total ${total} items`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="handleChangePage"
          @showSizeChange="handleChangePageSize"
          showSizeChanger/>
      </div>

    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'LoadBalancing',
  components: {
    Status
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
      lbRules: [],
      newTagsForm: this.$form.createForm(this),
      tagsModalVisible: false,
      tagsModalLoading: false,
      tags: [],
      selectedRule: null,
      stickinessModalVisible: false,
      stickinessPolicies: [],
      stickinessPolicyForm: this.$form.createForm(this),
      stickinessModalLoading: false,
      selectedStickinessPolicy: null,
      stickinessPolicyMethod: 'LbCookie',
      stickinessNoCache: null,
      stickinessIndirect: null,
      stickinessPostOnly: null,
      stickinessRequestLearn: null,
      stickinessPrefix: null,
      editRuleModalVisible: false,
      editRuleModalLoading: false,
      editRuleDetails: {
        name: '',
        algorithm: '',
        protocol: ''
      },
      newRule: {
        algorithm: 'roundrobin',
        name: '',
        privateport: '',
        publicport: '',
        protocol: 'tcp',
        virtualmachineid: [],
        vmguestip: []
      },
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
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.publicport'),
          dataIndex: 'publicport'
        },
        {
          title: this.$t('label.privateport'),
          dataIndex: 'privateport'
        },
        {
          title: this.$t('label.algorithm'),
          scopedSlots: { customRender: 'algorithm' }
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
          title: this.$t('label.action.configure.stickiness'),
          scopedSlots: { customRender: 'stickiness' }
        },
        {
          title: this.$t('label.add.vms'),
          scopedSlots: { customRender: 'add' }
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
          width: 220
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
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.zonename'),
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
  mounted () {
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
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
  },
  methods: {
    fetchData () {
      this.fetchListTiers()
      this.fetchLBRules()
    },
    fetchListTiers () {
      this.tiers.loading = true

      api('listNetworks', {
        account: this.resource.account,
        domainid: this.resource.domainid,
        supportedservices: 'Lb',
        vpcid: this.resource.vpcid
      }).then(json => {
        this.tiers.data = json.listnetworksresponse.network || []
        this.newRule.tier = this.tiers.data && this.tiers.data[0].id ? this.tiers.data[0].id : null
        this.$forceUpdate()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.tiers.loading = false })
    },
    fetchLBRules () {
      this.loading = true
      this.lbRules = []
      this.stickinessPolicies = []

      api('listLoadBalancerRules', {
        listAll: true,
        publicipid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.lbRules = response.listloadbalancerrulesresponse.loadbalancerrule || []
        this.totalCount = response.listloadbalancerrulesresponse.count || 0
      }).then(() => {
        if (this.lbRules.length > 0) {
          setTimeout(() => {
            this.fetchLBRuleInstances()
          }, 100)
          this.fetchLBStickinessPolicies()
          return
        }
        this.loading = false
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    fetchLBRuleInstances () {
      for (const rule of this.lbRules) {
        this.loading = true
        api('listLoadBalancerRuleInstances', {
          listAll: true,
          lbvmips: true,
          id: rule.id
        }).then(response => {
          this.$set(rule, 'ruleInstances', response.listloadbalancerruleinstancesresponse.lbrulevmidip)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }
    },
    fetchLBStickinessPolicies () {
      this.loading = true
      this.lbRules.forEach(rule => {
        api('listLBStickinessPolicies', {
          listAll: true,
          lbruleid: rule.id
        }).then(response => {
          this.stickinessPolicies.push(...response.listlbstickinesspoliciesresponse.stickinesspolicies)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
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
    returnStickinessLabel (id) {
      const match = this.stickinessPolicies.filter(policy => policy.lbruleid === id)
      if (match.length > 0 && match[0].stickinesspolicy.length > 0) {
        return match[0].stickinesspolicy[0].methodname
      }
      return 'Configure'
    },
    openTagsModal (id) {
      this.tagsModalLoading = true
      this.tagsModalVisible = true
      this.tags = []
      this.selectedRule = id
      this.newTagsForm.resetFields()
      api('listTags', {
        resourceId: id,
        resourceType: 'LoadBalancer',
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
      this.tagsModalLoading = true

      e.preventDefault()
      this.newTagsForm.validateFields((err, values) => {
        if (err) {
          this.tagsModalLoading = false
          return
        }

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule,
          resourceType: 'LoadBalancer'
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
        })
      })
    },
    handleDeleteTag (tag) {
      this.tagsModalLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: tag.resourceid,
        resourceType: 'LoadBalancer'
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
    openStickinessModal (id) {
      this.stickinessModalVisible = true
      this.selectedRule = id
      const match = this.stickinessPolicies.find(policy => policy.lbruleid === id)

      if (match && match.stickinesspolicy.length > 0) {
        this.selectedStickinessPolicy = match.stickinesspolicy[0]
        this.stickinessPolicyMethod = this.selectedStickinessPolicy.methodname
        this.$nextTick(() => {
          this.stickinessPolicyForm.setFieldsValue({ methodname: this.selectedStickinessPolicy.methodname })
          this.stickinessPolicyForm.setFieldsValue({ name: this.selectedStickinessPolicy.name })
          this.stickinessPolicyForm.setFieldsValue({ cookieName: this.selectedStickinessPolicy.params['cookie-name'] })
          this.stickinessPolicyForm.setFieldsValue({ mode: this.selectedStickinessPolicy.params.mode })
          this.stickinessPolicyForm.setFieldsValue({ domain: this.selectedStickinessPolicy.params.domain })
          this.stickinessPolicyForm.setFieldsValue({ length: this.selectedStickinessPolicy.params.length })
          this.stickinessPolicyForm.setFieldsValue({ holdtime: this.selectedStickinessPolicy.params.holdtime })
          this.stickinessNoCache = !!this.selectedStickinessPolicy.params.nocache
          this.stickinessIndirect = !!this.selectedStickinessPolicy.params.indirect
          this.stickinessPostOnly = !!this.selectedStickinessPolicy.params.postonly
          this.stickinessRequestLearn = !!this.selectedStickinessPolicy.params['request-learn']
          this.stickinessPrefix = !!this.selectedStickinessPolicy.params.prefix
        })
      }
    },
    handleAddStickinessPolicy (data, values) {
      api('createLBStickinessPolicy', {
        ...data,
        lbruleid: this.selectedRule,
        name: values.name,
        methodname: values.methodname
      }).then(response => {
        this.$pollJob({
          jobId: response.createLBStickinessPolicy.jobid,
          successMessage: `Successfully configured sticky policy`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: 'Failed to configure sticky policy',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: `Updating sticky policy...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleDeleteStickinessPolicy () {
      this.stickinessModalLoading = true
      api('deleteLBStickinessPolicy', { id: this.selectedStickinessPolicy.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteLBstickinessrruleresponse.jobid,
          successMessage: `Successfully removed sticky policy`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: 'Failed to remove sticky policy',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: `Removing sticky policy...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleSubmitStickinessForm (e) {
      this.stickinessModalLoading = true
      e.preventDefault()
      this.stickinessPolicyForm.validateFields((err, values) => {
        if (err) {
          this.stickinessModalLoading = false
          return
        }
        if (values.methodname === 'none') {
          this.handleDeleteStickinessPolicy()
          return
        }

        values.nocache = this.stickinessNoCache
        values.indirect = this.stickinessIndirect
        values.postonly = this.stickinessPostOnly
        values.requestLearn = this.stickinessRequestLearn
        values.prefix = this.stickinessPrefix

        let data = {}
        let count = 0
        Object.entries(values).forEach(([key, val]) => {
          if (val && key !== 'name' && key !== 'methodname') {
            if (key === 'cookieName') {
              data = { ...data, ...{ [`param[${count}].name`]: 'cookie-name' } }
            } else if (key === 'requestLearn') {
              data = { ...data, ...{ [`param[${count}].name`]: 'request-learn' } }
            } else {
              data = { ...data, ...{ [`param[${count}].name`]: key } }
            }
            data = { ...data, ...{ [`param[${count}].value`]: val } }
            count++
          }
        })

        this.handleAddStickinessPolicy(data, values)
      })
    },
    handleStickinessMethodSelectChange (e) {
      this.stickinessPolicyMethod = e
    },
    handleDeleteInstanceFromRule (instance, rule, ip) {
      this.loading = true
      api('removeFromLoadBalancerRule', {
        id: rule.id,
        'vmidipmap[0].vmid': instance.loadbalancerruleinstance.id,
        'vmidipmap[0].vmip': ip
      }).then(response => {
        this.$pollJob({
          jobId: response.removefromloadbalancerruleresponse.jobid,
          successMessage: `Successfully removed instance from rule`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: 'Failed to remove instance',
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: `Removing...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    openEditRuleModal (rule) {
      this.selectedRule = rule
      this.editRuleModalVisible = true
      this.editRuleDetails.name = this.selectedRule.name
      this.editRuleDetails.algorithm = this.selectedRule.algorithm
      this.editRuleDetails.protocol = this.selectedRule.protocol
    },
    handleSubmitEditForm () {
      this.loading = true
      this.editRuleModalLoading = true
      api('updateLoadBalancerRule', {
        ...this.editRuleDetails,
        id: this.selectedRule.id
      }).then(response => {
        this.$pollJob({
          jobId: response.updateloadbalancerruleresponse.jobid,
          successMessage: `Successfully edited rule`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: 'Failed to edit rule',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: `Updating rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
        this.closeModal()
      })
    },
    handleDeleteRule (rule) {
      this.loading = true
      api('deleteLoadBalancerRule', {
        id: rule.id
      }).then(response => {
        this.$pollJob({
          jobId: response.deleteloadbalancerruleresponse.jobid,
          successMessage: `Successfully deleted rule`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: 'Failed to delete rule',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: `Deleting rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
        this.closeModal()
      })
    },
    handleOpenAddVMModal () {
      if (!this.selectedRule) {
        if (!this.newRule.name) {
          this.$refs.newRuleName.classList.add('error')
        } else {
          this.$refs.newRuleName.classList.remove('error')
        }
        if (!this.newRule.publicport) {
          this.$refs.newRulePublicPort.classList.add('error')
        } else {
          this.$refs.newRulePublicPort.classList.remove('error')
        }
        if (!this.newRule.privateport) {
          this.$refs.newRulePrivatePort.classList.add('error')
        } else {
          this.$refs.newRulePrivatePort.classList.remove('error')
        }
        if (!this.newRule.name || !this.newRule.publicport || !this.newRule.privateport) return
      }
      this.addVmModalVisible = true
      this.fetchVirtualMachines()
    },
    fetchNics (e, index) {
      if (!e.target.checked) {
        this.newRule.virtualmachineid[index] = null
        this.nics[index] = null
        this.newRule.vmguestip[index] = null
        return
      }
      this.newRule.virtualmachineid[index] = e.target.value
      this.addVmModalNicLoading = true

      api('listNics', {
        virtualmachineid: e.target.value,
        networkid: this.resource.associatednetworkid
      }).then(response => {
        if (!response || !response.listnicsresponse || !response.listnicsresponse.nic[0]) return
        const newItem = []
        newItem.push(response.listnicsresponse.nic[0].ipaddress)
        if (response.listnicsresponse.nic[0].secondaryip) {
          newItem.push(...response.listnicsresponse.nic[0].secondaryip.map(ip => ip.ipaddress))
        }
        this.nics[index] = newItem
        this.newRule.vmguestip[index] = this.nics[index][0]
        this.addVmModalNicLoading = false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    fetchVirtualMachines () {
      this.vmCount = 0
      this.vms = []
      this.addVmModalLoading = true
      const networkId = ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.newRule.tier : this.resource.associatednetworkid
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
        this.vms = response.listvirtualmachinesresponse.virtualmachine || []
        this.vms.forEach((vm, index) => {
          this.newRule.virtualmachineid[index] = null
          this.nics[index] = null
          this.newRule.vmguestip[index] = null
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addVmModalLoading = false
      })
    },
    handleAssignToLBRule (data) {
      const vmIDIpMap = {}

      let count = 0
      let innerCount = 0
      this.newRule.vmguestip.forEach(ip => {
        if (Array.isArray(ip)) {
          ip.forEach(i => {
            vmIDIpMap[`vmidipmap[${innerCount}].vmid`] = this.newRule.virtualmachineid[count]
            vmIDIpMap[`vmidipmap[${innerCount}].vmip`] = i
            innerCount++
          })
        } else {
          vmIDIpMap[`vmidipmap[${innerCount}].vmid`] = this.newRule.virtualmachineid[count]
          vmIDIpMap[`vmidipmap[${innerCount}].vmip`] = ip
          innerCount++
        }
        count++
      })

      this.loading = true
      api('assignToLoadBalancerRule', {
        id: data,
        ...vmIDIpMap
      }).then(response => {
        this.$pollJob({
          jobId: response.assigntoloadbalancerruleresponse.jobid,
          successMessage: `Successfully assigned VM`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: 'Failed to assign VM',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: `Assigning VM...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      })
    },
    handleAddNewRule () {
      this.loading = true

      if (this.selectedRule) {
        this.handleAssignToLBRule(this.selectedRule.id)
        return
      }

      const networkId = ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.newRule.tier : this.resource.associatednetworkid
      api('createLoadBalancerRule', {
        openfirewall: false,
        networkid: networkId,
        publicipid: this.resource.id,
        algorithm: this.newRule.algorithm,
        name: this.newRule.name,
        privateport: this.newRule.privateport,
        protocol: this.newRule.protocol,
        publicport: this.newRule.publicport
      }).then(response => {
        this.addVmModalVisible = false
        this.handleAssignToLBRule(response.createloadbalancerruleresponse.id)
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })

      // assigntoloadbalancerruleresponse.jobid
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.stickinessModalVisible = false
      this.stickinessModalLoading = false
      this.selectedStickinessPolicy = null
      this.stickinessPolicyMethod = 'LbCookie'
      this.stickinessNoCache = null
      this.stickinessIndirect = null
      this.stickinessPostOnly = null
      this.editRuleModalVisible = false
      this.editRuleModalLoading = false
      this.addVmModalLoading = false
      this.addVmModalNicLoading = false
      this.vms = []
      this.nics = []
      this.addVmModalVisible = false
      this.newRule.virtualmachineid = []
      this.newTagsForm.resetFields()
      this.stickinessPolicyForm.resetFields()
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

<style lang="scss" scoped>
  .rule {

    &-container {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 760px) {
        margin-right: -20px;
        margin-bottom: -10px;
      }

    }

    &__row {
      display: flex;
      flex-wrap: wrap;
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
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__required {
      margin-right: 5px;
      color: red;
    }

    .error-text {
      display: none;
      color: red;
      font-size: 0.8rem;
    }

    .error {

      input {
        border-color: red;
      }

      .error-text {
        display: block;
      }

    }

    &--column {
      flex-direction: column;
      margin-right: 0;
      align-items: flex-end;

      .form__item {
        width: 100%;
        padding-right: 0;
      }

    }

    &__item {
      display: flex;
      flex-direction: column;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 1200px) {
        margin-bottom: 0;
        flex: 1;
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
    margin-bottom: 10px;
  }

  .tags-modal {

    .ant-divider {
      margin-top: 0;
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

  .modal-loading {
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

  .ant-list-item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
      align-items: center;
    }

  }

  .rule-instance-collapse {
    width: 100%;
    margin-left: -15px;

    .ant-collapse-item {
      border: 0;
    }

  }

  .rule-instance-list {
    display: flex;
    flex-direction: column;

    &__item {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      div {
        margin-left: 25px;
        margin-bottom: 10px;
      }
    }
  }

  .edit-rule {

    .ant-select {
      width: 100%;
    }

    &__item {
      margin-bottom: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }

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

  .custom-ant-form {
    .ant-form-item-label {
      font-weight: bold;
      line-height: 1;
    }
    .ant-form-item {
      margin-bottom: 10px;
    }
  }

  .custom-ant-list {
    .ant-list-item-action {
      margin-top: 10px;
      margin-left: 0;

      @media (min-width: 760px) {
        margin-top: 0;
        margin-left: 24px;
      }

    }
  }

  .rule-instance-collapse {
    .ant-collapse-header,
    .ant-collapse-content {
      margin-left: -12px;
    }
  }

  .rule {
    .ant-list-item-content-single {
      width: 100%;

      @media (min-width: 760px) {
        width: auto;
      }

    }
  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
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
  }

  .input-search {
    margin-bottom: 10px;
    width: 220px;
    float: right;
  }
</style>
