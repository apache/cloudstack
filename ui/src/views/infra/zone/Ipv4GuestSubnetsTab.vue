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
      :disabled="!('createIpv4SubnetForZone' in $store.getters.apis)"
      type="primary"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpv4SubnetModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.ipv4.subnet') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="ipv4SubnetColumns"
      :dataSource="ipv4Subnets"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'subnet'">
          {{ record.subnet }}
        </template>
        <template v-if="column.key === 'domain'">
          {{ record.domain }}
        </template>
        <template v-if="column.key === 'account'">
          {{ record.account }}
        </template>
        <template v-if="column.key === 'project'">
          {{ record.project }}
        </template>
        <template v-if="column.key === 'actions'">
          <div
            class="actions"
            style="text-align: right" >
            <router-link :to="{ name: 'ipv4subnets', query: { parentid: record.id }}">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.view') + ' ' + $t('label.ipv4.subnets')"
                icon="environment-outlined"/>
            </router-link>
            <tooltip-button
              v-if="!record.domain && record.subnet"
              tooltipPlacement="bottom"
              :tooltip="$t('label.add.account')"
              icon="user-add-outlined"
              @onClick="() => handleOpenAddAccountForIpv4GuestSubnetModal(record)"
              :disabled="!('dedicateIpv4SubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              v-if="record.domain"
              tooltipPlacement="bottom"
              :tooltip="$t('label.release.account')"
              icon="user-delete-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleRemoveAccountFromIpv4GuestSubnet(record.id)"
              :disabled="!('releaseIpv4SubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.update.ipv4.subnet')"
              icon="edit-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleUpdateIpv4SubnetModal(record)"
              :disabled="!('updateIpv4SubnetForZone' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.remove.ipv4.subnet')"
              icon="delete-outlined"
              type="primary"
              :danger="true"
              @onClick="handleDeleteIpv4Subnet(record.id)"
              :disabled="!('deleteIpv4SubnetForZone' in $store.getters.apis)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="ipv4SubnetPage"
      :pageSize="ipv4SubnetPageSize"
      :total="ipv4SubnetsTotal"
      :showTotal="total => `${$t('label.total')} ${ipv4SubnetsTotal} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changeIpv4SubnetPage"
      @showSizeChange="changeIpv4SubnetPageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="accountForIpv4GuestSubnetModal"
      v-if="selectedIpv4GuestSubnet"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="accountForIpv4GuestSubnetModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}</div>
          <div>{{ selectedIpv4GuestSubnet.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.domain') }}</div>
          <div>{{ selectedIpv4GuestSubnet.domain }}</div>
        </div>
      </div>

      <div :span="24" class="action-button">
        <a-button @click="accountForIpv4GuestSubnetModal = false">{{ $t('label.close') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      v-if="addAccountForIpv4GuestSubnetModal"
      :zIndex="1001"
      :closable="true"
      :maskClosable="false"
      :visible="addAccountForIpv4GuestSubnetModal"
      :title="$t('label.add.account')"
      :footer="null"
      @cancel="addAccountForIpv4GuestSubnetModal = false">
      <a-spin :spinning="domainsLoading" v-ctrl-enter="handleAddAccountForIpv4GuestSubnet">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}:</div>
          <a-input v-model:value="addAccountForIpv4GuestSubnet.account" v-focus="true"></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('label.domain') }}:</div>
          <a-select
            v-model:value="addAccountForIpv4GuestSubnet.domain"
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
          <a-button @click="addAccountForIpv4GuestSubnetModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddAccountForIpv4GuestSubnet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
      v-if="addIpv4SubnetModal"
      :visible="addIpv4SubnetModal"
      :title="$t('label.add.ipv4.subnet')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="addIpv4SubnetModal = false">
      <a-form
        :ref="ipv4SubnetFormRef"
        :model="ipv4SubnetForm"
        :rules="ipv4SubnetRules"
        @finish="handleAddIpv4Subnet"
        v-ctrl-enter="handleAddIpv4Subnet"
        layout="vertical"
        class="form"
      >
        <a-form-item name="subnet" ref="subnet" :label="$t('label.subnet')" class="form__item">
          <a-input v-focus="true" v-model:value="ipv4SubnetForm.subnet" />
        </a-form-item>
        <div class="form__item">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch v-model:checked="showAccountForIpv4GuestSubnetFields" @change="handleShowAccountForIpv4GuestSubnetFields" />
        </div>
        <div v-if="showAccountForIpv4GuestSubnetFields" style="margin-top: 20px;">
          <div v-html="$t('label.ipv4.subnet.set.reservation.desc')"></div><br>
          <a-spin :spinning="domainsLoading">
            <a-form-item name="account" ref="account" :label="$t('label.account')" class="form__item">
              <a-input v-model:value="ipv4SubnetForm.account"></a-input>
            </a-form-item>
            <a-form-item name="domain" ref="domain" :label="$t('label.domain')" class="form__item">
              <a-select
                v-model:value="ipv4SubnetForm.domain"
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
          <a-button @click="addIpv4SubnetModal = false; showAccountForIpv4GuestSubnetFields = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddIpv4Subnet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :visible="updateIpv4SubnetModal"
      :title="$t('label.update.ip.range')"
      v-if="selectedIpv4GuestSubnet"
      :maskClosable="false"
      :footer="null"
      @cancel="updateIpv4SubnetModal = false">
      <a-form
        :ref="updateIpv4SubnetRef"
        :model="formUpdateIpv4Subnet"
        :rules="updateIpv4SubnetRules"
        @finish="handleAddIpv4Subnet"
        v-ctrl-enter="handleAddIpv4Subnet"
        layout="vertical"
        class="form"
      >
        <div>
          <a-form-item name="subnet" ref="subnet" :label="$t('label.subnet')" class="form__item">
            <a-input v-focus="true" v-model:value="formUpdateIpv4Subnet.subnet"></a-input>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="updateIpv4SubnetModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleUpdateIpv4Subnet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
    <br>
    <br>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'Ipv4GuestSubnetsTab',
  components: {
    ResourceIcon,
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
      componentLoading: false,
      selectedIpv4GuestSubnet: null,
      ipv4Subnets: [],
      showAccountForIpv4GuestSubnetFields: false,
      accountForIpv4GuestSubnetModal: false,
      addAccountForIpv4GuestSubnetModal: false,
      addAccountForIpv4GuestSubnet: {
        account: null,
        domain: null
      },
      domains: [],
      domainsLoading: false,
      addIpv4SubnetModal: false,
      updateIpv4SubnetModal: false,
      ipv4SubnetPage: 1,
      ipv4SubnetPageSize: 10,
      ipv4SubnetsTotal: 0,
      ipv4SubnetColumns: [
        {
          title: this.$t('label.subnet'),
          dataIndex: 'subnet'
        },
        {
          title: this.$t('label.domain'),
          dataIndex: 'domain'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.project'),
          dataIndex: 'project'
        },
        {
          key: 'actions',
          title: this.$t('label.actions'),
          width: '20%'
        }
      ]
    }
  },
  beforeCreate () {
    this.form = null
    this.formRef = null
    this.rules = null
    this.ipv4SubnetForm = null
    this.ipv4SubnetFormRef = null
    this.ipv4SubnetRules = null
  },
  created () {
    this.initAddIpv4SubnetForm()
    this.initUpdateIpv4SubnetForm()
    this.fetchData()
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
    initAddIpv4SubnetForm () {
      this.ipv4SubnetFormRef = ref()
      this.ipv4SubnetForm = reactive({
      })
      this.ipv4SubnetRules = reactive({
        subnet: [{ required: true, message: this.$t('label.required') }]
      })
    },
    initUpdateIpv4SubnetForm () {
      this.updateIpv4SubnetRef = ref()
      this.formUpdateIpv4Subnet = reactive({})
      this.updateIpv4SubnetRules = reactive({
        subnet: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchDomains () {
      this.domainsLoading = true
      api('listDomains', {
        details: 'min',
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain ? response.listdomainsresponse.domain : []
        if (this.domains.length > 0) {
          this.addAccountForIpv4GuestSubnet.domain = this.domains[0].id
          this.ipv4SubnetForm.domain = this.domains[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchData () {
      this.fetchZoneIpv4Subnet()
    },
    fetchZoneIpv4Subnet () {
      this.componentLoading = true
      api('listIpv4SubnetsForZone', {
        zoneid: this.resource.id,
        projectid: -1,
        showicon: true,
        page: this.ipv4SubnetPage,
        pagesize: this.ipv4SubnetPageSize
      }).then(response => {
        this.ipv4Subnets = response?.listipv4subnetsforzoneresponse?.zoneipv4subnet || []
        this.ipv4SubnetsTotal = response?.listipv4subnetsforzoneresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleAddAccountForIpv4GuestSubnet () {
      if (this.domainsLoading) return
      this.domainsLoading = true

      if (this.addIpv4SubnetModal === true) {
        this.addAccountForIpv4GuestSubnetModal = false
        return
      }

      var params = {
        id: this.selectedIpv4GuestSubnet.id,
        zoneid: this.selectedIpv4GuestSubnet.zoneid,
        domainid: this.addAccountForIpv4GuestSubnet.domain
      }

      if (this.addAccountForIpv4GuestSubnet.account) {
        params.account = this.addAccountForIpv4GuestSubnet.account
      }

      api('dedicateIpv4SubnetForZone', params).then(response => {
        this.$pollJob({
          jobId: response.dedicateipv4subnetforzoneresponse.jobid,
          title: this.$t('label.dedicate.ipv4.subnet'),
          successMessage: this.$t('message.success.dedicate.ipv4.subnet'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          errorMessage: this.$t('error.dedicate.ipv4.subnet.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addAccountForIpv4GuestSubnetModal = false
        this.domainsLoading = false
        this.fetchZoneIpv4Subnet()
      })
    },
    handleRemoveAccountFromIpv4GuestSubnet (id) {
      this.componentLoading = true
      api('releaseIpv4SubnetForZone', { id }).then(response => {
        this.$pollJob({
          jobId: response.releaseipv4subnetforzoneresponse.jobid,
          title: this.$t('label.release.dedicated.ipv4.subnet'),
          successMessage: this.$t('message.success.release.dedicated.ipv4.subnet'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          errorMessage: this.$t('error.release.dedicate.ipv4.subnet'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchZoneIpv4Subnet()
      })
    },
    handleOpenAddAccountForIpv4GuestSubnetModal (item) {
      if (!this.addIpv4SubnetModal) {
        this.selectedIpv4GuestSubnet = item
      }
      this.addAccountForIpv4GuestSubnetModal = true
      this.fetchDomains()
    },
    handleShowAccountForIpv4GuestSubnetFields () {
      if (this.showAccountForIpv4GuestSubnetFields) {
        this.fetchDomains()
      }
    },
    handleOpenAddIpv4SubnetModal () {
      this.initAddIpv4SubnetForm()
      this.addIpv4SubnetModal = true
    },
    handleUpdateIpv4SubnetModal (item) {
      this.initUpdateIpv4SubnetForm()
      this.selectedIpv4GuestSubnet = item
      this.updateIpv4SubnetModal = true
      this.formUpdateIpv4Subnet.subnet = this.selectedIpv4GuestSubnet?.subnet || ''
    },
    handleDeleteIpv4Subnet (id) {
      this.componentLoading = true
      api('deleteIpv4SubnetForZone', { id }).then(response => {
        this.$pollJob({
          jobId: response.deleteipv4subnetforzoneresponse.jobid,
          successMessage: this.$t('message.success.delete.ipv4.subnet'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          errorMessage: this.$t('message.delete.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneIpv4Subnet()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
        this.fetchZoneIpv4Subnet()
      })
    },
    handleAddIpv4Subnet (e) {
      if (this.componentLoading) return
      this.ipv4SubnetFormRef.value.validate().then(() => {
        const values = toRaw(this.ipv4SubnetForm)
        this.componentLoading = true
        this.addIpv4SubnetModal = false
        this.showAccountForIpv4GuestSubnetFields = false
        var params = {
          zoneId: this.resource.id,
          subnet: values.subnet,
          domainid: values.domain,
          account: values.account
        }
        api('createIpv4SubnetForZone', params).then(response => {
          this.$pollJob({
            jobId: response.createipv4subnetforzoneresponse.jobid,
            title: this.$t('label.add.ipv4.subnet'),
            description: values.subnet,
            successMessage: this.$t('message.success.add.ipv4.subnet'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            },
            errorMessage: this.$t('message.add.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createipv4subnetforzoneresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchZoneIpv4Subnet()
        })
      }).catch(error => {
        this.ipv4SubnetFormRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleUpdateIpv4Subnet (e) {
      if (this.componentLoading) return
      this.updateIpv4SubnetRef.value.validate().then(() => {
        const values = toRaw(this.formUpdateIpv4Subnet)

        this.componentLoading = true
        this.updateIpv4SubnetModal = false
        var params = {
          id: this.selectedIpv4GuestSubnet.id,
          subnet: values.subnet
        }
        api('updateIpv4SubnetForZone', params).then(response => {
          this.$pollJob({
            jobId: response.updateipv4subnetforzoneresponse.jobid,
            title: this.$t('label.update.ipv4.subnet'),
            description: values.subnet,
            successMessage: this.$t('message.success.update.ipv4.subnet'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            },
            errorMessage: this.$t('message.update.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchZoneIpv4Subnet()
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.updateipv4subnetforzoneresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchZoneIpv4Subnet()
        })
      })
    },
    changeIpv4SubnetPage (page, pageSize) {
      this.ipv4SubnetPage = page
      this.ipv4SubnetPageSize = pageSize
      this.fetchZoneIpv4Subnet()
    },
    changeIpv4SubnetPageSize (currentPage, pageSize) {
      this.ipv4SubnetPage = currentPage
      this.ipv4SubnetPageSize = pageSize
      this.fetchZoneIpv4Subnet()
    }
  }
}
</script>

<style lang="scss" scoped>
  .pagination {
    margin-top: 20px;
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
  .ant-select {
    width: 100%;
  }

</style>
