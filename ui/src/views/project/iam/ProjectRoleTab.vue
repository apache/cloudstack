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
<template>
  <div>
    <a-button type="dashed" icon="plus" style="width: 100%; margin-bottom: 15px" @click="openCreateModal">
      {{ $t('label.create.project.role') }}
    </a-button>
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-table
          size="small"
          :loading="loading"
          :columns="columns"
          :dataSource="dataSource"
          :rowKey="(record,idx) => record.projectid + '-' + idx"
          :pagination="false">
          <template slot="expandedRowRender" slot-scope="record">
            <ProjectRolePermissionTab class="table" :resource="resource" :role="record"/>
          </template>
          <template slot="name" slot-scope="record"> {{ record }} </template>
          <template slot="description" slot-scope="record">
            {{ record }}
          </template>
          <span slot="action" slot-scope="text, record">
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.update.project.role')"
              icon="edit"
              size="small"
              style="margin:10px"
              @click="openUpdateModal(record)" />
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.remove.project.role')"
              type="danger"
              icon="delete"
              size="small"
              @click="deleteProjectRole(record)" />
          </span>
        </a-table>
        <a-modal
          :title="$t('label.edit.project.role')"
          v-model="editModalVisible"
          :footer="null"
          :afterClose="closeAction"
          :maskClosable="false">
          <a-form
            :form="form"
            @submit="updateProjectRole"
            layout="vertical">
            <a-form-item :label="$t('label.name')">
              <a-input v-decorator="['name']" autoFocus></a-input>
            </a-form-item>
            <a-form-item :label="$t('label.description')">
              <a-input v-decorator="['description']"></a-input>
            </a-form-item>
            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
              <a-button type="primary" @click="updateProjectRole" :loading="loading">{{ $t('label.ok') }}</a-button>
            </div>
            <span slot="action" slot-scope="text, record">
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.update.project.role')"
                icon="edit"
                size="small"
                style="margin:10px"
                @click="openUpdateModal(record)" />
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.remove.project.role')"
                type="danger"
                icon="edit"
                size="small"
                @click="deleteProjectRole(record)" />
            </span>
          </a-form>
        </a-modal>
        <a-modal
          :title="$t('label.create.project.role')"
          v-model="createModalVisible"
          :footer="null"
          :afterClose="closeAction"
          :maskClosable="false">
          <a-form
            :form="form"
            @submit="createProjectRole"
            layout="vertical">
            <a-form-item :label="$t('label.name')">
              <a-input
                v-decorator="[ 'name', { rules: [{ required: true, message: 'Please provide input' }] }]"
                autoFocus></a-input>
            </a-form-item>
            <a-form-item :label="$t('label.description')">
              <a-input v-decorator="[ 'description' ]"></a-input>
            </a-form-item>
            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
              <a-button type="primary" @click="createProjectRole" :loading="loading">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-modal>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { api } from '@/api'
import ProjectRolePermissionTab from '@/views/project/iam/ProjectRolePermissionTab'
import TooltipButton from '@/components/view/TooltipButton'
export default {
  name: 'ProjectRoleTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ProjectRolePermissionTab,
    TooltipButton
  },
  data () {
    return {
      columns: [],
      dataSource: [],
      loading: false,
      createModalVisible: false,
      editModalVisible: false,
      selectedRole: null,
      projectPermisssions: [],
      customStyle: 'margin-bottom: -10px; border-bottom-style: none'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.columns = [
      {
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: '35%',
        scopedSlots: { customRender: 'name' }
      },
      {
        title: this.$t('label.description'),
        dataIndex: 'description'
      },
      {
        title: this.$t('label.action'),
        dataIndex: 'action',
        width: 100,
        scopedSlots: { customRender: 'action' }
      }
    ]
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    resource (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listProjectRoles', { projectid: this.resource.id }).then(json => {
        const projectRoles = json.listprojectrolesresponse.projectrole
        if (!projectRoles || projectRoles.length === 0) {
          this.dataSource = []
          return
        }
        this.dataSource = projectRoles
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    openUpdateModal (role) {
      this.selectedRole = role
      this.editModalVisible = true
    },
    openCreateModal () {
      this.createModalVisible = true
    },
    updateProjectRole (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        var params = {}
        this.loading = true
        params.projectid = this.resource.id
        params.id = this.selectedRole.id
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        api('updateProjectRole', params).then(response => {
          this.$notification.success({
            message: this.$t('label.update.project.role'),
            description: this.$t('label.update.project.role')
          })
          this.fetchData()
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      if (this.editModalVisible) {
        this.editModalVisible = false
      }
      if (this.createModalVisible) {
        this.createModalVisible = false
      }
    },
    createProjectRole (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        var params = {}
        params.projectid = this.resource.id
        for (const key in values) {
          const input = values[key]
          if (input === undefined) {
            continue
          }
          params[key] = input
        }
        api('createProjectRole', params).then(response => {
          this.$notification.success({
            message: this.$t('label.create.project.role'),
            description: this.$t('label.create.project.role')
          })
          this.fetchData()
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    deleteProjectRole (role) {
      this.loading = true
      api('deleteProjectRole', {
        projectid: this.resource.id,
        id: role.id
      }).then(response => {
        this.$notification.success({
          message: this.$t('label.delete.project.role'),
          description: this.$t('label.delete.project.role')
        })
        this.fetchData()
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.action-button {
    text-align: right;
    button {
      margin-right: 5px;
    }
  }
</style>
