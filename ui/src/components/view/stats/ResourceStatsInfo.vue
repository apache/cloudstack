// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <div>
    <div v-if="messages.length > 1">
      <ul>
        <li v-for="(msg, index) in messages" :key="index">
          {{ msg }}
        </li>
      </ul>
    </div>
    <p v-else-if="messages.length === 1">
      {{ messages[0] }}.
    </p>
  </div>
</template>

<script>
export default {
  name: 'ResourceStatsInfo',
  props: {
    resourceType: {
      type: String,
      default: null
    }
  },
  data () {
    return {
      info: [
        {
          resourceType: 'CHART',
          messageList: [
            this.$t('message.chart.statistic.info'),
            this.$t('message.chart.statistic.info.hypervisor.additionals')
          ]
        },
        {
          resourceType: 'CPU',
          messageList: [
            this.$t('message.cpu.usage.info')
          ]
        },
        {
          resourceType: 'MEM',
          messageList: [
            this.$t('message.memory.usage.info.negative.value'),
            this.$t('message.memory.usage.info.hypervisor.additionals')
          ]
        },
        {
          resourceType: 'NET',
          messageList: [
            this.$t('message.network.usage.info.sum.of.vnics'),
            this.$t('message.network.usage.info.data.points')
          ]
        },
        {
          resourceType: 'DISK',
          messageList: [
            this.$t('message.disk.usage.info.sum.of.disks'),
            this.$t('message.disk.usage.info.data.points')
          ]
        }
      ],
      messages: []
    }
  },
  mounted () {
    for (const element of this.info) {
      if (element.resourceType === this.resourceType) {
        this.messages = element.messageList
        if (this.$route.fullPath.startsWith('/volume/')) {
          this.messages = this.messages.filter(x => x !== this.$t('message.disk.usage.info.sum.of.disks'))
        }
      }
    }
  }
}
</script>
