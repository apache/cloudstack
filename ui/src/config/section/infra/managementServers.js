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
  permission: ['listManagementServersMetrics'],
  columns: () => {
    const fields = ['name', 'state', 'version']
    const metricsFields = ['collectiontime', 'availableprocessors', 'cpuload', 'heapmemoryused', 'agentcount']
    if (store.getters.metrics) {
      fields.push(...metricsFields)
    }
    return fields
  },
  details: ['collectiontime', 'usageislocal', 'dbislocal', 'lastserverstart', 'lastserverstop', 'lastboottime', 'version', 'loginfo', 'systemtotalcpucycles', 'systemloadaverages', 'systemcycleusage', 'systemmemorytotal', 'systemmemoryfree', 'systemmemoryvirtualsize', 'availableprocessors', 'javadistribution', 'javaversion', 'osdistribution', 'kernelversion', 'agentcount', 'sessions', 'heapmemoryused', 'heapmemorytotal', 'threadsblockedcount', 'threadsdeamoncount', 'threadsnewcount', 'threadsrunnablecount', 'threadsterminatedcount', 'threadstotalcount', 'threadswaitingcount'],
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
    }
  ]
}
