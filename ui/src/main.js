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

import { createApp, h } from 'vue'
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
  localesPlugin,
  dialogUtilPlugin,
  cpuArchitectureUtilPlugin,
  imagesUtilPlugin
} from './utils/plugins'
import { VueAxios } from './utils/request'
import directives from './utils/directives'
import Cookies from 'js-cookie'
import { api } from '@/api'
import { applyCustomGuiTheme } from './utils/guiTheme'

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
vueApp.use(dialogUtilPlugin)
vueApp.use(cpuArchitectureUtilPlugin)
vueApp.use(imagesUtilPlugin)
vueApp.use(extensions)
vueApp.use(directives)

const renderError = (err) => {
  console.error('Fatal error during app initialization: ', err)
  const ErrorComponent = {
    render: () => h(
      'div',
      { style: 'font-family: sans-serif; text-align: center; padding: 2rem;' },
      [
        h('h2', { style: 'color: #ff4d4f;' }, 'We\'re experiencing a problem'),
        h('p', 'The application could not be loaded due to a configuration issue. Please try again later.'),
        h('details', { style: 'margin-top: 20px;' }, [
          h('summary', { style: 'cursor: pointer;' }, 'Technical details'),
          h('pre', {
            style: 'text-align: left; display: inline-block; margin-top: 10px;'
          }, 'Missing or malformed config.json. Please ensure the file is present, accessible, and contains valid JSON. Check the browser console for more information.')
        ])
      ]
    )
  }
  createApp(ErrorComponent).mount('#app')
}

fetch('config.json?ts=' + Date.now())
  .then(response => {
    if (!response.ok) {
      throw new Error(`Failed to fetch config.json: ${response.status} ${response.statusText}`)
    }
    return response.json()
  })
  .then(async config => {
    vueProps.$config = config
    let baseUrl = config.apiBase
    if (config.multipleServer) {
      baseUrl = (config.servers[0].apiHost || '') + config.servers[0].apiBase
    }

    vueProps.axios.defaults.baseURL = baseUrl

    const userid = Cookies.get('userid')
    let accountid = null
    let domainid = null

    if (userid !== undefined && Cookies.get('sessionkey')) {
      await api('listUsers', { userid: userid }).then(response => {
        accountid = response.listusersresponse.user[0].accountid
        domainid = response.listusersresponse.user[0].domainid
      })
    }

    await applyCustomGuiTheme(accountid, domainid)

    loadLanguageAsync().then(() => {
      vueApp.use(store)
        .use(router)
        .use(i18n)
        .use(bootstrap)
        .mount('#app')
    })
  }).catch(error => {
    renderError(error)
  })
