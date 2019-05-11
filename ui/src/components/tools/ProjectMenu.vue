<template>
  <span class="project-wrapper" :disabled="true">
    <a-select
      size="default"
      style="width: 75%"
      defaultValue="Default View"
      :value="selectedProject"
      :disabled="isDisabled()"
      :filterOption="filterProject"
      @change="changeProject"
      showSearch>
      <a-select-option v-for="(project, index) in projects" :key="index">
        {{ project.name }}
      </a-select-option>
    </a-select>
  </span>
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
      api('listProjects').then(json => {
        this.projects = [{ name: 'Default View' }]
        if (json && json.listprojectsresponse && json.listprojectsresponse.project) {
          this.projects.push(...json.listprojectsresponse.project)
        }
        const currentProject = Vue.ls.get(CURRENT_PROJECT)
        for (var project of this.projects) {
          if (project.id === currentProject.id) {
            this.selectedProject = project.name
            break
          }
        }
      })
    },
    isDisabled () {
      return !store.getters.apis.hasOwnProperty('listProjects')
    },
    changeProject (index) {
      const project = this.projects[index]
      this.selectedProject = project.name
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
