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
  name: 'backuprepository',
  title: 'label.backup.repository',
  icon: 'inbox-outlined',
  docHelp: 'adminguide/nas_plugin.html',
  permission: ['listBackupRepositories'],
  searchFilters: ['zoneid'],
  columns: ['name', 'provider', 'type', 'address', 'zonename'],
  details: ['name', 'type', 'address', 'provider', 'zonename', 'crosszoneinstancecreation'],
  actions: [
    {
      api: 'addBackupRepository',
      icon: 'plus-outlined',
      label: 'label.backup.repository.add',
      listView: true,
      args: [
        'name', 'provider', 'address', 'type', 'mountopts', 'zoneid', 'crosszoneinstancecreation'
      ],
      mapping: {
        type: {
          options: ['nfs', 'cifs', 'ceph']
        },
        provider: {
          value: (record) => { return 'nas' }
        }
      }
    },
    {
      api: 'updateBackupRepository',
      icon: 'edit-outlined',
      label: 'label.backup.repository.edit',
      message: 'message.action.edit.backup.repository',
      args: ['name', 'address', 'mountopts', 'crosszoneinstancecreation'],
      dataView: true,
      popup: true
    },
    {
      api: 'deleteBackupRepository',
      icon: 'delete-outlined',
      label: 'label.backup.repository.remove',
      message: 'message.action.delete.backup.repository',
      dataView: true,
      popup: true
    }
  ]
}
