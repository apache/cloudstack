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
      details: ['name', 'id', 'displaytext', 'checksum', 'hypervisor', 'format', 'ostypename', 'size', 'isready', 'passwordenabled', 'directdownload', 'isextractable', 'isdynamicallyscalable', 'ispublic', 'isfeatured', 'crosszones', 'type', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus',
          label: 'Create template',
          listView: true,
          args: ['url', 'name', 'displaytext', 'directdownload', 'zoneids', 'hypervisor', 'format', 'ostypeid', 'checksum', 'isextractable', 'passwordenabled', 'sshkeyenabled', 'isdynamicallyscalable', 'ispublic', 'isfeatured', 'isrouting', 'requireshvm']
        },
        {
          api: 'getUploadParamsForVolume',
          icon: 'upload',
          label: 'Upload Local Template',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalTemplate.vue')
        },
        {
          api: 'updateTemplate',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['id', 'name', 'displaytext', 'passwordenabled', 'sshkeyenabled', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractTemplate',
          icon: 'cloud-download',
          label: 'Download Template',
          dataView: true,
          args: ['id', 'zoneid', 'mode']
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'reconciliation',
          label: 'Update template permissions',
          dataView: true,
          args: ['id', 'op', 'accounts', 'projectids']
        },
        {
          api: 'copyTemplate',
          icon: 'copy',
          label: 'Copy Template',
          args: ['id', 'sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteTemplate',
          icon: 'delete',
          label: 'Delete Template',
          args: ['id', 'zoneid'],
          dataView: true,
          groupAction: true
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
      details: ['name', 'id', 'displaytext', 'checksum', 'ostypename', 'size', 'bootable', 'isready', 'directdownload', 'isextractable', 'ispublic', 'isfeatured', 'crosszones', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'registerIso',
          icon: 'plus',
          label: 'Register ISO',
          listView: true,
          args: ['url', 'name', 'displaytext', 'directdownload', 'zoneid', 'bootable', 'ostypeid', 'isextractable', 'ispublic', 'isfeatured']
        },
        {
          api: 'getUploadParamsForIso',
          icon: 'upload',
          label: 'Upload Local Iso',
          listView: true,
          popup: true,
          component: () => import('@/views/storage/UploadLocalIso.vue')
        },
        {
          api: 'updateIso',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['id', 'name', 'displaytext', 'bootable', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractIso',
          icon: 'cloud-download',
          label: 'Download ISO',
          dataView: true,
          args: ['id', 'zoneid', 'mode']
        },
        {
          api: 'updateIsoPermissions',
          icon: 'reconciliation',
          label: 'Update ISO Permissions',
          dataView: true,
          args: ['id', 'op', 'accounts', 'projectids']
        },
        {
          api: 'copyIso',
          icon: 'copy',
          label: 'Copy ISO',
          args: ['id', 'sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteIso',
          icon: 'delete',
          label: 'Delete ISO',
          args: ['id', 'zoneid'],
          dataView: true,
          groupAction: true
        }
      ]
    }
  ]
}
