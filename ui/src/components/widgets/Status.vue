<template>
  <a-tooltip placement="bottom">
    <template slot="title">
      {{ text }}
    </template>
    <a-badge style="display: inline-flex" :title="text" :status="getBadgeStatus(text)" :text="getText()" />
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
    getText () {
      if (this.displayText && this.text) {
        return this.text.charAt(0).toUpperCase() + this.text.slice(1)
      }
      return ''
    },
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
        case 'Disabled':
        case 'Down':
        case 'Error':
        case 'Stopped':
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
/deep/ .ant-badge-status-dot {
  width: 12px;
  height: 12px;
  margin-top: 5px;
}
</style>
