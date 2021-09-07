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
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'PhysicalNetworksTab',
  components: {
    Status
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
      fetchLoading: false
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.fetchData()
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
            network.traffictype = json.listtraffictypesresponse.traffictype.filter(e => { return e.traffictype }).map(e => { return e.traffictype }).join(', ')
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
