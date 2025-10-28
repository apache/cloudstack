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
   <a-form
    ref="formRef"
    name="form"
    :model="dynamicValidateForm"
  >
    <a-space
      v-for="(pair, index) in dynamicValidateForm.pairArray"
      :key="pair.id"
    >
      <a-form-item :name="['pairArray', index, 'key']">
        <a-input v-model:value="pair.key" :placeholder="$t('label.key')" />
      </a-form-item>
      <a-form-item :name="['pairArray', index, 'value']">
        <a-input v-model:value="pair.value" :placeholder="$t('label.value')" />
      </a-form-item>
      <MinusCircleOutlined @click="removePair(pair)" />
    </a-space>
    <a-form-item>
      <a-button type="dashed" block @click="addKeyValue()">
        <PlusOutlined />
        {{ $t('label.add.key.value') }}
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script>
import { ref, reactive } from 'vue'

export default {
  name: 'KeyValuePairInput',
  props: {
    pairs: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    var pairArray = []
    for (var key in this.pairs) {
      pairArray.push({ key: key, value: this.pairs[key] })
    }
    return {
      dynamicValidateForm: reactive({
        pairArray: reactive(pairArray)
      }),
      formRef: ref()
    }
  },
  created () {},
  watch: {
    dynamicValidateForm: {
      handler: function (val) {
        this.$emit('update-pairs', val.pairArray.reduce((obj, item) => {
          obj[item.key] = item.value
          return obj
        }, {}))
      },
      deep: true
    }
  },
  computed: {},
  methods: {
    addKeyValue () {
      this.dynamicValidateForm.pairArray.push({ id: Date.now(), key: '', value: '' })
    },
    removePair (pair) {
      const index = this.dynamicValidateForm.pairArray.indexOf(pair)
      this.dynamicValidateForm.pairArray.splice(index, 1)
    }
  }
}
</script>
