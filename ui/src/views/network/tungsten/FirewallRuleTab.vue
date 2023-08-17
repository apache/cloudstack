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
    <a-spin :spinning="loading">
      <a-button
        :disabled="!('createTungstenFabricFirewallRule' in $store.getters.apis)"
        type="dashed"
        style="width: 100%; margin-bottom: 15px"
        @click="addFirewallRule">
        <template #icon><plus-outlined /></template>
        {{ $t('label.add.firewallrule') }}
      </a-button>
      <a-table
        size="small"
        style="overflow-y: auto"
        :loading="fetchLoading"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="item => item.uuid"
        :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-popconfirm
              :title="$t('message.confirm.remove.firewall.rule')"
              @confirm="removeFirewallRule(record.uuid)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.remove.firewall.rule')"
                danger
                type="primary"
                icon="delete-outlined"
                :loading="deleteLoading" />
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <div style="display: block; text-align: right; margin-top: 10px;">
        <a-pagination
          class="row-element pagination"
          size="small"
          :current="page"
          :pageSize="pageSize"
          :total="totalCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="changePage"
          @showSizeChange="changePageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>
      </div>
    </a-spin>

    <a-modal
      v-if="firewallRuleModal"
      :visible="firewallRuleModal"
      :title="$t('label.add.tungsten.firewall.policy')"
      :closable="true"
      :footer="null"
      @cancel="closeAction"
      centered
      width="450px">
      <div v-ctrl-enter="submitFirewallRule">
        <a-form :ref="formRef" :model="form" :rules="rules" layout="vertical">
          <a-form-item name="name" ref="name">
            <template #label>
              <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
            </template>
            <a-input
              v-focus="true"
              v-model:value="form.name"
              :placeholder="apiParams.name.description"/>
          </a-form-item>
          <a-form-item name="action" ref="action">
            <template #label>
              <tooltip-label :title="$t('label.action')" :tooltip="apiParams.action.description"/>
            </template>
            <a-select
              v-model:value="form.action"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.action.description">
              <a-select-option value="pass">PASS</a-select-option>
              <a-select-option value="deny">DENY</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="servicegroupuuid" ref="servicegroupuuid">
            <template #label>
              <tooltip-label :title="$t('label.servicegroupuuid')" :tooltip="apiParams.servicegroupuuid.description"/>
            </template>
            <a-select
              v-model:value="form.servicegroupuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="serviceGroup.loading"
              :placeholder="apiParams.servicegroupuuid.description">
              <a-select-option
                v-for="group in serviceGroup.opts"
                :key="group.uuid"
                :label="group.name || group.description || group.displaytext">
                {{ group.name || group.description || group.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="sourcetype" ref="sourcetype">
            <template #label>
              <tooltip-label :title="$t('label.sourcetype')" :tooltip="$t('label.sourcetype')"/>
            </template>
            <a-select v-model:value="form.sourcetype">
              <a-select-option value="srctag">{{ $t('label.tag') }}</a-select-option>
              <a-select-option value="srcaddress">{{ $t('label.group') }}</a-select-option>
              <a-select-option value="srcnetwork">{{ $t('label.network') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="srctaguuid" ref="srctaguuid" v-if="form.sourcetype === 'srctag'">
            <template #label>
              <tooltip-label :title="$t('label.srctaguuid')" :tooltip="apiParams.srctaguuid.description"/>
            </template>
            <a-select
              v-model:value="form.srctaguuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="srcTag.loading"
              :placeholder="apiParams.srctaguuid.description">
              <a-select-option
                v-for="tag in srcTag.opts"
                :key="tag.uuid"
                :label="tag.name || tag.description || tag.displaytext">
                {{ tag.name || tag.description || tag.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="srcaddressgroupuuid" ref="srcaddressgroupuuid" v-if="form.sourcetype === 'srcaddress'">
            <template #label>
              <tooltip-label :title="$t('label.srcaddressgroupuuid')" :tooltip="apiParams.srcaddressgroupuuid.description"/>
            </template>
            <a-select
              v-model:value="form.srcaddressgroupuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="srcAddress.loading"
              :placeholder="apiParams.srcaddressgroupuuid.description">
              <a-select-option
                v-for="address in srcAddress.opts"
                :key="address.uuid"
                :label="address.name || address.description || address.displaytext">
                {{ address.name || address.description || address.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="srcnetworkuuid" ref="srcnetworkuuid" v-if="form.sourcetype === 'srcnetwork'">
            <template #label>
              <tooltip-label :title="$t('label.srcnetworkuuid')" :tooltip="apiParams.srcnetworkuuid.description"/>
            </template>
            <a-select
              v-model:value="form.srcnetworkuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networks.loading"
              :placeholder="apiParams.srcnetworkuuid.description">
              <a-select-option v-for="network in networks.opts" :key="network.uuid" :label="network.name || network.description || network.displaytext">
                {{ network.name || network.description || network.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="direction" ref="direction">
            <template #label>
              <tooltip-label :title="$t('label.direction')" :tooltip="apiParams.direction.description"/>
            </template>
            <a-select
              v-model:value="form.direction"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.direction.description">
              <a-select-option value="oneway">ONE WAY</a-select-option>
              <a-select-option value="twoway">TWO WAY</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="sourcetype" ref="sourcetype">
            <template #label>
              <tooltip-label :title="$t('label.destinationtype')" :tooltip="$t('label.destinationtype')"/>
            </template>
            <a-select v-model:value="form.destinationtype">
              <a-select-option value="destag">{{ $t('label.tag') }}</a-select-option>
              <a-select-option value="desaddress">{{ $t('label.group') }}</a-select-option>
              <a-select-option value="desnetwork">{{ $t('label.network') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="desttaguuid" ref="desttaguuid" v-if="form.destinationtype === 'destag'">
            <template #label>
              <tooltip-label :title="$t('label.desttaguuid')" :tooltip="apiParams.desttaguuid.description"/>
            </template>
            <a-select
              v-model:value="form.desttaguuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="srcTag.loading"
              :placeholder="apiParams.desttaguuid.description">
              <a-select-option v-for="tag in srcTag.opts" :key="tag.uuid" :label="tag.name || tag.description || tag.displaytext">
                {{ tag.name || tag.description || tag.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="destaddressgroupuuid" ref="destaddressgroupuuid" v-if="form.destinationtype === 'desaddress'">
            <template #label>
              <tooltip-label :title="$t('label.destaddressgroupuuid')" :tooltip="apiParams.destaddressgroupuuid.description"/>
            </template>
            <a-select
              v-model:value="form.destaddressgroupuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="srcAddress.loading"
              :placeholder="apiParams.destaddressgroupuuid.description">
              <a-select-option v-for="address in srcAddress.opts" :key="address.uuid" :label="address.name || address.description || address.displaytext">
                {{ address.name || address.description || address.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="destnetworkuuid" ref="destnetworkuuid" v-if="form.destinationtype === 'desnetwork'">
            <template #label>
              <tooltip-label :title="$t('label.destnetworkuuid')" :tooltip="apiParams.destnetworkuuid.description"/>
            </template>
            <a-select
              v-model:value="form.destnetworkuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="networks.loading"
              :placeholder="apiParams.destnetworkuuid.description">
              <a-select-option v-for="network in networks.opts" :key="network.uuid" :label="network.name || network.description || network.displaytext">
                {{ network.name || network.description || network.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="tagtypeuuid" ref="tagtypeuuid">
            <template #label>
              <tooltip-label :title="$t('label.tagtypeuuid')" :tooltip="apiParams.tagtypeuuid.description"/>
            </template>
            <a-select
              v-model:value="form.tagtypeuuid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="tagType.loading"
              :placeholder="apiParams.tagtypeuuid.description">
              <a-select-option v-for="tag in tagType.opts" :key="tag.uuid" :label="tag.name || tag.description || tag.displaytext">
                {{ tag.name || tag.description || tag.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="sequence" ref="sequence">
            <template #label>
              <tooltip-label :title="$t('label.sequence')" :tooltip="apiParams.sequence.description"/>
            </template>
            <a-input
              v-focus="true"
              v-model:value="form.sequence"
              :placeholder="apiParams.sequence.description"/>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button ref="submit" type="primary" @click="submitFirewallRule" :loading="addLoading">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'FirewallRuleTab',
  components: {
    TooltipLabel,
    TooltipButton
  },
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
      zoneId: undefined,
      fetchLoading: false,
      firewallRuleModal: false,
      addLoading: false,
      deleteLoading: false,
      columns: [{
        title: this.$t('label.name'),
        dataIndex: 'name'
      }, {
        title: this.$t('label.action'),
        dataIndex: 'action'
      }, {
        title: this.$t('label.servicegroupuuid'),
        dataIndex: 'servicegroup'
      }, {
        title: this.$t('label.srcaddressgroupuuid'),
        dataIndex: 'srctag'
      }, {
        title: this.$t('label.direction'),
        dataIndex: 'direction'
      }, {
        title: this.$t('label.destaddressgroupuuid'),
        dataIndex: 'desttag'
      }, {
        title: this.$t('label.tagtypeuuid'),
        dataIndex: 'tagtype'
      }, {
        title: this.$t('label.actions'),
        key: 'actions',
        width: 80
      }],
      dataSource: [],
      page: 1,
      pageSize: this.$store.getters.defaultListViewPageSize,
      totalCount: 0,
      serviceGroup: {
        loading: false,
        opts: []
      },
      srcTag: {
        loading: false,
        opts: []
      },
      srcAddress: {
        loading: false,
        opts: []
      },
      networks: {
        loading: false,
        opts: []
      },
      tagType: {
        loading: false,
        opts: []
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createTungstenFabricFirewallRule')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  watch: {
    resource () {
      this.fetchData()
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        action: 'pass',
        direction: 'oneway',
        sequence: 1,
        sourcetype: 'srctag',
        destinationtype: 'destag'
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        action: [{ required: true, message: this.$t('message.error.select') }],
        servicegroupuuid: [{ required: true, message: this.$t('message.error.select') }],
        srctaguuid: [{ required: true, message: this.$t('message.error.select') }],
        srcaddressgroupuuid: [{ required: true, message: this.$t('message.error.select') }],
        srcnetworkuuid: [{ required: true, message: this.$t('message.error.select') }],
        direction: [{ required: true, message: this.$t('message.error.select') }],
        desttaguuid: [{ required: true, message: this.$t('message.error.select') }],
        destaddressgroupuuid: [{ required: true, message: this.$t('message.error.select') }],
        destnetworkuuid: [{ required: true, message: this.$t('message.error.select') }],
        sequence: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      if (!this.resource.uuid || !('zoneid' in this.$route.query)) {
        return
      }
      this.zoneId = this.$route.query.zoneid
      this.fetchLoading = true
      this.dataSource = []
      this.totalCount = 0
      api('listTungstenFabricFirewallRule', {
        zoneid: this.zoneId,
        firewallpolicyuuid: this.resource.uuid,
        page: this.page,
        pagesize: this.pageSize,
        listAll: true
      }).then(json => {
        this.dataSource = json?.listtungstenfabricfirewallruleresponse?.firewallrule || []
        this.totalCount = json?.listtungstenfabricfirewallruleresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    addFirewallRule () {
      this.firewallRuleModal = true
      this.fetchServiceGroup()
      this.fetchTag()
      this.fetchAddressGroup()
      this.fetchNetworks()
      this.fetchTagType()
    },
    fetchServiceGroup () {
      this.serviceGroup.loading = true
      this.serviceGroup.opts = []
      api('listTungstenFabricServiceGroup', { zoneid: this.zoneId }).then(json => {
        this.serviceGroup.opts = json?.listtungstenfabricservicegroupresponse?.servicegroup || []
      }).finally(() => {
        this.serviceGroup.loading = false
      })
    },
    fetchTag () {
      this.srcTag.loading = true
      this.srcTag.opts = []
      api('listTungstenFabricTag', { zoneid: this.zoneId }).then(json => {
        this.srcTag.opts = json?.listtungstenfabrictagresponse?.tag || []
      }).finally(() => {
        this.srcTag.loading = false
      })
    },
    fetchAddressGroup () {
      this.srcAddress.loading = true
      this.srcAddress.opts = []
      api('listTungstenFabricAddressGroup', { zoneid: this.zoneId }).then(json => {
        this.srcAddress.opts = json?.listtungstenfabricaddressgroupresponse?.addressgroup || []
      }).finally(() => {
        this.srcAddress.loading = false
      })
    },
    fetchNetworks () {
      this.networks.loading = true
      this.networks.opts = []
      api('listTungstenFabricNetwork', { zoneid: this.zoneId, listAll: true }).then(json => {
        this.networks.opts = json?.listtungstenfabricnetworkresponse?.network || []
      }).finally(() => {
        this.networks.loading = false
      })
    },
    fetchTagType () {
      this.tagType.loading = true
      this.tagType.opts = []
      api('listTungstenFabricTagType', { zoneid: this.zoneId }).then(json => {
        this.tagType.opts = json?.listtungstenfabrictagtyperesponse?.tagtype || []
      }).finally(() => {
        this.tagType.loading = false
      })
    },
    submitFirewallRule () {
      if (this.addLoading) return
      this.addLoading = true
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.firewallpolicyuuid = this.resource.uuid
        params.zoneid = this.zoneId
        params.name = values.name
        params.action = values.action
        params.servicegroupuuid = values.servicegroupuuid
        params.direction = values.direction
        params.sequence = values.sequence
        if (values.sourcetype === 'srctag') {
          params.srctaguuid = values.srctaguuid
        } else if (values.sourcetype === 'srcaddress') {
          params.srcaddressgroupuuid = values.srcaddressgroupuuid
        } else if (values.sourcetype === 'srcnetwork') {
          params.srcnetworkuuid = values.srcnetworkuuid
        }
        if (values.destinationtype === 'destag') {
          params.desttaguuid = values.desttaguuid
        } else if (values.destinationtype === 'desaddress') {
          params.destaddressgroupuuid = values.destaddressgroupuuid
        } else if (values.destinationtype === 'desnetwork') {
          params.destnetworkuuid = values.destnetworkuuid
        }
        params.tagtypeuuid = values.tagtypeuuid

        api('createTungstenFabricFirewallRule', params).then(json => {
          this.$pollJob({
            jobId: json.createtungstenfabricfirewallruleresponse.jobid,
            title: this.$t('label.add.firewallrule'),
            description: params.name,
            successMessage: this.$t('message.success.add.firewallrule'),
            successMethod: () => {
              this.fetchData()
              this.addLoading = false
              this.firewallRuleModal = false
            },
            errorMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            errorMessage: this.$t('message.add.firewallrule.failed'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.addLoading = false
            },
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.addLoading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.addLoading = false
      })
    },
    removeFirewallRule (uuid) {
      if (this.deleteLoading) return
      this.deleteLoading = true
      const params = {}
      params.zoneid = this.zoneId
      params.firewallruleuuid = uuid

      api('deleteTungstenFabricFirewallRule', params).then(json => {
        this.$pollJob({
          jobId: json.deletetungstenfabricfirewallruleresponse.jobid,
          title: this.$t('label.delete.tungsten.firewall.policy'),
          description: params.firewallpolicyuuid,
          successMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          errorMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          loadingMessage: this.$t('message.deleting.firewall.policy'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.deleteLoading = false
          },
          action: {
            isFetchData: false
          }
        })
      }).finally(() => {
        this.deleteLoading = false
      })
    },
    closeAction () {
      this.firewallRuleModal = false
      this.formRef.value.resetFields()
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>
