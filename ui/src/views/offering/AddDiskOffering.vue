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
  name: 'AddDiskOffering',
  components: {
    DiskOfferingForm
  },
  data () {
    return {
      formRef: null,
      form: reactive({}),
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createDiskOffering')
  },
  created () {
  },
  methods: {
    isAdmin () {
      return isAdmin()
    },
    handleSubmit (e) {
      if (e && e.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return

      this.$refs.formRef.validate().then((values) => {
        var params = {
          name: values.name,
          displaytext: values.displaytext,
          storageType: values.storagetype,
          cacheMode: values.writecachetype,
          provisioningType: values.provisioningtype,
          customized: values.customdisksize,
          disksizestrictness: values.disksizestrictness,
          encrypt: values.encryptdisk
        }
        if (values.customdisksize !== true) {
          params.disksize = values.disksize
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
          var tags = values.tags.join(',')
          params.tags = tags
        }
        if (values.ispublic !== true) {
          var domainIndexes = values.domainid
          var domainId = null
          if (domainIndexes && domainIndexes.length > 0) {
            var domainIds = []
            const domains = this.$refs.formRef.domains
            for (var i = 0; i < domainIndexes.length; i++) {
              domainIds.push(domains[domainIndexes[i]].id)
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
          const zones = this.$refs.formRef.zones
          for (var j = 0; j < zoneIndexes.length; j++) {
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
        postAPI('createDiskOffering', params).then(json => {
          this.$emit('publish-disk-offering-id', json?.creatediskofferingresponse?.diskoffering?.id)
          this.$message.success(`${this.$t('message.disk.offering.created')} ${values.name}`)
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

<style scoped lang="scss">
  .form-layout {
    width: 80vw;

    @media (min-width: 800px) {
      width: 480px;
    }
  }
</style>
