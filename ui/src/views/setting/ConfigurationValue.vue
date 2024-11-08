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
        <a-tooltip :title="editableValue?'true':'false'">
          <a-switch
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:checked="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)"
          />
         </a-tooltip>
      </span>
      <span v-else-if="configrecord.type ==='Number'">
        <a-tooltip :title="editableValue">
          <a-input-number
            style="width: 20vw;"
            :defaultValue="actualValue"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)"
          />
        </a-tooltip>
      </span>
      <span v-else-if="configrecord.type ==='Decimal'">
        <a-tooltip :title="editableValue">
          <a-input-number
            style="width: 20vw"
            :defaultValue="actualValue"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)"
          />
        </a-tooltip>
      </span>
      <span v-else-if="configrecord.type ==='Range'">
        <a-row>
          <a-col>
            <a-tooltip :title="editableValue">
              <a-slider
                style="width: 13vw"
                class="config-slider-value"
                :defaultValue="configrecord.value * 100"
                :min="0"
                :max="100"
                :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
                v-model:value="editableValue"
                @keydown.esc="editableValueKey = null"
                @pressEnter="updateConfigurationValue(configrecord)"
                @change="value => setConfigurationEditable(configrecord, value)"
              />
            </a-tooltip>
          </a-col>
          <a-col>
            <a-tooltip :title="editableValue">
              <a-input-number
                style="width: 5vw; margin-left: 10px; float: right"
                class="config-slider-text"
                :defaultValue="configrecord.value * 100"
                :min="0"
                :max="100"
                :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
                :formatter="(value) => `${value}%`"
                v-model:value="editableValue"
                @keydown.esc="editableValueKey = null"
                @pressEnter="updateConfigurationValue(configrecord)"
                @change="value => setConfigurationEditable(configrecord, value)"
              />
            </a-tooltip>
          </a-col>
        </a-row>
      </span>
      <span v-else-if="configrecord.type === 'Select'">
        <a-tooltip :title="editableValue">
          <a-select
            style="width: 20vw"
            :defaultValue="actualValue"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)">
            <a-select-option
              v-for="value of configrecord.options.split(',')"
              :key="value">
              {{ value }}
            </a-select-option>
          </a-select>
        </a-tooltip>
      </span>
      <span v-else-if="configrecord.type === 'Order' || configrecord.type === 'WhitespaceSeparatedListWithOptions'">
        <a-tooltip :title="editableValue.join(', ')">
          <b v-if="configrecord.type === 'Order'">
            {{ $t('message.select.deselect.to.sort') }}
          </b>
          <b v-else>
            {{ $t('message.select.deselect.desired.options') }}
          </b>
          <br />
          <a-select
            style="width: 20vw"
            mode="multiple"
            :defaultValue="actualValue"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)">
            <a-select-option
              v-for="value of configrecord.options.split(',')"
              :key="value">
              {{ value }}
            </a-select-option>
          </a-select>
        </a-tooltip>
      </span>
      <span v-else-if="configrecord.type === 'CSV'">
        <a-tooltip :title="editableValue.join(', ')">
          <b>{{ $t('message.type.values.to.add') }}</b>
          <br />
          <a-select
            style="width: 20vw"
            mode="tags"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)">
          </a-select>
        </a-tooltip>
      </span>
      <span v-else>
        <a-tooltip :title="editableValue">
          <a-textarea
            style="width: 20vw; word-break: break-all"
            :defaultValue="actualValue"
            :disabled="(!('updateConfiguration' in $store.getters.apis) || configDisabled)"
            v-model:value="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateConfigurationValue(configrecord)"
            @change="value => setConfigurationEditable(configrecord, value)"
          />
        </a-tooltip>
      </span>
      <span class="actions">
        <tooltip-button
          :tooltip="$t('label.cancel')"
          @onClick="cancelEditConfigurationValue(configrecord)"
          v-if="editableValueKey !== null"
          iconType="CloseCircleTwoTone"
          iconTwoToneColor="#f5222d"
          :disabled="valueLoading" />
        <tooltip-button
          :tooltip="$t('label.ok')"
          @onClick="updateConfigurationValue(configrecord)"
          v-if="editableValueKey !== null"
          iconType="CheckCircleTwoTone"
          iconTwoToneColor="#52c41a"
          :disabled="valueLoading" />
        <tooltip-button
          :tooltip="$t('label.reset.config.value')"
          @onClick="resetConfigurationValue(configrecord)"
          v-if="editableValueKey === null"
          icon="reload-outlined"
          :disabled="(!('resetConfiguration' in $store.getters.apis) || configDisabled || valueLoading)" />
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
    configDisabled: {
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
      valueLoading: this.loading,
      actualValue: null,
      editableValue: null,
      editableValueKey: null
    }
  },
  created () {
    this.setConfigData()
  },
  watch: {
  },
  methods: {
    setConfigData () {
      this.valueLoading = false
      this.editableValue = this.getEditableValue(this.configrecord)
      this.actualValue = this.editableValue
      this.editableValueKey = null
    },
    updateConfigurationValue (configrecord) {
      this.valueLoading = true
      this.editableValueKey = null
      var newValue = this.editableValue
      if (configrecord.type === 'Range') {
        newValue = newValue / 100
      }
      if (['Order', 'CSV'].includes(configrecord.type)) {
        newValue = newValue.join(',')
      }
      if (configrecord.type === 'WhitespaceSeparatedListWithOptions') {
        newValue = newValue.join(' ')
      }
      const params = {
        name: configrecord.name,
        value: newValue
      }
      api('updateConfiguration', params).then(json => {
        this.editableValue = this.getEditableValue(json.updateconfigurationresponse.configuration)
        this.actualValue = this.editableValue
        this.$emit('change-config', { value: newValue })
        this.$store.dispatch('RefreshFeatures')
        this.$messageConfigSuccess(`${this.$t('message.setting.updated')} ${configrecord.name}`, configrecord)
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
        this.editableValue = this.actualValue
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: error?.response?.data?.updateconfigurationresponse?.errortext || this.$t('message.error.save.setting')
        })
      }).finally(() => {
        this.valueLoading = false
        this.$emit('refresh')
      })
    },
    resetConfigurationValue (configrecord) {
      this.valueLoading = true
      this.editableValueKey = null
      api('resetConfiguration', {
        name: configrecord.name
      }).then(json => {
        this.editableValue = this.getEditableValue(json.resetconfigurationresponse.configuration)
        this.actualValue = this.editableValue
        var newValue = this.editableValue
        if (configrecord.type === 'Range') {
          newValue = newValue / 100
        }
        this.$emit('change-config', { value: newValue })
        this.$store.dispatch('RefreshFeatures')
        this.$messageConfigSuccess(`${this.$t('label.setting')} ${configrecord.name} ${this.$t('label.reset.config.value')}`, configrecord)
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
        this.editableValue = this.actualValue
        console.error(error)
        this.$message.error(this.$t('message.error.reset.config'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.reset.config')
        })
      }).finally(() => {
        this.valueLoading = false
        this.$emit('refresh')
      })
    },
    getEditableValue (configrecord) {
      if (configrecord.type === 'Range') {
        return Number(configrecord.value) * 100 || 0
      }
      if (configrecord.type === 'Boolean') {
        return configrecord.value === 'true'
      }
      if (configrecord.type === 'Number' || configrecord.type === 'Decimal') {
        if (configrecord.value) {
          return Number(configrecord.value)
        } else if (configrecord.defaultvalue) {
          return Number(configrecord.defaultvalue)
        }
        return 0
      }
      if (['Order', 'CSV'].includes(configrecord.type)) {
        if (configrecord.value && configrecord.value.length > 0) {
          return String(configrecord.value).split(',')
        } else {
          return []
        }
      }
      if (configrecord.type === 'WhitespaceSeparatedListWithOptions') {
        if (configrecord.value && configrecord.value.length > 0) {
          return String(configrecord.value).split(' ')
        }

        return []
      }
      if (configrecord.value) {
        return String(configrecord.value)
      }
      if (configrecord.defaultvalue) {
        return String(configrecord.defaultvalue)
      }
      return ''
    },
    cancelEditConfigurationValue (configrecord) {
      this.editableValueKey = null
      this.editableValue = this.getEditableValue(configrecord)
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
    margin-left: 10px;
    width: 100px;
}
</style>
