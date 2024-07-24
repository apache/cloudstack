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
    <a-col :md="24" :lg="24">
      <div>
        <a-form-item :label="$t('label.select.source.vcenter.datacenter')" name="vmwareopt" ref="vmwareopt">
          <a-radio-group
            style="text-align: center; width: 100%"
            v-model:value="vcenterSelectedOption"
            buttonStyle="solid"
            @change="onVcenterTypeChange">
            <a-radio-button value="existing" style="width: 50%; text-align: center">
              {{ $t('label.existing') }}
            </a-radio-button>
            <a-radio-button value="new" style="width: 50%; text-align: center">
              {{ $t('label.external') }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
      </div>
      <div v-if="vcenterSelectedOption === 'existing'">
        <a-form-item name="sourcezoneid" ref="sourcezoneid" :label="$t('label.zoneid')">
          <a-select
            v-model:value="form.sourcezoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            @change="onSelectZoneId"
            :loading="loading"
          >
            <a-select-option v-for="zoneitem in zones" :key="zoneitem.id" :label="zoneitem.name">
              <span>
                <resource-icon v-if="zoneitem.icon" :image="zoneitem.icon" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ zoneitem.name }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="sourcezoneid">
          <a-form-item :label="$t('label.vcenter')" name="vmwaredatacenter" ref="vmwaredatacenter" v-if="existingvcenter.length > 0">
            <a-select
              v-model:value="form.vmwaredatacenter"
              :loading="loading"
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return  option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :placeholder="$t('label.vcenter.datacenter')"
              @change="onSelectExistingVmwareDatacenter">
              <a-select-option v-for="opt in existingvcenter" :key="opt.id">
                  {{ 'VC: ' + opt.vcenter + ' - DC: ' + opt.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <div v-else>
            {{ $t('message.list.zone.vmware.datacenter.empty') }}
          </div>
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
          @click="listVmwareDatacenterVms">{{ $t('label.list.vmware.vcenter.vms') }}</a-button>
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
    this.fetchZones()
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
    },
    fetchZones () {
      this.loading = true
      api('listZones', { showicon: true }).then(response => {
        this.zones = response.listzonesresponse.zone || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    onSelectZoneId (value) {
      this.sourcezoneid = value
      this.listZoneVmwareDcs()
    },
    listZoneVmwareDcs () {
      this.loading = true
      api('listVmwareDcs', { zoneid: this.sourcezoneid }).then(response => {
        if (response.listvmwaredcsresponse.VMwareDC && response.listvmwaredcsresponse.VMwareDC.length > 0) {
          this.existingvcenter = response.listvmwaredcsresponse.VMwareDC
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    onSelectExistingVmwareDatacenter (value) {
      this.selectedExistingVcenterId = value
    },
    onVcenterTypeChange () {
      this.$emit('onVcenterTypeChanged', this.vcenterSelectedOption)
    }
  }
}
</script>

<style scoped>
.card-footer {
  text-align: right;
}

.card-footer button {
  width: 50%;
  text-align: center;
}
</style>
