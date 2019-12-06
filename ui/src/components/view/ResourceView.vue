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
  <resource-layout>
    <div slot="left">
      <slot name="info-card">
        <info-card :resource="resource" :loading="loading" />
      </slot>
    </div>
    <a-spin :spinning="loading" slot="right">
      <a-card
        class="spin-content"
        :bordered="true"
        style="width:100%">
        <a-tabs
          style="width: 100%"
          :animated="false"
          :defaultActiveKey="tabs[0].name"
          @change="onTabChange" >
          <a-tab-pane
            v-for="tab in tabs"
            :tab="$t(tab.name)"
            :key="tab.name"
            v-if="'show' in tab ? tab.show(resource, $route) : true">
            <component :is="tab.component" :resource="resource" :loading="loading" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-spin>
  </resource-layout>
</template>

<script>

import DetailsTab from '@/components/view/DetailsTab'
import InfoCard from '@/components/view/InfoCard'
import ResourceLayout from '@/layouts/ResourceLayout'

export default {
  name: 'ResourceView',
  components: {
    InfoCard,
    ResourceLayout
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    tabs: {
      type: Array,
      default: function () {
        return [{
          name: 'details',
          component: DetailsTab
        }]
      }
    }
  },
  methods: {
    onTabChange (key) {
      this.activeTab = key
    }
  }
}
</script>

<style lang="less" scoped>
</style>
