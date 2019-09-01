<template>
  <div style="padding-top: 12px">
    <a-row :gutter="12">
      <a-col :span="24">
        <h2>
          <font-awesome-icon :icon="['fab', osLogo]" size="lg" style="color: #666;" />
          {{ vm.name }} <span v-if="vm.instancename">({{ vm.instancename }})</span>
          <a :href="'/client/console?cmd=access&vm=' + vm.id" target="_blank">
            <a-button shape="circle">
              <a-icon type="right-square" />
            </a-button>
          </a>
        </h2>
      </a-col>
      <a-col :span="8">
        <p>
          <status :text="vm.state" displayText /> <br/>
          <font-awesome-icon :icon="['fab', osLogo]" size="lg" style="color: #666;" /> {{ guestOsName }} <br/>
          <font-awesome-icon :icon="['fas', 'microchip']" />
          CPU: {{ vm.cpunumber }} x {{ vm.cpuspeed }} Mhz <br/>
          <font-awesome-icon :icon="['fas', 'memory']" />
          Memory: {{ vm.memory }} MiB <br/>
          <font-awesome-icon :icon="['fas', 'database']" />
          Storage: {{ (totalStorage / (1024 * 1024 * 1024.0)).toFixed(2) }} GiB <br/>
          <font-awesome-icon :icon="['fas', 'ethernet']" /> {{ vm.nic.length }} NIC(s): <br/>
          <span v-for="eth in vm.nic" :key="eth.id">
            {{ eth.ipaddress }} <router-link :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link> <br/>
          </span>
        </p>
      </a-col>

      <a-col :span="8">
        <p>
          HA: {{ vm.haenable }} <br />
          Dynamic Scalable: {{ vm.isdynamicallyscalable }} <br />
          Offering: <router-link :to="{ path: '/computeoffering/' + vm.serviceofferingid }">{{ vm.serviceofferingname }}</router-link> <br/>
          Template: <router-link :to="{ path: '/template/' + vm.templateid }">{{ vm.templatename }}</router-link> <br/>
          Host: <router-link :to="{ path: '/host/' + vm.hostid }">{{ vm.hostname }}</router-link> ({{ vm.hypervisor }}) <br/>
          Zone: <router-link :to="{ path: '/zone/' + vm.zoneid }">{{ vm.zonename }}</router-link> <br/>
          Account: <router-link :to="{ path: '/account?name=' + vm.account }">{{ vm.account }}</router-link> <br />
          Domain: <router-link :to="{ path: '/domain/' + vm.domainid }">{{ vm.domain }}</router-link> <br/>
          Created: {{ vm.created }} <br />

        </p>
      </a-col>

      <a-col :span="8">

        <a-tag closable>key=value</a-tag>
        <a-tag color="green">green</a-tag>
        <a-tag color="red">red</a-tag>
        <a-tag color="blue">blue</a-tag>

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

      </a-col>

      <a-col :span="16">
        <a-card title="VM Hardware">
          <a-collapse>
            <a-collapse-panel :header="'Storage: ' + volumes.length" >
              <a-list
                itemLayout="horizontal"
                :dataSource="volumes"
              >
                <a-list-item slot="renderItem" slot-scope="item">
                  <a-list-item-meta :description="item.id">
                    <a slot="title" href="">
                      <router-link :to="{ path: '/volume/' + item.id }">{{ item.name }}</router-link>
                    </a> ({{ item.type }})
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'database']" />
                    </a-avatar>
                  </a-list-item-meta>
                  <p>
                    State: {{ item.state }}<br/>
                    Type: {{ item.type }}<br/>
                    Size: {{ (item.size / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
                    Physical Size: {{ (item.physicalsize / (1024 * 1024 * 1024.0)).toFixed(4) }} GB<br/>
                    Provisioning: {{ item.provisioningtype }}<br/>
                    Storage Pool: {{ item.storage }} ({{ item.storagetype }})<br/>
                  </p>
                </a-list-item>
              </a-list>

            </a-collapse-panel>
            <a-collapse-panel :header="'Network Adapter(s): ' + vm.nic.length" >
              <a-list
                itemLayout="horizontal"
                :dataSource="vm.nic"
              >
                <a-list-item slot="renderItem" slot-scope="item">
                  <a-list-item-meta :description="item.id">
                    <a slot="title" href="https://vue.ant.design/">{{ item.ipaddress }} <span v-show="item.isdefault">(Default)</span></a>
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'ethernet']" />
                    </a-avatar>
                  </a-list-item-meta>
                  <p>
                    Network: <router-link :to="{ path: '/guestnetwork/' + item.networkid }">{{ item.networkname }}</router-link> <br/>
                    Type: {{ item.type }}<br/>
                    Mac Address: {{ item.macaddress }}<br/>
                    Netmask: {{ item.netmask }}<br/>
                    Gateway: {{ item.gateway }}<br/>
                    Broadcast URI: {{ item.broadcasturi }}<br/>
                    Isolation URI: {{ item.isolationuri }}<br/>
                  </p>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>

      <a-col :span="8">
        Notes
        <a-comment>
          <a-avatar
            slot="avatar"
            icon="cloud"
            alt="Han Solo"
          />
          <div slot="content">
            <a-form-item>
              <a-textarea :rows="4" @change="handleChange" :value="value" ></a-textarea>
            </a-form-item>
            <a-form-item>
              <a-button
                htmlType="submit"
                :loading="submitting"
                @click="handleSubmit"
                type="primary"
              >
                Add Note
              </a-button>
            </a-form-item>
          </div>
        </a-comment>

      </a-col>

    </a-row>

  </div>
</template>

<script>

import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'InstanceView',
  components: {
    Status
  },
  props: {
    vm: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      volumes: [],
      totalStorage: 0,
      guestOsName: '',
      osLogo: 'linux',
      tags: [],
      inputVisible: false,
      inputValue: ''
    }
  },
  watch: {
    vm: function (newVm, oldVm) {
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
          this.osLogo = 'cloud'
        }
      })
    }
  }
}
</script>

<style scoped>
</style>
