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
  <div>
    <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">

        <a-row :gutter="12">
          <a-col :md="24" :lg="12">
            <a-form-item :label="'Select source vCenter:'" name="vmwareopt" ref="vmwareopt" v-if="zoneid != ''">
              <a-radio-group
                v-model:value="vcenterSelectedOption"
                buttonStyle="solid">
                <a-radio-button value="existing">
                  Linked vCenter
                </a-radio-button>
                <a-radio-button value="new">
                  New vCenter
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
                No vCenter is linked to the zone, please select New vCenter
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
                @click="listVmwareStoppedVms">List Stopped VMs</a-button>
            </div>
          </a-col>

          <a-col :md="24" :lg="12">
            <p>Select a Stopped VM:</p>
            <a-table
              class="instances-card-table"
              :loading="loading"
              :rowSelection="stoppedVmsSelection"
              :rowKey="(record, index) => record"
              :columns="stoppedVmsColumns"
              :data-source="stoppedVms"
              :pagination="{ pageSizeOptions: ['10', '15'], showSizeChanger: true}"
              size="middle"
              :rowClassName="getRowClassName"
            >
            </a-table>
          </a-col>
        </a-row>
    </a-form>
  </div>
</template>

<script>
import { api } from '@/api'
import { ref, reactive } from 'vue'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'RegisterTemplateFromVcenter',
  props: {
    zoneid: {
      type: String,
      required: true
    }
  },
  components: {
    TooltipLabel
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
      stoppedVms: [],
      stoppedVmSelectedRows: [],
      stoppedVmsColumns: [
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
        }
      ]
    }
  },
  computed: {
    stoppedVmsSelection () {
      return {
        type: 'radio',
        selectedRowKeys: this.stoppedVmSelectedRows || [],
        onChange: this.onStoppedVmSelectRow
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('registerTemplateFromVmware')
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
    handlerSelectExistingVmwareDc (value) {
      this.selectedExistingVcenterId = value
    },
    fetchVmwareDatacenters () {
      api('listVmwareDcs', {
        zoneid: this.zoneid
      }).then(response => {
        if (response.listvmwaredcsresponse.VMwareDC && response.listvmwaredcsresponse.VMwareDC.length > 0) {
          this.existingvcenter = response.listvmwaredcsresponse.VMwareDC
        }
      })
    },
    onStoppedVmSelectRow (value) {
      this.stoppedVmSelectedRows = value
      const obj = {}
      if (this.vcenterSelectedOption === 'existing') {
        obj.vcenterid = this.selectedExistingVcenterId
      } else {
        obj.datacentername = this.datacenter
        obj.vcenter = this.vcenter
        obj.username = this.username
        obj.password = this.password
      }
      obj.host = value[0].hostname
      obj.vmname = value[0].virtualmachinename
      obj.clustername = value[0].clustername
      this.$emit('select-stopped-vm', obj)
    },
    listVmwareStoppedVms () {
      this.loading = true
      const params = {}
      if (this.vcenterSelectedOption === 'new') {
        params.datacentername = this.datacenter
        params.vcenter = this.vcenter
        params.username = this.username
        params.password = this.password
      } else {
        params.existingvcenterid = this.selectedExistingVcenterId
      }
      api('listVmwareDcStoppedVms', params).then(json => {
        this.stoppedVms = json.listvmwaredcstoppedvmsresponse.vmwarestoppedvm
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
