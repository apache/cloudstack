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
  <a-list-item v-if="['host', 'computeoffering'].includes($route.meta.name) && filteredExternalDetails">
    <div>
      <strong>{{ ['computeoffering'].includes($route.meta.name) ? $t('label.externaldetails') : $t('label.configuration.details') }}</strong>
      <div>
        <object-list-table :data-map="filteredExternalDetails" />
      </div>
    </div>
  </a-list-item>
  <a-list-item v-else-if="['cluster'].includes($route.meta.name)">
    <div>
      <strong>{{ $t('label.configuration.details') }}</strong>
      <div>
        <object-list-table :data-map="extensionResourceDetails" />
      </div>
    </div>
  </a-list-item>
</template>

<script>
import { getAPI } from '@/api'
import ObjectListTable from '@/components/view/ObjectListTable'
import { getFilteredExternalDetails } from '@/utils/extension'

export default {
  name: 'ExternalConfigurationDetails',
  components: {
    ObjectListTable
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      extension: {},
      loading: false
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem && newItem.id !== oldItem.id) {
          this.fetchData()
        }
      }
    }
  },
  computed: {
    filteredExternalDetails () {
      const detailsKeys = {
        host: 'details',
        computeoffering: 'serviceofferingdetails'
      }
      const detailsKey = detailsKeys[this.$route.meta.name]
      if (!detailsKey || !this.resource) {
        return null
      }
      return getFilteredExternalDetails(this.resource[detailsKey])
    },
    extensionResourceDetails () {
      if (!this.resource?.id || !this.extension?.resources) {
        return null
      }
      const resource = this.extension.resources.find(r => r.id === this.resource.id)
      if (!resource || !resource.details || typeof resource.details !== 'object') {
        return null
      }
      return resource.details
    }
  },
  methods: {
    fetchData () {
      if (!['cluster'].includes(this.$route.meta.name) || !this.resource.extensionid) {
        this.extension = {}
        return
      }
      this.loading = true
      const params = {
        id: this.resource.extensionid,
        details: 'resource'
      }
      getAPI('listExtensions', params).then(json => {
        this.extension = json.listextensionsresponse.extension[0]
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
