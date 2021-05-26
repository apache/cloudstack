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
      v-if="description && description.length > 0"
      v-html="$t(description)">
    </a-card>
    <a-form
      class="form-content"
      :form="form"
      @submit="handleSubmit">
      <a-form-item
        :label="$t('label.vlan.range')"
        v-bind="formItemLayout"
        :help="validMessage"
        :validate-status="validStatus"
        has-feedback>
        <a-form-item
          has-feedback
          :style="{ display: 'inline-block', width: 'calc(50% - 12px)' }">
          <a-input-number
            v-decorator="['vlanRangeStart', {
              rules: [{
                validator: validateFromTo,
                fromInput: true,
                compare: 'vlanRangeEnd',
                initialValue: getPrefilled('vlanRangeStart')
              }]
            }]"
            style="width: 100%;"
            autoFocus
          />
        </a-form-item>
        <span :style="{ display: 'inline-block', width: '24px', textAlign: 'center' }">
          -
        </span>
        <a-form-item
          has-feedback
          :style="{ display: 'inline-block', width: 'calc(50% - 12px)' }">
          <a-input-number
            v-decorator="['vlanRangeEnd', {
              rules: [{
                validator: validateFromTo,
                toInput: true,
                compare: 'vlanRangeStart',
                initialValue: getPrefilled('vlanRangeEnd')
              }]
            }]"
            style="width: 100%;"
          />
        </a-form-item>
      </a-form-item>
    </a-form>
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
  </div>
</template>

<script>
export default {
  name: 'AdvancedGuestTrafficForm',
  props: {
    prefillContent: {
      type: Object,
      default: function () {
        return {}
      }
    },
    description: {
      type: String,
      default: 'label.creating.iprange'
    },
    isFixError: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formItemLayout: {
        labelCol: { span: 8 },
        wrapperCol: { span: 12 }
      },
      validStatus: '',
      validMessage: ''
    }
  },
  created () {
    this.form = this.$form.createForm(this, {
      onFieldsChange: (_, changedFields) => {
        this.$emit('fieldsChanged', changedFields)
      }
    })
  },
  mounted () {
    this.fillValue()
  },
  methods: {
    fillValue () {
      const fieldVal = {}
      fieldVal.vlanRangeStart = this.getPrefilled('vlanRangeStart')
      this.form.setFieldsValue(fieldVal)
      fieldVal.vlanRangeEnd = this.getPrefilled('vlanRangeEnd')
      this.form.setFieldsValue(fieldVal)
    },
    getPrefilled (key) {
      return this.prefillContent[key] ? this.prefillContent[key].value : null
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        this.validStatus = ''
        this.validMessage = ''
        if (err) {
          return
        }
        if (!this.checkFromTo(values.vlanRangeStart, values.vlanRangeEnd)) {
          this.validStatus = 'error'
          this.validMessage = this.$t('message.error.vlan.range')
          return
        }
        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }
        this.$emit('nextPressed')
      })
    },
    handleBack (e) {
      this.$emit('backPressed')
    },
    validateFromTo (rule, value, callback) {
      let fromVal = ''
      let toVal = ''
      this.validStatus = ''
      this.validMessage = ''
      if (rule.fromInput) {
        fromVal = value
        toVal = this.form.getFieldValue(rule.compare)
      } else if (rule.toInput) {
        toVal = value
        fromVal = this.form.getFieldValue(rule.compare)
      }
      if (!this.checkFromTo(fromVal, toVal)) {
        this.validStatus = 'error'
        this.validMessage = this.$t('message.error.vlan.range')
      }
      callback()
    },
    checkFromTo (fromVal, toVal) {
      if (!fromVal) fromVal = 0
      if (!toVal) toVal = 0
      if (fromVal > toVal) {
        return false
      }
      return true
    }
  }
}
</script>

<style scoped lang="less">
  .form-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
    background-color: #fafafa;
    min-height: 200px;
    text-align: center;
    vertical-align: center;
    margin-top: 8px;
    max-height: 300px;
    overflow-y: auto;
    padding: 16px 20px 0;

    /deep/.has-error {
      .ant-form-explain {
        text-align: left;
      }
    }

    /deep/.ant-form-item-control {
      text-align: left;
    }
  }

  .form-action {
    margin-top: 16px;
  }
</style>
