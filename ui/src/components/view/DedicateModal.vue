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
  <a-modal
    v-model="dedicatedDomainModal"
    :title="label"
    @cancel="closeModal"
    @ok="handleDedicateForm">
    <DedicateDomain
      @domainChange="id => domainId = id"
      @accountChange="id => dedicatedAccount = id"
      :error="domainError" />
  </a-modal>
</template>

<script>
import { api } from '@/api'
import DedicateDomain from './DedicateDomain'

export default {
  components: {
    DedicateDomain
  },
  inject: ['parentFetchData'],
  props: {
    active: {
      type: Boolean,
      required: true
    },
    label: {
      type: String,
      required: true
    },
    resource: {
      type: Object,
      required: true
    },
    fetchData: {
      type: Function,
      required: true
    }
  },
  data () {
    return {
      dedicatedDomainModal: false,
      domainId: null,
      dedicatedAccount: null,
      domainError: false
    }
  },
  watch: {
    active () {
      this.dedicatedDomainModal = this.active
    }
  },
  mounted () {
    this.dedicatedDomainModal = this.active
  },
  methods: {
    fetchParentData () {
      this.fetchData()
    },
    closeModal () {
      this.$emit('close')
    },
    dedicateZone () {
      if (!this.domainId) {
        this.domainError = true
        return
      }
      api('dedicateZone', {
        zoneId: this.resource.id,
        domainId: this.domainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicatezoneresponse.jobid,
          successMessage: `Successfully dedicated zone`,
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully dedicated zone',
              jobid: response.dedicatezoneresponse.jobid,
              description: `Domain ID: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to dedicate zone',
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: `Dedicating zone...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
      })
    },
    dedicatePod () {
      if (!this.domainId) {
        this.domainError = true
        return
      }
      api('dedicatePod', {
        podId: this.resource.id,
        domainId: this.domainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicatepodresponse.jobid,
          successMessage: `Successfully dedicated pod`,
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully dedicated pod',
              jobid: response.dedicatepodresponse.jobid,
              description: `Domain ID: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to dedicate pod',
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: `Dedicating pod...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
      })
    },
    dedicateCluster () {
      if (!this.domainId) {
        this.domainError = true
        return
      }
      api('dedicateCluster', {
        clusterId: this.resource.id,
        domainId: this.domainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicateclusterresponse.jobid,
          successMessage: `Successfully dedicated cluster`,
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully dedicated cluster',
              jobid: response.dedicateclusterresponse.jobid,
              description: `Domain ID: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to dedicate cluster',
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: `Dedicating cluster...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
      })
    },
    dedicateHost () {
      if (!this.domainId) {
        this.domainError = true
        return
      }
      api('dedicateHost', {
        hostId: this.resource.id,
        domainId: this.domainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicatehostresponse.jobid,
          successMessage: `Successfully dedicated host`,
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: 'Successfully dedicated host',
              jobid: response.dedicatehostresponse.jobid,
              description: `Domain ID: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: 'Failed to dedicate host',
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: `Dedicating host...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
      })
    },
    handleDedicateForm () {
      if (this.$route.meta.name === 'zone') {
        this.dedicateZone()
      }
      if (this.$route.meta.name === 'pod') {
        this.dedicatePod()
      }
      if (this.$route.meta.name === 'cluster') {
        this.dedicateCluster()
      }
      if (this.$route.meta.name === 'host') {
        this.dedicateHost()
      }
    }
  }
}

</script>
