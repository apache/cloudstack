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
    <a-form
      class="form-layout"
      layout="vertical"
      :ref="formRef"
      :model="form"
      v-ctrl-enter="handleSubmit"
      @finish="handleSubmit">
      <div style="margin-bottom: 12px">{{ $t('message.configure.cpu.baseline') }}</div>
      <a-form-item name="model" ref="model">
        <template #label>
          <tooltip-label :title="$t('label.cpu.baseline.model')" :tooltip="$t('message.configure.cpu.baseline')"/>
        </template>
        <a-auto-complete
          v-model:value="form.model"
          :options="modelOptions"
          :placeholder="$t('label.cpu.baseline.model')"
          :filterOption="filterOption"
          v-focus="true"
          allowClear />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'ConfigureCpuBaseline',
  components: {
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
      configName: 'cluster.cpu.baseline.model',
      suggestions: ['auto', 'Penryn', 'Nehalem', 'Westmere', 'SandyBridge', 'IvyBridge',
        'Haswell-noTSX', 'Haswell', 'Broadwell-noTSX', 'Broadwell', 'Skylake-Client',
        'Skylake-Server', 'Cascadelake-Server', 'Icelake-Server',
        'Opteron_G4', 'Opteron_G5', 'EPYC', 'EPYC-Rome', 'EPYC-Milan']
    }
  },
  computed: {
    modelOptions () {
      return this.suggestions.map(value => { return { value } })
    }
  },
  created () {
    this.formRef = ref()
    this.form = reactive({ model: '' })
    this.fetchCurrent()
  },
  methods: {
    filterOption (input, option) {
      return option.value.toLowerCase().indexOf((input || '').toLowerCase()) >= 0
    },
    fetchCurrent () {
      this.loading = true
      getAPI('listConfigurations', { name: this.configName, clusterid: this.resource.id }).then(json => {
        const cfg = json?.listconfigurationsresponse?.configuration?.[0]
        this.form.model = cfg && cfg.value ? cfg.value : ''
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit () {
      this.loading = true
      postAPI('updateConfiguration', {
        name: this.configName,
        value: this.form.model || '',
        clusterid: this.resource.id
      }).then(() => {
        // read the cluster-scoped value back so 'auto' shows the computed model that was actually pinned
        return getAPI('listConfigurations', { name: this.configName, clusterid: this.resource.id })
      }).then(json => {
        const cfg = json?.listconfigurationsresponse?.configuration?.[0]
        const shown = (cfg && cfg.value) || this.form.model || this.$t('label.none')
        this.$message.success({
          content: `${this.$t('label.cpu.baseline.model')}: ${shown}`,
          duration: 2
        })
        this.$emit('refresh-data')
        this.onCloseAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    onCloseAction () {
      this.$emit('close-action')
    }
  }
}
</script>
