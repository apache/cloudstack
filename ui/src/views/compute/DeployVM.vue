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
    <a-row :gutter="12">
      <a-col :md="24" :lg="17">
        <a-card :bordered="true" :title="this.$t('newInstance')">
          <a-form
            :form="form"
            @submit="handleSubmit"
            layout="vertical"
          >
            <a-form-item :label="this.$t('name')">
              <a-input
                v-decorator="['name']"
                :placeholder="this.$t('vm.name.description')"
              />
            </a-form-item>

            <a-form-item :label="this.$t('zoneid')">
              <a-select
                v-decorator="['zoneid', {
                  rules: [{ required: zoneId.required, message: 'Please select option' }]
                }]"
                :placeholder="this.$t('vm.zone.description')"
              >
                <a-select-option
                  v-for="(opt, optIndex) in zoneId.opts"
                  :key="optIndex"
                  :value="opt.id"
                >
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-collapse
              :accordion="true"
              defaultActiveKey="templates"
            >
              <a-collapse-panel :header="this.$t('Templates')" key="templates">
                <template-selection
                  :templates="templateId.opts"
                ></template-selection>
                <a-form-item :label="this.$t('diskSize')">
                  <a-row>
                    <a-col :span="10">
                      <a-slider
                        :min="0"
                        :max="1024"
                        v-decorator="['rootdisksize']"
                      />
                    </a-col>
                    <a-col :span="4">
                      <a-input-number
                        v-decorator="['rootdisksize', {
                          rules: [{ required: false, message: 'Please enter a number' }]
                        }]"
                        :placeholder="this.$t('vm.rootdisksize')"
                        :formatter="value => `${value} GB`"
                        :parser="value => value.replace(' GB', '')"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-collapse-panel>
              <a-collapse-panel :header="this.$t('ISOs')" key="isos">
                <!-- ToDo: Add iso selection -->
              </a-collapse-panel>
            </a-collapse>

            <compute-selection
              :compute-items="serviceOfferingId.opts"
              :value="serviceOffering ? serviceOffering.id : ''"
              @select-compute-item="($event) => updateComputeOffering($event)"
            ></compute-selection>

            <a-form-item :label="this.$t('diskOfferingId')">
              <a-select
                v-decorator="['diskofferingid', {
                  rules: [{ required: diskOfferingId.required, message: 'Please select option' }]
                }]"
                :placeholder="this.$t('vm.diskoffering.description')"
              >
                <a-select-option
                  v-for="(opt, optIndex) in diskOfferingId.opts"
                  :key="optIndex"
                  :value="opt.id"
                >
                  {{ opt.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <div class="card-footer">
              <!-- ToDo extract as component -->
              <a-button @click="() => this.$router.back()">{{ this.$t('cancel') }}</a-button>
              <a-button type="primary" @click="handleSubmit">{{ this.$t('submit') }}</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7">
        <info-card :resource="vm" :title="this.$t('yourInstance')" >
          <div slot="details" v-if="vm.diskofferingid || instanceConfig.rootdisksize">
            <a-icon type="hdd"></a-icon>
            <span style="margin-left: 10px">
              <span v-if="instanceConfig.rootdisksize">{{ instanceConfig.rootdisksize }} GB (Root)</span>
              <span v-if="instanceConfig.rootdisksize && instanceConfig.diskofferingid"> | </span>
              <span v-if="instanceConfig.diskofferingid">{{ diskOffering.disksize }} GB (Data)</span>
            </span>
          </div>
        </info-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import Vue from 'vue'
import { api } from '@/api'
import store from '@/store'
import _ from 'lodash'

import InfoCard from '@/components/view/InfoCard'
import ComputeSelection from './wizard/ComputeSelection'
import TemplateSelection from './wizard/TemplateSelection'

export default {
  name: 'Wizard',
  components: {
    InfoCard,
    ComputeSelection,
    TemplateSelection
  },
  props: {
    visible: {
      type: Boolean
    }
  },
  data () {
    return {
      vm: {},
      params: [],
      visibleParams: [
        'name',
        'templateid',
        'serviceofferingid',
        'diskofferingid',
        'zoneid',
        'rootdisksize'
      ],
      instanceConfig: [],
      template: {},
      serviceOffering: {},
      diskOffering: {},
      zone: {}
    }
  },
  computed: {
    filteredParams () {
      return this.visibleParams.map((fieldName) => {
        return this.params.find((param) => fieldName === param.name)
      })
    },
    templateId () {
      return this.getParam('templateid')
    },
    serviceOfferingId () {
      return this.getParam('serviceofferingid')
    },
    diskOfferingId () {
      return this.getParam('diskofferingid')
    },
    zoneId () {
      return this.getParam('zoneid')
    }
  },
  watch: {
    instanceConfig (instanceConfig) {
      this.template = this.templateId.opts.find((option) => option.id === instanceConfig.templateid)
      this.serviceOffering = this.serviceOfferingId.opts.find((option) => option.id === instanceConfig.computeofferingid)
      this.diskOffering = this.diskOfferingId.opts.find((option) => option.id === instanceConfig.diskofferingid)
      this.zone = this.zoneId.opts.find((option) => option.id === instanceConfig.zoneid)

      if (this.zone) {
        this.vm['zoneid'] = this.zone.id
        this.vm['zonename'] = this.zone.name
      }

      if (this.template) {
        this.vm['templateid'] = this.template.id
        this.vm['templatename'] = this.template.displaytext
        this.vm['ostypeid'] = this.template.ostypeid
        this.vm['ostypename'] = this.template.ostypename
      }

      if (this.serviceOffering) {
        this.vm['serviceofferingid'] = this.serviceOffering.id
        this.vm['serviceofferingname'] = this.serviceOffering.displaytext
        this.vm['cpunumber'] = this.serviceOffering.cpunumber
        this.vm['cpuspeed'] = this.serviceOffering.cpuspeed
        this.vm['memory'] = this.serviceOffering.memory
      }

      if (this.diskOffering) {
        this.vm['diskofferingid'] = this.diskOffering.id
        this.vm['diskofferingname'] = this.diskOffering.displaytext
        this.vm['diskofferingsize'] = this.diskOffering.disksize
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (props, fields) => {
        this.instanceConfig = { ...this.form.getFieldsValue(), ...fields }
        this.vm = this.instanceConfig
      }
    })
    this.form.getFieldDecorator('computeofferingid', { initialValue: [], preserve: true })
  },
  created () {
    this.params = store.getters.apis[this.$route.name]['params']
    this.filteredParams.forEach((param) => {
      this.fetchOptions(param)
    })
    Vue.nextTick().then(() => {
      this.instanceConfig = this.form.getFieldsValue() // ToDo: maybe initialize with some other defaults
    })
  },
  methods: {
    updateComputeOffering (id) {
      this.form.setFieldsValue({
        computeofferingid: id
      })
    },
    getParam (paramName) {
      return this.params.find((param) => param.name === paramName)
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmit () {
      console.log('wizard submit')
    },
    fetchOptions (param) {
      const paramName = param.name
      const possibleName = `list${paramName.replace('id', '').toLowerCase()}s`
      let possibleApi
      if (paramName === 'id') {
        possibleApi = this.apiName
      } else {
        possibleApi = _.filter(Object.keys(store.getters.apis), (api) => {
          return api.toLowerCase().startsWith(possibleName)
        })[0]
      }

      if (!possibleApi) {
        return
      }

      param.loading = true
      param.opts = []
      const params = {}
      params.listall = true
      if (possibleApi === 'listTemplates') {
        params.templatefilter = 'executable'
      }
      api(possibleApi, params).then((response) => {
        param.loading = false
        _.map(response, (responseItem, responseKey) => {
          if (!responseKey.includes('response')) {
            return
          }
          _.map(responseItem, (response, key) => {
            if (key === 'count') {
              return
            }
            param.opts = response
            this.$forceUpdate()
          })
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .card-footer {
    text-align: right;

    button + button {
      margin-left: 8px;
    }
  }

  .ant-list-item-meta-avatar {
    font-size: 1rem;
  }

  .ant-collapse {
    margin: 2rem 0;
  }
</style>
