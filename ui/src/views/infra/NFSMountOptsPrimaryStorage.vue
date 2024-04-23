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
              <span v-html="$t('message.action.edit.nfs.options')"></span>
            </template>
          </a-alert>
          <p></p>
          <a-form-item name="nfsMountOpts" ref="nfsMountOpts" :label="$t('label.nfsmountopts')">
            <template #label>
              <tooltip-label :title="$t('label.nfsmountopts')" :tooltip="$t('message.nfs.nfsmountopts.description')"/>
            </template>
            <a-input v-model:value="form.nfsMountOpts" :placeholder="$t('message.nfs.nfsmountopts.description')" />
          </a-form-item>
          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button @click="handleSubmitForm" ref="submit" >{{ $t('label.ok') }}</a-button>
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
      loading: false
    }
  },
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({ })
      this.rules = reactive({
      })
    },
    handleSubmitForm () {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var params = {
          id: this.resource.id
        }
        params['details[0].nfsmountopts'] = values.nfsMountOpts

        this.updateStoragePool(params)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeAction () {
      this.$emit('close-action')
    },
    updateStoragePool (args) {
      api('updateStoragePool', args).then(json => {
        this.$message.success(`${this.$t('message.success.edit.nfsmountopts')}: ${this.resource.name}`)
        this.$emit('refresh-data')
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
