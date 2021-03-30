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
  <div class="form-layout">
    <a-spin :spinning="loading">
      <a-form
        :form="form"
        layout="vertical">
        <a-form-item>
          <span slot="label">
            {{ $t('label.name') }}
            <a-tooltip :title="apiParams.name.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['name', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.name.description"
            autoFocus/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.displaytext') }}
            <a-tooltip :title="apiParams.displaytext.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['displaytext', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.displaytext.description"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.zoneid') }}
            <a-tooltip :title="apiParams.zoneid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            :loading="loadingZone"
            v-decorator="['zoneid', {
              initialValue: this.selectedZone,
              rules: [{ required: true, message: `${this.$t('label.required')}`}]
            }]"
            @change="val => changeZone(val)">
            <a-select-option v-for="zone in zones" :key="zone.id">
              {{ zone.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.cidr') }}
            <a-tooltip :title="apiParams.cidr.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['cidr', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.cidr.description"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.networkdomain') }}
            <a-tooltip :title="apiParams.networkdomain.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-input
            v-decorator="['networkdomain']"
            :placeholder="apiParams.networkdomain.description"/>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.vpcofferingid') }}
            <a-tooltip :title="apiParams.vpcofferingid.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-select
            :loading="loadingOffering"
            v-decorator="['vpcofferingid', {
              initialValue: this.selectedOffering,
              rules: [{ required: true, message: `${this.$t('label.required')}`}]}]">
            <a-select-option :value="offering.id" v-for="offering in vpcOfferings" :key="offering.id">
              {{ offering.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <span slot="label">
            {{ $t('label.start') }}
            <a-tooltip :title="apiParams.start.description">
              <a-icon type="info-circle" style="color: rgba(0,0,0,.45)" />
            </a-tooltip>
          </span>
          <a-switch v-decorator="['start', {initialValue: true}]" defaultChecked />
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>
<script>
import { api } from '@/api'
export default {
  name: 'CreateVpc',
  data () {
    return {
      loading: false,
      loadingZone: false,
      loadingOffering: false,
      selectedZone: '',
      zones: [],
      vpcOfferings: [],
      selectedOffering: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = {}
    var apiConfig = this.$store.getters.apis.createVPC || []
    apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.fetchZones()
    },
    fetchZones () {
      this.loadingZone = true
      api('listZones', { listAll: true }).then((response) => {
        const listZones = response.listzonesresponse.zone || []
        this.zones = listZones.filter(zone => !zone.securitygroupsenabled)
        this.selectedZone = ''
        if (this.zones.length > 0) {
          this.selectedZone = this.zones[0].id
          this.changeZone(this.selectedZone)
        }
      }).finally(() => {
        this.loadingZone = false
      })
    },
    changeZone (value) {
      this.selectedZone = value
      if (this.selectedZone === '') {
        this.selectedOffering = ''
        return
      }
      this.fetchOfferings()
    },
    fetchOfferings () {
      this.loadingOffering = true
      api('listVPCOfferings', { zoneid: this.selectedZone, state: 'Enabled' }).then((reponse) => {
        this.vpcOfferings = reponse.listvpcofferingsresponse.vpcoffering
        this.selectedOffering = this.vpcOfferings[0].id || ''
      }).finally(() => {
        this.loadingOffering = false
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        const params = {}
        for (const key in values) {
          const input = values[key]
          if (input === '' || input === null || input === undefined) {
            continue
          }
          params[key] = input
        }
        this.loading = true
        const title = this.$t('label.add.vpc')
        const description = this.$t('message.success.add.vpc.network')
        api('createVPC', params).then(json => {
          const jobId = json.createvpcresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              successMethod: result => {
                this.$store.dispatch('AddAsyncJob', {
                  title: title,
                  jobid: jobId,
                  description: description,
                  status: this.$t('progress')
                })
              },
              loadingMessage: `${title} ${this.$t('label.in.progress')}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
            this.$emit('refresh-data')
          }
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.form-layout {
  width: 80vw;
  @media (min-width: 700px) {
    width: 600px;
  }
}

.form {
  margin: 10px 0;
}

.action-button {
  text-align: right;
  button {
    margin-right: 5px;
  }
}
</style>
