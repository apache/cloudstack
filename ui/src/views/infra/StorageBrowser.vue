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
    <a-drawer
      :visible="showDrawer"
      :closable="true"
      @close="onDrawerClose"
      size='small'
      placement='right'>
        <info-card :resource="drawerResource" :loading="drawerLoading" />
    </a-drawer>
    <a-card class="breadcrumb-card">
      <a-row>
        <a-col :span="24" style="padding-left: 12px">
          <a-breadcrumb :routes="getRoutes()">
            <template #itemRender="{ route }">
              <span v-if="['/', ''].includes(route.path) && route.breadcrumbName === 'root'">
                <a @click="openDir('')">
                  <home-outlined/>
                </a>
              </span>
              <span v-else>
                <a @click="openDir(route.path)">
                {{ route.breadcrumbName }}
                </a>
              </span>
            </template>
          </a-breadcrumb>
          </a-col>
          <a-col>
          <a-tooltip placement="bottom">
            <template #title>{{ $t('label.refresh') }}</template>
            <a-button
              style="margin-top: 4px"
              :loading="loading"
              shape="round"
              size="small"
              @click="fetchData()"
            >
            <template #icon><ReloadOutlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
          </a-tooltip>
        </a-col>
      </a-row>
    </a-card>

    <!-- <a-button shape="circle" @click="deleteSelected">
      <link-outlined/>
    </a-button>
 -->
    <a-button shape="circle" @click="openMigrationModal">
      <arrows-alt-outlined/>
    </a-button>

    <!-- <a-modal
      :visible="showDeleteModal"
      :confirmLoading="deleteModalLoading"
      @ok="deleteSelected"
      @cancel="showDeleteModal = false"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')">
      <span v-html="$t('message.delete.confirm', { name: selectedRowKeys.length })" />
    </a-modal> -->

    <a-modal
      :visible="showMigrateModal"
      :confirmLoading="migrateModalLoading"
      @close-action="showMigrateModal = false"
      :footer="null"
      width="50%"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')">
      <div>
        <span v-html="$t('message.migrate.confirm', { name: selectedRowKeys.length })" />
        <migrate-image-store-resource
          :sourceImageStore="resource"
          :templateIdsToMigrate="templateIdsToMigrate"
          :snapshotIdsToMigrate="snapshotIdsToMigrate"
          @close-action="showMigrateModal = false"
        />
    </div>
    </a-modal>

    <div>
      <a-table
        :columns="columns"
        :row-key="record => record.name"
        :data-source="dataSource"
        :pagination="{ current: page, pageSize: pageSize, total: total }"
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key == 'name'">
            <template v-if="record.isdirectory">
              <a @click="openDir(`${this.browserPath}${record.name}/`)">
                <folder-outlined /> {{ record.name }}
              </a>
            </template>
            <template v-else-if="record.snapshotid">
              <a @click="openDrawer(record)">
                <build-outlined/>
                {{ record.name }}
              </a>
            </template>
            <template v-else-if="record.volumeid">
              <a @click="openDrawer(record)">
                <hdd-outlined/>
                {{ record.name }}
              </a>
            </template>
            <template v-else-if="record.templateid">
              <a @click="openDrawer(record)">
                <usb-outlined v-if="record.format === 'ISO'"/>
                <save-outlined v-else />
                  {{ record.name }}
              </a>
            </template>
            <template v-else>
              {{ record.name }}
            </template>
          </template>
          <template v-if="column.key == 'size'">
            {{ convertBytes(record.size) }}
          </template>
          <template v-if="column.key == 'lastupdated'">
            {{ $toLocalDate(record.lastupdated) }}
          </template>
          <template v-if="column.key == 'related'">
            <template v-if="record.snapshotid">
              <router-link :to="{ path: '/snapshot/' + record.snapshotid }" target='_blank' >
                {{  $t('label.snapshot') }}
              </router-link>
            </template>
            <template v-if="record.volumeid">
              <router-link :to="{ path: '/volume/' + record.volumeid }" target='_blank' >
                {{  $t('label.volume') }}
              </router-link>
            </template>
            <template v-else-if="record.templateid">
              <router-link v-if="record.format === 'ISO'" :to="{ path: '/iso/' + record.templateid }" target='_blank' >
                {{  $t('label.iso') }}
              </router-link>
              <router-link v-else :to="{ path: '/template/' + record.templateid }" target='_blank'>
                {{  $t('label.templatename') }}
              </router-link>
            </template>
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script>
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import InfoCard from '@/components/view/InfoCard'
import MigrateImageStoreResource from '@/views/storage/MigrateImageStoreResource'

