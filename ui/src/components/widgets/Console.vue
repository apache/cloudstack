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
  <a
    v-if="['vm', 'systemvm', 'router', 'ilbvm'].includes($route.meta.name) && 'listVirtualMachines' in $store.getters.apis && 'createConsoleEndpoint' in $store.getters.apis"
    @click="consoleUrl">
    <a-button style="margin-left: 5px" shape="circle" type="dashed" :size="size" :disabled="['Stopped', 'Error', 'Destroyed'].includes(resource.state) || resource.hostcontrolstate === 'Offline'" >
      <code-outlined v-if="!copyUrlToClipboard"/>
      <copy-outlined v-else />
    </a-button>
  </a>
</template>

<script>
import { SERVER_MANAGER } from '@/store/mutation-types'
import { api } from '@/api'

export default {
  name: 'Console',
  props: {
    resource: {
      type: Object,
      required: true
    },
    size: {
      type: String,
      default: 'small'
    },
    copyUrlToClipboard: Boolean
  },
  data () {
    return {
      url: ''
    }
  },
  methods: {
    consoleUrl () {
      const params = {}
      params.virtualmachineid = this.resource.id
      api('createConsoleEndpoint', params).then(json => {
        this.url = (json && json.createconsoleendpointresponse) ? json.createconsoleendpointresponse.consoleendpoint.url : '#/exception/404'
        if (json.createconsoleendpointresponse.consoleendpoint.success) {
          if (this.copyUrlToClipboard) {
            this.$message.success({
              content: this.$t('label.copied.clipboard')
            })
            const hiddenElement = document.createElement('textarea')
            hiddenElement.value = this.url
            document.body.appendChild(hiddenElement)
            hiddenElement.focus()
            hiddenElement.select()

            document.execCommand('copy')
            document.body.removeChild(hiddenElement)
          } else {
            window.open(this.url, '_blank')
          }
        } else {
          this.$notification.error({
            message: this.$t('error.execute.api.failed') + ' ' + 'createConsoleEndpoint',
            description: json.createconsoleendpointresponse.consoleendpoint.details
          })
        }
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  },
  computed: {
    server () {
      if (!this.$config.multipleServer) {
        return this.$config.apiBase.replace('/api', '')
      }
      const serverStorage = this.$localStorage.get(SERVER_MANAGER)
      const apiBase = serverStorage.apiBase.replace('/api', '')
      if (!serverStorage.apiHost || serverStorage.apiHost === '/') {
        return [location.origin, apiBase].join('')
      }
      return [serverStorage.apiHost, apiBase].join('')
    }
  }
}
</script>
