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
    }
  },
  data () {
    return {
      name: '',
      osLogo: ['fas', 'image']
    }
  },
  computed: {
    logo: function () {
      if (!this.name) {
        this.fetchData()
      }
      return this.osLogo
    }
  },
  watch: {
    osId: function () {
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      if (this.osName) {
        this.discoverOsLogo(this.osName)
      } else if (this.osId) {
        this.findOsName(this.osId)
      }
    },
    findOsName (osId) {
      if (!('listOsTypes' in this.$store.getters.apis)) {
        return
      }
      this.name = 'linux'
      getAPI('listOsTypes', { id: osId }).then(json => {
        if (json && json.listostypesresponse && json.listostypesresponse.ostype && json.listostypesresponse.ostype.length > 0) {
          this.discoverOsLogo(json.listostypesresponse.ostype[0].description)
        } else {
          this.discoverOsLogo('Linux')
        }
      })
    },
    getFontAwesomeIcon (name) {
      return ['fab', name]
    },
    discoverOsLogo (name) {
      this.name = name
      this.$emit('update-osname', this.name)
      const osname = name.toLowerCase()
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

<style scoped>
</style>
