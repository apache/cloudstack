export default {
  name: 'project',
  title: 'Projects',
  icon: 'project',
  permission: [ 'listProjects' ],
  resourceType: 'Project',
  columns: ['name', 'state', 'displaytext', 'account', 'domain'],
  details: ['name', 'id', 'displaytext', 'projectaccountname', 'vmtotal', 'cputotal', 'memorytotal', 'volumetotal', 'iptotal', 'vpctotal', 'templatetotal', 'primarystoragetotal', 'account', 'domain'],
  actions: [
    {
      api: 'createProject',
      icon: 'plus',
      label: 'New Project',
      listView: true,
      args: ['name', 'displaytext']
    },
    {
      api: 'updateProject',
      icon: 'edit',
      label: 'Edit Project',
      dataView: true,
      args: ['id', 'displaytext']
    },
    {
      api: 'activateProject',
      icon: 'play-circle',
      label: 'Activate Project',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state === 'Suspended' }
    },
    {
      api: 'suspendProject',
      icon: 'pause-circle',
      label: 'Suspend Project',
      dataView: true,
      args: ['id'],
      show: (record) => { return record.state !== 'Suspended' }
    },
    {
      api: 'deleteProject',
      icon: 'delete',
      label: 'Delete Project',
      dataView: true,
      args: ['id']
    }
  ]
}
