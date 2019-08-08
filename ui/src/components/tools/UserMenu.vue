<template>
  <div class="user-wrapper">

    <translation-menu class="action"/>
    <header-notice class="action"/>
    <a-dropdown>
      <span class="action ant-dropdown-link user-dropdown-menu">
        <a-avatar class="avatar" size="small" :src="avatar()"/>
        <span>{{ nickname() }}</span>
      </span>
      <a-menu slot="overlay" class="user-dropdown-menu-wrapper">
        <a-menu-item key="0">
          <router-link :to="{ name: 'account' }">
            <a-icon type="user"/>
            <span>Profile</span>
          </router-link>
        </a-menu-item>
        <a-menu-item key="1">
          <router-link :to="{ name: 'account' }">
            <a-icon type="setting"/>
            <span>Account</span>
          </router-link>
        </a-menu-item>
        <a-menu-item key="2" disabled>
          <a-icon type="setting"/>
          <span>测试</span>
        </a-menu-item>
        <a-menu-item key="3" disabled>
          <a :href="helpUrl" target="_blank">
            <a-icon type="question-circle-o"></a-icon>
            <span>Help</span>
          </a>
        </a-menu-item>
        <a-menu-divider/>
        <a-menu-item key="4">
          <a href="javascript:;" @click="handleLogout">
            <a-icon type="logout"/>
            <span>Logout</span>
          </a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
  </div>
</template>

<script>
import config from '@/config/defaultSettings'
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
      helpUrl: config.helpUrl
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
  .user-dropdown-menu {
    span {
      user-select: none;
    }
  }

  .user-dropdown-menu-wrapper.ant-dropdown-menu {
    padding: 4px 0;

    .ant-dropdown-menu-item {
      width: 160px;
    }

    .ant-dropdown-menu-item > .anticon:first-child,
    .ant-dropdown-menu-item > a > .anticon:first-child,
    .ant-dropdown-menu-submenu-title > .anticon:first-child .ant-dropdown-menu-submenu-title > a > .anticon:first-child {
      min-width: 12px;
      margin-right: 8px;
    }

  }
</style>
