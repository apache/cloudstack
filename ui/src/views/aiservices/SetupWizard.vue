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

<!-- The admin setup wizard (SPEC.md sections 7 and 9).

     Every failing check shows its remedy: an operator working through
     a wizard cannot read the source to find out what went wrong. Only
     a failure blocks — a warning is worth saying, and `skipped` is the
     normal state of a zone nothing has observed yet, which is every
     zone on its first run. -->
<template>
  <div class="ai-setup">
    <!-- A wizard that could not reach the engine used to render as a
         wizard with no checks — and "Continue" was enabled, because
         canProceed([]) is true. Saying why comes first. -->
    <p v-if="failed" data-test="load-failed" class="ai-setup__failed">
      Could not read the setup checks — {{ failed }}
    </p>
    <p v-else-if="!rows.length" data-test="empty" class="ai-setup__empty">
      No setup checks have been run yet.
    </p>

    <section
      v-for="status in order"
      :key="status"
      v-show="grouped[status].length"
      :class="['ai-setup__group', `is-${status}`]"
    >
      <h4 class="ai-setup__status">{{ status }}</h4>
      <ul>
        <li v-for="check in grouped[status]" :key="check.name">
          <strong>{{ check.name }}</strong>
          <span class="ai-setup__detail">{{ check.detail }}</span>
          <p v-if="check.remedy" class="ai-setup__remedy">{{ check.remedy }}</p>
        </li>
      </ul>
    </section>

    <button
      data-test="proceed"
      class="ai-setup__proceed"
      :disabled="!proceedable || null"
    >
      Continue
    </button>
  </div>
</template>

<script>
import { getAPI } from '@/api'

import { engineReason } from '@/utils/errors'
import { canProceed, groupChecks } from '@/utils/setup'

export default {
  name: 'AiSetupWizard',
  props: {
    checks: { type: Array, default: null }
  },
  data () {
    return { fetched: [], failed: '', order: ['fail', 'warn', 'skipped', 'pass'] }
  },
  computed: {
    rows () {
      return this.checks ?? this.fetched
    },
    grouped () {
      return groupChecks(this.rows)
    },
    proceedable () {
      // Checks that could not be read are not checks that passed.
      return !this.failed && canProceed(this.rows)
    }
  },
  mounted () {
    if (this.checks === null) this.load()
  },
  methods: {
    load () {
      getAPI('listAiSetupChecks', {}).then((json) => {
        this.fetched = json?.listaisetupchecksresponse?.aisetupcheck ?? []
        this.failed = ''
      }).catch((problem) => {
        // Shown, never thrown, and shown as the engine said it. An
        // unhandled rejection leaves a blank screen with the reason
        // only in the browser console, where nobody looks.
        this.failed = engineReason(problem)
        this.fetched = []
      })
    }
  }
}
</script>
