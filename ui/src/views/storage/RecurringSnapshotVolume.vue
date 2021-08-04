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
  <div class="snapshot-layout">
    <a-tabs defaultActiveKey="1" :animated="false">
      <a-tab-pane :tab="$t('label.schedule')" key="1">
        <FormSchedule
          :loading="loading"
          :resource="resource"
          :dataSource="dataSource"
          @close-action="closeAction"
          @refresh="handleRefresh"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.scheduled.snapshots')" key="2">
        <ScheduledSnapshots
          :loading="loading"
          :resource="resource"
          :dataSource="dataSource"
          @refresh="handleRefresh"
          @close-action="closeAction"/>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { api } from '@/api'
import FormSchedule from '@/views/storage/FormSchedule'
import ScheduledSnapshots from '@/views/storage/ScheduledSnapshots'

export default {
  name: 'RecurringSnapshotVolume',
  components: {
    FormSchedule,
    ScheduledSnapshots
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      dataSource: []
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      const params = {}
      this.dataSource = []
      this.loading = true
      params.volumeid = this.resource.id
      api('listSnapshotPolicies', params).then(json => {
        this.loading = false
        const listSnapshotPolicies = json.listsnapshotpoliciesresponse.snapshotpolicy
        if (listSnapshotPolicies && listSnapshotPolicies.length > 0) {
          this.dataSource = listSnapshotPolicies
        }
      })
    },
    handleRefresh () {
      this.fetchData()
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
  .snapshot-layout {
    max-width: 600px;
  }
</style>
