<template>
  <div>
    TODO: Implement agnostic resource settings table etc.

    <list-view
      :columns="columns"
      :items="settings " />
  </div>
</template>

<script>
import ListView from '@/components/widgets/ListView'

export default {
  name: 'ResourceSettingsTab',
  components: {
    ListView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      columns: [],
      settings: []
    }
  },
  methods: {
  }
}
</script>

<style scoped>
</style>
