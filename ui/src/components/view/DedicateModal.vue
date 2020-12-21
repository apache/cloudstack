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
    :maskClosable="false"
    :okText="$t('label.ok')"
    :cancelText="$t('label.cancel')"
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
          successMessage: this.$t('label.zone.dedicated'),
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('label.zone.dedicated'),
              jobid: response.dedicatezoneresponse.jobid,
              description: `${this.$t('label.domain.id')} : ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: this.$t('error.dedicate.zone.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: this.$t('message.dedicating.zone'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
          successMessage: this.$t('label.pod.dedicated'),
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('label.pod.dedicated'),
              jobid: response.dedicatepodresponse.jobid,
              description: `${this.$t('label.domainid')}: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: this.$t('error.dedicate.pod.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: this.$t('message.dedicating.pod'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
          successMessage: this.$t('message.cluster.dedicated'),
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('message.cluster.dedicated'),
              jobid: response.dedicateclusterresponse.jobid,
              description: `${this.$t('label.domainid')}: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: this.$t('error.dedicate.cluster.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: this.$t('message.dedicating.cluster'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
          successMessage: this.$t('message.host.dedicated'),
          successMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('message.host.dedicated'),
              jobid: response.dedicatehostresponse.jobid,
              description: `${this.$t('label.domainid')}: ${this.dedicatedDomainId}`,
              status: 'progress'
            })
          },
          errorMessage: this.$t('error.dedicate.host.failed'),
          errorMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
          },
          loadingMessage: this.$t('message.dedicating.host'),
          catchMessage: this.$t('error.fetching.async.job.result'),
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
