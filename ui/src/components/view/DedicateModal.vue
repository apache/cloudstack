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
    :visible="dedicatedDomainModal"
    :title="label"
    :closable="true"
    :maskClosable="false"
    :footer="null"
    @cancel="closeModal">
    <div v-ctrl-enter="handleDedicateForm">
      <DedicateDomain
        @domainChange="id => domainId = id"
        @accountChange="id => dedicatedAccount = id"
        :error="domainError" />
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleDedicateForm">{{ $t('label.ok') }}</a-button>
      </div>
    </div>
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
      domainError: false,
      isSubmitted: false
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
          title: this.$t('label.dedicate.zone'),
          description: `${this.$t('label.domain.id')} : ${this.domainId}`,
          successMessage: `${this.$t('label.zone.dedicated')}`,
          successMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          errorMessage: this.$t('error.dedicate.zone.failed'),
          errorMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.dedicating.zone'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
        this.isSubmitted = false
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
          title: this.$t('label.dedicate.pod'),
          description: `${this.$t('label.domain.id')} : ${this.domainId}`,
          successMessage: this.$t('label.pod.dedicated'),
          successMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          errorMessage: this.$t('error.dedicate.pod.failed'),
          errorMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.dedicating.pod'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
        this.isSubmitted = false
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
          title: this.$t('label.dedicate.cluster'),
          description: `${this.$t('label.domain.id')} : ${this.domainId}`,
          successMessage: this.$t('message.cluster.dedicated'),
          successMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          errorMessage: this.$t('error.dedicate.cluster.failed'),
          errorMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.dedicating.cluster'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
        this.isSubmitted = false
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
          title: this.$t('label.dedicate.host'),
          description: `${this.$t('label.domain.id')} : ${this.domainId}`,
          successMessage: this.$t('message.host.dedicated'),
          successMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainId = this.domainId
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          errorMessage: this.$t('error.dedicate.host.failed'),
          errorMethod: () => {
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          },
          loadingMessage: this.$t('message.dedicating.host'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.fetchParentData()
            this.dedicatedDomainModal = false
            this.isSubmitted = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.dedicatedDomainModal = false
        this.isSubmitted = false
      })
    },
    handleDedicateForm () {
      if (this.isSubmitted) {
        return
      }
      this.isSubmitted = true
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
