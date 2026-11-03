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
          <a-input v-model:value="form.provider" :disabled=true />
        </a-form-item>

        <div v-if="form.provider === 'Cloudian HyperStore'">
          <!-- HyperStore Only Object Store Configuration -->
          <a-form-item name="url" ref="url" :label="$t('label.cloudian.admin.url')">
            <a-input v-model:value="form.url" placeholder="https://admin-hostname:19443" />
          </a-form-item>
          <a-form-item name="validateSSL" ref="validateSSL">
            <a-checkbox v-model:checked="form.validateSSL">Validate SSL Certificate</a-checkbox>
          </a-form-item>
          <a-form-item name="accesskey" ref="accesskey" :label="$t('label.cloudian.admin.username')">
            <!-- Use accessKey field for the username to make provider shared configuration easier -->
            <a-input v-model:value="form.accesskey" />
          </a-form-item>
          <a-form-item name="secretkey" ref="secretkey" :label="$t('label.cloudian.admin.password')">
            <!-- Use secretKey field for the password to make provider shared configuration easier -->
            <a-input-password v-model:value="form.secretkey" autocomplete="off"/>
          </a-form-item>
          <a-form-item name="s3Url" ref="s3Url" :label="$t('label.cloudian.s3.url')" :rules="[{ required: true, message: this.$t('label.required') }]">
            <a-input v-model:value="form.s3Url" placeholder="https://s3-hostname or http://s3-hostname"/>
          </a-form-item>
          <a-form-item name="iamUrl" ref="iamUrl" :label="$t('label.cloudian.iam.url')" :rules="[{ required: true, message: this.$t('label.required') }]">
            <a-input v-model:value="form.iamUrl" placeholder="https://iam-hostname:16443 or http://iam-hostname:16080"/>
          </a-form-item>
        </div>

        <div v-else>
          <!-- Generic Object Store Configuration -->
          <a-form-item name="url" ref="url" :label="$t('label.url')">
            <a-input v-model:value="form.url" />
          </a-form-item>
          <a-form-item name="accesskey" ref="accesskey" :label="$t('label.access.key')">
            <a-input v-model:value="form.accesskey" />
          </a-form-item>
          <a-form-item name="secretkey" ref="secretkey" :label="$t('label.secret.key')">
            <a-input-password v-model:value="form.secretkey" autocomplete="off"/>
          </a-form-item>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button :loading=loading type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'UpdateObjectStoragePool',
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
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateObjectStoragePool')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        url: [{ required: true, message: this.$t('label.required') }],
        name: [{ required: true, message: this.$t('label.required') }],
        accesskey: [{ required: true, message: this.$t('label.required') }],
        secretkey: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.loading = false
      this.fillEditFormFieldValues()
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.loading = true
      form.name = this.resource.name
      form.provider = this.resource.providername
      form.url = this.resource.url
      api('listResourceDetails', {
        resourceid: this.resource.id,
        resourcetype: 'objectstore'
      }).then(json => {
        for (const detail of json.listresourcedetailsresponse.resourcedetail) {
          if (detail.value) {
            if (detail.key === 'validateSSL') {
              form[detail.key] = (detail.value.toLowerCase() === 'true')
            } else {
              form[detail.key] = detail.value
            }
          }
        }
      }).catch(error => {
        console.log(error)
      })
      this.loading = false
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(async () => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          id: this.resource.id,
          name: values.name,
          provider: values.provider,
          url: values.url
        }

        data['details[0].key'] = 'accesskey'
        data['details[0].value'] = values.accesskey
        data['details[1].key'] = 'secretkey'
        data['details[1].value'] = values.secretkey

        if (data.provider === 'Cloudian HyperStore') {
          data['details[2].key'] = 'validateSSL'
          data['details[2].value'] = values.validateSSL
          data['details[3].key'] = 's3Url'
          data['details[3].value'] = values.s3Url
          data['details[4].key'] = 'iamUrl'
          data['details[4].value'] = values.iamUrl
        }

        this.loading = true

        api('updateObjectStoragePool', data).then(_response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.update.object.storage'),
            description: `${this.$t('message.success.update.object.storage')}`
          })
          this.closeModal()
        }).catch(error => {
          console.log(error)
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('refresh-data')
      this.$emit('close-action')
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
