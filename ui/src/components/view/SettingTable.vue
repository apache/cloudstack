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
    <a-button type="dashed" style="width: 100%" icon="plus" @click="addSettingForm()">Add Setting</a-button>
    <a-list size="large">
      <a-list-item :key="index" v-for="(item, index) in items">
        <a-list-item-meta>
          <span slot="title">{{ item.name }}</span>
          <span slot="description" style="word-break: break-all">{{ item.value }}</span>
        </a-list-item-meta>
        <div slot="actions">
          <a-button size="small" shape="circle">
            <a-icon type="edit" />
          </a-button>
        </div>
        <div slot="actions">
          <a-button size="small" shape="circle">
            <a-icon type="close" />
          </a-button>
        </div>
      </a-list-item>
    </a-list>
  </div>
</template>

<script>
export default {
  name: 'SettingTable',
  props: {
    items: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
    }
  },
  methods: {
    addSettingForm () {
      console.log('Add setting button clicked')
    }
  }
}
</script>

<style scoped>
</style>
