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
  <a-list
    size="small"
    :dataSource="fetchBackupMetadataFields()">
    <template #renderItem="{item}">
      <a-list-item v-if="backupMetadata[item] !== undefined">
        <div style="width: 100%">
          <strong>{{ getFieldLabel(item) }}</strong>
          <br/>
          <div v-if="item === 'nics'">
            <div v-for="(nic, idx) in getNicEntities()" :key="idx">
              <router-link :to="{ path: '/guestnetwork/' + nic.networkid }">
                {{ nic.networkname || nic.networkid }}
              </router-link>
              <br/>
              IP Address: {{ nic.ipaddress }}
              <span v-if="nic.ip6address && nic.ip6address !== 'null'"> | IPv6: {{ nic.ip6address }}</span>
              <br/>
              MAC Address: {{ nic.macaddress }}
              <div v-if="idx < getNicEntities().length - 1" style="margin: 6px 0; border-bottom: 1px dashed #d9d9d9;"></div>
            </div>
          </div>
          <div v-else-if="item === 'templateid'">
            <router-link :to="{ path: '/' + (backupMetadata.isiso === 'true' ? 'iso' : 'template') + '/' + backupMetadata[item] }">
              {{ getTemplateDisplayName() }}
            </router-link>
          </div>
          <div v-else-if="item === 'serviceofferingid'">
            <router-link :to="{ path: '/computeoffering/' + backupMetadata[item] }">
              {{ getServiceOfferingDisplayName() }}
            </router-link>
          </div>
          <div v-else-if="item === 'vmsettings'">
            <div v-for="(value, key, index) in getVmSettings()" :key="key">
              {{ key }}
              <br/>
              {{ value }}
              <div v-if="index < Object.keys(getVmSettings()).length - 1" style="margin: 6px 0; border-bottom: 1px dashed #d9d9d9;"></div>
            </div>
          </div>
          <div v-else>{{ backupMetadata[item] }}</div>
        </div>
      </a-list-item>
    </template>
  </a-list>
</template>

<script>

export default {
  name: 'BackupMetadata',
  data () {
    return {
      backupMetadata: this.resource.vmdetails,
      templateName: '',
      isoName: ''
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  methods: {
    fetchBackupMetadataFields () {
      if (!this.backupMetadata || Object.keys(this.backupMetadata).length === 0) {
        return []
      }
      const fieldOrder = []
      fieldOrder.push('templateid')
      if (this.backupMetadata.isiso === 'true') {
        fieldOrder.push('hypervisor')
      }
      fieldOrder.push('serviceofferingid')
      fieldOrder.push('nics')
      fieldOrder.push('vmsettings')

      return fieldOrder.filter(field => this.backupMetadata[field] !== undefined)
    },
    getNicEntities () {
      if (this.backupMetadata.nics) {
        return JSON.parse(this.backupMetadata.nics)
      }
      return []
    },
    getFieldLabel (field) {
      if (field === 'templateid') {
        return this.backupMetadata.isiso === 'true' ? this.$t('label.iso') : this.$t('label.template')
      }
      if (field === 'vmsettings') {
        return this.$t('label.settings')
      }
      return this.$t('label.' + String(field).toLowerCase())
    },
    getTemplateDisplayName () {
      if (this.backupMetadata.templatename) {
        return this.backupMetadata.templatename
      }
      return this.backupMetadata.isiso === 'true' ? this.isoName : this.templateName
    },
    getServiceOfferingDisplayName () {
      if (this.backupMetadata.serviceofferingname) {
        return this.backupMetadata.serviceofferingname
      }
      return this.backupMetadata.serviceofferingid
    },
    getVmSettings () {
      if (this.backupMetadata.vmsettings) {
        return JSON.parse(this.backupMetadata.vmsettings)
      }
      return {}
    }
  }
}
</script>
