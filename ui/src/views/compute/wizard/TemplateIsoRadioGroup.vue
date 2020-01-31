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
  <a-form-item>
    <a-radio-group
      v-for="(os, osIndex) in osList"
      :key="osIndex"
      class="radio-group"
      v-decorator="[inputDecorator, {
        rules: [{ required: true, message: 'Please select option' }]
      }]"
    >
      <a-radio
        class="radio-group__radio"
        :value="os.id"
      >
        {{ os.displaytext }}&nbsp;
        <a-tag
          :visible="os.ispublic && !os.isfeatured"
          color="blue"
        >{{ $t('isPublic') }}</a-tag>
        <a-tag
          :visible="os.isfeatured"
          color="green"
        >{{ $t('isFeatured') }}</a-tag>
        <a-tag
          :visible="isSelf(os)"
          color="orange"
        >{{ $t('isSelf') }}</a-tag>
        <a-tag
          :visible="isShared(os)"
          color="cyan"
        >{{ $t('isShared') }}</a-tag>
      </a-radio>
    </a-radio-group>
  </a-form-item>
</template>

<script>
import store from '@/store'

export default {
  name: 'TemplateIsoRadioGroup',
  props: {
    osList: {
      type: Array,
      default: () => []
    },
    inputDecorator: {
      type: String,
      default: ''
    }
  },
  methods: {
    isShared (item) {
      return !item.ispublic && (item.account !== store.getters.userInfo.account)
    },
    isSelf (item) {
      return !item.ispublic && (item.account === store.getters.userInfo.account)
    }
  }
}
</script>

<style lang="less" scoped>
  .radio-group {
    display: block;

    &__radio {
      margin: 0.5rem 0;
    }
  }

  .ant-tag {
    margin-left: 0.4rem;
  }
</style>
