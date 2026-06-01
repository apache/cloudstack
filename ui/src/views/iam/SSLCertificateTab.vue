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
      <a-spin :spinning="loading">
        <a-button
          shape="round"
          style="left: 10px; float: right;margin-bottom: 10px; z-index: 8"
          @click="() => { showUploadForm = true }">
          <template #icon><plus-outlined /></template>
          {{ $t('label.upload.ssl.certificate') }}
        </a-button>
      </a-spin>
    </a-row>
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
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actions'" class="cert-button-action">
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.quickview')"
                type="primary"
                icon="eye-outlined"
                size="small"
                @onClick="onQuickView(record.id)" />
              <tooltip-button
                tooltipPlacement="top"
                :tooltip="$t('label.delete.sslcertificate')"
                :disabled="!('deleteSslCert' in $store.getters.apis)"
                type="primary"
                :danger="true"
                icon="delete-outlined"
                size="small"
                @onClick="onShowConfirm(record)" />
            </template>
          </template>
        </a-table>

        <a-list size="small" :dataSource="detailColumn" v-if="quickview">
          <div class="close-quickview">
            <a-button @click="() => { quickview = false }">{{ $t('label.close') }}</a-button>
          </div>
          <template #renderItem="{item}">
            <a-list-item v-if="item in detail">
                <div>
                  <strong>{{ $t(item) }}</strong>
                  <br/>
                  <div class="list-item-content">
                    {{ detail[item] }}
                  </div>
                </div>
            </a-list-item>
          </template>
        </a-list>
      </a-col>
    </a-row>

    <a-modal
      v-if="showUploadForm"
      :visible="showUploadForm"
      :title="$t('label.upload.ssl.certificate')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showUploadForm = false }"
      centered
      width="30vw">

      <a-form
        layout="vertical"
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="uploadSslCert"
        v-ctrl-enter="uploadSslCert"
      >
        <a-form-item name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description" tooltipPlacement="bottom"/>
          </template>
          <a-input
            id="name"
            :placeholder="apiParams.name.description"
            name="name"
            v-model:value="form.name"
          ></a-input>
        </a-form-item>

        <a-form-item name="certificate" ref="certificate" :required="true">
          <template #label>
            <tooltip-label :title="$t('label.certificate')" :tooltip="apiParams.certificate.description" tooltipPlacement="bottom"/>
          </template>
          <a-textarea
            id="certificate"
            rows="2"
            :placeholder="apiParams.certificate.description"
            v-focus="true"
            name="certificate"
            v-model:value="form.certificate"
          ></a-textarea>
        </a-form-item>

        <a-form-item name="privatekey" ref="privatekey" :required="true">
          <template #label>
            <tooltip-label :title="$t('label.privatekey')" :tooltip="apiParams.privatekey.description" tooltipPlacement="bottom"/>
          </template>
          <a-textarea
            id="privatekey"
            rows="2"
            :placeholder="apiParams.privatekey.description"
            name="privatekey"
            v-model:value="form.privatekey"
          ></a-textarea>
        </a-form-item>

        <a-form-item name="certchain" ref="certchain">
          <template #label>
            <tooltip-label :title="$t('label.certificate.chain')" :tooltip="apiParams.certchain.description" tooltipPlacement="bottom"/>
          </template>
          <a-textarea
            id="certchain"
            rows="2"
            :placeholder="apiParams.certchain.description"
            name="certchain"
            v-model:value="form.certchain"
          ></a-textarea>
        </a-form-item>

        <a-form-item name="password" ref="password">
          <template #label>
            <tooltip-label :title="$t('label.password')" :tooltip="apiParams.password.description" tooltipPlacement="bottom"/>
          </template>
          <a-input
            type="password"
            id="password"
            name="password"
            v-model:value="form.password"
          ></a-input>
        </a-form-item>

        <a-form-item name="enabledrevocationcheck" ref="enabledrevocationcheck">
          <template #label>
            <tooltip-label :title="$t('label.enabled.revocation.check')" :tooltip="apiParams.enabledrevocationcheck.description" tooltipPlacement="bottom"/>
          </template>
          <a-checkbox v-model:checked="form.enabledrevocationcheck"></a-checkbox>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="showUploadForm = false" class="close-button">
            {{ $t('label.cancel' ) }}
          </a-button>
          <a-button type="primary" ref="submit" :loading="uploading" @click="uploadSslCert">
            {{ $t('label.submit' ) }}
          </a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'
import { ref, reactive, toRaw } from 'vue'

export default {
  name: 'SSLCertificate',
  components: {
    TooltipLabel,
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
      loading: false,
      uploading: false,
      showUploadForm: false
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
    resource: {
      deep: true,
      handler (newValue) {
        if (Object.keys(newValue).length > 0 &&
          newValue.id &&
          this.tab === 'certificate'
        ) {
          this.quickview = false
          this.fetchData()
        }
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('uploadSslCert')
  },
  created () {
    this.columns = [
      {
        key: 'name',
        title: this.$t('label.name'),
        dataIndex: 'name'
      },
      {
        key: 'id',
        title: this.$t('label.certificateid'),
        dataIndex: 'id',
        width: 450
      },
      {
        key: 'actions',
        title: this.$t('label.actions'),
        dataIndex: 'actions',
        fixed: 'right',
        width: 80
      }
    ]
    this.detailColumn = ['name', 'certificate', 'certchain']
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        certificate: [{ required: true, message: this.$t('label.required') }],
        privatekey: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      const params = {}
      params.page = this.page
      params.pageSize = this.pageSize
      if (this.$route.meta.name === 'account') {
        params.accountid = this.resource.id
        delete params.projectid
      } else { // project
        params.projectid = this.resource.id
      }

      this.loading = true

      getAPI('listSslCerts', params).then(json => {
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

      postAPI('deleteSslCert', params).then(json => {
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
        title,
        okText: this.$t('label.ok'),
        okType: 'danger',
        cancelText: this.$t('label.cancel'),
        onOk () {
          self.onDelete(row)
        }
      })
    },
    uploadSslCert () {
      if (this.uploading) return
      this.formRef.value.validate().then(() => {
        const formValues = toRaw(this.form)
        this.uploading = true
        const params = {
          name: formValues.name,
          certificate: formValues.certificate,
          privatekey: formValues.privatekey
        }
        if (formValues.enabledrevocationcheck != null && formValues.enabledrevocationcheck) {
          params.enabledrevocationcheck = 'true'
        } else {
          params.enabledrevocationcheck = 'false'
        }
        if (this.$route.meta.name === 'account') {
          params.account = this.resource.name
          params.domainid = this.resource.domainid
        } else { // project
          params.projectid = this.resource.id
        }
        if (formValues.password) {
          params.password = formValues.password
        }
        if (formValues.certchain) {
          params.certchain = formValues.certchain
        }
        postAPI('uploadSslCert', params).then(json => {
          this.$notification.success({
            message: this.$t('message.success.upload.ssl.cert')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.fetchData()
          this.uploading = false
          this.showUploadForm = false
        })
      })
    }
  }
}
</script>

<style scoped>
:deep(.ant-table-fixed-right) {
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
