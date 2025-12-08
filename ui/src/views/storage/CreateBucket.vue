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
        <a-form-item name="objectstore" ref="objectstore" :label="$t('label.object.storage')">
          <a-select
            v-model:value="form.objectstore"
            @change="val => { form.objectstore = val }"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option :value="objectstore.id" v-for="objectstore in objectstores" :key="objectstore.id" :label="objectstore.name">
              {{ objectstore.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
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
        <a-form-item name="objectlocking" ref="objectlocking" :label="$t('label.objectlocking')">
          <a-switch
            v-model:checked="form.objectlocking"
            :checked="objectlocking"
            @change="val => { objectlocking = val }"/>
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
          <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
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
  name: 'CreateBucket',
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
      loading: false
    }
  },
  created () {
    this.initForm()
    this.policyList = ['Public', 'Private']
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
      })
      this.rules = reactive({
        name: [{ required: true, message: this.$t('label.required') }],
        objectstore: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.listObjectStores()
    },
    listObjectStores () {
      this.loading = true
      api('listObjectStoragePools').then(json => {
        this.objectstores = json.listobjectstoragepoolsresponse.objectstore || []
        if (this.objectstores.length > 0) {
          this.form.objectstore = this.objectstores[0].id
        }
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
          name: values.name,
          objectstorageid: values.objectstore,
          quota: values.quota,
          encryption: values.encryption,
          versioning: values.versioning,
          objectlocking: values.objectlocking,
          policy: values.policy
        }
        this.loading = true
        api('createBucket', data).then(response => {
          this.$pollJob({
            jobId: response.createbucketresponse.jobid,
            title: this.$t('label.create.bucket'),
            description: values.name,
            successMessage: this.$t('message.success.create.bucket'),
            errorMessage: this.$t('message.create.bucket.failed'),
            loadingMessage: this.$t('message.create.bucket.processing'),
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
  width: 85vw;

  @media (min-width: 1000px) {
    width: 35vw;
  }
}
</style>
