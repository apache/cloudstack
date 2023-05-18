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
  name: 'project',
  title: 'label.projects',
  icon: 'project-outlined',
  docHelp: 'adminguide/projects.html',
  permission: ['listProjects'],
  resourceType: 'Project',
  columns: ['name', 'state', 'displaytext', 'account', 'domain'],
  searchFilters: ['name', 'displaytext', 'domainid', 'account'],
  details: ['name', 'id', 'displaytext', 'projectaccountname', 'account', 'domain'],
  tabs: [
    {
      name: 'details',
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/ProjectDetailsTab.vue')))
    },
    {
      name: 'accounts',
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/AccountsTab.vue'))),
      show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) || record.isCurrentUserProjectAdmin }
    },
    {
      name: 'project.roles',
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/iam/ProjectRoleTab.vue'))),
      show: (record, route, user) => {
        return (['Admin', 'DomainAdmin'].includes(user.roletype) || record.isCurrentUserProjectAdmin) &&
        'listProjectRoles' in store.getters.apis
      }
    },
    {
      name: 'resources',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/ResourceCountUsage.vue')))
    },
    {
      name: 'limits',
      show: (record, route, user) => { return ['Admin', 'DomainAdmin'].includes(user.roletype) },
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/ResourceLimitTab.vue')))
    },
    {
      name: 'events',
      resourceType: 'Project',
      component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
      show: () => { return 'listEvents' in store.getters.apis }
    }
  ],
  actions: [
    {
      api: 'createProject',
      icon: 'plus-outlined',
      label: 'label.new.project',
      docHelp: 'adminguide/projects.html#creating-a-new-project',
      listView: true,
      args: ['name', 'displaytext']
    },
    {
      api: 'updateProjectInvitation',
      icon: 'key-outlined',
      label: 'label.enter.token',
      docHelp: 'adminguide/projects.html#accepting-a-membership-invitation',
      listView: true,
      popup: true,
      show: (record, store) => { return store.features.projectinviterequired },
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/InvitationTokenTemplate.vue')))
    },
    {
      api: 'listProjectInvitations',
      icon: 'team-outlined',
      label: 'label.project.invitation',
      docHelp: 'adminguide/projects.html#accepting-a-membership-invitation',
      listView: true,
      popup: true,
      showBadge: true,
      badgeNum: 0,
      param: {
        state: 'Pending'
      },
      show: (record, store) => { return store.features.projectinviterequired },
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/InvitationsTemplate.vue')))
    },
    {
      api: 'updateProject',
      icon: 'edit-outlined',
      label: 'label.edit.project.details',
      dataView: true,
      args: ['name', 'displaytext'],
      show: (record, store) => {
        return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype)) || record.isCurrentUserProjectAdmin
      }
    },
    {
      api: 'activateProject',
      icon: 'play-circle-outlined',
      label: 'label.activate.project',
      message: 'message.activate.project',
      dataView: true,
      show: (record, store) => {
        return ((['Admin', 'DomainAdmin'].includes(store.userInfo.roletype)) || record.isCurrentUserProjectAdmin) && record.state === 'Suspended'
      },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'suspendProject',
      icon: 'pause-circle-outlined',
      label: 'label.suspend.project',
      message: 'message.suspend.project',
      docHelp: 'adminguide/projects.html#sending-project-membership-invitations',
      dataView: true,
      show: (record, store) => {
        return ((['Admin', 'DomainAdmin'].includes(store.userInfo.roletype)) ||
        record.isCurrentUserProjectAdmin) && record.state !== 'Suspended'
      },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    },
    {
      api: 'addAccountToProject',
      icon: 'user-add-outlined',
      label: 'label.action.project.add.account',
      docHelp: 'adminguide/projects.html#adding-project-members-from-the-ui',
      dataView: true,
      popup: true,
      show: (record, store) => {
        return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype)) || record.isCurrentUserProjectAdmin
      },
      component: shallowRef(defineAsyncComponent(() => import('@/views/project/AddAccountOrUserToProject.vue')))
    },
    {
      api: 'deleteProject',
      icon: 'delete-outlined',
      label: 'label.delete.project',
      message: 'message.delete.project',
      docHelp: 'adminguide/projects.html#suspending-or-deleting-a-project',
      dataView: true,
      show: (record, store) => {
        return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || record.isCurrentUserProjectAdmin) && record.id !== store.project.id
      },
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, cleanup: values.cleanup || null } }) },
      args: (record, store) => {
        const fields = []
        if (store.apis.deleteProject.params.filter(x => x.name === 'cleanup').length > 0) {
          fields.push('cleanup')
        }
        return fields
      }
    }
  ]
}
