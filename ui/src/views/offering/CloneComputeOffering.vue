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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-alert
         v-if="resource"
         type="info"
         style="margin-bottom: 16px">
        <template #message>
          <div style="display: block; width: 100%;">
            <div style="display: block; margin-bottom: 8px;">
              <strong>{{ $t('message.clone.offering.from') }}: {{ resource.name }}</strong>
            </div>
            <div style="display: block; font-size: 12px;">
              {{ $t('message.clone.offering.edit.hint') }}
            </div>
          </div>
        </template>
      </a-alert>
      <ComputeOfferingForm
        :initialValues="form"
        :rules="rules"
        :apiParams="apiParams"
        :isSystem="isSystem"
        :isAdmin="isAdmin"
        :ref="formRef"
        @submit="handleSubmit">

        <!-- form content is provided by ComputeOfferingForm component -->
        <template #form-actions>
          <br/>
          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </template>
      </ComputeOfferingForm>
     </a-spin>
   </div>
 </template>

<script>
import ComputeOfferingForm from '@/components/offering/ComputeOfferingForm'
import { ref, reactive } from 'vue'
import { getAPI, postAPI } from '@/api'
import AddDiskOffering from '@/views/offering/AddDiskOffering'
import { isAdmin } from '@/role'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import DetailsInput from '@/components/widgets/DetailsInput'
import store from '@/store'

