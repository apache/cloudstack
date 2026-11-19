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

<!-- Fleet health (SPEC.md section 9).

     Template and driver versions, and drift between them. Drift
     matters because hosts on different drivers serve the same model
     inconsistently and fail in ways that look like model bugs. The fix
     is drain and replace — passthrough GPUs cannot migrate
     (ADR-0003) — which is the action this screen carries. -->
<template>
  <div class="ai-fleet__panel">
    <!-- Three states, kept apart on purpose. Emptying the list on a
         refusal renders a screen that could not reach the engine
         exactly like one with nothing to show, and the reason is the
         engine's own sentence because that is the one that names a
         remedy. -->
    <p v-if="failed" data-test="load-failed" class="ai-fleet__failed">
      Could not read fleet health — {{ failed }}
    </p>
    <p v-else-if="!rows.length" data-test="empty" class="ai-fleet__empty">
      No GPU hosts have been observed yet.
    </p>

    <table class="ai-fleet">
      <thead>
        <tr>
          <th>Host</th>
          <th>Template</th>
          <th>Driver</th>
          <th>State</th>
          <th>Nodes</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="host in rows"
          :key="host.host"
          data-test="host"
          :class="{ 'is-drifted': host.drift }"
        >
          <td>{{ host.host }}</td>
          <td>{{ host.template }}</td>
          <td>{{ host.driver }}</td>
          <td>{{ host.state }}</td>
          <td>{{ host.nodes }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { getAPI } from '@/api'

import { engineReason } from '@/utils/errors'

export default {
  name: 'AiFleetHealth',
  props: {
    hosts: { type: Array, default: null }
  },
  data () {
    return { fetched: [], failed: '' }
  },
  computed: {
    rows () {
      return this.hosts ?? this.fetched
    }
  },
  mounted () {
    if (this.hosts === null) this.load()
  },
  methods: {
    load () {
      getAPI('listAiGpuCapacity', {}).then((json) => {
        this.fetched = json?.listaigpucapacityresponse?.aigpucapacity ?? []
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
