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
  name: 'resourcealerts',
  title: 'label.resource.alert.rules',
  icon: 'BellOutlined',
  permission: ['listResourceAlertRules'],
  columns: ['name', 'resourcetype', 'metric', 'condition', 'threshold', 'severity', 'email'],
  details: ['name', 'id', 'resourcetype', 'resourceid', 'metric', 'condition', 'threshold', 'severity', 'message', 'email', 'resetinterval', 'account', 'domain', 'created'],
  searchFilters: ['name', 'resourcetype'],
  tabs: [{
    name: 'details',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
  }, {
    name: 'firedalerts',
    component: shallowRef(defineAsyncComponent(() => import('@/components/view/ResourceAlertFiredTab.vue'))),
    show: () => { return 'listResourceAlerts' in store.getters.apis }
  }],
  actions: [
    {
      api: 'createResourceAlertRule',
      icon: 'plus-outlined',
      label: 'label.create.resource.alert.rule',
      listView: true,
      popup: true,
      component: shallowRef(defineAsyncComponent(() => import('@/views/resourcealert/CreateResourceAlertRule.vue')))
    },
    {
      api: 'updateResourceAlertRule',
      icon: 'edit-outlined',
      label: 'label.edit',
      dataView: true,
      args: ['name', 'condition', 'threshold', 'severity', 'message', 'email', 'resetinterval'],
      mapping: {
        condition: {
          options: ['GT', 'GTE', 'LT', 'LTE', 'EQ']
        },
        severity: {
          options: ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
        }
      }
    },
    {
      api: 'deleteResourceAlertRule',
      icon: 'delete-outlined',
      label: 'label.delete',
      message: 'message.confirm.delete.resource.alert.rule',
      dataView: true,
      groupAction: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x.id } }) }
    }
  ]
}
