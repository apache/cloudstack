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
import Vue from 'vue'
import md5 from 'md5'
import { login, logout, api } from '@/api'
import { ACCESS_TOKEN, CURRENT_PROJECT, DEFAULT_THEME, ASYNC_JOB_IDS } from '@/store/mutation-types'
import { welcome } from '@/utils/util'

const user = {
  state: {
    token: '',
    name: '',
    welcome: '',
    avatar: '',
    info: {},
    apis: {},
    features: {},
    project: {},
    asyncJobIds: [],
    isLdapEnabled: false,
    cloudian: {}
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_PROJECT: (state, project) => {
      Vue.ls.set(CURRENT_PROJECT, project)
      state.project = project
    },
    SET_NAME: (state, { name, welcome }) => {
      state.name = name
      state.welcome = welcome
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_INFO: (state, info) => {
      state.info = info
    },
    SET_APIS: (state, apis) => {
      state.apis = apis
    },
    SET_FEATURES: (state, features) => {
      state.features = features
    },
    SET_ASYNC_JOB_IDS: (state, jobsJsonArray) => {
      Vue.ls.set(ASYNC_JOB_IDS, jobsJsonArray)
      state.asyncJobIds = jobsJsonArray
    },
    SET_LDAP: (state, isLdapEnabled) => {
      state.isLdapEnabled = isLdapEnabled
    },
    SET_CLOUDIAN: (state, cloudian) => {
      state.cloudian = cloudian
    },
    RESET_THEME: (state) => {
      Vue.ls.set(DEFAULT_THEME, 'light')
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

          Cookies.set('account', result.account)
          Cookies.set('domainid', result.domainid)
          Cookies.set('role', result.type)
          Cookies.set('sessionkey', result.sessionkey)
          Cookies.set('timezone', result.timezone)
          Cookies.set('timezoneoffset', result.timezoneoffset)
          Cookies.set('userfullname', result.firstname + ' ' + result.lastname)
          Cookies.set('userid', result.userid)
          Cookies.set('username', result.username)

          Vue.ls.set(ACCESS_TOKEN, result.sessionkey, 60 * 60 * 1000)
          commit('SET_TOKEN', result.sessionkey)
          commit('SET_PROJECT', {})
          commit('SET_ASYNC_JOB_IDS', [])

          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    GetInfo ({ commit }) {
      return new Promise((resolve, reject) => {
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
        }).catch(error => {
          reject(error)
        })

        api('listUsers').then(response => {
          const result = response.listusersresponse.user[0]
          commit('SET_INFO', result)
          commit('SET_NAME', { name: result.firstname + ' ' + result.lastname, welcome: welcome() })
          if ('email' in result) {
            commit('SET_AVATAR', 'https://www.gravatar.com/avatar/' + md5(result.email))
          } else {
            commit('SET_AVATAR', 'https://www.gravatar.com/avatar/' + md5('dev@cloudstack.apache.org'))
          }
        }).catch(error => {
          reject(error)
        })

        api('listCapabilities').then(response => {
          const result = response.listcapabilitiesresponse.capability
          commit('SET_FEATURES', result)
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

        commit('SET_TOKEN', '')
        commit('SET_PROJECT', {})
        commit('SET_APIS', {})
        commit('SET_CLOUDIAN', {})
        commit('RESET_THEME')
        Vue.ls.remove(CURRENT_PROJECT)
        Vue.ls.remove(ACCESS_TOKEN)
        Vue.ls.remove(ASYNC_JOB_IDS)

        logout(state.token).then(() => {
          Cookies.remove('account')
          Cookies.remove('domainid')
          Cookies.remove('role')
          Cookies.remove('sessionkey')
          Cookies.remove('timezone')
          Cookies.remove('timezoneoffset')
          Cookies.remove('userfullname')
          Cookies.remove('userid')
          Cookies.remove('username')

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
    AddAsyncJob ({ commit }, jobJson) {
      var jobsArray = Vue.ls.get(ASYNC_JOB_IDS, [])
      jobsArray.push(jobJson)
      commit('SET_ASYNC_JOB_IDS', jobsArray)
    }
  }
}

export default user
