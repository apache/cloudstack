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
  <a-spin :spinning="false" style="background-color: #fff;">
    <a-affix :offsetTop="78">
      <a-card class="breadcrumb-card" style="z-index: 10">
        <a-row>
          <a-col :span="device === 'mobile' ? 24 : 12" style="padding-left: 12px">
            <breadcrumb :resource="resource">
              <template #end>
                <a-button
                  :loading="loading"
                  style="margin-bottom: 5px"
                  shape="round"
                  size="small"
                  @onClick="fetchConfigurationData({ irefresh: true })">
                  <template #icon><reload-outlined /></template>
                  {{ $t('label.refresh') }}
                </a-button>
              </template>
            </breadcrumb>
          </a-col>
          <a-col
            :span="device === 'mobile' ? 24 : 12"
            :style="device === 'mobile' ? { float: 'right', 'margin-top': '12px', 'margin-bottom': '-6px', display: 'table' } : { float: 'right', display: 'table', 'margin-bottom': '-6px' }" >
            <slot name="action"></slot>
            <search-view
              :searchFilters="searchFilters"
              :searchParams="searchParams"
              :apiName="apiName"
              @search="onSearch"
              @change-filter="changeFilter"/>
          </a-col>
        </a-row>
      </a-card>
    </a-affix>
    <a-tabs tabPosition="left">
      <a-tab-pane
        v-for="(idx1, group) in items"
        :key="group"
        :tab="group" >

        <span v-if="group !=='All'">
          <a-tabs >
            <a-tab-pane
              v-for="(idx2, subgroup) in items[group]"
              :key="subgroup"
              :tab="subgroup" >

                <a-table
                  size="middle"
                  :showHeader="false"
                  :columns="columns"
                  :dataSource="items[group][subgroup]"
                  :rowKey="record => record.name"
                  :pagination="false"
                  :rowClassName="getRowClassName"
                  style="overflow-y: auto; margin-left: 10px" >

                  <template #displaytext="{text, record}">
                    {{ text }} + {{ record.name }} + {{ record.description }} + {{ record.value }}
                    <!-- <b>{{ text }}</b> {{ ' (' + record.name + ')' }} <br/> {{ record.description }} -->
                  </template>

                  <template #actions class="action">
                    <template>  <!-- <template #value="{record}"> above -->
                      <span v-if="record.type ==='Boolean'">
                        <a-switch
                          :defaultChecked="record.value==='true'?true:false"
                          :disabled="!('updateConfiguration' in $store.getters.apis)"
                          v-model:value="editableValue[record.name]"
                          @keydown.esc="editableValueKey[record.name] = null"
                          @pressEnter="updateConfigurationValue(record)"
                          @change="record => setConfigurationEditable(record)"
                        />
                      </span>
                      <span v-else-if="record.type ==='Number'">
                        <a-input-number
                          :defaultValue="record.value"
                          :disabled="!('updateConfiguration' in $store.getters.apis)"
                          v-model:value="editableValue[record.name]"
                          @keydown.esc="editableValueKey[record.name] = null"
                          @pressEnter="updateConfigurationValue(record)"
                          @change="record => setConfigurationEditable(record)"
                        />
                      </span>
                      <span v-else-if="record.type ==='Decimal'">
                        <a-input-number
                          :defaultValue="record.value"
                          :disabled="!('updateConfiguration' in $store.getters.apis)"
                          v-model:value="editableValue[record.name]"
                          @keydown.esc="editableValueKey[record.name] = null"
                          @pressEnter="updateConfigurationValue(record)"
                          @change="record => setConfigurationEditable(record)"
                        />
                      </span>
                      <span v-else-if="record.type ==='Range'">
                        <a-row :gutter="1">
                          <a-col :md="10" :lg="10">
                            <a-slider
                              :defaultValue="record.value * 100"
                              :min="0"
                              :max="100"
                              :disabled="!('updateConfiguration' in $store.getters.apis)"
                              v-model:value="editableValue[record.name]"
                              @keydown.esc="editableValueKey[record.name] = null"
                              @pressEnter="updateConfigurationValue(record)"
                              @change="record => setConfigurationEditable(record)"
                            />
                          </a-col>
                          <a-col :md="2" :lg="2">
                            <a-input-number
                              :defaultValue="record.value * 100"
                              :disabled=true
                              v-model:value="editableValue[record.name]"
                            />
                          </a-col>
                        </a-row>
                      </span>
                      <span v-else-if="record.type ==='List'">
                        <a-select
                          :defaultValue="record.value"
                          :disabled="!('updateConfiguration' in $store.getters.apis)"
                          v-model:value="editableValue[record.name]"
                          @keydown.esc="editableValueKey[record.name] = null"
                          @pressEnter="updateConfigurationValue(record)"
                          @change="record => setConfigurationEditable(record)">
                          <a-select-option
                            v-for="value in record.values"
                            :key="value.val">
                            {{ value.text }}
                          </a-select-option>
                        </a-select>
                      </span>
                      <span v-else>
                        <a-input
                          :defaultValue="record.value"
                          :disabled="!('updateConfiguration' in $store.getters.apis)"
                          v-model:value="editableValue[record.name]"
                          @keydown.esc="editableValueKey[record.name] = null"
                          @pressEnter="updateConfigurationValue(record)"
                          @change="record => setConfigurationEditable(record)"
                        />
                      </span>
                      <tooltip-button
                        :tooltip="$t('label.cancel')"
                        @onClick="resetConfigurationValue(record)"
                        v-if="editableValueKey[record.name] !== null"
                        iconType="CloseCircleTwoTone"
                        iconTwoToneColor="#f5222d" />
                      <tooltip-button
                        :tooltip="$t('label.ok')"
                        @onClick="updateConfigurationValue(record)"
                        v-if="editableValueKey[record.name] !== null"
                        iconType="CheckCircleTwoTone"
                        iconTwoToneColor="#52c41a" />
                      <tooltip-button
                        :tooltip="$t('label.reset.config.value')"
                        @onClick="resetConfigurationValue(record)"
                        v-if="editableValueKey[record.name] !== null"
                        icon="reload-outlined"
                        :disabled="!('updateConfiguration' in $store.getters.apis)" />
                    </template>
                  </template>
                </a-table>
            </a-tab-pane>
          </a-tabs>
        </span>
        <span v-else>
          <a-table
              size="middle"
              :showHeader="false"
              :columns="columns"
              :dataSource="items['All']['']"
              :rowKey="record => record.name"
              :pagination="false"
              :rowClassName="getRowClassName"
              style="overflow-y: auto; margin-left: 10px" >

              <template #displaytext="{text, record}">
                {{ text }} + {{ record.name }} + {{ record.description }}
                <!-- <b>{{ text }}</b> {{ ' (' + record.name + ')' }} <br/> {{ record.description }} -->
              </template>

              <template #value="{record}">
                <span v-if="record.type ==='Boolean'">
                  <a-switch
                    :defaultChecked="record.value==='true'?true:false"
                    :disabled="!('updateConfiguration' in $store.getters.apis)"
                    v-model:value="editableValue[record.name]"
                    @keydown.esc="editableValueKey[record.name] = null"
                    @pressEnter="updateConfigurationValue(record)"
                    @change="record => setConfigurationEditable(record)"
                    :loading="recordLoading"
                  />
                </span>
                <span v-else-if="record.type ==='Number'">
                  <a-input-number
                    :defaultValue="record.value"
                    :disabled="!('updateConfiguration' in $store.getters.apis)"
                    v-model:value="editableValue[record.name]"
                    @keydown.esc="editableValueKey[record.name] = null"
                    @pressEnter="updateConfigurationValue(record)"
                    @change="record => setConfigurationEditable(record)"
                  />
                </span>
                <span v-else-if="record.type ==='Decimal'">
                  <a-input-number
                    :defaultValue="record.value"
                    :disabled="!('updateConfiguration' in $store.getters.apis)"
                    v-model:value="editableValue[record.name]"
                    @keydown.esc="editableValueKey[record.name] = null"
                    @pressEnter="updateConfigurationValue(record)"
                    @change="record => setConfigurationEditable(record)"
                  />
                </span>
                <span v-else-if="record.type ==='Range'">
                  <a-row :gutter="1">
                    <a-col :md="10" :lg="10">
                      <a-slider
                        :defaultValue="record.value * 100"
                        :min="0"
                        :max="100"
                        :disabled="!('updateConfiguration' in $store.getters.apis)"
                        v-model:value="editableValue[record.name]"
                        @keydown.esc="editableValueKey[record.name] = null"
                        @pressEnter="updateConfigurationValue(record)"
                        @change="record => setConfigurationEditable(record)"
                      />
                    </a-col>
                    <a-col :md="2" :lg="2">
                      <a-input-number
                        :defaultValue="record.value * 100"
                        :disabled=true
                        v-model:value="editableValue[record.name]"
                      />
                    </a-col>
                  </a-row>
                </span>
                <span v-else-if="record.type ==='List'">
                  <a-select
                    :defaultValue="record.value"
                    :disabled="!('updateConfiguration' in $store.getters.apis)"
                    v-model:value="editableValue[record.name]"
                    @keydown.esc="editableValueKey[record.name] = null"
                    @pressEnter="updateConfigurationValue(record)"
                    @change="record => setConfigurationEditable(record)">
                    <a-select-option
                      v-for="value in record.values"
                      :key="value.val">
                      {{ value.text }}
                    </a-select-option>
                  </a-select>
                </span>
                <span v-else>
                  <a-input
                    :defaultValue="record.value"
                    :disabled="!('updateConfiguration' in $store.getters.apis)"
                    v-model:value="editableValue[record.name]"
                    @keydown.esc="editableValueKey[record.name] = null"
                    @pressEnter="updateConfigurationValue(record)"
                    @onClick="record => setConfigurationEditable(record)"
                    @change="record => setConfigurationEditable(record)"
                  />
                </span>
              </template>
              <template #actions="{record}">
                <tooltip-button
                  :tooltip="$t('label.cancel')"
                  @onClick="resetConfigurationValue(record)"
                  v-if="this.editableValueKey[record.name] !== null"
                  iconType="close-circle"
                  iconTwoToneColor="#f5222d" />
                <tooltip-button
                  :tooltip="$t('label.ok')"
                  @onClick="updateConfigurationValue(record)"
                  v-if="editableValueKey[record.name] !== null"
                  iconType="check-circle"
                  iconTwoToneColor="#52c41a" />
              </template>
            </a-table>
        </span>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import Breadcrumb from '@/components/widgets/Breadcrumb'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import ActionButton from '@/components/view/ActionButton'
