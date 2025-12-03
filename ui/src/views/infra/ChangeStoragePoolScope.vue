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
    <div class="form-layout" v-ctrl-enter="handleSubmitForm">
      <div class="form">
        <a-form
          :ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          @submit="handleSubmitForm">
          <a-alert type="warning">
            <template #message>
              <span
                v-html="(resource.scope=='ZONE' ? $t('message.action.primary.storage.scope.cluster') : $t('message.action.primary.storage.scope.zone')) +
                        '<br><br>' + $t('message.warn.change.primary.storage.scope')"></span>
            </template>
          </a-alert>
          <p></p>
          <a-form-item name="clusterid" ref="clusterid" v-if="resource.scope=='ZONE'">
            <template #label>
              <tooltip-label :title="$t('label.clustername')" :tooltip="placeholder.clusterid"/>
            </template>
            <a-select
              v-model:value="form.clusterid"
              :placeholder="placeholder.clusterid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              @change="handleChangeCluster">
              <a-select-option
                v-for="cluster in clustersList"
                :value="cluster.id"
                :key="cluster.id"
                :label="cluster.name">
                {{ cluster.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button @click="handleSubmitForm" ref="submit" type="primary">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </div>
    </div>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ChangeStoragePoolScope',
  mixins: [mixinForm],
  components: {
    DedicateDomain,
    ResourceIcon,
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
      loading: false,
      clustersList: [],
      selectedCluster: null,
      placeholder: {
        clusterid: null
      }
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({ })
      this.rules = reactive({
        clusterid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.fetchClusters(this.resource.zoneid)
    },
    fetchClusters (zoneId) {
      this.form.clusterid = null
      this.clustersList = []
      if (!zoneId) return
      this.zoneId = zoneId
      this.loading = true
      api('listClusters', { zoneid: zoneId }).then(response => {
        this.clustersList = response.listclustersresponse.cluster || []
        this.form.clusterid = this.clustersList[0].id || null
        if (this.form.clusterid) {
          this.handleChangeCluster(this.form.clusterid)
        }
      }).catch(error => {
        this.$notifyError(error)
        this.clustersList = []
        this.form.clusterid = null
      }).finally(() => {
        this.loading = false
      })
    },
    handleChangeCluster (value) {
      this.form.clusterid = value
      this.selectedCluster = this.clustersList.find(i => i.id === this.form.clusterid)
    },
    handleSubmitForm () {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        this.args = {}
        if (this.resource.scope === 'ZONE') {
          this.args = {
            id: this.resource.id,
            scope: 'CLUSTER',
            clusterid: values.clusterid
          }
        } else {
          this.args = {
            id: this.resource.id,
            scope: 'ZONE'
          }
        }

        this.changeStoragePoolScope(this.args)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    changeStoragePoolScope (args) {
      api('changeStoragePoolScope', args).then(json => {
        this.$pollJob({
          jobId: json.changestoragepoolscoperesponse.jobid,
          title: this.$t('message.success.change.scope'),
          description: args.name,
          successMessage: this.$t('message.success.change.scope'),
          successMethod: (result) => {
            this.closeAction()
          },
          errorMessage: this.$t('message.change.scope.failed'),
          loadingMessage: this.$t('message.change.scope.processing'),
          catchMessage: this.$t('error.fetching.async.job.result')
        })
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }

  }
}
</script>

<style lang="scss">
  .form {
    &__label {
      margin-bottom: 5px;

      .required {
        margin-left: 10px;
      }
    }
    &__item {
      margin-bottom: 20px;
    }
    .ant-select {
      width: 85vw;
      @media (min-width: 760px) {
        width: 400px;
      }
    }
  }

  .required {
    color: #ff0000;
    &-label {
      display: none;
      &--error {
        display: block;
      }
    }
  }
</style>
