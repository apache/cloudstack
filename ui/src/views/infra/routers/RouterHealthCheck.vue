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
  <a-spin :spinning="loading">
    <a-alert
      v-if="!routerHealthChecksEnabled"
      banner
      :message="$t('message.action.router.health.checks.disabled.warning')" />
    <div v-else>
      <a-button :disabled="!('getRouterHealthCheckResults' in $store.getters.apis)" type="primary" style="width: 100%; margin-bottom: 15px" @click="showGetHelathCheck">
        <template #icon><play-circle-outlined /></template>
        {{ $t('label.action.router.health.checks') }}
      </a-button>
      <a-table
        style="overflow-y: auto"
        :columns="columns"
        :dataSource="healthChecks"
        :pagination="false"
        :rowKey="record => record.checkname"
        size="large">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <status class="status" :text="record.success === true ? 'True' : 'False'" displayText />
          </template>
        </template>
      </a-table>

      <a-modal
        v-if="'getRouterHealthCheckResults' in $store.getters.apis && showGetHealthChecksForm"
        style="top: 20px;"
        :title="$t('label.action.router.health.checks')"
        :visible="showGetHealthChecksForm"
        :closable="true"
        :maskClosable="false"
        :footer="null"
        @cancel="onCloseGetHealthChecksForm"
        centered>
        <a-spin :spinning="loading" v-ctrl-enter="handleGetHealthChecksSubmit">
          <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleGetHealthChecksSubmit"
            layout="vertical"
           >
            <a-form-item name="performfreshchecks" ref="performfreshchecks">
              <template #label>
                <tooltip-label :title="$t('label.perform.fresh.checks')" :tooltip="apiParams.performfreshchecks.description"/>
              </template>
              <a-switch
                v-model:checked="form.performfreshchecks"
                :placeholder="apiParams.performfreshchecks.description"
                v-focus="true"/>
            </a-form-item>

            <div :span="24" class="action-button">
              <a-button @click="onCloseGetHealthChecksForm">{{ $t('label.cancel') }}</a-button>
              <a-button ref="submit" type="primary" @click="handleGetHealthChecksSubmit">{{ $t('label.ok') }}</a-button>
            </div>
          </a-form>
        </a-spin>
      </a-modal>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'RouterHealthCheck',
  components: {
    Status,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      routerHealthChecksEnabled: false,
      healthChecks: [],
      loading: false,
      columns: [
        {
          title: this.$t('label.router.health.check.name'),
          dataIndex: 'checkname'
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'checktype'
        },
        {
          key: 'status',
          title: this.$t('label.router.health.check.success')
        },
        {
          title: this.$t('label.router.health.check.last.updated'),
          dataIndex: 'lastupdated'
        },
        {
          title: this.$t('label.details'),
          dataIndex: 'details'
        }
      ],
      showGetHealthChecksForm: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('getRouterHealthCheckResults')
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        this.updateResource(newItem)
      }
    }
  },
  created () {
    this.initForm()
    this.updateResource(this.resource)
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    updateResource (resource) {
      if (!resource) {
        return
      }
      if (!resource.id) {
        return
      }
      this.checkConfigurationAndGetHealthChecks()
    },
    showGetHelathCheck () {
      this.showGetHealthChecksForm = true
    },
    onCloseGetHealthChecksForm () {
      this.showGetHealthChecksForm = false
    },
    handleGetHealthChecksSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.onCloseGetHealthChecksForm()
        this.checkConfigurationAndGetHealthChecks(values.performfreshchecks)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    checkConfigurationAndGetHealthChecks (performFreshChecks) {
      var params = { name: 'router.health.checks.enabled' }
      this.loading = true
      api('listConfigurations', params).then(json => {
        this.routerHealthChecksEnabled = false
        if (json.listconfigurationsresponse.configuration !== null) {
          var config = json.listconfigurationsresponse.configuration[0]
          if (config && config.name === params.name) {
            this.routerHealthChecksEnabled = config.value === 'true'
          }
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.loading = false
        if (this.routerHealthChecksEnabled) {
          this.getHealthChecks(performFreshChecks)
        }
      })
    },
    getHealthChecks (performFreshChecks) {
      var params = { routerid: this.resource.id }
      if (performFreshChecks) {
        params.performfreshchecks = performFreshChecks
      }
      this.loading = true
      api('getRouterHealthCheckResults', params).then(json => {
        this.healthChecks = json.getrouterhealthcheckresultsresponse.routerhealthchecks.healthchecks
      }).catch(error => {
        this.$notifyError(error)
      }).finally(f => {
        this.loading = false
      })
    }
  }
}
</script>
