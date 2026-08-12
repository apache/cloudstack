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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmit"
        layout="vertical">
        <a-form-item name="controlaffinitygroupids" ref="controlaffinitygroupids">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.control.nodes.affinitygroupid')" :tooltip="$t('label.cks.cluster.control.nodes.affinitygroupid')"/>
          </template>
          <a-select
            v-model:value="controlAffinityGroups"
            mode="multiple"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0"
            :loading="affinityGroupLoading"
            :placeholder="$t('label.cks.cluster.control.nodes.affinitygroupid')">
            <a-select-option v-for="opt in affinityGroups" :key="opt.id" :label="opt.name">
              {{ opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="workeraffinitygroupids" ref="workeraffinitygroupids">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.worker.nodes.affinitygroupid')" :tooltip="$t('label.cks.cluster.worker.nodes.affinitygroupid')"/>
          </template>
          <a-select
            v-model:value="workerAffinityGroups"
            mode="multiple"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0"
            :loading="affinityGroupLoading"
            :placeholder="$t('label.cks.cluster.worker.nodes.affinitygroupid')">
            <a-select-option v-for="opt in affinityGroups" :key="opt.id" :label="opt.name">
              {{ opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="resource.etcdnodes && resource.etcdnodes > 0" name="etcdaffinitygroupids" ref="etcdaffinitygroupids">
          <template #label>
            <tooltip-label :title="$t('label.cks.cluster.etcd.nodes.affinitygroupid')" :tooltip="$t('label.cks.cluster.etcd.nodes.affinitygroupid')"/>
          </template>
          <a-select
            v-model:value="etcdAffinityGroups"
            mode="multiple"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0"
            :loading="affinityGroupLoading"
            :placeholder="$t('label.cks.cluster.etcd.nodes.affinitygroupid')">
            <a-select-option v-for="opt in affinityGroups" :key="opt.id" :label="opt.name">
              {{ opt.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ChangeKubernetesClusterAffinity',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      affinityGroups: [],
      affinityGroupLoading: false,
      controlAffinityGroups: [],
      workerAffinityGroups: [],
      etcdAffinityGroups: []
    }
  },
  created () {
    this.initForm()
    this.fetchAffinityGroups()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    fetchAffinityGroups () {
      this.affinityGroupLoading = true
      const params = {
        domainid: this.resource.domainid,
        account: this.resource.account
      }
      getAPI('listAffinityGroups', params).then(json => {
        const groups = json.listaffinitygroupsresponse.affinitygroup
        if (groups && groups.length > 0) {
          this.affinityGroups = groups.filter(group => group.type !== 'ExplicitDedication')
        }
        this.prePopulateSelections()
      }).finally(() => {
        this.affinityGroupLoading = false
      })
    },
    prePopulateSelections () {
      this.controlAffinityGroups = this.parseAffinityGroupIds(this.resource.controlaffinitygroupids)
      this.workerAffinityGroups = this.parseAffinityGroupIds(this.resource.workeraffinitygroupids)
      this.etcdAffinityGroups = this.parseAffinityGroupIds(this.resource.etcdaffinitygroupids)
    },
    parseAffinityGroupIds (idsCsv) {
      if (!idsCsv) {
        return []
      }
      return idsCsv.split(',').filter(id => id.trim() !== '')
    },
    handleSubmit (e) {
      if (e) e.preventDefault()
      if (this.loading) return
      this.loading = true
      const params = {
        id: this.resource.id
      }
      let affinityIndex = 0
      const addAffinityGroups = (nodeType, groups) => {
        if (groups && groups.length > 0) {
          params[`nodeaffinitygroups[${affinityIndex}].node`] = nodeType
          params[`nodeaffinitygroups[${affinityIndex}].affinitygroup`] = groups.join(',')
          affinityIndex++
        }
      }
      addAffinityGroups('control', this.controlAffinityGroups)
      addAffinityGroups('worker', this.workerAffinityGroups)
      if (this.resource.etcdnodes && this.resource.etcdnodes > 0) {
        addAffinityGroups('etcd', this.etcdAffinityGroups)
      }
      postAPI('updateKubernetesClusterAffinityGroups', params).then(json => {
        this.$notification.success({
          message: this.$t('message.success.change.affinity.group')
        })
        this.$emit('close-action')
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    width: 80vw;

    @media (min-width: 600px) {
      width: 450px;
    }
  }
</style>
