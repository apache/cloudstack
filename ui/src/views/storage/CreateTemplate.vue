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
      <a-form-item ref="zoneid" name="zoneid">
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
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item ref="groupenabled" name="groupenabled">
            <a-checkbox-group
              v-model:value="form.groupenabled"
              style="width: 100%;"
            >
              <a-row>
                <a-col :span="12">
                  <a-checkbox value="passwordenabled">
                    {{ $t('label.passwordenabled') }}
                  </a-checkbox>
                </a-col>
                <a-col :span="12">
                  <a-checkbox value="isdynamicallyscalable">
                    {{ $t('label.isdynamicallyscalable') }}
                  </a-checkbox>
                </a-col>
                <a-col :span="12">
                  <a-checkbox value="requireshvm">
                    {{ $t('label.requireshvm') }}
                  </a-checkbox>
                </a-col>
                <a-col :span="12" v-if="isAdminRole">
                  <a-checkbox value="isfeatured">
                    {{ $t('label.isfeatured') }}
                  </a-checkbox>
                </a-col>
                <a-col :span="12" v-if="isAdminRole || $store.getters.features.userpublictemplateenabled">
                  <a-checkbox value="ispublic">
                    {{ $t('label.ispublic') }}
                  </a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
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
import { api } from '@/api'
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
      loading: false
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
      this.fetchSnapshotZones()
    },
    fetchOsTypes () {
      this.osTypes.opts = []
      this.osTypes.loading = true

      api('listOsTypes').then(json => {
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
      api('listZones', params).then(json => {
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
      api('listSnapshots', params).then(json => {
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
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        values.snapshotid = this.resource.id
        if (values.groupenabled) {
          const input = values.groupenabled
          for (const index in input) {
            const name = input[index]
            values[name] = true
          }
          delete values.groupenabled
        }
        this.loading = true
        api('createTemplate', values).then(response => {
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
