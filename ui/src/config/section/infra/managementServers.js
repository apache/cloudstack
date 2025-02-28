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
import { shallowRef, defineAsyncComponent } from 'vue'

export default {
  name: 'managementserver',
  title: 'label.management.servers',
  icon: 'CloudServerOutlined',
  docHelp: 'conceptsandterminology/concepts.html#management-server-overview',
  permission: ['listManagementServersMetrics'],
  resourceType: 'ManagementServer',
  columns: () => {
    const fields = ['name', 'state', 'ipaddress', 'version', 'osdistribution', 'pendingjobscount', 'agentscount']
    const metricsFields = ['collectiontime', 'availableprocessors', 'cpuload', 'heapmemoryused']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    return fields
  },
  details: ['ipaddress', 'collectiontime', 'usageislocal', 'dbislocal', 'lastserverstart', 'lastserverstop', 'lastboottime', 'version', 'loginfo', 'systemtotalcpucycles', 'systemloadaverages', 'systemcycleusage', 'systemmemorytotal', 'systemmemoryfree', 'systemmemoryvirtualsize', 'availableprocessors', 'javadistribution', 'javaversion', 'osdistribution', 'kernelversion', 'pendingjobscount', 'agentscount', 'sessions', 'heapmemoryused', 'heapmemorytotal', 'threadsblockedcount', 'threadsdeamoncount', 'threadsnewcount', 'threadsrunnablecount', 'threadsterminatedcount', 'threadstotalcount', 'threadswaitingcount'],
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
    },
    {
      name: 'management.server.peers',
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/ManagementServerPeerTab.vue')))
    },
    {
      name: 'pending.jobs',
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/AsyncJobsTab.vue')))
    },
    {
      name: 'connected.agents',
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/ConnectedAgentsTab.vue')))
    },
    {
      name: 'comments',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
    }
  ],
  actions: [
    {
      api: 'prepareForMaintenance',
      icon: 'plus-square-outlined',
      label: 'label.prepare.for.maintenance',
      message: 'message.prepare.for.maintenance',
      dataView: true,
      popup: true,
      confirmationText: 'MAINTENANCE',
      show: (record, store) => { return record.state === 'Up' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/Confirmation.vue')))
    },
    {
      api: 'cancelMaintenance',
      icon: 'minus-square-outlined',
      label: 'label.cancel.maintenance',
      message: 'message.cancel.maintenance',
      dataView: true,
      popup: true,
      show: (record, store) => { return ['PreparingForMaintenance', 'Maintenance'].includes(record.state) },
      mapping: {
        managementserverid: {
          value: (record, params) => { return record.id }
        }
      }
    },
    {
      api: 'prepareForShutdown',
      icon: 'exclamation-circle-outlined',
      label: 'label.prepare.for.shutdown',
      message: 'message.prepare.for.shutdown',
      dataView: true,
      popup: true,
      confirmationText: 'SHUTDOWN',
      show: (record, store) => { return record.state === 'Up' },
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/Confirmation.vue')))
    },
    {
      api: 'triggerShutdown',
      icon: 'poweroff-outlined',
      label: 'label.trigger.shutdown',
      message: 'message.trigger.shutdown',
      dataView: true,
      popup: true,
      confirmationText: 'SHUTDOWN',
      show: (record, store) => { return ['Up', 'Maintenance', 'PreparingForShutDown', 'ReadyToShutDown'].includes(record.state) },
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/Confirmation.vue')))
    },
    {
      api: 'cancelShutdown',
      icon: 'close-circle-outlined',
      label: 'label.cancel.shutdown',
      message: 'message.cancel.shutdown',
      docHelp: 'installguide/configuration.html#adding-a-zone',
      dataView: true,
      popup: true,
      show: (record, store) => { return ['PreparingForShutDown', 'ReadyToShutDown', 'ShuttingDown'].includes(record.state) },
      mapping: {
        managementserverid: {
          value: (record, params) => { return record.id }
        }
      }
    }
  ]
}
