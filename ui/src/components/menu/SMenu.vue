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
  <a-menu
      :mode="mode"
      :theme="theme"
      :openKeys="openKeys"
      v-model:selectedKeys="selectedKeys"
      @click="selectMenu"
      @openChange="onOpenChange"
    >
    <template v-for="(item, index) in menuData" :key="index">
      <a-sub-menu v-if="item.children && !item.hideChildrenInMenu" :key="item.path">
        <template #title>
          <span>
            <render-icon
            v-if="item.meta.icon && typeof (item.meta.icon) === 'string'"
            :icon="item.meta.icon"
            @click="() => { handleClickParentMenu(item) }"
            v-shortkey="item.meta.shortKey"
            @shortkey="handleMenuItemShortKey(item, '1')" />
            <span @click="() => { handleClickParentMenu(item) }">{{ $t(item.meta.title) }}</span>
            <span class="show-shortkey" v-if="$store.getters.showshortkeys && item.meta.shortKey">
              {{ getShortKeyLabel(item) }}
            </span>
          </span>
        </template>
        <template v-for="child in item.children" :key="child.path">
          <a-menu-item :key="child.path" v-if="!child.hidden">
            <router-link
              :to="{ name: child.name, target: child.meta.target || null }"
              v-shortkey="child.meta.shortKey"
              @shortkey="handleMenuItemShortKey(child, '2')">
              <render-icon
                v-if="child.meta.icon && typeof (child.meta.icon) === 'string'"
                :icon="child.meta.icon" />
              <render-icon v-else :svgIcon="child.meta.icon" />
              <span>{{ $t(child.meta.title) }}</span>
              <span class="show-shortkey" v-if="$store.getters.showshortkeys && child.meta.shortKey">
                {{ getShortKeyLabel(child) }}
              </span>
            </router-link>
          </a-menu-item>
        </template>
      </a-sub-menu>
      <a-menu-item v-else :key="item.path">
        <router-link
          :to="{ name: item.name, target: item.meta.target || null }"
          v-shortkey="item.meta.shortKey"
          @shortkey="handleMenuItemShortKey(item)">
          <render-icon
            v-if="item.meta.icon && typeof (item.meta.icon) === 'string'"
            :icon="item.meta.icon"
            @click="() => { handleClickParentMenu(item) }" />
          <span>{{ $t(item.meta.title) }}</span>
          <span class="show-shortkey" v-if="$store.getters.showshortkeys && item.meta.shortKey">
            {{ getShortKeyLabel(item) }}
          </span>
        </router-link>
      </a-menu-item>
    </template>
  </a-menu>
</template>

<script>

export default {
  name: 'SMenu',
  props: {
    menu: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data () {
    return {
      openKeys: [],
      selectedKeys: [],
      cachedOpenKeys: [],
      cachedPath: null
    }
  },
  computed: {
    rootSubmenuKeys: vm => {
      const keys = []
      vm.menu.forEach(item => keys.push(item.path))
      return keys
    },
    menuData () {
      return this.menu.filter(item => !item.hidden)
    }
  },
  created () {
    this.updateMenu()
  },
  watch: {
    collapsed (val) {
      this.openKeys = val ? [] : this.cachedOpenKeys
    },
    '$route.fullPath': function () {
      this.updateMenu()
    }
  },
  methods: {
    selectMenu (obj) {
      this.selectedKeys = [obj.key]
    },
    onOpenChange (openKeys) {
      const latestOpenKey = openKeys.find(key => this.openKeys.indexOf(key) === -1)
      if (this.rootSubmenuKeys.indexOf(latestOpenKey) === -1) {
        this.openKeys = openKeys
      } else {
        this.openKeys = latestOpenKey ? [latestOpenKey] : []
      }
    },
    updateMenu () {
      const routes = this.$route.matched.concat()

      if (routes.length >= 4 && this.$route.meta.hidden) {
        routes.pop()
        this.selectedKeys = [routes[2].path]
      } else {
        this.selectedKeys = [routes.pop().path]
      }

      const openKeys = []
      if (this.mode === 'inline') {
        routes.forEach(item => {
          openKeys.push(item.path)
        })
      }

      this.cachedPath = this.selectedKeys[0]
      this.cachedOpenKeys = openKeys
      if (!this.collapsed) {
        this.openKeys = openKeys
      }
    },
    routeMenuItem (redirect, path) {
      if (this.cachedPath === redirect) {
        return
      }
      if (redirect) {
        this.cachedPath = redirect
        setTimeout(() => this.$router.push({ path: path }))
      }
    },
    getShortKeyLabel (item) {
      if (item.meta.shortKey) {
        return item.meta.shortKey.join('+')
      }
      return null
    },
    handleClickParentMenu (menuItem) {
      this.routeMenuItem(menuItem.redirect, menuItem.path)
    },
    handleMenuItemShortKey (item, type) {
      if (type === 1) {
        this.handleClickParentMenu(item)
      } else {
        this.routeMenuItem(item.path, item.path)
      }
    }
  }
}
</script>

<style>
  .sider .ant-menu-vertical .ant-menu-item {
    margin-right: 0;
  }
  .show-shortkey {
    font-size: 10px;
    background-color: rgba(0, 0, 0, 0.9);
    padding: 2px 6px 2px 6px;
    border-radius: 4px;
    color: #e9e9e9;
    margin: 5px;
    border: solid .1em transparent;
    text-shadow: 2px 2px 2px rgba(255,255,255,0.1);
    box-shadow: -2px 2px 4px rgba(0, 0, 0, 0.8), -1px -1px 1px  rgba(0, 0, 0, 0.9);
  }
</style>
