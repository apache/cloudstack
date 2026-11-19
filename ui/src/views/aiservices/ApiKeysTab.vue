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

<!-- Scoped API keys for an endpoint (SPEC.md section 7).

     A key's secret exists for one moment: the engine returns it from
     createAiApiKey and stores a salted hash, so it can never be read
     back. This tab mints the key itself rather than leaving it to
     CloudStack's autogen dialog, which closed without showing the
     secret — the hooks that would have shown it, `action.response`
     and `successMethod`, are inside $pollJob and fire for an async
     command only. A key made that way could not be used by anyone.

     The list below carries metadata only. `lastused` is there because
     it is how a tenant decides whether an old key is still in use
     before revoking it. -->
<template>
  <div class="ai-keys__panel">
    <!-- Three states, kept apart on purpose. Emptying the list on a
         refusal renders a screen that could not reach the engine
         exactly like one with nothing to show, and the reason is the
         engine's own sentence because that is the one that names a
         remedy. -->
    <p v-if="failed" data-test="load-failed" class="ai-keys__failed">
      Could not read API keys — {{ failed }}
    </p>
    <p v-else-if="!rows.length" data-test="empty" class="ai-keys__empty">
      No API keys have been created for this account yet.
    </p>

    <!-- Shown once, and said so. This screen gets demonstrated and
         screenshotted, so the secret is dismissable and nothing holds
         it afterwards. -->
    <section v-if="secret" class="ai-keys__secret">
      <h4>Copy this key now — it is shown once and cannot be read back.</h4>
      <code data-test="new-secret" class="ai-keys__secret-value">{{ secret }}</code>
      <button data-test="dismiss-secret" class="ai-keys__dismiss" @click="secret = ''">
        Done
      </button>
    </section>

    <form class="ai-keys__new" @submit.prevent="create">
      <input
        v-model="draft.name"
        data-test="new-name"
        type="text"
        placeholder="a name for the key"
      >
      <input
        v-model="draft.models"
        data-test="new-models"
        type="text"
        placeholder="models this key may use; all if unset"
      >
      <input
        v-model="draft.cidrs"
        data-test="new-cidrs"
        type="text"
        placeholder="CIDRs the key may be used from"
      >
      <input
        v-model="draft.expires"
        data-test="new-expires"
        type="text"
        placeholder="when the key should expire"
      >
      <!-- @click as well as submit: a submit button's click does not
           submit a form under jsdom, so binding only the form would
           leave the test green against a button that does nothing in a
           browser, or the other way round. -->
      <button data-test="create" type="submit" :disabled="!draft.name" @click="create">
        Create key
      </button>
    </form>

    <p v-if="createFailed" data-test="create-failed" class="ai-keys__create-failed">
      Could not create the key — {{ createFailed }}
    </p>

    <table class="ai-keys">
      <thead>
        <tr>
          <th>Name</th>
          <th>Models</th>
          <th>Last used</th>
          <th>Expires</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="key in rows" :key="key.id" data-test="key">
          <td>{{ key.name }}</td>
          <td>{{ (key.allowedmodels || []).join(', ') || 'any' }}</td>
          <td>{{ key.lastused || 'never' }}</td>
          <td>{{ key.expires || 'never' }}</td>
          <td>{{ key.revoked ? 'revoked' : 'active' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'

import { engineReason } from '@/utils/errors'

export default {
  name: 'AiApiKeysTab',
  props: {
    // CloudStack hands a tab its parent `resource`, never a named
    // array, so a tab that only accepts props renders an empty table
    // however much the account has. null means "fetch for yourself",
    // the same arrangement the route screens use; the tests and any
    // parent holding the data still pass it in.
    keys: { type: Array, default: null }
  },
  data () {
    return {
      fetched: [],
      failed: '',
      secret: '',
      createFailed: '',
      draft: { name: '', models: '', cidrs: '', expires: '' }
    }
  },
  computed: {
    rows () {
      return this.keys ?? this.fetched
    }
  },
  mounted () {
    if (this.keys === null) this.load()
  },
  methods: {
    load () {
      getAPI('listAiApiKeys', {}).then((json) => {
        this.fetched = json?.listaiapikeysresponse?.aiapikey ?? []
        this.failed = ''
      }).catch((problem) => {
        // Shown, never thrown, and shown as the engine said it.
        // An unhandled rejection leaves a blank screen with the
        // reason only in the browser console, where nobody looks.
        this.failed = engineReason(problem)
        this.fetched = []
      })
    },
    /** A comma-separated field as the list the command expects. */
    listOf (value) {
      return value.split(',').map((item) => item.trim()).filter(Boolean)
    },
    create () {
      if (!this.draft.name) return

      // `models`, `cidrlist` and `expires` are the command's own
      // parameter names. The Terraform provider and the autogen action
      // both sent `allowedmodels`, `cidrs` and `expiresat`, which
      // CloudStack drops in silence — so a key restricted to two models
      // and a CIDR was created with neither.
      const params = { name: this.draft.name }
      const models = this.listOf(this.draft.models)
      const cidrs = this.listOf(this.draft.cidrs)
      if (models.length) params.models = models
      if (cidrs.length) params.cidrlist = cidrs
      if (this.draft.expires) params.expires = this.draft.expires

      postAPI('createAiApiKey', params).then((json) => {
        const made = json?.createaiapikeyresponse?.aiapikey ??
          json?.createaiapikeyresponse ?? {}
        this.secret = made.apikey ?? ''
        this.createFailed = ''
        this.draft = { name: '', models: '', cidrs: '', expires: '' }
        this.load()
      }).catch((problem) => {
        this.createFailed = engineReason(problem)
      })
    }
  }
}
</script>
