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
      permission: [ 'listTemplates' ],
      params: { 'templatefilter': 'executable' },
      resourceType: 'Template',
      columns: ['name', 'ostypename', 'status', 'hypervisor', 'account', 'domain'],
      details: ['name', 'id', 'displaytext', 'checksum', 'hypervisor', 'format', 'ostypename', 'size', 'isready', 'passwordenabled', 'directdownload', 'isextractable', 'isdynamicallyscalable', 'ispublic', 'isfeatured', 'crosszones', 'type', 'account', 'domain', 'created'],
      tabs: [{
        name: 'details',
        component: () => import('@/components/view/DetailsTab.vue')
      }, {
        name: 'zones',
        component: () => import('@/views/image/TemplateZones.vue')
      }, {
        name: 'settings',
        component: () => import('@/views/image/TemplateSettings.vue')
      }],
      actions: [
        {
          api: 'registerTemplate',
          icon: 'plus',
          label: 'Create template',
          listView: true,
          args: ['url', 'name', 'displaytext', 'directdownload', 'zoneids', 'hypervisor', 'format', 'ostypeid', 'checksum', 'isextractable', 'passwordenabled', 'sshkeyenabled', 'isdynamicallyscalable', 'ispublic', 'isfeatured', 'isrouting', 'requireshvm']
        },
        {
          api: 'getUploadParamsForVolume',
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
          args: ['id', 'name', 'displaytext', 'passwordenabled', 'sshkeyenabled', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractTemplate',
          icon: 'cloud-download',
          label: 'Download Template',
          dataView: true,
          args: ['id', 'zoneid', 'mode']
        },
        {
          api: 'updateTemplatePermissions',
          icon: 'reconciliation',
          label: 'Update template permissions',
          dataView: true,
          args: ['id', 'op', 'accounts', 'projectids']
        },
        {
          api: 'copyTemplate',
          icon: 'copy',
          label: 'Copy Template',
          args: ['id', 'sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteTemplate',
          icon: 'delete',
          label: 'Delete Template',
          args: ['id', 'zoneid'],
          dataView: true,
          groupAction: true
        }
      ]
    },
    {
      name: 'iso',
      title: 'ISOs',
      icon: 'usb',
      permission: [ 'listIsos' ],
      params: { 'isofilter': 'executable' },
      resourceType: 'ISO',
      columns: ['name', 'ostypename', 'account', 'domain'],
      details: ['name', 'id', 'displaytext', 'checksum', 'ostypename', 'size', 'bootable', 'isready', 'directdownload', 'isextractable', 'ispublic', 'isfeatured', 'crosszones', 'account', 'domain', 'created'],
      actions: [
        {
          api: 'registerIso',
          icon: 'plus',
          label: 'Register ISO',
          listView: true,
          args: ['url', 'name', 'displaytext', 'directdownload', 'zoneid', 'bootable', 'ostypeid', 'isextractable', 'ispublic', 'isfeatured']
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
          args: ['id', 'name', 'displaytext', 'bootable', 'ostypeid', 'isdynamicallyscalable', 'isrouting']
        },
        {
          api: 'extractIso',
          icon: 'cloud-download',
          label: 'Download ISO',
          dataView: true,
          args: ['id', 'zoneid', 'mode']
        },
        {
          api: 'updateIsoPermissions',
          icon: 'reconciliation',
          label: 'Update ISO Permissions',
          dataView: true,
          args: ['id', 'op', 'accounts', 'projectids']
        },
        {
          api: 'copyIso',
          icon: 'copy',
          label: 'Copy ISO',
          args: ['id', 'sourcezoneid', 'destzoneids'],
          dataView: true
        },
        {
          api: 'deleteIso',
          icon: 'delete',
          label: 'Delete ISO',
          args: ['id', 'zoneid'],
          dataView: true,
          groupAction: true
        }
      ]
    }
  ]
}
