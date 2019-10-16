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

import Vue from 'vue'
import md5 from 'md5'
import { login, logout, api } from '@/api'
import { ACCESS_TOKEN, CURRENT_PROJECT, ASYNC_JOB_IDS } from '@/store/mutation-types'
import { welcome } from '@/utils/util'

const user = {
  state: {
    token: '',
    name: '',
    welcome: '',
    avatar: '',
    info: {},
    apis: {},
    project: {},
    asyncJobIds: []
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
    SET_ASYNC_JOB_IDS: (state, jobsJsonArray) => {
      Vue.ls.set(ASYNC_JOB_IDS, jobsJsonArray)
      state.asyncJobIds = jobsJsonArray
    }
  },

  actions: {
    SetProject ({ commit }, project) {
      commit('SET_PROJECT', project)
    },
    Login ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        login(userInfo).then(response => {
          const result = response.loginresponse

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
        // Discover allowed APIs
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

        // Find user info
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
      })
    },
    Logout ({ commit, state }) {
      return new Promise((resolve) => {
        commit('SET_TOKEN', '')
        commit('SET_PROJECT', {})
        commit('SET_APIS', {})
        Vue.ls.remove(CURRENT_PROJECT)
        Vue.ls.remove(ACCESS_TOKEN)
        Vue.ls.remove(ASYNC_JOB_IDS)

        logout(state.token).then(() => {
          resolve()
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
