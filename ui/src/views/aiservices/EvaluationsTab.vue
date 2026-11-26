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

<!-- A model's evaluation runs and what the gate decided (SPEC.md
     section 8).

     A tab rather than a screen, because `listAiEvaluations` is scoped
     to a model — GET /v1/models/{model_name}/evaluations — so there is
     no model-less list to draw. The registry row is the model.

     The score is not the point: the gate blocks publishing, and what
     an operator needs to see is whether this revision was blocked and
     whether a root admin published past it anyway. An override is
     shown as its own state, not folded into "passed", because the two
     mean very different things to whoever reviews the catalog. -->
<template>
  <table class="ai-evaluations">
    <thead>
      <tr>
        <th>Revision</th>
        <th>Score</th>
        <th>Gate</th>
        <th>Summary</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="evaluation in rows"
        :key="evaluation.id"
        data-test="evaluation"
        :class="`is-${outcome(evaluation)}`"
      >
        <td>{{ evaluation.revision }}</td>
        <td>{{ evaluation.score ?? '—' }}</td>
        <td>{{ verdict(evaluation) }}</td>
        <td>{{ evaluation.summary }}</td>
      </tr>
      <tr v-if="rows.length === 0">
        <td colspan="4" data-test="empty">Not evaluated yet.</td>
      </tr>
    </tbody>
  </table>
</template>

<script>
import { getAPI } from '@/api'

export default {
  name: 'AiEvaluationsTab',
  props: {
    // The registry row CloudStack is showing.
    resource: { type: Object, default: () => ({}) },
    // Supplied by a test; when it is null the tab fetches for itself.
    evaluations: { type: Array, default: null }
  },
  data () {
    return { fetched: [] }
  },
  computed: {
    model () {
      return this.resource?.name ?? ''
    },
    rows () {
      return this.evaluations ?? this.fetched
    }
  },
  watch: {
    // CloudStack mounts a tab before the detail view has its row, and
    // again when the operator picks another model.
    model: {
      immediate: true,
      handler (model) {
        if (model && this.evaluations === null) this.load(model)
      }
    }
  },
  methods: {
    outcome (evaluation) {
      if (evaluation.overridden) return 'overridden'
      return evaluation.passed ? 'passed' : 'blocked'
    },
    verdict (evaluation) {
      switch (this.outcome(evaluation)) {
        case 'overridden':
          return 'Blocked, overridden by a root admin'
        case 'passed':
          return 'Passed'
        default:
          return 'Blocked from publishing'
      }
    },
    load (model) {
      getAPI('listAiEvaluations', { model }).then((json) => {
        this.fetched = json?.listaievaluationsresponse?.aievaluation ?? []
      })
    }
  }
}
</script>
