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
      <DiskOfferingForm
        ref="formRef"
        :initialValues="form"
        :apiParams="apiParams"
        :isAdmin="isAdmin"
        @submit="handleSubmit">
        <template #form-actions>
          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </template>
      </DiskOfferingForm>
    </a-spin>
  </div>
</template>

<script>
import DiskOfferingForm from '@/components/offering/DiskOfferingForm'
import { reactive } from 'vue'
import { postAPI } from '@/api'
import { isAdmin } from '@/role'

export default {
  name: 'CloneDiskOffering',
  components: {
    DiskOfferingForm
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      formRef: null,
      form: reactive({}),
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('cloneDiskOffering')
  },
  created () {
    this.populateFormFromResource()
  },
  methods: {
    populateFormFromResource () {
      if (!this.resource) return

      const r = this.resource
      this.form.name = r.name + ' - Clone'
      this.form.displaytext = r.displaytext

      if (r.storagetype) {
        this.form.storagetype = r.storagetype
      }
      if (r.provisioningtype) {
        this.form.provisioningtype = r.provisioningtype
      }
      if (r.customized !== undefined) {
        this.form.customdisksize = r.customized
      }
      if (r.disksize) this.form.disksize = r.disksize

      if (r.cachemode) {
        this.form.writecachetype = r.cachemode
      }

      if (r.disksizestrictness !== undefined) {
        this.form.disksizestrictness = r.disksizestrictness
      }
      if (r.encrypt !== undefined) {
        this.form.encryptdisk = r.encrypt
      }

      if (r.diskBytesReadRate || r.diskBytesReadRateMax || r.diskBytesWriteRate || r.diskBytesWriteRateMax || r.diskIopsReadRate || r.diskIopsWriteRate) {
        this.form.qostype = 'hypervisor'
        if (r.diskBytesReadRate) this.form.diskbytesreadrate = r.diskBytesReadRate
        if (r.diskBytesReadRateMax) this.form.diskbytesreadratemax = r.diskBytesReadRateMax
        if (r.diskBytesWriteRate) this.form.diskbyteswriterate = r.diskBytesWriteRate
        if (r.diskBytesWriteRateMax) this.form.diskbyteswriteratemax = r.diskBytesWriteRateMax
        if (r.diskIopsReadRate) this.form.diskiopsreadrate = r.diskIopsReadRate
        if (r.diskIopsWriteRate) this.form.diskiopswriterate = r.diskIopsWriteRate
      } else if (r.miniops || r.maxiops) {
        this.form.qostype = 'storage'
        if (r.miniops) this.form.diskiopsmin = r.miniops
        if (r.maxiops) this.form.diskiopsmax = r.maxiops
        if (r.hypervisorsnapshotreserve) this.form.hypervisorsnapshotreserve = r.hypervisorsnapshotreserve
      }
      if (r.iscustomizediops !== undefined) {
        this.form.iscustomizeddiskiops = r.iscustomizediops
      }

      if (r.tags) {
        this.form.tags = r.tags.split(',')
      }

      if (r.vspherestoragepolicy) this.form.storagepolicy = r.vspherestoragepolicy
    },
    isAdmin () {
      return isAdmin()
    },
    handleSubmit (e) {
      if (e && e.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return

      this.$refs.formRef.validate().then((values) => {
        const params = {
          sourceofferingid: this.resource.id,
          name: values.name
        }

        if (values.displaytext) {
          params.displaytext = values.displaytext
        }
        if (values.storagetype) {
          params.storagetype = values.storagetype
        }
        if (values.writecachetype) {
          params.cachemode = values.writecachetype
        }
        if (values.provisioningtype) {
          params.provisioningtype = values.provisioningtype
        }
        if (values.customdisksize !== undefined) {
          params.customized = values.customdisksize
        }
        if (values.disksizestrictness !== undefined) {
          params.disksizestrictness = values.disksizestrictness
        }
        if (values.encryptdisk !== undefined) {
          params.encrypt = values.encryptdisk
        }

        if (values.customdisksize !== true && values.disksize) {
          params.disksize = values.disksize
        }

        if (values.qostype === 'storage') {
          const customIops = values.iscustomizeddiskiops === true
          params.customizediops = customIops
          if (!customIops) {
            if (values.diskiopsmin != null && values.diskiopsmin.length > 0) {
              params.miniops = values.diskiopsmin
            }
            if (values.diskiopsmax != null && values.diskiopsmax.length > 0) {
              params.maxiops = values.diskiopsmax
            }
            if (values.hypervisorsnapshotreserve != null && values.hypervisorsnapshotreserve.length > 0) {
              params.hypervisorsnapshotreserve = values.hypervisorsnapshotreserve
            }
          }
        } else if (values.qostype === 'hypervisor') {
          if (values.diskbytesreadrate != null && values.diskbytesreadrate.length > 0) {
            params.bytesreadrate = values.diskbytesreadrate
          }
          if (values.diskbytesreadratemax != null && values.diskbytesreadratemax.length > 0) {
            params.bytesreadratemax = values.diskbytesreadratemax
          }
          if (values.diskbyteswriterate != null && values.diskbyteswriterate.length > 0) {
            params.byteswriterate = values.diskbyteswriterate
          }
          if (values.diskbyteswriteratemax != null && values.diskbyteswriteratemax.length > 0) {
            params.byteswriteratemax = values.diskbyteswriteratemax
          }
          if (values.diskiopsreadrate != null && values.diskiopsreadrate.length > 0) {
            params.iopsreadrate = values.diskiopsreadrate
          }
          if (values.diskiopswriterate != null && values.diskiopswriterate.length > 0) {
            params.iopswriterate = values.diskiopswriterate
          }
        }

        if (values.tags != null && values.tags.length > 0) {
          const tags = values.tags.join(',')
          params.tags = tags
        }

        if (values.ispublic !== true) {
          const domainIndexes = values.domainid
          let domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            const domainIds = []
            const domains = this.$refs.formRef.domains
            for (let i = 0; i < domainIndexes.length; i++) {
              domainIds.push(domains[domainIndexes[i]].id)
            }
            domainId = domainIds.join(',')
          }
          if (domainId) {
            params.domainid = domainId
          }
        }

        const zoneIndexes = values.zoneid
        let zoneId = null
        if (zoneIndexes && zoneIndexes.length > 0) {
          const zoneIds = []
          const zones = this.$refs.formRef.zones
          for (let j = 0; j < zoneIndexes.length; j++) {
            zoneIds.push(zones[zoneIndexes[j]].id)
          }
          zoneId = zoneIds.join(',')
        }
        if (zoneId) {
          params.zoneid = zoneId
        }

        if (values.storagepolicy) {
          params.storagepolicy = values.storagepolicy
        }

        this.loading = true
        postAPI('cloneDiskOffering', params).then(json => {
          this.$message.success(`${this.$t('message.success.clone.disk.offering')} ${values.name}`)
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
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 700px) {
      width: 550px;
    }
  }
</style>
