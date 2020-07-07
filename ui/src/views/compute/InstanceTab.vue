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
              okText="Yes"
              cancelText="No"
              v-if="!record.nic.isdefault"
            >
              <a-button
                :disabled="!('updateDefaultNicForVirtualMachine' in $store.getters.apis)"
                icon="check-square"
                shape="circle" />
            </a-popconfirm>
            <a-tooltip placement="bottom" v-if="record.nic.type !== 'L2'">
              <template slot="title">
                {{ "Change IP Address" }}
              </template>
              <a-button
                icon="swap"
                shape="circle"
                :disabled="!('updateVmNicIp' in $store.getters.apis)"
                @click="editIpAddressNic = record.nic.id; showUpdateIpModal = true" />
            </a-tooltip>
            <a-tooltip placement="bottom" v-if="record.nic.type !== 'L2'">
              <template slot="title">
                {{ "Manage Secondary IP Addresses" }}
              </template>
              <a-button
                icon="environment"
                shape="circle"
                :disabled="(!('addIpToNic' in $store.getters.apis) && !('addIpToNic' in $store.getters.apis))"
                @click="fetchSecondaryIPs(record.nic.id)" />
            </a-tooltip>
            <a-popconfirm
              :title="$t('message.network.removenic')"
              @confirm="removeNIC(record.nic)"
              okText="Yes"
              cancelText="No"
              v-if="!record.nic.isdefault"
            >
              <a-button
                :disabled="!('removeNicFromVirtualMachine' in $store.getters.apis)"
                type="danger"
                icon="delete"
                shape="circle" />
            </a-popconfirm>
          </span>
        </NicsTable>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.vm.snapshots')" key="vmsnapshots" v-if="'listVMSnapshot' in $store.getters.apis">
        <ListResourceTable
          apiName="listVMSnapshot"
          :params="{virtualmachineid: this.resource.id}"
          :columns="['name', 'state', 'type', 'created']"
          :routerlink="{name: 'name', prefix: '/vmsnapshot/'}"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.backup')" key="backups" v-if="'listBackups' in $store.getters.apis">
        <ListResourceTable
          apiName="listBackups"
          :params="{virtualmachineid: this.resource.id}"
          :columns="['id', 'state', 'type', 'created']"
          :routerlink="{name: 'id', prefix: '/backup/'}"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.settings')" key="settings">
        <DetailSettings :resource="resource" :loading="loading" />
      </a-tab-pane>
    </a-tabs>

    <a-modal
      :visible="showAddNetworkModal"
      :title="$t('label.network.addvm')"
      @cancel="closeModals"
      @ok="submitAddNetwork">
      {{ $t('message.network.addvm.desc') }}
      <div class="modal-form">
        <p class="modal-form__label">{{ $t('label.network') }}:</p>
        <a-select :defaultValue="addNetworkData.network" @change="e => addNetworkData.network = e">
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
      @cancel="closeModals"
      @ok="submitUpdateIP"
    >
      {{ $t('message.network.updateip') }}

      <div class="modal-form">
        <p class="modal-form__label">{{ $t('label.publicip') }}:</p>
        <a-input v-model="editIpAddressValue"></a-input>
      </div>
    </a-modal>

    <a-modal
      :visible="showSecondaryIpModal"
      :title="$t('label.acquire.new.secondary.ip')"
      :footer="null"
      :closable="false"
      class="wide-modal"
    >
      <p>
        {{ $t('message.network.secondaryip') }}
      </p>
      <a-divider />
      <a-input placeholder="Enter new secondary IP address" v-model="newSecondaryIp"></a-input>
      <div style="margin-top: 10px; display: flex; justify-content:flex-end;">
        <a-button @click="submitSecondaryIP" type="primary" style="margin-right: 10px;">Add Secondary IP</a-button>
        <a-button @click="closeModals">Close</a-button>
      </div>

      <a-divider />
      <a-list itemLayout="vertical">
        <a-list-item v-for="(ip, index) in secondaryIPs" :key="index">
          <a-popconfirm
            title="Release IP?"
            @confirm="removeSecondaryIP(ip.id)"
            okText="Yes"
            cancelText="No"
          >
            <a-button
              type="danger"
              shape="circle"
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

export default {
  name: 'InstanceTab',
  components: {
    ResourceLayout,
    DetailsTab,
    DetailSettings,
    NicsTable,
    Status,
    ListResourceTable
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
      ]
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
    }
  },
  methods: {
    handleChangeTab (e) {
      this.currentTab = e
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
          successMessage: `Successfully added network`,
          successMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          errorMessage: 'Adding network failed',
          errorMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          loadingMessage: `Adding network...`,
          catchMessage: 'Error encountered while fetching async job result',
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
          successMessage: `Successfully set ${item.networkname} to default. Please manually update the default NIC on the VM now.`,
          successMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          errorMessage: `Error setting ${item.networkname} to default`,
          errorMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          loadingMessage: `Setting ${item.networkname} to default...`,
          catchMessage: 'Error encountered while fetching async job result',
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
          successMessage: `Successfully updated IP Address`,
          successMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          errorMessage: `Error`,
          errorMethod: () => {
            this.loadingNic = false
            this.closeModals()
            this.parentFetchData()
          },
          loadingMessage: `Updating IP Address...`,
          catchMessage: 'Error encountered while fetching async job result',
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
          successMessage: `Successfully removed`,
          successMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          errorMessage: `There was an error`,
          errorMethod: () => {
            this.loadingNic = false
            this.parentFetchData()
          },
          loadingMessage: `Removing NIC...`,
          catchMessage: 'Error encountered while fetching async job result',
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
          successMessage: `Successfully added secondary IP Address`,
          successMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          errorMessage: `There was an error adding the secondary IP Address`,
          errorMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          loadingMessage: `Add Secondary IP address...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loadingNic = false
      })
    },
    removeSecondaryIP (id) {
      this.loadingNic = true

      api('removeIpFromNic', { id }).then(response => {
        this.$pollJob({
          jobId: response.removeipfromnicresponse.jobid,
          successMessage: `Successfully removed secondary IP Address`,
          successMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          errorMessage: `There was an error removing the secondary IP Address`,
          errorMethod: () => {
            this.loadingNic = false
            this.fetchSecondaryIPs(this.selectedNicId)
            this.parentFetchData()
          },
          loadingMessage: `Removing Secondary IP address...`,
          catchMessage: 'Error encountered while fetching async job result',
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
