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
      :ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      class="form"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >

      <a-form-item name="zoneid" ref="zoneid" class="form__item" :label="$t('label.zone')">
        <a-select
          v-model:value="form.zoneid"
          v-focus="true"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="zone in zonesList"
            :value="zone.id"
            :key="zone.id"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px" />
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item name="name" ref="name" class="form__item" :label="$t('label.podname')">
        <a-input
          :placeholder="placeholder.name"
          v-model:value="form.name"
        />
      </a-form-item>

      <a-form-item name="gateway" ref="gateway" class="form__item" :label="$t('label.reservedsystemgateway')">
        <a-input
          :placeholder="placeholder.gateway"
          v-model:value="form.gateway"
        />
      </a-form-item>

      <a-form-item name="netmask" ref="netmask" class="form__item" :label="$t('label.reservedsystemnetmask')">
        <a-input
          :placeholder="placeholder.netmask"
          v-model:value="form.netmask"
        />
      </a-form-item>

      <a-form-item name="startip" ref="startip" class="form__item" :label="$t('label.reservedsystemstartip')">
        <a-input
          :placeholder="placeholder.startip"
          v-model:value="form.startip"
        />
      </a-form-item>

      <a-form-item name="endip" ref="endip" class="form__item" :label="$t('label.reservedsystemendip')">
        <a-input
          :placeholder="placeholder.endip"
          v-model:value="form.endip"
        />
      </a-form-item>

      <div class="form__item">
        <div class="form__label">{{ $t('label.isdedicated') }}</div>
        <a-checkbox @change="toggleDedicate" />
      </div>

      <template v-if="showDedicated">
        <DedicateDomain
          @domainChange="id => dedicatedDomainId = id"
          @accountChange="id => dedicatedAccount = id"
          :error="domainError" />
      </template>

      <a-divider />

      <div :span="24" class="action-button">
        <a-button @click="() => $emit('close-action')">{{ $t('label.cancel') }}</a-button>
        <a-button @click="handleSubmit" ref="submit" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import DedicateDomain from '../../components/view/DedicateDomain'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ClusterAdd',
  components: {
    DedicateDomain,
    ResourceIcon
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      zonesList: [],
      showDedicated: false,
      dedicatedDomainId: null,
      dedicatedAccount: null,
      domainError: false,
      params: [],
      placeholder: {
        name: null,
        gateway: null,
        netmask: null,
        startip: null,
        endip: null
      }
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
        zoneid: [{ required: true, message: this.$t('label.required') }],
        name: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        startip: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.fetchZones()
    },
    fetchZones () {
      this.loading = true
      api('listZones', { showicon: true }).then(response => {
        this.zonesList = response.listzonesresponse.zone || []
        this.form.zoneid = this.zonesList[0].id
        this.params = this.$store.getters.apis.createPod.params
        Object.keys(this.placeholder).forEach(item => { this.returnPlaceholder(item) })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    toggleDedicate () {
      this.dedicatedDomainId = null
      this.dedicatedAccount = null
      this.showDedicated = !this.showDedicated
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        this.loading = true
        api('createPod', {
          zoneId: values.zoneid,
          name: values.name,
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip
        }).then(response => {
          const pod = response.createpodresponse.pod || {}
          if (pod.id && this.showDedicated) {
            this.dedicatePod(pod.id)
          }
          this.loading = false
          this.parentFetchData()
          this.$emit('close-action')
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createpodresponse.errortext,
            duration: 0
          })
          this.loading = false
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    dedicatePod (podId) {
      this.loading = true
      api('dedicatePod', {
        podId,
        domainid: this.dedicatedDomainId,
        account: this.dedicatedAccount
      }).then(response => {
        this.$pollJob({
          jobId: response.dedicatepodresponse.jobid,
          title: this.$t('message.pod.dedicated'),
          description: `${this.$t('label.domainid')} : ${this.dedicatedDomainId}`,
          successMessage: this.$t('message.pod.dedicated'),
          successMethod: () => {
            this.loading = false
          },
          errorMessage: this.$t('error.dedicate.pod.failed'),
          errorMethod: () => {
            this.loading = false
          },
          loadingMessage: this.$t('message.dedicating.pod'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.loading = false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `${this.$t('label.error')} ${error.response.status}`,
          description: error.response.data.errorresponse.errortext,
          duration: 0
        })
        this.loading = false
      })
    },
    returnPlaceholder (field) {
      this.params.find(i => {
        if (i.name === field) this.placeholder[field] = i.description
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .form {

    &__label {
      margin-bottom: 5px;
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

      &--visible {
        display: block;
      }

    }

  }
</style>
