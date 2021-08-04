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

import Menu from 'ant-design-vue/es/menu'
import Icon from 'ant-design-vue/es/icon'

const { Item, SubMenu } = Menu

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
    }
  },
  created () {
    this.updateMenu()
  },
  watch: {
    collapsed (val) {
      if (val) {
        this.cachedOpenKeys = this.openKeys.concat()
        this.openKeys = []
      } else {
        this.openKeys = this.cachedOpenKeys
      }
    },
    $route: function () {
      this.updateMenu()
    }
  },
  methods: {
    // select menu item
    onOpenChange (openKeys) {
      if (this.mode === 'horizontal') {
        this.openKeys = openKeys
        return
      }
      const latestOpenKey = openKeys.find(key => !this.openKeys.includes(key))
      if (!this.rootSubmenuKeys.includes(latestOpenKey)) {
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
      this.collapsed ? (this.cachedOpenKeys = openKeys) : (this.openKeys = openKeys)
    },

    // render
    renderItem (menu) {
      if (!menu.hidden) {
        return menu.children && !menu.hideChildrenInMenu ? this.renderSubMenu(menu) : this.renderMenuItem(menu)
      }
      return null
    },
    renderMenuItem (menu) {
      const target = menu.meta.target || null
      const props = {
        to: { name: menu.name },
        target: target
      }
      return (
        <Item {...{ key: menu.path }}>
          <router-link {...{ props }}>
            {this.renderIcon(menu.meta.icon, menu)}
            <span>{this.$t(menu.meta.title)}</span>
          </router-link>
        </Item>
      )
    },
    renderSubMenu (menu) {
      const itemArr = []
      const on = {
        click: () => {
          this.handleClickParentMenu(menu)
        }
      }
      if (!menu.hideChildrenInMenu) {
        menu.children.forEach(item => itemArr.push(this.renderItem(item)))
      }
      return (
        <SubMenu {...{ key: menu.path }}>
          <span slot="title">
            {this.renderIcon(menu.meta.icon, menu)}
            <span {...{ on: on }}>{this.$t(menu.meta.title)}</span>
          </span>
          {itemArr}
        </SubMenu>
      )
    },
    renderIcon (icon, menuItem) {
      if (icon === 'none' || icon === undefined) {
        return null
      }
      const props = {}
      const on = {
        click: () => {
          this.handleClickParentMenu(menuItem)
        }
      }
      typeof (icon) === 'object' ? props.component = icon : props.type = icon
      return (
        <Icon {... { props, on } } />
      )
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
  },
  render () {
    const { mode, theme, menu } = this
    const props = {
      mode: mode,
      theme: theme,
      openKeys: this.openKeys
    }
    const on = {
      select: obj => {
        this.selectedKeys = obj.selectedKeys
        this.$emit('select', obj)
      },
      openChange: this.onOpenChange
    }

    const menuTree = menu.map(item => {
      if (item.hidden) {
        return null
      }
      return this.renderItem(item)
    })
    // {...{ props, on: on }}
    return (
      <Menu vModel={this.selectedKeys} {...{ props, on: on }}>
        {menuTree}
      </Menu>
    )
  }
}
