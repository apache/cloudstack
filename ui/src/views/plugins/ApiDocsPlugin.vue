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
  <div>
    <resource-layout>
      <template #left>
        <a-card :bordered="false">
          <a-auto-complete
            v-model:value="query"
            :options="options"
            style="width: 100%"
            >
            <a-input-search
                size="default"
                :placeholder="$t('label.search')"
                v-model:value="query"
                allow-clear
                enter-button
                >
                <template #prefix><search-outlined /></template>
            </a-input-search>
          </a-auto-complete>
          <a-list style="margin-top: 12px; height:580px; overflow-y: scroll;" size="small" :data-source="Object.keys($store.getters.apis).sort()">
            <template #renderItem="{ item }">
              <a>
              <a-list-item v-if="item.toLowerCase().includes(query.toLowerCase())" @click="showApi(item)">
                <span v-if="selected === item">
                  <strong>{{ item }}</strong> <a-tag v-if="$store.getters.apis[item].isasync" color="blue">async</a-tag>
                </span>
                <span v-else>
                  {{ item }} <a-tag v-if="$store.getters.apis[item].isasync" color="blue">async</a-tag>
                </span>
              </a-list-item>
              </a>
            </template>
          </a-list>
          <a-divider style="margin-bottom: 12px" />
          <span>{{ Object.keys($store.getters.apis).length }} {{ $t('label.api.docs.count') }}</span>
        </a-card>
      </template>
      <template #right>
        <a-card
          class="spin-content"
          :bordered="true"
          style="width: 100%; overflow-x: auto">
          <span v-if="selected && selected in $store.getters.apis">
            <h2>{{ selected }} <a-tag v-if="$store.getters.apis[selected].isasync" color="blue">Asynchronous API</a-tag></h2>
            <p>{{ $store.getters.apis[selected].description }}</p>
            <h3>Request parameters:</h3>
            <a-table
              :columns="[{title: 'Parameter Name', dataIndex: 'name'}, {title: 'Required', dataIndex: 'required'}, {title: 'Type', dataIndex: 'type'}, {title: 'Description', dataIndex: 'description'}]"
              :data-source="$store.getters.apis[selected].params.sort((a, b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0)).sort((a, b) => (a.required > b.required) ? -1 : ((b.required > a.required) ? 1 : 0))"
              :pagination="false"
              size="small">
              <template #bodyCell="{text, record}">
                <span v-if="record.required === true"> <strong>{{ text }}</strong> </span>
                <span v-else>  {{ text }} </span>
              </template>
            </a-table>
            <br/>
            <h3>Response keys:</h3>
            <a-table
              :columns="[{title: 'Response Name', dataIndex: 'name'}, {title: 'Type', dataIndex: 'type'}, {title: 'Description', dataIndex: 'description'}]"
              :data-source="$store.getters.apis[selected].response"
              :pagination="false"
              size="small" />
          </span>
          <span v-else>
            <a-alert
              :message="$t('label.api.docs')"
              type="info"
              show-icon
              banner>
              <template #description>
                <a href="https://docs.cloudstack.apache.org/en/latest/developersguide/dev.html" target="_blank">{{ $t('label.api.docs.description') }}</a>
              </template>
            </a-alert>

            <a-result
              status="success"
              title="Download CloudStack CloudMonkey CLI"
              sub-title="For API automation and orchestration"
            >
              <template #extra>
                <a-button type="primary"><a href="https://github.com/apache/cloudstack-cloudmonkey/releases" target="_blank">Download CLI</a></a-button>
                <a-button><a href="https://github.com/apache/cloudstack-cloudmonkey/wiki/Usage" target="_blank">Read CLI Documentation</a></a-button>
                <br/>
                <br/>
                <div v-if="showKeys">
                  <key-outlined />
                  <strong>
                    {{ $t('label.apikey') }}
                    <tooltip-button
                      tooltipPlacement="right"
                      :tooltip="$t('label.copy') + ' ' + $t('label.apikey')"
                      icon="CopyOutlined"
                      type="dashed"
                      size="small"
                      @onClick="$message.success($t('label.copied.clipboard'))"
                      :copyResource="userkeys.apikey" />
                  </strong>
                  <div>
                    {{ userkeys.apikey.substring(0, 20) }}...
                  </div>
                  <br/>
                  <lock-outlined />
                  <strong>
                    {{ $t('label.secretkey') }}
                    <tooltip-button
                      tooltipPlacement="right"
                      :tooltip="$t('label.copy') + ' ' + $t('label.secretkey')"
                      icon="CopyOutlined"
                      type="dashed"
                      size="small"
                      @onClick="$message.success($t('label.copied.clipboard'))"
                      :copyResource="userkeys.secretkey" />
                  </strong>
                  <div>
                    {{ userkeys.secretkey.substring(0, 20) }}...
                  </div>
                </div>
              </template>
            </a-result>
          </span>
        </a-card>
      </template>
    </resource-layout>
  </div>
</template>

<script>
import { api } from '@/api'

import ResourceLayout from '@/layouts/ResourceLayout'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'ApiDocsPlugin',
  components: {
    ResourceLayout,
    TooltipButton
  },
  data () {
    return {
      query: '',
      selected: '',
      showKeys: false,
      userkeys: {},
      options: [
        { value: 'VirtualMachine', label: 'Instance' },
        { value: 'Kubernetes', label: 'Kubernetes' },
        { value: 'SSH', label: 'SSH' },
        { value: 'Volume', label: 'Volume' },
        { value: 'Snapshot', label: 'Snapshot' },
        { value: 'Backup', label: 'Backup' },
        { value: 'Network', label: 'Network' },
        { value: 'VPN', label: 'VPN' },
        { value: 'VPC', label: 'VPC' },
        { value: 'NetworkACL', label: 'Network ACL' },
        { value: 'SecurityGroup', label: 'Security Group' },
        { value: 'IpAddress', label: 'IP Address' },
        { value: 'Template', label: 'Template' },
        { value: 'ISO', label: 'ISO' },
        { value: 'Project', label: 'Project' },
        { value: 'Account', label: 'Account' },
        { value: 'User', label: 'User' },
        { value: 'Event', label: 'Event' },
        { value: 'Offering', label: 'Offering' },
        { value: 'Zone', label: 'Zone' }
      ]
    }
  },
  created () {
    if (!('getUserKeys' in this.$store.getters.apis)) {
      return
    }
    api('getUserKeys', { id: this.$store.getters.userInfo.id }).then(json => {
      this.userkeys = json.getuserkeysresponse.userkeys
      if (this.userkeys && this.userkeys.secretkey) {
        this.showKeys = true
      }
    })
  },
  methods: {
    showApi (api) {
      console.log(this.$route.params)
      console.log(api)
      this.selected = api
    }
  }
}
</script>

<style scoped lang="less">
</style>
