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
      <template slot="displaytext" slot-scope="record">
        <span>{{ record.elementName + ' - ' + record.name }}
          <a-tooltip :title="record.nicDescription" placement="top">
            <a-icon type="info-circle" class="table-tooltip-icon" />
          </a-tooltip>
        </span>
      </template>
      <div slot="size" slot-scope="record">
        <span v-if="record.size">
          {{ $bytesToHumanReadableSize(record.size) }}
        </span>
      </div>
      <template slot="selectednetwork" slot-scope="record">
        <span>{{ record.selectednetworkname || '' }}</span>
      </template>
      <template slot="select" slot-scope="record">
        <div style="display: flex; justify-content: flex-end;"><a-button @click="openNicNetworkSelector(record)">{{ record.selectednetworkid ? $t('label.change') : $t('label.select') }}</a-button></div>
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
          title: this.$t('label.nic'),
          scopedSlots: { customRender: 'displaytext' }
        },
        {
          title: this.$t('label.network'),
          scopedSlots: { customRender: 'selectednetwork' }
        },
        {
          title: '',
          scopedSlots: { customRender: 'select' }
        }
      ],
      selectedNicForNetworkSelection: {}
    }
  },
  methods: {
    resetSelection () {
      var nics = this.nics
      this.nics = []
      for (var nic of nics) {
        nic.selectednetworkid = null
        nic.selectednetworkname = ''
      }
      this.nics = nics
      this.updateNicToNetworkSelection()
    },
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
