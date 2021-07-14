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
import store from '@/store'

export default {
  name: 'comment',
  title: 'label.comments',
  icon: 'message',
  docHelp: 'adminguide/events.html',
  permission: ['listAnnotations'],
  columns: () => {
    const cols = ['created', 'annotation', 'entityid', 'entitytype', 'username']
    if (['Admin'].includes(store.getters.userInfo.roletype)) {
      cols.push('adminsonly')
    }
    return cols
  },
  searchFilters: ['annotation', 'username', 'keyword'],
  params: () => { return { annotationfilter: 'self' } },
  filters: () => {
    const filters = ['self']
    if (['Admin'].includes(store.getters.userInfo.roletype)) {
      filters.push('all')
    }
    return filters
  },
  actions: [
    {
      api: 'removeAnnotation',
      icon: 'delete',
      label: 'label.remove.annotation',
      message: 'message.remove.annotation',
      dataView: false,
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
      // show: (record, store) => {
      //   return ['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) && !record.isdefault &&
      //     !(record.domain === 'ROOT' && record.name === 'admin' && record.accounttype === 1)
      // }
    }
  ]
}
