export default {
  name: 'config',
  title: 'Configurations',
  icon: 'setting',
  permission: [ 'listConfigurations' ],
  children: [
    {
      name: 'globalsetting',
      title: 'Global Settings',
      icon: 'global',
      permission: [ 'listConfigurations' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'ldapsetting',
      title: 'LDAP Settings',
      icon: 'team',
      permission: [ 'listLdapConfigurations' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'hypervisorcapability',
      title: 'Hypervisor Capabilities',
      icon: 'database',
      permission: [ 'listHypervisorCapabilities' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
