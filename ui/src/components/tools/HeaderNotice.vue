<template>
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottom"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true"
    overlayClassName="header-notice-popover">
    <template slot="content">
      <a-spin :spinning="loading">
        <a-list>
          <a-list-item>
            <a-list-item-meta title="Notifications">
              <a-avatar :style="{backgroundColor: '#6887d0', verticalAlign: 'middle'}" icon="notification" slot="avatar"/>
              <a-button size="small" slot="description" @click="clearJobs">Clear All</a-button>
            </a-list-item-meta>
          </a-list-item>
          <a-list-item v-for="(job, index) in jobs" :key="index">
            <a-list-item-meta :title="job.title" :description="job.description">
              <a-avatar :style="job.style" :icon="job.icon" slot="avatar"/>
            </a-list-item-meta>
          </a-list-item>
        </a-list>
      </a-spin>
    </template>
    <span @click="showNotifications" class="header-notice-opener">
      <a-badge :count="jobs.length">
        <a-icon class="header-notice-icon" type="bell" />
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
export default {
  name: 'HeaderNotice',
  data () {
    return {
      loading: false,
      visible: false,
      jobs: []
    }
  },
  methods: {
    showNotifications () {
      this.visible = !this.visible
      this.jobs.push({ 'title': 'Start VM', description: 'VM Deployment', icon: 'check-circle', status: 'done', style: 'backgroundColor:#87d068' })
      this.jobs.push({ 'title': 'Start VM', description: 'VM Deployment', icon: 'loading', status: 'progress', style: 'backgroundColor:#ffbf00' })
      this.jobs.push({ 'title': 'Start VM', description: 'VM Deployment', icon: 'close-circle', status: 'failed', style: 'backgroundColor:#f56a00' })
    },
    clearJobs () {
      this.visible = false
      this.jobs = []
    }
  }
}
</script>

<style lang="less" scoped>
  .header-notice {
    display: inline-block;
    transition: all 0.3s;

    &-popover {
      top: 50px !important;
      width: 300px;
      top: 50px;
    }

    &-opener {
      display: inline-block;
      transition: all 0.3s;
      vertical-align: initial;
    }

    &-icon {
      font-size: 18px;
      padding: 4px;
    }
  }
</style>
