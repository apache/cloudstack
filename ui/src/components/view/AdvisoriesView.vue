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
  <div>
    <div v-for="advisory in advisories" :key="advisory.id" style="margin-bottom: 10px;">
      <a-alert
        :type="advisory.severity || 'info'"
        :show-icon="true"
        :closable="true"
        :message="$t(advisory.message)"
        @close="onAlertClose(advisory)">
        <template #description>
          <a-space direction="horizontal" size="small">
            <span v-for="(action, idx) in advisory.actions" :key="idx">
              <a-button
                v-if="typeof action.show === 'function' ? action.show($store) : action.show"
                size="small"
                :type="(action.primary || advisory.actions.length === 1) ? 'primary' : 'default'"
                @click="onAlertBtnClick(action, advisory)">
                {{ $t(action.label) }}
              </a-button>
            </span>
          </a-space>
        </template>
      </a-alert>
    </div>
  </div>
</template>

<script>

const DISMISSED_ADVISORIES_KEY = 'dismissed_advisories'

export default {
  name: 'AdvisoriesView',
  components: {
  },
  props: {},
  data () {
    return {
      advisories: []
    }
  },
  created () {
    this.evaluateAdvisories()
  },
  computed: {
  },
  methods: {
    async evaluateAdvisories () {
      this.advisories = []
      const metaAdvisories = this.$route.meta.advisories || []
      const dismissedAdvisories = this.$localStorage.get(DISMISSED_ADVISORIES_KEY) || []
      const advisoryPromises = metaAdvisories.map(async advisory => {
        if (dismissedAdvisories.includes(advisory.id)) {
          return null
        }
        const active = await Promise.resolve(advisory.condition(this.$store))
        if (active) {
          return advisory
        } else if (advisory.dismissOnConditionFail) {
          this.dismissAdvisory(advisory.id, true)
        }
        return null
      })
      const results = await Promise.all(advisoryPromises)
      this.advisories = results.filter(a => a !== null)
    },
    onAlertClose (advisory) {
      this.dismissAdvisory(advisory.id)
    },
    dismissAdvisory (advisoryId, skipUpdateLocal) {
      let dismissedAdvisories = this.$localStorage.get(DISMISSED_ADVISORIES_KEY) || []
      dismissedAdvisories = dismissedAdvisories.filter(id => id !== advisoryId)
      dismissedAdvisories.push(advisoryId)
      this.$localStorage.set(DISMISSED_ADVISORIES_KEY, dismissedAdvisories)
      if (skipUpdateLocal) {
        return
      }
      this.advisories = this.advisories.filter(advisory => advisory.id !== advisoryId)
    },
    undismissAdvisory (advisory, evaluate) {
      let dismissedAdvisories = this.$localStorage.get(DISMISSED_ADVISORIES_KEY) || []
      dismissedAdvisories = dismissedAdvisories.filter(id => id !== advisory.id)
      this.$localStorage.set(DISMISSED_ADVISORIES_KEY, dismissedAdvisories)
      if (evaluate) {
        Promise.resolve(advisory.condition(this.$store)).then(active => {
          if (active) {
            this.advisories.push(advisory)
          }
        })
      } else {
        this.advisories.push(advisory)
      }
    },
    handleAdvisoryActionError (action, advisory, evaluate) {
      if (action.errorMessage) {
        this.showActionMessage('error', advisory.id, action.errorMessage)
      }
      this.undismissAdvisory(advisory, evaluate)
    },
    handleAdvisoryActionResult (action, advisory, result) {
      if (result && action.successMessage) {
        this.showActionMessage('success', advisory.id, action.successMessage)
        return
      }
      this.handleAdvisoryActionError(action, advisory, false)
    },
    showActionMessage (type, key, content) {
      const data = {
        content: this.$t(content),
        key: key,
        duration: type === 'loading' ? 0 : 3
      }
      if (type === 'loading') {
        this.$message.loading(data)
      } else if (type === 'success') {
        this.$message.success(data)
      } else if (type === 'error') {
        this.$message.error(data)
      } else {
        this.$message.info(data)
      }
    },
    onAlertBtnClick (action, advisory) {
      this.dismissAdvisory(advisory.id)
      if (typeof action.run !== 'function') {
        return
      }
      if (action.loadingLabel) {
        this.showActionMessage('loading', advisory.id, action.loadingLabel)
      }
      const result = action.run(this.$store, this.$router)
      if (result instanceof Promise) {
        result.then(success => {
          this.handleAdvisoryActionResult(action, advisory, success)
        }).catch(() => {
          this.handleAdvisoryActionError(action, advisory, true)
        })
      } else {
        this.handleAdvisoryActionResult(action, advisory, result)
      }
    }
  }
}
</script>
