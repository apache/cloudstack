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
  <a-modal
    :visible="showUploadModal"
    :closable="true"
    :destroyOnClose="true"
    :title="$t('label.upload')"
    :maskClosable="false"
    :cancelText="$t('label.cancel')"
    @cancel="() => showUploadModal = false"
    :okText="$t('label.upload')"
    @ok="uploadFiles()"
    centered
    >
    <a-upload-dragger
      :multiple="true"
      :v-model:file-list="uploadFileList"
      listType="picture"
      :beforeUpload="beforeUpload">
      <p class="ant-upload-drag-icon">
        <cloud-upload-outlined />
      </p>
      <p class="ant-upload-text">
        {{ $t('label.volume.volumefileupload.description') }}
      </p>
    </a-upload-dragger>
    <a-divider dashed/>
    <tooltip-label bold :title="$t('label.upload.path')" :tooltip="$t('label.upload.description')"/>
    <br/>
    <a-input
      v-model:value="uploadDirectory"
      :placeholder="$t('label.upload.description')"
      :loading="loading"
      enter-button/>
    <a-divider dashed/>
    <tooltip-label bold :title="$t('label.metadata')" :tooltip="$t('label.metadata.upload.description')"/>
    <KeyValuePairInput :pairs="uploadMetaData" @update-pairs="(pairs) => uploadMetaData = pairs" />
  </a-modal>

  <a-drawer
    :visible="showObjectDetails"
    :closable="true"
    :maskClosable="true"
    @close="() => showObjectDetails = false"
    :title="record.name"
    >
    <div>
      <a-row justify="space-between">
        <a-col>
          <tooltip-label :title="$t('label.name')" bold/>
        </a-col>
        <a-col>
          {{ record.name.split('/').pop() }}
        </a-col>
      </a-row>
      <a-row justify="space-between">
        <a-col>
          <tooltip-label :title="$t('label.size')" bold/>
        </a-col>
        <a-col>
          {{ convertBytes(record.size) }}
        </a-col>
      </a-row>
      <a-row justify="space-between">
        <a-col>
          <tooltip-label :title="$t('label.last.updated')" bold/>
        </a-col>
        <a-col>
          {{ $toLocaleDate(record.lastModified) }}
        </a-col>
      </a-row>
      <a-row justify="space-between">
        <a-col>
          <tooltip-label :title="$t('label.url')" :tooltip="$t('label.object.url.description')" bold/>
        </a-col>
        <a-col>
          <a :href="record.url">{{ $t('label.link') }}</a>
        </a-col>
      </a-row>
      <a-row justify="space-between">
        <a-col>
          <tooltip-label :title="$t('label.object.presigned.url')" :tooltip="$t('label.object.presigned.url.description')" bold />
        </a-col>
        <a-col>
          <a :href="record.presignedUrl">{{ $t('label.link') }}</a>
        </a-col>
      </a-row>
        <a-divider>
          <tooltip-label :title="$t('label.metadata')" :tooltip="$t('label.metadata.description')"/>
        </a-divider>
        <template
          v-for="(value,key) in record.metadata"
          :key="key"
          >
          <a-row justify="space-between">
            <a-col>
              <tooltip-label :title="key" bold />
            </a-col>
            <a-col>
              {{ value }}
            </a-col>
          </a-row>
        </template>
    </div>
  </a-drawer>

  <div>
    <a-card class="breadcrumb-card">
      <a-row>
        <a-breadcrumb :routes="getRoutes()">
          <template #itemRender="{ route }">
            <span v-if="[''].includes(route.path) && route.breadcrumbName === 'root'">
              <a @click="openDir('')">
                <HomeOutlined/>
              </a>
            </span>
            <span v-else>
              <a @click="openDir(route.path)">
              {{ route.breadcrumbName }}
              </a>
            </span>
          </template>
        </a-breadcrumb>
      </a-row>
      <a-divider/>
      <a-row :gutter="[10,10]" :wrap="true">
        <a-col flex="75%">
          <a-input-search
            allowClear
            size="medium"
            v-model:value="searchPrefix"
            :placeholder="$t('label.objectstore.search')"
            :loading="loading"
            @search="listObjects()"
            :enter-button="$t('label.search')"/>
        </a-col>
        <a-col flex="auto">
          <a-button
            :loading="loading"
            style="margin-bottom: 5px"
            shape="round"
            size="medium"
            @click="listObjects()">
            <reload-outlined />
            {{ $t('label.refresh') }}
          </a-button>
        </a-col>
        <a-col flex="auto">
          <a-button
            :loading="loading"
            style="margin-bottom: 5px"
            shape="round"
            size="medium"
            type="primary"
            @click="() => showUploadModal = true">
            <upload-outlined />
            {{ $t('label.upload') }}
          </a-button>
        </a-col>
        <a-col flex="auto">
          <tooltip-button
            type="primary"
            size="medium"
            icon="delete-outlined"
            :tooltip="$t('label.delete')"
            v-if="selectedRows.length > 0"
            :danger="true"
            @onClick="removeObjects()"/>
        </a-col>
      </a-row>
    </a-card>

    <div>
      <a-table
        :columns="columns"
        :scroll="{ y: 300 }"
        :row-key="record => record"
        :data-source="records"
        :loading="loading"
        :pagination="{ current: page, pageSize: 1000, total: total, showSizeChanger: false }"
        :row-selection="{ selectedRowsKeys: selectedRows, onChange: onSelectChange }"
        @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key == 'name'">
            <template v-if="record.name === undefined && record.prefix">
              <a @click="openDir(record.prefix)">
                <folder-outlined /> {{ record.prefix.replace(this.browserPath, '').replace('/', '') }}
              </a>
            </template>
            <template v-else>
              <a @click="showObjectDescription(record)">
                {{ record.name.split('/').pop() }}
              </a>
            </template>
          </template>
          <template v-else-if="column.key == 'size'">
            <template v-if="record.name !== undefined && !record.prefix">
              {{ convertBytes(record.size) }}
            </template>
          </template>
          <template v-else-if="column.key == 'lastModified' && record.lastModified">
            {{ $toLocaleDate(record.lastModified) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script>
import { reactive } from 'vue'
import * as Minio from 'minio'
import { genericCompare } from '@/utils/sort.js'
import InfoCard from '@/components/view/InfoCard'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import KeyValuePairInput from '@/components/KeyValuePairInput'

export default {
  name: 'ObjectStoreBrowser',
  components: {
    InfoCard,
    TooltipButton,
    TooltipLabel,
    KeyValuePairInput
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
        title: this.$t('label.name'),
        sorter: (a, b) => genericCompare(a?.name || '', b?.name || '')
      },
      {
        key: 'size',
        title: this.$t('label.size'),
        sorter: (a, b) => genericCompare(a?.size || '', b?.size || '')
      },
      {
        key: 'lastModified',
        title: this.$t('label.last.updated'),
        sorter: (a, b) => genericCompare(a?.lastModified || '', b?.lastModified || '')
      }
    ]
    return {
      client: null,
      loading: false,
      records: [],
      browserPath: this.$route.query.browserPath || '',
      page: 1,
      pageStartAfterMap: { 1: '' },
      total: 0,
      columns: columns,
      selectedRows: [],
      searchPrefix: '',
      showUploadModal: false,
      uploadFileList: reactive([]),
      uploadDirectory: this.$route.query.browserPath || '',
      uploadMetaData: {},
      record: {},
      showObjectDetails: false,
      fetching: false
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    handleTableChange (pagination, filters, sorter) {
      if (this.page !== pagination.current) {
        this.page = pagination.current
        this.fetchData()
      }
    },
    fetchData () {
      this.loading = true
      this.records = []
      this.$router.replace(
        {
          query: {
            ...this.$route.query,
            browserPath: this.browserPath
          }
        }
      )
      if (!this.client) {
        this.initMinioClient()
      } else {
        this.listObjects()
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
      if (name === '/') {
        name = ''
      }
      this.browserPath = name
      this.uploadDirectory = name
      this.page = 1
      this.fetchData()
    },
    listObjects () {
      while (this.fetching) {
        // sleep for 500ms
        setTimeout(() => {
          console.log('waiting for previous request to complete')
        }, 500)
      }
      this.fetching = true
      this.records = []
      var stream = this.client.extensions.listObjectsV2WithMetadata(this.resource.name, this.browserPath + this.searchPrefix, false, this.pageStartAfterMap[this.page])
      stream.on('data', obj => {
        this.records.push(obj)
        if (this.records.length >= 1000) {
          stream.destroy()
        }
      })
      stream.on('end', obj => {
        var total = 0
        if (this.records.length > 0) {
          if (this.records.length >= 1000) {
            total = (this.page + 1) * 1000
            if (total > this.total) {
              this.total = total
            }
          } else {
            total = (this.page - 1) * 1000 + this.records.length
          }
          this.pageStartAfterMap[this.page + 1] = this.records[this.records.length - 1].name
        }
        if (total > this.total) {
          this.total = total
        }
        this.loading = false
        this.fetching = false
      })
      stream.on('error', err => {
        console.log(err)
        this.loading = false
        this.fetching = false
      })
    },
    removeObjects () {
      this.loading = true
      this.page = 1
      this.pageStartAfterMap = { 1: '' }
      const objectsToDelete = this.selectedRows.filter((row) => row.name).map((row) => row.name)
      const directoriesToDelete = this.selectedRows.filter((row) => row.prefix).map((row) => row.prefix)
      this.selectedRows = []
      this.removeDirectories(directoriesToDelete)
      if (objectsToDelete.length > 0) {
        this.client.removeObjects(this.resource.name, objectsToDelete, err => {
          if (err) {
            return this.$notification.error({
              message: this.$t('error.execute.api.failed'),
              description: err.message
            })
          }
          this.$notification.success({
            message: this.$t('label.delete'),
            description: this.$t('message.success.remove.objectstore.objects') + ' ' + objectsToDelete.length
          })
          this.listObjects()
        })
      }
    },
    removeDirectories (directoriesToDelete) {
      for (const directory of directoriesToDelete) {
        var objectsList = []
        const stream = this.client.listObjectsV2(this.resource.name, directory, true, '')
        stream.on('data', (obj) => {
          objectsList.push(obj.name)
        })

        stream.on('error', (err) => {
          console.log(err)
        })
        stream.on('end', (err) => {
          if (err) {
            return console.log(err)
          }
          this.client.removeObjects(this.resource.name, objectsList, err => {
            if (err) {
              return this.$notification.error({
                message: this.$t('error.execute.api.failed'),
                description: err.message
              })
            }
            this.$notification.success({
              message: this.$t('label.delete'),
              description: this.$t('message.success.remove.objectstore.directory') + ' ' + directory
            })
            console.log('Removed the objects successfully')
            this.listObjects()
          })
        })
      }
    },
    initMinioClient () {
      if (!this.client) {
        const url = /https?:\/\/([^/]+)\/?/.exec(this.resource.url.split(this.resource.name)[0])[1]
        const isHttps = /^https/.test(this.resource.url)
        this.client = new Minio.Client({
          endPoint: url.split(':')[0],
          port: url.split(':').length > 1 ? parseInt(url.split(':')[1]) : isHttps ? 443 : 80,
          useSSL: isHttps,
          accessKey: this.resource.accesskey,
          secretKey: this.resource.usersecretkey
        })
        this.listObjects()
      }
    },
    onSelectChange (selectedRow) {
      this.selectedRows = selectedRow
    },
    beforeUpload (file) {
      this.uploadFileList.push(file)
      return false
    },
    uploadFiles () {
      if (!this.uploadDirectory.endsWith('/')) {
        this.uploadDirectory = this.uploadDirectory + '/'
      }
      var promises = []
      while (this.uploadFileList.length > 0) {
        const file = this.uploadFileList.pop()
        const objectName = this.uploadDirectory + file.name
        promises.push(this.asyncUploadFile(file, objectName))
      }
      Promise.allSettled(promises).then(() => {
        this.uploadDirectory = this.browserPath
        this.uploadMetaData = {}
        this.uploadFileList = []
        this.listObjects()
      })
      this.showUploadModal = false
    },
    asyncUploadFile (file, objectName) {
      return new Promise((resolve, reject) => {
        file.arrayBuffer().then((buffer) => {
          this.client.putObject(this.resource.name, objectName, Buffer.from(buffer), file.size, this.uploadMetaData, err => {
            if (err) {
              return reject(this.$notification.error({
                message: this.$t('message.upload.failed'),
                description: err.message
              }))
            }
            return resolve(this.$notification.success({
              message: this.$t('message.success.upload'),
              description: objectName.split('/').pop()
            }))
          })
        })
      })
    },
    showObjectDescription (record) {
      this.record = { ...record }
      this.record.url = this.resource.url + '/' + record.name
      this.client.presignedGetObject(this.resource.name, record.name, 24 * 60 * 60, (err, presignedUrl) => {
        if (err) {
          return this.$notification.error({
            message: this.$t('error.execute.api.failed'),
            description: err.message
          })
        } else {
          this.record.presignedUrl = presignedUrl
        }
        this.showObjectDetails = true
      })
    },
    updateMetadata () {
      this.client.copyObject(
        new Minio.CopySourceOptions({ Bucket: this.resource.name, Object: this.record.name }),
        new Minio.CopyDestinationOptions({ Bucket: this.resource.name, Object: this.record.name, MetadataDirective: 'REPLACE', UserMetadata: this.record.metadata }),
        err => {
          if (err) {
            this.$notification.error({
              message: this.$t('error.execute.api.failed'),
              description: err.message
            })
          }
          this.$notification.success({
            message: this.$t('label.metadata'),
            description: this.$t('message.update.success')
          })
          this.listObjects()
        })
    }
  }
}
</script>
