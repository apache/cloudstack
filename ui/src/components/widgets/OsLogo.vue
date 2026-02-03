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

<template>
  <a-tooltip placement="bottom">
    <template #title>
      {{ name }}
    </template>
    <font-awesome-icon
      :icon="logo"
      :size="size"
      :style="[$store.getters.darkMode ? { color: 'rgba(255, 255, 255, 0.65)' } : { color: '#666' }]"
      />
  </a-tooltip>
</template>

<script>
import { getAPI } from '@/api'

const CACHE_TTL_MS = 30_000
const osTypeCache = new Map() // osId -> { ts, value?, promise? }

export default {
  name: 'OsLogo',
  props: {
    osId: {
      type: String,
      default: ''
    },
    osName: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'lg'
    },
    useCache: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return { name: '', osLogo: ['fas', 'image'] }
  },
  computed: { logo () { return this.osLogo } },
  mounted () { this.fetchData() },
  watch: {
    osId () { this.fetchData() },
    osName () { this.fetchData() }
  },
  methods: {
    async fetchOsTypeName (osId, useCache = this.useCache) {
      const now = Date.now()
      if (useCache) {
        const cached = osTypeCache.get(osId)
        if (cached?.value && (now - cached.ts) < CACHE_TTL_MS) return cached.value
        if (cached?.promise) return cached.promise
        const promise = getAPI('listOsTypes', { id: osId })
          .then(json => {
            const t = json?.listostypesresponse?.ostype
            const name = t?.length
              ? (t[0].description || t[0].osdisplayname || 'Linux')
              : 'Linux'
            osTypeCache.set(osId, { ts: Date.now(), value: name })
            return name
          })
          .catch(e => { osTypeCache.delete(osId); throw e })
        osTypeCache.set(osId, { ts: now, promise })
        return promise
      }
      const json = await getAPI('listOsTypes', { id: osId })
      const t = json?.listostypesresponse?.ostype
      return t?.length ? (t[0].description || t[0].osdisplayname || 'Linux') : 'Linux'
    },
    fetchData () {
      if (this.osName) {
        this.discoverOsLogo(this.osName)
      } else if (this.osId && ('listOsTypes' in this.$store.getters.apis)) {
        this.fetchOsTypeName(this.osId)
          .then(this.discoverOsLogo)
          .catch(() => this.discoverOsLogo('Linux'))
      }
    },
    discoverOsLogo (name) {
      this.name = name
      const osname = (name || '').toLowerCase()
      const logos = [
        { name: 'centos' },
        { name: 'debian' },
        { name: 'ubuntu' },
        { name: 'suse' },
        { name: 'redhat' },
        { name: 'fedora' },
        { name: 'linux' },
        { name: 'bsd', alternate: 'freebsd' },
        { name: 'apple' },
        { name: 'macos', alternate: 'apple' },
        { name: 'window', alternate: 'windows' },
        { name: 'dos', alternate: 'windows' },
        { name: 'oracle', alternate: 'java' }
      ]
      const match = logos.find(entry => osname.includes(entry.name))
      if (match) {
        this.osLogo = ['fab', match.alternate ? match.alternate : match.name]
        return
      }
      this.osLogo = ['fas', 'image']
    }
  }
}
</script>
