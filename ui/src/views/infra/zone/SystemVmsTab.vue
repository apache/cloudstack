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
      <a-list-item v-for="vm in vms" :key="vm.id" class="list__item">
        <div class="list__item-outer-container">
          <div class="list__item-container">
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.name') }}
              </div>
              <div>
                <router-link :to="{ path: '/systemvm/' + vm.id }">{{ vm.name }}</router-link>
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('label.vmstate') }}</div>
              <div><status :text="vm.state" displayText></status></div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('label.agentstate') }}</div>
              <div><status :text="vm.agentstate || $t('label.unknown')" displayText></status></div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.type') }}
              </div>
              <div>
                {{ vm.systemvmtype == 'consoleproxy' ? $t('label.console.proxy.vm') : $t('label.secondary.storage.vm') }}
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.publicip') }}
              </div>
              <div>
                {{ vm.publicip }}
              </div>
            </div>
            <div class="list__col">
              <div class="list__label">
                {{ $t('label.hostname') }}
              </div>
              <div>
                <router-link :to="{ path: '/host/' + vm.hostid }">{{ vm.hostname }}</router-link>
              </div>
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
  name: 'SystemVmsTab',
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
      vms: [],
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
      api('listSystemVms', { zoneid: this.resource.id }).then(json => {
        this.vms = json.listsystemvmsresponse.systemvm || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
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
