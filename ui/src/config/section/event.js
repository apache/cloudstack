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

export default {
  name: 'event',
  title: 'label.events',
  icon: 'ScheduleOutlined',
  docHelp: 'adminguide/events.html',
  permission: ['listEvents'],
  columns: ['level', 'type', 'state', 'description', 'resource', 'username', 'account', 'domain', 'created'],
  details: ['username', 'id', 'description', 'resourcetype', 'resourceid', 'state', 'level', 'type', 'account', 'domain', 'created'],
  searchFilters: ['level', 'domainid', 'account', 'keyword', 'resourcetype'],
  related: [{
    name: 'event',
    title: 'label.event.timeline',
    param: 'startid'
  }],
  filters: () => {
    return ['active', 'archived']
  },
  actions: [
    {
      api: 'archiveEvents',
      icon: 'book-outlined',
      label: 'label.archive.events',
      message: 'message.confirm.archive.selected.events',
      docHelp: 'adminguide/events.html#deleting-and-archiving-events-and-alerts',
      dataView: true,
      successMessage: 'label.event.archived',
      groupAction: true,
      groupMap: (selection) => { return [{ ids: selection.join(',') }] },
      args: ['ids'],
      mapping: {
        ids: {
          value: (record) => { return record.id }
        }
      },
      show: (record) => {
        return !(record.archived)
      },
      groupShow: (selectedItems) => {
        return selectedItems.filter(x => { return !(x.archived) }).length > 0
      }
    },
    {
      api: 'deleteEvents',
      icon: 'delete-outlined',
      label: 'label.delete.events',
      message: 'message.confirm.remove.selected.events',
      docHelp: 'adminguide/events.html#deleting-and-archiving-events-and-alerts',
      dataView: true,
      successMessage: 'label.event.deleted',
      groupAction: true,
      groupMap: (selection) => { return [{ ids: selection.join(',') }] },
      args: ['ids'],
      mapping: {
        ids: {
          value: (record) => { return record.id }
        }
      }
    }
  ]
}
