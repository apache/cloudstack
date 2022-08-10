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
      v-ctrl-enter="handleSubmit"
      class="form-content"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
     >
      <div v-for="(field, index) in fields" :key="index">
        <a-form-item
          :name="field.key"
          :ref="field.key"
          :label="$t(field.title)"
          v-if="isDisplayInput(field)"
          v-bind="formItemLayout"
          :has-feedback="field.switch ? false : true">
          <a-select
            v-if="field.select"
            v-model:value="form[field.key]"
            :allowClear="true"
            v-focus="index === 0"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="option in field.options"
              :key="option.id"
              :value="option.id"
              :label="option.name || option.description"
            >
              {{ option.name || option.description }}
            </a-select-option>
          </a-select>
          <a-switch
            v-else-if="field.switch"
            v-model:checked="form[field.key]"
            v-focus="index === 0"
          />
          <a-checkbox
            v-else-if="field.checkbox"
            v-model:checked="form[field.key]"
            v-focus="index === 0">
          </a-checkbox>
          <a-input
            v-else-if="field.password"
            type="password"
            v-model:value="form[field.key]"
            v-focus="index === 0"
          />
          <a-radio-group
            v-else-if="field.radioGroup"
            v-model:value="form[field.key]"
            buttonStyle="solid">
            <span
              style="margin-right: 5px;"
               v-for="(radioItem, idx) in field.radioOption"
              :key="idx">
              <a-radio-button
                :value="radioItem.value"
                v-if="isDisplayItem(radioItem.condition)">
                {{ $t(radioItem.label) }}
              </a-radio-button>
            </span>
            <a-alert style="margin-top: 5px" type="warning" v-if="field.alert && isDisplayItem(field.alert.display)">
              <template #message>
                <span v-html="$t(field.alert.message)" />
              </template>
            </a-alert>
          </a-radio-group>
          <a-input
            v-else
            v-model:value="form[field.key]"
            v-focus="index === 0"
          />
        </a-form-item>
      </div>
    </a-form>
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
</template>

<script>
import { ref, reactive, toRaw } from 'vue'

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
    this.initForm()
  },
  computed: {
    hypervisor () {
      return this.prefillContent?.hypervisor || null
    }
  },
  mounted () {
    this.fillValue()
  },
  data: () => ({
    formItemLayout: {
      labelCol: { span: 8 },
      wrapperCol: { span: 12 }
    },
    ipV4Regex: /^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$/i,
    ipV6Regex: /^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}:([0-9A-Fa-f]{1,4}:)?[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){4}:([0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){3}:([0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){2}:([0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){6}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(([0-9A-Fa-f]{1,4}:){0,5}:((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|(::([0-9A-Fa-f]{1,4}:){0,5}((\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b)\.){3}(\b((25[0-5])|(1\d{2})|(2[0-4]\d)|(\d{1,2}))\b))|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:))$/i,
    formModel: {}
  }),
  watch: {
    formModel: {
      deep: true,
      handler (changedFields) {
        const fieldsChanged = toRaw(changedFields)
        this.$emit('fieldsChanged', fieldsChanged)
      }
    },
    'prefillContent.provider' (val) {
      if (['SolidFire', 'PowerFlex'].includes(val)) {
        this.form.primaryStorageProtocol = 'custom'
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    fillValue () {
      this.fields.forEach(field => {
        this.setRules(field)
        const fieldExists = this.isDisplayInput(field)
        if (!fieldExists) {
          return
        }
        if (field.key === 'agentUserName' && !this.getPrefilled(field)) {
          this.form[field.key] = 'Oracle'
        } else {
          if (field.switch || field.checkbox) {
            this.form[field.key] = this.isChecked(field)
          } else {
            this.form[field.key] = this.getPrefilled(field)
          }
        }
      })

      this.formModel = toRaw(this.form)
    },
    setRules (field) {
      this.rules[field.key] = []
      if (field.required) {
        this.rules[field.key].push({ required: field.required, message: this.$t(field.placeHolder) })
      }
      if (field.ipV4 || field.ipV6) {
        this.rules[field.key].push({
          ipV4: field.ipV4,
          ipV6: field.ipV6,
          validator: this.checkIpFormat,
          message: this.$t(field.message)
        })
      }
    },
    getPrefilled (field) {
      if (field.key === 'authmethod' && this.hypervisor !== 'KVM') {
        return field.value || field.defaultValue || 'password'
      }
      return this.prefillContent?.[field.key] || field.value || field.defaultValue || null
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        if (this.isFixError) {
          this.$emit('submitLaunchZone')
          return
        }
        this.$emit('nextPressed')
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    handleBack () {
      this.$emit('backPressed')
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
    },
    isDisplayInput (field) {
      if (!field.display && !field.hidden) {
        return true
      }
      const conditions = field.display || field.hidden
      if (!conditions || Object.keys(conditions).length === 0) {
        return true
      }
      let isShow = true
      Object.keys(conditions).forEach(key => {
        if (isShow) {
          const condition = conditions[key]
          const fieldVal = this.form[key]
            ? this.form[key]
            : (this.prefillContent?.[key] || null)

          if (field.hidden) {
            if (Array.isArray(condition) && condition.includes(fieldVal)) {
              isShow = false
            } else if (!Array.isArray(condition) && fieldVal === condition) {
              isShow = false
            }
          } else if (field.display) {
            if (Array.isArray(condition) && !condition.includes(fieldVal)) {
              isShow = false
            } else if (!Array.isArray(condition) && fieldVal !== condition) {
              isShow = false
            }
          }
        }
      })

      return isShow
    },
    isChecked (field) {
      if (this.prefillContent[field.key]) {
        return this.prefillContent[field.key]
      }
      if (!field.checked) {
        return false
      }
      return true
    },
    isDisplayItem (conditions) {
      if (!conditions || Object.keys(conditions).length === 0) {
        return true
      }
      let isShow = true
      Object.keys(conditions).forEach(key => {
        if (!isShow) return false

        const condition = conditions[key]
        const fieldVal = this.form[key]
          ? this.form[key]
          : (this.prefillContent?.[key] || null)
        if (Array.isArray(condition) && !condition.includes(fieldVal)) {
          isShow = false
        } else if (!Array.isArray(condition) && fieldVal !== condition) {
          isShow = false
        }
      })

      return isShow
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

    :deep(.has-error) {
      .ant-form-explain {
        text-align: left;
      }
    }

    :deep(.ant-form-item-control) {
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
