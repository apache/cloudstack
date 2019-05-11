import Vue from 'vue'
import VueCookies from 'vue-cookies'
import App from './App.vue'
import router from './router'
import store from './store/'
import { VueAxios } from './utils/request'

import Spin from 'ant-design-vue/es/spin/Spin'

import bootstrap from './core/bootstrap'
import './core/use'
import './permission' // permission control
import './utils/filter' // global filter

Vue.config.productionTip = false

Vue.use(VueAxios, router, VueCookies)

Spin.setDefaultIndicator({
  indicator: (h) => {
    return <a-icon type="loading" style="font-size: 30px" spin />
  }
})

new Vue({
  router,
  store,
  created () {
    bootstrap()
  },
  render: h => h(App)
}).$mount('#app')
