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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit"
       >
        <a-form-item name="scope" ref="scope">
          <template #label>
            <tooltip-label :title="$t('label.scope')" :tooltip="apiParams.scope.description"/>
          </template>
          <a-select
            v-model:value="form.scope"
            v-focus="true"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.scope.description" >
            <a-select-option :value="'cluster'" :label="$t('label.clusterid')"> {{ $t('label.clusterid') }} </a-select-option>
            <a-select-option :value="'zone'" :label="$t('label.zoneid')"> {{ $t('label.zoneid') }} </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="form.scope === 'zone'">
          <a-form-item name="hypervisor" ref="hypervisor">
            <template #label>
              <tooltip-label :title="$t('label.hypervisor')" :tooltip="apiParams.hypervisor.description"/>
            </template>
            <a-select
              v-model:value="form.hypervisor"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.hypervisor.description" >
              <a-select-option :value="hypervisor" v-for="(hypervisor, idx) in hypervisors" :key="idx">
                {{ hypervisor }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item name="zone" ref="zone">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            v-model:value="form.zone"
            @change="val => changeZone(val)"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option :value="zone.id" v-for="(zone) in zones" :key="zone.id" :label="zone.name">
              <span>
                <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ zone.name }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="form.scope === 'cluster' || form.scope === 'host'">
          <a-form-item name="pod" ref="pod">
            <template #label>
              <tooltip-label :title="$t('label.podid')" :tooltip="apiParams.podid.description"/>
            </template>
            <a-select
              v-model:value="form.pod"
              @change="val => changePod(val)"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.podid.description">
              <a-select-option :value="pod.id" v-for="(pod) in pods" :key="pod.id" :label="pod.name">
                {{ pod.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="cluster" ref="cluster">
            <template #label>
              <tooltip-label :title="$t('label.clusterid')" :tooltip="apiParams.clusterid.description"/>
            </template>
            <a-select
              v-model:value="form.cluster"
              @change="val => fetchHypervisor(val)"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.clusterid.description">
              <a-select-option :value="cluster.id" v-for="cluster in clusters" :key="cluster.id" :label="cluster.name">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div v-if="form.scope === 'host'">
          <a-form-item :label="$t('label.hostid')" name="host" ref="host">
            <a-select
              v-model:value="form.host"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option :value="host.id" v-for="host in hosts" :key="host.id" :label="host.name">
                {{ host.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input v-model:value="form.name" :placeholder="apiParams.name.description"/>
        </a-form-item>
        <a-form-item name="protocol" ref="protocol" v-if="form.scope === 'zone' || form.scope === 'cluster' || form.scope === 'host'">
          <template #label>
            <tooltip-label :title="$t('label.protocol')" :tooltip="$t('message.protocol.description')"/>
          </template>
          <a-select
            v-model:value="form.protocol"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.clusterid.description">
            <a-select-option :value="protocol" v-for="(protocol,idx) in protocols" :key="idx">
              {{ protocol }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div
          v-if="form.protocol === 'nfs' || form.protocol === 'SMB' || form.protocol === 'iscsi' || form.protocol === 'vmfs'|| form.protocol === 'Gluster' || form.protocol === 'Linstor' ||
            (form.protocol === 'PreSetup' && hypervisorType === 'VMware') || form.protocol === 'datastorecluster'">
          <a-form-item name="server" ref="server">
            <template #label>
              <tooltip-label :title="$t('label.server')" :tooltip="$t('message.server.description')"/>
            </template>
            <a-input v-model:value="form.server" :placeholder="$t('message.server.description')" />
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'nfs' || form.protocol === 'SMB' || form.protocol === 'ocfs2' || (form.protocol === 'PreSetup' && hypervisorType !== 'VMware') || form.protocol === 'SharedMountPoint'">
          <a-form-item name="path" ref="path">
            <template #label>
              <tooltip-label :title="$t('label.path')" :tooltip="$t('message.path.description')"/>
            </template>
            <a-input v-model:value="form.path" :placeholder="$t('message.path.description')"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'SMB'">
          <a-form-item :label="$t('label.smbusername')" name="smbUsername" ref="smbUsername">
            <a-input v-model:value="form.smbUsername"/>
          </a-form-item>
          <a-form-item :label="$t('label.smbpassword')" name="smbPassword" ref="smbPassword">
            <a-input-password v-model:value="form.smbPassword"/>
          </a-form-item>
          <a-form-item :label="$t('label.smbdomain')" name="smbDomain" ref="smbDomain">
            <a-input v-model:value="form.smbDomain"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'iscsi'">
          <a-form-item :label="$t('label.iqn')" name="iqn" ref="iqn">
            <a-input v-model:value="form.iqn"/>
          </a-form-item>
          <a-form-item :label="$t('label.lun')" name="lun" ref="lun">
            <a-input v-model:value="form.lun"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'vmfs' || (form.protocol === 'PreSetup' && hypervisorType === 'VMware') || form.protocol === 'datastorecluster'">
          <a-form-item name="vCenterDataCenter" ref="vCenterDataCenter">
            <template #label>
              <tooltip-label :title="$t('label.vcenterdatacenter')" :tooltip="$t('message.datacenter.description')"/>
            </template>
            <a-input v-model:value="form.vCenterDataCenter" :placeholder="$t('message.datacenter.description')"/>
          </a-form-item>
          <a-form-item name="vCenterDataStore" ref="vCenterDataStore">
            <template #label>
              <tooltip-label :title="$t('label.vcenterdatastore')" :tooltip="$t('message.datastore.description')"/>
            </template>
            <a-input v-model:value="form.vCenterDataStore" :placeholder="$t('message.datastore.description')"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol !== 'Linstor'">
          <a-form-item name="provider" ref="provider">
            <template #label>
              <tooltip-label :title="$t('label.providername')" :tooltip="apiParams.provider.description"/>
            </template>
            <a-select
              v-model:value="form.provider"
              @change="updateProviderAndProtocol"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="apiParams.provider.description">
              <a-select-option :value="provider" v-for="(provider,idx) in providers" :key="idx">
                {{ provider }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div v-if="form.provider !== 'DefaultPrimary' && form.provider !== 'PowerFlex' && form.provider !== 'Linstor'">
          <a-form-item name="managed" ref="managed">
            <template #label>
              <tooltip-label :title="$t('label.ismanaged')" :tooltip="apiParams.managed.description"/>
            </template>
            <a-checkbox-group v-model:value="form.managed" >
              <a-checkbox value="ismanaged"></a-checkbox>
            </a-checkbox-group>
          </a-form-item>
          <a-form-item name="capacityBytes" ref="capacityBytes">
            <template #label>
              <tooltip-label :title="$t('label.capacitybytes')" :tooltip="apiParams.capacitybytes.description"/>
            </template>
            <a-input v-model:value="form.capacityBytes" :placeholder="apiParams.capacitybytes.description" />
          </a-form-item>
          <a-form-item name="capacityIops" ref="capacityIops">
            <template #label>
              <tooltip-label :title="$t('label.capacityiops')" :tooltip="apiParams.capacityiops.description"/>
            </template>
            <a-input v-model:value="form.capacityIops" :placeholder="apiParams.capacityiops.description" />
          </a-form-item>
          <a-form-item name="url" ref="url">
            <template #label>
              <tooltip-label :title="$t('label.url')" :tooltip="apiParams.url.description"/>
            </template>
            <a-input v-model:value="form.url" :placeholder="apiParams.url.description" />
          </a-form-item>
        </div>
        <div v-if="form.provider === 'PowerFlex'">
          <a-form-item name="powerflexGateway" ref="powerflexGateway">
            <template #label>
              <tooltip-label :title="$t('label.powerflex.gateway')" :tooltip="$t('label.powerflex.gateway')"/>
            </template>
            <a-input v-model:value="form.powerflexGateway" :placeholder="$t('label.powerflex.gateway')"/>
          </a-form-item>
          <a-form-item name="powerflexGatewayUsername" ref="powerflexGatewayUsername">
            <template #label>
              <tooltip-label :title="$t('label.powerflex.gateway.username')" :tooltip="$t('label.powerflex.gateway.username')"/>
            </template>
            <a-input v-model:value="form.powerflexGatewayUsername" :placeholder="$t('label.powerflex.gateway.username')"/>
          </a-form-item>
          <a-form-item name="powerflexGatewayPassword" ref="powerflexGatewayPassword">
            <template #label>
              <tooltip-label :title="$t('label.powerflex.gateway.password')" :tooltip="$t('label.powerflex.gateway.password')"/>
            </template>
            <a-input-password v-model:value="form.powerflexGatewayPassword" :placeholder="$t('label.powerflex.gateway.password')"/>
          </a-form-item>
          <a-form-item name="powerflexStoragePool" ref="powerflexStoragePool">
            <template #label>
              <tooltip-label :title="$t('label.powerflex.storage.pool')" :tooltip="$t('label.powerflex.storage.pool')"/>
            </template>
            <a-input v-model:value="form.powerflexStoragePool" :placeholder="$t('label.powerflex.storage.pool')"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'RBD'">
          <a-form-item name="radosmonitor" ref="radosmonitor">
            <template #label>
              <tooltip-label :title="$t('label.rados.monitor')" :tooltip="$t('label.rados.monitor.description')"/>
            </template>
            <a-input v-model:value="form.radosmonitor" :placeholder="$t('label.rados.monitor')" />
          </a-form-item>
          <a-form-item name="radospool" ref="radospool" :label="$t('label.rados.pool')">
            <a-input v-model:value="form.radospool" :placeholder="$t('label.rados.pool')"/>
          </a-form-item>
          <a-form-item name="radosuser" ref="radosuser" :label="$t('label.rados.user')">
            <a-input v-model:value="form.radosuser" :placeholder="$t('label.rados.user')" />
          </a-form-item>
          <a-form-item name="radossecret" ref="radossecret" :label="$t('label.rados.secret')">
            <a-input v-model:value="form.radossecret" :placeholder="$t('label.rados.secret')" />
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'CLVM'">
          <a-form-item name="volumegroup" ref="volumegroup" :label="$t('label.volumegroup')">
            <a-input v-model:value="form.volumegroup" :placeholder="$t('label.volumegroup')" />
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'Gluster'">
          <a-form-item name="volume" ref="volume" :label="$t('label.volume')">
            <a-input v-model:value="form.volume" :placeholder="$t('label.volume')"/>
          </a-form-item>
        </div>
        <div v-if="form.protocol === 'Linstor'">
          <a-form-item name="capacityIops" ref="capacityIops">
            <template #label>
              <tooltip-label :title="$t('label.capacityiops')" :tooltip="apiParams.capacityiops.description"/>
            </template>
            <a-input v-model:value="form.capacityIops" :placeholder="apiParams.capacityiops.description" />
          </a-form-item>
          <a-form-item name="resourcegroup" ref="resourcegroup">
            <template #label>
              <tooltip-label :title="$t('label.resourcegroup')" :tooltip="$t('message.linstor.resourcegroup.description')"/>
            </template>
            <a-input v-model:value="form.resourcegroup" :placeholder="$t('message.linstor.resourcegroup.description')" />
          </a-form-item>
        </div>
        <a-form-item name="selectedTags" ref="selectedTags">
          <template #label>
            <tooltip-label :title="$t('label.storagetags')" :tooltip="apiParams.tags.description"/>
          </template>
          <a-select
            mode="tags"
            v-model:value="selectedTags"
            :placeholder="apiParams.tags.description">
            <a-select-option v-for="tag in storageTags" :key="tag.name">{{ tag.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddPrimaryStorage',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ResourceIcon
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      hypervisors: ['KVM', 'VMware', 'Hyperv', 'Any', 'Simulator'],
      protocols: [],
      providers: [],
      scope: 'cluster',
      zones: [],
      pods: [],
      clusters: [],
      hosts: [],
      selectedTags: [],
      storageTags: [],
      hypervisorType: '',
      size: 'default',
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createStoragePool')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        scope: 'cluster',
        hypervisor: this.hypervisors[0],
        provider: 'DefaultPrimary'
      })
      this.rules = reactive({
        zone: [{ required: true, message: this.$t('label.required') }],
        pod: [{ required: true, message: this.$t('label.required') }],
        cluster: [{ required: true, message: this.$t('label.required') }],
        host: [{ required: true, message: this.$t('label.required') }],
        name: [{ required: true, message: this.$t('label.required') }],
        protocol: [{ required: true, message: this.$t('label.required') }],
        server: [{ required: true, message: this.$t('label.required') }],
        path: [{ required: true, message: this.$t('label.required') }],
        smbUsername: [{ required: true, message: this.$t('label.required') }],
        smbPassword: [{ required: true, message: this.$t('label.required') }],
        smbDomain: [{ required: true, message: this.$t('label.required') }],
        iqn: [{ required: true, message: this.$t('label.required') }],
        lun: [{ required: true, message: this.$t('label.required') }],
        vCenterDataCenter: [{ required: true, message: this.$t('label.required') }],
        vCenterDataStore: [{ required: true, message: this.$t('label.required') }],
        provider: [{ required: true, message: this.$t('label.required') }],
        volumegroup: [{ required: true, message: this.$t('label.required') }],
        resourcegroup: [{ required: true, message: this.$t('label.required') }],
        powerflexGateway: [{ required: true, message: this.$t('label.required') }],
        powerflexGatewayUsername: [{ required: true, message: this.$t('label.required') }],
        powerflexGatewayPassword: [{ required: true, message: this.$t('label.required') }],
        powerflexStoragePool: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.getInfraData()
      this.listStorageTags()
      this.listStorageProviders()
    },
    getInfraData () {
      this.loading = true
      api('listZones', { showicon: true }).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.changeZone(this.zones[0] ? this.zones[0].id : '')
      }).finally(() => {
        this.loading = false
      })
    },
    changeZone (value) {
      this.form.zone = value
      if (this.form.zone === '') {
        this.form.pod = ''
        return
      }
      api('listPods', {
        zoneid: this.form.zone
      }).then(json => {
        this.pods = json.listpodsresponse.pod || []
        this.changePod(this.pods[0] ? this.pods[0].id : '')
      })
    },
    changePod (value) {
      this.form.pod = value
      if (this.form.pod === '') {
        this.form.cluster = ''
        return
      }
      api('listClusters', {
        podid: this.form.pod
      }).then(json => {
        this.clusters = json.listclustersresponse.cluster || []
        if (this.clusters.length > 0) {
          this.form.cluster = this.clusters[0].id
          this.fetchHypervisor()
        }
      }).then(() => {
        api('listHosts', {
          clusterid: this.form.cluster
        }).then(json => {
          this.hosts = json.listhostsresponse.host || []
          if (this.hosts.length > 0) {
            this.form.host = this.hosts[0].id
          }
        })
      })
    },
    listStorageProviders () {
      this.providers = []
      this.loading = true
      api('listStorageProviders', { type: 'primary' }).then(json => {
        var providers = json.liststorageprovidersresponse.dataStoreProvider || []
        for (const provider of providers) {
          this.providers.push(provider.name)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    listStorageTags () {
      this.loading = true
      api('listStorageTags').then(json => {
        this.storageTags = json.liststoragetagsresponse.storagetag || []
        if (this.storageTags) {
          this.storageTags = _.uniqBy(this.storageTags, 'name')
        }
      }).finally(() => {
        this.loading = false
      })
    },
    fetchHypervisor (value) {
      const cluster = this.clusters.find(cluster => cluster.id === this.form.cluster)
      this.hypervisorType = cluster.hypervisortype
      if (this.hypervisorType === 'KVM') {
        this.protocols = ['nfs', 'SharedMountPoint', 'RBD', 'CLVM', 'Gluster', 'Linstor', 'custom']
      } else if (this.hypervisorType === 'XenServer') {
        this.protocols = ['nfs', 'PreSetup', 'iscsi', 'custom']
      } else if (this.hypervisorType === 'VMware') {
        this.protocols = ['nfs', 'vmfs', 'custom']
        if ('importVsphereStoragePolicies' in this.$store.getters.apis) {
          this.protocols = ['nfs', 'PreSetup', 'datastorecluster', 'custom']
        }
      } else if (this.hypervisorType === 'Hyperv') {
        this.protocols = ['SMB']
      } else if (this.hypervisorType === 'Ovm') {
        this.protocols = ['nfs', 'ocfs2']
      } else if (this.hypervisorType === 'LXC') {
        this.protocols = ['nfs', 'SharedMountPoint', 'RBD']
      } else {
        this.protocols = ['nfs']
      }
      if (!value) {
        this.form.protocol = this.protocols[0]
      }
    },
    nfsURL (server, path) {
      var url
      if (path.substring(0, 1) !== '/') {
        path = '/' + path
      }
      if (server.indexOf('://') === -1) {
        url = 'nfs://' + server + path
      } else {
        url = server + path
      }

      return url
    },
    smbURL (server, path, smbUsername, smbPassword, smbDomain) {
      var url = ''
      if (path.substring(0, 1) !== '/') {
        path = '/' + path
      }
      if (server.indexOf('://') === -1) {
        url += 'cifs://'
      }
      url += (server + path)
      return url
    },
    presetupURL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'presetup://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    datastoreclusterURL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'datastorecluster://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    ocfs2URL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'ocfs2://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    SharedMountPointURL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'SharedMountPoint://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    rbdURL (monitor, pool, id, secret) {
      var url
      /*  Replace the + and / symbols by - and _ to have URL-safe base64 going to the API
          It's hacky, but otherwise we'll confuse java.net.URI which splits the incoming URI
      */
      secret = secret.replace(/\+/g, '-')
      secret = secret.replace(/\//g, '_')
      if (id !== null && secret !== null) {
        monitor = id + ':' + secret + '@' + monitor
      }
      if (pool.substring(0, 1) !== '/') {
        pool = '/' + pool
      }
      if (monitor.indexOf('://') === -1) {
        url = 'rbd://' + monitor + pool
      } else {
        url = monitor + pool
      }
      return url
    },
    clvmURL (vgname) {
      var url
      if (vgname.indexOf('://') === -1) {
        url = 'clvm://localhost/' + vgname
      } else {
        url = vgname
      }
      return url
    },
    vmfsURL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'vmfs://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    iscsiURL (server, iqn, lun) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'iscsi://' + server + iqn + '/' + lun
      } else {
        url = server + iqn + '/' + lun
      }
      return url
    },
    glusterURL (server, path) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'gluster://' + server + path
      } else {
        url = server + path
      }
      return url
    },
    powerflexURL (gateway, username, password, pool) {
      var url = 'powerflex://' + encodeURIComponent(username) + ':' + encodeURIComponent(password) + '@' +
       gateway + '/' + encodeURIComponent(pool)
      return url
    },
    updateProviderAndProtocol (value) {
      if (value === 'PowerFlex') {
        this.protocols = ['custom']
        this.form.protocol = 'custom'
      } else {
        this.fetchHypervisor(value)
      }
    },
    closeModal () {
      this.$emit('close-action')
    },
    linstorURL (server) {
      var url
      if (server.indexOf('://') === -1) {
        url = 'http://' + server
      } else {
        url = server
      }

      return url
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        var params = {
          scope: values.scope,
          zoneid: values.zone,
          name: values.name,
          provider: values.provider
        }
        if (values.scope === 'zone') {
          params.hypervisor = values.hypervisor
        }
        if (values.scope === 'cluster' || values.scope === 'host') {
          params.podid = values.pod
          params.clusterid = values.cluster
        }
        if (values.scope === 'host') {
          params.hostid = values.host
        }
        var server = values.server ? values.server : null
        var path = values.path ? values.path : null
        if (path !== null && path.substring(0, 1) !== '/') {
          path = '/' + path
        }
        var url = ''
        if (values.protocol === 'nfs') {
          url = this.nfsURL(server, path)
        } else if (values.protocol === 'SMB') {
          url = this.smbURL(server, path)
          const smbParams = {
            user: encodeURIComponent(values.smbUsername),
            password: encodeURIComponent(values.smbPassword),
            domain: values.smbDomain
          }
          Object.keys(smbParams).forEach((key, index) => {
            params['details[' + index.toString() + '].' + key] = smbParams[key]
          })
        } else if (values.protocol === 'PreSetup' && this.hypervisorType !== 'VMware') {
          server = 'localhost'
          url = this.presetupURL(server, path)
        } else if (values.protocol === 'PreSetup' && this.hypervisorType === 'VMware') {
          path = values.vCenterDataCenter
          if (path.substring(0, 1) !== '/') {
            path = '/' + path
          }
          path += '/' + values.vCenterDataStore
          url = this.presetupURL(server, path)
        } else if (values.protocol === 'datastorecluster' && this.hypervisorType === 'VMware') {
          path = values.vCenterDataCenter
          if (path.substring(0, 1) !== '/') {
            path = '/' + path
          }
          path += '/' + values.vCenterDataStore
          url = this.datastoreclusterURL(server, path)
        } else if (values.protocol === 'ocfs2') {
          url = this.ocfs2URL(server, path)
        } else if (values.protocol === 'SharedMountPoint') {
          server = 'localhost'
          url = this.SharedMountPointURL(server, path)
        } else if (values.protocol === 'CLVM') {
          var vg = (values.volumegroup.substring(0, 1) !== '/') ? ('/' + values.volumegroup) : values.volumegroup
          url = this.clvmURL(vg)
        } else if (values.protocol === 'RBD') {
          url = this.rbdURL(values.radosmonitor, values.radospool, values.radosuser, values.radossecret)
        } else if (values.protocol === 'vmfs') {
          path = values.vCenterDataCenter
          if (path.substring(0, 1) !== '/') {
            path = '/' + path
          }
          path += '/' + values.vCenterDataStore
          url = this.vmfsURL(server, path)
        } else if (values.protocol === 'Gluster') {
          var glustervolume = values.volume
          if (glustervolume.substring(0, 1) !== '/') {
            glustervolume = '/' + glustervolume
          }
          url = this.glusterURL(server, glustervolume)
        } else if (values.protocol === 'iscsi') {
          var iqn = values.iqn
          if (iqn.substring(0, 1) !== '/') {
            iqn = '/' + iqn
          }
          var lun = values.lun
          url = this.iscsiURL(server, iqn, lun)
        } else if (values.protocol === 'Linstor') {
          url = this.linstorURL(server)
          params.provider = 'Linstor'
          values.managed = false
          params['details[0].resourceGroup'] = values.resourcegroup
          if (values.capacityIops && values.capacityIops.length > 0) {
            params.capacityIops = values.capacityIops.split(',').join('')
          }
        }
        params.url = url
        if (values.provider !== 'DefaultPrimary' && values.provider !== 'PowerFlex') {
          if (values.managed) {
            params.managed = true
          } else {
            params.managed = false
          }
          if (values.capacityBytes && values.capacityBytes.length > 0) {
            params.capacityBytes = values.capacityBytes.split(',').join('')
          }
          if (values.capacityIops && values.capacityIops.length > 0) {
            params.capacityIops = values.capacityIops.split(',').join('')
          }
          if (values.url && values.url.length > 0) {
            params.url = values.url
          }
        }

        if (values.provider === 'PowerFlex') {
          params.url = this.powerflexURL(values.powerflexGateway, values.powerflexGatewayUsername,
            values.powerflexGatewayPassword, values.powerflexStoragePool)
        }

        if (this.selectedTags.length > 0) {
          params.tags = this.selectedTags.join()
        }
        this.loading = true
        api('createStoragePool', {}, 'POST', params).then(json => {
          this.$notification.success({
            message: this.$t('label.add.primary.storage'),
            description: this.$t('label.add.primary.storage')
          })
          this.closeModal()
          this.parentFetchData()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 1000px) {
    width: 500px;
  }
}
</style>
