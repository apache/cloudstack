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
  <span class="header-notice-opener">
    <a-select
      class="project-select"
      defaultValue="Default View"
      :value="$store.getters.project.displaytext || $store.getters.project.name || 'Default View'"
      :disabled="isDisabled()"
      :filterOption="filterProject"
      @change="changeProject"
      @focus="fetchData"
      showSearch>

      <a-tooltip placement="bottom" slot="suffixIcon">
        <template slot="title">
          <span>{{ $t('projects') }}</span>
        </template>
        <a-icon style="font-size: 20px; color: #999; margin-top: -5px" type="project" />
      </a-tooltip>

      <a-select-option v-for="(project, index) in projects" :key="index">
        {{ project.displaytext || project.name }}
      </a-select-option>
    </a-select>
  </span>
</template>

<script>
import store from '@/store'
import { api } from '@/api'

export default {
  name: 'ProjectMenu',
  data () {
    return {
      projects: []
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.isDisabled()) {
        return
      }
      var page = 1
      const getNextPage = () => {
        api('listProjects', { listAll: true, details: 'min', page: page, pageSize: 500 }).then(json => {
          if (page === 1) {
            this.projects = [{ name: 'Default View' }]
          }
          if (json && json.listprojectsresponse && json.listprojectsresponse.project) {
            this.projects.push(...json.listprojectsresponse.project)
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
    changeProject (index) {
      const project = this.projects[index]
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
    width: 30vw;
  }

  &-icon {
    font-size: 20px;
    line-height: 1;
    padding-top: 5px;
    padding-right: 5px;
  }
}
</style>
