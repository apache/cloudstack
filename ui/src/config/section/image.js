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
      component: () => import('@/components/CloudMonkey/Resource.vue')
    },
    {
      name: 'iso',
      title: 'ISOs',
      icon: 'usb',
      permission: [ 'listIsos' ],
      component: () => import('@/components/CloudMonkey/Resource.vue')
    }
  ]
}
