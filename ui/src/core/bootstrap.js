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

import { vueProps } from '@/vue-app'
import config from '@/config/settings'
import store from '@/store/'
import {
  ACCESS_TOKEN,
  CURRENT_PROJECT,
  DEFAULT_COLOR,
  DEFAULT_THEME,
  DEFAULT_LAYOUT_MODE,
  DEFAULT_COLOR_INVERTED,
  SIDEBAR_TYPE,
  DEFAULT_FIXED_HEADER,
  DEFAULT_FIXED_HEADER_HIDDEN,
  DEFAULT_FIXED_SIDEMENU,
  DEFAULT_CONTENT_WIDTH_TYPE,
  DEFAULT_MULTI_TAB,
  HEADER_NOTICES,
  VUE_VERSION
} from '@/store/mutation-types'

export default {
  install: (app) => {
    let vueVersion = vueProps.$localStorage.get(VUE_VERSION)
    if (vueVersion !== app.version) {
      vueVersion = app.version
      vueProps.$localStorage.clear()
    }

    store.commit('SET_VUE_VERSION', vueVersion)
    store.commit('SET_SIDEBAR_TYPE', vueProps.$localStorage.get(SIDEBAR_TYPE, true))
    store.commit('TOGGLE_THEME', vueProps.$localStorage.get(DEFAULT_THEME, config.navTheme))
    store.commit('TOGGLE_LAYOUT_MODE', vueProps.$localStorage.get(DEFAULT_LAYOUT_MODE, config.layout))
    store.commit('TOGGLE_FIXED_HEADER', vueProps.$localStorage.get(DEFAULT_FIXED_HEADER, config.fixedHeader))
    store.commit('TOGGLE_FIXED_SIDERBAR', vueProps.$localStorage.get(DEFAULT_FIXED_SIDEMENU, config.fixSiderbar))
    store.commit('TOGGLE_CONTENT_WIDTH', vueProps.$localStorage.get(DEFAULT_CONTENT_WIDTH_TYPE, config.contentWidth))
    store.commit('TOGGLE_FIXED_HEADER_HIDDEN', vueProps.$localStorage.get(DEFAULT_FIXED_HEADER_HIDDEN, config.autoHideHeader))
    store.commit('TOGGLE_INVERTED', vueProps.$localStorage.get(DEFAULT_COLOR_INVERTED, config.invertedMode))
    store.commit('TOGGLE_COLOR', vueProps.$localStorage.get(DEFAULT_COLOR, config.primaryColor))
    store.commit('TOGGLE_MULTI_TAB', vueProps.$localStorage.get(DEFAULT_MULTI_TAB, config.multiTab))
    store.commit('SET_TOKEN', vueProps.$localStorage.get(ACCESS_TOKEN))
    store.commit('SET_PROJECT', vueProps.$localStorage.get(CURRENT_PROJECT))
    store.commit('SET_HEADER_NOTICES', vueProps.$localStorage.get(HEADER_NOTICES) || [])
  }
}
