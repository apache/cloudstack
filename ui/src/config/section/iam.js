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
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'account',
      title: 'Accounts',
      icon: 'team',
      permission: [ 'listAccounts' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'domain',
      title: 'Domains',
      icon: 'block',
      permission: [ 'listDomains' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'role',
      title: 'Roles',
      icon: 'idcard',
      permission: [ 'listRoles' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
