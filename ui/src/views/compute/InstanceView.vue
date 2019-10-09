<template>
  <div style="padding-top: 12px" class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="7" style="margin-bottom: 12px">
        <a-card :bordered="true">
          <div class="account-center-avatarHolder">
            <div class="avatar">
              <font-awesome-icon :icon="['fab', osLogo]" size="4x" style="color: #666;" />
            </div>
            <div class="username">{{ vm.name }}
              <a :href="'/client/console?cmd=access&vm=' + vm.id" target="_blank">
                <a-button shape="circle" >
                  <a-icon type="right-square" />
                </a-button>
              </a>

            </div>
            <div class="bio">
              <a-tag>{{ vm.instancename }}</a-tag>
              <a-tag :color="vm.haenable ? 'green': 'red'">HA</a-tag>
              <a-tag :color="vm.isdynamicallyscalable ? 'green': 'red'">Dynamic Scalable</a-tag>
            </div>
          </div>
          <div class="account-center-detail">
            <p>
              <status :text="vm.state ? vm.state : ''" style="padding-left: 8px; padding-right: 5px"/>{{ vm.state }}
            </p>
            <p>
              <font-awesome-icon :icon="['fab', osLogo]" size="lg"/> {{ guestOsName }} <br/>
            </p>
            <p>
              <font-awesome-icon :icon="['fas', 'microchip']" />
              {{ vm.cpunumber }} CPU x {{ vm.cpuspeed }} Mhz
              (<router-link :to="{ path: '/computeoffering/' + vm.serviceofferingid }">{{ vm.serviceofferingname }}</router-link>)
              <a-progress style="padding-left: 32px" size="small" :percent="parseFloat(vm.cpuused)" />
            </p>
            <p>
              <font-awesome-icon :icon="['fas', 'memory']" />
              {{ vm.memory }} MB Memory
              <a-progress style="padding-left: 32px" size="small" :percent="parseFloat(100.0 * (vm.memorykbs - vm.memoryintfreekbs) / vm.memorykbs).toFixed(2)" />
            </p>
            <p>
              <font-awesome-icon :icon="['fas', 'database']" />
              {{ (totalStorage / (1024 * 1024 * 1024.0)).toFixed(2) }} GB Storage
              (<router-link :to="{ path: '/template/' + vm.templateid }">{{ vm.templatename }}</router-link>)<br/>
              <span style="padding-left: 32px">
                <a-tag color="green">Disk Read {{ vm.diskkbsread }} KB</a-tag>
                <a-tag color="blue">Disk Write {{ vm.diskkbswrite }} KB</a-tag>
              </span><br/>
              <span style="padding-left: 32px">
                <a-tag color="green">Disk Read (IO) {{ vm.diskioread }}</a-tag>
                <a-tag color="blue">Disk Write (IO) {{ vm.diskiowrite }}</a-tag>
              </span>
            </p>
            <p>
              <font-awesome-icon :icon="['fas', 'ethernet']" />
              <span>
                {{ vm && vm.nic ? vm.nic.length : 0 }} NIC(s):
                <a-tag color="green"><a-icon type="arrow-down" /> RX {{ vm.networkkbsread }} KB</a-tag>
                <a-tag color="blue"><a-icon type="arrow-up" /> TX {{ vm.networkkbswrite }} KB</a-tag>
              </span>
              <br/>
              <span style="padding-left: 34px" v-for="eth in vm.nic" :key="eth.id">
                {{ eth.ipaddress }} <router-link :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link> <br/>
              </span>
            </p>
            <p v-if="vm.group">
              <a-icon type="team" style="margin-left: 6px; margin-right: 10px" />
              {{ vm.group }}
            </p>

            <p v-if="vm.hostid">
              <a-icon type="desktop" style="margin-left: 6px; margin-right: 12px" />
              <router-link :to="{ path: '/host/' + vm.hostid }">{{ vm.hostname }}</router-link> ({{ vm.hypervisor }})
            </p>
            <p>
              <a-icon type="global" style="margin-left: 6px; margin-right: 12px" />
              <router-link :to="{ path: '/zone/' + vm.zoneid }">{{ vm.zonename }}</router-link>
            </p>
            <p>
              <a-icon type="user" style="margin-left: 6px; margin-right: 12px" />
              <router-link :to="{ path: '/account?name=' + vm.account }">{{ vm.account }}</router-link>
            </p>
            <p>
              <a-icon type="block" style="margin-left: 6px; margin-right: 12px" />
              <router-link :to="{ path: '/domain/' + vm.domainid }">{{ vm.domain }}</router-link>
            </p>
            <p>
              <a-icon type="calendar" style="margin-left: 6px; margin-right: 8px" /> {{ vm.created }}
            </p>
            <p>
              <a-icon type="barcode" style="margin-left: 6px; margin-right: 8px" /> {{ vm.id }}
            </p>
          </div>
          <a-divider/>

          <div class="account-center-tags">
            <div class="tagsTitle">Tags</div>
            <div>
              <a-tag closable>key=value</a-tag>

              <template v-for="(tag, index) in tags">
                <a-tooltip v-if="tag.length > 20" :key="tag" :title="tag">
                  <a-tag :key="tag" :closable="index !== 0" :afterClose="() => handleClose(tag)">
                    {{ `${tag.slice(0, 20)}...` }}
                  </a-tag>
                </a-tooltip>
                <a-tag v-else :key="tag" :closable="index !== 0" :afterClose="() => handleClose(tag)">
                  {{ tag }}
                </a-tag>
              </template>

              <a-input
                v-if="inputVisible"
                ref="input"
                type="text"
                size="small"
                :style="{ width: '78px' }"
                :value="inputValue"
                @change="handleInputChange"
                @blur="handleInputConfirm"
                @keyup.enter="handleInputConfirm"
              />
              <a-tag v-else @click="showInput" style="background: #fff; borderStyle: dashed;">
                <a-icon type="plus" /> New Tag
              </a-tag>
            </div>
          </div>
          <a-divider :dashed="true"/>

          <div class="account-center-team">
            <div class="teamTitle">Notes</div>
            <a-comment>
              <a-avatar
                slot="avatar"
                icon="edit"
              />
              <div slot="content">
                <a-form-item>
                  <a-textarea :rows="4" ></a-textarea>
                </a-form-item>
                <a-form-item>
                  <a-button
                    htmlType="submit"
                    type="primary"
                  >
                    Add Note
                  </a-button>
                </a-form-item>
              </div>
            </a-comment>

          </div>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="17">
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
                      <a-icon type="barcode"/> <router-link :to="{ path: '/volume/' + item.id }"> {{ item.id }}</router-link>
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
                      <a-icon type="barcode"/> {{ item.id}}
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
import ListView from '@/components/widgets/ListView'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceView',
  components: {
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
      inputVisible: false,
      inputValue: '',
      activeKey: ['1', '2', '3'],
      tags: ['os=centos', 'tag=value', 'demo=true'],
      tagInputVisible: false,
      tagInputValue: '',
      teams: [],
      teamSpinning: true,
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
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },

    handleInputChange (e) {
      this.inputValue = e.target.value
    },
    handleInputConfirm () {
      const inputValue = this.inputValue
      let tags = this.tags
      if (inputValue && tags.indexOf(inputValue) === -1) {
        tags = [...tags, inputValue]
      }
      console.log(tags)
      Object.assign(this, {
        tags,
        inputVisible: false,
        inputValue: ''
      })
    },

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
  .account-center-avatarHolder {
    text-align: center;
    margin-bottom: 24px;
    & > .avatar {
      margin: 0 auto;
      padding-top: 20px;
      width: 104px;
      //height: 104px;
      margin-bottom: 20px;
      border-radius: 50%;
      overflow: hidden;
      img {
        height: 100%;
        width: 100%;
      }
    }
    .username {
      color: rgba(0, 0, 0, 0.85);
      font-size: 20px;
      line-height: 28px;
      font-weight: 500;
      margin-bottom: 4px;
    }
  }
  .account-center-detail {
    p {
      margin-bottom: 8px;
      padding-left: 12px;
      position: relative;

      font-awesome-icon, .svg-inline--fa {
        width: 30px;
      }
    }
    .title {
      background-position: 0 0;
    }
    .group {
      background-position: 0 -22px;
    }
    .address {
      background-position: 0 -44px;
    }
  }
  .account-center-tags {
    .ant-tag {
      margin-bottom: 8px;
    }
  }
  .account-center-team {
    .members {
      a {
        display: block;
        margin: 12px 0;
        line-height: 24px;
        height: 24px;
        .member {
          font-size: 14px;
          color: rgba(0, 0, 0, 0.65);
          line-height: 24px;
          max-width: 100px;
          vertical-align: top;
          margin-left: 12px;
          transition: all 0.3s;
          display: inline-block;
        }
        &:hover {
          span {
            color: #1890ff;
          }
        }
      }
    }
  }
  .tagsTitle,
  .teamTitle {
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 12px;
  }
}
</style>
