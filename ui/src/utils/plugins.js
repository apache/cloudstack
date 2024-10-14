// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import _ from 'lodash'
import { i18n } from '@/locales'
import { api } from '@/api'
import { message, notification } from 'ant-design-vue'
import eventBus from '@/config/eventBus'
import store from '@/store'
import { sourceToken } from '@/utils/request'
import { toLocalDate, toLocaleDate } from '@/utils/date'

export const pollJobPlugin = {
  install (app) {
    app.config.globalProperties.$pollJob = function (options) {
      /**
       * @param {String} jobId
       * @param {String} [name='']
       * @param {String} [title='']
       * @param {String} [description='']
       * @param {String} [successMessage=Success]
       * @param {Function} [successMethod=() => {}]
       * @param {String} [errorMessage=Error]
       * @param {Function} [errorMethod=() => {}]
       * @param {Object} [showLoading=true]
       * @param {String} [loadingMessage=Loading...]
       * @param {String} [catchMessage=Error caught]
       * @param {Function} [catchMethod=() => {}]
       * @param {Object} [action=null]
       * @param {Object} [bulkAction=false]
       * @param {String} resourceId
       */
      const {
        jobId,
        name = '',
        title = '',
        description = '',
        successMessage = i18n.global.t('label.success'),
        successMethod = () => {},
        errorMessage = i18n.global.t('label.error'),
        errorMethod = () => {},
        loadingMessage = `${i18n.global.t('label.loading')}...`,
        showLoading = true,
        catchMessage = i18n.global.t('label.error.caught'),
        catchMethod = () => {},
        action = null,
        bulkAction = false,
        resourceId = null
      } = options

      store.dispatch('AddHeaderNotice', {
        key: jobId,
        title,
        description,
        status: 'progress',
        timestamp: new Date()
      })

      eventBus.on('update-job-details', (args) => {
        const { jobId, resourceId } = args
        const fullPath = this.$route.fullPath
        const path = this.$route.path
        var jobs = this.$store.getters.headerNotices.map(job => {
          if (job.key === jobId) {
            if (resourceId && !path.includes(resourceId)) {
              job.path = path + '/' + resourceId
            } else {
              job.path = fullPath
            }
          }
          return job
        })
        this.$store.commit('SET_HEADER_NOTICES', jobs)
      })

      options.originalPage = options.originalPage || this.$router.currentRoute.value.path
      api('queryAsyncJobResult', { jobId }).then(json => {
        const result = json.queryasyncjobresultresponse
        eventBus.emit('update-job-details', { jobId, resourceId })
        if (result.jobstatus === 1) {
          var content = successMessage
          if (successMessage === 'Success' && action && action.label) {
            content = i18n.global.t(action.label)
          }
          if (name) {
            content = content + ' - ' + name
          }
          message.success({
            content,
            key: jobId,
            duration: 2
          })
          store.dispatch('AddHeaderNotice', {
            key: jobId,
            title,
            description,
            status: 'done',
            duration: 2,
            timestamp: new Date()
          })
          eventBus.emit('update-job-details', { jobId, resourceId })
          // Ensure we refresh on the same / parent page
          const currentPage = this.$router.currentRoute.value.path
          const samePage = options.originalPage === currentPage || options.originalPage.startsWith(currentPage + '/')
          if (samePage && (!action || !('isFetchData' in action) || (action.isFetchData))) {
            eventBus.emit('async-job-complete', action)
          }
          successMethod(result)
        } else if (result.jobstatus === 2) {
          if (!bulkAction) {
            message.error({
              content: errorMessage,
              key: jobId,
              duration: 1
            })
          }
          var errMessage = errorMessage
          if (action && action.label) {
            errMessage = i18n.global.t(action.label)
          }
          var desc = result.jobresult.errortext
          if (name) {
            desc = `(${name}) ${desc}`
          }
          let onClose = () => {}
          if (!bulkAction) {
            let countNotify = store.getters.countNotify
            countNotify++
            store.commit('SET_COUNT_NOTIFY', countNotify)
            onClose = () => {
              let countNotify = store.getters.countNotify
              countNotify > 0 ? countNotify-- : countNotify = 0
              store.commit('SET_COUNT_NOTIFY', countNotify)
            }
          }
          notification.error({
            top: '65px',
            message: errMessage,
            description: desc,
            key: jobId,
            duration: 0,
            onClose: onClose
          })
          store.dispatch('AddHeaderNotice', {
            key: jobId,
            title,
            description: desc,
            status: 'failed',
            duration: 2,
            timestamp: new Date()
          })
          eventBus.emit('update-job-details', { jobId, resourceId })
          // Ensure we refresh on the same / parent page
          const currentPage = this.$router.currentRoute.value.path
          const samePage = options.originalPage === currentPage || options.originalPage.startsWith(currentPage + '/')
          if (samePage && (!action || !('isFetchData' in action) || (action.isFetchData))) {
            eventBus.emit('async-job-complete', action)
          }
          errorMethod(result)
        } else if (result.jobstatus === 0) {
          if (showLoading) {
            message.loading({
              content: loadingMessage,
              key: jobId,
              duration: 0
            })
          }
          setTimeout(() => {
            this.$pollJob(options, action)
          }, 3000)
        }
      }).catch(e => {
        console.error(`${catchMessage} - ${e}`)
        if (!sourceToken.isCancel(e)) {
          let countNotify = store.getters.countNotify
          countNotify++
          store.commit('SET_COUNT_NOTIFY', countNotify)
          notification.error({
            top: '65px',
            message: i18n.global.t('label.error'),
            description: catchMessage,
            duration: 0,
            onClose: () => {
              let countNotify = store.getters.countNotify
              countNotify > 0 ? countNotify-- : countNotify = 0
              store.commit('SET_COUNT_NOTIFY', countNotify)
            }
          })
        }
        catchMethod && catchMethod()
      })
    }
  }

}

