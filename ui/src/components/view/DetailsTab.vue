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
  <a-alert v-if="ip4routes" type="info" :showIcon="true" :message="$t('label.add.upstream.ipv4.routes')">
    <template #description>
      <p v-html="ip4routes" />
    </template>
  </a-alert>
  <a-alert v-if="ip6routes" type="info" :showIcon="true" :message="$t('label.add.upstream.ipv6.routes')">
    <template #description>
      <p v-html="ip6routes" />
    </template>
  </a-alert>
  <a-alert v-if="vnfAccessMethods" type="info" :showIcon="true" :message="$t('label.vnf.appliance.access.methods')">
    <template #description>
      <p v-html="vnfAccessMethods" />
    </template>
  </a-alert>
  <a-list
    size="small"
    :dataSource="fetchDetails()">
    <template #renderItem="{item}">
      <a-list-item v-if="(item in dataResource && !customDisplayItems.includes(item)) || (offeringDetails.includes(item) && dataResource.serviceofferingdetails)">
        <div style="width: 100%">
          <strong>{{ item === 'service' ? $t('label.supportedservices') : $t(getDetailTitle(item)) }}</strong>
          <br/>
          <div v-if="Array.isArray(dataResource[item]) && item === 'service'">
            <div v-for="(service, idx) in dataResource[item]" :key="idx">
              {{ service.name }} : {{ service.provider?.[0]?.name }}
            </div>
          </div>
          <div v-else-if="$route.meta.name === 'backup' && (item === 'size' || item === 'virtualsize')">
            {{ $bytesToHumanReadableSize(dataResource[item]) }}
            <a-tooltip placement="right">
              <template #title>
                {{ dataResource[item] }} bytes
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
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
          <div v-else-if="['template', 'iso'].includes($route.meta.name) && item === 'size'">
            <div>
              {{ parseFloat(dataResource.size / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GiB
            </div>
          </div>
          <div v-else-if="['volume', 'snapshot', 'template', 'iso'].includes($route.meta.name) && item === 'physicalsize'">
            <div>
              {{ parseFloat(dataResource.physicalsize / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GiB
            </div>
          </div>
          <div v-else-if="['volume', 'snapshot', 'template', 'iso'].includes($route.meta.name) && item === 'virtualsize'">
            <div>
              {{ parseFloat(dataResource.virtualsize / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GiB
            </div>
          </div>
          <div v-else-if="['name', 'type'].includes(item)">
            <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(dataResource[item])">{{ $t(dataResource[item].toLowerCase()) }}</span>
            <span v-else>{{ dataResource[item] }}</span>
          </div>
          <div v-else-if="['created', 'sent', 'lastannotated', 'collectiontime', 'lastboottime', 'lastserverstart', 'lastserverstop', 'removed', 'effectiveDate', 'endDate'].includes(item)">
            {{ $toLocaleDate(dataResource[item]) }}
          </div>
          <div style="white-space: pre-wrap;" v-else-if="$route.meta.name === 'quotatariff' && item === 'description'">{{ dataResource[item] }}</div>
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
          <div v-else-if="$route.meta.name === 'computeoffering' && offeringDetails.includes(item)">
            {{ dataResource.serviceofferingdetails[item] }}
          </div>
          <div v-else-if="item === 'headers'" style="white-space: pre-line;">
            {{ dataResource[item] }}
          </div>
          <div v-else-if="item === 'payload'" style="white-space: pre-wrap;">
            {{ JSON.stringify(JSON.parse(dataResource[item]), null, 4) || dataResource[item] }}
          </div>
          <div v-else-if="item === 'dedicatedresources'">
            <div v-for="(resource, idx) in sortDedicatedResourcesByName(dataResource[item])" :key="idx">
              <div>
                <router-link :to="getResourceLink(resource.resourcetype, resource.resourceid)">
                  {{ resource.resourcename }}
                </router-link>
              </div>
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
      <a-list-item v-else-if="['startdate', 'enddate'].includes(item)">
        <div>
          <strong>{{ $t('label.' + item.replace('date', '.date.and.time'))}}</strong>
          <br/>
          <div>{{ $toLocaleDate(dataResource[item]) }}</div>
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
import { genericCompare } from '@/utils/sort'

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
      dataResource: {},
      detailsTitles: []
    }
  },
  mounted () {
    this.dedicatedSectionActive = this.dedicatedRoutes.includes(this.$route.meta.name)
  },
  computed: {
    customDisplayItems () {
      var items = ['ip4routes', 'ip6routes', 'privatemtu', 'publicmtu', 'provider']
      if (this.$route.meta.name === 'webhookdeliveries') {
        items.push('startdate')
        items.push('enddate')
      }
      return items
    },
    vnfAccessMethods () {
      if (this.resource.templatetype === 'VNF' && ['vm', 'vnfapp'].includes(this.$route.meta.name)) {
        const accessMethodsDescription = []
        const accessMethods = this.resource.vnfdetails?.access_methods || null
        const username = this.resource.vnfdetails?.username || null
        const password = this.resource.vnfdetails?.password || null
        const sshPort = this.resource.vnfdetails?.ssh_port || 22
        const sshUsername = this.resource.vnfdetails?.ssh_user || null
        const sshPassword = this.resource.vnfdetails?.ssh_password || null
        let httpPath = this.resource.vnfdetails?.http_path || ''
        if (!httpPath.startsWith('/')) {
          httpPath = '/' + httpPath
        }
        const httpPort = this.resource.vnfdetails?.http_port || null
        let httpsPath = this.resource.vnfdetails?.https_path || ''
        if (!httpsPath.startsWith('/')) {
          httpsPath = '/' + httpsPath
        }
        const httpsPort = this.resource.vnfdetails?.https_port || null
        const webUsername = this.resource.vnfdetails?.web_user || null
        const webPassword = this.resource.vnfdetails?.web_password || null

        const credentials = []
        if (username) {
          credentials.push(this.$t('label.username') + ' : ' + username)
        }
        if (password) {
          credentials.push(this.$t('label.password.default') + ' : ' + password)
        }
        if (webUsername) {
          credentials.push('Web ' + this.$t('label.username') + ' : ' + webUsername)
        }
        if (webPassword) {
          credentials.push('Web ' + this.$t('label.password.default') + ' : ' + webPassword)
        }
        if (sshUsername) {
          credentials.push('SSH ' + this.$t('label.username') + ' : ' + sshUsername)
        }
        if (sshPassword) {
          credentials.push('SSH ' + this.$t('label.password.default') + ' : ' + sshPassword)
        }

        const managementDeviceIds = []
        if (this.resource.vnfnics) {
          for (const vnfnic of this.resource.vnfnics) {
            if (vnfnic.management) {
              managementDeviceIds.push(vnfnic.deviceid)
            }
          }
        }
        const managementIps = []
        for (const nic of this.resource.nic) {
          if (managementDeviceIds.includes(parseInt(nic.deviceid)) && nic.ipaddress) {
            managementIps.push(nic.ipaddress)
            if (nic.publicip) {
              managementIps.push(nic.publicip)
            }
          }
        }

        if (accessMethods) {
          const accessMethodsArray = accessMethods.split(',')
          for (const accessMethod of accessMethodsArray) {
            if (accessMethod === 'console') {
              accessMethodsDescription.push('- VM Console.')
            } else if (accessMethod === 'ssh-password') {
              accessMethodsDescription.push('- SSH with password' + (sshPort ? ' (SSH port is ' + sshPort + ').' : '.'))
            } else if (accessMethod === 'ssh-key') {
              accessMethodsDescription.push('- SSH with key' + (sshPort ? ' (SSH port is ' + sshPort + ').' : '.'))
            } else if (accessMethod === 'http') {
              for (const managementIp of managementIps) {
                const url = 'http://' + managementIp + (httpPort ? ':' + httpPort : '') + httpPath
                accessMethodsDescription.push('- Webpage: <a href="' + url + '" target="_blank>">' + url + '</a>')
              }
            } else if (accessMethod === 'https') {
              for (const managementIp of managementIps) {
                const url = 'https://' + managementIp + (httpsPort ? ':' + httpsPort : '') + httpsPath
                accessMethodsDescription.push('- Webpage: <a href="' + url + '" target="_blank">' + url + '</a>')
              }
            }
          }
        } else {
          accessMethodsDescription.push('- VM Console.')
        }
        if (credentials) {
          accessMethodsDescription.push('<br>' + this.$t('message.vnf.credentials.in.template.vnf.details'))
        }
        return accessMethodsDescription.join('<br>')
      }
      return null
    },
    offeringDetails () {
      return ['maxcpunumber', 'mincpunumber', 'minmemory', 'maxmemory']
    },
    ipV6Address () {
      if (this.dataResource.nic && this.dataResource.nic.length > 0) {
        return this.dataResource.nic.filter(e => { return e.ip6address }).map(e => { return e.ip6address }).join(', ')
      }
      return null
    },
    ip4routes () {
      if (this.resource.ip4routes && this.resource.ip4routes.length > 0) {
        var routes = []
        for (var route of this.resource.ip4routes) {
          routes.push(route.subnet + ' via ' + route.gateway)
        }
        return routes.join('<br>')
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
      let details = this.$route.meta.details

      if (!details) {
        return
      }

      if (typeof details === 'function') {
        details = details()
      }

      let detailsKeys = []
      for (const detail of details) {
        if (typeof detail === 'object') {
          const field = detail.field
          detailsKeys.push(field)
          this.detailsTitles[field] = detail.customTitle
        } else {
          detailsKeys.push(detail)
          this.detailsTitles[detail] = detail
        }
      }

      detailsKeys = this.projectname ? [...detailsKeys.filter(x => x !== 'account'), 'projectname'] : detailsKeys
      return detailsKeys
    },
    getDetailTitle (detail) {
      return `label.${String(this.detailsTitles[detail]).toLowerCase()}`
    },
    getResourceLink (type, id) {
      return `/${type.toLowerCase()}/${id}`
    },
    sortDedicatedResourcesByName (resources) {
      resources.sort((resource, otherResource) => {
        return genericCompare(resource.resourcename, otherResource.resourcename)
      })

      return resources
    }
  }
}
</script>
