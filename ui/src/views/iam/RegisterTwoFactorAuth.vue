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
  <h3> {{ $t('label.configure.app') }} </h3>
  <div> {{ $t('message.two.fa.auth.register.account') }} </div>
  <vue-qrious
    class="center-align"
    :value="resource.id"
    @change="onDataUrlChange"
  />
  <br />
  <div> {{ $t('message.two.fa.static.pin.part1') }} <a @click="generateStaticPin"> {{ $t('message.two.fa.static.pin.part2') }}</a></div>
  <br />
  <h3> {{ $t('label.enter.code') }} </h3>
  <a-form @finish="submitPin" v-ctrl-enter="submitPin" class="container">
    <a-input v-model:value="pin" />
    <div :span="24">
      <a-button ref="submit" type="primary" @click="submitPin">{{ $t('label.ok') }}</a-button>
    </div>
  </a-form>
  <a-modal
    v-if="showPin"
    :visible="showPin"
    :title="$t('label.two.factor.secret')"
    :closable="true"
    :footer="null"
    @cancel="onCloseModal"
    centered
    width="450px">
    <div> {{ pin }} </div>
  </a-modal>
</template>
<script>
import VueQrious from 'vue-qrious'
export default {
  name: 'RegisterTwoFactorAuth',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    VueQrious
  },
  data () {
    return {
      dataUrl: '',
      pin: '',
      showPin: false
    }
  },
  methods: {
    onDataUrlChange (dataUrl) {
      this.dataUrl = dataUrl
    },
    submitPin () {
      // call api
    },
    generateStaticPin () {
      this.pin = Math.floor(100000 + Math.random() * 900000)
      this.showPin = true
    },
    onCloseModal () {
      this.showPin = false
    }
  }
}
</script>
<style scoped>
  .center-align {
    display: block;
    margin-left: auto;
    margin-right: auto;
  }
  .container {
    display: flex;
  }
</style>
