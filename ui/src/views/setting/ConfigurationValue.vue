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
  <a-list>
    <a-list-item>
      <span v-if="configrecord.type ==='Boolean'">
        <a-switch
          :defaultChecked="configrecord.value==='true'?true:false"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:checked="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="updateConfigurationValue(configrecord)"
          @change="value => setConfigurationEditable(configrecord, value)"
        />
      </span>
      <span v-else-if="configrecord.type ==='Number'">
        <a-input-number
          :defaultValue="configrecord.value"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:value="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="updateConfigurationValue(configrecord)"
          @change="value => setConfigurationEditable(configrecord, value)"
        />
      </span>
      <span v-else-if="configrecord.type ==='Decimal'">
        <a-input-number
          :defaultValue="configrecord.value"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:value="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="updateConfigurationValue(configrecord)"
          @change="value => setConfigurationEditable(configrecord, value)"
        />
      </span>
      <span v-else-if="configrecord.type ==='Range'">
        <a-row :gutter="12">
          <a-col :md="10" :lg="11">
            <a-slider
              class="config-slider-value"
              :defaultValue="configrecord.value * 100"
              :min="0"
              :max="100"
              :disabled="!('updateConfiguration' in $store.getters.apis)"
              v-model:value="editableValue"
              @keydown.esc="editableValueKey = null"
              @pressEnter="updateConfigurationValue(configrecord)"
              @change="value => setConfigurationEditable(configrecord, value)"
            />
          </a-col>
          <a-col :md="4" :lg="4">
            <a-input-number
              class="config-slider-text"
              :defaultValue="configrecord.value * 100"
              :disabled=true
              v-model:value="editableValue"
            />
          </a-col>
        </a-row>
      </span>
      <span v-else-if="configrecord.type ==='List'">
        <a-select
          :defaultValue="configrecord.value"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:value="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="updateConfigurationValue(configrecord)"
          @change="value => setConfigurationEditable(configrecord, value)">
          <a-select-option
            v-for="value in configrecord.values"
            :key="value.val">
            {{ value.text }}
          </a-select-option>
        </a-select>
      </span>
      <span v-else>
        <a-input
          style="width: 13vw;float: right;margin-bottom: 10px; z-index: 8;"
          :defaultValue="configrecord.value"
          :disabled="!('updateConfiguration' in $store.getters.apis)"
          v-model:value="editableValue"
          @keydown.esc="editableValueKey = null"
          @pressEnter="updateConfigurationValue(configrecord)"
          @change="value => setConfigurationEditable(configrecord, value)"
        />
      </span>
      <span class="actions">
        <tooltip-button
          :tooltip="$t('label.cancel')"
          @onClick="cancelEditConfigurationValue(configrecord)"
          v-if="editableValueKey !== null"
          iconType="CloseCircleTwoTone"
          iconTwoToneColor="#f5222d" />
        <tooltip-button
          :tooltip="$t('label.ok')"
          @onClick="updateConfigurationValue(configrecord)"
          v-if="editableValueKey !== null"
          iconType="CheckCircleTwoTone"
          iconTwoToneColor="#52c41a" />
        <tooltip-button
          :tooltip="$t('label.reset.config.value')"
          @onClick="resetConfigurationValue(configrecord)"
          v-if="editableValueKey === null"
          icon="reload-outlined"
          :disabled="!('updateConfiguration' in $store.getters.apis)" />
      </span>
    </a-list-item>
  </a-list>
</template>
<script>
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'ConfigurationValue',
  components: {
    TooltipButton
  },
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
      actualValue: null,
      editableValue: null,
      editableValueKey: null
    }
  },
  created () {
    this.setData()
  },
  watch: {
  },
  methods: {
    setData () {
      this.fetchLoading = false
      this.setEditableValue(this.configrecord)
      this.actualValue = this.editableValue
      this.editableValueKey = null
    },
    updateConfigurationValue (configrecord) {
      console.log(configrecord)
      console.log(this.editableValue)
      this.fetchLoading = true
      this.editableValueKey = null
      var newValue = this.editableValue
      if (configrecord.type === 'Range') {
        newValue = newValue / 100
      }
      const params = {
        name: configrecord.name,
        value: newValue
      }
      api('updateConfiguration', params).then(json => {
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
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.save.setting')
        })
      }).finally(() => {
        this.fetchLoading = false
        this.$emit('refresh')
      })
    },
    resetConfigurationValue (configrecord) {
      this.fetchLoading = true
      this.editableValueKey = null
      api('resetConfiguration', {
        name: configrecord.name
      }).then(json => {
        if (configrecord.type === 'Range') {
          this.editableValue = Number(json.resetconfigurationresponse.configuration.value) * 100
        } else {
          this.editableValue = json.resetconfigurationresponse.configuration.value
        }
        this.$store.dispatch('RefreshFeatures')
        this.$message.success(`${this.$t('label.setting')} ${configrecord.name} ${this.$t('label.reset.config.value')}`)
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
        this.editableValue = configrecord.value
        console.error(error)
        this.$message.error(this.$t('message.error.reset.config'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.reset.config')
        })
      }).finally(() => {
        this.fetchLoading = false
        this.$emit('refresh')
      })
    },
    setEditableValue (configrecord) {
      if (configrecord.type === 'Range') {
        this.editableValue = Number(configrecord.value) * 100
      } else if (configrecord.type === 'Boolean') {
        if (configrecord.value === 'true') {
          this.editableValue = true
        } else {
          this.editableValue = false
        }
      } else if (configrecord.type === 'Number' || configrecord.type === 'Decimal') {
        if (configrecord.value) {
          this.editableValue = Number(configrecord.value)
        } else {
          this.editableValue = Number(configrecord.defaultvalue)
        }
      } else {
        if (configrecord.value) {
          this.editableValue = String(configrecord.value)
        } else {
          this.editableValue = ''
        }
      }
    },
    cancelEditConfigurationValue (configrecord) {
      this.editableValueKey = null
      this.setEditableValue(configrecord)
    },
    setConfigurationEditable (configrecord, value) {
      if (this.actualValue !== this.editableValue) {
        this.editableValueKey = 'edit'
      } else {
        this.editableValueKey = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.actions {
    margin-top: 20px;
    margin-left: -12px;

    @media (min-width: 480px) {
      margin-left: -24px;
    }

    @media (min-width: 760px) {
      margin-top: 0;
      margin-left: 0;
    }
}

.config-slider-value {
  width: 70px;
}

.config-slider-text {
  width: 50px;
  margin-left: 10px;
}
</style>
