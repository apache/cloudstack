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
  <a-list
    size="small"
    :loading="loading"
    :dataSource="usageList" >
    <a-list-item slot="renderItem" slot-scope="item" class="list-item" v-if="!($route.meta.name === 'project' && item === 'project')">
      <div class="list-item__container">
        <strong>
          {{ $t('label.' + item + 'limit') }}
        </strong>
        ({{ resource[item + 'available'] === '-1' ? $t('label.unlimited') : resource[item + 'available'] }} {{ $t('label.available') }})
        <div class="list-item__vals">
          <div class="list-item__data">
            {{ $t('label.used') }} / {{ $t('label.limit') }} : {{ resource[item + 'total'] }} / {{ resource[item + 'limit'] === '-1' ? $t('label.unlimited') : resource[item + 'limit'] }}
          </div>
          <a-progress
            status="normal"
            :percent="parseFloat(getPercentUsed(resource[item + 'total'], resource[item + 'limit']))"
            :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''" />
        </div>
      </div>
    </a-list-item>
  </a-list>
</template>

<script>
export default {
  name: 'ResourceCountUsageTab',
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
      usageList: [
        'vm', 'cpu', 'memory', 'primarystorage', 'volume', 'ip', 'network',
        'vpc', 'secondarystorage', 'snapshot', 'template', 'project'
      ]
    }
  },
  watch: {
    resource (newData, oldData) {
      if (!newData || !newData.id) {
        return
      }
      this.resource = newData
    }
  },
  methods: {
    getPercentUsed (total, limit) {
      return (limit === 'Unlimited') ? 0 : (total / limit) * 100
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
      margin-top: 10px;
      @media (min-width: 760px) {
        display: flex;
      }
    }
  }
</style>
