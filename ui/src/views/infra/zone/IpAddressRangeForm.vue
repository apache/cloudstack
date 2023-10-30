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
    <div v-ctrl-enter="handleSubmit">
      <a-table
        bordered
        :scroll="{ x: 500 }"
        :dataSource="ipRanges"
        :columns="columns"
        :pagination="false"
        style="margin-bottom: 24px; width: 100%" >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <tooltip-button
              :tooltip="$t('label.delete')"
              type="primary"
              :danger="true"
              icon="delete-outlined"
              @onClick="onDelete(record.key)" />
          </template>
        </template>
        <template #footer>
          <a-form
            :layout="isMobile() ? 'horizontal': 'inline'"
            :ref="formRef"
            :model="form"
            :rules="rules"
            @finish="handleAddRange"
           >
            <div class="form-row">
              <div class="form-col">
                <a-form-item name="gateway" ref="gateway">
                  <a-input
                    v-model:value="form.gateway"
                    :placeholder="$t('label.gateway')"
                    v-focus="true"
                  />
                </a-form-item>
              </div>
              <div class="form-col">
                <a-form-item name="netmask" ref="netmask">
                  <a-input
                    v-model:value="form.netmask"
                    :placeholder="$t('label.netmask')"
                  />
                </a-form-item>
              </div>
              <div class="form-col">
                <a-form-item name="vlan" ref="vlan">
                  <a-input
                    v-model:value="form.vlan"
                    :placeholder="$t('label.vlan')"
                  />
                </a-form-item>
              </div>
              <div class="form-col">
                <a-form-item name="startIp" ref="startIp">
                  <a-input
                    v-model:value="form.startIp"
                    :placeholder="$t('label.start.ip')"
                  />
                </a-form-item>
              </div>
              <div class="form-col">
                <a-form-item name="endIp" ref="endIp">
                  <a-input
                    v-model:value="form.endIp"
                    :placeholder="$t('label.end.ip')"
                  />
                </a-form-item>
              </div>
              <div class="form-col">
                <a-form-item :style="{ display: 'inline-block', float: 'right', marginRight: 0 }">
                  <a-button type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
                </a-form-item>
              </div>
            </div>
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
        <a-button class="button-next" ref="submit" type="primary" @click="handleSubmit">
          {{ $t('label.next') }}
        </a-button>
      </div>
    </div>
    <a-modal
      v-if="showError"
      :visible="showError"
      :closable="true"
      :maskClosable="false"
      :title="`${$t('label.error')}!`"
      :footer="null"
      @cancel="showError = false"
      centered
    >
      <div v-ctrl-enter="() => showError = false">
        <span>{{ $t('message.required.add.least.ip') }}</span>
        <div :span="24" class="action-button">
          <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import TooltipButton from '@/components/widgets/TooltipButton'
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
          width: 140
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask',
          width: 140
        },
        {
          title: this.$t('label.vlan'),
          dataIndex: 'vlan',
          width: 120
        },
        {
          title: this.$t('label.start.ip'),
          dataIndex: 'startIp',
          width: 140
        },
        {
          title: this.$t('label.end.ip'),
          dataIndex: 'endIp',
          width: 140
        },
        {
          key: 'actions',
          title: '',
          dataIndex: 'actions',
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
  created () {
    this.initForm()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        gateway: [{ required: true, message: this.$t('message.error.gateway') }],
        netmask: [{ required: true, message: this.$t('message.error.netmask') }],
        startIp: [{
          required: true,
          message: this.$t('message.error.startip')
        },
        {
          validator: this.checkIpFormat,
          ipV4: true,
          message: this.$t('message.error.ipv4.address')
        }],
        endIp: [{
          required: true,
          message: this.$t('message.error.startip')
        },
        {
          validator: this.checkIpFormat,
          ipV4: true,
          message: this.$t('message.error.ipv4.address')
        }]
      })
    },
    handleAddRange () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.ipRanges.push({
          key: this.ipRanges.length.toString(),
          gateway: values.gateway,
          netmask: values.netmask,
          vlan: values.vlan,
          startIp: values.startIp,
          endIp: values.endIp
        })
        this.formRef.value.resetFields()
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
      this.emitIpRanges()
    },
    isValidSetup () {
      return this.ipRanges && this.ipRanges.length > 0
    },
    handleSubmit () {
      if (this.isValidSetup()) {
        this.showError = false
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
    async checkIpFormat (rule, value) {
      if (!value || value === '') {
        return Promise.resolve()
      } else if (rule.ipV4 && !this.ipV4Regex.test(value)) {
        return Promise.reject(rule.message)
      } else if (rule.ipV6 && !this.ipV6Regex.test(value)) {
        return Promise.reject(rule.message)
      } else {
        return Promise.resolve()
      }
    }
  }
}
</script>

<style scoped lang="less">
.form-row {
  display: grid;
  grid-template-columns: 145px 145px 130px 145px 145px 70px;
  justify-content: center;

  @media (max-width: 768px) {
    display: flex;
    flex-direction: column;
  }
}
</style>
