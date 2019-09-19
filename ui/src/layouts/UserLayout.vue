<template>
  <div id="userLayout" :class="['user-layout', device]">
    <div class="user-layout-container">
      <div class="user-layout-header">
        <img src="~@/assets/banner.svg" class="user-layout-logo" alt="logo">
      </div>
      <route-view></route-view>
    </div>
  </div>
</template>

<script>
import RouteView from '@/layouts/RouteView'
import { mixinDevice } from '@/utils/mixin.js'

export default {
  name: 'UserLayout',
  components: { RouteView },
  mixins: [mixinDevice],
  data () {
    return {}
  },
  mounted () {
    document.body.classList.add('userLayout')
  },
  beforeDestroy () {
    document.body.classList.remove('userLayout')
  }
}
</script>

<style lang="less" scoped>
.user-layout {
  height: 100%;
  background: #fff;

  &-container {
    padding: 3rem 0;
    width: 100%;

    @media (min-height:600px) {
      padding: 0;
      position: relative;
      top: 50%;
      transform: translateY(-50%);
      margin-top: -50px;
    }
  }

  &-logo {
    width: 95%;
    max-width: 450px;
    border-style: none;
    margin: 0 auto 2rem;
    display: block;

    .mobile & {
      max-width: 300px;
      margin-bottom: 1rem;
    }
  }
}
</style>
