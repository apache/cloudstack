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
    <a-form
      class="form"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
      layout="vertical"
     >
      <a-form-item :label="$t('label.name')" name="name" ref="name">
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="$t('label.snapshot.name')"/>
      </a-form-item>
      <a-form-item :label="$t('label.volume')" name="volumeid" ref="volumeid">
        <a-select
          v-model:value="form.volumeid"
          :loading="loading"
          @change="id => (volumes.filter(x => x.id === id))"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(volume, index) in volumes"
            :value="volume.id"
            :key="index"
            :label="volume.displaytext || volume.name">
            {{ volume.displaytext || volume.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'CreateSnapshotFromVMSnapshot',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      volumes: [],
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
        name: [{ required: true, message: this.$t('message.error.name') }],
        volumeid: [{ required: true, message: this.$t('message.error.select') }]
      })
    },
    fetchData () {
      this.loading = true
      api('listVolumes', {
        virtualmachineid: this.resource.virtualmachineid,
        listall: true
      }).then(json => {
        this.volumes = json.listvolumesresponse.volume || []
        this.form.volumeid = this.volumes[0].id || ''
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.loading = true
        api('createSnapshotFromVMSnapshot', {
          name: values.name,
          volumeid: values.volumeid,
          vmsnapshotid: this.resource.id
        }).then(response => {
          this.$pollJob({
            jobId: response.createsnapshotfromvmsnapshotresponse.jobid,
            title: this.$t('message.success.create.snapshot.from.vmsnapshot'),
            description: values.name,
            successMessage: this.$t('message.success.create.snapshot.from.vmsnapshot'),
            errorMessage: this.$t('message.create.snapshot.from.vmsnapshot.failed'),
            loadingMessage: this.$t('message.create.snapshot.from.vmsnapshot.progress'),
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
    },
    closeModal () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
</style>
