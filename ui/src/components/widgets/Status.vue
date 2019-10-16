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
  <a-tooltip placement="bottom">
    <template slot="title">
      {{ text }}
    </template>
    <a-badge style="display: inline-flex" :title="text" :status="getBadgeStatus(text)" :text="getText()" />
  </a-tooltip>
</template>

<script>

export default {
  name: 'Status',
  props: {
    text: {
      type: String,
      required: true
    },
    displayText: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    getText () {
      if (this.displayText && this.text) {
        return this.text.charAt(0).toUpperCase() + this.text.slice(1)
      }
      return ''
    },
    getBadgeStatus (state) {
      var status = 'default'
      switch (state) {
        case 'Running':
        case 'Ready':
        case 'Up':
        case 'BackedUp':
        case 'Allocated':
        case 'Implemented':
        case 'Enabled':
        case 'enabled':
        case 'Active':
        case 'Completed':
        case 'Started':
          status = 'success'
          break
        case 'Disabled':
        case 'Down':
        case 'Error':
        case 'Stopped':
          status = 'error'
          break
        case 'Migrating':
        case 'Starting':
        case 'Stopping':
        case 'Scheduled':
          status = 'processing'
          break
        case 'Alert':
        case 'Created':
          status = 'warning'
          break
      }
      return status
    }
  }
}
</script>

<style scoped>
/deep/ .ant-badge-status-dot {
  width: 12px;
  height: 12px;
  margin-top: 5px;
}
</style>
