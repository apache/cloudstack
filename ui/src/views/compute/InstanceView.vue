<template>
  <div style="padding-top: 12px" class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="8" style="margin-bottom: 12px">
        <info-card :resource="resource" resourceType="UserVm" showNotes>
          <div slot="avatar">
            <font-awesome-icon :icon="['fab', osLogo]" size="4x" style="color: #666;" />
          </div>
          <div slot="name">
            <h4>{{ vm.name }}
              <a :href="'/client/console?cmd=access&vm=' + vm.id" target="_blank">
                <a-button shape="circle" >
                  <a-icon type="right-square" />
                </a-button>
              </a>
            </h4>
            <div>
              <a-tag>{{ vm.instancename }}</a-tag>
              <a-tag :color="vm.haenable ? 'green': 'red'">HA</a-tag>
              <a-tag :color="vm.isdynamicallyscalable ? 'green': 'red'">Dynamic Scalable</a-tag>
            </div>
          </div>
          <div slot="details">
            <div class="vm-detail">
              <font-awesome-icon :icon="['fab', osLogo]" size="lg"/>
              <span style="margin-left: 8px">{{ guestOsName }}</span>
            </div>
            <div class="vm-detail">
              <font-awesome-icon :icon="['fas', 'microchip']" />
              <span class="vm-detail">{{ vm.cpunumber }} CPU x {{ vm.cpuspeed }} Mhz
              </span>
              <a-progress style="padding-left: 25px" size="small" :percent="parseFloat(vm.cpuused)" />
            </div>
            <div class="vm-detail">
              <font-awesome-icon :icon="['fas', 'memory']" style="margin-left: -2px"/>
              <span class="vm-detail">{{ vm.memory }} MB Memory
              </span>
              <a-progress style="padding-left: 25px" size="small" :percent="parseFloat(100.0 * (vm.memorykbs - vm.memoryintfreekbs) / vm.memorykbs).toFixed(2)" />
            </div>
            <div class="vm-detail">
              <font-awesome-icon :icon="['fas', 'database']" />
              <span class="vm-detail" style="margin-left: 12px">{{ (totalStorage / (1024 * 1024 * 1024.0)).toFixed(2) }} GB Storage
              </span>
              <div style="margin-left: 25px">
                <a-tag><a-icon type="download" /> Read {{ vm.diskkbsread }} KB</a-tag>
                <a-tag><a-icon type="upload" /> Write {{ vm.diskkbswrite }} KB</a-tag><br/>
                <a-tag><a-icon type="download" /> Read (IO) {{ vm.diskioread }}</a-tag>
                <a-tag><a-icon type="upload" /> Write (IO) {{ vm.diskiowrite }}</a-tag>
              </div>
            </div>
            <div class="vm-detail">
              <font-awesome-icon :icon="['fas', 'ethernet']" />
              <span class="vm-detail">{{ vm.nic.length }} NIC(s)
                <a-tag><a-icon type="arrow-down" /> RX {{ vm.networkkbsread }} KB</a-tag>
                <a-tag><a-icon type="arrow-up" /> TX {{ vm.networkkbswrite }} KB</a-tag>
              </span>
              <div style="margin-left: 25px" v-for="(eth, index) in vm.nic" :key="eth.id">
                <a-icon type="api"/> eth{{ index }} {{ eth.ipaddress }}
                (<router-link :to="{ path: '/guestnetwork/' + eth.networkid }">{{ eth.networkname }}</router-link>)
              </div>
            </div>
          </div>
        </info-card>
      </a-col>
      <a-col :md="24" :lg="16">
        <a-card
          style="width:100%"
          title="Hardware"
          :bordered="true"
        >
          <a-collapse v-model="activeKey">
            <a-collapse-panel :header="'ISO: ' + vm.isoname" v-if="vm.isoid" key="1">
              <a-list
                itemLayout="horizontal">
                <a-list-item>
                  <a-list-item-meta :description="vm.isoid">
                    <a slot="title" href="">
                      <router-link :to="{ path: '/iso/' + vm.isoid }">{{ vm.isoname }}</router-link>
                    </a> ({{ vm.isoname }})
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'compact-disc']" />
                    </a-avatar>
                  </a-list-item-meta>
                </a-list-item>
              </a-list>
            </a-collapse-panel>

            <a-collapse-panel :header="'Disks: ' + volumes.length" key="2">
              <a-list
                size="small"
                itemLayout="horizontal"
                :dataSource="volumes"
              >
                <a-list-item slot="renderItem" slot-scope="item">
                  <a-list-item-meta>
                    <div slot="title">
                      <router-link :to="{ path: '/volume/' + item.id }">{{ item.name }}</router-link> ({{ item.type }}) <br/>
                      <status :text="item.state" displayText /><br/>
                    </div>
                    <div slot="description">
                      <a-icon type="barcode"/> {{ item.id }}
                    </div>
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'database']" />
                    </a-avatar>
                  </a-list-item-meta>
                  <p>
                    Size: {{ (item.size / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
                    Physical Size: {{ (item.physicalsize / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
                    Provisioning: {{ item.provisioningtype }}<br/>
                    Storage Pool: {{ item.storage }} ({{ item.storagetype }})<br/>
                  </p>
                </a-list-item>
              </a-list>

            </a-collapse-panel>
            <a-collapse-panel :header="'Network Adapter(s): ' + (vm && vm.nic ? vm.nic.length : 0)" key="3" >
              <a-list
                size="small"
                itemLayout="horizontal"
                :dataSource="vm.nic"
              >
                <a-list-item slot="renderItem" slot-scope="item">
                  <a-list-item-meta>
                    <div slot="title">
                      <span v-show="item.isdefault">(Default) </span>
                      <router-link :to="{ path: '/guestnetwork/' + item.networkid }">{{ item.networkname }} </router-link><br/>
                      Mac Address: {{ item.macaddress }}<br/>
                      <span v-if="item.ipaddress">Address: {{ item.ipaddress }} <br/></span>
                      Netmask: {{ item.netmask }}<br/>
                      Gateway: {{ item.gateway }}<br/>
                    </div>
                    <div slot="description">
                      <a-icon type="barcode"/> {{ item.id }}
                    </div>
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'ethernet']" />
                    </a-avatar>
                  </a-list-item-meta>
                  <p>
                    Type: {{ item.type }}<br/>
                    Broadcast URI: {{ item.broadcasturi }}<br/>
                    Isolation URI: {{ item.isolationuri }}<br/>
                  </p>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
        <a-card
          style="width:100%; margin-top: 12px"
          title="Settings"
          :bordered="true"
        >
          <list-view
            :columns="settingsColumns"
            :items="settings " />
        </a-card>
      </a-col>
    </a-row>

  </div>
</template>

<script>

import { api } from '@/api'
import InfoCard from '@/views/common/InfoCard'
import ListView from '@/components/widgets/ListView'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceView',
  components: {
    InfoCard,
    ListView,
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      vm: {},
      volumes: [],
      totalStorage: 0,
      guestOsName: '',
      osLogo: 'linux',
      activeKey: ['1', '2', '3'],
      settingsColumns: [
        {
          title: this.$t('name'),
          dataIndex: 'name',
          sorter: true
        }, {
          title: this.$t('value'),
          dataIndex: 'value'
        }, {
          title: this.$t('action'),
          dataIndex: 'actions'
        }
      ],
      settings: []
    }
  },
  created () {
    this.vm = this.resource
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.vm = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      api('listVolumes', { 'listall': true, 'virtualmachineid': this.vm.id }).then(json => {
        this.volumes = json.listvolumesresponse.volume
        this.totalStorage = 0
        for (var volume of this.volumes) {
          this.totalStorage += volume.size
        }
      })
      api('listOsTypes', { 'id': this.vm.ostypeid }).then(json => {
        this.guestOsName = json.listostypesresponse.ostype[0].description
        const osname = this.guestOsName.toLowerCase()
        if (osname.includes('centos')) {
          this.osLogo = 'centos'
        } else if (osname.includes('ubuntu')) {
          this.osLogo = 'ubuntu'
        } else if (osname.includes('suse')) {
          this.osLogo = 'suse'
        } else if (osname.includes('redhat')) {
          this.osLogo = 'redhat'
        } else if (osname.includes('fedora')) {
          this.osLogo = 'fedora'
        } else if (osname.includes('linux')) {
          this.osLogo = 'linux'
        } else if (osname.includes('bsd')) {
          this.osLogo = 'freebsd'
        } else if (osname.includes('apple')) {
          this.osLogo = 'apple'
        } else if (osname.includes('window') || osname.includes('dos')) {
          this.osLogo = 'windows'
        } else if (osname.includes('oracle')) {
          this.osLogo = 'java'
        } else {
          this.osLogo = 'linux'
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
  .vm-detail {
    margin-bottom: 8px;
    margin-left: 10px;
    margin-right: 10px;
  }
}
</style>
