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
import App from './App.vue'
import router from './router'
import store from './store'
import { i18n, loadLanguageAsync } from './locales'

import bootstrap from './core/bootstrap'
import './core/lazy_use'
import './core/ext'
import './permission' // permission control
import './utils/filter' // global filter
import { pollJobPlugin, notifierPlugin, toLocaleDatePlugin, configUtilPlugin } from './utils/plugins'
import { VueAxios } from './utils/request'

Vue.config.productionTip = false
Vue.use(VueAxios, router)
Vue.use(pollJobPlugin)
Vue.use(notifierPlugin)
Vue.use(toLocaleDatePlugin)

fetch('config.json').then(response => response.json()).then(config => {
  Vue.prototype.$config = config
  Vue.axios.defaults.baseURL = config.apiBase

  loadLanguageAsync().then(() => {
    new Vue({
      router,
      store,
      i18n,
      created: bootstrap,
      render: h => h(App)
    }).$mount('#app')
  })
})

Vue.use(configUtilPlugin)
