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
  <a-dropdown>
    <span class="action ant-dropdown-link translation-menu">
      <font-awesome-icon :icon="['fas', 'language']" size="lg" />
    </span>
    <a-menu
      slot="overlay"
      :selectedKeys="[language]"
      @click="onClick">
      <a-menu-item key="en" value="enUS">English</a-menu-item>
      <a-menu-item key="hi" value="hi">हिन्दी</a-menu-item>
      <a-menu-item key="ja_JP" value="jpJP">日本語</a-menu-item>
      <a-menu-item key="ko_KR" value="koKR">한국어</a-menu-item>
      <a-menu-item key="zh_CN" value="zhCN">简体中文</a-menu-item>
      <a-menu-item key="ar" value="arEG">Arabic</a-menu-item>
      <a-menu-item key="ca" value="caES">Catalan</a-menu-item>
      <a-menu-item key="de_DE" value="deDE">Deutsch</a-menu-item>
      <a-menu-item key="es" value="esES">Español</a-menu-item>
      <a-menu-item key="fr_FR" value="frFR">Français</a-menu-item>
      <a-menu-item key="it_IT" value="itIT">Italiano</a-menu-item>
      <a-menu-item key="hu" value="huHU">Magyar</a-menu-item>
      <a-menu-item key="nl_NL" value="nlNL">Nederlands</a-menu-item>
      <a-menu-item key="nb_NO" value="nbNO">Norsk</a-menu-item>
      <a-menu-item key="pl" value="plPL">Polish</a-menu-item>
      <a-menu-item key="pt_BR" value="ptBR">Português brasileiro</a-menu-item>
      <a-menu-item key="ru_RU" value="ruRU">Русский</a-menu-item>
      <a-menu-item key="el_GR" value="elGR">Ελληνικά</a-menu-item>
    </a-menu>
  </a-dropdown>
</template>

<script>
import Vue from 'vue'
import moment from 'moment'
import 'moment/locale/zh-cn'
import { loadLanguageAsync } from '@/locales'

moment.locale('en')

export default {
  name: 'TranslationMenu',
  data () {
    return {
      language: 'en'
    }
  },
  mounted () {
    this.language = Vue.ls.get('LOCALE') || 'en'
    this.setLocale(this.language)
  },
  methods: {
    moment,
    onClick (e) {
      let localeValue = e.key
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
      Vue.ls.set('LOCALE', localeValue)
      loadLanguageAsync(localeValue)
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
