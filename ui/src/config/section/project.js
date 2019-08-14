export default {
  name: 'project',
  title: 'Projects',
  icon: 'project',
  permission: [ 'listProjects' ],
  component: () => import('@/components/CloudMonkey/Resource.vue'),
  columns: ['name', 'state', 'displaytext', 'account', 'domain']
}
