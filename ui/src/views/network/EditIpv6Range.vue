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
      <a-form :form="form" :loading="loading" @submit="handleSubmit" layout="vertical">
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.ip6cidr')" :tooltip="apiParams.ip6cidr.description"/>
          <a-input
            v-decorator="['ip6cidr', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.ip6cidr.description"
            autoFocus />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.ip6gateway')" :tooltip="apiParams.ip6gateway.description"/>
          <a-input
            v-decorator="['ip6gateway', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.ip6gateway.description" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.routeripv6')" :tooltip="apiParams.routeripv6.description"/>
          <a-input
            v-decorator="['routeripv6']"
            :placeholder="apiParams.routeripv6.description" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.routeripv6gateway')" :tooltip="apiParams.routeripv6gateway.description"/>
          <a-input
            v-decorator="['routeripv6gateway', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.routeripv6gateway.description" />
        </a-form-item>
        <a-form-item>
          <tooltip-label slot="label" :title="$t('label.routeripv6vlan')" :tooltip="apiParams.routeripv6vlan.description"/>
          <a-input
            v-decorator="['routeripv6vlan', {
              rules: [{ required: true, message: $t('message.error.required.input') }]
            }]"
            :placeholder="apiParams.routeripv6vlan.description" />
        </a-form-item>
        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'EditIpv6Range',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    currentAction: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      rangeId: null
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('updateIpv6Range')
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.rangeId = this.$route.params.id || null
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
          form.getFieldDecorator(field.name, { initialValue: fieldValue })
        }
      })
      this.loading = false
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.form.validateFields((err, values) => {
        if (err) {
          return
        }
        this.loading = true
        const params = {
          id: this.rangeId,
          ip6cidr: values.ip6cidr,
          ip6gateway: values.ip6gateway,
          routeripv6: values.routeripv6,
          routeripv6gateway: values.routeripv6gateway,
          routeripv6vlan: values.routeripv6vlan
        }
        api('updateIpv6Range', params).then(response => {
          this.$emit('refresh-data')
          this.$notification.success({
            message: this.$t('label.update.ipv6.range'),
            description: `${this.$t('message.success.update.ipv6.range')} ${params.ip6cidr}`
          })
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: this.$t('message.request.failed'),
            description: (error.response && error.response.headers && error.response.headers['x-description']) || error.message,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;
  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
