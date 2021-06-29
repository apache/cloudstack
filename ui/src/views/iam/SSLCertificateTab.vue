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
    <a-row :gutter="12">
      <a-col :md="24" :lg="24">
        <a-table
          size="small"
          :loading="loading"
          :columns="columns"
          :dataSource="dataSource"
          :rowKey="record => record.id"
          :pagination="false"
          v-if="!quickview"
        >
          <span slot="action" slot-scope="text, record" class="cert-button-action">
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.quickview')"
              type="primary"
              icon="eye"
              size="small"
              @click="onQuickView(record.id)" />
            <tooltip-button
              tooltipPlacement="top"
              :tooltip="$t('label.delete.sslcertificate')"
              :disabled="!('deleteSslCert' in $store.getters.apis)"
              type="danger"
              icon="delete"
              size="small"
              @click="onShowConfirm(record)" />
          </span>
        </a-table>

        <a-list size="small" :dataSource="detailColumn" v-if="quickview">
          <div class="close-quickview">
            <a-button @click="() => { this.quickview = false }">{{ $t('label.close') }}</a-button>
          </div>
          <a-list-item slot="renderItem" slot-scope="item" v-if="item in detail">
            <div>
              <strong>{{ $t(item) }}</strong>
              <br/>
              <div class="list-item-content">
                {{ detail[item] }}
              </div>
            </div>
          </a-list-item>
        </a-list>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'SSLCertificate',
  components: {
    TooltipButton
  },
  data () {
    return {
      columns: [],
      dataSource: [],
      selectedRowKeys: [],
      detailColumn: [],
      detail: [],
      page: 1,
      pageSize: 10,
      quickview: false,
      loading: false
    }
  },
  props: {
    resource: {
      type: Object,
      default () {
        return {}
      }
    },
    tab: {
      type: String,
      default () {
        return ''
      }
    }
  },
  watch: {
    tab (newValue, oldValue) {
      if (newValue === 'certificate') {
        this.quickview = false
        this.fetchData()
      }
    },
    resource (newValue, oldValue) {
      if (Object.keys(newValue).length > 0 &&
        newValue.id &&
        this.tab === 'certificate'
      ) {
        this.quickview = false
        this.fetchData()
      }
    }
  },
  created () {
    this.columns = [
      {
        title: this.$t('label.name'),
        dataIndex: 'name',
        scopedSlots: { customRender: 'name' }
      },
      {
        title: this.$t('label.certificateid'),
        dataIndex: 'id',
        width: 450,
        scopedSlots: { customRender: 'id' }
      },
      {
        title: this.$t('label.action'),
        dataIndex: 'action',
        fixed: 'right',
        width: 80,
        scopedSlots: { customRender: 'action' }
      }
    ]
    this.detailColumn = ['name', 'certificate', 'certchain']
    this.fetchData()
  },
  methods: {
    fetchData () {
      const params = {}
      params.listAll = true
      params.page = this.page
      params.pageSize = this.pageSize
      params.accountid = this.resource.id

      this.loading = true

      api('listSslCerts', params).then(json => {
        const listSslResponse = json.listsslcertsresponse.sslcert

        // check exists json response
        if (!listSslResponse || Object.keys(listSslResponse).length === 0) {
          this.dataSource = []
          return
        }

        this.dataSource = listSslResponse
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    onQuickView (id) {
      this.loading = true
      const detail = this.dataSource.filter(item => item.id === id)
      this.detail = detail[0]
      this.quickview = true
      this.loading = false
    },
    onDelete (row) {
      const params = {}
      params.id = row.id

      // show loading
      const message = `${this.$t('label.delete.certificate')} ${this.$t('label.in.progress.for')} ${row.name}`
      const loading = this.$message.loading(message, 0)

      api('deleteSslCert', params).then(json => {
        const jsonResponse = json.deletesslcertresponse

        // hide loading
        setTimeout(loading)

        if (jsonResponse.success) {
          this.$message.success(this.$t('message.success.delete'), 3)
          this.fetchData()
        } else {
          this.$message.error(this.$t('message.delete.failed'), 3)
        }
      }).catch(error => {
        // hide loading
        setTimeout(loading)

        // show error
        this.$notifyError(error)
      })
    },
    onShowConfirm (row) {
      const self = this
      const title = `${this.$t('label.deleteconfirm')} ${this.$t('label.certificate')}`

      this.$confirm({
        title: title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.onDelete(row)
        }
      })
    }
  }
}
</script>

<style scoped>
/deep/.ant-table-fixed-right {
  z-index: 5;
}

.cert-button-action button {
  margin-right: 5px;
}

.list-item-content {
  word-break: break-word;
}

.close-quickview {
  text-align: right;
  margin-top: 12px;
  line-height: 32px;
  height: 32px;
}
</style>
