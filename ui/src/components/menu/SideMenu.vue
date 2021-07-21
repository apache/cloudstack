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
  <a-layout-sider
    :class="['sider', isDesktop() ? null : 'shadow', theme, fixSiderbar ? 'ant-fixed-sidemenu' : null ]"
    width="256px"
    :collapsible="collapsible"
    v-model="collapsed"
    :trigger="null">
    <logo />
    <s-menu
      :collapsed="collapsed"
      :menu="menus"
      :theme="theme"
      :mode="mode"
      @select="onSelect"></s-menu>
  </a-layout-sider>

</template>

<script>
import ALayoutSider from 'ant-design-vue/es/layout/Sider'
import Logo from '../header/Logo'
import SMenu from './index'
import { mixin, mixinDevice } from '@/utils/mixin.js'

export default {
  name: 'SideMenu',
  components: { ALayoutSider, Logo, SMenu },
  mixins: [mixin, mixinDevice],
  props: {
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    collapsible: {
      type: Boolean,
      required: false,
      default: false
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    },
    menus: {
      type: Array,
      required: true
    }
  },
  methods: {
    onSelect (obj) {
      this.$emit('menuSelect', obj)
    }
  }
}
</script>

<style lang="less" scoped>
.sider {
  box-shadow: 2px 0 6px rgba(0, 21, 41, .35);
  position: relative;
  z-index: 10;
  height: auto;

  /deep/ .ant-layout-sider-children {
    overflow-y: hidden;
    &:hover {
      overflow-y: auto;
    }
  }

  /deep/ .ant-menu-vertical .ant-menu-item {
    margin-top: 0px;
    margin-bottom: 0px;
  }

  /deep/ .ant-menu-inline .ant-menu-item:not(:last-child) {
    margin-bottom: 0px;
  }

  /deep/ .ant-menu-inline .ant-menu-item {
    margin-top: 0px;
  }

  &.ant-fixed-sidemenu {
    position: fixed;
    height: 100%;
  }

  &.light {
    box-shadow: 2px 0px 8px 0px rgba(29, 35, 41, 0.05);

    .ant-menu-light {
      border-right-color: transparent;
      padding: 14px 0;
    }
  }

  &.dark {
    .ant-menu-dark {
      border-right-color: transparent;
      padding: 14px 0;
    }
  }
}
</style>
