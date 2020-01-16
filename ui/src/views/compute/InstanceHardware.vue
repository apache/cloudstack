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
    <a-collapse v-model="activeKey" :bordered="false">
      <a-collapse-panel :header="'ISO: ' + vm.isoname" v-if="vm.isoid" key="1">
        <a-list
          itemLayout="horizontal">
          <a-list-item>
            <a-list-item-meta>
              <div slot="avatar">
                <a-avatar>
                  <a-icon type="usb" />
                </a-avatar>
              </div>
              <div slot="title">
                <router-link :to="{ path: '/iso/' + vm.isoid }">{{ vm.isoname }}</router-link> <br/>
                <a-icon type="barcode"/> {{ vm.isoid }}
              </div>
            </a-list-item-meta>
          </a-list-item>
        </a-list>
      </a-collapse-panel>

      <a-collapse-panel :header="'Disks: ' + volumes.length" key="2">
        <a-list
          size="small"
          itemLayout="vertical"
          :dataSource="volumes"
        >
          <a-list-item slot="renderItem" slot-scope="item" class="list-item">
            <a-list-item-meta>
              <div slot="avatar">
                <a-avatar>
                  <a-icon type="hdd" />
                </a-avatar>
              </div>
              <div slot="title" class="title">
                <router-link :to="{ path: '/volume/' + item.id }">{{ item.name }} </router-link>
                <div class="tags">
                  <a-tag v-if="item.type">
                    {{ item.type }}
                  </a-tag>
                  <a-tag v-if="item.state">
                    {{ item.state }}
                  </a-tag>
                  <a-tag v-if="item.provisioningtype">
                    {{ item.provisioningtype }}
                  </a-tag>
                </div>
              </div>
            </a-list-item-meta>
            <div>
              <a-divider class="divider-small" />
              <div class="attribute">
                <div class="label">{{ $t('size') }}</div>
                <div>{{ (item.size / (1024 * 1024 * 1024.0)).toFixed(2) }} GB</div>
              </div>
              <div class="attribute" v-if="item.physicalsize">
                <div class="label">{{ $t('physicalsize') }}</div>
                <div>{{ (item.physicalsize / (1024 * 1024 * 1024.0)).toFixed(4) }} GB</div>
              </div>
              <div class="attribute">
                <div class="label">{{ $t('storagePool') }}</div>
                <div>{{ item.storage }} ({{ item.storagetype }})</div>
              </div>
              <div class="attribute">
                <div class="label">{{ $t('id') }}</div>
                <div><a-icon type="barcode"/> {{ item.id }}</div>
              </div>
            </div>
          </a-list-item>
        </a-list>
      </a-collapse-panel>

      <a-collapse-panel :header="'Network Adapter(s): ' + (vm && vm.nic ? vm.nic.length : 0)" key="3" >
        <a-button type="primary" @click="showAddModal" :loading="loadingNic">
          <a-icon type="plus"></a-icon> {{ $t('label.network.addVM') }}
        </a-button>
        <a-divider class="divider-small" />
        <a-list
          size="small"
          itemLayout="vertical"
          :dataSource="vm.nic"
          class="list"
          :loading="loadingNic"
        >
          <a-list-item slot="renderItem" slot-scope="item" class="list-item">
            <a-list-item-meta>
              <div slot="avatar">
                <a-avatar slot="avatar">
                  <a-icon type="wifi" />
                </a-avatar>
              </div>
              <div slot="title" class="title">
                <router-link :to="{ path: '/guestnetwork/' + item.networkid }">{{ item.networkname }} </router-link>
                <div class="title__details">
                  <div class="actions">
                    <a-popconfirm
                      title="Please confirm that you would like to make this NIC the default for this VM."
                      @confirm="setAsDefault(item)"
                      okText="Yes"
                      cancelText="No"
                      v-if="!item.isdefault"
                    >
                      <a-button
                        icon="check-square"
                        size="small"
                        shape="round" />
                    </a-popconfirm>
                    <a-tooltip placement="bottom" v-if="item.type !== 'L2'">
                      <template slot="title">
                        {{ "Change IP Address" }}
                      </template>
                      <a-button
                        icon="swap"
                        size="small"
                        shape="round"
                        @click="editIpAddressNic = item.id; showUpdateIpModal = true" />
                    </a-tooltip>
                    <a-tooltip placement="bottom" v-if="item.type !== 'L2'">
                      <template slot="title">
                        {{ "Manage Secondary IP Addresses" }}
                      </template>
                      <a-button
                        icon="environment"
                        size="small"
                        shape="round"
                        @click="fetchSecondaryIPs(item.id)" />
                    </a-tooltip>
                    <a-popconfirm
                      :title="$t('message.network.removeNIC')"
                      @confirm="removeNIC(item)"
                      okText="Yes"
                      cancelText="No"
                      v-if="!item.isdefault"
                    >
                      <a-button
                        type="danger"
                        icon="delete"
                        size="small"
                        shape="round" />
                    </a-popconfirm>
                  </div>
                  <div class="tags">
                    <a-tag v-if="item.isdefault">
                      {{ $t('default') }}
                    </a-tag>
                    <a-tag v-if="item.type">
                      {{ item.type }}
                    </a-tag>
                    <a-tag v-if="item.broadcasturi">
                      {{ item.broadcasturi }}
                    </a-tag>
                    <a-tag v-if="item.isolationuri">
                      {{ item.isolationuri }}
                    </a-tag>
                  </div>
                </div>
              </div>
            </a-list-item-meta>
            <div>
              <a-divider class="divider-small" />
              <div class="attribute">
                <div class="label">{{ $t('macaddress') }}</div>
                <div>{{ item.macaddress }}</div>
              </div>
              <div class="attribute" v-if="item.ipaddress">
                <div class="label">{{ $t('IP Address') }}</div>
                <div>{{ item.ipaddress }}</div>
              </div>
              <div class="attribute" v-if="item.secondaryip && item.secondaryip.length > 0 && item.type !== 'L2'">
                <div class="label">{{ $t('Secondary IPs') }}</div>
                <div>{{ item.secondaryip.map(x => x.ipaddress).join(', ') }}</div>
              </div>
              <div class="attribute" v-if="item.netmask">
                <div class="label">{{ $t('netmask') }}</div>
                <div>{{ item.netmask }}</div>
              </div>
              <div class="attribute" v-if="item.gateway">
                <div class="label">{{ $t('gateway') }}</div>
                <div>{{ item.gateway }}</div>
              </div>
              <div class="attribute">
                <div class="label">{{ $t('id') }}</div>
                <div><a-icon type="barcode"/> {{ item.id }}</div>
              </div>
            </div>
          </a-list-item>
        </a-list>
      </a-collapse-panel>
    </a-collapse>

    <a-modal
      :visible="showAddNetworkModal"
      :title="$t('label.network.addVM')"
      @cancel="closeModals"
      @ok="submitAddNetwork">
      {{ $t('message.network.addVM.desc') }}

      <div class="modal-form">
        <p class="modal-form__label">{{ $t('Network') }}:</p>
        <a-select :defaultValue="addNetworkData.network" @change="e => addNetworkData.network === e">
          <a-select-option
            v-for="network in addNetworkData.allNetworks"
            :key="network.id"
            :value="network.id">
            {{ network.name }}
          </a-select-option>
        </a-select>
        <p class="modal-form__label">{{ $t('publicip') }}:</p>
        <a-input v-model="addNetworkData.ip"></a-input>
      </div>

    </a-modal>

    <a-modal
      :visible="showUpdateIpModal"
      :title="$t('label.change.ipaddress')"
      @cancel="closeModals"
      @ok="submitUpdateIP"
    >
      {{ $t('message.network.updateIp') }}

      <div class="modal-form">
        <p class="modal-form__label">{{ $t('publicip') }}:</p>
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
        {{ $t('message.network.secondaryIP') }}
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
              size="small"
              icon="delete" />
            {{ ip.ipaddress }}
          </a-popconfirm>
        </a-list-item>
      </a-list>
    </a-modal>

  </div>
</template>

<script>

import { api } from '@/api'
import ResourceLayout from '@/layouts/ResourceLayout'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceHardware',
  components: {
    ResourceLayout,
    Status
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
  inject: ['parentFetchData'],
  data () {
    return {
      vm: {},
      volumes: [],
      totalStorage: 0,
      activeKey: ['1', '2', '3'],
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
      newSecondaryIp: ''
    }
  },
  created () {
    this.vm = this.resource
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.vm = newItem
      if (newItem.id === oldItem.id) {
        return
      }
      this.fetchData()
    }
  },
  methods: {
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
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
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.errorresponse.errortext
          })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.addiptovmnicresponse.errortext
        })
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
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
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
