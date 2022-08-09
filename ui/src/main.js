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

import { vueApp, vueProps } from './vue-app'
import router from './router'
import store from './store'
import { i18n, loadLanguageAsync } from './locales'

import bootstrap from './core/bootstrap'
import './core/lazy_use'
import extensions from './core/ext'
import './permission' // permission control
import './utils/filter' // global filter
import {
  pollJobPlugin,
  notifierPlugin,
  toLocaleDatePlugin,
  configUtilPlugin,
  apiMetaUtilPlugin,
  showIconPlugin,
  resourceTypePlugin,
  fileSizeUtilPlugin,
  genericUtilPlugin,
  localesPlugin
} from './utils/plugins'
import { VueAxios } from './utils/request'
import directives from './utils/directives'

vueApp.use(VueAxios, router)
vueApp.use(pollJobPlugin)
vueApp.use(notifierPlugin)
vueApp.use(toLocaleDatePlugin)
vueApp.use(configUtilPlugin)
vueApp.use(apiMetaUtilPlugin)
vueApp.use(showIconPlugin)
vueApp.use(resourceTypePlugin)
vueApp.use(fileSizeUtilPlugin)
vueApp.use(localesPlugin)
vueApp.use(genericUtilPlugin)
vueApp.use(extensions)
vueApp.use(directives)

fetch('config.json').then(response => response.json()).then(config => {
  vueProps.$config = config
  let basUrl = config.apiBase
  if (config.multipleServer) {
    basUrl = (config.servers[0].apiHost || '') + config.servers[0].apiBase
  }

  vueProps.axios.defaults.baseURL = basUrl

  loadLanguageAsync().then(() => {
    vueApp.use(store)
      .use(router)
      .use(i18n)
      .use(bootstrap)
      .mount('#app')
  })
})
