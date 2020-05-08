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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-form :form="form" layout="vertical">
        <a-form-item :label="$t('scope')">
          <a-select v-decorator="['scope', { initialValue: 'cluster' }]" @change="val => { this.scope = val }">
            <a-select-option :value="'cluster'"> {{ $t('clusterid') }} </a-select-option>
            <a-select-option :value="'zone'"> {{ $t('zoneid') }} </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="this.scope === 'zone'">
          <a-form-item :label="$t('hypervisor')">
            <a-select
              v-decorator="['hypervisor', { initialValue: hypervisors[0]}]"
              @change="val => this.selectedHypervisor = val">
              <a-select-option :value="hypervisor" v-for="(hypervisor, idx) in hypervisors" :key="idx">
                {{ hypervisor }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item :label="$t('zoneid')">
          <a-select
            v-decorator="['zone', { initialValue: this.zoneSelected, rules: [{ required: true, message: 'required'}] }]"
            @change="val => changeZone(val)">
            <a-select-option :value="zone.id" v-for="(zone) in zones" :key="zone.id">
              {{ zone.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="this.scope === 'cluster' || this.scope === 'host'">
          <a-form-item :label="$t('podId')">
            <a-select
              v-decorator="['pod', { initialValue: this.podSelected, rules: [{ required: true, message: 'required'}] }]"
              @change="val => changePod(val)">
              <a-select-option :value="pod.id" v-for="(pod) in pods" :key="pod.id">
                {{ pod.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('clusterId')">
            <a-select
              v-decorator="['cluster', { initialValue: this.clusterSelected, rules: [{ required: true, message: 'required'}] }]"
              @change="val => fetchHypervisor(val)">
              <a-select-option :value="cluster.id" v-for="cluster in clusters" :key="cluster.id">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div v-if="this.scope === 'host'">
          <a-form-item :label="$t('hostId')">
            <a-select
              v-decorator="['host', { initialValue: this.hostSelected, rules: [{ required: true, message: 'required'}] }]"
              @change="val => this.hostSelected = val">
              <a-select-option :value="host.id" v-for="host in hosts" :key="host.id">
                {{ host.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item :label="$t('name')">
          <a-input v-decorator="['name', { rules: [{ required: true, message: 'required' }] }]"/>
        </a-form-item>
        <a-form-item :label="$t('protocol')">
          <a-select
            v-decorator="['protocol', { initialValue: this.protocols[0], rules: [{ required: true, message: 'required'}] }]"
            @change="val => this.protocolSelected = val">
            <a-select-option :value="protocol" v-for="(protocol,idx) in protocols" :key="idx">
              {{ protocol }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div
          v-if="protocolSelected === 'nfs' || protocolSelected === 'SMB' || protocolSelected === 'iscsi' || protocolSelected === 'vmfs'|| protocolSelected === 'Gluster'">
          <a-form-item :label="$t('server')">
            <a-input v-decorator="['server', { rules: [{ required: true, message: 'required' }] }]" />
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'nfs' || protocolSelected === 'SMB' || protocolSelected === 'ocfs2' || protocolSelected === 'preSetup'|| protocolSelected === 'SharedMountPoint'">
          <a-form-item :label="$t('path')">
            <a-input v-decorator="['path', { rules: [{ required: true, message: 'required' }] }]" />
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'SMB'">
          <a-form-item :label="$t('smbUsername')">
            <a-input v-decorator="['smbUsername', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
          <a-form-item :label="$t('smbPassword')">
            <a-input-password v-decorator="['smbPassword', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
          <a-form-item :label="$t('smbDomain')">
            <a-input v-decorator="['smbDomain', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'iscsi'">
          <a-form-item :label="$t('iqn')">
            <a-input v-decorator="['iqn', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
          <a-form-item :label="$t('lun')">
            <a-input v-decorator="['lun', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'vmfs'">
          <a-form-item :label="$t('vCenterDataCenter')">
            <a-input v-decorator="['vCenterDataCenter', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
          <a-form-item :label="$t('vCenterDataStore')">
            <a-input v-decorator="['vCenterDataStore', { rules: [{ required: true, message: 'required' }] }]"/>
          </a-form-item>
        </div>
        <a-form-item :label="$t('providername')">
          <a-select
            v-decorator="['provider', { initialValue: providerSelected, rules: [{ required: true, message: 'required'}] }]"
            @change="val => this.providerSelected = val">
            <a-select-option :value="provider" v-for="(provider,idx) in providers" :key="idx">
              {{ provider }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="this.providerSelected !== 'DefaultPrimary'">
          <a-form-item :label="$t('isManaged')">
            <a-checkbox-group v-decorator="['managed']" >
              <a-checkbox value="ismanaged"></a-checkbox>
            </a-checkbox-group>
          </a-form-item>
          <a-form-item :label="$t('capacityBytes')">
            <a-input v-decorator="['capacityBytes']" />
          </a-form-item>
          <a-form-item :label="$t('capacityIops')">
            <a-input v-decorator="['capacityIops']" />
          </a-form-item>
          <a-form-item :label="$t('url')">
            <a-input v-decorator="['url']" />
          </a-form-item>
        </div>
        <div v-if="this.protocolSelected === 'RBD'">
          <a-form-item :label="$t('radosmonitor')">
            <a-input v-decorator="['radosmonitor']" />
          </a-form-item><a-form-item :label="$t('radospool')">
            <a-input v-decorator="['radospool']" />
          </a-form-item>
          <a-form-item :label="$t('radosuser')">
            <a-input v-decorator="['radosuser']" />
          </a-form-item>
          <a-form-item :label="$t('radossecret')">
            <a-input v-decorator="['radossecret']" />
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'CLVM'">
          <a-form-item :label="$t('volumegroup')">
            <a-input v-decorator="['volumegroup', { rules: [{ required: true, message: 'required'}] }]" />
          </a-form-item>
        </div>
        <div v-if="protocolSelected === 'Gluster'">
          <a-form-item :label="$t('volume')">
            <a-input v-decorator="['volume']" />
          </a-form-item>
        </div>
        <a-form-item :label="$t('storagetags')">
          <a-select
            mode="tags"
            v-model="selectedTags"
          >
            <a-select-option v-for="tag in storageTags" :key="tag.name">{{ tag.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <div class="actions">
        <a-button @click="closeModal">{{ $t('cancel') }}</a-button>
        <a-button type="primary" @click="handleSubmit">{{ $t('ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
export default {
  name: 'AddPrimaryStorage',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      hypervisors: ['KVM', 'VMware', 'Hyperv', 'Any'],
      protocols: [],
      providers: [],
      scope: 'cluster',
      zones: [],
      pods: [],
      clusters: [],
      hosts: [],
      selectedTags: [],
      storageTags: [],
      zoneId: '',
      zoneSelected: '',
      podSelected: '',
      clusterSelected: '',
      hostSelected: '',
      hypervisorType: '',
      protocolSelected: 'nfs',
      providerSelected: 'DefaultPrimary',
      selectedHypervisor: 'KVM',
      size: 'default',
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.getInfraData()
      this.listStorageTags()
      this.listStorageProviders()
    },
    getInfraData () {
      this.loading = true
      api('listZones').then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.changeZone(this.zones[0] ? this.zones[0].id : '')
      }).finally(() => {
        this.loading = false
      })
    },
    changeZone (value) {
      this.zoneSelected = value
      if (this.zoneSelected === '') {
        this.podSelected = ''
        return
      }
      api('listPods', {
        zoneid: this.zoneSelected
      }).then(json => {
        this.pods = json.listpodsresponse.pod || []
        this.changePod(this.pods[0] ? this.pods[0].id : '')
      })
    },
    changePod (value) {
      this.podSelected = value
      if (this.podSelected === '') {
        this.clusterSelected = ''
        return
      }
      api('listClusters', {
        podid: this.podSelected
      }).then(json => {
        this.clusters = json.listclustersresponse.cluster || []
        if (this.clusters.length > 0) {
          this.clusterSelected = this.clusters[0].id
          this.fetchHypervisor()
        }
      }).then(() => {
        api('listHosts', {
          clusterid: this.clusterSelected
        }).then(json => {
          this.hosts = json.listhostsresponse.host || []
          if (this.hosts.length > 0) {
            this.hostSelected = this.hosts[0].id
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
      }).finally(() => {
        this.loading = false
      })
    },
    fetchHypervisor (value) {
      const cluster = this.clusters.find(cluster => cluster.id === this.clusterSelected)
      this.hypervisorType = cluster.hypervisortype
      if (this.hypervisorType === 'KVM') {
        this.protocols = ['nfs', 'SharedMountPoint', 'RBD', 'CLVM', 'Gluster', 'custom']
      } else if (this.hypervisorType === 'XenServer') {
        this.protocols = ['nfs', 'preSetup', 'iscsi', 'custom']
      } else if (this.hypervisorType === 'VMware') {
        this.protocols = ['nfs', 'vmfs', 'custom']
      } else if (this.hypervisorType === 'Hyperv') {
        this.protocols = ['SMB']
      } else if (this.hypervisorType === 'Ovm') {
        this.protocols = ['nfs', 'ocfs2']
      } else if (this.hypervisorType === 'LXC') {
        this.protocols = ['nfs', 'SharedMountPoint', 'RBD']
      } else {
        this.protocols = ['nfs']
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
      secret = secret.replace('+', '-')
      secret = secret.replace('/', '_')
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
    closeModal () {
      this.$parent.$parent.close()
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
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
            user: values.smbUsername,
            password: values.smbPassword,
            domain: values.smbDomain
          }
          Object.keys(smbParams).forEach((key, index) => {
            params['details[' + index.toString() + '].' + key] = smbParams[key]
          })
        } else if (values.protocol === 'PreSetup') {
          url = this.presetupURL(server, path)
        } else if (values.protocol === 'ocfs2') {
          url = this.ocfs2URL(server, path)
        } else if (values.protocol === 'SharedMountPoint') {
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
        }
        params.url = url
        if (values.provider !== 'DefaultPrimary') {
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
        if (this.selectedTags.length > 0) {
          params.tags = this.selectedTags.join()
        }
        this.loading = true
        api('createStoragePool', params).then(json => {
          this.$notification.success({
            message: this.$t('label.add.primary.storage'),
            description: this.$t('label.add.primary.storage')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
          this.closeModal()
          this.parentFetchData()
        })
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
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
