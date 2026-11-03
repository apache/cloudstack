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
  <div class="register-template">
    <a-card class="ant-form-text" style="text-align: justify; margin: 10px 0; padding: 15px;" v-html="$t('message.desc.register.template')" />
    <div v-if="standalone" class="zone-select">
      <span class="zone-select__label">{{ $t('label.zone') }}</span>
      <a-select
        v-model:value="selectedZoneId"
        :loading="zonesLoading"
        :disabled="submitting"
        :placeholder="$t('message.catalog.select.zone')"
        showSearch
        optionFilterProp="label"
        style="width: 100%"
        @change="refreshZone"
      >
        <a-select-option
          v-for="zone in zones"
          :key="zone.id"
          :value="zone.id"
          :label="zone.name"
          :disabled="!isZoneEligible(zone)"
        >
          {{ zone.name }}
          <a-tag v-if="zone.type === 'Edge'" style="margin-left: 8px">{{ $t('label.edge') }}</a-tag>
          <a-tag v-if="zone.allocationstate === 'Disabled'" style="margin-left: 8px">{{ $t('label.disabled') }}</a-tag>
          <span v-if="!isZoneEligible(zone)" class="zone-select__reason">{{ $t('message.catalog.no.kvm') }}</span>
        </a-select-option>
      </a-select>
    </div>
    <a-alert v-if="catalogError" type="error" show-icon :message="catalogError" style="margin-top: 8px" />
    <a-alert v-if="!hasImageStore" type="warning" show-icon :message="$t('message.catalog.no.image.store')" style="margin-top: 8px" />
    <a-alert v-if="noClusters" type="info" show-icon :message="$t('message.catalog.no.clusters')" style="margin-top: 8px" />
    <div v-if="effectiveZoneId" class="switches">
      <span class="switch-item">
        <a-switch size="small" v-model:checked="showOtherArchs" :disabled="submitting" />
        {{ $t('label.show.other.archs') }}
      </span>
      <span class="switch-item">
        <a-switch size="small" v-model:checked="showRegistered" :disabled="submitting" />
        {{ $t('label.show.registered') }}
      </span>
    </div>
    <a-empty v-if="standalone && !effectiveZoneId" :description="$t(hasEligibleZone ? 'message.catalog.select.zone' : 'message.catalog.no.kvm')" style="margin: 40px 0" />
    <a-table
      v-else
      :columns="columns"
      :dataSource="visibleTemplates"
      :rowSelection="rowSelection"
      :loading="loading"
      :scroll="{ x: 760, y: 450 }"
      size="middle"
      :rowKey="record => record.id"
      :pagination="false"
      class="form-content"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <os-logo :osName="record.name" size="xl" />
          {{ record.name }}
        </template>
        <template v-else-if="column.key === 'status'">
          {{ statusText(record) }}
        </template>
      </template>
    </a-table>
    <div class="form-action">
      <a-button v-if="standalone" class="button-back" :disabled="submitting" @click="handleClose">{{ $t('label.close') }}</a-button>
      <a-button v-else class="button-back" @click="handleDone">{{ selectedRowKeys.length > 0 ? $t('label.done') : $t('label.skip') }}</a-button>
      <div class="register-actions">
        <template v-if="effectiveZoneId">
          <a-checkbox v-model:checked="makePublic" :disabled="registerDisabled">{{ $t('label.ispublic') }}</a-checkbox>
          <a-checkbox v-model:checked="makeFeatured" :disabled="registerDisabled">{{ $t('label.isfeatured') }}</a-checkbox>
        </template>
        <a-button
          class="register-button"
          type="primary"
          :loading="submitting"
          :disabled="registerDisabled"
          @click="handleSubmit"
          ref="submit"
        >{{ $t('label.register.template') }}</a-button>
      </div>
    </div>

    <a-modal
      :visible="showAlert"
      :footer="null"
      style="top: 20px;"
      centered
      width="auto"
      @cancel="showAlert = false"
    >
      <template #title>
        {{ $t('label.warning') }}
      </template>
      <a-alert type="warning">
        <template #message>
          <span v-html="$t('message.warn.select.template')" />
        </template>
      </a-alert>
      <a-divider style="margin-top: 0;"></a-divider>
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import OsLogo from '@/components/widgets/OsLogo'

