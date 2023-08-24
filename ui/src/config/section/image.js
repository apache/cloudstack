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
import kubernetes from '@/assets/icons/kubernetes.svg?inline'
import store from '@/store'

export default {
  name: 'image',
  title: 'label.images',
  icon: 'picture-outlined',
  docHelp: 'adminguide/templates.html',
  children: [
    {
      name: 'template',
      title: 'label.templates',
      icon: 'save-outlined',
      docHelp: 'adminguide/templates.html',
      permission: ['listTemplates'],
      params: { templatefilter: 'self', showunique: 'true' },
      resourceType: 'Template',
      filters: ['self', 'shared', 'featured', 'community'],
      columns: () => {
        var fields = ['name',
          {
            state: (record) => {
              if (record.isready) {
                return 'Ready'
              }
              return 'Not Ready'
            }
          }, 'ostypename', 'hypervisor']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('size')
          fields.push('account')
        }
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          fields.push('order')
        }
        return fields
      },
      details: () => {
        var fields = ['name', 'id', 'displaytext', 'checksum', 'hypervisor', 'format', 'ostypename', 'size', 'physicalsize', 'isready', 'passwordenabled',
          'crossZones', 'directdownload', 'deployasis', 'ispublic', 'isfeatured', 'isextractable', 'isdynamicallyscalable', 'crosszones', 'type',
          'account', 'domain', 'created', 'userdatadetails', 'userdatapolicy']
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          fields.push('templatetype', 'url')
        }
        return fields
      },
      searchFilters: ['name', 'zoneid', 'tags'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'templateid'
      }],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'zones',
        component: shallowRef(defineAsyncComponent(() => import('@/views/image/TemplateZones.vue')))
      }, {
        name: 'settings',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailSettings')))
      },
      {
        name: 'events',
        resourceType: 'Template',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus-outlined',
          label: 'label.action.register.template',
          docHelp: 'adminguide/templates.html#uploading-templates-from-a-remote-http-server',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/RegisterOrUploadTemplate.vue')))
        },
        {
          api: 'registerTemplate',
          icon: 'cloud-upload-outlined',
          label: 'label.upload.template.from.local',
          docHelp: 'adminguide/templates.html#uploading-templates-and-isos-from-a-local-computer',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/RegisterOrUploadTemplate.vue')))
        },
        {
          api: 'updateTemplate',
          icon: 'edit-outlined',
          label: 'label.action.edit.template',
          dataView: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              record.isready
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/UpdateTemplate.vue')))
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'share-alt-outlined',
          label: 'label.action.template.share',
          dataView: true,
          args: (record, store) => {
            const fields = ['isfeatured', 'isextractable']
            if (['Admin'].includes(store.userInfo.roletype) || store.features.userpublictemplateenabled) {
              fields.unshift('ispublic')
            }
            return fields
          },
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              record.templatetype !== 'SYSTEM' &&
              record.isready
          }
        },
        {
          api: 'extractTemplate',
          icon: 'cloud-download-outlined',
          label: 'label.action.download.template',
          message: 'message.action.download.template',
          docHelp: 'adminguide/templates.html#exporting-templates',
          dataView: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              ((record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              record.templatetype !== 'SYSTEM' &&
              record.isextractable) &&
              record.isready
          },
          args: ['zoneid', 'mode'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            },
            mode: {
              value: (record) => { return 'HTTP_DOWNLOAD' }
            }
          },
          response: (result) => { return `Please click <a href="${result.template.url}" target="_blank">${result.template.url}</a> to download.` }
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'reconciliation-outlined',
          label: 'label.action.template.permission',
          docHelp: 'adminguide/templates.html#sharing-templates-with-other-accounts-projects',
          dataView: true,
          popup: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              record.templatetype !== 'SYSTEM' &&
              record.isready
          },
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/UpdateTemplateIsoPermissions')))
        }
      ]
    },
    {
      name: 'iso',
      title: 'label.isos',
      icon: 'usb-outlined',
      docHelp: 'adminguide/templates.html#working-with-isos',
      permission: ['listIsos'],
      params: { isofilter: 'self', showunique: 'true' },
      resourceType: 'ISO',
      filters: ['self', 'shared', 'featured', 'community'],
      columns: () => {
        var fields = ['name',
          {
            state: (record) => {
              if (record.isready) {
                return 'Ready'
              }
              return 'Not Ready'
            }
          }, 'ostypename']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('size')
          fields.push('account')
        }
        if (['Admin'].includes(store.getters.userInfo.roletype)) {
          fields.push('order')
        }
        return fields
      },
      details: ['name', 'id', 'displaytext', 'checksum', 'ostypename', 'size', 'bootable', 'isready', 'directdownload', 'isextractable', 'ispublic', 'isfeatured', 'crosszones', 'account', 'domain', 'created', 'userdatadetails', 'userdatapolicy', 'url'],
      searchFilters: ['name', 'zoneid', 'tags'],
      related: [{
        name: 'vm',
        title: 'label.instances',
        param: 'isoid'
      }],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
      }, {
        name: 'zones',
        component: shallowRef(defineAsyncComponent(() => import('@/views/image/IsoZones.vue')))
      },
      {
        name: 'events',
        resourceType: 'Iso',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
        show: () => { return 'listEvents' in store.getters.apis }
      },
      {
        name: 'comments',
        component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
      }],
      actions: [
        {
          api: 'registerIso',
          icon: 'plus-outlined',
          label: 'label.action.register.iso',
          docHelp: 'adminguide/templates.html#id10',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/RegisterOrUploadIso.vue')))
        },
        {
          api: 'registerIso',
          icon: 'cloud-upload-outlined',
          label: 'label.upload.iso.from.local',
          docHelp: 'adminguide/templates.html#id10',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/RegisterOrUploadIso.vue')))
        },
        {
          api: 'updateIso',
          icon: 'edit-outlined',
          label: 'label.action.edit.iso',
          dataView: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              !(record.account === 'system' && record.domainid === 1) &&
              record.isready
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/UpdateISO.vue')))
        },
        {
          api: 'updateIsoPermissions',
          icon: 'share-alt-outlined',
          label: 'label.action.iso.share',
          dataView: true,
          args: (record, store) => {
            const fields = ['isfeatured', 'isextractable']
            if (['Admin'].includes(store.userInfo.roletype) || store.features.userpublictemplateenabled) {
              fields.unshift('ispublic')
            }
            return fields
          },
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              !(record.account === 'system' && record.domainid === 1) &&
              record.isready
          }
        },
        {
          api: 'extractIso',
          icon: 'cloud-download-outlined',
          label: 'label.action.download.iso',
          message: 'message.action.download.iso',
          docHelp: 'adminguide/templates.html#exporting-templates',
          dataView: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account && record.isextractable) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              !(record.account === 'system' && record.domainid === 1) &&
              record.isready
          },
          args: ['zoneid', 'mode'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            },
            mode: {
              value: (record) => { return 'HTTP_DOWNLOAD' }
            }
          },
          response: (result) => { return `Please click <a href="${result.iso.url}" target="_blank">${result.iso.url}</a> to download.` }
        },
        {
          api: 'updateIsoPermissions',
          icon: 'reconciliation-outlined',
          label: 'label.action.iso.permission',
          docHelp: 'adminguide/templates.html#sharing-templates-with-other-accounts-projects',
          dataView: true,
          args: ['op', 'accounts', 'projectids'],
          popup: true,
          show: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || // If admin or owner or belongs to current project
              (record.domainid === store.userInfo.domainid && record.account === store.userInfo.account) ||
              (record.domainid === store.userInfo.domainid && record.projectid && store.project && store.project.id && record.projectid === store.project.id)) &&
              !(record.account === 'system' && record.domainid === 1) &&
              record.isready
          },
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/UpdateTemplateIsoPermissions')))
        }
      ]
    },
    {
      name: 'kubernetesiso',
      title: 'label.kubernetes.isos',
      icon: shallowRef(kubernetes),
      docHelp: 'plugins/cloudstack-kubernetes-service.html#kubernetes-supported-versions',
      permission: ['listKubernetesSupportedVersions'],
      columns: ['name', 'state', 'semanticversion', 'isostate', 'mincpunumber', 'minmemory', 'zonename'],
      details: ['name', 'semanticversion', 'supportsautoscaling', 'zoneid', 'zonename', 'isoid', 'isoname', 'isostate', 'mincpunumber', 'minmemory', 'supportsha', 'state', 'created'],
      actions: [
        {
          api: 'addKubernetesSupportedVersion',
          icon: 'plus-outlined',
          label: 'label.kubernetes.version.add',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/AddKubernetesSupportedVersion.vue')))
        },
        {
          api: 'updateKubernetesSupportedVersion',
          icon: 'edit-outlined',
          label: 'label.kubernetes.version.update',
          dataView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/image/UpdateKubernetesSupportedVersion.vue')))
        },
        {
          api: 'deleteKubernetesSupportedVersion',
          icon: 'delete-outlined',
          label: 'label.kubernetes.version.delete',
          message: 'message.kubernetes.version.delete',
          dataView: true
        }
      ]
    }
  ]
}
