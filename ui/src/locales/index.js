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
import VueI18n from 'vue-i18n'

const loadedLanguage = []
const messages = {}

Vue.use(VueI18n)

export const i18n = new VueI18n({
  locale: Vue.ls ? Vue.ls.get('LOCALE') || 'en' : 'en',
  fallbackLocale: 'en',
  silentTranslationWarn: true,
  messages: messages
})

export function loadLanguageAsync (lang) {
  if (!lang) {
    lang = Vue.ls ? Vue.ls.get('LOCALE') || 'en' : 'en'
  }
  if (loadedLanguage.includes(lang)) {
    return Promise.resolve(setLanguage(lang))
  }

  return fetch(`locales/${lang}.json`)
    .then(response => response.json())
    .then(json => Promise.resolve(setLanguage(lang, json)))
}

function setLanguage (lang, message) {
  if (i18n) {
    i18n.locale = lang

    if (message && Object.keys(message).length > 0) {
      i18n.setLocaleMessage(lang, message)
    }
  }

  if (!loadedLanguage.includes(lang)) {
    loadedLanguage.push(lang)
  }

  if (message && Object.keys(message).length > 0) {
    messages[lang] = message
  }
}
