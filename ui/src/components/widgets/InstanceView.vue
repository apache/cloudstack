<template>
  <div style="padding-top: 12px">
    <a-row>
      <a-col :span="24">
        <h2>{{ vm.displayname }} ({{ vm.name }})</h2>
      </a-col>
      <a-col :span="5">
        <a :href="'/client/console?cmd=access&vm=' + vm.id" target="_blank">
          <a-avatar shape="square" :size="200"
            style="color: #fff; backgroundColor: #333; width: 175px; height: 120px;">
            <span style="text-align: bottom">
            <a-icon type="right-square"/>
            {{ vm.instancename }}
            </span>
          </a-avatar>
        </a>
      </a-col>
      <a-col :span="15">
        <p>
          ID: {{ vm.id }} <br/>
          State: {{ vm.state }} <br/>
          Guest OS: {{ guestOsName }} <br/>
          Offering: <router-link :to="{ path: '/computeoffering/' + vm.serviceofferingid }">{{ vm.serviceofferingname }}</router-link> <br/>
          Template: <router-link :to="{ path: '/template/' + vm.templateid }">{{ vm.templatename }}</router-link> <br/>
          Host: <router-link :to="{ path: '/host/' + vm.hostid }">{{ vm.hostname }}</router-link> ({{ vm.hypervisor }}) <br/>
          Zone: <router-link :to="{ path: '/zone/' + vm.zoneid }">{{ vm.zonename }}</router-link> <br/>

          IP Addresses: <ul>
            <li v-for="eth in vm.nic">
              {{ eth.ipaddress }} <router-link :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link> <br/>
            </li>
          </ul>
        </p>
      </a-col>
      <a-col :span="4">
        <p>
          <font-awesome-icon :icon="['fas', 'microchip']" />
          CPU: {{ vm.cpunumber }} x {{ vm.cpuspeed }} Mhz <br/>
          <font-awesome-icon :icon="['fas', 'memory']" />
          Memory: {{ vm.memory }} MiB <br/>
          <font-awesome-icon :icon="['fas', 'database']" />
          Storage: {{ (totalStorage / (1024 * 1024 * 1024.0)).toFixed(2) }} GiB <br/>
        </p>
      </a-col>

      <a-col :span="16">
        <a-card title="VM Hardware">
          <a-collapse>
            <a-collapse-panel :header="'CPU: ' + vm.cpunumber">
              <p>{{ vm.cpunumber }} CPU(s) x {{ vm.cpuspeed }} Mhz</p>
            </a-collapse-panel>
            <a-collapse-panel :header="'Memory: ' + vm.memory + ' MB'">
              <p>Total Memory: {{ vm.memory }} MiB<br/>Free Memory: {{ vm.memoryintfreekbs }} kBs</p>
            </a-collapse-panel>
            <a-collapse-panel :header="'Storage: ' + volumes.length" >
              <a-list
                itemLayout="horizontal"
                :dataSource="volumes"
              >
                <a-list-item slot="renderItem" slot-scope="item, index">
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
                <a-list-item slot="renderItem" slot-scope="item, index">
                  <a-list-item-meta :description="item.id">
                    <a slot="title" href="https://vue.ant.design/">{{item.ipaddress}} <span v-show="item.isdefault">(Default)</span></a>
                    <a-avatar slot="avatar">
                      <font-awesome-icon :icon="['fas', 'ethernet']" />
                    </a-avatar>
                  </a-list-item-meta>
                  <p>
                    Network: <router-link :to="{ path: '/guestnetwork/' + item.networkid }">{{ item.networkname }}</router-link> <br/>
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


    </a-row>

  </div>
</template>

<script>

import { api } from '@/api'

export default {
  name: 'InstanceView',
  components: {
  },
  props: {
    vm: {
      type: Object,
      required: true,
      default: {}
    }
  },
  data () {
    return {
      volumes: [],
      totalStorage: 0,
      guestOsName: ''
    }
  },
  watch: {
    vm: function (newVm, oldVm) {
      this.fetchData()
    }
  },
  methods: {
    fetchData() {
      api('listVolumes', { 'listall': true, 'virtualmachineid': this.vm.id }).then(json => {
        this.volumes = json.listvolumesresponse.volume
        this.totalStorage = 0
        for (var volume of this.volumes) {
          this.totalStorage += volume.size
        }
      })
      api('listOsTypes', { 'id': this.vm.ostypeid }).then(json => {
        this.guestOsName = json.listostypesresponse.ostype[0].description
      })
    }
  }
}
</script>

<style scoped>
</style>
