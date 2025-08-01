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
    layout="vertical">
    <a-alert type="warning" show-icon>
      <template #message>
        <span style="margin-bottom: 5px" v-html="$t('message.reinstall.vm')" />
      </template>
    </a-alert>
    <os-based-image-selection
      v-if="isModernImageSelection"
      :imageTypeSelectionAllowed="false"
      :guestOsCategories="options.guestOsCategories"
      :guestOsCategoriesLoading="loading.guestOsCategories"
      :selectedGuestOsCategoryId="selectedGuestOsCategoryId"
      :imageItems="options.templates"
      :imagesLoading="loading.templates"
      :filterOption="filterOption"
      :preFillContent="dataPreFill"
      @change-guest-os-category="onSelectGuestOsCategory"
      @handle-image-search-filter="($event) => fetchAllTemplates($event)"
      @update-image="updateFieldValue" />
    <a-form-item v-else>
      <template-iso-selection
        input-decorator="templateid"
        :items="options.templates"
        :selected="tabKey"
        :loading="loading.templates"
        :preFillContent="dataPreFill"
        :key="templateKey"
        @handle-search-filter="($event) => fetchAllTemplates($event)"
        @update-template-iso="updateFieldValue" />
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
        :items="options.diskOfferings"
        :row-count="count.diskOfferings"
        :zoneId="resource.zoneId"
        :value="diskOffering ? diskOffering.id : ''"
        :loading="loading.diskOfferings"
        :preFillContent="dataPreFill"
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
import { getAPI, postAPI } from '@/api'
import DiskOfferingSelection from '@views/compute/wizard/DiskOfferingSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import OsBasedImageSelection from '@views/compute/wizard/OsBasedImageSelection'
import TemplateIsoSelection from '@views/compute/wizard/TemplateIsoSelection'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import _ from 'lodash'

