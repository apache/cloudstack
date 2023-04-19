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
    <external-link class="action"/>
    <translation-menu class="action"/>
    <header-notice class="action"/>
    <label class="user-menu-server-info action" v-if="$config.multipleServer">
      <database-outlined />
      {{ server.name || server.apiBase || 'Local-Server' }}
    </label>
    <a-dropdown>
      <span class="user-menu-dropdown action">
        <span v-if="image">
          <resource-icon :image="image" size="2x" style="margin-right: 5px"/>
        </span>
        <a-avatar v-else-if="userInitials" class="user-menu-avatar avatar" size="small" :style="{ backgroundColor: '#1890ff', color: 'white' }">
          {{ userInitials }}
        </a-avatar>
        <a-avatar v-else class="user-menu-avatar avatar" size="small" :style="{ backgroundColor: '#1890ff', color: 'white' }">
          <template #icon><user-outlined /></template>
        </a-avatar>
        <span>{{ nickname() }}</span>
      </span>
      <template #overlay>
        <a-menu class="user-menu-wrapper" @click="handleClickMenu">
          <a-menu-item class="user-menu-item" key="profile">
            <UserOutlined class="user-menu-item-icon" />
            <span class="user-menu-item-name">{{ $t('label.profilename') }}</span>
          </a-menu-item>
          <a-menu-item class="user-menu-item" key="timezone">
            <ClockCircleOutlined class="user-menu-item-icon" />
            <span class="user-menu-item-name" style="margin-right: 5px">{{ $t('label.use.local.timezone') }}</span>
            <a-switch :checked="$store.getters.usebrowsertimezone" />
          </a-menu-item>
          <a-menu-item class="user-menu-item" key="document">
            <QuestionCircleOutlined class="user-menu-item-icon" />
            <span class="user-menu-item-name">{{ $t('label.help') }}</span>
          </a-menu-item>
          <a-menu-divider/>
          <a-menu-item class="user-menu-item" key="logout">
            <LogoutOutlined class="user-menu-item-icon" />
            <span class="user-menu-item-name">{{ $t('label.logout') }}</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script>
import { api } from '@/api'
import ExternalLink from './ExternalLink'
import HeaderNotice from './HeaderNotice'
import TranslationMenu from './TranslationMenu'
import { mapActions, mapGetters } from 'vuex'
import ResourceIcon from '@/components/view/ResourceIcon'
import eventBus from '@/config/eventBus'
import { SERVER_MANAGER } from '@/store/mutation-types'

export default {
  name: 'UserMenu',
  components: {
    ExternalLink,
    TranslationMenu,
    HeaderNotice,
    ResourceIcon
  },
  data () {
    return {
      image: '',
      userInitials: '',
      countNotify: 0
    }
  },
  created () {
    this.userInitials = (this.$store.getters.userInfo.firstname.toUpperCase().charAt(0) || '') +
      (this.$store.getters.userInfo.lastname.toUpperCase().charAt(0) || '')
    this.getIcon()
    eventBus.on('refresh-header', () => {
      this.getIcon()
    })
    this.$store.watch(
      (state, getters) => getters.countNotify,
      (newValue, oldValue) => {
        this.countNotify = newValue
      }
    )
  },
  watch: {
    image () {
      this.getIcon()
    }
  },
  computed: {
    server () {
      return this.$localStorage.get(SERVER_MANAGER) || this.$config.servers[0]
    }
  },
  methods: {
    ...mapActions(['Logout']),
    ...mapGetters(['nickname', 'avatar']),
    toggleUseBrowserTimezone () {
      this.$store.dispatch('SetUseBrowserTimezone', !this.$store.getters.usebrowsertimezone)
    },
    async getIcon () {
      await this.fetchResourceIcon(this.$store.getters.userInfo.id)
    },
    fetchResourceIcon (id) {
      return new Promise((resolve, reject) => {
        api('listUsers', {
          id: id,
          showicon: true
        }).then(json => {
          const response = json.listusersresponse.user || []
          if (response?.[0]) {
            this.image = response[0]?.icon?.base64image || ''
            resolve(this.image)
          }
        }).catch(error => {
          reject(error)
        })
      })
    },
    handleClickMenu (item) {
      switch (item.key) {
        case 'profile':
          this.$router.push(`/accountuser/${this.$store.getters.userInfo.id}`)
          break
        case 'timezone':
          this.toggleUseBrowserTimezone()
          break
        case 'document':
          window.open(this.$config.docBase, '_blank')
          break
        case 'logout':
          this.handleLogout()
          break
      }
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
    },
    clearAllNotify () {
      this.$store.commit('SET_COUNT_NOTIFY', 0)
      this.$notification.destroy()
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

  &-server-info {
    .anticon {
      margin-right: 5px;
    }
  }
}
</style>
