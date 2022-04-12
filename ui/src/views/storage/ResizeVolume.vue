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
      <a-form-item name="size" ref="size" :label="$t('label.sizegb')">
        <a-input
          v-model:value="form.size"
          :placeholder="$t('label.disksize')"/>
      </a-form-item>
      <div v-if="customDiskOfferingIops">
        <a-form-item name="miniops" ref="miniops" :label="$t('label.miniops')">
          <a-input
            v-model:value="form.miniops"
            :placeholder="$t('label.miniops')"/>
        </a-form-item>
        <a-form-item name="maxiops" ref="maxiops" :label="$t('label.maxiops')">
          <a-input
            v-model:value="form.maxiops"
            :placeholder="$t('label.maxiops')"/>
        </a-form-item>
      </div>
      <a-form-item name="shrinkOk" ref="shrinkOk" :label="$t('label.shrinkok')" v-if="!['XenServer'].includes(resource.hypervisor)">
        <a-switch
          v-model:checked="form.shrinkOk"
          :checked="shrinkOk"
          @change="val => { shrinkOk = val }"/>
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
  name: 'ResizeVolume',
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
      this.form = reactive({})
      this.rules = reactive({
        size: [{ required: true, message: this.$t('message.error.size') }],
        miniops: [{ required: true, message: this.$t('message.error.number') }],
        maxiops: [{ required: true, message: this.$t('message.error.number') }]
      })
    },
    fetchData () {
      this.loading = true
      api('listDiskOfferings', {
        zoneid: this.resource.zoneid,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
        this.form.diskofferingid = this.offerings[0].id || ''
        this.customDiskOffering = this.offerings[0].iscustomized || false
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        this.loading = true
        values.id = this.resource.id
        api('resizeVolume', values).then(response => {
          this.$pollJob({
            jobId: response.resizevolumeresponse.jobid,
            title: this.$t('message.success.resize.volume'),
            description: values.name,
            successMessage: this.$t('message.success.resize.volume'),
            successMethod: () => {},
            errorMessage: this.$t('message.resize.volume.failed'),
            errorMethod: () => {
              this.closeModal()
            },
            loadingMessage: `Volume resize is in progress`,
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.loading = false
              this.closeModal()
            }
          })
          this.closeModal()
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.errorresponse.errortext,
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
