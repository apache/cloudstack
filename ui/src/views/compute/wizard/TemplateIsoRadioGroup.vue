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
    <a-list
      class="form-item-scroll"
      itemLayout="vertical"
      size="small"
      :dataSource="osList"
      :pagination="false">
      <a-list-item slot="renderItem" slot-scope="os, osIndex" key="os.id" @click="onClickRow(os)">
        <a-radio-group
          class="radio-group"
          :key="osIndex"
          v-model="value"
          @change="($event) => updateSelectionTemplateIso($event.target.value)">
          <a-radio
            class="radio-group__radio"
            :value="os.id">
            {{ os.displaytext }}&nbsp;
            <os-logo
              class="radio-group__os-logo"
              :osId="os.ostypeid"
              :os-name="os.osName" />
          </a-radio>
        </a-radio-group>
      </a-list-item>
    </a-list>

    <div style="display: block; text-align: right;">
      <a-pagination
        size="small"
        :current="options.page"
        :pageSize="options.pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10', '20', '40', '80', '100', '200']"
        @change="onChangePage"
        @showSizeChange="onChangePageSize"
        showSizeChanger>
        <template slot="buildOptionText" slot-scope="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>
    </div>
  </a-form-item>
</template>

<script>
import OsLogo from '@/components/widgets/OsLogo'

export default {
  name: 'TemplateIsoRadioGroup',
  components: { OsLogo },
  props: {
    osList: {
      type: Array,
      default: () => []
    },
    inputDecorator: {
      type: String,
      default: ''
    },
    selected: {
      type: String,
      default: ''
    },
    itemCount: {
      type: Number,
      default: 0
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      value: '',
      options: {
        page: 1,
        pageSize: 10
      }
    }
  },
  mounted () {
    this.onSelectTemplateIso()
  },
  watch: {
    selected (newVal, oldVal) {
      if (newVal === oldVal) return
      this.onSelectTemplateIso()
    }
  },
  methods: {
    onSelectTemplateIso () {
      if (this.inputDecorator === 'templateid') {
        this.value = !this.preFillContent.templateid ? this.selected : this.preFillContent.templateid
      } else {
        this.value = !this.preFillContent.isoid ? this.selected : this.preFillContent.isoid
      }

      this.$emit('emit-update-template-iso', this.inputDecorator, this.value)
    },
    updateSelectionTemplateIso (id) {
      this.$emit('emit-update-template-iso', this.inputDecorator, id)
    },
    onChangePage (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onChangePageSize (page, pageSize) {
      this.options.page = page
      this.options.pageSize = pageSize
      this.$emit('handle-search-filter', this.options)
    },
    onClickRow (os) {
      this.value = os.id
      this.$emit('emit-update-template-iso', this.inputDecorator, this.value)
    }
  }
}
</script>

<style lang="less" scoped>
  .radio-group {
    margin: 0.5rem 0;

    /deep/.ant-radio {
      margin-right: 20px;
    }

    &__os-logo {
      position: absolute;
      top: 0;
      left: 0;
      margin-top: 2px;
      margin-left: 23px;
    }
  }

  /deep/.ant-spin-container {
    max-height: 200px;
    overflow-y: auto;
  }

  .pagination {
    margin-top: 20px;
    float: right;
  }

  /deep/.ant-list-split .ant-list-item {
    cursor: pointer;
  }
</style>
