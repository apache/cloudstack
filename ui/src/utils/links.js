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

async function isValidObject (apiName, id, params) {
  try {
    const allParams = { ...params, listAll: true, id }
    const json = await api(apiName, allParams)
    const responseName = Object.keys(json).find(key => key.endsWith('response')) || apiName.toLowerCase() + 'response'
    const response = json?.[responseName]
    if (!response) {
      return false
    }
    const objectName = Object.keys(response).find(key => key !== 'count')
    if (!objectName || !Array.isArray(response[objectName])) {
      return false
    }
    return response[objectName].some(item => item.id === id)
  } catch (e) {
    return false
  }
}

export function validateLinks (router, isStatic, resource) {
  const validLinks = {
    volume: false
  }

  if (isStatic) {
    return validLinks
  }

  if (resource.volumeid && router.resolve('/volume/' + resource.volumeid).matched[0].redirect !== '/exception/404') {
    if (resource.volumestate) {
      validLinks.volume = resource.volumestate !== 'Expunged'
    } else {
      validLinks.volume = true
    }
  }

  return validLinks
}

export async function validateLinksAsync (router, isStatic, resource) {
  const validLinks = {
    volume: false,
    template: false,
    iso: false
  }
  const pendingChecks = []

  if (isStatic) {
    return validLinks
  }

  if (resource.volumeid && router.resolve('/volume/' + resource.volumeid).matched[0].redirect !== '/exception/404') {
    if (resource.volumestate) {
      validLinks.volume = resource.volumestate !== 'Expunged'
    } else {
      validLinks.volume = true
    }
  }

  if (resource.templateid) {
    const templatePath = (resource.templateformat === 'ISO' ? '/iso/' : '/template/') + resource.templateid
    if (router.resolve(templatePath).matched[0].redirect !== '/exception/404') {
      pendingChecks.push(
        isValidObject('listTemplates', resource.templateid, { templatefilter: 'executable' }).then(result => {
          validLinks.template = result
        })
      )
    }
  }

  if (resource.isoid) {
    const isoPath = '/iso/' + resource.isoid
    if (router.resolve(isoPath).matched[0].redirect !== '/exception/404') {
      pendingChecks.push(
        isValidObject('listIsos', resource.isoid, { isofilter: 'executable' }).then(result => {
          validLinks.iso = result
        })
      )
    }
  }

  if (pendingChecks.length) {
    await Promise.all(pendingChecks).catch(error => {
      console.error('Error validating links:', error)
    })
  }

  return validLinks
}
