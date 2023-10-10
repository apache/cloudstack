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
  <a-spin :spinning="fetchLoading">
    <a-button
      type="primary"
      style="width: 100%; margin-bottom: 10px"
      @click="showAddPhyNetModal"
      :loading="loading"
      :disabled="!('createPhysicalNetwork' in $store.getters.apis)">
      <template #icon><plus-outlined /></template> {{ $t('label.add.physical.network') }}
    </a-button>
    <a-list class="list">
      <a-list-item v-for="network in networks" :key="network.id" class="list__item">
        <div class="list__item-outer-container">
          <div class="list__item-container">
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.name') }}
              </div>
              <div>
                <router-link :to="{ path: '/physicalnetwork/' + network.id }">{{ network.name }}</router-link>
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('label.state') }}</div>
              <div><status :text="network.state" displayText></status></div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.isolationmethods') }}
              </div>
              <div>
                {{ network.isolationmethods }}
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.vlan') }}
              </div>
              <div>{{ network.vlan }}</div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.traffictype') }}
              </div>
              <div>
                {{ network.traffictype }}
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.broadcastdomainrange') }}
              </div>
              <div>{{ network.broadcastdomainrange }}</div>
            </div>
          </div>
        </div>
      </a-list-item>
    </a-list>
    <a-modal
      :visible="addPhyNetModal"
      :title="$t('label.add.physical.network')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeModals">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleAddPhyNet"
        v-ctrl-enter="handleAddPhyNet"
        layout="vertical"
        class="form"

      >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
          </template>
          <a-input v-model:value="form.name" :placeholder="apiParams.name.description" />
        </a-form-item>
        <a-form-item name="isolationmethods" ref="isolationmethods">
          <template #label>
            <tooltip-label :title="$t('label.isolationmethods')" :tooltip="apiParams.isolationmethods.description"/>
          </template>
          <a-select
            v-model:value="form.isolationmethods"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            v-focus="true"
            :placeholder="apiParams.isolationmethods.description">
            <a-select-option v-for="i in isolationMethods" :key="i" :value="i" :label="i">{{ i }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="vlan" ref="vlan">
          <template #label>
            <tooltip-label :title="$t('label.vlan')" :tooltip="apiParams.vlan.description"/>
          </template>
          <a-input v-model:value="form.vlan" :placeholder="apiParams.vlan.description" />
        </a-form-item>
        <a-form-item name="tags" ref="tags">
          <template #label>
            <tooltip-label :title="$t('label.tags')" :tooltip="apiParams.tags.description"/>
          </template>
          <a-select
            mode="tags"
            v-model:value="form.tags"
            :placeholder="apiParams.tags.description">
          </a-select>
        </a-form-item>
        <a-form-item name="networkspeed" ref="networkspeed">
          <template #label>
            <tooltip-label :title="$t('label.networkspeed')" :tooltip="apiParams.networkspeed.description"/>
          </template>
          <a-input v-model:value="form.networkspeed" :placeholder="apiParams.networkspeed.description" />
        </a-form-item>
        <a-form-item name="broadcastdomainrange" ref="broadcastdomainrange">
          <template #label>
            <tooltip-label :title="$t('label.broadcastdomainrange')" :tooltip="apiParams.broadcastdomainrange.description"/>
          </template>
          <a-input v-model:value="form.broadcastdomainrange" :placeholder="apiParams.broadcastdomainrange.description" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="closeModals">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddPhyNet">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'PhysicalNetworksTab',
  components: {
    Status,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      networks: [],
      fetchLoading: false,
      addPhyNetModal: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createPhysicalNetwork')
    console.log(this.apiParams)
  },
  created () {
    this.fetchData()
    this.initAddPhyNetForm()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  computed: {
    isolationMethods () {
      return ['VLAN', 'VXLAN', 'GRE', 'STT', 'BCF_SEGMENT', 'SSP', 'ODL', 'L3VPN', 'VCS']
    }
  },
  methods: {
    fetchData () {
      this.fetchLoading = true
      api('listPhysicalNetworks', { zoneid: this.resource.id }).then(json => {
        this.networks = json.listphysicalnetworksresponse.physicalnetwork || []
        this.fetchTrafficLabels()
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    fetchTrafficLabels () {
      const promises = []
      for (const network of this.networks) {
        promises.push(new Promise((resolve, reject) => {
          api('listTrafficTypes', { physicalnetworkid: network.id }).then(json => {
            if (json.listtraffictypesresponse.traffictype) {
              network.traffictype = json.listtraffictypesresponse.traffictype.filter(e => { return e.traffictype }).map(e => { return e.traffictype }).join(', ')
            }
            resolve()
          }).catch(error => {
            this.$notifyError(error)
            reject(error)
          })
        }))
      }
      Promise.all(promises).finally(() => {
        this.fetchLoading = false
      })
    },
    showAddPhyNetModal () {
      this.addPhyNetModal = true
    },
    initAddPhyNetForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        name: [{ required: true, message: this.$t('label.required') }]
      })
    },
    handleAddPhyNet () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        values.zoneid = this.resource.id
        if (values.tags) {
          values.tags = values.tags.join()
        }
        console.log(values)
        api('createPhysicalNetwork', values).then(response => {
          this.$pollJob({
            jobId: response.createphysicalnetworkresponse.jobid,
            successMessage: this.$t('message.success.add.physical.network'),
            successMethod: () => {
              this.fetchData()
              this.closeModals()
            },
            errorMessage: this.$t('message.add.physical.network.failed'),
            errorMethod: () => {
              this.fetchData()
              this.closeModals()
            },
            loadingMessage: this.$t('message.add.physical.network.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.closeModals()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        })
      })
    },
    closeModals () {
      this.addPhyNetModal = false
    }
  }
}
</script>

<style lang="scss" scoped>
.list {

  &__label {
    font-weight: bold;
  }

  &__col {
    flex: 1;

    @media (min-width: 480px) {
      &:not(:last-child) {
        margin-right: 20px;
      }
    }
  }

  &__item {
    margin-right: -8px;
    align-items: flex-start;

    &-outer-container {
      width: 100%;
    }

    &-container {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 480px) {
        flex-direction: row;
        margin-bottom: 10px;
      }
    }
  }
}
</style>