export default {
  name: 'ReinstallVM',
  components: {
    DiskOfferingSelection,
    DiskSizeSelection,
    TemplateIsoSelection,
    OsBasedImageSelection,
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
      selectedGuestOsCategoryId: null,
      options: {
        templates: {},
        diskOfferings: [],
        guestOsCategories: []
      },
      loading: {
        templates: false,
        diskOfferings: false,
        guestOsCategories: false
      },
      count: {
        diskOfferings: 0
      },
      rootDiskSizeKey: 'details[0].rootdisksize',
      minIopsKey: 'details[0].minIops',
      maxIopsKey: 'details[0].maxIops',
      rootdisksize: 0,
      minIops: 0,
      maxIops: 0,
      diskOffering: {},
      imageSearchFilters: null,
      templateid: null,
      templateKey: 0,
      dataPreFill: {
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
  computed: {
    isNormalAndDomainUser () {
      return ['DomainAdmin', 'User'].includes(this.$store.getters.userInfo.roletype)
    },
    isModernImageSelection () {
      return this.$config.imageSelectionInterface === undefined || this.$config.imageSelectionInterface === 'modern'
    },
    imageSelection () {
      return this.isModernImageSelection ? 'modern' : 'legacy'
    },
    showUserCategoryForModernImageSelection () {
      return this.$config.showUserCategoryForModernImageSelection === undefined || this.$config.showUserCategoryForModernImageSelection
    },
    showAllCategoryForModernImageSelection () {
      return this.$config.showAllCategoryForModernImageSelection
    }
  },
  methods: {
    fetchData () {
      this.fetchDiskOfferings({})
      if (this.isModernImageSelection) {
        this.fetchGuestOsCategories()
      } else {
        this.fetchAllTemplates()
      }
    },
    getImageFilters (params, forReset) {
      if (this.isModernImageSelection) {
        if (this.selectedGuestOsCategoryId === '0') {
          return ['self']
        }
        if (this.isModernImageSelection && params && !forReset) {
          if (params.featured) {
            return ['featured']
          } else if (params.public) {
            return ['community']
          }
        }
        return this.isNormalAndDomainUser ? ['executable'] : ['all']
      }
      return [
        'featured',
        'community',
        'selfexecutable',
        'sharedexecutable'
      ]
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
      postAPI('restoreVirtualMachine', params).then(response => {
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
    fetchGuestOsCategories () {
      this.loading.guestOsCategories = true
      const params = {
        zoneid: this.resource.zoneid,
        arch: this.resource.arch,
        isiso: false,
        featured: true,
        showicon: true
      }
      getAPI('listOsCategories', params).then((response) => {
        this.options.guestOsCategories = response?.listoscategoriesresponse?.oscategory || []
        if (this.showUserCategoryForModernImageSelection) {
          const userCategory = {
            id: '0',
            name: this.$t('label.user')
          }
          if (this.$store.getters.avatar) {
            userCategory.icon = {
              base64image: this.$store.getters.avatar
            }
          }
          this.options.guestOsCategories.push(userCategory)
        }
        if (this.showAllCategoryForModernImageSelection) {
          this.options.guestOsCategories.push({
            id: '-1',
            name: this.$t('label.all')
          })
        }
        this.selectedGuestOsCategoryId = this.options.guestOsCategories[0].id
      }).finally(() => {
        this.loading.guestOsCategories = false
        this.fetchAllTemplates()
      })
    },
    fetchAllTemplates (params) {
      const promises = []
      const templates = {}
      this.loading.templates = true
      this.imageSearchFilters = params
      const templateFilters = this.getImageFilters(params)
      templateFilters.forEach((filter) => {
        templates[filter] = { count: 0, template: [] }
        promises.push(this.fetchTemplates(filter, params))
      })
      this.options.templates = templates
      Promise.all(promises).then((response) => {
        response.forEach((resItem, idx) => {
          templates[templateFilters[idx]] = _.isEmpty(resItem.listtemplatesresponse) ? { count: 0, template: [] } : resItem.listtemplatesresponse
          this.options.templates = { ...templates }
        })
      }).catch((reason) => {
        console.log(reason)
      }).finally(() => {
        this.loading.templates = false
      })
    },
    fetchTemplates (templateFilter, params) {
      const args = Object.assign({}, params)
      if (this.isModernImageSelection && this.selectedGuestOsCategoryId && !['-1', '0'].includes(this.selectedGuestOsCategoryId)) {
        args.oscategoryid = this.selectedGuestOsCategoryId
      }
      if (args.keyword || (args.category && args.category !== templateFilter)) {
        args.page = 1
        args.pageSize = args.pageSize || 10
      }
      args.zoneid = this.resource.zoneid
      args.templatefilter = templateFilter
      args.isready = true
      if (this.resource.arch) {
        args.arch = this.resource.arch
      }
      args.details = 'all'
      args.showicon = 'true'

      return new Promise((resolve, reject) => {
        getAPI('listTemplates', args).then((response) => {
          resolve(response)
        }).catch((reason) => {
          reject(reason)
        })
      })
    },
    fetchDiskOfferings (params) {
      getAPI('listDiskOfferings', { zoneid: this.resource.zoneid, listall: true, ...params }).then((response) => {
        this.options.diskOfferings = response?.listdiskofferingsresponse?.diskoffering || []
        this.count.diskOfferings = response?.listdiskofferingsresponse?.count || 0
      })
    },
    onSelectGuestOsCategory (value) {
      this.selectedGuestOsCategoryId = value
      this.fetchAllTemplates(this.imageSearchFilters)
    },
    onSelectDiskSize (rowSelected) {
      this.diskOffering = rowSelected
      this.dataPreFill.diskofferingid = rowSelected.id
    },
    updateFieldValue (input, value) {
      this[input] = value
      this.dataPreFill[input] = value
    },
    filterOption (input, option) {
      return option.label.toUpperCase().indexOf(input.toUpperCase()) >= 0
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
