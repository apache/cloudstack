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

<!-- What a guardrail profile actually screens (SPEC.md section 8).

     A tab rather than a detail row: the engine keys these by name and
     the plugin carries them as a named list, and CloudStack's
     DetailsTab prints a value rather than rendering one, so a list
     there reads "[object Object]".

     `locked` is called out because it is the thing being sold. A
     school sets education-safe on their domain precisely so an account
     beneath cannot relax it, and a tenant looking at the profile
     screening them should not have to infer that. -->
<template>
  <div class="ai-checks">
    <p v-if="!rows.length" data-test="empty" class="ai-checks__empty">
      This profile screens nothing.
    </p>

    <ul v-else class="ai-checks__list">
      <li
        v-for="check in rows"
        :key="check.name"
        data-test="check"
        :class="['ai-checks__item', `is-${check.action}`, { 'is-locked': check.policy === 'locked' }]"
      >
        <strong class="ai-checks__name">{{ check.name }}</strong>
        <span class="ai-checks__action">{{ check.action }}</span>
        <span v-if="check.policy === 'locked'" class="ai-checks__locked">
          locked — cannot be relaxed below this level
        </span>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'AiGuardrailChecksTab',
  props: {
    // CloudStack hands a tab its parent `resource` and nothing else.
    resource: { type: Object, default: () => ({}) },
    // Supplied by a test, or by a parent that already has them.
    checks: { type: Array, default: null }
  },
  computed: {
    rows () {
      return this.checks ?? this.resource?.checks ?? []
    }
  }
}
</script>
