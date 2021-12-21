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
    <span v-if="configrecord.type ==='Boolean'">
      <a-switch
        :defaultChecked="configrecord.value==='true'?true:false"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        :value="configrecord.value"
        @pressEnter="updateConfigurationValue(configrecord)"
        @change="configrecord => setConfigurationEditable(configrecord)"
      />
    </span>
    <span v-else-if="configrecord.type ==='Number'">
      <a-input-number
        :defaultValue="configrecord.value"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        :value="configrecord.value"
        @pressEnter="updateConfigurationValue(configrecord)"
        @change="configrecord => setConfigurationEditable(configrecord)"
      />
    </span>
    <span v-else-if="configrecord.type ==='Decimal'">
      <a-input-number
        :defaultValue="configrecord.value"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        :value="configrecord.value"
        @pressEnter="updateConfigurationValue(configrecord)"
        @change="configrecord => setConfigurationEditable(configrecord)"
      />
    </span>
    <span v-else-if="configrecord.type ==='Range'">
      <a-row :gutter="1">
        <a-col :md="10" :lg="10">
          <a-slider
            :defaultValue="configrecord.value * 100"
            :min="0"
            :max="100"
            :disabled="!('updateConfiguration' in $store.getters.apis)"
            :value="configrecord.value"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="configrecord => setConfigurationEditable(configrecord)"
          />
        </a-col>
        <a-col :md="2" :lg="2">
          <a-input-number
            :defaultValue="configrecord.value * 100"
            :disabled=true
            :value="configrecord.value"
          />
        </a-col>
      </a-row>
    </span>
    <span v-else-if="configrecord.type ==='List'">
      <a-select
        :defaultValue="configrecord.value"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        :value="configrecord.value"
        @pressEnter="updateConfigurationValue(configrecord)"
        @change="configrecord => setConfigurationEditable(configrecord)">
        <a-select-option
          v-for="value in configrecord.values"
          :key="value.val">
          {{ value.text }}
        </a-select-option>
      </a-select>
    </span>
    <span v-else>
      <a-input
        :defaultValue="configrecord.value"
        :disabled="!('updateConfiguration' in $store.getters.apis)"
        :value="configrecord.value"
        @pressEnter="updateConfigurationValue(configrecord)"
        @change="configrecord => setConfigurationEditable(configrecord)"
      />
    </span>
    <tooltip-button
      :tooltip="$t('label.cancel')"
      @onClick="resetConfigurationValue(configrecord)"
      v-if="editable !== null"
      iconType="CloseCircleTwoTone"
      iconTwoToneColor="#f5222d" />
    <tooltip-button
      :tooltip="$t('label.ok')"
      @onClick="updateConfigurationValue(configrecord)"
      v-if="editable !== null"
      iconType="CheckCircleTwoTone"
      iconTwoToneColor="#52c41a" />
    <tooltip-button
      :tooltip="$t('label.reset.config.value')"
      @onClick="resetConfigurationValue(configrecord)"
      v-if="editable !== null"
      icon="reload-outlined"
      :disabled="!('updateConfiguration' in $store.getters.apis)" />
    <label class="font-bold block">
      {{ configrecord.value }}
    </label>
  </div>
</template>
<script>
import { api } from '@/api'

export default {
  name: 'ConfigurationValue',
  props: {
    configrecord: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    actions: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      fetchLoading: false,
      editable: 'true'
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
  },
  methods: {
    fetchData () {
      this.fetchLoading = false
    },
    updateConfigurationValue (configrecord) {
      this.fetchLoading = true
      api('updateConfiguration', {
        name: configrecord.name,
        value: this.editableValue
      }).then(json => {
        configrecord.value = this.editableValue
        this.$store.dispatch('RefreshFeatures')
        this.$message.success(`${this.$t('message.setting.updated')} ${configrecord.name}`)
        if (json.updateconfigurationresponse &&
          json.updateconfigurationresponse.configuration &&
          !json.updateconfigurationresponse.configuration.isdynamic &&
          ['Admin'].includes(this.$store.getters.userInfo.roletype)) {
          this.$notification.warning({
            message: this.$t('label.status'),
            description: this.$t('message.restart.mgmt.server')
          })
        }
      }).catch(error => {
        this.editableValue = configrecord.value
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
      }).finally(() => {
        this.fetchLoading = false
        this.$emit('refresh')
      })
    },
    resetConfigurationValue (configrecord) {
      this.fetchLoading = true
      api('resetConfiguration', {
        name: configrecord.name
      }).then(json => {
        this.$store.dispatch('RefreshFeatures')
        this.$message.success(`${this.$t('message.setting.updated')} ${configrecord.name}`)
        if (json.resetconfigurationresponse &&
          json.resetconfigurationresponse.configuration &&
          !json.resetconfigurationresponse.configuration.isdynamic &&
          ['Admin'].includes(this.$store.getters.userInfo.roletype)) {
          this.$notification.warning({
            message: this.$t('label.status'),
            description: this.$t('message.restart.mgmt.server')
          })
        }
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
      }).finally(() => {
        this.fetchLoading = false
        this.$emit('refresh')
      })
    },
    setConfigurationEditable (configrecord) {
      this.fetchLoading = false
    }
  }
}
</script>
