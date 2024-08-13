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
  <div class="footer">
    <div class="line">
      <span v-html="$config.footer" />
    </div>
    <div class="line" v-if="$store.getters.userInfo.roletype === 'Admin'">
      CloudStack {{ $store.getters.features.cloudstackversion }}
      <span v-if="showVersionUpdate()">
        <a-divider type="vertical" />
        <a
          :href="'https://github.com/apache/cloudstack/releases/tag/' + $store.getters.latestVersion.version"
          target="_blank">
            <info-circle-outlined />
            {{ $t('label.new.version.available') + ': ' + $store.getters.latestVersion.version }}
        </a>
      </span>
      <a-divider type="vertical" />
      <a href="https://github.com/apache/cloudstack/discussions" target="_blank">
        <github-outlined />
        {{ $t('label.report.bug') }}
      </a>
    </div>
  </div>
</template>

<script>
import semver from 'semver'
import { getParsedVersion } from '@/utils/util'

export default {
  name: 'LayoutFooter',
  data () {
    return {
    }
  },
  methods: {
    showVersionUpdate () {
      if (this.$store.getters?.features?.cloudstackversion && this.$store.getters?.latestVersion?.version) {
        const currentVersion = getParsedVersion(this.$store.getters?.features?.cloudstackversion)
        const latestVersion = getParsedVersion(this.$store.getters?.latestVersion?.version)
        return semver.valid(currentVersion) && semver.valid(latestVersion) && semver.gt(latestVersion, currentVersion)
      }
      return false
    }
  }
}
</script>

<style lang="less" scoped>
  .footer {
    padding: 0 16px;
    margin: 48px 0 24px;
    text-align: center;

    .line {
      margin-bottom: 8px;
    }
    .copyright {
      font-size: 14px;
    }
  }
</style>
