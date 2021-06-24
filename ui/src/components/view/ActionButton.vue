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
  <span class="row-action-button">
    <a-tooltip arrowPointAtCenter placement="bottomRight" v-if="resource && resource.id && dataView">
      <template slot="title">
        {{ $t('label.view.console') }}
      </template>
      <console :resource="resource" :size="size" />
    </a-tooltip>
    <a-tooltip
      v-for="(action, actionIndex) in actions"
      :key="actionIndex"
      arrowPointAtCenter
      placement="bottomRight">
      <template slot="title">
        {{ $t(action.label) }}
      </template>
      <a-badge
        class="button-action-badge"
        :overflowCount="9"
        :count="actionBadge[action.api] ? actionBadge[action.api].badgeNum : 0"
        v-if="action.api in $store.getters.apis &&
          action.showBadge && (
            (!dataView && ((action.listView && ('show' in action ? action.show(resource, $store.getters) : true)) || (action.groupAction && selectedRowKeys.length > 0 && ('groupShow' in action ? action.show(resource, $store.getters) : true)))) ||
            (dataView && action.dataView && ('show' in action ? action.show(resource, $store.getters) : true))
          )"
        :disabled="'disabled' in action ? action.disabled(resource, $store.getters) : false" >
        <a-button
          :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
          :shape="!dataView && action.icon === 'plus' ? 'round' : 'circle'"
          style="margin-left: 5px"
          :size="size"
          @click="execAction(action)">
          <span v-if="!dataView && action.icon === 'plus'">
            {{ $t(action.label) }}
          </span>
          <a-icon v-if="(typeof action.icon === 'string')" :type="action.icon" />
          <font-awesome-icon v-else :icon="action.icon" />
        </a-button>
      </a-badge>
      <a-button
        v-if="action.api in $store.getters.apis &&
          !action.showBadge && (
            (!dataView && ((action.listView && ('show' in action ? action.show(resource, $store.getters) : true)) || (action.groupAction && selectedRowKeys.length > 0 && ('groupShow' in action ? action.show(resource, $store.getters) : true)))) ||
            (dataView && action.dataView && ('show' in action ? action.show(resource, $store.getters) : true))
          )"
        :disabled="'disabled' in action ? action.disabled(resource, $store.getters) : false"
        :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
        :shape="!dataView && ['plus', 'user-add'].includes(action.icon) ? 'round' : 'circle'"
        style="margin-left: 5px"
        :size="size"
        @click="execAction(action)">
        <span v-if="!dataView && ['plus', 'user-add'].includes(action.icon)">
          {{ $t(action.label) }}
        </span>
        <a-icon v-if="(typeof action.icon === 'string')" :type="action.icon" />
        <font-awesome-icon v-else :icon="action.icon" />
      </a-button>
    </a-tooltip>
  </span>
</template>

<script>
import { api } from '@/api'
import Console from '@/components/widgets/Console'

export default {
  name: 'ActionButton',
  components: {
    Console
  },
  data () {
    return {
      actionBadge: {}
    }
  },
  mounted () {
    this.handleShowBadge()
  },
  props: {
    actions: {
      type: Array,
      default () {
        return []
      }
    },
    resource: {
      type: Object,
      default () {
        return {}
      }
    },
    dataView: {
      type: Boolean,
      default: false
    },
    selectedRowKeys: {
      type: Array,
      default () {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: 'default'
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.handleShowBadge()
    }
  },
  methods: {
    execAction (action) {
      action.resource = this.resource
      if (action.docHelp) {
        action.docHelp = this.$applyDocHelpMappings(action.docHelp)
      }
      this.$emit('exec-action', action)
    },
    handleShowBadge () {
      this.actionBadge = {}
      const arrAsync = []
      const actionBadge = this.actions.filter(action => action.showBadge === true)

      if (actionBadge && actionBadge.length > 0) {
        const dataLength = actionBadge.length

        for (let i = 0; i < dataLength; i++) {
          const action = actionBadge[i]

          arrAsync.push(new Promise((resolve, reject) => {
            api(action.api, action.param).then(json => {
              let responseJsonName
              const response = {}

              response.api = action.api
              response.count = 0

              for (const key in json) {
                if (key.includes('response')) {
                  responseJsonName = key
                  break
                }
              }

              if (json[responseJsonName] && json[responseJsonName].count && json[responseJsonName].count > 0) {
                response.count = json[responseJsonName].count
              }

              resolve(response)
            }).catch(error => {
              reject(error)
            })
          }))
        }

        Promise.all(arrAsync).then(response => {
          for (let j = 0; j < response.length; j++) {
            this.$set(this.actionBadge, response[j].api, {})
            this.$set(this.actionBadge[response[j].api], 'badgeNum', response[j].count)
          }
        }).catch(() => {})
      }
    }
  }
}
</script>

<style scoped >
.button-action-badge {
  margin-left: 5px;
}

/deep/.button-action-badge .ant-badge-count {
  right: 10px;
  z-index: 8;
}
</style>
