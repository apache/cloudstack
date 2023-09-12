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
  name: 'TungstenFabric',
  components: { TungstenFabricTableView },
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
          name: 'network.policy',
          api: 'listTungstenFabricPolicy',
          actions: [
            {
              api: 'createTungstenFabricPolicy',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.policy',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'name',
                  required: true
                }
              ]
            },
            {
              api: 'deleteTungstenFabricPolicy',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.policy',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.policy',
              args: {
                policyuuid: (record) => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              key: 'name'
            },
            {
              dataIndex: 'network',
              title: this.$t('label.network'),
              key: 'network'
            },
            {
              title: this.$t('label.actions'),
              key: 'actions',
              width: 150
            }
          ]
        },
        {
          name: 'application.policy.set',
          api: 'listTungstenFabricApplicationPolicySet',
          actions: [
            {
              api: 'createTungstenFabricApplicationPolicySet',
              label: 'label.add.tungsten.policy.set',
              icon: 'plus-outlined',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'name',
                  required: true
                }
              ]
            },
            {
              api: 'deleteTungstenFabricApplicationPolicySet',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.policy.set',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.policy.set',
              args: {
                applicationpolicysetuuid: (record) => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              key: 'name'
            },
            {
              dataIndex: 'firewallpolicy',
              title: this.$t('label.firewallpolicy'),
              key: 'firewallpolicy'
            },
            {
              dataIndex: 'tag',
              title: this.$t('label.tag'),
              key: 'tag'
            },
            {
              title: this.$t('label.actions'),
              key: 'actions',
              width: 150
            }
          ]
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
              icon: 'close-outlined',
              label: 'label.remove.logical.network',
              dataView: true,
              popup: true,
              fields: [
                {
                  label: 'label.network',
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
            key: 'name'
          }, {
            dataIndex: 'network',
            title: this.$t('label.network'),
            key: 'network'
          }, {
            title: this.$t('label.actions'),
            key: 'actions',
            width: 150
          }]
        },
        {
          name: 'tag',
          api: 'listTungstenFabricTag',
          actions: [
            {
              api: 'createTungstenFabricTag',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.tag',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'tagtype',
                  label: 'label.tag.type',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricTagType',
                  loading: false,
                  opts: []
                },
                {
                  name: 'tagvalue',
                  label: 'label.tag.value',
                  required: true
                }
              ]
            },
            {
              api: 'removeTungstenFabricTag',
              icon: 'close-outlined',
              label: 'label.remove.tungsten.tag',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'networkuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  optGet: record => record.network
                },
                {
                  name: 'vmuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  optGet: record => record.vm
                },
                {
                  name: 'nicuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  optGet: record => record.nic
                }
              ],
              show: record => record.network.length > 0 || record.vm.length > 0 || record.nic.length > 0,
              args: {
                taguuid: (record) => record.uuid
              }
            },
            {
              api: 'deleteTungstenFabricTag',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.fabric.tag',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.tag',
              args: {
                taguuid: (record) => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              key: 'name'
            },
            {
              title: this.$t('label.actions'),
              key: 'actions',
              width: 150
            }
          ]
        },
        {
          name: 'tag.type',
          api: 'listTungstenFabricTagType',
          actions: [
            {
              api: 'createTungstenFabricTagType',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.tag.type',
              dataView: false,
              listView: true,
              popup: true,
              fields: [{
                name: 'name',
                required: true
              }]
            },
            {
              api: 'deleteTungstenFabricTagType',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.fabric.tag.type',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.tag.type',
              args: {
                tagtypeuuid: (record) => record.uuid
              }
            }
          ],
          columns: [{
            dataIndex: 'name',
            title: this.$t('label.name'),
            key: 'name'
          }, {
            title: this.$t('label.actions'),
            key: 'actions',
            width: 150
          }]
        },
        {
          name: 'service.group',
          api: 'listTungstenFabricServiceGroup',
          actions: [
            {
              api: 'createTungstenFabricServiceGroup',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.service.group',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'name',
                  required: true
                },
                {
                  name: 'protocol',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  optGet: (record) => {
                    return [{
                      uuid: 'any',
                      name: 'ANY'
                    }, {
                      uuid: 'tcp',
                      name: 'TCP'
                    }, {
                      uuid: 'udp',
                      name: 'UDP'
                    }, {
                      uuid: 'icmp',
                      name: 'ICMP'
                    }]
                  },
                  value: 'any'
                },
                {
                  name: 'startport',
                  required: true,
                  value: '-1'
                },
                {
                  name: 'endport',
                  required: true,
                  value: '-1'
                }
              ]
            },
            {
              api: 'deleteTungstenFabricServiceGroup',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.service.group',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.service.group',
              args: {
                servicegroupuuid: record => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              key: 'name'
            },
            {
              dataIndex: 'protocol',
              title: this.$t('label.protocol'),
              key: 'protocol'
            },
            {
              dataIndex: 'startport',
              title: this.$t('label.startport'),
              key: 'startport'
            },
            {
              dataIndex: 'endport',
              title: this.$t('label.endport'),
              key: 'endport'
            },
            {
              title: this.$t('label.actions'),
              key: 'actions',
              width: 150
            }
          ]
        },
        {
          name: 'address.group',
          api: 'listTungstenFabricAddressGroup',
          actions: [
            {
              api: 'createTungstenFabricAddressGroup',
              icon: 'plus-outlined',
              label: 'label.add.tungsten.address.group',
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'name',
                  required: true
                },
                {
                  name: 'ipprefix',
                  required: true
                },
                {
                  name: 'ipprefixlen',
                  required: true
                }
              ]
            },
            {
              api: 'deleteTungstenFabricAddressGroup',
              icon: 'delete-outlined',
              label: 'label.delete.tungsten.address.group',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.address.group',
              args: {
                addressgroupuuid: record => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              key: 'name'
            },
            {
              dataIndex: 'ipprefix',
              title: this.$t('label.ipprefix'),
              key: 'ipprefix'
            },
            {
              dataIndex: 'ipprefixlen',
              title: this.$t('label.ipprefixlen'),
              key: 'ipprefixlen'
            },
            {
              title: this.$t('label.actions'),
              key: 'actions',
              width: 150
            }
          ]
        }
      ]
    }
  }
}
</script>

<style scoped>
</style>
