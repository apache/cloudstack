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
    <a-form
      class="form-content"
      :form="form"
      @submit="handleSubmit">
      <a-form-item>
        <a-radio-group
          v-decorator="['zoneType', {
            rules: [{
              required: true,
              message: $t('message.error.zone.type'),
              initialValue: zoneType
            }]
          }]">
          <a-card :gutter="12" class="card-item" v-if="$config.basicZoneEnabled">
            <a-col :md="6" :lg="6">
              <a-radio class="card-form-item" value="Basic">{{ $t('label.basic') }}</a-radio>
            </a-col>
            <a-col :md="18" :lg="18">
              <a-card class="ant-form-text zone-support">{{ $t(zoneDescription.Basic) }}</a-card>
            </a-col>
          </a-card>
          <a-card :gutter="12" class="card-item">
            <a-col :md="6" :lg="6">
              <a-radio class="card-form-item" value="Advanced" v-if="$config.basicZoneEnabled">{{ $t('label.advanced') }}</a-radio>
              <span style="margin-top: 20px;" class="card-form-item" v-else>
                <a-icon type="setting" style="margin-right: 10px" />
                {{ $t('label.advanced') }}
              </span>
            </a-col>
            <a-col :md="18" :lg="18">
              <a-card class="ant-form-text zone-support">{{ $t(zoneDescription.Advanced) }}</a-card>
            </a-col>
            <a-col :md="6" :lg="6" style="margin-top: 15px">
              <a-form-item
                class="card-form-item"
                v-bind="formItemLayout">
                <a-switch
                  class="card-form-item"
                  v-decorator="['securityGroupsEnabled', { valuePropName: 'checked' }]"
                  :value="securityGroupsEnabled"
                  :disabled="!isAdvancedZone"
                  autoFocus
                />
              </a-form-item>
              <span>{{ $t('label.menu.security.groups') }}</span>
            </a-col>
            <a-col :md="18" :lg="18" style="margin-top: 15px;">
              <a-card class="zone-support">{{ $t(zoneDescription.SecurityGroups) }}</a-card>
            </a-col>
          </a-card>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <div class="form-action">
      <a-button type="primary" @click="handleSubmit" class="button-next">
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
    }
  },
  data: () => ({
    formItemLayout: {
      labelCol: { span: 6 },
      wrapperCol: { span: 14 }
    },
    zoneDescription: {
      Basic: 'message.desc.basic.zone',
      Advanced: 'message.desc.advanced.zone',
      SecurityGroups: 'message.advanced.security.group'
    }
  }),
  beforeCreate () {
    this.form = this.$form.createForm(this, {
      onFieldsChange: (_, changedFields) => {
        this.$emit('fieldsChanged', changedFields)
      }
    })
  },
  mounted () {
    this.form.setFieldsValue({
      zoneType: this.zoneType,
      securityGroupsEnabled: this.securityGroupsEnabled
    })
  },
  computed: {
    isAdvancedZone () {
      return this.zoneType === 'Advanced'
    },
    zoneType () {
      return this.prefillContent.zoneType ? this.prefillContent.zoneType.value : (this.$config.basicZoneEnabled ? 'Basic' : 'Advanced')
    },
    securityGroupsEnabled () {
      return this.isAdvancedZone && (this.prefillContent.securityGroupsEnabled ? this.prefillContent.securityGroupsEnabled.value : false)
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          this.$emit('nextPressed')
        }
      })
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
    padding: 8px;
    padding-top: 16px;
    margin-top: 8px;
  }

  .card-item {
    margin-top: 10px;

    .card-form-item {
      float: left;
    }

    .checkbox-advance {
      margin-top: 10px;
    }

    .zone-support {
      text-align: justify;
      background: #fafafa;
    }
  }
</style>
