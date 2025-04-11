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
import { api } from '@/api'
import { Button } from 'ant-design-vue'
import eventBus from '@/config/eventBus'

import DeployVMFromBackup from '@/components/view/DeployVMFromBackup'

export default {
  name: 'CreateVMFromBackup',
  components: {
    DeployVMFromBackup
  },
  data () {
    return {
      configure: false,
      dataPreFill: {},
      vmdetails: {},
      serviceOffering: {},
      loading: true
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  created () {
    this.fetchBackupVmDetails().then(() => {
      this.fetchServiceOffering()
      this.loading = false
    })
  },
  methods: {
    fetchBackupVmDetails () {
      this.serviceOfferings = []
      return api('listBackups', {
        id: this.resource.id,
        listvmdetails: true
      }).then(response => {
        const backups = response.listbackupsresponse.backup || []
        this.vmdetails = backups[0].vmdetails
      })
    },
    fetchServiceOffering () {
      this.serviceOfferings = []
      api('listServiceOfferings', {
        zoneid: this.resource.zoneid,
        id: this.vmdetails.serviceofferingid,
        listall: true
      }).then(response => {
        const serviceOfferings = response.listserviceofferingsresponse.serviceoffering || []
        this.serviceOffering = serviceOfferings[0]
      })
    },
    populatePreFillData () {
      this.dataPreFill.zoneid = this.resource.zoneid
      this.dataPreFill.isIso = (this.vmdetails.isiso === 'true')
      this.dataPreFill.backupid = this.resource.id
      this.dataPreFill.computeofferingid = this.vmdetails.serviceofferingid
      this.dataPreFill.templateid = this.vmdetails.templateid
      this.dataPreFill.networkids = (this.vmdetails.networkids || '').split(',')
      this.diskofferingids = (this.vmdetails.diskofferingids || '').split(',')
      this.miniops = (this.vmdetails.miniops || '').split(',').map(item => item === 'null' ? '' : item)
      this.maxiops = (this.vmdetails.maxiops || '').split(',').map(item => item === 'null' ? '' : item)
      this.deviceid = (this.vmdetails.deviceids || '').split(',').map(item => item === 'null' ? '' : item)
      const volumes = JSON.parse(this.resource.volumes)
      const disksdetails = volumes.map((volume, index) => ({
        name: volume.path,
        type: volume.type,
        size: volume.size / (1024 * 1024 * 1024),
        diskofferingid: this.diskofferingids[index],
        miniops: this.miniops[index],
        maxiops: this.maxiops[index],
        deviceid: this.deviceid[index]
      })).filter(volume => volume.type !== 'ROOT')
      this.dataPreFill.datadisksdetails = disksdetails.map((disk, index) => ({
        id: index,
        ...disk
      }))
      const rootdisksdetails = volumes.map((volume, index) => ({
        size: volume.size / (1024 * 1024 * 1024),
        type: volume.type,
        diskofferingid: this.diskofferingids[index]
      })).filter(volume => volume.type === 'ROOT')
      if (this.serviceOffering.diskofferingid === rootdisksdetails[0].diskofferingid || this.dataPreFill.isIso) {
        this.dataPreFill.overridediskoffering = false
      } else {
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

      const title = this.$t('label.create.instance.from.backup')
      const description = ''
      const password = this.$t('label.password')

      api('createVMFromBackup', args, 'GET', null).then(response => {
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
