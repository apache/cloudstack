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
  <div>
    <a-card
      class="ant-form-text"
      style="text-align: justify; margin: 10px 0; padding: 24px;"
      v-html="$t(description)">
    </a-card>
    <a-table
      bordered
      :scroll="{ x: 500 }"
      :dataSource="ipRanges"
      :columns="columns"
      :pagination="false"
      style="margin-bottom: 24px; width: 100%" >
      <template slot="actions" slot-scope="text, record">
        <tooltip-button :tooltip="$t('label.delete')" type="danger" icon="delete" @click="onDelete(record.key)" />
      </template>
      <template slot="footer">
        <a-form
          :layout="isMobile() ? 'horizontal': 'inline'"
          :form="form"
          @submit="handleAddRange">
          <a-row :gutter="12">
            <a-col :md="4" :lg="4">
              <a-form-item>
                <a-input
                  v-decorator="[ 'gateway', {
                    rules: [{ required: true, message: $t('message.error.gateway') }]
                  }]"
                  :placeholder="$t('label.gateway')"
                  autoFocus
                />
              </a-form-item>
            </a-col>
            <a-col :md="4" :lg="4">
              <a-form-item>
                <a-input
                  v-decorator="[ 'netmask', {
                    rules: [{ required: true, message: $t('message.error.netmask') }]
                  }]"
                  :placeholder="$t('label.netmask')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="4" :lg="4">
              <a-form-item>
                <a-input
                  v-decorator="[ 'vlan', { rules: [{ required: false }] }]"
                  :placeholder="$t('label.vlan')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="4" :lg="4">
              <a-form-item>
                <a-input
                  v-decorator="[ 'startIp', {
                    rules: [
                      {
                        required: true,
                        message: $t('message.error.startip')
                      },
                      {
                        validator: checkIpFormat,
                        ipV4: true,
                        message: $t('message.error.ipv4.address')
                      }
                    ]
                  }]"
                  :placeholder="$t('label.start.ip')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="4" :lg="4">
              <a-form-item>
                <a-input
                  v-decorator="[ 'endIp', {
                    rules: [
                      {
                        required: true,
                        message: $t('message.error.endip')
                      },
                      {
                        validator: checkIpFormat,
                        ipV4: true,
                        message: $t('message.error.ipv4.address')
                      }]
                  }]"
                  :placeholder="$t('label.end.ip')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="4" :lg="4">
              <a-form-item :style="{ display: 'inline-block', float: 'right' }">
                <a-button type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>
    </a-table>
    <div class="form-action">
      <a-button
        v-if="!isFixError"
        class="button-prev"
        @click="handleBack">
        {{ $t('label.previous') }}
      </a-button>
      <a-button class="button-next" type="primary" @click="handleSubmit">
        {{ $t('label.next') }}
      </a-button>
    </div>
    <a-modal
      :visible="showError"
      :maskClosable="false"
      :title="`${$t('label.error')}!`"
      :okText="$t('label.ok')"
      :cancelText="$t('label.cancel')"
      @ok="() => { showError = false }"
      @cancel="() => { showError = false }"
      centered
    >
      <span>{{ $t('message.required.add.least.ip') }}</span>
    </a-modal>
  </div>
</template>
<script>

import TooltipButton from '@/components/view/TooltipButton'
import { mixinDevice } from '@/utils/mixin.js'

export default {
  components: {
    TooltipButton
  },
  mixins: [mixinDevice],
  props: {
    traffic: {
      type: String,
      default: '0'
    },
    description: {
      type: String,
      default: 'label.creating.iprange'
    },
    prefillContent: {
      type: Object,
      default: function () {
        return {}
      }
    },
    isFixError: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formItemLayout: {
        wrapperCol: { span: 0 }
      },
      ipRanges: [],
      columns: [
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway',
          width: 150
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask',
          width: 150
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan',
          width: 120
        },
        {
          title: this.$t('label.start.ip'),
          dataIndex: 'startIp',
          width: 130
        },
        {
          title: this.$t('label.end.ip'),
          dataIndex: 'endIp',
          width: 130
        },
        {
          title: '',
          dataIndex: 'actions',
          scopedSlots: { customRender: 'actions' },
          width: 70
        }
      ],
      showError: false,
      ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i,
      ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i
    }
  },
  mounted () {
    const prefilledIpRangesKey = this.traffic + '-ipranges'
    if (this.prefillContent[prefilledIpRangesKey]) {
      this.ipRanges = this.prefillContent[prefilledIpRangesKey]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    handleAddRange (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          this.ipRanges.push({
            key: this.ipRanges.length.toString(),
            gateway: values.gateway,
            netmask: values.netmask,
            vlan: values.vlan,
            startIp: values.startIp,
            endIp: values.endIp
          })
          this.form.resetFields()
        }
      })
      this.emitIpRanges()
    },
    isValidSetup () {
      return this.ipRanges && this.ipRanges.length > 0
    },
    handleSubmit (e) {
      if (this.isValidSetup()) {
        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }

        this.$emit('nextPressed', this.ipRanges)
      } else {
        this.showError = true
      }
    },
    handleBack (e) {
      this.$emit('backPressed')
    },
    onDelete (key) {
      const ipRanges = [...this.ipRanges]
      this.ipRanges = ipRanges.filter(item => item.key !== key)
      this.emitIpRanges()
    },
    emitIpRanges () {
      const trafficRanges = {}
      trafficRanges[this.traffic + '-ipranges'] = this.ipRanges
      this.$emit('fieldsChanged', trafficRanges)
    },
    checkIpFormat (rule, value, callback) {
      if (!value || value === '') {
        callback()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        callback(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        callback(rule.message)
      } else {
        callback()
      }
    }
  }
}
</script>
