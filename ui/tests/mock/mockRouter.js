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

import VueRouter from 'vue-router'

const mockRouter = {
  routes: [
    {
      path: '/',
      name: 'home',
      meta: { icon: 'home' },
      children: []
    }
  ],
  mock: (routes = []) => {
    mockRouter.routes[0].children = [
      {
        path: '/exception',
        name: 'exception',
        children: [
          {
            path: '/exception/403',
            name: 403,
            hidden: true,
            meta: { icon: 'icon-error-test' }
          },
          {
            path: '/exception/404',
            name: 404,
            hidden: true,
            meta: { icon: 'icon-error-test' }
          },
          {
            path: '/exception/500',
            name: 500,
            hidden: true,
            meta: { icon: 'icon-error-test' }
          }
        ]
      }
    ]
    if (routes && routes.length > 0) {
      mockRouter.routes[0].children = [...mockRouter.routes[0].children, ...routes]
    }

    return new VueRouter({ routes: mockRouter.routes, mode: 'history' })
  }
}

export default mockRouter
