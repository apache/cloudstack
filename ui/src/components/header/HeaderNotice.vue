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
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottom"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true"
    overlayClassName="header-notice-popover">
    <template slot="content">
      <a-spin :spinning="loading">
        <a-list style="min-width: 200px; max-width: 300px">
          <a-list-item>
            <a-list-item-meta :title="$t('label.notifications')">
              <a-avatar :style="{backgroundColor: '#6887d0', verticalAlign: 'middle'}" icon="notification" slot="avatar"/>
              <a-button size="small" slot="description" @click="clearJobs">{{ $t('label.clear.list') }}</a-button>
            </a-list-item-meta>
          </a-list-item>
          <a-list-item v-for="(job, index) in jobs" :key="index">
            <a-list-item-meta :title="job.title" :description="job.description">
              <a-avatar :style="notificationAvatar[job.status].style" :icon="notificationAvatar[job.status].icon" slot="avatar"/>
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
import { api } from '@/api'
import store from '@/store'

export default {
  name: 'HeaderNotice',
  data () {
    return {
      loading: false,
      visible: false,
      jobs: [],
      poller: null,
      notificationAvatar: {
        done: { icon: 'check-circle', style: 'backgroundColor:#87d068' },
        progress: { icon: 'loading', style: 'backgroundColor:#ffbf00' },
        failed: { icon: 'close-circle', style: 'backgroundColor:#f56a00' }
      }
    }
  },
  methods: {
    showNotifications () {
      this.visible = !this.visible
    },
    clearJobs () {
      this.jobs = this.jobs.filter(x => x.status === 'progress')
      this.$store.commit('SET_ASYNC_JOB_IDS', this.jobs)
    },
    startPolling () {
      this.poller = setInterval(() => {
        this.pollJobs()
      }, 4000)
    },
    async pollJobs () {
      var hasUpdated = false
      for (var i in this.jobs) {
        if (this.jobs[i].status === 'progress') {
          await api('queryAsyncJobResult', { jobid: this.jobs[i].jobid }).then(json => {
            var result = json.queryasyncjobresultresponse
            if (result.jobstatus === 1 && this.jobs[i].status !== 'done') {
              hasUpdated = true
              const title = this.jobs[i].title
              const description = this.jobs[i].description
              this.$message.success({
                content: title + (description ? ' - ' + description : ''),
                key: this.jobs[i].jobid,
                duration: 2
              })
              this.jobs[i].status = 'done'
            } else if (result.jobstatus === 2 && this.jobs[i].status !== 'failed') {
              hasUpdated = true
              this.jobs[i].status = 'failed'
              if (result.jobresult.errortext !== null) {
                this.jobs[i].description = '(' + this.jobs[i].description + ') ' + result.jobresult.errortext
              }
              this.$notification.error({
                message: this.jobs[i].title,
                description: this.jobs[i].description,
                key: this.jobs[i].jobid,
                duration: 0
              })
            }
          }).catch(function (e) {
            console.log(this.$t('error.fetching.async.job.result') + e)
          })
        }
      }
      if (hasUpdated) {
        this.$store.commit('SET_ASYNC_JOB_IDS', this.jobs.reverse())
      }
    }
  },
  beforeDestroy () {
    clearInterval(this.poller)
  },
  created () {
    this.startPolling()
  },
  mounted () {
    this.jobs = (store.getters.asyncJobIds || []).reverse()
    this.$store.watch(
      (state, getters) => getters.asyncJobIds,
      (newValue, oldValue) => {
        if (oldValue !== newValue && newValue !== undefined) {
          this.jobs = newValue.reverse()
        }
      }
    )
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
