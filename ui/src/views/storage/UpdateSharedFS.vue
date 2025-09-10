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
      <a-form-item name="name" ref="name" :label="$t('label.name')">
        <a-input
          v-model:value="form.name"
          :placeholder="$t('label.name')"/>
      </a-form-item>
      <a-form-item name="description" ref="description" :label="$t('label.description')">
        <a-input
          v-model:value="form.description"
          :placeholder="$t('label.description')"/>
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
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'updateSharedFS',
  mixins: [mixinForm],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    TooltipLabel
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateSharedFileSystem')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    closeModal () {
      this.$emit('close-action')
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

        fieldName = field.name
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
          name: values.name,
          description: values.description
        }
        console.log(data)
        console.log(this.form)
        this.loading = true
        api('updateSharedFileSystem', data).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.update.sharedfs'),
            description: `${this.$t('message.success.update.sharedfs')} ${data.name}`
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
  width: 85vw;

  @media (min-width: 760px) {
    width: 500px;
  }
}
</style>
