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
  <div v-if="resourceType && resourceId" >
    <a-tooltip v-if="resourceIcon" placement="top" :title="resourceIconTooltip">
      <render-icon style="font-size: 16px; margin-right: 5px" :icon="resourceIcon" />
    </a-tooltip>
    <a-tag v-else>{{ resourceType }}</a-tag>
    <router-link v-if="resourceRoute && $router.resolve(resourceRoute)" :to="{ path: resourceRoute }">{{ resourceName || resourceId }}</router-link>
    <span v-else>{{ resourceName || resourceId }}</span>
  </div>
</template>

<script>

export default {
  name: 'ResourceLabel',
  props: {
    resourceType: {
      type: String,
      default: ''
    },
    resourceId: {
      type: String,
      default: ''
    },
    resourceName: {
      type: String,
      default: ''
    },
    altText: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      resourceRoute: '',
      resourceIcon: '',
      resourceIconTooltip: ''
    }
  },
  created () {
    if (this.resourceType) {
      var routePrefix = this.$getRouteFromResourceType(this.resourceType)
      if (routePrefix && this.resourceId) {
        this.resourceRoute = '/' + routePrefix + '/' + this.resourceId
      }
      this.resourceIcon = this.$getIconFromResourceType(this.resourceType)
      this.resourceIconTooltip = this.$t('label.' + this.resourceType.toString().toLowerCase())
    }
  }
}
</script>
