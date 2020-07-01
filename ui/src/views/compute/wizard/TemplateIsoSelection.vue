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
      :placeholder="$t('label.search')"
      v-model="filter"
      @search="filterDataSource">
    </a-input-search>
    <a-spin :spinning="loading">
      <a-tabs
        :animated="false"
        :defaultActiveKey="Object.keys(dataSource)[0]"
        v-model="filterType"
        tabPosition="top"
        @change="changeFilterType">
        <a-tab-pane
          v-for="filterItem in filterOpts"
          :key="filterItem.id"
          :tab="$t(filterItem.name)">
          <TemplateIsoRadioGroup
            v-if="filterType===filterItem.id"
            :osList="dataSource[filterItem.id]"
            :itemCount="itemCount[filterItem.id]"
            :input-decorator="inputDecorator"
            :selected="checkedValue"
            :preFillContent="preFillContent"
            @handle-filter-tag="filterDataSource"
            @emit-update-template-iso="updateTemplateIso"
          ></TemplateIsoRadioGroup>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<script>
import { getNormalizedOsName } from '@/utils/icons'
import TemplateIsoRadioGroup from '@views/compute/wizard/TemplateIsoRadioGroup'
import store from '@/store'

export default {
  name: 'TemplateIsoSelection',
  components: { TemplateIsoRadioGroup },
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
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      filter: '',
      filteredItems: this.items,
      checkedValue: '',
      dataSource: {},
      itemCount: {},
      visibleFilter: false,
      filterOpts: [{
        id: 'featured',
        name: 'label.featured'
      }, {
        id: 'community',
        name: 'label.community'
      }, {
        id: 'selfexecutable',
        name: 'label.my.templates'
      }, {
        id: 'sharedexecutable',
        name: 'label.sharedexecutable'
      }],
      osType: '',
      filterType: '',
      oldInputDecorator: ''
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
      this.filterType = Object.keys(this.dataSource)[0]
    },
    inputDecorator (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.oldInputDecorator = this.inputDecorator
        this.filter = ''
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    mappingDataSource () {
      const mappedItems = {
        featured: [],
        community: [],
        selfexecutable: [],
        sharedexecutable: []
      }
      const itemCount = {
        featured: 0,
        community: 0,
        selfexecutable: 0,
        sharedexecutable: 0
      }
      this.filteredItems.forEach((os) => {
        os.osName = getNormalizedOsName(os.ostypename)
        if (os.isPublic && os.isfeatured) {
          mappedItems.community.push(os)
          itemCount.community = itemCount.community + 1
        } else if (os.isfeatured) {
          mappedItems.featured.push(os)
          itemCount.featured = itemCount.featured + 1
        } else {
          const isSelf = !os.ispublic && (os.account === store.getters.userInfo.account)
          if (isSelf) {
            mappedItems.selfexecutable.push(os)
            itemCount.selfexecutable = itemCount.selfexecutable + 1
          } else {
            mappedItems.sharedexecutable.push(os)
            itemCount.sharedexecutable = itemCount.sharedexecutable + 1
          }
        }
      })
      this.itemCount = itemCount
      return mappedItems
    },
    updateTemplateIso (name, id) {
      this.checkedValue = id
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
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const filtered = values.filter || []
        this.filter = ''
        filtered.map(item => {
          if (this.filter.length === 0) {
            this.filter += 'is:' + item
          } else {
            this.filter += '; is:' + item
          }
        })
        this.filterDataSource(this.filter)
      })
    },
    onClear () {
      const field = { filter: undefined }
      this.form.setFieldsValue(field)
      this.filter = ''
      this.filterDataSource('')
    },
    changeFilterType (value) {
      this.filterType = value
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
