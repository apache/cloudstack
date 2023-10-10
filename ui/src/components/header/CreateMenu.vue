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
  <a-dropdown>
    <template #overlay>
      <a-menu>
        <a-menu-item style="width: 100%; padding: 12px">
          <router-link :to="{ path: '/action/deployVirtualMachine'}">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <cloud-server-outlined/>
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.instance') }}
                </h3>
                <small>{{ $t('label.create.instance') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
        <a-menu-item style="width: 100%; padding: 12px" v-if="'listKubernetesClusters' in $store.getters.apis">
          <router-link :to="{ path: '/kubernetes', query: { action: 'createKubernetesCluster' } }">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <font-awesome-icon :icon="['fa-solid', 'fa-dharmachakra']" />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.kubernetes') }}
                </h3>
                <small>{{ $t('label.kubernetes.cluster.create') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
        <a-menu-item style="width: 100%; padding: 12px">
          <router-link :to="{ path: '/volume', query: { action: 'createVolume' } }">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <hdd-outlined />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.volume') }}
                </h3>
                <small>{{ $t('label.action.create.volume') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
        <a-menu-item style="width: 100%; padding: 12px">
          <router-link :to="{ path: '/guestnetwork', query: { action: 'createNetwork' } }">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <apartment-outlined />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.network') }}
                </h3>
                <small>{{ $t('label.add.network') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
        <a-menu-item style="width: 100%; padding: 12px">
          <router-link :to="{ path: '/vpc', query: { action: 'createVPC' } }">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <deployment-unit-outlined />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.vpc') }}
                </h3>
                <small>{{ $t('label.add.vpc') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
        <a-menu-item style="width: 100%; padding: 12px">
          <router-link :to="{ path: '/template', query: { action: 'registerTemplate' } }">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <picture-outlined />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ $t('label.templatename') }}
                </h3>
                <small>{{ $t('label.action.register.template') }}</small>
              </a-col>
            </a-row>
          </router-link>
        </a-menu-item>
      </a-menu>
    </template>
    <a-button type="primary">
      {{ $t('label.create') }}
      <DownOutlined />
    </a-button>
  </a-dropdown>
</template>

<script>

export default {
  name: 'CreateMenu',
  components: {
  }
}
</script>
