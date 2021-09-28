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
    <span v-if="showError">
      <a-alert type="error" :message="$t('message.error.cloudian.console')" showIcon />
      <br/>
      <a-button @click="doSso()">{{ $t('label.try.again') }}</a-button>
    </span>
    <span v-else>
      <a-alert type="info" :message="$t('message.info.cloudian.console')" showIcon />
    </span>
    <br/>
  </div>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'CloudianPlugin',
  created () {
    this.doSso()
  },
  data () {
    return {
      showError: false
    }
  },
  methods: {
    doSso () {
      this.showError = false
      api('cloudianSsoLogin').then(json => {
        const url = json.cloudianssologinresponse.cloudianssologin.url
        const cmcWindow = window.open(url, 'CMCWindow')
        cmcWindow.focus()
      }).catch(error => {
        this.$notifyError(error)
        this.showError = true
      })
    }
  }
}
</script>
