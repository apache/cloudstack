<template>
  <a-tooltip placement="bottom">
    <template slot="title">
      {{ text }}
    </template>
    <span>
      <a-badge :title="text" :status="getBadgeStatus(text)" />
      <span v-if="displayText">{{ text }}</span>
    </span>
  </a-tooltip>
</template>

<script>

export default {
  name: 'Status',
  components: {
  },
  props: {
    text: {
      type: String,
      required: true
    },
    displayText: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
    }
  },
  methods: {
    getBadgeStatus (state) {
      var status = 'default'
      switch (state) {
        case 'Running':
        case 'Ready':
        case 'Up':
        case 'BackedUp':
        case 'Allocated':
        case 'Implemented':
        case 'Enabled':
        case 'enabled':
        case 'Active':
        case 'Completed':
        case 'Started':
          status = 'success'
          break
        case 'Stopped':
        case 'Error':
          status = 'error'
          break
        case 'Migrating':
        case 'Starting':
        case 'Stopping':
        case 'Scheduled':
          status = 'processing'
          break
        case 'Alert':
        case 'Created':
          status = 'warning'
          break
      }
      return status
    }
  }
}
</script>

<style scoped>
.ant-badge-status-dot {
  width: 14px;
  height: 14px;
}
</style>
