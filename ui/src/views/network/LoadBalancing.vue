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
        <div class="form__item" ref="newRuleName">
          <div class="form__label"><span class="form__required">*</span>{{ $t('name') }}</div>
          <a-input v-model="newRule.name"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item" ref="newRulePublicPort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('publicport') }}</div>
          <a-input v-model="newRule.publicport"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item" ref="newRulePrivatePort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('privateport') }}</div>
          <a-input v-model="newRule.privateport"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('algorithm') }}</div>
          <a-select v-model="newRule.algorithm">
            <a-select-option value="roundrobin">Round-robin</a-select-option>
            <a-select-option value="leastconn">Least connections</a-select-option>
            <a-select-option value="source">Source</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('protocol') }}</div>
          <a-select v-model="newRule.protocol" style="min-width: 100px">
            <a-select-option value="tcp-proxy">TCP Proxy</a-select-option>
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label" style="white-space: nowrap;">{{ $t('label.add.VMs') }}</div>
          <a-button type="primary" @click="handleOpenAddVMModal">Add</a-button>
        </div>
      </div>
    </div>

    <a-divider />

    <a-table
      size="small"
      style="overflow-y: auto"
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
          {{ $t('add') }}
        </a-button>
      </template>
      <template slot="expandedRowRender" slot-scope="record">
        <div class="rule-instance-list">
          <div v-for="instance in record.ruleInstances" :key="instance.loadbalancerruleinstance.id">
            <div v-for="ip in instance.lbvmipaddresses" :key="ip" class="rule-instance-list__item">
              <div>
                <a-icon type="desktop" />
                <router-link :to="{ path: '/vm/' + record.virtualmachineid }">
                  {{ instance.loadbalancerruleinstance.displayname }}
                </router-link>
              </div>
              <div>{{ ip }}</div>
              <div><status :text="instance.loadbalancerruleinstance.state" displayText /></div>
              <a-button
                size="small"
                shape="round"
                type="danger"
                icon="delete"
                @click="() => handleDeleteInstanceFromRule(instance, record, ip)" />
            </div>
          </div>
        </div>
      </template>
      <template slot="actions" slot-scope="record">
        <div class="actions">
          <a-button size="small" shape="circle" icon="edit" @click="() => openEditRuleModal(record)"></a-button>
          <a-button size="small" shape="circle" icon="tag" @click="() => openTagsModal(record.id)" />
          <a-popconfirm
            :title="$t('label.delete') + '?'"
            @confirm="handleDeleteRule(record)"
            okText="Yes"
            cancelText="No"
          >
            <a-button size="small" shape="circle" type="danger" icon="delete" />
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

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null" :afterClose="closeModal" class="tags-modal">
      <span v-show="tagsModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

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

      <a-divider></a-divider>

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('done') }}</a-button>
    </a-modal>

    <a-modal
      title="Configure Sticky Policy"
      v-model="stickinessModalVisible"
      :footer="null"
      :afterClose="closeModal"
      :okButtonProps="{ props: {htmlType: 'submit'}}">

      <span v-show="stickinessModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <a-form :form="stickinessPolicyForm" @submit="handleSubmitStickinessForm" class="custom-ant-form">
        <a-form-item label="Stickiness method">
          <a-select v-decorator="['methodname']" @change="handleStickinessMethodSelectChange">
            <a-select-option value="LbCookie">LbCookie</a-select-option>
            <a-select-option value="AppCookie">AppCookie</a-select-option>
            <a-select-option value="SourceBased">SourceBased</a-select-option>
            <a-select-option value="none">None</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="Sticky Name"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie' || stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['name', { rules: [{ required: true, message: 'Please specify a sticky name'}] }]" />
        </a-form-item>
        <a-form-item
          label="Cookie name"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-decorator="['cookieName']" />
        </a-form-item>
        <a-form-item
          label="Mode"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-decorator="['mode']" />
        </a-form-item>
        <a-form-item label="No cache" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['nocache']" v-model="stickinessNoCache"></a-checkbox>
        </a-form-item>
        <a-form-item label="Indirect" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['indirect']" v-model="stickinessIndirect"></a-checkbox>
        </a-form-item>
        <a-form-item label="Post only" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-decorator="['postonly']" v-model="stickinessPostOnly"></a-checkbox>
        </a-form-item>
        <a-form-item label="Domain" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-input v-decorator="['domain']" />
        </a-form-item>
        <a-form-item label="Length" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-decorator="['length']" type="number" />
        </a-form-item>
        <a-form-item label="Hold time" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-decorator="['holdtime']" type="number" />
        </a-form-item>
        <a-form-item label="Request learn" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-decorator="['requestLearn']" v-model="stickinessRequestLearn"></a-checkbox>
        </a-form-item>
        <a-form-item label="Prefix" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-decorator="['prefix']" v-model="stickinessPrefix"></a-checkbox>
        </a-form-item>
        <a-form-item label="Table size" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['tablesize']" />
        </a-form-item>
        <a-form-item label="Expires" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-decorator="['expire']" />
        </a-form-item>
        <a-button type="primary" html-type="submit">OK</a-button>
      </a-form>
    </a-modal>

    <a-modal title="Edit rule" v-model="editRuleModalVisible" :afterClose="closeModal" @ok="handleSubmitEditForm">
      <span v-show="editRuleModalLoading" class="modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <div class="edit-rule" v-if="selectedRule">
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('name') }}</p>
          <a-input v-model="editRuleDetails.name" />
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('algorithm') }}</p>
          <a-select v-model="editRuleDetails.algorithm">
            <a-select-option value="roundrobin">Round-robin</a-select-option>
            <a-select-option value="leastconn">Least connections</a-select-option>
            <a-select-option value="source">Source</a-select-option>
          </a-select>
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('protocol') }}</p>
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

      <a-icon v-if="addVmModalLoading" type="loading"></a-icon>

      <div v-else>
        <div class="vm-modal__header">
          <span style="min-width: 200px;">{{ $t('name') }}</span>
          <span>{{ $t('instancename') }}</span>
          <span>{{ $t('displayname') }}</span>
          <span>{{ $t('ip') }}</span>
          <span>{{ $t('account') }}</span>
          <span>{{ $t('zonenamelabel') }}</span>
          <span>{{ $t('state') }}</span>
          <span>{{ $t('select') }}</span>
        </div>

        <a-checkbox-group style="width: 100%;">
          <div v-for="(vm, index) in vms" :key="index" class="vm-modal__item">
            <span style="min-width: 200px;">
              <span>
                {{ vm.name }}
              </span>
              <a-icon v-if="addVmModalNicLoading" type="loading"></a-icon>
              <a-select
                v-else-if="!addVmModalNicLoading && newRule.virtualmachineid[index] === vm.id"
                mode="multiple"
                v-model="newRule.vmguestip[index]"
              >
                <a-select-option v-for="(nic, nicIndex) in nics[index]" :key="nic" :value="nic">
                  {{ nic }}{{ nicIndex === 0 ? ' (Primary)' : null }}
                </a-select-option>
              </a-select>
            </span>
            <span>{{ vm.instancename }}</span>
            <span>{{ vm.displayname }}</span>
            <span></span>
            <span>{{ vm.account }}</span>
            <span>{{ vm.zonename }}</span>
            <span>{{ vm.state }}</span>
            <a-checkbox :value="vm.id" @change="e => fetchNics(e, index)" />
          </div>
        </a-checkbox-group>
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
          title: this.$t('name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('publicport'),
          dataIndex: 'publicport'
        },
        {
          title: this.$t('privateport'),
          dataIndex: 'privateport'
        },
        {
          title: this.$t('algorithm'),
          scopedSlots: { customRender: 'algorithm' }
        },
        {
          title: this.$t('protocol'),
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: this.$t('state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.action.configure.stickiness'),
          scopedSlots: { customRender: 'stickiness' }
        },
        {
          title: this.$t('label.add.VMs'),
          scopedSlots: { customRender: 'add' }
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
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
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.createtagsresponse.errortext
          })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.deletetagsresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.createLBStickinessPolicy.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
      this.addVmModalLoading = true
      api('listVirtualMachines', {
        listAll: true,
        page: 1,
        pagesize: 500,
        networkid: this.resource.associatednetworkid,
        account: this.resource.account,
        domainid: this.resource.domainid
      }).then(response => {
        this.vms = response.listvirtualmachinesresponse.virtualmachine
        this.vms.forEach((vm, index) => {
          this.newRule.virtualmachineid[index] = null
          this.nics[index] = null
          this.newRule.vmguestip[index] = null
        })
        this.addVmModalLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.closeModal()
      })
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
        if (!response.listnicsresponse.nic[0]) return
        const newItem = []
        newItem.push(response.listnicsresponse.nic[0].ipaddress)
        if (response.listnicsresponse.nic[0].secondaryip) {
          newItem.push(...response.listnicsresponse.nic[0].secondaryip.map(ip => ip.ipaddress))
        }
        this.nics[index] = newItem
        this.newRule.vmguestip[index] = this.nics[index][0]
        this.addVmModalNicLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.closeModal()
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

      api('createLoadBalancerRule', {
        openfirewall: false,
        networkid: this.resource.associatednetworkid,
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.createloadbalancerruleresponse.errortext
        })
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
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }
</style>
