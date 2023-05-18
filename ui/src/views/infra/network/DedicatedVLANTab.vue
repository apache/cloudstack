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
      style="width: 100%"
      @click="handleOpenModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.dedicate.vlan.vni.range') }}
    </a-button>
    <a-table
      size="small"
      style="overflow-y: auto; margin-top: 20px;"
      :loading="fetchLoading"
      :columns="columns"
      :dataSource="items"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-popconfirm
            :title="`${$t('label.delete')}?`"
            @confirm="handleDelete(record)"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')"
            placement="top"
          >
            <tooltip-button
              :tooltip="$t('label.delete')"
              :disabled="!('releaseDedicatedGuestVlanRange' in $store.getters.apis)"
              icon="delete-outlined"
              type="primary"
              :danger="true" />
          </a-popconfirm>
        </template>
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
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="modal"
      :title="$t('label.dedicate.vlan.vni.range')"
      :maskClosable="false"
      :footer="null"
      @cancel="modal = false">
      <a-spin :spinning="formLoading" v-ctrl-enter="handleSubmit">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical"
         >
          <a-form-item name="range" ref="range" :label="$t('label.vlanrange')">
            <a-input v-model:value="form.range" v-focus="true" />
          </a-form-item>

          <a-form-item name="scope" ref="scope" :label="$t('label.scope')">
            <a-select
              v-model:value="form.scope"
              @change="handleScopeChange"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option value="account" :label="$t('label.account')">{{ $t('label.account') }}</a-select-option>
              <a-select-option value="project" :label="$t('label.project')">{{ $t('label.project') }}</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item name="domain" ref="domain" :label="$t('label.domain')">
            <a-select
              @change="handleDomainChange"
              v-model:value="form.domain"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="domain in domains" :key="domain.id" :value="domain.id" :label="domain.path || domain.name || domain.description">
                <span>
                  <resource-icon v-if="domain && domain.icon" :image="domain.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <block-outlined v-else style="margin-right: 5px" />
                  {{ domain.path || domain.name || domain.description }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item name="account" ref="account" :label="$t('label.account')" v-if="form.scope === 'account'">
            <a-select
              v-model:value="form.account"
              showSearch
              optionFilterProp="value"
              :filterOption="(input, option) => {
                return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="account in accounts"
                :key="account.id"
                :value="account.name">
                <span>
                  <resource-icon v-if="account && account.icon" :image="account.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <team-outlined v-else style="margin-right: 5px" />
                  {{ account.name }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item name="project" ref="project" :label="$t('label.project')" v-if="form.scope === 'project'">
            <a-select
              v-model:value="form.project"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option
                v-for="project in projects"
                :key="project.id"
                :value="project.id"
                :label="project.name">
                <span>
                  <resource-icon v-if="project && project.icon" :image="project.icon.base64image" size="1x" style="margin-right: 5px"/>
                  <project-outlined v-else style="margin-right: 5px" />
                  {{ project.name }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="modal = false">{{ $t('label.cancel') }}</a-button>
            <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'DedicatedVLANTab',
  components: {
    TooltipButton,
    ResourceIcon
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
          key: 'actions',
          title: this.$t('label.actions')
        }
      ]
    }
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
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        scope: 'account'
      })
      this.rules = reactive({
        range: [{ required: true, message: this.$t('label.required') }],
        domain: [{ required: true, message: this.$t('label.required') }],
        account: [{ required: true, message: this.$t('label.required') }],
        project: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
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
        showicon: true,
        listAll: true
      }).then(response => {
        this.domains = response.listdomainsresponse.domain || []
        if (this.domains.length > 0) {
          this.form.domain = this.domains[0].id
          this.fetchAccounts(this.form.domain)
        } else {
          this.form.domain = null
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
        showicon: true,
        listAll: true
      }).then(response => {
        this.accounts = response.listaccountsresponse.account
          ? response.listaccountsresponse.account : []
        if (this.accounts.length > 0) {
          this.form.account = this.accounts[0].name
        } else {
          this.form.account = null
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
        showicon: true,
        details: 'min'
      }).then(response => {
        this.projects = response.listprojectsresponse.project
          ? response.listprojectsresponse.project : []
        if (this.projects.length > 0) {
          this.form.project = this.projects[0].id
        } else {
          this.form.project = null
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
        this.$pollJob({
          jobId: response.releasededicatedguestvlanrangeresponse.jobid,
          title: this.$t('label.delete.dedicated.vlan.range'),
          description: `${this.$t('label.delete.dedicated.vlan.range')} ${item.guestvlanrange} ${this.$t('label.for')} ${item.account}`,
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
      if (this.formLoading) return
      this.formRef.value.validate().then(() => {
        this.formLoading = true
        this.parentStartLoading()
        const fieldValues = toRaw(this.form)

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
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    fetchBasedOnScope (e) {
      if (e === 'account') this.fetchAccounts(this.form.domain)
      if (e === 'project') this.fetchProjects(this.form.domain)
    },
    handleDomainChange () {
      setTimeout(() => {
        this.fetchBasedOnScope(this.form.scope)
      }, 100)
    },
    handleScopeChange (e) {
      this.fetchBasedOnScope(e)
    },
    handleOpenModal () {
      this.modal = true
      this.formLoading = true
      this.initForm()
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
