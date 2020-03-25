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
    <a-input-search
      class="search-input"
      placeholder="Search"
      v-model="filter"
      @search="filterDataSource"/>
    <a-spin :spinning="loading">
      <a-tabs
        tabPosition="top"
        :animated="false"
        :defaultActiveKey="Object.keys(dataSource)[0]">
        <a-tab-pane v-for="(osList, osName) in dataSource" :key="osName">
          <span slot="tab">
            <os-logo :os-name="osName"></os-logo>
          </span>
          <TemplateIsoRadioGroup
            :osType="osName"
            :osList="dataSource[osName]"
            :input-decorator="inputDecorator"
            :selected="checkedValue"
            :itemCount="itemCount[osName]"
            @handle-filter-tag="filterDataSource"
            @emit-update-template-iso="updateTemplateIso"
          ></TemplateIsoRadioGroup>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<script>
import OsLogo from '@/components/widgets/OsLogo'
import { getNormalizedOsName } from '@/utils/icons'
import _ from 'lodash'
import TemplateIsoRadioGroup from '@views/compute/wizard/TemplateIsoRadioGroup'
import store from '@/store'

export default {
  name: 'TemplateIsoSelection',
  components: { TemplateIsoRadioGroup, OsLogo },
  props: {
    items: {
      type: Array,
      default: () => []
    },
    inputDecorator: {
      type: String,
      default: ''
    },
    selected: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      filter: '',
      filteredItems: this.items,
      checkedValue: '',
      dataSource: {},
      itemCount: {}
    }
  },
  watch: {
    items (items) {
      this.filteredItems = []
      this.checkedValue = ''
      if (items && items.length > 0) {
        this.filteredItems = items
        this.checkedValue = items[0].id
      }
      this.dataSource = this.mappingDataSource()
    },
    inputDecorator (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.filter = ''
      }
    }
  },
  methods: {
    mappingDataSource () {
      let mappedItems = {}
      const itemCount = {}
      this.filteredItems.forEach((os) => {
        const osName = getNormalizedOsName(os.ostypename)
        if (Array.isArray(mappedItems[osName])) {
          mappedItems[osName].push(os)
          itemCount[osName] = itemCount[osName] + 1
        } else {
          mappedItems[osName] = [os]
          itemCount[osName] = 1
        }
      })
      mappedItems = _.mapValues(mappedItems, (list) => {
        let featuredItems = list.filter((item) => item.isfeatured)
        let nonFeaturedItems = list.filter((item) => !item.isfeatured)
        featuredItems = _.sortBy(featuredItems, (item) => item.displaytext.toLowerCase())
        nonFeaturedItems = _.sortBy(nonFeaturedItems, (item) => item.displaytext.toLowerCase())
        return featuredItems.concat(nonFeaturedItems) // pin featured isos/templates at the top
      })
      this.itemCount = itemCount
      return mappedItems
    },
    updateTemplateIso (name, id) {
      this.$emit('update-template-iso', name, id)
    },
    filterDataSource (strQuery) {
      if (strQuery !== '' && strQuery.includes('is:')) {
        this.filteredItems = []
        this.filter = strQuery
        const filters = strQuery.split(';')
        filters.forEach((filter) => {
          const query = filter.replace(/ /g, '')
          const data = this.filterDataSourceByTag(query)
          this.filteredItems = this.filteredItems.concat(data)
        })
      } else if (strQuery !== '') {
        this.filteredItems = this.items.filter((item) => item.displaytext.toLowerCase().includes(strQuery.toLowerCase()))
      } else {
        this.filteredItems = this.items
      }
      this.dataSource = this.mappingDataSource()
    },
    filterDataSourceByTag (tag) {
      let arrResult = []
      if (tag.includes('public')) {
        arrResult = this.items.filter((item) => {
          return item.ispublic && item.isfeatured
        })
      } else if (tag.includes('featured')) {
        arrResult = this.items.filter((item) => {
          return item.isfeatured
        })
      } else if (tag.includes('self')) {
        arrResult = this.items.filter((item) => {
          return !item.ispublic && (item.account === store.getters.userInfo.account)
        })
      } else if (tag.includes('shared')) {
        arrResult = this.items.filter((item) => {
          return !item.ispublic && (item.account !== store.getters.userInfo.account)
        })
      }

      return arrResult
    }
  }
}
</script>

<style lang="less" scoped>
  .search-input {
    width: 25vw;
    z-index: 8;
    position: absolute;
    top: 11px;
    right: 10px;

    @media (max-width: 600px) {
      position: relative;
      width: 100%;
      top: 0;
      right: 0;
    }
  }

  /deep/.ant-tabs-nav-scroll {
    min-height: 45px;
  }
</style>
