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
  <a-popover v-if="enabled && actionsExist" triggers="hover" placement="topLeft">
    <template slot="content">
      <action-button
        :size="size"
        :actions="actions"
        :dataView="true"
        :resource="resource"
        @exec-action="execAction" />
    </template>
    <a-button shape="circle" size="small" icon="more" style="float: right; background-color: transparent; border-color: transparent"/>
  </a-popover>
</template>

<script>
import ActionButton from '@/components/view/ActionButton'

export default {
  name: 'QuickView',
  components: {
    ActionButton
  },
  props: {
    actions: {
      type: Array,
      default: () => []
    },
    enabled: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'default'
    },
    resource: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  watch: {
    resource () {
      this.actionsExist = this.doActionsExist()
    }
  },
  data () {
    return {
      actionsExist: false
    }
  },
  mounted () {
    this.actionsExist = this.doActionsExist()
  },
  methods: {
    execAction (action) {
      this.$emit('exec-action', action)
    },
    doActionsExist () {
      return this.actions.filter(x =>
        x.api in this.$store.getters.apis &&
        ('show' in x ? x.show(this.resource, this.$store.getters) : true) &&
        x.dataView).length > 0
    }
  }
}
</script>
