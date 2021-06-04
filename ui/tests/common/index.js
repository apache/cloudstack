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

import mockI18n from '../mock/mockI18n'
import mockStore from '../mock/mockStore'
import mockRouter from '../mock/mockRouter'

import localVue from '../setup'
import { mount } from '@vue/test-utils'
import { pollJobPlugin, notifierPlugin, configUtilPlugin } from '@/utils/plugins'

localVue.use(pollJobPlugin)
localVue.use(notifierPlugin)
localVue.use(configUtilPlugin)

function createMockRouter (newRoutes = []) {
  let routes = []
  if (!newRoutes || Object.keys(newRoutes).length === 0) {
    return mockRouter.mock(routes)
  }

  routes = [...newRoutes]

  return mockRouter.mock(routes)
}

function createMockI18n (locale = 'en', messages = {}) {
  return mockI18n.mock(locale, messages)
}

function createMockStore (state = {}, actions = {}) {
  return mockStore.mock(state, actions)
}

function decodeHtml (html) {
  const text = document.createElement('textarea')
  text.innerHTML = html

  return text.value
}

function createFactory (component, options) {
  var {
    router = null,
    i18n = null,
    store = null,
    props = {},
    data = {},
    mocks = {}
  } = options

  if (!router) router = createMockRouter()
  if (!i18n) i18n = createMockI18n()
  if (!store) store = createMockStore()

  return mount(component, {
    localVue,
    router,
    i18n,
    store,
    propsData: props,
    mocks,
    data () {
      return { ...data }
    }
  })
}

export default {
  createFactory,
  createMockRouter,
  createMockI18n,
  createMockStore,
  decodeHtml
}
