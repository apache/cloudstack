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
  <a-list size="small" :dataSource="rows" :loading="loading">
    <template #renderItem="{ item }">
      <a-list-item>
        <div style="width: 100%">
          <strong>{{ $t(item.label) }}</strong>
          <br/>
          <copy-label v-if="item.key === 'id'" :label="resource.id" />
          <a-tag v-else-if="item.key === 'jobstatus'" :color="jobStatusColor(jobDisplayStatus(resource))">{{ $t(jobStatusLabel(jobDisplayStatus(resource))) }}</a-tag>
          <resource-label
            v-else-if="item.key === 'resource'"
            :resourceType="resource.jobinstancetype"
            :resourceId="resource.jobinstanceid"
            :resourceName="resource.jobinstancename || resource.jobinstanceid" />
          <router-link v-else-if="item.key === 'account'" :to="{ path: '/account/' + resource.accountid }">{{ resource.account }}</router-link>
          <router-link v-else-if="item.key === 'domainpath'" :to="{ path: '/domain/' + resource.domainid, query: { tab: 'details' } }">{{ resource.domainpath }}</router-link>
          <div v-else-if="item.key === 'jobresult'" class="job-result-detail">
            <a href="javascript:;" v-clipboard:copy="jobResultPretty(resource.jobresult)" @click="$message.success($t('label.copied.clipboard'))">
              <copy-outlined /> {{ $t('label.copy') }}
            </a>
            <pre>{{ jobResultPretty(resource.jobresult) }}</pre>
          </div>
          <span v-else>{{ item.value }}</span>
        </div>
      </a-list-item>
    </template>
  </a-list>
</template>

<script>
import CopyLabel from '@/components/widgets/CopyLabel'
import ResourceLabel from '@/components/widgets/ResourceLabel'
import { jobStatusLabel, jobStatusColor, jobDisplayStatus, jobResultPretty } from '@/utils/asyncjob'

export default {
  name: 'JobDetailsTab',
  components: {
    CopyLabel,
    ResourceLabel
  },
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
  computed: {
    rows () {
      const job = this.resource
      const rows = [
        { key: 'id', label: 'label.id' },
        { key: 'cmd', label: 'label.command', value: job.cmd },
        { key: 'jobstatus', label: 'label.status' }
      ]
      if (job.cancelrequested) {
        rows.push({ key: 'cancelreason', label: 'label.cancelreason', value: job.cancelreason || '-' })
      }
      if (job.jobinstanceid) {
        rows.push({ key: 'resourcetype', label: 'label.resourcetype', value: job.jobinstancetype })
        rows.push({ key: 'resource', label: 'label.resource' })
      }
      rows.push(
        { key: 'account', label: 'label.account' },
        { key: 'domainpath', label: 'label.domain' },
        { key: 'initmanagementservername', label: 'label.managementserver.initiated', value: job.initmanagementservername },
        { key: 'executingmanagementservername', label: 'label.managementserver.executing', value: job.executingmanagementservername },
        { key: 'completemanagementservername', label: 'label.managementserver.completed', value: job.completemanagementservername },
        { key: 'created', label: 'label.created', value: job.created ? this.$toLocaleDate(job.created) : '-' },
        { key: 'completed', label: 'label.completed', value: job.completed ? this.$toLocaleDate(job.completed) : '-' },
        { key: 'jobresult', label: 'label.jobresult' })
      return rows.filter(row => row.value !== null && row.value !== undefined || !('value' in row))
    }
  },
  methods: {
    jobStatusLabel,
    jobStatusColor,
    jobDisplayStatus,
    jobResultPretty
  }
}
</script>

<style scoped lang="less">
.job-result-detail {
  pre {
    margin-top: 6px;
    max-height: 480px;
    overflow: auto;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
