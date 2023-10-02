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
  <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
    <a-col :md="24" :lg="18">
      <a-form-item :label="$t('label.register.template.from.vmware.vm.select.vcenter')" name="vmwareopt" ref="vmwareopt" v-if="zoneid != ''">
        <a-radio-group
          v-model:value="vcenterSelectedOption"
          buttonStyle="solid">
          <a-radio-button value="existing">
            {{ $t('label.register.template.from.vmware.vm.linked.vcenter') }}
          </a-radio-button>
          <a-radio-button value="new">
            {{ $t('label.register.template.from.vmware.vm.new.vcenter') }}
          </a-radio-button>
        </a-radio-group>
      </a-form-item>

      <div v-if="vcenterSelectedOption === 'existing'">
        <div v-if="existingvcenter.length > 0">
          <a-form-item :label="$t('label.vcenter')" name="vmwaredatacenter" ref="vmwaredatacenter">
            <a-select
              v-model:value="form.vmwaredatacenter"
              :loading="loading"
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              placeholder="Select Zones"
              @change="handlerSelectExistingVmwareDc">
              <a-select-option v-for="opt in existingvcenter" :key="opt.id">
                  {{ 'VC: ' + opt.vcenter + ' - DC: ' + opt.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div v-else>
          {{ $t('message.register.template.from.vmware.vm.select.new.vcenter') }}
        </div>
      </div>
      <div v-else-if="vcenterSelectedOption === 'new'">
        <a-form-item ref="vcenter" name="vcenter">
          <template #label>
            <tooltip-label :title="$t('label.vcenter')" :tooltip="apiParams.vcenter.description"/>
          </template>
          <a-input
            v-model:value="vcenter"
            :placeholder="apiParams.vcenter.description"
          />
        </a-form-item>
        <a-form-item ref="datacenter" name="datacenter">
          <template #label>
            <tooltip-label :title="$t('label.vcenter.datacenter')" :tooltip="apiParams.datacentername.description"/>
          </template>
          <a-input
            v-model:value="datacenter"
            :placeholder="apiParams.datacentername.description"
          />
        </a-form-item>
        <a-form-item ref="username" name="username">
          <template #label>
            <tooltip-label :title="$t('label.vcenter.username')" :tooltip="apiParams.username.description"/>
          </template>
          <a-input
            v-model:value="username"
            :placeholder="apiParams.username.description"
          />
        </a-form-item>
        <a-form-item ref="password" name="password">
          <template #label>
            <tooltip-label :title="$t('label.vcenter.password')" :tooltip="apiParams.password.description"/>
          </template>
          <a-input-password
            v-model:value="password"
            :placeholder="apiParams.password.description"
          />
        </a-form-item>
      </div>
      <div class="card-footer">
        <a-button
          v-if="vcenterSelectedOption == 'existing' || vcenterSelectedOption == 'new'"
          :disabled="(vcenterSelectedOption === 'new' && (vcenter === '' || datacentername === '' || username === '' || password === '')) ||
            (vcenterSelectedOption === 'existing' && selectedExistingVcenterId === '')"
          :loading="loading"
          type="primary"
          @click="listVmwareDatacenterVms">{{ $t('label.register.template.from.vmware.vm.list.vcenter.vms') }}</a-button>
      </div>
    </a-col>
  </a-form>
</template>

<script>
import { api } from '@/api'
import { ref, reactive } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import Status from '@/components/widgets/Status'

export default {
  name: 'SelectVmwareVcenter',
  props: {
    zoneid: {
      type: String,
      required: true
    }
  },
  components: {
    TooltipLabel,
    Status
  },
  data () {
    return {
      vcenter: '',
      datacenter: '',
      username: '',
      password: '',
      loading: false,
      zones: {},
      vcenterSelectedOption: '',
      existingvcenter: [],
      selectedExistingVcenterId: '',
      selectedPoweredOnVm: false,
      vmwareDcVms: [],
      vmwareDcVmSelectedRows: [],
      vmwareDcVmsColumns: [
        {
          title: this.$t('label.hostname'),
          dataIndex: 'hostname'
        },
        {
          title: this.$t('label.cluster'),
          dataIndex: 'clustername'
        },
        {
          title: this.$t('label.virtualmachinename'),
          dataIndex: 'virtualmachinename'
        },
        {
          title: this.$t('label.powerstate'),
          key: 'powerstate',
          dataIndex: 'powerstate'
        }
      ]
    }
  },
  computed: {
    vmwareDcVmsSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.vmwareDcVmSelectedRows || [],
        onChange: this.onVmwareDcVmSelectRow
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('listVmwareDcVms')
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        vcenter: '',
        username: '',
        password: ''
      })
      this.rules = reactive({})
    },
    listVmwareDatacenterVms () {
      this.loading = true
      this.$emit('loadingVmwareUnmanagedInstances')
      const params = {}
      if (this.vcenterSelectedOption === 'new') {
        params.datacentername = this.datacenter
        params.vcenter = this.vcenter
        params.username = this.username
        params.password = this.password
      } else {
        params.existingvcenterid = this.selectedExistingVcenterId
      }
      api('listVmwareDcVms', params).then(json => {
        const obj = {
          params: params,
          response: json.listvmwaredcvmsresponse
        }
        this.$emit('listedVmwareUnmanagedInstances', obj)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
