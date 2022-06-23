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
  <div class="take-snapshot" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading || actionLoading">
      <a-alert type="warning">
        <template #message>
          <div v-html="$t('label.header.volume.take.snapshot')" />
        </template>
      </a-alert>
      <a-form
        class="form"
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit"
       >
        <a-row :gutter="12">
          <a-col :md="24" :lg="24">
            <a-form-item :label="$t('label.name')" name="name" ref="name">
              <a-input
                v-model:value="form.name"
                :placeholder="apiParams.name.description"
                v-focus="true" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24" v-if="!supportsStorageSnapshot">
            <a-form-item :label="$t('label.asyncbackup')" name="asyncbackup" ref="asyncbackup">
              <a-switch v-model:checked="form.asyncbackup" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24" v-if="quiescevm" name="quiescevm" ref="quiescevm">
            <a-form-item :label="$t('label.quiescevm')">
              <a-switch v-model:checked="form.quiescevm" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider/>
        <div class="tagsTitle">{{ $t('label.tags') }}</div>
        <div>
          <div v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="true">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </div>
          <div v-if="inputVisible">
            <a-input-group
              type="text"
              size="small"
              @blur="handleInputConfirm"
              @keyup.enter="handleInputConfirm"
              compact>
              <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" :placeholder="$t('label.key')" />
              <a-input
                class="tag-disabled-input"
                style=" width: 30px; border-left: 0; pointer-events: none; text-align: center"
                placeholder="="
                disabled />
              <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
              <tooltip-button :tooltip="$t('label.ok')" icon="check-outlined" size="small" @onClick="handleInputConfirm" />
              <tooltip-button :tooltip="$t('label.cancel')" icon="close-outlined" size="small" @onClick="inputVisible=false" />
            </a-input-group>
          </div>
          <a-tag v-else @click="showInput" class="btn-add-tag" style="borderStyle: dashed;">
            <plus-outlined /> {{ $t('label.new.tag') }}
          </a-tag>
        </div>
        <div :span="24" class="action-button">
          <a-button
            :loading="actionLoading"
            @click="closeAction">
            {{ $t('label.cancel') }}
          </a-button>
          <a-button
            v-if="handleShowButton()"
            :loading="actionLoading"
            type="primary"
            ref="submit"
            @click="handleSubmit">
            {{ $t('label.ok') }}
          </a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'TakeSnapshot',
  mixins: [mixinForm],
  components: {
    TooltipButton
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      actionLoading: false,
      quiescevm: false,
      supportsStorageSnapshot: false,
      inputValue: '',
      inputKey: '',
      inputVisible: '',
      tags: [],
      dataSource: []
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createSnapshot')
  },
  created () {
    this.initForm()
    this.quiescevm = this.resource.quiescevm
    this.supportsStorageSnapshot = this.resource.supportsstoragesnapshot
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: undefined,
        asyncbackup: undefined,
        quiescevm: false
      })
      this.rules = reactive({})
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.actionLoading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        let params = {}
        params.volumeId = this.resource.id
        if (values.name) {
          params.name = values.name
        }
        params.asyncBackup = false
        if (values.asyncbackup) {
          params.asyncBackup = values.asyncbackup
        }
        params.quiescevm = false
        if (values.quiescevm) {
          params.quiescevm = values.quiescevm
        }
        for (let i = 0; i < this.tags.length; i++) {
          const formattedTagData = {}
          const tag = this.tags[i]
          formattedTagData['tags[' + i + '].key'] = tag.key
          formattedTagData['tags[' + i + '].value'] = tag.value
          params = Object.assign({}, params, formattedTagData)
        }

        this.actionLoading = true
        const title = this.$t('label.action.take.snapshot')
        const description = this.$t('label.volume') + ' ' + this.resource.id
        api('createSnapshot', params).then(json => {
          const jobId = json.createsnapshotresponse.jobid
          if (jobId) {
            this.$pollJob({
              jobId,
              title,
              description: values.name || this.resource.id,
              successMethod: result => {},
              loadingMessage: `${title} ${this.$t('label.in.progress.for')} ${description}`,
              catchMessage: this.$t('error.fetching.async.job.result')
            })
          }
          this.closeAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleVisibleInterval (intervalType) {
      if (this.dataSource.length === 0) {
        return false
      }
      const dataSource = this.dataSource.filter(item => item.intervaltype === intervalType)
      if (dataSource && dataSource.length > 0) {
        return true
      }
      return false
    },
    handleShowButton () {
      if (this.dataSource.length === 0) {
        return true
      }
      const dataSource = this.dataSource.filter(item => item.intervaltype === this.intervalValue)
      if (dataSource && dataSource.length > 0) {
        return false
      }
      return true
    },
    handleKeyChange (e) {
      this.inputKey = e.target.value
    },
    handleValueChange (e) {
      this.inputValue = e.target.value
    },
    handleInputConfirm () {
      this.tags.push({
        key: this.inputKey,
        value: this.inputValue
      })
      this.inputVisible = false
      this.inputKey = ''
      this.inputValue = ''
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="less" scoped>
.form {
  margin-top: 10px;
}

.take-snapshot {
  width: 85vw;

  @media (min-width: 760px) {
    width: 500px;
  }
}

.ant-tag {
  margin-bottom: 10px;
}

.ant-divider {
  margin-top: 0;
}

.tagsTitle {
  font-weight: 500;
  margin-bottom: 12px;
}
</style>
