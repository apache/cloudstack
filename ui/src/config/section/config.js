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
  icon: 'setting',
  permission: ['listConfigurations'],
  children: [
    {
      name: 'globalsetting',
      title: 'label.global.settings',
      icon: 'setting',
      permission: ['listConfigurations'],
      columns: ['name', 'description', 'category', 'value', 'actions']
    },
    {
      name: 'ldapsetting',
      title: 'label.ldap.configuration',
      icon: 'team',
      permission: ['listLdapConfigurations'],
      columns: ['hostname', 'port', 'domainid'],
      details: ['hostname', 'port', 'domainid'],
      actions: [
        {
          api: 'addLdapConfiguration',
          icon: 'plus',
          label: 'label.configure.ldap',
          listView: true,
          args: [
            'hostname', 'port', 'domainid'
          ]
        },
        {
          api: 'deleteLdapConfiguration',
          icon: 'delete',
          label: 'label.remove.ldap',
          message: 'message.remove.ldap',
          dataView: true,
          args: ['hostname'],
          mapping: {
            hostname: {
              value: (record) => { return record.hostname }
            }
          }
        }
      ]
    },
    {
      name: 'hypervisorcapability',
      title: 'label.hypervisor.capabilities',
      icon: 'database',
      permission: ['listHypervisorCapabilities'],
      columns: ['hypervisor', 'hypervisorversion', 'maxguestslimit', 'maxhostspercluster'],
      details: ['hypervisor', 'hypervisorversion', 'maxguestslimit', 'maxdatavolumeslimit', 'maxhostspercluster', 'securitygroupenabled', 'storagemotionenabled'],
      actions: [
        {
          api: 'updateHypervisorCapabilities',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['maxguestslimit']
        }
      ]
    }
  ]
}
