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
  <a-spin :spinning="loading">
    <div v-show="!showAddDetail">
      <a-button type="dashed" style="width: 100%" icon="plus" @click="showAddDetail = true">Add Setting</a-button>
    </div>
    <div v-show="showAddDetail">
      <a-auto-complete
        style="width: 100%"
        :value="newKey"
        :dataSource="Object.keys(detailOptions)"
        placeholder="Name"
        @change="e => onAddInputChange(e, 'newKey')" />
      <a-auto-complete
        style="width: 100%"
        :value="newValue"
        :dataSource="detailOptions[newKey]"
        placeholder="Value"
        @change="e => onAddInputChange(e, 'newValue')" />
      <a-button type="dashed" style="width: 50%" icon="close" @click="showAddDetail = false">Cancel</a-button>
      <a-button type="primary" style="width: 50%" icon="plus" @click="addDetail">Add Setting</a-button>
    </div>
    <a-list size="large">
      <a-list-item :key="index" v-for="(item, index) in details">
        <a-list-item-meta>
          <span slot="title">{{ item.name }}</span>
          <span slot="description" style="word-break: break-all">
            <span v-if="item.edit" style="display: flex">
              <a-auto-complete
                style="width: 100%"
                :value="item.value"
                :dataSource="detailOptions[item.name]"
                @change="val => handleInputChange(val, index)"
                @pressEnter="e => updateDetail(index)" />
              <a-button shape="circle" size="small" @click="updateDetail(index)" style="margin: 2px">
                <a-icon type="check-circle" theme="twoTone" twoToneColor="#52c41a" style="font-size: 24px"/>
              </a-button>
              <a-button shape="circle" size="small" @click="hideEditDetail(index)" style="margin: 2px">
                <a-icon type="close-circle" theme="twoTone" twoToneColor="#eb2f96" style="font-size: 24px"/>
              </a-button>
            </span>
            <span v-else>{{ item.value }}</span>
          </span>
        </a-list-item-meta>
        <div slot="actions">
          <a-button shape="circle" @click="showEditDetail(index)">
            <a-icon type="edit" />
          </a-button>
        </div>
        <div slot="actions">
          <a-popconfirm
            title="Delete setting?"
            @confirm="deleteDetail(index)"
            okText="Yes"
            cancelText="No"
            placement="left"
          >
            <a-button shape="circle">
              <a-icon type="delete" theme="twoTone" twoToneColor="#f5222d" />
            </a-button>
          </a-popconfirm>
        </div>
      </a-list-item>
    </a-list>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'DetailSettings',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      details: [],
      detailOptions: {},
      showAddDetail: false,
      newKey: '',
      newValue: '',
      loading: false,
      resourceType: 'UserVm'
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.updateResource(newItem)
    }
  },
  mounted () {
    this.updateResource(this.resource)
  },
  methods: {
    updateResource (resource) {
      if (!resource) {
        return
      }
      this.resource = resource
      this.resourceType = this.$route.meta.resourceType
      if (!resource.details) {
        return
      }
      this.details = Object.keys(this.resource.details).map(k => {
        return { name: k, value: this.resource.details[k], edit: false }
      })
      api('listDetailOptions', { resourcetype: this.resourceType, resourceid: this.resource.id }).then(json => {
        this.detailOptions = json.listdetailoptionsresponse.detailoptions.details
      })
    },
    showEditDetail (index) {
      this.details[index].edit = true
      this.details[index].originalValue = this.details[index].value
      this.$set(this.details, index, this.details[index])
    },
    hideEditDetail (index) {
      this.details[index].edit = false
      this.details[index].value = this.details[index].originalValue
      this.$set(this.details, index, this.details[index])
    },
    handleInputChange (val, index) {
      this.details[index].value = val
      this.$set(this.details, index, this.details[index])
    },
    onAddInputChange (val, obj) {
      this[obj] = val
    },
    runApi () {
      var apiName = ''
      if (this.resourceType === 'UserVm') {
        apiName = 'updateVirtualMachine'
      } else if (this.resourceType === 'Template') {
        apiName = 'updateTemplate'
      }
      if (!(apiName in this.$store.getters.apis)) {
        this.$notification.error({
          message: 'Failed to execute API: ' + apiName,
          description: 'User is not permitted to use the API'
        })
        return
      }

      const params = { id: this.resource.id }
      this.details.forEach(function (item, index) {
        params['details[0].' + item.name] = item.value
      })
      this.loading = true
      api(apiName, params).then(json => {
        var details = {}
        if (this.resourceType === 'UserVm') {
          details = json.updatevirtualmachineresponse.virtualmachine.details
        } else if (this.resourceType === 'Template') {
          details = json.updatetemplateresponse.template.details
        }
        this.details = Object.keys(details).map(k => {
          return { name: k, value: details[k], edit: false }
        })
      }).catch(error => {
        this.$notification.error({
          message: 'Failed to add setting',
          description: error.response.headers['x-description']
        })
      }).finally(f => {
        this.loading = false
        this.showAddDetail = false
        this.newKey = ''
        this.newValue = ''
      })
    },
    addDetail () {
      this.details.push({ name: this.newKey, value: this.newValue })
      this.runApi()
    },
    updateDetail (index) {
      this.runApi()
    },
    deleteDetail (index) {
      this.details.splice(index, 1)
      this.runApi()
    }
  }
}
</script>
