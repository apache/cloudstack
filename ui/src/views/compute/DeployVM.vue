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
            <a-collapse
              :accordion="false"
              :bordered="false"
              defaultActiveKey="basic"
            >
              <a-collapse-panel :header="this.$t('BasicSetup')" key="basic">
                <a-form-item :label="this.$t('name')">
                  <a-input
                    v-decorator="['name']"
                    :placeholder="this.$t('vm.name.description')"
                  />
                </a-form-item>

                <a-form-item :label="this.$t('zoneid')">
                  <a-select
                    v-decorator="['zoneid', {
                      rules: [{ required: true, message: 'Please select option' }]
                    }]"
                    :placeholder="this.$t('vm.zone.description')"
                    :options="zoneSelectOptions"
                    @change="onSelectZoneId"
                  ></a-select>
                </a-form-item>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('templateIso')" key="templates-isos">
                <a-collapse
                  :accordion="true"
                  defaultActiveKey="templates"
                  @change="onTemplatesIsosCollapseChange"
                >
                  <a-collapse-panel :header="this.$t('Templates')" key="templates">
                    <template-iso-selection
                      input-decorator="templateid"
                      :items="options.templates"
                    ></template-iso-selection>
                    <disk-size-selection
                      input-decorator="rootdisksize"
                    ></disk-size-selection>
                  </a-collapse-panel>

                  <a-collapse-panel :header="this.$t('ISOs')" key="isos">
                    <template-iso-selection
                      input-decorator="isoid"
                      :items="options.isos"
                    ></template-iso-selection>
                  </a-collapse-panel>
                </a-collapse>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('serviceOfferingId')" key="compute">
                <compute-selection
                  :compute-items="options.serviceOfferings"
                  :value="serviceOffering ? serviceOffering.id : ''"
                  @select-compute-item="($event) => updateComputeOffering($event)"
                ></compute-selection>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('diskOfferingId')" key="disk">
                <disk-offering-selection
                  :items="options.diskOfferings"
                  :value="diskOffering ? diskOffering.id : ''"
                  @select-disk-offering-item="($event) => updateDiskOffering($event)"
                ></disk-offering-selection>
                <disk-size-selection
                  v-if="diskOffering && diskOffering.iscustomized"
                  input-decorator="size"
                ></disk-size-selection>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('Affinity Groups')" key="affinity">
                <affinity-group-selection
                  :items="options.affinityGroups"
                  :value="affinityGroupIds"
                  @select-affinity-group-item="($event) => updateAffinityGroups($event)"
                ></affinity-group-selection>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('networks')" key="networks">
                <a-collapse
                  :accordion="false"
                >
                  <a-collapse-panel
                    :header="$t('existingNetworks')"
                  >
                    <network-selection
                      :items="options.networks"
                      :value="networkOfferingIds"
                      @select-network-item="($event) => updateNetworks($event)"
                    ></network-selection>
                  </a-collapse-panel>

                  <a-collapse-panel
                    :header="$t('addNewNetworks')"
                  >
                    <network-creation></network-creation>
                  </a-collapse-panel>
                </a-collapse>

                <network-configuration
                  v-if="networks.length > 0"
                  :items="networks"
                ></network-configuration>
              </a-collapse-panel>

              <a-collapse-panel :header="this.$t('sshKeyPairs')" key="sshKeyPairs">
                <ssh-key-pair-selection
                  :items="options.sshKeyPairs"
                  :value="sshKeyPair ? sshKeyPair.name : ''"
                  @select-ssh-key-pair-item="($event) => updateSshKeyPairs($event)"
                ></ssh-key-pair-selection>
              </a-collapse-panel>
            </a-collapse>

            <div class="card-footer">
              <!-- ToDo extract as component -->
              <a-button @click="() => this.$router.back()">{{ this.$t('cancel') }}</a-button>
              <a-button type="primary" @click="handleSubmit">{{ this.$t('submit') }}</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
      <a-col :md="24" :lg="7" v-if="!isMobile()">
        <a-affix :offsetTop="75">
          <info-card :resource="vm" :title="this.$t('yourInstance')">
            <!-- ToDo: Refactor this, maybe move everything to the info-card component -->
            <div slot="details" v-if="diskSize" style="margin-bottom: 12px;">
              <a-icon type="hdd"></a-icon>
              <span style="margin-left: 10px">{{ diskSize }}</span>
            </div>
            <div slot="details" v-if="networks">
              <div v-for="network in networks" :key="network.id" style="margin-bottom: 12px;">
                <a-icon type="api"></a-icon>
                <span style="margin-left: 10px">{{ network.name }}</span>
              </div>
            </div>
          </info-card>
        </a-affix>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import Vue from 'vue'
