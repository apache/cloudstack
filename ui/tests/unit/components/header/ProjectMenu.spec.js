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

import { mount, flushPromises } from '@vue/test-utils'
import { createStore } from 'vuex'

import common from '../../../common'
import ProjectMenu from '@/components/header/ProjectMenu.vue'

const i18n = common.createMockI18n('en', {
  en: { 'label.default.view': 'Default view', 'message.switch.to': 'Switched to' }
})
const project = { id: 'project-1', name: 'Project 1', displaytext: 'Project 1' }
let store
let actions

const createStoreWithProject = (current) => {
  actions = {
    ProjectView: jest.fn(),
    ToggleTheme: jest.fn()
  }
  return createStore({
    state: { user: { project: current, apis: { listProjects: {} } } },
    getters: {
      project: state => state.user.project,
      apis: state => state.user.apis
    },
    mutations: {
      SET_PROJECT: (state, project) => { state.user.project = project }
    },
    actions: {
      ...actions,
      SetProject: ({ commit }, project) => commit('SET_PROJECT', project)
    }
  })
}

const mountMenu = () => mount(ProjectMenu, {
  global: {
    plugins: [store, i18n],
    stubs: { InfiniteScrollSelect: true },
    mocks: {
      $message: { success: jest.fn() },
      $route: { name: 'dashboard' },
      $router: { push: jest.fn() }
    }
  }
})

const selectOption = async (wrapper, option) => {
  wrapper.findComponent({ name: 'InfiniteScrollSelect' }).vm.$emit('change-option', option)
  await flushPromises()
}

describe('Components > Header > ProjectMenu.vue', () => {
  it('stores the selected project', async () => {
    store = createStoreWithProject({})
    const wrapper = mountMenu()

    await selectOption(wrapper, project)

    expect(store.getters.project).toEqual(project)
    expect(wrapper.vm.selectedProjectId).toBe(project.id)
    wrapper.unmount()
  })

  it('stores no project when switching back to Default view', async () => {
    store = createStoreWithProject(project)
    const wrapper = mountMenu()
    expect(wrapper.vm.selectedProjectId).toBe(project.id)

    await selectOption(wrapper, wrapper.vm.defaultOption)

    expect(store.getters.project).toEqual({})
    expect(store.getters.project.id).toBeUndefined()
    expect(wrapper.vm.selectedProjectId).toBe(wrapper.vm.defaultOption.id)
    expect(actions.ToggleTheme).toHaveBeenLastCalledWith(expect.anything(), 'light')
    wrapper.unmount()
  })
})
