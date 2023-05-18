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
  <div :class="['form-layout', { 'form-list': selectedRowKeys.length > 0 }]" v-ctrl-enter="handleSubmit">
    <div v-if="selectedRowKeys.length === 0">
      <a-alert type="warning">
        <template #message>
          <span v-html="resource.backupofferingid ? $t('message.action.destroy.instance.with.backups') : $t('message.action.destroy.instance')"></span>
        </template>
      </a-alert>
      <br/>
      <a-spin :spinning="loading">
        <a-form
          :model="form"
          :ref="formRef"
          :rules="rules"
          @finish="handleSubmit"
          layout="vertical">
          <a-form-item
            name="expunge"
            ref="expunge"
            v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.allowuserexpungerecovervm">
            <template #label>
              <tooltip-label :title="$t('label.expunge')" :tooltip="apiParams.expunge.description"/>
            </template>
            <a-switch v-model:checked="form.expunge" v-focus="true" />
          </a-form-item>

          <a-form-item v-if="volumes.length > 0" name="volumeids" ref="volumeids">
            <template #label>
              <tooltip-label :title="$t('label.delete.volumes')" :tooltip="apiParams.volumeids.description"/>
            </template>
            <a-select
              v-model:value="form.volumeids"
              :placeholder="$t('label.delete.volumes')"
              mode="multiple"
              :loading="loading"
              v-focus="$store.getters.userInfo.roletype !== 'Admin' && !$store.getters.features.allowuserexpungerecovervm"
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }" >
              <a-select-option v-for="volume in volumes" :key="volume.id" :label="volume.name">
                {{ volume.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <p v-else v-html="$t('label.volume.empty')" />

          <div :span="24" class="action-button">
            <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
            <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
          </div>
        </a-form>
      </a-spin>
    </div>
    <div v-else>
      <div v-if="!showGroupActionModal">
        <div>
          <a-alert type="error">
            <message-outlined type="exclamation-circle" style="color: red; fontSize: 30px; display: inline-flex" />
            <template #message>
              <span style="padding-left: 5px" v-html="`<b>${selectedRowKeys.length} ` + $t('label.items.selected') + `. </b>`" />
              <span v-html="$t(action.currentAction.message)" />
            </template>
          </a-alert>
        </div>
        <div v-if="selectedRowKeys.length > 0" class="row-keys">
          <a-divider />
          <a-form layout="vertical">
            <a-table
              v-if="selectedRowKeys.length > 0"
              size="middle"
              :columns="chosenColumns"
              :dataSource="selectedItems"
              :rowKey="(record, idx) => record.id || record.name || record.usageType || idx + '-' + Math.random()"
              :pagination="true"
              style="overflow-y: auto"
            >
              <template
                #expandedRowRender="{ record } "
                style="margin: 0">
                <a-form-item :label="$t('label.delete.volumes')" v-if="listVolumes[record.id].opts.length > 0">
                  <a-select
                    mode="multiple"
                    showSearch
                    optionFilterProp="label"
                    :filterOption="(input, option) => {
                      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }"
                    :loading="listVolumes[record.id].loading"
                    :placeholder="$t('label.delete.volumes')"
                    @change="(value) => onChangeVolume(record.id, value)">
                    <a-select-option v-for="item in listVolumes[record.id].opts" :key="item.id" :label="item.name || item.description">
                      {{ item.name || item.description }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <span v-else v-html="$t('label.volume.empty')" />
              </template>
            </a-table>
            <a-form-item v-if="$store.getters.userInfo.roletype === 'Admin' || $store.getters.features.allowuserexpungerecovervm">
              <template #label>
                <tooltip-label :title="$t('label.expunge')" :tooltip="apiParams.expunge.description"/>
              </template>
              <a-switch v-model:checked="expunge" v-focus="true" />
            </a-form-item>
          </a-form>
        </div>

        <div :span="24" class="action-button">
          <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </div>
    <bulk-action-progress
      :showGroupActionModal="showGroupActionModal"
      :selectedItems="selectedItemsProgress"
      :selectedColumns="selectedColumns"
      :message="modalInfo"
      @handle-cancel="handleCancel" />
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'
import BulkActionProgress from '@/components/view/BulkActionProgress'

export default {
  name: 'DestroyVM',
  components: {
    TooltipLabel,
    BulkActionProgress
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    action: {
      type: Object,
      default: () => {}
    },
    selectedRowKeys: {
      type: Array,
      default: () => []
    },
    selectedItems: {
      type: Array,
      default: () => []
    },
    chosenColumns: {
      type: Array,
      default: () => []
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      volumes: [],
      loading: false,
      volumeIds: {},
      listVolumes: {},
      selectedColumns: [],
      selectedItemsProgress: [],
      showGroupActionModal: false,
      modalInfo: {},
      expunge: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('destroyVirtualMachine')
  },
  created () {
    this.formRef = ref()
    this.form = reactive({})
    this.rules = reactive({
      volumeids: [{ type: 'array' }]
    })
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (this.selectedRowKeys.length === 0) {
        this.fetchVolumes()
      } else {
        const promises = []
        this.selectedRowKeys.forEach(vmId => {
          this.listVolumes[vmId] = {
            loading: true,
            opts: []
          }
          promises.push(this.callListVolume(vmId))
        })
        Promise.all(promises).then((data) => {
          data.forEach(item => {
            this.listVolumes[item.id].loading = false
            this.listVolumes[item.id].opts = item.volumes || []
          })
        })
      }
    },
    async fetchVolumes () {
      this.loading = true
      const data = await this.callListVolume(this.resource.id)
      this.volumes = data.volumes || []
      this.loading = false
    },
    callListVolume (vmId) {
      return new Promise((resolve) => {
        this.volumes = []
        api('listVolumes', {
          virtualMachineId: vmId,
          type: 'DATADISK',
          details: 'min',
          listall: 'true'
        }).then(json => {
          const volumes = json.listvolumesresponse.volume || []
          resolve({
            id: vmId,
            volumes
          })
        })
      })
    },
    onChangeVolume (vmId, volumes) {
      this.volumeIds[vmId] = volumes
    },
    handleCancel () {
      this.$emit('cancel-bulk-action')
      this.showGroupActionModal = false
      this.selectedItemsProgress = []
      this.selectedColumns = []
      this.closeAction()
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      if (this.selectedRowKeys.length > 0) {
        this.destroyGroupVMs()
      } else {
        this.formRef.value.validate().then(async () => {
          const values = toRaw(this.form)
          const params = {
            id: this.resource.id
          }
          if (values.volumeids) {
            params.volumeids = values.volumeids.join(',')
          }
          if (values.expunge) {
            params.expunge = values.expunge
          }
          this.loading = true
          try {
            const jobId = await this.destroyVM(params)
            await this.$pollJob({
              jobId,
              title: this.$t('label.action.destroy.instance'),
              description: this.resource.name,
              loadingMessage: `${this.$t('message.deleting.vm')} ${this.resource.name}`,
              catchMessage: this.$t('error.fetching.async.job.result'),
              successMessage: `${this.$t('message.success.delete.vm')} ${this.resource.name}`,
              successMethod: () => {
                if (this.$route.path.includes('/vm/') && values.expunge) {
                  this.$router.go(-1)
                } else {
                  this.parentFetchData()
                }
              },
              action: {
                isFetchData: false
              }
            })
            await this.closeAction()
            this.loading = false
          } catch (error) {
            await this.$notifyError(error)
            await this.closeAction()
            this.loading = false
          }
        }).catch(error => {
          this.formRef.value.scrollToField(error.errorFields[0].name)
        })
      }
    },
    destroyVM (params) {
      return new Promise((resolve, reject) => {
        api('destroyVirtualMachine', params).then(json => {
          const jobId = json.destroyvirtualmachineresponse.jobid
          return resolve(jobId)
        }).catch(error => {
          return reject(error)
        })
      })
    },
    destroyGroupVMs () {
      this.selectedColumns = Array.from(this.chosenColumns)
      this.selectedItemsProgress = Array.from(this.selectedItems)
      this.selectedItemsProgress = this.selectedItemsProgress.map(v => ({ ...v, status: 'InProgress' }))
      this.selectedColumns.splice(0, 0, {
        key: 'status',
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      this.showGroupActionModal = true
      this.modalInfo.title = this.action.currentAction.label
      this.modalInfo.docHelp = this.action.currentAction.docHelp
      const promises = []
      this.selectedRowKeys.forEach(vmId => {
        const params = {}
        params.id = vmId
        if (this.volumeIds[vmId] && this.volumeIds[vmId].length > 0) {
          params.volumeids = this.volumeIds[vmId].join(',')
        }
        if (this.expunge) {
          params.expunge = this.expunge
        }
        promises.push(this.callGroupApi(params))
      })
      this.$message.info({
        content: this.$t(this.action.currentAction.label),
        key: this.action.currentAction.label,
        duration: 3
      })
      this.loading = true
      Promise.all(promises).finally(() => {
        this.loading = false
        this.parentFetchData()
      })
    },
    callGroupApi (params) {
      return new Promise((resolve, reject) => {
        const resource = this.selectedItems.filter(item => item.id === params.id)[0] || {}
        this.destroyVM(params).then(jobId => {
          this.updateResourceState(resource.id, 'InProgress', jobId)
          this.$pollJob({
            jobId,
            showLoading: false,
            bulkAction: false,
            title: this.$t('label.action.destroy.instance'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            successMessage: `${this.$t('message.success.delete.vm')} ${resource.name}`,
            successMethod: () => {
              this.updateResourceState(resource.id, 'success')
              return resolve()
            },
            errorMethod: () => {
              this.updateResourceState(resource.id, 'failed')
            },
            action: {
              isFetchData: false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
          this.updateResourceState(resource.id, 'failed')
          return reject(error)
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    updateResourceState (resource, state, jobId) {
      const objIndex = this.selectedItemsProgress.findIndex(item => item.id === resource)
      if (state && objIndex !== -1) {
        this.selectedItemsProgress[objIndex].status = state
      }
      if (jobId && objIndex !== -1) {
        this.selectedItemsProgress[objIndex].jobid = jobId
      }
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
  .form-layout {
    &.form-list {
      max-width: 60vw;
    }

    &:not(.form-list) {
      width: 60vw;

      @media (min-width: 500px) {
        width: 450px;
      }
    }

    .row-keys {
      .ant-select {
        display: block;
        width: 90%;
      }
    }
  }
</style>
