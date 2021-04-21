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
            icon="reload"
            size="small"
            shape="round"
            @click="fetchData()" >
            {{ $t('label.refresh') }}
          </a-button>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            icon="safety-certificate"
            size="small"
            shape="round"
            @click="sslFormVisible = true">
            {{ $t('label.sslcertificates') }}
          </a-button>
          <a-modal
            :title="$t('label.sslcertificates')"
            :visible="sslFormVisible"
            :footer="null"
            :maskClosable="false"
            :cancelText="$t('label.cancel')"
            @cancel="sslModalClose">
            <p>
              {{ $t('message.update.ssl') }}
            </p>

            <a-form @submit.prevent="handleSslFormSubmit" ref="sslForm" :form="form">
              <a-form-item :required="true">
                <span slot="label">
                  {{ $t('label.root.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.name.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-textarea
                  id="rootCert"
                  rows="2"
                  :placeholder="$t('label.root.certificate')"
                  :autoFocus="true"
                  name="rootCert"
                  v-decorator="[
                    'root',
                    {rules: [{ required: true, message: `${$t('label.required')}` }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <transition-group name="fadeInUp" tag="div">
                <a-form-item
                  v-for="(item, index) in intermediateCertificates"
                  :key="`key-${index}`"
                  class="intermediate-certificate">
                  <span slot="label">
                    {{ $t('label.intermediate.certificate') + ` ${index + 1} ` }}
                    <a-tooltip placement="bottom" :title="apiParams.id.description">
                      <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                    </a-tooltip>
                  </span>
                  <a-textarea
                    :id="`intermediateCert${index}`"
                    rows="2"
                    :placeholder="$t('label.intermediate.certificate') + ` ${index + 1}`"
                    :name="`intermediateCert${index}`"
                    v-decorator="[
                      `intermediate${index + 1}`,
                      {validateTrigger:'change'}
                    ]"
                  ></a-textarea>
                </a-form-item>
              </transition-group>

              <a-form-item>
                <a-button @click="addIntermediateCert">
                  <a-icon type="plus-circle" />
                  {{ $t('label.add.intermediate.certificate') }}
                </a-button>
              </a-form-item>

              <a-form-item :required="true">
                <span slot="label">
                  {{ $t('label.server.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.certificate.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-textarea
                  id="serverCert"
                  rows="2"
                  :placeholder="$t('label.server.certificate')"
                  name="serverCert"
                  v-decorator="[
                    'server',
                    {rules: [{ required: true, message: `${$t('label.required')}` }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <a-form-item :required="true">
                <span slot="label">
                  {{ $t('label.pkcs.private.certificate') }}
                  <a-tooltip placement="bottom" :title="apiParams.privatekey.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-textarea
                  id="pkcsKey"
                  rows="2"
                  :placeholder="$t('label.pkcs.private.certificate')"
                  name="pkcsKey"
                  v-decorator="[
                    'pkcs',
                    {rules: [{ required: true, message: `${$t('label.required')}` }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <a-form-item :required="true">
                <span slot="label">
                  {{ $t('label.domain.suffix') }}
                  <a-tooltip placement="bottom" :title="apiParams.domainsuffix.description">
                    <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
                  </a-tooltip>
                </span>
                <a-input
                  id="dnsSuffix"
                  :placeholder="$t('label.domain.suffix')"
                  name="dnsSuffix"
                  v-decorator="[
                    'dns',
                    {rules: [{ required: true, message: `${$t('label.required')}` }], validateTrigger:'change'}
                  ]"
                ></a-input>
              </a-form-item>

              <a-form-item class="controls">
                <a-button @click="this.sslModalClose" class="close-button">
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
    <a-col
      :md="6"
      style="margin-bottom: 12px"
      v-for="(section, index) in sections"
      v-if="routes[section]"
      :key="index">
      <chart-card :loading="loading">
        <div class="chart-card-inner">
          <router-link :to="{ name: section.substring(0, section.length - 1) }">
            <h2>{{ $t(routes[section].title) }}</h2>
            <h2><a-icon :type="routes[section].icon" /> {{ stats[section] }}</h2>
          </router-link>
        </div>
      </chart-card>
    </a-col>
  </a-row>
</template>

<script>
import { api } from '@/api'
import router from '@/router'

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'InfraSummary',
  components: {
    Breadcrumb,
    ChartCard
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
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.uploadCustomCertificate || {}
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.routes = {}
      for (const section of this.sections) {
        const node = router.resolve({ name: section.substring(0, section.length - 1) })
        this.routes[section] = {
          title: node.route.meta.title,
          icon: node.route.meta.icon
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
      this.form.resetFields()
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

      this.form.validateFields(errors => {
        if (errors) {
          this.sslFormSubmitting = false
          return
        }

        const formValues = this.form.getFieldsValue()

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
      })
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
