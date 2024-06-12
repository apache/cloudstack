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
  <a-dropdown v-if="accessibleCreateActions && accessibleCreateActions.length > 0">
    <template #overlay>
      <a-menu>
        <a-menu-item style="width: 100%; padding: 12px" v-for="menuItem in accessibleCreateActions" :key="menuItem.api">
          <router-link :to="menuItem.route">
            <a-row>
              <a-col style="margin-right: 12px">
                <a-avatar :style="{ backgroundColor: $config.theme['@primary-color'] }">
                  <template #icon>
                    <render-icon v-if="(typeof menuItem.icon === 'string')" :icon="menuItem.icon" />
                    <font-awesome-icon v-else :icon="menuItem.icon" />
                  </template>
                </a-avatar>
              </a-col>
              <a-col>
                <h3 style="margin-bottom: 0px;">
                  {{ menuItem.title }}
                </h3>
                <small>{{ menuItem.subtitle }}</small>
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
  beforeCreate () {
    const menuItems = [
      {
        api: 'deployVirtualMachine',
        title: this.$t('label.instance'),
        subtitle: this.$t('label.create.instance'),
        icon: 'cloud-server-outlined',
        route: { path: '/action/deployVirtualMachine' }
      },
      {
        api: 'createKubernetesCluster',
        title: this.$t('label.kubernetes'),
        subtitle: this.$t('label.kubernetes.cluster.create'),
        icon: ['fa-solid', 'fa-dharmachakra'],
        route: { path: '/kubernetes', query: { action: 'createKubernetesCluster' } }
      },
      {
        api: 'createVolume',
        title: this.$t('label.volume'),
        subtitle: this.$t('label.action.create.volume'),
        icon: 'hdd-outlined',
        route: { path: '/volume', query: { action: 'createVolume' } }
      },
      {
        api: 'createNetwork',
        title: this.$t('label.network'),
        subtitle: this.$t('label.add.network'),
        icon: 'apartment-outlined',
        route: { path: '/guestnetwork', query: { action: 'createNetwork' } }
      },
      {
        api: 'createVPC',
        title: this.$t('label.vpc'),
        subtitle: this.$t('label.add.vpc'),
        icon: 'deployment-unit-outlined',
        route: { path: '/vpc', query: { action: 'createVPC' } }
      },
      {
        api: 'registerTemplate',
        title: this.$t('label.templatename'),
        subtitle: this.$t('label.action.register.template'),
        icon: 'picture-outlined',
        route: { path: '/template', query: { action: 'registerTemplate' } }
      },
      {
        api: 'deployVnfAppliance',
        title: this.$t('label.vnf.appliance'),
        subtitle: this.$t('label.vnf.appliance.add'),
        icon: 'gateway-outlined',
        route: { path: '/action/deployVnfAppliance' }
      }
    ]
    this.accessibleCreateActions = menuItems.filter(m => m.api in this.$store.getters.apis)
  }
}
</script>
