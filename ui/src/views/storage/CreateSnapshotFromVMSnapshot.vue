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
    <a-form class="form" :form="form" @submit="handleSubmit" layout="vertical">
      <a-form-item :label="$t('label.name')">
        <a-input
          autoFocus
          v-decorator="['name', {
            rules: [{ required: true, message: $t('message.error.name') }]
          }]"
          :placeholder="$t('label.snapshot.name')"/>
      </a-form-item>
      <a-form-item :label="$t('label.volume')">
        <a-select
          v-decorator="['volumeid', {
            initialValue: selectedVolumeId,
            rules: [{ required: true, message: $t('message.error.select') }]}]"
          :loading="loading"
          @change="id => (volumes.filter(x => x.id === id))"
        >
          <a-select-option
            v-for="(volume, index) in volumes"
            :value="volume.id"
            :key="index">
            {{ volume.displaytext || volume.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
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
      selectedVolumeId: '',
      loading: false
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listVolumes', {
        virtualmachineid: this.resource.virtualmachineid,
        listall: true
      }).then(json => {
        this.volumes = json.listvolumesresponse.volume || []
        this.selectedVolumeId = this.volumes[0].id || ''
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        api('createSnapshotFromVMSnapshot', {
          name: values.name,
          volumeid: values.volumeid,
          vmsnapshotid: this.resource.id
        }).then(response => {
          this.$pollJob({
            jobId: response.createsnapshotfromvmsnapshotresponse.jobid,
            successMessage: this.$t('message.success.create.snapshot.from.vmsnapshot'),
            successMethod: () => {
              this.$store.dispatch('AddAsyncJob', {
                title: this.$t('message.success.create.snapshot.from.vmsnapshot'),
                jobid: response.createsnapshotfromvmsnapshotresponse.jobid,
                description: values.name,
                status: 'progress'
              })
              this.$emit('refresh-data')
            },
            errorMessage: this.$t('message.create.snapshot.from.vmsnapshot.failed'),
            errorMethod: () => {
              this.$emit('refresh-data')
            },
            loadingMessage: this.$t('message.create.snapshot.from.vmsnapshot.progress'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.$emit('refresh-data')
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
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

.action-button {
  text-align: right;

  button {
    margin-right: 5px;
  }
}
</style>
