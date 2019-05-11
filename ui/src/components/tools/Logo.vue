<template>
  <div class="logo">
    <img src="../../assets/logo.png"/>
    <project-menu></project-menu>
  </div>
</template>

<script>
import ProjectMenu from '../tools/ProjectMenu'

export default {
  name: 'Logo',
  components: {
    ProjectMenu
  },
  props: {
    title: {
      type: String,
      default: 'CloudStack',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  }
}
</script>

<style>
  .logo img {
    margin-right: 10px;
  }
</style>
