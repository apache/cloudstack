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
  <div class="form">
    <div v-if="!configure">
      <div style="margin-bottom: 10px">
        <a-alert type="warning">
          <template #message>
            <div v-html="$t('message.action.create.instance.from.backup')"></div>
          </template>
        </a-alert>
      </div>
      <a-form :model="form" layout="vertical">
        <a-form-item :label="$t('label.name.optional')" name="name">
          <a-input v-model:value="form.name" />
        </a-form-item>
        <a-form-item v-if="resource.isbackupvmexpunged" name="preserveIpAddresses" style="margin-top: 8px">
          <a-switch v-model:checked="form.preserveIpAddresses" />
          <template #label>
            <tooltip-label :title="$t('label.use.backup.ip.address')" :tooltip="$t('label.use.backup.ip.address.tooltip')"/>
          </template>
        </a-form-item>
      </a-form>
      <div class="card-footer">
        <a-button @click="closeAction">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button @click="setConfigure" :loading="loading">
          {{ $t('label.configure.instance') }}
        </a-button>
        <a-button style="margin-left: 10px" type="primary" ref="submit" :loading="loading" @click="handleSubmit">
          {{ $t('label.ok') }}
        </a-button>
      </div>
  </div>
    <div v-else class="form">
      <DeployVMFromBackup
        :preFillContent="dataPreFill"
        @close-action="closeAction"/>
    </div>
  </div>
</template>

<script>

import { h } from 'vue'
import { getAPI, postAPI } from '@/api'
import { Button } from 'ant-design-vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import eventBus from '@/config/eventBus'

import DeployVMFromBackup from '@/components/view/DeployVMFromBackup'

export default {
  name: 'CreateVMFromBackup',
  components: {
    DeployVMFromBackup,
    TooltipLabel
  },
  data () {
    return {
      configure: false,
      dataPreFill: {},
      vmdetails: {},
      serviceOffering: {},
      loading: true,
      form: {
        name: '',
        preserveIpAddresses: false
      }
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  async created () {
    await Promise.all[(
      this.fetchServiceOffering(),
      this.fetchBackupOffering()
    )]
    this.loading = false
  },
  methods: {
    fetchServiceOffering () {
      return getAPI('listServiceOfferings', {
        zoneid: this.resource.zoneid,
        id: this.resource.vmdetails.serviceofferingid,
        listall: true
      }).then(response => {
        const serviceOfferings = response.listserviceofferingsresponse.serviceoffering || []
        this.serviceOffering = serviceOfferings[0]
      })
    },
    fetchBackupOffering () {
      return getAPI('listBackupOfferings', {
        id: this.resource.backupofferingid,
        listall: true
      }).then(response => {
        const backupOfferings = response.listbackupofferingsresponse.backupoffering || []
        this.backupOffering = backupOfferings[0]
      })
    },
    populatePreFillData () {
      this.vmdetails = this.resource.vmdetails
      this.dataPreFill.zoneid = this.resource.zoneid
      this.dataPreFill.crosszoneinstancecreation = this.backupOffering?.crosszoneinstancecreation || this.backupOffering.provider === 'dummy'
      this.dataPreFill.isIso = (this.vmdetails.isiso === 'true')
      this.dataPreFill.ostypeid = this.resource.vmdetails.ostypeid
      this.dataPreFill.ostypename = this.resource.vmdetails.osname
      this.dataPreFill.backupid = this.resource.id
      this.dataPreFill.computeofferingid = this.vmdetails.serviceofferingid
      this.dataPreFill.templateid = this.vmdetails.templateid
      this.dataPreFill.allowtemplateisoselection = true
      this.dataPreFill.isoid = this.vmdetails.templateid
      this.dataPreFill.allowIpAddressesFetch = this.resource.isbackupvmexpunged
      if (this.vmdetails.nics) {
        const nics = JSON.parse(this.vmdetails.nics)
        this.dataPreFill.networkids = nics.map(nic => nic.networkid)
        this.dataPreFill.ipAddresses = nics.map(nic => nic.ipaddress)
        this.dataPreFill.macAddresses = nics.map(nic => nic.macaddress)
      }
      const volumes = JSON.parse(this.resource.volumes)
      const disksdetails = volumes.map((volume, index) => ({
        name: volume.path,
        type: volume.type,
        size: volume.size / (1024 * 1024 * 1024),
        diskofferingid: volume.diskOfferingId,
        miniops: volume.minIops,
        maxiops: volume.maxIops,
        deviceid: volume.deviceId
      })).filter(volume => volume.type !== 'ROOT')
      this.dataPreFill.datadisksdetails = disksdetails.map((disk, index) => ({
        id: index,
        ...disk
      }))
      const rootdisksdetails = volumes.map((volume, index) => ({
        size: volume.size / (1024 * 1024 * 1024),
        type: volume.type,
        diskofferingid: volume.diskOfferingId
      })).filter(volume => volume.type === 'ROOT')
      if (this.dataPreFill.isIso) {
        this.dataPreFill.diskofferingid = rootdisksdetails[0].diskofferingid
        this.dataPreFill.size = rootdisksdetails[0].size
        this.dataPreFill.overridediskoffering = false
      } else if (this.serviceOffering && this.serviceOffering.diskofferingid !== rootdisksdetails[0].diskofferingid) {
        this.dataPreFill.overridediskoffering = true
        this.dataPreFill.diskofferingid = rootdisksdetails[0].diskofferingid
        this.dataPreFill.size = rootdisksdetails[0].size
      }
    },
    setConfigure () {
      this.populatePreFillData()
      this.configure = true
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      console.log('submit')
      e.preventDefault()
      const args = {}
      args.zoneid = this.resource.zoneid
      args.backupid = this.resource.id

      if (this.form.name) {
        args.name = this.form.name
        args.displayname = this.form.name
      }

      if (this.form.preserveIpAddresses) {
        args.preserveip = this.form.preserveIpAddresses
      }

      const title = this.$t('label.create.instance.from.backup')
      const description = ''
      const password = this.$t('label.password')

      postAPI('createVMFromBackup', args, 'GET', null).then(response => {
        const jobId = response.deployvirtualmachineresponse.jobid
        if (jobId) {
          this.$pollJob({
            jobId,
            title,
            description,
            successMethod: result => {
              const vm = result.jobresult.virtualmachine
              const name = vm.displayname || vm.name || vm.id
              if (vm.password) {
                this.$notification.success({
                  message: password + ` ${this.$t('label.for')} ` + name,
                  description: vm.password,
                  btn: () => h(
                    Button,
                    {
                      type: 'primary',
                      size: 'small',
                      onClick: () => this.copyToClipboard(vm.password)
                    },
                    () => [this.$t('label.copy.password')]
                  ),
                  duration: 0
                })
              }
              eventBus.emit('vm-refresh-data')
            },
            loadingMessage: `${title} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            action: {
              isFetchData: false
            }
          })
        }
        // Sending a refresh in case it hasn't picked up the new VM
        new Promise(resolve => setTimeout(resolve, 3000)).then(() => {
          eventBus.emit('vm-refresh-data')
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loading.deploy = false
      }).finally(() => {
        this.form.stayonpage = false
        this.loading.deploy = false
      })
      this.$emit('close-action')
    }
  }
}

</script>

<style lang="scss" scoped>
  .card-footer {
    text-align: right;
    margin-top: 2rem;

    button + button {
      margin-left: 8px;
    }
  }

.form {
  width: 80vw;

  @media (min-width: 500px) {
    min-width: 400px;
    width: 100%;
  }
}
</style>
