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
          <router-link :to="{ name: 'account' }">
            <a-icon class="user-menu-item-icon" type="user"/>
            <span class="user-menu-item-name">Profile</span>
          </router-link>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="1">
          <router-link :to="{ name: 'account' }">
            <a-icon class="user-menu-item-icon" type="setting"/>
            <span class="user-menu-item-name">Account</span>
          </router-link>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="2" disabled>
          <a-icon class="user-menu-item-icon" type="setting"/>
          <span class="user-menu-item-name">Disabled</span>
        </a-menu-item>
        <a-menu-item class="user-menu-item" key="3" disabled>
          <a :href="helpUrl" target="_blank">
            <a-icon class="user-menu-item-icon" type="question-circle-o"></a-icon>
            <span class="user-menu-item-name">Help</span>
          </a>
        </a-menu-item>
        <a-menu-divider/>
        <a-menu-item class="user-menu-item" key="4">
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
