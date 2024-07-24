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
  <div>
    <child-component :resource="vm"></child-component>
  </div>
</template>

<script>
import { api } from '@/api'
import ChildComponent from '../../views/compute/InstanceTab.vue'

export default {
  name: 'FileShareAccessTab',
  components: {
    ChildComponent
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      instanceLoading: false,
      virtualmachines: [],
      vm: {}
    }
  },
  created () {
    this.fetchInstances()
  },
  methods: {
    fetchInstances () {
      if (!this.resource.virtualmachineid) {
        return
      }
      this.instanceLoading = true
      this.loading = true
      var params = {
        id: this.resource.virtualmachineid,
        listall: true
      }
      api('listVirtualMachines', params).then(json => {
        this.virtualmachines = json.listvirtualmachinesresponse.virtualmachine || []
        this.vm = this.virtualmachines[0]
      }).finally(() => {
        this.loading = false
      })
      this.instanceLoading = false
    }
  }
}
</script>

<style lang="css" scoped>
.title {
  font-weight: bold;
  margin-bottom: 14px;
  font-size: 16px;
}

.content {
  font-size: 16px;
}
</style>
