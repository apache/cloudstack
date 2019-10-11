export default {
  name: 'iam',
  title: 'Identity and Access',
  icon: 'solution',
  permission: [ 'listAccounts', 'listUsers', 'listDomains', 'listRoles' ],
  children: [
    {
      name: 'user',
      title: 'Users',
      icon: 'user',
      permission: [ 'listUsers' ],
      columns: ['username', 'state', 'firstname', 'lastname', 'email', 'account', 'domain'],
      details: ['username', 'id', 'firstname', 'lastname', 'email', 'usersource', 'timezone', 'rolename', 'roletype', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'createUser',
          icon: 'plus',
          label: 'label.add.user',
          listView: true,
          args: [
            'username', 'password', 'email', 'firstname', 'lastname', 'timezone', 'domainid', 'account', 'accounttype'
          ]
        },
        {
          api: 'updateUser',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: [
            'id', 'username', 'email', 'firstname', 'lastname', 'timezone'
          ]
        },
        {
          api: 'updateUser',
          icon: 'plus',
          label: 'Change password',
          dataView: true,
          args: [
            'id', 'currentPassword', 'password'
          ]
        },
        {
          api: 'registerUserKeys',
          icon: 'register',
          label: 'Register user keys',
          dataView: true,
          args: [
            'id'
          ]
        },
        {
          api: 'disableUser',
          icon: 'disabled',
          label: 'Disable user',
          dataView: true,
          hidden: (record) => { return record.resourcestate === 'Disabled' },
          args: [
            'id'
          ]
        },
        {
          api: 'enableUser',
          icon: 'enabled',
          label: 'Enable user',
          dataView: true,
          hidden: (record) => { return record.resourcestate === 'Enabled' },
          args: [
            'id'
          ]
        },
        {
          api: 'deleteUser',
          icon: 'delete',
          label: 'Delete user',
          dataView: true,
          args: [
            'id'
          ]
        }
      ]
    },
    {
      name: 'account',
      title: 'Accounts',
      icon: 'team',
      permission: [ 'listAccounts' ],
      columns: ['name', 'state', 'firstname', 'lastname', 'rolename', 'roletype', 'domain'],
      details: ['name', 'id', 'rolename', 'roletype', 'domain', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'receivedbytes', 'sentbytes', 'vmlimit', 'iplimit', 'volumelimit', 'snapshotlimit', 'templatelimit', 'vpclimit', 'cpulimit', 'memorylimit', 'networklimit', 'primarystoragelimit', 'secondarystoragelimit'],
      actions: [
        {
          api: 'createAccount',
          icon: 'plus',
          label: 'label.add.account',
          listView: true,
          args: [
            'username', 'email', 'firstname', 'lastname', 'password', 'domainid', 'account', 'roleid', 'timezone', 'networkdomain'
          ]
        },
        {
          api: 'updateResourceLimit',
          icon: 'plus',
          label: 'Update resource limit',
          dataView: true,
          args: [
            'resourceType', 'max', 'domainid', 'account'
          ]
        },
        {
          api: 'updateResourceCount',
          icon: 'plus',
          label: 'Update resource count',
          dataView: true,
          args: [
            'domainid', 'account'
          ]
        },
        {
          api: 'disableAccount',
          icon: 'plus',
          label: 'Disable account',
          dataView: true,
          hidden: (record) => { return record.resourcestate === 'Disabled' },
          args: [
            'lock', 'domainid', 'account'
          ]
        },
        {
          api: 'enableAccount',
          icon: 'plus',
          label: 'Enable account',
          dataView: true,
          hidden: (record) => { return record.resourcestate === 'Enabled' },
          args: [
            'domainid', 'account'
          ]
        },
        {
          api: 'disableAccount',
          icon: 'plus',
          label: 'Lock account',
          dataView: true,
          hidden: (record) => { return record.resourcestate === 'Disabled' },
          args: [
            'lock', 'domainid', 'account'
          ]
        },
        {
          api: 'deleteAccount',
          icon: 'delete',
          label: 'Delete account',
          dataView: true,
          hidden: (record) => { return record.name === 'admin' },
          args: [
            'id'
          ]
        }
      ]
    },
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: [ 'listDomains' ],
      resourceType: 'Domain',
      columns: ['name', 'state', 'path', 'parentdomainname', 'level'],
      details: ['name', 'id', 'path', 'parentdomainname', 'level', 'networkdomain', 'iptotal', 'vmtotal', 'volumetotal', 'vmlimit', 'iplimit', 'volumelimit', 'snapshotlimit', 'templatelimit', 'vpclimit', 'cpulimit', 'memorylimit', 'networklimit', 'primarystoragelimit', 'secondarystoragelimit'],
      actions: [
        {
          api: 'createDomain',
          icon: 'plus',
          label: 'label.add.domain',
          listView: true,
          args: [
            'parentdomainid', 'name', 'networkdomain', 'domainid'
          ]
        },
        {
          api: 'updateDomain',
          icon: 'edit',
          label: 'label.action.edit.domain',
          dataView: true,
          args: [
            'id',
            'networkdomain'
          ]
        },
        {
          api: 'updateResourceCount',
          icon: 'updateresourcecount',
          label: 'label.action.update.resource.count',
          dataView: true,
          args: [
            'domainid'
          ]
        },
        {
          api: 'deleteDomain',
          icon: 'delete',
          label: 'label.delete.domain',
          dataView: true,
          hidden: (record) => { return record.level === 0 },
          args: [
            'id',
            'cleanup'
          ]
        }
      ]
    },
    {
      name: 'role',
      title: 'Roles',
      icon: 'idcard',
      permission: [ 'listRoles' ],
      columns: ['name', 'type', 'description'],
      details: ['name', 'id', 'type', 'description'],
      actions: [
        {
          api: 'createRole',
          icon: 'plus',
          label: 'Create Role',
          listView: true,
          args: [
            'name', 'description', 'type', 'tags', 'state', 'status', 'allocationstate'
          ]
        },
        {
          api: 'updateRole',
          icon: 'edit',
          label: 'Edit role',
          dataView: true,
          args: [
            'id', 'name', 'description'
          ]
        },
        {
          api: 'deleteRole',
          icon: 'delete',
          label: 'label.delete.role',
          dataView: true,
          args: [
            'id'
          ]
        }
      ]
    }
  ]
}
