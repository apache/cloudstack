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
    <a-form :ref="formRef" :model="form" :rules="rules">
      <a-form-item
        name="tungstenroutingpolicyfromtermcommunities"
        ref="tungstenroutingpolicyfromtermcommunities"
        :label="$t('label.community')"
        v-bind="formItemLayout">
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-model:value="form.tungstenroutingpolicyfromtermcommunities"
          @change="(value) => changeFieldValue('tungstenroutingpolicyfromtermcommunities', value)">
          <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        name="tungstenroutingpolicymatchall"
        ref="tungstenroutingpolicymatchall"
        :label="$t('label.matchall')"
        v-bind="formItemLayout">
        <a-checkbox
          v-model:checked="form.tungstenroutingpolicymatchall"
          @change="(e) => changeFieldValue('tungstenroutingpolicymatchall', e.target.checked)"
        />
      </a-form-item>
      <a-form-item
        name="tungstenroutingpolicyprotocol"
        ref="tungstenroutingpolicyprotocol"
        :label="$t('label.protocol')"
        v-bind="formItemLayout">
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-model:value="form.tungstenroutingpolicyprotocol"
          @change="(value) => changeFieldValue('tungstenroutingpolicyprotocol', value)">
          <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-table
        bordered
        :dataSource="prefixList"
        :columns="prefixColumns"
        :pagination="false"
        :rowKey="(record, idx) => idx"
        style="margin-bottom: 24px; width: 100%">
        <template #prefix="{ text, index }">
          <a-input :value="text" @change="e => onCellChange(index, 'prefix', e.target.value)" />
        </template>
        <template #prefixtype="{ text, index }">
          <a-select
            style="width: 100%"
            :defaultValue="text"
            @change="value => onCellChange(index, 'prefixtype', value)"
            showSearch
            :dropdownMatchSelectWidth="false"
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="extract"> extract </a-select-option>
            <a-select-option value="longer"> longer </a-select-option>
            <a-select-option value="orlonger"> orlonger </a-select-option>
          </a-select>
        </template>
        <template #termtype="{ text, index }">
          <a-select
            style="width: 100%"
            :defaultValue="text"
            @change="value => onCellChange(index, 'termtype', value)"
            showSearch
            optionFilterProp="label"
            :dropdownMatchSelectWidth="false"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="addcommunity"> add community </a-select-option>
            <a-select-option value="setcommunity"> set community </a-select-option>
            <a-select-option value="removecommunity"> remove community </a-select-option>
            <a-select-option value="local-preference"> local-preference </a-select-option>
            <a-select-option value="med"> med </a-select-option>
            <a-select-option value="action"> action </a-select-option>
            <a-select-option value="as-path"> as-path </a-select-option>
          </a-select>
        </template>
        <template #termvalue="{ text, index }">
          <a-input
            v-if="prefixList[index].termtype !== 'action'"
            :value="text"
            @change="e => onCellChange(index, 'termvalue', e.target.value)" />
          <a-select v-else value="default" @change="value => onCellChange(index, 'termvalue', value)">
            <a-select-option value="default">default</a-select-option>
            <a-select-option value="accept">accept</a-select-option>
            <a-select-option value="reject">reject</a-select-option>
            <a-select-option value="next">next</a-select-option>
          </a-select>
        </template>
        <template #action="{ index }">
          <tooltip-button
            :tooltip="$t('label.delete.prefix')"
            danger
            type="primary"
            icon="delete-outlined"
            @onClick="() => deletePrefix(index)" />
        </template>
        <template #footer>
          <a-button @click="addNewPrefix">
            {{ $t('label.add.prefix') }}
          </a-button>
        </template>
      </a-table>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'RoutingPolicyTerms',
  components: { TooltipButton },
  props: {
    formModel: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      formItemLayout: {
        labelCol: { span: 8 },
        wrapperCol: { span: 12 }
      },
      listCommunities: [],
      prefixList: [],
      prefixColumns: [
        {
          title: this.$t('label.prefix'),
          dataIndex: 'prefix',
          slots: { customRender: 'prefix' }
        },
        {
          title: this.$t('label.prefix.type'),
          dataIndex: 'prefixtype',
          slots: { customRender: 'prefixtype' }
        },
        {
          title: this.$t('label.term.type'),
          dataIndex: 'termtype',
          slots: { customRender: 'termtype' }
        },
        {
          title: this.$t('label.value'),
          dataIndex: 'termvalue',
          slots: { customRender: 'termvalue' }
        },
        {
          title: this.$t('label.action'),
          slots: { customRender: 'action' }
        }
      ]
    }
  },
  created () {
    this.initForm()
    this.prefixList = this.formModel?.prefixList || []
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        tungstenroutingpolicyfromtermcommunities: this.formModel?.tungstenroutingpolicyfromtermcommunities || [],
        tungstenroutingpolicymatchall: this.formModel?.tungstenroutingpolicymatchall || false,
        tungstenroutingpolicyprotocol: this.formModel?.tungstenroutingpolicyprotocol || [],
        prefixList: this.formModel?.prefixList || []
      })
      this.rules = reactive({
        tungstenroutingpolicyfromtermcommunities: [{ type: 'array' }],
        tungstenroutingpolicyprotocol: [{ type: 'array' }]
      })
    },
    addNewPrefix () {
      const index = this.prefixList.length
      this.prefixList.push({
        prefix: 'Prefix ' + (index + 1),
        prefixtype: 'extract',
        termtype: 'addcommunity',
        termvalue: ''
      })
      this.form.prefixList = this.prefixList
      this.emitEvents()
    },
    deletePrefix (index) {
      this.prefixList.splice(index, 1)
      this.form.prefixList = this.prefixList
      this.emitEvents()
    },
    changeFieldValue (field, value) {
      this.form[field] = value
      this.emitEvents()
    },
    onCellChange (key, name, value) {
      this.prefixList[key][name] = value
      this.form.prefixList = this.prefixList
      this.emitEvents()
    },
    emitEvents () {
      this.$emit('onChangeFields', this.form)
    }
  }
}
</script>
