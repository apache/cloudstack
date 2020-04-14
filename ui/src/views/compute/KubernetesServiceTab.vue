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
  <a-spin :spinning="networkLoading">
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('details')" key="details">
        <DetailsTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('access')" key="access">
        <a-card title="Kubernetes Cluster Config" :loading="this.versionLoading">
          <div v-if="this.clusterConfig !== ''">
            <a-textarea :value="this.clusterConfig" :rows="5" readonly />
            <div :span="24" class="action-button">
              <a-button @click="downloadKubernetesClusterConfig" type="primary">{{ this.$t('Download') }}</a-button>
            </div>
          </div>
          <div v-else>
            <p>Kubernetes cluster kubeconfig not available currently</p>
          </div>
        </a-card>
        <a-card title="Using CLI" :loading="this.versionLoading">
          <a-timeline>
            <a-timeline-item>
              <p>
                Download kubeconfig for the cluster<br><br>
                The <code>kubectl</code> command-line tool uses kubeconfig files to find the information it needs to choose a cluster and communicate with the API server of a cluster.
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                Download <code>kubectl</code> tool for cluster's Kubernetes version<br><br>
                Linux: <a :href="this.kubectlLinuxLink">{{ this.kubectlLinuxLink }}</a><br>
                MacOS: <a :href="this.kubectlMacLink">{{ this.kubectlMacLink }}</a><br>
                Windows: <a :href="this.kubectlWindowsLink">{{ this.kubectlWindowsLink }}</a>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                Use <code>kubectl</code> and <code>kubeconfig</code> file to access cluster<br><br>
                <code><b>kubectl --kubeconfig /custom/path/kube.conf {COMMAND}</b></code><br><br>

                <em>List pods</em><br>
                <code>kubectl --kubeconfig /custom/path/kube.conf get pods --all-namespaces</code><br>
                <em>List nodes</em><br>
                <code>kubectl --kubeconfig /custom/path/kube.conf get nodes --all-namespaces</code><br>
                <em>List services</em><br>
                <code>kubectl --kubeconfig /custom/path/kube.conf get services --all-namespaces</code>
              </p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
        <a-card title="Kubernetes Dashboard UI">
          <a-timeline>
            <a-timeline-item>
              <p>
                Run proxy locally<br><br>
                <code><b>kubectl --kubeconfig /custom/path/kube.conf proxy</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                Open URL in browser<br><br>
                <a href="http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"><code>http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/</code></a>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                Token for dashboard login can be retrieved using following command<br><br>
                <code><b>kubectl --kubeconfig /custom/path/kube.conf describe secret $(kubectl --kubeconfig /custom/path/kube.conf get secrets -n kubernetes-dashboard | grep kubernetes-dashboard-token | awk '{print $1}') -n kubernetes-dashboard</b></code>
              </p>
            </a-timeline-item>
          </a-timeline>
          <p>More about accessing dashboard UI, <a href="https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/#accessing-the-dashboard-ui">https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/#accessing-the-dashboard-ui</a></p>
        </a-card>
      </a-tab-pane>
      <a-tab-pane :tab="$t('instances')" key="instances">
        <a-table
          class="table"
          size="small"
          :columns="this.vmColumns"
          :dataSource="this.virtualmachines"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template slot="name" slot-scope="text, record">
            <router-link :to="{ path: '/vm/' + record.id }">{{ record.name }}</router-link>
          </template>
          <template slot="state" slot-scope="text">
            <status :text="text ? text : ''" displayText />
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane :tab="$t('firewall')" key="firewall">
        <FirewallRules :resource="this.publicIpAddress" :loading="this.networkLoading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('portforwarding')" key="portforwarding">
        <PortForwarding :resource="this.publicIpAddress" :loading="this.networkLoading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('loadbalancing')" key="loadbalancing">
        <LoadBalancing :resource="this.publicIpAddress" :loading="this.networkLoading" />
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { mixinDevice } from '@/utils/mixin.js'
import DetailsTab from '@/components/view/DetailsTab'
import FirewallRules from '@/views/network/FirewallRules'
import PortForwarding from '@/views/network/PortForwarding'
import LoadBalancing from '@/views/network/LoadBalancing'
import Status from '@/components/widgets/Status'

