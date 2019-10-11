export default {
  name: 'project',
  title: 'Projects',
  icon: 'project',
  permission: [ 'listProjects' ],
  resourceType: 'Project',
  columns: ['name', 'state', 'displaytext', 'account', 'domain'],
  details: ['name', 'id', 'displaytext', 'projectaccountname', 'vmtotal', 'cputotal', 'memorytotal', 'volumetotal', 'iptotal', 'vpctotal', 'templatetotal', 'primarystoragetotal', 'account', 'domain']
}
