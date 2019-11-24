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
      <a
        v-if="item.meta.docHelp"
        style="margin-right: 5px"
        :href="docBase + '/' + item.meta.docHelp"
        target="_blank">
        <a-icon type="question-circle-o"></a-icon>
      </a>
      <router-link
        v-if="item && item.name"
        :to="{ path: item.path === '' ? '/' : item.path }"
      >
        <a-icon v-if="index == 0" :type="item.meta.icon" style="font-size: 16px" @click="resetToMainView" />
        {{ $t(item.meta.title) }}
      </router-link>
      <span v-else-if="$route.params.id">
        {{ $route.params.id }}
      </span>
      <span v-else>
        {{ $t(item.meta.title) }}
      </span>
      <a-tooltip v-if="index === (breadList.length - 1)" placement="bottom">
        <template slot="title">
          {{ "Refresh" }}
        </template>
        <a-button
          shape="circle"
          style="margin-left: 8px"
          @click="$emit('refresh')" >
          <a-icon
            type="reload"
            style="margin-left: 0px"
            @click="$emit('refresh')"
          />
        </a-button>
      </a-tooltip>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script>
import config from '@/config/settings'

export default {
  name: 'Breadcrumb',
  data () {
    return {
      name: '',
      breadList: [],
      docBase: config.docBase
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
  margin-bottom: 8px;
}

.ant-breadcrumb .anticon {
  margin-left: 8px;
  vertical-align: text-bottom;
}
</style>
