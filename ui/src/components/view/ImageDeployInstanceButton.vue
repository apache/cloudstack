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
  <span>
    <a-dropdown-button
      type="primary"
      :loading="loading"
      v-if="allowed"
      :disabled="!zones || zones.length === 0">
        <rocket-outlined />
        {{ $t('label.create.vm') }}
        <template #icon><down-outlined /></template>
        <template #overlay>
          <a-menu type="primary"  @click="handleDeployInstanceMenu">
              <a-menu-item v-for="zone in zones" :key="zone.id">
              <span v-if="zone.icon && zone.icon.base64image">
                  <resource-icon :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              </span>
              <global-outlined v-else style="margin-right: 5px" />
              {{ zone.name }}
              </a-menu-item>
          </a-menu>
        </template>
    </a-dropdown-button>
  </span>
</template>

<script>
import { getAPI } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ImageDeployInstanceButton',
  components: {
    ResourceIcon
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    osCategoryId: {
      type: String,
      default: null
    }
  },
  emits: ['update-zones'],
  data () {
    return {
      imageApi: 'listTemplates',
      loading: false,
      zones: []
    }
  },
  mounted () {
    if (this.$route.meta.name === 'iso') {
      this.imageApi = 'listIsos'
    }
    this.fetchData()
  },
  watch: {
    resource (newValue) {
      if (newValue?.id) {
        this.fetchData()
      }
    }
  },
  computed: {
    allowed () {
      return (this.$route.meta.name === 'template' ||
        (this.$route.meta.name === 'iso' && this.resource.bootable))
    }
  },
  methods: {
    fetchData () {
      this.fetchResourceData()
    },
    fetchResourceData () {
      if (!this.resource || !this.resource.id) {
        return
      }
      const params = {
        id: this.resource.id,
        templatefilter: 'executable',
        listall: true
      }

      this.dataSource = []
      this.itemCount = 0
      this.loading = true
      this.zones = []
      getAPI(this.imageApi, params).then(json => {
        const imageResponse = json?.[this.imageApi.toLowerCase() + 'response']?.[this.$route.meta.name] || []
        this.zones = imageResponse.map(i => ({
          id: i.zoneid,
          name: i.zonename
        }))
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        if (this.zones.length !== 0) {
          this.$emit('update-zones', this.zones)
        }
        this.fetchZonesForIcon()
      })
    },
    fetchZonesForIcon () {
      if (!this.zones) {
        return
      }
      const zoneids = this.zones.map(z => z.id)
      this.loading = true
      const params = { showicon: true, ids: zoneids.join(',') }
      getAPI('listZones', params).then(json => {
        this.zones = json.listzonesresponse.zone || []
      }).finally(() => {
        this.loading = false
        if (this.zones.length !== 0) {
          this.$emit('update-zones', this.zones)
        }
      })
    },
    handleDeployInstanceMenu (e) {
      const query = { zoneid: e.key }
      query[this.$route.meta.name + 'id'] = this.resource.id
      if (this.resource.arch) {
        query.arch = this.resource.arch
      }
      if (this.osCategoryId) {
        query.oscategoryid = this.osCategoryId
      }
      this.$router.push({
        path: '/action/deployVirtualMachine',
        query: query
      })
    }
  }
}
</script>
