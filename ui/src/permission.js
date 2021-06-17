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
import { i18n } from './locales'
import router from './router'
import store from './store'

import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import message from 'ant-design-vue/es/message'
import notification from 'ant-design-vue/es/notification'
import { setDocumentTitle } from '@/utils/domUtil'
import { ACCESS_TOKEN, APIS } from '@/store/mutation-types'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

const allowList = ['login'] // no redirect allowlist

router.beforeEach((to, from, next) => {
  // start progress bar
  NProgress.start()
  if (to.meta && typeof to.meta.title !== 'undefined') {
    const title = i18n.t(to.meta.title) + ' - ' + Vue.prototype.$config.appTitle
    setDocumentTitle(title)
  }
  const validLogin = Vue.ls.get(ACCESS_TOKEN) || Cookies.get('userid') || Cookies.get('userid', { path: '/client' })
  if (validLogin) {
    if (to.path === '/user/login') {
      next({ path: '/dashboard' })
      NProgress.done()
    } else {
      if (Object.keys(store.getters.apis).length === 0) {
        const cachedApis = Vue.ls.get(APIS, {})
        if (Object.keys(cachedApis).length > 0) {
          message.loading(`${i18n.t('label.loading')}...`, 1.5)
        }
        store
          .dispatch('GetInfo')
          .then(apis => {
            store.dispatch('GenerateRoutes', { apis }).then(() => {
              router.addRoutes(store.getters.addRouters)
              const redirect = decodeURIComponent(from.query.redirect || to.path)
              if (to.path === redirect) {
                next({ ...to, replace: true })
              } else {
                next({ path: redirect })
              }
            })
          })
          .catch(() => {
            notification.error({
              message: 'Error',
              description: i18n.t('message.error.discovering.feature'),
              duration: 0
            })
            store.dispatch('Logout').then(() => {
              next({ path: '/user/login', query: { redirect: to.fullPath } })
            })
          })
      } else {
        next()
      }
    }
  } else {
    if (allowList.includes(to.name)) {
      next()
    } else {
      next({ path: '/user/login', query: { redirect: to.fullPath } })
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done() // finish progress bar
})
