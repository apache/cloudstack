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
  <a-spin :spinning="loading">
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="apiParams.name.description" />
      </a-form-item>
      <a-form-item ref="displaytext" name="displaytext">
        <template #label>
          <tooltip-label :title="$t('label.displaytext')" :tooltip="apiParams.displaytext.description"/>
        </template>
        <a-input
          v-model:value="form.displaytext"
          :placeholder="apiParams.displaytext.description" />
      </a-form-item>
      <a-form-item v-if="resource.intervaltype" ref="zoneid" name="zoneid">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          :loading="loading"
          :placeholder="apiParams.zoneid.description"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="domainid" ref="domainid" v-if="'listDomains' in $store.getters.apis">
          <template #label>
            <tooltip-label :title="$t('label.domainid')" :tooltip="apiParams.domainid.description"/>
          </template>
          <a-select
            v-model:value="form.domainid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="domainLoading"
            :placeholder="apiParams.domainid.description"
            @change="val => { handleDomainChange(val) }">
            <a-select-option v-for="(opt, optIndex) in this.domains" :key="optIndex" :label="opt.path || opt.name || opt.description" :value="opt.id">
              <span>
                <resource-icon v-if="opt && opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <block-outlined v-else style="margin-right: 5px" />
                {{ opt.path || opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="account" ref="account" v-if="domainid">
          <template #label>
            <tooltip-label :title="$t('label.account')" :tooltip="apiParams.account.description"/>
          </template>
          <a-select
            v-model:value="form.account"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.account.description"
            @change="val => { handleAccountChange(val) }">
            <a-select-option v-for="(acc, index) in accounts" :value="acc.name" :key="index">
              {{ acc.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

      <a-form-item
          name="ostypeid"
          ref="ostypeid"
          :label="$t('label.ostypeid')">
          <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          v-model:value="form.ostypeid"
          :loading="osTypes.loading"
          :placeholder="apiParams.ostypeid.description">
          <a-select-option v-for="opt in osTypes.opts" :key="opt.id" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
          </a-select-option>
          </a-select>
      </a-form-item>
      <a-form-item
        name="arch"
        ref="arch">
        <template #label>
          <tooltip-label :title="$t('label.arch')" :tooltip="apiParams.arch.description"/>
        </template>
        <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          v-model:value="form.arch"
          :placeholder="apiParams.arch.description">
          <a-select-option v-for="opt in architectureTypes.opts" :key="opt.id">
            {{ opt.name || opt.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-row :gutter="12">
        <a-col :md="24" :lg="12">
          <a-form-item ref="isdynamicallyscalable" name="isdynamicallyscalable">
            <template #label>
              <tooltip-label :title="$t('label.isdynamicallyscalable')" :tooltip="apiParams.isdynamicallyscalable.description"/>
            </template>
            <a-switch v-model:checked="form.isdynamicallyscalable" />
          </a-form-item>
          <a-form-item ref="requireshvm" name="requireshvm">
            <template #label>
              <tooltip-label :title="$t('label.requireshvm')" :tooltip="apiParams.requireshvm.description"/>
            </template>
            <a-switch v-model:checked="form.requireshvm" />
          </a-form-item>
          <a-form-item ref="passwordenabled" name="passwordenabled">
            <template #label>
              <tooltip-label :title="$t('label.passwordenabled')" :tooltip="apiParams.passwordenabled.description"/>
            </template>
            <a-switch v-model:checked="form.passwordenabled" />
          </a-form-item>
          <a-form-item
            ref="ispublic"
            name="ispublic"
            v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.userpublictemplateenabled" >
            <template #label>
              <tooltip-label :title="$t('label.ispublic')" :tooltip="apiParams.ispublic.description"/>
            </template>
            <a-switch v-model:checked="form.ispublic" />
          </a-form-item>
          <a-form-item ref="isfeatured" name="isfeatured" v-if="$store.getters.userInfo.roletype === 'Admin'">
              <template #label>
                <tooltip-label :title="$t('label.isfeatured')" :tooltip="apiParams.isfeatured.description"/>
              </template>
              <a-switch v-model:checked="form.isfeatured" />
          </a-form-item>
        </a-col>
      </a-row>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateTemplate',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      snapshotZoneIds: [],
      zones: [],
      osTypes: {},
      loading: false,
      domains: [],
      accounts: [],
      domainLoading: false,
      domainid: null,
      account: null,
      architectureTypes: {}
    }
  },
  computed: {
    isAdminRole () {
      return this.$store.getters.userInfo.roletype === 'Admin'
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createTemplate')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        groupenabled: ['requireshvm']
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        ostypeid: [{ required: true, message: this.$t('message.error.select') }],
        groupenabled: [{ type: 'array' }]
      })
    },
    fetchData () {
      this.fetchOsTypes()
      if (this.resource.intervaltype) {
        this.fetchSnapshotZones()
      }
      if ('listDomains' in this.$store.getters.apis) {
        this.fetchDomains()
      }
      this.architectureTypes.opts = this.$fetchCpuArchitectureTypes()
    },
    fetchOsTypes () {
      this.osTypes.opts = []
      this.osTypes.loading = true

      getAPI('listOsTypes').then(json => {
        const listOsTypes = json.listostypesresponse.ostype
        this.osTypes.opts = listOsTypes
        this.defaultOsType = this.osTypes.opts[1].description
        this.defaultOsId = this.osTypes.opts[1].name
      }).finally(() => {
        this.osTypes.loading = false
      })
    },
    fetchZones (id) {
      this.loading = true
      const params = { showicon: true }
      if (Array.isArray(id)) {
        params.ids = id.join()
      } else {
        params.id = id
      }
      getAPI('listZones', params).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
      }).finally(() => {
        this.loading = false
      })
    },
    fetchSnapshotZones () {
      this.loading = true
      this.snapshotZoneIds = []
      const params = {
        showunique: false,
        id: this.resource.id
      }
      getAPI('listSnapshots', params).then(json => {
        const snapshots = json.listsnapshotsresponse.snapshot || []
        for (const snapshot of snapshots) {
          if (!this.snapshotZoneIds.includes(snapshot.zoneid)) {
            this.snapshotZoneIds.push(snapshot.zoneid)
          }
        }
      }).finally(() => {
        if (this.snapshotZoneIds && this.snapshotZoneIds.length > 0) {
          this.fetchZones(this.snapshotZoneIds)
        }
      })
    },
    fetchDomains () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      getAPI('listDomains', params).then(json => {
        this.domains = json.listdomainsresponse.domain
      }).finally(() => {
        this.domainLoading = false
        this.handleDomainChange(null)
      })
    },
    async handleDomainChange (domain) {
      this.domainid = domain
      this.form.account = null
      this.account = null
      if ('listAccounts' in this.$store.getters.apis) {
        await this.fetchAccounts()
      }
    },
    fetchAccounts () {
      return new Promise((resolve, reject) => {
        getAPI('listAccounts', {
          domainid: this.domainid
        }).then(response => {
          this.accounts = response?.listaccountsresponse?.account || []
          resolve(this.accounts)
        }).catch(error => {
          this.$notifyError(error)
        })
      })
    },
    handleAccountChange (acc) {
      if (acc) {
        this.account = acc.name
      } else {
        this.account = acc
      }
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {}
        if (this.resource.intervaltype) {
          params.snapshotid = this.resource.id
        } else {
          params.volumeid = this.resource.id
        }

        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        this.loading = true
        postAPI('createTemplate', params).then(response => {
          this.$pollJob({
            jobId: response.createtemplateresponse.jobid,
            title: this.$t('message.success.create.template'),
            description: values.name,
            successMessage: this.$t('message.success.create.template'),
            successMethod: (result) => {
              this.closeModal()
            },
            errorMessage: this.$t('message.create.template.failed'),
            loadingMessage: this.$t('message.create.template.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    min-width: 400px;
    width: 100%;
  }
}
</style>
