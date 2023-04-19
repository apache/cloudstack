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
  <a-spin :spinning="loadingData">
    <a-form
            :ref="formRef"
            :model="form"
            :rules="rules"
            layout="vertical"
            class="form"
            v-ctrl-enter="handleSubmit"
            @finish="handleSubmit">
      <div v-if="template && template.userdataid">
        <a-text type="primary">
            The template "{{ $t(this.template.name) }}" is linked to Userdata "{{ $t(this.template.userdataname) }}" with override policy "{{ $t(this.template.userdatapolicy) }}"
        </a-text><br/><br/>
        <div v-if="templateUserDataParams.length > 0 && !doUserdataOverride">
          <a-text type="primary" v-if="this.template && this.template.userdataid && templateUserDataParams.length > 0">
              Enter the values for the variables in userdata
          </a-text>
          <a-input-group>
            <a-table
              size="small"
              style="overflow-y: auto"
              :columns="userDataParamCols"
              :dataSource="templateUserDataParams"
              :pagination="false"
              :rowKey="record => record.key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'value'">
                  <a-input v-model:value="templateUserDataValues[record.key]" />
                </template>
              </template>
            </a-table>
          </a-input-group>
        </div><br/><br/>
      </div>
      <div v-if="userdataDefaultOverridePolicy === 'ALLOWOVERRIDE' || userdataDefaultOverridePolicy === 'APPEND' || !userdataDefaultOverridePolicy">
        <span v-if="userdataDefaultOverridePolicy === 'ALLOWOVERRIDE'" >
          {{ $t('label.userdata.do.override') }}
          <a-switch v-model:checked="doUserdataOverride" style="margin-left: 10px"/>
        </span>
        <span v-if="userdataDefaultOverridePolicy === 'APPEND'">
          {{ $t('label.userdata.do.append') }}
          <a-switch v-model:checked="doUserdataAppend" style="margin-left: 10px"/>
        </span>
        <a-step>
          <template #description>
            <div v-if="doUserdataOverride || doUserdataAppend || !userdataDefaultOverridePolicy" style="margin-top: 15px">
              <a-card
                :tabList="userdataTabList"
                :activeTabKey="userdataTabKey"
                @tabChange="key => onUserdataTabChange(key, 'userdataTabKey')">
                <div v-if="userdataTabKey === 'userdataregistered'">
                  <a-step
                    v-if="isUserAllowedToListUserDatas">
                    <template #description>
                      <div>
                        <user-data-selection
                          :items="items"
                          :row-count="rowCount.userDatas"
                          :disabled="template.userdatapolicy === 'DENYOVERRIDE'"
                          :loading="loading.userDatas"
                          :preFillContent="dataPreFill"
                          @select-user-data-item="($event) => updateUserData($event)"
                          @handle-search-filter="($event) => handleSearchFilter('userData', $event)"
                        />
                        <div v-if="userDataParams.length > 0">
                          <a-input-group>
                            <a-table
                              size="small"
                              style="overflow-y: auto"
                              :columns="userDataParamCols"
                              :dataSource="userDataParams"
                              :pagination="false"
                              :rowKey="record => record.key">
                              <template #bodyCell="{ column, record }">
                                <template v-if="column.key === 'value'">
                                  <a-input v-model:value="userDataValues[record.key]" />
                                </template>
                              </template>
                            </a-table>
                          </a-input-group>
                        </div>
                      </div>
                    </template>
                  </a-step>
                </div>
                <div v-else>
                  <a-form-item name="userdata" ref="userdata" >
                    <a-textarea
                      placeholder="Userdata"
                      v-model:value="form.userdata">
                    </a-textarea>
                  </a-form-item>
                </div>
              </a-card>
            </div>
          </template>
        </a-step>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loadingData" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { genericCompare } from '@/utils/sort.js'
import UserDataSelection from '@views/compute/wizard/UserDataSelection'

