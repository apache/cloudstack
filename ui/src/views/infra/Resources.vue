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
    <a-list>
      <a-list-item v-for="item in resourcesList" :key="item.id" class="list-item">
        <div class="list-item__container">
          <div class="list-item__data list-item__title">{{ returnCapacityTitle(item.type) }}</div>
          <div class="list-item__vals">
            <div class="list-item__data">
              Allocated:
              {{ convertByType(item.type, item.capacityused) }} / {{ convertByType(item.type, item.capacitytotal) }}
            </div>
            <a-progress
              status="normal"
              :percent="parseFloat(item.percentused)"
              :format="p => parseFloat(item.percentused).toFixed(2) + '%'" />
          </div>
        </div>
      </a-list-item>
    </a-list>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'Resources',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (this.resource && this.resource.id) {
        this.fetchData()
      }
    }
  },
  data () {
    return {
      fetchLoading: false,
      resourcesList: []
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      const entity = this.$route.meta.name + 'id'
      const params = {}
      params[entity] = this.resource.id
      this.fetchLoading = true
      api('listCapacity', params).then(response => {
        this.resourcesList = response.listcapacityresponse.capacity
        this.animatePercentVals()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    animatePercentVals () {
      this.resourcesList.forEach(resource => {
        const percent = resource.percentused
        resource.percentused = 0
        setTimeout(() => {
          resource.percentused = percent
        }, 200)
      })
    },
    convertBytes (val) {
      if (val < 1024 * 1024) return `${(val / 1024).toFixed(2)} KB`
      if (val < 1024 * 1024 * 1024) return `${(val / 1024 / 1024).toFixed(2)} MB`
      if (val < 1024 * 1024 * 1024 * 1024) return `${(val / 1024 / 1024 / 1024).toFixed(2)} GB`
      if (val < 1024 * 1024 * 1024 * 1024 * 1024) return `${(val / 1024 / 1024 / 1024 / 1024).toFixed(2)} TB`
      return val
    },
    convertHz (val) {
      if (val < 1000) return `${val} Mhz`
      return `${(val / 1000).toFixed(2)} GHz`
    },
    convertByType (type, val) {
      switch (type) {
        case 0: return this.convertBytes(val)
        case 1: return this.convertHz(val)
        case 2: return this.convertBytes(val)
        case 3: return this.convertBytes(val)
        case 6: return this.convertBytes(val)
        case 9: return this.convertBytes(val)
        case 11: return this.convertBytes(val)
        default: return val
      }
    },
    returnCapacityTitle (type) {
      switch (type) {
        case 0: return this.$t('label.memory')
        case 1: return this.$t('label.cpu')
        case 2: return this.$t('label.primary.storage.used')
        case 3: return this.$t('label.primary.storage.allocated')
        case 4: return this.$t('label.public.ips')
        case 5: return this.$t('label.management.ips')
        case 6: return this.$t('label.secondary.storage')
        case 7: return this.$t('label.vlan')
        case 8: return this.$t('label.direct.ips')
        case 9: return this.$t('label.local.storage')
        case 19: return this.$t('label.gpu')
        case 90: return this.$t('label.num.cpu.cores')
        default: return ''
      }
    }
  }
}
</script>

<style scoped lang="scss">
  .list-item {

    &__container {
      max-width: 90%;
      width: 100%;

      @media (min-width: 760px) {
        max-width: 95%;
      }
    }

    &__title {
      font-weight: bold;
    }

    &__data {
      margin-right: 20px;
      white-space: nowrap;
    }

    &__vals {
      @media (min-width: 760px) {
        display: flex;
      }
    }
  }
</style>
