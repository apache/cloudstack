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
  <resource-layout>
    <template #left>
      <a-spin :spinning="loading">
        <a-card :bordered="false">
          <a-input-search
            size="default"
            :placeholder="$t('label.search')"
            v-model:value="searchQuery"
            @search="onSearch"
          >
            <template #prefix><search-outlined /></template>
          </a-input-search>
          <a-spin :spinning="loadingSearch">
            <a-tree
              showLine
              v-if="treeViewData.length > 0"
              class="list-tree-view"
              :treeData="treeViewData"
              :loadData="onLoadData"
              :expandAction="false"
              :showIcon="true"
              :selectedKeys="defaultSelected"
              :checkStrictly="true"
              @select="onSelect"
              @expand="onExpand"
              :expandedKeys="arrExpand">
              <template #icon="{data}">
                <block-outlined v-if="data.isLeaf" />
              </template>
            </a-tree>
          </a-spin>
        </a-card>
      </a-spin>
    </template>
    <template #right>
      <a-spin :spinning="detailLoading">
        <a-card
          class="spin-content"
          :bordered="true"
          style="width:100%">
          <a-tabs
            style="width: 100%"
            :animated="false"
            :defaultActiveKey="tabs[0].name"
            @change="onTabChange" >
            <template v-for="tab in tabs" :tab="$t('label.' + tab.name)" :key="tab.name">
              <a-tab-pane :tab="$t('label.' + tab.name)" :key="tab.name" v-if="checkShowTabDetail(tab)">
                <keep-alive>
                  <component
                    v-if="tab.resourceType"
                    :is="tab.component"
                    :resource="resource"
                    :resourceType="tab.resourceType"
                    :loading="loading"
                    :tab="tabActive"
                    :bordered="false" />
                  <component
                    v-else
                    :is="tab.component"
                    :resource="resource"
                    :items="items"
                    :tab="tabActive"
                    :loading="loading"
                    :bordered="false" />
                </keep-alive>
              </a-tab-pane>
            </template>
          </a-tabs>
        </a-card>
      </a-spin>
    </template>
  </resource-layout>
</template>

<script>
import store from '@/store'
import { api } from '@/api'
import DetailsTab from '@/components/view/DetailsTab'
import ResourceView from '@/components/view/ResourceView'
import ResourceLayout from '@/layouts/ResourceLayout'
import eventBus from '@/config/eventBus'

