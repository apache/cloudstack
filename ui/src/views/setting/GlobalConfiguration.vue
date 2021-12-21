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
  <a-spin :spinning="false" style="background-color: #fff;">
    <a-affix :offsetTop="78">
      <a-card class="breadcrumb-card" style="z-index: 10">
        <a-row>
          <a-col :span="device === 'mobile' ? 24 : 12" style="padding-left: 12px">
            <breadcrumb :resource="resource">
              <template #end>
                <a-button
                  :loading="loading"
                  style="margin-bottom: 5px"
                  shape="round"
                  size="small"
                  @onClick="fetchConfigurationData({ irefresh: true })">
                  <template #icon><reload-outlined /></template>
                  {{ $t('label.refresh') }}
                </a-button>
              </template>
            </breadcrumb>
          </a-col>
          <a-col
            :span="device === 'mobile' ? 24 : 12"
            :style="device === 'mobile' ? { float: 'right', 'margin-top': '12px', 'margin-bottom': '-6px', display: 'table' } : { float: 'right', display: 'table', 'margin-bottom': '-6px' }" >
            <slot name="action"></slot>
            <search-view
              :searchFilters="searchFilters"
              :searchParams="searchParams"
              :apiName="apiName"
              @search="onSearch"
              @change-filter="changeFilter"/>
          </a-col>
        </a-row>
      </a-card>
    </a-affix>
    <a-tabs tabPosition="left">
      <a-tab-pane
        v-for="(group) in groups"
        :key="group.name"
        :tab="group.name" >

        <a-tabs >
          <a-tab-pane
            v-for="(subgroup) in group.subgroup"
            :key="subgroup.name"
            :tab="subgroup.name" >
             <ConfigurationTab :config="config" :group="group.name" :subgroup="subgroup.name" :loading="loading" />
          </a-tab-pane>
        </a-tabs>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import ActionButton from '@/components/view/ActionButton'
import InfoCard from '@/components/view/InfoCard'
import QuickView from '@/components/view/QuickView'
import TooltipButton from '@/components/widgets/TooltipButton'
import SearchView from '@/components/view/SearchView'
import ConfigurationTab from './ConfigurationTab'

export default {
  name: 'GlobalConfiguration',
  components: {
    Breadcrumb,
    Console,
    OsLogo,
    Status,
    ActionButton,
    InfoCard,
    QuickView,
    TooltipButton,
    SearchView,
    ConfigurationTab
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    actions: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      groups: [{ id: 1, name: 'Access', subgroup: [{ name: 'Account' }, { name: 'Domain' }, { name: 'Project' }] }, { id: 2, name: 'Storage', subgroup: [{ name: 'Primary Storage' }, { name: 'Template' }, { name: 'Volume' }, { name: 'Snapshot' }] }, { id: 3, name: 'Compute', subgroup: [{ name: 'Kubernetes' }, { name: 'VirtualMachine' }] }, { id: 4, name: 'Others', subgroup: [{ name: 'Misc' }] }],
      config: [{ category: 'Advanced', group: 'Access', subgroup: 'Account', name: 'account.allow.expose.host.hostname', value: '10000', description: 'If set to true, it allows the hypervisor host name on which the VM is spawned on to be exposed to the VM', isdynamic: true, component: 'VirtualMachineManager', displaytext: 'Account allow expose host hostname', type: 'Number' },
        { category: 'Storage', group: 'Storage', subgroup: 'Template', name: 'storage.template.cleanup.enabled', value: 'true', description: 'Enable/disable template cleanup activity, only take effect when overall storage cleanup is enabled', isdynamic: false, component: 'StorageManager', displaytext: 'Storage template cleanup enabled', type: 'Boolean' }],
      configLoading: false,
      recordLoading: false,
      selectedRowKeys: [],
      dataView: true,
      searchView: true,
      searchFilters: [],
      searchParams: {},
      filter: '',
      apiName: 'listConfigurations'
    }
  },
  created () {
    this.fetchConfigurationGroups()
    this.fetchConfigurationData()
  },
  watch: {
  },
  methods: {
    fetchData (callback) {
      this.configLoading = true
      const params = {
        [this.scopeKey]: this.resource.id,
        listAll: true
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      api('listConfigurations', params).then(response => {
        this.config = response.listconfigurationsresponse.configuration
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.configLoading = false
        if (!callback) return
        callback()
      })
    },
    fetchConfigurationGroups () {
      this.configLoading = true
      const params = {
        pagesize: -1
      }
      api('listConfigurationGroups', params).then(response => {
        this.groups = response.listconfigurationgroupsresponse.configurationgroup
        console.log(this.groups)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.configLoading = false
      })
    },
    fetchConfigurationData () {
      this.configLoading = true
      const params = {
        listAll: true,
        pagesize: -1
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      api('listConfigurations', params).then(response => {
        this.config = response.listconfigurationsresponse.configuration
        console.log(this.config)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.configLoading = false
      })
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    onSearch (opts) {
    },
    changeFilter (filter) {
    }
  }
}

</script>

<style scoped>
/deep/ .ant-table-thead {
  background-color: #f9f9f9;
}

/deep/ .ant-table-small > .ant-table-content > .ant-table-body {
  margin: 0;
}

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>

<style scoped lang="scss">
  .shift-btns {
    display: flex;
  }
  .shift-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    font-size: 12px;

    &:not(:last-child) {
      margin-right: 5px;
    }

    &--rotated {
      font-size: 10px;
      transform: rotate(90deg);
    }

  }

  .alert-notification-threshold {
    background-color: rgba(255, 231, 175, 0.75);
    color: #e87900;
    padding: 10%;
  }

  .alert-disable-threshold {
    background-color: rgba(255, 190, 190, 0.75);
    color: #f50000;
    padding: 10%;
  }
</style>
