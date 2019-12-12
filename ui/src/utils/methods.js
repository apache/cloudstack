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

/**
 * Reusable queryAsyncJobResult method
 * @param {String} jobId
 * @param {String} successMessage
 * @param {Function} successMethod
 * @param {String} errorMessage
 * @param {Function} errorMethod
 * @param {String} loadingMessage
 * @param {String} catchMessage
 * @param {Function} catchMethod
 * @param {Number} loadingDuration
 */
export const pollActionCompletion = ({
  jobId, successMessage, successMethod, errorMessage, errorMethod, loadingMessage, catchMessage, catchMethod, loadingDuration = 3
}) => {
  function runApi () {
    api('queryAsyncJobResult', { jobId }).then(json => {
      const result = json.queryasyncjobresultresponse

      if (result.jobstatus === 1) {
        message.success(successMessage || 'Success')
        successMethod && successMethod()
      } else if (result.jobstatus === 2) {
        notification.error({
          message: errorMessage || 'Error',
          description: result.jobresult.errortext || 'Error'
        })
        errorMethod && errorMethod()
      } else if (result.jobstatus === 0) {
        message
          .loading(loadingMessage, loadingDuration)
          .then(() => runApi())
      }
    }).catch(e => {
      console.error(`${catchMessage} - ${e}`)
      catchMethod && catchMethod()
    })
  }
  runApi()
}
