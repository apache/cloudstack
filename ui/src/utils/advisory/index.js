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
import { getAPI } from '@/api'

/**
 * Generic helper to check if an API has no items (useful for advisory conditions)
 * @param {Object} store - Vuex store instance
 * @param {string} apiName - Name of the API to call (e.g., 'listNetworks')
 * @param {Object} params - Optional parameters to merge with defaults
 * @param {Function} filterFunc - Optional function to filter items. If provided, returns true if no items match the filter.
 * @param {string} itemsKey - Optional key for items array in response. If not provided, will be deduced from apiName
 * @returns {Promise<boolean>} - Returns true if no items exist (advisory should be shown), false otherwise
 */
export async function hasNoItems (store, apiName, params = {}, filterFunc = null, itemsKey = null) {
  if (!(apiName in store.getters.apis)) {
    return false
  }

  // If itemsKey not provided, deduce it from apiName
  if (!itemsKey) {
    // Remove 'list' prefix: listNetworks -> Networks
    let key = apiName.replace(/^list/i, '')
    // Convert to lowercase
    key = key.toLowerCase()
    // Handle plural forms: remove trailing 's' or convert 'ies' to 'y'
    if (key.endsWith('ies')) {
      key = key.slice(0, -3) + 'y'
    } else if (key.endsWith('s')) {
      key = key.slice(0, -1)
    }
    itemsKey = key
  }

  const allParams = {
    listall: true,
    ...params
  }

  if (filterFunc == null) {
    allParams.page = 1
    allParams.pageSize = 1
  }

  console.debug(`Checking if API ${apiName} has no items with params`, allParams)

  try {
    const json = await getAPI(apiName, allParams)
    // Auto-derive response key: listNetworks -> listnetworksresponse
    const responseKey = `${apiName.toLowerCase()}response`
    const items = json?.[responseKey]?.[itemsKey] || []
    if (filterFunc) {
      const a = !items.some(filterFunc)
      console.debug(`API ${apiName} has ${items.length} items, after filter has items: ${items.filter(filterFunc)[0]}, returning ${a}`)
      const it = items.filter(filterFunc)
      console.debug(`Filtered items:`, it)
      return a
    }
    return items.length === 0
  } catch (error) {
    return false
  }
}
