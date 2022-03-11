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
    <a-tabs tab-position="left">
      <a-tab-pane
        v-for="tab in tabs"
        :key="tab.name"
        :tab="$t('label.' + tab.name)">
        <tungsten-fabric-table-view
          :apiName="tab.api"
          :actions="tab.actions"
          :columns="tab.columns"
          :resource="resource"
          :loading="loading"
          :tab="tab.name" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import TungstenFabricTableView from '@/views/network/tungsten/TungstenFabricTableView'

export default {
  name: 'TungstenFabricRouting',
  components: {
    TungstenFabricTableView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      tabs: [
        {
          name: 'network.route.table',
          api: 'listTungstenFabricNetworkRouteTable',
          actions: [
            {
              api: 'createTungstenFabricNetworkRouteTable',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.fabric.route',
              listView: true,
              popup: true,
              fields: [{
                name: 'tungstennetworkroutetablename',
                required: true
              }]
            },
            {
              api: 'removeTungstenFabricNetworkRouteTable',
              icon: 'delete-outlined',
              label: 'label.remove.network.route.table',
              dataView: true,
              confirm: true,
              message: 'label.confirm.remove.network.route.table',
              args: {
                tungstennetworkroutetableuuid: record => record.uuid
              }
            }
          ],
          columns: [{
            dataIndex: 'name',
            title: this.$t('label.name'),
            slots: { customRender: 'name' }
          }, {
            dataIndex: 'network',
            title: this.$t('label.network'),
            slots: { customRender: 'network' }
          }, {
            title: this.$t('label.action'),
            slots: { customRender: 'action' },
            width: 150
          }]
        },
        {
          name: 'interface.route.table',
          api: 'listTungstenFabricInterfaceRouteTable',
          actions: [
            {
              api: 'createTungstenFabricInterfaceRouteTable',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.interface.route',
              listView: true,
              popup: true,
              fields: [{
                name: 'tungsteninterfaceroutetablename',
                required: true
              }]
            },
            {
              api: 'removeTungstenFabricInterfaceRouteTable',
              icon: 'delete-outlined',
              label: 'label.remove.interface.route.table',
              dataView: true,
              confirm: true,
              message: 'label.confirm.remove.route.table',
              args: {
                tungsteninterfaceroutetableuuid: record => record.uuid
              }
            }
          ],
          columns: [{
            dataIndex: 'name',
            title: this.$t('label.name'),
            slots: { customRender: 'name' }
          }, {
            dataIndex: 'tungstenvms',
            title: this.$t('label.tungstenvms'),
            slots: { customRender: 'tungstenvms' }
          }, {
            title: this.$t('label.action'),
            slots: { customRender: 'action' },
            width: 150
          }]
        },
        {
          name: 'routing.policy',
          api: 'listTungstenFabricRoutingPolicy',
          actions: [
            {
              api: 'createTungstenFabricRoutingPolicy',
              icon: 'plus-outlined',
              label: 'label.add.routing.policy',
              title: 'label.create.tungsten.routing.policy',
              listView: true,
              popup: true,
              component: () => import('@/views/network/tungsten/AddRoutingPolicy.vue')
            },
            {
              api: 'removeTungstenFabricRoutingPolicy',
              icon: 'delete-outlined',
              label: 'label.remove.routing.policy',
              dataView: true,
              confirm: true,
              message: 'message.confirm.remove.routing.policy',
              args: {
                tungstenroutingpolicyuuid: record => record.uuid
              }
            }
          ],
          columns: [{
            dataIndex: 'name',
            title: this.$t('label.name'),
            slots: { customRender: 'name' }
          }, {
            dataIndex: 'tungstenroutingpolicyterm',
            title: this.$t('label.tungstenroutingpolicyterm'),
            slots: { customRender: 'tungstenroutingpolicyterm' }
          }, {
            title: this.$t('label.action'),
            slots: { customRender: 'action' },
            width: 150
          }]
        },
        {
          name: 'tungsten.logical.router',
          api: 'listTungstenFabricLogicalRouter',
          actions: [
            {
              api: 'createTungstenFabricLogicalRouter',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.logical.route',
              listView: true,
              popup: true,
              fields: [{
                name: 'name',
                required: true
              }]
            },
            {
              api: 'removeTungstenFabricNetworkGatewayFromLogicalRouter',
              icon: 'close',
              label: 'label.remove.logical.network',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'networkuuid',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [],
                  optGet: record => record.network
                }
              ],
              show: record => record.network.length > 0,
              args: {
                logicalrouteruuid: record => record.uuid
              }
            },
            {
              api: 'deleteTungstenFabricLogicalRouter',
              icon: 'delete-outlined',
              label: 'label.remove.logical.router',
              dataView: true,
              confirm: true,
              message: 'label.confirm.remove.logical.router',
              args: {
                logicalrouteruuid: record => record.uuid
              }
            }
          ],
          columns: [{
            dataIndex: 'name',
            title: this.$t('label.name'),
            slots: { customRender: 'name' }
          }, {
            dataIndex: 'network',
            title: this.$t('label.network'),
            slots: { customRender: 'network' }
          }, {
            title: this.$t('label.action'),
            slots: { customRender: 'action' },
            width: 150
          }]
        }
      ]
    }
  }
}
</script>

<style scoped>
</style>
