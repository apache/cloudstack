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
  name: 'tools',
  title: 'label.tools',
  icon: 'tool-outlined',
  children: [
    {
      name: 'comment',
      title: 'label.comments',
      icon: 'message-outlined',
      docHelp: 'adminguide/events.html',
      permission: ['listAnnotations'],
      columns: () => {
        const cols = ['entityid', 'entitytype', 'annotation', 'created', 'username']
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          cols.push('adminsonly')
        }
        return cols
      },
      searchFilters: ['entitytype', 'keyword'],
      params: () => { return { annotationfilter: 'self' } },
      filters: () => {
        const filters = ['self', 'all']
        return filters
      },
      actions: [
        {
          api: 'removeAnnotation',
          icon: 'delete-outlined',
          label: 'label.remove.annotation',
          message: 'message.remove.annotation',
          dataView: false,
          groupAction: true,
          popup: true,
          groupShow: (selectedItems, storegetters) => {
            if (['Admin'].includes(store.getters.userInfo.roletype)) {
              return true
            }
            // Display only if the selected items are comments created by the user
            return selectedItems.filter(x => { return x.username !== store.getters.userInfo.username }).length === 0
          },
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'usage',
      title: 'label.usage',
      icon: 'ContainerOutlined',
      permission: ['listUsageRecords'],
      meta: { title: 'label.usage', icon: 'ContainerOutlined' },
      component: () => import('@/views/infra/UsageRecords.vue')
    },
    {
      name: 'manageinstances',
      title: 'label.action.import.export.instances',
      icon: 'interaction-outlined',
      docHelp: 'adminguide/virtual_machines.html#importing-and-unmanaging-virtual-machine',
      resourceType: 'UserVm',
      permission: ['listInfrastructure', 'listUnmanagedInstances'],
      component: () => import('@/views/tools/ManageInstances.vue')
    },
    {
      name: 'managevolumes',
      title: 'label.action.import.unmanage.volumes',
      icon: 'interaction-outlined',
      docHelp: 'adminguide/virtual_machines.html#importing-and-unmanaging-volume',
      resourceType: 'UserVm',
      permission: ['listInfrastructure', 'listVolumesForImport'],
      component: () => import('@/views/tools/ManageVolumes.vue')
    },
    {
      name: 'webhook',
      title: 'label.webhooks',
      icon: 'node-index-outlined',
      docHelp: 'adminguide/webhooks.html',
      permission: ['listWebhooks'],
      columns: () => {
        const cols = ['name', 'payloadurl', 'state', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          cols.splice(3, 0, 'account', 'domain', 'scope')
        }
        if (store.getters.listAllProjects) {
          cols.push('project')
        }
        return cols
      },
      details: ['name', 'id', 'description', 'scope', 'payloadurl', 'sslverification', 'secretkey', 'state', 'account', 'domainid'],
      searchFilters: () => {
        var filters = ['state']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          filters.push('scope', 'domainid', 'account')
        }
        return filters
      },
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'recent.deliveries',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/WebhookDeliveriesTab.vue')))
        }
      ],
      actions: [
        {
          api: 'createWebhook',
          icon: 'plus-outlined',
          label: 'label.create.webhook',
          docHelp: 'adminguide/events.html#creating-webhooks',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/tools/CreateWebhook.vue')))
        },
        {
          api: 'updateWebhook',
          icon: 'edit-outlined',
          label: 'label.update.webhook',
          dataView: true,
          popup: true,
          args: ['name', 'description', 'payloadurl', 'sslverification', 'secretkey', 'state'],
          mapping: {
            state: {
              options: ['Enabled', 'Disabled']
            }
          }
        },
        {
          api: 'updateWebhook',
          icon: 'play-circle-outlined',
          label: 'label.enable.webhook',
          message: 'message.confirm.enable.webhook',
          dataView: true,
          groupAction: true,
          popup: true,
          defaultArgs: { state: 'Enabled' },
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
          show: (record) => { return ['Disabled'].includes(record.state) }
        },
        {
          api: 'updateWebhook',
          icon: 'pause-circle-outlined',
          label: 'label.disable.webhook',
          message: 'message.confirm.disable.webhook',
          dataView: true,
          groupAction: true,
          popup: true,
          defaultArgs: { state: 'Disabled' },
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
          show: (record) => { return ['Enabled'].includes(record.state) }
        },
        {
          api: 'executeWebhookDelivery',
          icon: 'right-square-outlined',
          label: 'label.test.webhook.delivery',
          message: 'message.test.webhook.delivery',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/tools/TestWebhookDelivery.vue')))
        },
        {
          api: 'deleteWebhook',
          icon: 'delete-outlined',
          label: 'label.delete.webhook',
          message: 'message.delete.webhook',
          dataView: true,
          groupAction: true,
          popup: true,
          groupShow: (selectedItems, storegetters) => {
            if (['Admin'].includes(storegetters.userInfo.roletype)) {
              return true
            }
          },
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'webhookdeliveries',
      title: 'label.webhook.deliveries',
      icon: 'gateway-outlined',
      hidden: true,
      permission: ['listWebhookDeliveries'],
      columns: () => {
        const cols = ['payload', 'eventtype', 'webhookname', 'success', 'response', 'duration']
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          cols.splice(3, 0, 'managementservername')
        }
        return cols
      },
      details: () => {
        const fields = ['id', 'eventid', 'eventtype', 'headers', 'payload', 'success', 'response', 'startdate', 'enddate']
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          fields.splice(1, 0, 'managementserverid', 'managementservername')
        }
        return fields
      },
      actions: [
        {
          api: 'executeWebhookDelivery',
          icon: 'retweet-outlined',
          label: 'label.redeliver',
          message: 'message.redeliver.webhook.delivery',
          dataView: true,
          popup: true
        },
        {
          api: 'deleteWebhookDelivery',
          icon: 'delete-outlined',
          label: 'label.delete.webhook.delivery',
          message: 'message.delete.webhook.delivery',
          dataView: true,
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    }
  ]
}
