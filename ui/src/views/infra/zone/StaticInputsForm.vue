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
      v-if="description && description.length > 0"
      v-html="$t(description)">
    </a-card>
    <a-form
      class="form-content"
      :form="form"
      @submit="handleSubmit">
      <a-form-item
        v-for="(field, index) in this.fields"
        :key="index"
        :label="$t(field.title)"
        v-if="isDisplayInput(field.display)"
        v-bind="formItemLayout"
        :has-feedback="field.switch ? false : true">
        <a-select
          v-if="field.select"
          v-decorator="[field.key, {
            rules: [
              {
                required: field.required,
                message: $t(field.placeHolder),
                initialValue: getPrefilled(field.key)
              }
            ]
          }]"
          :allowClear="true"
          :autoFocus="index === 0"
        >
          <a-select-option
            v-for="option in field.options"
            :key="option.id"
            :value="option.id"
          >
            {{ option.name || option.description }}
          </a-select-option>
        </a-select>
        <a-switch
          v-else-if="field.switch"
          v-decorator="[field.key]"
          :default-checked="isChecked(field)"
          :autoFocus="index === 0"
        />
        <a-input
          v-else-if="field.password"
          type="password"
          v-decorator="[field.key, {
            rules: [
              {
                required: field.required,
                message: $t(field.placeHolder),
                initialValue: getPrefilled(field.key)
              }
            ]
          }]"
          :autoFocus="index === 0"
        />
        <a-input
          v-else
          v-decorator="[field.key, {
            rules: [
              {
                required: field.required,
                message: $t(field.placeHolder),
                initialValue: getPrefilled(field.key)
              },
              {
                validator: checkIpFormat,
                ipV4: field.ipV4,
                ipV6: field.ipV6,
                message: $t(field.message)
              }
            ]
          }]"
          :autoFocus="index === 0"
        />
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
  props: {
    prefillContent: {
      type: Object,
      default: function () {
        return {}
      }
    },
    fields: {
      type: Array,
      default: function () {
        return []
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
  created () {
    this.form = this.$form.createForm(this, {
      onFieldsChange: (_, changedFields) => {
        this.$emit('fieldsChanged', changedFields)
      }
    })
  },
  data: () => ({
    formItemLayout: {
      labelCol: { span: 8 },
      wrapperCol: { span: 12 }
    },
    ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i,
    ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i
  }),
  mounted () {
    this.fillValue(true)
  },
  watch: {
    fields () {
      this.fillValue(false)
    }
  },
  methods: {
    fillValue (autoFill) {
      this.fields.forEach(field => {
        const fieldExists = this.isDisplayInput(field.display)
        if (!fieldExists) {
          return
        }
        const fieldVal = {}
        if (field.key === 'agentUserName' && !this.getPrefilled(field.key)) {
          fieldVal[field.key] = 'Oracle'
        } else {
          fieldVal[field.key] = this.getPrefilled(field.key)
        }
        if (autoFill) {
          this.form.setFieldsValue(fieldVal)
        } else {
          this.form.getFieldDecorator(field.key, { initialValue: this.getPrefilled(field.key) })
        }
      })
    },
    getPrefilled (key) {
      return this.prefillContent[key] ? this.prefillContent[key].value : null
    },
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (err) {
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
    },
    isDisplayInput (conditions) {
      if (!conditions || Object.keys(conditions).length === 0) {
        return true
      }
      let isShow = false
      Object.keys(conditions).forEach(key => {
        const condition = conditions[key]
        const fieldVal = this.form.getFieldValue(key)
          ? this.form.getFieldValue(key)
          : (this.prefillContent[key] ? this.prefillContent[key].value : null)
        if (Array.isArray(condition) && condition.includes(fieldVal)) {
          isShow = true
          return false
        } else if (!Array.isArray(condition) && fieldVal === condition) {
          isShow = true
          return false
        }

        return true
      })

      return isShow
    },
    isChecked (field) {
      if (this.prefillContent[field.key] && this.prefillContent[field.key].value) {
        return this.prefillContent[field.key].value
      }
      if (!field.checked) {
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

  .ant-form-text {
    text-align: justify;
    margin: 10px 0;
    padding: 24px;
    width: 100%;
  }

  .form-action {
    margin-top: 16px;
  }
</style>