export default {
  name: 'ResetUserData',
  components: {
    UserDataSelection
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      items: [],
      total: 0,
      columns: [
        {
          dataIndex: 'name',
          title: this.$t('label.name'),
          sorter: function (a, b) { return genericCompare(a[this.dataIndex] || '', b[this.dataIndex] || '') },
          width: '40%'
        },
        {
          dataIndex: 'account',
          title: this.$t('account'),
          width: '30%'
        },
        {
          dataIndex: 'domain',
          title: this.$t('domain'),
          width: '30%'
        }
      ],
      selectedRowKeys: [],
      options: {
        page: 1,
        pageSize: 10,
        listall: false,
        response: 'json'
      },
      filter: '',
      rowCount: {},
      loading: {
        userDatas: false
      },
      loadingData: false,
      doUserdataOverride: false,
      doUserdataAppend: false,
      userdataTabList: [],
      userdataDefaultOverridePolicy: 'ALLOWOVERRIDE',
      userData: {},
      userDataParams: [],
      userDataParamCols: [
        {
          title: this.$t('label.key'),
          dataIndex: 'key'
        },
        {
          title: this.$t('label.value'),
          dataIndex: 'value',
          key: 'value'
        }
      ],
      userDataValues: {},
      templateUserDataCols: [
        {
          title: this.$t('label.userdata'),
          dataIndex: 'userdata'
        },
        {
          title: this.$t('label.userdatapolicy'),
          dataIndex: 'userdataoverridepolicy'
        }
      ],
      templateUserDataParams: [],
      templateUserDataValues: {},
      template: {},
      userdataTabKey: 'userdataregistered',
      dataPreFill: {},
      userdata: {}
    }
  },
  created () {
    this.initForm()
    this.fetchData()
    this.loadUserdataTabList()
    this.fetchTemplateData()
    this.dataPreFill = this.preFillContent && Object.keys(this.preFillContent).length > 0 ? this.preFillContent : {}
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    fetchData () {
      this.loadingData = true
      this.items = []
      this.total = 0
      api('listUserData', this.options).then(response => {
        this.total = response.listuserdataresponse.count
        if (this.total !== 0) {
          this.items = response.listuserdataresponse.userdata
        }
      }).finally(() => {
        this.loadingData = false
      })
    },
    fetchTemplateData () {
      const params = {}
      params.id = this.resource.templateid
      params.isrecursive = true
      params.templatefilter = 'all'
      var apiName = 'listTemplates'
      api(apiName, params).then(json => {
        const templateResponses = json.listtemplatesresponse.template
        this.template = templateResponses[0]
        this.updateTemplateLinkedUserData(this.template.userdataid)
        this.userdataDefaultOverridePolicy = this.template.userdatapolicy
      })
    },
    updateTemplateLinkedUserData (id) {
      if (id === '0') {
        return
      }
      this.templateUserDataParams = []

      api('listUserData', { id: id }).then(json => {
        const resp = json?.listuserdataresponse?.userdata || []
        if (resp) {
          var params = resp[0].params
          if (params) {
            var dataParams = params.split(',')
          }
          var that = this
          that.templateUserDataParams = []
          if (dataParams) {
            dataParams.forEach(function (val, index) {
              that.templateUserDataParams.push({
                id: index,
                key: val
              })
            })
          }
        }
      })
    },
    loadUserdataTabList () {
      this.userdataTabList = [{
        key: 'userdataregistered',
        tab: this.$t('label.userdata.registered')
      },
      {
        key: 'userdatatext',
        tab: this.$t('label.userdata.text')
      }]
    },
    onUserdataTabChange (key, type) {
      this[type] = key
      this.userDataParams = []
    },
    sanitizeReverse (value) {
      const reversedValue = value
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')

      return reversedValue
    },
    isUserAllowedToListUserDatas () {
      return Boolean('listUserData' in this.$store.getters.apis)
    },
    updateUserData (id) {
      if (id === '0') {
        this.form.userdataid = undefined
        return
      }
      this.form.userdataid = id
      this.userDataParams = []
      api('listUserData', { id: id }).then(json => {
        const resp = json?.listuserdataresponse?.userdata || []
        if (resp.length > 0) {
          var params = resp[0].params
          if (params) {
            var dataParams = params.split(',')
          }
          var that = this
          dataParams.forEach(function (val, index) {
            that.userDataParams.push({
              id: index,
              key: val
            })
          })
        }
      })
    },
    onSelectChange (selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    handleSearch (keyword) {
      this.filter = keyword
      this.options.keyword = keyword
      this.fetchData()
    },
    handleTableChange (pagination) {
      this.options.page = pagination.current
      this.options.pageSize = pagination.pageSize
      this.fetchData()
    },
    handleSubmit () {
      if (this.loadingData) return
      const values = toRaw(this.form)
      this.loadingData = true
      console.log(values)
      const params = {
        id: this.resource.id
      }
      if (values.userdata && values.userdata.length > 0) {
        params.userdata = encodeURIComponent(btoa(this.sanitizeReverse(values.userdata)))
      }
      if (values.userdataid) {
        params.userdataid = values.userdataid
      }
      var idx = 0
      if (this.templateUserDataValues) {
        for (const [key, value] of Object.entries(this.templateUserDataValues)) {
          params['userdatadetails[' + idx + '].' + `${key}`] = value
          idx++
        }
      }
      if (this.userDataValues) {
        for (const [key, value] of Object.entries(this.userDataValues)) {
          params['userdatadetails[' + idx + '].' + `${key}`] = value
          idx++
        }
      }
      api('resetUserDataForVirtualMachine', params).then(json => {
        this.$message.success({
          content: `${this.$t('label.action.userdata.reset')} - ${this.$t('label.success')}`,
          duration: 2
        })
        this.$emit('refresh-data')
        this.closeAction()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loadingData = false
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="scss">
.form {
  width: 90vw;
  @media (min-width: 800px) {
    width: 45vw;
  }
}
</style>
