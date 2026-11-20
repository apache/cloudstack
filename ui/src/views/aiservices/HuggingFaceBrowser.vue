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

<!-- Searching Hugging Face and checking a model (SPEC.md sections 5, 9).

     The compatibility report is the part that matters: pickle weights
     and `trust_remote_code` are blocked by default, and a model's
     licence is checked at mirror time as well as deploy time. A result
     that cannot be mirrored says why here, before anyone tries. -->
<template>
  <div class="ai-hf">
    <!-- Searching on mount would hit the Hub for every operator who
         opened the screen, and the results would mean nothing. So the
         screen waits — but it has to be able to search when asked,
         which it could not: it took a `models` prop CloudStack never
         passes and called nothing at all. -->
    <form class="ai-hf__search" @submit.prevent="search">
      <input
        v-model="query"
        data-test="query"
        type="search"
        placeholder="Search the Hugging Face Hub"
      >
      <!-- @click as well as the form's submit: a submit button's
           click does not submit a form under jsdom, so binding only
           the form would leave the test green against a button that
           does nothing in a browser, or vice versa. -->
      <button data-test="search" type="submit" :disabled="!query" @click="search">
        Search
      </button>
    </form>

    <p v-if="failed" data-test="load-failed" class="ai-hf__failed">
      Could not search the Hub — {{ failed }}
    </p>
    <p v-else-if="searched && !rows.length" data-test="empty" class="ai-hf__none">
      No models on the Hub matched that search.
    </p>

    <ul class="ai-hf__results">
      <li v-for="model in rows" :key="model.repoid || model.repo" data-test="result">
        <strong>{{ model.repoid || model.repo }}</strong>
        <span class="ai-hf__meta">
          {{ model.task }} · {{ model.license }} · {{ model.downloads }} downloads
        </span>
        <ul v-if="model.problems && model.problems.length" class="ai-hf__problems">
          <li v-for="problem in model.problems" :key="problem">{{ problem }}</li>
        </ul>
        <span v-else class="ai-hf__ok">Compatible</span>
      </li>
    </ul>
  </div>
</template>

<script>
import { getAPI } from '@/api'

import { engineReason } from '@/utils/errors'

export default {
  name: 'AiHuggingFaceBrowser',
  props: {
    // null means "fetch for yourself" — CloudStack renders this as a
    // route view and passes nothing. Unlike the other screens this one
    // fetches on demand rather than on mount, because a search costs
    // somebody else's rate limit.
    models: { type: Array, default: null }
  },
  data () {
    return { query: '', results: [], failed: '', searched: false }
  },
  computed: {
    rows () {
      return this.models ?? this.results
    }
  },
  methods: {
    search () {
      if (!this.query) return
      getAPI('searchAiHfModels', { query: this.query }).then((json) => {
        this.results = json?.searchaihfmodelsresponse?.aihfmodel ?? []
        this.failed = ''
        this.searched = true
      }).catch((problem) => {
        // The Hub is somebody else's service; a failed search is a
        // normal outcome, not an exception to leave unhandled — and
        // "no results" is a different answer from "could not ask".
        this.failed = engineReason(problem)
        this.results = []
        this.searched = true
      })
    }
  }
}
</script>
