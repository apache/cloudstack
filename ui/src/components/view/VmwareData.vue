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
  <a-list-item v-if="vmwaredc">
    <div>
      <div style="margin-bottom: 10px;">
        <div><strong>{{ $t('label.vmwaredcname') }}</strong></div>
        <div>{{ vmwaredc.name }}</div>
      </div>
      <div style="margin-bottom: 10px;">
        <div><strong>{{ $t('label.vmwaredcvcenter') }}</strong></div>
        <div>{{ vmwaredc.vcenter }}</div>
      </div>
      <div style="margin-bottom: 10px;">
        <div><strong>{{ $t('label.vmwaredcid') }}</strong></div>
        <div>{{ vmwaredc.id }}</div>
      </div>
    </div>
  </a-list-item>
</template>

<script>
import { api } from '@/api'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      vmwaredc: null
    }
  },
  watch: {
    resource (newItem, oldItem) {
      if (this.resource && this.resource.id && newItem && newItem.id !== oldItem.id) {
        this.fetchData()
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (!this.resource.id) return
      this.$set(this.resource, 'vmwaredc', null)
      api('listVmwareDcs', {
        zoneid: this.resource.id
      }).then(response => {
        if (response.listvmwaredcsresponse.VMwareDC && response.listvmwaredcsresponse.VMwareDC.length > 0) {
          this.vmwaredc = response.listvmwaredcsresponse.VMwareDC[0]
        }
        this.$set(this.resource, 'vmwaredc', this.vmwaredc)
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>
