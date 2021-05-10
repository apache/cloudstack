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
  <a-spin :spinning="loading">
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('label.details')" key="details">
        <DetailsTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.iso')" key="cdrom" v-if="vm.isoid">
        <a-icon type="usb" />
        <router-link :to="{ path: '/iso/' + vm.isoid }">{{ vm.isoname }}</router-link> <br/>
        <a-icon type="barcode"/> {{ vm.isoid }}
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.volumes')" key="volumes" v-if="'listVolumes' in $store.getters.apis">
        <a-table
          class="table"
          size="small"
          :columns="volumeColumns"
          :dataSource="volumes"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template slot="name" slot-scope="text, item">
            <a-icon type="hdd" />
            <router-link :to="{ path: '/volume/' + item.id }">
              {{ text }}
            </router-link>
            <a-tag v-if="item.provisioningtype">
              {{ item.provisioningtype }}
            </a-tag>
          </template>
          <template slot="state" slot-scope="text">
            <status :text="text ? text : ''" />{{ text }}
          </template>
          <template slot="size" slot-scope="text, item">
            {{ parseFloat(item.size / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GB
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.nics')" key="nics" v-if="'listNics' in $store.getters.apis">
        <a-button
          type="dashed"
          style="width: 100%; margin-bottom: 10px"
          @click="showAddModal"
          :loading="loadingNic"
          :disabled="!('addNicToVirtualMachine' in $store.getters.apis)">
          <a-icon type="plus"></a-icon> {{ $t('label.network.addvm') }}
        </a-button>
        <NicsTable :resource="vm" :loading="loading">
          <span slot="actions" slot-scope="record">
            <a-popconfirm
              :title="$t('label.set.default.nic')"
              @confirm="setAsDefault(record.nic)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
              v-if="!record.nic.isdefault"
            >
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.set.default.nic')"
                :disabled="!('updateDefaultNicForVirtualMachine' in $store.getters.apis)"
                icon="check-square" />
            </a-popconfirm>
            <tooltip-button
              v-if="record.nic.type !== 'L2'"
              tooltipPlacement="bottom"
              :tooltip="$t('label.change.ip.addess')"
              icon="swap"
              :disabled="!('updateVmNicIp' in $store.getters.apis)"
              @click="onChangeIPAddress(record)" />
            <tooltip-button
              v-if="record.nic.type !== 'L2'"
              tooltipPlacement="bottom"
              :tooltip="$t('label.edit.secondary.ips')"
              icon="environment"
              :disabled="(!('addIpToNic' in $store.getters.apis) && !('addIpToNic' in $store.getters.apis))"
              @click="onAcquireSecondaryIPAddress(record)" />
            <a-popconfirm
              :title="$t('message.network.removenic')"
              @confirm="removeNIC(record.nic)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
              v-if="!record.nic.isdefault"
            >
              <tooltip-button
                tooltipPlacement="bottom"
                :tooltip="$t('label.action.delete.nic')"
                :disabled="!('removeNicFromVirtualMachine' in $store.getters.apis)"
                type="danger"
                icon="delete" />
            </a-popconfirm>
          </span>
        </NicsTable>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.vm.snapshots')" key="vmsnapshots" v-if="'listVMSnapshot' in $store.getters.apis">
        <ListResourceTable
          apiName="listVMSnapshot"
          :resource="resource"
          :params="{virtualmachineid: this.resource.id}"
          :columns="['displayname', 'state', 'type', 'created']"
          :routerlinks="(record) => { return { displayname: '/vmsnapshot/' + record.id } }"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.backup')" key="backups" v-if="'listBackups' in $store.getters.apis">
        <ListResourceTable
          apiName="listBackups"
          :resource="resource"
          :params="{virtualmachineid: this.resource.id}"
          :columns="['id', 'status', 'type', 'created']"
          :routerlinks="(record) => { return { id: '/backup/' + record.id } }"
          :showSearch="false"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.securitygroups')" key="securitygroups" v-if="this.resource.securitygroup && this.resource.securitygroup.length > 0">
        <ListResourceTable
          :items="this.resource.securitygroup"
          :columns="['name', 'description']"
          :routerlinks="(record) => { return { name: '/securitygroups/' + record.id } }"
          :showSearch="false"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.settings')" key="settings">
        <DetailSettings :resource="resource" :loading="loading" />
      </a-tab-pane>
    </a-tabs>

    <a-modal
      :visible="showAddNetworkModal"
      :title="$t('label.network.addvm')"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @cancel="closeModals"
      @ok="submitAddNetwork">
      {{ $t('message.network.addvm.desc') }}
      <div class="modal-form">
        <p class="modal-form__label">{{ $t('label.network') }}:</p>
        <a-select :defaultValue="addNetworkData.network" @change="e => addNetworkData.network = e" autoFocus>
          <a-select-option
            v-for="network in addNetworkData.allNetworks"
            :key="network.id"
            :value="network.id">
            {{ network.name }}
          </a-select-option>
        </a-select>
        <p class="modal-form__label">{{ $t('label.publicip') }}:</p>
        <a-input v-model="addNetworkData.ip"></a-input>
      </div>
    </a-modal>

    <a-modal
      :visible="showUpdateIpModal"
      :title="$t('label.change.ipaddress')"
      :maskClosable="false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @cancel="closeModals"
      @ok="submitUpdateIP"
    >
      {{ $t('message.network.updateip') }}

      <div class="modal-form">
        <p class="modal-form__label">{{ $t('label.publicip') }}:</p>
        <a-select
          showSearch
          v-if="editNicResource.type==='Shared'"
          v-model="editIpAddressValue"
          :loading="listIps.loading"
          :autoFocus="editNicResource.type==='Shared'">
          <a-select-option v-for="ip in listIps.opts" :key="ip.ipaddress">
            {{ ip.ipaddress }}
          </a-select-option>
        </a-select>
        <a-input
          v-else
          v-model="editIpAddressValue"
          :autoFocus="editNicResource.type!=='Shared'"></a-input>
      </div>
    </a-modal>

    <a-modal
      :visible="showSecondaryIpModal"
      :title="$t('label.acquire.new.secondary.ip')"
      :maskClosable="false"
      :footer="null"
      :closable="false"
      class="wide-modal"
    >
      <p>
        {{ $t('message.network.secondaryip') }}
      </p>
      <a-divider />
      <div class="modal-form">
        <p class="modal-form__label">{{ $t('label.publicip') }}:</p>
        <a-select
          showSearch
          v-if="editNicResource.type==='Shared'"
          v-model="newSecondaryIp"
          :loading="listIps.loading">
          <a-select-option v-for="ip in listIps.opts" :key="ip.ipaddress">
            {{ ip.ipaddress }}
          </a-select-option>
        </a-select>
        <a-input
          v-else
          :placeholder="$t('label.new.secondaryip.description')"
          v-model="newSecondaryIp"></a-input>
      </div>

      <div style="margin-top: 10px; display: flex; justify-content:flex-end;">
        <a-button @click="submitSecondaryIP" type="primary" style="margin-right: 10px;">{{ $t('label.add.secondary.ip') }}</a-button>
        <a-button @click="closeModals">{{ $t('label.close') }}</a-button>
      </div>

      <a-divider />
      <a-list itemLayout="vertical">
        <a-list-item v-for="(ip, index) in secondaryIPs" :key="index">
          <a-popconfirm
            :title="`${$t('label.action.release.ip')}?`"
            @confirm="removeSecondaryIP(ip.id)"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')"
          >
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.action.release.ip')"
              type="danger"
              icon="delete" />
            {{ ip.ipaddress }}
          </a-popconfirm>
        </a-list-item>
      </a-list>
    </a-modal>

  </a-spin>
</template>

<script>

import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import ResourceLayout from '@/layouts/ResourceLayout'
import Status from '@/components/widgets/Status'
import DetailsTab from '@/components/view/DetailsTab'
import DetailSettings from '@/components/view/DetailSettings'
import NicsTable from '@/views/network/NicsTable'
import ListResourceTable from '@/components/view/ListResourceTable'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'InstanceTab',
  components: {
    ResourceLayout,
    DetailsTab,
    DetailSettings,
    NicsTable,
    Status,
    ListResourceTable,
    TooltipButton
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
  inject: ['parentFetchData'],
  data () {
    return {
      vm: {},
      volumes: [],
      totalStorage: 0,
      currentTab: 'details',
      showAddNetworkModal: false,
      showUpdateIpModal: false,
      showSecondaryIpModal: false,
      addNetworkData: {
        allNetworks: [],
        network: '',
        ip: ''
      },
      loadingNic: false,
      editIpAddressNic: '',
      editIpAddressValue: '',
      editNetworkId: '',
      secondaryIPs: [],
      selectedNicId: '',
      newSecondaryIp: '',
      volumeColumns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.size'),
          dataIndex: 'size',
          scopedSlots: { customRender: 'size' }
        }
      ],
      editNicResource: {},
      listIps: {
        loading: false,
        opts: []
      }
    }
  },
  created () {
    this.vm = this.resource
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.vm = newItem
      this.fetchData()
    },
    $route: function (newItem, oldItem) {
      this.setCurrentTab()
    }
  },
  mounted () {
    this.setCurrentTab()
  },
  methods: {
    setCurrentTab () {
      this.currentTab = this.$route.query.tab ? this.$route.query.tab : 'details'
    },
    handleChangeTab (e) {
      this.currentTab = e
      const query = Object.assign({}, this.$route.query)
      query.tab = e
      history.replaceState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    },
    fetchData () {
      this.volumes = []
      if (!this.vm || !this.vm.id) {
        return
      }
      api('listVolumes', { listall: true, virtualmachineid: this.vm.id }).then(json => {
        this.volumes = json.listvolumesresponse.volume
        if (this.volumes) {
          this.volumes.sort((a, b) => { return a.deviceid - b.deviceid })
        }
        this.$set(this.resource, 'volumes', this.volumes)
      })
    },
    listNetworks () {
      api('listNetworks', {
        listAll: 'true',
        zoneid: this.vm.zoneid
      }).then(response => {
        this.addNetworkData.allNetworks = response.listnetworksresponse.network.filter(network => !this.vm.nic.map(nic => nic.networkid).includes(network.id))
        this.addNetworkData.network = this.addNetworkData.allNetworks[0].id
      })
    },
    fetchSecondaryIPs (nicId) {
      this.showSecondaryIpModal = true
      this.selectedNicId = nicId
      api('listNics', {
        nicId: nicId,
        keyword: '',
        virtualmachineid: this.vm.id
      }).then(response => {
        this.secondaryIPs = response.listnicsresponse.nic[0].secondaryip
      })
    },
    fetchPublicIps (networkid) {
      this.listIps.loading = true
      this.listIps.opts = []
      api('listPublicIpAddresses', {
        networkid: networkid,
        allocatedonly: false,
        forvirtualnetwork: false
      }).then(json => {
        const listPublicIps = json.listpublicipaddressesresponse.publicipaddress || []
        listPublicIps.forEach(item => {
          if (item.state === 'Free') {
            this.listIps.opts.push({
              ipaddress: item.ipaddress
            })
          }
        })
        this.listIps.opts.sort(function (a, b) {
          const currentIp = a.ipaddress.replaceAll('.', '')
          const nextIp = b.ipaddress.replaceAll('.', '')
          if (parseInt(currentIp) < parseInt(nextIp)) { return -1 }
          if (parseInt(currentIp) > parseInt(nextIp)) { return 1 }
          return 0
        })
      }).finally(() => {
        this.listIps.loading = false
      })
    },
    showAddModal () {
      this.showAddNetworkModal = true
      this.listNetworks()
    },
    closeModals () {
      this.showAddNetworkModal = false
      this.showUpdateIpModal = false
      this.showSecondaryIpModal = false
      this.addNetworkData.network = ''
      this.addNetworkData.ip = ''
      this.editIpAddressValue = ''
      this.newSecondaryIp = ''
    },
    onChangeIPAddress (record) {
      this.editNicResource = record.nic
      this.editIpAddressNic = record.nic.id
      this.showUpdateIpModal = true
      if (record.nic.type === 'Shared') {
        this.fetchPublicIps(record.nic.networkid)
      }
    },
    onAcquireSecondaryIPAddress (record) {
      if (record.nic.type === 'Shared') {
        this.fetchPublicIps(record.nic.networkid)
      } else {
        this.listIps.opts = []
      }

      this.editNicResource = record.nic
      this.editNetworkId = record.nic.networkid
      this.fetchSecondaryIPs(record.nic.id)
    },
    submitAddNetwork () {
      const params = {}
      params.virtualmachineid = this.vm.id
      params.networkid = this.addNetworkData.network
      if (this.addNetworkData.ip) {
        params.ipaddress = this.addNetworkData.ip
      }
      this.showAddNetworkModal = false
      this.loadingNic = true
      api('addNicToVirtualMachine', params).then(response => {
        this.$pollJob({
          jobId: response.addnictovirtualmachineresponse.jobid,
          successMessage: this.$t('message.success.add.network'),
          successMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          errorMessage: this.$t('message.add.network.failed'),
          errorMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.add.network.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loadingNic = false
      })
    },
    setAsDefault (item) {
      this.loadingNic = true
      api('updateDefaultNicForVirtualMachine', {
        virtualmachineid: this.vm.id,
        nicid: item.id
      }).then(response => {
        this.$pollJob({
          jobId: response.updatedefaultnicforvirtualmachineresponse.jobid,
          successMessage: `${this.$t('label.success.set')} ${item.networkname} ${this.$t('label.as.default')}. ${this.$t('message.set.default.nic.manual')}.`,
          successMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          errorMessage: `${this.$t('label.error.setting')} ${item.networkname} ${this.$t('label.as.default')}`,
          errorMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          loadingMessage: `${this.$t('label.setting')} ${item.networkname} ${this.$t('label.as.default')}...`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loadingNic = false
      })
    },
    submitUpdateIP () {
      this.loadingNic = true
      this.showUpdateIpModal = false
      api('updateVmNicIp', {
        nicId: this.editIpAddressNic,
        ipaddress: this.editIpAddressValue
      }).then(response => {
        this.$pollJob({
          jobId: response.updatevmnicipresponse.jobid,
          successMessage: this.$t('message.success.update.ipaddress'),
          successMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          errorMessage: this.$t('label.error'),
          errorMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.update.ipaddress.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          }
        })
      })
        .catch(error => {
          this.$notifyError(error)
          this.loadingNic = false
        })
    },
    removeNIC (item) {
      this.loadingNic = true

      api('removeNicFromVirtualMachine', {
        nicid: item.id,
        virtualmachineid: this.vm.id
      }).then(response => {
        this.$pollJob({
          jobId: response.removenicfromvirtualmachineresponse.jobid,
          successMessage: this.$t('message.success.remove.nic'),
          successMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          errorMessage: this.$t('message.error.remove.nic'),
          errorMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.remove.nic.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          }
        })
      })
        .catch(error => {
          this.$notifyError(error)
          this.loadingNic = false
        })
    },
    submitSecondaryIP () {
      this.loadingNic = true

      const params = {}
      params.nicid = this.selectedNicId
      if (this.newSecondaryIp) {
        params.ipaddress = this.newSecondaryIp
      }

      api('addIpToNic', params).then(response => {
        this.$pollJob({
          jobId: response.addiptovmnicresponse.jobid,
          successMessage: this.$t('message.success.add.secondary.ipaddress'),
          successMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          errorMessage: this.$t('message.error.add.secondary.ipaddress'),
          errorMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.add.secondary.ipaddress.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loadingNic = false
      }).finally(() => {
        this.newSecondaryIp = null
        this.fetchPublicIps(this.editNetworkId)
      })
    },
    removeSecondaryIP (id) {
      this.loadingNic = true

      api('removeIpFromNic', { id }).then(response => {
        this.$pollJob({
          jobId: response.removeipfromnicresponse.jobid,
          successMessage: this.$t('message.success.remove.secondary.ipaddress'),
          successMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.fetchPublicIps(this.editNetworkId)
            this.parentFetchData()
          },
          errorMessage: this.$t('message.error.remove.secondary.ipaddress'),
          errorMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          loadingMessage: this.$t('message.remove.secondary.ipaddress.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loadingNic = false
        this.fetchSecondaryIPs(this.selectedNicId)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .page-header-wrapper-grid-content-main {
    width: 100%;
    height: 100%;
    min-height: 100%;
    transition: 0.3s;
    .vm-detail {
      .svg-inline--fa {
        margin-left: -1px;
        margin-right: 8px;
      }
      span {
        margin-left: 10px;
      }
      margin-bottom: 8px;
    }
  }

  .list {
    margin-top: 20px;

    &__item {
      display: flex;
      flex-direction: column;
      align-items: flex-start;

      @media (min-width: 760px) {
        flex-direction: row;
        align-items: center;
      }
    }
  }

  .modal-form {
    display: flex;
    flex-direction: column;

    &__label {
      margin-top: 20px;
      margin-bottom: 5px;
      font-weight: bold;

      &--no-margin {
        margin-top: 0;
      }
    }
  }

  .actions {
    display: flex;
    flex-wrap: wrap;

    button {
      padding: 5px;
      height: auto;
      margin-bottom: 10px;
      align-self: flex-start;

      &:not(:last-child) {
        margin-right: 10px;
      }
    }

  }

  .label {
    font-weight: bold;
  }

  .attribute {
    margin-bottom: 10px;
  }

  .ant-tag {
    padding: 4px 10px;
    height: auto;
  }

  .title {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;

    a {
      margin-right: 30px;
      margin-bottom: 10px;
    }

    .ant-tag {
      margin-bottom: 10px;
    }

    &__details {
      display: flex;
    }

    .tags {
      margin-left: 10px;
    }

  }

  .ant-list-item-meta-title {
    margin-bottom: -10px;
  }

  .divider-small {
    margin-top: 20px;
    margin-bottom: 20px;
  }

  .list-item {

    &:not(:first-child) {
      padding-top: 25px;
    }

  }
</style>

<style scoped>
.wide-modal {
  min-width: 50vw;
}

/deep/ .ant-list-item {
  padding-top: 12px;
  padding-bottom: 12px;
}
</style>
