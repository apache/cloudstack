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
  <div class="take-snapshot">
    <a-spin :spinning="loading || actionLoading">
      <a-alert type="warning">
        <span slot="message" v-html="$t('label.header.volume.take.snapshot')" />
      </a-alert>
      <a-form
        class="form"
        :form="form"
        layout="vertical"
        @submit="handleSubmit">
        <a-row :gutter="12">
          <a-col :md="24" :lg="24">
            <a-form-item :label="$t('label.name')">
              <a-input
                v-decorator="['name']"
                :placeholder="apiParams.name.description"
                autoFocus />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24" v-if="!supportsStorageSnapshot">
            <a-form-item :label="$t('label.asyncbackup')">
              <a-switch v-decorator="['asyncbackup']" />
            </a-form-item>
          </a-col>
          <a-col :md="24" :lg="24" v-if="quiescevm">
            <a-form-item :label="$t('label.quiescevm')">
              <a-switch v-decorator="['quiescevm']" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider/>
        <div class="tagsTitle">{{ $t('label.tags') }}</div>
        <div>
          <template v-for="(tag, index) in tags">
            <a-tag :key="index" :closable="true">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </template>
          <div v-if="inputVisible">
            <a-input-group
              type="text"
              size="small"
              @blur="handleInputConfirm"
              @keyup.enter="handleInputConfirm"
              compact>
              <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" :placeholder="$t('label.key')" />
              <a-input style=" width: 30px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
              <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
              <tooltip-button :tooltip="$t('label.ok')" icon="check" size="small" @click="handleInputConfirm" />
              <tooltip-button :tooltip="$t('label.cancel')" icon="close" size="small" @click="inputVisible=false" />
            </a-input-group>
          </div>
          <a-tag v-else @click="showInput" style="background: #fff; borderStyle: dashed;">
            <a-icon type="plus" /> {{ $t('label.new.tag') }}
          </a-tag>
        </div>
        <div :span="24" class="action-button">
          <a-button
            :loading="actionLoading"
            @click="closeAction">
            {{ this.$t('label.cancel') }}
          </a-button>
          <a-button
            v-if="handleShowButton()"
            :loading="actionLoading"
            type="primary"
            @click="handleSubmit">
            {{ this.$t('label.ok') }}
          </a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'TakeSnapshot',
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
    this.form = this.$form.createForm(this)
    this.apiConfig = this.$store.getters.apis.createSnapshot || {}
    this.apiParams = {}
    this.apiConfig.params.forEach(param => {
      this.apiParams[param.name] = param
    })
  },
  mounted () {
    this.quiescevm = this.resource.quiescevm
    this.supportsStorageSnapshot = this.resource.supportsstoragesnapshot
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

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
              successMethod: result => {
                const successDescription = result.jobresult.snapshot.name
                this.$store.dispatch('AddAsyncJob', {
                  title: title,
                  jobid: jobId,
                  description: successDescription,
                  status: 'progress'
                })
              },
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
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
