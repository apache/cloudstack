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
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['username', 'state', 'firstname', 'lastname', 'email', 'account', 'domain']
    },
    {
      name: 'account',
      title: 'Accounts',
      icon: 'team',
      permission: [ 'listAccounts' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'firstname', 'lastname', 'rolename', 'roletype', 'domain']
    },
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: [ 'listDomains' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'state', 'path', 'parentdomainname', 'level']
    },
    {
      name: 'role',
      title: 'Roles',
      icon: 'idcard',
      permission: [ 'listRoles' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'type', 'description']
    }
  ]
}
