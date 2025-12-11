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
  <block-radio-group-select>
    <template #radio-option="{ item }">
      <span>
        <resource-icon
        v-if="item && item.icon && item.icon.base64image"
        :image="item.icon.base64image"
          size="2x" />
        <global-outlined size="2x" v-else />
        {{ item.name }}
      </span>
    </template>
    <template #select-option="{ item }">
      <span>
        <resource-icon v-if="item.icon && item.icon.base64image" :image="item.icon.base64image" size="2x" style="margin-right: 5px"/>
        <global-outlined v-else style="margin-right: 5px" />
        {{ item.name }}
      </span>
    </template>
  </block-radio-group-select>
</template>

<script>
import BlockRadioGroupSelect from '@/components/widgets/BlockRadioGroupSelect.vue'
import ResourceIcon from '@/components/view/ResourceIcon'

export default {
  name: 'ZoneBlockRadioGroupSelect',
  components: {
    BlockRadioGroupSelect,
    ResourceIcon
  }
}
</script>
