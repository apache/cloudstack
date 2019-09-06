<template>

  <a-dropdown>
    <span class="action ant-dropdown-link translation-menu">
      <flag :iso="flag" style="font-size: 12px; margin-right: -25px; margin-top: -20px" />
      <a-icon type="global"></a-icon>
    </span>
    <a-menu slot="overlay" @click="onClick">
      <a-menu-item key="en" :value="enUS"><flag iso="gb"/> English</a-menu-item>
      <a-menu-item key="hi" :value="hi"><flag iso="in"/> हिन्दी</a-menu-item>
      <a-menu-item key="ja_JP" :value="jpJP"><flag iso="jp"/> 日本語</a-menu-item>
      <a-menu-item key="ko_KR" :value="koKR"><flag iso="kr"/> 한국어</a-menu-item>
      <a-menu-item key="zh_CN" :value="zhCN"><flag iso="cn"/> 简体中文</a-menu-item>
      <a-menu-item key="ar" :value="arEG"><flag iso="ae"/> Arabic</a-menu-item>
      <a-menu-item key="ca" :value="caES"><flag iso="ca"/> Catalan</a-menu-item>
      <a-menu-item key="de_DE" :value="deDE"><flag iso="de"/> Deutsch</a-menu-item>
      <a-menu-item key="es" :value="esES"><flag iso="es"/> Españo</a-menu-item>
      <a-menu-item key="fr_FR" :value="frFR"><flag iso="fr"/> Français</a-menu-item>
      <a-menu-item key="it_IT" :value="itIT"><flag iso="it"/> Italiano</a-menu-item>
      <a-menu-item key="hu" :value="huHU"><flag iso="hu"/> Magyar</a-menu-item>
      <a-menu-item key="nl_NL" :value="nlNL"><flag iso="nl"/> Nederlands</a-menu-item>
      <a-menu-item key="nb_NO" :value="nbNO"><flag iso="no"/> Norsk</a-menu-item>
      <a-menu-item key="pl" :value="plPL"><flag iso="pl"/> Polish</a-menu-item>
      <a-menu-item key="pt_BR" :value="ptBR"><flag iso="pt"/> Português brasileiro</a-menu-item>
      <a-menu-item key="ru_RU" :value="ruRU"><flag iso="ru"/> Русский</a-menu-item>
    </a-menu>
  </a-dropdown>

</template>

<script>
import Vue from 'vue'
import enUS from 'ant-design-vue/lib/locale-provider/en_US'
import arEG from 'ant-design-vue/lib/locale-provider/ar_EG'
import caES from 'ant-design-vue/lib/locale-provider/ca_ES'
import deDE from 'ant-design-vue/lib/locale-provider/de_DE'
import esES from 'ant-design-vue/lib/locale-provider/es_ES'
import frFR from 'ant-design-vue/lib/locale-provider/fr_FR'
import huHU from 'ant-design-vue/lib/locale-provider/hu_HU'
import itIT from 'ant-design-vue/lib/locale-provider/it_IT'
import jpJP from 'ant-design-vue/lib/locale-provider/ja_JP'
import koKR from 'ant-design-vue/lib/locale-provider/ko_KR'
import nbNO from 'ant-design-vue/lib/locale-provider/nb_NO'
import nlNL from 'ant-design-vue/lib/locale-provider/nl_NL'
import plPL from 'ant-design-vue/lib/locale-provider/pl_PL'
import ptBR from 'ant-design-vue/lib/locale-provider/pt_BR'
import ruRU from 'ant-design-vue/lib/locale-provider/ru_RU'
import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN'
import moment from 'moment'
import 'moment/locale/zh-cn'

import FlagIcon from 'vue-flag-icon'
Vue.use(FlagIcon)

moment.locale('en')

export default {
  name: 'TranslationMenu',
  components: {
    FlagIcon
  },
  data () {
    return {
      flag: 'gb',
      enUS,
      arEG,
      caES,
      deDE,
      esES,
      frFR,
      huHU,
      itIT,
      jpJP,
      koKR,
      nbNO,
      nlNL,
      plPL,
      ptBR,
      ruRU,
      zhCN
    }
  },
  methods: {
    moment,
    onClick (e) {
      var localeValue = e.key
      if (!localeValue) {
        localeValue = 'en'
      }
      this.$locale = localeValue
      moment.locale(localeValue)
      this.$i18n.locale = localeValue
      Vue.ls.set('current_locale', localeValue)
      this.flag = e.item.$children[0].iso
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
