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

<!-- Budgets and where a tenant stands (SPEC.md section 7).

     Field names are CloudStack's, not the engine's: a response field is
     serialised under its lower-cased `api` name, so `percent_used`
     arrives as `percentused`. Reading the engine's spelling renders
     nothing and says nothing, which is how this tab came to show an
     empty percentage over a budget that was blocking spend.

     There was a "Set in" column saying whether a budget came from
     platform.yaml or the API. The engine sends no such field and never
     has, so every budget claimed to come from platform.yaml. It is gone
     rather than guessed at. -->
<template>
  <div class="ai-budgets__panel">
    <!-- Three states, kept apart on purpose. Emptying the list on a
         refusal renders a screen that could not reach the engine
         exactly like one with nothing to show, and the reason is the
         engine's own sentence because that is the one that names a
         remedy. -->
    <p v-if="failed" data-test="load-failed" class="ai-budgets__failed">
      Could not read budgets — {{ failed }}
    </p>
    <p v-else-if="!rows.length" data-test="empty" class="ai-budgets__empty">
      No budget has been set for this account.
    </p>

    <table class="ai-budgets">
      <thead>
        <tr>
          <th>Account</th>
          <th>Monthly</th>
          <th>Spent</th>
          <th>Used</th>
          <th>At the limit</th>
          <th>Set in</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="budget in rows"
          :key="budget.account"
          data-test="budget"
          :class="{ 'is-blocked': budget.blocked }"
        >
          <td>{{ budget.account }}</td>
          <td>{{ budget.monthly }} {{ budget.currency }}</td>
          <td>{{ budget.spent }} {{ budget.currency }}</td>
          <td>{{ budget.percentused }}%</td>
          <td>{{ budget.onexceed === 'block' ? 'stop serving' : 'alert only' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { getAPI } from '@/api'

import { engineReason } from '@/utils/errors'

export default {
  name: 'AiBudgetsTab',
  props: {
    // CloudStack hands a tab its parent `resource`, never a named
    // array, so a tab that only accepts props renders an empty table
    // however much the account has. null means "fetch for yourself",
    // the same arrangement the route screens use; the tests and any
    // parent holding the data still pass it in.
    budgets: { type: Array, default: null }
  },
  data () {
    return { fetched: [], failed: '' }
  },
  computed: {
    rows () {
      return this.budgets ?? this.fetched
    }
  },
  mounted () {
    if (this.budgets === null) this.load()
  },
  methods: {
    load () {
      getAPI('listAiBudgets', {}).then((json) => {
        this.fetched = json?.listaibudgetsresponse?.aibudget ?? []
        this.failed = ''
      }).catch((problem) => {
        // Shown, never thrown, and shown as the engine said it.
        // An unhandled rejection leaves a blank screen with the
        // reason only in the browser console, where nobody looks.
        this.failed = engineReason(problem)
        this.fetched = []
      })
    }
  }
}
</script>
