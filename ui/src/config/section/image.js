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
  name: 'image',
  title: 'Images',
  icon: 'picture',
  children: [
    {
      name: 'template',
      title: 'Templates',
      icon: 'save',
      permission: ['listTemplates'],
      params: { templatefilter: 'executable' },
      resourceType: 'Template',
      columns: ['name', 'ostypename', 'status', 'hypervisor', 'account', 'domain', 'order'],
      details: ['name', 'id', 'displaytext', 'checksum', 'hypervisor', 'format', 'ostypename', 'size', 'isready', 'passwordenabled', 'directdownload', 'isextractable', 'isdynamicallyscalable', 'ispublic', 'isfeatured', 'crosszones', 'type', 'account', 'domain', 'created'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'templateid'
      }],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'zones',
        component: () => import('@/views/image/TemplateZones.vue')
      }, {
        name: 'settings',
        component: () => import('@/components/view/DetailSettings')
      }],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus',
          label: 'Create template',
          listView: true,
          popup: true,
          component: () => import('@/views/image/RegisterTemplate.vue')
        },
        {
          api: 'getUploadParamsForTemplate',
          icon: 'cloud-upload',
          label: 'Upload Local Template',
          listView: true,
          popup: true,
          component: () => import('@/views/image/UploadLocalTemplate.vue')
        },
        {
          api: 'updateTemplate',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['name', 'displaytext', 'passwordenabled', 'sshkeyenabled', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractTemplate',
          icon: 'cloud-download',
          label: 'Download Template',
          dataView: true,
          show: (record) => { return record && record.isextractable },
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
          icon: 'reconciliation',
          label: 'Update template permissions',
          dataView: true,
          popup: true,
          show: (record, user) => { return (['Admin', 'DomainAdmin'].includes(user.roletype) && (record.domainid === user.domainid && record.account === user.account) || record.templatetype !== 'BUILTIN') },
          component: () => import('@/views/image/UpdateTemplatePermissions')
        },
        {
          api: 'copyTemplate',
          icon: 'copy',
          label: 'Copy Template',
          args: ['sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteTemplate',
          icon: 'delete',
          label: 'Delete Template',
          args: ['zoneid'],
          dataView: true,
          groupAction: true
        }
      ]
    },
    {
      name: 'iso',
      title: 'ISOs',
      icon: 'usb',
      permission: ['listIsos'],
      params: { isofilter: 'executable' },
      resourceType: 'ISO',
      columns: ['name', 'ostypename', 'account', 'domain'],
      details: ['name', 'id', 'displaytext', 'checksum', 'ostypename', 'size', 'bootable', 'isready', 'directdownload', 'isextractable', 'ispublic', 'isfeatured', 'crosszones', 'account', 'domain', 'created'],
      related: [{
        name: 'vm',
        title: 'Instances',
        param: 'isoid'
      }],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'zones',
        component: () => import('@/views/image/IsoZones.vue')
      }],
      actions: [
        {
          api: 'registerIso',
          icon: 'plus',
          label: 'Register ISO',
          listView: true,
          popup: true,
          component: () => import('@/views/image/RegisterIso.vue')
        },
        {
          api: 'getUploadParamsForIso',
          icon: 'cloud-upload',
          label: 'Upload Local Iso',
          listView: true,
          popup: true,
          component: () => import('@/views/image/UploadLocalIso.vue')
        },
        {
          api: 'updateIso',
          icon: 'edit',
          label: 'label.edit',
          dataView: true,
          args: ['name', 'displaytext', 'bootable', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractIso',
          icon: 'cloud-download',
          label: 'Download ISO',
          dataView: true,
          show: (record) => { return record && record.isextractable },
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
          icon: 'reconciliation',
          label: 'Update ISO Permissions',
          dataView: true,
          args: ['op', 'accounts', 'projectids']
        },
        {
          api: 'copyIso',
          icon: 'copy',
          label: 'Copy ISO',
          args: ['sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteIso',
          icon: 'delete',
          label: 'Delete ISO',
          args: ['zoneid'],
          dataView: true,
          groupAction: true
        }
      ]
    }
  ]
}