import InfoCard from '@/components/view/InfoCard'
import QuickView from '@/components/view/QuickView'
import TooltipButton from '@/components/widgets/TooltipButton'
import SearchView from '@/components/view/SearchView'

export default {
  name: 'GlobalSettingsView',
  components: {
    Breadcrumb,
    Console,
    OsLogo,
    Status,
    ActionButton,
    InfoCard,
    QuickView,
    TooltipButton,
    SearchView
  },
  propsData: {
    // columns: {
    //   type: Array,
    //   required: true
    // },
    // items: {
    //   type: Array,
    //   required: true
    // },
    loading: {
      type: Boolean,
      default: false
    },
    actions: {
      type: Array,
      default: () => []
    },
    // editableValueKey: {
    //   type: Object,
    //   default () {
    //     return {}
    //   }
    // },
    editableValue: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      columns: [
        {
          title: 'Display Text',
          dataIndex: 'displaytext',
          scopedSlots: { customRender: 'displaytext' }
        },
        {
          title: 'Value',
          dataIndex: 'value',
          scopedSlots: { customRender: 'value' }
        }
      ],
      items: {},
      selectedRowKeys: [],
      editableValueKey: {},
      // editableValue: {},
      tabLoading: false,
      recordLoading: false,
      dataView: true,
      searchView: true,
      searchFilters: [],
      searchParams: {},
      filter: '',
      apiName: 'listConfigurations'
    }
  },
  watch: {
    editableValue: function (newVal, oldVal) {
      console.log('watching...')
      console.log(newVal)
      console.log(oldVal)
      for (var i = 0; i < this.editableValue.length; i++) {
      }
    }
  },
  created () {
    this.fetchConfigurationData()
  },
  methods: {
    fetchData (callback) {
      this.tabLoading = true
      const params = {
        [this.scopeKey]: this.resource.id,
        listAll: true
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      api('listConfigurations', params).then(response => {
        this.items = response.listconfigurationsresponse.configuration
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.tabLoading = false
        if (!callback) return
        callback()
      })
    },
    fetchConfigurationData () {
      this.tabLoading = true
      const params = {
        listAll: true,
        pagesize: -1
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      api('listConfigurations', params).then(response => {
        const configItems = response.listconfigurationsresponse.configuration
        console.log(configItems)
        const items = {}
        const allGroup = 'All'
        const nullSubGroup = ''
        items[allGroup] = {}
        items[allGroup][nullSubGroup] = []
        configItems.map(configItem => {
          items[allGroup][nullSubGroup].push(configItem)
          this.editableValueKey[configItem.name] = configItem.value
          // this.editableValueKey[configItem.name] = null
          // if (configItem.type === 'Range') {
          //   this.editableValue[configItem.name] = configItem.value * 100
          // } else if (configItem.type === 'Boolean') {
          //   if (configItem.value === 'true') {
          //     this.editableValue[configItem.name] = true
          //   } else {
          //     this.editableValue[configItem.name] = false
          //   }
          // } else {
          //   this.editableValue[configItem.name] = configItem.value
          // }
          if (configItem.group in items) {
            if (configItem.subgroup in items[configItem.group]) {
              items[configItem.group][configItem.subgroup].push(configItem)
            } else {
              items[configItem.group][configItem.subgroup] = [configItem]
            }
          } else {
            items[configItem.group] = {}
            items[configItem.group][configItem.subgroup] = [configItem]
          }
        })
        console.log(items)
        for (var key in items) {
          console.log(key)
          for (var k in items[key]) {
            console.log(' - ', k)
          }
        }
        this.items = items
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.tabLoading = false
      })
    },
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'light-row'
      }
      return 'dark-row'
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    updateConfigurationValue (record) {
      this.tabLoading = true
      this.editableValueKey[record.name] = null
      api('updateConfiguration', {
        name: record.name,
        value: this.editableValue[record.name]
      }).then(json => {
        record.value = this.editableValue[record.name]
        this.$store.dispatch('RefreshFeatures')
        this.$message.success(`${this.$t('message.setting.updated')} ${record.name}`)
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
        this.editableValue[record.name] = record.value
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
      }).finally(() => {
        this.tabLoading = false
        this.$emit('refresh')
      })
    },
    resetConfigurationValue (record) {
      this.tabLoading = true
      this.editableValueKey[record.name] = null
      this.editableValue[record.name] = record.value
      this.tabLoading = false
    },
    setConfigurationEditable (record) {
      this.editableValueKey[record.name] = record.name
    },
    onSearch (opts) {
    },
    changeFilter (filter) {
    }
  }
}
</script>

<style scoped>
/deep/ .ant-table-thead {
  background-color: #f9f9f9;
}

/deep/ .ant-table-small > .ant-table-content > .ant-table-body {
  margin: 0;
}

/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>

<style scoped lang="scss">
  .shift-btns {
    display: flex;
  }
  .shift-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    font-size: 12px;

    &:not(:last-child) {
      margin-right: 5px;
    }

    &--rotated {
      font-size: 10px;
      transform: rotate(90deg);
    }

  }

  .alert-notification-threshold {
    background-color: rgba(255, 231, 175, 0.75);
    color: #e87900;
    padding: 10%;
  }

  .alert-disable-threshold {
    background-color: rgba(255, 190, 190, 0.75);
    color: #f50000;
    padding: 10%;
  }
</style>
