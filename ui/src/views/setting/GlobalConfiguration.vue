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
  <a-spin :spinning="configLoading" style="background-color: #fff;">
    <a-affix :offsetTop="78">
      <a-card class="breadcrumb-card" style="z-index: 10">
        <a-row>
          <a-col :span="device === 'mobile' ? 24 : 12" style="padding-left: 12px">
            <breadcrumb>
              <template #end>
                <a-button
                  :loading="configLoading"
                  style="margin-bottom: 5px"
                  shape="round"
                  size="small"
                  @click="refreshConfigurationData()">
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
    <a-tabs
      tabPosition="left"
      :animated="false"
      @change="handleChangeConfigGroupTab" >
      <a-tab-pane
        key=''
        tab='All Settings' >
          <AllConfigurationsTab :loading="configLoading" />
      </a-tab-pane>
      <a-tab-pane
        v-for="(group) in groups"
        :key="group.name"
        :tab="group.name" >

        <a-tabs
          :animated="false"
          @change="handleChangeConfigSubGroupTab" >
          <a-tab-pane
            v-for="(subgroup) in group.subgroup"
            :key="subgroup.name"
            :tab="subgroup.name" >
             <ConfigurationTab :group="group.name" :subgroup="subgroup.name" :loading="configLoading" />
          </a-tab-pane>
        </a-tabs>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { mixin, mixinDevice } from '@/utils/mixin.js'
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
import AllConfigurationsTab from './AllConfigurationsTab'

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
    ConfigurationTab,
    AllConfigurationsTab
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
      configLoading: this.loading,
      configGroup: '',
      configSubGroup: '',
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
  },
  methods: {
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
      if (this.configGroup.length > 0) {
        params.group = this.configGroup
      }
      if (this.configSubGroup.length > 0) {
        params.subgroup = this.configSubGroup
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
    handleChangeConfigGroupTab (e) {
      this.configGroup = e
      if (this.configGroup.length > 0) {
        for (const groupIndex in this.groups) {
          if (this.groups[groupIndex].name === this.configGroup) {
            const group = this.groups[groupIndex]
            this.configSubGroup = group.subgroup[0].name
          }
        }
      } else {
        this.configSubGroup = ''
      }
      if (this.configGroup.length > 0 && this.configSubGroup.length > 0) {
        const query = Object.assign({}, this.$route.query)
        delete query.page
        delete query.pagesize
        query.group = this.configGroup
        query.subgroup = this.configSubGroup
        history.pushState(
          {},
          null,
          '#' + this.$route.path + '?' + Object.keys(query).map(key => {
            return (
              encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
            )
          }).join('&')
        )
      } else {
        history.pushState(
          {},
          null,
          '#' + this.$route.path
        )
      }
    },
    handleChangeConfigSubGroupTab (e) {
      this.configSubGroup = e
      if (this.configGroup.length > 0 && this.configSubGroup.length > 0) {
        const query = Object.assign({}, this.$route.query)
        delete query.page
        delete query.pagesize
        query.group = this.configGroup
        query.subgroup = this.configSubGroup
        history.pushState(
          {},
          null,
          '#' + this.$route.path + '?' + Object.keys(query).map(key => {
            return (
              encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
            )
          }).join('&')
        )
      } else {
        history.pushState(
          {},
          null,
          '#' + this.$route.path
        )
      }
    },
    refreshConfigurationData () {
      // this.fetchConfigurationGroups()
      // const query = Object.assign({}, this.$route.query)
      // this.$router.push({ query })
      // this.$router.push('/globalsetting')
      this.onSearch({})
    },
    onSearch (opts) {
      this.configLoading = true
      const query = Object.assign({}, this.$route.query)
      for (const key in this.searchParams) {
        delete query[key]
      }
      delete query.name
      delete query.q
      this.searchParams = {}
      if (opts && Object.keys(opts).length > 0) {
        this.searchParams = opts
        if ('searchQuery' in opts) {
          const value = opts.searchQuery
          if (value && value.length > 0) {
            query.name = value
            query.q = value
          }
          this.searchParams = {}
        } else {
          Object.assign(query, opts)
        }
      }
      if (this.configGroup.length > 0) {
        query.group = this.configGroup
      }
      if (this.configSubGroup.length > 0) {
        query.subgroup = this.configSubGroup
      }
      if (this.filter) {
        query.keyword = this.filter
      }
      this.$router.push({ query })
      this.configLoading = false
    },
    changeFilter (filter) {
      const query = Object.assign({}, this.$route.query)
      query.filter = filter
      this.$router.push({ query })
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
