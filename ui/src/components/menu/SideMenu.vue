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
import Logo from '../tools/Logo'
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
    overflow-y: auto;
  }

  &.ant-fixed-sidemenu {
    position: fixed;
    height: 100%;
  }

  &.light {
    background-color: #fff;
    box-shadow: 2px 0px 8px 0px rgba(29, 35, 41, 0.05);

    .ant-menu-light {
      border-right-color: transparent;
      padding: 10px 0;
    }
  }

  &.dark {
    .ant-menu-dark {
      border-right-color: transparent;
      padding: 10px 0;
    }
  }
}
</style>