// Lower-case the host, drop the scheme, any query/fragment and trailing slashes.
export const normalizeUrl = (url) => {
  const stripped = String(url || '').trim().replace(/^[a-z][a-z0-9+.-]*:\/\//i, '').replace(/[?#].*$/, '').replace(/\/+$/, '')
  const slash = stripped.indexOf('/')
  return slash === -1 ? stripped.toLowerCase() : stripped.slice(0, slash).toLowerCase() + stripped.slice(slash)
}

export default {
  name: 'ZoneWizardRegisterTemplate',
  components: {
    OsLogo
  },
  inheritAttrs: false,
  props: {
    zoneid: {
      type: String,
      required: false
    },
    arch: {
      type: String,
      required: false
    },
    zoneSuperType: {
      type: String,
      required: false
    }
  },
  emits: ['refresh-data', 'close-action'],
  data: () => ({
    columns: null,
    loading: false,
    catalog: [],
    catalogError: null,
    predefinedTemplates: [],
    rowKey: 0,
    selectedRowKeys: [],
    defaultOsTypeId: null,
    deployedTemplates: {},
    templateStates: {},
    rowErrors: {},
    submitting: false,
    registeredAny: false,
    showAlert: false,
    zones: [],
    zonesLoading: false,
    selectedZoneId: undefined,
    zoneClusters: {},
    hasImageStore: true,
    showOtherArchs: false,
    showRegistered: false,
    makePublic: true,
    makeFeatured: true,
    zoneRequest: 0
  }),
  created () {
    this.initForm()
  },
  async mounted () {
    await this.fetchCatalog()
    if (this.standalone) {
      await this.fetchZones()
    } else {
      await this.refreshZone()
    }
  },
  computed: {
    standalone () {
      return !this.zoneid
    },
    effectiveZoneId () {
      return this.zoneid || this.selectedZoneId
    },
    selectedZone () {
      return this.zones.find(zone => zone.id === this.selectedZoneId)
    },
    effectiveSuperType () {
      return this.zoneSuperType || (this.selectedZone?.type === 'Edge' ? 'Edge' : 'Core')
    },
    effectiveArchs () {
      if (this.arch) {
        return [this.arch]
      }
      return this.zoneClusters[this.effectiveZoneId]?.archs || []
    },
    noClusters () {
      return this.standalone && !!this.selectedZoneId && !this.zoneClusters[this.selectedZoneId]
    },
    hasEligibleZone () {
      return this.zones.some(zone => this.isZoneEligible(zone))
    },
    visibleTemplates () {
      return this.predefinedTemplates.filter(template => {
        if (template.reason && !this.showOtherArchs) {
          return false
        }
        return !(template.preRegistered && !this.showRegistered)
      })
    },
    registerDisabled () {
      if (this.submitting || !this.hasImageStore) {
        return true
      }
      return this.standalone && (this.selectedRowKeys.length === 0 || !!this.catalogError)
    },
    rowSelection () {
      return {
        selectedRowKeys: this.selectedRowKeys || [],
        onChange: this.onSelectRow,
        getCheckboxProps: (record) => {
          return {
            disabled: record.disabled
          }
        }
      }
    }
  },
  methods: {
    async initForm () {
      this.columns = [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          key: 'name',
          width: 200,
          sorter: (a, b) => { return genericCompare(a.name || '', b.name || '') }
        },
        {
          title: this.$t('label.arch'),
          dataIndex: 'arch',
          key: 'arch',
          width: 80,
          sorter: (a, b) => { return genericCompare(a.arch || '', b.arch || '') }
        },
        {
          title: this.$t('label.url'),
          dataIndex: 'url',
          key: 'url',
          width: 300,
          ellipsis: true
        },
        {
          title: this.$t('label.status'),
          key: 'status',
          width: 220
        }
      ]
      this.defaultOsTypeId = await this.fetchOsTypeId('Other Linux (64-bit)')
    },
    handleDone () {
      this.$emit('refresh-data')
    },
    handleClose () {
      if (this.registeredAny) {
        this.$emit('refresh-data')
      }
      this.$emit('close-action')
    },
    async handleSubmit () {
      await this.stepRegisterTemplates()
    },
    onSelectRow (value) {
      this.selectedRowKeys = value
    },
    statusText (record) {
      if (this.deployedTemplates[record.id] === true) {
        return this.$t('label.catalog.status.registered', { state: this.templateStates[record.id] || this.$t('label.catalog.status.pending') })
      }
      if (this.rowErrors[record.id]) {
        return this.$t('label.catalog.status.failed', { error: this.rowErrors[record.id] })
      }
      return record.reason || this.$t('label.catalog.status.available')
    },
    isZoneEligible (zone) {
      const clusters = this.zoneClusters[zone.id]
      return !clusters || clusters.hypervisors.includes('KVM')
    },
    async fetchZones () {
      this.zonesLoading = true
      try {
        const [zonesJson, clustersJson] = await Promise.all([
          getAPI('listZones', { listall: true }),
          getAPI('listClusters', { pagesize: -1 })
        ])
        const zoneClusters = {}
        for (const cluster of clustersJson?.listclustersresponse?.cluster || []) {
          const entry = zoneClusters[cluster.zoneid] || (zoneClusters[cluster.zoneid] = { hypervisors: [], archs: [] })
          if (!entry.hypervisors.includes(cluster.hypervisortype)) {
            entry.hypervisors.push(cluster.hypervisortype)
          }
          if (cluster.hypervisortype === 'KVM' && cluster.arch && !entry.archs.includes(cluster.arch)) {
            entry.archs.push(cluster.arch)
          }
        }
        this.zoneClusters = zoneClusters
        this.zones = zonesJson?.listzonesresponse?.zone || []
      } catch (error) {
        console.error('Error fetching zones:', error)
        this.zones = []
      } finally {
        this.zonesLoading = false
      }
      const eligible = this.zones.filter(zone => this.isZoneEligible(zone))
      if (eligible.length === 1) {
        this.selectedZoneId = eligible[0].id
        await this.refreshZone()
      }
    },
    // Rebuild rows and re-run the per-zone checks; used on mount (wizard) and on zone change.
    async refreshZone () {
      const request = ++this.zoneRequest
      this.buildRows()
      this.hasImageStore = true
      if (!this.effectiveZoneId) {
        return
      }
      await Promise.all([
        this.checkImageStore(request),
        this.fetchRegisteredTemplates(request)
      ])
    },
    buildRows () {
      const archs = this.effectiveArchs
      this.selectedRowKeys = []
      this.deployedTemplates = {}
      this.templateStates = {}
      this.rowErrors = {}
      this.predefinedTemplates = this.catalog.map(entry => {
        const row = { ...entry }
        // Edge zones use direct download over http; rewrite a copy, never the catalog
        if (this.effectiveSuperType === 'Edge' && row.url.startsWith('https://')) {
          row.url = row.url.replace('https://', 'http://')
        }
        if (archs.length > 0 && !archs.includes(row.arch)) {
          row.disabled = true
          row.reason = this.$t('message.catalog.reason.arch', { arch: row.arch })
        }
        return row
      })
    },
    async checkImageStore (request) {
      if (this.effectiveSuperType === 'Edge') {
        return
      }
      try {
        const json = await getAPI('listImageStores', { zoneid: this.effectiveZoneId })
        if (request === this.zoneRequest) {
          this.hasImageStore = (json?.listimagestoresresponse?.imagestore || []).length > 0
        }
      } catch (error) {
        console.error('Error fetching image stores:', error)
      }
    },
    async fetchRegisteredTemplates (request) {
      let registered = []
      try {
        const json = await getAPI('listTemplates', {
          templatefilter: 'all',
          zoneid: this.effectiveZoneId,
          hypervisor: 'KVM',
          listall: true,
          pagesize: -1
        })
        registered = json?.listtemplatesresponse?.template || []
      } catch (error) {
        console.error('Error fetching registered templates:', error)
      }
      if (request !== this.zoneRequest) {
        return
      }
      const known = {}
      for (const template of registered) {
        if (template.url) {
          known[normalizeUrl(template.url)] = template.isready ? 'Ready' : template.status
        }
      }
      for (const row of this.predefinedTemplates) {
        const state = known[normalizeUrl(row.url)]
        if (state !== undefined) {
          this.deployedTemplates[row.id] = true
          this.templateStates[row.id] = state
          row.preRegistered = true
          row.disabled = true
        }
      }
    },
    async registerTemplate (templateData) {
      const params = {
        displaytext: templateData.name + ' ' + templateData.arch,
        format: this.getImageFormat(templateData.url),
        hypervisor: 'KVM',
        name: templateData.name,
        arch: templateData.arch,
        url: templateData.url,
        ostypeid: await this.fetchOsTypeId(templateData.name),
        zoneid: this.effectiveZoneId,
        ispublic: this.makePublic,
        isfeatured: this.makeFeatured
      }
      if (this.effectiveSuperType === 'Edge') {
        params.directdownload = true
      }
      return new Promise((resolve, reject) => {
        postAPI('registerTemplate', params).then(json => {
          const result = json.registertemplateresponse.template[0]
          resolve(result)
        }).catch(error => {
          reject(new Error(error?.response?.headers?.['x-description'] || error?.message))
        })
      })
    },
    async stepRegisterTemplates () {
      if (this.submitting) {
        return
      }
      const templatesToRegister = this.predefinedTemplates.filter(template => this.selectedRowKeys.includes(template.id) && this.deployedTemplates[template.id] !== true)
      if (templatesToRegister.length === 0) {
        this.showAlert = true
        return
      }
      this.submitting = true
      try {
        await this.submitTemplates(templatesToRegister)
      } finally {
        this.submitting = false
      }
    },
    async submitTemplates (templatesToRegister) {
      const registrationResults = []
      for (const templateData of templatesToRegister) {
        const promise = this.registerTemplate(templateData)
          .then(result => ({
            id: templateData.id,
            status: 'success',
            name: templateData.name,
            state: result?.isready ? 'Ready' : result?.status
          }))
          .catch(error => ({
            id: templateData.id,
            status: 'error',
            name: templateData.name,
            error: error.message
          }))
        registrationResults.push(promise)
      }
      const results = await Promise.all(registrationResults)
      const successful = results.filter(r => r.status === 'success')
      const failed = results.filter(r => r.status === 'error')

      if (successful.length > 0) {
        this.registeredAny = true
        this.$notification.success({
          message: this.$t('label.register.template'),
          description: this.$t('message.catalog.register.success', { names: successful.map(r => r.name).join(', ') })
        })

        successful.forEach(r => {
          this.deployedTemplates[r.id] = true
          this.templateStates[r.id] = r.state
          delete this.rowErrors[r.id]
          this.predefinedTemplates.find(t => t.id === r.id).disabled = true
        })
        this.selectedRowKeys = this.selectedRowKeys.filter(id => !successful.some(r => r.id === id))
      }
      if (failed.length > 0) {
        this.$notification.error({
          message: this.$t('label.register.template'),
          description: this.$t('message.catalog.register.failed', { names: failed.map(r => r.name).join(', ') })
        })

        failed.forEach(r => {
          this.rowErrors[r.id] = r.error
        })
      }
    },
    async fetchOsTypeId (osName) {
      let osTypeId = this.defaultOsTypeId
      try {
        const json = await getAPI('listOsTypes', { keyword: osName, filter: 'name,id' })
        if (json && json.listostypesresponse && json.listostypesresponse.ostype && json.listostypesresponse.ostype.length > 0) {
          osTypeId = json.listostypesresponse.ostype[0].id
        }
      } catch (error) {
        console.error('Error fetching OS types:', error)
      }
      return osTypeId
    },
    getImageFormat (url) {
      const fileExtension = url.split('.').pop()
      var format = fileExtension
      switch (fileExtension) {
        case 'img':
          format = 'RAW'
          break
        case 'qcow2':
          format = 'qcow2'
          break
        default:
          format = 'RAW'
      }
      return format
    },
    // The catalog is fetched once and kept untouched; rows are built from copies per zone.
    async fetchCatalog () {
      this.loading = true
      this.catalogError = null
      try {
        const response = await fetch('./cloud-image-templates.json')
        if (!response.ok) {
          throw new Error(`Error fetching predefined templates, status_code: ${response.status}`)
        }
        this.catalog = await response.json()
      } catch (error) {
        console.error('Error fetching predefined templates:', error)
        this.catalog = []
        this.catalogError = error.message
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .register-template {
    width: 80vw;
    max-width: 1000px;
  }

  .zone-select {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;

    &__reason {
      margin-left: 8px;
      color: rgba(0, 0, 0, 0.45);
    }
  }

  .switches {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-top: 8px;
  }

  .form-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
    background-color: #fafafa;
    min-height: 440px;
    text-align: center;
    vertical-align: center;
    padding: 8px;
    padding-top: 16px;
    margin-top: 8px;
    overflow-y: auto;
  }

  .form-action {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 16px;
  }

  .register-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    margin-left: auto;
  }
</style>
