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
  <a-card :loading="loading" :body-style="{ padding: '20px 24px 8px' }" :bordered="false">
    <div class="chart-card-header">
      <div class="chart-card-meta">
        <span class="chart-card-title">
          <slot name="title">
            {{ title }}
          </slot>
        </span>
        <span class="chart-card-action">
          <slot name="action"></slot>
        </span>
      </div>
      <div class="chart-card-total">
        <slot name="total">
          <span>{{ typeof total === 'function' && total() || total }}</span>
        </slot>
      </div>
    </div>
    <div class="chart-card-content">
      <div class="chart-card-content-fix">
        <slot></slot>
      </div>
    </div>
    <div class="chart-card-footer">
      <div class="chart-card-field">
        <slot name="footer"></slot>
      </div>
    </div>
  </a-card>
</template>

<script>
export default {
  name: 'ChartCard',
  props: {
    title: {
      type: String,
      default: ''
    },
    total: {
      type: [Function, Number, String],
      required: false,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style lang="less" scoped>
.chart-card {
  &-header {
    position: relative;
    overflow: hidden;
    width: 100%;
  }

  &-meta {
    position: relative;
    overflow: hidden;
    width: 100%;
    color: rgba(0, 0, 0, .45);
    font-size: 14px;
    line-height: 22px;
  }

  &-action {
    cursor: pointer;
    position: absolute;
    top: 0;
    right: 0;
  }

  &-footer {
    border-top: 1px solid #e8e8e8;
    padding-top: 9px;
    margin-top: 8px;
  }

  &-field {
    position: relative;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin: 0;
  }

  &-content {
    margin-top: -42px;
    margin-bottom: 12px;
    position: relative;
    height: 100%;
    width: 100%;
  }

  &-content-fix {
      position: relative;
    left: 0;
    bottom: 0;
    width: 100%;
  }

  &-total {
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-all;
    white-space: nowrap;
    color: #000;
    margin-top: 4px;
    margin-bottom: 0;
    font-size: 30px;
    line-height: 38px;
    height: 38px;
  }
}
</style>
