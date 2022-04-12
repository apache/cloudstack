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
            @click="() => { handleClickParentMenu(item) }" />
            <span @click="() => { handleClickParentMenu(item) }">{{ $t(item.meta.title) }}</span>
          </span>
        </template>
        <template v-for="children in item.children" :key="children.path">
          <a-menu-item :key="children.path" v-if="!children.hidden">
            <router-link :to="{ name: children.name, target: children.meta.target || null }">
              <render-icon
                v-if="children.meta.icon && typeof (children.meta.icon) === 'string'"
                :icon="children.meta.icon" />
              <render-icon v-else :svgIcon="children.meta.icon" />
              <span>{{ $t(children.meta.title) }}</span>
            </router-link>
          </a-menu-item>
        </template>
      </a-sub-menu>
      <a-menu-item v-else :key="item.path">
        <router-link :to="{ name: item.name, target: item.meta.target || null }">
          <render-icon
            v-if="item.meta.icon && typeof (item.meta.icon) === 'string'"
            :icon="item.meta.icon"
            @click="() => { handleClickParentMenu(item) }" />
          <span>{{ $t(item.meta.title) }}</span>
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
    handleClickParentMenu (menuItem) {
      if (this.cachedPath === menuItem.redirect) {
        return
      }
      if (menuItem.redirect) {
        this.cachedPath = menuItem.redirect
        setTimeout(() => this.$router.push({ path: menuItem.path }))
      }
    }
  }
}
</script>

<style>
.sider .ant-menu-vertical .ant-menu-item {
  margin-right: 0;
}
</style>
