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
            {{ $t('refresh') }}
          </a-button>
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            icon="safety-certificate"
            size="small"
            shape="round"
            @click="sslFormVisible = true">
            {{ $t('Setup SSL Certificate') }}
          </a-button>
          <a-modal
            :title="$t('SSL Certificate')"
            :visible="sslFormVisible"
            :footer="null"
            @cancel="sslModalClose">
            <p>
              Please submit a new X.509 compliant SSL certificate chain to be updated to each console proxy and secondary storage virtual instance:
            </p>

            <a-form @submit.prevent="handleSslFormSubmit" ref="sslForm" :form="form">
              <a-form-item label="Root certificate" :required="true">
                <a-textarea
                  id="rootCert"
                  rows="2"
                  placeholder="Root certificate"
                  :autoFocus="true"
                  name="rootCert"
                  v-decorator="[
                    'root',
                    {rules: [{ required: true, message: 'Required' }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <transition-group name="fadeInUp" tag="div">
                <a-form-item
                  v-for="(item, index) in intermediateCertificates"
                  :key="`key-${index}`"
                  class="intermediate-certificate"
                  :label="`Intermediate certificate ${index + 1}`">
                  <a-textarea
                    :id="`intermediateCert${index}`"
                    rows="2"
                    :placeholder="`Intermediate certificate ${index + 1}`"
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
                  Add intermediate certificate
                </a-button>
              </a-form-item>

              <a-form-item label="Server certificate" :required="true">
                <a-textarea
                  id="serverCert"
                  rows="2"
                  placeholder="Server certificate"
                  name="serverCert"
                  v-decorator="[
                    'server',
                    {rules: [{ required: true, message: 'Required' }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <a-form-item label="PKCS#8 Private Key" :required="true">
                <a-textarea
                  id="pkcsKey"
                  rows="2"
                  placeholder="PKCS#8 Private Key"
                  name="pkcsKey"
                  v-decorator="[
                    'pkcs',
                    {rules: [{ required: true, message: 'Required' }], validateTrigger:'change'}
                  ]"
                ></a-textarea>
              </a-form-item>

              <a-form-item label="DNS Domain Suffix (i.e., xyz.com)" :required="true">
                <a-input
                  id="dnsSuffix"
                  placeholder="DNS Domain Suffix (i.e., xyz.com)"
                  name="dnsSuffix"
                  v-decorator="[
                    'dns',
                    {rules: [{ required: true, message: 'Required' }], validateTrigger:'change'}
                  ]"
                ></a-input>
              </a-form-item>

              <a-form-item class="controls">
                <a-button @click="this.sslModalClose" type="danger" class="close-button">
                  Cancel
                </a-button>
                <a-button type="primary" htmlType="submit" :loading="sslFormSubmitting">
                  Submit
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
            <h1><a-icon :type="routes[section].icon" /> {{ stats[section] }}</h1>
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
  },
  mounted () {
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
          console.log(result)
          console.log(this.maxCerts)
          console.log(count)
          this.$message.success('Certificate Uploaded: ' + result.jobresult.customcertificate.message)
          this.$notification.success({
            message: 'Certificate Uploaded',
            description: result.jobresult.customcertificate.message || 'Certificate successfully uploaded'
          })
        } else if (result.jobstatus === 2) {
          this.$notification.error({
            message: 'Certificate Upload Failed',
            description: result.jobresult.errortext || 'Failed to update SSL Certificate. Failed to pass certificate validation check',
            duration: 0
          })
        } else if (result.jobstatus === 0) {
          this.$message
            .loading('Certificate upload in progress: ' + count, 2)
            .then(() => this.pollActionCompletion(jobId, count))
        }
      }).catch(e => {
        console.log('Error encountered while fetching async job result' + e)
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
    margin-left: -36px;
    margin-right: -36px;
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
