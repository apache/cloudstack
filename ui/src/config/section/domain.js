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

export default {
  name: 'iam',
  title: 'Domains',
  icon: 'block',
  permission: ['listDomains'],
  children: [
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: ['listDomains'],
      resourceType: 'Domain',
      columns: ['name', 'state', 'path', 'parentdomainname', 'level'],
      details: ['name', 'id', 'path', 'parentdomainname', 'level', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'vmlimit', 'iplimit', 'volumelimit', 'snapshotlimit', 'templatelimit', 'vpclimit', 'cpulimit', 'memorylimit', 'networklimit', 'primarystoragelimit', 'secondarystoragelimit'],
      related: [{
        name: 'account',
        title: 'Accounts',
        param: 'domainid'
      }],
      tabs: [
        {
          name: 'Domain',
          component: () => import('@/components/view/InfoCard.vue'),
          show: (record, route) => { return route.path === '/domain' }
        },
        {
          name: 'details',
          component: () => import('@/components/view/DetailsTab.vue')
        },
        {
          name: 'limits',
          show: (record, route, user) => { return ['Admin'].includes(user.roletype) },
          component: () => import('@/components/view/ResourceLimitTab.vue')
        },
        {
          name: 'Settings',
          component: () => import('@/components/view/SettingsTab.vue'),
          show: (record, route, user) => { return ['Admin'].includes(user.roletype) }
        }
      ],
      treeView: true,
      actions: [
        {
          api: 'createDomain',
          icon: 'plus',
          label: 'label.add.domain',
          listView: true,
          dataView: true,
          args: ['parentdomainid', 'name', 'networkdomain', 'domainid'],
          mapping: {
            parentdomainid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'updateDomain',
          icon: 'edit',
          label: 'label.action.edit.domain',
          listView: true,
          dataView: true,
          args: ['name', 'networkdomain']
        },
        {
          api: 'updateResourceCount',
          icon: 'sync',
          label: 'label.action.update.resource.count',
          listView: true,
          dataView: true,
          args: ['domainid'],
          mapping: {
            domainid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'linkDomainToLdap',
          icon: 'link',
          label: 'Link Domain to LDAP Group/OU',
          listView: true,
          dataView: true,
          args: ['type', 'domainid', 'name', 'accounttype', 'admin'],
          mapping: {
            type: {
              options: ['GROUP', 'OU']
            },
            accounttype: {
              options: ['0', '2']
            },
            domainid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteDomain',
          icon: 'delete',
          label: 'label.delete.domain',
          listView: true,
          dataView: true,
          show: (record) => { return record.level !== 0 },
          args: ['cleanup']
        }
      ]
    }
  ]
}
