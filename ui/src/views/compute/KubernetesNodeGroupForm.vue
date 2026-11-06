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
        <a-form-item v-if="mode === 'create'" name="name" ref="name">
          <template #label>
            <tooltip-label :title="$t('label.name')" :tooltip="$t('message.kubernetes.node.group.name')"/>
          </template>
          <a-input
            v-model:value="form.name"
            :maxlength="20"
            :placeholder="$t('message.kubernetes.node.group.name')"
            v-focus="true" />
        </a-form-item>
        <template v-if="mode !== 'update'">
          <a-form-item name="serviceofferingid" ref="serviceofferingid">
            <template #label>
              <tooltip-label :title="$t('label.serviceofferingid')" :tooltip="$t('message.kubernetes.node.group.serviceoffering')"/>
            </template>
            <a-select
              v-model:value="form.serviceofferingid"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="serviceOfferingLoading"
              :placeholder="$t('message.kubernetes.node.group.serviceoffering')">
              <a-select-option v-for="opt in serviceOfferings" :key="opt.id" :value="opt.id" :label="opt.name || opt.description">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="mode === 'create'" name="templateid" ref="templateid">
            <template #label>
              <tooltip-label :title="$t('label.template')" :tooltip="$t('message.kubernetes.node.group.template')"/>
            </template>
            <a-select
              v-model:value="form.templateid"
              showSearch
              allowClear
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              :loading="templateLoading"
              :placeholder="$t('message.kubernetes.node.group.template')">
              <a-select-option v-for="opt in templates" :key="opt.id" :value="opt.id" :label="opt.name || opt.displaytext">
                {{ opt.name || opt.displaytext }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="size" ref="size">
            <template #label>
              <tooltip-label :title="$t('label.size')" :tooltip="$t('message.kubernetes.node.group.size')"/>
            </template>
            <a-input-number
              v-model:value="form.size"
              :min="minimumSize"
              :max="maximumSize"
              :precision="0"
              style="width: 100%"
              :placeholder="$t('message.kubernetes.node.group.size')" />
          </a-form-item>
        </template>
        <template v-if="mode !== 'scale'">
          <a-form-item name="autoscalingenabled" ref="autoscalingenabled">
            <template #label>
              <tooltip-label :title="$t('label.kubernetes.node.group.autoscaling')" :tooltip="$t('message.kubernetes.node.group.autoscaling')"/>
            </template>
            <a-switch v-model:checked="form.autoscalingenabled" />
          </a-form-item>
          <a-row v-if="form.autoscalingenabled" :gutter="12">
            <a-col :span="12">
              <a-form-item name="minsize" ref="minsize">
                <template #label>
                  <tooltip-label :title="$t('label.minsize')" :tooltip="$t('message.kubernetes.node.group.minsize')"/>
                </template>
                <a-input-number v-model:value="form.minsize" :min="isDefault ? 1 : 0" :precision="0" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item name="maxsize" ref="maxsize">
                <template #label>
                  <tooltip-label :title="$t('label.maxsize')" :tooltip="$t('message.kubernetes.node.group.maxsize')"/>
                </template>
                <a-input-number v-model:value="form.maxsize" :min="Math.max(1, form.minsize || 0)" :precision="0" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item>
            <template #label>
              <tooltip-label :title="$t('label.kubernetes.node.labels')" :tooltip="$t('message.kubernetes.node.group.labels')"/>
            </template>
            <a-row v-for="(label, idx) in labels" :key="'label-' + idx" :gutter="8" class="pair-row">
              <a-col :span="11">
                <a-input v-model:value="label.key" :placeholder="$t('label.key')" />
              </a-col>
              <a-col :span="11">
                <a-input v-model:value="label.value" :placeholder="$t('label.value')" />
              </a-col>
              <a-col :span="2">
                <minus-circle-outlined class="pair-remove" @click="labels.splice(idx, 1)" />
              </a-col>
            </a-row>
            <a-button type="dashed" block @click="labels.push({ key: '', value: '' })">
              <template #icon><plus-outlined /></template>
              {{ $t('label.kubernetes.node.label.add') }}
            </a-button>
          </a-form-item>
          <a-form-item>
            <template #label>
              <tooltip-label
                :title="$t('label.kubernetes.node.taints')"
                :tooltip="$t(isDefault ? 'message.kubernetes.node.group.default.taints' : 'message.kubernetes.node.group.taints')"/>
            </template>
            <a-row v-for="(taint, idx) in taints" :key="'taint-' + idx" :gutter="8" class="pair-row">
              <a-col :span="7">
                <a-input v-model:value="taint.key" :placeholder="$t('label.key')" />
              </a-col>
              <a-col :span="7">
                <a-input v-model:value="taint.value" :placeholder="$t('label.value')" />
              </a-col>
              <a-col :span="8">
                <a-select v-model:value="taint.effect" style="width: 100%">
                  <a-select-option v-for="effect in availableTaintEffects" :key="effect" :value="effect">
                    {{ effect }}
                  </a-select-option>
                </a-select>
              </a-col>
              <a-col :span="2">
                <minus-circle-outlined class="pair-remove" @click="taints.splice(idx, 1)" />
              </a-col>
            </a-row>
            <a-button type="dashed" block @click="taints.push({ key: '', value: '', effect: availableTaintEffects[0] })">
              <template #icon><plus-outlined /></template>
              {{ $t('label.kubernetes.node.taint.add') }}
            </a-button>
          </a-form-item>
        </template>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { isAdminOrDomainAdmin } from '@/role'
import TooltipLabel from '@/components/widgets/TooltipLabel'

const NODE_GROUP_NAME_PATTERN = /^[a-z0-9]([-a-z0-9]*[a-z0-9])?$/
const MAX_NODE_GROUP_NAME_LENGTH = 20
const TAINT_EFFECTS = ['NoSchedule', 'PreferNoSchedule', 'NoExecute']

export default {
  name: 'KubernetesNodeGroupForm',
  components: {
    TooltipLabel
  },
  props: {
    // create | scale | update
    mode: {
      type: String,
      required: true
    },
    // the Kubernetes cluster
    resource: {
      type: Object,
      required: true
    },
    // the node group, for scale and update
    nodeGroup: {
      type: Object,
      default: null
    }
  },
  emits: ['close-action', 'refresh-data'],
  data () {
    return {
      loading: false,
      serviceOfferings: [],
      serviceOfferingLoading: false,
      templates: [],
      templateLoading: false,
      labels: [],
      taints: []
    }
  },
  computed: {
    isDefault () {
      return !!this.nodeGroup?.isdefault
    },
    // The default node group has to accept the system pods
    availableTaintEffects () {
      return this.isDefault ? ['PreferNoSchedule'] : TAINT_EFFECTS
    },
    autoscalingEnabled () {
      return this.mode === 'scale' ? !!this.nodeGroup?.autoscalingenabled : !!this.form.autoscalingenabled
    },
    minimumSize () {
      if (this.autoscalingEnabled) {
        return this.mode === 'scale' ? this.nodeGroup.minsize : (this.form.minsize || 0)
      }
      return this.isDefault ? 1 : 0
    },
    maximumSize () {
      if (this.autoscalingEnabled) {
        return this.mode === 'scale' ? this.nodeGroup.maxsize : this.form.maxsize
      }
      return undefined
    }
  },
  created () {
    this.initForm()
    if (this.mode !== 'update') {
      this.fetchServiceOfferings()
    }
    if (this.mode === 'create') {
      this.fetchCksTemplates()
    }
    if (this.mode === 'update' && this.nodeGroup) {
      this.labels = Object.entries(this.nodeGroup.nodelabels || {}).map(([key, value]) => ({ key, value }))
      this.taints = (this.nodeGroup.nodetaints || []).map(this.parseTaint)
    }
    this.originalLabels = this.normalizeLabels(this.labels)
    this.originalTaints = this.normalizeTaints(this.taints)
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: undefined,
        serviceofferingid: this.nodeGroup?.serviceofferingid,
        templateid: undefined,
        size: this.nodeGroup ? this.nodeGroup.size : 1,
        autoscalingenabled: !!this.nodeGroup?.autoscalingenabled,
        minsize: this.nodeGroup?.minsize ?? (this.nodeGroup?.isdefault ? 1 : 0),
        maxsize: this.nodeGroup?.maxsize ?? Math.max(1, this.nodeGroup ? this.nodeGroup.size : 1)
      })
      const rules = {}
      if (this.mode === 'create') {
        rules.name = [
          { required: true, message: this.$t('message.error.required.input') },
          { validator: this.validateName }
        ]
        rules.serviceofferingid = [{ required: true, message: this.$t('message.error.select') }]
      }
      if (this.mode !== 'update') {
        rules.size = [{ required: true, message: this.$t('message.error.required.input') }]
      }
      this.rules = reactive(rules)
    },
    async validateName (rule, value) {
      if (value && (value.length > MAX_NODE_GROUP_NAME_LENGTH || !NODE_GROUP_NAME_PATTERN.test(value))) {
        return Promise.reject(this.$t('message.kubernetes.node.group.name'))
      }
      return Promise.resolve()
    },
    // Taints are returned in the key[=value]:effect format
    parseTaint (taint) {
      const effectIndex = taint.lastIndexOf(':')
      const keyValue = effectIndex >= 0 ? taint.substring(0, effectIndex) : taint
      const effect = effectIndex >= 0 ? taint.substring(effectIndex + 1) : 'NoSchedule'
      const valueIndex = keyValue.indexOf('=')
      return {
        key: valueIndex >= 0 ? keyValue.substring(0, valueIndex) : keyValue,
        value: valueIndex >= 0 ? keyValue.substring(valueIndex + 1) : '',
        effect: effect
      }
    },
    normalizeLabels (labels) {
      return labels
        .map(l => ({ key: (l.key || '').trim(), value: (l.value || '').trim() }))
        .filter(l => l.key || l.value)
    },
    normalizeTaints (taints) {
      return taints
        .map(t => ({ key: (t.key || '').trim(), value: (t.value || '').trim(), effect: t.effect }))
        .filter(t => t.key || t.value)
    },
    validateAutoscaling (size) {
      if (!this.form.autoscalingenabled) {
        return true
      }
      const { minsize, maxsize } = this.form
      let error = null
      if (minsize === null || minsize === undefined || maxsize === null || maxsize === undefined) {
        error = 'message.kubernetes.node.group.autoscaling.required'
      } else if (maxsize < Math.max(1, minsize)) {
        error = 'message.kubernetes.node.group.autoscaling.range'
      } else if (size !== undefined && (size < minsize || size > maxsize)) {
        error = 'message.kubernetes.node.group.autoscaling.size'
      }
      if (error) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t(error)
        })
        return false
      }
      return true
    },
    validatePairs (labels, taints) {
      if (labels.some(l => !l.key) || taints.some(t => !t.key)) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.kubernetes.node.group.key.required')
        })
        return false
      }
      return true
    },
    addLabelParams (params, labels) {
      labels.forEach((label, idx) => {
        params['nodelabels[' + idx + '].key'] = label.key
        params['nodelabels[' + idx + '].value'] = label.value
      })
    },
    addTaintParams (params, taints) {
      taints.forEach((taint, idx) => {
        params['nodetaints[' + idx + '].key'] = taint.key
        params['nodetaints[' + idx + '].value'] = taint.value
        params['nodetaints[' + idx + '].effect'] = taint.effect
      })
    },
    fetchServiceOfferings () {
      this.serviceOfferingLoading = true
      this.getMinimumResources().then(({ minCpu, minMemory }) => {
        return getAPI('listServiceOfferings', { cpunumber: minCpu, memory: minMemory }).then(json => {
          const items = json?.listserviceofferingsresponse?.serviceoffering || []
          this.serviceOfferings = items.filter(x => x.iscustomized === false)
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.serviceOfferingLoading = false
      })
    },
    // Offerings must satisfy the Kubernetes version requirements; when scaling, they can not be smaller than the current one
    async getMinimumResources () {
      let minCpu = 0
      let minMemory = 0
      if (this.resource.kubernetesversionid) {
        const json = await getAPI('listKubernetesSupportedVersions', { id: this.resource.kubernetesversionid })
        const version = json?.listkubernetessupportedversionsresponse?.kubernetessupportedversion?.[0]
        if (version) {
          minCpu = version.mincpunumber || 0
          minMemory = version.minmemory || 0
        }
      }
      if (this.mode === 'scale' && this.nodeGroup?.serviceofferingid) {
        const json = await getAPI('listServiceOfferings', { id: this.nodeGroup.serviceofferingid })
        const offering = json?.listserviceofferingsresponse?.serviceoffering?.[0]
        if (offering) {
          minCpu = Math.max(minCpu, offering.cpunumber || 0)
          minMemory = Math.max(minMemory, offering.memory || 0)
        }
      }
      return { minCpu, minMemory }
    },
    fetchCksTemplates () {
      const filters = isAdminOrDomainAdmin() ? ['all'] : ['self', 'featured', 'community']
      this.templateLoading = true
      const requests = filters.map(filter => getAPI('listTemplates', {
        templatefilter: filter,
        forcks: true,
        isready: true,
        zoneid: this.resource.zoneid
      }).then(json => json?.listtemplatesresponse?.template || []))
      Promise.all(requests).then(results => {
        const templates = {}
        results.flat().forEach(t => { templates[t.id] = t })
        this.templates = Object.values(templates)
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.templateLoading = false
      })
    },
    handleSubmit (e) {
      if (e?.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        if (this.mode === 'create') {
          this.createNodeGroup(values)
        } else if (this.mode === 'scale') {
          this.scaleNodeGroup(values)
        } else {
          this.updateNodeGroup()
        }
      }).catch(error => {
        if (error?.errorFields?.length > 0) {
          this.formRef.value.scrollToField(error.errorFields[0].name)
        }
      })
    },
    createNodeGroup (values) {
      const labels = this.normalizeLabels(this.labels)
      const taints = this.normalizeTaints(this.taints)
      if (!this.validatePairs(labels, taints) || !this.validateAutoscaling(values.size)) {
        return
      }
      const params = {
        clusterid: this.resource.id,
        name: values.name.trim(),
        serviceofferingid: values.serviceofferingid,
        size: values.size
      }
      if (values.templateid) {
        params.templateid = values.templateid
      }
      this.addLabelParams(params, labels)
      this.addTaintParams(params, taints)
      if (values.autoscalingenabled) {
        params.autoscalingenabled = true
        params.minsize = values.minsize
        params.maxsize = values.maxsize
      }
      this.submit('createKubernetesClusterNodeGroup', params, 'label.kubernetes.node.group.create',
        'message.success.create.kubernetes.node.group', params.name)
    },
    scaleNodeGroup (values) {
      const params = { id: this.nodeGroup.id }
      if (values.size !== this.nodeGroup.size) {
        params.size = values.size
      }
      if (values.serviceofferingid && values.serviceofferingid !== this.nodeGroup.serviceofferingid) {
        params.serviceofferingid = values.serviceofferingid
      }
      if (!params.size && params.size !== 0 && !params.serviceofferingid) {
        this.$message.info(this.$t('message.kubernetes.node.group.nothing.to.change'))
        return
      }
      this.submit('scaleKubernetesClusterNodeGroup', params, 'label.kubernetes.node.group.scale',
        'message.success.scale.kubernetes.node.group', this.nodeGroup.name)
    },
    updateNodeGroup () {
      const labels = this.normalizeLabels(this.labels)
      const taints = this.normalizeTaints(this.taints)
      if (!this.validatePairs(labels, taints) || !this.validateAutoscaling(this.nodeGroup.size)) {
        return
      }
      const params = { id: this.nodeGroup.id }
      if (JSON.stringify(labels) !== JSON.stringify(this.originalLabels)) {
        if (labels.length > 0) {
          this.addLabelParams(params, labels)
        } else {
          params.cleanupnodelabels = true
        }
      }
      if (JSON.stringify(taints) !== JSON.stringify(this.originalTaints)) {
        if (taints.length > 0) {
          this.addTaintParams(params, taints)
        } else {
          params.cleanupnodetaints = true
        }
      }
      const autoscalingEnabled = !!this.form.autoscalingenabled
      if (autoscalingEnabled !== !!this.nodeGroup.autoscalingenabled) {
        params.autoscalingenabled = autoscalingEnabled
      }
      if (autoscalingEnabled && (this.form.minsize !== this.nodeGroup.minsize || this.form.maxsize !== this.nodeGroup.maxsize)) {
        params.minsize = this.form.minsize
        params.maxsize = this.form.maxsize
      }
      if (Object.keys(params).length === 1) {
        this.$message.info(this.$t('message.kubernetes.node.group.nothing.to.change'))
        return
      }
      this.submit('updateKubernetesClusterNodeGroup', params, 'label.kubernetes.node.group.update',
        'message.success.update.kubernetes.node.group', this.nodeGroup.name)
    },
    submit (apiName, params, titleKey, successKey, name) {
      this.loading = true
      postAPI(apiName, params).then(json => {
        const jobId = json[apiName.toLowerCase() + 'response'].jobid
        this.$pollJob({
          jobId,
          title: this.$t(titleKey),
          description: name,
          loadingMessage: `${this.$t(titleKey)} ${name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: `${this.$t(successKey)} ${name}`,
          successMethod: () => {
            this.$emit('refresh-data')
          }
        })
        this.closeAction()
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
    width: 100%;
  }

  .pair-row {
    margin-bottom: 8px;
  }

  .pair-remove {
    font-size: 18px;
    line-height: 32px;
    cursor: pointer;
  }
</style>
