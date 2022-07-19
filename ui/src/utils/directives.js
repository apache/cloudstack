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

const ENTER_KEY_CODE = 13
let lastFocusElm = null

export default {
  install: (app) => {
    app.directive('ctrlEnter', {
      mounted: (el, binding, vnode) => {
        el.addEventListener('keydown', (e) => {
          if (e.ctrlKey && e.keyCode === ENTER_KEY_CODE) {
            e.preventDefault()
            lastFocusElm = e.target
            const vm = binding.instance
            vm.$refs.submit.$el.focus()
          }
        })

        el.addEventListener('keyup', (e) => {
          if (!e.ctrlKey || e.keyCode !== ENTER_KEY_CODE) {
            e.preventDefault()
            return
          }

          e.preventDefault()
          if (typeof binding.value === 'function') {
            if (lastFocusElm) lastFocusElm.focus()
            const argument = binding.arg || e
            binding.value(argument)
          }
        })
      }
    })

    app.directive('focus', {
      mounted: (el, binding, vnode) => {
        if (binding.value) {
          el.focus()
        }
      }
    })
  }
}
