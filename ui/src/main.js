import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import i18n from './locales'
import { VueAxios } from './utils/request'

import bootstrap from './core/bootstrap'
import './core/use'
import './core/ext'
import './permission' // permission control
import './utils/filter' // global filter

Vue.config.productionTip = false
Vue.use(VueAxios, router)

new Vue({
  router,
  store,
  i18n,
  created: bootstrap,
  render: h => h(App)
}).$mount('#app')