import { api } from '@/api'
import _ from 'lodash'
import { mixin, mixinDevice } from '@/utils/mixin.js'
import store from '@/store'

import InfoCard from '@/components/view/InfoCard'
import ComputeSelection from './wizard/ComputeSelection'
import DiskOfferingSelection from '@views/compute/wizard/DiskOfferingSelection'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'
import TemplateIsoSelection from '@views/compute/wizard/TemplateIsoSelection'
import AffinityGroupSelection from '@views/compute/wizard/AffinityGroupSelection'
import NetworkSelection from '@views/compute/wizard/NetworkSelection'
import NetworkConfiguration from '@views/compute/wizard/NetworkConfiguration'
import NetworkCreation from '@views/compute/wizard/NetworksCreation'
import SshKeyPairSelection from '@views/compute/wizard/SshKeyPairSelection'

export default {
  name: 'Wizard',
  components: {
    SshKeyPairSelection,
    NetworkCreation,
    NetworkConfiguration,
    NetworkSelection,
    AffinityGroupSelection,
    TemplateIsoSelection,
    DiskSizeSelection,
    DiskOfferingSelection,
    InfoCard,
    ComputeSelection
  },
  props: {
    visible: {
      type: Boolean
    }
  },
  mixins: [mixin, mixinDevice],
  data () {
    return {
      vm: {},
      options: {
        templates: [],
        isos: [],
        serviceOfferings: [],
        diskOfferings: [],
        zones: [],
        affinityGroups: [],
        networks: [],
        sshKeyPairs: []
      },
      instanceConfig: [],
      template: {},
      iso: {},
      serviceOffering: {},
      diskOffering: {},
      affinityGroups: [],
      networks: [],
      zone: {},
      sshKeyPair: {},
      isoFilter: [
        'executable',
        'selfexecutable',
        'sharedexecutable'
      ]
    }
  },
  computed: {
    diskSize () {
      const rootDiskSize = _.get(this.instanceConfig, 'rootdisksize', 0)
      const customDiskSize = _.get(this.instanceConfig, 'size', 0)
      const diskOfferingDiskSize = _.get(this.diskOffering, 'disksize', 0)
      const dataDiskSize = diskOfferingDiskSize > 0 ? diskOfferingDiskSize : customDiskSize
      const size = []
      if (rootDiskSize > 0) {
        size.push(`${rootDiskSize} GB (Root)`)
      }
      if (dataDiskSize > 0) {
        size.push(`${dataDiskSize} GB (Data)`)
      }
      return size.join(' | ')
    },
    affinityGroupIds () {
      return _.map(this.affinityGroups, 'id')
    },
    params () {
      return {
        templates: {
          list: 'listTemplates',
          options: {
            templatefilter: 'executable',
            zoneid: _.get(this.zone, 'id')
          }
        },
        serviceOfferings: {
          list: 'listServiceOfferings'
        },
        diskOfferings: {
          list: 'listDiskOfferings'
        },
        zones: {
          list: 'listZones'
        },
        affinityGroups: {
          list: 'listAffinityGroups'
        },
        sshKeyPairs: {
          list: 'listSSHKeyPairs'
        },
        networks: {
          list: 'listNetworks',
          options: {
            zoneid: _.get(this.zone, 'id'),
            canusefordeploy: true,
            projectid: store.getters.project.id
          }
        }
      }
    },
    networkOfferingIds () {
      return _.map(this.networks, 'id')
    },
    zoneSelectOptions () {
      return this.options.zones.map((zone) => {
        return {
          label: zone.name,
          value: zone.id
        }
      })
    }
  },
  watch: {
    instanceConfig (instanceConfig) {
      this.template = _.find(this.options.templates, (option) => option.id === instanceConfig.templateid)
      this.iso = _.find(this.options.isos, (option) => option.id === instanceConfig.isoid)
      this.serviceOffering = _.find(this.options.serviceOfferings, (option) => option.id === instanceConfig.computeofferingid)
      this.diskOffering = _.find(this.options.diskOfferings, (option) => option.id === instanceConfig.diskofferingid)
      this.zone = _.find(this.options.zones, (option) => option.id === instanceConfig.zoneid)
      this.affinityGroups = _.filter(this.options.affinityGroups, (option) => _.includes(instanceConfig.affinitygroupids, option.id))
      this.networks = _.filter(this.options.networks, (option) => _.includes(instanceConfig.networkids, option.id))
      this.sshKeyPair = _.find(this.options.sshKeyPairs, (option) => option.name === instanceConfig.keypair)

      if (this.zone) {
        this.vm.zoneid = this.zone.id
        this.vm.zonename = this.zone.name
      }

      if (this.template) {
        this.vm.templateid = this.template.id
        this.vm.templatename = this.template.displaytext
        this.vm.ostypeid = this.template.ostypeid
        this.vm.ostypename = this.template.ostypename
      }

      if (this.iso) {
        this.vm.templateid = this.iso.id
        this.vm.templatename = this.iso.displaytext
        this.vm.ostypeid = this.iso.ostypeid
        this.vm.ostypename = this.iso.ostypename
      }

      if (this.serviceOffering) {
        this.vm.serviceofferingid = this.serviceOffering.id
        this.vm.serviceofferingname = this.serviceOffering.displaytext
        this.vm.cpunumber = this.serviceOffering.cpunumber
        this.vm.cpuspeed = this.serviceOffering.cpuspeed
        this.vm.memory = this.serviceOffering.memory
      }

      if (this.diskOffering) {
        this.vm.diskofferingid = this.diskOffering.id
        this.vm.diskofferingname = this.diskOffering.displaytext
        this.vm.diskofferingsize = this.diskOffering.disksize
      }

      if (this.affinityGroups) {
        this.vm.affinitygroup = this.affinityGroups
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onValuesChange: (props, fields) => {
        if (fields.isoid) {
          this.form.setFieldsValue({
            templateid: null,
            rootdisksize: 0
          })
        } else if (fields.templateid) {
          this.form.setFieldsValue({ isoid: null })
        }
        this.instanceConfig = { ...this.form.getFieldsValue(), ...fields }
        this.vm = this.instanceConfig
      }
    })
    this.form.getFieldDecorator('computeofferingid', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('diskofferingid', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('affinitygroupids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('isoid', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('networkids', { initialValue: [], preserve: true })
    this.form.getFieldDecorator('keypair', { initialValue: [], preserve: true })
  },
  created () {
    _.each(this.params, this.fetchOptions)
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
    updateDiskOffering (id) {
      this.form.setFieldsValue({
        diskofferingid: id
      })
    },
    updateAffinityGroups (ids) {
      this.form.setFieldsValue({
        affinitygroupids: ids
      })
    },
    updateNetworks (ids) {
      this.form.setFieldsValue({
        networkids: ids
      })
    },
    updateSshKeyPairs (name) {
      this.form.setFieldsValue({
        keypair: name
      })
    },
    getText (option) {
      return _.get(option, 'displaytext', _.get(option, 'name'))
    },
    handleSubmit () {
      console.log('wizard submit')
    },
    fetchOptions (param, name) {
      param.loading = true
      param.opts = []
      const options = param.options || {}
      options.listall = true
      api(param.list, options).then((response) => {
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
            this.options[name] = response
            this.$forceUpdate()
          })
        })
      }).catch(function (error) {
        console.log(error.stack)
        param.loading = false
      })
    },
    fetchIsos (isoFilter) {
      api('listIsos', {
        zoneid: _.get(this.zone, 'id'),
        isofilter: isoFilter,
        bootable: true
      }).then((response) => {
        const concatedIsos = _.concat(this.options.isos, _.get(response, 'listisosresponse.iso', []))
        this.options.isos = _.uniqWith(concatedIsos, _.isEqual)
        this.$forceUpdate()
      }).catch((reason) => {
        // ToDo: Handle errors
        console.log(reason)
      })
    },
    fetchAllIsos () {
      this.options.isos = []
      this.isoFilter.forEach((filter) => {
        this.fetchIsos(filter)
      })
    },
    onTemplatesIsosCollapseChange (key) {
      if (key === 'isos' && this.options.isos.length === 0) {
        this.fetchAllIsos()
      }
    },
    onSelectZoneId () {
      this.$nextTick(() => {
        if (this.options.isos.length !== 0) {
          this.fetchAllIsos()
        }
        this.fetchOptions(this.params.templates, 'templates')
        this.fetchOptions(this.params.networks, 'networks')
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .card-footer {
    text-align: right;
    margin-top: 2rem;

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

<style lang="less">
  @import url('../../style/index');

  .ant-table-selection-column {
    // Fix for the table header if the row selection use radio buttons instead of checkboxes
    > div:empty {
      width: 16px;
    }
  }

  .ant-collapse-borderless > .ant-collapse-item {
    border: 1px solid @border-color-split;
    border-radius: @border-radius-base !important;
    margin: 0 0 1.2rem;
  }
</style>
