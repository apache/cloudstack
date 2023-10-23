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

    <a-modal
      :title="$t('message.data.migration')"
      :visible="showMigrateModal"
      :maskClosable="true"
      :confirmLoading="migrateModalLoading"
      @cancel="showMigrateModal = false"
      :footer="null"
      width="50%"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')">
      <div>
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
        @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key == 'name'">
            <template v-if="record.isdirectory">
              <a @click="openDir(`${this.browserPath}${record.name}/`)">
                <folder-outlined /> {{ record.name }}
              </a>
            </template>
            <template v-else-if="resourceType === 'ImageStore'">
              <a @click="downloadFile(record)">
                <template v-if="record.snapshotid">
                  <build-outlined/>
                </template>
                <template v-else-if="record.volumeid">
                    <hdd-outlined/>
                </template>
                <template v-else-if="record.templateid">
                    <usb-outlined v-if="record.format === 'ISO'"/>
                    <save-outlined v-else />
                </template>
                {{ record.name }}
              </a>
            </template>
            <template v-else>
              <template v-if="record.snapshotid">
                  <build-outlined/>
                </template>
                <template v-else-if="record.volumeid">
                    <hdd-outlined/>
                </template>
                <template v-else-if="record.templateid">
                    <usb-outlined v-if="record.format === 'ISO'"/>
                    <save-outlined v-else />
                </template>
                {{ record.name }}
            </template>
          </template>
          <template v-if="column.key == 'size'">
            <template v-if="!record.isdirectory">
            {{ convertBytes(record.size) }}
            </template>
          </template>
          <template v-if="column.key == 'lastupdated'">
            {{ $toLocaleDate(record.lastupdated) }}
          </template>
          <template v-if="column.key == 'associatedResource'">
            <template v-if="record.snapshotid">
              <router-link :to="{ path: '/snapshot/' + record.snapshotid }" target='_blank' >
                {{ $t('label.snapshot') }}
              </router-link>
            </template>
            <template v-else-if="record.volumeid">
              <router-link :to="{ path: '/volume/' + record.volumeid }" target='_blank' >
                {{ $t('label.volume') }}
              </router-link>
            </template>
            <template v-else-if="record.templateid">
              <router-link v-if="record.format === 'ISO'" :to="{ path: '/iso/' + record.templateid }" target='_blank' >
                {{ $t('label.iso') }}
              </router-link>
              <router-link v-else :to="{ path: '/template/' + record.templateid }" target='_blank'>
                {{ $t('label.templatename') }}
              </router-link>
            </template>
            <template v-else>
              {{ $t('label.unknown') }}
            </template>
          </template>
          <template v-else-if="column.key === 'actions' && (record.templateid || record.snapshotid)">
              <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.migrate.data.from.image.store')"
              icon="arrows-alt-outlined"
              :copyResource="String(resource.id)"
              @onClick="openMigrationModal(record)" />
            </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script>
import { api } from '@/api'
import InfoCard from '@/components/view/InfoCard'
import TooltipButton from '@/components/widgets/TooltipButton'
import MigrateImageStoreResource from '@/views/storage/MigrateImageStoreResource'

export default {
  name: 'StorageBrowser',
  components: {
    InfoCard,
    MigrateImageStoreResource,
    TooltipButton
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
    var columns = [
      {
        key: 'name',
        title: this.$t('label.name')
      },
      {
        key: 'size',
        title: this.$t('label.size')
      },
      {
        key: 'lastupdated',
        title: this.$t('label.last.updated')
      },
      {
        key: 'associatedResource',
        title: this.$t('label.associated.resource')
      }
    ]
    if (this.resourceType === 'ImageStore') {
      columns.push({
        key: 'actions',
        title: this.$t('label.actions')
      })
    }
    return {
      loading: false,
      dataSource: [],
      browserPath: this.$route.query.browserPath || '',
      page: parseInt(this.$route.query.browserPage) || 1,
      pageSize: parseInt(this.$route.query.browserPageSize) || 10,
      total: 0,
      columns: columns,
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
    openMigrationModal (record) {
      if (record.snapshotid) {
        this.snapshotIdsToMigrate.push(record.snapshotid)
      } else if (record.templateid) {
        this.templateIdsToMigrate.push(record.templateid)
      }
      this.showMigrateModal = true
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
      })
    },
    fetchData () {
      this.dataSource = []
      this.$router.replace(
        {
          path: this.$route.path,
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
    downloadFile (record) {
      this.loading = true
      const params = {
        id: this.resource.id,
        path: `${this.browserPath}${record.name}`
      }
      api('downloadImageStoreObject', params).then(response => {
        const jobId = response.downloadimagestoreobjectresponse.jobid
        this.$pollJob({
          jobId: jobId,
          successMethod: (result) => {
            const url = result.jobresult.downloadimagestoreobjectresponse.url
            const name = result.jobresult.downloadimagestoreobjectresponse.name
            var elem = window.document.createElement('a')
            elem.setAttribute('href', new URL(url))
            elem.setAttribute('download', name)
            elem.setAttribute('target', '_blank')
            document.body.appendChild(elem)
            elem.click()
            document.body.removeChild(elem)
            this.loading = false
          },
          errorMethod: () => {
            this.loading = false
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loading = false
          }
        })
      }).catch(error => {
        console.error(error)
        this.$message.error(error)
        this.loading = false
      })
    }
  }
}
</script>
