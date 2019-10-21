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
  <a-tabs :defaultActiveKey="Object.keys(osTypes)[0]">
    <a-tab-pane v-for="(osList, osName) in osTypes" :key="osName">
      <span slot="tab">
        <os-logo :os-name="osName"></os-logo>
      </span>
      <a-form-item>
        <a-radio-group
          v-for="(os, osIndex) in osList"
          :key="osIndex"
          class="radio-group"
          v-decorator="['templateid', {
            rules: [{ required: true, message: 'Please select option' }]
          }]"
        >
          <a-radio
            class="radio-group__radio"
            :value="os.id"
          >{{ os.displaytext }}
          </a-radio>
        </a-radio-group>
      </a-form-item>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import OsLogo from '@/components/widgets/OsLogo'
import { getNormalizedOsName } from '@/utils/icons'

export default {
  name: 'TemplateSelection',
  components: { OsLogo },
  props: {
    templates: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {}
  },
  computed: {
    osTypes () {
      const mappedTemplates = {}
      this.templates.forEach((os) => {
        const osName = getNormalizedOsName(os.ostypename)
        if (Array.isArray(mappedTemplates[osName])) {
          mappedTemplates[osName].push(os)
        } else {
          mappedTemplates[osName] = [os]
        }
      })
      return mappedTemplates
    }
  }
}
</script>

<style lang="less" scoped>
  .radio-group {
    display: flex;
    flex-direction: column;

    &__radio {
      margin: 0.5rem 0;
    }
  }
</style>
