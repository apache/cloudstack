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
      <a-alert type="info" v-if="resource.state !== 'DISABLED'">
        <template #message>
        <div
          v-html="$t('message.autoscale.loadbalancer.update')" />
        </template>
      </a-alert>
    </div>
    <a-table
      size="small"
      class="list-view"
      :loading="loading"
      :columns="columns"
      :dataSource="lbRules"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'algorithm'">
        {{ returnAlgorithmName(record.algorithm) }}
        </template>
        <template v-if="column.key === 'protocol'">
          {{ getCapitalise(record.protocol) }}
        </template>
        <template v-if="column.key === 'stickiness'">
          <a-button @click="() => openStickinessModal(record.id)">
            {{ returnStickinessLabel(record.id) }}
          </a-button>
        </template>
        <template v-if="column.key === 'actions'">
          <div class="actions">
            <tooltip-button :tooltip="$t('label.edit')" icon="edit-outlined" @onClick="() => openEditRuleModal(record)" />
            <tooltip-button :tooltip="$t('label.edit.tags')" :disabled="!('updateLoadBalancerRule' in $store.getters.apis)" icon="tag-outlined" @onClick="() => openTagsModal(record.id)" />
            <a-popconfirm
              :title="$t('label.delete') + '?'"
              @confirm="handleDeleteRule(record)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
            >
              <tooltip-button
                :tooltip="$t('label.delete')"
                :disabled="!('deleteLoadBalancerRule' in $store.getters.apis) || record.autoscalevmgroup"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </div>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <div class="rule-instance-list">
          <div v-for="instance in record.ruleInstances" :key="instance.loadbalancerruleinstance.id">
            <div v-for="ip in instance.lbvmipaddresses" :key="ip" class="rule-instance-list__item">
              <div>
                <status :text="instance.loadbalancerruleinstance.state" />
                <desktop-outlined />
                <router-link :to="{ path: '/vm/' + instance.loadbalancerruleinstance.id }">
                  {{ instance.loadbalancerruleinstance.displayname }}
                </router-link>
              </div>
              <div>{{ ip }}</div>
              <tooltip-button
                :disabled='record.autoscalevmgroup && record.autoscalevmgroup.state != "DISABLED"'
                :tooltip="$t('label.remove.vm.from.lb')"
                type="primary"
                :danger="true"
                icon="delete-outlined"
                @onClick="() => handleDeleteInstanceFromRule(instance, record, ip)" />
            </div>
          </div>
        </div>
      </template>
    </a-table>
    <a-modal
      v-if="tagsModalVisible"
      :title="$t('label.edit.tags')"
      :visible="tagsModalVisible"
      :footer="null"
      :closable="true"
      :afterClose="closeModal"
      :maskClosable="false"
      class="tags-modal"
      @cancel="tagsModalVisible = false">
      <span v-show="tagsModalLoading" class="modal-loading">
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
          <a-form-item ref="key" name="key">
            <a-input
              v-focus="true"
              v-model:value="form.key" />
          </a-form-item>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.value') }}</p>
          <a-form-item ref="value" name="value">
            <a-input v-model:value="form.value" />
          </a-form-item>
        </div>
        <a-button :disabled="!('createTags' in $store.getters.apis)" type="primary" ref="submit" @click="handleAddTag">{{ $t('label.add') }}</a-button>
      </a-form>

      <a-divider />

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" @close="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.done') }}</a-button>
    </a-modal>

    <a-modal
      :title="$t('label.configure.sticky.policy')"
      :visible="stickinessModalVisible"
      :footer="null"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :okButtonProps="{ props: {htmlType: 'submit'}}"
      @cancel="stickinessModalVisible = false">

      <span v-show="stickinessModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmitStickinessForm"
        v-ctrl-enter="handleSubmitStickinessForm"
        class="custom-ant-form"
       >
        <a-form-item name="methodname" ref="methodname" :label="$t('label.stickiness.method')">
          <a-select
            v-focus="true"
            v-model:value="form.methodname"
            @change="handleStickinessMethodSelectChange"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="LbCookie" :label="$t('label.lb.cookie')">{{ $t('label.lb.cookie') }}</a-select-option>
            <a-select-option value="AppCookie" :label="$t('label.lb.cookie')">{{ $t('label.app.cookie') }}</a-select-option>
            <a-select-option value="SourceBased" :label="$t('label.lb.cookie')">{{ $t('label.source.based') }}</a-select-option>
            <a-select-option value="none" :label="$t('label.lb.cookie')">{{ $t('label.none') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="name"
          ref="name"
          :label="$t('label.sticky.name')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie' || stickinessPolicyMethod === 'SourceBased'">
          <a-input v-model:value="form.name" />
        </a-form-item>
        <a-form-item
          name="cookieName"
          ref="cookieName"
          :label="$t('label.sticky.cookie-name')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-model:value="form.cookieName" />
        </a-form-item>
        <a-form-item
          name="mode"
          ref="mode"
          :label="$t('label.sticky.mode')"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie'">
          <a-input v-model:value="form.mode" />
        </a-form-item>
        <a-form-item name="nocache" ref="nocache" :label="$t('label.sticky.nocache')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-model:checked="form.nocache"></a-checkbox>
        </a-form-item>
        <a-form-item name="indirect" ref="indirect" :label="$t('label.sticky.indirect')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-model:checked="form.indirect"></a-checkbox>
        </a-form-item>
        <a-form-item name="postonly" ref="postonly" :label="$t('label.sticky.postonly')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-checkbox v-model:checked="form.postonly"></a-checkbox>
        </a-form-item>
        <a-form-item name="domain" ref="domain" :label="$t('label.domain')" v-show="stickinessPolicyMethod === 'LbCookie'">
          <a-input v-model:value="form.domain" />
        </a-form-item>
        <a-form-item name="length" ref="length" :label="$t('label.sticky.length')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-model:value="form.length" type="number" />
        </a-form-item>
        <a-form-item name="holdtime" ref="holdtime" :label="$t('label.sticky.holdtime')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-input v-model:value="form.holdtime" type="number" />
        </a-form-item>
        <a-form-item name="requestLearn" ref="requestLearn" :label="$t('label.sticky.request-learn')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-model:checked="form.requestLearn"></a-checkbox>
        </a-form-item>
        <a-form-item name="prefix" ref="prefix" :label="$t('label.sticky.prefix')" v-show="stickinessPolicyMethod === 'AppCookie'">
          <a-checkbox v-model:checked="form.prefix"></a-checkbox>
        </a-form-item>
        <a-form-item name="tablesize" ref="tablesize" :label="$t('label.sticky.tablesize')" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-model:value="form.tablesize" />
        </a-form-item>
        <a-form-item name="expire" ref="expire" :label="$t('label.sticky.expire')" v-show="stickinessPolicyMethod === 'SourceBased'">
          <a-input v-model:value="form.expire" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="stickinessModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmitStickinessForm">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :title="$t('label.edit.rule')"
      :visible="editRuleModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="editRuleModalVisible = false">
      <span v-show="editRuleModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <div class="edit-rule" v-if="selectedRule" v-ctrl-enter="handleSubmitEditForm">
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.name') }}</p>
          <a-input v-focus="true" v-model:value="editRuleDetails.name" />
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.algorithm') }}</p>
          <a-select
            v-model:value="editRuleDetails.algorithm"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="roundrobin">{{ $t('label.lb.algorithm.roundrobin') }}</a-select-option>
            <a-select-option value="leastconn">{{ $t('label.lb.algorithm.leastconn') }}</a-select-option>
            <a-select-option value="source">{{ $t('label.lb.algorithm.source') }}</a-select-option>
          </a-select>
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.protocol') }}</p>
          <a-select
            v-model:value="editRuleDetails.protocol"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="tcp-proxy">{{ $t('label.tcp.proxy') }}</a-select-option>
            <a-select-option value="tcp">{{ $t('label.tcp') }}</a-select-option>
            <a-select-option value="udp">{{ $t('label.udp') }}</a-select-option>
          </a-select>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="() => editRuleModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmitEditForm">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw, nextTick } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'LoadBalancing',
  mixins: [mixinForm],
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
      lbRules: [],
      tagsModalVisible: false,
      tagsModalLoading: false,
      tags: [],
      selectedRule: null,
      selectedTier: null,
      stickinessModalVisible: false,
      stickinessPolicies: [],
      stickinessModalLoading: false,
      selectedStickinessPolicy: null,
      stickinessPolicyMethod: 'LbCookie',
      editRuleModalVisible: false,
      editRuleModalLoading: false,
      editRuleDetails: {
        name: '',
        algorithm: '',
        protocol: ''
      },
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
          key: 'algorithm'
        },
        {
          title: this.$t('label.protocol'),
          key: 'protocol'
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.action.configure.stickiness'),
          key: 'stickiness'
        },
        {
          title: this.$t('label.actions'),
          key: 'actions'
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
          key: 'name',
          width: 220
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          key: 'state'
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
          dataIndex: 'actions',
          key: 'actions',
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
      this.rules = reactive({})
    },
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
        this.selectedTier = this.tiers.data?.[0]?.id ? this.tiers.data[0].id : null
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
        id: this.resource.lbruleid,
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
          this.fetchAutoScaleVMgroups()
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
          rule.ruleInstances = response.listloadbalancerruleinstancesresponse.lbrulevmidip
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
    fetchAutoScaleVMgroups () {
      this.loading = true
      this.lbRules.forEach(rule => {
        api('listAutoScaleVmGroups', {
          listAll: true,
          lbruleid: rule.id
        }).then(response => {
          rule.autoscalevmgroup = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]
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
    getCapitalise (val) {
      if (!val) {
        return
      }
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
    },
    openTagsModal (id) {
      this.initForm()
      this.rules = {
        key: [{ required: true, message: this.$t('message.specify.tag.key') }],
        value: [{ required: true, message: this.$t('message.specify.tag.value') }]
      }
      this.tagsModalLoading = true
      this.tagsModalVisible = true
      this.tags = []
      this.selectedRule = id
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
      if (this.tagsModalLoading) return
      this.tagsModalLoading = true

      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule,
          resourceType: 'LoadBalancer'
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
        }).catch(error => {
          this.$notifyError(error)
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.tagsModalLoading = false
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
    openStickinessModal (id) {
      this.initForm()
      this.rules = {
        methodname: [{ required: true, message: this.$t('message.error.specify.stickiness.method') }]
      }
      this.stickinessModalVisible = true
      this.selectedRule = id
      const match = this.stickinessPolicies.find(policy => policy.lbruleid === id)

      if (match && match.stickinesspolicy.length > 0) {
        this.selectedStickinessPolicy = match.stickinesspolicy[0]
        this.stickinessPolicyMethod = this.selectedStickinessPolicy.methodname
        nextTick().then(() => {
          this.form.methodname = this.selectedStickinessPolicy.methodname
          this.form.name = this.selectedStickinessPolicy.name
          this.form.cookieName = this.selectedStickinessPolicy.params['cookie-name']
          this.form.mode = this.selectedStickinessPolicy.params.mode
          this.form.domain = this.selectedStickinessPolicy.params.domain
          this.form.length = this.selectedStickinessPolicy.params.length
          this.form.holdtime = this.selectedStickinessPolicy.params.holdtime
          this.form.nocache = !!this.selectedStickinessPolicy.params.nocache
          this.form.indirect = !!this.selectedStickinessPolicy.params.indirect
          this.form.postonly = !!this.selectedStickinessPolicy.params.postonly
          this.form.requestLearn = !!this.selectedStickinessPolicy.params['request-learn']
          this.form.prefix = !!this.selectedStickinessPolicy.params.prefix
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
          successMessage: this.$t('message.success.config.sticky.policy'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.config.sticky.policy.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.config.sticky.policy.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.closeModal()
      })
    },
    handleDeleteStickinessPolicy () {
      this.stickinessModalLoading = true
      api('deleteLBStickinessPolicy', { id: this.selectedStickinessPolicy.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteLBstickinessrruleresponse.jobid,
          successMessage: this.$t('message.success.remove.sticky.policy'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.remove.sticky.policy.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.remove.sticky.policy.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    handleSubmitStickinessForm (e) {
      if (this.stickinessModalLoading) return
      this.stickinessModalLoading = true
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        if (values.methodname === 'none') {
          this.handleDeleteStickinessPolicy()
          return
        }

        if (values.name === null || values.name === undefined || values.name === '') {
          this.$notification.error({
            message: this.$t('label.error'),
            description: this.$t('message.error.specify.sticky.name')
          })
          return
        }

        values.nocache = this.form.nocache
        values.indirect = this.form.indirect
        values.postonly = this.form.postonly
        values.requestLearn = this.form.requestLearn
        values.prefix = this.form.prefix

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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.stickinessModalLoading = false
      })
    },
    handleStickinessMethodSelectChange (e) {
      if (this.formRef.value) this.formRef.value.resetFields()
      this.stickinessPolicyMethod = e
      this.form.methodname = e
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
          successMessage: this.$t('message.success.remove.instance.rule'),
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.remove.instance.failed'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: this.$t('message.remove.instance.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
      if (this.editRuleModalLoading) return
      this.loading = true
      this.editRuleModalLoading = true
      api('updateLoadBalancerRule', {
        ...this.editRuleDetails,
        id: this.selectedRule.id
      }).then(response => {
        this.$pollJob({
          jobId: response.updateloadbalancerruleresponse.jobid,
          successMessage: this.$t('message.success.edit.rule'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.edit.rule.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.edit.rule.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
      })
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.stickinessModalVisible = false
      this.stickinessModalLoading = false
      this.selectedStickinessPolicy = null
      this.stickinessPolicyMethod = 'LbCookie'
      this.editRuleModalVisible = false
      this.editRuleModalLoading = false
      this.vms = []
      this.nics = []
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
    width: 50%;
    float: right;
  }
</style>