export default {
  name: 'CreateComputeOffering',
  mixins: [mixinForm],
  components: {
    ComputeOfferingForm,
    AddDiskOffering,
    ResourceIcon,
    TooltipLabel,
    DetailsInput
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      isSystem: false,
      naturalNumberRule: {
        type: 'number',
        validator: this.validateNumber
      },
      wholeNumberRule: {
        type: 'number',
        validator: async (rule, value) => {
          if (value && (isNaN(value) || value < 0)) {
            return Promise.reject(this.$t('message.error.number'))
          }
          return Promise.resolve()
        }
      },
      storageType: 'shared',
      provisioningType: 'thin',
      cacheMode: 'none',
      offeringType: 'fixed',
      isCustomizedDiskIops: false,
      isPublic: true,
      domains: [],
      domainLoading: false,
      zones: [],
      zoneLoading: false,
      selectedDeploymentPlanner: null,
      storagePolicies: null,
      storageTags: [],
      storageTagLoading: false,
      deploymentPlanners: [],
      deploymentPlannerLoading: false,
      plannerModeVisible: false,
      plannerMode: '',
      selectedGpuCard: '',
      showDiskOfferingModal: false,
      gpuCardLoading: false,
      gpuCards: [],
      loading: false,
      dynamicscalingenabled: true,
      diskofferingstrictness: false,
      encryptdisk: false,
      computeonly: true,
      diskOfferingLoading: false,
      diskOfferings: [],
      selectedDiskOfferingId: '',
      qosType: '',
      isDomainAdminAllowedToInformTags: false,
      isLeaseFeatureEnabled: this.$store.getters.features.instanceleaseenabled,
      showLeaseOptions: false,
      expiryActions: ['STOP', 'DESTROY'],
      defaultLeaseDuration: 90,
      defaultLeaseExpiryAction: 'STOP',
      leaseduration: undefined,
      leaseexpiryaction: undefined,
      vgpuProfiles: [],
      vgpuProfileLoading: false,
      externalDetailsEnabled: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('cloneServiceOffering')
  },
  created () {
    this.zones = [
      {
        id: null,
        name: this.$t('label.all.zone')
      }
    ]
    if (this.$route.meta.name === 'systemoffering') {
      this.isSystem = true
    }
    this.initForm()
    this.fetchData()
    this.isPublic = isAdmin()
    this.form.ispublic = this.isPublic
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        systemvmtype: 'domainrouter',
        offeringtype: this.offeringType,
        ispublic: this.isPublic,
        dynamicscalingenabled: true,
        plannermode: this.plannerMode,
        gpucardid: this.selectedGpuCard,
        vgpuprofile: '',
        gpucount: '1',
        gpudisplay: false,
        computeonly: this.computeonly,
        storagetype: this.storageType,
        provisioningtype: this.provisioningType,
        cachemode: this.cacheMode,
        qostype: this.qosType,
        iscustomizeddiskiops: this.isCustomizedDiskIops,
        diskofferingid: this.selectedDiskOfferingId,
        diskofferingstrictness: this.diskofferingstrictness,
        encryptdisk: this.encryptdisk,
        leaseduration: this.leaseduration,
        leaseexpiryaction: this.leaseexpiryaction
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.required.input') }],
        cpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        cpuspeed: [
          { required: true, message: this.$t('message.error.required.input') },
          this.wholeNumberRule
        ],
        mincpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        maxcpunumber: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        memory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        minmemory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        maxmemory: [
          { required: true, message: this.$t('message.error.required.input') },
          this.naturalNumberRule
        ],
        networkrate: [this.naturalNumberRule],
        rootdisksize: [this.naturalNumberRule],
        diskbytesreadrate: [this.naturalNumberRule],
        diskbyteswriterate: [this.naturalNumberRule],
        diskiopsreadrate: [this.naturalNumberRule],
        diskiopswriterate: [this.naturalNumberRule],
        diskiopsmin: [this.naturalNumberRule],
        diskiopsmax: [this.naturalNumberRule],
        hypervisorsnapshotreserve: [this.naturalNumberRule],
        domainid: [{ type: 'array', required: true, message: this.$t('message.error.select') }],
        diskofferingid: [{ required: true, message: this.$t('message.error.select') }],
        gpucount: [{
          type: 'number',
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value < 1)) {
              return Promise.reject(this.$t('message.error.number.minimum.one'))
            }
            return Promise.resolve()
          }
        }],
        zoneid: [{
          type: 'array',
          validator: async (rule, value) => {
            if (value && value.length > 1 && value.indexOf(0) !== -1) {
              return Promise.reject(this.$t('message.error.zone.combined'))
            }
            return Promise.resolve()
          }
        }],
        leaseduration: [this.naturalNumberRule]
      })
    },
    fetchData () {
      this.fetchDomainData()
      this.fetchZoneData()
      this.fetchGPUCards()
      if (isAdmin()) {
        this.fetchStorageTagData()
        this.fetchDeploymentPlannerData()
      } else if (this.isDomainAdmin()) {
        this.checkIfDomainAdminIsAllowedToInformTag()
        if (this.isDomainAdminAllowedToInformTags) {
          this.fetchStorageTagData()
        }
      }
      this.fetchDiskOfferings()
      this.populateFormFromResource()
    },
    populateFormFromResource () {
      if (!this.resource) return

      // Pre-fill form with source offering values
      const r = this.resource
      this.form.name = r.name + ' - Clone'
      this.form.displaytext = r.displaytext

      if (r.iscustomized) {
        if (r.cpunumber || r.cpuspeed || r.memory) {
          this.offeringType = 'customconstrained'
          this.form.offeringtype = 'customconstrained'
        } else {
          this.offeringType = 'customunconstrained'
          this.form.offeringtype = 'customunconstrained'
        }
      } else {
        this.offeringType = 'fixed'
        this.form.offeringtype = 'fixed'
      }

      if (r.cpunumber) this.form.cpunumber = r.cpunumber
      if (r.cpuspeed) this.form.cpuspeed = r.cpuspeed
      if (r.memory) this.form.memory = r.memory

      if (r.mincpunumber) this.form.mincpunumber = r.mincpunumber
      if (r.maxcpunumber) this.form.maxcpunumber = r.maxcpunumber
      if (r.minmemory) this.form.minmemory = r.minmemory
      if (r.maxmemory) this.form.maxmemory = r.maxmemory

      if (r.hosttags) this.form.hosttags = r.hosttags
      if (r.networkrate) this.form.networkrate = r.networkrate
      if (r.offerha !== undefined) this.form.offerha = r.offerha
      if (r.dynamicscalingenabled !== undefined) {
        this.form.dynamicscalingenabled = r.dynamicscalingenabled
        this.dynamicscalingenabled = r.dynamicscalingenabled
      }
      if (r.limitcpuuse !== undefined) this.form.limitcpuuse = r.limitcpuuse
      if (r.isvolatile !== undefined) this.form.isvolatile = r.isvolatile

      if (r.storagetype) {
        this.storageType = r.storagetype
        this.form.storagetype = r.storagetype
      }
      if (r.provisioningtype) {
        this.provisioningType = r.provisioningtype
        this.form.provisioningtype = r.provisioningtype
      }
      if (r.cachemode) {
        this.cacheMode = r.cachemode
        this.form.cachemode = r.cachemode
      }

      if (r.diskofferingstrictness !== undefined) {
        this.form.diskofferingstrictness = r.diskofferingstrictness
        this.diskofferingstrictness = r.diskofferingstrictness
      }
      if (r.encryptroot !== undefined) {
        this.form.encryptdisk = r.encryptroot
        this.encryptdisk = r.encryptroot
      }

      if (r.diskBytesReadRate || r.diskBytesWriteRate || r.diskIopsReadRate || r.diskIopsWriteRate) {
        this.qosType = 'hypervisor'
        this.form.qostype = 'hypervisor'
        if (r.diskBytesReadRate) this.form.diskbytesreadrate = r.diskBytesReadRate
        if (r.diskBytesWriteRate) this.form.diskbyteswriterate = r.diskBytesWriteRate
        if (r.diskIopsReadRate) this.form.diskiopsreadrate = r.diskIopsReadRate
        if (r.diskIopsWriteRate) this.form.diskiopswriterate = r.diskIopsWriteRate
      } else if (r.miniops || r.maxiops) {
        this.qosType = 'storage'
        this.form.qostype = 'storage'
        if (r.miniops) this.form.diskiopsmin = r.miniops
        if (r.maxiops) this.form.diskiopsmax = r.maxiops
        if (r.hypervisorsnapshotreserve) this.form.hypervisorsnapshotreserve = r.hypervisorsnapshotreserve
      }
      if (r.iscustomizediops !== undefined) {
        this.form.iscustomizeddiskiops = r.iscustomizediops
        this.isCustomizedDiskIops = r.iscustomizediops
      }

      if (r.rootdisksize) this.form.rootdisksize = r.rootdisksize

      if (r.tags) {
        this.form.storagetags = r.tags.split(',')
      }

      if (r.gpucardid) {
        this.form.gpucardid = r.gpucardid
        this.selectedGpuCard = r.gpucardid
        if (r.gpucardid) {
          this.fetchVgpuProfiles(r.gpucardid)
        }
      }
      if (r.vgpuprofileid) this.form.vgpuprofile = r.vgpuprofileid
      if (r.gpucount) this.form.gpucount = r.gpucount
      if (r.gpudisplay !== undefined) this.form.gpudisplay = r.gpudisplay

      if (r.leaseduration) {
        this.form.leaseduration = r.leaseduration
        this.showLeaseOptions = true
      }
      if (r.leaseexpiryaction) this.form.leaseexpiryaction = r.leaseexpiryaction

      if (r.purgeresources !== undefined) this.form.purgeresources = r.purgeresources

      if (r.vspherestoragepolicy) this.form.storagepolicy = r.vspherestoragepolicy

      if (r.systemvmtype) this.form.systemvmtype = r.systemvmtype

      if (r.deploymentplanner) {
        this.form.deploymentplanner = r.deploymentplanner
        this.handleDeploymentPlannerChange(r.deploymentplanner)
      }

      if (r.serviceofferingdetails && Object.keys(r.serviceofferingdetails).length > 0) {
        this.externalDetailsEnabled = true
        this.form.externaldetails = r.serviceofferingdetails
      }
    },
    fetchGPUCards () {
      this.gpuCardLoading = true
      getAPI('listGpuCards', {
      }).then(json => {
        this.gpuCards = json.listgpucardsresponse.gpucard || []
        this.gpuCards.unshift({
          id: '',
          name: this.$t('label.none')
        })
      }).finally(() => {
        this.gpuCardLoading = false
      })
    },
    fetchDiskOfferings () {
      this.diskOfferingLoading = true
      getAPI('listDiskOfferings', {
        listall: true
      }).then(json => {
        this.diskOfferings = json.listdiskofferingsresponse.diskoffering || []
        if (this.selectedDiskOfferingId === '') {
          this.selectedDiskOfferingId = this.diskOfferings[0].id || ''
        }
      }).finally(() => {
        this.diskOfferingLoading = false
      })
    },
    isAdmin () {
      return isAdmin()
    },
    isDomainAdmin () {
      return ['DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    checkIfDomainAdminIsAllowedToInformTag () {
      const params = { id: store.getters.userInfo.accountid }
      getAPI('isAccountAllowedToCreateOfferingsWithTags', params).then(json => {
        this.isDomainAdminAllowedToInformTags = json.isaccountallowedtocreateofferingswithtagsresponse.isallowed.isallowed
      })
    },
    fetchDomainData () {
      const params = {}
      params.listAll = true
      params.showicon = true
      params.details = 'min'
      this.domainLoading = true
      getAPI('listDomains', params).then(json => {
        const listDomains = json.listdomainsresponse.domain
        this.domains = this.domains.concat(listDomains)
      }).finally(() => {
        this.domainLoading = false
      })
    },
    fetchZoneData () {
      const params = {}
      params.showicon = true
      this.zoneLoading = true
      getAPI('listZones', params).then(json => {
        const listZones = json.listzonesresponse.zone
        if (listZones) {
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
      })
    },
    fetchStorageTagData () {
      this.storageTagLoading = true
      this.storageTags = []
      getAPI('listStorageTags').then(json => {
        const tags = json.liststoragetagsresponse.storagetag || []
        for (const tag of tags) {
          if (!this.storageTags.includes(tag.name)) {
            this.storageTags.push(tag.name)
          }
        }
      }).finally(() => {
        this.storageTagLoading = false
      })
    },
    fetchDeploymentPlannerData () {
      this.deploymentPlannerLoading = true
      getAPI('listDeploymentPlanners').then(json => {
        const planners = json.listdeploymentplannersresponse.deploymentPlanner
        this.deploymentPlanners = this.deploymentPlanners.concat(planners)
        this.deploymentPlanners.unshift({ name: '' })
        this.form.deploymentplanner = this.deploymentPlanners.length > 0 ? this.deploymentPlanners[0].name : ''
      }).finally(() => {
        this.deploymentPlannerLoading = false
      })
    },
    handleSubmit (e) {
      if (e && e.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return

      this.formRef.value.validate().then((values) => {
        var params = {
          issystem: this.isSystem,
          name: values.name,
          displaytext: values.displaytext,
          storagetype: values.storagetype,
          provisioningtype: values.provisioningtype,
          cachemode: values.cachemode,
          customized: values.offeringtype !== 'fixed',
          offerha: values.offerha === true,
          limitcpuuse: values.limitcpuuse === true,
          dynamicscalingenabled: values.dynamicscalingenabled,
          diskofferingstrictness: values.diskofferingstrictness,
          encryptroot: values.encryptdisk,
          purgeresources: values.purgeresources,
          leaseduration: values.leaseduration,
          leaseexpiryaction: values.leaseexpiryaction
        }

        if (values.diskofferingid) {
          params.diskofferingid = values.diskofferingid
        }

        if (values.vgpuprofile) {
          params.vgpuprofileid = values.vgpuprofile
        }
        if (values.gpucount && values.gpucount > 0) {
          params.gpucount = values.gpucount
        }
        if (values.gpudisplay !== undefined) {
          params.gpudisplay = values.gpudisplay
        }

        if (values.offeringtype === 'fixed') {
          params.cpunumber = values.cpunumber
          params.cpuspeed = values.cpuspeed
          params.memory = values.memory
        } else {
          if (values.cpuspeed != null &&
               values.mincpunumber != null &&
               values.maxcpunumber != null &&
               values.minmemory != null &&
               values.maxmemory != null) {
            params.cpuspeed = values.cpuspeed
            params.mincpunumber = values.mincpunumber
            params.maxcpunumber = values.maxcpunumber
            params.minmemory = values.minmemory
            params.maxmemory = values.maxmemory
          }
        }

        if (values.networkrate != null && values.networkrate.length > 0) {
          params.networkrate = values.networkrate
        }
        if (values.rootdisksize != null && values.rootdisksize.length > 0) {
          params.rootdisksize = values.rootdisksize
        }
        if (values.qostype === 'storage') {
          var customIops = values.iscustomizeddiskiops === true
          params.customizediops = customIops
          if (!customIops) {
            if (values.diskiopsmin != null && values.diskiopsmin.length > 0) {
              params.miniops = values.diskiopsmin
            }
            if (values.diskiopsmax != null && values.diskiopsmax.length > 0) {
              params.maxiops = values.diskiopsmax
            }
            if (values.hypervisorsnapshotreserve !== undefined &&
               values.hypervisorsnapshotreserve != null && values.hypervisorsnapshotreserve.length > 0) {
              params.hypervisorsnapshotreserve = values.hypervisorsnapshotreserve
            }
          }
        } else if (values.qostype === 'hypervisor') {
          if (values.diskbytesreadrate != null && values.diskbytesreadrate.length > 0) {
            params.bytesreadrate = values.diskbytesreadrate
          }
          if (values.diskbyteswriterate != null && values.diskbyteswriterate.length > 0) {
            params.byteswriterate = values.diskbyteswriterate
          }
          if (values.diskiopsreadrate != null && values.diskiopsreadrate.length > 0) {
            params.iopsreadrate = values.diskiopsreadrate
          }
          if (values.diskiopswriterate != null && values.diskiopswriterate.length > 0) {
            params.iopswriterate = values.diskiopswriterate
          }
        }
        if (values.storagetags != null && values.storagetags.length > 0) {
          var tags = values.storagetags.join(',')
          params.tags = tags
        }
        if (values.hosttags != null && values.hosttags.length > 0) {
          params.hosttags = values.hosttags
        }
        if ('deploymentplanner' in values &&
           values.deploymentplanner !== undefined &&
           values.deploymentplanner != null && values.deploymentplanner.length > 0) {
          params.deploymentplanner = values.deploymentplanner
        }
        if ('deploymentplanner' in values &&
           values.deploymentplanner !== undefined &&
           values.deploymentplanner === 'ImplicitDedicationPlanner' &&
           values.plannermode !== undefined &&
           values.plannermode !== '') {
          params['serviceofferingdetails[0].key'] = 'ImplicitDedicationMode'
          params['serviceofferingdetails[0].value'] = values.plannermode
        }
        if ('isvolatile' in values && values.isvolatile !== undefined) {
          params.isvolatile = values.isvolatile === true
        }
        if ('systemvmtype' in values && values.systemvmtype !== undefined) {
          params.systemvmtype = values.systemvmtype
        }

        if ('leaseduration' in values && values.leaseduration !== undefined) {
          params.leaseduration = values.leaseduration
        }

        if ('leaseexpiryaction' in values && values.leaseexpiryaction !== undefined) {
          params.leaseexpiryaction = values.leaseexpiryaction
        }

        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds = domainIds.concat(this.domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }
        var zoneIndexes = values.zoneid
        var zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          var zoneIds = []
          for (var j = 0; j < zoneIndexes.length; j++) {
            zoneIds = zoneIds.concat(this.zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }
        if (values.storagepolicy) {
          params.storagepolicy = values.storagepolicy
        }
        if (values.externaldetails) {
          Object.entries(values.externaldetails).forEach(([key, value]) => {
            params['externaldetails[0].' + key] = value
          })
        }

        params.sourceofferingid = this.resource.id

        postAPI('cloneServiceOffering', params).then(json => {
          const message = this.isSystem
            ? `${this.$t('message.clone.service.offering')}: `
            : `${this.$t('message.clone.compute.offering')}: `
          this.$message.success(message + values.name)
          this.$emit('refresh-data')
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.error.number'))
      }
      return Promise.resolve()
    }
  }
}
</script>

 <style scoped lang="scss">
  .form-layout {
    width: 80vw;
    @media (min-width: 800px) {
      width: 700px;
    }
  }
</style>
