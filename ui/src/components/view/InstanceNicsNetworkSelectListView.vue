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
    <a-table
      class="top-spaced"
      size="small"
      style="max-height: 250px; overflow-y: auto"
      :columns="nicColumns"
      :dataSource="nics"
      :pagination="false"
      :rowKey="record => record.InstanceID">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'displaytext'">
          <span>{{ record.elementName + ' - ' + record.name }}
            <a-tooltip :title="record.nicDescription" placement="top">
              <info-circle-outlined class="table-tooltip-icon" />
            </a-tooltip>
          </span>
        </template>
        <template v-if="column.key === 'size'">
          <span v-if="record.size">
            {{ $bytesToHumanReadableSize(record.size) }}
          </span>
        </template>
        <template v-if="column.key === 'selectednetwork'">
          <span>{{ record.selectednetworkname || '' }}</span>
        </template>
        <template v-if="column.key === 'select'">
          <div style="display: flex; justify-content: flex-end;"><a-button @click="openNicNetworkSelector(record)">{{ record.selectednetworkid ? $t('label.change') : $t('label.select') }}</a-button></div>
        </template>
      </template>
    </a-table>

    <a-modal
      :visible="!(!selectedNicForNetworkSelection.id)"
      :title="$t('label.select.network')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      :cancelText="$t('label.cancel')"
      @cancel="closeNicNetworkSelector()"
      centered
      width="auto">
      <nic-network-select-form
        :resource="selectedNicForNetworkSelection"
        :zoneid="zoneid"
        :isOpen="!(!selectedNicForNetworkSelection.id)"
        @close-action="closeNicNetworkSelector()"
        @select="handleNicNetworkSelection" />
    </a-modal>
  </div>
</template>

<script>
import NicNetworkSelectForm from '@/components/view/NicNetworkSelectForm'

export default {
  name: 'InstanceNicsNetworkSelectListView',
  components: {
    NicNetworkSelectForm
  },
  props: {
    nics: {
      type: Array,
      required: true
    },
    zoneid: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      nicColumns: [
        {
          key: 'displaytext',
          title: this.$t('label.nic')
        },
        {
          key: 'selectednetwork',
          title: this.$t('label.network')
        },
        {
          key: 'select',
          title: ''
        }
      ],
      selectedNicForNetworkSelection: {}
    }
  },
  methods: {
    openNicNetworkSelector (nic) {
      this.selectedNicForNetworkSelection = nic
    },
    closeNicNetworkSelector () {
      this.selectedNicForNetworkSelection = {}
    },
    handleNicNetworkSelection (nicId, network) {
      for (const nic of this.nics) {
        if (nic.id === nicId) {
          nic.selectednetworkid = network.id
          nic.selectednetworkname = network.name
          break
        }
      }
      this.updateNicToNetworkSelection()
    },
    updateNicToNetworkSelection () {
      var nicToNetworkSelection = []
      for (const nic of this.nics) {
        if (nic.selectednetworkid && nic.selectednetworkid !== -1) {
          nicToNetworkSelection.push({ nic: nic.id, network: nic.selectednetworkid })
        }
      }
      this.$emit('select', nicToNetworkSelection)
    }
  }
}
</script>

<style scoped lang="less">
  .top-spaced {
    margin-top: 20px;
  }
</style>
