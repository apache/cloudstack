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
  <a-alert type="error" v-if="['vm', 'systemvm', 'router', 'ilbvm'].includes($route.meta.name) && 'hostcontrolstate' in resource && resource.hostcontrolstate !== 'Enabled'">
    <template #message>
      <div class="title">
        {{ $t('message.host.controlstate') }} {{ resource.hostcontrolstate }}. {{ $t('message.host.controlstate.retry') }}
      </div>
    </template>
  </a-alert>
  <a-alert v-if="ip6routes" type="info" :showIcon="true" :message="$t('label.add.upstream.ipv6.routes')">
    <template #description>
      <p v-html="ip6routes" />
    </template>
  </a-alert>
  <a-list
    size="small"
    :dataSource="fetchDetails()">
    <template #renderItem="{item}">
      <a-list-item v-if="item in dataResource && !customDisplayItems.includes(item)">
        <div>
          <strong>{{ item === 'service' ? $t('label.supportedservices') : $t('label.' + String(item).toLowerCase()) }}</strong>
          <br/>
          <div v-if="Array.isArray(dataResource[item]) && item === 'service'">
            <div v-for="(service, idx) in dataResource[item]" :key="idx">
              {{ service.name }} : {{ service.provider[0].name }}
            </div>
          </div>
          <div v-else-if="$route.meta.name === 'backup' && item === 'volumes'">
            <div v-for="(volume, idx) in JSON.parse(dataResource[item])" :key="idx">
              <router-link :to="{ path: '/volume/' + volume.uuid }">{{ volume.type }} - {{ volume.path }}</router-link> ({{ parseFloat(volume.size / (1024.0 * 1024.0 * 1024.0)).toFixed(1) }} GB)
            </div>
          </div>
          <div v-else-if="$route.meta.name === 'computeoffering' && item === 'rootdisksize'">
            <div>
              {{ dataResource.rootdisksize }} GB
            </div>
          </div>
          <div v-else-if="['name', 'type'].includes(item)">
            <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(dataResource[item])">{{ $t(dataResource[item].toLowerCase()) }}</span>
            <span v-else>{{ dataResource[item] }}</span>
          </div>
          <div v-else-if="['created', 'sent', 'lastannotated', 'collectiontime', 'lastboottime', 'lastserverstart', 'lastserverstop'].includes(item)">
            {{ $toLocaleDate(dataResource[item]) }}
          </div>
          <div v-else-if="$route.meta.name === 'userdata' && item === 'userdata'">
            <div style="white-space: pre-wrap;"> {{ decodeUserData(dataResource.userdata)}} </div>
          </div>
          <div v-else-if="$route.meta.name === 'guestnetwork' && item === 'egressdefaultpolicy'">
            {{ dataResource[item]? $t('message.egress.rules.allow') : $t('message.egress.rules.deny') }}
          </div>
          <div v-else-if="item === 'securitygroup'">
            <div v-if="dataResource[item] && dataResource[item].length > 0">
              <span v-for="(securityGroup, idx) in dataResource[item]" :key="idx">
                {{ securityGroup.name }} &nbsp;
              </span>
            </div>
          </div>
          <div v-else>{{ dataResource[item] }}</div>
        </div>
      </a-list-item>
      <a-list-item v-else-if="item === 'ip6address' && ipV6Address && ipV6Address.length > 0">
        <div>
          <strong>{{ $t('label.' + String(item).toLowerCase()) }}</strong>
          <br/>
          <div>{{ ipV6Address }}</div>
        </div>
      </a-list-item>
      <a-list-item v-else-if="(item === 'privatemtu' && !['L2', 'Shared'].includes(dataResource['type'])) || (item === 'publicmtu' && dataResource['type'] !== 'L2')">
        <div>
          <strong>{{ $t('label.' + String(item).toLowerCase()) }}</strong>
          <br/>
          <div>{{ dataResource[item] }}</div>
        </div>
      </a-list-item>
    </template>
    <HostInfo :resource="dataResource" v-if="$route.meta.name === 'host' && 'listHosts' in $store.getters.apis" />
    <DedicateData :resource="dataResource" v-if="dedicatedSectionActive" />
    <VmwareData :resource="dataResource" v-if="$route.meta.name === 'zone' && 'listVmwareDcs' in $store.getters.apis" />
  </a-list>
</template>

<script>
import DedicateData from './DedicateData'
import HostInfo from '@/views/infra/HostInfo'
import VmwareData from './VmwareData'

export default {
  name: 'DetailsTab',
  components: {
    DedicateData,
    HostInfo,
    VmwareData
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    items: {
      type: Object,
      default: () => {}
    },
    loading: {
      type: Boolean,
      default: false
    },
    bordered: {
      type: Boolean,
      default: false
    },
    tab: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      dedicatedRoutes: ['zone', 'pod', 'cluster', 'host'],
      dedicatedSectionActive: false,
      projectname: '',
      dataResource: {}
    }
  },
  mounted () {
    this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
  },
  computed: {
    customDisplayItems () {
      return ['ip6routes', 'privatemtu', 'publicmtu']
    },
    ipV6Address () {
      if (this.dataResource.nic && this.dataResource.nic.length > 0) {
        return this.dataResource.nic.filter(e => { return e.ip6address }).map(e => { return e.ip6address }).join(', ')
      }

      return null
    },
    ip6routes () {
      if (this.resource.ip6routes && this.resource.ip6routes.length > 0) {
        var routes = []
        for (var route of this.resource.ip6routes) {
          routes.push(route.subnet + ' via ' + route.gateway)
        }
        return routes.join('<br>')
      }
      return null
    }
  },
  created () {
    this.dataResource = this.resource
    this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.dataResource = this.resource
        if ('account' in this.dataResource && this.dataResource.account.startsWith('PrjAcct-')) {
          this.projectname = this.dataResource.account.substring(this.dataResource.account.indexOf('-') + 1, this.dataResource.account.lastIndexOf('-'))
          this.dataResource.projectname = this.projectname
        }
      }
    },
    $route () {
      this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
      this.fetchProjectAdmins()
    }
  },
  methods: {
    decodeUserData (userdata) {
      const decodedData = Buffer.from(userdata, 'base64')
      return decodedData.toString('utf-8')
    },
    fetchProjectAdmins () {
      if (!this.dataResource.owner) {
        return false
      }
      var owners = this.dataResource.owner
      var projectAdmins = []
      for (var owner of owners) {
        projectAdmins.push(Object.keys(owner).includes('user') ? owner.account + '(' + owner.user + ')' : owner.account)
      }
      this.dataResource.account = projectAdmins.join()
    },
    fetchDetails () {
      var details = this.$route.meta.details
      if (typeof details === 'function') {
        details = details()
      }
      details = this.projectname ? [...details.filter(x => x !== 'account'), 'projectname'] : details
      return details
    }
  }
}
</script>
