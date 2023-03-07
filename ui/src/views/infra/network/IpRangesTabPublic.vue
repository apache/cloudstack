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
  <a-spin :spinning="componentLoading">
    <a-button
      :disabled="!('createVlanIpRange' in $store.getters.apis)"
      type="dashed"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpRangeModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.ip.range') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'gateway'">
          {{ record.gateway || record.ip6gateway }}
        </template>
        <template v-if="column.key === 'cidr'">
          {{ record.cidr || record.ip6cidr }}
        </template>
        <template v-if="column.key === 'startip'">
          {{ record.startip || record.startipv6 }}
        </template>
        <template v-if="column.key === 'endip'">
          {{ record.endip || record.endipv6 }}
        </template>
        <template v-if="column.key === 'account' && !basicGuestNetwork">
          <a-button @click="() => handleOpenAccountModal(record)">{{ `[${record.domain}] ${record.account === undefined ? '' : record.account}` }}</a-button>
        </template>
        <template v-if="column.key === 'actions'">
          <div
            class="actions"
            style="text-align: right" >
            <tooltip-button
              v-if="record.account === 'system' && !basicGuestNetwork && record.gateway && !record.ip6gateway"
              tooltipPlacement="bottom"
              :tooltip="$t('label.add.account')"
              icon="user-add-outlined"
              @onClick="() => handleOpenAddAccountModal(record)"
              :disabled="!('dedicatePublicIpRange' in $store.getters.apis)" />
            <tooltip-button
              v-if="record.account !== 'system' && !basicGuestNetwork"
              tooltipPlacement="bottom"
              :tooltip="$t('label.release.account')"
              icon="user-delete-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleRemoveAccount(record.id)"
              :disabled="!('releasePublicIpRange' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.update.ip.range')"
              icon="edit-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleUpdateIpRangeModal(record)"
              :disabled="!('updateVlanIpRange' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.remove.ip.range')"
              icon="delete-outlined"
              type="primary"
              :danger="true"
              @onClick="handleDeleteIpRange(record.id)"
              :disabled="!('deleteVlanIpRange' in $store.getters.apis)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="total"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="accountModal"
      v-if="selectedItem"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="accountModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}</div>
          <div>{{ selectedItem.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.domain') }}</div>
          <div>{{ selectedItem.domain }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.system.vms') }}</div>
          <div>{{ selectedItem.forsystemvms }}</div>
        </div>
      </div>

      <div :span="24" class="action-button">
        <a-button @click="accountModal = false">{{ $t('label.close') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      v-if="addAccountModal"
      :zIndex="1001"
      :closable="true"
      :maskClosable="false"
      :visible="addAccountModal"
      :title="$t('label.add.account')"
      :footer="null"
      @cancel="addAccountModal = false">
      <a-spin :spinning="domainsLoading" v-ctrl-enter="handleAddAccount">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}:</div>
          <a-input v-model:value="addAccount.account" v-focus="true"></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('label.domain') }}:</div>
          <a-select
            v-model:value="addAccount.domain"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="domain in domains"
              :key="domain.id"
              :value="domain.id"
              :label="domain.path || domain.name || domain.description">{{ domain.path || domain.name || domain.description }}
            </a-select-option>
          </a-select>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="addAccountModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddAccount">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
      v-if="addIpRangeModal"
      :visible="addIpRangeModal"
      :title="$t('label.add.ip.range')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="addIpRangeModal = false">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleAddIpRange"
        v-ctrl-enter="handleAddIpRange"
        layout="vertical"
        class="form"

      >
        <a-form-item name="iptype" ref="iptype" :label="$t('label.ip.range.type')" class="form__item">
          <a-radio-group
            v-model:value="form.iptype"
            buttonStyle="solid">
            <a-radio-button value="">
              {{ $t('label.ip.v4') }}
            </a-radio-button>
            <a-radio-button value="ip6">
              {{ $t('label.ip.v6') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="podid" ref="podid" :label="$t('label.podid')" class="form__item" v-if="basicGuestNetwork">
          <a-select
            v-model:value="form.podid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true">
            <a-select-option v-for="pod in pods" :key="pod.id" :value="pod.id" :label="pod.name">{{ pod.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="vlan" ref="vlan" :label="$t('label.vlan')" class="form__item" v-if="!basicGuestNetwork">
          <a-input v-model:value="form.vlan" />
        </a-form-item>
        <div v-if="form.iptype==='ip6'">
          <a-form-item name="ip6gateway" ref="ip6gateway" :label="$t('label.gateway')" class="form__item">
            <a-input v-model:value="form.ip6gateway" />
          </a-form-item>
          <a-form-item name="ip6cidr" ref="ip6cidr" :label="$t('label.cidr')" class="form__item">
            <a-input v-model:value="form.ip6cidr" />
          </a-form-item>
        </div>
        <div v-else>
          <a-form-item name="gateway" ref="gateway" :label="$t('label.gateway')" class="form__item">
            <a-input v-model:value="form.gateway" />
          </a-form-item>
          <a-form-item name="netmask" ref="netmask" :label="$t('label.netmask')" class="form__item">
            <a-input v-model:value="form.netmask" />
          </a-form-item>
          <a-form-item name="startip" ref="startip" :label="$t('label.startip')" class="form__item">
            <a-input v-model:value="form.startip" />
          </a-form-item>
          <a-form-item name="endip" ref="endip" :label="$t('label.endip')" class="form__item">
            <a-input v-model:value="form.endip" />
          </a-form-item>
        </div>
        <div class="form__item" v-if="!basicGuestNetwork && form.iptype != 'ip6'">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch v-model:checked="showAccountFields" @change="handleShowAccountFields" />
        </div>
        <div v-if="showAccountFields && !basicGuestNetwork" style="margin-top: 20px;">
          <div v-html="$t('label.set.reservation.desc')"></div>
          <a-form-item name="forsystemvms" ref="forsystemvms" :label="$t('label.system.vms')" class="form__item">
            <a-switch v-model:checked="form.forsystemvms" />
          </a-form-item>
          <a-spin :spinning="domainsLoading">
            <a-form-item name="account" ref="account" :label="$t('label.account')" class="form__item">
              <a-input v-model:value="form.account"></a-input>
            </a-form-item>
            <a-form-item name="domain" ref="domain" :label="$t('label.domain')" class="form__item">
              <a-select
                v-model:value="form.domain"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :value="domain.id"
                  :label="domain.path || domain.name || domain.description">{{ domain.path || domain.name || domain.description }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-spin>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="addIpRangeModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddIpRange">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :visible="updateIpRangeModal"
      :title="$t('label.update.ip.range')"
      v-if="selectedItem"
      :maskClosable="false"
      :footer="null"
      @cancel="updateIpRangeModal = false">
      <a-form
        :ref="updRangeRef"
        :model="formUpdRange"
        :rules="updRangeRules"
        @finish="handleAddIpRange"
        v-ctrl-enter="handleAddIpRange"
        layout="vertical"
        class="form"

      >
        <div v-if="selectedItem.ip6gateway && !selectedItem.gateway">
          <a-form-item name="ip6gateway" ref="ip6gateway" :label="$t('label.gateway')" class="form__item">
            <a-input v-model:value="formUpdRange.ip6gateway" />
          </a-form-item>
          <a-form-item name="ip6cidr" ref="ip6cidr" :label="$t('label.cidr')" class="form__item">
            <a-input v-model:value="formUpdRange.ip6cidr" />
          </a-form-item>
        </div>
        <div v-else>
          <a-form-item name="startip" ref="startip" :label="$t('label.startip')" class="form__item">
            <a-input v-focus="true" v-model:value="formUpdRange.startip"></a-input>
          </a-form-item>
          <a-form-item name="endip" ref="endip" :label="$t('label.endip')" class="form__item">
            <a-input v-model:value="formUpdRange.endip"></a-input>
          </a-form-item>
          <a-form-item name="gateway" ref="gateway" :label="$t('label.gateway')" class="form__item">
            <a-input v-model:value="formUpdRange.gateway"></a-input>
          </a-form-item>
          <a-form-item name="netmask" ref="netmask" :label="$t('label.netmask')" class="form__item">
            <a-input v-model:value="formUpdRange.netmask"></a-input>
          </a-form-item>
          <a-form-item name="forsystemvms" ref="forsystemvms" :label="$t('label.system.vms')" class="form__item">
            <a-switch v-model:checked="formUpdRange.forsystemvms"></a-switch>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="updateIpRangeModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleUpdateIpRange">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'IpRangesTabPublic',
  components: {
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
    },
    network: {
      type: Object,
      required: true
    },
    basicGuestNetwork: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      componentLoading: false,
      items: [],
      total: 0,
      selectedItem: null,
      accountModal: false,
      addAccountModal: false,
      addAccount: {
        account: null,
        domain: null
      },
      domains: [],
      domainsLoading: false,
      addIpRangeModal: false,
      updateIpRangeModal: false,
      showAccountFields: false,
      podsLoading: false,
      pods: [],
      page: 1,
      pageSize: 10,
      columns: [
        {
          key: 'gateway',
          title: this.$t('label.gateway')
        },
        {
          key: 'cidr',
          title: this.$t('label.cidr')
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan'
        },
        {
          key: 'startip',
          title: this.$t('label.startip')
        },
        {
          key: 'endip',
          title: this.$t('label.endip')
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ]
    }
  },
  created () {
    this.initFormUpdateRange()
    this.initAddIpRangeForm()
    if (!this.basicGuestNetwork) {
      this.columns.splice(6, 0,
        {
          key: 'account',
          title: this.$t('label.account')
        }
      )
    } else {
      this.columns.unshift({
        title: this.$t('label.pod'),
        dataIndex: 'podname'
      })
    }
    this.fetchData()
  },
  watch: {
    network (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    initAddIpRangeForm () {
      this.formRef = ref()
      this.form = reactive({
        iptype: ''
      })
      this.rules = reactive({
        podid: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        startip: [{ required: true, message: this.$t('label.required') }],
        endip: [{ required: true, message: this.$t('label.required') }],
        ip6gateway: [{ required: true, message: this.$t('label.required') }],
        ip6cidr: [{ required: true, message: this.$t('label.required') }]
      })
    },
    initFormUpdateRange () {
      this.updRangeRef = ref()
      this.formUpdRange = reactive({})
      this.updRangeRules = reactive({
        startip: [{ required: true, message: this.$t('label.required') }],
        endip: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        ip6gateway: [{ required: true, message: this.$t('label.required') }],
        ip6cidr: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.componentLoading = true
      api('listVlanIpRanges', {
        networkid: this.network.id,
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = response.listvlaniprangesresponse.vlaniprange ? response.listvlaniprangesresponse.vlaniprange : []
        this.total = response.listvlaniprangesresponse.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
      this.fetchPods()
    },
    fetchDomains () {
      this.domainsLoading = true
      api('listDomains', {
        details: 'min',
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain ? response.listdomainsresponse.domain : []
        if (this.domains.length > 0) {
          this.addAccount.domain = this.domains[0].id
          this.form.domain = this.domains[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchPods () {
      this.podsLoading = true
      api('listPods', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.total = response.listpodsresponse.count || 0
        this.pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      }).finally(() => {
        this.podsLoading = false
      })
    },
    handleAddAccount () {
      if (this.domainsLoading) return
      this.domainsLoading = true

      if (this.addIpRangeModal === true) {
        this.addAccountModal = false
        return
      }

      var params = {
        id: this.selectedItem.id,
        zoneid: this.selectedItem.zoneid,
        domainid: this.addAccount.domain
      }

      if (this.addAccount.account) {
        params.account = this.addAccount.account
      }

      api('dedicatePublicIpRange', params).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addAccountModal = false
        this.domainsLoading = false
        this.fetchData()
      })
    },
    handleRemoveAccount (id) {
      this.componentLoading = true
      api('releasePublicIpRange', { id }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchData()
      })
    },
    handleOpenAccountModal (item) {
      this.selectedItem = item
      this.accountModal = true
    },
    handleOpenAddAccountModal (item) {
      if (!this.addIpRangeModal) {
        this.selectedItem = item
      }
      this.addAccountModal = true
      this.fetchDomains()
    },
    handleShowAccountFields () {
      if (this.showAccountFields) {
        this.fetchDomains()
      }
    },
    handleOpenAddIpRangeModal () {
      this.initAddIpRangeForm()
      this.addIpRangeModal = true
    },
    handleUpdateIpRangeModal (item) {
      this.initFormUpdateRange()
      this.selectedItem = item
      this.updateIpRangeModal = true

      this.formUpdRange.startip = this.selectedItem?.startip || ''
      this.formUpdRange.endip = this.selectedItem?.endip || ''
      this.formUpdRange.gateway = this.selectedItem?.gateway || ''
      this.formUpdRange.netmask = this.selectedItem?.netmask || ''
      this.formUpdRange.forsystemvms = this.selectedItem?.forsystemvms || false
      this.formUpdRange.ip6gateway = this.selectedItem?.ip6gateway || ''
      this.formUpdRange.ip6cidr = this.selectedItem?.ip6cidr || ''
    },
    handleDeleteIpRange (id) {
      this.componentLoading = true
      api('deleteVlanIpRange', { id }).then(() => {
        this.$notification.success({
          message: 'Removed IP Range'
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
        this.fetchData()
      })
    },
    handleAddIpRange (e) {
      if (this.componentLoading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.componentLoading = true
        this.addIpRangeModal = false
        var ipRangeKeys = ['gateway', 'netmask', 'startip', 'endip']
        if (values.iptype === 'ip6') {
          ipRangeKeys = ['ip6gateway', 'ip6cidr']
        }
        var params = {}
        for (const key of ipRangeKeys) {
          params[key] = values[key]
        }
        if (!this.basicGuestNetwork) {
          params.zoneId = this.resource.zoneid
          params.vlan = values.vlan
          params.forsystemvms = values.forsystemvms
          params.account = values.forsystemvms ? null : values.account
          params.domainid = values.forsystemvms ? null : values.domain
          params.forvirtualnetwork = true
        } else {
          params.forvirtualnetwork = false
          params.podid = values.podid
          params.networkid = this.network.id
        }
        api('createVlanIpRange', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.add.iprange')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createvlaniprangeresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleUpdateIpRange (e) {
      if (this.componentLoading) return
      this.updRangeRef.value.validate().then(() => {
        const values = toRaw(this.formUpdRange)

        this.componentLoading = true
        this.updateIpRangeModal = false
        var params = {
          id: this.selectedItem.id,
          forsystemvms: values.forsystemvms
        }
        var ipRangeKeys = ['gateway', 'netmask', 'startip', 'endip']
        if (this.selectedItem.ip6gateway && !this.selectedItem.gateway) {
          ipRangeKeys = ['ip6gateway', 'ip6cidr']
        }
        for (const key of ipRangeKeys) {
          params[key] = values[key]
        }
        api('updateVlanIpRange', params).then(() => {
          this.$notification.success({
            message: this.$t('message.success.update.iprange')
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.updatevlaniprangeresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      })
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

<style lang="scss" scoped>
  .list {
    &__item {
      display: flex;
    }

    &__data {
      display: flex;
      flex-wrap: wrap;
    }

    &__col {
      flex-basis: calc((100% / 3) - 20px);
      margin-right: 20px;
      margin-bottom: 10px;
    }

    &__label {
      font-weight: bold;
    }
  }

  .ant-list-item {
    padding-top: 0;
    padding-bottom: 0;

    &:not(:first-child) {
      padding-top: 20px;
    }

    &:not(:last-child) {
      padding-bottom: 20px;
    }
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  .ant-select {
    width: 100%;
  }

  .form {
    .actions {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }

    }
  }

  .pagination {
    margin-top: 20px;
  }
</style>
