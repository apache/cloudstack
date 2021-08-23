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

import { axios } from '@/utils/request'
import store from '@/store'

export async function api (command, args = {}, method = 'GET', data = {}) {
  let params = {}
  args.command = command
  args.response = 'json'

  if (data) {
    params = new URLSearchParams()
    Object.entries(data).forEach(([key, value]) => {
      params.append(key, value)
    })
  }

  const exemptedAPIs = ['listLdapConfigurations', 'listCapabilities', 'listIdps', 'listApis', 'listInfrastructure', 'listAndSwitchSamlAccount']

  if ('page' in args || exemptedAPIs.includes(command) || !command.startsWith('list')) {
    return axios({
      params: {
        ...args
      },
      url: '/',
      method,
      data: params || {}
    })
  }

  const pagesize = store.getters.defaultPageSize
  let page = 1
  let items = []
  let done = false
  let response = null

  while (!done) {
    args.page = page
    args.pagesize = pagesize
    await axios({
      params: {
        ...args
      },
      url: '/',
      method,
      data: params || {}
    }).then(json => {
      var responseName
      var objectName
      for (const key in json) {
        if (key.includes('response')) {
          responseName = key
          break
        }
      }
      for (const key in json[responseName]) {
        if (key === 'count') {
          continue
        }
        objectName = key
        break
      }
      if (json[responseName][objectName]) {
        items = items.concat(json[responseName][objectName])
        console.log(command, page, responseName, objectName, items.length, json[responseName].count, 'WIP')
      }
      if (!json[responseName].count || json[responseName].count === items.length || !json[responseName][objectName]) {
        console.log(command, page, responseName, objectName, items.length, json[responseName].count, 'DONE')
        done = true
        json[responseName][objectName] = items
        console.log(json)
        response = new Promise((resolve) => {
          resolve(json)
        })
        return
      }
      page++
    }).catch(error => {
      response = new Promise((resolve, reject) => {
        reject(error)
      })
    })
  }
  return response
}

export function login (arg) {
  const params = new URLSearchParams()
  params.append('command', 'login')
  params.append('username', arg.username || arg.email)
  params.append('password', arg.password)
  params.append('domain', arg.domain)
  params.append('response', 'json')
  return axios({
    url: '/',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

export function logout () {
  return api('logout')
}