export const notifierPlugin = {
  install (app) {
    app.config.globalProperties.$notifyError = function (error) {
      console.log(error)
      var msg = i18n.global.t('message.request.failed')
      var desc = ''
      if (error && error.response) {
        if (error.response.status) {
          msg = `${i18n.global.t('message.request.failed')} (${error.response.status})`
        }
        if (error.message) {
          desc = error.message
        }
        if (error.response.headers && 'x-description' in error.response.headers) {
          desc = error.response.headers['x-description']
        }
        if (desc === '' && error.response.data) {
          const responseKey = _.findKey(error.response.data, 'errortext')
          if (responseKey) {
            desc = error.response.data[responseKey].errortext
          }
        }
      }
      let countNotify = store.getters.countNotify
      countNotify++
      store.commit('SET_COUNT_NOTIFY', countNotify)
      notification.error({
        top: '65px',
        message: msg,
        description: desc,
        duration: 0,
        onClose: () => {
          let countNotify = store.getters.countNotify
          countNotify > 0 ? countNotify-- : countNotify = 0
          store.commit('SET_COUNT_NOTIFY', countNotify)
        }
      })
    }

    app.config.globalProperties.$notification = {
      defaultConfig: {
        top: '65px',
        onClose: () => {
          let countNotify = store.getters.countNotify
          countNotify > 0 ? countNotify-- : countNotify = 0
          store.commit('SET_COUNT_NOTIFY', countNotify)
        }
      },
      setCountNotify: () => {
        let countNotify = store.getters.countNotify
        countNotify++
        store.commit('SET_COUNT_NOTIFY', countNotify)
      },
      info: (config) => {
        app.config.globalProperties.$notification.setCountNotify()
        config = Object.assign({}, app.config.globalProperties.$notification.defaultConfig, config)
        notification.info(config)
      },
      error: (config) => {
        app.config.globalProperties.$notification.setCountNotify()
        config = Object.assign({}, app.config.globalProperties.$notification.defaultConfig, config)
        notification.error(config)
      },
      success: (config) => {
        app.config.globalProperties.$notification.setCountNotify()
        config = Object.assign({}, app.config.globalProperties.$notification.defaultConfig, config)
        notification.success(config)
      },
      warning: (config) => {
        app.config.globalProperties.$notification.setCountNotify()
        config = Object.assign({}, app.config.globalProperties.$notification.defaultConfig, config)
        notification.warning(config)
      },
      warn: (config) => {
        app.config.globalProperties.$notification.setCountNotify()
        config = Object.assign({}, app.config.globalProperties.$notification.defaultConfig, config)
        notification.warn(config)
      },
      close: (key) => notification.close(key),
      destroy: () => notification.destroy()
    }

    app.config.globalProperties.$messageConfigSuccess = function (msg, configrecord) {
      if (configrecord.isdynamic) {
        msg += `. ${this.$t('message.setting.update.delay')}`
      }
      message.success(msg)
    }
  }
}

export const toLocaleDatePlugin = {
  install (app) {
    app.config.globalProperties.$toLocaleDate = function (date) {
      const { timezoneoffset, usebrowsertimezone } = this.$store.getters
      return toLocaleDate({ date, timezoneoffset, usebrowsertimezone })
    }

    app.config.globalProperties.$toLocalDate = function (date) {
      const { timezoneoffset, usebrowsertimezone } = this.$store.getters
      return toLocalDate({ date, timezoneoffset, usebrowsertimezone }).toISOString()
    }
  }
}

export const configUtilPlugin = {
  install (app) {
    app.config.globalProperties.$applyDocHelpMappings = function (docHelp) {
      var docHelpMappings = this.$config.docHelpMappings
      if (docHelp && docHelpMappings &&
        docHelpMappings.constructor === Object && Object.keys(docHelpMappings).length > 0) {
        for (var key in docHelpMappings) {
          if (docHelp.includes(key) && docHelp !== docHelpMappings[key]) {
            docHelp = docHelp.replace(key, docHelpMappings[key])
            break
          }
        }
      }
      return docHelp
    }
  }
}

