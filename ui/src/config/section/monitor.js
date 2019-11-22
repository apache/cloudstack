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
  name: 'monitor',
  title: 'Monitor',
  icon: 'compass',
  permission: ['listEvents', 'listAlerts'],
  children: [
    {
      name: 'event',
      title: 'Events',
      icon: 'schedule',
      permission: ['listEvents'],
      columns: ['username', 'description', 'state', 'level', 'type', 'account', 'domain', 'created'],
      details: ['username', 'id', 'description', 'state', 'level', 'type', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'archiveEvents',
          icon: 'book',
          label: 'Archive Event',
          dataView: true,
          args: ['ids']
        },
        {
          api: 'deleteEvents',
          icon: 'delete',
          label: 'Delete Event',
          dataView: true,
          args: ['ids']
        }
      ]
    },
    {
      name: 'alert',
      title: 'Alerts',
      icon: 'flag',
      permission: ['listAlerts'],
      columns: ['name', 'description', 'type', 'sent'],
      details: ['name', 'id', 'type', 'sent', 'description'],
      actions: [
        {
          api: 'archiveAlerts',
          icon: 'book',
          label: 'Archive Alert',
          dataView: true,
          args: ['ids']
        },
        {
          api: 'deleteAlerts',
          icon: 'delete',
          label: 'Delete Alert',
          dataView: true,
          args: ['ids']
        }
      ]
    }
  ]
}
