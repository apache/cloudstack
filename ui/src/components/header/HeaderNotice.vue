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
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottom"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true"
    overlayClassName="header-notice-popover">
    <template #content>
      <a-spin :spinning="loading">
        <a-list style="min-width: 200px; max-width: 300px">
          <a-list-item>
            <a-list-item-meta :title="$t('label.notifications')">
              <template #avatar>
                <a-avatar :style="{ backgroundColor: '#6887d0', verticalAlign: 'middle' }">
                  <template #icon><notification-outlined /></template>
                </a-avatar>
              </template>
              <template #description><a-button size="small" @click="clearJobs">{{ $t('label.clear.list') }}</a-button></template>
            </a-list-item-meta>
          </a-list-item>
          <a-list-item v-for="(notice, index) in notices" :key="index">
            <template #title>{{ notice.path }} </template>
            <a-list-item-meta :title="notice.title">
              <template #avatar>
                <a-avatar :style="notificationAvatar[notice.status].style">
                  <template #icon>
                    <render-icon :icon="notificationAvatar[notice.status].icon" />
                  </template>
                </a-avatar>
              </template>
              <template #description>
                <span v-if="getResourceName(notice.description, 'name') && notice.path && !['VPC_RESTART_REQUIRED', 'NETWORK_RESTART_REQUIRED'].includes(notice.key)">
                  <router-link :to="{ path: notice.path}">{{ getResourceName(notice.description, "name") + ' - ' }}</router-link>
                  {{ getResourceName(notice.description, "msg") }}</span>
                <span v-else-if="notice.path && ['VPC_RESTART_REQUIRED', 'NETWORK_RESTART_REQUIRED'].includes(notice.key)">
                  <router-link :to="{ path: notice.path, query: notice.query }">{{ notice.description }}</router-link>
                </span>
                <span v-else>{{ notice.description }}</span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </a-list>
      </a-spin>
    </template>
    <span @click="showNotifications" class="header-notice-opener">
      <a-badge :count="notices.length">
        <bell-outlined class="header-notice-icon" />
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
import store from '@/store'

export default {
  name: 'HeaderNotice',
  data () {
    return {
      loading: false,
      visible: false,
      notices: [],
      poller: null,
      notificationAvatar: {
        done: { icon: 'check-circle-outlined', style: { backgroundColor: '#87d068' } },
        progress: { icon: 'loading-outlined', style: { backgroundColor: '#ffbf00' } },
        failed: { icon: 'close-circle-outlined', style: { backgroundColor: '#f56a00' } }
      }
    }
  },
  methods: {
    showNotifications () {
      this.visible = !this.visible
    },
    clearJobs () {
      this.notices = this.notices.filter(x => x.status === 'progress')
      this.$store.commit('SET_HEADER_NOTICES', this.notices)
    },
    getResourceName (description, data) {
      if (description) {
        if (data === 'name') {
          const name = description.match(/\(([^)]+)\)/)
          return name ? name[1] : null
        }
        const msg = description.substring(description.indexOf(')') + 1)
        return msg
      }
    }
  },
  mounted () {
    this.notices = (store.getters.headerNotices || []).reverse()
    this.$store.watch(
      (state, getters) => getters.headerNotices,
      (newValue, oldValue) => {
        if (oldValue !== newValue && newValue !== undefined) {
          this.notices = newValue
        }
      }
    )
  }
}
</script>

<style lang="less" scoped>
  .header-notice {
    display: inline-block;
    transition: all 0.3s;

    &-popover {
      top: 50px !important;
      width: 300px;
      top: 50px;
    }

    &-opener {
      display: inline-block;
      transition: all 0.3s;
      vertical-align: initial;
    }

    &-icon {
      font-size: 18px;
      padding: 4px;
    }
  }
</style>
