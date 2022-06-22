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
  <span v-if="$config.externalLinks.some(item => item.link)" >
    <span class="action" v-if="$config.externalLinks.length == 1">
      <a-tooltip placement="bottom">
        <template #title>
          {{ $t('label.redirect') + ' ' + ( $config.externalLinks[0].title || $config.externalLinks[0].link) }}
        </template>
        <a-button shape="circle" >
          <a :href="$config.externalLinks[0]['link']" target="_blank">
            <img v-if="$config.externalLinksIcon" :src="$config.externalLinksIcon" :style="{height: '24px', padding: '2px', align: 'center'}"/>
            <link-outlined v-else/>
          </a>
        </a-button>
      </a-tooltip>
    </span>
    <a-dropdown v-else-if="$config.externalLinks.length > 1">
      <span class="action ant-dropdown-link">
        <a-button shape="circle" >
          <img v-if="$config.externalLinksIcon" :style="{height: '24px', padding: '2px', align: 'center'}" :src="$config.externalLinksIcon">
          <link-outlined v-else/>
        </a-button>
      </span>
      <template #overlay>
        <a-menu class="user-menu-wrapper">
          <span v-for="external in $config.externalLinks" :key="external.link">
            <a-menu-item  v-if="external.link" :key="external.link">
              <a :href="external.link" target="_blank">
                <img v-if="external.icon" :src="external.icon" :style="{ height: '18px', width: '18px', align: 'center' }"/>
                <link-outlined v-else/>
                {{ external.title || external.link }}
              </a>
            </a-menu-item>
          </span>
        </a-menu>
      </template>
    </a-dropdown>
  </span>
</template>
