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
          action.showBadge &&
          ((!dataView && (action.listView || action.groupAction && selectedRowKeys.length > 0)) || (dataView && action.dataView)) &&
          ('show' in action ? action.show(resource, $store.getters.userInfo) : true)">
        <a-button
          :icon="action.icon"
          :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
          shape="circle"
          style="margin-right: 5px"
          @click="execAction(action)" />
      </a-badge>
      <a-button
        v-if="action.api in $store.getters.apis &&
          !action.showBadge &&
          ((!dataView && (action.listView || action.groupAction && selectedRowKeys.length > 0)) || (dataView && action.dataView)) &&
          ('show' in action ? action.show(resource, $store.getters.userInfo) : true)"
        :icon="action.icon"
        :type="action.icon === 'delete' ? 'danger' : (action.icon === 'plus' ? 'primary' : 'default')"
        shape="circle"
        style="margin-left: 5px"
        @click="execAction(action)" />
    </a-tooltip>
  </span>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'ActionButton',
  data () {
    return {
      actionBadge: []
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
      this.$emit('exec-action', action)
    },
    handleShowBadge () {
      const dataBadge = {}
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

              if (json[responseJsonName].count && json[responseJsonName].count > 0) {
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
            this.$set(dataBadge, response[j].api, {})
            this.$set(dataBadge[response[j].api], 'badgeNum', response[j].count)
          }
        })

        this.actionBadge = dataBadge
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
