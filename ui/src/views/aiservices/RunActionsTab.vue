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

<!-- One reconcile run's actions (SPEC.md section 4).

     A failed action stops only its dependants, so a run can be part
     applied; the outcome per action is what tells an operator how far
     it got.

     It reads the run CloudStack handed it. It used to fetch the whole
     run list and read action fields off each *run* — so every cell was
     empty, whatever had happened. A run carries its actions as a nested
     list, which nothing declared until now, so there was no data source
     to read even if it had looked in the right place. -->
<template>
  <div class="ai-run-actions__panel">
    <!-- Three states, kept apart on purpose. Emptying the list on a
         refusal renders a screen that could not reach the engine
         exactly like one with nothing to show, and the reason is the
         engine's own sentence because that is the one that names a
         remedy. -->
    <p v-if="!rows.length" data-test="empty" class="ai-run-actions__empty">
      This run recorded no actions.
    </p>

    <table class="ai-run-actions">
      <thead>
        <tr>
          <th>Action</th>
          <th>Resource</th>
          <th>Outcome</th>
          <th>Detail</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="action in rows"
          :key="action.id"
          data-test="action"
          :class="`is-${action.outcome}`"
        >
          <td>{{ action.kind }}</td>
          <td>{{ action.resourcetype }} / {{ action.name }}</td>
          <td>{{ action.outcome }}</td>
          <td>{{ action.error || action.detail }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'AiRunActionsTab',
  props: {
    // CloudStack hands a tab its parent `resource` — here, the run the
    // operator opened. The actions come with it; there is nothing to
    // fetch, and fetching the run list was the bug.
    resource: { type: Object, default: () => ({}) },
    // Supplied by a test, or by a parent that already has them.
    actions: { type: Array, default: null }
  },
  computed: {
    rows () {
      return this.actions ?? this.resource?.actions ?? []
    }
  }
}
</script>
