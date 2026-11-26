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

<!-- Copy-ready snippets for an endpoint (SPEC.md section 7).

     The engine renders these; nothing is built here. They read the key
     from the environment rather than carrying one, because a snippet
     is copied into tickets and chats where a live key would outlive
     any session. -->
<template>
  <div class="ai-snippets">
    <section
      v-for="snippet in rows"
      :key="snippet.name"
      data-test="snippet"
      class="ai-snippets__item"
    >
      <h4 class="ai-snippets__name">{{ snippet.name }}</h4>
      <pre class="ai-snippets__code"><code>{{ snippet.code }}</code></pre>
    </section>
  </div>
</template>

<script>
export default {
  name: 'AiSnippetsTab',
  props: {
    // The endpoint row CloudStack is showing. A tab is handed this
    // and nothing else, so reading only a `snippets` prop meant the
    // tab rendered empty however much the endpoint carried.
    resource: { type: Object, default: () => ({}) },
    // Supplied by a test, or by a parent that already has them.
    snippets: { type: Array, default: null }
  },
  computed: {
    rows () {
      return this.snippets ?? this.resource?.snippets ?? []
    }
  }
}
</script>
