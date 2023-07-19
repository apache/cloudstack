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
    <a-button type="dashed" style="width: 100%; margin-bottom: 15px" @click="openCreateModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.create.project.role') }}
    </a-button>
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-table
          size="small"
          :loading="loading"
          :columns="columns"
          :dataSource="dataSource"
          :rowKey="(record, index) => record.projectid + '-' + index"
          :pagination="false">
          <template #expandedRowRender="{ record }">
            <ProjectRolePermissionTab class="table" :resource="resource" :role="record"/>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'"> {{ record.name }} </template>
            <template v-if="column.key === 'description'">
              {{ record.description }}
            </template>
            <template v-if="column.key === 'actions'">
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.update.project.role')"
                icon="edit-outlined"
                size="small"
                style="margin:10px"
                @onClick="openUpdateModal(record)" />
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.remove.project.role')"
                type="primary"
                :danger="true"
                icon="delete-outlined"
                size="small"
                @onClick="deleteProjectRole(record)" />
            </template>
          </template>
        </a-table>
        <a-modal
          :title="$t('label.edit.project.role')"
          :visible="editModalVisible"
          :footer="null"
          :afterClose="closeAction"
          :maskClosable="false"
          :closable="true"
          @cancel="closeAction">
          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            layout="vertical"
            @finish="updateProjectRole"
            v-ctrl-enter="updateProjectRole"
           >
            <a-form-item ref="name" name="name" :label="$t('label.name')">
              <a-input v-model:value="form.name" v-focus="true"></a-input>
            </a-form-item>
            <a-form-item ref="description" name="description" :label="$t('label.description')">
              <a-input v-model:value="form.description"></a-input>
            </a-form-item>
            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" ref="submit" @click="updateProjectRole" :loading="loading">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-modal>
        <a-modal
          :title="$t('label.create.project.role')"
          :visible="createModalVisible"
          :footer="null"
          :afterClose="closeAction"
          :maskClosable="false"
          :closable="true"
          @cancel="closeAction">
          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="createProjectRole"
            v-ctrl-enter="createProjectRole"
            layout="vertical"
           >
            <a-form-item ref="name" name="name" :label="$t('label.name')">
              <a-input
                v-model:value="form.name"
                v-focus="true"></a-input>
            </a-form-item>
            <a-form-item ref="description" name="description" :label="$t('label.description')">
              <a-input v-model:value="form.description"></a-input>
            </a-form-item>
            <div :span="24" class="action-button">
              <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
              <a-button type="primary" ref="submit" @click="createProjectRole" :loading="loading">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-modal>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import ProjectRolePermissionTab from '@/views/project/iam/ProjectRolePermissionTab'
import TooltipButton from '@/components/widgets/TooltipButton'

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
      customStyle: 'margin-bottom: 0; border: none'
    }
  },
  created () {
    this.columns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name',
        width: '35%'
      },
      {
        key: 'description',
        title: this.$t('label.description'),
        dataIndex: 'description'
      },
      {
        key: 'actions',
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        width: 100
      }
    ]
    this.initForm()
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
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
      this.rules = {}
    },
    openCreateModal () {
      this.createModalVisible = true
      this.rules = {
        name: [{ required: true, message: this.$t('message.error.required.input') }]
      }
    },
    updateProjectRole (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
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
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
