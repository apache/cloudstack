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
      columns: [ 'name', 'description', 'category', 'value' ]
    },
    {
      name: 'ldapsetting',
      title: 'LDAP Configuration',
      icon: 'team',
      permission: [ 'listLdapConfigurations' ],
      columns: [ 'hostname', 'port' ],
      actions: [
        {
          api: 'addLdapConfiguration',
          icon: 'plus',
          label: 'label.configure.ldap',
          listView: true,
          args: [
            'hostname', 'port'
          ]
        }
      ]
    },
    {
      name: 'hypervisorcapability',
      title: 'Hypervisor Capabilities',
      icon: 'database',
      permission: [ 'listHypervisorCapabilities' ],
      columns: [ 'hypervisor', 'hypervisorversion', 'maxguestlimit', 'maxdatavolumeslimit', 'maxhostspercluster' ],
      actions: [
        {
          api: 'updateHypervisorCapabilities',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: [
            'id', 'maxguestslimit'
          ]
        }
      ]
    }
  ]
}
