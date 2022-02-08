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
import axios from 'axios'
import router from '@/router'
import { VueAxios } from './axios'
import notification from 'ant-design-vue/es/notification'
import { CURRENT_PROJECT } from '@/store/mutation-types'
import { i18n } from '@/locales'
import store from '@/store'

let source
const service = axios.create({
  timeout: 600000
})

const err = (error) => {
  const response = error.response
  let countNotify = store.getters.countNotify
  if (response) {
    console.log(response)
    if (response.status === 403) {
      const data = response.data
      countNotify++
      store.commit('SET_COUNT_NOTIFY', countNotify)
      notification.error({
        top: '65px',
        message: i18n.t('label.forbidden'),
        description: data.message,
        onClose: () => {
          let countNotify = store.getters.countNotify
          countNotify > 0 ? countNotify-- : countNotify = 0
          store.commit('SET_COUNT_NOTIFY', countNotify)
        }
      })
    }
    if (response.status === 401) {
      if (response.config && response.config.params && ['listIdps', 'cloudianIsEnabled'].includes(response.config.params.command)) {
        return
      }
      for (const key in response.data) {
        if (key.includes('response')) {
          if (response.data[key].errortext.includes('not available for user')) {
            countNotify++
            store.commit('SET_COUNT_NOTIFY', countNotify)
            notification.error({
              top: '65px',
              message: 'Error',
              description: response.data[key].errortext + ' ' + i18n.t('error.unable.to.proceed'),
              duration: 0,
              onClose: () => {
                let countNotify = store.getters.countNotify
                countNotify > 0 ? countNotify-- : countNotify = 0
                store.commit('SET_COUNT_NOTIFY', countNotify)
              }
            })
            return
          }
        }
      }
      countNotify++
      store.commit('SET_COUNT_NOTIFY', countNotify)
      notification.error({
        top: '65px',
        message: i18n.t('label.unauthorized'),
        description: i18n.t('message.authorization.failed'),
        key: 'http-401',
        duration: 0,
        onClose: () => {
          let countNotify = store.getters.countNotify
          countNotify > 0 ? countNotify-- : countNotify = 0
          store.commit('SET_COUNT_NOTIFY', countNotify)
        }
      })
      store.dispatch('Logout').then(() => {
        if (router.history.current.path !== '/user/login') {
          router.push({ path: '/user/login', query: { redirect: router.history.current.fullPath } })
        }
      })
    }
    if (response.status === 404) {
      countNotify++
      store.commit('SET_COUNT_NOTIFY', countNotify)
      notification.error({
        top: '65px',
        message: i18n.t('label.not.found'),
        description: i18n.t('message.resource.not.found'),
        onClose: () => {
          let countNotify = store.getters.countNotify
          countNotify > 0 ? countNotify-- : countNotify = 0
          store.commit('SET_COUNT_NOTIFY', countNotify)
        }
      })
      router.push({ path: '/exception/404' })
    }
  }
  if (error.isAxiosError && !error.response) {
    countNotify++
    store.commit('SET_COUNT_NOTIFY', countNotify)
    notification.warn({
      top: '65px',
      message: error.message || i18n.t('message.network.error'),
      description: i18n.t('message.network.error.description'),
      key: 'network-error',
      onClose: () => {
        let countNotify = store.getters.countNotify
        countNotify > 0 ? countNotify-- : countNotify = 0
        store.commit('SET_COUNT_NOTIFY', countNotify)
      }
    })
  }
  return Promise.reject(error)
}

// request interceptor
service.interceptors.request.use(config => {
  source = sourceToken.getSource()
  config.cancelToken = source.token
  if (config && config.params) {
    config.params.response = 'json'
    const project = Vue.ls.get(CURRENT_PROJECT)
    if (!config.params.projectid && project && project.id) {
      if (config.params.command === 'listTags') {
        config.params.projectid = '-1'
      } else {
        config.params.projectid = project.id
      }
    }
  }
  return config
}, err)

// response interceptor
service.interceptors.response.use((response) => {
  return response.data
}, err)

const installer = {
  vm: {},
  install (Vue) {
    Vue.use(VueAxios, service)
  }
}

const sourceToken = {
  init: () => { source = axios.CancelToken.source() },
  isCancel: (e) => {
    return axios.isCancel(e)
  },
  getSource: () => {
    if (!source) sourceToken.init()
    return source
  },
  cancel: () => {
    if (!source) sourceToken.init()
    source.cancel()
  }
}

export {
  installer as VueAxios,
  service as axios,
  sourceToken
}
