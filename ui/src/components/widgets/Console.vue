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
    @click="consoleAccessCheck">
    <a-button style="margin-left: 5px" shape="circle" type="dashed" :size="size" :disabled="['Stopped', 'Error', 'Destroyed'].includes(resource.state)" >
      <code-outlined />
    </a-button>
    <a-modal :visible="tokenModalVisible" title="Extra validation token requested" @ok="handleSubmit" @cancel="() => tokenModalVisible = false">
      <div class="form-layout" v-ctrl-enter="handleSubmit">
        <a-form :form="form" layout="horizontal" @submit="handleSubmit">
          <p>The admin has enabled the extra validation for the console access. Please specify a security token:</p>
          <a-form-item>
            <tooltip-label slot="label" title="Token" tooltip="Extra security token"/>
            <a-input v-decorator="['token', { initialValue: '', rules: [{ required: true, message: 'Please provide a token' }] }]"/>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </a>
</template>

<script>
import { SERVER_MANAGER } from '@/store/mutation-types'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

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
    }
  },
  data () {
    return {
      url: '',
      tokenModalVisible: false,
      tokenValidationEnabled: false
    }
  },
  components: {
    TooltipLabel
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.verifyExtraValidationEnabled()
  },
  methods: {
    verifyExtraValidationEnabled () {
      api('listConfigurations', { name: 'consoleproxy.extra.security.validation.enabled' }).then(json => {
        this.tokenValidationEnabled = json.listconfigurationsresponse.configuration !== null && json.listconfigurationsresponse.configuration[0].value === 'true'
      })
    },
    consoleAccessCheck () {
      if (this.tokenValidationEnabled) {
        this.tokenModalVisible = true
      } else {
        this.consoleUrl()
      }
    },
    consoleUrl (token) {
      const params = {}
      if (token) {
        params.token = token
      }
      params.virtualmachineid = this.resource.id
      api('createConsoleEndpoint', params).then(json => {
        this.url = (json && json.createconsoleendpointresponse) ? json.createconsoleendpointresponse.consoleendpoint.url : '#/exception/404'
        window.open(this.url, '_blank')
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFieldsAndScroll((err, values) => {
        if (err) return
        this.tokenModalVisible = false
        this.consoleUrl(values.token)
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
