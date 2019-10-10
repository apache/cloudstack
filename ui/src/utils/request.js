import Vue from 'vue'
import axios from 'axios'
import store from '@/store'
import config from '@/config/settings'
import { VueAxios } from './axios'
import notification from 'ant-design-vue/es/notification'
import { ACCESS_TOKEN, CURRENT_PROJECT } from '@/store/mutation-types'

const service = axios.create({
  baseURL: config.apiBase,
  timeout: 60000
})

const err = (error) => {
  if (error.response) {
    console.log('error has occurred')
    console.log(error)
    const token = Vue.ls.get(ACCESS_TOKEN)
    if (error.response.status === 403) {
      const data = error.response.data
      notification.error({ message: 'Forbidden', description: data.message })
    }
    if (error.response.status === 401) {
      notification.error({ message: 'Unauthorized', description: 'Authorization verification failed' })
      if (token) {
        store.dispatch('Logout').then(() => {
          setTimeout(() => {
            window.location.reload()
          }, 1500)
        })
      }
    }
    if (error.response.status === 404) {
      notification.error({ message: 'Not Found', description: 'Resource not found' })
      this.$router.push({ path: '/exception/404' })
    }
  }
  return Promise.reject(error)
}

// request interceptor
service.interceptors.request.use(config => {
  const project = Vue.ls.get(CURRENT_PROJECT)
  if (config && config.params) {
    config.params['response'] = 'json'
    if (project && project.id) {
      config.params['projectid'] = project.id
    }
  }
  return config
}, err)

// response interceptor
service.interceptors.response.use((response) => {
  return response.data
}, err)

const installer = {
  vm: {},
  install (Vue) {
    Vue.use(VueAxios, service)
  }
}

export {
  installer as VueAxios,
  service as axios
}
