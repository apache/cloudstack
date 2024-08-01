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

import { axios, sourceToken } from '@/utils/request'
import { message, notification } from 'ant-design-vue'

export function api (command, args = {}, method = 'GET', data = {}) {
  let params = {}
  args.command = command
  args.response = 'json'

  if (data) {
    params = new URLSearchParams()
    Object.entries(data).forEach(([key, value]) => {
      params.append(key, value)
    })
  }

  return axios({
    params: {
      ...args
    },
    url: '/',
    method,
    data: params || {}
  })
}

export function login (arg) {
  if (!sourceToken.checkExistSource()) {
    sourceToken.init()
  }

  // Logout before login is called to purge any duplicate sessionkey cookies
  api('logout')

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
      'content-type': 'application/x-www-form-urlencoded'
    }
  })
}

export function logout () {
  sourceToken.cancel()
  message.destroy()
  notification.destroy()
  return api('logout')
}

export function oauthlogin (arg) {
  if (!sourceToken.checkExistSource()) {
    sourceToken.init()
  }

  // Logout before login is called to purge any duplicate sessionkey cookies
  api('logout')

  const params = new URLSearchParams()
  params.append('command', 'oauthlogin')
  params.append('email', arg.email)
  params.append('secretcode', arg.secretcode)
  params.append('provider', arg.provider)
  params.append('domain', arg.domain)
  params.append('response', 'json')
  return axios({
    url: '/',
    method: 'post',
    data: params,
    headers: {
      'content-type': 'application/x-www-form-urlencoded'
    }
  })
}
