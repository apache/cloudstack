<template>

  <a-dropdown>
    <span class="action ant-dropdown-link translation-menu">
      <a-icon type="global"></a-icon>
    </span>
    <a-menu slot="overlay" @click="onClick">
      <a-menu-item key="en" :value="enUS">English</a-menu-item>
      <a-menu-item key="cn" :value="null">Chinese</a-menu-item>
      <a-menu-item key="jp" :value="null">Japanese</a-menu-item>
      <a-menu-item key="fr" :value="null">French</a-menu-item>
    </a-menu>
  </a-dropdown>

</template>

<script>
import enUS from 'ant-design-vue/lib/locale-provider/en_US'
import moment from 'moment'
import 'moment/locale/zh-cn'
moment.locale('en')

export default {
  name: 'TranslationMenu',
  components: {
  },
  data () {
    return {
      locale: null,
      enUS
    }
  },
  methods: {
    moment,
    onClick (e) {
      const localeValue = e.target.value
      this.locale = localeValue
      if (!localeValue) {
        moment.locale('en')
      } else {
        moment.locale('zh-cn')
      }
    }
  }
}
</script>

<style lang="less" scoped>
.translation-menu {
  font-size: 18px;
  line-height: 1;
}
</style>
