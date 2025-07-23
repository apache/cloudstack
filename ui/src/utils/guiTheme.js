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
import { getAPI } from '@/api'

export async function applyCustomGuiTheme (accountid, domainid) {
  await fetch('config.json').then(response => response.json()).then(config => {
    vueProps.$config = config
  })

  let guiTheme

  if (accountid != null) {
    guiTheme = await fetchGuiTheme({ accountid: accountid })
  }

  if (guiTheme === undefined && domainid != null) {
    guiTheme = await fetchGuiTheme({ domainid: domainid })
  }

  if (guiTheme === undefined) {
    guiTheme = await fetchGuiTheme({ commonname: window.location.hostname })
  }

  if (guiTheme === undefined) {
    guiTheme = await fetchGuiTheme({ listonlydefaulttheme: true })
  }

  await applyDynamicCustomization(guiTheme)
}

async function fetchGuiTheme (params) {
  return await getAPI('listGuiThemes', params).then(response => {
    if (response.listguithemesresponse.guiThemes) {
      return response.listguithemesresponse.guiThemes[0]
    }
  }).catch(error => {
    console.error('Error fetching GUI theme:', error)
    return null
  })
}

async function applyDynamicCustomization (response) {
  let jsonConfig

  if (response?.jsonconfiguration) {
    jsonConfig = JSON.parse(response?.jsonconfiguration)
  }

  // Sets custom GUI fields only if is not nullish.
  vueProps.$config.appTitle = jsonConfig?.appTitle ?? vueProps.$config.appTitle
  vueProps.$config.footer = jsonConfig?.footer ?? vueProps.$config.footer
  vueProps.$config.loginFooter = jsonConfig?.loginFooter ?? vueProps.$config.loginFooter
  vueProps.$config.logo = jsonConfig?.logo ?? vueProps.$config.logo
  vueProps.$config.minilogo = jsonConfig?.minilogo ?? vueProps.$config.minilogo
  vueProps.$config.banner = jsonConfig?.banner ?? vueProps.$config.banner

  if (jsonConfig?.error) {
    vueProps.$config.error[403] = jsonConfig?.error[403] ?? vueProps.$config.error[403]
    vueProps.$config.error[404] = jsonConfig?.error[404] ?? vueProps.$config.error[404]
    vueProps.$config.error[500] = jsonConfig?.error[500] ?? vueProps.$config.error[500]
  }

  if (jsonConfig?.plugins) {
    jsonConfig.plugins.forEach(plugin => {
      vueProps.$config.plugins.push(plugin)
    })
  }

  vueProps.$config.favicon = jsonConfig?.favicon ?? vueProps.$config.favicon
  vueProps.$config.css = response?.css ?? null

  await applyStaticCustomization(vueProps.$config.favicon, vueProps.$config.css)
}

async function applyStaticCustomization (favicon, css) {
  document.getElementById('favicon').href = favicon

  let style = document.getElementById('guiThemeCSS')
  if (style != null) {
    style.innerHTML = css
  } else {
    style = document.createElement('style')
    style.setAttribute('id', 'guiThemeCSS')
    style.innerHTML = css
    document.body.appendChild(style)
  }
}
