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
  <div style="margin-top: 10px;">
    <label>{{ $t('message.vnf.select.networks') }}</label>
  </div>
  <a-form
    :ref="formRef"
    :model="form"
    :rules="rules">
    <a-table
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.deviceid"
      size="middle"
      :scroll="{ y: 225 }">
      <template #deviceid="{ text }">
        <div>{{ text }}</div>
      </template>
      <template #name="{ text }">
        <div>{{ text }}</div>
      </template>
      <template #required="{ record }">
        <span v-if="record.required">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #management="{ record }">
        <span v-if="record.management">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #description="{record}">
        <span> {{ record.description }} </span>
      </template>
      <template #network="{ record }">
        <a-form-item style="display: block" :name="'nic-' + record.deviceid">
          <a-select
            @change="updateNicNetworkValue($event, record.deviceid)"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option key="" >{{ }}</a-select-option>
            <a-select-option v-for="network in networks" :key="network.id">
              {{ network.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>
    </a-table>
  </a-form>
</template>

<script>
import { ref, reactive } from 'vue'
export default {
  name: 'VnfNicsSelection',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    networks: {
      type: Array,
      default: () => []
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      values: {},
      columns: [
        {
          dataIndex: 'deviceid',
          title: this.$t('label.deviceid'),
          width: '10%',
          slots: { customRender: 'deviceid' }
        },
        {
          dataIndex: 'name',
          title: this.$t('label.name'),
          width: '15%',
          slots: { customRender: 'name' }
        },
        {
          dataIndex: 'required',
          title: this.$t('label.required'),
          width: '10%',
          slots: { customRender: 'required' }
        },
        {
          dataIndex: 'management',
          title: this.$t('label.vnf.nic.management'),
          width: '15%',
          slots: { customRender: 'management' }
        },
        {
          dataIndex: 'description',
          title: this.$t('label.description'),
          width: '35%',
          slots: { customRender: 'description' }
        },
        {
          dataIndex: 'network',
          title: this.$t('label.network'),
          width: '25%',
          slots: { customRender: 'network' }
        }
      ]
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})

      const form = {}
      const rules = {}

      this.form = reactive(form)
      this.rules = reactive(rules)
    },
    updateNicNetworkValue (value, deviceid) {
      this.values[deviceid] = this.networks.filter(network => network.id === value)?.[0] || null
      this.$emit('update-vnf-nic-networks', this.values)
    }
  }
}
</script>

<style lang="less" scoped>
  .ant-table-wrapper {
    margin: 2rem 0;
  }

  :deep(.ant-table-tbody) > tr > td {
    cursor: pointer;
  }

  .ant-form .ant-form-item {
    margin-bottom: 0;
    padding-bottom: 0;
  }
</style>
