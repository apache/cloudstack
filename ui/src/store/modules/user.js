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

import Cookies from 'js-cookie'
import message from 'ant-design-vue/es/message'
import notification from 'ant-design-vue/es/notification'

import { vueProps } from '@/vue-app'
import router from '@/router'
import store from '@/store'
import { login, logout, api } from '@/api'
import { i18n } from '@/locales'

import {
  ACCESS_TOKEN,
  CURRENT_PROJECT,
  DEFAULT_THEME,
  APIS,
  ZONES,
  TIMEZONE_OFFSET,
  USE_BROWSER_TIMEZONE,
  HEADER_NOTICES,
  DOMAIN_STORE,
  DARK_MODE,
  CUSTOM_COLUMNS
} from '@/store/mutation-types'

const user = {
  state: {
    token: '',
    name: '',
    avatar: '',
    info: {},
    apis: {},
    features: {},
    project: {},
    headerNotices: [],
    isLdapEnabled: false,
    cloudian: {},
    zones: {},
    timezoneoffset: 0.0,
    usebrowsertimezone: false,
    domainStore: {},
    darkMode: false,
    defaultListViewPageSize: 20,
    countNotify: 0,
    loginFlag: false,
    logoutFlag: false,
    customColumns: {},
    shutdownTriggered: false,
    twoFaEnabled: false,
    twoFaProvider: '',
    twoFaIssuer: ''
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_TIMEZONE_OFFSET: (state, timezoneoffset) => {
      vueProps.$localStorage.set(TIMEZONE_OFFSET, timezoneoffset)
      state.timezoneoffset = timezoneoffset
    },
    SET_USE_BROWSER_TIMEZONE: (state, bool) => {
      vueProps.$localStorage.set(USE_BROWSER_TIMEZONE, bool)
      state.usebrowsertimezone = bool
    },
    SET_PROJECT: (state, project = {}) => {
      vueProps.$localStorage.set(CURRENT_PROJECT, project)
      state.project = project
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_INFO: (state, info) => {
      state.info = info
    },
    SET_APIS: (state, apis) => {
      state.apis = apis
      vueProps.$localStorage.set(APIS, apis)
    },
    SET_FEATURES: (state, features) => {
      state.features = features
    },
    SET_HEADER_NOTICES: (state, noticeJsonArray) => {
      vueProps.$localStorage.set(HEADER_NOTICES, noticeJsonArray)
      state.headerNotices = noticeJsonArray
    },
    SET_LDAP: (state, isLdapEnabled) => {
      state.isLdapEnabled = isLdapEnabled
    },
    SET_CLOUDIAN: (state, cloudian) => {
      state.cloudian = cloudian
    },
    RESET_THEME: (state) => {
      vueProps.$localStorage.set(DEFAULT_THEME, 'light')
    },
    SET_ZONES: (state, zones) => {
      state.zones = zones
      vueProps.$localStorage.set(ZONES, zones)
    },
    SET_DOMAIN_STORE (state, domainStore) {
      state.domainStore = domainStore
      vueProps.$localStorage.set(DOMAIN_STORE, domainStore)
    },
    SET_DARK_MODE (state, darkMode) {
      state.darkMode = darkMode
      vueProps.$localStorage.set(DARK_MODE, darkMode)
    },
    SET_DEFAULT_LISTVIEW_PAGE_SIZE: (state, defaultListViewPageSize) => {
      state.defaultListViewPageSize = defaultListViewPageSize
    },
    SET_COUNT_NOTIFY (state, number) {
      state.countNotify = number
    },
    SET_CUSTOM_COLUMNS: (state, customColumns) => {
      vueProps.$localStorage.set(CUSTOM_COLUMNS, customColumns)
      state.customColumns = customColumns
    },
    SET_SHUTDOWN_TRIGGERED: (state, shutdownTriggered) => {
      state.shutdownTriggered = shutdownTriggered
    },
    SET_LOGOUT_FLAG: (state, flag) => {
      state.logoutFlag = flag
    },
    SET_2FA_ENABLED: (state, flag) => {
      state.twoFaEnabled = flag
    },
    SET_2FA_PROVIDER: (state, flag) => {
      state.twoFaProvider = flag
    },
    SET_2FA_ISSUER: (state, flag) => {
      state.twoFaIssuer = flag
    },
    SET_LOGIN_FLAG: (state, flag) => {
      state.loginFlag = flag
    }
  },

  actions: {
    SetProject ({ commit }, project) {
      commit('SET_PROJECT', project)
    },
    Login ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        login(userInfo).then(response => {
          const result = response.loginresponse || {}
          Cookies.set('account', result.account, { expires: 1 })
          Cookies.set('domainid', result.domainid, { expires: 1 })
          Cookies.set('role', result.type, { expires: 1 })
          Cookies.set('timezone', result.timezone, { expires: 1 })
          Cookies.set('timezoneoffset', result.timezoneoffset, { expires: 1 })
          Cookies.set('userfullname', result.firstname + ' ' + result.lastname, { expires: 1 })
          Cookies.set('userid', result.userid, { expires: 1 })
          Cookies.set('username', result.username, { expires: 1 })
          vueProps.$localStorage.set(ACCESS_TOKEN, result.sessionkey, 24 * 60 * 60 * 1000)
          commit('SET_TOKEN', result.sessionkey)
          commit('SET_TIMEZONE_OFFSET', result.timezoneoffset)

          const cachedUseBrowserTimezone = vueProps.$localStorage.get(USE_BROWSER_TIMEZONE, false)
          commit('SET_USE_BROWSER_TIMEZONE', cachedUseBrowserTimezone)
          const darkMode = vueProps.$localStorage.get(DARK_MODE, false)
          commit('SET_DARK_MODE', darkMode)
          const cachedCustomColumns = vueProps.$localStorage.get(CUSTOM_COLUMNS, {})
          commit('SET_CUSTOM_COLUMNS', cachedCustomColumns)

          commit('SET_APIS', {})
          commit('SET_NAME', '')
          commit('SET_AVATAR', '')
          commit('SET_INFO', {})
          commit('SET_PROJECT', {})
          commit('SET_HEADER_NOTICES', [])
          commit('SET_FEATURES', {})
          commit('SET_LDAP', {})
          commit('SET_CLOUDIAN', {})
          commit('SET_DOMAIN_STORE', {})
          commit('SET_LOGOUT_FLAG', false)
          commit('SET_2FA_ENABLED', (result.is2faenabled === 'true'))
          commit('SET_2FA_PROVIDER', result.providerfor2fa)
          commit('SET_2FA_ISSUER', result.issuerfor2fa)
          commit('SET_LOGIN_FLAG', false)
          notification.destroy()

          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    GetInfo ({ commit }, switchDomain) {
      return new Promise((resolve, reject) => {
        const cachedApis = switchDomain ? {} : vueProps.$localStorage.get(APIS, {})
        const cachedZones = vueProps.$localStorage.get(ZONES, [])
        const cachedTimezoneOffset = vueProps.$localStorage.get(TIMEZONE_OFFSET, 0.0)
        const cachedUseBrowserTimezone = vueProps.$localStorage.get(USE_BROWSER_TIMEZONE, false)
        const cachedCustomColumns = vueProps.$localStorage.get(CUSTOM_COLUMNS, {})
        const domainStore = vueProps.$localStorage.get(DOMAIN_STORE, {})
        const darkMode = vueProps.$localStorage.get(DARK_MODE, false)
        const hasAuth = Object.keys(cachedApis).length > 0

        commit('SET_DOMAIN_STORE', domainStore)
        commit('SET_DARK_MODE', darkMode)
        if (hasAuth) {
          console.log('Login detected, using cached APIs')
          commit('SET_ZONES', cachedZones)
          commit('SET_APIS', cachedApis)
          commit('SET_TIMEZONE_OFFSET', cachedTimezoneOffset)
          commit('SET_USE_BROWSER_TIMEZONE', cachedUseBrowserTimezone)
          commit('SET_CUSTOM_COLUMNS', cachedCustomColumns)

          // Ensuring we get the user info so that store.getters.user is never empty when the page is freshly loaded
          api('listUsers', { username: Cookies.get('username'), listall: true }).then(response => {
            const result = response.listusersresponse.user[0]
            commit('SET_INFO', result)
            commit('SET_NAME', result.firstname + ' ' + result.lastname)
            resolve(cachedApis)
          }).catch(error => {
            reject(error)
          })
        } else if (store.getters.loginFlag) {
          const hide = message.loading(i18n.global.t('message.discovering.feature'), 0)
          api('listZones').then(json => {
            const zones = json.listzonesresponse.zone || []
            commit('SET_ZONES', zones)
          }).catch(error => {
            reject(error)
          })
          api('listApis').then(response => {
            const apis = {}
            const apiList = response.listapisresponse.api
            for (var idx = 0; idx < apiList.length; idx++) {
              const api = apiList[idx]
              const apiName = api.name
              apis[apiName] = {
                params: api.params,
                response: api.response
              }
            }
            commit('SET_APIS', apis)
            resolve(apis)
            store.dispatch('GenerateRoutes', { apis }).then(() => {
              store.getters.addRouters.map(route => {
                router.addRoute(route)
              })
            })
            hide()
            message.success(i18n.global.t('message.sussess.discovering.feature'))
          }).catch(error => {
            reject(error)
          })
        }

        api('listUsers', { username: Cookies.get('username') }).then(response => {
          const result = response.listusersresponse.user[0]
          commit('SET_INFO', result)
          commit('SET_NAME', result.firstname + ' ' + result.lastname)
        }).catch(error => {
          reject(error)
        })

        api('listCapabilities').then(response => {
          const result = response.listcapabilitiesresponse.capability
          commit('SET_FEATURES', result)
          if (result && result.defaultuipagesize) {
            commit('SET_DEFAULT_LISTVIEW_PAGE_SIZE', result.defaultuipagesize)
          }
        }).catch(error => {
          reject(error)
        })

        api('listLdapConfigurations').then(response => {
          const ldapEnable = (response.ldapconfigurationresponse.count > 0)
          commit('SET_LDAP', ldapEnable)
        }).catch(error => {
          reject(error)
        })

        api('cloudianIsEnabled').then(response => {
          const cloudian = response.cloudianisenabledresponse.cloudianisenabled || {}
          commit('SET_CLOUDIAN', cloudian)
        }).catch(ignored => {
        })
      })
    },

    Logout ({ commit, state }) {
      return new Promise((resolve) => {
        var cloudianUrl = null
        if (state.cloudian.url && state.cloudian.enabled) {
          cloudianUrl = state.cloudian.url + 'logout.htm?redirect=' + encodeURIComponent(window.location.href)
        }

        Object.keys(Cookies.get()).forEach(cookieName => {
          Cookies.remove(cookieName)
          Cookies.remove(cookieName, { path: '/client' })
        })

        commit('SET_TOKEN', '')
        commit('SET_APIS', {})
        commit('SET_PROJECT', {})
        commit('SET_HEADER_NOTICES', [])
        commit('SET_FEATURES', {})
        commit('SET_LDAP', {})
        commit('SET_CLOUDIAN', {})
        commit('RESET_THEME')
        commit('SET_DOMAIN_STORE', {})
        commit('SET_LOGOUT_FLAG', true)
        commit('SET_2FA_ENABLED', false)
        commit('SET_2FA_PROVIDER', '')
        commit('SET_2FA_ISSUER', '')
        commit('SET_LOGIN_FLAG', false)
        vueProps.$localStorage.remove(CURRENT_PROJECT)
        vueProps.$localStorage.remove(ACCESS_TOKEN)
        vueProps.$localStorage.remove(HEADER_NOTICES)

        logout(state.token).then(() => {
          message.destroy()
          if (cloudianUrl) {
            window.location.href = cloudianUrl
          } else {
            resolve()
          }
        }).catch(() => {
          resolve()
        })
      })
    },
    AddHeaderNotice ({ commit }, noticeJson) {
      if (!noticeJson || !noticeJson.title) {
        return
      }
      const noticeArray = vueProps.$localStorage.get(HEADER_NOTICES, [])
      const noticeIdx = noticeArray.findIndex(notice => notice.key === noticeJson.key)
      if (noticeIdx === -1) {
        noticeArray.push(noticeJson)
      } else {
        noticeArray[noticeIdx] = noticeJson
      }

      commit('SET_HEADER_NOTICES', noticeArray)
    },
    ProjectView ({ commit }, projectid) {
      return new Promise((resolve, reject) => {
        api('listApis', { projectid: projectid }).then(response => {
          const apis = {}
          const apiList = response.listapisresponse.api
          for (var idx = 0; idx < apiList.length; idx++) {
            const api = apiList[idx]
            const apiName = api.name
            apis[apiName] = {
              params: api.params,
              response: api.response
            }
          }
          commit('SET_APIS', apis)
          resolve(apis)
          store.dispatch('GenerateRoutes', { apis }).then(() => {
            store.getters.addRouters.map(route => {
              router.addRoute(route)
            })
          })
        }).catch(error => {
          reject(error)
        })
      })
    },
    RefreshFeatures ({ commit }) {
      return new Promise((resolve, reject) => {
        api('listCapabilities').then(response => {
          const result = response.listcapabilitiesresponse.capability
          resolve(result)
          commit('SET_FEATURES', result)
        }).catch(error => {
          reject(error)
        })
      })
    },
    UpdateConfiguration ({ commit }) {
      return new Promise((resolve, reject) => {
        api('listLdapConfigurations').then(response => {
          const ldapEnable = (response.ldapconfigurationresponse.count > 0)
          commit('SET_LDAP', ldapEnable)
        }).catch(error => {
          reject(error)
        })
      })
    },
    SetDomainStore ({ commit }, domainStore) {
      commit('SET_DOMAIN_STORE', domainStore)
    },
    SetDarkMode ({ commit }, darkMode) {
      commit('SET_DARK_MODE', darkMode)
    },
    SetLoginFlag ({ commit }, loggedIn) {
      commit('SET_LOGIN_FLAG', loggedIn)
    }
  }
}

export default user
