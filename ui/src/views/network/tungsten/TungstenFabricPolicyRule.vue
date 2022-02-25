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
    <a-button
      :disabled="!('addTungstenFabricPolicyRule' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      {{ $t('label.add.rule') }}
    </a-button>
    <a-table
      size="small"
      :loading="loading || fetchLoading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
      <template slot="srcnetwork" slot-scope="text, record">
        <span>{{ record.srcipprefix + '/' + record.srcipprefixlen }}</span>
      </template>
      <template slot="sourceport" slot-scope="text, record">
        <span>{{ record.srcstartport + ':' + record.srcendport }}</span>
      </template>
      <template slot="destnetwork" slot-scope="text, record">
        <span>{{ record.destipprefix + '/' + record.destipprefixlen }}</span>
      </template>
      <template slot="destport" slot-scope="text, record">
        <span>{{ record.deststartport + ':' + record.destendport }}</span>
      </template>
      <template slot="ruleAction" slot-scope="text, record">
        <a-popconfirm
          v-if="'removeTungstenFabricPolicyRule' in $store.getters.apis"
          placement="topRight"
          :title="$t('message.delete.tungsten.policy.rule')"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.no')"
          :loading="deleteLoading"
          @confirm="deleteRule(record)"
        >
          <tooltip-button
            :tooltip="$t('label.delete.rule')"
            type="danger"
            icon="delete" />
        </a-popconfirm>
      </template>
    </a-table>

    <div style="display: block; text-align: right; margin-top: 10px;">
      <a-pagination
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="pageSizeOptions"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>

    <a-modal
      v-if="showAction"
      :visible="showAction"
      :title="$t('label.add.rule')"
      :maskClosable="false"
      :footer="null"
      @cancel="showAction = false"
      v-ctrl-enter="handleSubmit">
      <a-form :form="form" layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.action')" :tooltip="apiParams.action.description"/>
          <a-select
            :auto-focus="true"
            v-decorator="['action', {
              initialValue: 'pass',
              rules: [{ required: true, message: $t('message.error.select') }]
            }]"
            :placeholder="apiParams.action.description">
            <a-select-option value="pass">PASS</a-select-option>
            <a-select-option value="deny">DENY</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.direction')" :tooltip="apiParams.direction.description"/>
          <a-select
            v-decorator="['direction', {
              initialValue: 'oneway',
              rules: [{ required: true, message: $t('message.error.select') }]
            }]"
            :placeholder="apiParams.direction.description">
            <a-select-option value="oneway">ONE WAY</a-select-option>
            <a-select-option value="twoway">TWO WAY</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.protocol')" :tooltip="apiParams.protocol.description"/>
          <a-select
            v-decorator="['protocol', {
              initialValue: 'tcp',
              rules: [{ required: true, message: $t('message.error.select') }]
            }]"
            :placeholder="apiParams.protocol.description">
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
            <a-select-option value="icmp">ICMP</a-select-option>
            <a-select-option value="any">ANY</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.srcnetwork')" :tooltip="apiParams.srcnetwork.description"/>
          <a-input
            v-decorator="['srcnetwork', {
              initialValue: 'any',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.srcnetwork.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.srcipprefix')" :tooltip="apiParams.srcnetwork.description"/>
          <a-input
            v-decorator="['srcipprefix', {
              initialValue: '0.0.0.0',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.srcipprefix.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.srcipprefixlen')" :tooltip="apiParams.srcipprefixlen.description"/>
          <a-input
            v-decorator="['srcipprefixlen', {
              initialValue: '0',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.srcipprefixlen.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.srcstartport')" :tooltip="apiParams.srcstartport.description"/>
          <a-input
            v-decorator="['srcstartport', {
              initialValue: '-1',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.srcstartport.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.srcendport')" :tooltip="apiParams.srcendport.description"/>
          <a-input
            v-decorator="['srcendport', {
              initialValue: '-1',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.srcendport.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.destnetwork')" :tooltip="apiParams.destnetwork.description"/>
          <a-input
            v-decorator="['destnetwork', {
              initialValue: 'any',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.destnetwork.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.destipprefix')" :tooltip="apiParams.destipprefix.description"/>
          <a-input
            v-decorator="['destipprefix', {
              initialValue: '0.0.0.0',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.destipprefix.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.destipprefixlen')" :tooltip="apiParams.destipprefixlen.description"/>
          <a-input
            v-decorator="['destipprefixlen', {
              initialValue: '0',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.destipprefixlen.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.deststartport')" :tooltip="apiParams.deststartport.description"/>
          <a-input
            v-decorator="['deststartport', {
              initialValue: '-1',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.deststartport.description"/>
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.destendport')" :tooltip="apiParams.destendport.description"/>
          <a-input
            v-decorator="['destendport', {
              initialValue: '-1',
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.destendport.description"/>
        </a-form-item>
      </a-form>

      <div :span="24" class="action-button">
        <a-button :loading="actionLoading" @click="() => { showAction = false }">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="actionLoading" type="primary" ref="submit" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TungstenFabricPolicyRule',
  components: {
    TooltipButton,
    TooltipLabel
  },
  mixins: [mixinDevice],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      showAction: false,
      zoneId: null,
      fetchLoading: false,
      actionLoading: false,
      deleteLoading: false,
      dataSource: [],
      itemCount: 0,
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      columns: [
        {
          title: this.$t('label.action'),
          dataIndex: 'action',
          scopedSlots: { customRender: 'action' }
        },
        {
          title: this.$t('label.direction'),
          dataIndex: 'direction',
          scopedSlots: { customRender: 'direction' }
        },
        {
          title: this.$t('label.protocol'),
          dataIndex: 'protocol',
          scopedSlots: { customRender: 'protocol' }
        },
        {
          title: this.$t('label.srcnetwork'),
          dataIndex: 'srcnetwork',
          scopedSlots: { customRender: 'srcnetwork' }
        },
        {
          title: this.$t('label.sourceport'),
          dataIndex: 'sourceport',
          scopedSlots: { customRender: 'sourceport' }
        },
        {
          title: this.$t('label.destnetwork'),
          dataIndex: 'destnetwork',
          scopedSlots: { customRender: 'destnetwork' }
        },
        {
          title: this.$t('label.destport'),
          dataIndex: 'destport',
          scopedSlots: { customRender: 'destport' }
        },
        {
          dataIndex: 'ruleAction',
          scopedSlots: { customRender: 'ruleAction' },
          width: 50
        }
      ]
    }
  },
  computed: {
    pageSizeOptions () {
      const sizes = [20, 50, 100, 200, this.$store.getters.defaultListViewPageSize]
      if (this.device !== 'desktop') {
        sizes.unshift(10)
      }
      return [...new Set(sizes)].sort(function (a, b) {
        return a - b
      }).map(String)
    }
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('addTungstenFabricPolicyRule')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) {
        return
      }
      this.zoneId = this.$route.query.zoneid

      const params = {}
      params.zoneid = this.zoneId
      params.policyuuid = this.resource.uuid

      this.dataSource = []
      this.fetchLoading = true
      api('listTungstenFabricPolicyRule', params).then(json => {
        this.dataSource = json?.listtungstenfabricpolicyruleresponse?.rule || []
        this.itemCount = json?.listtungstenfabricpolicyruleresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.fetchLoading = false })
    },
    onShowAction () {
      this.showAction = true
    },
    handleSubmit () {
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

        const params = {}
        params.zoneid = this.zoneId
        params.policyuuid = this.resource.uuid
        params.action = values.action
        params.direction = values.direction
        params.protocol = values.protocol
        params.srcnetwork = values.srcnetwork
        params.srcipprefix = values.srcipprefix
        params.srcipprefixlen = values.srcipprefixlen
        params.srcstartport = values.srcstartport
        params.srcendport = values.srcendport
        params.destnetwork = values.destnetwork
        params.destipprefix = values.destipprefix
        params.destipprefixlen = values.destipprefixlen
        params.deststartport = values.deststartport
        params.destendport = values.destendport

        this.actionLoading = true
        api('addTungstenFabricPolicyRule', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricpolicyruleresponse.jobid,
            title: this.$t('label.add.rule'),
            successMessage: `${this.$t('message.success.add.policy.rule')}`,
            successMethod: () => {
              this.fetchData()
            },
            errorMessage: this.$t('message.error.add.policy.rule'),
            loadingMessage: this.$t('message.loading.add.policy.rule'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
            },
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.showAction = false
          this.actionLoading = false
        })
      })
    },
    deleteRule (record) {
      const params = {}
      params.zoneid = this.zoneId
      params.policyuuid = this.resource.uuid
      params.ruleuuid = record.uuid

      this.deleteLoading = true
      api('removeTungstenFabricPolicyRule', params).then(json => {
        this.$pollJob({
          jobId: json.removetungstenfabricpolicyruleresponse.jobid,
          title: this.$t('label.delete.rule'),
          description: record.uuid,
          successMessage: `${this.$t('message.success.delete.tungsten.policy.rule')} ${record.uuid}`,
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.error.delete.tungsten.policy.rule'),
          loadingMessage: this.$t('message.loading.delete.tungsten.policy.rule'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
          },
          action: {
            isFetchData: false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.deleteLoading = false })
    },
    onChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    onChangePageSize (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style scoped>
</style>
