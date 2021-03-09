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
  <a-breadcrumb class="breadcrumb">
    <a-breadcrumb-item v-for="(item, index) in breadList" :key="index">
      <router-link
        v-if="item && item.name"
        :to="{ path: item.path === '' ? '/' : item.path }"
      >
        <a-icon v-if="index == 0" :type="item.meta.icon" style="font-size: 16px" @click="resetToMainView" />
        {{ $t(item.meta.title) }}
      </router-link>
      <span v-else-if="$route.params.id">
        <label
          v-if="'name' in resource &&
            ['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(resource.name)">
          <span>
            {{ $t(resource.name.toLowerCase()) }}
          </span>
        </label>
        <label v-else>
          {{ resource.displayname || resource.displaytext || resource.name || resource.hostname || resource.username || resource.ipaddress || $route.params.id }}
        </label>
      </span>
      <span v-else>
        {{ $t(item.meta.title) }}
      </span>
      <span v-if="index === (breadList.length - 1)" style="margin-left: 5px">
        <a-tooltip placement="bottom">
          <template slot="title">
            {{ $t('label.open.documentation') }}
          </template>
          <a
            v-if="item.meta.docHelp"
            style="margin-right: 12px"
            :href="$config.docBase + '/' + $route.meta.docHelp"
            target="_blank">
            <a-icon type="question-circle-o"></a-icon>
          </a>
        </a-tooltip>
        <slot name="end">
        </slot>
      </span>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script>

export default {
  name: 'Breadcrumb',
  props: {
    resource: {
      type: Object,
      default: function () {
        return {}
      }
    }
  },
  data () {
    return {
      name: '',
      breadList: []
    }
  },
  created () {
    this.getBreadcrumb()
  },
  watch: {
    $route () {
      this.getBreadcrumb()
    }
  },
  methods: {
    getBreadcrumb () {
      this.name = this.$route.name
      this.breadList = []
      this.$route.matched.forEach((item) => {
        if (item && item.parent && item.parent.name !== 'index' && !item.path.endsWith(':id')) {
          this.breadList.pop()
        }
        this.breadList.push(item)
      })
    },
    resetToMainView () {
      this.$store.dispatch('SetProject', {})
      this.$store.dispatch('ToggleTheme', 'light')
    }
  }
}
</script>

<style>
.ant-breadcrumb {
  vertical-align: text-bottom;
}

.ant-breadcrumb .anticon {
  vertical-align: text-bottom;
}
</style>
