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
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input
            v-model:value="form.name"
            :placeholder="apiParams.name.description"
            v-focus="true" />
        </a-form-item>
        <a-form-item name="description" ref="description">
          <template #label>
            <tooltip-label :title="$t('label.description')" :tooltip="apiParams.description.description"/>
          </template>
          <a-input
            v-model:value="form.description"
            :placeholder="apiParams.description.description"/>
        </a-form-item>
        <a-form-item name="zoneid" ref="zoneid">
          <template #label>
            <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
          </template>
          <a-select
            id="zone-selection"
            v-model:value="form.zoneid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="zoneLoading"
            :placeholder="apiParams.zoneid.description"
            @change="val => { handleZoneChange(this.zones[val]) }">
            <a-select-option v-for="(opt, optIndex) in zones" :key="optIndex" :label="opt.name || opt.description">
              <span>
                <resource-icon v-if="opt.icon" :image="opt.icon.base64image" size="1x" style="margin-right: 5px"/>
                <global-outlined v-else style="margin-right: 5px" />
                {{ opt.name || opt.description }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item ref="hypervisor" name="hypervisor">
          <template #label>
            <tooltip-label :title="$t('label.hypervisor')" :tooltip="apiParams.hypervisor.description"/>
          </template>
          <a-select
            v-model:value="form.hypervisor"
            :loading="hypervisorLoading"
            :placeholder="apiParams.hypervisor.description"
            showSearch
            optionFilterProp="label"
            @change="val => { handleZoneHypervisorChange(val) }">
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="(opt, optIndex) in selectedZoneHypervisors" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="kubernetesversionid" ref="kubernetesversionid">
          <template #label>
            <tooltip-label :title="$t('label.kubernetesversionid')" :tooltip="apiParams.kubernetesversionid.description"/>
          </template>
          <a-select
            id="version-selection"
            v-model:value="form.kubernetesversionid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="kubernetesVersionLoading"
            :placeholder="apiParams.kubernetesversionid.description"
            @change="val => { handleKubernetesVersionChange(kubernetesVersions[val]) }">
            <a-select-option v-for="(opt, optIndex) in kubernetesVersions" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="serviceofferingid" ref="serviceofferingid">
          <template #label>
            <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="apiParams.serviceofferingid.description"/>
          </template>
          <a-select
            id="offering-selection"
            v-model:value="form.serviceofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="apiParams.serviceofferingid.description">
            <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="noderootdisksize" ref="noderootdisksize">
          <template #label>
            <tooltip-label :title="$t('label.noderootdisksize')" :tooltip="apiParams.noderootdisksize.description"/>
          </template>
          <a-input
            v-model:value="form.noderootdisksize"
            :placeholder="apiParams.noderootdisksize.description"/>
        </a-form-item>
        <a-form-item name="networkid" ref="networkid">
          <template #label>
            <tooltip-label :title="$t('label.networkid')" :tooltip="apiParams.networkid.description"/>
          </template>
          <a-select
            id="network-selection"
            v-model:value="form.networkid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="networkLoading"
            :placeholder="apiParams.networkid.description">
            <a-select-option v-for="(opt, optIndex) in networks" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="haenable" ref="haenable" v-if="selectedKubernetesVersion != null && selectedKubernetesVersion !== undefined && selectedKubernetesVersion.supportsha === true">
          <template #label>
            <tooltip-label :title="$t('label.haenable')" :tooltip="apiParams.haenable?.description || ''"/>
          </template>
          <a-switch v-model:checked="form.haenable" />
        </a-form-item>
        <a-form-item v-if="form.haenable" name="controlnodes" ref="controlnodes">
          <template #label>
            <tooltip-label :title="$t('label.controlnodes')" :tooltip="apiParams.controlnodes.description"/>
          </template>
          <a-input
            v-model:value="form.controlnodes"
            :placeholder="apiParams.controlnodes.description"/>
        </a-form-item>
        <a-form-item v-if="form.haenable && !selectedZone.isnsxenabled" name="externalloadbalanceripaddress" ref="externalloadbalanceripaddress">
          <template #label>
            <tooltip-label :title="$t('label.externalloadbalanceripaddress')" :tooltip="apiParams.externalloadbalanceripaddress.description"/>
          </template>
          <a-input
            v-model:value="form.externalloadbalanceripaddress"
            :placeholder="apiParams.externalloadbalanceripaddress.description"/>
        </a-form-item>
        <a-form-item name="size" ref="size">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.size')" :tooltip="apiParams.size.description"/>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
        <a-form-item name="keypair" ref="keypair">
          <template #label>
            <tooltip-label :title="$t('label.keypair')" :tooltip="apiParams.keypair.description"/>
          </template>
          <a-select
            id="keypair-selection"
            v-model:value="form.keypair"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="keyPairLoading"
            :placeholder="apiParams.keypair.description">
            <a-select-option v-for="(opt, optIndex) in keyPairs" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- Advanced configurations -->
        <a-form-item name="advancedmode" ref="advancedmode">
          <template #label>
            <tooltip-label :title="$t('label.isadvanced')" />
          </template>
          <a-switch v-model:checked="form.advancedmode" />
        </a-form-item>
        <a-form-item v-if="form.advancedmode" name="enablecsi" ref="enablecsi" :label="$t('label.enable.csi')">
            <template #label>
              <tooltip-label :title="$t('label.enable.csi')" :tooltip="apiParams.enablecsi.description"/>
            </template>
            <a-switch v-model:checked="form.enablecsi" />
          </a-form-item>
        <a-form-item v-if="form.advancedmode" name="controlofferingid" ref="controlofferingid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.control.nodes.offeringid')" :tooltip="$t('label.cks.cluster.control.nodes.offeringid')"/>
          </template>
          <a-select
            id="control-offering-selection"
            v-model:value="form.controlofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.control.nodes.offeringid')">
            <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode" name="controltemplateid" ref="controltemplateid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.control.nodes.templateid')" :tooltip="$t('label.cks.cluster.control.nodes.templateid')"/>
          </template>
          <a-select
            id="control-template-selection"
            v-model:value="form.controltemplateid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.control.nodes.templateid')">
            <a-select-option v-for="(opt, optIndex) in templates" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode" name="workerofferingid" ref="workerofferingid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.worker.nodes.offeringid')" :tooltip="$t('label.cks.cluster.worker.nodes.offeringid')"/>
          </template>
          <a-select
            id="worker-offering-selection"
            v-model:value="form.workerofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.worker.nodes.offeringid')">
            <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode" name="workertemplateid" ref="workertemplateid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.worker.nodes.templateid')" :tooltip="$t('label.cks.cluster.worker.nodes.templateid')"/>
          </template>
          <a-select
            id="worker-template-selection"
            v-model:value="form.workertemplateid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.worker.nodes.templateid')">
            <a-select-option v-for="(opt, optIndex) in templates" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode" name="etcdnodes" ref="etcdnodes">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.etcd.nodes')" :tooltip="apiParams.etcdnodes.description"/>
          </template>
          <a-input
            v-model:value="form.etcdnodes"
            :placeholder="apiParams.etcdnodes.description"/>
        </a-form-item>
        <a-form-item v-if="form.advancedmode && form.etcdnodes && form.etcdnodes > 0" name="etcdofferingid" ref="etcdofferingid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.etcd.nodes.offeringid')" :tooltip="$t('label.cks.cluster.etcd.nodes.offeringid')"/>
          </template>
          <a-select
            id="etcd-offering-selection"
            v-model:value="form.etcdofferingid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.etcd.nodes.offeringid')">
            <a-select-option v-for="(opt, optIndex) in serviceOfferings" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode && form?.etcdnodes > 0" name="etcdtemplateid" ref="etcdtemplateid">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.etcd.nodes.templateid')" :tooltip="$t('label.cks.cluster.etcd.nodes.templateid')"/>
          </template>
          <a-select
            id="etcd-template-selection"
            v-model:value="form.etcdtemplateid"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :loading="serviceOfferingLoading"
            :placeholder="$t('label.cks.cluster.etcd.nodes.templateid')">
            <a-select-option v-for="(opt, optIndex) in templates" :key="optIndex" :label="opt.name || opt.description">
              {{ opt.name || opt.description }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.advancedmode && isASNumberRequired() && !form.networkid" name="asnumber" ref="asnumber">
          <template #label>
              <tooltip-label :title="$t('label.asnumber')" :tooltip="apiParams.asnumber.description"/>
            </template>
            <a-select
             v-model:value="form.asnumber"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="asNumberLoading"
              :placeholder="apiParams.asnumber.description"
              @change="val => { handleASNumberChange(val) }">
              <a-select-option v-for="(opt, optIndex) in asNumbersZone" :key="optIndex" :label="opt.asnumber">
                {{ opt.asnumber }}
              </a-select-option>
            </a-select>
        </a-form-item>
        <a-form-item  v-if="form.advancedmode" name="cniconfigurationid" ref="cniconfigurationid">
          <template #label>
            <tooltip-label :title="$t('label.cniconfiguration')" :tooltip="$t('label.cniconfiguration')"/>
          </template>
            <user-data-selection
              :items="cniConfigurationData"
              :row-count="cniConfigurationData.length"
              :zoneId="zoneId"
              :loading="cniConfigLoading"
              :preFillContent="dataPreFill"
              :showSearch="false"
              @select-user-data-item="($event) => updateCniConfig($event)"
              @handle-search-filter="($event) => handleSearchFilter('userData', $event)"
            />
            <div v-if="cniConfigParams.length > 0">
              <a-input-group>
                <a-table
                  size="small"
                  style="overflow-y: auto"
                  :columns="userDataParamCols"
                  :dataSource="cniConfigParams"
                  :pagination="false"
                  :rowKey="record => record.key">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'value'">
                      <div v-if="record.key === 'AS_NUMBER'">
                        <a-input style="width: 100%;text-wrap: wrap;" :disabled="true" value="Value is obtained from the network or is automatically obtained at the time of cluster creation" />
                      </div>
                      <a-input v-else v-model:value="cniConfigValues[record.key]" />
                    </template>
                  </template>
                </a-table>
              </a-input-group>
            </div>
        </a-form-item>

        <!-- Experimentation Features -->
        <div v-if="$store.getters.features.kubernetesclusterexperimentalfeaturesenabled">
          <a-form-item name="privateregistry" ref="privateregistry" :label="$t('label.private.registry')">
            <template #label>
              <tooltip-label :title="$t('label.private.registry')" :tooltip="apiParams.keprivateregistryypair.description"/>
            </template>
            <a-switch v-model:checked="form.privateregistry" />
          </a-form-item>
          <div v-if="form.privateregistry">
            <a-form-item name="dockerregistryusername" ref="dockerregistryusername">
              <template #label>
                <tooltip-label :title="$t('label.username')" :tooltip="apiParams.dockerregistryusername.description"/>
              </template>
              <a-input
                v-model:value="form.dockerregistryusername"
                :placeholder="apiParams.dockerregistryusername.description"/>
            </a-form-item>
            <a-form-item name="dockerregistrypassword" ref="dockerregistrypassword">
              <template #label>
                <tooltip-label :title="$t('label.password')" :tooltip="apiParams.dockerregistrypassword.description"/>
              </template>
              <a-input-password
                v-model:value="form.dockerregistrypassword"
                :placeholder="apiParams.dockerregistrypassword.description"/>
            </a-form-item>
            <a-form-item name="dockerregistryurl" ref="dockerregistryurl">
              <template #label>
                <tooltip-label :title="$t('label.url')" :tooltip="apiParams.dockerregistryurl.description"/>
              </template>
              <a-input
                v-model:value="form.dockerregistryurl"
                :placeholder="apiParams.dockerregistryurl.description"/>
            </a-form-item>
          </div>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import UserDataSelection from '@views/compute/wizard/UserDataSelection'

export default {
  name: 'CreateKubernetesCluster',
  mixins: [mixinForm],
  components: {
    TooltipLabel,
    ResourceIcon,
    UserDataSelection
  },
  props: {},
  data () {
    return {
      zones: [],
      zoneLoading: false,
      selectedZone: {},
      kubernetesVersions: [],
      kubernetesVersionLoading: false,
      selectedKubernetesVersion: {},
      serviceOfferings: [],
      serviceOfferingLoading: false,
      networks: [],
      networkLoading: false,
      keyPairs: [],
      keyPairLoading: false,
      loading: false,
      templates: [],
      templateLoading: false,
      selectedZoneHypervisors: [],
      hypervisorLoading: false,
      configLoading: false,
      cniConfigurationData: [],
      cniConfigLoading: false,
      cniConfigParams: [],
      cniConfigValues: {},
      userDataParamCols: [
        {
          title: this.$t('label.key'),
          dataIndex: 'key'
        },
        {
          title: this.$t('label.value'),
          dataIndex: 'value',
          key: 'value'
        }
      ],
      cksNetworkOfferingName: null,
      cksNetworkOffering: null,
      asNumbersZone: [],
      asNumberLoading: false,
      selectedAsNumber: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createKubernetesCluster')
  },
  created () {
    this.emptyEntry = {
      id: null,
      name: ''
    }
    this.networks = [this.emptyEntry]
    this.keyPairs = [this.emptyEntry]
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        controlnodes: 3,
        size: 1,
        noderootdisksize: 8,
        hypervisor: null
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('message.error.kubecluster.name') }],
        zoneid: [{ required: true, message: this.$t('message.error.zone.for.cluster') }],
        kubernetesversionid: [{ required: true, message: this.$t('message.error.version.for.cluster') }],
        serviceofferingid: [{ required: true, message: this.$t('message.error.serviceoffering.for.cluster') }],
        noderootdisksize: [
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value < 8)) {
                return Promise.reject(this.$t('message.validate.min').replace('{0}', '8GB'))
              }
              return Promise.resolve()
            }
          }
        ],
        size: [
          {
            required: true,
            message: this.$t('message.error.size.for.cluster')
          },
          { type: 'number', validator: this.validateNumber }
        ],
        dockerregistryusername: [{ required: true, message: this.$t('label.required') }],
        dockerregistrypassword: [{ required: true, message: this.$t('label.required') }],
        dockerregistryurl: [{ required: true, message: this.$t('label.required') }],
        controlnodes: [
          { required: true, message: this.$t('message.error.input.value') },
          {
            validator: async (rule, value) => {
              if (value && (isNaN(value) || value < 2)) {
                return Promise.reject(this.$t('message.validate.number'))
              }
              return Promise.resolve()
            }
          }
        ]
      })
    },
    fetchData () {
      this.fetchZoneData()
      this.fetchKeyPairData()
      this.fetchCksTemplates()
      this.fetchCKSNetworkOfferingName()
      this.fetchCniConfigurations()
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    isASNumberRequired () {
      return !this.isObjectEmpty(this.cksNetworkOffering) && this.cksNetworkOffering.specifyasnumber && this.cksNetworkOffering.routingmode && this.cksNetworkOffering.routingmode.toLowerCase() === 'dynamic'
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    isUserAllowedToListCniConfig () {
      console.log(Boolean('listCniConfiguration' in this.$store.getters.apis))
      return Boolean('listCniConfiguration' in this.$store.getters.apis)
    },
    fetchZoneData () {
      const params = {}
      this.zoneLoading = true
      params.showicon = true
      getAPI('listZones', params).then(json => {
        var listZones = json.listzonesresponse.zone
        if (listZones) {
          listZones = listZones.filter(x => x.allocationstate === 'Enabled')
          this.zones = this.zones.concat(listZones)
        }
      }).finally(() => {
        this.zoneLoading = false
        if (this.arrayHasItems(this.zones)) {
          this.form.zoneid = 0
          this.handleZoneChange(this.zones[0])
        }
      })
    },
    handleZoneChange (zone) {
      this.selectedZone = zone
      this.fetchKubernetesVersionData()
      this.fetchNetworkData()
      this.fetchZoneHypervisors()
      this.fetchZoneASNumbers()
    },
    handleASNumberChange (selectedIndex) {
      this.selectedAsNumber = this.asNumbersZone[selectedIndex].asnumber
      this.form.asnumber = this.selectedAsNumber
    },
    fetchKubernetesVersionData () {
      this.kubernetesVersions = []
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone)) {
        params.zoneid = this.selectedZone.id
      }
      this.kubernetesVersionLoading = true
      getAPI('listKubernetesSupportedVersions', params).then(json => {
        const versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion
        if (this.arrayHasItems(versionObjs)) {
          for (var i = 0; i < versionObjs.length; i++) {
            if (versionObjs[i].state === 'Enabled' && versionObjs[i].isostate === 'Ready') {
              this.kubernetesVersions.push(versionObjs[i])
            }
          }
        }
      }).finally(() => {
        this.kubernetesVersionLoading = false
        if (this.arrayHasItems(this.kubernetesVersions)) {
          this.form.kubernetesversionid = 0
          this.handleKubernetesVersionChange(this.kubernetesVersions[0])
        }
      })
    },
    handleKubernetesVersionChange (version) {
      this.selectedKubernetesVersion = version
      this.fetchServiceOfferingData()
    },
    fetchServiceOfferingData () {
      this.serviceOfferings = []
      const params = {}
      this.serviceOfferingLoading = true
      getAPI('listServiceOfferings', params).then(json => {
        var items = json.listserviceofferingsresponse.serviceoffering
        var minCpu = 2
        var minMemory = 2048
        if (!this.isObjectEmpty(this.selectedKubernetesVersion)) {
          minCpu = this.selectedKubernetesVersion.mincpunumber
          minMemory = this.selectedKubernetesVersion.minmemory
        }
        if (items != null) {
          for (var i = 0; i < items.length; i++) {
            if (items[i].iscustomized === false &&
                items[i].cpunumber >= minCpu && items[i].memory >= minMemory) {
              this.serviceOfferings.push(items[i])
            }
          }
        }
      }).finally(() => {
        this.serviceOfferingLoading = false
        if (this.arrayHasItems(this.serviceOfferings)) {
          this.form.serviceofferingid = 0
          this.form.controlofferingid = undefined
          this.form.workerofferingid = undefined
          this.form.etcdofferingid = undefined
        }
      })
    },
    isAdminOrDomainAdmin () {
      return ['Admin', 'DomainAdmin'].includes(this.$store.getters.userInfo.roletype)
    },
    fetchCksTemplates () {
      var filters = []
      if (this.isAdminOrDomainAdmin()) {
        filters = ['all']
      } else {
        filters = ['self', 'featured', 'community']
      }
      var ckstemplates = []
      for (const filtername of filters) {
        const params = {
          templatefilter: filtername,
          forcks: true,
          isready: true
        }
        this.templateLoading = true
        getAPI('listTemplates', params).then(json => {
          var templates = json?.listtemplatesresponse?.template || []
          ckstemplates.push(...templates)
        }).finally(() => {
          this.templateLoading = false
        })
      }
      this.templates = ckstemplates
    },
    fetchNetworkData () {
      const params = {}
      if (!this.isObjectEmpty(this.selectedZone)) {
        params.zoneid = this.selectedZone.id
      }
      this.networkLoading = true
      this.networks = []
      getAPI('listNetworks', params).then(json => {
        var listNetworks = json.listnetworksresponse.network
        if (this.arrayHasItems(listNetworks)) {
          listNetworks = listNetworks.filter(n => n.type !== 'L2')
          this.networks = listNetworks
        }
      }).finally(() => {
        this.networkLoading = false
        this.networks = [this.emptyEntry].concat(this.networks)
        if (this.arrayHasItems(this.networks)) {
          this.form.networkid = 0
        }
      })
    },
    fetchKeyPairData () {
      const params = {}
      this.keyPairLoading = true
      getAPI('listSSHKeyPairs', params).then(json => {
        const listKeyPairs = json.listsshkeypairsresponse.sshkeypair
        if (this.arrayHasItems(listKeyPairs)) {
          for (var i = 0; i < listKeyPairs.length; i++) {
            this.keyPairs.push({
              id: listKeyPairs[i].name,
              description: listKeyPairs[i].name
            })
          }
        }
      }).finally(() => {
        this.keyPairLoading = false
        if (this.arrayHasItems(this.keyPairs)) {
          this.form.keypair = 0
        }
      })
    },
    fetchZoneHypervisors () {
      const params = {
        zoneid: this.selectedZone.id
      }
      this.hypervisorLoading = true

      getAPI('listHypervisors', params).then(json => {
        const listResponse = json.listhypervisorsresponse.hypervisor || []
        this.selectedZoneHypervisors = listResponse.filter(hypervisor => hypervisor.name !== 'External')
      }).finally(() => {
        this.hypervisorLoading = false
      })
    },
    handleZoneHypervisorChange (index) {
      this.form.hypervisor = index
    },
    fetchCKSNetworkOfferingName () {
      const params = {
        name: 'cloud.kubernetes.cluster.network.offering'
      }
      this.configLoading = true
      getAPI('listConfigurations', params).then(json => {
        if (json.listconfigurationsresponse.configuration !== null) {
          const config = json.listconfigurationsresponse.configuration[0]
          if (config && config.name === params.name) {
            this.cksNetworkOfferingName = config.value
          }
        }
      }).then(() => {
        this.fetchCKSNetworkOffering(this.cksNetworkOfferingName)
      }).finally(() => {
        this.configLoading = false
      })
    },
    fetchCKSNetworkOffering (offeringName) {
      return new Promise((resolve, reject) => {
        const args = {
          name: offeringName
        }

        getAPI('listNetworkOfferings', args).then(json => {
          const listNetworkOfferings = json.listnetworkofferingsresponse.networkoffering || []
          resolve(listNetworkOfferings)
          this.cksNetworkOffering = listNetworkOfferings[0] || {}
        }).catch(error => {
          resolve(error)
        })
      })
    },
    fetchZoneASNumbers () {
      const params = {}
      params.zoneid = this.selectedZone.id
      params.isallocated = false
      getAPI('listASNumbers', params).then(json => {
        this.asNumbersZone = json.listasnumbersresponse.asnumber
      })
    },
    fetchCniConfigurations () {
      this.cniConfigLoading = true
      getAPI('listCniConfiguration', {}).then(
        response => {
          const listResponse = response.listcniconfigurationresponse.cniconfig || []
          if (listResponse) {
            this.cniConfigurationData = listResponse
          }
        }).finally(() => {
        this.cniConfigLoading = false
      })
    },
    updateCniConfig (id) {
      if (id === '0') {
        this.form.cniconfigurationid = undefined
        return
      }
      this.form.cniconfigurationid = id
      this.cniConfigParams = []
      getAPI('listCniConfiguration', { id: id }).then(json => {
        const resp = json?.listcniconfigurationresponse?.cniconfig || []
        if (resp) {
          var params = resp[0].params
          if (params) {
            var dataParams = params.split(',')
          }
          var that = this
          dataParams.forEach(function (val, index) {
            that.cniConfigParams.push({
              id: index,
              key: val
            })
          })
        }
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.loading = true
        const params = {
          name: values.name,
          description: values.description,
          zoneid: this.zones[values.zoneid].id,
          kubernetesversionid: this.kubernetesVersions[values.kubernetesversionid].id,
          serviceofferingid: this.serviceOfferings[values.serviceofferingid].id,
          size: values.size,
          clustertype: 'CloudManaged'
        }
        if (values.hypervisor !== null) {
          params.hypervisor = this.selectedZoneHypervisors[values.hypervisor].name.toLowerCase()
        }
        var advancedOfferings = 0
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'controlofferingid') && this.arrayHasItems(this.serviceOfferings) && this.serviceOfferings[values.controlofferingid].id != null) {
          params['nodeofferings[' + advancedOfferings + '].node'] = 'control'
          params['nodeofferings[' + advancedOfferings + '].offering'] = this.serviceOfferings[values.controlofferingid].id
          advancedOfferings++
        }
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'workerofferingid') && this.arrayHasItems(this.serviceOfferings) && this.serviceOfferings[values.workerofferingid].id != null) {
          params['nodeofferings[' + advancedOfferings + '].node'] = 'worker'
          params['nodeofferings[' + advancedOfferings + '].offering'] = this.serviceOfferings[values.workerofferingid].id
          advancedOfferings++
        }
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'etcdnodes') && values.etcdnodes > 0) {
          params.etcdnodes = values.etcdnodes
          if (this.isValidValueForKey(values, 'etcdofferingid') && this.arrayHasItems(this.serviceOfferings) && this.serviceOfferings[values.etcdofferingid].id != null) {
            params['nodeofferings[' + advancedOfferings + '].node'] = 'etcd'
            params['nodeofferings[' + advancedOfferings + '].offering'] = this.serviceOfferings[values.etcdofferingid].id
            advancedOfferings++
          }
        }
        var advancedTemplates = 0
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'controltemplateid') && this.arrayHasItems(this.templates) && this.templates[values.controltemplateid].id != null) {
          params['nodetemplates[' + advancedTemplates + '].node'] = 'control'
          params['nodetemplates[' + advancedTemplates + '].template'] = this.templates[values.controltemplateid].id
          advancedTemplates++
        }
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'workertemplateid') && this.arrayHasItems(this.templates) && this.templates[values.workertemplateid].id != null) {
          params['nodetemplates[' + advancedTemplates + '].node'] = 'worker'
          params['nodetemplates[' + advancedTemplates + '].template'] = this.templates[values.workertemplateid].id
          advancedTemplates++
        }
        if (this.isValidValueForKey(values, 'advancedmode') && values.advancedmode && this.isValidValueForKey(values, 'etcdnodes') && values.etcdnodes > 0) {
          params.etcdnodes = values.etcdnodes
          if (this.isValidValueForKey(values, 'etcdtemplateid') && this.arrayHasItems(this.templates) && this.templates[values.etcdtemplateid].id != null) {
            params['nodetemplates[' + advancedTemplates + '].node'] = 'etcd'
            params['nodetemplates[' + advancedTemplates + '].template'] = this.templates[values.etcdtemplateid].id
            advancedTemplates++
          }
        }
        if (this.isValidValueForKey(values, 'noderootdisksize') && values.noderootdisksize > 0) {
          params.noderootdisksize = values.noderootdisksize
        }
        if (this.isValidValueForKey(values, 'haenable') && values.haenable &&
          this.isValidValueForKey(values, 'controlnodes') && values.controlnodes > 0) {
          params.controlnodes = values.controlnodes
        }
        if (this.isValidValueForKey(values, 'externalloadbalanceripaddress') && values.externalloadbalanceripaddress !== '') {
          params.externalloadbalanceripaddress = values.externalloadbalanceripaddress
        }
        if (this.isValidValueForKey(values, 'networkid') && this.arrayHasItems(this.networks) && this.networks[values.networkid].id != null) {
          params.networkid = this.networks[values.networkid].id
        }
        if (this.isValidValueForKey(values, 'keypair') && this.arrayHasItems(this.keyPairs) && this.keyPairs[values.keypair].id != null) {
          params.keypair = this.keyPairs[values.keypair].id
        }
        if (values.privateregistry) {
          params.dockerregistryusername = values.dockerregistryusername
          params.dockerregistrypassword = values.dockerregistrypassword
          params.dockerregistryurl = values.dockerregistryurl
        }

        if (values.cniconfigurationid) {
          params.cniconfigurationid = values.cniconfigurationid
        }

        if (values.enablecsi) {
          params.enablecsi = values.enablecsi
        }

        var idx = 0
        if (this.cniConfigValues) {
          for (const [key, value] of Object.entries(this.cniConfigValues)) {
            params['cniconfigdetails[' + idx + '].' + `${key}`] = value
            idx++
          }
        }
        if ('asnumber' in values && this.isASNumberRequired()) {
          params.asnumber = values.asnumber
        }

        postAPI('createKubernetesCluster', params).then(json => {
          const jobId = json.createkubernetesclusterresponse.jobid
          this.$pollJob({
            jobId,
            title: this.$t('label.kubernetes.cluster.create'),
            description: values.name,
            loadingMessage: `${this.$t('label.kubernetes.cluster.create')} ${values.name} ${this.$t('label.in.progress')}`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: this.$t('message.success.create.kubernetes.cluster') + ' ' + values.name
          })
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    async validateNumber (rule, value) {
      if (value && (isNaN(value) || value <= 0)) {
        return Promise.reject(this.$t('message.validate.number'))
      }
      return Promise.resolve()
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
