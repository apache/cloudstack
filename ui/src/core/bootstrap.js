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
  ASYNC_JOB_IDS
} from '@/store/mutation-types'

export default {
  install: (app) => {
    window.ls = app.config.globalProperties.$localStorage

    store.commit('SET_SIDEBAR_TYPE', window.ls.get(SIDEBAR_TYPE, true))
    store.commit('TOGGLE_THEME', window.ls.get(DEFAULT_THEME, config.navTheme))
    store.commit('TOGGLE_LAYOUT_MODE', window.ls.get(DEFAULT_LAYOUT_MODE, config.layout))
    store.commit('TOGGLE_FIXED_HEADER', window.ls.get(DEFAULT_FIXED_HEADER, config.fixedHeader))
    store.commit('TOGGLE_FIXED_SIDERBAR', window.ls.get(DEFAULT_FIXED_SIDEMENU, config.fixSiderbar))
    store.commit('TOGGLE_CONTENT_WIDTH', window.ls.get(DEFAULT_CONTENT_WIDTH_TYPE, config.contentWidth))
    store.commit('TOGGLE_FIXED_HEADER_HIDDEN', window.ls.get(DEFAULT_FIXED_HEADER_HIDDEN, config.autoHideHeader))
    store.commit('TOGGLE_INVERTED', window.ls.get(DEFAULT_COLOR_INVERTED, config.invertedMode))
    store.commit('TOGGLE_COLOR', window.ls.get(DEFAULT_COLOR, config.primaryColor))
    store.commit('TOGGLE_MULTI_TAB', window.ls.get(DEFAULT_MULTI_TAB, config.multiTab))
    store.commit('SET_TOKEN', window.ls.get(ACCESS_TOKEN))
    store.commit('SET_PROJECT', window.ls.get(CURRENT_PROJECT))
    store.commit('SET_ASYNC_JOB_IDS', window.ls.get(ASYNC_JOB_IDS) || [])
  }
}
