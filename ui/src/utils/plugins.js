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

export const pollJobPlugin = {
  install (Vue) {
    Vue.prototype.$pollJob = function (options) {
      /**
       * @param {String} jobId
       * @param {String} [name='']
       * @param {String} [successMessage=Success]
       * @param {Function} [successMethod=() => {}]
       * @param {String} [errorMessage=Error]
       * @param {Function} [errorMethod=() => {}]
       * @param {Object} [showLoading=true]
       * @param {String} [loadingMessage=Loading...]
       * @param {String} [catchMessage=Error caught]
       * @param {Function} [catchMethod=() => {}]
       * @param {Object} [action=null]
       */
      const {
        jobId,
        name = '',
        successMessage = i18n.t('label.success'),
        successMethod = () => {},
        errorMessage = i18n.t('label.error'),
        errorMethod = () => {},
        loadingMessage = `${i18n.t('label.loading')}...`,
        showLoading = true,
        catchMessage = i18n.t('label.error.caught'),
        catchMethod = () => {},
        action = null
      } = options

      api('queryAsyncJobResult', { jobId }).then(json => {
        const result = json.queryasyncjobresultresponse
        if (result.jobstatus === 1) {
          var content = successMessage
          if (successMessage === 'Success' && action && action.label) {
            content = i18n.t(action.label)
          }
          if (name) {
            content = content + ' - ' + name
          }
          message.success({
            content: content,
            key: jobId,
            duration: 2
          })
          eventBus.$emit('async-job-complete', action)
          successMethod(result)
        } else if (result.jobstatus === 2) {
          message.error({
            content: errorMessage,
            key: jobId,
            duration: 1
          })
          var title = errorMessage
          if (action && action.label) {
            title = i18n.t(action.label)
          }
          var desc = result.jobresult.errortext
          if (name) {
            desc = `(${name}) ${desc}`
          }
          notification.error({
            message: title,
            description: desc,
            key: jobId,
            duration: 0
          })
          eventBus.$emit('async-job-complete', action)
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
        notification.error({
          message: i18n.t('label.error'),
          description: catchMessage,
          duration: 0
        })
        catchMethod && catchMethod()
      })
    }
  }

}

export const notifierPlugin = {
  install (Vue) {
    Vue.prototype.$notifyError = function (error) {
      console.log(error)
      var msg = i18n.t('message.request.failed')
      var desc = ''
      if (error && error.response) {
        if (error.response.status) {
          msg = `${i18n.t('message.request.failed')} (${error.response.status})`
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
      notification.error({
        message: msg,
        description: desc,
        duration: 0
      })
    }
  }
}

export const toLocaleDatePlugin = {
  install (Vue) {
    Vue.prototype.$toLocaleDate = function (date) {
      var timezoneOffset = this.$store.getters.timezoneoffset
      if (this.$store.getters.usebrowsertimezone) {
        // Since GMT+530 is returned as -330 (mins to GMT)
        timezoneOffset = new Date().getTimezoneOffset() / -60
      }
      var milliseconds = Date.parse(date)
      // e.g. "Tue, 08 Jun 2010 19:13:49 GMT", "Tue, 25 May 2010 12:07:01 UTC"
      var dateWithOffset = new Date(milliseconds + (timezoneOffset * 60 * 60 * 1000)).toUTCString()
      // e.g. "08 Jun 2010 19:13:49 GMT", "25 May 2010 12:07:01 UTC"
      dateWithOffset = dateWithOffset.substring(dateWithOffset.indexOf(', ') + 2)
      // e.g. "08 Jun 2010 19:13:49", "25 May 2010 12:10:16"
      dateWithOffset = dateWithOffset.substring(0, dateWithOffset.length - 4)
      return dateWithOffset
    }
  }
}

export const configUtilPlugin = {
  install (Vue) {
    Vue.prototype.$applyDocHelpMappings = function (docHelp) {
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
