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
        layout="vertical"
        @finish="handleSubmit"
       >
        <div style="margin-bottom: 10px">
          <a-alert type="warning">
            <template #message>
              <div v-html="$t('message.backup.create')"></div>
            </template>
          </a-alert>
        </div>
        <div v-if="canSetNameAndDescription">
          <a-form-item name="name" ref="name" :label="$t('label.name')">
            <a-input v-model:value="form.name" v-focus="true" />
          </a-form-item>
          <a-form-item name="description" ref="description" :label="$t('label.description')">
            <a-input v-model:value="form.description"/>
          </a-form-item>
        </div>
        <a-form-item v-if="provider === 'nas'" name="quiescevm" ref="quiescevm">
          <a-switch v-model:checked="form.quiescevm" />
          <template #label>
            <tooltip-label :title="$t('label.quiescevm')" :tooltip="apiParams.quiescevm.description"/>
          </template>
        </a-form-item>
      </a-form>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'StartBackup',
  mixins: [mixinForm],
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
      provider: null,
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createBackup')
  },
  created () {
    this.initForm()
    this.getBackupProvider()
  },
  computed: {
    canSetNameAndDescription () {
      return ['nas', 'dummy'].includes(this.provider)
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
    },
    getBackupProvider () {
      this.loading = true
      getAPI('listBackupOfferings', { id: this.resource.backupofferingid }).then(json => {
        this.provider = json.listbackupofferingsresponse.backupoffering[0].provider
        console.log('this.provider', this.provider)
      }).finally(() => {
        this.loading = false
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          virtualmachineid: this.resource.id,
          name: this.form.name,
          description: this.form.description,
          quiescevm: this.form.quiescevm
        }
        this.loading = true
        postAPI('createBackup', data).then(response => {
          this.$pollJob({
            jobId: response.createbackupresponse.jobid,
            title: this.$t('label.create.bucket'),
            description: values.name,
            errorMessage: this.$t('message.create.backup.failed'),
            loadingMessage: `${this.$t('label.create.backup')}: ${this.resource.name || this.resource.id}`,
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}

</script>
<style lang="scss" scoped>
.form-layout {
  width: 80vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
