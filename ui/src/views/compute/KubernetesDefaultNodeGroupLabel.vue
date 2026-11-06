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
    <span>{{ labelParts[0] }}</span>
    <a :href="nodeGroupsTabUrl" @click="$emit('navigate')">{{ nodeGroupName }}</a>
    <span>{{ labelParts[1] }}</span>
    <a-tooltip v-if="tooltip" :title="tooltip">
      <info-circle-outlined class="tooltip-icon" />
    </a-tooltip>
  </span>
</template>

<script>
const GROUP_PLACEHOLDER = '@@group@@'

// Form label mentioning the default node group of a Kubernetes cluster, whose name links to the node groups tab of the cluster
export default {
  name: 'KubernetesDefaultNodeGroupLabel',
  props: {
    // translation key with a {group} placeholder for the name of the default node group
    keypath: {
      type: String,
      required: true
    },
    tooltip: {
      type: String,
      default: null
    },
    // the Kubernetes cluster
    resource: {
      type: Object,
      required: true
    }
  },
  emits: ['navigate'],
  computed: {
    nodeGroupName () {
      const nodeGroup = (this.resource.nodegroups || []).find(group => group.isdefault)
      return nodeGroup ? nodeGroup.name : 'default'
    },
    // e.g. #/kubernetes/<cluster id>?tab=nodegroups, relative to the current page of the UI
    nodeGroupsTabUrl () {
      return this.$router.resolve({ path: '/kubernetes/' + this.resource.id, query: { tab: 'nodegroups' } }).href
    },
    labelParts () {
      const parts = this.$t(this.keypath, { group: GROUP_PLACEHOLDER }).split(GROUP_PLACEHOLDER)
      return [parts[0], parts.slice(1).join('')]
    }
  }
}
</script>

<style scoped lang="scss">
  .tooltip-icon {
    color: rgba(0,0,0,.45);
    margin-left: 2px;
  }
</style>
