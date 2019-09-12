import Vue from 'vue'
import VueI18n from 'vue-i18n'
import App from './App.vue'
import router from './router'
import store from './store'
import { VueAxios } from './utils/request'

import Spin from 'ant-design-vue/es/spin/Spin'

import { library } from '@fortawesome/fontawesome-svg-core'
import { fab } from '@fortawesome/free-brands-svg-icons'
import { fas } from '@fortawesome/free-solid-svg-icons'
import { far } from '@fortawesome/free-regular-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

import bootstrap from './core/bootstrap'
import './core/use'
import './permission' // permission control
import './utils/filter' // global filter

library.add(fab, far, fas)
Vue.component('font-awesome-icon', FontAwesomeIcon)
Vue.config.productionTip = false

Vue.use(VueI18n, VueAxios, router)

Spin.setDefaultIndicator({
  indicator: (h) => {
    return <a-icon type="loading" style="font-size: 30px" spin />
  }
})

function loadLocaleMessages () {
  const locales = require.context('./locales', true, /[A-Za-z0-9-_,\s]+\.json$/i)
  const messages = {}
  locales.keys().forEach(key => {
    const matched = key.match(/([A-Za-z0-9-_]+)\./i)
    if (matched && matched.length > 1) {
      const locale = matched[1]
      messages[locale] = locales(key)
    }
  })
  return messages
}

const i18n = new VueI18n({
  locale: Vue.ls.get('current_locale') || 'en',
  fallbackLocale: 'en',
  messages: loadLocaleMessages()
})

export default i18n

new Vue({
  router,
  store,
  i18n,
  created () {
    bootstrap()
  },
  render: h => h(App)
}).$mount('#app')
