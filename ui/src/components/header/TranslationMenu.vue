<template>

  <a-dropdown>
    <span class="action ant-dropdown-link translation-menu">
      <font-awesome-icon :icon="['fas', 'language']" size="lg" />
    </span>
    <a-menu
      slot="overlay"
      :selectedKeys="[language]"
      @click="onClick">
      <a-menu-item key="en" :value="enUS">English</a-menu-item>
      <a-menu-item key="hi" :value="hi">हिन्दी</a-menu-item>
      <a-menu-item key="ja_JP" :value="jpJP">日本語</a-menu-item>
      <a-menu-item key="ko_KR" :value="koKR">한국어</a-menu-item>
      <a-menu-item key="zh_CN" :value="zhCN">简体中文</a-menu-item>
      <a-menu-item key="ar" :value="arEG">Arabic</a-menu-item>
      <a-menu-item key="ca" :value="caES">Catalan</a-menu-item>
      <a-menu-item key="de_DE" :value="deDE">Deutsch</a-menu-item>
      <a-menu-item key="es" :value="esES">Españo</a-menu-item>
      <a-menu-item key="fr_FR" :value="frFR">Français</a-menu-item>
      <a-menu-item key="it_IT" :value="itIT">Italiano</a-menu-item>
      <a-menu-item key="hu" :value="huHU">Magyar</a-menu-item>
      <a-menu-item key="nl_NL" :value="nlNL">Nederlands</a-menu-item>
      <a-menu-item key="nb_NO" :value="nbNO">Norsk</a-menu-item>
      <a-menu-item key="pl" :value="plPL">Polish</a-menu-item>
      <a-menu-item key="pt_BR" :value="ptBR">Português brasileiro</a-menu-item>
      <a-menu-item key="ru_RU" :value="ruRU">Русский</a-menu-item>
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
import hi from 'ant-design-vue/lib/locale-provider/he_IL'
import moment from 'moment'
import 'moment/locale/zh-cn'

moment.locale('en')

export default {
  name: 'TranslationMenu',
  data () {
    return {
      language: 'en',
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
      zhCN,
      hi
    }
  },
  methods: {
    moment,
    onClick (e) {
      var localeValue = e.key
      if (!localeValue) {
        localeValue = 'en'
      }
      this.setLocale(localeValue)
    },
    setLocale (localeValue) {
      this.$locale = localeValue
      this.$i18n.locale = localeValue
      this.language = localeValue
      moment.locale(localeValue)
      Vue.ls.set('current_locale', localeValue)
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
