export default {
  name: 'image',
  title: 'Images',
  icon: 'picture',
  children: [
    {
      name: 'template',
      title: 'Templates',
      icon: 'save',
      permission: [ 'listTemplates' ],
      params: { 'templatefilter': 'executable' },
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'ostypename', 'status', 'hypervisor', 'account', 'domain'],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus',
          label: 'Create template',
          listView: true,
          args: ['displaytext', 'format', 'hypervisor', 'name', 'ostypeid', 'url', 'account', 'bits', 'checksum', 'details', 'directdownload', 'domainid', 'isdynamicallyscalable', 'isextractable', 'isfeatured', 'ispublic', 'isrouting', 'passwordenabled', 'projectid', 'requireshvm', 'sshkeyenabled', 'templatetag', 'zoneid', 'zoneids']
        }
      ]
    },
    {
      name: 'iso',
      title: 'ISOs',
      icon: 'usb',
      permission: [ 'listIsos' ],
      component: () => import('@/components/CloudMonkey/Resource.vue'),
      columns: ['name', 'ostypename', 'account', 'domain']
    }
  ]
}
