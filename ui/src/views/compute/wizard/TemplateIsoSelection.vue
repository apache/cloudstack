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
  <a-tabs :defaultActiveKey="Object.keys(osTypes)[0]" v-if="view === TAB_VIEW">
    <a-button icon="search" slot="tabBarExtraContent" @click="() => toggleView(FILTER_VIEW)"/>
    <a-tab-pane v-for="(osList, osName) in osTypes" :key="osName">
      <span slot="tab">
        <os-logo :os-name="osName"></os-logo>
      </span>
      <TemplateIsoRadioGroup
        :osList="osList"
        :input-decorator="inputDecorator"
      ></TemplateIsoRadioGroup>
    </a-tab-pane>
  </a-tabs>
  <div v-else>
    <a-input class="search-input" v-model="filter">
      <a-icon slot="prefix" type="search"/>
      <a-icon slot="addonAfter" type="close" @click="toggleView(TAB_VIEW)"/>
    </a-input>
    <TemplateIsoRadioGroup
      :osList="filteredItems"
      :input-decorator="inputDecorator"
    ></TemplateIsoRadioGroup>
  </div>
</template>

<script>
import OsLogo from '@/components/widgets/OsLogo'
import { getNormalizedOsName } from '@/utils/icons'
import _ from 'lodash'
import TemplateIsoRadioGroup from '@views/compute/wizard/TemplateIsoRadioGroup'

export const TAB_VIEW = 1
export const FILTER_VIEW = 2

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
    }
  },
  data () {
    return {
      TAB_VIEW: TAB_VIEW,
      FILTER_VIEW: FILTER_VIEW,
      visible: false,
      filter: '',
      filteredItems: this.items,
      view: TAB_VIEW
    }
  },
  computed: {
    osTypes () {
      let mappedItems = {}
      this.items.forEach((os) => {
        const osName = getNormalizedOsName(os.ostypename)
        if (Array.isArray(mappedItems[osName])) {
          mappedItems[osName].push(os)
        } else {
          mappedItems[osName] = [os]
        }
      })
      mappedItems = _.mapValues(mappedItems, (list) => {
        let featuredItems = list.filter((item) => item.isfeatured)
        let nonFeaturedItems = list.filter((item) => !item.isfeatured)
        featuredItems = _.sortBy(featuredItems, (item) => item.displaytext.toLowerCase())
        nonFeaturedItems = _.sortBy(nonFeaturedItems, (item) => item.displaytext.toLowerCase())
        return featuredItems.concat(nonFeaturedItems) // pin featured isos/templates at the top
      })
      return mappedItems
    }
  },
  watch: {
    items (items) {
      this.filteredItems = items
    },
    filter (filterString) {
      if (filterString !== '') {
        this.filteredItems = this.filteredItems.filter((item) => item.displaytext.toLowerCase().includes(filterString))
      } else {
        this.filteredItems = this.items
      }
    }
  },
  methods: {
    toggleView (view) {
      this.view = view
    }
  }
}
</script>

<style lang="less" scoped>
  .search-input {
    margin: 0.5rem 0 1rem;
  }
</style>