export default {
  name: 'KubernetesServiceTab',
  components: {
    DetailsTab,
    FirewallRules,
    PortForwarding,
    LoadBalancing,
    Status
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
  data () {
    return {
      clusterConfigLoading: false,
      clusterConfig: '',
      versionLoading: false,
      kubernetesVersion: {},
      kubectlLinuxLink: 'https://storage.googleapis.com/kubernetes-release/release/v1.16.0/bin/linux/amd64/kubectl',
      kubectlMacLink: 'https://storage.googleapis.com/kubernetes-release/release/v1.16.0/bin/darwin/amd64/kubectl',
      kubectlWindowsLink: 'https://storage.googleapis.com/kubernetes-release/release/v1.16.0/bin/windows/amd64/kubectl.exe',
      instanceLoading: false,
      virtualmachines: [],
      vmColumns: [],
      networkLoading: false,
      network: {},
      publicIpAddress: {},
      currentTab: 'details'
    }
  },
  created () {
    if (this.isAdminOrDomainAdmin()) {
      this.vmColumns = [
        {
          title: this.$t('name'),
          dataIndex: 'name',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: this.$t('state'),
          dataIndex: 'state',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: this.$t('instancename'),
          dataIndex: 'instancename'
        },
        {
          title: this.$t('ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          title: this.$t('zonename'),
          dataIndex: 'zonename'
        }
      ]
    } else {
      this.vmColumns = [
        {
          title: this.$t('name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('displayname'),
          dataIndex: 'displayname'
        },
        {
          title: this.$t('ipaddress'),
          dataIndex: 'ipaddress'
        },
        {
          title: this.$t('zonename'),
          dataIndex: 'zonename'
        },
        {
          title: this.$t('state'),
          dataIndex: 'state'
        }
      ]
    }
  },
  mounted () {
    this.handleFetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.handleFetchData()
      }
    }
  },
  methods: {
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    handleChangeTab (e) {
      this.currentTab = e
    },
    handleFetchData () {
      this.fetchKubernetesClusterConfig()
      this.fetchKubernetesVersion()
      this.fetchInstances()
      this.fetchPublicIpAddress()
    },
    fetchKubernetesClusterConfig () {
      this.clusterConfigLoading = true
      this.clusterConfig = ''
      if (!this.isObjectEmpty(this.resource)) {
        var params = {}
        params.id = this.resource.id
        api('getKubernetesClusterConfig', params).then(json => {
          const config = json.getkubernetesclusterconfigresponse.clusterconfig
          if (!this.isObjectEmpty(config) &&
            this.isValidValueForKey(config, 'configdata') &&
            config.configdata !== '') {
            this.clusterConfig = config.configdata
          } else {
            this.$notification.error({
              message: 'Request Failed',
              description: 'Unable to retrieve Kubernetes cluster config'
            })
          }
        }).finally(() => {
          this.clusterConfigLoading = false
          if (!this.isObjectEmpty(this.kubernetesVersion) && this.isValidValueForKey(this.kubernetesVersion, 'semanticversion')) {
            this.kubectlLinuxLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/linux/amd64/kubectl'
            this.kubectlMacLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/darwin/amd64/kubectl'
            this.kubectlWindowsLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/windows/amd64/kubectl.exe'
          }
        })
      }
    },
    fetchKubernetesVersion () {
      this.versionLoading = true
      this.virtualmachines = []
      if (!this.isObjectEmpty(this.resource) && this.isValidValueForKey(this.resource, 'kubernetesversionid') &&
        this.resource.kubernetesversionid !== '') {
        var params = {}
        params.id = this.resource.kubernetesversionid
        api('listKubernetesSupportedVersions', params).then(json => {
          const versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion
          if (this.arrayHasItems(versionObjs)) {
            this.kubernetesVersion = versionObjs[0]
          }
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: error.response.headers['x-description']
          })
        }).finally(() => {
          this.versionLoading = false
          if (!this.isObjectEmpty(this.kubernetesVersion) && this.isValidValueForKey(this.kubernetesVersion, 'semanticversion')) {
            this.kubectlLinuxLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/linux/amd64/kubectl'
            this.kubectlMacLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/darwin/amd64/kubectl'
            this.kubectlWindowsLink = 'https://storage.googleapis.com/kubernetes-release/release/v' + this.kubernetesVersion.semanticversion + '/bin/windows/amd64/kubectl.exe'
          }
        })
      }
    },
    fetchInstances () {
      this.instanceLoading = true
      this.virtualmachines = []
      if (!this.isObjectEmpty(this.resource) && this.arrayHasItems(this.resource.virtualmachineids)) {
        var params = {}
        if (this.isAdminOrDomainAdmin()) {
          params.listall = true
        }
        if (this.isValidValueForKey(this.resource, 'projectid') &&
          this.resource.projectid !== '') {
          params.projectid = this.resource.projectid
        }
        params.ids = this.resource.virtualmachineids.join()
        api('listVirtualMachines', params).then(json => {
          const listVms = json.listvirtualmachinesresponse.virtualmachine
          if (this.arrayHasItems(listVms)) {
            for (var i = 0; i < listVms.length; ++i) {
              var vm = listVms[i]
              if (vm.nic && vm.nic.length > 0 && vm.nic[0].ipaddress) {
                vm.ipaddress = vm.nic[0].ipaddress
                listVms[i] = vm
              }
            }
            this.virtualmachines = this.virtualmachines.concat(listVms)
          }
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: error.response.headers['x-description']
          })
        }).finally(() => {
          this.instanceLoading = false
        })
      }
    },
    fetchPublicIpAddress () {
      this.networkLoading = true
      var params = {
        listAll: true,
        forvirtualnetwork: true
      }
      if (!this.isObjectEmpty(this.resource)) {
        if (this.isValidValueForKey(this.resource, 'projectid') &&
          this.resource.projectid !== '') {
          params.projectid = this.resource.projectid
        }
        if (this.isValidValueForKey(this.resource, 'associatednetworkid')) {
          params.associatednetworkid = this.resource.associatednetworkid
        }
      }
      api('listPublicIpAddresses', params).then(json => {
        const ips = json.listpublicipaddressesresponse.publicipaddress
        if (this.arrayHasItems(ips)) {
          this.publicIpAddress = ips[0]
        }
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        this.networkLoading = false
      })
    },
    downloadKubernetesClusterConfig () {
      var blob = new Blob([this.clusterConfig], { type: 'text/plain' })
      var filename = 'kube.conf'
      if (window.navigator.msSaveOrOpenBlob) {
        window.navigator.msSaveBlob(blob, filename)
      } else {
        var elem = window.document.createElement('a')
        elem.href = window.URL.createObjectURL(blob)
        elem.download = filename
        document.body.appendChild(elem)
        elem.click()
        document.body.removeChild(elem)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .list {

    &__item,
    &__row {
      display: flex;
      flex-wrap: wrap;
      width: 100%;
    }

    &__item {
      margin-bottom: -20px;
    }

    &__col {
      flex: 1;
      margin-right: 20px;
      margin-bottom: 20px;
    }

    &__label {
      font-weight: bold;
    }

  }

  .pagination {
    margin-top: 20px;
  }

  .table {
    margin-top: 20px;
    overflow-y: auto;
  }

  .action-button {
    margin-top: 10px;
    text-align: right;

    button {
      margin-right: 5px;
    }
  }
</style>
