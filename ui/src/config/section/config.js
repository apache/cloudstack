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
      searchFilters: ['domainid', 'hostname', 'port'],
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
      name: 'oauthsetting',
      title: 'label.oauth.configuration',
      icon: 'login-outlined',
      docHelp: 'adminguide/accounts.html#using-an-ldap-server-for-user-authentication',
      permission: ['listOauthProvider'],
      columns: ['provider', 'enabled', 'description', 'clientid', 'secretkey', 'redirecturi'],
      details: ['provider', 'description', 'enabled', 'clientid', 'secretkey', 'redirecturi'],
      actions: [
        {
          api: 'registerOauthProvider',
          icon: 'plus-outlined',
          label: 'label.register.oauth',
          listView: true,
          dataView: false,
          args: [
            'provider', 'description', 'clientid', 'redirecturi', 'secretkey'
          ],
          mapping: {
            provider: {
              options: ['google', 'github']
            }
          }
        },
        {
          api: 'updateOauthProvider',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          popup: true,
          args: ['description', 'clientid', 'redirecturi', 'secretkey']
        },
        {
          api: 'updateOauthProvider',
          icon: 'play-circle-outlined',
          label: 'label.enable.provider',
          message: 'message.confirm.enable.provider',
          dataView: true,
          defaultArgs: { enabled: true },
          show: (record) => { return record.enabled === false }
        },
        {
          api: 'updateOauthProvider',
          icon: 'pause-circle-outlined',
          label: 'label.disable.provider',
          message: 'message.confirm.disable.provider',
          dataView: true,
          defaultArgs: { enabled: false },
          show: (record) => { return record.enabled === true }
        },
        {
          api: 'deleteOauthProvider',
          icon: 'delete-outlined',
          label: 'label.action.delete.oauth.provider',
          message: 'message.action.delete.guest.os',
          dataView: true,
          popup: true
        }
      ]
    },
    {
      name: 'backuprepository',
      title: 'label.backup.repository',
      icon: 'inbox-outlined',
      docHelp: 'adminguide/backup_and_recovery.html',
      permission: ['listBackupRepositories'],
      searchFilters: ['zoneid'],
      columns: ['name', 'provider', 'type', 'address', 'zonename'],
      details: ['name', 'type', 'address', 'provider', 'zonename'],
      actions: [
        {
          api: 'addBackupRepository',
          icon: 'plus-outlined',
          label: 'label.backup.repository.add',
          listView: true,
          args: [
            'name', 'provider', 'address', 'type', 'mountopts', 'zoneid'
          ],
          mapping: {
            type: {
              options: ['nfs']
            },
            provider: {
              value: (record) => { return 'nas' }
            }
          }
        },
        {
          api: 'deleteBackupRepository',
          icon: 'delete-outlined',
          label: 'label.backup.repository.remove',
          message: 'message.action.delete.backup.repository',
          dataView: true,
          popup: true
        }
      ]
    },
    {
      name: 'hypervisorcapability',
      title: 'label.hypervisor.capabilities',
      icon: 'database-outlined',
      docHelp: 'adminguide/hosts.html?highlight=Hypervisor%20capabilities#hypervisor-capabilities',
      permission: ['listHypervisorCapabilities'],
      searchFilters: ['hypervisor'],
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
    },
    {
      name: 'guestos',
      title: 'label.guest.os',
      docHelp: 'adminguide/guest_os.html#guest-os',
      icon: 'laptop-outlined',
      permission: ['listOsTypes', 'listOsCategories'],
      columns: ['name', 'oscategoryname', 'isuserdefined'],
      details: ['name', 'oscategoryname', 'isuserdefined'],
      related: [{
        name: 'guestoshypervisormapping',
        title: 'label.guest.os.hypervisor.mappings',
        param: 'ostypeid'
      }],
      actions: [
        {
          api: 'addGuestOs',
          icon: 'plus-outlined',
          label: 'label.add.guest.os',
          listView: true,
          dataView: false,
          args: ['osdisplayname', 'oscategoryid'],
          mapping: {
            oscategoryid: {
              api: 'listOsCategories',
              params: (record) => { return { oscategoryid: record.id } }
            }
          }
        },
        {
          api: 'updateGuestOs',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          popup: true,
          args: ['osdisplayname']
        },
        {
          api: 'addGuestOsMapping',
          icon: 'link-outlined',
          label: 'label.add.guest.os.hypervisor.mapping',
          dataView: true,
          popup: true,
          args: ['ostypeid', 'hypervisor', 'hypervisorversion', 'osnameforhypervisor', 'osmappingcheckenabled', 'forced'],
          mapping: {
            ostypeid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'removeGuestOs',
          icon: 'delete-outlined',
          label: 'label.action.delete.guest.os',
          message: 'message.action.delete.guest.os',
          dataView: true,
          popup: true
        }
      ]
    },
    {
      name: 'guestoshypervisormapping',
      title: 'label.guest.os.hypervisor.mappings',
      docHelp: 'adminguide/guest_os.html#guest-os-hypervisor-mapping',
      icon: 'api-outlined',
      permission: ['listGuestOsMapping'],
      columns: ['hypervisor', 'hypervisorversion', 'osdisplayname', 'osnameforhypervisor'],
      details: ['hypervisor', 'hypervisorversion', 'osdisplayname', 'osnameforhypervisor', 'isuserdefined'],
      filters: ['all', 'kvm', 'vmware', 'xenserver', 'lxc', 'ovm3'],
      searchFilters: ['osdisplayname', 'osnameforhypervisor', 'hypervisor', 'hypervisorversion'],
      actions: [
        {
          api: 'addGuestOsMapping',
          icon: 'plus-outlined',
          label: 'label.add.guest.os.hypervisor.mapping',
          listView: true,
          dataView: false,
          args: ['ostypeid', 'hypervisor', 'hypervisorversion', 'osnameforhypervisor', 'osmappingcheckenabled', 'forced']
        },
        {
          api: 'updateGuestOsMapping',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          popup: true,
          args: ['osnameforhypervisor', 'osmappingcheckenabled']
        },
        {
          api: 'removeGuestOsMapping',
          icon: 'delete-outlined',
          label: 'label.action.delete.guest.os.hypervisor.mapping',
          message: 'message.action.delete.guest.os.hypervisor.mapping',
          dataView: true,
          popup: true
        }
      ]
    }
  ]
}
