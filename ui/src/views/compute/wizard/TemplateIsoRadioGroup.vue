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
      :pagination="pagination">
      <a-list-item slot="renderItem" slot-scope="os, osIndex" key="os.id">
        <a-radio-group
          class="radio-group"
          :key="osIndex"
          v-model="value"
          @change="($event) => updateSelectionTemplateIso($event.target.value)">
          <a-radio
            class="radio-group__radio"
            :value="os.id">
            {{ os.displaytext }}&nbsp;
            <a-tag
              :visible="os.ispublic && !os.isfeatured"
              color="blue"
              @click="onFilterTag('is: public')"
            >{{ $t('isPublic') }}</a-tag>
            <a-tag
              :visible="os.isfeatured"
              color="green"
              @click="onFilterTag('is: featured')"
            >{{ $t('isFeatured') }}</a-tag>
            <a-tag
              :visible="isSelf(os)"
              color="orange"
              @click="onFilterTag('is: self')"
            >{{ $t('isSelf') }}</a-tag>
            <a-tag
              :visible="isShared(os)"
              color="cyan"
              @click="onFilterTag('is: shared')"
            >{{ $t('isShared') }}</a-tag>
          </a-radio>
        </a-radio-group>
      </a-list-item>
    </a-list>
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
    },
    selected: {
      type: String,
      default: ''
    },
    itemCount: {
      type: Number,
      default: 0
    }
  },
  data () {
    return {
      value: '',
      page: 1,
      pageSize: 10
    }
  },
  created () {
    this.value = this.selected
    this.$emit('emit-update-template-iso', this.inputDecorator, this.value)
  },
  watch: {
    inputDecorator (value) {
      if (value === 'templateid') {
        this.value = this.selected
      }
    }
  },
  computed: {
    pagination () {
      return {
        size: 'small',
        page: 1,
        pageSize: 10,
        total: this.itemCount,
        showSizeChanger: true,
        onChange: this.onChangePage,
        onShowSizeChange: this.onChangePageSize
      }
    }
  },
  methods: {
    isShared (item) {
      return !item.ispublic && (item.account !== store.getters.userInfo.account)
    },
    isSelf (item) {
      return !item.ispublic && (item.account === store.getters.userInfo.account)
    },
    updateSelectionTemplateIso (id) {
      this.$emit('emit-update-template-iso', this.inputDecorator, id)
    },
    onChangePage (page, pageSize) {
      this.pagination.page = page
      this.pagination.pageSize = pageSize
      this.$forceUpdate()
    },
    onChangePageSize (page, pageSize) {
      this.pagination.page = page
      this.pagination.pageSize = pageSize
      this.$forceUpdate()
    },
    onFilterTag (tag) {
      this.$emit('handle-filter-tag', tag)
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

  /deep/.ant-spin-container {
    max-height: 200px;
    overflow-y: auto;
  }

  .pagination {
    margin-top: 20px;
    float: right;
  }
</style>
