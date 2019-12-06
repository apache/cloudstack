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
            <span class="user-menu-item-name">Profile</span>
          </router-link>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="1" disabled>
          <a :href="docBase" target="_blank">
            <a-icon class="user-menu-item-icon" type="question-circle-o"></a-icon>
            <span class="user-menu-item-name">Help</span>
          </a>
        </a-menu-item>
        <a-menu-divider/>
        <a-menu-item class="user-menu-item" key="2">
          <a href="javascript:;" @click="handleLogout">
            <a-icon class="user-menu-item-icon" type="logout"/>
            <span class="user-menu-item-name">Logout</span>
          </a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
  </div>
</template>

<script>
import config from '@/config/settings'
import HeaderNotice from './HeaderNotice'
import TranslationMenu from './TranslationMenu'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'UserMenu',
  components: {
    TranslationMenu,
    HeaderNotice
  },
  data () {
    return {
      docBase: config.docBase
    }
  },
  methods: {
    ...mapActions(['Logout']),
    ...mapGetters(['nickname', 'avatar']),
    handleLogout () {
      return this.Logout({}).then(() => {
        window.location.reload()
      }).catch(err => {
        this.$message.error({
          title: '错误',
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
    width: 160px;
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
