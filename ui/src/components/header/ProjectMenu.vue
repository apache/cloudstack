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
    <infinite-scroll-select
      v-if="!isDisabled"
      v-model:value="selectedProjectId"
      class="project-select"
      api="listProjects"
      :apiParams="projectsApiParams"
      resourceType="project"
      :defaultOption="defaultOption"
      defaultIcon="project-outlined"
      :pageSize="100"
      :selectFirstOption="shouldHideDefaultView"
      @change-option="changeProject"/>
  </span>
</template>

<script>
import { getAPI } from '@/api'
import InfiniteScrollSelect from '@/components/widgets/InfiniteScrollSelect'
import eventBus from '@/config/eventBus'

export default {
  name: 'ProjectMenu',
  components: {
    InfiniteScrollSelect
  },
  data () {
    return {
      selectedProjectId: null,
      availableProjects: [],
      timestamp: new Date().getTime()
    }
  },
  created () {
    this.listUserAvailableProjects()
    this.selectedProjectId = this.$store.getters?.project?.id || this.defaultOption?.id
    this.$store.dispatch('ToggleTheme', this.selectedProjectId ? 'dark' : 'light')
  },
  computed: {
    shouldHideDefaultView () {
      return this.$store.getters.features.disabledefaultview && this.availableProjects.length > 0
    },
    isDisabled () {
      return !('listProjects' in this.$store.getters.apis)
    },
    defaultOption () {
      const isDefaultViewDisabled = this.shouldHideDefaultView

      if (isDefaultViewDisabled) {
        return null
      }

      return { id: 0, name: this.$t('label.default.view') }
    },
    projectsApiParams () {
      return {
        details: 'min',
        listall: true,
        timestamp: this.timestamp
      }
    }
  },
  mounted () {
    this.unwatchProject = this.$store.watch(
      (state, getters) => getters.project?.id,
      (newId) => {
        this.selectedProjectId = newId
      }
    )
    eventBus.on('projects-updated', () => {
      this.timestamp = new Date().getTime()
      this.listUserAvailableProjects()
    })
  },
  beforeUnmount () {
    if (this.unwatchProject) {
      this.unwatchProject()
    }
  },
  methods: {
    changeProject (project) {
      this.$store.dispatch('ProjectView', project.id)
      this.$store.dispatch('SetProject', project)
      this.$store.dispatch('ToggleTheme', project.id ? 'dark' : 'light')
      this.$message.success(`${this.$t('message.switch.to')} "${project.displaytext || project.name}"`)
      if (this.$route.name !== 'dashboard') {
        this.$router.push({ name: 'dashboard' })
      }
    },
    listUserAvailableProjects () {
      getAPI('listProjects', { details: 'min', listall: true })
        .then((response) => {
          this.availableProjects = response.listprojectsresponse?.project || []
          if (this.shouldHideDefaultView && !this.$store.getters.project?.id) {
            this.selectedProjectId = null
          }
        })
        .catch((error) => {
          this.$notifyError(error)
        })
    }
  }
}
</script>

<style lang="less" scoped>
.project {
  &-select {
    width: 27vw;
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
