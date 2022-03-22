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

import registerRequireContextHook from 'babel-plugin-require-context-hook/register'
import { config } from '@vue/test-utils'

import componentsUse from '@/core/lazy_lib/components_use'
import iconsUse from '@/core/lazy_lib/icons_use'
import extensions from '@/core/ext'
import directives from '@/utils/directives'

registerRequireContextHook()

config.global.plugins.push(componentsUse)
config.global.plugins.push(iconsUse)
config.global.plugins.push(extensions)
config.global.plugins.push(directives)

window.matchMedia = window.matchMedia || function () {
  return {
    matches: false,
    addListener: function () {},
    removeListener: function () {}
  }
}
