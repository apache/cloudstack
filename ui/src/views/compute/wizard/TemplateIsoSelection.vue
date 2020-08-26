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
      @search="handleSearch">
    </a-input-search>
    <a-spin :spinning="loading">
      <a-tabs
        :animated="false"
        :defaultActiveKey="filterOpts[0].id"
        v-model="filterType"
        tabPosition="top"
        @change="changeFilterType">
        <a-tab-pane
          v-for="filterItem in filterOpts"
          :key="filterItem.id"
          :tab="$t(filterItem.name)">
          <TemplateIsoRadioGroup
            v-if="filterType===filterItem.id"
            :osList="items[filterItem.id][inputDecorator.slice(0, -2)] || []"
            :itemCount="items[filterItem.id].count || 0"
            :input-decorator="inputDecorator"
            :selected="checkedValue"
            :preFillContent="preFillContent"
            @emit-update-template-iso="updateTemplateIso"
            @handle-search-filter="($event) => eventPagination($event)"
          ></TemplateIsoRadioGroup>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<script>
import TemplateIsoRadioGroup from '@views/compute/wizard/TemplateIsoRadioGroup'

export default {
  name: 'TemplateIsoSelection',
  components: { TemplateIsoRadioGroup },
  props: {
    items: {
      type: Object,
      default: () => {}
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
      checkedValue: '',
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
      filterType: 'featured',
      pagination: false
    }
  },
  watch: {
    items (items) {
      const key = this.inputDecorator.slice(0, -2)
      for (const filter of this.filterOpts) {
        if (items[filter.id] && items[filter.id][key] && items[filter.id][key].length > 0) {
          if (!this.pagination) {
            this.filterType = filter.id
            this.checkedValue = items[filter.id][key][0].id
          }
          break
        }
      }
    },
    inputDecorator (newValue, oldValue) {
      if (newValue !== oldValue) {
        this.filter = ''
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    updateTemplateIso (name, id) {
      this.checkedValue = id
      this.$emit('update-template-iso', name, id)
    },
    handleSearch (value) {
      if (!this.filter && !value) {
        return
      }
      this.pagination = false
      this.filter = value
      const options = {
        page: 1,
        pageSize: 10,
        keyword: this.filter
      }
      this.emitSearchFilter(options)
    },
    eventPagination (options) {
      this.pagination = true
      this.emitSearchFilter(options)
    },
    emitSearchFilter (options) {
      options.category = this.filterType
      this.$emit('handle-search-filter', options)
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
