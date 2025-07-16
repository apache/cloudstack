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
        <a-form-item name="name" ref="name" :label="$t('label.name')">
          <a-input v-model:value="form.name" v-focus="true" />
        </a-form-item>
        <a-form-item name="provider" ref="provider" :label="$t('label.providername')">
          <a-select
            v-model:value="form.provider"
            @change="val => { form.provider = val }"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              :value="prov"
              v-for="(prov,idx) in providers"
              :key="idx"
            >{{ prov }}</a-select-option>
          </a-select>
        </a-form-item>

        <div v-if="form.provider === 'Cloudian HyperStore'">
          <!-- HyperStore Only Object Store Configuration -->
          <a-form-item name="url" ref="url" :label="$t('label.cloudian.admin.url')">
            <a-input v-model:value="form.url" placeholder="https://admin-hostname:19443" />
          </a-form-item>
          <a-form-item name="validateSSL" ref="validateSSL">
            <a-checkbox v-model:checked="form.validateSSL">Validate SSL Certificate</a-checkbox>
          </a-form-item>
          <a-form-item name="accessKey" ref="accessKey" :label="$t('label.cloudian.admin.username')">
            <!-- Use accessKey field for the username to make provider shared configuration easier -->
            <a-input v-model:value="form.accessKey" />
          </a-form-item>
          <a-form-item name="secretKey" ref="secretKey" :label="$t('label.cloudian.admin.password')">
            <!-- Use secretKey field for the password to make provider shared configuration easier -->
            <a-input-password v-model:value="form.secretKey" autocomplete="off"/>
          </a-form-item>
          <a-form-item name="s3Url" ref="s3Url" :label="$t('label.cloudian.s3.url')" :rules="[{ required: true, message: this.$t('label.required') }]">
            <a-input v-model:value="form.s3Url" placeholder="https://s3-hostname or http://s3-hostname"/>
          </a-form-item>
          <a-form-item name="iamUrl" ref="iamUrl" :label="$t('label.cloudian.iam.url')" :rules="[{ required: true, message: this.$t('label.required') }]">
            <a-input v-model:value="form.iamUrl" placeholder="https://iam-hostname:16443 or http://iam-hostname:16080"/>
          </a-form-item>
        </div>

        <div v-else>
          <!-- Non-HyperStore Object Stores -->
          <a-form-item name="url" ref="url" :label="$t('label.url')">
            <a-input v-model:value="form.url" />
          </a-form-item>
          <a-form-item name="accessKey" ref="accessKey" :label="$t('label.access.key')">
            <a-input v-model:value="form.accessKey" />
          </a-form-item>
          <a-form-item name="secretKey" ref="secretKey" :label="$t('label.secret.key')">
            <a-input-password v-model:value="form.secretKey" autocomplete="off"/>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'AddObjectStorage',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon
  },
  inject: ['parentFetchData'],
  data () {
    return {
      providers: ['MinIO', 'Ceph', 'Cloudian HyperStore', 'Simulator'],
      zones: [],
      loading: false
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        provider: 'MinIO',
        validateSSL: true
      })
      this.rules = reactive({
        url: [{ required: true, message: this.$t('label.required') }],
        name: [{ required: true, message: this.$t('label.required') }],
        accessKey: [{ required: true, message: this.$t('label.required') }],
        secretKey: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
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
          name: values.name
        }
        var provider = values.provider

        data.provider = provider
        data.url = values.url
        data['details[0].key'] = 'accesskey'
        data['details[0].value'] = values.accessKey
        data['details[1].key'] = 'secretkey'
        data['details[1].value'] = values.secretKey

        if (provider === 'Cloudian HyperStore') {
          data['details[2].key'] = 'validateSSL'
          data['details[2].value'] = values.validateSSL
          data['details[3].key'] = 's3Url'
          data['details[3].value'] = values.s3Url
          data['details[4].key'] = 'iamUrl'
          data['details[4].value'] = values.iamUrl
        }

        this.loading = true

        try {
          await this.addObjectStore(data)

          this.$notification.success({
            message: this.$t('label.add.object.storage'),
            description: this.$t('message.success.add.object.storage')
          })
          this.loading = false
          this.closeModal()
          this.parentFetchData()
        } catch (error) {
          this.$notifyError(error)
          this.loading = false
        }
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    addObjectStore (params) {
      return new Promise((resolve, reject) => {
        getAPI('addObjectStoragePool', params).then(json => {
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.form-layout {
  width: 85vw;

  @media (min-width: 1000px) {
    width: 35vw;
  }
}
</style>
