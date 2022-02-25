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
    <a-form :form="form">
      <a-form-item :label="$t('label.community')" v-bind="formItemLayout">
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-decorator="['tungstenroutingpolicyfromtermcommunities', {
            initialValue: formModel.tungstenroutingpolicyfromtermcommunities,
            rules: [{ type: 'array' }]
          }]"
          @change="(value) => changeFieldValue('tungstenroutingpolicyfromtermcommunities', value)">
          <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="$t('label.matchall')" v-bind="formItemLayout">
        <a-checkbox
          v-decorator="[
            'tungstenroutingpolicymatchall',
            {
              initialValue: formModel.tungstenroutingpolicymatchall,
              valuePropName: 'checked',
            },
          ]"
          @change="(e) => changeFieldValue('tungstenroutingpolicymatchall', e.target.checked)"
        />
      </a-form-item>
      <a-form-item :label="$t('label.protocol')" v-bind="formItemLayout">
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-decorator="['tungstenroutingpolicyprotocol', {
            initialValue: formModel.tungstenroutingpolicyprotocol,
            rules: [{ type: 'array' }]
          }]"
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
        <template slot="prefix" slot-scope="text, record, index">
          <a-input :value="text" @change="e => onCellChange(index, 'prefix', e.target.value)" />
        </template>
        <template slot="prefixtype" slot-scope="text, record, index">
          <a-select
            style="width: 100%"
            :defaultValue="text"
            @change="value => onCellChange(index, 'prefixtype', value)"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="extract"> extract </a-select-option>
            <a-select-option value="longer"> longer </a-select-option>
            <a-select-option value="orlonger"> orlonger </a-select-option>
          </a-select>
        </template>
        <template slot="termtype" slot-scope="text, record, index">
          <a-select
            style="width: 100%"
            :defaultValue="text"
            @change="value => onCellChange(index, 'termtype', value)"
            showSearch
            optionFilterProp="children"
            :filterOption="(input, option) => {
              return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
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
        <template slot="termvalue" slot-scope="text, record, index">
          <a-input :value="text" @change="e => onCellChange(index, 'termvalue', e.target.value)" />
        </template>
        <template slot="action" slot-scope="text, record, index">
          <tooltip-button
            :tooltip="$t('label.delete.prefix')"
            type="danger"
            icon="delete"
            @click="() => deletePrefix(index)" />
        </template>
        <template slot="footer">
          <a-button @click="addNewPrefix">
            {{ $t('label.add.prefix') }}
          </a-button>
        </template>
      </a-table>
    </a-form>
  </div>
</template>

<script>
export default {
  name: 'RoutingPolicyTerms',
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
          scopedSlots: { customRender: 'prefix' }
        },
        {
          title: this.$t('label.prefix.type'),
          dataIndex: 'prefixtype',
          scopedSlots: { customRender: 'prefixtype' }
        },
        {
          title: this.$t('label.term.type'),
          dataIndex: 'termtype',
          scopedSlots: { customRender: 'termtype' }
        },
        {
          title: this.$t('label.value'),
          dataIndex: 'termvalue',
          scopedSlots: { customRender: 'termvalue' }
        },
        {
          title: this.$t('label.action'),
          scopedSlots: { customRender: 'action' }
        }
      ]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    addNewPrefix () {
      const index = this.prefixList.length
      this.prefixList.push({
        prefix: 'Prefix ' + (index + 1),
        prefixtype: 'extract',
        termtype: 'addcommunity',
        termvalue: ''
      })
      this.emitEvents()
    },
    deletePrefix (index) {
      this.prefixList.splice(index, 1)
      this.emitEvents()
    },
    changeFieldValue (field, value) {
      this.formModel[field] = value
      this.emitEvents()
    },
    onCellChange (key, name, value) {
      this.prefixList[key][name] = value
      this.emitEvents()
    },
    emitEvents () {
      this.$emit('onChangeFields', this.formModel, this.prefixList)
    }
  }
}
</script>
