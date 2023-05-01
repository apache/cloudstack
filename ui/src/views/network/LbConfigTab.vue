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
    <div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('label.lb.config.name') }}</div>
          <br>
          <a-select v-model:value="lbConfig.name" style="width: 100%;" @change="populateValues">
            <a-select-option
              v-for="config in globalLbConfigs"
              :key="config.name">{{ config.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.description') }}</div>
          <br>
          {{ lbConfig.description }}
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.lb.config.default.value') }}</div>
          <br>
          {{ lbConfig.defaultvalue }}
        </div>
        <div class="form__item" ref="lbconfigValue">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.lb.config.value') }}</div>
          <br>
          <a-input v-model:value="lbConfig.value"></a-input>
          <span class="error-text">Required</span>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.action') }}</div>
          <br>
          <a-button ref="submit" :disabled="!('createLoadBalancerConfig' in $store.getters.apis)" type="primary" @click="addLbConfig">
            <template #icon><plus-outlined /></template>
            {{ $t('label.add') }}
          </a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="savedLbConfigs"
      :pagination="false"
      :rowKey="record => record.id">
      <template #actions="{record}">
        <a-button :disabled="!('deleteLoadBalancerConfig' in $store.getters.apis)" shape="circle" type="danger" @click="deleteLbConfig(record)" >
          <template #icon><delete-outlined /></template>
        </a-button>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `Total ${total} items`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger/>
  </div>
</template>

<script>
import { api } from '@/api'
export default {
  name: 'LbConfigTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: true,
      globalLbConfigs: [],
      savedLbConfigs: [],
      lbConfig: {
        scope: 'Network',
        name: null,
        description: null,
        defaultvalue: null,
        value: null,
        forced: 'true',
        networkid: this.resource.id
      },
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.lb.config.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.description'),
          dataIndex: 'description'
        },
        {
          title: this.$t('label.lb.config.default.value'),
          dataIndex: 'defaultvalue'
        },
        {
          title: this.$t('label.lb.config.value'),
          dataIndex: 'value'
        },
        {
          title: this.$t('label.action'),
          slots: { customRender: 'actions' }
        }
      ]
    }
  },
  mounted () {
    this.fetchData(this.resource.id)
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      // this.resource = newItem
      this.fetchData(newItem.id)
    }
  },
  methods: {
    fetchData (id) {
      this.loading = true
      // Populate all lb confings
      api('listLoadBalancerConfigs', {
        scope: 'Network',
        listall: 'true',
        networkid: id
      }).then(response => {
        this.globalLbConfigs = response.listloadbalancerconfigsresponse.loadbalancerconfig || []
        this.lbConfig.name = this.globalLbConfigs[0].name
        this.lbConfig.description = this.globalLbConfigs[0].description
        this.lbConfig.defaultvalue = this.globalLbConfigs[0].defaultvalue
      }).finally(() => {
        this.loading = false
      })
      // Fetch saved lb configs
      api('listLoadBalancerConfigs', {
        scope: 'Network',
        networkid: id
      }).then(response => {
        this.savedLbConfigs = response.listloadbalancerconfigsresponse.loadbalancerconfig || []
        this.totalCount = response.listloadbalancerconfigsresponse.count || 0
      }).finally(() => {
        this.loading = false
      })
    },
    deleteLbConfig (rule) {
      this.loading = true
      api('deleteLoadBalancerConfig', { id: rule.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteloadbalancerconfigresponse.jobid,
          successMessage: `Successfully removed Load Balancer config`,
          successMethod: () => this.fetchData(this.resource.id),
          errorMessage: 'Removing Load Balancer config failed',
          errorMethod: () => this.fetchData(this.resource.id),
          loadingMessage: `Deleting Load Balancer config...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => this.fetchData(this.resource.id)
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData(this.resource.id)
      })
    },
    addLbConfig () {
      if (!this.lbConfig.value) {
        this.$refs.lbconfigValue.classList.add('error')
        return
      } else {
        this.$refs.lbconfigValue.classList.remove('error')
      }
      this.loading = true
      api('createLoadBalancerConfig', { ...this.lbConfig }).then(response => {
        this.$pollJob({
          jobId: response.createloadbalancerconfigresponse.jobid,
          successMessage: `Successfully created new load balancer config`,
          successMethod: () => {
            this.resetValues()
            this.fetchData(this.resource.id)
          },
          errorMessage: 'Adding new Load Balancer Config failed',
          errorMethod: () => {
            this.resetValues()
            this.fetchData(this.resource.id)
          },
          loadingMessage: `Adding new Load Balancer config...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.resetValues()
            this.fetchData(this.resource.id)
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.resetValues()
        this.fetchData(this.resource.id)
      })
    },
    resetValues () {
      this.lbConfig.name = ''
      this.lbConfig.description = null
      this.lbConfig.defaultValue = null
      this.lbConfig.value = null
      this.lbConfig.networkid = this.resource.id
    },
    populateValues () {
      for (let i = 0; i < this.globalLbConfigs.length; i++) {
        if (this.lbConfig.name === this.globalLbConfigs[i].name) {
          this.lbConfig.description = this.globalLbConfigs[i].description
          this.lbConfig.defaultvalue = this.globalLbConfigs[i].defaultvalue
        }
      }
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData(this.resource.id)
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData(this.resource.id)
    }
  }
}
</script>

<style scoped lang="scss">
.rule {
  &-container {
    display: flex;
    width: 100%;
    flex-wrap: wrap;
    margin-right: -20px;
    margin-bottom: -10px;
  }
  &__item {
    padding-right: 20px;
    margin-bottom: 20px;
    @media (min-width: 760px) {
      flex: 1;
    }
  }
  &__title {
    font-weight: bold;
  }
}
.add-btn {
  width: 100%;
  padding-top: 15px;
  padding-bottom: 15px;
  height: auto;
}
.add-actions {
  display: flex;
  justify-content: flex-end;
  margin-right: -20px;
  margin-bottom: 20px;
  @media (min-width: 760px) {
    margin-top: 20px;
  }
  button {
    margin-right: 20px;
  }
}
.form {
  display: flex;
  margin-right: -20px;
  margin-bottom: 20px;
  flex-direction: column;
  align-items: flex-start;
  @media (min-width: 760px) {
    flex-direction: row;
  }
  &__item {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding-right: 20px;
    margin-bottom: 20px;
    @media (min-width: 760px) {
      margin-bottom: 0;
      flex: 1;
    }
    input,
    .ant-select {
      margin-top: auto;
    }
  }
  &__label {
    font-weight: bold;
  }
  &__required {
    margin-right: 5px;
    color: red;
  }
  .error-text {
    display: none;
    color: red;
    font-size: 0.8rem;
  }
  .error {
    input {
      border-color: red;
    }
    .error-text {
      display: block;
    }
  }
}
.pagination {
  margin-top: 20px;
}
</style>
