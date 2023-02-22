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
  <a-table
    class="table"
    size="small"
    :columns="columns"
    :dataSource="jobs"
    :rowKey="item => item.id"
    :pagination="false" >
    <template #cmd="{ text }">
      {{ text.split('.').pop() }}
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'AsyncJobsTab',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      jobs: [],
      columns: [
        {
          title: this.$t('label.command'),
          dataIndex: 'cmd',
          slots: { customRender: 'cmd' }
        },
        {
          title: this.$t('label.resourcetype'),
          dataIndex: 'jobinstancetype'
        },
        {
          title: this.$t('label.created'),
          dataIndex: 'created'
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem) {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.jobs = []
      api('listAsyncJobs', { listall: true, isrecursive: true }).then(json => {
        this.jobs = json.listasyncjobsresponse.asyncjobs || []
      })
    }
  }
}
</script>
