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
  <div class="form-layout">
    <a-form :form="form" layout="vertical">
      <a-form-item :label="$t('label.diskoffering')" v-if="resource.type !== 'ROOT'">
        <a-select
          v-decorator="['diskofferingid', {
            initialValue: selectedDiskOfferingId,
            rules: [{ required: true, message: `${this.$t('message.error.select')}` }]}]"
          :loading="loading"
          :placeholder="$t('label.diskoffering')"
          @change="id => (customDiskOffering = offerings.filter(x => x.id === id)[0].iscustomized || false)"
          :autoFocus="resource.type !== 'ROOT'"
        >
          <a-select-option
            v-for="(offering, index) in offerings"
            :value="offering.id"
            :key="index"
          >{{ offering.displaytext || offering.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <div v-if="customDiskOffering || resource.type === 'ROOT'">
        <a-form-item :label="$t('label.sizegb')">
          <a-input
            v-decorator="['size', {
              rules: [{ required: true, message: $t('message.error.size') }]}]"
            :placeholder="$t('label.disksize')"
            :autoFocus="customDiskOffering || resource.type === 'ROOT'"/>
        </a-form-item>
      </div>
      <a-form-item :label="$t('label.shrinkok')">
        <a-checkbox v-decorator="['shrinkok']" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>
<script>
import { api } from '@/api'

export default {
  name: 'ResizeVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      offerings: [],
      selectedDiskOfferingId: '',
      customDiskOffering: false,
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
      api('listDiskOfferings', {
        zoneid: this.resource.zoneid,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
        this.selectedDiskOfferingId = this.offerings[0].id || ''
        this.customDiskOffering = this.offerings[0].iscustomized || false
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
        values.id = this.resource.id
        api('resizeVolume', values).then(response => {
          this.$pollJob({
            jobId: response.resizevolumeresponse.jobid,
            successMessage: this.$t('message.success.resize.volume'),
            successMethod: () => {
              this.$store.dispatch('AddAsyncJob', {
                title: this.$t('message.success.resize.volume'),
                jobid: response.resizevolumeresponse.jobid,
                description: values.name,
                status: 'progress'
              })
            },
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
.action-button {
  text-align: right;
  button {
    margin-right: 5px;
  }
}
</style>
