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

import { shallowRef, defineAsyncComponent } from 'vue'
import store from '@/store'

export default {
  name: 'pod',
  title: 'label.pods',
  icon: 'appstore-outlined',
  docHelp: 'conceptsandterminology/concepts.html#about-pods',
  permission: ['listPods'],
  columns: ['name', 'allocationstate', 'gateway', 'netmask', 'zonename'],
  details: ['name', 'id', 'allocationstate', 'netmask', 'gateway', 'zonename'],
  related: [{
    name: 'cluster',
    title: 'label.clusters',
    param: 'podid'
  }, {
    name: 'host',
    title: 'label.hosts',
    param: 'podid'
  }],
  resourceType: 'Pod',
  filters: () => {
    const filters = ['enabled', 'disabled']
    return filters
  },
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'resources',
    component: shallowRef(defineAsyncComponent(() => import('@/views/infra/Resources.vue')))
  }, {
    name: 'events',
    resourceType: 'Pod',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
    show: () => { return 'listEvents' in store.getters.apis }
  }, {
    name: 'comments',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
  }],
  actions: [
    {
      api: 'createPod',
      icon: 'plus-outlined',
      label: 'label.add.pod',
      docHelp: 'installguide/configuration.html#adding-a-pod',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/infra/PodAdd.vue')))
    },
    {
      api: 'updatePod',
      icon: 'edit-outlined',
      label: 'label.edit',
      dataView: true,
      args: ['name', 'netmask', 'gateway']
    },
    {
      api: 'updatePod',
      icon: 'play-circle-outlined',
      label: 'label.action.enable.pod',
      message: 'message.action.enable.pod',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      show: (record) => { return record.allocationstate === 'Disabled' },
      args: ['allocationstate'],
      mapping: {
        allocationstate: {
          value: (record) => 'Enabled'
        }
      }
    },
    {
      api: 'updatePod',
      icon: 'pause-circle-outlined',
      label: 'label.action.disable.pod',
      message: 'message.action.disable.pod',
      docHelp: 'adminguide/hosts.html#disabling-and-enabling-zones-pods-and-clusters',
      dataView: true,
      show: (record) => { return record.allocationstate === 'Enabled' },
      args: ['allocationstate'],
      mapping: {
        allocationstate: {
          value: (record) => 'Disabled'
        }
      }
    },
    {
      api: 'startRollingMaintenance',
      icon: 'setting-outlined',
      label: 'label.start.rolling.maintenance',
      message: 'label.start.rolling.maintenance',
      dataView: true,
      args: ['timeout', 'payload', 'forced', 'podids'],
      mapping: {
        podids: {
          value: (record) => { return record.id }
        }
      }
    },
    {
      api: 'deletePod',
      icon: 'delete-outlined',
      label: 'label.action.delete.pod',
      message: 'message.action.delete.pod',
      dataView: true
    }
  ]
}
