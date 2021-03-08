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
    <template slot="title">
      {{ name }}
    </template>
    <font-awesome-icon :icon="['fab', logo]" :size="size" style="color: #666;" v-if="logo !== 'debian'" />
    <debian-icon v-else-if="logo === 'debian'" :style="{ height: size === '4x' ? '56px' : '16px', width: size === '4x' ? '56px' : '16px', marginBottom: '-4px' }" />
  </a-tooltip>
</template>

<script>
import { api } from '@/api'
import DebianIcon from '@/assets/icons/debian.svg?inline'

export default {
  name: 'OsLogo',
  components: {
    DebianIcon
  },
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
      osLogo: 'linux'
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
    osId: function (newItem, oldItem) {
      this.osId = newItem
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
      api('listOsTypes', { id: osId }).then(json => {
        if (json && json.listostypesresponse && json.listostypesresponse.ostype && json.listostypesresponse.ostype.length > 0) {
          this.discoverOsLogo(json.listostypesresponse.ostype[0].description)
        } else {
          this.discoverOsLogo('Linux')
        }
      })
    },
    discoverOsLogo (name) {
      this.name = name
      const osname = name.toLowerCase()
      if (osname.includes('centos')) {
        this.osLogo = 'centos'
      } else if (osname.includes('debian')) {
        this.osLogo = 'debian'
      } else if (osname.includes('ubuntu')) {
        this.osLogo = 'ubuntu'
      } else if (osname.includes('suse')) {
        this.osLogo = 'suse'
      } else if (osname.includes('redhat')) {
        this.osLogo = 'redhat'
      } else if (osname.includes('fedora')) {
        this.osLogo = 'fedora'
      } else if (osname.includes('linux')) {
        this.osLogo = 'linux'
      } else if (osname.includes('bsd')) {
        this.osLogo = 'freebsd'
      } else if (osname.includes('apple')) {
        this.osLogo = 'apple'
      } else if (osname.includes('window') || osname.includes('dos')) {
        this.osLogo = 'windows'
      } else if (osname.includes('oracle')) {
        this.osLogo = 'java'
      } else {
        this.osLogo = 'linux'
      }
      this.$emit('update-osname', this.name)
    }
  }
}
</script>

<style scoped>
</style>
