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
    <a-divider/>

    <div class="title">
      <div class="form__label">
        <tooltip-label :title="$t('label.vnf.nics')" :tooltip="apiParams.vnfnics.description"/>
      </div>
    </div>
    <div>
      <a-button
        type="dashed"
        style="width: 100%"
        :disabled="!('updateVnfTemplate' in $store.getters.apis && isAdminOrOwner())"
        @click="onShowAddVnfNic">
        <template #icon><plus-outlined /></template>
        {{ $t('label.vnf.nic.add') }}
      </a-button>
    </div>

    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="vnfNics"
      :pagination="false"
      :rowKey="record => record.deviceid">
      <template #deviceid="{record}">
        <span> {{ record.deviceid }} </span>
      </template>
      <template #name="{record}">
        <span> {{ record.name }} </span>
      </template>
      <template #required="{ record }">
        <span v-if="record.required">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #management="{ record }">
        <span v-if="record.management">{{ $t('label.yes') }}</span>
        <span v-else>{{ $t('label.no') }}</span>
      </template>
      <template #description="{record}">
        <span> {{ record.description }} </span>
      </template>
      <template #actions="{ record }">
        <div class="shift-btns" v-if="'updateVnfTemplate' in $store.getters.apis && isAdminOrOwner()">
          <a-tooltip placement="top">
            <template #title>{{ $t('label.vnf.nic.edit') }}</template>
            <a-button shape="round" @click="onShowEditVnfNic(record)" class="shift-btn">
              <EditOutlined class="shift-btn" />
            </a-button>
          </a-tooltip>
          <a-tooltip placement="top">
            <template #title>{{ $t('label.move.up.row') }}</template>
            <a-button shape="round" @click="moveVnfNicUp(record)" class="shift-btn">
              <CaretUpOutlined class="shift-btn" />
            </a-button>
          </a-tooltip>
          <a-tooltip placement="top">
            <template #title>{{ $t('label.move.down.row') }}</template>
            <a-button shape="round" @click="moveVnfNicDown(record)" class="shift-btn">
              <CaretDownOutlined class="shift-btn" />
            </a-button>
          </a-tooltip>
          <a-popconfirm
            :title="$t('label.vnf.nic.delete') + '?'"
            @confirm="deleteVnfNic(record)"
            :okText="$t('label.yes')"
            :cancelText="$t('label.no')"
          >
            <template #title>{{ $t('label.vnf.nic.delete') }}</template>
            <a-button shape="round" class="shift-btn" :danger="true" type="primary">
              <DeleteOutlined class="shift-btn" />
            </a-button>
          </a-popconfirm>
        </div>
      </template>
    </a-table>

    <a-modal
      :title="$t('label.vnf.nic.add')"
      :visible="showAddVnfNic"
      :afterClose="closeVnfNicModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeVnfNicModal">

      <div class="new-vnf-nic"  v-ctrl-enter="addVnfNic">
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.deviceid')" :tooltip="$t('label.vnf.nic.deviceid')"/>
          </div>
          <a-input v-model:value="newVnfNic.deviceid" type="number" v-focus="true"></a-input>
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.name')" :tooltip="$t('label.vnf.nic.name')"/>
          </div>
          <a-input v-model:value="newVnfNic.name"></a-input>
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.required')" :tooltip="$t('label.vnf.nic.required')"/>
          </div>
          <a-switch v-model:checked="newVnfNic.required" :checked="true" />
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.vnf.nic.management')" :tooltip="$t('label.vnf.nic.management.description')"/>
          </div>
          <a-switch v-model:checked="newVnfNic.management" :checked="false" />
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <tooltip-label :title="$t('label.description')"  :tooltip="$t('label.vnf.nic.description')"/>
          </div>
          <a-input v-model:value="newVnfNic.description"></a-input>
        </div>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="showAddVnfNic = false">{{ $t('label.cancel') }}</a-button>
        <a-button ref="submit" type="primary" @click="addVnfNic">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      :title="$t('label.vnf.nic.add')"
      :visible="showAddVnfNic"
      :afterClose="closeVnfNicModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeVnfNicModal">

      <div class="new-vnf-nic"  v-ctrl-enter="addVnfNic">
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.deviceid')" :tooltip="$t('label.vnf.nic.deviceid')"/>
          </div>
          <a-input v-model:value="newVnfNic.deviceid" type="number" v-focus="true"></a-input>
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.name')" :tooltip="$t('label.vnf.nic.name')"/>
          </div>
          <a-input v-model:value="newVnfNic.name"></a-input>
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.required')" :tooltip="$t('label.vnf.nic.required')"/>
          </div>
          <a-switch v-model:checked="newVnfNic.required" :checked="true" />
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <span class="new-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.vnf.nic.management')" :tooltip="$t('label.vnf.nic.management.description')"/>
          </div>
          <a-switch v-model:checked="newVnfNic.management" :checked="false" />
        </div>
        <div class="new-vnf-nic__item">
          <div class="new-vnf-nic__label">
            <tooltip-label :title="$t('label.description')"  :tooltip="$t('label.vnf.nic.description')"/>
          </div>
          <a-input v-model:value="newVnfNic.description"></a-input>
        </div>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="showAddVnfNic = false">{{ $t('label.cancel') }}</a-button>
        <a-button ref="submit" type="primary" @click="addVnfNic">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>

    <a-modal
      :title="$t('label.vnf.nic.edit')"
      :visible="showEditVnfNic"
      :afterClose="closeVnfNicModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeVnfNicModal">

      <div class="update-vnf-nic"  v-ctrl-enter="editVnfNic">
        <div class="update-vnf-nic__item">
          <div class="update-vnf-nic__label">
            <span class="update-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.deviceid')" :tooltip="$t('label.vnf.nic.deviceid')"/>
          </div>
          <span> {{ updateVnfNic.deviceid }} </span>
        </div>
        <div class="update-vnf-nic__item">
          <div class="update-vnf-nic__label">
            <span class="update-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.name')" :tooltip="$t('label.vnf.nic.name')"/>
          </div>
          <a-input v-model:value="updateVnfNic.name"></a-input>
        </div>
        <div class="update-vnf-nic__item">
          <div class="update-vnf-nic__label">
            <span class="update-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.required')" :tooltip="$t('label.vnf.nic.required')"/>
          </div>
          <a-switch v-model:checked="updateVnfNic.required" :checked="true" />
        </div>
        <div class="update-vnf-nic__item">
          <div class="update-vnf-nic__label">
            <span class="update-vnf-nic__required">*</span>
            <tooltip-label :title="$t('label.vnf.nic.management')" :tooltip="$t('label.vnf.nic.management.description')"/>
          </div>
          <a-switch v-model:checked="updateVnfNic.management" :checked="false" />
        </div>
        <div class="update-vnf-nic__item">
          <div class="update-vnf-nic__label">
            <tooltip-label :title="$t('label.description')"  :tooltip="$t('label.vnf.nic.description')"/>
          </div>
          <a-input v-model:value="updateVnfNic.description"></a-input>
        </div>
      </div>
      <div :span="24" class="action-button">
        <a-button @click="showEditVnfNic = false">{{ $t('label.cancel') }}</a-button>
        <a-button ref="submit" type="primary" @click="editVnfNic">{{ $t('label.ok') }}</a-button>
      </div>
    </a-modal>

    <a-divider/>
    <div class="title">
      <div class="form__label">
        <tooltip-label :title="$t('label.vnf.details')" :tooltip="apiParams.vnfdetails.description"/>
      </div>
    </div>
    <div v-show="!showAddVnfDetail">
      <a-button
        type="dashed"
        style="width: 100%"
        :disabled="!('updateVnfTemplate' in $store.getters.apis && isAdminOrOwner())"
        @click="onShowAddVnfDetail">
        <template #icon><plus-outlined /></template>
        {{ $t('label.vnf.detail.add') }}
      </a-button>
    </div>

    <div v-show="showAddVnfDetail">
      <a-input-group
        type="text"
        compact>
        <a-auto-complete
          class="detail-input"
          ref="keyElm"
          :filterOption="filterOption"
          v-model:value="newKey"
          :options="detailKeys"
          :placeholder="$t('label.name')" />
        <a-input
          class="tag-disabled-input"
          style=" width: 30px; border-left: 0; pointer-events: none; text-align: center"
          placeholder="="
          disabled />
        <a-select
          v-if="newKey === 'access_methods'"
          class="detail-input"
          v-model:value="newValues"
          mode="multiple"
          :placeholder="$t('label.value')"
          :filterOption="filterOption">
          <a-select-option v-for="opt in detailValues" :key="opt.value" :label="opt.value">
              <span>
                {{ opt.value }}
              </span>
          </a-select-option>
        </a-select>
        <a-input
          v-else
          class="detail-input"
          :filterOption="filterOption"
          v-model:value="newValue"
          :options="detailValues"
          :placeholder="$t('label.value')" />
        <tooltip-button :tooltip="$t('label.add.setting')" :shape="null" icon="check-outlined" @onClick="addVnfDetail" buttonClass="detail-button" />
        <tooltip-button :tooltip="$t('label.cancel')" :shape="null" icon="close-outlined" @onClick="closeVnfDetail" buttonClass="detail-button" />
      </a-input-group>
      <p v-if="error" style="color: red"> {{ $t(error) }} </p>
    </div>

    <a-list size="large">
      <a-list-item :key="index" v-for="(item, index) in vnfDetails">
        <a-list-item-meta>
          <template #title>
            {{ item.name }}
          </template>
          <template #description>
            <div v-if="item.edit" style="display: flex">
              <a-select
                v-if="item.name === 'access_methods'"
                class="detail-input"
                v-model:value="item.values"
                mode="multiple"
                :placeholder="$t('label.value')"
                :filterOption="filterOption">
                <a-select-option v-for="opt in getEditDetailOptions(vnfDetailOptions[item.name])" :key="opt.value" :label="opt.value">
                  <span>
                    {{ opt.value }}
                  </span>
                </a-select-option>
              </a-select>
              <a-auto-complete
                v-else
                style="width: 100%"
                v-model:value="item.displayvalue"
                :options="getEditDetailOptions(vnfDetailOptions[item.name])"
                @change="val => handleInputChange(val, index)"
                @pressEnter="e => updateVnfDetail(index)" />
              <tooltip-button
                buttonClass="edit-button"
                :tooltip="$t('label.cancel')"
                @onClick="hideEditVnfDetail(index)"
                v-if="item.edit"
                iconType="close-circle-two-tone"
                iconTwoToneColor="#f5222d" />
              <tooltip-button
                buttonClass="edit-button"
                :tooltip="$t('label.ok')"
                @onClick="updateVnfDetail(index)"
                v-if="item.edit"
                iconType="check-circle-two-tone"
                iconTwoToneColor="#52c41a" />
            </div>
            <span v-else style="word-break: break-all">{{ item.displayvalue }}</span>
          </template>
        </a-list-item-meta>
        <template #actions>
          <div
            v-if="'updateVnfTemplate' in $store.getters.apis && isAdminOrOwner()">
            <tooltip-button
              :tooltip="$t('label.edit')"
              icon="edit-outlined"
              @onClick="showEditVnfDetail(index)" />
          </div>
          <div
            v-if="'updateVnfTemplate' in $store.getters.apis && isAdminOrOwner()">
            <a-popconfirm
              :title="`${$t('label.delete.setting')}?`"
              @confirm="deleteVnfDetail(index)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
              placement="left"
            >
              <tooltip-button :tooltip="$t('label.delete')" type="primary" :danger="true" icon="delete-outlined" />
            </a-popconfirm>
          </div>
        </template>
      </a-list-item>
    </a-list>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { api } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'TemplateVnfSettings',
  components: {
    TooltipButton,
    TooltipLabel,
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      resourceType: 'VnfTemplate',
      vnfDetailOptions: {},
      columns: [],
      vnfNics: [],
      previousVnfNics: [],
      showAddVnfNic: false,
      showEditVnfNic: false,
      newVnfNic: {
        deviceid: null,
        name: null,
        required: true,
        management: false,
        description: null
      },
      updateVnfNic: {
        deviceid: null,
        name: null,
        required: true,
        management: false,
        description: null
      },
      vnfDetails: [],
      previousVnfDetails: [],
      showAddVnfDetail: false,
      newKey: '',
      newValue: '',
      newValues: [],
      error: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateVnfTemplate')
  },
  computed: {
    detailKeys () {
      return Object.keys(this.vnfDetailOptions).map(key => {
        return { value: key }
      })
    },
    detailValues () {
      if (!this.newKey) {
        return []
      }
      if (!Array.isArray(this.vnfDetailOptions[this.newKey])) {
        if (this.vnfDetailOptions[this.newKey]) {
          return { value: this.vnfDetailOptions[this.newKey] }
        } else {
          return ''
        }
      }
      return this.vnfDetailOptions[this.newKey].map(value => {
        return { value: value }
      })
    }
  },
  created () {
    this.columns = [
      {
        title: this.$t('label.deviceid'),
        dataIndex: 'deviceid',
        slots: { customRender: 'deviceid' }
      },
      {
        title: this.$t('label.name'),
        dataIndex: 'name'
      },
      {
        title: this.$t('label.required'),
        dataIndex: 'required',
        slots: { customRender: 'required' }
      },
      {
        title: this.$t('label.vnf.nic.management'),
        dataIndex: 'management',
        slots: { customRender: 'management' }
      },
      {
        title: this.$t('label.description'),
        dataIndex: 'description',
        slots: { customRender: 'description' }
      },
      {
        title: this.$t('label.action'),
        slots: { customRender: 'actions' }
      }
    ]
    this.columns.push({
      title: '',
      dataIndex: 'action',
      width: 100,
      slots: { customRender: 'action' }
    })

    const userInfo = this.$store.getters.userInfo
    if (!['Admin'].includes(userInfo.roletype) &&
      (userInfo.account !== this.resource.account || userInfo.domain !== this.resource.domain)) {
      this.columns = this.columns.filter(col => { return col.dataIndex !== 'status' })
    }
    this.initForm()
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && !this.showGroupActionModal) {
        this.fetchData()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        zoneid: [{ type: 'array', required: true, message: this.$t('message.error.select') }]
      })
      this.previousVnfNics = this.resource.vnfnics?.slice() || []
      this.previousVnfDetails = []
      if (this.resource.vnfdetails) {
        this.previousVnfDetails = Object.keys(this.resource.vnfdetails).map(k => {
          return {
            name: k,
            value: this.resource.vnfdetails[k],
            displayvalue: this.getDisplayValue(k, this.resource.vnfdetails[k]),
            values: k === 'access_methods' ? this.resource.vnfdetails[k].split(',') : null,
            edit: false
          }
        })
      }
      api('listDetailOptions', { resourcetype: this.resourceType }).then(json => {
        this.vnfDetailOptionsInApi = json.listdetailoptionsresponse.detailoptions.details
        this.vnfDetailOptions = {}
        Object.keys(this.vnfDetailOptionsInApi).sort().forEach(k => {
          this.vnfDetailOptions[k] = this.vnfDetailOptionsInApi[k]
        })
      })
    },
    fetchData () {
      this.vnfNics = this.previousVnfNics.slice() || []
      this.vnfDetails = this.previousVnfDetails.slice() || []
    },
    isAdminOrOwner () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) ||
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.account === this.$store.getters.userInfo.account) ||
        this.resource.project && this.resource.projectid === this.$store.getters.project.id
    },
    onShowAddVnfNic () {
      this.showAddVnfNic = true
    },
    filterOption (input, option) {
      return (
        option.value.toUpperCase().indexOf(input.toUpperCase()) >= 0
      )
    },
    addVnfNic () {
      if (!this.newVnfNic.deviceid || !this.newVnfNic.name) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.please.enter.valid.value')
        })
        return
      }
      if (this.newVnfNic && this.newVnfNic.deviceid && this.newVnfNic.name) {
        this.vnfNics.push({
          deviceid: this.newVnfNic.deviceid,
          name: this.newVnfNic.name,
          required: this.newVnfNic.required,
          management: this.newVnfNic.management,
          description: this.newVnfNic.description
        })
      }
      this.updateVnfTemplateNics()
    },
    deleteVnfNic (record) {
      for (var index = 0; index < this.vnfNics.length; index++) {
        var nic = this.vnfNics[index]
        if (nic.deviceid === record.deviceid) {
          this.vnfNics.splice(index, 1)
          break
        }
      }
      this.updateVnfTemplateNics()
    },
    onShowEditVnfNic (record) {
      this.updateVnfNic.deviceid = record.deviceid
      this.updateVnfNic.name = record.name
      this.updateVnfNic.required = record.required
      this.updateVnfNic.management = record.management
      this.updateVnfNic.description = record.description
      this.showEditVnfNic = true
    },
    getEditDetailOptions (values) {
      if (!values) {
        return
      }
      var data = values.map(value => { return { value: value } })
      return data
    },
    editVnfNic () {
      if (!this.updateVnfNic.name) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.please.enter.valid.value')
        })
        return
      }
      if (this.updateVnfNic && this.updateVnfNic.name) {
        for (var index = 0; index < this.vnfNics.length; index++) {
          var nic = this.vnfNics[index]
          if (nic.deviceid === this.updateVnfNic.deviceid) {
            this.vnfNics[index] = this.updateVnfNic
          }
        }
      }
      this.updateVnfTemplateNics()
    },
    moveVnfNicUp (record) {
      const deviceid = record.deviceid
      let currentNic = null
      let previousNic = null
      for (var index = 0; index < this.vnfNics.length; index++) {
        var nic = this.vnfNics[index]
        if (nic.deviceid === record.deviceid) {
          currentNic = JSON.parse(JSON.stringify(nic))
          this.vnfNics[index] = currentNic
        } else if (nic.deviceid === record.deviceid - 1) {
          previousNic = JSON.parse(JSON.stringify(nic))
          this.vnfNics[index] = previousNic
        }
      }
      if (currentNic && previousNic) {
        currentNic.deviceid = deviceid - 1
        previousNic.deviceid = deviceid
        const currentRequired = currentNic.required
        currentNic.required = previousNic.required
        previousNic.required = currentRequired
        this.updateVnfTemplateNics()
      } else {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.vnf.nic.move.up.fail')
        })
      }
    },
    moveVnfNicDown (record) {
      const deviceid = record.deviceid
      let currentNic = null
      let nextNic = null
      for (var index = 0; index < this.vnfNics.length; index++) {
        var nic = this.vnfNics[index]
        if (nic.deviceid === record.deviceid) {
          currentNic = JSON.parse(JSON.stringify(nic))
          this.vnfNics[index] = currentNic
        } else if (nic.deviceid === record.deviceid + 1) {
          nextNic = JSON.parse(JSON.stringify(nic))
          this.vnfNics[index] = nextNic
        }
      }
      if (currentNic && nextNic) {
        currentNic.deviceid = deviceid + 1
        nextNic.deviceid = deviceid
        const currentRequired = currentNic.required
        currentNic.required = nextNic.required
        nextNic.required = currentRequired
        this.updateVnfTemplateNics()
      } else {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: this.$t('message.vnf.nic.move.down.fail')
        })
      }
    },
    updateVnfTemplateDetails () {
      this.updateVnfTemplate(true, false)
    },
    updateVnfTemplateNics () {
      this.updateVnfTemplate(false, true)
    },
    updateVnfTemplate (areDetailsChanged, areNicsChanged) {
      const apiName = 'updateVnfTemplate'
      if (!(apiName in this.$store.getters.apis)) {
        this.$notification.error({
          message: this.$t('error.execute.api.failed') + ' ' + apiName,
          description: this.$t('message.user.not.permitted.api')
        })
        return
      }

      const params = { id: this.resource.id }
      if (areDetailsChanged) {
        if (this.vnfDetails.length === 0) {
          params.cleanupvnfdetails = true
        } else {
          this.vnfDetails.forEach(function (item, index) {
            params['vnfdetails[0].' + item.name] = item.value
          })
        }
      }

      if (areNicsChanged) {
        let i = 0
        if (this.vnfNics.length === 0) {
          params.cleanupvnfnics = true
        }
        for (var index = 0; index < this.vnfNics.length; index++) {
          var nic = this.vnfNics[index]
          params['vnfnics[' + i + '].deviceid'] = nic.deviceid
          params['vnfnics[' + i + '].name'] = nic.name
          params['vnfnics[' + i + '].required'] = nic.required
          params['vnfnics[' + i + '].management'] = nic.management
          params['vnfnics[' + i + '].description'] = nic.description
          i++
        }
      }

      api(apiName, params).then(json => {
        this.vnfNics = json.updatetemplateresponse.template.vnfnics || []
        const details = json.updatetemplateresponse.template.vnfdetails || []
        this.vnfDetails = Object.keys(details).map(k => {
          return {
            name: k,
            value: details[k],
            displayvalue: this.getDisplayValue(k, details[k]),
            values: k === 'access_methods' ? details[k].split(',') : null,
            edit: false
          }
        })
        this.previousVnfNics = this.vnfNics.slice()
        this.previousVnfDetails = this.vnfDetails.slice()
        this.closeVnfDetail()
        this.closeVnfNicModal()
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      }).finally(f => {
      })
    },
    getDisplayValue (name, value) {
      return name.includes('password') ? '********' : value
    },
    showEditVnfDetail (index) {
      this.vnfDetails[index].edit = true
      this.vnfDetails[index].originalValue = this.vnfDetails[index].value
    },
    hideEditVnfDetail (index) {
      this.vnfDetails[index].edit = false
      this.vnfDetails[index].value = this.vnfDetails[index].originalValue
    },
    handleInputChange (val, index) {
      this.vnfDetails[index].value = val
    },
    updateVnfDetail (index) {
      if (Array.isArray(this.vnfDetails[index].values) && this.vnfDetails[index].values.length > 0) {
        this.vnfDetails[index].value = this.vnfDetails[index].values.join(',')
      }
      this.vnfDetails[index].value = this.vnfDetails[index].displayvalue
      this.vnfDetails[index].displayvalue = this.getDisplayValue(this.vnfDetails[index].name, this.vnfDetails[index].value)
      this.updateVnfTemplateDetails()
    },
    onShowAddVnfDetail () {
      this.showAddVnfDetail = true
      setTimeout(() => {
        this.$refs.keyElm.focus()
      })
    },
    addVnfDetail () {
      if (this.newKey === '' || (this.newValue === '' && this.newValues.length === 0)) {
        this.error = this.$t('message.error.provide.setting')
        return
      }
      this.error = false
      this.newValueString = ''
      if (this.newValues.length > 0) {
        this.newValueString = this.newValues.join(',')
      } else {
        this.newValueString = this.newValue
      }
      this.vnfDetails.push({ name: this.newKey, value: this.newValueString })
      this.updateVnfTemplateDetails()
    },
    deleteVnfDetail (index) {
      this.vnfDetails.splice(index, 1)
      this.updateVnfTemplateDetails()
    },
    closeVnfDetail () {
      this.newKey = ''
      this.newValue = ''
      this.newValues = []
      this.error = false
      this.showAddVnfDetail = false
    },
    closeVnfNicModal () {
      this.showAddVnfNic = false
      this.showEditVnfNic = false
      this.newVnfNic = {
        deviceid: null,
        name: null,
        required: true,
        management: false,
        description: null
      }
    }
  }
}
</script>

<style lang="less" scoped>
.row-element {
  margin-top: 15px;
  margin-bottom: 15px;
}

.detail-input {
  width: calc(calc(100% / 2) - 45px);
}

.detail-button {
  width: 30px;
}

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

.new-vnf-nic {
  &__item {
    margin-bottom: 10px;
  }

  &__label {
    margin-bottom: 5px;
    font-weight: bold;
  }

  &__required {
    margin-right: 5px;
    color: red;
  }
}

.update-vnf-nic {
  &__item {
    margin-bottom: 10px;
  }

  &__label {
    margin-bottom: 5px;
    font-weight: bold;
  }

  &__required {
    margin-right: 5px;
    color: red;
  }
}
</style>
