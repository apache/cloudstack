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
      <a-form layout="vertical">
        <a-form-item v-if="hasInherited">
          <template #label>
            <tooltip-label :title="$t('label.kubernetes.node.inherited')" :tooltip="$t('message.kubernetes.node.inherited')"/>
          </template>
          <a-tag v-for="(value, key) in (nodeGroup.nodelabels || {})" :key="'label-' + key" class="pair-tag">{{ key }}={{ value }}</a-tag>
          <a-tag v-for="taint in (nodeGroup.nodetaints || [])" :key="'taint-' + taint" class="pair-tag">{{ taint }}</a-tag>
        </a-form-item>
        <a-form-item>
          <template #label>
            <tooltip-label :title="$t('label.kubernetes.node.labels')" :tooltip="$t('message.kubernetes.node.labels')"/>
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
              :tooltip="$t(isDefault ? 'message.kubernetes.node.default.taints' : 'message.kubernetes.node.taints')"/>
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
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

const TAINT_EFFECTS = ['NoSchedule', 'PreferNoSchedule', 'NoExecute']

// Edits the Kubernetes labels and taints of a node of a node group, in addition to the labels and taints of its node group
export default {
  name: 'KubernetesNodeLabelsForm',
  components: {
    TooltipLabel
  },
  props: {
    // the node group of the node
    nodeGroup: {
      type: Object,
      required: true
    },
    // the node, as returned in the nodes of the node group
    node: {
      type: Object,
      required: true
    }
  },
  emits: ['close-action', 'refresh-data'],
  data () {
    return {
      loading: false,
      labels: [],
      taints: []
    }
  },
  computed: {
    isDefault () {
      return !!this.nodeGroup.isdefault
    },
    // The nodes of the default node group have to accept the system pods
    availableTaintEffects () {
      return this.isDefault ? ['PreferNoSchedule'] : TAINT_EFFECTS
    },
    hasInherited () {
      return Object.keys(this.nodeGroup.nodelabels || {}).length > 0 || (this.nodeGroup.nodetaints || []).length > 0
    }
  },
  created () {
    this.labels = Object.entries(this.node.nodelabels || {}).map(([key, value]) => ({ key, value }))
    this.taints = (this.node.nodetaints || []).map(this.parseTaint)
    this.originalLabels = this.normalizeLabels(this.labels)
    this.originalTaints = this.normalizeTaints(this.taints)
  },
  methods: {
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
    handleSubmit (e) {
      if (e?.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return
      const labels = this.normalizeLabels(this.labels)
      const taints = this.normalizeTaints(this.taints)
      if (labels.some(l => !l.key) || taints.some(t => !t.key)) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.kubernetes.node.group.key.required')
        })
        return
      }
      const params = { id: this.node.id }
      if (JSON.stringify(labels) !== JSON.stringify(this.originalLabels)) {
        if (labels.length > 0) {
          labels.forEach((label, idx) => {
            params['nodelabels[' + idx + '].key'] = label.key
            params['nodelabels[' + idx + '].value'] = label.value
          })
        } else {
          params.cleanupnodelabels = true
        }
      }
      if (JSON.stringify(taints) !== JSON.stringify(this.originalTaints)) {
        if (taints.length > 0) {
          taints.forEach((taint, idx) => {
            params['nodetaints[' + idx + '].key'] = taint.key
            params['nodetaints[' + idx + '].value'] = taint.value
            params['nodetaints[' + idx + '].effect'] = taint.effect
          })
        } else {
          params.cleanupnodetaints = true
        }
      }
      if (Object.keys(params).length === 1) {
        this.$message.info(this.$t('message.kubernetes.node.group.nothing.to.change'))
        return
      }
      const title = this.$t('label.kubernetes.node.update')
      this.loading = true
      postAPI('updateKubernetesClusterNode', params).then(json => {
        const jobId = json.updatekubernetesclusternoderesponse.jobid
        this.$pollJob({
          jobId,
          title,
          description: this.node.name,
          loadingMessage: `${title} ${this.node.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: `${this.$t('message.success.update.kubernetes.node')} ${this.node.name}`,
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

  .pair-tag {
    margin-bottom: 4px;
  }

  .pair-remove {
    font-size: 18px;
    line-height: 32px;
    cursor: pointer;
  }
</style>
