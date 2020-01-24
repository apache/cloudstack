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
  <a-spin :spinning="fetchLoading">
    <a-button type="dashed" icon="plus" style="width: 100%" @click="handleOpenModal">{{ $t('label.dedicate.vlan.vni.range') }}</a-button>
    <a-list class="list">
      <a-list-item v-for="item in items" :key="item.id" class="list__item">
        <div class="list__item-outer-container">
          <div class="list__item-container">
            <div class="list__col">
              <div class="list__label">{{ $t('vlanrange') }}</div>
              <div>{{ item.guestvlanrange }}</div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('domain') }}</div>
              <div>{{ item.domain }}</div>
            </div>
            <div class="list__col">
              <div class="list__label">{{ $t('account') }}</div>
              <div>{{ item.account }}</div>
            </div>
          </div>
          <div class="list__col">
            <div class="list__label">{{ $t('id') }}</div>
            <div>{{ item.id }}</div>
          </div>
        </div>
        <div slot="actions">
          <a-popconfirm
            :title="`${$t('label.delete')}?`"
            @confirm="handleDelete(item)"
            okText="Yes"
            cancelText="No"
            placement="top"
          >
            <a-button icon="delete" type="danger" shape="round"></a-button>
          </a-popconfirm>
        </div>
      </a-list-item>
    </a-list>

    <a-modal v-model="modal" :title="$t('label.dedicate.vlan.vni.range')" @ok="handleSubmit">
      <a-spin :spinning="formLoading">
        <a-form
          :form="form"
          @submit="handleSubmit"
          layout="vertical" >
          <a-form-item :label="$t('vlanRange')">
            <a-input
              v-decorator="['range', {
                rules: [{ required: true, message: 'Required' }]
              }]"
            ></a-input>
          </a-form-item>

          <a-form-item :label="$t('scope')">
            <a-select defaultValue="account" v-model="selectedScope" @change="handleScopeChange">
              <a-select-option value="account">{{ $t('account') }}</a-select-option>
              <a-select-option value="project">{{ $t('project') }}</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('domain')">
            <a-select
              @change="handleDomainChange"
              v-decorator="['domain', {
                rules: [{ required: true, message: 'Required' }]
              }]"
            >
              <a-select-option v-for="domain in domains" :key="domain.id" :value="domain.id">{{ domain.name }}</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('account')" v-if="selectedScope === 'account'">
            <a-select
              v-decorator="['account', {
                rules: [{ required: true, message: 'Required' }]
              }]"
            >
              <a-select-option
                v-for="account in accounts"
                :key="account.id"
                :value="account.name">
                {{ account.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('project')" v-if="selectedScope === 'project'">
            <a-select
              v-decorator="['project', {
                rules: [{ required: true, message: 'Required' }]
              }]"
            >
              <a-select-option
                v-for="project in projects"
                :key="project.id"
                :value="project.id">
                {{ project.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>

  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'DedicatedVLANTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  inject: ['parentStartLoading', 'parentFinishLoading'],
  data () {
    return {
      fetchLoading: false,
      formLoading: false,
      items: [],
      domains: [],
      accounts: [],
      projects: [],
      modal: false,
      selectedScope: 'account'
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData () {
      this.form.resetFields()
      api('listDedicatedGuestVlanRanges', {
        physicalnetworkid: this.resource.id
      }).then(response => {
        this.items = response.listdedicatedguestvlanrangesresponse.dedicatedguestvlanrange
          ? response.listdedicatedguestvlanrangesresponse.dedicatedguestvlanrange : []
        this.parentFinishLoading()
        this.formLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.parentFinishLoading()
        this.formLoading = false
      })
    },
    fetchDomains () {
      api('listDomains', {
        details: 'min',
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain
          ? response.listdomainsresponse.domain : []
        if (this.domains.length > 0) {
          this.form.setFieldsValue({
            domain: this.domains[0].id
          })
          this.fetchAccounts(this.form.getFieldValue('domain'))
        } else {
          this.form.setFieldsValue({
            domain: null
          })
        }
        this.formLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.formLoading = false
      })
    },
    fetchAccounts (e) {
      this.formLoading = true
      api('listAccounts', {
        domainid: e,
        details: 'min',
        listAll: true
      }).then(response => {
        this.accounts = response.listaccountsresponse.account
          ? response.listaccountsresponse.account : []
        if (this.accounts.length > 0) {
          this.form.setFieldsValue({
            account: this.accounts[0].name
          })
        } else {
          this.form.setFieldsValue({
            account: null
          })
        }
        this.formLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.formLoading = false
      })
    },
    fetchProjects (e) {
      this.formLoading = true
      api('listProjects', {
        domainid: e,
        details: 'min'
      }).then(response => {
        this.projects = response.listprojectsresponse.project
          ? response.listprojectsresponse.project : []
        if (this.projects.length > 0) {
          this.form.setFieldsValue({
            project: this.projects[0].id
          })
        } else {
          this.form.setFieldsValue({
            project: null
          })
        }
        this.formLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.formLoading = false
      })
    },
    handleDelete (item) {
      this.parentStartLoading()
      api('releaseDedicatedGuestVlanRange', {
        id: item.id
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Deleted dedicated VLAN/VNI range ${item.guestvlanrange} for ${item.account}`,
          jobid: response.releasededicatedguestvlanrangeresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.releasededicatedguestvlanrangeresponse.jobid,
          successMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          },
          errorMessage: 'Deleting failed',
          errorMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          },
          loadingMessage: `Deleting ${item.id}`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          }
        })
      }).catch(error => {
        console.log(error)
        this.$message.error('Failed to delete.')
        this.fetchData()
        this.parentFinishLoading()
      })
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields(errors => {
        if (errors) return

        this.formLoading = true
        this.parentStartLoading()
        const fieldValues = this.form.getFieldsValue()

        api('dedicateGuestVlanRange', {
          physicalnetworkid: this.resource.id,
          vlanrange: fieldValues.range,
          domainid: fieldValues.domain,
          account: fieldValues.account,
          projectid: fieldValues.project
        }).then(response => {
          this.modal = false
          this.fetchData()
        }).catch(error => {
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.dedicateguestvlanrangeresponse.errortext
          })
          this.modal = false
          this.fetchData()
        })
      })
    },
    fetchBasedOnScope (e) {
      if (e === 'account') this.fetchAccounts(this.form.getFieldValue('domain'))
      if (e === 'project') this.fetchProjects(this.form.getFieldValue('domain'))
    },
    handleDomainChange () {
      setTimeout(() => {
        this.fetchBasedOnScope(this.selectedScope)
      }, 100)
    },
    handleScopeChange (e) {
      this.fetchBasedOnScope(e)
    },
    handleOpenModal () {
      this.modal = true
      this.formLoading = true
      this.fetchDomains()
    }
  }
}
</script>

<style lang="scss" scoped>
.list {

  &__label {
    font-weight: bold;
  }

  &__col {
    flex: 1;

    @media (min-width: 480px) {
      &:not(:last-child) {
        margin-right: 20px;
      }
    }
  }

  &__item {
    margin-right: -8px;

    &-outer-container {
      width: 100%;
    }

    &-container {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 480px) {
        flex-direction: row;
        margin-bottom: 10px;
      }
    }
  }
}
</style>
