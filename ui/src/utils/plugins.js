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

import { api } from '@/api'
import { message, notification } from 'ant-design-vue'

export const pollJobPlugin = {

  install (Vue) {
    Vue.prototype.$pollJob = function (options) {
      /**
       * @param {String} jobId
       * @param {String} [successMessage=Success]
       * @param {Function} [successMethod=() => {}]
       * @param {String} [errorMessage=Error]
       * @param {Function} [errorMethod=() => {}]
       * @param {String} [loadingMessage=Loading...]
       * @param {String} [catchMessage=Error caught]
       * @param {Function} [catchMethod=() => {}]
       * @param {Number} [loadingDuration=3]
       * @param {Object} [action=null]
       */
      const {
        jobId,
        successMessage = 'Success',
        successMethod = () => {},
        errorMessage = 'Error',
        errorMethod = () => {},
        loadingMessage = 'Loading...',
        catchMessage = 'Error caught',
        catchMethod = () => {},
        loadingDuration = 3,
        action = null
      } = options

      api('queryAsyncJobResult', { jobId }).then(json => {
        const result = json.queryasyncjobresultresponse

        if (result.jobstatus === 1) {
          message.success(successMessage)
          successMethod(result)
        } else if (result.jobstatus === 2) {
          notification.error({
            message: errorMessage,
            description: result.jobresult.errortext
          })
          errorMethod(result)
        } else if (result.jobstatus === 0) {
          message
            .loading(loadingMessage, loadingDuration)
            .then(() => this.$pollJob(options, action))
        }
      }).catch(e => {
        console.error(`${catchMessage} - ${e}`)
        notification.error({
          message: 'Error',
          description: catchMessage
        })
        catchMethod && catchMethod()
      })
    }
  }

}
