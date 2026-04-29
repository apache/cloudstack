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
  <span v-if="$config.plugins.some(item => item.path && item.isExternalLink)">
    <span class="action" v-if="$config.plugins.length == 1">
      <a-tooltip placement="bottom">
        <template #title>
          {{ $t('label.redirect') + ' ' + ( $config.plugins[0].name || $config.plugins[0].path) }}
        </template>
        <a-button shape="circle" >
          <a :href=" $config.plugins[0].path" target="_blank">
            <img v-if="$config.plugins[0].icon" :src="$config.plugins[0].icon" :style="{height: '24px', padding: '2px', align: 'center'}"/>
            <link-outlined v-else/>
          </a>
        </a-button>
      </a-tooltip>
    </span>
    <a-dropdown v-else-if="$config.plugins.length > 1 && $config.plugins.some(item => item.path && item.isExternalLink)">
      <span class="action ant-dropdown-link">
        <a-button shape="circle" >
          <link-outlined/>
        </a-button>
      </span>
      <template #overlay>
        <a-menu class="user-menu-wrapper">
          <span v-for="external in $config.plugins" :key="external.isExternalLink">
            <a-menu-item  v-if="external.path && external.isExternalLink=='true'" :key="external.isExternalLink">
              <a :href="external.path" target="_blank">
                <img v-if="external.icon" :src="external.icon" :style="{ height: '18px', width: '18px', align: 'center' }"/>
                <link-outlined v-else/>
                {{ external.name || external.path }}
              </a>
            </a-menu-item>
          </span>
        </a-menu>
      </template>
    </a-dropdown>
  </span>
</template>
