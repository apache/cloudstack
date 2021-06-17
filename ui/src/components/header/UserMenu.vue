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
  <div class="user-menu">

    <translation-menu class="action"/>
    <header-notice class="action"/>
    <a-dropdown>
      <span class="user-menu-dropdown action">
        <a-avatar class="user-menu-avatar avatar" size="small" :src="avatar()"/>
        <span>{{ nickname() }}</span>
      </span>
      <a-menu slot="overlay" class="user-menu-wrapper">
        <a-menu-item class="user-menu-item" key="0">
          <router-link :to="{ path: '/accountuser/' + $store.getters.userInfo.id }">
            <a-icon class="user-menu-item-icon" type="user"/>
            <span class="user-menu-item-name">{{ $t('label.profilename') }}</span>
          </router-link>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="1">
          <a @click="toggleUseBrowserTimezone">
            <a-icon class="user-menu-item-icon" type="clock-circle"/>
            <span class="user-menu-item-name" style="margin-right: 5px">{{ $t('label.use.local.timezone') }}</span>
            <a-switch
              :checked="$store.getters.usebrowsertimezone" />
          </a>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="2" disabled>
          <a :href="$config.docBase" target="_blank">
            <a-icon class="user-menu-item-icon" type="question-circle-o"></a-icon>
            <span class="user-menu-item-name">{{ $t('label.help') }}</span>
          </a>
        </a-menu-item>
        <a-menu-divider/>
        <a-menu-item class="user-menu-item" key="3">
          <a href="javascript:;" @click="handleLogout">
            <a-icon class="user-menu-item-icon" type="logout"/>
            <span class="user-menu-item-name">{{ $t('label.logout') }}</span>
          </a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
  </div>
</template>

<script>
import HeaderNotice from './HeaderNotice'
import TranslationMenu from './TranslationMenu'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'UserMenu',
  components: {
    TranslationMenu,
    HeaderNotice
  },
  methods: {
    ...mapActions(['Logout']),
    ...mapGetters(['nickname', 'avatar']),
    toggleUseBrowserTimezone () {
      this.$store.dispatch('SetUseBrowserTimezone', !this.$store.getters.usebrowsertimezone)
    },
    handleLogout () {
      return this.Logout({}).then(() => {
        this.$router.push('/user/login')
      }).catch(err => {
        this.$message.error({
          title: 'Failed to Logout',
          description: err.message
        })
      })
    }
  }
}
</script>

<style lang="less" scoped>
.user-menu {
  &-wrapper {
    padding: 4px 0;
  }

  &-item {
    width: auto;
  }

  &-item-name {
    user-select: none;
    margin-left: 8px;
  }

  &-item-icon i {
    min-width: 12px;
    margin-right: 8px;
  }
}
</style>
