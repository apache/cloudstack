export default {
  name: 'config',
  title: 'Configuration',
  icon: 'setting',
  permission: [ 'listConfigurations' ],
  children: [
    {
      name: 'globalsetting',
      title: 'Global Settings',
      icon: 'global',
      permission: [ 'listConfigurations' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: [ 'name', 'description', 'category', 'value' ]
    },
    {
      name: 'ldapsetting',
      title: 'LDAP Configuration',
      icon: 'team',
      permission: [ 'listLdapConfigurations' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: [ 'hostname', 'port' ]
    },
    {
      name: 'hypervisorcapability',
      title: 'Hypervisor Capabilities',
      icon: 'database',
      permission: [ 'listHypervisorCapabilities' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: [ 'hypervisor', 'hypervisorversion', 'maxguestlimit', 'maxdatavolumeslimit', 'maxhostspercluster' ]
    }
  ]
}
