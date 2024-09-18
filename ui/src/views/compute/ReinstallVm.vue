// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-form
    v-ctrl-enter="handleSubmit"
    @finish="handleSubmit"
    layout="vertical"
  >
    <a-alert
      type="warning"
      show-icon
    >
      <template #message><span
          style="margin-bottom: 5px"
          v-html="$t('message.reinstall.vm')"
        /></template>
    </a-alert>
    <a-form-item>
      <template-iso-selection
        input-decorator="templateid"
        :items="templates"
        :selected="tabKey"
        :loading="loading.templates"
        :preFillContent="dataPrefill"
        :key="templateKey"
        @handle-search-filter="($event) => fetchAllTemplates($event)"
        @update-template-iso="updateFieldValue"
      />
    </a-form-item>
    <a-form-item>
      <template #label>
        <tooltip-label
          :title="$t('label.override.root.diskoffering')"
          :tooltip="apiParams.diskofferingid.description"
        />
      </template>
      <a-switch
        v-model:checked="overrideDiskOffering"
        @change="val => { overrideDiskOffering = val }"
      />
    </a-form-item>
    <a-form-item v-if="overrideDiskOffering">
      <disk-offering-selection
        :items="diskOfferings"
        :row-count="diskOfferingCount"
        :zoneId="resource.zoneId"
        :value="diskOffering ? diskOffering.id : ''"
        :loading="loading.diskOfferings"
        :preFillContent="dataPrefill"
        :isIsoSelected="false"
        :isRootDiskOffering="true"
        @on-selected-disk-size="onSelectDiskSize"
        @handle-search-filter="($event) => fetchDiskOfferings($event)"
      />
    </a-form-item>
    <a-form-item v-if="diskOffering && (diskOffering.iscustomized || diskOffering.iscustomizediops)">
      <disk-size-selection
        input-decorator="rootdisksize"
        :diskSelected="diskOffering"
        :isCustomized="diskOffering.iscustomized"
        @handler-error="handlerError"
        @update-disk-size="updateFieldValue"
        @update-root-disk-iops-value="updateFieldValue"
      />
    </a-form-item>
    <a-form-item v-if="!(diskOffering && diskOffering.iscustomized)">
      <template #label>
        <tooltip-label
          :title="$t('label.override.rootdisk.size')"
          :tooltip="apiParams.rootdisksize.description"
        />
      </template>
      <a-switch
        v-model:checked="overrideDiskSize"
        @change="val => { overrideDiskSize = val }"
      />
      <disk-size-selection
        v-if="overrideDiskSize"
        input-decorator="rootdisksize"
        :isCustomized="true"
        @update-disk-size="(input, value) => updateFieldValue('overrideRootDiskSize', value)"
        style="margin-top: 10px;"
      />
    </a-form-item>
    <a-form-item>
      <template #label>
        <tooltip-label
          :title="$t('label.expunge')"
          :tooltip="apiParams.expunge.description"
        />
      </template>
      <a-switch
        v-model:checked="expungeDisk"
        @change="val => { expungeDisk = val }"
      />
    </a-form-item>
    <div
      :span="24"
      class="action-button"
    >
      <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
      <a-button
        ref="submit"
        type="primary"
        @click="handleSubmit"
      >{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
</template>

<script>
import { api } from '@/api'
import DiskOfferingSelection from '@views/compute/wizard/DiskOfferingSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import TemplateIsoSelection from '@views/compute/wizard/TemplateIsoSelection'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import _ from 'lodash'

export default {
  name: 'ReinstallVM',
  components: {
    DiskOfferingSelection,
    DiskSizeSelection,
    TemplateIsoSelection,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      overrideDiskOffering: false,
      overrideDiskSize: false,
      expungeDisk: false,
      selectedDiskOffering: {},
      loading: {
        templates: false,
        diskOfferings: false
      },
      rootDiskSizeKey: 'details[0].rootdisksize',
      minIopsKey: 'details[0].minIops',
      maxIopsKey: 'details[0].maxIops',
      rootdisksize: 0,
      minIops: 0,
      maxIops: 0,
      templateFilter: [
        'featured',
        'community',
        'selfexecutable',
        'sharedexecutable'
      ],
      diskOffering: {},
      diskOfferingCount: 0,
      templateKey: 0,
      dataPrefill: {
        templateid: this.resource.templateid,
        diskofferingid: this.resource.diskofferingid
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('restoreVirtualMachine')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchDiskOfferings({})
      this.fetchAllTemplates()
    },
    closeAction () {
      this.$emit('close-action')
    },
    handlerError (error) {
      this.error = error
    },
    handleSubmit () {
      const params = {
        virtualmachineid: this.resource.id
      }
      if (this.templateid) {
        params.templateid = this.templateid
      }
      if (this.overrideDiskOffering) {
        params.diskofferingid = this.diskOffering.id
        if (this.diskOffering.iscustomized) {
          params[this.rootDiskSizeKey] = this.rootdisksize
        }
        if (this.diskOffering.iscustomizediops) {
          params[this.minIopsKey] = this.minIops
          params[this.maxIopsKey] = this.maxIops
        }
      }
      if (this.overrideDiskSize && this.overrideRootDiskSize) {
        params.rootdisksize = this.overrideRootDiskSize
      }
      params.expunge = this.expungeDisk
      api('restoreVirtualMachine', params).then(response => {
        this.$pollJob({
          jobId: response.restorevmresponse.jobid,
          successMessage: this.$t('label.reinstall.vm') + ' ' + this.$t('label.success'),
          successMethod: (result) => {
            const vm = result.jobresult.virtualmachine || {}
            const name = vm.displayname || vm.name || vm.id
            if (result.jobstatus === 1 && vm.password) {
              this.$notification.success({
                message: `${this.$t('label.reinstall.vm')}: ` + name,
                description: `${this.$t('label.password.reset.confirm')}: ` + vm.password,
                duration: 0
              })
            }
          },
          errorMessage: this.$t('label.reinstall.vm') + ' ' + this.$t('label.failed'),
          errorMethod: (result) => {
            this.closeAction()
          },
          loadingMessage: this.$t('label.reinstall.vm') + ': ' + this.resource.name,
          catchMessage: this.$t('error.fetching.async.job.result')
        })
      }).catch(error => {
        this.$notifyError(error)
        this.closeAction()
      }).finally(() => {
        this.closeAction()
      })
    },
    fetchAllTemplates (params) {
      const promises = []
      const templates = {}
      this.loading.templates = true
      this.templateFilter.forEach((filter) => {
        templates[filter] = { count: 0, template: [] }
        promises.push(this.fetchTemplates(filter, params))
      })
      this.templates = templates
      Promise.all(promises).then((response) => {
        response.forEach((resItem, idx) => {
          templates[this.templateFilter[idx]] = _.isEmpty(resItem.listtemplatesresponse) ? { count: 0, template: [] } : resItem.listtemplatesresponse
          this.templates = { ...templates }
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.templates = false
      })
    },
    fetchTemplates (templateFilter, params) {
      const args = Object.assign({}, params)
      if (args.keyword || args.category !== templateFilter) {
        args.page = 1
        args.pageSize = args.pageSize || 10
      }
      args.zoneid = _.get(this.zone, 'id')
      args.templatefilter = templateFilter
      args.details = 'all'
      args.showicon = 'true'

      return new Promise((resolve, reject) => {
        api('listTemplates', args).then((response) => {
          resolve(response)
        }).catch((reason) => {
          reject(reason)
        })
      })
    },
    fetchDiskOfferings (params) {
      api('listDiskOfferings', { zoneid: this.resource.zoneid, listall: true, ...params }).then((response) => {
        this.diskOfferings = response?.listdiskofferingsresponse?.diskoffering || []
        this.diskOfferingCount = response?.listdiskofferingsresponse?.count || 0
      })
    },
    onSelectDiskSize (rowSelected) {
      this.diskOffering = rowSelected
      this.dataPrefill.diskofferingid = rowSelected.id
    },
    updateFieldValue (input, value) {
      this[input] = value
      this.dataPrefill[input] = value
    }
  }
}
</script>

<style
  scoped
  lang="scss"
>
.ant-form {
  width: 90vw;

  @media (min-width: 700px) {
    width: 50vw;
  }
}
</style>
