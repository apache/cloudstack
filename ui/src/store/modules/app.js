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
import {
  SIDEBAR_TYPE,
  DEFAULT_THEME,
  DEFAULT_LAYOUT_MODE,
  DEFAULT_COLOR,
  DEFAULT_COLOR_INVERTED,
  DEFAULT_FIXED_HEADER,
  DEFAULT_FIXED_SIDEMENU,
  DEFAULT_FIXED_HEADER_HIDDEN,
  DEFAULT_CONTENT_WIDTH_TYPE,
  DEFAULT_MULTI_TAB,
  USE_BROWSER_TIMEZONE,
  SERVER_MANAGER,
  VUE_VERSION,
  CUSTOM_COLUMNS
} from '@/store/mutation-types'

const app = {
  state: {
    version: process.env.PACKAGE_VERSION || 'main',
    sidebar: true,
    device: 'desktop',
    theme: '',
    layout: '',
    contentWidth: '',
    fixedHeader: false,
    fixSiderbar: false,
    autoHideHeader: false,
    color: null,
    inverted: true,
    multiTab: true,
    metrics: false,
    listAllProjects: false,
    server: '',
    vueVersion: ''
  },
  mutations: {
    SET_SIDEBAR_TYPE: (state, type) => {
      state.sidebar = type
      vueProps.$localStorage.set(SIDEBAR_TYPE, type)
    },
    CLOSE_SIDEBAR: (state, ls) => {
      vueProps.$localStorage.set(SIDEBAR_TYPE, true)
      state.sidebar = false
    },
    TOGGLE_DEVICE: (state, device) => {
      state.device = device
    },
    TOGGLE_THEME: (state, theme) => {
      vueProps.$localStorage.set(DEFAULT_THEME, theme)
      state.theme = theme
    },
    TOGGLE_LAYOUT_MODE: (state, layout) => {
      vueProps.$localStorage.set(DEFAULT_LAYOUT_MODE, layout)
      state.layout = layout
    },
    TOGGLE_FIXED_HEADER: (state, fixed) => {
      vueProps.$localStorage.set(DEFAULT_FIXED_HEADER, fixed)
      state.fixedHeader = fixed
    },
    TOGGLE_FIXED_SIDERBAR: (state, fixed) => {
      vueProps.$localStorage.set(DEFAULT_FIXED_SIDEMENU, fixed)
      state.fixSiderbar = fixed
    },
    TOGGLE_FIXED_HEADER_HIDDEN: (state, show) => {
      vueProps.$localStorage.set(DEFAULT_FIXED_HEADER_HIDDEN, show)
      state.autoHideHeader = show
    },
    TOGGLE_CONTENT_WIDTH: (state, type) => {
      vueProps.$localStorage.set(DEFAULT_CONTENT_WIDTH_TYPE, type)
      state.contentWidth = type
    },
    TOGGLE_COLOR: (state, color) => {
      vueProps.$localStorage.set(DEFAULT_COLOR, color)
      state.color = color
    },
    TOGGLE_INVERTED: (state, flag) => {
      vueProps.$localStorage.set(DEFAULT_COLOR_INVERTED, flag)
      state.inverted = flag
    },
    TOGGLE_MULTI_TAB: (state, bool) => {
      vueProps.$localStorage.set(DEFAULT_MULTI_TAB, bool)
      state.multiTab = bool
    },
    SET_METRICS: (state, bool) => {
      state.metrics = bool
    },
    SET_LIST_ALL_PROJECTS: (state, bool) => {
      state.listAllProjects = bool
    },
    SET_USE_BROWSER_TIMEZONE: (state, bool) => {
      vueProps.$localStorage.set(USE_BROWSER_TIMEZONE, bool)
      state.usebrowsertimezone = bool
    },
    SET_SERVER: (state, server) => {
      vueProps.$localStorage.set(SERVER_MANAGER, server)
      state.server = server
    },
    SET_VUE_VERSION: (state, version) => {
      vueProps.$localStorage.set(VUE_VERSION, version)
      state.vueVersion = version
    },
    SET_CUSTOM_COLUMNS: (state, customColumns) => {
      vueProps.$localStorage.set(CUSTOM_COLUMNS, customColumns)
      state.customColumns = customColumns
    },
    SET_SHUTDOWN_TRIGGERED: (state, shutdownTriggered) => {
      state.shutdownTriggered = shutdownTriggered
    }
  },
  actions: {
    setSidebar ({ commit }, type) {
      commit('SET_SIDEBAR_TYPE', type)
    },
    CloseSidebar ({ commit }) {
      commit('CLOSE_SIDEBAR')
    },
    ToggleDevice ({ commit }, device) {
      commit('TOGGLE_DEVICE', device)
    },
    ToggleTheme ({ commit }, theme) {
      commit('TOGGLE_THEME', theme)
    },
    ToggleLayoutMode ({ commit }, mode) {
      commit('TOGGLE_LAYOUT_MODE', mode)
    },
    ToggleFixedHeader ({ commit }, fixedHeader) {
      if (!fixedHeader) {
        commit('TOGGLE_FIXED_HEADER_HIDDEN', false)
      }
      commit('TOGGLE_FIXED_HEADER', fixedHeader)
    },
    ToggleFixSiderbar ({ commit }, fixSiderbar) {
      commit('TOGGLE_FIXED_SIDERBAR', fixSiderbar)
    },
    ToggleFixedHeaderHidden ({ commit }, show) {
      commit('TOGGLE_FIXED_HEADER_HIDDEN', show)
    },
    ToggleContentWidth ({ commit }, type) {
      commit('TOGGLE_CONTENT_WIDTH', type)
    },
    ToggleColor ({ commit }, color) {
      commit('TOGGLE_COLOR', color)
    },
    ToggleInverted ({ commit }, invertedFlag) {
      commit('TOGGLE_INVERTED', invertedFlag)
    },
    ToggleMultiTab ({ commit }, bool) {
      commit('TOGGLE_MULTI_TAB', bool)
    },
    SetMetrics ({ commit }, bool) {
      commit('SET_METRICS', bool)
    },
    SetListAllProjects ({ commit }, bool) {
      commit('SET_LIST_ALL_PROJECTS', bool)
    },
    SetUseBrowserTimezone ({ commit }, bool) {
      commit('SET_USE_BROWSER_TIMEZONE', bool)
    },
    SetServer ({ commit }, server) {
      commit('SET_SERVER', server)
    },
    SetCustomColumns ({ commit }, bool) {
      commit('SET_CUSTOM_COLUMNS', bool)
    },
    SetShutdownTriggered ({ commit }, bool) {
      commit('SET_SHUTDOWN_TRIGGERED', bool)
    }
  }
}

export default app
