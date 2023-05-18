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
  <div>
    <a-affix v-if="this.$store.getters.shutdownTriggered" >
      <a-alert :message="$t('message.shutdown.triggered')" type="error" banner :showIcon="false" class="shutdownHeader" />
    </a-affix>
    <a-layout class="layout" :class="[device]">
      <a-affix style="z-index: 200" :offsetTop="this.$store.getters.shutdownTriggered ? 25 : 0">
        <template v-if="isSideMenu()">
          <a-drawer
            v-if="isMobile()"
            :wrapClassName="'drawer-sider ' + navTheme"
            :closable="false"
            :visible="collapsed"
            placement="left"
            @close="() => this.collapsed = false"
          >
            <side-menu
              :menus="menus"
              :theme="navTheme"
              :collapsed="false"
              :collapsible="true"
              mode="inline"
              @menuSelect="menuSelect"></side-menu>
          </a-drawer>
          <side-menu
            v-else
            mode="inline"
            :menus="menus"
            :theme="navTheme"
            :collapsed="collapsed"
            :collapsible="true"></side-menu>
        </template>
        <template v-else>
          <a-drawer
            v-if="isMobile()"
            :wrapClassName="'drawer-sider ' + navTheme"
            placement="left"
            @close="() => this.collapsed = false"
            :closable="false"
            :visible="collapsed"
          >
            <side-menu
              :menus="menus"
              :theme="navTheme"
              :collapsed="false"
              :collapsible="true"
              mode="inline"
              @menuSelect="menuSelect"></side-menu>
          </a-drawer>
        </template>

        <drawer :visible="showSetting" placement="right" v-if="isAdmin && (isDevelopmentMode || allowSettingTheme)">
          <template #handler>
            <a-button type="primary" size="large">
              <close-outlined v-if="showSetting" />
              <setting-outlined v-else />
            </a-button>
          </template>
          <template #drawer>
            <setting :visible="showSetting" />
          </template>
        </drawer>

      </a-affix>

      <a-layout :class="[layoutMode, `content-width-${contentWidth}`]" :style="{ paddingLeft: contentPaddingLeft, minHeight: '100vh' }">
        <!-- layout header -->
        <a-affix style="z-index: 100">
          <global-header
            :style="this.$store.getters.shutdownTriggered ? 'margin-top: 25px;' : null"
            :mode="layoutMode"
            :menus="menus"
            :theme="navTheme"
            :collapsed="collapsed"
            :device="device"
            @toggle="toggle"
          />
        </a-affix>

        <a-button
          v-if="showClear"
          type="default"
          size="small"
          class="button-clear-notification"
          @click="onClearNotification">{{ $t('label.clear.notification') }}</a-button>

        <!-- layout content -->
        <a-layout-content
        class="layout-content"
        :class="{'is-header-fixed': fixedHeader}">
          <slot></slot>
        </a-layout-content>

        <!-- layout footer -->
        <a-layout-footer style="padding: 0">
          <global-footer />
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </div>
</template>

<script>
import SideMenu from '@/components/menu/SideMenu'
import GlobalHeader from '@/components/page/GlobalHeader'
import GlobalFooter from '@/components/page/GlobalFooter'
import { triggerWindowResizeEvent } from '@/utils/util'
import { mapState, mapActions } from 'vuex'
import { mixin, mixinDevice } from '@/utils/mixin.js'
import { isAdmin } from '@/role'
import { api } from '@/api'
import Drawer from '@/components/widgets/Drawer'
import Setting from '@/components/view/Setting.vue'

export default {
  name: 'GlobalLayout',
  components: {
    SideMenu,
    GlobalHeader,
    GlobalFooter,
    Drawer,
    Setting
  },
  mixins: [mixin, mixinDevice],
  data () {
    return {
      collapsed: false,
      menus: [],
      showSetting: false,
      showClear: false
    }
  },
  computed: {
    ...mapState({
      mainMenu: state => state.permission.addRouters
    }),
    isAdmin () {
      return isAdmin()
    },
    isDevelopmentMode () {
      return process.env.NODE_ENV === 'development'
    },
    allowSettingTheme () {
      return this.$config.allowSettingTheme
    },
    contentPaddingLeft () {
      if (!this.fixSidebar || this.isMobile()) {
        return '0'
      }
      if (this.sidebarOpened) {
        return '256px'
      }
      return '80px'
    }
  },
  watch: {
    sidebarOpened (val) {
      this.collapsed = !val
    },
    mainMenu (newMenu) {
      this.menus = newMenu.find((item) => item.path === '/').children
    },
    '$store.getters.darkMode' (darkMode) {
      if (darkMode) {
        document.body.classList.add('dark-mode')
      } else {
        document.body.classList.remove('dark-mode')
      }
    },
    '$store.getters.countNotify' (countNotify) {
      this.showClear = false
      if (countNotify && countNotify > 0) {
        this.showClear = true
      }
    }
  },
  provide: function () {
    return {
      parentToggleSetting: this.toggleSetting
    }
  },
  created () {
    this.menus = this.mainMenu.find((item) => item.path === '/').children
    this.collapsed = !this.sidebarOpened
    setInterval(this.checkShutdown, 5000)
  },
  mounted () {
    const layoutMode = this.$config.theme['@layout-mode'] || 'light'
    this.$store.dispatch('SetDarkMode', (layoutMode === 'dark'))
    if (layoutMode === 'dark') {
      document.body.classList.add('dark-mode')
    }
    const userAgent = navigator.userAgent
    if (userAgent.indexOf('Edge') > -1) {
      this.$nextTick(() => {
        this.collapsed = !this.collapsed
        setTimeout(() => {
          this.collapsed = !this.collapsed
        }, 16)
      })
    }
    const countNotify = this.$store.getters.countNotify
    this.showClear = false
    if (countNotify && countNotify > 0) {
      this.showClear = true
    }
  },
  beforeUnmount () {
    document.body.classList.remove('dark')
  },
  methods: {
    ...mapActions(['setSidebar']),
    toggle () {
      this.collapsed = !this.collapsed
      this.setSidebar(!this.collapsed)
      triggerWindowResizeEvent()
    },
    paddingCalc () {
      let left = ''
      if (this.sidebarOpened) {
        left = this.isDesktop() ? '256px' : '80px'
      } else {
        left = this.isMobile() ? '0' : (this.fixSidebar ? '80px' : '0')
      }
      return left
    },
    menuSelect () {
      if (!this.isDesktop()) {
        this.collapsed = false
      }
    },
    toggleSetting (showSetting) {
      this.showSetting = showSetting
    },
    onClearNotification () {
      this.$notification.destroy()
      this.$store.commit('SET_COUNT_NOTIFY', 0)
    },
    checkShutdown () {
      api('readyForShutdown', {}).then(json => {
        this.$store.dispatch('SetShutdownTriggered', json.readyforshutdownresponse.readyforshutdown.shutdowntriggered || false)
      })
    }
  }
}
</script>

<style lang="less">
.layout-content {
  &.is-header-fixed {
    margin: 78px 12px 0;
  }
}

// Todo try to get this rules scoped
.ant-drawer.drawer-sider {
  .sider {
    box-shadow: none;
  }

  &.dark {
    .ant-drawer-content {
      background-color: rgb(0, 21, 41);
    }
  }

  &.light {
    box-shadow: none;

    .ant-drawer-content {
      background-color: #fff;
    }
  }

  .ant-drawer-body {
    padding: 0
  }
}

.shutdownHeader {
  font-weight: bold;
  height: 25px;
  text-align: center;
  padding: 0px;
  margin: 0px;
  width: 100vw;
  position: absolute;
}

</style>