export const showIconPlugin = {
  install (app) {
    app.config.globalProperties.$showIcon = function (resource) {
      var resourceType = this.$route.path.split('/')[1]
      if (resource) {
        resourceType = resource
      }
      if (['zone', 'zones', 'template', 'iso', 'account', 'accountuser', 'vm', 'domain', 'project', 'vpc', 'guestnetwork'].includes(resourceType)) {
        return true
      } else {
        return false
      }
    }
  }
}

export const resourceTypePlugin = {
  install (app) {
    app.config.globalProperties.$getResourceType = function () {
      const type = this.$route.path.split('/')[1]
      if (type === 'vm') {
        return 'UserVM'
      } else if (type === 'accountuser') {
        return 'User'
      } else if (type === 'guestnetwork') {
        return 'Network'
      } else {
        return type
      }
    }

    app.config.globalProperties.$getRouteFromResourceType = function (resourceType) {
      switch (resourceType) {
        case 'VirtualMachine':
          return 'vm'
        case 'DomainRouter':
          return 'router'
        case 'ConsoleProxy':
          return 'systemvm'
        case 'User':
          return 'accountuser'
        case 'Network':
          return 'guestnetwork'
        case 'ServiceOffering':
          return 'computeoffering'
        case 'IpAddress':
          return 'publicip'
        case 'NetworkAcl':
          return 'acllist'
        case 'KubernetesCluster':
          return 'kubernetes'
        case 'KubernetesSupportedVersion':
          return 'kubernetesiso'
        case 'SystemVm':
        case 'PhysicalNetwork':
        case 'Backup':
        case 'SecurityGroup':
        case 'StoragePool':
        case 'ImageStore':
        case 'Template':
        case 'Iso':
        case 'Host':
        case 'Volume':
        case 'Account':
        case 'Snapshot':
        case 'Project':
        case 'Domain':
        case 'DiskOffering':
        case 'NetworkOffering':
        case 'VpcOffering':
        case 'BackupOffering':
        case 'Zone':
        case 'Vpc':
        case 'VmSnapshot':
        case 'Pod':
        case 'Cluster':
        case 'Role':
        case 'AffinityGroup':
        case 'VpnCustomerGateway':
        case 'AutoScaleVmGroup':
        case 'QuotaTariff':
          return resourceType.toLowerCase()
      }
      return ''
    }

    app.config.globalProperties.$getIconFromResourceType = function (resourceType) {
      var routePath = this.$getRouteFromResourceType(resourceType)
      if (!routePath) return ''
      var route = this.$router.resolve('/' + routePath)
      return route?.meta?.icon || ''
    }
  }
}

export const apiMetaUtilPlugin = {
  install (app) {
    app.config.globalProperties.$getApiParams = function () {
      var apiParams = {}
      for (var argument of arguments) {
        var apiConfig = this.$store.getters.apis[argument] || {}
        if (apiConfig && apiConfig.params) {
          apiConfig.params.forEach(param => {
            apiParams[param.name] = param
          })
        }
      }
      return apiParams
    }
  }
}

export const localesPlugin = {
  install (app) {
    app.config.globalProperties.$t = i18n.global.t
  }
}

const KiB = 1024
const MiB = 1024 * KiB
const GiB = 1024 * MiB
const TiB = 1024 * GiB

export const fileSizeUtilPlugin = {
  install (app) {
    app.config.globalProperties.$bytesToGiB = function (bytes) {
      if (bytes == null || bytes === 0) {
        return 0
      }
      return (bytes / GiB).toFixed(2)
    }
    app.config.globalProperties.$bytesToHumanReadableSize = function (bytes) {
      if (bytes == null) {
        return ''
      }
      if (bytes < KiB && bytes >= 0) {
        return bytes + ' bytes'
      }
      if (bytes < MiB) {
        return (bytes / KiB).toFixed(2) + ' KiB'
      } else if (bytes < GiB) {
        return (bytes / MiB).toFixed(2) + ' MiB'
      } else if (bytes < TiB) {
        return (bytes / GiB).toFixed(2) + ' GiB'
      } else {
        return (bytes / TiB).toFixed(2) + ' TiB'
      }
    }
  }
}

function isBase64 (str) {
  try {
    const decoded = new TextDecoder().decode(Uint8Array.from(atob(str), c => c.charCodeAt(0)))
    return btoa(decoded) === str
  } catch (err) {
    return false
  }
}

export const genericUtilPlugin = {
  install (app) {
    app.config.globalProperties.$isValidUuid = function (uuid) {
      const regexExp = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/gi
      return regexExp.test(uuid)
    }

    app.config.globalProperties.$toBase64AndURIEncoded = function (text) {
      if (isBase64(text)) {
        return text
      }
      return encodeURI(btoa(unescape(encodeURIComponent(text))))
    }
  }
}

export function createPathBasedOnVmType (vmtype, virtualmachineid) {
  let path = ''
  switch (vmtype) {
    case 'ConsoleProxy':
    case 'SecondaryStorageVm':
      path = '/systemvm/'
      break
    case 'DomainRouter':
      path = '/router/'
      break
    default:
      path = '/vm/'
  }

  return path + virtualmachineid
}
