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

<!-- The tenant playground (SPEC.md sections 7 and 9).

     "Streaming chat with effective profiles, token counts, latency",
     over a 15-minute model-scoped session key so real keys never reach
     the browser.

     The session key is deliberately never rendered. It is short-lived,
     but this screen gets demoed and screenshotted, and a credential on
     screen is a credential in someone's slide deck. -->
<template>
  <div class="ai-playground">
    <!-- Two different reasons to show no models, and they had looked
         the same: a refused listAiModels left an empty select beside
         a disabled "Open session" button, which reads as a provider
         that sells nothing rather than an engine that cannot be
         reached. -->
    <p v-if="failed" data-test="load-failed" class="ai-playground__failed">
      Could not open the playground — {{ failed }}
    </p>

    <header v-if="active" class="ai-playground__header">
      <span class="ai-playground__model">{{ active.model }}</span>
      <span class="ai-playground__expiry">
        Session expires {{ expiresAt }}
      </span>
    </header>

    <div v-else class="ai-playground__empty">
      <p>Open a session to try this endpoint.</p>

      <p v-if="!failed && !models.length" data-test="empty" class="ai-playground__none">
        No models are published to this account yet.
      </p>

      <!-- A session is only minted when asked for. Opening one on
           page load would issue a credential nobody requested and
           start its fifteen minutes running against a tenant who is
           only reading the screen. -->
      <select
        v-model="chosen"
        data-test="model"
        class="ai-playground__model-select"
      >
        <option v-for="name in models" :key="name" :value="name">{{ name }}</option>
      </select>
      <button
        data-test="open-session"
        class="ai-playground__open"
        :disabled="!chosen"
        @click="openSession"
      >
        Open session
      </button>
    </div>

    <div v-if="active" data-test="transcript" class="ai-playground__transcript">
      <div
        v-for="(turn, index) in transcript"
        :key="index"
        :class="['ai-playground__turn', `is-${turn.role}`]"
      >
        <p class="ai-playground__content">{{ turn.content }}</p>
        <p v-if="turn.usage" class="ai-playground__usage">
          {{ turn.usage.input }} in / {{ turn.usage.output }} out
          · {{ turn.usage.latencyMs }} ms to first token
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'

import { engineReason } from '@/utils/errors'

export default {
  name: 'AiPlayground',
  props: {
    // What createAiPlaygroundSession returned. CloudStack renders this
    // as a route view and passes it nothing, so null means "fetch for
    // yourself" — the same arrangement every other screen in this
    // section uses. Without it this screen showed its empty state for
    // ever and never called the command at all.
    session: { type: Object, default: null },
    transcript: { type: Array, default: () => [] }
  },
  data () {
    return { models: [], chosen: '', opened: null, failed: '' }
  },
  computed: {
    active () {
      return this.session ?? this.opened
    },
    expiresAt () {
      if (!this.active?.expires) return 'shortly'
      return new Date(this.active.expires).toLocaleTimeString()
    }
  },
  mounted () {
    if (this.session === null) this.loadModels()
  },
  methods: {
    loadModels () {
      getAPI('listAiModels', {}).then((json) => {
        this.models = (json?.listaimodelsresponse?.aimodel ?? []).map((m) => m.name)
        if (!this.chosen && this.models.length) this.chosen = this.models[0]
        this.failed = ''
      }).catch((problem) => {
        this.failed = engineReason(problem)
        this.models = []
      })
    },
    openSession () {
      if (!this.chosen) return
      postAPI('createAiPlaygroundSession', { model: this.chosen }).then((json) => {
        this.opened = json?.createaiplaygroundsessionresponse?.aiplaygroundsession ?? null
      }).catch((problem) => {
        // A refused session leaves the screen as it was rather than
        // half-opened, and says why: a playground key is minted on
        // demand, so a tenant clicking the button is owed an answer.
        this.failed = engineReason(problem)
        this.opened = null
      })
    }
  }
}
</script>
