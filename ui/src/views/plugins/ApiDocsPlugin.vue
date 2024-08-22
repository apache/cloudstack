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
            :options="options.filter(value => value.value.toLowerCase().includes(query.toLowerCase()))"
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
              <a-list-item
                v-if="item.toLowerCase().includes(query.toLowerCase())"
                @click="showApi(item)"
                style="padding-left: 12px"
                :class="selectedApi === item ? 'selected-item' : ''">
                {{ item }} <a-tag v-if="$store.getters.apis[item].isasync" color="blue">async</a-tag>
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
          <span v-if="selectedApi && selectedApi in $store.getters.apis">
            <h2>{{ selectedApi }}
              <a-tag v-if="$store.getters.apis[selectedApi].isasync" color="blue">Asynchronous API</a-tag>
              <a-tag v-if="$store.getters.apis[selectedApi].since">Since {{ $store.getters.apis[selectedApi].since }}</a-tag>
              <tooltip-button
                tooltipPlacement="right"
                :tooltip="$t('label.copy') + ' ' + selectedApi"
                icon="CopyOutlined"
                type="outlined"
                size="small"
                @onClick="$message.success($t('label.copied.clipboard'))"
                :copyResource="selectedApi" />
            </h2>
            <p>{{ $store.getters.apis[selectedApi].description }}</p>
            <h3>{{ $t('label.request') }} {{ $t('label.params') }}:</h3>
            <a-table
              :columns="[{title: $t('label.name'), dataIndex: 'name'}, {title: $t('label.required'), dataIndex: 'required'}, {title: $t('label.type'), dataIndex: 'type'}, {title: $t('label.description'), dataIndex: 'description'}]"
              :data-source="selectedParams"
              :pagination="false"
              size="small">
              <template #bodyCell="{text, column, record}">
                <a-tag v-if="record.since && column.dataIndex === 'description'">Since {{ record.since }}</a-tag>
                <span v-if="record.required === true"><strong>{{ text }}</strong></span>
                <span v-else>{{ text }}</span>
              </template>
            </a-table>
            <br/>
            <h3>{{ $t('label.response') }} {{ $t('label.params') }}:</h3>
            <a-table
              :columns="[{title: $t('label.name'), dataIndex: 'name'}, {title: $t('label.type'), dataIndex: 'type'}, {title: $t('label.description'), dataIndex: 'description'}]"
              :data-source="selectedResponse"
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
              :title="$t('label.download') + ' CloudStack CloudMonkey CLI'"
              sub-title="For API automation and orchestration"
            >
              <template #extra>
                <a-button type="primary"><a href="https://github.com/apache/cloudstack-cloudmonkey/releases" target="_blank">{{ $t('label.download') }} CLI</a></a-button>
                <a-button><a href="https://github.com/apache/cloudstack-cloudmonkey/wiki/Usage" target="_blank">{{ $t('label.open.documentation') }} (CLI)</a></a-button>
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
      selectedApi: '',
      selectedParams: [],
      selectedResponse: [],
      showKeys: false,
      userkeys: {},
      options: [
        { value: 'VirtualMachine', label: 'Instance' },
        { value: 'Kubernetes', label: 'Kubernetes' },
        { value: 'Volume', label: 'Volume' },
        { value: 'Snapshot', label: 'Snapshot' },
        { value: 'Backup', label: 'Backup' },
        { value: 'Network', label: 'Network' },
        { value: 'IpAddress', label: 'IP Address' },
        { value: 'VPN', label: 'VPN' },
        { value: 'VPC', label: 'VPC' },
        { value: 'NetworkACL', label: 'Network ACL' },
        { value: 'SecurityGroup', label: 'Security Group' },
        { value: 'Template', label: 'Template' },
        { value: 'ISO', label: 'ISO' },
        { value: 'SSH', label: 'SSH' },
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
      this.selectedApi = api
      this.selectedParams = this.$store.getters.apis[api].params
        .sort((a, b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
        .sort((a, b) => (a.required > b.required) ? -1 : ((b.required > a.required) ? 1 : 0))
        .filter(value => Object.keys(value).length > 0)
      this.selectedResponse = this.$store.getters.apis[api].response.filter(value => Object.keys(value).length > 0)
    }
  }
}
</script>
