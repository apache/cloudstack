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
  <a-row :gutter="12">
    <a-col :md="24">
      <a-card class="breadcrumb-card">
        <a-col :md="24" style="display: flex">
          <breadcrumb style="padding-top: 6px; padding-left: 8px" />
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="fetchData()" >
            <template #icon><ReloadOutlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            size="small"
            shape="round"
            @click="sslFormVisible = true">
            <template #icon><SafetyCertificateOutlined /></template>
            {{ $t('label.sslcertificates') }}
          </a-button>
          <a-modal
            v-if="sslFormVisible"
            :title="$t('label.sslcertificates')"
            :visible="sslFormVisible"
            :footer="null"
            :maskClosable="false"
            :cancelText="$t('label.cancel')"
            @cancel="sslModalClose">
            <p>
              {{ $t('message.update.ssl') }}
            </p>

            <a-form @submit.prevent="handleSslFormSubmit" :ref="formRef" :model="form" :rules="rules">
              <a-form-item name="root" ref="root" :required="true">
                <template #label>
                  {{ $t('label.root.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.name.description">
                    <info-circle-outlined />
                  </a-tooltip>
                </template>
                <a-textarea
                  id="rootCert"
                  rows="2"
                  :placeholder="$t('label.root.certificate')"
                  :autoFocus="true"
                  name="rootCert"
                  v-model:value="form.root"
                ></a-textarea>
              </a-form-item>

              <transition-group name="fadeInUp" tag="div">
                <a-form-item
                  v-for="(item, index) in intermediateCertificates"
                  :key="`key-${index}`"
                  :name="`intermediate${index + 1}`"
                  :ref="`intermediate${index + 1}`"
                  class="intermediate-certificate">
                  <template #label>
                    {{ $t('label.intermediate.certificate') + ` ${index + 1} ` }}
                    <a-tooltip placement="bottom" :title="apiParams.id.description">
                      <info-circle-outlined />
                    </a-tooltip>
                  </template>
                  <a-textarea
                    :id="`intermediateCert${index}`"
                    rows="2"
                    :placeholder="$t('label.intermediate.certificate') + ` ${index + 1}`"
                    :name="`intermediateCert${index}`"
                    v-model:value="form[`intermediate${index + 1}`]"
                  ></a-textarea>
                </a-form-item>
              </transition-group>

              <a-form-item>
                <a-button @click="addIntermediateCert">
                  <PlusCircleOutlined />
                  {{ $t('label.add.intermediate.certificate') }}
                </a-button>
              </a-form-item>

              <a-form-item name="server" ref="server" :required="true">
                <template #label>
                  {{ $t('label.server.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.certificate.description">
                    <info-circle-outlined />
                  </a-tooltip>
                </template>
                <a-textarea
                  id="serverCert"
                  rows="2"
                  :placeholder="$t('label.server.certificate')"
                  name="serverCert"
                  v-model:value="form.server"
                ></a-textarea>
              </a-form-item>

              <a-form-item name="pkcsKey" ref="pkcsKey" :required="true">
                <template #label>
                  {{ $t('label.pkcs.private.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.privatekey.description">
                    <info-circle-outlined />
                  </a-tooltip>
                </template>
                <a-textarea
                  id="pkcsKey"
                  rows="2"
                  :placeholder="$t('label.pkcs.private.certificate')"
                  name="pkcsKey"
                  v-model:value="form.pkcs"
                ></a-textarea>
              </a-form-item>

              <a-form-item name="dns" ref="dns" :required="true">
                <template #label>
                  {{ $t('label.domain.suffix') }}
                  <a-tooltip placement="bottom" :title="apiParams.domainsuffix.description">
                    <info-circle-outlined />
                  </a-tooltip>
                </template>
                <a-input
                  id="dnsSuffix"
                  :placeholder="$t('label.domain.suffix')"
                  name="dnsSuffix"
                  v-model:value="form.dns"
                ></a-input>
              </a-form-item>

              <a-form-item class="controls">
                <a-button @click="sslModalClose" class="close-button">
                  {{ $t('label.cancel' ) }}
                </a-button>
                <a-button type="primary" htmlType="submit" :loading="sslFormSubmitting">
                  {{ $t('label.submit' ) }}
                </a-button>
              </a-form-item>
            </a-form>
          </a-modal>
        </a-col>
      </a-card>
    </a-col>
    <template v-for="(section, index) in sections" :key="index">
      <a-col
        :md="6"
        style="margin-bottom: 12px"
        v-if="routes[section]">
        <chart-card :loading="loading">
          <div class="chart-card-inner">
            <router-link :to="{ name: section.substring(0, section.length - 1) }">
              <h2>{{ $t(routes[section].title) }}</h2>
              <h2><render-icon :icon="routes[section].icon" /> {{ stats[section] }}</h2>
            </router-link>
          </div>
        </chart-card>
      </a-col>
    </template>
  </a-row>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import router from '@/router'
import RenderIcon from '@/utils/renderIcon'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'InfraSummary',
  components: {
    Breadcrumb,
    ChartCard,
    RenderIcon
  },
  data () {
    return {
      loading: true,
      routes: {},
      sections: ['zones', 'pods', 'clusters', 'hosts', 'storagepools', 'imagestores', 'systemvms', 'routers', 'cpusockets', 'managementservers', 'alerts', 'ilbvms'],
      sslFormVisible: false,
      stats: {},
      intermediateCertificates: [],
      sslFormSubmitting: false,
      maxCerts: 0
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('uploadCustomCertificate')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        root: [{ required: true, message: this.$t('label.required') }],
        server: [{ required: true, message: this.$t('label.required') }],
        pkcsKey: [{ required: true, message: this.$t('label.required') }],
        dns: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.routes = {}
      for (const section of this.sections) {
        const node = router.resolve({ name: section.substring(0, section.length - 1) })
        this.routes[section] = {
          title: node.meta.title,
          icon: node.meta.icon
        }
      }
      this.listInfra()
    },
    listInfra () {
      this.loading = true
      api('listInfrastructure').then(json => {
        this.stats = []
        if (json && json.listinfrastructureresponse && json.listinfrastructureresponse.infrastructure) {
          this.stats = json.listinfrastructureresponse.infrastructure
        }
      }).finally(f => {
        this.loading = false
      })
    },

    resetSslFormData () {
      this.formRef.value.resetFields()
      this.intermediateCertificates = []
      this.sslFormSubmitting = false
      this.sslFormVisible = false
    },

    sslModalClose () {
      this.resetSslFormData()
    },

    addIntermediateCert () {
      this.intermediateCertificates.push('')
    },

    pollActionCompletion (jobId, count) {
      api('queryAsyncJobResult', { jobid: jobId }).then(json => {
        const result = json.queryasyncjobresultresponse
        if (result.jobstatus === 1 && this.maxCerts === count) {
          this.$message.success(`${this.$t('label.certificate.upload')}: ${result.jobresult.customcertificate.message}`)
          this.$notification.success({
            message: this.$t('label.certificate.upload'),
            description: result.jobresult.customcertificate.message || this.$t('message.success.certificate.upload')
          })
        } else if (result.jobstatus === 2) {
          this.$notification.error({
            message: this.$t('label.certificate.upload.failed'),
            description: result.jobresult.errortext || this.$t('label.certificate.upload.failed.description'),
            duration: 0
          })
        } else if (result.jobstatus === 0) {
          this.$message
            .loading(`${this.$t('message.certificate.upload.processing')}: ${count}`, 2)
            .then(() => this.pollActionCompletion(jobId, count))
        }
      }).catch(e => {
        console.log(this.$t('error.fetching.async.job.result') + e)
      })
    },

    handleSslFormSubmit () {
      this.sslFormSubmitting = true

      this.formRef.value.validate().then(() => {
        const formValues = toRaw(this.form)

        this.maxCerts = 2 + Object.keys(formValues).length
        let count = 1
        let data = {
          id: count,
          certificate: formValues.root,
          domainsuffix: formValues.dns,
          name: 'root'
        }
        api('uploadCustomCertificate', {}, 'POST', data).then(response => {
          this.pollActionCompletion(response.uploadcustomcertificateresponse.jobid, count)
        }).then(() => {
          this.sslModalClose()
        })

        Object.keys(formValues).forEach(key => {
          if (key.includes('intermediate')) {
            count = count + 1
            const data = {
              id: count,
              certificate: formValues[key],
              domainsuffix: formValues.dns,
              name: key
            }
            api('uploadCustomCertificate', {}, 'POST', data).then(response => {
              this.pollActionCompletion(response.uploadcustomcertificateresponse.jobid, count)
            }).then(() => {
              this.sslModalClose()
            })
          }
        })

        count = count <= 2 ? 3 : count + 1
        data = {
          id: count,
          certificate: formValues.server,
          domainsuffix: formValues.dns,
          privatekey: formValues.pkcs
        }
        api('uploadCustomCertificate', {}, 'POST', data).then(response => {
          this.pollActionCompletion(response.uploadcustomcertificateresponse.jobid, count)
        }).then(() => {
          this.sslModalClose()
        })
      }).finally(() => { this.sslFormSubmitting = false })
    }
  }
}
</script>

<style lang="scss" scoped>

  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

  .chart-card-inner {
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
  }
  .intermediate-certificate {
    opacity: 1;
    transform: none;
    transition: opacity 0.2s ease 0s, transform 0.5s ease;
    will-change: transform;
  }
  .intermediate-certificate.fadeInUp-enter-active {
    opacity: 0;
    transform: translateY(10px);
    transition: none;
  }
  .controls {
    display: flex;
    justify-content: flex-end;
  }
  .close-button {
    margin-right: 20px;
  }
  .ant-form-item {
    margin-bottom: 10px;
  }
</style>
