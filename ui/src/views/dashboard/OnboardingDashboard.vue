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
  <div class="onboarding">
    <div class="head">
      <h2>{{ $t('label.installwizard.title') }}</h2>
      <h3>{{ $t('label.installwizard.subtitle') }}</h3>
    </div>
    <div class="body">
      <div class="intro">
        <div class="title">{{ $t('label.what.is.cloudstack') }}</div>
        <div class="subtitle">{{ $t('label.introduction.to.cloudstack') }}</div>
        <p>{{ $t('message.installwizard.copy.whatiscloudstack') }}</p>
        <img class="center" src="assets/bg-what-is-cloudstack.png">
        <pre>{{ $t('message.installwizard.cloudstack.helptext.header') }}</pre>
        <pre>{{ $t('message.installwizard.cloudstack.helptext.website') }}<a href="https://cloudstack.apache.org" target="_blank">https://cloudstack.apache.org</a></pre>
        <pre>{{ $t('message.installwizard.cloudstack.helptext.document') }}<a :href="'https://docs.cloudstack.apache.org/en/' + cloudstackminorversion" target="_blank">https://docs.cloudstack.apache.org/en/{{ cloudstackminorversion }}</a></pre>
        <pre>{{ $t('message.installwizard.cloudstack.helptext.releasenotes') }}<a :href="'https://docs.cloudstack.apache.org/en/' + cloudstackminorversion + '/releasenotes/'" target="_blank">https://docs.cloudstack.apache.org/en/{{ cloudstackminorversion }}/releasenotes/</a></pre>

        <pre>{{ $t('message.installwizard.cloudstack.helptext.mailinglists') }}<a href="https://cloudstack.apache.org/mailing-lists.html" target="_blank">https://cloudstack.apache.org/mailing-lists.html</a></pre>
        <pre>{{ $t('message.installwizard.cloudstack.helptext.survey') }}<a href="https://cloudstack.apache.org/survey.html" target="_blank">https://cloudstack.apache.org/survey.html</a></pre>
        <pre>{{ $t('message.installwizard.cloudstack.helptext.issues') }}<a href="https://github.com/apache/cloudstack/issues/new" target="_blank">https://github.com/apache/cloudstack/issues/new</a></pre>

        <a-button @click="() => { step = 1 }" type="primary">
          {{ $t('label.continue.install') }}
          <double-right-outlined />
        </a-button>
      </div>
    </div>
    <a-modal
      :title="$t('message.change.password')"
      :visible="step === 1"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="closeAction"
      centered
      width="auto">
      <change-user-password
        :resource="resource"
        @close-action="() => { if (step !== 2) step = 0 }"
        @refresh-data="() => { step = 2 }" />
    </a-modal>
    <a-modal
      :title="$t('label.installwizard.addzoneintro.title')"
      :visible="step === 2"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="closeAction"
      centered
      width="auto">
      <zone-wizard
        @close-action="closeAction"
        @refresh-data="parentFetchData" />
    </a-modal>
  </div>
</template>

<script>
import ChangeUserPassword from '@/views/iam/ChangeUserPassword.vue'
import ZoneWizard from '@/views/infra/zone/ZoneWizard.vue'

export default {
  name: 'OnboardingDashboard',
  components: {
    ChangeUserPassword,
    ZoneWizard
  },
  inject: ['parentFetchData'],
  data () {
    return {
      step: 0,
      cloudstackminorversion: this.$store.getters.features.cloudstackversion.split('-')[0],
      resource: {
        id: this.$store.getters.userInfo.id,
        username: this.$store.getters.userInfo.username
      }
    }
  },
  methods: {
    closeAction () {
      this.step = 0
    }
  }
}
</script>

<style scoped lang="scss">

.onboarding {
  font-family: sans-serif;
  padding: 20px 10px 50px 10px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  @media (min-width: 760px) {
    padding: 20px 10% 50px 10%;
  }
}

.head {
  text-align: center;
}

h2 {
  font-size: 28px;
}

h3 {
  font-size: 20px;
}

.body {
  padding: 56px 20px 20px 20px;
}

.title {
  margin: auto auto 30px;
  font-size: 22px;
}

.subtitle {
  font-size: 12px;
  font-weight: bold;
}

p {
  font-family: sans-serif;
  text-align: justify;
  font-size: 15px;
  line-height: 23px;
  white-space: pre-line;
}

pre {
  font-family: sans-serif;
  text-align: justify;
  font-size: 15px;
  white-space: pre-wrap;
  margin: 0px 0px;
}

.center {
  display: block;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 10px;
  text-align: center;
}

button {
  float: right;
}
</style>
