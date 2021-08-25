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
              icon: 'plus',
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
              api: 'applyTungstenFabricPolicy',
              icon: 'form',
              label: 'label.apply.tungsten.network.policy',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'networkuuid',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricNetwork',
                  loading: false,
                  opts: []
                },
                {
                  name: 'majorsequence',
                  required: true,
                  type: 'number'
                },
                {
                  name: 'minorsequence',
                  required: true,
                  type: 'number'
                }
              ],
              args: {
                policyuuid: (record) => record.uuid
              }
            },
            {
              api: 'removeTungstenFabricPolicy',
              icon: 'close',
              label: 'label.remove.tungsten.network.policy',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'networkuuid',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [],
                  optGet: (record) => record.network
                }
              ],
              show: (record) => record.network.length > 0,
              args: {
                policyuuid: (record) => record.uuid
              }
            },
            {
              api: 'deleteTungstenFabricPolicy',
              icon: 'delete',
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
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'network',
              title: this.$t('label.network'),
              scopedSlots: { customRender: 'network' }
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
              icon: 'plus',
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
              api: 'addTungstenFabricFirewallPolicy',
              icon: 'form',
              label: 'label.apply.tungsten.firewall.policy',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'firewallpolicyuuid',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricFirewallPolicy',
                  loading: false,
                  opts: []
                },
                {
                  name: 'taguuid',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricTag',
                  loading: false,
                  opts: []
                },
                {
                  name: 'sequence',
                  required: true,
                  type: 'number'
                }
              ],
              show: record => record.firewallpolicy.length === 0,
              args: {
                applicationpolicysetuuid: (record) => record.uuid
              }
            },
            {
              api: 'removeTungstenFabricFirewallPolicy',
              icon: 'close',
              label: 'label.remove.tungsten.firewall.policy',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'firewallpolicyuuid',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [],
                  optGet: (record) => record.firewallpolicy
                }
              ],
              show: (record) => record.firewallpolicy.length > 0,
              args: {
                applicationpolicysetuuid: (record) => record.uuid
              }
            },
            {
              api: 'deleteTungstenFabricApplicationPolicySet',
              icon: 'delete',
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
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'firewallpolicy',
              title: this.$t('label.firewallpolicy'),
              scopedSlots: { customRender: 'firewallpolicy' }
            },
            {
              dataIndex: 'tag',
              title: this.$t('label.tag'),
              scopedSlots: { customRender: 'tag' }
            }
          ]
        },
        {
          name: 'firewall.policy',
          api: 'listTungstenFabricFirewallPolicy',
          actions: [
            {
              api: 'createTungstenFabricFirewallPolicy',
              icon: 'plus',
              label: 'label.add.firewall.policy',
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
              api: 'addTungstenFabricFirewallRule',
              icon: 'form',
              label: 'label.add.tungsten.firewall.rule',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'firewallruleuuid',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricFirewallRule',
                  loading: false,
                  opts: []
                },
                {
                  name: 'sequence',
                  required: true,
                  type: 'number'
                }
              ],
              show: (record) => record.firewallrule.length === 0,
              args: {
                firewallpolicyuuid: (record) => record.uuid
              }
            },
            {
              api: 'removeTungstenFabricFirewallRule',
              icon: 'close',
              label: 'label.remove.tungsten.firewall.rule',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'firewallruleuuid',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [],
                  optGet: (record) => record.firewallrule
                }
              ],
              show: (record) => record.firewallrule.length > 0,
              args: {
                firewallpolicyuuid: (record) => record.uuid
              }
            },
            {
              api: 'deleteTungstenFabricFirewallPolicy',
              icon: 'delete',
              label: 'label.delete.tungsten.firewall.policy',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.firewall.policy',
              args: {
                firewallpolicyuuid: (record) => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'firewallrule',
              title: this.$t('label.firewallrule'),
              scopedSlots: { customRender: 'firewallrule' }
            }
          ]
        },
        {
          name: 'firewallrule',
          api: 'listTungstenFabricFirewallRule',
          actions: [
            {
              api: 'createTungstenFabricFirewallRule',
              icon: 'plus',
              label: 'label.add.tungsten.firewall.rule',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'name',
                  required: true
                },
                {
                  name: 'action',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [{
                    uuid: 'pass',
                    name: 'PASS'
                  }, {
                    uuid: 'deny',
                    name: 'DENY'
                  }]
                },
                {
                  name: 'servicegroupuuid',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricServiceGroup',
                  loading: false,
                  opts: []
                },
                {
                  name: 'srctaguuid',
                  required: false,
                  type: 'uuid',
                  api: 'listTungstenFabricTag',
                  loading: false,
                  opts: []
                },
                {
                  name: 'srcaddressgroupuuid',
                  required: false,
                  type: 'uuid',
                  api: 'listTungstenFabricAddressGroup',
                  loading: false,
                  opts: []
                },
                {
                  name: 'direction',
                  required: true,
                  type: 'uuid',
                  loading: false,
                  opts: [
                    {
                      id: 'oneway',
                      name: 'ONE WAY'
                    },
                    {
                      id: 'twoway',
                      name: 'TWO WAY'
                    }
                  ]
                },
                {
                  name: 'desttaguuid',
                  required: false,
                  type: 'uuid',
                  api: 'listTungstenFabricTag',
                  loading: false,
                  opts: []
                },
                {
                  name: 'destaddressgroupuuid',
                  required: false,
                  type: 'uuid',
                  api: 'listTungstenFabricAddressGroup',
                  loading: false,
                  opts: []
                },
                {
                  name: 'tagtypeuuid',
                  required: false,
                  type: 'uuid',
                  api: 'listTungstenFabricTagType',
                  loading: false,
                  opts: []
                }
              ]
            },
            {
              api: 'deleteTungstenFabricFirewallRule',
              icon: 'delete',
              label: 'label.delete.tungsten.firewall.rule',
              dataView: true,
              confirm: true,
              message: 'label.confirm.delete.tungsten.firewall.rule',
              args: {
                firewallruleuuid: (record) => record.uuid
              }
            }
          ],
          columns: [
            {
              dataIndex: 'name',
              title: this.$t('label.name'),
              scopedSlots: { customRender: 'name' }
            }
          ]
        },
        {
          name: 'tag',
          api: 'listTungstenFabricTag',
          actions: [
            {
              api: 'createTungstenFabricTag',
              icon: 'plus',
              label: 'label.add.tungsten.tag',
              dataView: false,
              listView: true,
              popup: true,
              fields: [
                {
                  name: 'tagtype',
                  required: true,
                  type: 'uuid',
                  api: 'listTungstenFabricTagType',
                  loading: false,
                  opts: []
                },
                {
                  name: 'tagvalue',
                  required: true
                }
              ]
            },
            {
              api: 'applyTungstenFabricTag',
              icon: 'form',
              label: 'label.apply.tungsten.tag',
              dataView: true,
              popup: true,
              fields: [
                {
                  name: 'networkuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  api: 'listTungstenFabricNetwork'
                },
                {
                  name: 'vmuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  api: 'listTungstenFabricVm'
                },
                {
                  name: 'nicuuid',
                  type: 'uuid',
                  multiple: true,
                  loading: false,
                  opts: [],
                  api: 'listTungstenFabricNic'
                }
              ],
              args: {
                taguuid: (record) => record.uuid
              }
            },
            {
              api: 'removeTungstenFabricTag',
              icon: 'close',
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
              icon: 'delete',
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
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'network',
              title: this.$t('label.network'),
              scopedSlots: { customRender: 'network' }
            },
            {
              dataIndex: 'vm',
              title: this.$t('label.instance'),
              scopedSlots: { customRender: 'vm' }
            },
            {
              dataIndex: 'nic',
              title: this.$t('label.nic'),
              scopedSlots: { customRender: 'nic' }
            }
          ]
        },
        {
          name: 'tag.type',
          api: 'listTungstenFabricTagType',
          actions: [
            {
              api: 'createTungstenFabricTagType',
              icon: 'plus',
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
              icon: 'delete',
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
            scopedSlots: { customRender: 'name' }
          }]
        },
        {
          name: 'service.group',
          api: 'listTungstenFabricServiceGroup',
          actions: [
            {
              api: 'createTungstenFabricServiceGroup',
              icon: 'plus',
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
                  required: true
                },
                {
                  name: 'startport',
                  required: true
                },
                {
                  name: 'endport',
                  required: true
                }
              ]
            },
            {
              api: 'deleteTungstenFabricServiceGroup',
              icon: 'delete',
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
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'protocol',
              title: this.$t('label.protocol'),
              scopedSlots: { customRender: 'protocol' }
            },
            {
              dataIndex: 'startport',
              title: this.$t('label.startport'),
              scopedSlots: { customRender: 'startport' }
            },
            {
              dataIndex: 'endport',
              title: this.$t('label.endport'),
              scopedSlots: { customRender: 'endport' }
            }
          ]
        },
        {
          name: 'address.group',
          api: 'listTungstenFabricAddressGroup',
          actions: [
            {
              api: 'createTungstenFabricAddressGroup',
              icon: 'plus',
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
              icon: 'delete',
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
              scopedSlots: { customRender: 'name' }
            },
            {
              dataIndex: 'ipprefix',
              title: this.$t('label.ipprefix'),
              scopedSlots: { customRender: 'ipprefix' }
            },
            {
              dataIndex: 'ipprefixlen',
              title: this.$t('label.ipprefixlen'),
              scopedSlots: { customRender: 'ipprefixlen' }
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
