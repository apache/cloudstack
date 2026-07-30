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
    <a-select
      v-model:value="selectedVolumeId"
      :placeholder="$t('label.select.volume')"
      :loading="volumesLoading"
      @change="onVolumeChange"
      style="margin-right: 6px; margin-bottom: 6px"
    >
      <a-select-option
        v-for="volume in volumes"
        :key="volume.uuid"
        :value="volume.uuid"
      >
        {{ volume.uuid }} ({{ $bytesToHumanReadableSize(volume.size) }})
      </a-select-option>
    </a-select>
    <a-select
      v-model:value="selectedFilesystem"
      :placeholder="$t('label.select.filesystem')"
      :loading="filesystemLoading"
      :disabled="filesystemLoading"
      @change="onFsChange"
    >
      <a-select-option
        v-for="fs in filesystems"
        :key="fs.name"
        :value="fs.name"
      >
        {{ fs.name }} ({{ fs.format }})
      </a-select-option>
    </a-select>
    <a-card class="breadcrumb-card">
      <a-row>
        <a-col :span="24" style="padding-left: 12px">
          <a-breadcrumb :routes="getRoutes()">
            <template #itemRender="{ route }">
              <span v-if="['/'].includes(route.path) && route.breadcrumbName === 'root'">
                <a @click="openDir('/', false, null)">
                  <home-outlined/>
                </a>
              </span>
              <span v-else>
                <a @click="openDir(route.path, route.isSymlink, null)">
                {{ route.breadcrumbName }}
                </a>
              </span>
            </template>
          </a-breadcrumb>
        </a-col>
      </a-row>
    </a-card>

    <div>
      <a-table
        :columns="columns"
        :row-key="record => record.name"
        :data-source="dataSource"
        :pagination="paginationConfig"
        @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key == 'name'">
            <template v-if="record.isdirectory">
              <a @click="openDir(`${this.browserPath}${record.name}/`, record.issymlink, record.canonicalpath)">
                <folder-outlined />
                {{ record.issymlink ? record.name + ' -> ' + record.canonicalpath : record.name }}
              </a>
            </template>
            <template v-else>
              <file-outlined/>
              {{ record.issymlink ? record.name + '->' + record.canonicalpath : record.name }}
            </template>
          </template>
          <template v-if="column.key == 'issymlink'">
            {{ record.issymlink }}
          </template>
          <template v-if="column.key == 'size'">
            <template v-if="!record.isdirectory">
              {{ $bytesToHumanReadableSize(record.size) }}
            </template>
          </template>
          <template v-if="column.key == 'lastupdated'">
            {{ $toLocaleDate(record.lastupdated) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.download')"
              icon="download-outlined"
              @onClick="downloadFile(record)" />
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script>
import { getAPI } from '@/api'
import InfoCard from '@/components/view/InfoCard'
import TooltipButton from '@/components/widgets/TooltipButton'
import { reactive } from 'vue'

export default {
  name: 'BackupFileBrowser',
  components: {
    InfoCard,
    TooltipButton
  },
  props: {
    resource: {
      type: Object,
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
        key: 'issymlink',
        title: this.$t('label.symlink')
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
        key: 'actions',
        title: this.$t('label.actions')
      }
    ]
    return {
      loading: false,
      dataSource: [],
      browserPath: '/',
      columns: columns,
      volumes: [],
      filesystems: [],
      volumesLoading: false,
      filesystemLoading: false,
      selectedVolumeId: null,
      selectedFilesystem: null,
      paginationConfig: reactive({
        current: 1,
        pageSize: 10,
        total: 0,
        showSizeChanger: true
      })
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.dataSource = []
      this.volumes = []
      this.filesystems = []
      this.fetchVolumes()
      this.fetchFileSystems()
    },
    handleTableChange (pag, filters, sorter) {
      this.paginationConfig.current = pag.current
      this.paginationConfig.pageSize = pag.pageSize
    },
    fetchVolumes () {
      this.volumesLoading = true
      for (const volume of JSON.parse(this.resource.volumes)) {
        this.volumes.push(volume)
      }
      this.selectedVolumeId = this.volumes[0].uuid
      this.volumesLoading = false
    },
    fetchFileSystems () {
      this.filesystemLoading = true
      getAPI('listBackupFilesystems', {
        backupid: this.resource.id,
        volumeid: this.selectedVolumeId
      }).then(json => {
        this.filesystems = json.listbackupfilesystemsresponse.datastoreobject ?? []
        this.filesystems.sort((a, b) => b.size - a.size)
        this.selectedFilesystem = this.filesystems[0]?.name
        this.browserPath = '/'
        this.onFsChange(this.selectedFilesystem)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.filesystemLoading = false
      })
    },
    fetchFiles (dir, isSymlink) {
      return getAPI('listBackupFiles', {
        backupid: this.resource.id,
        volumeid: this.selectedVolumeId,
        path: dir,
        filesystem: this.selectedFilesystem,
        issymlink: isSymlink ?? null
      }).then(json => {
        this.dataSource = json.listbackupfilesresponse.datastoreobject
        this.paginationConfig.total = json.listbackupfilesresponse.count
        this.paginationConfig.pageSize = 10
        this.paginationConfig.current = 1
      }).catch(error => {
        const split = this.browserPath.split('/')
        if (split.length > 2) {
          split.splice(split.length - 2, 1)
          this.browserPath = split.join('/')
        }

        this.$notifyError(error)
      })
    },
    getRoutes () {
      let path = '/'
      const routeList = [{
        path: path,
        breadcrumbName: 'root',
        isSymlink: false
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
    filterOption (input, option) {
      return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    onVolumeChange (volumeId) {
      const volume = this.volumes.find(v => v.uuid === volumeId)
      if (volume) {
        this.selectedVolumeId = volumeId
        this.dataSource = []
        this.fetchFileSystems()
      }
    },
    async onFsChange (filesystemName) {
      if (this.loading) {
        return
      }
      this.loading = true
      const filesystem = this.filesystems.find(fs => fs.name === filesystemName)
      if (filesystem) {
        this.selectedFilesystem = filesystemName
        await this.fetchFiles('/', false)
      }
      this.loading = false
    },
    async openDir (name, isSymlink, canonicalPath) {
      if (this.loading) {
        return
      }
      this.loading = true
      this.browserPath = canonicalPath ? canonicalPath + '/' : name
      await this.fetchFiles(name, isSymlink)
      this.loading = false
    },
    downloadFile (record) {
      this.loading = true
      const params = {
        backupid: this.resource.id,
        volumeid: this.selectedVolumeId,
        filesystem: this.selectedFilesystem,
        path: `${this.browserPath}${record.name}`
      }
      getAPI('downloadBackupFile', params).then(response => {
        const jobId = response.downloadbackupfileresponse.jobid
        this.$pollJob({
          jobId: jobId,
          successMethod: (result) => {
            const url = result.jobresult.downloadbackupfileresponse.url
            const name = result.jobresult.downloadbackupfileresponse.name
            var elem = window.document.createElement('a')
            elem.setAttribute('href', new URL(url))
            elem.setAttribute('download', name)
            elem.setAttribute('target', '_blank')
            document.body.appendChild(elem)
            elem.click()
            document.body.removeChild(elem)
            this.loading = false
          },
          errorMethod: (error) => {
            this.$notifyError(error)
            this.loading = false
          },
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loading = false
          },
          action: { isFetchData: false }
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
