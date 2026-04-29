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
    <div class="setting-item" v-if="viewType === 'item'">
      <h3 class="title">{{ title }}</h3>
      <slot></slot>
    </div>

    <div class="setting-image-item" v-if="viewType === 'radio-group'">
      <a-row :span="24" style="display: flex;">
        <a-col v-for="(item) in items" :key="item.name" :style="colWidth">
          <a-tooltip :title="$t(`label.theme.${item.name}`)" placement="top">
            <div :class="['img-checkbox', item.disabled ? 'disabled' : '']" v-if="item.type==='image-checkbox'">
              <light v-if="item.icon==='light'" :style="{ height: '56px', width: '56px' }" />
              <dark v-if="item.icon==='dark'" :style="{ height: '56px', width: '56px' }"/>
              <div :class="['check-item', item.name === checked ? 'check-item-checked' : '']">
                <a-radio v-model:value="item.name" :disabled="item.disabled"></a-radio>
                <check-outlined :class="['check-icon', item.name]" />
              </div>
            </div>

            <div class="color-checkbox" v-else>
              <div
                :class="['check-color', item.color === checked ? 'check-color-checked' : '']"
                :style="{ backgroundColor: item.color }">
                <a-radio v-model:value="item.color"></a-radio>
                <check-outlined class="check-icon" />
              </div>
            </div>
          </a-tooltip>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script>

import light from '@/assets/icons/light.svg?inline'
import dark from '@/assets/icons/dark.svg?inline'

export default {
  name: 'SettingItem',
  components: {
    light,
    dark
  },
  props: {
    viewType: {
      type: String,
      required: true
    },
    title: {
      type: String,
      default: ''
    },
    items: {
      type: Array,
      default: () => []
    },
    checked: {
      type: String,
      default: ''
    }
  },
  computed: {
    colWidth () {
      if (this.items.length === 2) {
        return { width: '70px' }
      }

      return { width: `${(100 / this.items.length)}%` }
    }
  }
}
</script>

<style lang="less" scoped>
.setting-item{
  margin-bottom: 24px;
  padding: 0 24px;

  .title {
    line-height: 22px;
    margin-bottom: 12px;
  }
}

.img-checkbox {
  margin-right: 16px;
  position: relative;
  border-radius: 4px;
  cursor: pointer;
  width: 55px;
  height: 50px;

  .check-item {
    position: absolute;
    top: 0;
    width: 100%;
    padding-top: 18px;
    padding-left: 15px;
    height: 100%;
    font-size: 14px;
    font-weight: bold;

    .ant-radio-wrapper {
      opacity: 0;
      position: absolute;
      top: 0;
      left: 0;
      width: 48px;
      height: 48px;
    }

    .check-icon {
      display: none;
    }

    &-checked {
      .check-icon {
        display: block;
      }
    }
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.9;

    .ant-radio-wrapper {
      cursor: not-allowed;
    }
  }
}

.color-checkbox {
  position: relative;
  cursor: pointer;

  .check-color {
    width: 20px;
    height: 20px;
    position: absolute;

    .ant-radio-wrapper {
      opacity: 0;
      position: absolute;
      top: 0;
      left: 0;
      margin: 0;
      width: 20px;
      height: 20px;
    }

    .check-icon {
      display: none;
      position: absolute;
      top: 3px;
      left: 3px;
    }

    &-checked {
      .check-icon {
        display: block;
      }
    }
  }
}
</style>
