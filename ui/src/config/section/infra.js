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

import zones from '@/config/section/infra/zones'
import phynetworks from '@/config/section/infra/phynetworks'
import nsp from '@/config/section/infra/nsp'
import pods from '@/config/section/infra/pods'
import clusters from '@/config/section/infra/clusters'
import hosts from '@/config/section/infra/hosts'
import primaryStorages from '@/config/section/infra/primaryStorages'
import secondaryStorages from '@/config/section/infra/secondaryStorages'
import systemVms from '@/config/section/infra/systemVms'
import routers from '@/config/section/infra/routers'

export default {
  name: 'infra',
  title: 'Infrastructure',
  icon: 'bank',
  permission: ['listInfrastructure'],
  children: [
    {
      name: 'infrasummary',
      title: 'Summary',
      icon: 'read',
      permission: ['listInfrastructure'],
      component: () => import('@/views/infra/InfraSummary.vue')
    },
    zones,
    phynetworks,
    nsp,
    pods,
    clusters,
    hosts,
    primaryStorages,
    secondaryStorages,
    systemVms,
    routers,
    {
      name: 'cpusocket',
      title: 'CPU Sockets',
      icon: 'inbox',
      permission: ['listHosts'],
      params: { type: 'routing' },
      columns: ['hypervisor', 'hosts', 'cpusockets']
    },
    {
      name: 'managementserver',
      title: 'Management Servers',
      icon: 'rocket',
      permission: ['listManagementServers'],
      columns: ['name', 'state', 'version']
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
          args: ['ids'],
          mapping: {
            ids: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteAlerts',
          icon: 'delete',
          label: 'Delete Alert',
          dataView: true,
          args: ['ids'],
          mapping: {
            ids: {
              value: (record) => { return record.id }
            }
          }
        }
      ]
    }
  ]
}
