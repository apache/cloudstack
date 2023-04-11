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
  name: 'config',
  title: 'label.configuration',
  icon: 'setting-outlined',
  permission: ['listConfigurations', 'listInfrastructure'],
  children: [
    {
      name: 'globalsetting',
      title: 'label.global.settings',
      icon: 'setting-outlined',
      docHelp: 'adminguide/index.html#tuning',
      permission: ['listConfigurations'],
      listView: true,
      popup: true,
      component: () => import('@/views/setting/ConfigurationTab.vue')
    },
    {
      name: 'ldapsetting',
      title: 'label.ldap.configuration',
      icon: 'team-outlined',
      docHelp: 'adminguide/accounts.html#using-an-ldap-server-for-user-authentication',
      permission: ['listLdapConfigurations'],
      columns: ['hostname', 'port', 'domainid'],
      details: ['hostname', 'port', 'domainid'],
      actions: [
        {
          api: 'addLdapConfiguration',
          icon: 'plus-outlined',
          label: 'label.configure.ldap',
          listView: true,
          args: [
            'hostname', 'port', 'domainid'
          ]
        },
        {
          api: 'deleteLdapConfiguration',
          icon: 'delete-outlined',
          label: 'label.remove.ldap',
          message: 'message.remove.ldap',
          dataView: true,
          args: ['hostname', 'port', 'domainid'],
          mapping: {
            hostname: {
              value: (record) => { return record.hostname }
            },
            port: {
              value: (record) => { return record.port }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        }
      ]
    },
    {
      name: 'hypervisorcapability',
      title: 'label.hypervisor.capabilities',
      icon: 'database-outlined',
      docHelp: 'adminguide/hosts.html?highlight=Hypervisor%20capabilities#hypervisor-capabilities',
      permission: ['listHypervisorCapabilities'],
      columns: ['hypervisor', 'hypervisorversion', 'maxguestslimit', 'maxhostspercluster'],
      details: ['hypervisor', 'hypervisorversion', 'maxguestslimit', 'maxdatavolumeslimit', 'maxhostspercluster', 'securitygroupenabled', 'storagemotionenabled'],
      actions: [
        {
          api: 'updateHypervisorCapabilities',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          args: ['maxguestslimit']
        }
      ]
    }
  ]
}
