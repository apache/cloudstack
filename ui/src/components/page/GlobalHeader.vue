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
  <a-layout-header v-if="!headerBarFixed" :class="[fixedHeader && 'ant-header-fixedHeader', sidebarOpened ? 'ant-header-side-opened' : 'ant-header-side-closed', theme ]" :style="{ padding: '0' }">
    <div v-if="mode === 'sidemenu'" class="header">
      <a-icon
        v-if="device==='mobile'"
        class="trigger"
        :type="collapsed ? 'menu-fold' : 'menu-unfold'"
        @click="toggle"></a-icon>
      <a-icon
        v-else
        class="trigger"
        :type="collapsed ? 'menu-unfold' : 'menu-fold'"
        @click="toggle"/>
      <project-menu v-if="device !== 'mobile'" />
      <saml-domain-switcher style="margin-left: 20px" />
      <user-menu></user-menu>
    </div>
    <div v-else :class="['top-nav-header-index', theme]">
      <div class="header-index-wide">
        <div class="header-index-left">
          <logo class="top-nav-header" :show-title="device !== 'mobile'" />
          <s-menu
            v-if="device !== 'mobile'"
            mode="horizontal"
            :menu="menus"
            :theme="theme"
          ></s-menu>
          <a-icon
            v-else
            class="trigger"
            :type="collapsed ? 'menu-fold' : 'menu-unfold'"
            @click="toggle"></a-icon>
        </div>
        <project-menu v-if="device !== 'mobile'" />
        <saml-domain-switcher style="margin-left: 20px" />
        <user-menu></user-menu>
      </div>
    </div>

  </a-layout-header>
</template>

<script>
import Breadcrumb from '@/components/widgets/Breadcrumb'
import Logo from '../header/Logo'
import SMenu from '../menu/'
import ProjectMenu from '../header/ProjectMenu'
import SamlDomainSwitcher from '../header/SamlDomainSwitcher'
import UserMenu from '../header/UserMenu'

import { mixin } from '@/utils/mixin.js'

export default {
  name: 'GlobalHeader',
  components: {
    Breadcrumb,
    Logo,
    SMenu,
    ProjectMenu,
    SamlDomainSwitcher,
    UserMenu
  },
  mixins: [mixin],
  props: {
    mode: {
      type: String,
      // sidemenu, topmenu
      default: 'sidemenu'
    },
    menus: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    },
    device: {
      type: String,
      required: false,
      default: 'desktop'
    }
  },
  data () {
    return {
      headerBarFixed: false
    }
  },
  mounted () {
    window.addEventListener('scroll', this.handleScroll)
  },
  methods: {
    handleScroll () {
      if (this.autoHideHeader) {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop
        if (scrollTop > 100) {
          this.headerBarFixed = true
        } else {
          this.headerBarFixed = false
        }
      } else {
        this.headerBarFixed = false
      }
    },
    toggle () {
      this.$emit('toggle')
    }
  }
}
</script>

<style lang="less" scoped>
.ant-layout-header {
  .anticon-menu-fold {
    font-size: 18px;
    line-height: 1;
  }

  .ant-breadcrumb {
    display: inline;
    vertical-align: middle;
  }
}
</style>
