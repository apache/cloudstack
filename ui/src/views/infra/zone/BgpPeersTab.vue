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
    <a-alert
      v-if="this.resource.ip4routing"
      type="info">
      <template #message>
        <div v-html="$t('message.bgp.peers.null')" />
      </template>
    </a-alert>
    <br>
    <a-button
      v-if="!this.resource.ip4routing"
      :disabled="!('createBgpPeer' in $store.getters.apis)"
      type="primary"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddBgpPeerModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.bgp.peer') }}
    </a-button>

    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="bgpPeersColumns"
      :dataSource="bgpPeers"
      :rowKey="record => record.id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'asnumber'">
          {{ record.asnumber }}
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
        <template v-if="column.key === 'actions' && !this.resource.ip4routing">
          <div
            class="actions"
            style="text-align: right" >
            <router-link :to="{ name: 'guestnetwork', query: { bgppeerid: record.id }}">
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.view') + ' ' + $t('label.networks')"
                icon="environment-outlined"/>
            </router-link>
            <tooltip-button
              v-if="!record.domain && record.asnumber"
              tooltipPlacement="bottom"
              :tooltip="$t('label.add.account')"
              icon="user-add-outlined"
              @onClick="() => handleOpenAddAccountForBgpPeerModal(record)"
              :disabled="!('dedicateBgpPeer' in $store.getters.apis)" />
            <tooltip-button
              v-if="record.domain"
              tooltipPlacement="bottom"
              :tooltip="$t('label.release.account')"
              icon="user-delete-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleRemoveAccountFromBgpPeer(record.id)"
              :disabled="!('releaseBgpPeer' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.update.bgp.peer')"
              icon="edit-outlined"
              type="primary"
              :danger="true"
              @onClick="() => handleUpdateBgpPeerModal(record)"
              :disabled="!('updateBgpPeer' in $store.getters.apis)" />
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.remove.bgp.peer')"
              icon="delete-outlined"
              type="primary"
              :danger="true"
              @onClick="handleDeleteBgpPeer(record.id)"
              :disabled="!('deleteBgpPeer' in $store.getters.apis)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      v-if="!this.resource.ip4routing"
      class="row-element pagination"
      size="small"
      :current="bgpPeersPage"
      :pageSize="bgpPeersPageSize"
      :total="bgpPeersTotal"
      :showTotal="total => `${$t('label.total')} ${bgpPeersTotal} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changeBgpPeerPage"
      @showSizeChange="changeBgpPeerPageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="accountForBgpPeerModal"
      v-if="selectedBgpPeer"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="accountForBgpPeerModal = false">
      <div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}</div>
          <div>{{ selectedBgpPeer.account }}</div>
        </div>
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.domain') }}</div>
          <div>{{ selectedBgpPeer.domain }}</div>
        </div>
      </div>

      <div :span="24" class="action-button">
        <a-button @click="accountForBgpPeerModal = false">{{ $t('label.close') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      v-if="addAccountForBgpPeerModal"
      :zIndex="1001"
      :closable="true"
      :maskClosable="false"
      :visible="addAccountForBgpPeerModal"
      :title="$t('label.add.account')"
      :footer="null"
      @cancel="addAccountForBgpPeerModal = false">
      <a-spin :spinning="domainsLoading" v-ctrl-enter="handleAddAccountForBgpPeer">
        <div style="margin-bottom: 10px;">
          <div class="list__label">{{ $t('label.account') }}:</div>
          <a-input v-model:value="addAccountForBgpPeer.account" v-focus="true"></a-input>
        </div>
        <div>
          <div class="list__label">{{ $t('label.domain') }}:</div>
          <a-select
            v-model:value="addAccountForBgpPeer.domain"
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
          <a-button @click="addAccountForBgpPeerModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddAccountForBgpPeer">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>

    <a-modal
      v-if="addBgpPeerModal"
      :visible="addBgpPeerModal"
      :title="$t('label.add.bgp.peer')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="addBgpPeerModal = false">
      <a-form
        :ref="bgpPeersFormRef"
        :model="bgpPeersForm"
        :rules="bgpPeersRules"
        @finish="handleAddBgpPeer"
        v-ctrl-enter="handleAddBgpPeer"
        layout="vertical"
        class="form"
      >
        <a-form-item name="asnumber" ref="asnumber" :label="$t('label.asnumber')" class="form__item">
          <a-input v-focus="true" v-model:value="bgpPeersForm.asnumber" />
        </a-form-item>
        <a-form-item name="ipaddress" ref="ipaddress" :label="$t('label.ipaddress')" class="form__item">
          <a-input v-model:value="bgpPeersForm.ipaddress" />
        </a-form-item>
        <a-form-item name="ip6address" ref="ip6address" :label="$t('label.ip6address')" class="form__item">
          <a-input v-model:value="bgpPeersForm.ip6address" />
        </a-form-item>
        <a-form-item name="password" ref="password" :label="$t('label.password')" class="form__item">
          <a-input v-model:value="bgpPeersForm.password" />
        </a-form-item>
        <div class="form__item">
          <div style="color: black;">{{ $t('label.set.reservation') }}</div>
          <a-switch v-model:checked="showAccountForBgpPeerFields" @change="handleShowAccountForBgpPeerFields" />
        </div>
        <div v-if="showAccountForBgpPeerFields" style="margin-top: 20px;">
          <div v-html="$t('label.bgp.peer.set.reservation.desc')"></div><br>
          <a-spin :spinning="domainsLoading">
            <a-form-item name="account" ref="account" :label="$t('label.account')" class="form__item">
              <a-input v-model:value="bgpPeersForm.account"></a-input>
            </a-form-item>
            <a-form-item name="domain" ref="domain" :label="$t('label.domain')" class="form__item">
              <a-select
                v-model:value="bgpPeersForm.domain"
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
          <a-button @click="addBgpPeerModal = false; showAccountForBgpPeerFields = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddBgpPeer">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :visible="updateBgpPeerModal"
      :title="$t('label.update.bgp.peer')"
      v-if="selectedBgpPeer"
      :maskClosable="false"
      :footer="null"
      @cancel="updateBgpPeerModal = false">
      <a-form
        :ref="updateBgpPeerRef"
        :model="formUpdateBgpPeer"
        :rules="updateBgpPeerRules"
        @finish="handleAddBgpPeer"
        v-ctrl-enter="handleAddBgpPeer"
        layout="vertical"
        class="form"
      >
        <div>
          <a-form-item name="asnumber" ref="asnumber" :label="$t('label.asnumber')" class="form__item">
            <a-input v-focus="true" v-model:value="formUpdateBgpPeer.asnumber"></a-input>
          </a-form-item>
          <a-form-item name="ipaddress" ref="ipaddress" :label="$t('label.ipaddress')" class="form__item">
            <a-input v-model:value="formUpdateBgpPeer.ipaddress" />
          </a-form-item>
          <a-form-item name="ip6address" ref="ip6address" :label="$t('label.ip6address')" class="form__item">
            <a-input v-model:value="formUpdateBgpPeer.ip6address" />
          </a-form-item>
          <a-form-item name="password" ref="password" :label="$t('label.password')" class="form__item">
            <a-input v-model:value="formUpdateBgpPeer.password" />
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="updateBgpPeerModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleUpdateBgpPeer">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      v-if="changeBgpPeersForNetworkModal"
      :visible="changeBgpPeersForNetworkModal"
      :title="$t('label.change.bgp.peers')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { changeBgpPeersForNetworkModal = false }"
      centered
      width="auto">
      <ChangeBgpPeersForNetwork
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="changeBgpPeersForNetworkModal = false" />
    </a-modal>

    <a-modal
      v-if="changeBgpPeersForVpcModal"
      :visible="changeBgpPeersForVpcModal"
      :title="$t('label.change.bgp.peers')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { changeBgpPeersForVpcModal = false }"
      centered
      width="auto">
      <ChangeBgpPeersForVpc
        :resource="resource"
        @refresh-data="fetchData"
        @close-action="changeBgpPeersForVpcModal = false" />
    </a-modal>

    <br>
    <a-button
      v-if="this.resource.ip4routing && this.$route.meta.name === 'guestnetwork'"
      type="primary"
      style="margin-bottom: 20px; width: 100%"
      @click="() => { changeBgpPeersForNetworkModal = true }"
      :disabled="!('changeBgpPeersForNetwork' in $store.getters.apis)">
      <template #icon><split-cells-outlined /></template>
      {{ $t('label.change.bgp.peers') }}
    </a-button>
    <a-button
      v-if="this.resource.ip4routing && this.$route.meta.name === 'vpc'"
      type="primary"
      style="margin-bottom: 20px; width: 100%"
      @click="() => { changeBgpPeersForVpcModal = true }"
      :disabled="!('changeBgpPeersForVpc' in $store.getters.apis)">
      <template #icon><split-cells-outlined /></template>
      {{ $t('label.change.bgp.peers') }}
    </a-button>
    <br>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipButton from '@/components/widgets/TooltipButton'
import ChangeBgpPeersForNetwork from '@/views/network/ChangeBgpPeerForNetwork.vue'
import ChangeBgpPeersForVpc from '@/views/network/ChangeBgpPeerForVpc.vue'

export default {
  name: 'BgpPeersTab',
  components: {
    ChangeBgpPeersForNetwork,
    ChangeBgpPeersForVpc,
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
      selectedBgpPeer: null,
      bgpPeers: [],
      showAccountForBgpPeerFields: false,
      accountForBgpPeerModal: false,
      addAccountForBgpPeerModal: false,
      addAccountForBgpPeer: {
        account: null,
        domain: null
      },
      domains: [],
      domainsLoading: false,
      addBgpPeerModal: false,
      updateBgpPeerModal: false,
      changeBgpPeersForNetworkModal: false,
      changeBgpPeersForVpcModal: false,
      bgpPeersPage: 1,
      bgpPeersPageSize: 10,
      bgpPeersTotal: 0,
      bgpPeersColumns: [
        {
          title: this.$t('label.asnumber'),
          dataIndex: 'asnumber'
        },
        {
          title: this.$t('label.ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          title: this.$t('label.ip6address'),
          dataIndex: 'ip6address'
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
    this.bgpPeersForm = null
    this.bgpPeersFormRef = null
    this.bgpPeersRules = null
  },
  created () {
    this.initAddBgpPeerForm()
    this.initUpdateBgpPeerForm()
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
    initAddBgpPeerForm () {
      this.bgpPeersFormRef = ref()
      this.bgpPeersForm = reactive({
      })
      this.bgpPeersRules = reactive({
        asnumber: [{ required: true, message: this.$t('label.required') }]
      })
    },
    initUpdateBgpPeerForm () {
      this.updateBgpPeerRef = ref()
      this.formUpdateBgpPeer = reactive({})
      this.updateBgpPeerRules = reactive({
        asnumber: [{ required: true, message: this.$t('label.required') }]
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
          this.addAccountForBgpPeer.domain = this.domains[0].id
          this.bgpPeersForm.domain = this.domains[0].id
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.domainsLoading = false
      })
    },
    fetchData () {
      if (this.resource.ip4routing) {
        this.bgpPeers = this.resource.bgppeers
      } else {
        this.fetchZoneBgpPeer()
      }
    },
    fetchZoneBgpPeer () {
      this.componentLoading = true
      api('listBgpPeers', {
        zoneid: this.resource.id,
        projectid: -1,
        showicon: true,
        page: this.bgpPeersPage,
        pagesize: this.bgpPeersPageSize
      }).then(response => {
        this.bgpPeers = response?.listbgppeersresponse?.bgppeer || []
        this.bgpPeersTotal = response?.listbgppeersresponse?.count || 0
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleAddAccountForBgpPeer () {
      if (this.domainsLoading) return
      this.domainsLoading = true

      if (this.addBgpPeerModal === true) {
        this.addAccountForBgpPeerModal = false
        return
      }

      var params = {
        id: this.selectedBgpPeer.id,
        zoneid: this.selectedBgpPeer.zoneid,
        domainid: this.addAccountForBgpPeer.domain
      }

      if (this.addAccountForBgpPeer.account) {
        params.account = this.addAccountForBgpPeer.account
      }

      api('dedicateBgpPeer', params).then(response => {
        this.$pollJob({
          jobId: response.dedicatebgppeerresponse.jobid,
          title: this.$t('label.dedicate.bgp.peer'),
          successMessage: this.$t('message.success.dedicate.bgp.peer'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          errorMessage: this.$t('error.dedicate.bgp.peer.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addAccountForBgpPeerModal = false
        this.domainsLoading = false
        this.fetchZoneBgpPeer()
      })
    },
    handleRemoveAccountFromBgpPeer (id) {
      this.componentLoading = true
      api('releaseBgpPeer', { id }).then(response => {
        this.$pollJob({
          jobId: response.releasebgppeerresponse.jobid,
          title: this.$t('label.release.dedicated.bgp.peer'),
          successMessage: this.$t('message.success.release.dedicated.bgp.peer'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          errorMessage: this.$t('error.release.dedicate.bgp.peer'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchZoneBgpPeer()
      })
    },
    handleOpenAddAccountForBgpPeerModal (item) {
      if (!this.addBgpPeerModal) {
        this.selectedBgpPeer = item
      }
      this.addAccountForBgpPeerModal = true
      this.fetchDomains()
    },
    handleShowAccountForBgpPeerFields () {
      if (this.showAccountForBgpPeerFields) {
        this.fetchDomains()
      }
    },
    handleOpenAddBgpPeerModal () {
      this.initAddBgpPeerForm()
      this.addBgpPeerModal = true
    },
    handleUpdateBgpPeerModal (item) {
      this.initUpdateBgpPeerForm()
      this.selectedBgpPeer = item
      this.updateBgpPeerModal = true
      this.formUpdateBgpPeer.asnumber = this.selectedBgpPeer?.asnumber || ''
      this.formUpdateBgpPeer.ipaddress = this.selectedBgpPeer?.ipaddress || ''
      this.formUpdateBgpPeer.ip6address = this.selectedBgpPeer?.ip6address || ''
    },
    handleDeleteBgpPeer (id) {
      this.componentLoading = true
      api('deleteBgpPeer', { id }).then(response => {
        this.$pollJob({
          jobId: response.deletebgppeerresponse.jobid,
          title: this.$t('label.delete.bgp.peer'),
          successMessage: this.$t('message.success.delete.bgp.peer'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          errorMessage: this.$t('message.delete.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchZoneBgpPeer()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
        this.fetchZoneBgpPeer()
      })
    },
    handleAddBgpPeer (e) {
      if (this.componentLoading) return
      this.bgpPeersFormRef.value.validate().then(() => {
        const values = toRaw(this.bgpPeersForm)
        this.componentLoading = true
        this.addBgpPeerModal = false
        this.showAccountForBgpPeerFields = false
        var params = {
          zoneId: this.resource.id,
          asnumber: values.asnumber,
          ipaddress: values.ipaddress,
          ip6address: values.ip6address,
          password: values.password,
          domainid: values.domain,
          account: values.account
        }
        api('createBgpPeer', params).then(response => {
          this.$pollJob({
            jobId: response.createbgppeerresponse.jobid,
            title: this.$t('label.add.bgp.peer'),
            successMessage: this.$t('message.success.add.bgp.peer'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            },
            errorMessage: this.$t('message.add.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createbgppeerresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchZoneBgpPeer()
        })
      }).catch(error => {
        this.bgpPeersFormRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleUpdateBgpPeer (e) {
      if (this.componentLoading) return
      this.updateBgpPeerRef.value.validate().then(() => {
        const values = toRaw(this.formUpdateBgpPeer)

        this.componentLoading = true
        this.updateBgpPeerModal = false
        var params = {
          id: this.selectedBgpPeer.id,
          asnumber: values.asnumber,
          ipaddress: values.ipaddress,
          ip6address: values.ip6address,
          password: values.password
        }
        api('updateBgpPeer', params).then(response => {
          this.$pollJob({
            jobId: response.updatebgppeerresponse.jobid,
            title: this.$t('label.update.bgp.peer'),
            successMessage: this.$t('message.success.update.bgp.peer'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            },
            errorMessage: this.$t('message.update.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchZoneBgpPeer()
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.updatebgppeerresponse?.errortext || error.response.data.errorresponse.errortext,
            duration: 0
          })
        }).finally(() => {
          this.componentLoading = false
          this.fetchZoneBgpPeer()
        })
      })
    },
    changeBgpPeerPage (page, pageSize) {
      this.bgpPeersPage = page
      this.bgpPeersPageSize = pageSize
      this.fetchZoneBgpPeer()
    },
    changeBgpPeerPageSize (currentPage, pageSize) {
      this.bgpPeersPage = currentPage
      this.bgpPeersPageSize = pageSize
      this.fetchZoneBgpPeer()
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
