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
  <DetailsTab :resource="resource" />
</template>
<script>
import DetailsTab from '@/components/view/DetailsTab'
export default {
  name: 'ProjectDetailsTab',
  components: {
    DetailsTab
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.determineOwner()
    }
  },
  methods: {
    determineOwner () {
      var owner = this.resource.owner || []
      // If current backend does not support multiple project admins
      if (owner.length === 0) {
        this.$set(this.resource, 'isCurrentUserProjectAdmin', this.resource.account === this.$store.getters.userInfo.account)
        return
      }
      owner = owner.filter(projectaccount => {
        return (projectaccount.userid && projectaccount.userid === this.$store.getters.userInfo.id) ||
          projectaccount.account === this.$store.getters.userInfo.account
      })
      this.$set(this.resource, 'isCurrentUserProjectAdmin', owner.length > 0)
    }
  }
}
</script>
