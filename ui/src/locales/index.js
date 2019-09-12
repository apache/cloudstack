import Vue from 'vue'
import VueI18n from 'vue-i18n'

function loadLocaleMessages () {
  const locales = require.context('./', true, /[A-Za-z0-9-_,\s]+\.json$/i)
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

Vue.use(VueI18n)

export default new VueI18n({
  locale: Vue.ls ? Vue.ls.get('current_locale') || 'en' : 'en',
  fallbackLocale: 'en',
  messages: loadLocaleMessages()
})
