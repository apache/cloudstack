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
      resourceType: 'Template',
      params: { 'templatefilter': 'executable' },
      columns: ['name', 'ostypename', 'status', 'hypervisor', 'account', 'domain'],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus',
          label: 'Create template',
          listView: true,
          args: ['displaytext', 'format', 'hypervisor', 'name', 'ostypeid', 'url', 'account', 'bits', 'checksum', 'details', 'directdownload', 'domainid', 'isdynamicallyscalable', 'isextractable', 'isfeatured', 'ispublic', 'isrouting', 'passwordenabled', 'projectid', 'requireshvm', 'sshkeyenabled', 'templatetag', 'zoneid', 'zoneids']
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: [
            'id', 'ispublic', 'isfeatured', 'isextractable'
          ]
        },
        {
          api: 'extractTemplate',
          icon: 'plus',
          label: 'Extract template',
          dataView: true,
          args: [
            'mode', 'id', 'zoneid'
          ]
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'plus',
          label: 'Update template permissions',
          dataView: true,
          args: [
            'id', 'op', 'accounts'
          ]
        }
      ]
    },
    {
      name: 'iso',
      title: 'ISOs',
      icon: 'usb',
      permission: [ 'listIsos' ],
      resourceType: 'ISO',
      columns: ['name', 'ostypename', 'account', 'domain'],
      actions: [
        {
          api: 'registerIso',
          icon: 'plus',
          label: 'Register ISO',
          listView: true,
          args: [
            'name', 'displayText', 'url', 'zoneid', 'isextractable', 'bootable', 'directdownload', 'osTypeId', 'ispublic', 'isfeatured', 'checksum'
          ]
        },
        {
          api: 'updateIsoPermissions',
          icon: 'edit',
          label: 'label.edit.iso',
          dataView: true,
          args: [
            'id', 'ispublic', 'isfeatured', 'isextractable'
          ]
        },
        {
          api: 'extractIso',
          icon: 'plus',
          label: 'label.extract.iso',
          dataView: true,
          args: [
            'mode', 'id'
          ]
        }
      ]
    }
  ]
}
