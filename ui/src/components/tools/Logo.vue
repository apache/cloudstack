<template>
  <div class="logo">
    <img class="logo-image" src="~@/assets/logo.svg"/>
    <project-menu></project-menu>
  </div>
</template>

<script>
import ProjectMenu from '../tools/ProjectMenu'

export default {
  name: 'Logo',
  components: {
    ProjectMenu
  },
  props: {
    title: {
      type: String,
      default: 'CloudStack',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  }
}
</script>

<style type="less" scoped>
.logo {
  height: 64px;
  position: relative;
  line-height: 64px;
  padding-left: 12px;
  -webkit-transition: all .3s;
  transition: all .3s;
  background: #002140;
  overflow: hidden;
}

.sider.light .logo {
  background: #fff;
  box-shadow: 1px 1px 0px 0px #e8e8e8;
}

.logo-image {
  width: 54px;
  margin-right: 12px;
  display: inline-block;
  vertical-align: middle;
}

</style>
