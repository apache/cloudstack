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

<!-- Resolved generation parameters (SPEC.md sections 6 and 9).

     "Resolved values per endpoint and key, with source level." The
     source is the whole point: a bare number tells a tenant nothing
     about why it is that or whether they may change it. -->
<template>
  <div class="ai-effective__screen">
    <!-- A resolved value is meaningless without knowing which
         deployment it was resolved for, and CloudStack renders this as
         a route view with no resource — so the screen asks which. It
         previously took a `parameters` prop nothing supplied and drew
         an empty table for ever. -->
    <p v-if="failed" data-test="load-failed" class="ai-effective__failed">
      Could not resolve parameters — {{ failed }}
    </p>

    <form class="ai-effective__picker" @submit.prevent="resolve">
      <select v-model="chosen" data-test="model">
        <option v-for="name in models" :key="name" :value="name">{{ name }}</option>
      </select>
      <button data-test="resolve" type="submit" :disabled="!chosen" @click="resolve">
        Resolve
      </button>
    </form>

    <table class="ai-effective">
    <thead>
      <tr>
        <th>Parameter</th>
        <th>Value</th>
        <th>Set at</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="parameter in described" :key="parameter.name">
        <td>{{ parameter.name }}</td>
        <td>{{ parameter.value }}</td>
        <td>
          {{ parameter.source }}
          <span
            v-if="parameter.locked"
            data-test="locked"
            class="ai-effective__locked"
            title="Locked at this level; it cannot be overridden below."
          >locked</span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { describeParameter } from '@/utils/profiles'

import { getAPI } from '@/api'
import { engineReason } from '@/utils/errors'

export default {
  name: 'AiEffectiveParameters',
  props: {
    // null means "fetch for yourself"; a parent holding the data, and
    // the rendering tests, still pass an array in.
    parameters: { type: Array, default: null }
  },
  data () {
    return { models: [], chosen: '', resolved: [], failed: '' }
  },
  computed: {
    described () {
      return (this.parameters ?? this.resolved).map(describeParameter)
    }
  },
  mounted () {
    if (this.parameters === null) this.loadModels()
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
    resolve () {
      if (!this.chosen) return
      getAPI('getAiEffectiveParameters', { model: this.chosen }).then((json) => {
        this.resolved =
          json?.getaieffectiveparametersresponse?.aieffectiveparameter ?? []
        this.failed = ''
      }).catch((problem) => {
        // A locked bound a tenant may not read resolves to a refusal,
        // and the sentence says which level locked it.
        this.failed = engineReason(problem)
        this.resolved = []
      })
    }
  }
}
</script>
