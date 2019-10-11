export default {
  name: 'imagestore',
  title: 'Secondary Storages',
  icon: 'picture',
  permission: [ 'listImageStores' ],
  columns: [ 'name', 'url', 'protocol', 'scope', 'zonename' ],
  details: [ 'name', 'id', 'url', 'protocol', 'provider', 'scope', 'zonename' ],
  actions: [
    {
      api: 'addImageStore',
      icon: 'plus',
      label: 'label.add.secondary.storage',
      listView: true,
      args: [
        'name', 'provider', 'zoneid', 'url'
      ]
    },
    {
      api: 'deleteImageStore',
      icon: 'delete',
      label: 'label.action.delete.primary.storage',
      dataView: true,
      args: [
        'id'
      ]
    }
  ]
}
