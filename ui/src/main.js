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

import { createApp } from 'vue'
import StoragePlugin from 'vue-web-storage'

import App from './App.vue'
import router from './router'
import store from './store'
import { i18n, loadLanguageAsync } from './locales'

import bootstrap from './core/bootstrap'
import lazyUsePlugs from './core/lazy_use'
import permission from './permission' // permission control
import filter from './utils/filter' // global filter

import { pollJobPlugin, notifierPlugin, toLocaleDatePlugin, configUtilPlugin } from './utils/plugins'
import { VueAxios } from './utils/request'
import setting from '@/config/settings'

const app = createApp(App)

app.config.productionTip = false
app.use(VueAxios, router)
app.use(pollJobPlugin)
app.use(permission)
app.use(notifierPlugin)
app.use(toLocaleDatePlugin)
app.use(configUtilPlugin)
app.use(filter)

fetch('config.json').then(response => response.json()).then(config => {
  app.config.globalProperties.$config = config
  app.use(StoragePlugin, setting.storageOptions)
  // set global localStorage for using
  window.ls = app.config.globalProperties.$localStorage
  window.appPrototype = app.config.globalProperties

  loadLanguageAsync().then(() => {
    app.use(store)
      .use(lazyUsePlugs)
      .use(router)
      .use(i18n)
      .use(bootstrap)
      .mount('#app')

    app.config.globalProperties.axios.defaults.baseURL = config.apiBase
  })
})
