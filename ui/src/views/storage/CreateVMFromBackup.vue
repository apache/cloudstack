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
        <a-button @click="setConfigure">
          {{ $t('label.configure.instance') }}
        </a-button>
        <a-button style="margin-left: 10px" type="primary" ref="submit" @click="handleSubmit">
          {{ $t('label.ok') }}
        </a-button>
      </div>
  </div>
    <div v-else class="form">
      <DeployVMFromBackup
        :preFillContent="dataPreFill"
        @close-action="closeModal"/>
    </div>
  </div>
</template>

<script>

import { h } from 'vue'
import { api } from '@/api'
import { Button } from 'ant-design-vue'
import eventBus from '@/config/eventBus'

import DeployVMFromBackup from '@/views/compute/DeployVMFromBackup'

export default {
  name: 'CreateVMFromBackup',
  components: {
    DeployVMFromBackup
  },
  data () {
    return {
      configure: false,
      dataPreFill: {}
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  created () {
    this.dataPreFill.backupid = this.resource.id
    this.dataPreFill.computeofferingid = this.resource.vmdetails.serviceofferingid
    this.dataPreFill.templateid = this.resource.vmdetails.templateid
    this.dataPreFill.networkids = (this.resource.vmdetails.networkids || '').split(',')
    this.diskofferingids = (this.resource.vmdetails.diskofferingids || '').split(',')
    this.miniops = (this.resource.vmdetails.miniops || '').split(',').map(item => item === 'null' ? '' : item)
    this.maxiops = (this.resource.vmdetails.maxiops || '').split(',').map(item => item === 'null' ? '' : item)
    const volumes = JSON.parse(this.resource.volumes).slice(1)
    const datadisksdetails = volumes.map((volume, index) => ({
      id: index,
      name: volume.uuid,
      size: volume.size / (1024 * 1024 * 1024),
      diskofferingid: this.diskofferingids[index],
      miniops: this.miniops[index],
      maxiops: this.maxiops[index]
    }))
    this.dataPreFill.datadisksdetails = datadisksdetails
  },
  methods: {
    setConfigure () {
      this.configure = true
    },
    closeAction () {
      this.$emit('close-action')
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      console.log('wizard submit')
      e.preventDefault()
      const args = {}
      args.zoneid = this.resource.zoneid
      args.backupid = this.resource.id

      const title = this.$t('label.create.new.instance.from.backup')
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
