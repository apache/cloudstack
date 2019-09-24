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
      columns: ['username', 'state', 'firstname', 'lastname', 'email', 'account', 'domain']
    },
    {
      name: 'account',
      title: 'Accounts',
      icon: 'team',
      permission: [ 'listAccounts' ],
      columns: ['name', 'state', 'firstname', 'lastname', 'rolename', 'roletype', 'domain']
    },
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: [ 'listDomains' ],
      columns: ['name', 'state', 'path', 'parentdomainname', 'level']
    },
    {
      name: 'role',
      title: 'Roles',
      icon: 'idcard',
      permission: [ 'listRoles' ],
      columns: ['name', 'type', 'description']
    }
  ]
}
