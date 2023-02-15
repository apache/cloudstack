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
      v-if="!isDisabled()"
      class="project-select"
      :loading="loading"
      v-model:value="projectSelected"
      :filterOption="filterProject"
      @change="changeProject"
      @focus="fetchData"
      showSearch>

      <template #suffixIcon>
        <a-tooltip placement="bottom">
          <template #title>
            <span>{{ $t('label.projects') }}</span>
          </template>
          <span class="custom-suffix-icon">
            <ProjectOutlined v-if="!loading" class="ant-select-suffix" />
            <LoadingOutlined v-else />
          </span>
        </a-tooltip>
      </template>

      <a-select-option
        v-for="(project, index) in projects"
        :key="index"
        :label="project.displaytext || project.name">
        <span>
          <resource-icon v-if="project.icon && project.icon.base64image" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
          <project-outlined v-else style="margin-right: 5px" />
          {{ project.displaytext || project.name }}
        </span>
      </a-select-option>
    </a-select>
  </span>
</template>

<script>
import store from '@/store'
import { api } from '@/api'
import _ from 'lodash'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ProjectMenu',
  components: {
    ResourceIcon
  },
  data () {
    return {
      projects: [],
      loading: false
    }
  },
  created () {
    this.fetchData()
  },
  computed: {
    projectSelected () {
      let projectIndex = 0
      if (this.$store.getters?.project?.id) {
        projectIndex = this.projects.findIndex(project => project.id === this.$store.getters.project.id)
      }

      return projectIndex
    }
  },
  methods: {
    fetchData () {
      if (this.isDisabled()) {
        return
      }
      var page = 1
      const projects = []
      const getNextPage = () => {
        this.loading = true
        api('listProjects', { listAll: true, details: 'min', page: page, pageSize: 500, showIcon: true }).then(json => {
          if (json?.listprojectsresponse?.project) {
            projects.push(...json.listprojectsresponse.project)
          }
          if (projects.length < json.listprojectsresponse.count) {
            page++
            getNextPage()
          }
        }).finally(() => {
          this.projects = _.orderBy(projects, ['displaytext'], ['asc'])
          this.projects.unshift({ name: this.$t('label.default.view') })
          this.loading = false
        })
      }
      getNextPage()
    },
    isDisabled () {
      return !Object.prototype.hasOwnProperty.call(store.getters.apis, 'listProjects')
    },
    changeProject (index) {
      const project = this.projects[index]
      this.$store.dispatch('ProjectView', project.id)
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id === undefined ? 'light' : 'dark')
      this.$message.success(`${this.$t('message.switch.to')} "${project.displaytext || project.name}"`)
      if (this.$route.name !== 'dashboard') {
        this.$router.push({ name: 'dashboard' })
      }
    },
    filterProject (input, option) {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
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

.custom-suffix-icon {
  font-size: 20px;
  position: absolute;
  top: 0;
  right: 1px;
  margin-top: -5px;
}
</style>