export default {
  name: 'TreeView',
  components: {
    ResourceLayout,
    ResourceView
  },
  props: {
    treeData: {
      type: Array,
      required: true
    },
    treeSelected: {
      type: Object,
      required: true
    },
    tabs: {
      type: Array,
      default () {
        return [{
          name: 'details',
          component: DetailsTab
        }]
      }
    },
    loadedKeys: {
      type: Array,
      default () {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    },
    treeStore: {
      type: Object,
      default () {
        return {}
      }
    },
    treeDeletedKey: {
      type: String,
      default: null
    }
  },
  provide: function () {
    return {
      parentFetchData: null,
      parentToggleLoading: null
    }
  },
  data () {
    return {
      detailLoading: false,
      loadingSearch: false,
      tabActive: 'details',
      selectedTreeKey: '',
      resource: {},
      defaultSelected: [],
      treeVerticalData: [],
      treeViewData: [],
      oldTreeViewData: [],
      apiList: '',
      apiChildren: '',
      apiDetail: '',
      metaName: '',
      page: 1,
      pageSize: 20,
      items: [],
      showSetting: false,
      oldSearchQuery: '',
      searchQuery: '',
      arrExpand: [],
      rootKey: ''
    }
  },
  created: function () {
    this.metaName = this.$route.meta.name
    this.apiList = this.$route.meta.permission[0] ? this.$route.meta.permission[0] : ''
    this.apiChildren = this.$route.meta.permission[1] ? this.$route.meta.permission[1] : ''
    eventBus.on('refresh-domain-icon', () => {
      this.getDetailResource(this.selectedTreeKey)
    })
  },
  watch: {
    loading () {
      this.detailLoading = this.loading
      this.treeViewData = []
      this.arrExpand = []
      if (!this.loading) {
        this.treeViewData = this.treeData.slice()
        this.treeVerticalData = this.treeData.slice()

        if (this.treeViewData.length > 0) {
          this.oldTreeViewData = this.treeViewData.slice()
          this.rootKey = this.treeViewData[0].key
        }
      }
    },
    treeSelected: {
      deep: true,
      handler () {
        if (Object.keys(this.treeSelected).length === 0) {
          return
        }

        if (Object.keys(this.resource).length > 0) {
          this.selectedTreeKey = this.resource.key
          this.$emit('change-resource', this.resource)

          // set default expand
          if (this.defaultSelected.length > 1) {
            const arrSelected = this.defaultSelected
            this.defaultSelected = []
            this.defaultSelected.push(arrSelected[0])
          }

          return
        }

        this.resource = this.treeSelected
        this.resource = this.createResourceData(this.resource)
        this.selectedTreeKey = this.treeSelected.key
        this.defaultSelected.push(this.selectedTreeKey)

        // set default expand
        if (this.defaultSelected.length > 1) {
          const arrSelected = this.defaultSelected
          this.defaultSelected = []
          this.defaultSelected.push(arrSelected[0])
        }
      }
    },
    treeVerticalData: {
      deep: true,
      handler () {
        if (!this.treeStore.isExpand) {
          return
        }
        if (this.treeStore.expands && this.treeStore.expands.length > 0) {
          for (const expandKey of this.treeStore.expands) {
            if (this.arrExpand.includes(expandKey)) {
              continue
            }
            const keyVisible = this.treeVerticalData.findIndex(item => item.key === expandKey)
            if (keyVisible > -1) this.arrExpand.push(expandKey)
          }
        }
      }
    }
  },
  methods: {
    onLoadData (treeNode) {
      if (this.searchQuery !== '' && treeNode.eventKey !== this.rootKey) {
        return new Promise(resolve => {
          resolve()
        })
      }

      const params = {
        listAll: true,
        id: treeNode.eventKey,
        showicon: true
      }

      return new Promise(resolve => {
        api(this.apiChildren, params).then(json => {
          const dataResponse = this.getResponseJsonData(json)
          const dataGenerate = this.generateTreeData(dataResponse)
          treeNode.dataRef.children = dataGenerate

          if (this.treeVerticalData.length === 0) {
            this.treeVerticalData = this.treeViewData
          }

          this.treeViewData = [...this.treeViewData]
          this.oldTreeViewData = this.treeViewData

          for (let i = 0; i < dataGenerate.length; i++) {
            const resource = this.treeVerticalData.filter(item => item.id === dataGenerate[i].id)

            if (!resource || resource.length === 0) {
              this.treeVerticalData.push(dataGenerate[i])
            } else {
              this.treeVerticalData.filter((item, index) => {
                if (item.id === dataGenerate[i].id) {
                  // replace all value of tree data
                  Object.keys(dataGenerate[i]).forEach((value, idx) => {
                    this.treeVerticalData[index][value] = dataGenerate[i][value]
                  })
                }
              })
            }
          }

          this.handleSelectResource(treeNode.eventKey)
          resolve()
        })
      })
    },
    async handleSelectResource (treeKey) {
      if (this.treeDeletedKey && this.treeDeletedKey === this.treeStore?.treeSelected?.key) {
        const treeSelectedKey = this.treeStore.treeSelected.parentdomainid
        if (treeSelectedKey === this.rootKey) {
          this.resource = this.treeVerticalData[0]

          this.selectedTreeKey = treeSelectedKey
          this.defaultSelected = [treeSelectedKey]
          this.$emit('change-resource', this.resource)
          await this.setTreeStore(false, false, this.resource)
          return
        }
        const resourceIndex = await this.treeVerticalData.findIndex(item => item.id === treeSelectedKey)
        if (resourceIndex > -1) {
          const resource = await this.getDetailResource(treeSelectedKey)
          this.resource = await this.createResourceData(resource)

          this.selectedTreeKey = treeSelectedKey
          this.defaultSelected = [treeSelectedKey]
          this.$emit('change-resource', this.resource)
          await this.setTreeStore(false, false, this.resource)
          return
        }
      }
      const treeSelectedKey = this.treeStore.treeSelected.key
      const resourceIndex = await this.treeVerticalData.findIndex(item => item.id === treeSelectedKey)
      if (resourceIndex > -1) {
        this.selectedTreeKey = treeSelectedKey
        this.defaultSelected = [treeSelectedKey]

        this.resource = this.treeStore.treeSelected
        this.$emit('change-resource', this.resource)
      }
    },
    async onSelect (selectedKeys, event) {
      if (!event.selected) {
        return
      }

      // check item tree selected, set selectedTreeKey
      if (selectedKeys && selectedKeys[0]) {
        this.selectedTreeKey = selectedKeys[0]
      }

      this.defaultSelected = []
      this.defaultSelected.push(this.selectedTreeKey)
      const resource = await this.getDetailResource(this.selectedTreeKey)
      this.resource = await this.createResourceData(resource)
      const index = this.treeVerticalData.findIndex(item => item.key === this.selectedTreeKey)
      this.treeVerticalData[index] = this.resource

      this.$emit('change-resource', this.resource)
      await this.setTreeStore(false, false, this.resource)
    },
    onExpand (treeExpand) {
      const treeStore = this.treeStore
      this.arrExpand = treeExpand
      treeStore.isExpand = true
      treeStore.expands = this.arrExpand
      treeStore.treeSelected = this.resource
      this.$emit('change-tree-store', treeStore)
    },
    onSearch (value) {
      if (this.searchQuery === '' && this.oldSearchQuery === '') {
        return
      }

      this.searchQuery = value
      this.newTreeData = this.treeViewData
      this.treeVerticalData = this.newTreeData

      // set parameter for the request
      const params = {}
      params.listall = true

      // Check the search query to set params and variables using reset data
      if (this.searchQuery !== '') {
        this.oldSearchQuery = this.searchQuery
        params.keyword = this.searchQuery
      } else if (this.metaName === 'domain') {
        this.oldSearchQuery = ''
        params.id = this.$store.getters.userInfo.domainid
      }

      this.arrExpand = []
      this.treeViewData = []
      this.loadingSearch = true
      this.$emit('change-tree-store', {})
      api(this.apiList, params).then(json => {
        const listDomains = this.getResponseJsonData(json)
        this.treeVerticalData = this.treeVerticalData.concat(listDomains)

        if (!listDomains || listDomains.length === 0) {
          return
        }

        if (listDomains[0].id === this.rootKey) {
          const rootDomain = this.generateTreeData(listDomains)
          this.treeViewData = rootDomain
          this.defaultSelected = []
          this.defaultSelected.push(this.treeViewData[0].key)
          this.resource = this.treeViewData[0]
          this.$emit('change-resource', this.resource)

          return
        }

        this.recursiveTreeData(listDomains)

        if (this.treeViewData && this.treeViewData[0]) {
          this.defaultSelected = []
          this.defaultSelected.push(this.treeViewData[0].key)
          this.resource = this.treeViewData[0]
          this.$emit('change-resource', this.resource)
        }

        // check treeViewData, set to expand first children
        if (this.treeViewData &&
            this.treeViewData[0] &&
            this.treeViewData[0].children &&
            this.treeViewData[0].children.length > 0
        ) {
          this.arrExpand.push(this.treeViewData[0].children[0].key)
        }
      }).finally(() => {
        this.loadingSearch = false
      })
    },
    onTabChange (key) {
      this.tabActive = key
    },
    setTreeStore (arrExpand, isExpand, resource) {
      const treeStore = this.treeStore
      if (arrExpand) treeStore.expands = arrExpand
      if (isExpand) treeStore.isExpand = true
      if (resource) treeStore.treeSelected = resource
      this.$emit('change-tree-store', treeStore)
    },
    getDetailResource (selectedKey) {
      return new Promise(resolve => {
        // set api name and parameter
        const apiName = this.$route.meta.permission[0]
        const params = {}

        // set id to parameter
        params.id = selectedKey
        params.listAll = true
        params.showicon = true
        params.page = 1
        params.pageSize = 1

        this.detailLoading = true
        api(apiName, params).then(json => {
          const jsonResponse = this.getResponseJsonData(json)
          resolve(jsonResponse[0])
        }).catch(() => {
          resolve()
        }).finally(() => {
          this.detailLoading = false
        })
      })
    },
    getResponseJsonData (json) {
      let responseName
      let objectName
      let hasJobId = false
      for (const key in json) {
        if (key.includes('response')) {
          for (const res in json[key]) {
            if (res === 'jobid') {
              hasJobId = true
              break
            }
          }
          responseName = key
          break
        }
      }

      for (const key in json[responseName]) {
        if (key === 'count') {
          continue
        }

        objectName = key
        break
      }
      if (hasJobId) {
        return {}
      }
      return json[responseName][objectName]
    },
    checkShowTabDetail (tab) {
      if ('show' in tab) {
        return tab.show(this.resource, this.$route, store.getters.userInfo)
      }
      // get permission from the route
      const permission = tab.permission ? tab.permission[0] : ''

      // check permission not exists
      if (!permission || permission === '') {
        return true
      }

      // Check the permissions to see the tab for a user
      if (!Object.prototype.hasOwnProperty.call(store.getters.apis, permission)) {
        return false
      }

      return true
    },
    generateTreeData (jsonData) {
      if (!jsonData || jsonData.length === 0) {
        return []
      }

      for (let i = 0; i < jsonData.length; i++) {
        jsonData[i] = this.createResourceData(jsonData[i])
      }

      return jsonData
    },
    createResourceData (resource) {
      if (!resource || Object.keys(resource) === 0) {
        return {}
      }

      Object.keys(resource).forEach((value, idx) => {
        if (resource[value] === 'Unlimited') {
          resource.value = '-1'
        }
      })
      resource.title = resource.name
      resource.key = resource.id
      if (resource?.icon) {
        resource.resourceIcon = resource.icon
        delete resource.icon
      }

      if (!resource.haschild) {
        resource.isLeaf = true
      }

      return resource
    },
    recursiveTreeData (treeData) {
      const maxLevel = Math.max.apply(Math, treeData.map((o) => { return o.level }))
      const items = treeData.filter(item => item.level <= maxLevel)
      this.treeViewData = this.getNestedChildren(items, 0, maxLevel)
      this.oldTreeViewData = this.treeViewData
    },
    getNestedChildren (dataItems, level, maxLevel, id) {
      if (level > maxLevel) {
        return
      }

      let items = []

      if (!id || id === '') {
        items = dataItems.filter(item => item.level === level)
      } else {
        items = dataItems.filter(item => {
          let parentKey = ''
          const arrKeys = Object.keys(item)
          for (let i = 0; i < arrKeys.length; i++) {
            if (arrKeys[i].indexOf('parent') > -1 && arrKeys[i].indexOf('id') > -1) {
              parentKey = arrKeys[i]
              break
            }
          }

          return parentKey ? item[parentKey] === id : item.level === level
        })
      }

      if (items.length === 0) {
        return this.getNestedChildren(dataItems, (level + 1), maxLevel)
      }

      for (let i = 0; i < items.length; i++) {
        items[i] = this.createResourceData(items[i])

        if (items[i].haschild) {
          items[i].children = this.getNestedChildren(dataItems, (level + 1), maxLevel, items[i].key)
        }
      }

      return items
    }
  }
}
</script>

<style lang="less" scoped>
:deep(.ant-tree).list-tree-view {
  overflow-y: hidden;
  margin-top: 10px;
}
:deep(.ant-tree).ant-tree-directory {
  li.ant-tree-treenode-selected {
    span.ant-tree-switcher {
      color: rgba(0, 0, 0, 0.65);
    }
    span.ant-tree-node-content-wrapper.ant-tree-node-selected > span {
      color: rgba(0, 0, 0, 0.65);
      background-color: #bae7ff;
    }
    span.ant-tree-node-content-wrapper::before {
      background: #ffffff;
    }
  }

  .ant-tree-child-tree {
    li.ant-tree-treenode-selected {
      span.ant-tree-switcher {
        color: rgba(0, 0, 0, 0.65);
      }
      span.ant-tree-node-content-wrapper::before {
        background: #ffffff;
      }
    }
  }
}

:deep(.ant-tree-show-line) .ant-tree-switcher.ant-tree-switcher-noop {
  display: none
}

:deep(.ant-tree-node-content-wrapper-open) > span:first-child,
:deep(.ant-tree-node-content-wrapper-close) > span:first-child {
  display: none;
}

:deep(.ant-tree-icon__customize) {
  padding-right: 5px;
  background: transparent;
}

:deep(.ant-tree) {
  .ant-tree-treenode {
    .ant-tree-node-content-wrapper {
      margin-left: 0;
      margin-bottom: 2px;

      &:before {
        border-left: 1px solid #d9d9d9;
        content: " ";
        height: 100%;
        height: calc(100% - 15px);
        left: 12px;
        margin: 22px 0 0;
        position: absolute;
        width: 1px;
        top: 0;
      }
    }

    &.ant-tree-treenode-switcher-close, &.ant-tree-treenode-switcher-open, &.ant-tree-treenode-leaf-last {
      .ant-tree-node-content-wrapper:before {
        display: none;
      }
    }
  }
}

:deep(.ant-tree-show-line) .ant-tree-indent-unit:before {
  bottom: -2px;
  top: -5px;
}
</style>
