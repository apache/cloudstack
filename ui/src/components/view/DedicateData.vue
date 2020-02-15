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
  <a-list-item v-if="dedicatedDomainId">
    <div>
      <div style="margin-bottom: 10px;">
        <strong>{{ $t('dedicated') }}</strong>
        <div>Yes</div>
      </div>
      <p>
        <strong>{{ $t('domainid') }}</strong><br/>
        <router-link :to="{ path: '/domain/' + dedicatedDomainId }">{{ dedicatedDomainId }}</router-link>
      </p>
      <p v-if="dedicatedAccountId">
        <strong>{{ $t('account') }}</strong><br/>
        <router-link :to="{ path: '/account/' + dedicatedAccountId }">{{ dedicatedAccountId }}</router-link>
      </p>
      <a-button style="margin-top: 10px; margin-bottom: 10px;" type="danger" @click="handleRelease">
        {{ releaseButtonLabel }}
      </a-button>
    </div>
  </a-list-item>
  <a-list-item v-else>
    <div>
      <strong>{{ $t('dedicated') }}</strong>
      <div>No</div>
      <a-button type="primary" style="margin-top: 10px; margin-bottom: 10px;" @click="modalActive = true">
        {{ dedicatedButtonLabel }}
      </a-button>
    </div>
    <DedicateModal
      :resource="resource"
      :active="modalActive"
      :label="dedicatedModalLabel"
      @close="modalActive = false"
      :fetchData="fetchData" />
  </a-list-item>
</template>

<script>
import { api } from '@/api'
import DedicateModal from './DedicateModal'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    DedicateModal
  },
  inject: ['parentFetchData'],
  data () {
    return {
      modalActive: false,
      dedicatedButtonLabel: 'Dedicate',
      releaseButtonLabel: 'Release',
      dedicatedModalLabel: 'Dedicate',
      dedicatedDomainId: null,
      dedicatedAccountId: null
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (this.resource && this.resource.id && newItem && newItem.id !== oldItem.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      if (this.$route.meta.name === 'zone') {
        this.fetchDedicatedZones()
        this.releaseButtonLabel = this.$t('label.release.dedicated.zone')
        this.dedicatedButtonLabel = this.$t('label.dedicate.zone')
        this.dedicatedModalLabel = this.$t('label.dedicate.zone')
      }
      if (this.$route.meta.name === 'pod') {
        this.fetchDedicatedPods()
        this.releaseButtonLabel = this.$t('label.release.dedicated.pod')
        this.dedicatedButtonLabel = this.$t('label.dedicate.pod')
        this.dedicatedModalLabel = this.$t('label.dedicate.pod')
      }
      if (this.$route.meta.name === 'cluster') {
        this.fetchDedicatedClusters()
        this.releaseButtonLabel = this.$t('label.release.dedicated.cluster')
        this.dedicatedButtonLabel = this.$t('label.dedicate.cluster')
        this.dedicatedModalLabel = this.$t('label.dedicate.cluster')
      }
      if (this.$route.meta.name === 'host') {
        this.fetchDedicatedHosts()
        this.releaseButtonLabel = this.$t('label.release.dedicated.host')
        this.dedicatedButtonLabel = this.$t('label.dedicate.host')
        this.dedicatedModalLabel = this.$t('label.dedicate.host')
      }
    },
    fetchDedicatedZones () {
      api('listDedicatedZones', {
        zoneid: this.resource.id
      }).then(response => {
        if (response.listdedicatedzonesresponse.dedicatedzone &&
            response.listdedicatedzonesresponse.dedicatedzone.length > 0) {
          this.dedicatedDomainId = response.listdedicatedzonesresponse.dedicatedzone[0].domainid
          this.dedicatedAccountId = response.listdedicatedzonesresponse.dedicatedzone[0].accountid
        }
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    fetchDedicatedPods () {
      api('listDedicatedPods', {
        podid: this.resource.id
      }).then(response => {
        if (response.listdedicatedpodsresponse.dedicatedpod &&
            response.listdedicatedpodsresponse.dedicatedpod.length > 0) {
          this.dedicatedDomainId = response.listdedicatedpodsresponse.dedicatedpod[0].domainid
          this.dedicatedAccountId = response.listdedicatedpodsresponse.dedicatedpod[0].accountid
        }
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    fetchDedicatedClusters () {
      api('listDedicatedClusters', {
        clusterid: this.resource.id
      }).then(response => {
        if (response.listdedicatedclustersresponse.dedicatedcluster &&
            response.listdedicatedclustersresponse.dedicatedcluster.length > 0) {
          this.dedicatedDomainId = response.listdedicatedclustersresponse.dedicatedcluster[0].domainid
          this.dedicatedAccountId = response.listdedicatedclustersresponse.dedicatedcluster[0].accountid
        }
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    fetchDedicatedHosts () {
      api('listDedicatedHosts', {
        hostid: this.resource.id
      }).then(response => {
        if (response.listdedicatedhostsresponse.dedicatedhost &&
            response.listdedicatedhostsresponse.dedicatedhost.length > 0) {
          this.dedicatedDomainId = response.listdedicatedhostsresponse.dedicatedhost[0].domainid
          this.dedicatedAccountId = response.listdedicatedhostsresponse.dedicatedhost[0].accountid
        }
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    releaseDedidcatedZone () {
      api('releaseDedicatedZone', {
        zoneid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.releasededicatedzoneresponse.jobid,
          successMessage: `Successfully released dedicated zone`,
          successMethod: () => {
            this.parentFetchData()
            this.dedicatedDomainId = null
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully released dedicated zone',
              jobid: response.releasededicatedzoneresponse.jobid,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to release dedicated zone',
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: `Releasing dedicated zone...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    releaseDedidcatedPod () {
      api('releaseDedicatedPod', {
        podid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.releasededicatedpodresponse.jobid,
          successMessage: `Successfully released dedicated pod`,
          successMethod: () => {
            this.parentFetchData()
            this.dedicatedDomainId = null
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully released dedicated pod',
              jobid: response.releasededicatedpodresponse.jobid,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to release dedicated pod',
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: `Releasing dedicated pod...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    releaseDedidcatedCluster () {
      api('releaseDedicatedCluster', {
        clusterid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.releasededicatedclusterresponse.jobid,
          successMessage: `Successfully released dedicated cluster`,
          successMethod: () => {
            this.parentFetchData()
            this.dedicatedDomainId = null
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully released dedicated cluster',
              jobid: response.releasededicatedclusterresponse.jobid,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to release dedicated cluster',
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: `Releasing dedicated cluster...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    releaseDedidcatedHost () {
      api('releaseDedicatedHost', {
        hostid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.releasededicatedhostresponse.jobid,
          successMessage: `Successfully released dedicated host`,
          successMethod: () => {
            this.parentFetchData()
            this.dedicatedDomainId = null
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully released dedicated host',
              jobid: response.releasededicatedhostresponse.jobid,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to release dedicated host',
          errorMethod: () => {
            this.parentFetchData()
          },
          loadingMessage: `Releasing dedicated host...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    handleRelease () {
      this.modalActive = false
      if (this.$route.meta.name === 'zone') {
        this.releaseDedidcatedZone()
      }
      if (this.$route.meta.name === 'pod') {
        this.releaseDedidcatedPod()
      }
      if (this.$route.meta.name === 'cluster') {
        this.releaseDedidcatedCluster()
      }
      if (this.$route.meta.name === 'host') {
        this.releaseDedidcatedHost()
      }
    }
  }
}
</script>
