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
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item name="zoneSuperType" ref="zoneSuperType">
        <a-radio-group v-model:value="form.zoneSuperType">
          <a-card class="card-item">
            <a-row :gutter="12">
              <a-col :md="6" :lg="6">
                <a-radio class="card-form-item" value="Core">{{ $t('label.core') }}</a-radio>
              </a-col>
              <a-col :md="18" :lg="18">
                <a-card class="ant-form-text zone-support">{{ $t(zoneDescription.Core) }}</a-card>
              </a-col>
            </a-row>
          </a-card>
          <a-card class="card-item">
            <a-row :gutter="12">
              <a-col :md="6" :lg="6">
                <a-radio class="card-form-item" value="Edge">{{ $t('label.edge') }}</a-radio>
              </a-col>
              <a-col :md="18" :lg="18">
                <a-card class="ant-form-text zone-support"><span v-html="$t(zoneDescription.Edge)"></span></a-card>
              </a-col>
            </a-row>
          </a-card>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <div class="form-action">
      <a-button ref="submit" type="primary" @click="handleSubmit" class="button-next">
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
    }
  },
  data: () => ({
    formItemLayout: {
      labelCol: { span: 6 },
      wrapperCol: { span: 14 }
    },
    zoneDescription: {
      Core: 'message.desc.core.zone',
      Edge: 'message.desc.edge.zone'
    },
    formModel: {}
  }),
  created () {
    this.initForm()
  },
  watch: {
    formModel: {
      deep: true,
      handler (changedFields) {
        const fieldsChanged = toRaw(changedFields)
        this.$emit('fieldsChanged', fieldsChanged)
      }
    }
  },
  computed: {
    zoneSuperType () {
      return this.prefillContent.zoneSuperType ? this.prefillContent.zoneSuperType : 'Core'
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        zoneSuperType: this.zoneSuperType
      })
      this.rules = reactive({
        zoneSuperType: [{ required: true, message: this.$t('message.error.zone.type') }]
      })
      this.formModel = toRaw(this.form)
    },
    handleSubmit () {
      if (this.form.zoneSuperType === 'Edge') {
        this.form.zoneType = 'Advanced'
      }
      this.formRef.value.validate().then(() => {
        this.$emit('nextPressed')
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    }
  }
}
</script>
<style scoped lang="less">
  .form-content {
    border: 1px dashed #e9e9e9;
    border-radius: 6px;
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
      font-weight: bold;
        font-size: 15px;
    }

    .checkbox-advance {
      margin-top: 10px;
    }

    .zone-support {
      text-align: justify;
    }
  }
</style>
