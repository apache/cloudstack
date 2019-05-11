<template>
  <div class="page-header-index-wide">
    <div v-if="showCapacityDashboard && !project">
      <capacity-dashboard/>
    </div>
    <div v-else>
      <usage-dashboard/>
    </div>
  </div>
</template>

<script>
import store from '@/store'
import { mapState } from 'vuex'
import CapacityDashboard from './CapacityDashboard'
import UsageDashboard from './UsageDashboard'

export default {
  name: 'Dashboard',
  components: {
    CapacityDashboard,
    UsageDashboard
  },
  data () {
    return {
      showCapacityDashboard: false,
      project: false
    }
  },
  computed: mapState(['project']),
  mounted () {
    this.showCapacityDashboard = store.getters.apis.hasOwnProperty('listCapacity')
    this.project = store.getters.project !== undefined && store.getters.project.id !== undefined
    this.$store.watch(
      (state, getters) => getters.project,
      (newValue, oldValue) => {
        if (newValue === undefined || newValue.id === undefined) {
          this.project = false
        } else {
          this.project = true
        }
      }
    )
  }
}
</script>
