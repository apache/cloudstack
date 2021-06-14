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
    <a-button
      :disabled="!('dedicateGuestVlanRange' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="width: 100%"
      @click="handleOpenModal">{{ $t('label.dedicate.vlan.vni.range') }}</a-button>
    <a-table
      size="small"
      style="overflow-y: auto; margin-top: 20px;"
      :loading="fetchLoading"
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.id">
      <template slot="actions" slot-scope="record">
        <a-popconfirm
          :title="`${$t('label.delete')}?`"
          @confirm="handleDelete(record)"
          :okText="$t('label.yes')"
          :cancelText="$t('label.no')"
          placement="top"
        >
          <tooltip-button :tooltip="$t('label.delete')" :disabled="!('releaseDedicatedGuestVlanRange' in $store.getters.apis)" icon="delete" type="danger" />
        </a-popconfirm>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template slot="buildOptionText" slot-scope="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-model="modal"
      :title="$t('label.dedicate.vlan.vni.range')"
      :maskClosable="false"
      @ok="handleSubmit">
      <a-spin :spinning="formLoading">
        <a-form
          :form="form"
          @submit="handleSubmit"
          layout="vertical" >
          <a-form-item :label="$t('label.vlanrange')">
            <a-input
              v-decorator="['range', {
                rules: [{ required: true, message: `${$t('label.required')}` }]
              }]"
              autoFocus
            ></a-input>
          </a-form-item>

          <a-form-item :label="$t('label.scope')">
            <a-select defaultValue="account" v-model="selectedScope" @change="handleScopeChange">
              <a-select-option value="account">{{ $t('label.account') }}</a-select-option>
              <a-select-option value="project">{{ $t('label.project') }}</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('label.domain')">
            <a-select
              @change="handleDomainChange"
              v-decorator="['domain', {
                rules: [{ required: true, message: `${$t('label.required')}` }]
              }]"
            >
              <a-select-option v-for="domain in domains" :key="domain.id" :value="domain.id">{{ domain.path || domain.name || domain.description }}</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('label.account')" v-if="selectedScope === 'account'">
            <a-select
              v-decorator="['account', {
                rules: [{ required: true, message: `${$t('label.required')}` }]
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

          <a-form-item :label="$t('label.project')" v-if="selectedScope === 'project'">
            <a-select
              v-decorator="['project', {
                rules: [{ required: true, message: `${$t('label.required')}` }]
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
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'DedicatedVLANTab',
  components: {
    TooltipButton
  },
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
      selectedScope: 'account',
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.vlanrange'),
          dataIndex: 'guestvlanrange'
        },
        {
          title: this.$t('label.domain'),
          dataIndex: 'domain'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'actions' }
        }
      ]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
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
      this.formLoading = true
      api('listDedicatedGuestVlanRanges', {
        physicalnetworkid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.items = response.listdedicatedguestvlanrangesresponse.dedicatedguestvlanrange || []
        this.totalCount = response.listdedicatedguestvlanrangesresponse.count || 0
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
      }).finally(() => {
        this.formLoading = false
        this.parentFinishLoading()
      })
    },
    fetchDomains () {
      api('listDomains', {
        details: 'min',
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain || []
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
        this.$notifyError(error)
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
        this.$notifyError(error)
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
        this.$notifyError(error)
        this.formLoading = false
      })
    },
    handleDelete (item) {
      this.parentStartLoading()
      api('releaseDedicatedGuestVlanRange', {
        id: item.id
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `${this.$t('label.delete.dedicated.vlan.range')} ${item.guestvlanrange} ${this.$t('label.for')} ${item.account}`,
          jobid: response.releasededicatedguestvlanrangeresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.releasededicatedguestvlanrangeresponse.jobid,
          successMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          },
          errorMessage: this.$t('label.deleting.failed'),
          errorMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          },
          loadingMessage: `${this.$t('label.deleting')} ${item.id}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.parentFinishLoading()
          }
        })
      }).catch(error => {
        console.log(error)
        this.$message.error(this.$t('message.fail.to.delete'))
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
          this.$notifyError(error)
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
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
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

.pagination {
  margin-top: 20px;
}
</style>
