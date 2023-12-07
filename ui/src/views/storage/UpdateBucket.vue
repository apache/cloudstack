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
    <a-form
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      @finish="handleSubmit"
    >
      <a-form-item name="quota" ref="quota" :label="$t('label.quotagb')">
        <a-input
          v-model:value="form.quota"
          :placeholder="$t('label.quota')"/>
      </a-form-item>
      <a-form-item name="encryption" ref="encryption" :label="$t('label.encryption')">
        <a-switch
          v-model:checked="form.encryption"
          :checked="encryption"
          @change="val => { encryption = val }"/>
      </a-form-item>
      <a-form-item name="versioning" ref="versioning" :label="$t('label.versioning')">
        <a-switch
          v-model:checked="form.versioning"
          :checked="versioning"
          @change="val => { versioning = val }"/>
      </a-form-item>
      <a-form-item name="Bucket Policy" ref="policy" :label="$t('label.bucket.policy')">
        <a-select
          v-model:value="form.policy"
          @change="val => { form.policy = val }"
          showSearch
          optionFilterProp="value"
          :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
          <a-select-option
            :value="policy"
            v-for="(policy,idx) in policyList"
            :key="idx"
          >{{ policy }}</a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>
<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'

export default {
  name: 'updateBucket',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      offerings: [],
      customDiskOffering: false,
      loading: false,
      customDiskOfferingIops: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateBucket')
  },
  created () {
    this.initForm()
    this.policyList = ['Public', 'Private']
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
      })
    },
    fetchData () {
      this.loading = false
      this.fillEditFormFieldValues()
    },
    fillEditFormFieldValues () {
      const form = this.form
      this.loading = true
      Object.keys(this.apiParams).forEach(item => {
        const field = this.apiParams[item]
        let fieldValue = null
        let fieldName = null

        if (field.type === 'list' || field.name === 'account') {
          fieldName = field.name.replace('ids', 'name').replace('id', 'name')
        } else {
          fieldName = field.name
        }
        fieldValue = this.resource[fieldName] ? this.resource[fieldName] : null
        if (fieldValue) {
          form[field.name] = fieldValue
        }
      })
      this.loading = false
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        var data = {
          id: this.resource.id,
          quota: values.quota,
          encryption: values.encryption,
          versioning: values.versioning,
          objectlocking: values.objectlocking,
          policy: values.policy
        }

        this.loading = true
        api('updateBucket', data).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.bucket.update'),
            description: `${this.$t('message.success.update.bucket')}`
          })
          this.closeModal()
        }).catch(error => {
          console.log(error)
          this.$notification.error({
            message: `${this.$t('label.bucket.update')} ${this.$t('label.error')}`,
            description: error.response.data.updatebucketresponse.errortext,
            duration: 0
          })
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

  @media (min-width: 760px) {
    width: 500px;
  }
}
</style>
