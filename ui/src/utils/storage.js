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

/**
 * Set storage
 *
 * @param name
 * @param content
 * @param maxAge
 */
export const setStore = (name, content, maxAge = null) => {
  if (!global.window || !name) {
    return
  }

  if (typeof content !== 'string') {
    content = JSON.stringify(content)
  }

  const storage = global.window.localStorage

  storage.setItem(name, content)
  if (maxAge && !isNaN(parseInt(maxAge))) {
    const timeout = parseInt(new Date().getTime() / 1000)
    storage.setItem(`${name}_expire`, timeout + maxAge)
  }
}

/**
 * Get storage
 *
 * @param name
 * @returns {*}
 */
export const getStore = name => {
  if (!global.window || !name) {
    return
  }

  const content = window.localStorage.getItem(name)
  const _expire = window.localStorage.getItem(`${name}_expire`)

  if (_expire) {
    const now = parseInt(new Date().getTime() / 1000)
    if (now > _expire) {
      return
    }
  }

  try {
    return JSON.parse(content)
  } catch (e) {
    return content
  }
}

/**
 * Clear storage
 *
 * @param name
 */
export const clearStore = name => {
  if (!global.window || !name) {
    return
  }

  window.localStorage.removeItem(name)
  window.localStorage.removeItem(`${name}_expire`)
}

/**
 * Clear all storage
 */
export const clearAll = () => {
  if (!global.window || !name) {
    return
  }

  window.localStorage.clear()
}
