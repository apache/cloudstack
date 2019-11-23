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

<template>
  <a-popover
    class="project"
    v-model="visible"
    trigger="click"
    placement="bottom"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true">
    <template slot="content">
      <a-menu style="margin: -12px -16px">
        <a-menu-item>
          <a-icon class="project-icon" type="login" />
          <a-select
            class="project-select"
            size="default"
            defaultValue="Default View"
            :value="selectedProject"
            :disabled="isDisabled()"
            :filterOption="filterProject"
            @change="changeProject"
            showSearch>
            <a-select-option v-for="(project, index) in projects" :key="index">
              {{ project.displaytext || project.name }}
            </a-select-option>
          </a-select>
        </a-menu-item>
        <a-menu-item>
          <router-link :to="{ path: '/project' }">
            <a-icon class="project-icon" type="project" />
            {{ $t('Projects') }}
          </router-link>
        </a-menu-item>
      </a-menu>
    </template>
    <span @click="visible = !visible" class="header-notice-opener">
      <a-icon class="project-icon" type="project" />
    </span>
  </a-popover>
</template>

<script>
import Vue from 'vue'
import { api } from '@/api'
import store from '@/store'
import { CURRENT_PROJECT } from '@/store/mutation-types'

export default {
  name: 'ProjectMenu',
  data () {
    return {
      visible: false,
      projects: [],
      selectedProject: 'Default View'
    }
  },
  mounted () {
    this.fetchData()
  },
  computed: {
  },
  methods: {
    fetchData () {
      if (this.isDisabled()) {
        return
      }
      // TODO: refactor fetching project list for project selector
      this.projects = []
      var page = 1
      const getNextPage = () => {
        api('listProjects', { listAll: true, details: 'min', page: page, pageSize: 500 }).then(json => {
          if (this.projects.length === 0) {
            this.projects.push({ name: 'Default View' })
          }
          if (json && json.listprojectsresponse && json.listprojectsresponse.project) {
            this.projects.push(...json.listprojectsresponse.project)
          }
          const currentProject = Vue.ls.get(CURRENT_PROJECT)
          for (var project of this.projects) {
            if (project.id === currentProject.id) {
              this.setSelectedProject(project)
              break
            }
          }
          if (this.projects.length - 1 < json.listprojectsresponse.count) {
            page++
            getNextPage()
          }
        })
      }
      getNextPage()
    },
    isDisabled () {
      return !Object.prototype.hasOwnProperty.call(store.getters.apis, 'listProjects')
    },
    setSelectedProject (project) {
      this.selectedProject = project.displaytext || project.name
    },
    changeProject (index) {
      const project = this.projects[index]
      this.setSelectedProject(project)
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
      this.$message.success(`Switched to "${project.name}"`)
      this.$router.push({ name: 'dashboard' })
    },
    filterProject (input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  }
}
</script>

<style lang="less" scoped>
.project {
  &-select {
    width: 200px;
  }

  &-icon {
    font-size: 20px;
    line-height: 1;
    padding-top: 5px;
    padding-right: 5px;
  }
}
</style>
