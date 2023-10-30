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
  <a-row :gutter="12">
    <a-col :md="24">
      <a-card class="breadcrumb-card">
        <a-col :span="12" style="display: inline-flex">
          <breadcrumb style="padding-top: 6px; padding-left: 8px" />
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="fetchConfigurationData()" >
            <template #icon><ReloadOutlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
        </a-col>
        <a-col :span="12" style="float: right">
          <a-input-search
          style="width: 25vw; float: right; margin-bottom: 10px; z-index: 8; display: flex"
          :placeholder="$t('label.search')"
          v-model:value="filter"
          @search="changePage()"
          v-focus="true" />
        </a-col>
      </a-card>
    </a-col>
    <a-col :span="24">
      <a-card style="margin-left: 10px;">
        <a-spin :spinning="configLoading">
          <a-tabs
            tabPosition="left"
            :animated="false"
            :activeKey="this.group || ''"
            @change="changeGroupTab" >
            <a-tab-pane
              key=''
              tab='All Settings' >
                <ConfigurationTable
                  :columns="columns"
                  :config="config"
                  :count="count"
                  :page="page"
                  :pagesize="pagesize"
                  @change-page="changePage" />
            </a-tab-pane>
            <a-tab-pane
              v-for="(group) in groups"
              :key="group.name"
              :tab="group.name" >
              <a-tabs
                :activeKey="this.subgroup || ''"
                :animated="false"
                @change="changeSubgroupTab" >
                <a-tab-pane
                  v-for="(subgroup) in group.subgroup"
                  :key="subgroup.name"
                  :tab="subgroup.name" >
                  <ConfigurationHierarchy
                    :columns="columns"
                    :config="config" />
                </a-tab-pane>
              </a-tabs>
            </a-tab-pane>
          </a-tabs>
        </a-spin>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'
import { mixin, mixinDevice } from '@/utils/mixin.js'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import ActionButton from '@/components/view/ActionButton'
import InfoCard from '@/components/view/InfoCard'
import QuickView from '@/components/view/QuickView'
import TooltipButton from '@/components/widgets/TooltipButton'
import ConfigurationHierarchy from './ConfigurationHierarchy'
import ConfigurationTable from './ConfigurationTable'

export default {
  name: 'ConfigurationTab',
  components: {
    Breadcrumb,
    OsLogo,
    Status,
    ActionButton,
    InfoCard,
    QuickView,
    TooltipButton,
    ConfigurationHierarchy,
    ConfigurationTable
  },
  mixins: [mixin, mixinDevice],
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
      groups: [],
      config: [],
      configLoading: true,
      page: 1,
      pagesize: this.$store.getters.defaultListViewPageSize,
      group: '',
      subgroup: '',
      filter: '',
      count: 0,
      apiName: 'listConfigurations',
      columns: [
        {
          title: 'name',
          dataIndex: 'name',
          key: 'name'
        },
        {
          title: 'value',
          dataIndex: 'value',
          key: 'value',
          width: '29%'
        }
      ]
    }
  },
  watch: {
    '$route.fullPath': function () {
      this.group = this.$route.query.group || ''
      this.subgroup = this.$route.query.subgroup || ''
      this.page = parseInt(this.$route.query.page) || 1
      this.pagesize = parseInt(this.$route.query.pagesize) || this.pagesize
      this.filter = this.$route.query.filter || ''
    },
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchConfigurationData()
      }
    }
  },
  created () {
    this.fetchConfigurationGroups()
  },
  methods: {
    fetchConfigurationGroups () {
      this.configLoading = true
      const params = {
        pagesize: -1
      }
      api('listConfigurationGroups', params).then(response => {
        this.groups = response.listconfigurationgroupsresponse.configurationgroup
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.group = this.$route.query.group || ''
        this.subgroup = this.$route.query.subgroup || ''
        this.fetchConfigurationData()
      })
    },
    fetchConfigurationData () {
      this.configLoading = true
      const params = {
        listAll: true
      }
      if (this.group.length > 0) {
        params.group = this.group
        params.pagesize = -1
      } else {
        params.pagesize = this.pagesize || 20
        params.page = this.page || 1
      }
      if (this.subgroup.length > 0) {
        params.subgroup = this.subgroup
      }
      if (this.filter) {
        params.keyword = this.filter
      }

      api('listConfigurations', params).then(response => {
        this.config = []
        let config = response.listconfigurationsresponse.configuration || []
        this.count = response.listconfigurationsresponse.count || 0
        if (this.group.length > 0) {
          config = this.convertConfigToHierarchy(config)
        }
        this.config = config
        window.scrollTo(0, 0)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.configLoading = false
      })
    },
    convertConfigToHierarchy (config) {
      var hierarchy = {}
      for (var c of config) {
        if (c.parent && c.parent.length !== 0) {
          if (hierarchy[c.parent]) {
            hierarchy[c.parent].push(c)
          } else {
            hierarchy[c.parent] = [c]
          }
        }
      }
      for (c of config) {
        if (hierarchy[c.name]) {
          c.children = hierarchy[c.name]
        }
      }
      config = config.filter(c => !c.parent)
      return config
    },
    changePage (page, pagesize) {
      const query = {}
      if (page) {
        query.page = page
        this.page = page
      } else {
        this.page = 1
      }
      if (pagesize) {
        query.pagesize = pagesize
        this.pagesize = pagesize
      }
      if (this.filter) {
        query.filter = this.filter
      }
      if (this.group !== '') {
        query.group = this.group
      }
      if (this.subgroup !== '') {
        query.subgroup = this.subgroup
      }
      this.pushToHistory(query)
      this.fetchConfigurationData()
    },
    pushToHistory (query) {
      history.pushState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    },
    changeGroupTab (e) {
      this.group = e
      if (this.group.length > 0) {
        for (const groupIndex in this.groups) {
          if (this.groups[groupIndex].name === this.group) {
            const group = this.groups[groupIndex]
            this.subgroup = group.subgroup[0].name
          }
        }
      } else {
        this.group = ''
        this.subgroup = ''
        this.changePage(1, this.pagesize)
      }
      if (this.group.length > 0 && this.subgroup.length > 0) {
        const query = Object.assign({}, this.$route.query)
        delete query.page
        delete query.pagesize
        query.group = this.group
        query.subgroup = this.subgroup
        query.filter = this.filter
        // this.pagesize = -1
        this.page = 0
        this.pushToHistory(query)
        this.fetchConfigurationData()
      }
    },
    changeSubgroupTab (e) {
      this.subgroup = e || this.subgroup
      if (this.group.length > 0 && this.subgroup.length > 0) {
        const query = Object.assign({}, this.$route.query)
        delete query.page
        delete query.pagesize
        query.group = this.group
        query.subgroup = this.subgroup
        // this.pagesize = -1
        this.page = 0
        this.pushToHistory(query)
        this.fetchConfigurationData()
      } else {
        history.pushState(
          {},
          null,
          '#' + this.$route.path
        )
      }
    }
  }
}

</script>

<style scoped lang="scss">
  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

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