export default {
  name: 'StorageBrowser',
  components: {
    InfoCard,
    MigrateImageStoreResource
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      dataSource: [],
      browserPath: this.$route.query.browserPath || '',
      page: parseInt(this.$route.query.browserPage) || 1,
      pageSize: parseInt(this.$route.query.browserPageSize) || 10,
      selectedRowKeys: [],
      total: 0,
      columns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          sorter: function (a, b) { return genericCompare(a[this.key] || '', b[this.key] || '') }
        },
        {
          key: 'size',
          title: this.$t('label.size'),
          sorter: function (a, b) { return genericCompare(a[this.key] || '', b[this.key] || '') }
        },
        {
          key: 'lastupdated',
          title: this.$t('label.last.updated'),
          sorter: function (a, b) { return genericCompare(a[this.key] || '', b[this.key] || '') }
        },
        {
          key: 'related',
          title: this.$t('label.related'),
          sorter: function (a, b) { return genericCompare(a[this.key] || '', b[this.key] || '') }
        }
      ],
      showDrawer: false,
      drawerLoading: false,
      drawerResource: {},
      drawerShowMoreDetailsRoute: {},
      migrateModalLoading: false,
      showMigrateModal: false,
      templateIdsToMigrate: [],
      snapshotIdsToMigrate: []
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    openMigrationModal () {
      for (const record of this.dataSource) {
        if (this.selectedRowKeys.includes(record.name)) {
          if (record.snapshotid) {
            this.snapshotIdsToMigrate.push(record.snapshotid)
          } else if (record.templateid) {
            this.templateIdsToMigrate.push(record.templateid)
          }
        }
      }
      this.showMigrateModal = true
    },
    onSelectChange (changableRowKeys) {
      console.log('selectedRowKeys changed: ', changableRowKeys)
      this.selectedRowKeys = changableRowKeys
    },
    handleTableChange (pagination, filters, sorter) {
      this.page = pagination.current
      this.pageSize = pagination.pageSize
      this.fetchData()
    },
    fetchImageStoreObjects () {
      this.loading = true
      api('listImageStoreObjects', {
        path: this.browserPath,
        id: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.dataSource = json.listimagestoreobjectsresponse.datastoreobject
        this.total = json.listimagestoreobjectsresponse.count
      }).finally(() => {
        this.loading = false
        this.onSelectChange([])
      })
    },
    fetchPrimaryStoreObjects () {
      this.loading = true
      api('listStoragePoolObjects', {
        path: this.browserPath,
        id: this.resource.id,
        page: this.page,
        pagesize: this.pageSize
      }).then(json => {
        this.dataSource = json.liststoragepoolobjectsresponse.datastoreobject
        this.total = json.liststoragepoolobjectsresponse.count
      }).finally(() => {
        this.loading = false
        this.onSelectChange([])
      })
    },
    fetchData () {
      this.dataSource = []
      this.$router.replace(
        {
          query: {
            ...this.$route.query,
            browserPath: this.browserPath,
            browserPage: this.page,
            browserPageSize: this.browserPageSize
          }
        }
      )
      if (this.resourceType === 'ImageStore') {
        this.fetchImageStoreObjects()
      } else if (this.resourceType === 'PrimaryStorage') {
        this.fetchPrimaryStoreObjects()
      }
    },
    getRoutes () {
      let path = ''
      const routeList = [{
        path: path,
        breadcrumbName: 'root'
      }]
      for (const route of this.browserPath.split('/')) {
        if (route) {
          path = `${path}${route}/`
          routeList.push({
            path: path,
            breadcrumbName: route
          })
        }
      }
      return routeList
    },
    convertBytes (val) {
      if (val < 1024 * 1024) return `${(val / 1024).toFixed(2)} KB`
      if (val < 1024 * 1024 * 1024) return `${(val / 1024 / 1024).toFixed(2)} MB`
      if (val < 1024 * 1024 * 1024 * 1024) return `${(val / 1024 / 1024 / 1024).toFixed(2)} GB`
      if (val < 1024 * 1024 * 1024 * 1024 * 1024) return `${(val / 1024 / 1024 / 1024 / 1024).toFixed(2)} TB`
      return val
    },
    openDir (name) {
      this.browserPath = name
      this.page = 1
      this.pageSize = 10
      this.fetchData()
    },
    openDrawer (record) {
      if (record.snapshotid) {
        this.fetchSnapshot(record.snapshotid)
      } else if (record.templateid) {
        if (record.format === 'ISO') {
          this.fetchISO(record.templateid)
        } else {
          this.fetchTemplate(record.templateid)
        }
      }
    },
    fetchSnapshot (snapshotid) {
      this.showDrawer = true
      this.drawerLoading = true
      this.drawerShowMoreDetailsRoute = { path: '/snapshot/' + snapshotid }
      api('listSnapshots', {
        id: snapshotid,
        listall: true
      }).then(json => {
        this.drawerResource = json.listsnapshotsresponse.snapshot[0]
      }).finally(() => {
        this.drawerLoading = false
      })
      return null
    },
    fetchTemplate (templateid) {
      this.showDrawer = true
      this.drawerLoading = true
      this.drawerShowMoreDetailsRoute = { path: '/template/' + templateid }
      api('listTemplates', {
        id: templateid,
        listall: true,
        templatefilter: 'all'
      }).then(json => {
        this.drawerResource = json.listtemplatesresponse.template[0]
      }).finally(() => {
        this.drawerLoading = false
      })
      return null
    },
    fetchISO (templateid) {
      this.showDrawer = true
      this.drawerLoading = true
      this.drawerShowMoreDetailsRoute = { path: '/template/' + templateid }
      api('listIsos', {
        id: templateid,
        listall: true,
        isofilter: 'all'
      }).then(json => {
        this.drawerResource = json.listisosresponse.iso[0]
      }).finally(() => {
        this.drawerLoading = false
      })
      return null
    },
    onDrawerClose () {
      this.showDrawer = false
    }
  }
}
</script>
