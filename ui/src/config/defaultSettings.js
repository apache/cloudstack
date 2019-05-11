export default {
  primaryColor: '#39A7DE', // primary color of ant design
  navTheme: 'light', // theme for nav menu
  layout: 'sidemenu', // nav menu position: sidemenu or topmenu
  contentWidth: 'Fixed', // layout of content: Fluid or Fixed, only works when layout is topmenu
  fixedHeader: true, // sticky header
  fixSiderbar: true, // sticky siderbar
  autoHideHeader: false, //  auto hide header
  colorWeak: false,
  multiTab: false, // enable to have tab/route history stuff
  // CloudStack options
  apiBase: '/client/api',
  helpUrl: 'http://docs.cloudstack.apache.org',
  appTitle: 'CloudStack',
  // vue-ls options
  storageOptions: {
    namespace: 'cs__', // key prefix
    name: 'ls', // name variable Vue.[ls] or this.[$ls],
    storage: 'local' // storage name session, local, memory
  }
}
